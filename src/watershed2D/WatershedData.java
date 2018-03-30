package watershed2D;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * global data of the watershed process
 */
public class WatershedData {
	/*
	// Image Source given by the user
	private ImagePlus 		mImPSource = null;*/
	private byte[] 			mSourceImg = null;
	// ImagePlus used for the watershed processing
	private ImagePlus 		mImPW = null;
	// image width
	private int				mWidth;
	// image height
	private int				mHeight;
	// image pixel tab
	private int[]			mPixValue = null;

	private byte[] 			mGradImg = null;

	// number of seeds
	private int 			mNbBassins;
	// tabs of coordinates of seeds
	private int[] 		mSeedsCoord = null;
	// region color
	private int[][] 		mRegionColor = null;

	// tab of label associated to each pixel
	private int[]			mLabels=null;

	// 4-connexity if true else 8-connexity
	private boolean 		mConn4 = true;
	// data of the watershed lines
	private SetOfWL 		mSetOfWL = null;
	// data of regions
	private int[] 			mRegionSize = null;
	// maximum size for the merging
	private int 			mSizeThreshold;

	private byte[] 			mForegroundSeeds;
	private byte[] 			mBackgroundSeeds;

	//true if the watershed processing is done
	private boolean 		mIsDone=false;
	private boolean			mIsMergingDone=false;
	//if true, the watershed lines are build
	private int 			mNbofStep;

	// Results Image 

	private ImagePlus mResultImage=null;
	private ImagePlus mWLImage=null;
	private ImagePlus mColouredRegionImage=null;
	private ImagePlus mSegmentedRegionImage=null;





