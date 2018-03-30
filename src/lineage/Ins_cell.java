package lineage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Arrow;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Vector;

import Ij_Plugin.Ins_find_peaks;

//import Ij_Plugin.Ins_find_peaks;

public class Ins_cell implements Comparable{	
	private int slice;	
	private int length;
	private double skeletonLength;
	private int[] position;
	private int timeIndex;
	private int cell_num=-1;
	private Roi roi;
	
	private Roi roi1;
	private Roi roi2;
	
	private String label;
	protected String name;
	protected String time="-1";
	private boolean vitrualState=false;
	
	
	protected double eGrowthRate;	
	protected double pGrowthRate;	
	protected double cGrowthDiff;
	protected double growthDiff;
	
	protected double sumIntensity=0;
	
	protected double meanIntensity = 0;
	
	protected double[] meanIntensityArray= null;
	protected double medianIntensity = 0;
	
	protected double meanIntensityBackground=0;
	protected double area=0;
	
	protected double[] areaArray;
	
	//protected double bgIntensity = 0;
	
	protected Ins_cell nextTopCell=null;
	protected Ins_cell nextBottomCell=null;
	protected Ins_cell parent=null;
	protected Arrow arrowToTop=null;
	protected Arrow arrowToBottom=null;
	protected Ins_cell nextCell=null;
	protected Arrow arrowToNext=null;
	
	protected Ins_cell previousCell = null;
	protected Ins_cell previousLowerCell = null;
	
	
	protected boolean ifLastCell =false;
	
	//protected boolean ifdevidedNextFrame;
	protected boolean ifdevidedFromPreviousCell = false;
	
	protected String type ="NO"; //0-comet 1-dot 2-comet&dot -1-to be defined
	protected int typeIndex;
	protected Foci[] foci;
	protected Comet[] comet;
	protected double sumFCintensity = 0;
	protected int numFoci=0;
	protected int numComet=0;	
	private Color pointToNext = null;	
	protected double xCentroid;
	protected double yCentroid;
	protected boolean identifyFC;
	
	protected String fociInfo = "";
	protected String cometInfo = "";
	protected String cellInfo="";
	
	protected int split_current_cell = 0;
	protected int merge_previous_cell = 0;
	protected boolean mergedByOr = false;
	protected boolean manulTrackingCell = false;
	private boolean errorSegmentation = false;
	private double min;
	private double max;
	private double h5;
	private double h25;
	private boolean split;
	private boolean filamentChecked;
	private boolean filamentation;
	private int cellID = 0;
	private Roi[] aggRois;
	private String[][] aggInfo;
	private PointRoi[] pointRois;
	int pR = 0;
	int aR = 0;
	private double meanGFP;
	private double medianGFP;
	private boolean findFoci = false;
	private boolean isFoci = false;
	private Ins_cell[] attachedCell;
	private boolean roiAdded = false;
	private int divideNext = 0;
	public double[] fourierDesc;
	private double medianBG;
	private double medianBGAgg;
	private boolean theEnd = false;

	
	
	public Ins_cell(Roi roi) {
		if(roi == null)
			this.label = "Root Parent";
		else {
			this.label = roi.getName();
			this.slice = roi.getPosition();		
			this.length = roi.getBounds().height;
			this.roi = roi;
			this.meanIntensityArray = new double[1];
			this.areaArray = new double[1];
			this.parent = null;
			
			aggRois = new Roi[10];
			pointRois = new PointRoi[10];
			
			position = new int[]{roi.getBounds().x,roi.getBounds().y};
			
			if(position == null)
				timeIndex = -1;
			else
				timeIndex=Lineage.computeTimeIndex(position[0] + roi.getBounds().width*0.5);
		}
		
	}
	
	public double getArea()
	{
		return areaArray[0];
	}
	public int setAreaToAreaArray(double area)
	{
		if(areaArray == null)
				return -1;
		
		if(area == 0)
			return -1;
		
		for(int i=0; i<areaArray.length;i++)
		{
			if(areaArray[i] != 0)
				continue;
			areaArray[i] = area;
			return i;
		}
		return -1;
	}
	
	public boolean getIfSplit()
	{
		return split;
	}
	
	public void setIntensityToMeanIntensityArray(double intensity, int index)
	{
		if(index < 0)
			return;		
		if(meanIntensityArray == null || index >= meanIntensityArray.length)
				return;
		meanIntensityArray[index] = intensity;
	}
	
	public void setMedianIntensity(double intensity)
	{
		this.medianIntensity = intensity;
	}
	
	public void setArea(double area)
	{
		this.area = area;
	}
	
	public void setMeanIntensity(double intensity)
	{
		this.meanIntensity = intensity;
	}
	
