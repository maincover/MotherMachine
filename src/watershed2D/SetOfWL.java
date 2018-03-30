package watershed2D;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.util.ArrayList;
import java.util.Vector;


/**
 * This classes the descriptor of a set of watershed line of a image. 
 */
public class SetOfWL {
	
	
	// watershed line associated to each pixel
	private Vector[] mWLOfPixel;
	// watershed line associated to each regions
	private WatershedLine[] mWLOfRegions;
	// watershed line associated to each regions
	private Vector<WatershedLine> mWLList;

	// Strength associated to each pixel
	private byte[] mStrengthOfPixel;
	private boolean mIsStrengthCalculted;
	
	private int mWidth;
	private int mHeight;
	private int mNbRegions;


	
/*PUBLIC*/
	
	/**
	 * constructor
	 * @param width width of the image
	 * @param height height of the image
	 * @param nbRegions number of segmented regions
	 * @param mapOfStrength pixels array of the gradient image
	 */
	public SetOfWL(int width, int height, int nbRegions, byte[] mapOfStrength){
		
		mWLOfPixel = new Vector[width*height];
		mWLOfRegions = new WatershedLine[nbRegions*nbRegions];
		mWLList = new Vector<WatershedLine>(20,10);
		mStrengthOfPixel = mapOfStrength;
		mIsStrengthCalculted = false;
		mNbRegions = nbRegions;
		mWidth = width;
		mHeight = height;

		
	}


	
	/**
	 * add a pixel to a watershed line if it exists then create this watershed line and add it
	 * @param r1Label label of the first region
	 * @param r2Label label of the second region
	 * @param xp pixel x position
	 * @param yp pixel y position
	 */
	public void addWL(int r1Label, int r2Label, int xp, int yp){
		

	
		int indR1 = r1Label/2-1;
		int indR2 = r2Label/2-1;
		
		if(isPixelOfWL(xp, yp, r1Label, r2Label)==false){
			WatershedLine wl = searchWL(r1Label,r2Label);
			if(wl==null){
				// update of the watershed lines list
				wl = new WatershedLine(r1Label,r2Label);
				mWLList.addElement(wl);
				mWLOfRegions[indR1 + indR2*mNbRegions]=wl;
				mWLOfRegions[indR2 + indR1*mNbRegions]=wl;
			}
			// add the pixel to the pixel list of the WL
			wl.attributePixel(xp, yp, mStrengthOfPixel[xp + yp*mWidth]&0xff);
			attributeWLtoPixel(xp, yp, wl);

			mIsStrengthCalculted= false;
		}
		
	}
	
	/**
	 * merging region according to strength of watershed lines
	 * @param threshold threshold of strength
	 * @param wData watershed data
	 */
	public void mergingProcess(int threshold, int[] labels){
		
		WatershedLine currentWL = null;
		
		
		//merging of regions which have too weak watershed lines
		int i=0;
		while(i<mWLList.size()){
			IJ.showStatus("Merging ...");
			IJ.showProgress(i, mWLList.size()+1);
			currentWL = mWLList.elementAt(i);
			if(currentWL.getStrength()<threshold &&
			   currentWL.isBorderWithBackground()==false){
				
				merge(currentWL.getLabelRegion1(), currentWL, labels);
				i=0;
				
			}
			else{
				i++;
			}
		}
		IJ.showProgress(1.0);
		
	}
	