	/**
	 * Constructor
	 * @param imp ImagePlus of the source image which will is flooded
	 * @param SeedsFgImP ImagePlus of the foreground seeds image
	 * @param SeedsBgImP ImagePlus of the background seeds image
	 * @param conn4	true if 4-connexity of region, false if 8-connexity
	 * @param minLevel level where from watershed rises
	 * @param maxLevel level where watershed stops
	 */
	public WatershedData   (ImagePlus impS,
			ImagePlus impW,
			ImagePlus impGradient,
			byte[] foregroundSeeds,
			byte[] backgroundSeeds,
			int ObjectSize,
			boolean conn4,
			boolean isBgCompleted,
			int nbOfStep){

		// 	source image
		mSourceImg = (byte[])impS.getProcessor().getPixelsCopy();
		// 	image used for the watershed (gradient image)
		mImPW = new ImagePlus(impW.getTitle(), impW.getProcessor().duplicate());
		//	image used for the merging (gradient image)
		mGradImg = (byte[])impGradient.getProcessor().getPixelsCopy();
		// 	seeds image of the foreground
		//mSeedsFgImP = new ImagePlus(seedsFgImP.getTitle(), seedsFgImP.getProcessor().duplicate());////
		// seeds tab of the foreground
		mForegroundSeeds = foregroundSeeds;
		//	seeds image of the background
		//mSeedsBgImP = new ImagePlus(seedsBgImP.getTitle(), seedsBgImP.getProcessor().duplicate());////
		// seeds tab of the background
		mBackgroundSeeds = backgroundSeeds;

		mSizeThreshold = ObjectSize;
		mConn4 = conn4;
		//mWLtoBuild=wLtoBuild;
		mNbofStep = nbOfStep;

		mWidth = mImPW.getWidth();
		mHeight = mImPW.getHeight();


		//recording of data from images
		int cmpFg=0;
		int cmpBg=0;
		mPixValue = new int[mWidth*mHeight];
		mLabels = new int[mWidth*mHeight];

		boolean[] SeedsFgCoordtemp = new boolean[mWidth*mHeight];
		boolean[] SeedsBgCoordtemp = new boolean[mWidth*mHeight];

		//	source image pixels value
		byte[] tmpPix;
		tmpPix = (byte[])mImPW.getProcessor().getPixels();
		for(int i=0; i<tmpPix.length; i++){
			mPixValue[i]=tmpPix[i]&0xff;
		}


		if(isBgCompleted){
			// background seeds representes all background pixel
			for(int i=0; i<mWidth; i++){
				for(int j=0; j<mHeight; j++){

					// source image pixels value
					//mPixValue[i*mHeight+j] = mImPW.getProcessor().getPixel(i,j); ///

					mLabels[i+j*mWidth] = 0;

					//if(mSeedsBgImP.getProcessor().getPixel(i,j)==255){///
					if((mBackgroundSeeds[i+j*mWidth]&0xff)==255){
						// label of background  region : definitive label 
						mLabels[i+j*mWidth] = 2;
						////}else if(mSeedsFgImP.getProcessor().getPixel(i,j)==255){ ////
					}else if((mForegroundSeeds[i+j*mWidth]&0xff)==255){
						//label of foreground region
						//N.B: definitive associated label are par
						//  and temporary associated label are odd
						mLabels[i+j*mWidth]= (cmpFg+1)*2+1;
						// the foreground seed is saved
						SeedsFgCoordtemp[i+j*mWidth] = true;
						cmpFg++;
					}					
				}
			}
		}
		else{
			// background seeds representes a part of background pixel

			ImageProcessor erodedSeedsBgImProc = new ByteProcessor(mWidth,mHeight,mBackgroundSeeds.clone(),null);
			//erodedSeedsBgImProc.setPixels(mBackgroundSeeds);
			//ImageProcessor erodedSeedsBgImProc = mSeedsBgImP.getProcessor().duplicate();
			((ByteProcessor)erodedSeedsBgImProc).erode(1,0);
			byte[] erodedBackgroundSeeds = (byte[])erodedSeedsBgImProc.getPixels();

			for(int i=0; i<mWidth; i++){
				for(int j=0; j<mHeight; j++){

					// source image pixels value
					//mPixValue[i*mHeight+j] = mImPW.getProcessor().getPixel(i,j); ////

					mLabels[i+j*mWidth] = 0;

					//if(mSeedsBgImP.getProcessor().getPixel(i,j)==255){////
					if((mBackgroundSeeds[i+j*mWidth]&0xff)==255){
						if((erodedBackgroundSeeds[i+j*mWidth]&0xff)==255){
							// definitive label of background region : this pixel is not a seed
							mLabels[i+j*mWidth] = 2;
						}
						else{
							// temporary label of background region : this pixel is a seed
							mLabels[i+j*mWidth] = 1;
							// the background seed is saved
							SeedsBgCoordtemp[i+j*mWidth] = true;
							cmpBg++;
						}
						//}else if(mSeedsFgImP.getProcessor().getPixel(i,j)==255){////
					}else if((mForegroundSeeds[i+j*mWidth]&0xff)==255){
						//label of foreground region
						//N.B: definitive associated label are par
						//     and temporary associated label are odd
						mLabels[i+j*mWidth]= (cmpFg+1)*2+1;
						// the foreground seed is saved
						SeedsFgCoordtemp[i+j*mWidth] = true;
						cmpFg++;
					}	
				}
			}
		}



		//the number of seeds has to not be too height to the algoithm runs
		mNbBassins = cmpFg+1;
		if(4*mNbBassins==Integer.MAX_VALUE){
			System.err.println("Too many seeds");
			System.exit(-1);
		}

		//tab of index of seeds
		mSeedsCoord = new int[cmpFg+cmpBg];
		//color of seeds
		mRegionColor = new int[mNbBassins][3];
		// background color
		mRegionColor[0][0]=255;
		mRegionColor[0][1]=255;
		mRegionColor[0][2]=255;

		if(isBgCompleted){

			for(int i=0, k=0 ; i<SeedsFgCoordtemp.length ; i++){

				if(SeedsFgCoordtemp[i]){
					mSeedsCoord[k] = i;
					mRegionColor[k+1][0]=(int)(Math.random()*255);
					mRegionColor[k+1][1]=(int)(Math.random()*255);
					mRegionColor[k+1][2]=(int)(Math.random()*255);
					k++;
				}

			}
		}
		else{
			// there are a seeds for the background

			for(int i=0, k=0, n=0; i<SeedsFgCoordtemp.length ; i++){

				if(SeedsBgCoordtemp[i]){
					mSeedsCoord[k] = i;
					k++;
				}
				else if(SeedsFgCoordtemp[i]){
					mSeedsCoord[k] = i;
					mRegionColor[n+1][0]=(int)(Math.random()*255);
					mRegionColor[n+1][1]=(int)(Math.random()*255);
					mRegionColor[n+1][2]=(int)(Math.random()*255);
					k++;
					n++;
				}

			}
		}
		mRegionSize = new int[mNbBassins];
		mSetOfWL = new SetOfWL(mWidth,mHeight, mNbBassins, mGradImg);
	}





