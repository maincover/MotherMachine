package watershed2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.measure.Measurements;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ColorStatistics;
import ij.process.ImageProcessor;
import ij.process.PolygonFiller;

import java.awt.Frame;
import java.awt.Rectangle;

public class Merging {

	
	private static int CONVEXITY_INDEX=0;
	private static int CIRCULARITY_INDEX=1;
	//private static int SHAPE_INDEX=1;
	private static int NUMBER_OF_METRICS=2;

	private int[] mLabels=null;
	private int[] mRegionSize=null;
	private double[] mRegionMeanGrayValue=null;
	private SetOfWL mSetOfWL=null;
	private int mWidth;
	private int mHeight;
	private boolean mIsConn4;
	
	public byte[] mRegionSeeds;
	private boolean[] mIsNotOnEdge;
	
	private double thresholdCircularity;
	private double thresholdConvexity;	
	//private double thresholdShape;
	
	private byte[] imgTemp;
	
	private double[] mMean;	
	private double[][] mInverseCovarianceMatrix;
	private double mDeterminant;
	private boolean mMeanCovMatrixCalculated = false;
	private double[] mScore;
	
	ImagePlus gradientImg = null;
	Roi[] mRoi = null;


	public Merging(WatershedData wLData)
	{
		
		mLabels=wLData.getLabels();
		mRegionSize=wLData.getRegionSize();
		mSetOfWL=wLData.getSetOfWL();
		mWidth=wLData.getWidth();
		mHeight=wLData.getHeight();
		mIsConn4=wLData.isConn4();
		gradientImg = wLData.getWatershedImageUsed();
		imgTemp = wLData.getImPUser();
		
		// find one point by region
		MaximumFinder fm = new MaximumFinder();
		ImagePlus imgSegmented = wLData.getSegmentedRegionImage();

		ByteProcessor bp = (ByteProcessor)( imgSegmented.getProcessor());
		bp.invert();
		bp = fm.findMaxima(bp, 0, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, false);
		mRegionSeeds = (byte[])bp.getPixels();
		mIsNotOnEdge=new boolean[mRegionSize.length];
		for(int i=0; i<mWidth; i++){
			mIsNotOnEdge[mLabels[i]/2-1]=false;
			mIsNotOnEdge[mLabels[i+(mHeight-1)*mWidth]/2-1]=false;
		}
		for(int j=1; j<mHeight-1; j++){
			mIsNotOnEdge[mLabels[j*mWidth]/2-1]=false;
			mIsNotOnEdge[mLabels[(mWidth-1)+j*mWidth]/2-1]=false;
		}
		
	}

	/**
	 * attribute a pixel to a region. (The pixel will be use to determine the ROI of the region)
	 */
	public void process(){

		mRoi = fillHolesAndBuildRoi(mRegionSeeds);

		SetOfMetrics[] metrics = calculateRegionMetrics(mRoi);
		
//		System.out.println("=======");
//		for(int i=0; i<metrics.length; i++){
//			if(metrics[i]!=null)
//				System.out.println(metrics[i].getMetric(0)+"    "+metrics[i].getMetric(1));
//		}
//		System.out.println("=======");
		
		findThresholds(metrics);
		boolean[] goodCandidates = selectGoodCandidates(metrics);
		displayThe4Region(metrics,mRoi);
		displayMetrics(metrics, goodCandidates);
		
		calculMeanOfGoodsAndCovarianceMatrix(metrics, goodCandidates);
		
		mScore = new double[metrics.length];
		//System.out.println("Score :");
		for(int i=0; i<metrics.length; i++){
			if(metrics[i]!=null)
				mScore[i]=calculScore(metrics[i]);
				//System.out.println("  "+mScore[i]);
		}
		watershedBreaker();
	} 

	
	