	/**
	 * merging of two region
	 * @param indR1 label of the first region
	 * @param indR2 label of the second region
	 * @param wData watershed data
	 */
	public void mergingRegion(int r1Label, int r2Label, int[] labels){
		
		WatershedLine wl = searchWL(r1Label, r2Label);
		if(wl!=null)
			merge(r1Label,wl, labels);
		
	}
	
	
	/**
	 * merge two regions in the set of watershed line structure
	 * @param regionLabel label of the final merge region
	 * @param wL watershed line object of two regions
	 * @param wData watershed data
	 */
	private void merge(int regionLabel, WatershedLine wL, int[] labels){
		
		if(wL!=null){
			int[] neighborhoodOfR1;
			int[] neighborhoodOfR2;
			int r1Label = wL.getLabelRegion1();
			int r2Label = wL.getLabelRegion2();
			
			
			WatershedLine wL1;
			WatershedLine wL2;
			
			//IJ.write("Merging : Region "+((indR1/2)-1)+" + Region "+((indR2/2)-1));
			

			// the region result of merging will have the label regionLabel
			// or background if one region is the background
			if(r2Label==regionLabel){ 
				int tmp = r1Label;
				r1Label = r2Label;
				r2Label = tmp;
			}
			if(r2Label==2){ 
				int tmp = r1Label;
				r1Label = r2Label;
				r2Label = tmp;
			}
			
			
			for(int i=0; i<labels.length; i++){
				if(labels[i]==r2Label)
					labels[i]=r1Label;
			}
			
			// merge of watershed lines if necessary
			neighborhoodOfR1 = getNeighborhood(r1Label); 
			neighborhoodOfR2 = getNeighborhood(r2Label);

			boolean isMerged;

			for(int j=0; j<neighborhoodOfR2.length; j++){

				isMerged = false;
				wL2 = searchWL(r2Label,neighborhoodOfR2[j]);

				for(int i=0; i<neighborhoodOfR1.length; i++){

					
					if(neighborhoodOfR1[i]==neighborhoodOfR2[j]){
						// for each common neighboor, merging of the watershed lines
						wL1 = searchWL(r1Label,neighborhoodOfR1[i]);
					
						// merge of wL2 to wL1
						int nbPix = wL2.getNbPixel();
						int xp, yp;
						for(int n=0; n<nbPix; n++){
							xp = wL2.getXPix(n);
							yp = wL2.getYPix(n);
							wL1.attributePixel(xp, yp, 0);
							removeWLfromPixel(xp, yp, wL2);
							attributeWLtoPixel(xp, yp, wL1);
						}
						wL1.setStrengthSum(wL1.getStrengthSum()+wL2.getStrengthSum());
						
						
						isMerged=true;
						
						
						mWLList.removeElement(wL2);
					}
				}
				
				
				if(isMerged){
					// wL2 is merged with wL1 : remove of wL2 in the data
					mWLOfRegions[(r2Label/2-1) + (neighborhoodOfR2[j]/2-1)*mNbRegions]=null;
					mWLOfRegions[(neighborhoodOfR2[j]/2-1) + (r2Label/2-1)*mNbRegions]=null;
					
				}
				else{

					// wL2 is a now a WL between r1Label and the old neighbor of r2Label
					if(wL2.getLabelRegion1()==r2Label)
						wL2.setLabelRegion1(r1Label);
					else
						wL2.setLabelRegion2(r1Label);
					
					mWLOfRegions[(r1Label/2-1) + (neighborhoodOfR2[j]/2-1)*mNbRegions]=wL2;
					mWLOfRegions[(neighborhoodOfR2[j]/2-1) + (r1Label/2-1)*mNbRegions]=wL2;
					mWLOfRegions[(r2Label/2-1) + (neighborhoodOfR2[j]/2-1)*mNbRegions]=null;
					mWLOfRegions[(neighborhoodOfR2[j]/2-1) + (r2Label/2-1)*mNbRegions]=null;
				}
				mWLOfRegions[(r1Label/2-1) + (r1Label/2-1)*mNbRegions]=null;
				
			}
			
			// removing of the watershed line between the merged regions
			mWLList.removeElement(wL);
			mWLOfRegions[(r1Label/2-1) + (r2Label/2-1)*mNbRegions]=null;
			mWLOfRegions[(r2Label/2-1) + (r1Label/2-1)*mNbRegions]=null;
			removeWLtoItsPixels(wL);
			

			//public void clearOnTab(WatershedLine[] wLOfPixel, int width){
				

			//}
			
			
			
			wL = null;
			
		}
		
	}
	

	
	/**
	 * get the neighbor regions of a given region
	 * @param regionLabel label of the region studied
	 * @return tab of indices of the neighbor region
	 */
	public int[] getNeighborhood(int regionLabel){
		
		int regionIndex = regionLabel/2-1;
		
		int[] neighboorhood=null;
		WatershedLine currentWl = null;
		int cmp = 0;
		for(int i=0; i<mNbRegions; i++){

			currentWl = mWLOfRegions[regionIndex + i*mNbRegions];
			
			if( currentWl!=null ){
				cmp++;
				int[] tmp = new int[cmp];
				if(neighboorhood!=null)
					System.arraycopy(neighboorhood, 0, tmp, 0, cmp-1);
				neighboorhood=tmp;

				neighboorhood[cmp-1]= (i+1)*2;

			}
		}

		return neighboorhood;
		
	}

	
	
