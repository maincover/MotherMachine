
import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;

import Ij_Plugin.Ins_find_peaks;
import Stabilizer.Ins_param;
import Stabilizer.Ins_stabilizer;
import Threshold.Auto_Threshold_v;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.CurveFitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


public class Ins_seg_preprocessing {
	private ImagePlus mImp = null;
	private boolean debug_mode=false;
	private int startX_ref;
	private int startY_ref;
	private boolean imageCropped = false;
	
	private float inter_channel;//49.45f
	private float roi_width;
	private float channel_prefix_pos[];
	private Ins_param ins_param; 
	private int height_align; 
	

	public Ins_seg_preprocessing(ImagePlus imp, Ins_param ins_param) {
		mImp = imp;
		imageCropped = false;
		this.startX_ref = ins_param.getStartX();
		this.startY_ref = ins_param.getStartY();				
		this.channel_prefix_pos = ins_param.getchannel_prefix_pos();
		this.inter_channel = ins_param.getInterChannelLength();
		this.roi_width = ins_param.getRoi_width();
		this.height_align = ins_param.getHeight_align();
		this.ins_param = ins_param;
		imp.unlock();
	}


	public void buildImages_preProcess(String posName){
		ImagePlus mImp_crop;
		if (mImp.getHeight() == height_align)
			this.imageCropped = true;
		
		if (!imageCropped)
		{
			mImp_crop=cropImage(mImp,debug_mode);//least square method to get rough center positions		
			mImp_crop.setTitle(posName+"_Ins_Cropped");
		}else {
			mImp_crop = mImp;
			IJ.showMessage("Image already aligned, segmentation directly on cropped image");
		}
		
		IJ.log("Start stabilizer...");
		Ins_stabilizer stabilizer = new Ins_stabilizer(ins_param, mImp_crop);		
		if(stabilizer.run())
		{
			this.mImp=stabilizer.getStackout();
			this.mImp.setTitle(posName+"_Ins_Stabilized");
		}else {
			IJ.showMessage("Stabilize process cancelled, only the crop image will be used for segmentation");
			this.mImp = mImp_crop;
		}		
	}
	
	
	public ImagePlus getCurrentImage()
	{
		if(mImp == null)
		{
			IJ.error("Image NULL");
			throw new Error("Image not ready");
		}
		else {			
			return mImp;			
		}		
	}
	
	
	/**
	 * 
	 * @param im
	 *
	 * @param size_tube important for eliminating the bottom edge
	 * @param debug
	 * @return
	 */
	public ImagePlus cropImage(ImagePlus im, Boolean debug){
			ImagePlus img=im;
			if(img == null)
				return null;
			float[] Gx=new float[]{
					-1,0,1,
					-1,0,1,
					-1,0,1
			};
			
			float[] Gx2=new float[]{
					1,0,-1,
					1,0,-1,
					1,0,-1
			};
			
			//int width_align = ins_param.getWidth_align();
			int height_align = ins_param.getHeight_align();
			float[] f_s = ins_param.getchannel_prefix_pos();
			int width_crop_image = (int)f_s[f_s.length-1];
			
			int width=im.getWidth();
			int height = im.getHeight();
			

			
			//-----------------------Step 1, Update the inter-distance between channels--------------
			int pSize = 100;
			if (pSize >= img.getStackSize())
				pSize = img.getStackSize();
			
			float[][] pos_refine_l_array = new float[pSize][];
			float[][] pos_refine_r_array = new float[pSize][];
			ImagePlus[] eigenLargestImp_array = new ImagePlus[pSize];
			ImagePlus[] eigenSmallestImp_array = new ImagePlus[pSize]; 
			ByteProcessor[] bpEigenLargest_array = new ByteProcessor[pSize];
			int[] position_v_array = new int[pSize];
			ImageProcessor[] ip_y0_array = new ImageProcessor[pSize];
			ImageProcessor[] ip_y1_array = new ImageProcessor[pSize];
			for (int i=1; i<=pSize; i++) {
				IJ.showStatus("Cropping " + i + "/" + img.getStackSize() + 
						" ... (Press 'ESC' to Cancel)");
				if (IJ.escapePressed())
					break;
				ImageProcessor ip = img.getImageStack().getProcessor(i);
				ImagePlus[] eigenImp = (new featureJ.FJ_Structure()).getEigenImg(new ImagePlus("", ip),"8.0","3.0");
				ImagePlus eigenLargestImp = eigenImp[0];
				ImagePlus eigenSmallestImp = eigenImp[1];
				eigenLargestImp_array[i-1] = eigenLargestImp;
				eigenSmallestImp_array[i-1] = eigenSmallestImp;
				
				ShortProcessor spEigenLargest = eigenLargestImp.getProcessor().convertToShortProcessor();
				int level = Auto_Threshold_v.getThreshold("Mean", spEigenLargest);
				spEigenLargest.threshold(level);
				ByteProcessor bpEigenLargest = spEigenLargest.convertToByteProcessor();
				bpEigenLargest.autoThreshold();	
				bpEigenLargest_array[i-1] = bpEigenLargest;				
				
				double ratio = 0.2;
				int position_v = startY_ref;
				if(startY_ref>0)
				{
					int position_v0 = (int)(startY_ref-height_align*ratio >0? startY_ref - height_align*ratio:0);
					for(int v=position_v0;v<startY_ref + height_align; v++)
					{
						int countZero = 0;
						if(v<0 || v>=height)
							continue;						
						for(int u=startX_ref;u<startX_ref+width_crop_image;u++)
						{
							if(u<0 || u>= width)
								continue;							
							if(bpEigenLargest.get(u, v) == 255)
								countZero ++;
						}
						if (countZero > 100)
						{
							position_v = v;
							System.out.println("	slice - " +i+" find position y: " + position_v);
							break;
						}
					}
				}else {
					position_v = Math.abs(position_v);
				}
				if(position_v + height_align > ip.getHeight())
				{
					position_v = ip.getHeight() - height_align;
				}
				
				position_v_array[i-1] = position_v;
				bpEigenLargest.setRoi(0, position_v, width, (int)(height_align));										
				ImageProcessor ip_y0 = bpEigenLargest.crop();
				ImageProcessor ip_y1 = bpEigenLargest.crop();																		
				ip_y0.convolve(Gx, 3, 3);
				ip_y1.convolve(Gx2, 3, 3);				
				ip_y0_array[i-1] = ip_y0;
				ip_y1_array[i-1] = ip_y1;
				
				pos_refine_l_array[i-1] = refinePosition_toArray(ip_y0, (int)(startX_ref-0.5*roi_width));
				pos_refine_r_array[i-1] = refinePosition_toArray(ip_y1, (int)(startX_ref+roi_width));//int pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l+channel_prefix_pos[1]*0.3-channel_prefix_pos[0]*0.3));
				if(i==-1)
				{
					for(int k=0;k<pos_refine_l_array[0].length;k++)
						Ins_seg_panel.addRoiToManager(new Line(pos_refine_l_array[0][k], 0, pos_refine_l_array[0][k], ip_y0.getHeight()));
					
					for(int k=0;k<pos_refine_r_array[0].length;k++)
					{
						Roi.setColor(Color.RED);
						Roi l2 = new Line(pos_refine_r_array[0][k], 0, pos_refine_r_array[0][k], ip_y0.getHeight());
						//l2.setColor(Color.RED);
						Ins_seg_panel.addRoiToManager(l2);
					}
					
					ip.setRoi(0, position_v, width, (int)(height_align));
					new ImagePlus("original",ip.crop()).show();
						
					new ImagePlus("ip_y1",ip_y1).show();
						
					new ImagePlus("ip_y0",ip_y0).show();
					/*ByteProcessor bp_y0 = (ByteProcessor)ip_y0.duplicate();
					bp_y0.setInterpolationMethod(ImageProcessor.NONE);
					bp_y0.setBackgroundValue(0);
					bp_y0.rotate(2);
					new ImagePlus("bp_y0 2 degree",bp_y0).show();*/
					//break;
				}
			}
			
			float[][] xDiff = new float[pos_refine_l_array[0].length][width];
			
			for(int i=0;i<pSize; i++)
			{
				for(int j=0; j<pos_refine_l_array[i].length;j++)
				{
					float diff = pos_refine_l_array[i][j]-pos_refine_l_array[i][0];
					xDiff[j][(int)(diff)]++;
					diff = pos_refine_r_array[i][j]-pos_refine_r_array[i][0];
					xDiff[j][(int)(diff)]++;					
				}
			}
			
			float[] xDiff_max = new float[xDiff.length];
			for(int i=0; i<xDiff.length;i++)
			{
				System.out.println("diff i : "+ i );
				int max_diff = -1;
				int j_save = 0;
				for(int j=0; j<xDiff[i].length;j++)
				{
					if(xDiff[i][j] > 0)
					{
						if(xDiff[i][j]>max_diff)
						{
							max_diff = (int)xDiff[i][j];
							j_save = j;
						}
					}	
				}
				xDiff_max[i] = j_save;
				System.out.println("inter distance : " + j_save + " count :" + max_diff);
			}
			ins_param.update_channel_prefix_pos(xDiff_max);
			int width_align = ins_param.getWidth_align();
			if(width < width_align)
			{
				IJ.error("cropped width is bigger than original width");
				return null;
			}
			
			// --------------------End Step 1 ------------------------------
			// ---------------------Step 1-2 Fit curver to get rid of outlier
			/*double[] sorted_xDiff = new double[xDiff_max.length];
			System.arraycopy(xDiff_max, 0, sorted_xDiff, 0, xDiff_max.length);
			Arrays.sort(sorted_xDiff);
			double median = sorted_xDiff[sorted_xDiff.length/2 - 1>=0?sorted_xDiff.length/2 - 1:0];
			double[] x_diff = new double[xDiff_max.length];
			System.arraycopy(xDiff_max, 0, x_diff, 0, x_diff.length);
			for(int i=0; i<x_diff.length;i++)
			{
				if((xDiff_max[i] - median) >= 5) // get rid of some outliers for fitting
				{
					x_diff[i] = median;
				}
			}
			
			
			double[] xData = new double[xDiff_max.length];
			double[] yData = new double[xDiff_max.length];
			for(int i=0; i<xData.length ; i++ )
			{
				xData[i] = i+1;
				yData[i] = xDiff_max[i];
			}
			CurveFitter cF = new CurveFitter(xData, yData);
			cF.doFit(CurveFitter.POLY3);
			IJ.log(cF.getStatusString()+ " good fitness : "+ cF.getFitGoodness());
			
			double[] residual = cF.getResiduals();
			double sd_residual = cF.getSD();
			for(int i=0; i<residual.length;i++)
			{
				double z_score = residual[i]/sd_residual;
				IJ.log("(Inter) Original value at :  "+ i + " is "+ yData[i] + " fitted value "+ cF.f(xData[i]) +" residual : " + residual[i]+  " sd :"+ sd_residual+  " zscore: " + z_score);
				if(Math.abs(z_score) >= 1.0)
				{
					double n_v =  cF.f(xData[i]);
					System.out.println("Change the outlier at point i "+ i + " value of "+ yData[i] + " by value of "+ n_v);
					IJ.log("	(Inter)Change the outlier at point i "+ i + " value of "+ yData[i] + " by value of "+ n_v);
					yData[i] = n_v;
				}
			}
			for(int i=0; i<xDiff_max.length-1;i++)
			{
				System.out.println("(Inter)i: "+ i + " stats_y : " + xDiff_max[i] + " yData : "  + yData[i]);
			}*/
			//ins_param.update_channel_prefix_pos(xDiff_max);
			
			// ----------------------End Step 1-2
			
			//----------------------Step 2 : estimate relative y position------------------
			int[][] relativeY=new int[pSize][];
			for (int i=1; i<=pSize; i++) {
				IJ.showStatus("Cropping " + i + "/" + img.getStackSize() + 
						" ... (Press 'ESC' to Cancel)");
				if (IJ.escapePressed())
					break;
			
				ImageProcessor ip_y0 = ip_y0_array[i-1];
				ImageProcessor ip_y1 = ip_y1_array[i-1];
				
				float pos_refine_l = refinePosition(ip_y0, (int)(startX_ref-0.5*roi_width), false);//int pos_refine_l = refinePosition(ip_y0, startX_ref);
				float pos_refine_r = refinePosition(ip_y1, (int)(startX_ref+roi_width),true);//int pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l+channel_prefix_pos[1]*0.3-channel_prefix_pos[0]*0.3));
				
				if(i==1)
				{
					float[] pos_x = ins_param.getchannel_prefix_pos();
					for(int k=0; k<pos_x.length;k=k+2)
						Ins_seg_panel.addRoiToManager(new Roi(pos_refine_l+pos_x[k], 0, pos_refine_r-pos_refine_l, ip_y0.getHeight()));
					
					new ImagePlus("ip_y1",ip_y1).show();
					new ImagePlus("ip_y0",ip_y0).show();

					ImageProcessor ip = img.getImageStack().getProcessor(i);
					ip.setRoi(0, position_v_array[i-1], width, (int)(height_align));
					new ImagePlus("original",ip.crop()).show();
					
					//break;
				}
				
				double position_x= (pos_refine_l+pos_refine_r)*0.5;
				int position_v = position_v_array[i-1];
				//float[] position_x = ins_param.getchannel_prefix_pos();
				ImagePlus eigenSmallestImp = eigenSmallestImp_array[i-1];
				relativeY[i-1] = refinePositionChannelHead(ins_param, eigenSmallestImp, (float)position_x, position_v, i);
			}
			
			int[][] pos_relative_y = new int[relativeY[0].length][height];
			for(int i=0;i<relativeY.length;i++)
			{
				for(int j=0; j<relativeY[i].length;j++)
				{
					//System.out.println("j : "+ j + " length : " + relativeY[i].length + " v : " +relativeY[i][j]);
					pos_relative_y[j][relativeY[i][j]]++;
				}
			}
			
			for(int i=0;i<pos_relative_y.length;i++)
			{
				System.out.println("relative_y stats : "+ i);
				for(int j=0;j<pos_relative_y[i].length;j++)
				{
					if(pos_relative_y[i][j]>0)
						System.out.println("relative y : "+ j + " count : "+ pos_relative_y[i][j]);
				}
			}
			
			int[] stats_y = new int[relativeY[0].length];
			for(int i=0;i<pos_relative_y.length;i++)
			{
				System.out.println("i : "+i);
				int max_v = 0;
				int j_sav = 0;
				for(int j=0; j<pos_relative_y[i].length;j++)
				{
					if(max_v<=pos_relative_y[i][j])
					{
						max_v = pos_relative_y[i][j];
						j_sav = j;
					}
				}
				stats_y[i] = j_sav;
				System.out.println("value y : " + j_sav);;
			}
			//------------End Step 2------------------
			//-------------Step 2-3 curve fitting to get rid of outlier------
			double[] xData = new double[stats_y.length - 1];
			double[] yData = new double[stats_y.length - 1];
			for(int i=0; i<xData.length ; i++ )
			{
				xData[i] = i+1;
				yData[i] = stats_y[i];
			}
			CurveFitter cF = new CurveFitter(xData, yData);
			cF.doFit(CurveFitter.POLY3);
			IJ.log(cF.getStatusString()+ " good fitness : "+ cF.getFitGoodness());
			
			double[] residual = cF.getResiduals();
			double sd_residual = cF.getSD();
			for(int i=0; i<residual.length;i++)
			{
				double z_score = residual[i]/sd_residual;
				IJ.log("Original value at :  "+ i + " is "+ yData[i] + " fitted value "+ cF.f(xData[i]) +" residual : " + residual[i]+  " sd :"+ sd_residual+  " zscore: " + z_score);
				if(Math.abs(z_score) >= 1.0)
				{
					double n_v =  cF.f(xData[i]);
					System.out.println("Change the outlier at point i "+ i + " value of "+ yData[i] + " by value of "+ n_v);
					IJ.log("	Change the outlier at point i "+ i + " value of "+ yData[i] + " by value of "+ n_v);
					yData[i] = n_v;
				}
			}
			for(int i=0; i<stats_y.length-1;i++)
			{
				stats_y[i] = (int)(yData[i]);
			}
			
			//-------------End step 2-3-----------------
			
			//---------------Step 3 re-estimate the crop image position---------------
			int max_relative = 0;
			for(int i=0;i<stats_y.length-1;i++)
			{
				if(Math.abs(stats_y[i])>max_relative)
					max_relative = Math.abs(stats_y[i]);
			}
			// update height_align
			height_align = height_align + max_relative;
			ins_param.setHeight_align(height_align);
			
			ImageStack imp_crop=new ImageStack(width_align, height_align);
			
			int heightAlignMin = stats_y[stats_y.length-1];			
			ins_param.setHeight_align_min(heightAlignMin);
			int[] relative_headPosition = new int[stats_y.length-1];
			System.arraycopy(stats_y, 0, relative_headPosition, 0, relative_headPosition.length);
			ins_param.setRelative_headPosition(relative_headPosition);	
			
			ins_param.setPosition_l(new int[img.getStackSize()]);
			ins_param.setPosition_h(new int[img.getStackSize()]);
			
			for (int i=1; i<=img.getStackSize(); i++) {
				IJ.showStatus("Cropping " + i + "/" + img.getStackSize() + 
						" ... (Press 'ESC' to Cancel)");
				if (IJ.escapePressed())
					break;
				ImageProcessor ip = img.getImageStack().getProcessor(i);
				ImagePlus[] eigenImp = (new featureJ.FJ_Structure()).getEigenImg(new ImagePlus("", ip),"8.0","3.0");
				ImagePlus eigenLargestImp = eigenImp[0];
				ImagePlus eigenSmallestImp = eigenImp[1];
				
				ShortProcessor spEigenLargest = eigenLargestImp.getProcessor().convertToShortProcessor();
				int level = Auto_Threshold_v.getThreshold("Mean", spEigenLargest);
				spEigenLargest.threshold(level);
				ByteProcessor bpEigenLargest = spEigenLargest.convertToByteProcessor();
				bpEigenLargest.autoThreshold();	

				if(i==-1)
				{
					new ImagePlus("", ip).duplicate().show();
					eigenLargestImp.show();
					eigenSmallestImp.show();
					System.out.println("mean level : " + level);
					new ImagePlus("triangle threshold", bpEigenLargest).show();
				}				
				double ratio = 0.2;
				int position_v = startY_ref;
				if(startY_ref>0)
				{
					int position_v0 = (int)(startY_ref-height_align*ratio >0? startY_ref - height_align*ratio:0);
					for(int v=position_v0;v<startY_ref + height_align; v++)
					{
						int countZero = 0;
						if(v<0 || v>=height)
							continue;						
						for(int u=startX_ref;u<startX_ref+width_crop_image;u++)
						{
							if(u<0 || u>= width)
								continue;							
							if(bpEigenLargest.get(u, v) == 255)
								countZero ++;
						}
						if (countZero > 100)
						{
							position_v = v;
							System.out.println("	slice - " +i+" find position y: " + position_v);
							break;
						}
					}
				}else {
					position_v = Math.abs(position_v);
				}
				bpEigenLargest.setRoi(0, position_v, width, (int)(height_align));										
				ImageProcessor ip_y0 = bpEigenLargest.crop();
				ImageProcessor ip_y1 = bpEigenLargest.crop();																		
				ip_y0.convolve(Gx, 3, 3);
				ip_y1.convolve(Gx2, 3, 3);
				
				float pos_refine_l = refinePosition(ip_y0, (int)(startX_ref-0.5*roi_width), false);//int pos_refine_l = refinePosition(ip_y0, startX_ref);
				float pos_refine_r = refinePosition(ip_y1, (int)(startX_ref+roi_width),true);//int pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l+channel_prefix_pos[1]*0.3-channel_prefix_pos[0]*0.3));
				
				if(i==-1)
				{
					new ImagePlus("", ip).duplicate().show();
					eigenLargestImp.show();
					eigenSmallestImp.show();
					new ImagePlus("triangle threshold", bpEigenLargest).show();
					new ImagePlus("ip_y0", ip_y0).show();
					new ImagePlus("ip_y1", ip_y1).show();
					System.out.println("pos_l : " + pos_refine_l + " pos_r : " + pos_refine_r);
				}
				
				double position_x= (pos_refine_l+pos_refine_r)*0.5;
				ins_param.getPosition_l()[i-1] = (int)(position_x-roi_width/2 > 1 ? position_x-roi_width/2:1);
				if(position_v + height_align > ip.getHeight())
				{
					position_v = ip.getHeight() - height_align;
				}
				ins_param.getPosition_h()[i-1] = position_v;
				
				//if(i==1) // compute only once
					//refinePositionChannelHead(ins_param, eigenSmallestImp, (int)position_x, position_v);
				
				ip.setRoi(ins_param.getPosition_l()[i-1], ins_param.getPosition_h()[i-1], width_align, height_align);	// 1 for more precision depends on experiments
				ImageProcessor ip2 = ip.crop();					
				if(ip2.getWidth()!=imp_crop.getWidth() || ip2.getHeight()!=imp_crop.getHeight())
				{
					System.out.println("pos left : " + ins_param.getPosition_l()[i-1] + " pos top : " + position_v+ " slice : " + i);
					continue;
				}
				imp_crop.addSlice(img.getStack().getSliceLabel(i), ip2);
			}
			ImagePlus imp = new ImagePlus("refine pos image", imp_crop);
			imp.show();
			return imp;
	}
	