	private void setFociIntensity()
	{
		float sumAreaFC = 0;
		if(foci !=null)
		{
			for(int i=0;i<foci.length;i++)
			{
				if(foci[i]!=null)
				{
					numFoci ++;		
					sumFCintensity = sumFCintensity+foci[i].sumIntensity;
					sumAreaFC = sumAreaFC + foci[i].area;
				}
			}
		}
		
		if(comet!=null)
		{
			for(int i=0;i<comet.length;i++)
			{
				if(comet[i]!=null)
				{
					numComet ++;
					sumFCintensity = sumFCintensity+comet[i].sumIntensity;
					sumAreaFC = sumAreaFC + comet[i].area;
				}
			}
		}
		this.meanIntensityBackground = (sumIntensity - sumFCintensity)>0?(sumIntensity - sumFCintensity)/(area-sumAreaFC):0;
		setType();
	}
	

	public void setAsVirtualCell()
	{
		this.vitrualState = true;
	}
	
	public boolean getDeadState()
	{
		return this.vitrualState;
	}
	/**
	 * 
	 * @param roi
	 * @param xCentroid
	 * @param yCentroid
	 * @param area
	 * @param sumIntensity
	 */
	public Ins_cell(Roi roi,double xCentroid, double yCentroid,double area,double sumIntensity)
	{
		this.roi = roi;
		this.length = roi.getBounds().height;
		this.label = roi.getName();
		this.position = new int[]{roi.getBounds().x,roi.getBounds().y};
		this.xCentroid = xCentroid;
		this.yCentroid = yCentroid;
		this.area = area;
		this.sumIntensity = sumIntensity;
	}
	
	public void copy(Ins_cell cell)
	{
		this.timeIndex = cell.timeIndex;
		this.identifyFC = cell.identifyFC;
		if(identifyFC)
		{			
			Foci[] focis = new Foci[cell.foci.length];			
			for(int i=0;i<foci.length;i++)
			{
				if(foci[i]==null)
					continue;				
				double[] xy= foci[i].center;
				if(roi.contains((int)xy[0],(int)xy[1]))
				{
					focis[i] = new Foci(foci[i]);
				}
			}
			this.foci = focis;
			setFociIntensity();
		}
	}
	
	public void setSkeletonLength(double length)
	{
		this.skeletonLength = length;
	}
	
	
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	private void setType()
	{
		if(numFoci >0 && numComet == 0)
			type = "F";		
		else if(numFoci == 0 && numComet > 0)
			type = "C";	
		else if(numFoci > 0 && numComet > 0)
			type = "FC";
	}
	
	
	public Roi getRoi()
	{
		return roi;
	}
	
	public void setRoi(Roi roi) {
		roi.setPosition(slice);
		roi.setName(label);
		roi.setStrokeColor(roi.getStrokeColor());
		this.length = roi.getBounds().height;
		this.position = new int[]{roi.getBounds().x,roi.getBounds().y};
		this.roi = roi;
	}
	
	public void setCentroid(double xc, double yc)
	{
		this.xCentroid = xc;
		this.yCentroid = yc;
	}
	
	public double getYcenter()
	{
		return roi.getBounds().y + roi.getBounds().height*0.5;
	}
	
	public Arrow getLeftArrow()
	{
		return arrowToTop;
	}
	
	public Arrow getRightArrow()
	{
		return arrowToBottom;
	}
	
	public void setLeftArrow(Arrow toLeft)
	{
		this.arrowToTop = toLeft;
	}
	
	public void setRightArrow(Arrow toRight)
	{
		this.arrowToBottom = toRight;
	}
	
	public void setParentRoi(Ins_cell parent)
	{
		this.parent = parent;
	}
	
