package watershed2D;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * This class implements the progress of the different steps of algorithm
 */
public class Processing implements Runnable {

	private byte[] foregroundSeeds1;
	private byte[] backgroundSeeds1;
	private byte[] foregroundSeeds2;
	private byte[] backgroundSeeds2;

	private boolean mIsInit = false;

	private Thread mThread = null;
	private WatershedData mWData1 = null;
	private WatershedData mWData2 = null;
	SeededWatershed mSWE = null;

	private boolean mProgression = true;
	private ImagePlus mImp = null;
	private ImagePlus mImpGrad = null;
	private ImagePlus mImpGradMerge = null;
	private ImagePlus mImpSegmentedRegion = null;
	private ImagePlus mImpDistanceMap16bits = null;
	private ImagePlus mImpDistanceMap8bits = null;

	private boolean mImageType;
	private boolean mIsConn4;
	private boolean mSizeMerging;
	private boolean mStep1= false;
	private boolean mStep2= false;
	private boolean mSizeExcluding;
	private int[] mPixelsOfRegionBorder=null;

	//private int mObjectSize;
	private int mMinObjectSize=0;
	private int mMaxObjectSize=0;

	private int mFirstSensitivity = 0;
	private int mSecondSensitivity = 0;
	private int mObjectSize=0;

	private boolean mStopThread=false;
	private JButton mButton=null;
	private String mButtonTitleInit=null;
	private boolean mIsDone=false;



	/**
	 * constructors
	 */	

	/**
	 * take the different values enter by the user.
	 * @param imp ImagePlus of the source image
	 * @param firstMergingSensitivity value used for the first merging
	 * @param secondMergingSensitivity value used for the second merging
	 * @param objectSize mean size of an object
	 * @param minObjectSize minimum size to keep objects
	 * @param maxObjectSize maximum size to keep objects
	 * @param button button used to start or stop the processing
	 * @param buttonTitleInit name of the button
	 * @return ShortProcessor the distance map
	 */
	public Processing(ImagePlus imp,
			ImagePlus foregroundImp,
			ImagePlus backgroundImp,
			boolean[] param, 
			int firstMergingSensitivity, 
			int secondMergingSensitivity,
			int objectSize,
			int minObjectSize,
			int maxObjectSize,
			JButton button,
			String buttonTitleInit) {

		mImp= new ImagePlus(imp.getTitle(), imp.getProcessor().duplicate());
		mImp.getProcessor().smooth();
		
		if(param[0]==MainDialog.DARK)
			mImp.getProcessor().invert();

		mImageType = param[1];
		mProgression = param[2];
		mIsConn4 = param[3];
		mSizeMerging = param[4];
		//mWLtoBuild = param[5];
		mSizeExcluding = param[5];

		mObjectSize=objectSize;
		mMinObjectSize=minObjectSize;
		mMaxObjectSize=maxObjectSize;

		mFirstSensitivity = firstMergingSensitivity;
		mSecondSensitivity = secondMergingSensitivity;

		mButton = button;
		mButtonTitleInit = buttonTitleInit;
		mIsDone=false;

		
		
		backgroundImp.getProcessor().invert();
		backgroundSeeds1 = (byte[])backgroundImp.getProcessor().getPixels();
		backgroundImp.getProcessor().erode();
		//backgroundImp.show();
//		IJ.save(backgroundImp, "./background.tif");
//				
		
		foregroundSeeds1 = SeedSearcher.foregroundSeedsByUser(foregroundImp);		
		if (imp.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Source Image 8-bits required.");
			return;
		}

		mIsInit = true;
	}	


