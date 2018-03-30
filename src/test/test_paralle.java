package test;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Frame;
import java.awt.List;
import java.util.Hashtable;
import java.util.Vector;

public class test_paralle implements PlugIn {
public static boolean debug = false;
	static class EvenGenerator {

		private int currentValue = 0;
		private boolean cancled = false;

		public int next() {
			++currentValue;       //危险！
			++currentValue;
			return currentValue;
		}

		public boolean isCancled() {
			return cancled;
		}
		public void cancle() {
			cancled = true;
		}
	}

	static class EvenChecker implements Runnable {

		private EvenGenerator generator;

		public EvenChecker(EvenGenerator generator) {
			this.generator = generator;
		}

		@Override
		public void run() {
			int nextValue;
			while(!generator.isCancled()) {
				nextValue = generator.next();
				if(nextValue % 2 != 0 && nextValue % 2 == 0) {
					System.out.println(nextValue + "不是一个偶数!");
					generator.cancle();
				}
			}
		}
	}



	public static void main(String[] args) 
	{
//		Frame frame = WindowManager.getFrame("ROI Manager");
//		if(frame == null)
//			IJ.run("ROI Manager...");
//		frame = WindowManager.getFrame("ROI Manager");		
//		if (frame==null || !(frame instanceof RoiManager))
//		{return;}
//		frame.setVisible(true);		
//		RoiManager roiManager = (RoiManager)frame;			
//		List list;
//		Hashtable<String, Roi> rois;
//		list = roiManager.getList();
//		rois = roiManager.getROIs();
//
//		if(list.getItemCount() < 1)
//			return;
//		for(int i=0;i<list.getItemCount();i++)
//		{
//			Roi roi = rois.get(list.getItem(i));				
//			ByteProcessor bIp = (ByteProcessor)roi.getMask();
//			new ImagePlus("bIp", bIp.duplicate()).show();
//			bIp.fill(roi);
//			ByteProcessor bIpDilate = (ByteProcessor)IJ.createImage("dilate", "8-bit black", roi.getBounds().width+2, roi.getBounds().height+2, 1).getProcessor();
//			bIpDilate.insert(bIp, 0, 0);								
//			//bIpDilate.dilate();
//			//bIpDilate.dilate();
//			new ImagePlus("before-skeleton", bIpDilate.duplicate()).show();
//			bIpDilate.skeletonize();
//			new ImagePlus("skeleton", bIpDilate).show();
//		}
		
		ImagePlus skeletonImp = IJ.openImage();
		ImagePlus binaryImp = IJ.openImage();		
		test_paralle test_paralle = new test_paralle();
		//test_paralle.getSkeletonLength(skeletonImp,binaryImp);
	}



	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub

