package watershed2D;

import ij.IJ;

import java.util.ArrayList;
import java.util.Vector;


/**
 * this class implements the engin of the seeded watershed
 */
public class SeededWatershed {

	// object where is recorded all obtained data from the watershed process
	private WatershedData mWData = null;
	//if true, we print timing data
	private boolean mProgression = true;
	//if true, the processing has to stop
	private boolean mStop = false;
	// hierarchical FIFO struture
	private ArrayList<Vector<Integer>> mHStack = new ArrayList<Vector<Integer>>(256);
	//tab of labels
	private int[] mLabels = null;
	//tab of pixels values
	private int[] mPixValue = null;
	//width of image
	private int mWidth;
	//height of image
	private int mHeight;
	//current FIFO
	private Vector<Integer> mCurrentStack = null;
	//gray level of the current level of flooding
	private int mCurrentGrayLevel;
	//level of the stack alpha 
	private int mAlphaLevel = 1;
	// alpha FIFO
	private Vector<Integer> mAlphaStack = new Vector<Integer>(20,10);
	//maximum gray level of flooding
	private int mMaxLevel= 255;
	//minimum gray level of flooding
	private int mMinLevel= 0;
	//tab to find the neighborhood
	private int[] mDx=null;
	private int[] mDy=null;
	private int mNbNeighbor;

	//progression counters 
	private int mCountDone=0;
	private int mCountToDo=0;

	private int mID;


	/**
	 * Constructor
	 * @param progression true if timing data are asked
	 * @param wData global data of the watershed process
	 * @param num ID of watershed engin
	 */
	public SeededWatershed(boolean progression,WatershedData wData, int num){


		mProgression = progression;
		mWData = wData;
		mID = num;
		// initialization of hierarchical FIFO structure
		Vector<Integer> stack = null;

		for(int i=mMinLevel; i<=mMaxLevel; i++){
			// FIFO of gray level i
			stack = new Vector<Integer>(20,10);
			mHStack.add(stack);
		}

		mCurrentStack = mHStack.get(0);
		mCurrentGrayLevel = mMinLevel;
		mWidth = mWData.getWidth();
		mHeight = mWData.getHeight();
		mPixValue = mWData.getPixValue();
		mLabels = mWData.getLabels();


		int[] seedsCoord  = mWData.getSeedsCoord();

		// initialization of FIFO hierarchical stack 
		// in function of seeds

		for (int i = 0; i < seedsCoord.length; i++){
			// push the pixel in the FIFO hierarchical structure
			push(seedsCoord[i]);
		}


		// neighborhood tabs
		if (mWData.isConn4()){
			mNbNeighbor = 4;
			mDx = new int[4];
			mDy = new int[4];

			mDx[0] = mDx[3] = 0;
			mDx[1] = -1;
			mDx[2] = 1;
			mDy[1] = mDy[2] = 0;
			mDy[0] = -1;
			mDy[3] = 1;
		}
		else {
			mNbNeighbor = 8;
			mDx = new int[8];
			mDy = new int[8];

			mDx[0] = mDx[3] = mDx[5] = -1;
			mDx[2] = mDx[4] = mDx[7] = 1;
			mDx[1] = mDx[6] = 0;
			mDy[0] = mDy[1] = mDy[2] = -1;
			mDy[5] = mDy[6] = mDy[7] = 1;
			mDy[3] = mDy[4] = 0;
		}
	}




	/**
	 * push a pixel in the FIFO hierarchical structure
	 * @param pixInd index of the pixel
	 */
	private void push(int pixInd){

		// gray level of the pixel
		int graylevel = mPixValue[pixInd];

		// if the associated stack to the gray level was deleted
		// we push the pixel into the current stack
		if(graylevel<=mCurrentGrayLevel){
			mCountToDo++;
			mCurrentStack.addElement(new Integer(pixInd));
			//System.out.println("PUSH at lvl "+mCurrentGrayLevel+"(0) => length:"+mCurrentStack.size());
		}
		else{
			mHStack.get(graylevel-mCurrentGrayLevel).addElement(new Integer(pixInd));
			//System.out.println("PUSH at lvl "+graylevel+"("+(graylevel-mCurrentGrayLevel)+") => length:"+(((Vector)mHStack.get(graylevel-mCurrentGrayLevel)).size()));
		}

	}



	/**
	 * push a pixel in the FIFO hierarchical structure
	 * @param pixInd index of the pixel
	 */
	private void pushAlpha(int pixInd){
		mCountToDo++;
		mAlphaStack.addElement(new Integer(pixInd));
	}


	/**
	 * pop a pixel in the FIFO hierarchical structure
	 */
	private int pop(){
		if(mCurrentStack.size()!=0 ){
			int v = ((Integer)mCurrentStack.firstElement()).intValue();
			mCurrentStack.remove(0);
			mCountDone++;
			mCountToDo--;
			return v;
		}
		else return -1;
	}

	/**
	 * verify if the alpha stack is empty
	 * @return true if the alpha stack is empty
	 */
	private boolean IsAlphaEmpty(){
		if (mAlphaStack.size()==0)
			return true;
		else 
			return false;
	}

