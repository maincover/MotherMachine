
import java.awt.Rectangle;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import Ij_Plugin.Ins_find_peaks;
import Stabilizer.Ins_param;
import Stabilizer.Ins_stabilizer;
import Threshold.Auto_Threshold_v;


public class Ins_seg_preprocessing {
	private ImagePlus mImp = null;
	private boolean debug_mode=false;
	private int startX_ref;
	private int startY_ref;
	private boolean imageCropped = false;
	
	private float inter_channel;//49.45f
	private float roi_width;
	private float channel_prefix_pos[];
	private float[] dynamicInterChannelLength;
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
					-2,0,2,
					-1,0,1
			};
			
			float[] Gx2=new float[]{
					1,0,-1,
					2,0,-2,
					1,0,-1
			};
			
			int width_align = ins_param.getWidth_align();
			int height_align = ins_param.getHeight_align();
			int width=im.getWidth();
			int height = im.getHeight();
			ImageStack imp_crop=new ImageStack(width_align, height_align);

			if(width < width_align)
			{
				IJ.error("cropped width is bigger than original width");
				return null;
			}
			
			ins_param.setPosition_l(new int[img.getStackSize()]);
			ins_param.setPosition_h(new int[img.getStackSize()]);
			ins_param.setPostion_x(new double[img.getStackSize()][]);
			
			
			int pSize = 40;
			
			float[][] pos_refine_l = new float[pSize][];
			float[][] pos_refine_r = new float[pSize][];
			for (int i=1; i<=pSize; i++) {
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
						for(int u=startX_ref*3;u<width*0.8;u++)
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
				pos_refine_l[i-1] = refinePosition(ip_y0, startX_ref);
				pos_refine_r[i-1] = refinePosition(ip_y1,  (int)(startX_ref+0.5*roi_width));//int pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l+channel_prefix_pos[1]*0.3-channel_prefix_pos[0]*0.3));
				
			}
			
			float[][] xDiff = new float[pos_refine_l[0].length-1][width];
			
			for(int i=0;i<pSize; i++)
			{
				//xDiff[(int)pos_refine_l[i][0]]++;
				//xDiff[(int)pos_refine_r[i][0]]++;
				for(int j=0; j<pos_refine_l[i].length-1;j++)
				{
					float diff = pos_refine_l[i][j+1]-pos_refine_l[i][j];
					xDiff[j][(int)(diff)]++;
					diff = pos_refine_r[i][j+1]-pos_refine_r[i][j];
					xDiff[j][(int)(diff)]++;					
				}
			}
			
			float[] xDiff_max = new float[xDiff.length];
			for(int i=0; i<xDiff.length;i++)
			{
				System.out.println("diff i : "+ i );
				int max_diff = -1;
				for(int j=0; j<xDiff[i].length;j++)
				{
					if(xDiff[i][j] > 0)
					{
						if(xDiff[i][j]>max_diff)
						{
							max_diff = (int)xDiff[i][j];
						}
					}	
				}
				xDiff_max[i] = max_diff;
				System.out.println("inter distance : " + max_diff);
			}
			ins_param.compute_channel_prefix_pos(xDiff_max);
			
				
			
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
						for(int u=startX_ref*3;u<width*0.8;u++)
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
				
				
				
				float[] pos_refine_l = refinePosition(ip_y0, startX_ref);
				float[] pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l[0]+0.3*roi_width));//int pos_refine_r = refinePosition(ip_y1,  (int)(pos_refine_l+channel_prefix_pos[1]*0.3-channel_prefix_pos[0]*0.3));
				
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
				
				double[] position_x = new double[pos_refine_l.length];
				for(int m=0;m<pos_refine_l.length;m++)
				{
					position_x[m]= (pos_refine_l[m]+pos_refine_r[m])*0.5;
					System.out.println("x[m]: "+ " m: " + m + " x : "+ position_x[m]);
				}
				//ins_param.getPosition_l()[i-1] = pos_refine_l[0];
				
				ins_param.getPosition_l()[i-1] = (int)(position_x[0]-roi_width/2 > 0 ? position_x[0]-roi_width/2:0);
				ins_param.getPostion_x()[i-1] = position_x;
				if(position_v + height_align > ip.getHeight())
				{
					position_v = ip.getHeight() - height_align;
				}
				ins_param.getPosition_h()[i-1] = position_v;
				
				if(i==1) // compute only once
					refinePositionChannelHead(ins_param, eigenSmallestImp, position_x, position_v);
				
				ip.setRoi(ins_param.getPosition_l()[i-1], ins_param.getPosition_h()[i-1], width_align, height_align);	// 1 for more precision depends on experiments
				ImageProcessor ip2 = ip.crop();					
				if(ip2.getWidth()!=imp_crop.getWidth() || ip2.getHeight()!=imp_crop.getHeight())
				{
					System.out.println("pos left : " + ins_param.getPosition_l()[i-1] + " pos top : " + position_v+ " slice : " + i);
					continue;
				}
				imp_crop.addSlice(img.getStack().getSliceLabel(i), ip2);
			}
			ImagePlus imp = new ImagePlus("PRE-CROP", imp_crop);
			imp.show();
			return imp;
	}
	
	
	private void refinePositionChannelHead(Ins_param ins_param,ImagePlus smallestEigenImp, double[] position_c, float position_y) 
	{
		ShortProcessor spEigenSmallest = smallestEigenImp.getProcessor().convertToShortProcessor();
		int level = Auto_Threshold_v.getThreshold("Mean", spEigenSmallest);
		spEigenSmallest.threshold(level);
		ByteProcessor bpEigenSmallest = spEigenSmallest.convertToByteProcessor();
		bpEigenSmallest.autoThreshold();	
		
		//float[] channel_prefix_pos = ins_param.getchannel_prefix_pos();
		ImageProcessor ip = bpEigenSmallest;
		double ratio = 0.2;
		int[] relative_headPosition = new int[position_c.length];
		
		int heightAlignMin = height_align;
		
		for(int i=0,j=0;i<position_c.length;i++,j++)
		{
			int x = (int)(position_c[i] - roi_width*0.25);
			int y = (int)(position_y-height_align*ratio);
			double[] profile = getRowMedianProfile(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)), ip);
			System.out.println("position i: "+i+" x: "+x);;
			Roi r = new Roi(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio));
			ip.setRoi(new Rectangle(x, y, (int)(roi_width*0.5), (int)(2*height_align*ratio)));
			Ins_seg_panel.addRoiToManager(r);
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
	public float[] refinePosition(ImageProcessor ip , int startX_reference){
		
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
				
		final int inter = (int)inter_channel/2;		
		
		float[] dynamicStartX=new float[channel_prefix_pos.length/2];
		
		for(int p=0; p < channel_prefix_pos.length; p=p+2)
		{
			float startX_ref = channel_prefix_pos[p] + startX_reference;
			int max_v = 0;
			for(int startX = (int)(startX_ref - roi_width); startX < startX_ref+inter; startX++) //label 3
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
		//IJ.log("the minimum difference corresponds the first left center:  "+difference + " first_center: "+first_left_center);		
		return dynamicStartX;
	}
	
	

}	