	/**
	 * return a tab of pixels of region border
	 */
	public int[] getPixelsOfRegionBorder() throws InterruptedException{

		int[][] pix = mSetOfWL.getPixelList(true);

		if(pix==null){
			IJ.log("error : getListofRegionPixelofWL() return a NULL tab");
			throw new InterruptedException();
		}
		else{

			int count = 0;
			for(int i=0; i<pix.length; i++){
				if(mLabels[pix[i][0] + pix[i][1]*mWidth] != 2)
					count++;
			}
			int[] result = new int[count];
			count = 0;
			int px,py,ind;
			for(int i=0; i<pix.length; i++){
				px = pix[i][0];
				py = pix[i][1];
				ind = px + py*mWidth;
				if(mLabels[ind] != 2){
					result[count] = ind;
					count++;
				}
			}

			return result;
		}

	}



	/**
	 * add a new watershed line to the set
	 * @param r1Label label of the first region
	 * @param r2Label label of the second region
	 * @param xp pixel x position
	 * @param yp pixel y position
	 */
	public void addWLPixel(int r1Label, int r2Label, int xp, int yp){

		mSetOfWL.addWL(r1Label, r2Label, xp, yp);

	}



	/**
	 * construct the different images displayed at the end
	 * @return true if images are correctly built
	 */
	public boolean buildResultsImages(){

		String titleBegin = "Step "+mNbofStep;
		if(mIsMergingDone) 
			titleBegin = titleBegin+" (merging) - ";
		else
			titleBegin = titleBegin+" - ";

		mColouredRegionImage = ImageBuilder.buildRegionImage(this, titleBegin+"Coulored Regions");
		mSegmentedRegionImage = ImageBuilder.buildSegmentedRegionImage(this, titleBegin+"Binary Regions Image");
		mWLImage = ImageBuilder.buildWLImage(this, titleBegin+"Watershed Lines");
		mResultImage = ImageBuilder.buildResultImage(this,mSourceImg, titleBegin+"Results of the Watershed (view 2)");


		if(mColouredRegionImage==null ||
				mSegmentedRegionImage==null ||
				mWLImage==null ||
				mResultImage==null)
		{ 
			return false;
		}
		else{// all images are built
			return true;
		}
	}

	/**
	 * display the source image given by the user
	 */
	public void displaySource(){
		ImageBuilder.display("Source Image", mWidth, mHeight, mSourceImg);
	}

	/**
	 * display the coloured image of regions
	 * @param toRebuild if true : the image must be rebuilt before display, else : the image isn't rebuilt if it is already built 
	 * @return true if a display can be done
	 */
	public boolean displayRegion(boolean toRebuild){

		String titleBegin = "Step "+mNbofStep;
		if(mIsMergingDone) 
			titleBegin = titleBegin+" (merging) - ";
		else
			titleBegin = titleBegin+" - ";

		if(toRebuild || mColouredRegionImage==null)
			mColouredRegionImage = ImageBuilder.buildRegionImage(this, titleBegin+"Coulored Regions");

		if(mColouredRegionImage==null)
			return false;
		else{
			ImageBuilder.display(mColouredRegionImage);
			return true;
		}
	}