	/**
	 * processing of the neighborhood of a pixel which is poped from the hierachical FIFO
	 * @param pixInd index of the poped pixel
	 */
	private void neighborhoodProcessing(int pixInd){
		int px = pixInd%mWidth;
		int py = pixInd/mWidth;
		int npx, npy;


		//index of the neighbor
		int neighborInd;

		// for each neighbor
		for(int i = 0 ; i<mNbNeighbor;i++){

			npx=px+mDx[i];
			npy=py+mDy[i];

			if(npx>=0 && npx<mWidth
				&& npy>=0 && npy<mHeight ){

				neighborInd = (npx) + (npy)*mWidth;
				if(mLabels[neighborInd]==0){
					//the neighbor isn't flooded
					if(mPixValue[neighborInd]<=mCurrentGrayLevel){
						//the neighbor is at the same or inferior level
						// we push it in the alpha stack
						pushAlpha(neighborInd);
						mPixValue[neighborInd]=255+mAlphaLevel;
					}
					else{
						push(neighborInd);
					}

					//labelisation of the neighbor
					mLabels[neighborInd]=mLabels[pixInd]-1;

				}
				else {
					//the neighbor is already flooded

					if(mLabels[neighborInd]%2==1){
						//the neighbor label isn't definitive
						
						if(mPixValue[pixInd]==mPixValue[neighborInd]){
							//the neighbor label has the same intensity


							if(mLabels[neighborInd]+1!=mLabels[pixInd]){
								mWData.addWLPixel(mLabels[pixInd],mLabels[neighborInd]+1,px,py);
							}


						}
						else{
							//the neighbor label hasn't the same intensity


							if(mLabels[pixInd]==mLabels[neighborInd]+1){
								//the neighbor belongs to the same bassin
							}
							else{
								// the neighbor doesn't belong to the same bassin

								mWData.addWLPixel(mLabels[pixInd],mLabels[neighborInd]+1,px,py);
								//mWData.addWLPixel(mLabels[pixInd],mLabels[neighborInd]+1,npx,npy);
								
							}
						}
					}
					else{
						//the neighbor label is definitive

						// update of watershed line
						if(mLabels[neighborInd]!=mLabels[pixInd]){
								mWData.addWLPixel(mLabels[pixInd],mLabels[neighborInd],px,py);
								//mWData.addWLPixel(mLabels[pixInd],mLabels[neighborInd],npx,npy);
							
						}
					}

				}
			}
			else{
				// ERROR : out of bounds
			}
		}

	}


	/**
	 * processing of a pixel which is poped from the hierachical FIFO
	 * @param pixInd index of the pixel to process
	 */
	private void pixelProcessing(int pixInd){

			// the label is maked as definitive
			mLabels[pixInd]++;

			// Processing of the neighborhood
			neighborhoodProcessing(pixInd);

			if(mAlphaLevel>1)
				mPixValue[pixInd]=mCurrentGrayLevel;
	}


	/**
	 * trigger of the end of the thread
	 */
	public synchronized void shutDown() {
		mStop=true;
	}


	/**
	 * stop the threadif it has to be stopped
	 * @throws InterruptedException
	 */
	private synchronized void testEnd() throws InterruptedException {
		if( mStop ) {
			throw new InterruptedException();
		} 
	}


	/**
	 * Flooding of the watershed process
	 */
	public void process() throws InterruptedException {

		int c=0;


		//flooding loop
		while(mCurrentGrayLevel<=mMaxLevel){


			testEnd();

			//pixel index
			int pixInd=-1;

			if (mProgression==MainDialog.PROGRESSION){
				if(mAlphaLevel==1){
					IJ.write("Watershed "+mID+" : flooding of gray level " + mCurrentGrayLevel);
				}
				//stateOfStack();
			}
			IJ.showStatus("Watershed "+mID+" ...");
			IJ.showProgress(mCurrentGrayLevel, mMaxLevel+10);
			//pop the first element of the stack current
			pixInd = pop();



			//while the stack isn't empty
			while(pixInd!=-1){
				testEnd();

				IJ.showStatus("Watershed "+mID+" ...");

				//poped pixel processing
				pixelProcessing(pixInd);
				//pop the first element of the stack current
				pixInd = pop();
				c++;
			}

			if(IsAlphaEmpty()){
				//delete the stack of the gray level
				mHStack.remove(0);
				// rising of the flood
				mCurrentGrayLevel++;
				mAlphaLevel=1;
				if(mCurrentGrayLevel<=mMaxLevel)
					mCurrentStack=mHStack.get(0);

				mCountDone=0;
				mCountToDo=mCurrentStack.size();

			}
			else{
				// alpha stack processing
				mCurrentStack = mAlphaStack;
				mAlphaStack = new Vector<Integer>(20,10);
				mAlphaLevel++;
			}
		}


		testEnd();
		if (mProgression==MainDialog.PROGRESSION){
			if(mAlphaLevel==1){
				IJ.write("flooding : end");
			}
			//stateOfStack();
		}


		testEnd();
		for(int i=0; i<mWidth  ; i++){
			for(int j=0; j<mHeight ; j++){
				if(mLabels[i+j*mWidth]==0){
					//proccessing of unprocessed pixel by the watershed
					mLabels[i+j*mWidth]=2;
				}
			}
		}

		testEnd();

		IJ.showProgress(1.0);
		mWData.setLabels(mLabels);
		mWData.setIsDone(true);
	}
}