	/**
	 * indicates if the strength calculation of watershed lines is done
	 * @return true if all strengths are calculated, else false
	 */
	public boolean isStrengthCalculted() {
		return mIsStrengthCalculted;
	}
	
	
	/**
	 * calculate a automatic threshold for the strength of watershed lines
	 * in function of a facor (0-10)
	 * @param sensitivity 	factor of the threshold from 0 to 10 
	 * 					(0: low threshold ; 10: high threshold)
	 * @return a threshold
	 */
	public int getAutoThresholdOfWL(int sensitivity){
		
		double strengthSum =0;
		int wLNb = 0;

		WatershedLine currentWL = null;
		int[] strengthList = new int[mWLList.size()];
		int strength;
		for(int i=0; i<mWLList.size();i++){
			currentWL = mWLList.elementAt(i);
			if(currentWL.isBorderWithBackground()==false){  //not a background watershed line
				strength = currentWL.getStrength();
				strengthSum+=strength;
				strengthList[wLNb]=strength;
				wLNb++;
			}
		}

		if(wLNb==0)
			return 0;
		
		double mean = strengthSum/wLNb;
		
		double tmp;
		strengthSum =0;
		for(int i=0; i<wLNb;i++){
			tmp = (strengthList[i]-mean);
			strengthSum+= (tmp*tmp);
		}
		
		tmp = strengthSum/wLNb;
		double std = Math.sqrt(tmp);
		double strengthMax = mean+2*std;
		double step1 = mean/5;
		double step2 = (strengthMax-mean)/4;

		int threshold;
		
		if(sensitivity<=5)
			threshold=(int)Math.round( sensitivity*(step1) );
		else if(sensitivity<10)
			threshold=(int)Math.round( mean+(sensitivity-5)*(step2) );
		else 
			threshold=255;
		
		return threshold;	
	}
	

	
	/**
	 * return a table of coordinates of the Watershed line pixels
	 * @param getPixelOfWLwithBackground true if we want all watershed lines pixels; false if we don't want the pixels of watershed lines with the background
	 * @return table of coordinates of the Watershed line pixels
	 */
	public int[][] getPixelList(boolean getPixelOfWLwithBackground){

		WatershedLine currentWL = null;

		if(getPixelOfWLwithBackground){
			int pixNb = 0;
			for(int i=0; i<mWLList.size();i++){
				currentWL = mWLList.elementAt(i);
				pixNb += currentWL.getNbPixel();
			}
	
			if(pixNb==0)
				return null;
			
			int[][] tab = new int[pixNb][2];
			for(int i=0, n=0 ; i<mWLList.size();i++){
				currentWL = mWLList.elementAt(i);
				for(int j=0; j<currentWL.getNbPixel(); j++, n++){
					tab[n][0] = currentWL.getXPix(j);
					tab[n][1] = currentWL.getYPix(j);
				}
			}
			
			return tab;
			
		}
		else{
			int pixNb = 0;
			for(int i=0; i<mWLList.size();i++){
				currentWL = mWLList.elementAt(i);
				if(currentWL.isBorderWithBackground()==false)
					pixNb += currentWL.getNbPixel();
			}
	
			if(pixNb==0)
				return null;
			
			int[][] tab = new int[pixNb][2];
			for(int i=0, n=0 ; i<mWLList.size();i++){
				currentWL = mWLList.elementAt(i);
				if(currentWL.isBorderWithBackground()==false){
					for(int j=0; j<currentWL.getNbPixel(); j++, n++){
						tab[n][0] = currentWL.getXPix(j);
						tab[n][1] = currentWL.getYPix(j);
					}
				}
			}
			
			return tab;
		}
		
	}
	
	
	/**
	 * getter on the strength of a watershed line if it exits
	 * @param r1Label label of region 1
	 * @param r2Label label of region 2
	 * @return strength of the watershed line if it exists else return -1
	 */
	public int getStrengthOfWL(int r1Label, int r2Label){
		
		WatershedLine wl = searchWL(r1Label, r2Label);
		if(wl!=null)
			return wl.getStrength();
		else return -1;
		
	}
	
	
	/**
	 * getter on the size of a watershed line if it exits
	 * @param r1Label label of region 1
	 * @param r2Label label of region 2
	 * @return size of the watershed line if it exists else return -1
	 */
	public int getSizeOfWL(int r1Label, int r2Label){
		
		WatershedLine wl = searchWL(r1Label, r2Label);
		if(wl!=null)
			return wl.getNbPixel();
		else return -1;
	}
	
	
	/**
	 * remove the waterhsed line of a region
	 * @param label label of the region
	 */
	public void removeWLofRegion(int label){
		
		int ind = label/2-1;
		WatershedLine wl;
		
		for(int i =0; i<mNbRegions; i++){
			wl = mWLOfRegions[ind + i*mNbRegions];
			if(wl!=null){
				//update of the pixels tab
				removeWLtoItsPixels(wl);
				//update WL list
				mWLList.remove(wl);
				//update of the regions tab
				mWLOfRegions[ind + i*mNbRegions]=null;
				mWLOfRegions[i + ind*mNbRegions]=null;
			}
		}
		
		
	}

	
	/**
	 * @return an list of watershed lines
	 */
	public Vector<WatershedLine> getWLList(){
		return mWLList;
	}

/*private*/
	
