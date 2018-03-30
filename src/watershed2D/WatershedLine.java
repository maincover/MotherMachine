package watershed2D;


import java.util.Vector;

/**
 * 
 * @author Marc
 * This classe describe a watershed line giving its associated regions, the position and strength 
 * of the pixels and the strength of the watershed line.
 */
public class WatershedLine {

	// index of the first region
	protected int mLabelRegion1;
	// index of the second region
	protected int mLabelRegion2;
	// true if one region is the background
	protected boolean mIsBorderWithBackground;
	
	// position pixels lists of the watershed line
	protected Vector<Integer> mPixelsPosX = new Vector<Integer>(20, 10);
	protected Vector<Integer> mPixelsPosY = new Vector<Integer>(20, 10);
	// strength of each pixel of the watershed line
	protected int mStrengthSum=0;
	
	protected int mNbPix=0;
	// strength of the watershed line
	protected double mStrength=0;
	
	
	/**
	 * Constructor
	 * @param r1 index of the first region
	 * @param r2 index of the second region
	 */
	public WatershedLine(int r1, int r2){

		mLabelRegion1 = r1;
		mLabelRegion2 = r2;
		
		if(r1==2 || r2==2){
			mIsBorderWithBackground=true;
		}
		else{
			mIsBorderWithBackground=false;
		}
	}
	

	/**
	 * associate a pixel and its strength to the watershed line
	 * @param posX 	X position pixel
	 * @param poxY 	Y position pixel
	 * @param strength 	strength of the pixel
	 */
	public void attributePixel(int posX, int posY, int strength){
		
		mPixelsPosX.addElement(new Integer(posX));
		mPixelsPosY.addElement(new Integer(posY));
		mStrengthSum += strength;
		mNbPix++;
		
	}
	

	
	
	
	/*** getters and setters ***/
	
	/**
	 * return the strength of the line
	 * @return the strength of the line
	 */
	public int getStrength() {
		

		if(mNbPix==0){
			// no pixel is associated to the line
			return 0;
		}
		
		return mStrengthSum/mNbPix;
	}
	
	/**
	 * attribute a strength to the line
	 * @param strength the strength of the line
	 */
	public void setStrength(int strength) {
		mStrength = strength;
	}
	
	
	/**
	 * return the index of the first region
	 * @return the index of the first region
	 */
	public int getLabelRegion1() {
		return mLabelRegion1;
	}

	/**
	 * return the index of the second region
	 * @return the index of the second region
	 */
	public int getLabelRegion2() {
		return mLabelRegion2;
	}
    
	/**
	 * return the number of associated pixel to the Watershed line
	 * @return the number of associated pixel to the Watershed line 
	 */
	public int getNbPixel() {
		return mNbPix;
	}

	/**
	 * return the x coordinate associated to the pixels at an index
	 * @param ind index of a pixel
	 * @return the x coordinate associated to the pixels at an index
	 */
	public int getXPix(int ind) {
		return mPixelsPosX.elementAt(ind).intValue();
	}
	/**
	 * return the y coordinate associated to the pixels at an index
	 * @param ind index of a pixel
	 * @return the y coordinate associated to the pixels at an index
	 */
	public int getYPix(int ind) {
		return mPixelsPosY.elementAt(ind).intValue();
	}
	/**
	 * return the sum of all the watershed lines strength
	 * @return the index of the second region
	 */
	public int getStrengthSum() {
		return mStrengthSum;
	}

	/**
	 * allow to know if the watershed line is neighboor with the background
	 * @return true if the watershed line is neighboor with the background
	 */
	public boolean isBorderWithBackground() {
		return mIsBorderWithBackground;
	}
	
	/**
	 * attribute the label of the region 1
	 * @param indRegion1 index of the region 1
	 */
	public void setLabelRegion1(int indRegion1) {
		mLabelRegion1 = indRegion1;
		if(mLabelRegion1==2){
			mIsBorderWithBackground = true;
		}
	}
	/**
	 * attribute the label of the region 2
	 * @param indRegion1 index of the region 2
	 */
	public void setLabelRegion2(int indRegion2) {
		mLabelRegion2 = indRegion2;
		if(mLabelRegion2==2){
			mIsBorderWithBackground = true;
		}
	}


	public void setStrengthSum(int strengthSum) {
		mStrengthSum = strengthSum;
	}	
}