	/**
	 * create the different images used for the first watershed and merging
	 */
	private void BuildImages0(){
		if(mStep1){
			if (mProgression)
				IJ.log("Seeded images and gradient image Building...");

			mImpGrad = ImageBuilder.gradientImage(mImp,mImageType);
			if (mImageType == MainDialog.PEAKS)
				mImpGradMerge = ImageBuilder.buildGradientImageWithPeaksForMerging2(mImp);
			else
				mImpGradMerge = mImpGrad;

			//mImpGradMerge.show();
			//array of foreground pixels
			if(backgroundSeeds1 == null)
				backgroundSeeds1 = SeedSearcher.backgroundPixels1(mImp);/////
			//ImageBuilder.display(new ImagePlus("background",new ByteProcessor(mImp.getWidth(),mImp.getHeight(),backgroundSeeds1,null)));
			
			if(foregroundSeeds1==null)
				foregroundSeeds1 = SeedSearcher.searchForegroundSeeds1(mImp,new ImagePlus("foreground",new ByteProcessor(mImp.getWidth(),mImp.getHeight(),backgroundSeeds1,null)),mImageType);//////
			//array of background pixels
			//ImageBuilder.display("fore", mImpGrad.getWidth(), mImpGrad.getHeight(), foregroundSeeds1);
			//ImageBuilder.display("back", mImpGrad.getWidth(), mImpGrad.getHeight(), backgroundSeeds1);
	    	
			
			ImagePlus imgDupli = new ImagePlus("backgournd",new ByteProcessor(mImp.getWidth(),mImp.getHeight(),backgroundSeeds1,null));
			ImageProcessor ipBackgroundSeeds = imgDupli.getProcessor();    	    
			//ipBackgroundSeeds.invert();  	    
	    	//imgDupli.show();
			mImpDistanceMap16bits = ImageBuilder.buildDistanceMap(imgDupli);
			mImpDistanceMap16bits.show();
			mImpDistanceMap8bits = ImageBuilder.convertTo8bits(mImpDistanceMap16bits);													
			mImpDistanceMap8bits.getProcessor().invert();
			mImpDistanceMap8bits.show();
		}
	}
	
	

	/**
	 * create the different images used for the first watershed and merging
	 */
	private void BuildImages1(){
		if(mStep1){
			if (mProgression)
				IJ.log("Seeded images and gradient image Building...");

			mImpGrad = ImageBuilder.gradientImage(mImp,mImageType);
			if (mImageType == MainDialog.PEAKS)
				mImpGradMerge = ImageBuilder.buildGradientImageWithPeaksForMerging2(mImp);
			else
				mImpGradMerge = mImpGrad;

			//mImpGradMerge.show();
			//array of foreground pixels
			if(backgroundSeeds1 == null)
				backgroundSeeds1 = SeedSearcher.backgroundPixels1(mImp);/////
			//ImageBuilder.display(new ImagePlus("background",new ByteProcessor(mImp.getWidth(),mImp.getHeight(),backgroundSeeds1,null)));
			
			if(foregroundSeeds1==null)
				foregroundSeeds1 = SeedSearcher.searchForegroundSeeds1(mImp,new ImagePlus("background",new ByteProcessor(mImp.getWidth(),mImp.getHeight(),backgroundSeeds1,null)),mImageType);//////
			//array of background pixels
			//ImageBuilder.display("fore", mImpGrad.getWidth(), mImpGrad.getHeight(), foregroundSeeds1);
			//ImageBuilder.display("back", mImpGrad.getWidth(), mImpGrad.getHeight(), backgroundSeeds1);
		}
	}

	/**
	 * create the different images used for the second watershed and merging
	 */
	private void BuildImages2(){
		if(mStep2){
			if (mProgression)
				IJ.log("Seeded images and gradient image Building...");
			
	    	ImagePlus imgDupli = new ImagePlus("background image",mImp.getProcessor().duplicate());
	    	ImageProcessor ipBackgroundSeeds = imgDupli.getProcessor();    	    	    	
	    	ipBackgroundSeeds.threshold(ipBackgroundSeeds.getAutoThreshold());
	    	ipBackgroundSeeds.erode();
	    	ipBackgroundSeeds.invert();  	    
	    	//imgDupli.show();
	    	
			mImpDistanceMap16bits = ImageBuilder.buildDistanceMap(imgDupli);
			//mImpDistanceMap16bits.show();
			mImpDistanceMap8bits = ImageBuilder.convertTo8bits(mImpDistanceMap16bits);
			foregroundSeeds2 = SeedSearcher.foregroundSeeds2(mImpDistanceMap16bits);					
			
			backgroundSeeds2 = SeedSearcher.backgroundSeeds2(mWData1.getLabels());
			mImpDistanceMap8bits.getProcessor().invert();
		}
	}




	/**
	 * starting of the thread
	 */
	public void start() {
		if (mThread != null) {
			return;
		}
		mThread = new Thread(this);
		mThread.setPriority(Thread.MIN_PRIORITY);
		mThread.start();
	}


	/**
	 * trigger of the end of the thread
	 */
	public synchronized void shutDown() {
		if (mThread != null) {

			if(mSWE!=null)
				mSWE.shutDown();

			mStopThread=true;
		}
	}


	/**
	 * stop the threadif it has to be stopped
	 * @throws InterruptedException
	 */
	private synchronized void testEnd() throws InterruptedException {
		if( mStopThread ) {
			throw new InterruptedException();
		} 
	} 