	/**
	 * display the binary segmented image
	 * @param toRebuild if true : the image must be rebuilt before display, else : the image isn't rebuilt if it is already built 
	 * @return true if a display can be done
	 */
	public boolean displaySegmentedRegions(boolean toRebuild){

		String titleBegin = "Step "+mNbofStep;
		if(mIsMergingDone) 
			titleBegin = titleBegin+" (merging) - ";
		else
			titleBegin = titleBegin+" - ";

		if(toRebuild || mSegmentedRegionImage==null)
			mSegmentedRegionImage = ImageBuilder.buildSegmentedRegionImage(this, titleBegin+"Binary Regions Image");

		if(mSegmentedRegionImage==null)
			return false;
		else{
			ImageBuilder.display(mSegmentedRegionImage);
			return true;
		}
	}


	/**
	 * display the image of watershed lines
	 * @param toRebuild if true : the image must be rebuilt before display, else : the image isn't rebuilt if it is already built 
	 * @return true if a display can be done
	 */
	public boolean displayWL(boolean toRebuild){

		String titleBegin = "Step "+mNbofStep;
		if(mIsMergingDone) 
			titleBegin = titleBegin+" (merging) - ";
		else
			titleBegin = titleBegin+" - ";

		if(toRebuild || mWLImage==null){
			mWLImage = ImageBuilder.buildWLImage(this, titleBegin+"Watershed Lines");
			if(mWLImage==null)
				return false;

		}
		ImageBuilder.display(mWLImage);
		return true;

	}



	/**
	 * display the image of results of the watershed 
	 * @param toRebuild if true : the image must be rebuilt before display, else : the image isn't rebuilt if it is already built 
	 * @return true if a display can be done
	 */
	public boolean displayResult(boolean toRebuild){

		String titleBegin = "Step "+mNbofStep;
		if(mIsMergingDone) 
			titleBegin = titleBegin+" (merging) - ";
		else
			titleBegin = titleBegin+" - ";

		if(toRebuild || mResultImage==null){
			mResultImage = ImageBuilder.buildResultImage(this,mSourceImg, titleBegin+"Results of the Watershed (view 2)");
		}

		if(mResultImage==null)
			return false;
		else{
			ImageBuilder.display(mResultImage);
			return true;
		}
	}



	/**
	 * launch the region merging process
	 * @param Sensitivity value representing the merging sensibility (entered by the user with the slider)
	 * @param sizeMerging boolean -> if true, merging according to size
	 */
	public void mergingProcess(int Sensitivity, boolean sizeMerging, boolean shapeMerging){

		if(mIsDone)
		{
			if(shapeMerging)
			{
				// Initialization of shape merging
				
				Merging me = new Merging(this);
				me.process();
				
			}
			else{
				//determination of the threshold
				int threshold = thresholdOfStrength(Sensitivity);

				// merging according to strength of watershedlines
				if(threshold!=0){
					mSetOfWL.mergingProcess(threshold,getLabels()); 

			}

			// merging according to region sizes
			measureRegionSizes();
			if(sizeMerging)
				mergingRegionSize();


			mIsMergingDone = true;
			}
		}
	}