		Frame frame = WindowManager.getFrame("ROI Manager");
		if(frame == null)
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");		
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		frame.setVisible(true);		
		RoiManager roiManager = (RoiManager)frame;			
		List list;
		Hashtable<String, Roi> rois;
		list = roiManager.getList();
		rois = roiManager.getROIs();			
		for(int i=0;i<list.getItemCount();i++)
		{
			Roi roi = rois.get(list.getItem(i));	
			roi.setImage(null);
			ByteProcessor bIp = (ByteProcessor)IJ.createImage("dilate", "8-bit black", roi.getBounds().width, roi.getBounds().height, 1).getProcessor();
			//bIp.setColor(Color.black);
			//bIp.setRoi(roi);
			bIp = (ByteProcessor)roi.getMask();
			bIp.invertLut();
			//new ImagePlus("bIp", bIp.duplicate()).show();
			//bIp.fill(roi);
//			ByteProcessor bIpDilate = (ByteProcessor)IJ.createImage("dilate", "8-bit black", roi.getBounds().width, roi.getBounds().height, 1).getProcessor();
//			bIpDilate.invertLut();
//			bIpDilate.insert(bIp, 0, 0);								
			//bIpDilate.dilate();
			//				bIpDilate.dilate();
			//				bIpDilate.dilate();
			//				//bIpDilate.dilate();
			ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
			//binaryImp.show();
			//				bIpDilate.skeletonize();
			bIp.skeletonize();
			//new ImagePlus(list.getItem(i)+"skeleton", bIp).show();
			ImagePlus imp =WindowManager.getCurrentImage();
			getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,imp,roi.getBounds().x,roi.getBounds().y);
			imp.show();
		}
	}

	/**
	 * detect end point if it has only one neighbor of 3x3 
	 * 
	 * @param skeletonImp
	 * @return
	 */
	public double getSkeletonLength(ImagePlus skeletonImp,ImagePlus binaryImp, boolean showSkeleton, ImagePlus imp, int x, int y)
	{
		if(!(skeletonImp.getProcessor() instanceof ByteProcessor))
		{
			System.out.println("wrong type");
			return -1;
		}
		ByteProcessor skeletonIp = (ByteProcessor)skeletonImp.getProcessor();
		int backGround = (int)skeletonIp.getHistogramMax();
		//Byte[] pixels = (Byte[])skeletonImage.getProcessor().getPixels();
		ColorProcessor cp = (ColorProcessor)skeletonImp.getProcessor().convertToRGB();
		int[][] positionEndPoint = new int[10][2]; //end point not more than 5, otherwise give an error message,
		int indexEndPoint = 0;
		int mWidth = skeletonImp.getWidth();
		int mHeight = skeletonImp.getHeight();
		for(int i=0;i<mWidth;i++)
		{
			for(int j=0;j<mHeight;j++)
			{
				int p5 = skeletonIp.get(i, j);
				if(p5 == backGround)
					continue;		   
				int numberOfNeighbour = 0;
				int p1 = skeletonIp.getPixel(i-1, j-1);
				int p2 = skeletonIp.getPixel(i, j-1);
				int p3 = skeletonIp.getPixel(i+1, j-1);
				int p4 = skeletonIp.getPixel(i-1, j);
				int p6 = skeletonIp.getPixel(i+1, j);
				int p7 = skeletonIp.getPixel(i-1, j+1);
				int p8 = skeletonIp.getPixel(i, j+1);
				int p9 = skeletonIp.getPixel(i+1, j+1);
				if(p1 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p2 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p3 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p4 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p6 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p7 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p8 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(p9 != backGround)
					numberOfNeighbour = numberOfNeighbour +1;
				if(numberOfNeighbour==1)//end point touched
				{						
					positionEndPoint[indexEndPoint][0] = i;
					positionEndPoint[indexEndPoint][1] = j;
					indexEndPoint++;
					cp.set(i, j, 1000);
				}
			}
		}
		
		//new ImagePlus("EndPoint", cp).show();
		
		Branch[] branches = new Branch[indexEndPoint];
		//backGround = (int)bIp.getHistogramMax();
		
		ColorProcessor cpBranchesOnBinary = (ColorProcessor)binaryImp.getProcessor().convertToRGB();
		
		for(int indexEd=0;indexEd<indexEndPoint;indexEd++)
		{
			ColorProcessor cpPerBranch = (ColorProcessor)skeletonIp.convertToRGB();
			branches[indexEd] = new Branch(positionEndPoint[indexEd]);
			int[] actualPixel = new int[2]; 
			actualPixel[0] = branches[indexEd].getLastPixel()[0];
			actualPixel[1] = branches[indexEd].getLastPixel()[1];
			int[] previousPixel = branches[indexEd].getLastPixel();			
			int[] neighbour = getNeibghourifOnly(actualPixel, skeletonIp,actualPixel);			
			while(neighbour!=null)
			{
				actualPixel = new int[2]; 
				actualPixel[0] = neighbour[0];
				actualPixel[1] = neighbour[1];
				previousPixel = branches[indexEd].getLastPixel();
				branches[indexEd].addPixelToThisBranch(actualPixel, neighbour[2]);
				neighbour = getNeibghourifOnly(actualPixel, skeletonIp,previousPixel);
			}
			
			for(int[] p:branches[indexEd].getBranchPixels())
			{
				cpPerBranch.set(p[0], p[1],branches[indexEd].getColor());
				//cpBranchesOnBinary.set(p[0], p[1],branches[indexEd].getColor());
			}
			
			int extensionDirection = branches[indexEd].getExtensionDirection();			
			actualPixel = new int[2];
			actualPixel[0] = branches[indexEd].getStartPoint()[0];
			actualPixel[1] = branches[indexEd].getStartPoint()[1];
			
			ImageProcessor binaryIp= binaryImp.getProcessor();
			int[] neighbourExtension = getExtensionNeighbour(actualPixel, skeletonIp,binaryIp,extensionDirection);
			while(neighbourExtension!=null)
			{
				actualPixel = new int[2]; 
				actualPixel[0] = neighbourExtension[0];
				actualPixel[1] = neighbourExtension[1];
				previousPixel = branches[indexEd].getLastPixel();
				branches[indexEd].addPixelToExtension(actualPixel,extensionDirection);
				neighbourExtension = getExtensionNeighbour(actualPixel, skeletonIp, binaryIp,extensionDirection);
			}
			
			for(int[] p:branches[indexEd].getExtensionPixels())
			{
				cpPerBranch.set(p[0], p[1],branches[indexEd].getExtensionColor());
				//cpBranchesOnBinary.set(p[0], p[1],branches[indexEd].getExtensionColor());
			}
			//new ImagePlus(String.valueOf(indexEd), cpPerBranch).show();
		}
		
		
		// merge the branch
		double maxDistance = branches[0].getTotoalLength();
		int i_merge = 0;
		int j_merge = 0;
		
		for(int i=0; i<branches.length;i++)
		{
			for(int j=0;j<branches.length;j++)
			{
				if(i==j)
					continue;

				if(maxDistance < branches[i].getTotoalLength() + branches[j].getTotoalLength())//merging
				{
					maxDistance = branches[i].getTotoalLength() + branches[j].getTotoalLength();
					i_merge = i;
					j_merge = j;
				}
			}
		}
		//System.out.println("Length before merging : " + branches[i_merge].getTotoalLength());		
		branches[i_merge].mergeBranch(branches[j_merge]);
		//System.out.println("Length after merging : " + branches[i_merge].getTotoalLength());
		for(int[] p:branches[i_merge].getBranchPixels())
		{			
			cpBranchesOnBinary.set(p[0], p[1],branches[i_merge].getColor());
		}
		for(int[] p:branches[i_merge].getExtensionPixels())
		{		
			cpBranchesOnBinary.set(p[0], p[1],branches[i_merge].getExtensionColor());
		}
		//new ImagePlus("Branches on Binary", cpBranchesOnBinary).show();		
		if(showSkeleton && imp!=null)
		{			
			//cpBranchesOnBinary.invertLut();			
			if(!(imp.getProcessor() instanceof ColorProcessor))
				imp.setProcessor(imp.getProcessor().convertToRGB());			
			imp.getProcessor().insert(cpBranchesOnBinary, x, y);
		}
		return branches[i_merge].getTotoalLength();
		//return null;
		//return Arrays.copyOf(positionEndPoint, indexEndPoint);
	}
	
	private int[] getExtensionNeighbour(final int[] p, ImageProcessor skeletonIp, ImageProcessor bIp, final int direction)
	{
		int foreGround = (int)skeletonIp.getHistogramMin();
		int i=p[0];
		int j=p[1];
		if(direction == 1 && bIp.getPixel(i-1, j-1)!=foreGround)
		{
			return new int[]{i-1,j-1};
		}else if (direction == 2 && bIp.getPixel(i, j-1)!=foreGround) {
			return new int[]{i,j-1};
		}else if (direction == 3 && bIp.getPixel(i+1, j-1)!=foreGround) {
			return new int[]{i+1,j-1};
		}else if (direction == 4 && bIp.getPixel(i-1, j)!=foreGround) {
			return new int[]{i-1,j};
		}else if (direction == 6 && bIp.getPixel(i+1, j)!=foreGround) {
			return new int[]{i+1,j};
		}else if (direction == 7 && bIp.getPixel(i-1, j+1)!=foreGround) {
			return new int[]{i-1,j+1};
		}else if (direction == 8 && bIp.getPixel(i, j+1)!=foreGround) {
			return new int[]{i,j+1};
		}else if (direction == 9 && bIp.getPixel(i+1, j+1)!=foreGround) {
			return new int[]{i+1,j+1};
		}else {
			return null;
		}
		
	}
	private int[] getNeibghourifOnly(final int[] p, ImageProcessor bIp, final int[] previousP)
	{
		int i=p[0];
		int j=p[1];
		int numberOfNewNeighbour = 0;
		int p1 = bIp.getPixel(i-1, j-1);
		int p2 = bIp.getPixel(i, j-1);
		int p3 = bIp.getPixel(i+1, j-1);
		int p4 = bIp.getPixel(i-1, j);
		int p6 = bIp.getPixel(i+1, j);
		int p7 = bIp.getPixel(i-1, j+1);
		int p8 = bIp.getPixel(i, j+1);
		int p9 = bIp.getPixel(i+1, j+1);
		int[] neighbour = new int[3];
		int backGround = (int)bIp.getHistogramMax();
		if(p1 != backGround)
		{
			if((i-1)!=previousP[0] || (j-1)!=previousP[1])
			{
				neighbour[0] = i-1;
				neighbour[1] = j-1;
				neighbour[2] = 1;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p2 != backGround)
		{		
			if(i!=previousP[0] || (j-1)!=previousP[1])
			{
				neighbour[0] = i;
				neighbour[1] = j-1;
				neighbour[2] = 2;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}				
		}
		if(p3 != backGround)
		{
			if((i+1)!=previousP[0] || (j-1)!=previousP[1])
			{
				neighbour[0] = i+1;
				neighbour[1] = j-1;
				neighbour[2] = 3;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p4 != backGround)
		{
			if((i-1)!=previousP[0] || j!=previousP[1])
			{
				neighbour[0] = i-1;
				neighbour[1] = j;
				neighbour[2] = 4;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p6 != backGround)
		{			
			if((i+1)!=previousP[0] || j!=previousP[1])
			{
				neighbour[0] = i+1;
				neighbour[1] = j;
				neighbour[2] = 6;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p7 != backGround)
		{
			if((i-1)!=previousP[0] || (j+1)!=previousP[1])
			{
				neighbour[0] = i-1;
				neighbour[1] = j+1;
				neighbour[2] = 7;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p8 != backGround)
		{		
			if(i!=previousP[0] || (j+1)!=previousP[1])
			{
				neighbour[0] = i;
				neighbour[1] = j+1;
				neighbour[2] = 8;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		if(p9 != backGround)
		{
			if((i+1)!=previousP[0] || (j+1)!=previousP[1])
			{
				neighbour[0] = i+1;
				neighbour[1] = j+1;
				neighbour[2] = 9;
				numberOfNewNeighbour = numberOfNewNeighbour +1;
			}
		}
		
		if(numberOfNewNeighbour!=1 || i>bIp.getWidth() || j>bIp.getHeight())
			return null;
		else {			
			return neighbour;
		}
	}
}	

class Branch
{
	int label;
	Vector<int[]> branchPixels = new Vector<int[]>();
	Vector<int[]> extendPixels = new Vector<int[]>();	
	int[] startPoint;
	//int[] stopPoint;
	//float distance = 0.0f;
	int diagonalNumber = 0;
	int straightNumber = 0;
	int diagonalNumberExtension = 0;
	int straightNumberExtension = 0;
	int color;
	int extensionColor = 0xff0000;
	int direction;//relative to startPoint (end point of branch)
	boolean directionFlag;
	public Branch(int[] startPoint)
	{
		this.startPoint = startPoint;
		branchPixels.add(startPoint);
		color = (int)(Math.random()*65535.f);
		directionFlag=false;
	}
	
	public int getExtensionDirection()
	{
		if(direction == 1)
			return 9;
		else if(direction == 9)
			return 1;
		else if(direction == 2)
			return 8;
		else if(direction == 8)
			return 2;
		else if(direction == 3)
			return 7;
		else if(direction == 7)
			return 3;
		else if(direction == 4)
			return 6;
		else if(direction == 6)
			return 4;
		else {
			return 5;
		}
	}
	
	
	public void mergeBranch(Branch branch)
	{
		if(sameBranch(branch))
		{
			System.out.println("same branch merging");
			extendPixels.addAll(branch.getExtensionPixels());
			diagonalNumberExtension = diagonalNumberExtension + branch.diagonalNumberExtension;
			straightNumberExtension = straightNumberExtension + branch.straightNumberExtension;
		}else {
			System.out.println("different branch merging");
			branchPixels.addAll(branch.getBranchPixels());
			extendPixels.addAll(branch.getExtensionPixels());
			diagonalNumber = diagonalNumber + branch.diagonalNumber + 1; //not accurate
			straightNumber = straightNumber + branch.straightNumber + 1;
			diagonalNumberExtension = diagonalNumberExtension + branch.diagonalNumberExtension;
			straightNumberExtension = straightNumberExtension + branch.straightNumberExtension;
		}
//		if(branch.branchPixels.size() == this.branchPixels.size() && this.startPoint[0]==branch.getLastPixel()[0] && this.startPoint[1]==branch.getLastPixel()[1]
//				&& this.getLastPixel()[0] == branch.getStartPoint()[0] && this.getLastPixel()[1] == branch.getStartPoint()[1])
//		{
//			extendPixels.addAll(branch.getExtensionPixels());
//			diagonalNumber += branch.diagonalNumber;
//			straightNumber += branch.straightNumber;
//		}else {
//			branchPixels.addAll(branch.getPixels());
//			extendPixels.addAll(branch.getExtensionPixels());
//			
//			// compute the distance between two branches last point,
//			
//		}
	}
	
	private boolean sameBranch(Branch branch)
	{
		if(getMainBranchLength()!=branch.getMainBranchLength())
			return false;
		boolean found = false;
		for(int[] p:branch.getBranchPixels())
		{
			for(int[] pthis:branchPixels)
			{
				if(pthis[0] == p[0] && pthis[1] == p[1])
				{
					found = true;
					break;
				}
			}
			
			if(found)
				continue;
			else {
				return false;
			}
		}
		return true;
	}
			
//			for(int[] pthis:extendPixels)
//			{
//				if(pthis[0] == p[0] && pthis[1] == p[1])
//				{
//					found = true;
//					break;
//				}
//			}
//			
//			if(found)
//				continue;
			
//			if(!found)
//			{
//				return false;
//			}
		
		
		//return true;
		
//		found = false;
//		for(int[] p:branch.getExtensionPixels())
//		{
//			for(int[] pthis:extendPixels)
//			{
//				if(pthis[0] == p[0] && pthis[1] == p[1])
//				{
//					found = true;
//					break;
//				}
//			}
//			
//			if(found)
//				continue;
//			
//			for(int[] pthis:branchPixels)
//			{
//				if(pthis[0] == p[0] && pthis[1] == p[1])
//				{
//					found = true;
//					break;
//				}
//			}
//			
//			if(found)
//				continue;
//			
//			if(!found)
//			{
//				return false;
//			}
//		}
//		return true;
//	}
	
	/**
	 * 
	 * @param p
	 * @param position neighbour's position
	 */
	public void addPixelToThisBranch(int[] p, int position)
	{
		if(!directionFlag)
		{
			direction = position;
			directionFlag = true;
		}		
		if((position&1)!=0)
			diagonalNumber++;
		else {
			straightNumber++;
		}
		branchPixels.add(p);
	}
	
	
	public void addPixelToExtension(int[] p, int position)
	{		
		if((position&1)!=0)
			diagonalNumberExtension++;
		else {
			straightNumberExtension++;
		}
		extendPixels.add(p);
	}
	
	public double getTotoalLength()
	{
		return Math.sqrt(2.0d)*(diagonalNumber+diagonalNumberExtension) + (straightNumber+straightNumberExtension);
	}
	
	private double getMainBranchLength() // without considering extension length
	{
		return Math.sqrt(2.0d)*diagonalNumber + straightNumber;
	}
	
//	public void setStartPoint(int[] p)
//	{
//		startPoint = p;
//	}
	
//	public void setStopPoint(int[] p)
//	{
//		stopPoint = p;
//	}
	
	public int[] getLastPixel()
	{
//		final Iterator<int[]> itr = branchPixels.iterator();
//	    int[] lastElement = (int[])itr.next();
//	    while(itr.hasNext()) {
//	        lastElement=(int[])itr.next();
//	    }
//	    return lastElement;		
		return branchPixels.lastElement();
	}
	
	public int getColor()
	{
		return color;
	}
	
	public int getExtensionColor()
	{
		return extensionColor;
	}
	
	public Vector<int[]> getBranchPixels()
	{
		return branchPixels;
	}
	
	public Vector<int[]> getExtensionPixels()
	{
		return extendPixels;
	}
	
	/**
	 * 
	 * @return endPoint
	 */
	public int[] getStartPoint()
	{
		return startPoint;
	}
	
	
	
}



