	/**
	 * add Roi Manager
	 * @param pixelsMax
	 * @return
	 */
	public Roi[] fillHolesAndBuildRoi(byte[] pixelsMax)
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)						
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");
		if (frame==null || !(frame instanceof RoiManager))
			{return null;}
		RoiManager roiManager = (RoiManager)frame;				
		roiManager.setVisible(true);
		
		ImagePlus impRoi = IJ.createImage("", "8-bit", mWidth, mHeight, 2);
		
		
		
		Roi[] roiOfRegions = new Roi[mRegionSize.length];

		// build an image with label tab to build the ROIs
		ColorProcessor cp = new ColorProcessor(mWidth, mHeight, mLabels);
		int label;
		PolygonRoi roi;
		//ImageBuilder.display(new ImagePlus("regions",cp));

		for(int i=0; i<pixelsMax.length ;i++){
			if(pixelsMax[i]!=0){
				// Single point of region
				label = mLabels[i];

				// ROI Building and saving
				Wand wand = new Wand(cp);
				wand.autoOutline(i%mWidth, i/mWidth,mLabels[i],mLabels[i]);

				roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
				roiOfRegions[label/2-1] = (Roi)roi.clone();
				cp.setRoi(roi);


				// verify that the ROI correspond to the Region comparing the area
				ColorStatistics cs = new ColorStatistics(cp,Measurements.AREA,null);

				// bgWLtoRebuild become true if the WL with the background must be rebuilt
				boolean bgWLtoRebuild = false;
				boolean isDefinitiveRoi = (mRegionSize[label/2-1]==(int)cs.area);



				// building of the good ROI and fill the holes in the region
				while(isDefinitiveRoi==false){
					System.out.println(">label avec trou: "+label);
					int px,py,w,h,n;
					Rectangle r = roi.getBounds();
					px = r.x;
					py = r.y;
					w = r.width;
					h = r.height;
					// removing of the detectable holes of the region in the ROI

					PolygonFiller pf = new PolygonFiller();
					pf.setPolygon(wand.xpoints, wand.ypoints, wand.npoints);
					byte[] cachedMask = (byte[])pf.getMask(w, h).getPixels();

					int[] RegionToDelete = null;

					for (int y=0; y<h; y++) {
						for (int x=0; x<w; x++) {
							n = y * w + x;
							if(cachedMask[n]==-1){ //white 255
								int ind = (x+px) + (y+py)*mWidth;

								if(mLabels[ind]!=label){

									if(RegionToDelete!=null){
										boolean isSet=false;
										for(int k=0; k<RegionToDelete.length; k++){
											if(RegionToDelete[k]==mLabels[ind])
												isSet=true;
										}
										if(isSet==false){
											System.out.println("+label "+mLabels[ind]);
											// the of the label mLabels[ind] must be removed
											int[]tmp = new int[RegionToDelete.length+1];
											System.arraycopy(RegionToDelete, 0, tmp, 0, RegionToDelete.length);
											tmp[RegionToDelete.length]=mLabels[ind];
											RegionToDelete=tmp;
										}
									}else{
										System.out.println("+label "+mLabels[ind]);
										RegionToDelete = new int[1];
										RegionToDelete[0]=mLabels[ind];
									}
									mLabels[ind]=label;
								}
								else{
									isDefinitiveRoi=true;
								}
							}
						}
					}



					// deleting of the WL of regions in holes (RegionToDelete can't be null)
					for(int k=0; k<RegionToDelete.length; k++){

						if(RegionToDelete[k]==2){
							// be careful with the background
							//remove WL
							mSetOfWL.removeWL(label,2);
							// rebuild WL
							bgWLtoRebuild = true;
						}else{
							mSetOfWL.removeWLofRegion(RegionToDelete[k]);
							mRegionSize[RegionToDelete[k]/2-1]=0;
						}
					}


					if(isDefinitiveRoi==false){
						// the ROI isn't definitive => rebuild the ROI

						//update size of region tab
						mRegionSize[label/2-1]+=(int)cs.area;

						// ROI Building and saving
						wand = new Wand(cp);
						wand.autoOutline(i%mWidth, i/mWidth,mLabels[i],mLabels[i]);
						roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
						roiOfRegions[label/2-1] = (Roi)roi.clone();
						cp.setRoi(roi);
						cs = new ColorStatistics(cp,Measurements.AREA,null);

						isDefinitiveRoi = (mRegionSize[label/2-1]==(int)cs.area);

					}

					if(isDefinitiveRoi){
						// the ROI is definitive

						//update size of region tab
						System.out.println("Roi label: " + label);
						mRegionSize[label/2-1]=(int)cs.area;
						roiOfRegions[label/2-1] = (Roi)roi.clone();

					}
				}

				// rebuild of the WL with the background if is needed
				if(bgWLtoRebuild){

					int px,py,w,h,n;
					Rectangle r = roi.getBounds();
					px = r.x;
					py = r.y;
					w = r.width;
					h = r.height;

					// removing of the detectable holes of the region in the ROI

					PolygonFiller pf = new PolygonFiller();
					pf.setPolygon(wand.xpoints, wand.ypoints, wand.npoints);
					byte[] cachedMask = (byte[])pf.getMask(w, h).getPixels();


					//neighborhood tabs
					int[] dx,dy;
					int nbNeighbor;
					if (mIsConn4){
						nbNeighbor = 4;
						dx = new int[4];dy = new int[4];
						dx[0] = dx[3] = 0; dx[1] = -1; dx[2] = 1;
						dy[1] = dy[2] = 0; dy[0] = -1; dy[3] = 1;
					}
					else {
						nbNeighbor = 8;
						dx = new int[8];dy = new int[8];
						dx[0] = dx[3] = dx[5] = -1; dx[2] = dx[4] = dx[7] = 1; dx[1] = dx[6] = 0;
						dy[0] = dy[1] = dy[2] = -1; dy[5] = dy[6] = dy[7] = 1; dy[3] = dy[4] = 0;
					}

					//for each pixel of the ROI
					for (int y=0; y<h; y++) {
						for (int x=0; x<w; x++) {
							n = y * w + x;
							if(cachedMask[n]==-1){ //white 255
								int nx,ny;
								for(int k=0; k<nbNeighbor; k++){
									nx = x+px+dx[k];
									ny = y+py+dy[k];
									// if the neighbor is a background pixel : update WL
									if(nx>=0 && nx<mWidth && ny>=0 && ny<mHeight)
										if(mLabels[nx + ny*mWidth]==2){
											mSetOfWL.addWL(label,2,(x+px),(y+py));
										}
								}
							}
						}
					}
				}
				//roi.setPosition(1);
				roi.setPosition(1, 1, 1);
				roiManager.add(impRoi, roi, -1);
			}              
		}
		return roiOfRegions;
	}
	
	
	
	
	

	private SetOfMetrics[] calculateRegionMetrics(Roi[] roi)
	{
		// build an image with label tab to build the ROIs
		ColorProcessor cp = new ColorProcessor(mWidth, mHeight, mLabels); 
		
		//ImageProcessor cpTemp = cp.duplicate(); 
		//ImagePlus imgTemp = new ImagePlus("labels image",cpTemp);
		ImagePlus img = new ImagePlus("labels image",cp);
		
		SetOfMetrics[] metrics = new SetOfMetrics[mRegionSize.length];
		mRegionMeanGrayValue = new double[mRegionSize.length];
		//imgTemp.show();
		for(int i=0;i<mRegionSize.length;i++)
		{
			if(roi[i]!=null)
			{
				/*imgTemp.killRoi();
				imgTemp.setRoi(roi[i]);
				roi[i].update(true, false);*/
				mRegionMeanGrayValue[i] = MetricsCalculator.meanGrayValue(roi[i],gradientImg);
				metrics[i] = new SetOfMetrics(NUMBER_OF_METRICS);
				metrics[i].setMetric(MetricsCalculator.convexity(roi[i],img), CONVEXITY_INDEX);
				metrics[i].setMetric(MetricsCalculator.circularity(roi[i],img), CIRCULARITY_INDEX);
			}
		}
		return metrics;
	}
	
	private void findThresholds(SetOfMetrics[] setOfMetrics)
	{
		double convexitySum=0;
		double circularitySum=0;
		int cpt=0;
		for(int i=0;i<mRegionSize.length;i++)
		{
			if(setOfMetrics[i]!=null)
			{
				convexitySum+=setOfMetrics[i].getMetric(CONVEXITY_INDEX);
				circularitySum+=setOfMetrics[i].getMetric(CIRCULARITY_INDEX);
				cpt++;
			}
		}
		double meanConvexity=convexitySum/cpt;
		System.out.println("Mean convexity: "+meanConvexity);
		double meanCircularity=circularitySum/cpt;
		System.out.println("Mean Circularity: "+meanCircularity);
		double stdConvexity=0;
		double stdCircularity=0;
		for(int i=0;i<mRegionSize.length;i++)
		{
			if(setOfMetrics[i]!=null)
			{
				double tmp1 = (setOfMetrics[i].getMetric(CONVEXITY_INDEX)-meanConvexity);
				double tmp2 = (setOfMetrics[i].getMetric(CIRCULARITY_INDEX)-meanCircularity);
				stdConvexity = tmp1*tmp1;
				stdCircularity = tmp2*tmp2;
			}
		}
		stdConvexity = Math.sqrt(stdConvexity/cpt);
		System.out.println("std convexity: "+stdConvexity);
		stdCircularity = Math.sqrt(stdCircularity/cpt);
		System.out.println("std circularity: "+stdCircularity);
		thresholdConvexity=meanConvexity+stdConvexity;
		thresholdCircularity=meanCircularity+stdCircularity;
	}

	private boolean[] selectGoodCandidates(SetOfMetrics[] setOfMetrics)
	{
		boolean[] goodCandidates = new boolean[mRegionSize.length];
		for(int i=0;i<mRegionSize.length;i++)
		{	
			if(setOfMetrics[i]!=null&&	
			   setOfMetrics[i].getMetric(CONVEXITY_INDEX) >= thresholdConvexity &&
			   setOfMetrics[i].getMetric(CIRCULARITY_INDEX) >= thresholdCircularity //&&
			   //mIsNotOnEdge[i]
			   )
			{
				System.out.println("+convexity:"+setOfMetrics[i].getMetric(CONVEXITY_INDEX));
				System.out.println(" circularity:"+setOfMetrics[i].getMetric(CIRCULARITY_INDEX));
				
				goodCandidates[i]=true;
				
			}
			else{
				if(setOfMetrics[i]!=null){
				System.out.println(">convexity:"+setOfMetrics[i].getMetric(CONVEXITY_INDEX));
				System.out.println(" circularity:"+setOfMetrics[i].getMetric(CIRCULARITY_INDEX));
				}
				goodCandidates[i]=false;
			}
		}
		return goodCandidates;
	}
	
	private void displayMetrics(SetOfMetrics[] sm, boolean[] goodCandidates){
		
		ColorProcessor cp = new ColorProcessor(500, 500);
		ImagePlus img = new ImagePlus("Metrics", cp);
		
		double max0=0;
		double max1=0;
		
		for(int i=0;i<mRegionSize.length;i++)
		{
			if(sm[i]!=null)
			{
				if(sm[i].getMetric(CONVEXITY_INDEX)>max0)
					max0 = sm[i].getMetric(CONVEXITY_INDEX);
				if(sm[i].getMetric(CIRCULARITY_INDEX)>max1)
					max1 = sm[i].getMetric(CIRCULARITY_INDEX);
			}
		}
		
		System.out.println(" thresholdConvexity:"+thresholdConvexity+" \n thresholdCircularity:"+thresholdCircularity);
		System.out.println(" max0:"+max0+" max1:"+max1);
		for(int i=0; i<img.getHeight(); i++){
			cp.set(i, (int)((img.getWidth()-1)*thresholdCircularity/max1),0x00ff0000);
		}

		for(int i=0; i<img.getWidth(); i++){
			cp.set((int)((img.getHeight()-1)*thresholdConvexity/max0), i, 0x0000ff00);
		}
		

		for(int i=0;i<mRegionSize.length;i++)
		{
			if(sm[i]!=null)
			{
				cp.set((int)( (img.getWidth()-1) * sm[i].getMetric(CONVEXITY_INDEX)/max0 ), 
					   (int)((img.getHeight()-1) * sm[i].getMetric(CIRCULARITY_INDEX)/max1 ),
						0x00ffffff);
			}
		}
		img.show();
		
	}
	private void displayThe4Region(SetOfMetrics[] setOfMetrics, Roi[] roi)
	{
		ImagePlus img = new ImagePlus("imgConvCircularity",new ByteProcessor(mWidth, mHeight, imgTemp,null));
		ImageProcessor ip = img.getProcessor().duplicate();
		ImagePlus img1 = new ImagePlus("imgConvCircularity",ip.duplicate());
		ImagePlus img2 = new ImagePlus("imgNoConvCircularity",ip.duplicate());
		ImagePlus img3 = new ImagePlus("imgConvNoCircularity",ip.duplicate());
		ImagePlus img4 = new ImagePlus("imgNoConvNoCircularity",ip.duplicate());
		img1.getProcessor().invert();
		img2.getProcessor().invert();
		img3.getProcessor().invert();
		img4.getProcessor().invert();
		Line.setWidth(2);
		for(int i=0;i<mRegionSize.length;i++)
		{	
			if(setOfMetrics[i]!=null&&	
			   setOfMetrics[i].getMetric(CONVEXITY_INDEX) >= thresholdConvexity &&
			   setOfMetrics[i].getMetric(CIRCULARITY_INDEX) >= thresholdCircularity //&&
			   //mIsNotOnEdge[i]
			   )
					{
						//img1.setRoi(roi[i]);
						roi[i].drawPixels(img1.getProcessor());
					}
			else if(setOfMetrics[i]!=null&&	
					   setOfMetrics[i].getMetric(CONVEXITY_INDEX) <= thresholdConvexity &&
					   setOfMetrics[i].getMetric(CIRCULARITY_INDEX) >= thresholdCircularity //&&
					   //mIsNotOnEdge[i]
					   )
					{
						//img2.setRoi(roi[i]);
						roi[i].drawPixels(img2.getProcessor());
					}
			else if(setOfMetrics[i]!=null&&	
					   setOfMetrics[i].getMetric(CONVEXITY_INDEX) >= thresholdConvexity &&
					   setOfMetrics[i].getMetric(CIRCULARITY_INDEX) <= thresholdCircularity //&&
					   //mIsNotOnEdge[i]
					   )
					{
						//img3.setRoi(roi[i]);
						roi[i].drawPixels(img3.getProcessor());
					}
			else if(setOfMetrics[i]!=null&&	
					   setOfMetrics[i].getMetric(CONVEXITY_INDEX) <= thresholdConvexity &&
					   setOfMetrics[i].getMetric(CIRCULARITY_INDEX) <= thresholdCircularity //&&
					   //mIsNotOnEdge[i]
					   )
					{
						//img4.setRoi(roi[i]);
						roi[i].drawPixels(img4.getProcessor());
					}
		}
		img1.show();
		img2.show();
		img3.show();
		img4.show();
	}
	public boolean calculMeanOfGoodsAndCovarianceMatrix(SetOfMetrics[] sm, boolean[] isGoodCandidates)
	{
		
		if(NUMBER_OF_METRICS>0){
			
			// number of good candidates
			int cmp=0;
			for(int i=0; i<isGoodCandidates.length; i++){
				if(isGoodCandidates[i])
					cmp++;
			}
			
			if(cmp>0){
				
				// metrics of good candidates to build the covariance matrix
				double[][] metricsVector = new double[NUMBER_OF_METRICS][cmp];
				// mean value for each metric of good candidates
				mMean = new double[NUMBER_OF_METRICS];
				
				cmp=0;
				for(int i=0; i<sm.length; i++){
					if(isGoodCandidates[i]){
						for(int n=0; n<NUMBER_OF_METRICS; n++){
							metricsVector[n][cmp]=sm[i].getMetric(n);
							mMean[n]+=sm[i].getMetric(n);
						}
						cmp++;
					}
				}
				for(int n=0; n<NUMBER_OF_METRICS; n++){
					mMean[n]=mMean[n]/cmp;
				}
			
				
				// covariance Matrix
				double[][] covarianceMatrix = new double[NUMBER_OF_METRICS][NUMBER_OF_METRICS];
				
				for(int i=0; i<NUMBER_OF_METRICS; i++){
					for(int j=0; j<NUMBER_OF_METRICS; j++){
						double covariance=0;
						for(int k=0; k<cmp; k++){
							covariance+= (metricsVector[i][k] - mMean[i]) * (metricsVector[j][k]-mMean[j]);
						}
						covariance = covariance/cmp;
						covarianceMatrix[i][j]=covariance;
					}
				}
				
				//Determinant of covariance matrix
				if(NUMBER_OF_METRICS==2)
					mDeterminant = covarianceMatrix[0][0]*covarianceMatrix[1][1] - covarianceMatrix[0][1]*covarianceMatrix[1][0];
				else{
					System.out.println("Error : Determinant calculation for NxN matrix with N!=2 is'nt done");
				}
				
				// inverse covariance Matrix
				if(mDeterminant!=0){
					mInverseCovarianceMatrix = gaussR(covarianceMatrix, NUMBER_OF_METRICS);
				}else{
					return false;
				}
				mMeanCovMatrixCalculated = true;
				
				return true;
				
			}
			else{
				return false;
			}
			
		}
		else{
			return false;
		}
		
		
	}
	
	/**
	 * calculation of the score regarding the metrics of a region
	 * @param m set of metrics of a region
	 * @return the score
	 */
	public double calculScore(SetOfMetrics m)
	{
		if(mMeanCovMatrixCalculated){
			
			// score = a * exp((-1/2)*sqrt(b));
			// with a = 1 / ( (2*PI)^(NUMBER_OF_METRICS/2) * sqrt(mDeterminant) ) 
			// with b = trans(m-mean) * mInverseCovarianceMatrix * (m-mean)
			
			double a = Math.pow(2*Math.PI,-((double)(NUMBER_OF_METRICS))/2) * Math.pow(mDeterminant,-1/2);
			
			double b=0;
			double[] tmp1 = new double[NUMBER_OF_METRICS];
			for(int i=0; i<NUMBER_OF_METRICS; i++){
				tmp1[i] = m.getMetric(i)-mMean[i];
			}
			double[] tmp2 = multipyVectorAndSquareMatrix(tmp1, mInverseCovarianceMatrix, NUMBER_OF_METRICS);

			for(int i=0; i<NUMBER_OF_METRICS; i++){
				b += tmp1[i]*tmp2[i];
			}
			
			double score = a * Math.exp( (-0.5)*Math.sqrt(b) );
			
			return score;
		}
		else{
			System.out.println("Error (Merging.Enging.calculScore) : Means of Metrics and covariance matrix must be calculated before");
			return -1;
		}
	}
	
	private void multipySquareMatrix(double[][] m1, double[][] m2, int dim){

		System.out.println("Matrix :");
		double[][] result = new double[dim][dim];
		for(int i =0; i<dim; i++){
			for(int j =0; j<dim; j++){
				for(int n=0; n<dim; n++){
					result[i][j]+=m1[n][i]*m2[j][n];
				}
				System.out.print(result[i][j]+" ");
			}
			System.out.println("\n");
		}
	}
	
	/**
	 * multiplication of a square matrix NxN by a vector of dimension N
	 * @param v vector
	 * @param m matrix
	 * @param dim dimension of the vector and matrix
	 * @return the result vector
	 */
	private double[] multipyVectorAndSquareMatrix(double[] v, double[][] m, int dim){

		//System.out.println("Vector :");
		double[] result = new double[dim];
		for(int i =0; i<dim; i++){
			for(int j =0; j<dim; j++){
				result[i]+=v[j]*m[i][j];
			}
			//System.out.print(result[i]+" ");
		}
		//System.out.println("\n");
		
		return result;
	}
	
	
	/**
	 * Inversion of a square matrix with the gauss-jordan reduction method
	 * @param mat1 matrix to inverse
	 * @param dim dimxdim is the dimensions of the matrix
	 * @return the inverse matrix
	 */
	private double[][] gaussR(double[][] mat1, int dim)
	{
		
		if(mat1!=null && dim>0){		
			double[][] temp= new double[dim][dim];
			for(int i=0; i<dim; i++){
				for(int j=0; j<dim; j++){
					temp[i][j]=mat1[i][j];
				}
			}
			double[][] inverse = new double[dim][dim];
			                                    
			double a,b;
			a=0;
			b=0;
			int c=0;
			
			for(int i=0; i<dim; i++)
				inverse[i][i]=1;
			
			
			for(int k=0;k<dim;k++)
			{
				a=temp[k][k];
				//a musn't be equal to 0
				c=0;
				while(Math.abs(a)<0.000000001)
				{
					c++;
					for(int q=0;q<dim;q++)
					{
						temp[k][q]= temp[k][q] + temp[k+c][q];
						inverse[k][q] = inverse[k][q] + inverse[k+c][q];
					}
					a=temp[k][k];
				}
				
				//normalisation
				for(int l=0;l<dim;l++)
				{
					temp[k][l] = temp[k][l]/a;
					inverse[k][l] = inverse[k][l]/a;
				}
				
				// gauss-jordan reduction
				for(int i=0;i<dim;i++)
				{
					b=temp[i][k];
					if(i!=k)
					{
						for(int j=0;j<dim;j++)
						{
							temp[i][j] = temp[i][j]-b*temp[k][j];
							inverse[i][j] = inverse[i][j]-b*inverse[k][j];
						}
					}
				}
			}
			
			return inverse;
			
		}
		return null;
	}
	
	private void watershedBreaker()
	{
		ImagePlus img = new ImagePlus("roi", new ColorProcessor(mWidth, mHeight, mLabels.clone()));
		img.getProcessor().invert();
		//img.show();
		System.out.println("----debut watershedBreaker------");
		double thresholdB=10;
		int n=1;
		int indBestNeighbor;
		double gradientRatio;
		double max;
		double scoresRatio;
		int[] neighbor;
		int indNeighbor;
		SetOfMetrics metricsOfMerging=null;
		double scoreOfMerging;
		
		double sumRatio=0;
		int cpt = 0;
		
		while(n>0)
		{
			n=0;
			for(int i=1;i<mRegionSize.length;i++)
			{
				//System.out.println(">> label Region : "+(i+1)*2);
				gradientRatio=0;
				int labelRegion = (i+1)*2;
				neighbor = mSetOfWL.getNeighborhood(labelRegion);
				if(neighbor!=null)
				{
					indBestNeighbor=0;
					max = 0;
					scoresRatio=0;
					for(int j=0;j<neighbor.length;j++)
					{
						if(neighbor[j]!=2) // if not the background label
						{
							//System.out.println("  + label Neighbor : "+neighbor[j]);
							indNeighbor = neighbor[j]/2-1;	
							SetOfMetrics tmpMetrics =  calculMetricsOfTwoRoiMerging(i,indNeighbor);
							if(tmpMetrics!=null)
							{
								metricsOfMerging = tmpMetrics;
								scoreOfMerging = calculScore(metricsOfMerging);
								if (scoreOfMerging>max)
								{
									indBestNeighbor = indNeighbor;
									max=scoreOfMerging;
								}
							}
						}
					}
					if(max!=0)
					{
						scoresRatio=calculScoreRatio(i,indBestNeighbor);
						gradientRatio = calculGradientRatio(i,indBestNeighbor);
						sumRatio=sumRatio + gradientRatio*scoresRatio;
						cpt++;
						if((gradientRatio*scoresRatio)>=thresholdB)
						{
							
							//update
							Roi tmp = getTwoObjectsRoi(i,indBestNeighbor);
							
							if(tmp!=null){
								mSetOfWL.mergingRegion(labelRegion, (indBestNeighbor+1)*2, mLabels);
								n++;
								
								mRoi[i]=tmp;
								mRoi[indBestNeighbor]=null;
								mRoi[i].drawPixels(img.getProcessor());//
								img.updateAndDraw();//
								mRegionSize[i] = mRegionSize[i]+mRegionSize[indBestNeighbor];
								mRegionSize[indBestNeighbor]=0;
							}
						}
						
					}
				}
			}		
		}
		System.out.println("moyenne ratio : "+sumRatio/cpt);
		System.out.println("----fin watershedBreaker-----");
	}
	
	private double calculScoreRatio(int ind1, int ind2)
	{
		double Score1 = mScore[ind1];
		double Score2 = mScore[ind2];
		double globalScore = calculScore(calculMetricsOfTwoRoiMerging(ind1,ind2));
		
		double scoreRatio = (globalScore*2)/(Score1+Score2);
		return scoreRatio;	
	}
	
	private double calculGradientRatio(int ind1, int ind2)
	{
		double gradientRatio;
		double wLStrength = mSetOfWL.getStrengthOfWL((ind1+1)*2, (ind2+1)*2);
		gradientRatio = (mRegionMeanGrayValue[ind1]+mRegionMeanGrayValue[ind2]) / (2*wLStrength);

		return gradientRatio;
	}
	
	private SetOfMetrics calculMetricsOfTwoRoiMerging(int ind1, int ind2)
	{
        
		ShapeRoi s1 = new ShapeRoi((Roi)mRoi[ind1].clone());
        ShapeRoi s2 = new ShapeRoi((Roi)mRoi[ind2].clone());
        s1.or(s2);
        Roi[] globalRoi = s1.getRois();
		if(globalRoi.length>1)
		{
			return null;
		}
        SetOfMetrics globalRoiMetrics= new SetOfMetrics(NUMBER_OF_METRICS);
        
        ImagePlus img = new ImagePlus("labels image",new ColorProcessor(mWidth, mHeight, mLabels));
		globalRoiMetrics.setMetric(MetricsCalculator.convexity(globalRoi[0],img), CONVEXITY_INDEX);
		globalRoiMetrics.setMetric(MetricsCalculator.circularity(globalRoi[0],img), CIRCULARITY_INDEX);

		return globalRoiMetrics;
	}
	
	private Roi getTwoObjectsRoi(int ind1, int ind2)
	{
		Roi[] globalRoi=null;
        ShapeRoi s1 =  new ShapeRoi((Roi)mRoi[ind1].clone());
        ShapeRoi s2 =  new ShapeRoi((Roi)mRoi[ind2].clone());
        s1.or(s2);
        globalRoi = s1.getRois();
		if(globalRoi.length>1)
		{
			return null;
		}
		return globalRoi[0];
	}
}