	/**
	 * return true if the processing is stopped
	 */
	public boolean isStopped() {
		return mStopThread;
	}

//	/**
//	 * Implements the run for the Runnable.
//	 * different methods called during the progress of the segmentation
//	 */
//	public void run() {
//		try {
//			double t = System.currentTimeMillis();
//			if(mIsInit==false)
//				throw new InterruptedException();
//
//			mIsDone=false;
//			mWData1 = null;
//			mWData2 = null;
//			if(mProgression) IJ.getTextPanel().clear();
//
//			//////	 Step 1 //////
//
//			IJ.showStatus("Initialization of Step 1/2 ...");
//
//			if(mProgression) IJ.write("===== STEP 1 =====");
//			mStep1 = true;
//			mStep2 = false;
//			// initialization of the first step
//			BuildImages1();
//			testEnd();
//
//			//initialization of watershed data
//			mWData1 = new WatershedData(mImp,
//					mImpGrad,//
//					mImpGradMerge,
//					foregroundSeeds1,
//					backgroundSeeds1,
//					mObjectSize,
//					mIsConn4,
//					false,
//					1
//			);
//			testEnd();
//
//			//// First Watershed Processing ////
//			firstWatershedProcess();
//			testEnd();
//
//			//// MERGING 1 ////
//			//merging1();
//			testEnd();
//
//			//// images Building ////
//			if(mProgression) IJ.write("Images building ...");
//			IJ.showStatus("Images building ...");
//			mWData1.buildResultsImages();
//
//			//showResultsWindow("Results 1", mWData1);
//			testEnd();
//			//showResultsWindow("Results", mWData1);
//
//			////// Step 2 //////
//
//			IJ.showStatus("Initialization of Step 2/2 ...");
//
//			if(mProgression) IJ.write("===== STEP 2 =====");
//			mStep1 = false;
//			mStep2 = true;
//			// initialization of the second step
//			mImpSegmentedRegion = mWData1.getSegmentedRegionImage();
//			
//			
//			showResultsWindow("Results1", mWData1);
//			
//			if(mProgression) IJ.write("seeds images building");
//			
//			BuildImages2();
//			testEnd();
//
//			
//
//			
//			// initialization of watershed data
//			mWData2 = new WatershedData(mImp,
//					mImpGrad,
//					mImpGradMerge,
//					foregroundSeeds1,
//					backgroundSeeds2,
//					mObjectSize,
//					mIsConn4,
//					true,
//					2
//			);
//			testEnd();
//
//
//			//// second watershed processing ////
//			secondWatershedProcess();
//			testEnd();
//
//
//
//			//// display of results ////
//			//mWData2.displayRegionSize();
//			testEnd();
//
//			
//
//			//// merging 2 ////
//			merging2();
//			testEnd();
//			
//
//			//// Excluding on size region ////
//			//if(mSizeExcluding)
//				//mWData2.excludingRegion(mMinObjectSize,mMaxObjectSize);
//
//			//// images building ////
//			if(mProgression) IJ.write("Images building ...");
//			IJ.showStatus("Images building ...");
//			//mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);
//			//mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);
//			mWData2.buildResultsImages();
//
//			testEnd();
//
//			if(mProgression){
//				IJ.write("Time: " +  IJ.d2s(System.currentTimeMillis() - t) + " ms");
//				IJ.write("END");
//			}
//
//
//			mButton.setText(mButtonTitleInit);
//			mThread = null;
//			mIsDone=true;
//			IJ.showProgress(1.0);
//			IJ.showStatus("End");
//
//			showResultsWindow("Results", mWData2);
//		}
//		catch( InterruptedException e ) {
//
//			mButton.setText(mButtonTitleInit);
//			if(mProgression)
//				IJ.write("\nProcessing stopped !\n");
//
//			IJ.showProgress(1.1);
//			IJ.showStatus("Processing stopped");
//		}
//	}
	
	
//	/**
//	 * Implements the run for the Runnable.
//	 * different methods called during the progress of the segmentation
//	 */
//	public void run() {
//		try {
//			double t = System.currentTimeMillis();
//			if(mIsInit==false)
//				throw new InterruptedException();
//
//			mIsDone=false;
//			mWData2 = null;
//			if(mProgression) IJ.getTextPanel().clear();
//
//			//////	 Step 1 //////
//			IJ.showStatus("Initialization of Step 1/2 ...");
//
//			if(mProgression) IJ.log("===== STEP 1 =====");
//			mStep1 = true;
//			mStep2 = false;
//
//			////// Step 2 //////
//
//			IJ.showStatus("Initialization of Step 2/2 ...");
//
//			if(mProgression) IJ.log("===== STEP 2 =====");
//			
//			BuildImages1();
//			
//			if(mProgression) IJ.log("seeds images building");
//			
//			mStep1 = false;
//			mStep2 = true;
//			
//			BuildImages2();
//			testEnd();
//			
//			// initialization of watershed data
//			mWData2 = new WatershedData(mImp,
//					mImpDistanceMap8bits,
//					mImpGradMerge,
//					foregroundSeeds2,
//					backgroundSeeds1,
//					mObjectSize,
//					mIsConn4,
//					true,
//					2
//			);
//			testEnd();
//
//
//			//// second watershed processing ////
//			secondWatershedProcess();
//			testEnd();
//
//
//
//			//// display of results ////
//			//mWData2.displayRegionSize();
//			testEnd();
//
//			//// merging 2 ////
//			merging2();
//			//testEnd();
//			
//
//			//// Excluding on size region ////
//			//if(mSizeExcluding)
//				//mWData2.excludingRegion(mMinObjectSize,mMaxObjectSize);
//
//			//// images building ////
//			if(mProgression) IJ.log("Images building ...");
//			IJ.showStatus("Images building ...");
//			//mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);
//			//mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);
//			mWData2.buildResultsImages();
//
//			testEnd();
//
//			if(mProgression){
//				IJ.log("Time: " +  IJ.d2s(System.currentTimeMillis() - t) + " ms");
//				IJ.log("END");
//			}
//
//
//			mButton.setText(mButtonTitleInit);
//			mThread = null;
//			mIsDone=true;
//			IJ.showProgress(1.0);
//			IJ.showStatus("End");
//						
//			Merging me = new Merging(mWData2);
//			me.fillHolesAndBuildRoi(me.mRegionSeeds);						
//			showResultsWindow("Results", mWData2);
//		}
//		catch( InterruptedException e ) {
//
//			mButton.setText(mButtonTitleInit);
//			if(mProgression)
//				IJ.log("\nProcessing stopped !\n");
//
//			IJ.showProgress(1.1);
//			IJ.showStatus("Processing stopped");
//		}
//	}

