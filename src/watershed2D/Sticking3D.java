package watershed2D;

import java.util.Arrays;
import java.util.Comparator;

import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class Sticking3D {

	// attributes of regions of each slice
	private RegionAttribute[][] mRAttribute = null;
	// images of regions
	private int[][] mLabels;
	// images width
	private int mWidth;
	// sorted arrays of attributes region according to the coordinates of the bounding box
	private RegionAttribute[][] mXminSortedRAttribute = null;
	private RegionAttribute[][] mXmaxSortedRAttribute = null;
	private RegionAttribute[][] mYminSortedRAttribute = null;
	private RegionAttribute[][] mYmaxSortedRAttribute = null;


	public Sticking3D (Roi[][] roi, int[][] regionSize, int[][] labels, int imageWidth)
	{
		mLabels = labels;
		
		mRAttribute = new RegionAttribute[regionSize.length][];
		mXminSortedRAttribute = new RegionAttribute[regionSize.length][];
		mXmaxSortedRAttribute = new RegionAttribute[regionSize.length][];
		mYminSortedRAttribute = new RegionAttribute[regionSize.length][];
		mYmaxSortedRAttribute = new RegionAttribute[regionSize.length][];
		int cpt;
		for(int i=0;i<regionSize.length;i++)
		{
			cpt=0;
			// counter of region for this slice
			for(int j=0;j<regionSize[i].length;j++)
			{
				if(regionSize[i][j]!=0)
					cpt++;
			}
			mRAttribute[i] = new RegionAttribute[cpt];
			mXminSortedRAttribute[i] = new RegionAttribute[cpt];
			mXmaxSortedRAttribute[i] = new RegionAttribute[cpt];
			mYminSortedRAttribute[i] = new RegionAttribute[cpt];
			mYmaxSortedRAttribute[i] = new RegionAttribute[cpt];
			cpt=0;
			// initialization of arrays
			for(int j=0;j<mRAttribute[i].length;j++)
			{
				if(regionSize[i][j]!=0)
				{
					mRAttribute[i][cpt] = new RegionAttribute(regionSize[i][j],
															roi[i][j].getBounds().x,
															roi[i][j].getBounds().x+roi[i][j].getBounds().width,
															roi[i][j].getBounds().y,
															roi[i][j].getBounds().y+roi[i][j].getBounds().height,
															(j+1)*2,
															cpt);
					mXminSortedRAttribute[i][cpt] = mRAttribute[i][cpt];
					mXmaxSortedRAttribute[i][cpt] = mRAttribute[i][cpt];
					mYminSortedRAttribute[i][cpt] = mRAttribute[i][cpt];
					mYmaxSortedRAttribute[i][cpt] = mRAttribute[i][cpt];
					cpt++;
				}
					
			}
		}
		// sort
		for(int i=0;i<mXminSortedRAttribute.length;i++)
		{
			Arrays.sort(mXminSortedRAttribute[i],new XminComparator());
			Arrays.sort(mXmaxSortedRAttribute[i],new XmaxComparator());
			Arrays.sort(mYminSortedRAttribute[i],new YminComparator());
			Arrays.sort(mYmaxSortedRAttribute[i],new YmaxComparator());
		}	
	}
	
	public int[][] process()
	{
		int[][] associatedIndex = recovering();
		return build3D(associatedIndex);
		
	}

	public int[][] recovering()
	{
		// counter of number of different regions
		int numberOfRegions = 0;
		for(int i=0;i<mRAttribute.length;i++){
			numberOfRegions += mRAttribute[i].length;
		}
		
		//array of associated regions index
		int[][] associatedIndex = null;
		associatedIndex = new int[mRAttribute.length][];
		for(int i=0;i<mRAttribute.length;i++){
			
			associatedIndex[i] = new int[mRAttribute[i].length];
			for(int j=0;j<associatedIndex[i].length;j++)
			{
				associatedIndex[i][j]=-1;
			}
		}
		
		boolean[] goodCandidates=null;
		// for each slice
		for(int i=0;i<mRAttribute.length-1;i++)
		{
			// for each region : we search the regions of the next slice which recover this region in X Y
			for(int j=0;j<mRAttribute[i].length;j++)
			{
				goodCandidates = new boolean[mRAttribute[i+1].length];
				for(int k=mXminSortedRAttribute[i+1].length-1; mXminSortedRAttribute[i+1][k].getXmin() >= mRAttribute[i][j].getXmax(); k--)
				{
					goodCandidates[mXminSortedRAttribute[i+1][k].getIndex()]=false;					
				}
				for(int k=0; mXmaxSortedRAttribute[i+1][k].getXmax() <= mRAttribute[i][j].getXmin(); k++)
				{
					goodCandidates[mXmaxSortedRAttribute[i+1][k].getIndex()]=false;					
				}
				for(int k=mYminSortedRAttribute[i+1].length-1; mYminSortedRAttribute[i+1][k].getYmin() >= mRAttribute[i][j].getYmax(); k--)
				{
					goodCandidates[mYmaxSortedRAttribute[i+1][k].getIndex()]=false;					
				}
				for(int k=0; mYmaxSortedRAttribute[i+1][k].getYmax() <= mRAttribute[i][j].getYmin(); k++)
				{
					goodCandidates[mYmaxSortedRAttribute[i+1][k].getIndex()]=false;					
				}	
				
				for(int k=0;k<goodCandidates.length;k++)
				{
					if(goodCandidates[k])
					{

						// overlaping calculation
						
						double overlapSize =0;
						
						for(int x=mRAttribute[i][j].getXmin(); x<=mRAttribute[i][j].getXmax(); x++)
						{
							for(int y=mRAttribute[i][j].getYmin(); y<=mRAttribute[i][j].getYmax(); y++)
							{
								if(mLabels[i][x+y*mWidth]==mRAttribute[i][j].getLabel()
								   && mLabels[i+1][x+y*mWidth]==mRAttribute[i+1][k].getLabel() )
								{
									overlapSize++;
								}
							}
						}
						
						double n1 = overlapSize/mRAttribute[i][j].getArea();
						double n2 = overlapSize/mRAttribute[i+1][k].getArea();
						
						if( Math.max(n1,n2) > 0.8 )
						{
							associatedIndex[i][j] = mRAttribute[i+1][k].getIndex();
		                    numberOfRegions--;
						}
					}
				}
			}
		}
		return associatedIndex;
	}
	public int[][] build3D(int[][] associatedIndex)
	{
		int[][] resultImage = new int[mLabels.length][mLabels[0].length];
		int color=1;
		for (int i=0;i<associatedIndex.length;i++)
		{
			for(int j=0;j<associatedIndex[i].length;j++)
			{
				if(associatedIndex[i][j] != -2)
				{
					resultImage = attributeColor(resultImage,color,i,j);
					
					int iTemp = i+1;
					int jTemp = associatedIndex[i][j];
					associatedIndex[i][j] = -2;
					while(jTemp!=-1)
					{
						resultImage = attributeColor(resultImage,color,iTemp,jTemp);
						iTemp ++;
						jTemp = associatedIndex[iTemp][jTemp];
					}
					
					color++;
				}
			}
		}
		return resultImage;
	}
	
	public int[][] attributeColor(int[][] resultImage, int color, int slice, int region)
	{
		for(int i=mRAttribute[slice][region].getXmin();i<mRAttribute[slice][region].getXmax();i++)
		{
			for(int j=mRAttribute[slice][region].getYmin();j<mRAttribute[slice][region].getYmax();j++)
			{
//				if(mLabels[slice][i+j*mWidth]==mRAttribute[slice][region].getLabel())
//				{
					resultImage[slice][i+j*mWidth]=color;
//				}
			}
		}
		return resultImage;
	}
	
	
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*int[] goodCandidates;
		int xmin;
		int xmax;
		int ymin;
		int ymax;
		int labelRegion;
		int labelGoodCandidate;
		double eightyPourcentArea;
		int overlapSize=0;
		for(int i=0;i<mLabels.length-1;i++)
		{
			for(int j=0;j<mRoiBounds[i].length;j++)
			{
				labelRegion = mLabels[i][j];
				xmin = mRoiBounds[i][j][0];
				xmax = mRoiBounds[i][j][1];
				ymin = mRoiBounds[i][j][2];
				ymax = mRoiBounds[i][j][3];
				eightyPourcentArea = mRegionSize[i][j]*0.8;
				goodCandidates = new int[mRoiBounds[i+1].length];
				int cpt=0;
				for(int k=0;k<mRoiBounds[i+1].length;k++)
				{
					if((mRoiBounds[i+1][k][0] <= xmax && mRoiBounds[i+1][k][1] >= xmin)&&
						(mRoiBounds[i+1][k][2] <= ymax && mRoiBounds[i+1][k][3] >= ymin))
					{
						goodCandidates[cpt] = k;
						cpt++;
					}
				}

						
						
						
				if(goodCandidates.length>0)
				{
					for(int l=0;l<goodCandidates.length;l++)
					{
						//	overlapSize
						labelGoodCandidate = mLabels[i+1][goodCandidates[l]];
						for(int m=xmin;m<xmax;m++)
						{
							for(int n=ymin;n<ymax;n++)
							{
								if (ip.getPixel(m,n)==labelRegion && 
										ip2.getPixel(m,n)==labelGoodCandidate)
								{
									overlapSize++;
								}
							}
						}
						if(overlapSize>eightyPourcentArea)
						{
							mLabels[i+1][goodCandidates[l]]=labelRegion;
						}
					}
				}
			}
		}
	}*/
	private class RegionAttribute
	{
		private int mArea;
		private int mXmin;
		private int mXmax;
		private int mYmin;
		private int mYmax;
		private int mLabel;
		private int mIndex;
		private int mLabel3D;
		
		public RegionAttribute(int area, int xmin, int xmax, int ymin, int ymax, int label, int index)
		{
			mArea = area;
			mXmin = xmin;
			mXmax = xmax;
			mYmin = ymin;
			mYmax = ymax;
			mLabel = label;
			mIndex = index;
			mLabel3D = -1;
		}

		public int getArea() {
			return mArea;
		}

		public int getLabel() {
			return mLabel;
		}

		public int getLabel3D() {
			return mLabel3D;
		}

		public int getXmax() {
			return mXmax;
		}

		public int getXmin() {
			return mXmin;
		}

		public int getYmax() {
			return mYmax;
		}

		public int getYmin() {
			return mYmin;
		}

		public int getIndex() {
			return mIndex;
		}
		
	}
	
	private class XminComparator implements Comparator<RegionAttribute>
	{
		public int compare(RegionAttribute a1, RegionAttribute a2){
			if(a1.getXmin()>a2.getXmin())
			{
				return 1;
			}
			else if(a1.getXmin()<a2.getXmin())
			{
				return -1;
			}
			else{
				return 0;
			}
		}
		
		public boolean equals(RegionAttribute a){
			
			return false;
		}
	}
	
	private class XmaxComparator implements Comparator<RegionAttribute>
	{
		public int compare(RegionAttribute a1, RegionAttribute a2){
			if(a1.getXmax()>a2.getXmax())
			{
				return 1;
			}
			else if(a1.getXmax()<a2.getXmax())
			{
				return -1;
			}
			else{
				return 0;
			}
		}
		
		public boolean equals(RegionAttribute a){
			
			return false;
		}
	}
	
	private class YminComparator implements Comparator<RegionAttribute>
	{
		public int compare(RegionAttribute a1, RegionAttribute a2){
			if(a1.getYmin()>a2.getYmin())
			{
				return 1;
			}
			else if(a1.getYmin()<a2.getYmin())
			{
				return -1;
			}
			else{
				return 0;
			}
		}
		
		public boolean equals(RegionAttribute a){
			
			return false;
		}
	}
	
	private class YmaxComparator implements Comparator<RegionAttribute>
	{
		public int compare(RegionAttribute a1, RegionAttribute a2){
			if(a1.getYmax()>a2.getYmax())
			{
				return 1;
			}
			else if(a1.getYmax()<a2.getYmax())
			{
				return -1;
			}
			else{
				return 0;
			}
		}
		public boolean equals(RegionAttribute a){
			
			return false;
		}
	}
}