	private int[] refinePositionChannelHead(Ins_param ins_param,ImagePlus smallestEigenImp, float position_c, float position_y, int slice) 
	{
		ShortProcessor spEigenSmallest = smallestEigenImp.getProcessor().convertToShortProcessor();
		int level = Auto_Threshold_v.getThreshold("Mean", spEigenSmallest);
		spEigenSmallest.threshold(level);
		ByteProcessor bpEigenSmallest = spEigenSmallest.convertToByteProcessor();
		bpEigenSmallest.autoThreshold();	
		bpEigenSmallest.dilate();
		bpEigenSmallest.erode();
		
		float[] channel_prefix_pos = ins_param.getchannel_prefix_pos();
		ImageProcessor ip = bpEigenSmallest;
		double ratio = 0.2;
		
		//last one is the minimum height estimated
		int[] relative_headPosition = new int[channel_prefix_pos.length/2];
		
		int heightAlignMin = height_align;
		for(int i=0,j=0;i<channel_prefix_pos.length;i=i+2,j++)
		{
			int x = (int)(position_c + channel_prefix_pos[i] - roi_width*0.25);
			int y = (int)(position_y-height_align*ratio);
			double[] profile = getRowMedianProfile(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)), ip);
			ip.setRoi(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)));
			ip.fill();
			profile = diffArray(profile);			
			Ins_find_peaks peakFinder = new Ins_find_peaks(32, 0);				
			Object[] out = peakFinder.findPeaks(profile, true, 6);
			int[] position = (int[])out[0];
			if(position.length == 0)
			{
				profile = getRowMedianProfile(new Rectangle(x, y, (int)(roi_width*0.5), (int)height_align), ip);
				profile = diffArray(profile);
				peakFinder = new Ins_find_peaks(32, 0);				
				out = peakFinder.findPeaks(profile, true, 6);
				position = (int[])out[0];
				if(position.length == 0)
				{
					System.out.println("No relative position was found");
					ins_param.setRelative_headPosition(null);
					return relative_headPosition;
				}
			}
			Arrays.sort(position);
			relative_headPosition[j] = (int)position[0];			
		}
		//new ImagePlus("smallest eigen mean slice "+ slice, ip).show();
		int min = Integer.MAX_VALUE;
		for(int i=0; i<relative_headPosition.length;i++)
		{
			if(relative_headPosition[i]<min)
			{
				min = relative_headPosition[i];
			}
		}
		for(int i=0;i<relative_headPosition.length;i++)
		{
			relative_headPosition[i] = relative_headPosition[i] - min;
			//System.out.println("relative position is for channel ("+i+") is :" + relative_headPosition[i]);
			if(heightAlignMin > height_align - relative_headPosition[i])
				heightAlignMin = height_align - relative_headPosition[i];			
			
		}
		
		int[] out = new int[relative_headPosition.length+1];
		System.arraycopy(relative_headPosition, 0, out, 0, relative_headPosition.length);
		out[out.length-1] = heightAlignMin; 
		return out;
	}
	
	
	private void refinePositionChannelHead_old(Ins_param ins_param,ImagePlus smallestEigenImp, float position_c, float position_y) 
	{
		ShortProcessor spEigenSmallest = smallestEigenImp.getProcessor().convertToShortProcessor();
		int level = Auto_Threshold_v.getThreshold("Mean", spEigenSmallest);
		spEigenSmallest.threshold(level);
		ByteProcessor bpEigenSmallest = spEigenSmallest.convertToByteProcessor();
		bpEigenSmallest.autoThreshold();	
		
		float[] channel_prefix_pos = ins_param.getchannel_prefix_pos();
		ImageProcessor ip = bpEigenSmallest;
		double ratio = 0.2;
		int[] relative_headPosition = new int[channel_prefix_pos.length/2];
		
		int heightAlignMin = height_align;
		
		for(int i=0,j=0;i<channel_prefix_pos.length;i=i+2,j++)
		{
			int x = (int)(position_c + channel_prefix_pos[i] - roi_width*0.25);
			int y = (int)(position_y-height_align*ratio);
			double[] profile = getRowMedianProfile(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)), ip);
			ip.setRoi(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)));
			ip.fill();
			profile = diffArray(profile);			
			Ins_find_peaks peakFinder = new Ins_find_peaks(20, 0);				
			Object[] out = peakFinder.findPeaks(profile, true, 6);
			int[] position = (int[])out[0];
			if(position.length == 0)
			{
				profile = getRowMedianProfile(new Rectangle(x, y, (int)(roi_width*0.5), (int)height_align), ip);
				profile = diffArray(profile);
				peakFinder = new Ins_find_peaks(20, 0);				
				out = peakFinder.findPeaks(profile, true, 6);
				position = (int[])out[0];
				if(position.length == 0)
				{
					System.out.println("No relative position was found");
					ins_param.setRelative_headPosition(null);
					return;
				}
			}
			Arrays.sort(position);
			relative_headPosition[j] = (int)position[0];			
		}
		new ImagePlus("smallest eigen mean", ip).show();
		int min = Integer.MAX_VALUE;
		for(int i=0; i<relative_headPosition.length;i++)
		{
			
			if(relative_headPosition[i]<min)
			{
				min = relative_headPosition[i];
			}
		}
		for(int i=0;i<relative_headPosition.length;i++)
		{
			relative_headPosition[i] = relative_headPosition[i] - min;
			System.out.println("relative position is for channel ("+i+") is :" + relative_headPosition[i]);
			if(heightAlignMin > height_align - relative_headPosition[i])
				heightAlignMin = height_align - relative_headPosition[i];			
			
		}
		
		ins_param.setHeight_align_min(heightAlignMin);
		System.out.println("Height align adjusted : " + heightAlignMin);
		ins_param.setRelative_headPosition(relative_headPosition);	
	}
	
	private double[] diffArray(double[] profile) {
		double[] diff = new double[profile.length-1];
		for(int i = 0;i<diff.length;i++)
			diff[i] = profile[i+1]-profile[i];
				
		return diff;
	}


	public static double[] getRowMedianProfile(Rectangle rect, ImageProcessor ip) {
		double[][] profile = new double[rect.height][rect.width];
		double[] aLine;
		ip.setInterpolate(false);
		for (int x=rect.x,j=0; x<rect.x+rect.width; x++,j++) {
			aLine = ip.getLine(x, rect.y, x, rect.y+rect.height-1);
			for (int i=0; i<rect.height; i++) {
				if (!Double.isNaN(aLine[i])) {
					profile[i][j] = aLine[i];
				}
			}
		}
		double[] profileMedian = new double[rect.height];
		for (int i=0; i<rect.height; i++)
		{
			Arrays.sort(profile[i]);
			double median;
			if (profile[i].length % 2 == 0)
			    median = ((double)profile[i][profile[i].length/2] + (double)profile[i][profile[i].length/2 - 1])/2;
			else
			    median = (double) profile[i][profile[i].length/2];
			profileMedian[i] = median;
		}
		return profileMedian;
	}


	/**
	 * 
	 * @param ip should be horizontal edge image
	 * @param channel_prefix_pos
	 * @param pos0 approximate position, should be computed using the function positionMaxhisto 
	 * @return
	 */
	public float[] refinePosition_toArray(ImageProcessor ip , int startX_reference){
		
		//float channel_length=channel_prefix_pos[2]-channel_prefix_pos[0];//should be adjusted carefully		
		int hist[];
		int w=ip.getWidth();
		int h=ip.getHeight();		
		hist = new int[ip.getWidth()];
		// compute the sum value along the horizontal axe
		for(int u=0;u<w;u++)
		{			
			for(int v=0;v<h; v++)
			{
				for(int i=-2 ; i<=2 ; i++)
				{
					if(u+i <0 || u+i >=w)
						continue;
					hist[u] = hist[u]+ip.get(u+i, v);
				}
			}
			//System.out.println(" hist[" + u+"] = " + hist[u]);
		}		
				
		final int inter = (int)inter_channel;		
		
		float[] dynamicStartX=new float[channel_prefix_pos.length/2];
		
		for(int p=0; p < channel_prefix_pos.length; p=p+2)
		{
			float startX_ref = channel_prefix_pos[p] + startX_reference;
			int max_v = 0;
			for(int startX = (int)(startX_ref); startX < startX_ref+inter; startX++) //label 3
			{			
				if(startX < 0) 
					continue;
				if(startX >= hist.length)
					continue;
				int his = hist[(int)startX];
				if(max_v < his)
				{
					max_v = his;
					dynamicStartX[p/2] = startX;
				}				
			}
		}
		/*if(r_convolv)
		{
			return first_left_center-windows_s;
		}else {
			return first_left_center-(int)(windows_s*0.5);
		}*/
		//IJ.log("the minimum difference corresponds the first left center:  "+difference + " first_center: "+first_left_center);		
		return dynamicStartX;
	}
	
	/**
	 * 
	 * @param ip should be horizontal edge image
	 * @param channel_prefix_pos
	 * @param pos0 approximate position, should be computed using the function positionMaxhisto 
	 * @return
	 */
	public float refinePosition(ImageProcessor ip , int startX_reference, boolean r_convolv){
		//float channel_length=channel_prefix_pos[2]-channel_prefix_pos[0];//should be adjusted carefully		
		int hist[];
		int w=ip.getWidth();
		int h=ip.getHeight();		
		hist = new int[ip.getWidth()];
		int first_left_center=0;
		// compute the sum value along the horizontal axe
		int windows_s = 3;
		for(int u=0;u<w;u++)
		{			
			for(int v=0;v<=(int)(h*0.6); v++)
			{
				for(int i=-windows_s ; i<=windows_s ; i++)
				{
					if(u+i <0 || u+i >=w)
						continue;
					hist[u] = hist[u]+ip.get(u+i, v);
				}
			}
			//System.out.println(" hist[" + u+"] = " + hist[u]);
		}		
				
		final int inter = (int)inter_channel;		
		float difference = Float.MAX_VALUE;
		for(int startX = startX_reference; startX < startX_reference+inter; startX++) //label 3
		{			
			float dis=0f;
			int j = 0;
			for(int i=0; i < channel_prefix_pos.length; i=i+2) // pay attention, here i should be i+2 don't consider all channels, because the last one may be flowed out
			{
				float position=channel_prefix_pos[i]+startX;
				float max = 0f;
				int max_j=0;
				//now find the maximum around the "position" (range: inter) and
				//put the distance in the "dis"
				int j_l = -inter/2;
				int j_r = inter/2;
				
				//if(i==0)
					//j_l = 0;
				
				for(j=j_l; j <= j_r; j++)
				{
					if(position+j>=0 && position+j<hist.length)						
						if(max < hist[(int)position+j])
						{
							max = hist[(int)position+j];
							max_j=j;						
						}		
				}// now we have the the maximum local
				dis=dis + max_j*max_j;
				//System.out.println("StartX :  "+startX +" position : "+ position + " dis: "+dis + " i : "+i + " max_j :" + max_j);
			}
			
			// find the minimum difference of the index found and the prefixed index array 
			if (difference >= dis)
			{
				difference = dis;
				first_left_center = startX;				
				//System.out.println("----------StartX :  "+startX + " dis: "+dis);
			}			
		}
		
		
		//float[] xc = new float[channel_prefix_pos.length/2];
		/*for(int i=2; i < channel_prefix_pos.length; i=i+2) // pay attention, here i should be i+2 don't consider all channels, because the last one may be flowed out
		{
			float position=channel_prefix_pos[i]+first_left_center;
			float max = 0f;
			//now find the maximum around the "position" (range: inter) and
			//put the distance in the "dis"
			int j_l = -inter/4;
			int j_r = inter/4;
			
			for(int j=j_l; j <= j_r; j++)
			{
				if(position+j>=0 && position+j<hist.length)						
					if(max < hist[(int)position+j])
					{
						max = hist[(int)position+j];
						xc[i/2] = position + j;
					}		
			}// now we have the the maximum local
		}*/
		
		//xc[0] = first_left_center;		
		if(r_convolv)
		{
			//for(int i=0;i<xc.length; i++)
			//{
			//	xc[xc.length-1] = first_left_center + windows_s;
			//}
			return first_left_center+(int)(windows_s*0.5);
		}else {
			//for(int i=0;i<xc.length; i++)
			//{
			//	xc[xc.length-1] = first_left_center + (int)(windows_s*0.5);
			//}
			return first_left_center+(int)(windows_s*0.5);
		}
		//return xc;
		//IJ.log("the minimum difference corresponds the first left center:  "+difference + " first_center: "+first_left_center);		
		//return first_left_center;
	}
	
	

}	