	/**
	 * Implements the run for the Runnable.
	 * different methods called during the progress of the segmentation
	 */
	public void run() {
		try {


			double t = System.currentTimeMillis();

			
			if(mIsInit==false)
				throw new InterruptedException();

			mIsDone=false;
			mWData1 = null;
			mWData2 = null;
			if(mProgression) IJ.getTextPanel().clear();

			//////	 Step 1 //////

			IJ.showStatus("Initialization of Step 1/2 ...");

			if(mProgression) IJ.write("===== STEP 1 =====");
			mStep1 = true;
			mStep2 = false;
			// initialization of the first step
			BuildImages0();
			BuildImages1();
			testEnd();

			//initialization of watershed data
			mWData1 = new WatershedData(mImp,
					//mImpDistanceMap8bits,
					mImpGrad,//					
					mImpGradMerge,
					foregroundSeeds1,
					backgroundSeeds1,
					mObjectSize,
					mIsConn4,
					true,
					1
			);
			testEnd();

			//// First Watershed Processing ////
			firstWatershedProcess();
			testEnd();

			//// MERGING 1 ////
			merging1();
			testEnd();

			//// images Building ////
			if(mProgression) IJ.write("Images building ...");
			IJ.showStatus("Images building ...");
			mWData1.buildResultsImages();

			//showResultsWindow("Results 1", mWData1);
			testEnd();
			showResultsWindow("Results", mWData1);

			////// Step 2 //////

			IJ.showStatus("Initialization of Step 2/2 ...");

			if(mProgression) IJ.write("===== STEP 2 =====");
			mStep1 = false;
			mStep2 = true;
			// initialization of the second step
			mImpSegmentedRegion = mWData1.getSegmentedRegionImage();
			
			
			showResultsWindow("Results1", mWData1);
			
			Merging me = new Merging(mWData1);
			me.fillHolesAndBuildRoi(me.mRegionSeeds);	
			
//			if(mProgression) IJ.write("seeds images building");
//			
//			BuildImages2();
//			testEnd();
//
//			
//
//			// initialization of watershed data
//			mWData2 = new WatershedData(mImp,
//					mImpDistanceMap8bits,
//					mImpGradMerge,
//					foregroundSeeds1,
//					backgroundSeeds1,
//					mObjectSize,
//					mIsConn4,
//					true,
//					2
//			);
//			testEnd();
//
//
//			//// second watershed processing ////
//			secondWatershedProcess();
//			testEnd();
//
//
//
//			//// display of results ////
//			//mWData2.displayRegionSize();
//			testEnd();
//
//			
//
//			//// merging 2 ////
//			merging2();
//			testEnd();
//			
//
//			//// Excluding on size region ////
//			if(mSizeExcluding)
//				mWData2.excludingRegion(mMinObjectSize,mMaxObjectSize);
//
//			//// images building ////
//			if(mProgression) IJ.write("Images building ...");
//			IJ.showStatus("Images building ...");
//			//mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);
//			mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,true);
//			mWData2.buildResultsImages();
//
//			testEnd();
//
//			if(mProgression){
//				IJ.write("Time: " +  IJ.d2s(System.currentTimeMillis() - t) + " ms");
//				IJ.write("END");
//			}
//
//
//			mButton.setText(mButtonTitleInit);
//			mThread = null;
//			mIsDone=true;
//			IJ.showProgress(1.0);
//			IJ.showStatus("End");
//
//			showResultsWindow("Results", mWData2);
		}
		catch( InterruptedException e ) {

			mButton.setText(mButtonTitleInit);
			if(mProgression)
				IJ.write("\nProcessing stopped !\n");

			IJ.showProgress(1.1);
			IJ.showStatus("Processing stopped");
		}
	}

	
	/**
	 * build a gradient image from the source image
	 * @param title title of the results window
	 * @param wData watershed data
	 */
	private void showResultsWindow(String title, WatershedData wData){

		if(wData.isDone()){
			//Create and set up the window.
			JFrame frame = new JFrame(title);
			ResultsWindow rw = new ResultsWindow(wData);
			rw.setOpaque(true);

			frame.setContentPane(rw);
			//frame.addWindowListener(new AdaptedWindowListener(rw));

			//Display the window.
			frame.pack();
			frame.setResizable(false);
			frame.setVisible(true);
		}

	}