	/**
	 * search in the watershed lines list a Watershed line in function of its regions
	 * @param indR1 first region
	 * @param indR2 second region
	 * @return found WatershedLine object if it exists
	 */
	private WatershedLine searchWL(int r1Label, int r2Label){

		return mWLOfRegions[(r1Label/2-1) + (r2Label/2-1)*mNbRegions];
		
		
	}
	

	/**
	 * remove a watershed line between two region if it exists
	 * @param r1Label label of the region 1
	 * @param r2Label label of the region 2
	 */
	public void removeWL(int r1Label, int r2Label){

		int ind1 = r1Label/2-1;
		int ind2 = r2Label/2-1;
		if(mWLOfRegions[ind1 + ind2*mNbRegions]!=null){
			//update of the pixels tab
			removeWLtoItsPixels(mWLOfRegions[ind1 + ind2*mNbRegions]);
			mWLList.remove(mWLOfRegions[ind1 + ind2*mNbRegions]);
			//update of the regions tab
			mWLOfRegions[ind1 + ind2*mNbRegions]=null;
			mWLOfRegions[ind2 + ind1*mNbRegions]=null;
		}
		
	}
	
	/**
	 * determine if a pixel belong to a  WatershedLine between two region
	 * @param xp X position of the pixel
	 * @param yp Y position of the pixel
	 * @param r1Label label of the first region
	 * @param r2Label label of the second region
	 */
	private boolean isPixelOfWL(int xp, int yp, int r1Label, int r2Label){
		
		WatershedLine currentWL;
		Vector<WatershedLine> wlTab = (Vector<WatershedLine>)(mWLOfPixel[xp + yp*mWidth]);
		if(wlTab==null)
			return false;
		
		for(int i=0; i<wlTab.size(); i++){
			currentWL = wlTab.get(i);
			if(r1Label==currentWL.getLabelRegion1() && r2Label==currentWL.getLabelRegion2()
			   || r1Label==currentWL.getLabelRegion2() && r2Label==currentWL.getLabelRegion1())
				return true;
		}
		return false;
	}
	
	
	
	/**
	 * add a WatershedLine to the list of WatershedLine of a pixel
	 * @param xp X position of the pixel
	 * @param yp Y position of the pixel
	 * @param wl WatershedLine to add
	 */
	private void attributeWLtoPixel(int xp, int yp, WatershedLine wl){
		
		int ind = xp + yp*mWidth;
		Vector<WatershedLine> wlTab;
		
		wlTab = (Vector<WatershedLine>)mWLOfPixel[ind];
		
		if(wlTab==null){
			wlTab = new Vector<WatershedLine>(1,1);
			mWLOfPixel[ind] = wlTab;
		}
			
			
		wlTab.add(wl);
		
	}

	
	/**
	 * remove a WatershedLine from the list of WatershedLine of a pixel
	 * @param xp X position of the pixel
	 * @param yp Y position of the pixel
	 * @param wl WatershedLine to remove
	 */
	public void removeWLfromPixel(int xp, int yp, WatershedLine wl){
		
		int ind = xp + yp*mWidth;
		Vector<WatershedLine> wlTab;

		wlTab = (Vector<WatershedLine>)mWLOfPixel[ind];

		if(wlTab==null)
			return;
		
		if(wlTab.size()==1){
			if(wlTab.get(0)==wl)
				mWLOfPixel[ind]=null;
			return;
		}
		else{
			for(int i=0; i<wlTab.size(); i++){
				if(wlTab.get(i)==wl){
					wlTab.remove(i);
				}
			}
		}
		
	}


	/**
	 * update of the pixel tab before the removing of a WL
	 * @param wl the WatershedLine
	 */
	public void removeWLtoItsPixels(WatershedLine wl){
		
		for(int i=0; i<wl.getNbPixel(); i++){
			removeWLfromPixel(wl.getXPix(i), wl.getYPix(i), wl);
		}
		
	}
}
