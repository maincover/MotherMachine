package linear_programming;

import Ij_Plugin.Ins_find_peaks;
import cellst.Image.FftBandPassFilter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ProfilePlot;
import ij.process.ImageProcessor;

//BinaryTree.java 
public class BinaryTree { 
	// Root node pointer. Will be null for an empty tree. 
	private Node root; 
	double[] profile;


	/* 
--Node-- 
The binary tree is built using this nested node class. 
Each node stores one data element, and has left and right 
sub-tree pointer which may be null. 
The node is a "dumb" nested class -- we just use it for 
storage; it does not have any methods. 
	 */ 
	class Node { 
		Node left; 
		Node right; 
		double cost;
		int start;
		int end;
		int center;
		boolean activate = false;
		
		Node(final int center, int start, int end) { 
			left = null; 
			right = null; 
			this.center = center; 
			this.start = start;
			this.end = end;
			computeCost();			
		}
		
		public void computeCost()
		{
			
		}
		
		public boolean hasPeak(double tolerance, double minTrimDist)
		{
			return true;
		}
		
		public void setActivateH()
		{
			this.activate = true;
		}
	}

	/** 
Creates an empty binary tree -- a null root pointer. 
	 */ 
	public BinaryTree(double[] profile) { 
		root = null; 
		this.profile = profile;
	} 


	/** 
Returns true if the given target is in the binary tree. 
Uses a recursive helper. 
	 */ 
	public boolean lookup(int data) { 
		return(lookup(root, data)); 
	} 


	/** 
Recursive lookup  -- given a node, recur 
down searching for the given data. 
	 */ 
	private boolean lookup(Node node, int center) { 
		if (node==null) { 
			return(false); 
		}

		if (center==node.center) { 
			return(true); 
		} 
		else if (center<node.center) { 
			return(lookup(node.left, center)); 
		} 
		else { 
			return(lookup(node.right, center)); 
		} 
	} 


	/** 
Inserts the given data into the binary tree. 
Uses a recursive helper. 
	 */ 
	public void insert(int center, int start, int end) { 
		root = insert(root, center, start, end); 
	} 


	/** 
Recursive insert -- given a node pointer, recur down and 
insert the given data into the tree. Returns the new 
node pointer (the standard way to communicate 
a changed pointer back to the caller). 
	 */ 
	private Node insert(Node node, int position, int start, int end) { 
		if (node==null) { 
			node = new Node(position, start, end); 
		} 
		else { 
			if (position <= node.center) { 
				node.left = insert(node.left, position, start, end); 
			} 
			else { 
				node.right = insert(node.right, position, start, end); 
			} 
		}

		return(node); // in any case, return the new pointer to the caller 
	}
	
	
	public static void main(String[] args)
	{
		ImagePlus imp = IJ.openImage();

		
	}
	
	public static BinaryTree constructBinaryTree(ImagePlus imp)
	{
		int tolerance = 30; // because after fft, the pixel is saturated ,then 30 is a small value
		int minPeakDist = 20; // the smallest cell length, 
		FftBandPassFilter fftBandPassFilter = new FftBandPassFilter();
		fftBandPassFilter.setup("fft band pass filter", imp);
		ImageProcessor ip = imp.getProcessor();
		fftBandPassFilter.run(ip); // smoothing peak		
		ip = ip.convertToByte(true); // change to byte for less computing
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		ImagePlus impPlotProfile = new ImagePlus("profileImp", ip);				
		impPlotProfile.setRoi(width/4, 0, width/2, height);
		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);
		double[] profile = pPlot.getProfile();
		Ins_find_peaks peakFinder = new Ins_find_peaks(tolerance, minPeakDist);				
		Object[] out = peakFinder.findPeaks(profile);// to be changed the extreme points should not be presented
		int[] peakPosition = (int[])out[0];
		int leftcenter = height/2;
		int rightcenter = height/2;
		BinaryTree bTree = new BinaryTree(profile);		
		
		
		for(int i=0; i<peakPosition.length; i++)
		{			
			int peakPos = height - peakPosition[i]; //change coordinate from (top to bottom) to (left to right)
		
			if(peakPos<minPeakDist || peakPos>height-minPeakDist)
				continue;
			
			bTree.insert(peakPos, 0, height); //the root center is the first middle peak position
			break;
		}
		
		
				
		for(int i=0; i<peakPosition.length; i++)
		{			
			int peakPos = height - peakPosition[i]; //change coordinate from (top to bottom) to (left to right)
		
			if(peakPos<minPeakDist || peakPos>height-minPeakDist)
				continue;
			int minleft = Integer.MAX_VALUE;
			int minright = Integer.MAX_VALUE;
			int leftPeak = 0;
			int rightPeak = height;
			for(int j=0; j<i;j++)
			{
				int peakPos2 = height - peakPosition[j]; //change coordinate from (top to bottom) to (left to right)
				if(Math.abs(peakPos2 - peakPos) < minright && peakPos2 > peakPos)
				{
					minright = Math.abs(peakPos2 - peakPos);
					rightPeak = peakPos2;
				}				
				if(Math.abs(peakPos2 - peakPos) < minleft && peakPos2 < peakPos)
				{
					minleft = Math.abs(peakPos2 - peakPos);
					leftPeak = peakPos2;
				}
			}
			leftcenter = (peakPos - leftPeak)/2;
			rightcenter = (peakPos + rightPeak)/2;
			bTree.insert(leftcenter, leftPeak, peakPos);
			bTree.insert(rightcenter, peakPos, rightPeak);
		}
		return bTree;
	}
	
	/** 
	 Given a binary tree, prints out all of its root-to-leaf 
	 paths, one per line. Uses a recursive helper to do the work. 
	*/ 
	public void printPaths() { 
	  int[] path = new int[1000]; 
	  printPaths(root, path, 0); 
	}
	/** 
	 Recursive printPaths helper -- given a node, and an array containing 
	 the path from the root node up to but not including this node, 
	 prints out all the root-leaf paths. 
	*/ 
	private void printPaths(Node node, int[] path, int pathLen) { 
	  if (node==null) return;

	  // append this node to the path array 
	  path[pathLen] = node.center; 
	  pathLen++;

	  // it's a leaf, so print the path that led to here 
	  if (node.left==null && node.right==null) { 
	    printArray(path, pathLen); 
	  } 
	  else { 
	  // otherwise try both subtrees 
	    printPaths(node.left, path, pathLen); 
	    printPaths(node.right, path, pathLen); 
	  } 
	}

	/** 
	 Utility that prints ints from an array on one line. 
	*/ 
	private void printArray(int[] ints, int len) { 
	  int i; 
	  for (i=0; i<len; i++) { 
	   System.out.print(ints[i] + "-"); 
	  } 
	  System.out.println(); 
	} 
	
}