	/**
	 * run the first watershed
	 * @throws InterruptedException
	 */
	private void firstWatershedProcess() throws InterruptedException {


		// flooding
		if (mProgression==MainDialog.PROGRESSION) IJ.write("== Step Watershed 1 ==");

		if (mProgression==MainDialog.PROGRESSION) IJ.write("Initilization of the watershed processing");
		// initialization of Watershed
		mSWE = new SeededWatershed(mProgression, mWData1, 1);


		if (mProgression==MainDialog.PROGRESSION)
			IJ.write("Launching of the first watershed processing ");

		IJ.showStatus("First Watershed Processing ...");
		mSWE.process();
		mSWE=null;
		testEnd();

		mWData1.measureRegionSizes();

		if (mProgression==MainDialog.PROGRESSION){
			IJ.write("First Watershed finished");
		}
	}



	/**
	 * merging of region after the first watershed processing
	 */
	private synchronized void merging1()
	{
		if(mProgression==MainDialog.PROGRESSION) 
			IJ.write("== Step Merging 1 ==");

		if(mProgression==MainDialog.PROGRESSION) 
			IJ.write("Merging...");

		IJ.showStatus("Merging ...");
		mWData1.mergingProcess(mFirstSensitivity,mSizeMerging,false);


	}





	/**
	 * run the second watershed
	 * @throws InterruptedException
	 */
	private void secondWatershedProcess() throws InterruptedException {

		// flooding
		if(mProgression) IJ.write("== Step Watershed 2 ==");

		if(mProgression) IJ.write("Initilization of the watershed processing");
		// initialization of Watershed
		mSWE = new SeededWatershed(mProgression, mWData2, 2);


		if (mProgression==MainDialog.PROGRESSION) IJ.write("Launching of the second watershed processing ");

		mSWE.process();
		mSWE=null;
		testEnd();

		mWData2.measureRegionSizes();

		if (mProgression==MainDialog.PROGRESSION){
			IJ.write("Second watershed finished");
		}

	}




	/**
	 * merging of region after the first watershed processing
	 */
	private void merging2()
	{
		if(mProgression) 
			IJ.write("== Step Merging 2 ==");

		//merging 
		if(mProgression) 
			IJ.write("Merging ...");

		IJ.showStatus("Merging ...");
		mWData2.mergingProcess(mSecondSensitivity,mSizeMerging,false);

	}





	/**
	 * return the final watershed data 
	 * @return the watershedData
	 */
	public WatershedData getResults() {
		if(mIsDone)
			return mWData2;
		else return null;
	}
}	