	public Ins_cell getParent()
	{
		return parent;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public String getName()
	{
		return name;
	}
	
	public double getEstimatedGrowthRate()
	{
		return eGrowthRate;
	}
	
	public double getPresumedGrowthRate()
	{
		return pGrowthRate;
	}
	
	public void setEstimatedGrowthRate(double growthRate)
	{
		this.eGrowthRate = growthRate;
	}
	
	public void setPresumedGrowthRate(double growthRate)
	{
		this.pGrowthRate = growthRate;
	}
	
	public void setGrowthDiff(double growthDiff)
	{
		this.growthDiff = growthDiff;
	}
	
	public void setGrowthCDiff(double cGrowthDiff)
	{
		this.cGrowthDiff = cGrowthDiff;
	}
	
	public double getGrowthDiff()
	{
		return this.growthDiff;
	}
	
	public double getGrowthCDiff()
	{
		return this.cGrowthDiff;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public int getSlice(){
		return slice;
	}
	
	
//	public void setTimeIndex1(int time){
//		this.timeIndex = time;
//	}
	
	public void setTime(String time){
		this.time = time;
	}
	
	
	public int getTimeIndex()
	{
		return this.timeIndex;
	}
	
	public int[] getPosition(){
		return position;
	}
	
	public void setCellNumber(int num){
		cell_num = num;
	}
	
	public int getCellNumber()
	{
		return cell_num;
	}
	
	/**
	 * Not useful for correction of segmentation
	 * @param lastCell
	 */
	public void setifLastCell(int currentVectorSize) 
	{
		if(currentVectorSize == cell_num)
			ifLastCell = true;
	}
	

	
	public int getLength()
	{
		return length;
	}
	
	
	public void setPointNextColor(Color c)
	{
		this.pointToNext = c;
	}
	
	public Color getPointNextColor()
	{
		return pointToNext;
	}
	
	public String toString() {
		int last = 0;
		if(ifLastCell)
			last = 1;
		cellInfo = label+"\t"+
				last+"\t"+
				name+"\t" +
				timeIndex+"\t"+
				time+"\t"+
				cell_num+"\t"+
				length+"\t"+
				skeletonLength+"\t"+
				sumIntensity + "\t"+
				meanIntensityBackground  + "\t"+
				area + "\t"+
				xCentroid + "\t"+
				yCentroid + "\t"+
				type+ "\t" + 
				(ifdevidedFromPreviousCell ? "1":"0")+ "\t";
		
			if(foci != null)
			for(int i=0;i<foci.length;i++)
			{
				if(foci[i]!=null)
				{					
					fociInfo = fociInfo+cellInfo+"F" +"\t"
								+ String.valueOf(numFoci)+ "\t"
								+ String.valueOf(numComet)+ "\t"
								+ foci[i].label + "\t"
								+ String.valueOf(foci[i].area)+ "\t" 
								+ String.valueOf(foci[i].meanIntensity)+ "\t" 
								+ String.valueOf(foci[i].radius)+ "\t"
								+ String.valueOf(foci[i].center[0]) + "\t"
								+ String.valueOf(foci[i].center[1]) + "\t"
								+ String.valueOf(foci[i].reletiveCenterToCell[0])+"\t"
								+ String.valueOf(foci[i].reletiveCenterToCell[1])+ "\t"+ "\r"+"\n";	
				}
			}

			if(comet != null)
			for(int i=0;i<comet.length;i++)
			{
				if(comet[i]!=null)
				{
					cometInfo = cometInfo+cellInfo+"C" + "\t"
								+ String.valueOf(numFoci)+ "\t"
								+ String.valueOf(numComet)+"\t"
								+ comet[i].label+"\t"
								+ String.valueOf(comet[i].area)+ "\t" 
								+ String.valueOf(comet[i].meanIntensity)+ "\t" 
								+ String.valueOf(comet[i].length)+ "\t"
								+ String.valueOf(comet[i].center[0]) + "\t"
								+ String.valueOf(comet[i].center[1]) + "\t"
								+ String.valueOf(comet[i].reletiveCenterToCell[0]) + "\t"
								+ String.valueOf(comet[i].reletiveCenterToCell[1])+"\t"+ "\r"+"\n";
				}
			}		
		
		if(type.equals("NO"))
			return cellInfo+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"NA"+"\t"+"\r"+"\n";
		else
			return fociInfo + cometInfo;
	}
	
	public String toString2() {
		int last = 0;
		if(ifLastCell)
			last = 1;
		return (label+"\t"+
				last+"\t"+
				name+"\t" +
				timeIndex+"\t"+
				time+"\t"+				
				cell_num+"\t"+
				length+"\t"+
				xCentroid + "\t"+
				yCentroid + "\t"+
				cellID + 
				"\n");
	}
	
	public String toStringYifan() {
		int last = 0;
		if(ifLastCell)
			last = 1;

		String areaMeanintensity = "";
		if(areaArray!=null)			
		for(int i=0; i<areaArray.length; i++)
		{
			areaMeanintensity = areaMeanintensity +areaArray[i]+"\t"+ meanIntensityArray[i] + "\t";
		}
		
//		if(filamentation)
//			System.out.println("filamentation detected");
		
		int filam = filamentation?1:0;
		
		return (label+"\t"+
				last+"\t"+
				name+"\t" +
				timeIndex+"\t"+
//				time+"\t"+				
				cell_num+"\t"+
//				deadCell+"\t"+
				length+"\t"+
				xCentroid + "\t"+
				yCentroid + "\t"+
				area + "\t" + 
				meanIntensity + "\t"+
				medianIntensity+ "\t"+
				min + "\t"+
				max + "\t"+
				h5  + "\t"+
				h25 +
				"\n");
	}
	
	public String toStringAntoine() {
		int last = 0;
		if(ifLastCell)
			last = 1;

		String areaMeanintensity = "";
		if(areaArray!=null)			
		for(int i=0; i<areaArray.length; i++)
		{
			areaMeanintensity = areaMeanintensity +areaArray[i]+"\t"+ meanIntensityArray[i] + "\t";
		}
		
//		if(filamentation)
//			System.out.println("filamentation detected");
		
		int filam = filamentation?1:0;
		
		cellInfo = label+"\t"+
				last+"\t"+
				name+"\t" +
				timeIndex+"\t"+
				cell_num+"\t"+
				length+"\t"+
				xCentroid + "\t"+
				yCentroid + "\t"+
				area + "\t" + 
				meanIntensity + "\t"+
				medianIntensity+ "\t"+
				min + "\t"+
				max + "\t"+
				h5  + "\t"+
				h25 + "\t"+
				filam + "\t"+
				cellID + "\t"+
				divideNext + "\t" + 
				medianBG + "\t"+
				meanGFP + "\t" + 
				medianGFP + "\t" + 
				medianBGAgg + "\t";
		
		
			String aggString = "";
			if(aggInfo!=null)
			{			
				for(int i=0;i<aggInfo.length;i++)
				{
					if(aggInfo[i]!=null)
					{					
						aggString = aggString+cellInfo+aggInfo.length+"\t"
								+ String.valueOf(aggInfo[i][0])+"\t"
								+ String.valueOf(aggInfo[i][1])+"\t"
								+ String.valueOf(aggInfo[i][2])+"\t"
								+ String.valueOf(aggInfo[i][3])+"\t"
								+ String.valueOf(aggInfo[i][4])+"\t" + "\r"+"\n";	
					}
				}
			}else {
				aggString = aggString+cellInfo+String.valueOf(0)+"\t"
						+ String.valueOf(0)+"\t"
						+ String.valueOf(0)+"\t"
						+ String.valueOf(0)+"\t"
						+ String.valueOf(0)+"\t"
						+ String.valueOf(0)+"\t" + "\r"+"\n";	
			}
			if(aggString!="")
				return aggString;
			else {
				return cellInfo + "\r" + "\n";
			}
	}

	@Override
	public int compareTo(Object o) {
		if (slice < ((Ins_cell) o).slice) {
			return -1;
		}else if (slice > ((Ins_cell) o).slice) {
			return 1;
		}else {
			if (timeIndex < ((Ins_cell) o).timeIndex) {
				return -1;
			}else if (timeIndex > ((Ins_cell) o).timeIndex) {
				return 1;
			}else {
				if(position[1]+length/2 <((Ins_cell) o).position[1]+ ((Ins_cell) o).length/2){
					return -1;
				}else if(position[1]+length/2 >((Ins_cell) o).position[1]+ ((Ins_cell) o).length/2){
					return 1;
				}else {
					if(position[0] < ((Ins_cell) o).position[0])
						return -1;
					else {
						return 1;	
					}		
				}
			} 
		}
	}

	public void setError() {
		errorSegmentation = true;
	}
	
	public boolean getError(){
		return errorSegmentation;
	}

	public boolean mergeCell3(Ins_cell cell2, int openNumber, String name,Ins_cellsVector[] cellsTimeIndex) {
		if(cell2==null || cell2.getRoi()==null)
			return false;		
		ImageProcessor ip1 = roi.getMask();
		ImageProcessor ip2 = cell2.getRoi().getMask();				
		int x1 = getPosition()[0];
		int y1 = getPosition()[1];
		int width1 = ip1.getWidth();
		int height1 = ip1.getHeight();
		
		int x2 = cell2.getPosition()[0];
		int y2 = cell2.getPosition()[1];
		int width2 = ip2.getWidth();
		int height2 = ip2.getHeight();
		
		int enlarge = 30;
		int width = Math.max(x1+width1, x2+width2) - Math.min(x1, x2);		
		width = width + enlarge;			
		int height = Math.max(y1+height1, y2+height2) - Math.min(y1, y2);
		height = height + enlarge;
		
		ImagePlus imp = IJ.createImage(getLabel(), "8-bit black", width, height, 1);		
		ImageProcessor ip = imp.getProcessor();
		
		int xloc1 = x1-x2>0?x1-x2:0;
		int yloc1 = y1-y2>0?y1-y2:0;
		ip.insert(ip1, xloc1+enlarge/2, yloc1+enlarge/2);

		int xloc2 = x2-x1>0?x2-x1:0;
		int yloc2 = y2-y1>0?y2-y1:0;
		ip.insert(ip2, xloc2+enlarge/2, yloc2+enlarge/2);		
		ip.invert();
		//imp.duplicate().show();
		// first open process		
		ip.erode();
		ip.dilate();
		
		for(int i=0;i<openNumber;i++)
		{
			ip.dilate();			
		}		
		for(int i=0;i<openNumber;i++)
		{
			ip.erode();			
		}
		//imp.show();
		//merging finished, change lineage map
		
		for(int i=enlarge/2;i<ip.getWidth();i++)
			for(int j=enlarge/2;j<ip.getHeight();j++)
			{
				if(ip.get(i, j)==0)
				{
					xloc1 = i;
					yloc1 = j;
					break;
				}
			}
		Wand wand = new Wand(ip);
		wand.autoOutline(xloc1, yloc1);
		int x_insert = x1<x2?x1:x2;
		int y_insert = y1<y2?y1:y2;		
		int nPoints = wand.npoints;
		int[] X = new int[nPoints];
		int[] Y = new int[nPoints];
		for(int i=0;i<nPoints;i++)
		{
			X[i] = wand.xpoints[i]+x_insert - enlarge/2;
			Y[i] = wand.ypoints[i]+y_insert - enlarge/2;
		}		
		try {
			PolygonRoi roi_merge = new PolygonRoi(X, Y, nPoints, roi.getType());		
			roi_merge.setPosition(this.slice);
			roi_merge.setStrokeColor(new Color(0,255,0));
			this.setRoi(roi_merge);		
			Ins_cellsVector currentVector = cellsTimeIndex[getTimeIndex()];
			Vector<Ins_cell> cVector = currentVector.getCellVector();
			cVector.remove(cell2);			
/*			this.previousCell.nextTopCell = null;
			this.previousCell.nextBottomCell = null;
			this.previousCell.nextCell = this;*/
			setName(name);
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	public boolean mergeCell(Ins_cell cell2,Ins_cellsVector[] cellsTimeIndex,Color color) {
		if(cell2==null || cell2.getRoi()==null)
			return false;
		Roi nRoi = new Roi(roi.getBounds().x, roi.getBounds().y, roi.getBounds().width, cell2.getRoi().getBounds().y + cell2.getRoi().getBounds().height - roi.getBounds().y);
		nRoi.setPosition(this.slice);
		nRoi.setStrokeColor(color);			
		nRoi.setName(label);
		this.setRoi(nRoi);
		Ins_cellsVector currentVector = cellsTimeIndex[getTimeIndex()];
		Vector<Ins_cell> cVector = currentVector.getCellVector();
		cVector.remove(cell2);
		return true;
	}
	
//	public boolean mergeCell(Ins_cell cell2, String name,Ins_cellsVector[] cellsTimeIndex,ImageProcessor ip) {
//		if(cell2==null || cell2.getRoi()==null)
//			return false;
//		ShapeRoi s1 = new ShapeRoi(roi);
//		ShapeRoi s2 = new ShapeRoi(cell2.roi);
//		s1.or(s2);
//		Roi s3 = Ins_seg_panel.refineRoi(s1, ip, s1.getBounds().x, false,false);
//		if(s3 == null)
//			s3 = s1;
//		System.out.println("merge roi and refinement");
//		s3.setPosition(this.slice);
//		s3.setStrokeColor(new Color(0,255,0));			
//		this.setRoi(s3);
//		Ins_cellsVector currentVector = cellsTimeIndex[getTimeIndex()];
//		Vector<Ins_cell> cVector = currentVector.getCellVector();
//		cVector.remove(cell2);			
//		/*			this.previousCell.nextTopCell = null;
//			this.previousCell.nextBottomCell = null;
//			this.previousCell.nextCell = this;*/
//		setName(name);			
//		roi1 = (Roi)roi.clone();
//		roi1.setName("split r1");
//		roi2 = (Roi)cell2.getRoi().clone();
//		roi2.setName("split r2");
//		mergedByOr = true;						
//		return true;
//		/*} catch (Exception e) {
//			return false;
//		}*/
//
//	}
	
	public void divideCell(ImageProcessor ip,Ins_cellsVector[] cellsTimeIndex)
	{
		BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
		bSubtracter.rollingBallBackground(ip, 40, false, false, false, false, false);
		ImagePlus[] eigenImp = (new featureJ.FJ_Structure()).getEigenImg(new ImagePlus("", ip),"1.0","3.0");
		ImagePlus impPlotProfile = eigenImp[1]; //smallest Eigen Image
		impPlotProfile.getProcessor().convertToByte(true);
		double ratio = 0.3;
		int roi_width = this.getRoi().getBounds().width;
		int height = this.getRoi().getBounds().height;		
		double[] profile = Lineage.getRowMedianProfile(new Rectangle((int)Math.ceil(roi_width*(1-ratio)*0.5), 0, (int)(roi_width*ratio), height), impPlotProfile.getProcessor());
		double[] profile1 = new double[profile.length];
		for(int p=0; p<profile.length; p++)
		{
			double sum = 0;
			for(int q=p-2; q<p+3; q++)
			{
				if(q<0 || q>=profile.length)
					continue;
				sum = sum + profile[q];
			}
			profile1[p] = sum;
		}
		profile = profile1;
		int tolerance = 135;
		int minPeakDist = 12;		
		int[] position = null;
		Ins_cellsVector currentVector = cellsTimeIndex[getTimeIndex()];
		while (tolerance > 0) {
			Ins_find_peaks peakFinder = new Ins_find_peaks(tolerance, minPeakDist);				
			Object[] out = peakFinder.findPeaks(profile, true, 6);
			position = (int[])out[0]; 
			Arrays.sort(position);
			int[] newPos = new int[position.length];
			int t=0;
			for(int j=0;j<position.length;j++)
			{
				if(position[j] < minPeakDist || position[j] > profile.length - minPeakDist) //if(position[j] > profile.length - minPeakDist)
					continue;
				newPos[t++] = position[j];
			}
			int[] newPos2 = new int[t];
			System.arraycopy(newPos, 0, newPos2, 0, t);		
			position = newPos2;				
			if(position.length == 0)
			{
				tolerance = tolerance - 1;
			}else{
				if (position.length == 1)
					position = new int[]{position[0], height};
				else {
					int[] position2 = new int[position.length+1];
					System.arraycopy(position, 0, position2, 0, position.length);
					position2[position2.length-1] = height;
					position = position2;
				}
				
				for(int j=0;j < position.length;j++)
				{						
					Roi roi2;
					int h = 0;
					if(j==0)
					{
						roi2 = new Roi(roi.getBounds().x, roi.getBounds().y, roi_width, position[j]);
						roi2.setPosition(roi.getPosition());
						roi2.setStrokeColor(roi.getStrokeColor());
						roi2.setName(roi.getName());
						this.length = roi2.getBounds().height;
						this.roi = roi2;
					}else {
						h = position[j] - position[j-1];
						roi2 = new Roi(roi.getBounds().x, roi.getBounds().y + position[j-1], roi_width, h);
						roi2.setPosition(roi.getPosition());
						roi2.setStrokeColor(roi.getStrokeColor());
						currentVector.insertCell(new Ins_cell(roi2));
					}
				}
				break;
			}
		}
	}
	
	public void splitCell1(double ratio, Ins_cellsVector[] cellsTimeIndex)
	{
		if(!mergedByOr)
		{
			ByteProcessor ip;		
			try {
				if(roi instanceof PolygonRoi)
					ip = (ByteProcessor)((PolygonRoi)roi).getMask();
				else if (roi instanceof ShapeRoi) {
					ip = (ByteProcessor)((ShapeRoi)roi).getMask();
				}else if (roi.getType() == Roi.RECTANGLE ) {					
					Roi roi2 = new PolygonRoi(roi.getPolygon(), Roi.POLYGON);
					ip = (ByteProcessor)((PolygonRoi)roi2).getMask();
				}else {
					return;
				}
			} catch (Exception e) {
				System.out.println("label : " + getLabel());
				return;
			}
			//ip = (ByteProcessor)roi.getMask();		
			int cutY = (int)(length*ratio);
			for(int i=0;i<ip.getWidth();i++)
			{
				ip.set(i, cutY, 0);
			}
			ip.invert();
			Wand wand_upper = new Wand(ip);
			Wand wand_lower = new Wand(ip);
			wand_upper.autoOutline(ip.getWidth()/2,(int)(cutY/2));
			wand_lower.autoOutline(ip.getWidth()/2,(int)(cutY+(length-cutY)*0.5));		
			int nPointsUpper = wand_upper.npoints;
			int[] upperX = new int[nPointsUpper];
			int[] upperY = new int[nPointsUpper];
			for(int i=0;i<nPointsUpper;i++)
			{
				upperX[i] = wand_upper.xpoints[i]+position[0];
				upperY[i] = wand_upper.ypoints[i]+position[1];
			}
			int nPointsLower = wand_lower.npoints;
			int[] lowerX = new int[nPointsLower];
			int[] lowerY = new int[nPointsLower];
			for(int i=0;i<nPointsLower;i++)
			{
				lowerX[i] = wand_lower.xpoints[i]+position[0];
				lowerY[i] = wand_lower.ypoints[i]+position[1];
			}
			PolygonRoi roi_upper = new PolygonRoi(upperX, upperY, nPointsUpper, Roi.POLYGON);	
			PolygonRoi roi_lower = new PolygonRoi(lowerX, lowerY, nPointsLower, Roi.POLYGON);		
			int pos = roi.getPosition();		
			roi_upper.setPosition(pos);
			roi_lower.setPosition(pos);	
			roi_upper.setStrokeColor(new Color(255,72,170));
			roi_lower.setStrokeColor(new Color(255,72,170));
			this.setRoi(roi_upper);
			Ins_cell lowerCell = new Ins_cell(roi_lower);
			Ins_cellsVector currentCellsVector = cellsTimeIndex[getTimeIndex()];
			currentCellsVector.insertCell(lowerCell);		
		}else {
			if(roi1!=null)
				this.setRoi(roi1);
			if(roi2!=null)
			{
				Ins_cell lowerCell = new Ins_cell(roi2);
				Ins_cellsVector currentCellsVector = cellsTimeIndex[getTimeIndex()];
				currentCellsVector.insertCell(lowerCell);
			}
			System.out.println("Split merged ShapeRoi cells : " + roi1.getName() + "-"+roi2.getName());
		}
		this.split = true;
		mergedByOr = false;
	}
	
	public void changeRoiSlice(int slice)
	{
		this.roi.setPosition(slice);
		this.roi.setStrokeColor(new Color(255, 0, 255));
		this.slice = roi.getPosition();
	}

	public void setMinIntensity(double min) {
		this.min = min;
	}

	public void setMaxIntensity(double max) {
		this.max = max;
	}
	
	public void setBGMedian(double medianBG){
		this.medianBG = medianBG;
	}
	
	public void setBGAggMedian(double medianBGAgg){
		this.medianBGAgg = medianBGAgg;
	}

	public void setHist5(double h5) {
		// TODO Auto-generated method stub
		this.h5 = h5;
	}

	public void setHist25(double h25) {
		// TODO Auto-generated method stub
		this.h25 = h25;
	}

	public boolean filamentChecked() {
		return filamentChecked;
	}
	
	public void setFilamentState(boolean filament, Color color)
	{
		filamentation = filament;
		filamentChecked = true;		
		if(filamentation)
			setColor(color);
		else {
			setColor(new Color(255, 255, 0));
		}
	}
	
	public void setColor(Color color)
	{
		roi.setStrokeColor(color);
	}
	
	public boolean getFilamentState()
	{
		return filamentation;
	}

	public boolean regrouped() {
		if(cellID != 0)
			return true;
		else {
			return false;
		}
	}

	public void setCellID(int i) {
		this.cellID = i;
	}
	
	public int getCellID(){
		return this.cellID;
	}

	public void addAggRoi(Roi aAggRoi) {
		if(aAggRoi instanceof PointRoi)
		{
			for(int i=0; i<pR;i++)
			{
				PointRoi p = pointRois[i];
				if(p.getBounds().x == aAggRoi.getBounds().x && p.getBounds().y == aAggRoi.getBounds().y)
					return;
			}			
			if(pR<pointRois.length)
			{
				pointRois[pR] = (PointRoi)aAggRoi;
				pR++;
			}
		}
		else {
			if(pR<pointRois.length)
			{
				aggRois[aR] = aAggRoi;
				aR++;
			}
		}		
	}
	
	public void initializeAggRoi() {
		pR = 0;
		pointRois = new PointRoi[10];
	}

	public PointRoi getAvPr() {
		if(pR <= 0) return null;
		
		double x = 0;
		double y = 0;
		for(int i=0; i<pR;i++)
		{
			PointRoi p = pointRois[i];
			x = x + p.getBounds().x;
			y = y + p.getBounds().y;
		}
		PointRoi pRoi = new PointRoi(x/pR, y/pR);
		pRoi.setPosition(pointRois[0].getPosition());
		return pRoi;
	}
	
	public PointRoi[] getPointRois()
	{
		if(pR == 0)
			return null;
		PointRoi[] pRois = new PointRoi[pR];
		System.arraycopy(pointRois, 0, pRois, 0, pR);
		return pRois;
	}
	
	public Roi[] getAggRoisRegion1()
	{
		if(aR == 0)
			return null;
		Roi[] pRois = new Roi[aR];
		System.arraycopy(aggRois, 0, pRois, 0, aR);
		aggInfo = new String[aR][5];
		return pRois;
	}
	
	public PointRoi[] getAggRoisPoint()
	{
		if(pR == 0)
			return null;
		PointRoi[] pRois = new PointRoi[pR];
		System.arraycopy(pointRois, 0, pRois, 0, pR);
		return pRois;
	}
	public void createAggregateInfo() {
		aggInfo = new String[pR][5];
	}
	public void setAggregateInfo(double x, double y, double area2, double mean, double median,int i) {
		aggInfo[i][0] = String.valueOf(x);
		aggInfo[i][1] = String.valueOf(y);
		aggInfo[i][2] = String.valueOf(area2);
		aggInfo[i][3] = String.valueOf(mean);
		aggInfo[i][4] = String.valueOf(median);
	}

	public void setMeanGFP(double mean) {
		this.meanGFP = mean;
	}
	
	public void setMedianGFP(double median) {
		this.medianGFP = median;
	}


	// line to rectangular roi
	public void toRectangular() {
//		if(roi instanceof Line)
//		{
//			Line line = (Line)roi;
//			Roi nRoi = new Roi(timeIndex*(Lineage.roi_width+6), line.y1<line.y2?line.y1:line.y2, Lineage.roi_width, Math.abs(line.y2-line.y1));
//			this.setRoi(nRoi);
//		}
		
		if(roi.getType() == Roi.FREELINE)
		{
			PolygonRoi  fLine = (PolygonRoi) roi;
			Roi nRoi = new Roi(timeIndex*(Lineage.roi_width+6), fLine.getBounds().y, Lineage.roi_width, fLine.getBounds().height);
			this.setRoi(nRoi);
		}
	}
	
	public void toRectangular(int x, int width) {
		if(roi.getType() == Roi.FREELINE)
		{
			PolygonRoi  fLine = (PolygonRoi) roi;
			Roi nRoi = new Roi(x, fLine.getBounds().y, width, fLine.getBounds().height);
			this.setRoi(nRoi);
		}
	}

	public void resizeHeadToZero() {
		Roi nRoi = new Roi(timeIndex*(Lineage.roi_width+6), 0, Lineage.roi_width, roi.getBounds().y + roi.getBounds().height);
		this.setRoi(nRoi);

	}

	public void setFociState(boolean state) {
		this.findFoci  = state;
	}
	
	public boolean getFociState()
	{
		return findFoci;
	}

	public void setFociProperty(boolean b) 
	{
		this.isFoci  = b;
	}
	
	public boolean isFoci()
	{
		return isFoci;
	}

	public void addAttachedCell(Ins_cell actualCell) 
	{
		if(attachedCell == null)
		{
			attachedCell = new Ins_cell[2];
			attachedCell[0] = actualCell;
			return;
		}
		attachedCell[1] = actualCell;
	}
	
	public void addCorrectedAttachedCell(Ins_cell aCell)
	{
		attachedCell = new Ins_cell[2];
		attachedCell[0] = aCell;
	}
	
	public Ins_cell[] getAttachedCell()
	{
		return attachedCell;
	}

	public boolean fociIsadded() {
		return roiAdded ;
	}
	
	public void setFociAdded(boolean b){
		roiAdded = b;
	}

	public void setDivideNext(int divide) {
		divideNext  = divide;
	}
	
	class Foci{
		float area = 0;	
		float sumIntensity = 0;
		double meanIntensity = 0;	
		double[] center = new double[2];
		double[] reletiveCenterToCell = new double[2];
		double radius;
		String label;
		
		public Foci(Foci foci)
		{
			this.area = foci.area;
			this.sumIntensity = foci.sumIntensity;
			this.meanIntensity = foci.meanIntensity;
			this.center = foci.center;
			this.reletiveCenterToCell = foci.reletiveCenterToCell;
			this.radius = foci.radius;
			this.label = foci.label;
		}
		public Foci()
		{
			
		}
		
	}

	class Comet{
		float area = 0;
		float sumIntensity = 0;
		double meanIntensity = 0;
		double[] center;
		double[] reletiveCenterToCell;
		double length;
		String label;
		
		public Comet(Comet comet)
		{
			this.area = comet.area;
			this.sumIntensity = comet.sumIntensity;
			this.meanIntensity = comet.meanIntensity;
			this.center = comet.center;
			this.reletiveCenterToCell = comet.reletiveCenterToCell;
			this.length = comet.length;
			this.label = comet.label;
		}
		public Comet()
		{
			
		}
	}

	public double[] getFourierDescriptor() {
		return fourierDesc;
	}

	public void setFourierDescriptor(double[] fourierDescriptor) {
		this.fourierDesc = fourierDescriptor;		
	}

	public void setEndFlag(boolean b) {
		this.theEnd = b;
	}
	
	public boolean getEndFlag()
	{
		return theEnd;
	}
	


	





//	public void setBackGroundIntensity(double intensity) {
//		this.bgIntensity = intensity;
//	}
	
}