	/**
	 * merging according to region sizes (merging of small region with neighbor regions)
	 * @return void
	 */
	private void mergingRegionSize()
	{
		
		// calculation of the size threshold
		int sizeThreshold = 0;
		if (mSizeThreshold==-1)
		{	// user doesn't have give an explicit mean size of regions
			int sum = 0;
			int cmp = 0;
			for(int i=1;i<mRegionSize.length;i++)
			{
				if(mRegionSize[i]!=0){
					sum+=mRegionSize[i];
					cmp++;
				}
			}
			if(cmp!=0)
				sizeThreshold=(sum/cmp)/2;
			else
				sizeThreshold=0;
		}
		else {
			// user has give an explicit mean size of regions
			sizeThreshold=mSizeThreshold/2;
		}
		
		
		int[] neighborhood;
		int min, currentRegionLabel, smallerRegionLabel;
		int i=1;

		while(i<mRegionSize.length)
		{
			// thresholding
			if (mRegionSize[i]<=sizeThreshold && mRegionSize[i]!=0)
			{	
				currentRegionLabel = 2*(i+1);
				neighborhood = mSetOfWL.getNeighborhood(currentRegionLabel);
				int borderSize=0, borderSizeMax, neighInd;
				if(neighborhood!=null){
					// the region has at least one neighbor
					borderSizeMax = -1;
					min = Integer.MAX_VALUE;
					smallerRegionLabel = 0;
					int labelOfRegionWithLongestBorder=0;
					// search of the smaller neighbor and search of the biggest watershed line with a neighbor with a good size
					for(int j=0; j<neighborhood.length; j++){
						if (neighborhood[j]!=2){///
							neighInd = (neighborhood[j]/2-1);
							if( mRegionSize[neighInd] !=0){
								if(min > mRegionSize[neighInd]){// && 							
									smallerRegionLabel = neighborhood[j];
									min = mRegionSize[(smallerRegionLabel/2-1)];
								}
								if(mRegionSize[neighInd]>sizeThreshold){
									borderSize = mSetOfWL.getSizeOfWL(currentRegionLabel, neighborhood[j]);
									if(borderSize > borderSizeMax){
										labelOfRegionWithLongestBorder = neighborhood[j];
										borderSizeMax = borderSize;////
									}
								}
							}
						}
					}
					if(borderSizeMax!=-1){
						// if we found a neighbor with a good size we choose this
						mSetOfWL.mergingRegion(currentRegionLabel, labelOfRegionWithLongestBorder, getLabels());
						mRegionSize[(currentRegionLabel/2-1)] += mRegionSize[(labelOfRegionWithLongestBorder/2-1)]; 
						mRegionSize[(labelOfRegionWithLongestBorder/2-1)] = 0; 
					}

					else{
						// we didn't found a neighbor with a good size
						if(min!=Integer.MAX_VALUE && smallerRegionLabel!=2){
							mSetOfWL.mergingRegion(currentRegionLabel, smallerRegionLabel, getLabels());//
							mRegionSize[(currentRegionLabel/2-1)] += mRegionSize[(smallerRegionLabel/2-1)];// 
							mRegionSize[(smallerRegionLabel/2-1)] = 0; //
						}
						else
							i++;	
					}
				}
				else
					i++;
			}
			else
				i++;
		}
	}

	/**
	 * exclusion according to region sizes
	 * the region with a size inferior or superior of the limit entered by the user are excluded
	 * @param minSize minimum size of the region
	 * @param maxSize maximum size of the region
	 * @return void
	 */
	public void excludingRegion(int minSize,int maxSize)
	{
		int currentRegionLabel;
		for(int i=1;i<mRegionSize.length;i++)
		{	
			if (mRegionSize[i]<minSize || mRegionSize[i]>maxSize)
			{
				currentRegionLabel = 2*(i+1);
				mSetOfWL.mergingRegion(currentRegionLabel, 2, getLabels());
				mRegionSize[0] += mRegionSize[i];
				mRegionSize[i]=0;
			}
		}
	}	

	/**
	 * detection of an automatic treshold
	 * @param mergingSensitivity value representing the merging sensibility (entered by the user with the slider)
	 * @return the threshold
	 */
	public int thresholdOfStrength(int mergingSensitivity){	
		return mSetOfWL.getAutoThresholdOfWL(mergingSensitivity);
	}


	/**
	 * calculate region sizes
	 *
	 */
	public void measureRegionSizes(){
		if(mIsDone){
			mRegionSize = new int[mNbBassins];
			for(int i=0; i<mLabels.length ;i++){
				mRegionSize[(mLabels[i]/2)-1]++;
			}
		}
	}


	/**
	 * print region sizes
	 *
	 */
	public void displayRegionSize(){
		int cmp=0;
		for(int i=0; i<mRegionSize.length; i++){
			cmp = cmp + mRegionSize[i];
			IJ.write("Size of the Region "+i+" : "+mRegionSize[i]);
		}

	}


	/**** getters ****/


	/**
	 * allow to know if it's 4 connected
	 * @return true if it's 4 connected
	 */
	public boolean isConn4() {
		if(mConn4==MainDialog.CONNEXITY_4)
			return true;
		else
			return false;
	}

	/**
	 * allow to know the image height
	 * @return int representing the height image
	 */
	public int getHeight() {
		return mHeight;
	}

	/**
	 * allow to recover the pixel values of the source image
	 * @return an array of pixel image
	 */
	public int[] getPixValue() {
		return mPixValue;
	}

	/**
	 * allow to know the image width
	 * @return int representing the width image
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * allow to know labels of the regions
	 * @return array of the labels regions
	 */
	public int[] getLabels() {
		return mLabels;
	}

	/**
	 * allow to know the index of seeds
	 * @return array of the index of seeds
	 */
	public int[] getSeedsCoord() {
		return mSeedsCoord;
	}

	/**
	 * allow to know the number of seeds
	 * @return the number of seeds
	 */
	public int getNbSeeds() {
		return mNbBassins;
	}

	/**
	 * allow to know if the watershed processing is done
	 * @return true if the watershed processing is done
	 */
	public boolean isDone() {
		return mIsDone;
	}

	/**
	 * allow to know the size of the regions
	 * @return array of the regions sizes
	 */
	public int[] getRegionSize() {
		return mRegionSize;
	}

	/**
	 * return the array of pixels of the gradient image
	 * @return array of the regions sizes
	 */
	public byte[] getGradientMagnitudeImage(){
		return mGradImg;		
	}

	/**
	 * return the colors of the different regions
	 * @return array with the three color (R,G,B) for all the regions
	 */
	public int[][] getRegionColor() {
		return mRegionColor;
	}

	/**
	 * return the array of pixels of the foreground seeds image
	 * @return array of the pixels of foreground seeds image
	 */
	public byte[] getFgSeeds() {
		return mForegroundSeeds;
	}

	/**
	 * return the SetOfWL
	 * @return the SetOfWL
	 */
	public SetOfWL getSetOfWL() {
		return mSetOfWL;
	}

	/**
	 * return the source image
	 * @return return the pixels of the source image
	 */
	public byte[] getImPUser() {
		return mSourceImg;
	}

	public ImagePlus imagePlusConstruction(byte[] imgByte,String title)
	{
		return (new ImagePlus(title,new ByteProcessor(mWidth,mHeight,imgByte,null)));
	}

	/**
	 * return the binary segmented image
	 * @return imageplus of the binary segmented image
	 */
	public ImagePlus getSegmentedRegionImage() {
		if(mSegmentedRegionImage==null)
			mSegmentedRegionImage=ImageBuilder.buildSegmentedRegionImage(this);
		return mSegmentedRegionImage;
	}

	/**
	 * return the gradient image used for the watershed process
	 * @return imageplus of the gradient image
	 */
	public ImagePlus getWatershedImageUsed() {
		return mImPW;
	}

	/**
	 * allow to know the step.
	 * @return 1 -> first watershed process
	 * 2 -> second watershed process
	 */
	public int getNbofStep() {
		return mNbofStep;
	}

	public int getNbBassins() {
		return mNbBassins;
	}


	/**** Setters ****/

	/**
	 * attribute an array of regions labels 
	 * @param labels array of regions labels 
	 */
	public void setLabels(int[] labels) {
		mLabels = labels;
	}
	/**
	 * attribute if the watershed processing is done
	 * @param isDone true if the watershed processing is done
	 */
	public void setIsDone(boolean isDone) {
		mIsDone = isDone;
	}

	/**
	 * attribute an array of sizes regions
	 * @param regionSize array of regions sizes
	 */
	public void setRegionSize(int[] regionSize) {
		mRegionSize = regionSize;
	}

}
