package lineage;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Arrow;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.io.DirectoryChooser;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import psf.PointSourceDetection;
import Editor.Ins_editor;
import cellst.Image.FftBandPassFilter;
import cellst.Image.ImageFbt;
import cellst.Main.Fluo_Bac_Tracker;


public class Lineage implements PlugIn {
	public ImagePlus imp;
	private ImagePlus impRef; // the original seeds image for correcting the segmentation error.
	boolean selfCorrection = true;
	public static int roi_width;
	public int stackSize;
	public int widthImp;
	public static int blankwidth = 6; 
	public boolean writeResultTable = false;
	public double filamentRatio = 2.0d;
	public int channelNumber = 1;
	public boolean outputNewBinary = false;	
	public Ins_cellsVector[][] cellsTimeIndex;
	public static int minPeakDist = 10;
	public Color filamentColor = new Color(159, 0, 255, 200);
	public boolean allCellsInFirstChannel = false;
	private int cell_counter;
	private ArrayList<Ins_cell> cells;
	private boolean addVitrualCell;
	private boolean mergeFirst;
	private int iterTotal;
	private String pathRT;
	private String pathRoi;
	private boolean detectFoci;
	private boolean evaluation;
	private ResultsTable resultsTable;
	private boolean computeDesc;
	private boolean correctSegBySeeds;
	private boolean correctSegBySPF;
	private boolean noLineageAfterCorrection;
	private boolean mergeCells;
	public static int nFD = 10;
	private double sigma = 2.8;
	
	public Lineage(ImagePlus imp)
	{
		this.imp = imp;		
		try {			
			String impName = imp.getTitle();
			String[] name = impName.split("-ss-");				
			name = name[1].split("-roi-");
			stackSize = Integer.valueOf(name[0]);		
			name = impName.split("-roi-");
			name = name[1].split(".tif");
			roi_width = Integer.valueOf(name[0]);
			widthImp = imp.getWidth();
		} catch (ArrayIndexOutOfBoundsException e2) {
			IJ.showMessage("Image name doesn't include necessary information! (ss-?-roi-?)");					
		}
		GenericDialog genericDialog = new GenericDialog("Information");
		if(stackSize<=0)
			genericDialog.addNumericField("Stack size : ", WindowManager.getCurrentImage().getWidth()/(roi_width+6), 0);
		else {
			genericDialog.addNumericField("Stack size : ", stackSize, 0);
		}
		int interval = 1;
		String title = imp.getTitle().toLowerCase();
		if(title.indexOf("triple")!=-1)
			interval = 3;
		else if (title.indexOf("double")!=-1) {
			interval = 2;
		}else {
			interval = 1;
		}
		channelNumber = imp.getImageStackSize()/interval;
		genericDialog.addNumericField("Roi width : ", roi_width, 0);
		genericDialog.addNumericField("Number of slices for lineage", channelNumber, 0);
		genericDialog.addCheckbox("Detect aggregation", detectFoci);
		genericDialog.addNumericField("Filament ratio to normal", filamentRatio,2);
		genericDialog.addCheckbox("Consider all cells in the first channel", allCellsInFirstChannel);
		genericDialog.addCheckbox("Save Result Table", writeResultTable);
		genericDialog.addCheckbox("Add virtual cell", addVitrualCell);
		genericDialog.addCheckbox("Merge first", mergeFirst);
		genericDialog.addNumericField("Iteration steps", iterTotal, 0);
		genericDialog.showDialog();
		if (genericDialog.wasCanceled()) return;
		stackSize = (int)genericDialog.getNextNumber();
		roi_width = (int)genericDialog.getNextNumber();	
		channelNumber = (int)genericDialog.getNextNumber();
		detectFoci = genericDialog.getNextBoolean();
		filamentRatio = genericDialog.getNextNumber();
		allCellsInFirstChannel = genericDialog.getNextBoolean();
		writeResultTable = genericDialog.getNextBoolean();
		addVitrualCell = genericDialog.getNextBoolean();
		mergeFirst = genericDialog.getNextBoolean();
		iterTotal = (int)genericDialog.getNextNumber();		
		this.cell_counter = 0;
		this.cells = new ArrayList<Ins_cell>();
	}
	
	public Lineage(ImagePlus imp, int stackSize, int channelNumber, int roi_width, double sigma, double filamentRatio, boolean allCellsInFirstChannel,boolean writeResultTable, boolean addVitrualCell, boolean mergeFirst, int iterTotal, String path ,boolean detectFoci,boolean evaluation, boolean desc, boolean correctSeg,boolean correctSegBySPF, boolean noLineageAfterCorrection,boolean mergeCells) {
		this.imp = imp;
		this.channelNumber = channelNumber;
		this.widthImp = imp.getWidth();
		this.sigma = sigma;
		this.stackSize = stackSize;
		Lineage.roi_width = roi_width;
		this.filamentRatio = filamentRatio;
		this.outputNewBinary = false;
		this.allCellsInFirstChannel = allCellsInFirstChannel;
		this.writeResultTable = writeResultTable;
		this.addVitrualCell = addVitrualCell;
		this.mergeFirst = mergeFirst;
		this.iterTotal = iterTotal;
		this.evaluation = evaluation;
		resultsTable = new ResultsTable();
		if(path != null)
		{
			this.pathRT = path + File.separator + "RT" + File.separator;			
			File rtFolder = new File(pathRT);
			rtFolder.mkdirs();
			this.pathRoi = path + File.separator + imp.getTitle() + ".zip";
		}else {
			this.pathRT = null;
			this.pathRoi = null;
		}
		this.detectFoci = detectFoci;
		this.cell_counter = 0;
		this.cells = new ArrayList<Ins_cell>();
		this.computeDesc = desc;
		this.correctSegBySeeds = correctSeg;
		this.correctSegBySPF = correctSegBySPF;
		this.noLineageAfterCorrection = noLineageAfterCorrection;
		this.mergeCells = mergeCells;
		if(correctSegBySPF || correctSegBySeeds)
		{
			GenericDialog gDialog = new GenericDialog("Select image");
			gDialog.addCheckbox("The next slice is the same image", selfCorrection);
			gDialog.showDialog();
			if(gDialog.wasCanceled())
			{
				selfCorrection = true;
				return;
			}
			selfCorrection = gDialog.getNextBoolean();
			if(!selfCorrection)
				this.impRef = IJ.openImage();
		}
	}

	@Override
	public void run(String arg) {
		Roi[][] r = writeCellstoRT();		
		if(r==null)
			return;
		Roi[][] roiToManager = new Roi[1][];				
		roiToManager[0] = getRoiFromManager();
		if(pathRoi!=null)
		{
			saveRoiToZip(roiToManager, pathRoi);				
			System.out.println("Save roi to ->"+ pathRoi);
		}
		System.out.println("lineage done !");
		IJ.log("lineage done !");
	}
	
	public Ins_cellsVector[][] getCellsVector()
	{
		return cellsTimeIndex;
	}
	
	
	public Roi[][] writeCellstoRT()
	{
		Roi[] roisArrayRois = getRoiManager().getRoisAsArray();
		int count = roisArrayRois.length;
		if(count == 0)
		{
			return null;
		}
		return lineage(false, roisArrayRois, filamentRatio,imp);		
	}

	/**
	 * 
	 * @param writeToResult write result table for all binary channels, not work for open channel
	 * @return
	 */
	public Roi[][] lineage(boolean openchannel, Roi[] roiArray, double filamentRatio,ImagePlus cImp)
	{		
		String outputDirectoryPath = "./";
		if(writeResultTable && pathRT == null)
		{
			DirectoryChooser directoryChooser = new DirectoryChooser("Select output folder");
			outputDirectoryPath = directoryChooser.getDirectory();
			if(outputDirectoryPath == null)
			{
				outputDirectoryPath = WindowManager.getCurrentImage().getOriginalFileInfo().directory;
				IJ.showMessage("No output folder selected, result table will be saved in " + WindowManager.getCurrentImage().getOriginalFileInfo().directory);
			}
			System.out.println("output folder : " + outputDirectoryPath);
		}else {
			outputDirectoryPath = pathRT;
		}
		
		int stackVolume = cImp.getStackSize();
		cellsTimeIndex = new Ins_cellsVector[stackVolume][];
		int increment = stackVolume/channelNumber;
		
		if(evaluation)
		{
			resultsTable.incrementCounter();
			resultsTable.addLabel("Slice", "Number of cells");
		}
		
		Roi[][] roisArrayToManager = new Roi[stackVolume][]; 
		ImagePlus impFFT=null;
		if(writeResultTable)
		{	
			impFFT = fftFromOriginal(imp);
			IJ.save(impFFT, outputDirectoryPath+"fft-"+imp.getShortTitle()+".tif");
			
		}
		for(int slice = 1;slice <= stackVolume; slice += increment)//stackVolumeqsdfqsdf
		{
			System.out.println("lineage on slice : " + slice+ " / " + stackVolume + " cell_counter : " + cell_counter);
			cell_counter += doLineageAndCorrect(slice,roiArray, roisArrayToManager, cImp, outputDirectoryPath,increment,impFFT);			
		}

		long startTime = System.nanoTime();
		deleteRoiToManager();
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1000000000.0;
		
		startTime = System.nanoTime();
		System.out.println("delete all rois using time : " + duration + " s");
		getRoiManager().setEditMode(cImp, false);
		for(int i=0; i<roisArrayToManager.length;i++)
		{
			Roi[] roisSlice = roisArrayToManager[i];			
			if(roisSlice==null || roisSlice.length==0)
				continue;						
			for(int j=0; j<roisSlice.length;j++)
			{
				if(roisSlice[j] instanceof PointRoi)
					roisSlice[j].setName(getLabelPoint(roisSlice[j]));
				getRoiManager().addRoi(roisSlice[j]);								
			}
		}
		endTime = System.nanoTime();
		duration = (endTime - startTime)/1000000000.0;
		System.out.println("add all rois using time : " + duration + " s");		
		return roisArrayToManager;
	}
	
	private int doLineageAndCorrect(int slice, Roi[] roiArray,Roi[][] roisArrayToManager, ImagePlus imp, String outputDirectoryPath, int increment, ImagePlus impFFT)
	{
		boolean detectPhilament = false;
		if(filamentRatio >= 1.0)
		{
			detectPhilament = true;
		}
		boolean identifyColor = false;
		Roi[] roisSlice1 = constructRoisSlice(roiArray, slice);
		Ins_cellsVector cellsTimeIndex1[] = null;
		if(roisSlice1!=null)
		{
			Ins_cell[] cells1 = constructCellsListFromStack(roisSlice1, identifyColor);
			if(cells1 != null)
			{
				Arrays.sort(cells1);
				setCellsNumber(cells1);
				cellsTimeIndex1 = constructCellVectors(stackSize,cells1);	
			}else {
				return 0;
			}
		}else {
			return 0;
		}
		cellsTimeIndex[slice-1] = cellsTimeIndex1;

		//the wrong cell Roi can contain one added Line roi, then this cell Roi will be replaced by the added Line Roi
		manualCorrection(cellsTimeIndex[slice-1]);

		if(correctSegBySeeds)
		{							
			correctSegPSF(cellsTimeIndex[slice-1], imp,slice);
		}else if(correctSegBySPF){	
			correctSegPSFOnly(cellsTimeIndex[slice-1], imp,slice);
		}
		
		if(increment >= 2 && mergeCells)
		{
			System.out.println("Enter into merge cells process");
			Roi[] roisSlice2 = constructRoisSlice(roiArray, slice+1);
			Ins_cellsVector cellsTimeIndex2[] = null;
			if(roisSlice2!=null)
			{
				Ins_cell[] cells2 = constructCellsListFromStack(roisSlice2, identifyColor);
				if(cells2 != null)
				{
					Arrays.sort(cells2);
					setCellsNumber(cells2);
					cellsTimeIndex2 = constructCellVectors(stackSize,cells2);	
					manualCorrection(cellsTimeIndex2);
				}
			}
			Roi[][] roiCombine = combineCellsTimeIndexToRois(cellsTimeIndex1, cellsTimeIndex2, slice);
			roisArrayToManager[slice-1] = roiCombine[0];
			roisArrayToManager[slice] = roiCombine[1];
			return 0;
		}else if (mergeCells) {
			System.out.println("		Not possible to merge cell, because the slice interval is not 2, lineage continue!");
		}
		
		if(noLineageAfterCorrection)
		{			
			System.out.println("stop lineage process by correcting seeds only");
			roisArrayToManager[slice-1] = cellsTimeIndexToRois(cellsTimeIndex[slice-1],roisArrayToManager[slice-1]);
			return roisArrayToManager[slice-1].length;
		}

		Ins_cell rootCell = cellsTimeIndex[slice-1][0].getCell(1);
		if(rootCell == null)
			return 0;		
		String rName="A";//root name of the cell
		rootCell.setName(rName);
		rootCell.parent = new Ins_cell(null);
		if(allCellsInFirstChannel){
			String name = "New";
			for(int a=2;a<=cellsTimeIndex[slice-1][0].getCellVector().size();a++)
			{
				switch (a) {
				case 2:
					name = "B";
					break;
				case 3:
					name = "C";
					break;
				case 4:
					name = "D";
					break;
				case 5:
					name = "E";
					break;
				case 6:
					name = "F";
				default:
					name = "NoN";
					break;
				}
				cellsTimeIndex[slice-1][0].getCell(a).setName(name);
				cellsTimeIndex[slice-1][0].getCell(a).parent = new Ins_cell(null);
			}
		}
		
		Vector<Ins_cell> errorCellsVector = new Vector<Ins_cell>(10);			
		roisArrayToManager[slice-1] = lineageCellsTimeIndex(cellsTimeIndex[slice-1], errorCellsVector,roisArrayToManager[slice-1]);
		//correctErrorCell(errorCellsVector,cellsTimeIndex);
		//System.out.println("----------------before cells' number: " + errorCellsVector.size());
		errorCellsVector = new Vector<Ins_cell>(10);
		cellsTimeIndex[slice-1] = reinitialCellsTimeIndex(cellsTimeIndex[slice-1],identifyColor);
		roisArrayToManager[slice-1] = lineageCellsTimeIndex(cellsTimeIndex[slice-1], errorCellsVector,roisArrayToManager[slice-1]);
		//System.out.println("----------------after cells' number: " + errorCellsVector.size());
		
		
		int iterNumber = 1;
		boolean green  = true;
		boolean red = false;
		// competition initied by user
		if(!mergeFirst)
		{
			green = false;
			red = true;
		}
		// redo
		while(errorCellsVector.size()>0 && iterNumber <= iterTotal)
		{
			if(iterNumber > 0.5 * iterTotal)
			{
				if(!mergeFirst)
				{
					green = true;
					red = false;
				}else {
					green = false;
					red = true;
				}
			}
			System.out.println("------------------- iteration : "+ iterNumber + (red?" split":" merge")) ;
			correctErrorCell(errorCellsVector,cellsTimeIndex[slice-1],green, red, imp);
			cellsTimeIndex[slice-1] = reinitialCellsTimeIndex(cellsTimeIndex[slice-1],identifyColor);
			errorCellsVector = new Vector<Ins_cell>(10);			
			roisArrayToManager[slice-1] = lineageCellsTimeIndex(cellsTimeIndex[slice-1], errorCellsVector,roisArrayToManager[slice-1]);
			iterNumber++;
		}
		
		int filamentLength = 0;
		if(detectPhilament)
		{
			Vector<Integer> v= getAllCellSizeinVector(cellsTimeIndex[slice-1]);
			int count = 0, ints[] = new int[v.size()];
			for(int i: v) ints[count++] = i;
			Arrays.sort(ints);
			filamentLength = (int) (filamentRatio * ints[ints.length/2]);
			//System.out.println("filament length" + filamentLength);
			identifyColor = true;
			filamentDetection(cellsTimeIndex[slice-1],filamentLength);
			cellsTimeIndex[slice-1] = reinitialCellsTimeIndex(cellsTimeIndex[slice-1],identifyColor);
			errorCellsVector = new Vector<Ins_cell>(10);
			roisArrayToManager[slice-1] = lineageCellsTimeIndex(cellsTimeIndex[slice-1], errorCellsVector,roisArrayToManager[slice-1]);
		}
		
		if(detectFoci && increment == 1)
			detectFoci = false;
		
		if(detectFoci)
		{
			//System.out.println("detect Foci");
			Roi[] aggRoi = detectFociPSF(slice, imp, increment,cellsTimeIndex[slice-1]);
			if(aggRoi != null && aggRoi.length>0)
			{
//				if(slice == 7)
//					System.out.println("break here");
				aggRoi = chargeFociFunc(cellsTimeIndex[slice-1],aggRoi);
				if(aggRoi!=null)
				{
					Roi[] roiTemp = new Roi[aggRoi.length + roisArrayToManager[slice-1].length];
					System.arraycopy(roisArrayToManager[slice-1], 0, roiTemp, 0, roisArrayToManager[slice-1].length);
					System.arraycopy(aggRoi, 0, roiTemp, roisArrayToManager[slice-1].length, aggRoi.length);
					roisArrayToManager[slice-1] = roiTemp;
				}
			}
		}else if (increment == 2) {
			System.out.println("Oval, point, line treatment - Foci");
			Roi[] aggRoi = constructFociRoisSlice(roiArray, slice+1);
			if(aggRoi != null)
			{
				aggRoi = chargeFociFunc(cellsTimeIndex[slice-1],aggRoi);
				if(aggRoi!=null)
				{
					Roi[] roiTemp = new Roi[aggRoi.length + roisArrayToManager[slice-1].length];
					System.arraycopy(roisArrayToManager[slice-1], 0, roiTemp, 0, roisArrayToManager[slice-1].length);
					System.arraycopy(aggRoi, 0, roiTemp, roisArrayToManager[slice-1].length, aggRoi.length);
					roisArrayToManager[slice-1] = roiTemp;
				}
			}
		}
	
		
		//regroup cell, distribute the cells which have the ancestor and descendant the cell ID, otherwise, mark 0 ou -1, make this at the last step
		int count = regroupCell(cellsTimeIndex[slice-1]);		
		if(writeResultTable && increment > 1){
			String nameL = String.valueOf(slice);
			Ins_editor logEditor = new Ins_editor();
			logEditor.setTitle(nameL);
			logEditor.append("cell_label"+"\t"+
					"last_cell" + "\t"+	 // check if it's the last cell add
					"name"+"\t" +
					"timeIndex"+"\t"+
					"cell_num"+"\t"+						
					"length"+"\t"+
					"xCenter"+"\t"+
					"yCenter"+"\t"+
					"cell_area" + "\t"+
					"meanRFP"  + "\t"+							
					"medianRFP"  + "\t"+
					"min" + "\t" + 
					"max" + "\t" +
					"h5"  + "\t" +
					"h25" + "\t" +
					"filament" +"\t"+					
					"cellID" + "\t"+
					"divideNext" + "\t"+
					"bgCell" + "\t"+
					"meanGFP"+"\t"+
					"medianGFP"+"\t"+
					"bgGFP"+"\t"+ 
					"numberFoci"+"\t"+
					"x"+"\t"+
					"y"+"\t"+
					"area" + "\t"+
					"mean"+"\t"+
					"median"+"\t"+"\r"+"\n");			
			Roi[] aggRoi = wrtieResultTologEditor(cellsTimeIndex[slice-1], logEditor,imp,slice,increment);
			logEditor.setPath(outputDirectoryPath+nameL+".txt");
			System.out.println("save file : " + outputDirectoryPath+nameL+".txt");
			logEditor.save();			
			Roi[] roiTemp = new Roi[aggRoi.length + roisArrayToManager[slice-1].length];
			System.arraycopy(roisArrayToManager[slice-1], 0, roiTemp, 0, roisArrayToManager[slice-1].length);
			System.arraycopy(aggRoi, 0, roiTemp, roisArrayToManager[slice-1].length, aggRoi.length);
			roisArrayToManager[slice-1] = roiTemp;
		}else if (writeResultTable) {	
			// preprocessing imp to get fft-toOriginalImage,
			String name = "";
			int sliceMid = -1;
			if(slice%3 == 1)
			{	
				name = "cfpLineage";
				sliceMid = slice + 1;
			}
			else {
				name = "rfpLineage";
				sliceMid = slice;
			}
			for(int i=sliceMid-1;i<=sliceMid+1;i++)
			{
				String nameL = "";
				ImageProcessor ip = impFFT.getImageStack().getProcessor(i);				
				if(i==sliceMid-1)
					nameL = "slice-" + String.valueOf(sliceMid-1) + "-" + "CFP-" +name+"-"+imp.getShortTitle(); 
				else if (i==sliceMid) {
					nameL = "slice-" + String.valueOf(sliceMid-1) + "-" + "RFP-" +name+"-"+imp.getShortTitle();
				}else if (i==sliceMid+1) {
					nameL = "slice-" + String.valueOf(sliceMid-1) + "-" + "YFP-" +name+"-"+imp.getShortTitle();
				}
				Ins_editor logEditor = new Ins_editor();
				logEditor.setTitle(nameL);
				//logEditor.setVisible(true);
				logEditor.append("cell_label"+"\t"+
						"last_cell" + "\t"+	 // check if it's the last cell add
						"name"+"\t" +
						"timeIndex"+"\t"+
						"cell_num"+"\t"+						
						"length"+"\t"+
						"xCenter"+"\t"+
						"yCenter"+"\t"+
						"cell_area" + "\t"+
						"meanIntensity"  + "\t"+							
						"medianIntensity"  + "\t"+
						"min" + "\t" + 
						"max" + "\t" +
						"h5"  + "\t" +
						"h25" + 
						"\t"+ "\r"+"\n");
				wrtieResultTologEditorYifan(cellsTimeIndex[slice-1], logEditor,ip);
				logEditor.setPath(outputDirectoryPath+nameL+".txt");
				IJ.log("save file : " + outputDirectoryPath+nameL+".txt");
				logEditor.save();
			}
		}
		IJ.showStatus(slice+" "+"done!");
		//regroup cell, distribute the cells which have the ancestor and descendant the cell ID, otherwise, mark 0 ou -1, make this at the last step
		//int count = regroupCell(cellsTimeIndex[slice-1]);
		if(evaluation)
		{
			resultsTable.show("Evaluation table");			
			resultsTable.addValue("Slice " + slice, count);
		}
		if(computeDesc)
			assignGrowthRate(cellsTimeIndex[slice-1]);
		IJ.showStatus(slice+" "+"done!");
		return count;
	}
	
	private ImagePlus fftFromOriginal(ImagePlus imp) {
		ImageStack ims = imp.getImageStack();
		int height = imp.getHeight();
		FftBandPassFilter fftBandPassFilter = new FftBandPassFilter();
		fftBandPassFilter.setup("", imp);
		fftBandPassFilter.setLargeSmallFilterSize(60, 3);
		
		ImagePlus impFFTOut = IJ.createImage("", imp.getWidth(), height, ims.getSize(), imp.getBitDepth());
		ImageStack imsFFTOut = impFFTOut.getImageStack();
		
		for(int s=1; s<=ims.getSize();s++)
		{
			IJ.showStatus("FFT runing on slice : " + s);
			int n=4;
			ImageProcessor ip = ims.getProcessor(s);
			ImagePlus impFFt = IJ.createImage("fft", roi_width*n, height, 1, imp.getBitDepth());
			ImageProcessor ipFFt = impFFt.getProcessor();
			BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
			ImageProcessor ipFFTOut = imsFFTOut.getProcessor(s);
			for(int slice=0;slice<stackSize;slice++)
			{
				if(slice%n == 0)
				{
					ipFFt = impFFt.getProcessor();
					for(int a=slice; a<slice+n; a++)
					{
						int x= a*(roi_width+6);
						Roi aroi = new Roi(x, 0, roi_width, height);
						ip.setRoi(aroi);
						ImageProcessor ip2 = ip.crop();
						ipFFt.insert(ip2, a%n*(roi_width), 0);
						if(a>=stackSize)
						{
							ip.setRoi(new Roi(slice*(roi_width+6),0,roi_width,height));
							ipFFt.insert(ip.crop(),a%n*(roi_width),0);
						}
					}
					fftBandPassFilter.setup("fft band pass filter", impFFt);
					fftBandPassFilter.run(ipFFt);
				}
				int xx= (slice%n)*(roi_width);			
				Roi roiChannel = new Roi(xx, 0, roi_width, height);
				ipFFt.setRoi(roiChannel);
				ImageProcessor ipChannel = ipFFt.crop();
				bSubtracter.rollingBallBackground(ipChannel, 40, false, false, false, true, false);
				ipFFTOut.insert(ipChannel, slice*(roi_width+6), 0);			
			}
		}
		
		return impFFTOut;
	}
	
	public static void main(String[] args)
	{
		ImageJ ij = new ImageJ();
		ImagePlus imp = IJ.openImage();
		Lineage lineage = new Lineage(imp);
		ImagePlus impFFT = lineage.fftFromOriginal(imp);
		impFFT.show();
		
	}
	

	/**
	 * 
	 * @param cellsTimeIndex
	 * @param logEditor
	 * @param imp
	 * @param slice
	 * @param increment
	 * @return compute background of the trans/RFP and GFP, when increment == 2
	 */
	private Roi[] wrtieResultTologEditor(Ins_cellsVector[] cellsTimeIndex, Ins_editor logEditor, ImagePlus imp, int slice, int increment)
	{		
		ArrayList<Roi> aggRois = new ArrayList<Roi>();
		int mOption = 0;
		mOption = mOption|Measurements.CENTROID|Measurements.MEAN|Measurements.MEDIAN|Measurements.AREA|Measurements.MIN_MAX;
		ImageProcessor ip = imp.getImageStack().getProcessor(slice);
		ImageProcessor ipdup = ip.duplicate();
		ipdup.setValue(0);
		ImageProcessor ip2 = null;
		ImageProcessor ip2dup = null;

		ip2 = imp.getImageStack().getProcessor(slice + 1);
		ip2dup = ip2.duplicate();
		ip2dup.setValue(0);

		int mOptions = ImageStatistics.AREA|ImageStatistics.MEAN|ImageStatistics.MEDIAN|ImageStatistics.CENTROID;
		int rwidth = roi_width;
		int y = 0;
		int rheight = ip.getHeight();

		//String s = "";
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			int x = actualChannel.getTimeIndex()*(roi_width+6);
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);
				if(actualCell == null || actualCell.parent==null)
					continue;
				Roi roi = actualCell.getRoi();
				ipdup.fill(roi);
				ip2dup.fill(roi);
			}
			ip2dup.setRoi(x, y, rwidth, rheight);
			ipdup.setRoi(x, y, rwidth, rheight);
			ImageProcessor ipSlice = ipdup.crop();
			int[] numArray = new int[roi_width*rheight];
			int[] numArray2 = new int[roi_width*rheight];
			int k = 0;
			for(int xx=0;xx<ipSlice.getWidth();xx++)
			{
				for(int yy=0;yy<ipSlice.getHeight();yy++)
				{
					if(ipSlice.get(xx, yy)==0)
						continue;
					numArray[k] = ipdup.get(xx, yy);
					numArray2[k] = ip2dup.get(xx, yy);
					k++;
				}
			}
			int[] numArrayNew = new int[k];
			int[] numArray2New = new int[k];
			System.arraycopy(numArray, 0, numArrayNew, 0, k);
			System.arraycopy(numArray2, 0, numArray2New, 0, k);
			numArray = numArrayNew;
			numArray2 = numArray2New;
			Arrays.sort(numArray);
			Arrays.sort(numArray2);
			double median;
			double median2;
			if (numArray.length % 2 == 0)
			{
			    median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
			    median2 = ((double)numArray2[numArray.length/2] + (double)numArray2[numArray.length/2 - 1])/2;
			}else{
				median = (double) numArray[numArray.length/2];
				median2 = (double) numArray2[numArray.length/2];
			}
			
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);
				if(actualCell == null || actualCell.parent==null)
					continue;
				// compute the cell information
				Roi roi = actualCell.getRoi();
				String name = getLabel(roi);
				actualCell.setLabel(name);				
				if(roi instanceof Arrow || roi instanceof PointRoi)
					continue;
				ip.resetRoi();
				ip.setRoi(roi);
				int[] hist = ip.getHistogram();
				int[] cumulateHist = new int[hist.length];
				cumulateHist[0] = hist[0];
				for(int h=1; h<hist.length; h++)
				{					
					cumulateHist[h] = cumulateHist[h-1] + hist[h];
				}
				int size = cumulateHist[cumulateHist.length - 1];
				double cumulativePercentage = 0;
				int h5 = 0;
				int h25 = 0;
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;					
					if(cumulativePercentage>=0.05)
					{
						h5 = h;
						break;
					}
				}
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;
					if(cumulativePercentage>=0.25)
					{
						h25 = h;
						break;
					}
				}							
					
				ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
				actualCell.setArea(statsCell.area);
				actualCell.setCentroid(statsCell.xCentroid, statsCell.yCentroid);	
				actualCell.setMeanIntensity(statsCell.mean);
				actualCell.setMedianIntensity(statsCell.median);
				actualCell.setMinIntensity(statsCell.min);
				actualCell.setMaxIntensity(statsCell.max);
				actualCell.setHist5(h5);
				actualCell.setHist25(h25);
				actualCell.setBGMedian(median);
				
				
//				if(increment > 1)
//				{
					Roi[] aRois = actualCell.getAggRoisPoint();
					if(aRois!=null)
					{
						actualCell.createAggregateInfo();
						for(int m=0; m<aRois.length; m++)
						{
							ip2.setRoi(aRois[m]);
							ImageStatistics iStatistics = ImageStatistics.getStatistics(ip2, mOptions, null);
							actualCell.setAggregateInfo(iStatistics.xCentroid,iStatistics.yCentroid,iStatistics.area, iStatistics.mean, iStatistics.median,m);
						}
					}
					ip2.setRoi(roi);
					ImageStatistics iStatistics = ImageStatistics.getStatistics(ip2, mOptions, null);
					actualCell.setMeanGFP(iStatistics.mean);
					actualCell.setMedianGFP(iStatistics.median);
					actualCell.setBGAggMedian(median2);
//				}
				logEditor.append(actualCell.toStringAntoine());
			}
		}
		//IJ.save(new ImagePlus("", ip2dup), "d:/ip2dupFillOutsideRoi.tif");
		//logEditor.append(s);
		return aggRois.toArray(new Roi[aggRois.size()]);
	}
	
	private void wrtieResultTologEditor(Ins_cellsVector[] cellsTimeIndex, Ins_editor logEditor, ImageProcessor ip)
	{
		int mOption = 0;
		mOption = mOption|Measurements.CENTROID|Measurements.MEAN|Measurements.MEDIAN|Measurements.AREA|Measurements.MIN_MAX;
		
		
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);
				if(actualCell == null || actualCell.parent==null)
					continue;
				// compute the cell information
				//System.out.println(" time index : " + i + " cell name : " + actualCell.getName());
				Roi roi = actualCell.getRoi();
				String name = getLabel(roi);
				actualCell.setLabel(name);				
				if(roi instanceof Arrow || roi instanceof PointRoi)
					continue;
				ip.resetRoi();
				ip.setRoi(roi);
				//new ImagePlus("slice-", ip).show();
				int[] hist = ip.getHistogram();
				int[] cumulateHist = new int[hist.length];
				cumulateHist[0] = hist[0];
				for(int h=1; h<hist.length; h++)
				{					
					cumulateHist[h] = cumulateHist[h-1] + hist[h];
				}
				int size = cumulateHist[cumulateHist.length - 1];
				double cumulativePercentage = 0;
				int h5 = 0;
				int h25 = 0;
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;					
					if(cumulativePercentage>=0.05)
					{
						h5 = h;
						break;
					}
				}
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;
					if(cumulativePercentage>=0.25)
					{
						h25 = h;
						break;
					}
				}							
					
				ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
				//int index = actualCell.setAreaToAreaArray(statsCell.area);
				//actualCell.setM
				actualCell.setArea(statsCell.area);
				actualCell.setCentroid(statsCell.xCentroid, statsCell.yCentroid);	
				actualCell.setMeanIntensity(statsCell.mean);
				actualCell.setMedianIntensity(statsCell.median);
				actualCell.setMinIntensity(statsCell.min);
				actualCell.setMaxIntensity(statsCell.max);
				actualCell.setHist5(h5);
				actualCell.setHist25(h25);
				
				// part of ditect aggregate
				//if(mChRFP.isSelected())
					logEditor.append(actualCell.toStringYifan());
//				else {
//					logEditor.append(actualCell.toString2());
//				}
			}
		}
	}
	
	private void wrtieResultTologEditorYifan(Ins_cellsVector[] cellsTimeIndex, Ins_editor logEditor, ImageProcessor ip)
	{
		int mOption = 0;
		mOption = mOption|Measurements.CENTROID|Measurements.MEAN|Measurements.MEDIAN|Measurements.AREA|Measurements.MIN_MAX;
		ip.resetMinAndMax();
		ImageProcessor ip16 = ip.convertToShort(true);		
		double min = ip.getMin();
		double max = ip.getMax();
		double scale;
		if ((max-min)==0.0)
			scale = 1.0;
		else
			scale = 65535.0/(max-min);
		
		
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);
				if(actualCell == null || actualCell.parent==null)
					continue;
				// compute the cell information
				//System.out.println(" time index : " + i + " cell name : " + actualCell.getName());
				Roi roi = actualCell.getRoi();
				String name = getLabel(roi);
				actualCell.setLabel(name);				
				if(roi instanceof Arrow || roi instanceof PointRoi)
					continue;
				
				ip.resetRoi();
				ip.setRoi(roi);
				
				ip16.resetRoi();
				ip16.setRoi(roi);

				int[] hist = ip16.getHistogram();
				int[] cumulateHist = new int[hist.length];
				cumulateHist[0] = hist[0];
				for(int h=1; h<hist.length; h++)
				{					
					cumulateHist[h] = cumulateHist[h-1] + hist[h];
				}
				int size = cumulateHist[cumulateHist.length - 1];
				double cumulativePercentage = 0;
				double h5 = 0;
				double h25 = 0;
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;					
					if(cumulativePercentage>=0.05)
					{
						h5 = h;
						break;
					}
				}
				for(int h=hist.length-1;h>=0;h--)
				{
					cumulativePercentage = 1-(double)cumulateHist[h]/(double)size;
					if(cumulativePercentage>=0.25)
					{
						h25 = h;
						break;
					}
				}			
				

				h5 = h5/scale + min;
				h25 = h25/scale + min;
				
				ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
				//int index = actualCell.setAreaToAreaArray(statsCell.area);
				//actualCell.setM
				actualCell.setArea(statsCell.area);
				actualCell.setCentroid(statsCell.xCentroid, statsCell.yCentroid);	
				actualCell.setMeanIntensity(statsCell.mean);
				actualCell.setMedianIntensity(statsCell.median);
				actualCell.setMinIntensity(statsCell.min);
				actualCell.setMaxIntensity(statsCell.max);
				actualCell.setHist5(h5);
				actualCell.setHist25(h25);
				logEditor.append(actualCell.toStringYifan());
			}
		}
	}
	
	private Roi[] constructFociRoisSlice(Roi[] roiArray, int slice)
	{
		int count = roiArray.length;
		Roi[] currentRois = new Roi[count];		
		int j=0;
		for (int i=0; i<count; i++) {	
			Roi roi = roiArray[i];
			if(roi.getPosition() != slice)
				continue;
			// keep line roi but next should add keep shape roi but not rectangular roi
			int type = roi.getType();
			if(!(type==Roi.POINT || type==Roi.FREELINE || type == Roi.OVAL ))
				continue;
			roi.setStrokeColor(new Color(255,255,0));
			currentRois[j] = roi;
			j++;
		}
		if(j==0)
			return null;
		Roi[] sliceRois = new Roi[j];
		System.arraycopy(currentRois, 0, sliceRois, 0, j);
		return sliceRois;
	}
	
	private Roi[] chargeFociFunc(Ins_cellsVector[] cellsTimeIndex, Roi[] aggRoi) 
	{		
		Ins_cellsVector aggregateTimeIndex[] = null;
		Ins_cell[] cells1 = constructCellsListFromStack(aggRoi, false);
		if(cells1 != null)
		{
			Arrays.sort(cells1);
			setCellsNumber(cells1);
			aggregateTimeIndex = constructCellVectors(stackSize,cells1);	
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		int height = imp.getHeight();
		// treat oval time index
		boolean deleteAll = false;
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualAggChannel = aggregateTimeIndex[i];
			for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
			{
				Ins_cell actualCell = actualAggChannel.getCell(b);
				if(actualCell == null)
					continue;
				Roi aroi = actualCell.getRoi();
				if(aroi.getType() == Roi.OVAL) 
				{
					deleteAll = aroi.getBounds().height > height*0.8;
					if(deleteAll)
						break;
					int tIndex1 = computeTimeIndex(aroi.getBounds().x);
					int tIndex2 = computeTimeIndex(aroi.getBounds().x + aroi.getBounds().width);
					for(int j=tIndex1; j<=tIndex2; j++)
					{
						if(j<0 || j>= cellsTimeIndex.length)
							continue;
						Ins_cellsVector fociChannel = aggregateTimeIndex[j];
						for(int a = 1; a<=fociChannel.getCellVector().size(); a++)
						{
							Ins_cell actualPoint = fociChannel.getCell(a);
							if(actualPoint == null)
								continue;
							Roi roi = actualPoint.getRoi();
							if(!(roi instanceof PointRoi))
								continue;						
							if(aroi.getBounds().contains(roi.getBounds().x, roi.getBounds().y))
							{
								fociChannel.removeCell(actualPoint);
								a = 0;
							}
						}
					}
					actualAggChannel.removeCell(actualCell);
				}
			}
			if(deleteAll)
				return null;
		}

		// treat foci time index
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];
			Ins_cellsVector actualAggChannel = aggregateTimeIndex[i];
			for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
			{
				Ins_cell aPoint = actualAggChannel.getCell(b);
				if(aPoint == null)
					continue;
				Roi aroi = aPoint.getRoi();
				if(aroi instanceof PointRoi) // avoid the Line and any other shape of Roi, here only pointRoi is considered as Aggregate
				{
					aPoint.setFociProperty(true);
					Roi aAggRoi = aPoint.getRoi();
					double fociY = aAggRoi.getBounds().y;
					for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
					{
						Ins_cell aCell = actualChannel.getCell(a);
						if(aCell == null)
							continue;
						Roi roi = aCell.getRoi();
						if(roi instanceof Line || roi instanceof PointRoi || roi instanceof OvalRoi)
							continue;
						if(fociY >= roi.getBounds().y && fociY < roi.getBounds().y + roi.getBounds().height)
						{
							aPoint.addAttachedCell(aCell);
						}
					}
					//if point not arributed to an attached cell, attribute to the closest one
					if(aPoint.getAttachedCell()!=null)
						continue;
					double min = Double.MAX_VALUE;
					int index = -1;
					for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
					{
						Ins_cell aCell = actualChannel.getCell(a);
						if(aCell == null)
							continue;
						// compute the cell information
						Roi roi = aCell.getRoi();
						if(roi instanceof Line || roi instanceof PointRoi || roi instanceof OvalRoi)
							continue;
						if(Math.abs(fociY - roi.getBounds().y - roi.getBounds().height*0.5)<min)
						{
							min = Math.abs(fociY - roi.getBounds().y - roi.getBounds().height*0.5); 
							index = a;
						}
					}
					if(index > 0)
						aPoint.addAttachedCell(actualChannel.getCell(index));
				}
			}
		}
		
		// treat line time index
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
//			Ins_cellsVector actualChannel = cellsTimeIndex[i];
			Ins_cellsVector actualAggChannel = aggregateTimeIndex[i];

			for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
			{
				Ins_cell actualLine = actualAggChannel.getCell(b);
				if(actualLine == null)
					continue;
				Roi lineRoi = actualLine.getRoi();
				int type = lineRoi.getType();
				if(type != Roi.FREELINE)			
					continue;				
				int tIndex1 = computeTimeIndex(lineRoi.getBounds().x);
				int tIndex2 = computeTimeIndex(lineRoi.getBounds().x + lineRoi.getBounds().width);

//				if(tIndex2 - tIndex1 > 2)
//					System.out.println("diagonal line detected");

				for(int j=tIndex1; j<=tIndex2; j++)
				{
					Ins_cellsVector cellChannel = cellsTimeIndex[j];
					Ins_cellsVector fociChannel = aggregateTimeIndex[j];
					for(int a = 1; a<=fociChannel.getCellVector().size(); a++)
					{
						Ins_cell actualAgg = fociChannel.getCell(a);
						if(actualAgg == null)
							continue;
						Roi pRoi = actualAgg.getRoi();
						if(!(pRoi instanceof PointRoi))
							continue;
						
						if(pRoi.getBounds().y >= lineRoi.getBounds().y && pRoi.getBounds().y < lineRoi.getBounds().y+lineRoi.getBounds().height)
						{						
							// add potential point roi to current cell, then compute cost function to define if there is a foci
							double project = 0;
							Ins_cell attachedCell = null;
							for(int c = 1; c<=cellChannel.getCellVector().size(); c++)
							{
								Ins_cell actualCell = cellChannel.getCell(c);
								double projection = maxProjectRatio(actualCell, actualLine);
								if(projection > project)
								{
									attachedCell = actualCell;
									project = projection;
								}
							}
							if(attachedCell!=null)
							{
								actualAgg.addCorrectedAttachedCell(attachedCell);
								//System.out.println("max projection : " + project);
							}
						}
					}
				}
			}
		}
		
		// add detected foci and attached cell to the result list
		ArrayList<Roi> aggRois = new ArrayList<Roi>();
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualAggChannel = aggregateTimeIndex[i];
			for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
			{
				Ins_cell actualAgg = actualAggChannel.getCell(b);
				if(actualAgg == null)
					continue;
				aggRois.add(actualAgg.getRoi());				
				Ins_cell[] attachedCell = actualAgg.getAttachedCell();
				if(attachedCell==null)
					continue;
				addAttachedCellRoiToVector(aggRois, attachedCell[0],actualAgg.getRoi());
				attachedCell[0].addAggRoi(actualAgg.getRoi());
			}
		}
		return aggRois.toArray(new Roi[aggRois.size()]);
		
	}
	
		private void addAttachedCellRoiToVector(ArrayList<Roi> aggRois,
				Ins_cell attachedCell, Roi aggRoi) {
			if(attachedCell.fociIsadded())
			{
				aggRoi.setStrokeColor(attachedCell.getPointNextColor());
				return;
			}
			int position = aggRoi.getPosition();
			attachedCell.setFociAdded(true);
			Roi cellRoi = (Roi)attachedCell.getRoi().clone();				
			cellRoi.setPosition(position);
			aggRois.add(cellRoi);
			if(attachedCell.nextCell!=null)
			{
				Roi arrow = attachedCell.arrowToNext;
				if(arrow!=null)
				{
					arrow = (Roi)arrow.clone();
					arrow.setPosition(position);
					arrow.setName(getLabelArrow(position, arrow));
					aggRoi.setStrokeColor(arrow.getStrokeColor());				
					aggRois.add(arrow);
				}
			}else if (attachedCell.nextTopCell!=null) {
				Roi arrow = attachedCell.arrowToTop;
				if(arrow!=null)
				{
					arrow = (Roi)arrow.clone();
					arrow.setPosition(position);
					arrow.setName(getLabelArrow(position, arrow));
					aggRoi.setStrokeColor(arrow.getStrokeColor());
					aggRois.add(arrow);
				}
				if(attachedCell.nextBottomCell!=null)
				{
					arrow = attachedCell.arrowToBottom;
					if(arrow!=null)
					{
						arrow = (Roi)arrow.clone();
						arrow.setPosition(position);
						arrow.setName(getLabelArrow(position, arrow));
						aggRois.add(arrow);
					}
				}
			}else {
				aggRoi.setStrokeColor(attachedCell.getPointNextColor());
			}
		}
		
		private ImageProcessor getPSFRssV(ImageProcessor ip){
			double sigma = 1.2;
			boolean refineMaskLoG = false;
			boolean refineMaskValid = false;
			boolean whiteObject = true;
			PointSourceDetection pSD = new PointSourceDetection(new ImagePlus("laplacien", ip), sigma, refineMaskLoG, refineMaskValid,whiteObject);
			pSD.process(false);		
			ImagePlus RssV = pSD.getRssV();
			ImageProcessor ipRssV = RssV.getProcessor();
			return ipRssV;
		}
		
		private Roi[] detectFociPSF(int slice1, ImagePlus imp, int increment,Ins_cellsVector[] cellsTimeIndex) 
		{
			ImageProcessor ip = imp.getImageStack().getProcessor(slice1 + 1);
			ImageProcessor ipRssv = getPSFRssV(ip);
			ImageProcessor sIpRSSV = ipRssv.convertToByte(true);
			sIpRSSV.invert();
//			IJ.save(new ImagePlus("", sIpRSSV), "d:/"+"ipRSSV-slice-"+String.valueOf(slice1)+".tif");
			Roi[] roisSlice1 = new Roi[50];
			int[] number = new int[]{0,roisSlice1.length};

			for(int ss=0; ss<stackSize;ss++)
			{	
				//boolean debug = ss == 2118/(roi_width+6);
				int xx= ss*(roi_width+6);			
				Roi roi = new Roi(xx, 0, roi_width, imp.getHeight());
				sIpRSSV.setRoi(roi);
				ImageProcessor ip2 = sIpRSSV.crop();
//				if(ss == 250)
//					IJ.save(new ImagePlus("", ip2), "d:/"+String.valueOf(slice1)+"---"+String.valueOf(ss)+".tif");
				MaximumFinder mFinder = new MaximumFinder();
				Polygon points = mFinder.getMaxima(ip2, 5, true);
				for(int i=0;i<points.npoints;i++)
				{
					Roi pRoi = new PointRoi(ss*(roi_width+6)+points.xpoints[i], points.ypoints[i]);
					//PointRoi p = new PointRoi(ss*(roi_width + 6)+points.xpoints[i],points.ypoints[i]);
					if (number[0]==number[1]) {
						Roi[] toRoitemp = new Roi[number[1]*2];						  
						System.arraycopy(roisSlice1, 0, toRoitemp, 0, number[1]);
						roisSlice1= toRoitemp;
						number[1] *= 2;
					}
					pRoi.setPosition(slice1 + 1);
					roisSlice1[number[0]] = pRoi;
					number[0]++;
				}
			}
			
			System.out.println("point detection in slice : " + (slice1+1)+" is " + number[0]);
			if(number[0] == 0)
				return null;

			Ins_cellsVector aggregateTimeIndex[] = null;
			if(number[0] > 0)
			{
				Roi[] toRoitemp = new Roi[number[0]];						  
				System.arraycopy(roisSlice1, 0, toRoitemp, 0, number[0]);
				roisSlice1= toRoitemp;						 
				Ins_cell[] cells1 = constructCellsListFromStack(roisSlice1, false);
				if(cells1 != null)
				{
					try {
						Arrays.sort(cells1);
						setCellsNumber(cells1);
						aggregateTimeIndex = constructCellVectors(stackSize,cells1);	
					} catch (Exception e) {
						roisSlice1 = null;
					}
				}
			}else {
				return null;
			}						

			for(int i=0; i < cellsTimeIndex.length; i++)
			{
				Ins_cellsVector actualChannel = cellsTimeIndex[i];
				Ins_cellsVector actualAggChannel = null;
				if(aggregateTimeIndex!=null)
					actualAggChannel = aggregateTimeIndex[i];
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					Ins_cell actualCell = actualChannel.getCell(a);
					if(actualCell == null)
						continue;
					// compute the cell information
					Roi roi = actualCell.getRoi();
					String name = getLabel(roi);
					actualCell.setLabel(name);				
					if(roi instanceof Arrow || roi instanceof PointRoi)
						continue;					
					if(actualAggChannel!=null)
					{
						for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
						{
							Ins_cell actualAgg = actualAggChannel.getCell(b);
							if(actualAgg == null)
								break;
							Roi aAggRoi = actualAgg.getRoi();
							if(roi.getBounds().contains(aAggRoi.getBounds()))
							{
								// add potential point roi to current cell, then compute cost function to define if there is a foci
								actualCell.addAggRoi(aAggRoi);
							}
						}
					}					
				}
			}
			ArrayList<Roi> aggRois = new ArrayList<Roi>();	
			for(int i=0; i < cellsTimeIndex.length; i++)
			{
				Ins_cellsVector actualChannel = cellsTimeIndex[i];
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					Ins_cell actualCell = actualChannel.getCell(a);
					if(actualCell == null)
						continue;
					PointRoi[] pRois = actualCell.getAggRoisPoint();
					if(pRois==null)
						continue;
					if(actualCell.getFociState())
					{
						for(Roi r:pRois)
							aggRois.add(r);						
						continue;
					}

					boolean findFoci = false;
					Ins_cell nextCell = actualCell.nextCell;					
					if(nextCell != null)
					{	
						if(nextCell.getAggRoisPoint()!=null)
							findFoci = true;
					}else {
						nextCell = actualCell.nextTopCell;
						if(nextCell!=null)
						{
							if (nextCell.getAggRoisPoint()!=null) {
								findFoci = true;
							}else {
								nextCell = actualCell.nextBottomCell;
								if (nextCell!=null && nextCell.getAggRoisPoint()!=null) {
									findFoci = true;
								}
							}
						}
					}
					if(findFoci)
					{
						for(Roi r:pRois)
							aggRois.add(r);
					}
					actualCell.setFociState(findFoci);
					if(nextCell!=null)
						nextCell.setFociState(findFoci);
				}
			}
			return aggRois.toArray(new Roi[aggRois.size()]);
		}
	
		private Roi[] detectFociFunc1(int slice1, ImagePlus imp, int increment,Ins_cellsVector[] cellsTimeIndex) 
		{
			//return null;
			ImageProcessor ip = imp.getImageStack().getProcessor(slice1 + 1);
			ImagePlus impDog = IJ.createImage("impDog-"+String.valueOf(slice1+1), ip.getWidth(), ip.getHeight(), 1, ip.getBitDepth());
			ImageProcessor ipDog = impDog.getProcessor();
			//		LoG3D loG3D = new LoG3D(false);
			//		ImageProcessor ipF = ip.convertToFloat();
			//		ImageWare imageWare = Builder.create(ipF.getFloatArray());
			//		ImageWare imageWare2 = loG3D.doLoG_Separable(imageWare, 1.8, 1.8);
			//		ImageStack ims = imageWare2.buildImageStack();
			//		ImagePlus imp2 = new ImagePlus("imageware2", ims);
			//		ShortProcessor sp = imp2.getProcessor().convertToShortProcessor(true);

			ShortProcessor sp = ip.convertToShortProcessor(true);
			//		sp.invert(); // bright foci on black background

			//FftBandPassFilter fftBandPassFilter = new FftBandPassFilter(3,40);
			//FftBandPassFilter.filterLargeDia = 40;

			BackgroundSubtracter bSubstractor = new BackgroundSubtracter();
			int n = stackSize;
			ImagePlus impFFt = IJ.createImage("fft", roi_width*n, imp.getHeight(), 1, 32);
			ImageProcessor ipFFt = impFFt.getProcessor();

			Roi[] roisSlice1 = new Roi[50];
			int[] number = new int[]{0,roisSlice1.length};
			//int nPoints = 0;

			for(int ss=0; ss<stackSize;ss++)
			{	
				boolean debug = ss == 2118/(roi_width+6);
				if(ss%n == 0)
				{
					for(int a=ss; a<ss+n; a++)
					{
						int x= a*(roi_width+6);
						Roi roi = new Roi(x, 0, roi_width, imp.getHeight());
						sp.setRoi(roi);
						ImageProcessor ip2 = sp.crop();
						ipFFt.insert(ip2, a%n*(roi_width), 0);
						if(a>=stackSize)
						{
							ipFFt.setRoi(new Roi(a%n*(roi_width),0,roi_width,imp.getHeight()));
							ipFFt.setValue(0);
							ipFFt.fill();
						}
					}				
					bSubstractor.rollingBallBackground(ipFFt, 18, false, false, false, true, false);
					ipFFt = ipFFt.convertToShort(false);
					//ipFFt = ipFFt.convertToByte(true);//(false);
					IJ.save(new ImagePlus("", ipFFt), "d:/"+String.valueOf(slice1)+"-"+String.valueOf(ss)+".tif");
				}
				int xx= (ss%n)*(roi_width);			
				Roi roi = new Roi(xx, 0, roi_width, imp.getHeight());
				ipFFt.setRoi(roi);
				ImageProcessor ip2 = ipFFt.crop();

				//if(ss < 2)
				//IJ.save(new ImagePlus("", ip2), "d:/"+String.valueOf(slice1)+"---"+String.valueOf(ss)+".tif");

				MaximumFinder mFinder = new MaximumFinder();
				ByteProcessor bp = mFinder.findMaxima(ip2, 70, ImageProcessor.NO_THRESHOLD, MaximumFinder.IN_TOLERANCE, true, false);
				Wand wand = new Wand(bp);
				Polygon points = mFinder.getMaxima(ip2, 70, true);
				for(int i=0;i<points.npoints;i++)
				{
					if(points.xpoints[i]<2 || points.xpoints[i]>=roi_width-2)
						continue;
					wand.autoOutline(points.xpoints[i], points.ypoints[i]);
					int[] xpoints = wand.xpoints;
					for(int j=0;j<xpoints.length;j++)
						xpoints[j] = xpoints[j] + ss*(roi_width + 6);
					Roi pRoi = new PolygonRoi(xpoints, wand.ypoints, wand.npoints, Roi.FREEROI);

					if(pRoi.getBounds().height > minPeakDist*0.5 || pRoi.getBounds().width > minPeakDist*0.5)
						continue;

					pRoi = new PointRoi(points.xpoints[i] + ss*(roi_width+6), points.ypoints[i]);
					//PointRoi p = new PointRoi(ss*(roi_width + 6)+points.xpoints[i],points.ypoints[i]);
					if (number[0]==number[1]) {
						Roi[] toRoitemp = new Roi[number[1]*2];						  
						System.arraycopy(roisSlice1, 0, toRoitemp, 0, number[1]);
						roisSlice1= toRoitemp;
						number[1] *= 2;
					}

					pRoi.setPosition(slice1 + 1);
					roisSlice1[number[0]] = pRoi;
					number[0]++;
					//addRoiToManager(p);
				}
				ipDog.insert(ip2, ss*(roi_width + 6), 0);
			}


			System.out.println("point detection in slice : " + (slice1+1)+" is " + number[0]);
			if(number[0] == 0)
				return null;

			Ins_cellsVector aggregateTimeIndex[] = null;
			if(number[0] > 0)
			{
				Roi[] toRoitemp = new Roi[number[0]];						  
				System.arraycopy(roisSlice1, 0, toRoitemp, 0, number[0]);
				roisSlice1= toRoitemp;						 
				Ins_cell[] cells1 = constructCellsListFromStack(roisSlice1, false);
				if(cells1 != null)
				{
					try {
						Arrays.sort(cells1);
						setCellsNumber(cells1);
						aggregateTimeIndex = constructCellVectors(stackSize,cells1);	
					} catch (Exception e) {
						roisSlice1 = null;
					}
				}
			}else {
				return null;
			}						

			for(int i=0; i < cellsTimeIndex.length; i++)
			{
				Ins_cellsVector actualChannel = cellsTimeIndex[i];
				Ins_cellsVector actualAggChannel = null;
				if(aggregateTimeIndex!=null)
					actualAggChannel = aggregateTimeIndex[i];
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					Ins_cell actualCell = actualChannel.getCell(a);
					if(actualCell == null)
						continue;
					// compute the cell information
					Roi roi = actualCell.getRoi();
					String name = getLabel(roi);
					actualCell.setLabel(name);				
					if(roi instanceof Arrow || roi instanceof PointRoi)
						continue;					
					if(actualAggChannel!=null)
					{
						for(int b = 1; b<=actualAggChannel.getCellVector().size(); b++)
						{
							Ins_cell actualAgg = actualAggChannel.getCell(b);
							if(actualAgg == null)
								break;
							Roi aAggRoi = actualAgg.getRoi();
							if(roi.getBounds().contains(aAggRoi.getBounds()))
							{
								// add potential point roi to current cell, then compute cost function to define if there is a foci
								actualCell.addAggRoi(aAggRoi);
							}
						}
					}					
				}
			}
			ArrayList<Roi> aggRois = new ArrayList<Roi>();	
			for(int i=0; i < cellsTimeIndex.length; i++)
			{
				Ins_cellsVector actualChannel = cellsTimeIndex[i];
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					Ins_cell actualCell = actualChannel.getCell(a);
					if(actualCell == null)
						continue;
					PointRoi[] pRois = actualCell.getAggRoisPoint();
					if(pRois==null)
						continue;
					if(actualCell.getFociState())
					{
						for(Roi r:pRois)
							aggRois.add(r);						
						continue;
					}

					boolean findFoci = false;
					Ins_cell nextCell = actualCell.nextCell;					
					if(nextCell != null)
					{	
						if(nextCell.getAggRoisPoint()!=null)
							findFoci = true;
					}else {
						nextCell = actualCell.nextTopCell;
						if(nextCell!=null)
						{
							if (nextCell.getAggRoisPoint()!=null) {
								findFoci = true;
							}else {
								nextCell = actualCell.nextBottomCell;
								if (nextCell!=null && nextCell.getAggRoisPoint()!=null) {
									findFoci = true;
								}
							}
						}
					}
					if(findFoci)
					{
						for(Roi r:pRois)
							aggRois.add(r);
					}
					actualCell.setFociState(findFoci);
					if(nextCell!=null)
						nextCell.setFociState(findFoci);
				}
			}
			return aggRois.toArray(new Roi[aggRois.size()]);
		}
	
	private void correctErrorCell(Vector<Ins_cell> errorCellsVector, Ins_cellsVector[] cellsTimeIndex, boolean green, boolean red, ImagePlus imp)
	{
		ImageStack ims = imp.getImageStack();
		for(Ins_cell errorCell : errorCellsVector)
		{
			Ins_cell currentCell = errorCell;
			ImageProcessor ip = ims.getProcessor(currentCell.getRoi().getPosition());
			
			String name = errorCell.getName();
			int splitScore = 0;
			int mergeScore = 0;
			
			while (currentCell.previousCell!=null && currentCell.previousCell.getName()!=null && currentCell.previousCell.getName().equals(name)) {
				splitScore++;
				//System.out.println(" current name : " + name + " previous name : " + currentCell.previousCell.getName() + " label : " + currentCell.previousCell.getLabel());
				currentCell = currentCell.previousCell;
			}
			
			Ins_cell mergeStartCell = currentCell.previousCell;
			if (mergeStartCell==null) {
				break;
			}			

			boolean forceMerge = false;
			currentCell = errorCell;
			while (currentCell.nextCell!=null) {
				currentCell = currentCell.nextCell;
				mergeScore++;
				if(mergeScore > splitScore)
				{
					forceMerge = true;
					break;
				}
			}
			
			if(currentCell.nextCell==null && currentCell.nextTopCell==null)
			{
				//System.out.println("two null : mergeScore: " + mergeScore + " splitScore : "+splitScore + " label : " + errorCell.getLabel());
				if(mergeScore>splitScore && green)
				{
					// do the same as no touch null					
					// process of merging cells
					Ins_cell cell1 = mergeStartCell.nextTopCell;
					Ins_cell cell2 = mergeStartCell.nextBottomCell;
					do {
						if(cell1!=null && cell2!=null)
							cell1.mergeCell(cell2,cellsTimeIndex,null);
						cell1 = cell1!=null?cell1.nextCell:null;
						cell2 = cell2!=null?cell2.nextCell:null;
					} while (cell1!=errorCell && cell1!=null && cell2!=null);
					
				}else if (mergeScore<=splitScore && red) {				
					currentCell = errorCell;
					Roi roiCurrent = currentCell.getRoi();				
					ip.setRoi(roiCurrent);
					ImageProcessor ipRoi = ip.crop();
					currentCell.divideCell(ipRoi,cellsTimeIndex);
					do{
						currentCell = currentCell.nextCell;
						if(currentCell == null)
							break;					
						roiCurrent = currentCell.getRoi();
						ip.setRoi(roiCurrent);
						ipRoi = ip.crop();					
						currentCell.divideCell(ipRoi,cellsTimeIndex);
					}while(currentCell!=null);
				}
				
				if(mergeScore>splitScore)
				{
					errorCell.getRoi().setStrokeColor(new Color(0, 255, 0));
				}else if(mergeScore<splitScore){
					errorCell.getRoi().setStrokeColor(new Color(255, 0, 0));
				}else{
					errorCell.getRoi().setStrokeColor(new Color(0, 0, 255));
				}
				continue;
			}
			
			boolean stateDivide = false;
			if(currentCell.nextTopCell!=null && currentCell.nextBottomCell!=null)
			{
				splitScore++;
				currentCell = currentCell.nextTopCell;					
				stateDivide = true;
			}
			
			while(stateDivide && currentCell.nextCell!=null && !currentCell.nextCell.getError())
			{
				currentCell = currentCell.nextCell;
				splitScore++;
			}

			if(currentCell.nextCell!=null && currentCell.nextCell.getError())
			{
				while (currentCell.nextCell!=null) {
					currentCell = currentCell.nextCell;
					mergeScore++;					
				}
			}
			//System.out.println("		mergeScore: " + mergeScore + " splitScore : "+splitScore + " name : " + errorCell.getLabel());
			if(forceMerge || (mergeScore>splitScore && green))
			{
				errorCell.getRoi().setStrokeColor(new Color(0, 255, 0));
				// process of merging cells
				Ins_cell cell1 = mergeStartCell.nextTopCell;
				Ins_cell cell2 = mergeStartCell.nextBottomCell;
				do {
					if(cell1!=null && cell2!=null)
						cell1.mergeCell(cell2, cellsTimeIndex, null);
					cell1 = cell1!=null?cell1.nextCell:null;
					cell2 = cell2!=null?cell2.nextCell:null;
				} while (cell1!=errorCell && cell1!=null && cell2!=null);

				//arrow_num = addArrowToManager(cell1, errorCell, arrow_num);
			}else if (mergeScore<=splitScore && red) {				
				currentCell = errorCell;
				Roi roiCurrent = currentCell.getRoi();				
				ip.setRoi(roiCurrent);
				ImageProcessor ipRoi = ip.crop();
				currentCell.divideCell(ipRoi,cellsTimeIndex);
				
				do{
					currentCell = currentCell.nextCell;
					if(currentCell == null)
						break;					
					roiCurrent = currentCell.getRoi();
					ip.setRoi(roiCurrent);
					ipRoi = ip.crop();			
					currentCell.divideCell(ipRoi,cellsTimeIndex);
//					System.out.println("2.1.1, split cell : " + currentCell.getLabel()  + " split ratio : " + ratio);
				}while(currentCell!=null);
			}
			if(forceMerge||mergeScore>splitScore)
			{
				errorCell.getRoi().setStrokeColor(new Color(0, 255, 0));
			}else if(mergeScore<splitScore){
				errorCell.getRoi().setStrokeColor(new Color(255, 0, 0));
			}else{
				errorCell.getRoi().setStrokeColor(new Color(0, 0, 255));
			}
		}
	}
	
	/**
	 * 
	 * @param list
	 * @param rois
	 * @return construct the cells from the list and rois precomputed
	 */
	private Ins_cell[] constructCellsListFromStack(Roi[] rois,boolean identifyColor)
	{
		int count = rois.length;
		Ins_cell[] cells= new Ins_cell[count];	

		int j=0;

		for(int i=0 ; i< count; i++)
		{				
			Roi roi = rois[i];					
			if(!(roi instanceof Arrow))
			{
				cells[j]= new Ins_cell(roi);
				if(identifyColor && roi.getStrokeColor()!=null && roi.getStrokeColor().equals(filamentColor))
					cells[j].setFilamentState(true, filamentColor);
				
				if(roi instanceof Line)
					cells[j].setEndFlag(true);
				
				j = j+1;
			}
		}
		if (j==0)
		{
			return null;
		}
		Ins_cell[] cells2 = new Ins_cell[j];
		System.arraycopy(cells, 0, cells2, 0, j);
		return cells2;
	}
	
	
	private void setCellsNumber(Ins_cell[] cells)
	{
		int channel_pre = cells[0].getTimeIndex();
		int slice_pre = cells[0].getSlice();
		int num = 1;
		cells[0].setCellNumber(num);			
		for(int i=1 ; i< cells.length; i++)
		{				
			int channel = cells[i].getTimeIndex();
			int slice = cells[i].getSlice();
			if(channel == channel_pre && slice == slice_pre)
			{
				num = num + 1;
				cells[i].setCellNumber(num);
			}else {
				num = 1;
				cells[i].setCellNumber(num);
			}
			channel_pre = channel;
			slice_pre = slice;
		}
	}
	private Ins_cellsVector[] constructCellVectors(int stackSize, Ins_cell[] cells)
	{
		Ins_cellsVector[] cellsVectors = new Ins_cellsVector[stackSize];
		for(int i=0;i<stackSize;i++)
		{
			cellsVectors[i] = new Ins_cellsVector(i);
			cellsVectors[i].setTime("No time");

			for(int j=0 ; j< cells.length; j++)
			{
				if(i==cells[j].getTimeIndex())
				{								
					cellsVectors[i].insertCell(cells[j].getCellNumber(), cells[j]);
					if(cells[j].manulTrackingCell)
					{
						cellsVectors[i].insertTrackingCells(cells[j]);							
					}
				}
			}
		}

		return cellsVectors;
	}
	
	private Roi[] constructRoisSlice(Roi[] roiArray, int slice)
	{
		int count = roiArray.length;
		Roi[] currentRois = new Roi[count];		
		int j=0;
		for (int i=0; i<count; i++) {	
			Roi cellRoi = roiArray[i];
			if(cellRoi.getPosition() != slice)
				continue;
			// get rid off the small cell, but keep the horizontal line
			if(!(cellRoi instanceof PointRoi) && cellRoi.getBounds().height < 0.5*minPeakDist && cellRoi.getBounds().width < 1.5*roi_width)
				continue;
			if (!(cellRoi instanceof Arrow)) {
				cellRoi.setStrokeColor(new Color(255,255,0));
				currentRois[j] = cellRoi;
				j++;
			}			
		}
		if(j==0)
		{
			return null;
		}
		Roi[] sliceRois = new Roi[j];
		System.arraycopy(currentRois, 0, sliceRois, 0, j);
		return sliceRois;
	}
	
	/**
	 * user input with freehand
	 * @param r
	 * @return
	 */
	public ArrayList<Ins_cell> getExamples_cell(int slice, Roi r) {
		ArrayList<Ins_cell> aList = new ArrayList<Ins_cell>(5);
		Ins_cellsVector[] cellsTimeIndex_s = cellsTimeIndex[slice-1];
		int tIndex1 = computeTimeIndex(r.getBounds().x);
		int tIndex2 = computeTimeIndex(r.getBounds().x + r.getBounds().width);
		for(int j=tIndex1; j<=tIndex2; j++)
		{
			Ins_cellsVector channel = cellsTimeIndex_s[j];
			for(int i=0; i<channel.getCellVector().size();i++)
			{
				Ins_cell cell = channel.getCellVector().get(i);
				Roi roi = cell.getRoi();
				int[] x = r.getPolygon().xpoints;
				int[] y = r.getPolygon().ypoints;
				final int n = r.getPolygon().npoints;
				for (int k=0; k<n; k++)
				{
					if(roi.contains(x[k], y[k]))
					{
						aList.add(cell);
						break;
					}
				}
			}
		}
		return aList;
	}
	
	
	
	/**
	 * four cases, case 3 to divide is based on the peak finder, needs to know if it's trans of fluo image, and transform them to eigenSmallest image.
	 * 1, to merge
	 * 2, to divide
	 * 3, to delete
	 * 4, to adjust
	 * @param cellsTimeIndex
	 */
	private void manualCorrection(Ins_cellsVector[] cellsTimeIndex) {
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];
			for(int a=0;a<actualChannel.getCellVector().size();a++)
			{
				//System.out.println(" a : " + a + " vector size : " + actualChannel.getCellVector().size());
				Ins_cell actualCell = actualChannel.getCellVector().get(a);
				Roi roi = actualCell.getRoi();
				int t = roi.getType();
				if(t == Roi.FREELINE)//to adjust
				{
					boolean horizontal = isHorizontalLine(roi);
					boolean mergeLine = isMergedLine(roi);
					if(horizontal)
					{
						int tIndex1 = computeTimeIndex(roi.getBounds().x);
						int tIndex2 = computeTimeIndex(roi.getBounds().x + roi.getBounds().width);
						for(int j=tIndex1; j<=tIndex2; j++)
						{
							Ins_cellsVector channel = cellsTimeIndex[j];
							Ins_cell cell = channel.getCell(1);
							if(cell == null)
								continue;
							if(cell == actualCell)
								cell = channel.getCell(2);
							if(cell == null || cell.getRoi() instanceof PointRoi)
								continue;
							cell.resizeHeadToZero();
						}
						//System.out.println("horizontal line found, tIndex1 is " + tIndex1 + "tIndex 2 is " + tIndex2);
						actualChannel.removeCell(actualCell);
						a = -1;
					}else if(mergeLine)
					{
						int tIndex1 = computeTimeIndex(roi.getBounds().x);
						int tIndex2 = computeTimeIndex(roi.getBounds().x + roi.getBounds().width);
						for(int j=tIndex1; j<=tIndex2; j++)
						{
							Ins_cellsVector channel = cellsTimeIndex[j];
							Ins_cell cellMerge = null;
							Ins_cell cellMerge2 = null;
							for(int b=0;b<channel.getCellVector().size();b++)
							{
								Ins_cell cell1 = channel.getCellVector().get(b);
								if(cell1.getRoi().getType()!=Roi.RECTANGLE && cell1.getRoi().getType()!=Roi.TRACED_ROI)
									continue;
								if(projectRatioLength(cell1, actualCell) > 0.2)
								{
									cellMerge = cell1;									
									if(b+1 < channel.getCellVector().size())
									{
										for(int k=b+1; k<channel.getCellVector().size(); k++)
										{
											Ins_cell bCell = channel.getCellVector().get(k);
											if(bCell.getRoi().getType() != Roi.RECTANGLE && bCell.getRoi().getType() != Roi.TRACED_ROI)
												continue;
											if(projectRatioLength(bCell, actualCell) > 0.2)
											{
												cellMerge2 = channel.getCellVector().get(k);
												break;
											}
										}										
									}
									break;
								}
							}
							if(cellMerge!=null && cellMerge2 != null)
								cellMerge.mergeCell(cellMerge2, cellsTimeIndex, new Color(0,0,255));
						}
						actualChannel.removeCell(actualCell);
						a = -1;
						//System.out.println("Merge line found, tIndex1 is " + tIndex1 + "tIndex 2 is " + tIndex2);
					}else {
						int minX = Integer.MAX_VALUE;
						int maxX = Integer.MIN_VALUE;
						for(int b=0;b<actualChannel.getCellVector().size();b++)
						{
							Ins_cell bctualCell = actualChannel.getCellVector().get(b);
							Roi bRoi = bctualCell.getRoi();
							int type = bRoi.getType();
							//System.out.println("b : " + b + " size : " + actualChannel.getCellVector().size() + " y: " + bRoi.getBounds().y);
							if(type != Roi.RECTANGLE && type!=Roi.TRACED_ROI)
								continue;
							// actual Roi is Line, then compute the ratio on the rectancular Roi
							if(projectRatioLength(bctualCell,actualCell) > 0.2)
							{					
								//when find the rectangular Roi, the bctualCell should be removed from the cellsTimeIndex,
								actualChannel.removeCell(bctualCell);
								if(bRoi.getBounds().x < minX)
									minX = bRoi.getBounds().x;
								if(bRoi.getBounds().width + bRoi.getBounds().x > maxX)
									maxX = bRoi.getBounds().width + bRoi.getBounds().x;
								a = -1;
								b = -1;
								//System.out.println("cell is removed, cell position: " + bctualCell.getRoi().getPosition() + " x: " + bctualCell.getRoi().getBounds().x + " y: " + bctualCell.getRoi().getBounds().y);
							}
						}
						if(minX == Integer.MAX_VALUE && maxX == Integer.MIN_VALUE)
						{
							for(int b=0;b<actualChannel.getCellVector().size();b++)
							{
								Ins_cell bctualCell = actualChannel.getCellVector().get(b);
								Roi bRoi = bctualCell.getRoi();
								int type = bRoi.getType();
								//System.out.println("b : " + b + " size : " + actualChannel.getCellVector().size() + " y: " + bRoi.getBounds().y);
								if(type != Roi.RECTANGLE && type!=Roi.TRACED_ROI)
									continue;
								if(bRoi.getBounds().x < minX)
									minX = bRoi.getBounds().x;
								if(bRoi.getBounds().width + bRoi.getBounds().x > maxX)
									maxX = bRoi.getBounds().width + bRoi.getBounds().x;								
							}
						}						
						//whether the superposed of the rectancular cell is found or not, the Line Roi Cell should be replaced by the correct rectangular one
						actualCell.toRectangular(minX, maxX-minX);
						//System.out.println("Line to Rectangular, cell position: " + actualCell.getRoi().getPosition() + " x: " + actualCell.getRoi().getBounds().x + " y: " + actualCell.getRoi().getBounds().y);
					}
				}else if (t == Roi.OVAL ) {// to Delete
					actualChannel.removeCell(actualCell);
					int tIndex1 = computeTimeIndex(roi.getBounds().x);
					int tIndex2 = computeTimeIndex(roi.getBounds().x + roi.getBounds().width);					
					for(int j=tIndex1; j<=tIndex2; j++)
					{
						if(j >= cellsTimeIndex.length || j < 0)
							continue;
						Ins_cellsVector channel = cellsTimeIndex[j];						
						for(int b=0;b<channel.getCellVector().size();b++)
						{
						
							Ins_cell bctualCell = channel.getCellVector().get(b);
							Roi bRoi = bctualCell.getRoi();
							int type = bRoi.getType();
							//System.out.println("b : " + b + " size : " + actualChannel.getCellVector().size() + " y: " + bRoi.getBounds().y);
							if(type != Roi.RECTANGLE && type!=Roi.TRACED_ROI)// priority for point roi instead of Line roi
								continue;						
							//if(roi.getBounds().y >= bRoi.getBounds().y && roi.getBounds().y <= bRoi.getBounds().y + bRoi.getBounds().height)
							if(maxProjectRatio(bctualCell, actualCell) > 0)
							{
								//when find the rectangular Roi, the bctualCell should be removed from the cellsTimeIndex,
								channel.removeCell(bctualCell);
								b = -1;
							}
						}
					}
					a = -1;
				}else if (t == Roi.FREEROI) {// to merge
					actualChannel.removeCell(actualCell);
					int tIndex1 = computeTimeIndex(roi.getBounds().x);
					int tIndex2 = computeTimeIndex(roi.getBounds().x + roi.getBounds().width);
					for(int j=tIndex1; j<=tIndex2; j++)
					{
						Ins_cellsVector channel = cellsTimeIndex[j];
						Ins_cell cellMerge = null;
						Ins_cell cellMerge2 = null;
						for(int b=0;b<channel.getCellVector().size();b++)
						{
							Ins_cell cell1 = channel.getCellVector().get(b);
							if(cell1.getRoi().getType()!=Roi.RECTANGLE && cell1.getRoi().getType()!=Roi.TRACED_ROI)
								continue;
							if(projectRatioLength(cell1, actualCell) > 0)
							{
								cellMerge = cell1;									
								if(b+1 < channel.getCellVector().size())
								{
									for(int k=b+1; k<channel.getCellVector().size(); k++)
									{
										Ins_cell bCell = channel.getCellVector().get(k);
										if(bCell.getRoi().getType() != Roi.RECTANGLE && bCell.getRoi().getType() != Roi.TRACED_ROI)
											continue;
										if(projectRatioLength(bCell, actualCell) > 0)
										{
											cellMerge2 = channel.getCellVector().get(k);
											break;
										}
									}
								}
								break;
							}
						}
						if(cellMerge!=null && cellMerge2 != null)
							cellMerge.mergeCell(cellMerge2, cellsTimeIndex, new Color(0,0,255));
					}
				}else if (t == Roi.POINT) {					
					actualChannel.removeCell(actualCell);
					for(int b=0;b<actualChannel.getCellVector().size();b++)
					{
						Ins_cell bctualCell = actualChannel.getCellVector().get(b);
						Roi bRoi = bctualCell.getRoi();
						//System.out.println("b : " + b + " size : " + actualChannel.getCellVector().size() + " y: " + bRoi.getBounds().y);
						if(bRoi.getType() != Roi.RECTANGLE && bRoi.getType() != Roi.TRACED_ROI)// priority for point roi instead of Line roi
							continue;						
						// actual Roi is Line, then compute the ratio on the rectancular Roi
						if(roi.getBounds().y >= bRoi.getBounds().y && roi.getBounds().y <= bRoi.getBounds().y + bRoi.getBounds().height)
						{
							ImageProcessor ip = imp.getImageStack().getProcessor(bRoi.getPosition());
							ip.setRoi(bRoi);
							bctualCell.divideCell(ip.crop(),cellsTimeIndex);
							break;
						}
					}
					a = -1;
				}
			}
		}
	}
	
	public static ImageProcessor getSeedsFromPSF(ImageProcessor ip, double sigma) {		
		boolean refineMaskLoG = false;
		boolean refineMaskValid = false;
		boolean whiteObject = true;
		PointSourceDetection pSD = new PointSourceDetection(new ImagePlus("maskV", ip), sigma, refineMaskLoG, refineMaskValid,whiteObject);
		pSD.process(false);		
		ImagePlus mask = pSD.getMaskV();
		ImageProcessor ipSeeds = mask.getProcessor();
		ipSeeds = ipSeeds.convertToByteProcessor();
		ipSeeds.invert();
		ipSeeds.autoThreshold();
		ImagePlus impSeeds = new ImagePlus("maskV", ipSeeds);
		//impSeeds.show();
		int sizeMin = 10;
		int sizeMax = 1000000;
		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
		pAnalyzer.analyze(impSeeds,ipSeeds);
		Roi[] rois = getRoiFromManager();
		ImagePlus imp = IJ.createImage("", "8-bit white", mask.getWidth(), mask.getHeight(), 1);
		ImageProcessor ipParticle = imp.getProcessor();
		ipParticle.setValue(0);;
		for(int i=0;i<rois.length;i++)
		{
			if(rois[i].getBounds().height <= ip.getHeight()*0.06667)//1/15
				continue;
			ipParticle.fill(rois[i]);
		}
		return ipParticle;
	}
	
	
	private void correctSegPSFOnly(Ins_cellsVector[] cellsTimeIndex, ImagePlus imp, int slice) {
		ImageStack ims = imp.getImageStack();		
		ImageProcessor ipSeeds = ims.getProcessor(slice);
		ImageProcessor ipCells = ims.getProcessor(slice+1);
		if(impRef!=null)
		{		
			ipCells = impRef.getImageStack().getProcessor(slice);
		}
		ImageCalculator iCalculator = new ImageCalculator();
		ImagePlus impDiff = iCalculator.run("difference create", new ImagePlus("", ipSeeds), new ImagePlus("", ipCells));
		ImageProcessor ipDiff = impDiff.getProcessor().convertToByte(false);
		ImagePlus impNew = IJ.createImage("cell channel", "8-bit white", imp.getWidth(), imp.getHeight(), 1);
		ipDiff.threshold(1);
		ipDiff.invert();
		
		int sizeMin = 2;
		int sizeMax = Integer.MAX_VALUE;
		ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(0,Measurements.RECT,rt,sizeMin,sizeMax);
		pAnalyzer.analyze(new ImagePlus("", ipDiff),ipDiff);
		float[] cx = rt.getColumn(ResultsTable.ROI_X);
		float[] cWidth = rt.getColumn(ResultsTable.ROI_WIDTH);
		if(cx == null || cx.length == 0)
		{
			System.out.println("		Lineage -> No manual seeds or background is found in slice : " + slice);
			return;
		}
		int[] tIndex = new int[cellsTimeIndex.length];
		for(int i=0;i<tIndex.length; i++)
			tIndex[i] = -1;
		int m=0;
		for(int i=0; i<cx.length;i++)
		{
			int tstart = computeTimeIndex(cx[i]);
			int tend = computeTimeIndex(cx[i]+cWidth[i]);
			for(int t=tstart;t<=tend;t++)
			{
				boolean repeat = false;
				for(int j=0;j<tIndex.length;j++)
				{
					if(tIndex[j] == t)
					{
						repeat = true;
						break;
					}				
				}
				if(!repeat)
				{
					tIndex[m] = t;
					m++;
				}
			}
		}
		int[] temp = new int[m];
		System.arraycopy(tIndex, 0, temp, 0, m);
		tIndex = temp;
		Arrays.sort(tIndex);

		int height = imp.getHeight();
		for(int i=0; i<tIndex.length; i++)
		{
			int timeIndex=tIndex[i];
			if(timeIndex == cellsTimeIndex.length)
				timeIndex = cellsTimeIndex.length - 1;
//			if(slice==7)
//				System.out.println(" slice : " + slice + " t : " + timeIndex);
			Ins_cellsVector actualChannel = cellsTimeIndex[timeIndex];
			ImageProcessor ipChannel = impNew.getProcessor();
			ipChannel.setValue(0);
			if(actualChannel == null)
				break;
			for(int a=0;a<actualChannel.getCellVector().size();a++)
			{
				Ins_cell aCell = actualChannel.getCellVector().get(a);
				ipChannel.fill(aCell.getRoi());
				actualChannel.removeCell(aCell);
				a=-1;
			}
			ipChannel.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			ipDiff.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			ipSeeds.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			ImageProcessor ipDiffChannel = ipDiff.crop();
			ImageProcessor ipSeedsChannel = ipSeeds.crop();
			ipChannel = ipChannel.crop();
//			new ImagePlus("ipchannel before", ipChannel.duplicate()).show();
			for(int x=0;x<ipChannel.getWidth();x++)
			{
				for(int y=0;y<ipChannel.getHeight();y++)
				{
					if( ipDiffChannel.get(x, y)!=0)
						continue;					
					if(ipSeedsChannel.get(x, y)<=0)
					{						
						ipChannel.set(x, y, 255);
					}else {
						ipChannel.set(x, y, 0);
					}
				}
			}
//			System.out.println(" slice : " + slice + " t : " + timeIndex);
////			new ImagePlus("ipchannel seeds", ipSeedsChannel).show();
//			new ImagePlus("ipchannel diff", ipDiffChannel).show();
//			new ImagePlus("ipchannel after", ipChannel).show();
//						
//			IJ.save(new ImagePlus("", ipDiffChannel), "d:/ipDiff-"+i+".tif");
//			IJ.save(new ImagePlus("", ipCorrect), "d:/ipcorrect-"+i+".tif");
//			IJ.save(new ImagePlus("", ipChannelMask), "d:/ipmask-"+i+".tif");
//			IJ.save(new ImagePlus("", ipChannelSeeds), "d:/ipseeds-"+i+".tif");
			
//			if(i==0)
//			{
//				IJ.save(new ImagePlus("", ipChannelMask), "d:/ipChannelMask.tif");
//				IJ.save(new ImagePlus("", ipChannelSeeds), "d:/ipChannelSeeds.tif");
//			}
			

			
			//deleteRoiToManager();			
			ParticleAnalyzer pAnalyzerCell = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
			pAnalyzerCell.analyze(new ImagePlus("", ipChannel),ipChannel);
			//				if(i==0)
			//				{
			//					IJ.save(impSeg, "d:/ipChannelSeg.tif");
			//				}
			if(getRoiManager().getCount() > 0)
			{
				System.out.println("t : " + timeIndex + " count : " + getRoiManager().getCount());
				Roi[] roisSlice = getRoiManager().getRoisAsArray();
				Roi[] roisSliceNew = new Roi[roisSlice.length];
				int u=0;
				for(Roi r:roisSlice)
				{
					Polygon polyRoi = r.getPolygon();
					for(int l=0;l<polyRoi.npoints;l++)
					{
						polyRoi.xpoints[l] = polyRoi.xpoints[l]+timeIndex*(roi_width+6);
					}
					Roi rPolyRoi = new PolygonRoi(polyRoi, Roi.TRACED_ROI);
					rPolyRoi.setPosition(slice);
					roisSliceNew[u] = rPolyRoi;
					u++;
				}					
				Ins_cell[] cells1 = constructCellsListFromStack(roisSliceNew, false);
				if(cells1 != null)
				{
					Arrays.sort(cells1);
					setCellsNumber(cells1);
					for(int n=0 ; n< cells1.length; n++)
					{
						actualChannel.insertCell(cells1[n].getCellNumber(), cells1[n]);
					}
				}		
				deleteRoiToManager();	
			}
				
		}
	}
	
	private void correctSegPSF(Ins_cellsVector[] cellsTimeIndex, ImagePlus imp, int slice) {
		ImageStack ims = imp.getImageStack();
		ImageProcessor ipSeeds = ims.getProcessor(slice);
		ImageProcessor ipCells = ims.getProcessor(slice+1);
		if(impRef!=null)
		{		
			ipCells = impRef.getImageStack().getProcessor(slice);
		}		
		ImageCalculator iCalculator = new ImageCalculator();
		ImagePlus impDiff = iCalculator.run("difference create", new ImagePlus("", ipSeeds), new ImagePlus("", ipCells));
		ImageProcessor ipDiff = impDiff.getProcessor().convertToByte(false);
		ipDiff.threshold(1);
		ipDiff.invert();
		//IJ.saveAsTiff(impDiff, "d:/impDiff.tif");
		//impDiff.show();
		//new ImagePlus("diff-slice-"+slice, ipDiff).show();
		//IJ.save(new ImagePlus("diff-slice-"+slice, ipDiff), "d:/impDiffThreshold.tif");
		
		int sizeMin = 2;
		int sizeMax = (int)(roi_width*imp.getHeight());
		ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(0,Measurements.CENTROID,rt,sizeMin,sizeMax);
		pAnalyzer.analyze(new ImagePlus("", ipDiff),ipDiff);
		float[] cx = rt.getColumn(ResultsTable.X_CENTROID);
		if(cx == null || cx.length == 0)
		{
			System.out.println("		Lineage -> No manual seeds or background is found in slice : " + slice);
			return;
		}
		int[] tIndex = new int[cx.length];
		for(int i=0;i<tIndex.length; i++)
			tIndex[i] = -1;
		int m=0;
		for(int i=0; i<cx.length;i++)
		{
			int t = computeTimeIndex(cx[i]);
			boolean repeat = false;
			for(int j=0;j<cx.length;j++)
			{
				if(tIndex[j] == t)
				{
					repeat = true;
					break;
				}				
			}
			if(!repeat)
			{
				tIndex[m] = t;
				m++;
			}
		}
		int[] temp = new int[m];
		System.arraycopy(tIndex, 0, temp, 0, m);
		tIndex = temp;
		Arrays.sort(tIndex);

		int height = imp.getHeight();
		Fluo_Bac_Tracker fTracker = new Fluo_Bac_Tracker();
		for(int i=0; i<tIndex.length; i++)
		{
			int timeIndex=tIndex[i];
			Ins_cellsVector actualChannel = cellsTimeIndex[timeIndex];
			if(actualChannel == null)
				break;
			for(int a=0;a<actualChannel.getCellVector().size();a++)
			{
				Ins_cell aCell = actualChannel.getCellVector().get(a);
				actualChannel.removeCell(aCell);
				a=-1;
			}
			ipCells.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			
			ImageProcessor ipChannel = ipCells.crop();
			ImageProcessor ipChannelMask = ipCells.crop();
			ImageProcessor ipChannelSeeds = getSeedsFromPSF(ipChannelMask,sigma);
			
			AutoThresholder thresholder = new AutoThresholder();
			ipChannelMask = ipChannelMask.convertToByte(true);
	
			int level = thresholder.getThreshold("Otsu", ipChannelMask.getHistogram());
			// get corrected seeds and mark, then change the seeds and mask, seeds should be white, mask should be black
			ipSeeds.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			ImageProcessor ipCorrect = ipSeeds.crop();
			ipDiff.setRoi(timeIndex*(roi_width+6), 0, roi_width,height);
			ImageProcessor ipDiffChannel = ipDiff.crop();
			
			double meanNewSeed = 0.0;
			int count = 0;
			for(int x=0;x<ipCorrect.getWidth();x++)
			{
				for(int y=0;y<ipCorrect.getHeight();y++)
				{
					if(ipCorrect.get(x, y) == 65535 && ipDiffChannel.get(x, y)==0)
					{
						meanNewSeed+=ipChannelMask.get(x, y);
						count++;
					}
				}
			}
			if(count>0)
				meanNewSeed = meanNewSeed/count;
			if(meanNewSeed > 0)//redo the threshold
			{
				System.out.println("		Lineage -> threshold from Otsu : " + level + " threshold from seeds: " + meanNewSeed*0.8);
				level = (int)(meanNewSeed*0.8)<level?(int)(meanNewSeed*0.8):level;				
			}
			ipChannelMask.threshold(level);

			for(int x=0;x<ipCorrect.getWidth();x++)
			{
				for(int y=0;y<ipCorrect.getHeight();y++)
				{
					if(ipCorrect.get(x, y) == 0)
					{
						ipChannelMask.set(x, y, 0);
						ipChannelSeeds.set(x, y, 255);
					}
					if(ipCorrect.get(x, y) == 65535 && ipDiffChannel.get(x, y)==0)
					{
						ipChannelSeeds.set(x, y, 0); // binary of seeds ip, but 16 bits for the cell ip
						ipChannelMask.set(x, y, 255);
					}
				}
			}
			ipChannelMask.dilate();
			ipChannelMask.erode();			
			ipChannelMask.invert();
			
//			IJ.save(new ImagePlus("", ipDiffChannel), "d:/ipDiff-"+i+".tif");
//			IJ.save(new ImagePlus("", ipCorrect), "d:/ipcorrect-"+i+".tif");
//			IJ.save(new ImagePlus("", ipChannelMask), "d:/ipmask-"+i+".tif");
//			IJ.save(new ImagePlus("", ipChannelSeeds), "d:/ipseeds-"+i+".tif");
			
//			if(i==0)
//			{
//				IJ.save(new ImagePlus("", ipChannelMask), "d:/ipChannelMask.tif");
//				IJ.save(new ImagePlus("", ipChannelSeeds), "d:/ipChannelSeeds.tif");
//			}
			

			ImageFbt impFbt = new ImageFbt(ipChannel, ipChannelMask);
			float[] viscosity = impFbt.computeViscosity(1, 25);
			int[] seedsF;
			
			try {
				seedsF = impFbt.seedsFromBinary(0, ipChannelSeeds,10);
				if(seedsF == null || false)
					continue;
				int[] blobs = impFbt.dilate_ShapeF(seedsF,viscosity ,50, fTracker);
				ImagePlus impSeg  = impFbt.converteLabelToImageB(blobs,"");
				impSeg.getProcessor().threshold(127);// > 100 as the connected region
				impSeg.getProcessor().invert();
				ImageProcessor ipSegmented = impSeg.getProcessor();
				ipSegmented.autoThreshold();
				ParticleAnalyzer pAnalyzerCell = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
				pAnalyzerCell.analyze(impSeg,ipSegmented);
//				if(i==0)
//				{
//					IJ.save(impSeg, "d:/ipChannelSeg.tif");
//				}
				if(getRoiManager().getCount() > 0)
				{
					Roi[] roisSlice = getRoiManager().getRoisAsArray();
					Roi[] roisSliceNew = new Roi[roisSlice.length];
					int u=0;
					for(Roi r:roisSlice)
					{
						Polygon polyRoi = r.getPolygon();
						for(int l=0;l<polyRoi.npoints;l++)
						{
							polyRoi.xpoints[l] = polyRoi.xpoints[l]+timeIndex*(roi_width+6);
						}
						Roi rPolyRoi = new PolygonRoi(polyRoi, Roi.TRACED_ROI);
						rPolyRoi.setPosition(slice);
						roisSliceNew[u] = rPolyRoi;
						u++;
					}					
					Ins_cell[] cells1 = constructCellsListFromStack(roisSliceNew, false);
					if(cells1 != null)
					{
						Arrays.sort(cells1);
						setCellsNumber(cells1);
						for(int n=0 ; n< cells1.length; n++)
						{
							actualChannel.insertCell(cells1[n].getCellNumber(), cells1[n]);
						}
					}
				}
				
			} catch (InterruptedException e) {
				IJ.error("Dilatation segmentation failed");
			}
		}
	}
	

	
	private Roi[] cellsTimeIndexToRois(Ins_cellsVector[] cellsTimeIndex, Roi[] arrayRoi)
	{
		Ins_cell actualCell = null;

		int[] numberRois=new int[3];;

		numberRois[0] = 0; // npoints
		numberRois[1] = 100; //maxpoints
		numberRois[2] = 1; //arrow start number
		arrayRoi = new Roi[numberRois[1]];
		
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				actualCell = actualChannel.getCell(a);
				if (numberRois[0]==numberRois[1]) {
					Roi[] toRoitemp = new Roi[numberRois[1]*2];			  
					System.arraycopy(arrayRoi, 0, toRoitemp, 0, numberRois[1]);
					arrayRoi= toRoitemp;						 
					numberRois[1] *= 2;						  
				}
				arrayRoi[numberRois[0]] = actualCell.getRoi();
				numberRois[0]++;
			}
		}
		Roi[] toRoitemp = new Roi[numberRois[0]];						  
		System.arraycopy(arrayRoi, 0, toRoitemp, 0, numberRois[0]);
		arrayRoi= toRoitemp;
		return arrayRoi;
	}
	
	private Roi[][] combineCellsTimeIndexToRois(Ins_cellsVector[] cellsTimeIndex1, Ins_cellsVector[] cellsTimeIndex2, int slice1)
	{
//		if(cellsTimeIndex1.length!=cellsTimeIndex2.length)
//			return null;	
		Roi[] arrayRoi1 = null;
		Roi[] arrayRoi2 = null;
		if(cellsTimeIndex1==null && cellsTimeIndex2 ==null)
		{
			
		}else if(cellsTimeIndex1==null)
		{
			arrayRoi2 = cellsTimeIndexToRois(cellsTimeIndex2, arrayRoi2);
			arrayRoi1 = new Roi[arrayRoi2.length];
			int i=0;
			for(Roi r:arrayRoi2)
			{
				arrayRoi1[i] = (Roi)r.clone();
				arrayRoi1[i].setPosition(r.getPosition()-1);
			}
		}else if (cellsTimeIndex2 == null) {
			arrayRoi1 = cellsTimeIndexToRois(cellsTimeIndex1, arrayRoi1);
			arrayRoi2 = null;
		}else {
			arrayRoi1 = projectTimeIndex2ToTimeIndex1(cellsTimeIndex1, cellsTimeIndex2, slice1);
			arrayRoi2 = projectTimeIndex2ToTimeIndex1(cellsTimeIndex2, cellsTimeIndex1, slice1+1);
		}		
		Roi[][] roiRes = new Roi[2][];
		roiRes[0] = arrayRoi1;
		roiRes[1] = arrayRoi2;
		return roiRes;
	}
	
	private Roi[] projectTimeIndex2ToTimeIndex1(Ins_cellsVector[] cellsTimeIndex1, Ins_cellsVector[] cellsTimeIndex2, int aSlice) {
		if(cellsTimeIndex1 == null || cellsTimeIndex2 == null)
			return null;

		int[] numberRois=new int[3];
		numberRois[0] = 0; // npoints
		numberRois[1] = 100; //maxpoints
		numberRois[2] = 1; //arrow start number
		Roi[] arrayRoi1 = new Roi[numberRois[1]];
		
		for(int i=0; i < cellsTimeIndex1.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex1[i];
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{			
				Roi roi = actualChannel.getCell(a).getRoi();
				if (numberRois[0]==numberRois[1]) {
					Roi[] toRoitemp = new Roi[numberRois[1]*2];			  
					System.arraycopy(arrayRoi1, 0, toRoitemp, 0, numberRois[1]);
					arrayRoi1= toRoitemp;						 
					numberRois[1] *= 2;						  
				}
				arrayRoi1[numberRois[0]] = roi;
				numberRois[0]++;				
			}
			
			Ins_cellsVector nextSliceChannel = cellsTimeIndex2[i];
			// start from next slice, if a cell is found as non-superposed cell on the current slice, then it should be added to the current slice.
			// we should of course keep all the cells in the current slice
			Ins_cell cellNextSlice = null;
			for(int b = 1; b<=nextSliceChannel.getCellVector().size(); b++)
			{
				cellNextSlice = nextSliceChannel.getCell(b);
				Roi roiNextSlice = cellNextSlice.getRoi();
				if(roiNextSlice instanceof Arrow)
					continue;
				
				boolean superposedCellFound = false;
				Ins_cell actualCell = null;
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					actualCell = actualChannel.getCell(a);				
					Roi roi = actualCell.getRoi();
					if(roi instanceof Arrow)
					{
						superposedCellFound=true;
						continue;
					}
					if(maxProjectRatio(cellNextSlice, actualCell) > 0.1 && !superposedCellFound)
					{
						superposedCellFound = true;
					}
				}

				if(!superposedCellFound)
				{
					if (numberRois[0]==numberRois[1]) {
						Roi[] toRoitemp = new Roi[numberRois[1]*2];			  
						System.arraycopy(arrayRoi1, 0, toRoitemp, 0, numberRois[1]);
						arrayRoi1= toRoitemp;						 
						numberRois[1] *= 2;						  
					}
					arrayRoi1[numberRois[0]] = (Roi)roiNextSlice.clone();

					arrayRoi1[numberRois[0]].setPosition(aSlice);
					numberRois[0]++;
				}
			}
		}
		Roi[] toRoitemp = new Roi[numberRois[0]];						  
		System.arraycopy(arrayRoi1, 0, toRoitemp, 0, numberRois[0]);
		return toRoitemp;
	}

	private Roi[] lineageCellsTimeIndex(Ins_cellsVector[] cellsTimeIndex, Vector<Ins_cell> errorCellsVector, Roi[] arrayRoi)
	{
		Ins_cell actualCell = null;
		Ins_cell nextCell = null;

		int[] numberRois=new int[3];

		numberRois[0] = 0; // npoints
		numberRois[1] = 100; //maxpoints
		numberRois[2] = 1; //arrow start number
		arrayRoi = new Roi[numberRois[1]];
		
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			Ins_cellsVector nextChannel = null;			
			if(i < cellsTimeIndex.length-1)
				nextChannel = cellsTimeIndex[i+1];
			
			//add the last cells in roi manager
			if(nextChannel == null || nextChannel.getCellVector().size() == 0)
			{
				for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
				{
					actualCell = actualChannel.getCell(a);

					if (numberRois[0]==numberRois[1]) {
						Roi[] toRoitemp = new Roi[numberRois[1]*2];			  
						System.arraycopy(arrayRoi, 0, toRoitemp, 0, numberRois[1]);
						arrayRoi= toRoitemp;						 
						numberRois[1] *= 2;						  
					}					  
					arrayRoi[numberRois[0]] = actualCell.getRoi();
					numberRois[0]++;
				}
				break;
			}
			
			boolean noLineage = true;
			//for avoiding the deplacement of cell
			for(int a = 1; a<=actualChannel.getCellVector().size(); a++)
			{
				actualCell = actualChannel.getCell(a);
				if (numberRois[0]==numberRois[1]) {
					Roi[] toRoitemp = new Roi[numberRois[1]*2];			  
					System.arraycopy(arrayRoi, 0, toRoitemp, 0, numberRois[1]);
					arrayRoi= toRoitemp;						 
					numberRois[1] *= 2;						  
				}

				//Roi oRoi = actualCell.getRoi();					  
				arrayRoi[numberRois[0]] = actualCell.getRoi();
				numberRois[0]++;
				
				if(actualCell.getEndFlag())
					continue;
				
				Ins_cell lowerCell = actualChannel.getCell(a+1);
				//System.out.println("number of Roi: " + numberRois[0]);

				if(i==cellsTimeIndex.length-1)
					continue;
					//roiManager.addRoi(actualCell.getRoi());
				
				if(actualCell==null || actualCell.parent==null)
				{
					continue;
				}
				
				if(actualCell.nextCell!=null || actualCell.nextTopCell!=null || actualCell.nextBottomCell!=null)
					continue;				
				
				
				if(addVitrualCell)
				{
					boolean virtual = true;
					for(int n = 1; n<=nextChannel.getCellVector().size(); n++)
					{
						nextCell = nextChannel.getCell(n);
						if(nextCell==null)
							break;						
						if(maxProjectRatio(actualCell, nextCell) > 0 )
						{
							virtual = false;
							break;
						}
					}					
					if(virtual)
					{
						System.out.println(" add virtual cell");
						PolygonRoi actuRoi = (PolygonRoi)actualCell.getRoi();
						int[] x = actuRoi.getPolygon().xpoints;
						int[] y = actuRoi.getPolygon().ypoints;
						int np = actuRoi.getPolygon().npoints;
						for(int xi = 0; xi<x.length;xi++)
							x[xi] = x[xi] + roi_width + blankwidth;						
						PolygonRoi sRoi1 = new PolygonRoi(x, y,np, actuRoi.getType());
						sRoi1.setStrokeColor(new Color(0, 0, 139));
						sRoi1.setPosition(actualCell.getSlice());						
						Ins_cell vCell = new Ins_cell(sRoi1);
						vCell.setAsVirtualCell();
						nextChannel.insertCell(vCell);
					}
				}
	
				// find the first next cell which is not null and has no previous cell
				for(int n = 1; n<=nextChannel.getCellVector().size(); n++)
				{
					nextCell = nextChannel.getCell(n);
					if(nextCell==null)
						break;
					if(nextCell.parent==null)
						break;
				}
				if(nextCell == null || actualCell.parent == null || nextCell.parent != null)					
					continue;
				
				Object[] res = createArrows(actualCell,lowerCell, nextCell, cellsTimeIndex, i, 1,nextChannel.getCellVector().size(),1,errorCellsVector, numberRois, arrayRoi);
				numberRois = (int[])res[0];
				arrayRoi = (Roi[])res[1];
				noLineage = false;
			}
			
			if(noLineage)
				break;
		}

		Roi[] toRoitemp = new Roi[numberRois[0]];						  
		System.arraycopy(arrayRoi, 0, toRoitemp, 0, numberRois[0]);
		arrayRoi= toRoitemp;
		return arrayRoi;
	}
	
	
	private int regroupCell(Ins_cellsVector[] cellsTimeIndex) 
	{
		
		int cellID = 0;
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a=1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);								
				if(actualCell == null || actualCell.parent==null)
					continue;			
				if(actualCell.regrouped())
					continue;				
				boolean groupCell = false;
				//find if the cell has the ancestor and the descendant, 
				while (actualCell!=null) {	
					//case: the current cell is divided from the previous cell, but immediately divided into two cell, only one cell should be marked
					if(actualCell.nextCell == null && actualCell.nextTopCell!=null)
					{
						groupCell = true;
						cellID++;
						break;				
					//case: the current cell doesn't have any descendant, so marked as -1, negative
					}else if (actualCell.nextCell==null && actualCell.nextTopCell == null){
						groupCell = false;
						break;
					}else {
						actualCell = actualCell.nextCell;
					}					
				}	
				
				//mark the cellID, if have no descendant, mark -1;
				actualCell = actualChannel.getCell(a);
				while (actualCell!=null) {
					if(groupCell)
					{						
						actualCell.setCellID(cellID);
					}
					else {
						actualCell.setCellID(-1);
					}
					actualCell = actualCell.nextCell;					
				}
			}
		}
		
		int count = 0;
		cellID = 0;
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a=1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);								
				if(actualCell == null || actualCell.parent==null)
					continue;
				count++;
				cells.add(actualCell);
				if(actualCell.nextCell!=null)
					actualCell.setDivideNext(0);
				else if (actualCell.nextTopCell!=null) {
					actualCell.setDivideNext(1);
				}
			}
		}
		return count;
	}
	
	private void assignGrowthRate(Ins_cellsVector[] cellsTimeIndex)
	{
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];		
			for(int a=1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);								
				if(actualCell == null || actualCell.parent==null)
					continue;				
				double growth_rate  = 1.2d;
				
				// 0, exceptionally if cell has one previousLowerCell, means an error, so put the value negative
				// 1, cell has next cell, 
				// 2, cell has next top and bottom cell, 
				// 3, cell has only next top cell 
				// 4, cell has NO next, no bottom, neither top cell,
				if(actualCell.nextCell!=null && actualCell.nextCell.previousLowerCell!=null)
				{
					growth_rate = Math.max((double)actualCell.nextCell.getLength()/(double)actualCell.getLength(), (double)actualCell.nextCell.getLength()/(double)actualCell.nextCell.previousLowerCell.getLength());
				}else if(actualCell.nextCell!=null)
				{
					growth_rate = (double)actualCell.nextCell.getLength()/(double)actualCell.getLength();
				}else if (actualCell.nextTopCell!=null && actualCell.nextBottomCell!=null) {
					growth_rate = (double)(actualCell.nextTopCell.getLength() + actualCell.nextBottomCell.getLength())/(double)actualCell.getLength();
				}else if(actualCell.previousCell != null){
					growth_rate = actualCell.previousCell.getEstimatedGrowthRate();
				}
				actualCell.setEstimatedGrowthRate(growth_rate);
				
				double presumed_growth_rate = 1.2d;
				if(actualCell.previousCell!=null)
					presumed_growth_rate = actualCell.previousCell.getEstimatedGrowthRate();
				actualCell.setPresumedGrowthRate(presumed_growth_rate);

				double growthDiff = 0;
				double growthCDiff = 0;
				if(actualCell.nextCell!=null)
				{
					growthDiff = actualCell.nextCell.getLength() - actualCell.getLength();
					growthCDiff = actualCell.nextCell.getYcenter() - actualCell.getYcenter();
				}else if (actualCell.nextTopCell!=null && actualCell.nextBottomCell!=null) {
					growthDiff = (double)(actualCell.nextTopCell.getLength() + actualCell.nextBottomCell.getLength())- (double)actualCell.getLength();
					growthCDiff = (double)(actualCell.nextTopCell.getYcenter() + actualCell.nextBottomCell.getYcenter())- (double)actualCell.getYcenter();
				}else if(actualCell.previousCell!=null){
					growthDiff = actualCell.previousCell.getGrowthDiff()*actualCell.getEstimatedGrowthRate();
					growthCDiff = actualCell.previousCell.getGrowthCDiff()*actualCell.getEstimatedGrowthRate();
				}
				actualCell.setGrowthDiff(growthDiff);
				actualCell.setGrowthCDiff(growthCDiff);
				
				
				
				//Open curve fourier descriptor
				
				Roi cellRoi = actualCell.getRoi();
				if(i==13)
				{
					System.out.println("stop");
				}
				if(actualCell.previousCell == null)
					break;
				Roi pRoi = actualCell.previousCell.getRoi();
				Roi nRoi;
				if(pRoi == null)
					break;
				
	
				
				if(actualCell.nextCell!=null)
				{
					nRoi = actualCell.nextCell.getRoi();
				}else if (actualCell.nextBottomCell!=null) {
					nRoi = actualCell.nextBottomCell.getRoi();
				}else{
					actualCell.setFourierDescriptor(actualCell.previousCell.getFourierDescriptor());
					continue;
				}
				
				float[] xPoints = new float[]{(float)(pRoi.getBounds().x + 0.5*pRoi.getBounds().width), (float)(cellRoi.getBounds().x + 0.5*cellRoi.getBounds().width), (float)(nRoi.getBounds().x + 0.5*nRoi.getBounds().width)};
				float[] yPoints = new float[]{(float)(pRoi.getBounds().y + 0.5*pRoi.getBounds().height), (float)(cellRoi.getBounds().y + 0.5*cellRoi.getBounds().height), (float)(nRoi.getBounds().y + 0.5*nRoi.getBounds().height)};
				
				PolygonRoi roi = new PolygonRoi(xPoints, yPoints, 3, PolygonRoi.POLYLINE);
				FloatPolygon poly = roi.getInterpolatedPolygon(3, false);
				roi = new PolygonRoi(poly,PolygonRoi.POLYLINE);
				int n = roi.getNCoordinates();
				double[] x = new double[n-1];
				double[] y = new double[n-1];

				int[] xp = roi.getXCoordinates();
				int[] yp = roi.getYCoordinates();

				for (int j = 0; j < n - 1; j++){
					double delta = Math.sqrt((xp[j+1]-xp[j])*(xp[j+1]-xp[j])+(yp[j+1]-yp[j])*(yp[j+1]-yp[j]));
					x[j] = (double)(xp[j+1]-xp[j])/delta;
					y[j] = (double)(yp[j+1]-yp[j])/delta;
				}
				OpenCurveFD efd = new OpenCurveFD(x, y, nFD);
				actualCell.setFourierDescriptor(efd.openCurveFd);				
				if(i==1)
				{
					if(actualCell.previousCell!=null)
						actualCell.previousCell.setFourierDescriptor(actualCell.getFourierDescriptor());
				}
				//System.out.println("compute fourier descriptor");
			}
			//System.out.println(i + " compute fourier descriptor");
		}
	}
	
	
	
	private Ins_cellsVector[] reinitialCellsTimeIndex(Ins_cellsVector[] cellsTimeIndex,boolean identifyColor) {
		Vector<Roi> rois = new Vector<Roi>(10);
		for(int i=0; i < cellsTimeIndex.length; i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];
			for(Ins_cell cell:actualChannel.getCellVector())
			{
				rois.add(cell.getRoi());
			}
		}
		Roi[] rois2 = rois.toArray(new Roi[rois.size()]);
		Ins_cell[] cells = constructCellsListFromStack(rois2, identifyColor);;		
		
		if(cells == null)
			return null;
		Arrays.sort(cells);
		setCellsNumber(cells);		
		cellsTimeIndex = constructCellVectors(stackSize,cells);
		Ins_cell rootCell = cellsTimeIndex[0].getCell(1);		
		if(rootCell == null)
			return null;		
		String rName="A";//root name of the cell
		rootCell.setName(rName);
		Ins_cell rootParent =new Ins_cell(null); 
		rootCell.parent = rootParent;
		
		if(allCellsInFirstChannel){
			String name = "New";
			for(int a=2;a<=cellsTimeIndex[0].getCellVector().size();a++)
			{
				if(true){
					switch (a) {
					case 2:
						name = "B";
						break;
					case 3:
						name = "C";
						break;
					case 4:
						name = "D";
						break;
					case 5:
						name = "E";
						break;
					default:
						name = "No-name";
						break;
					}
					cellsTimeIndex[0].getCell(a).setName(name);
				}				
				cellsTimeIndex[0].getCell(a).parent = new Ins_cell(null);
			}
		}
		
		return cellsTimeIndex;
	}
	private void filamentDetection(Ins_cellsVector[] cellsTimeIndex, int filamentLength) 
	{
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a=1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);								
				if(actualCell == null || actualCell.parent==null)
					continue;			
				
				if(actualCell.filamentChecked())
					continue;
				
				while(actualCell!=null)
				{					
					if(actualCell.filamentChecked())
						break;
															
					if(actualCell.getLength() > filamentLength)
					{	
						actualCell.setFilamentState(true, filamentColor);						
						//forward
						Ins_cell nc = actualCell.nextCell;
						while(nc!=null)
						{
							nc.setFilamentState(true, filamentColor);
							nc = nc.nextCell;
						}						
						//backward
						Ins_cell pc	= actualCell.previousCell;						
						while (pc!=null && pc.nextCell!=null&&pc.nextTopCell==null) {							
							pc.setFilamentState(true, filamentColor);							
							pc = pc.previousCell;
						}						
						break;
					}else {
						actualCell.setFilamentState(false, filamentColor);
					}					
					actualCell = actualCell.nextCell;
				}			
			}	
		}
	}
	
	private Object[] createArrows(Ins_cell actualCell, Ins_cell lowerCell, Ins_cell nextCell,Ins_cellsVector cellsTimeIndex[], int actualTimeIndex, int depth, int currentVectorSize, int direction, Vector<Ins_cell> errorCellsVector, int[] n, Roi[] arrayRoi)
	{
		if(actualCell == null || nextCell== null || actualCell.parent==null || nextCell.parent!=null || actualTimeIndex+1>=cellsTimeIndex.length)
		{
			return new Object[]{n, arrayRoi};
		}				
				
		int slice_cell = actualCell.getRoi().getPosition();
		Color actualPointNextColor = actualCell.getPointNextColor();
		if(actualPointNextColor == null)
		{
			actualCell.setPointNextColor(new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255)));
			actualPointNextColor = actualCell.getPointNextColor();
		}		
		
		
		if(actualCell.getLength() < 1.055*nextCell.getLength() || (actualCell.getLength() <= 1.5*nextCell.getLength() &&centerMovement(actualCell, nextCell) <= 0.1))//in case sometimes the next cell can be lightly smaller than the current cell, decision: the current cell is not divided into two cells, but exception for filament
		{
			actualCell.nextCell = nextCell;
			nextCell.previousCell = actualCell;
			nextCell.parent = actualCell.parent;
			nextCell.setPointNextColor(actualPointNextColor);
			Rectangle r = actualCell.getRoi().getBounds();		
			double ox1 = actualCell.getPosition()[0]+r.width/2+2;
			double oy1 = actualCell.getPosition()[1]+r.height/2;			
			r=nextCell.getRoi().getBounds();				
			double ox2 = nextCell.getPosition()[0]+r.width/2-2;
			double oy2= nextCell.getPosition()[1]+r.height/2;
			Arrow arrow= new Arrow(ox1, oy1, ox2, oy2);
			arrow.setStrokeColor(actualPointNextColor);
			arrow.setHeadSize(2);
			actualCell.arrowToNext = arrow;
			n[2] = setArrowName(slice_cell, arrow, n[2]);

			if (n[0]==n[1]) {
				Roi[] toRoitemp = new Roi[n[1]*2];						  
				System.arraycopy(arrayRoi, 0, toRoitemp, 0, n[1]);
				arrayRoi= toRoitemp;						 
				n[1] *= 2;
			}
			arrayRoi[n[0]] = arrow;
			n[0]++;

			nextCell.name = actualCell.name;				
			nextCell.setifLastCell(currentVectorSize);

			if(lowerCell!=null && lowerCell.parent!=null && 1.6* actualCell.getLength() < nextCell.getLength() && lowerCell.getLength() < nextCell.getLength()&&(actualCell.getLength()+lowerCell.getLength())<nextCell.getLength()*1.2) //if the cell is growed too much faster, can be an error of the lineage
			{
				nextCell.previousLowerCell = lowerCell;	
				lowerCell.nextCell = nextCell;
				nextCell.setError();
				errorCellsVector.add(nextCell);
				r = lowerCell.getRoi().getBounds();									
				ox1 = lowerCell.getPosition()[0]+r.width/2+2;
				oy1 = lowerCell.getPosition()[1]+r.height/2;
				arrow= new Arrow(ox1, oy1, ox2, oy2);
				arrow.setStrokeColor(lowerCell.getPointNextColor());
				arrow.setHeadSize(2);
				lowerCell.arrowToNext = arrow;
				n[2] = setArrowName(slice_cell, arrow, n[2]);
				
					if (n[0]==n[1]) {
						Roi[] toRoitemp = new Roi[n[1]*2];						  
						System.arraycopy(arrayRoi, 0, toRoitemp, 0, n[1]);
						arrayRoi= toRoitemp;						 
						n[1] *= 2;
					}
					arrayRoi[n[0]] = arrow;
					n[0]++;
			}else {
				//System.out.println(" not same parent : " + nextCell.getLabel()+ " topcellparent " + actualCell.getParent().getLabel() + " lowcellparent " + lowerCell.getParent().getLabel());
			}
			return new Object[]{n, arrayRoi};
		}else { // into the case that the current cell can be divided into two cells, but check two particular cases first
			Ins_cell nextCellBottom;
			if(direction == 1)
			{
				nextCellBottom = cellsTimeIndex[actualTimeIndex+1].getCell(nextCell.getCellNumber()+1);
			}
			else {
				nextCellBottom = cellsTimeIndex[actualTimeIndex+1].getCell(nextCell.getCellNumber()-1);
			}			
			
			if((nextCellBottom != null && nextCellBottom.parent==null && (nextCellBottom.getLength() + nextCell.getLength()) > actualCell.getLength() * 1.55) || 
					(nextCellBottom==null && actualCell.getLength() < 1.25*nextCell.getLength()) || (nextCellBottom != null && maxProjectRatio(actualCell, nextCellBottom) == 0)) // add length condition, the total length of the two cells should be longer than the actual cell but not too long
			{
				actualCell.nextCell = nextCell;
				nextCell.previousCell = actualCell;
				nextCell.parent = actualCell.parent;
				nextCell.setPointNextColor(actualPointNextColor);
				Rectangle r = actualCell.getRoi().getBounds();
				double ox1 = actualCell.getPosition()[0]+r.width/2+2;
				double oy1 = actualCell.getPosition()[1]+r.height/2;			
				r=nextCell.getRoi().getBounds();				
				double ox2 = nextCell.getPosition()[0]+r.width/2-2;
				double oy2= nextCell.getPosition()[1]+r.height/2;
				Arrow arrow= new Arrow(ox1, oy1, ox2, oy2);				
				arrow.setStrokeColor(actualPointNextColor);
				arrow.setHeadSize(2);		
				actualCell.arrowToNext = arrow;
				//actualCell.arrowToTop = arrow;
				n[2] = setArrowName(slice_cell, arrow, n[2]);
				
					  if (n[0]==n[1]) {
						  Roi[] toRoitemp = new Roi[n[1]*2];						  
						  System.arraycopy(arrayRoi, 0, toRoitemp, 0, n[1]);
						  arrayRoi= toRoitemp;						 
						  n[1] *= 2;
					  }
					  arrayRoi[n[0]] = arrow;
					  n[0]++;
				
				nextCell.name = actualCell.name;				
				nextCell.setifLastCell(currentVectorSize);				
				return new Object[]{n, arrayRoi}; 
			}
			
			nextCell.parent = actualCell;
			nextCell.previousCell = actualCell;
			actualCell.nextTopCell = nextCell;
			actualCell.nextTopCell.ifdevidedFromPreviousCell = true;
			Color pointNextC = new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255));
			actualCell.nextTopCell.setPointNextColor(pointNextC);
			actualCell.nextTopCell.setifLastCell(currentVectorSize);
			
			Rectangle r = actualCell.getRoi().getBounds();									
			double ox1 = actualCell.getPosition()[0]+r.width/2+2;
			double oy1 = actualCell.getPosition()[1]+r.height/2;					
			r = nextCell.getRoi().getBounds();
			double ox2 = nextCell.getPosition()[0]+r.width/2-2;
			double oy2= nextCell.getPosition()[1]+r.height/2;
			Arrow arrow= new Arrow(ox1, oy1, ox2, oy2);		
			arrow.setStrokeColor(pointNextC);
			arrow.setHeadSize(2);			
			actualCell.arrowToTop = arrow;
			n[2] = setArrowName(slice_cell, arrow, n[2]);			
			
			
				  if (n[0]==n[1]) {
					  Roi[] toRoitemp = new Roi[n[1]*2];						  
					  System.arraycopy(arrayRoi, 0, toRoitemp, 0, n[1]);
					  arrayRoi= toRoitemp;						 
					  n[1] *= 2;
				  }
				  arrayRoi[n[0]] = arrow;
				  n[0]++;
			
			if(direction == 1){				
				actualCell.nextBottomCell =nextCellBottom;
				actualCell.nextTopCell.name = actualCell.name+"H";
			}else {
				if(nextCell.getCellNumber()-1 > 0)
				{
					actualCell.nextBottomCell = nextCellBottom;
				}
				actualCell.nextTopCell.name = actualCell.name+"T";
			}
			
			if(actualCell.nextBottomCell != null && actualCell.nextBottomCell.parent == null)
			{
				actualCell.nextBottomCell.parent = actualCell;
				actualCell.nextBottomCell.previousCell = actualCell;
				actualCell.nextBottomCell.ifdevidedFromPreviousCell = true;				
				actualCell.nextBottomCell.setPointNextColor(actualPointNextColor);
				r = actualCell.nextBottomCell.getRoi().getBounds();
				ox2 = actualCell.nextBottomCell.getPosition()[0]+r.width/2-2;
				oy2= actualCell.nextBottomCell.getPosition()[1]+r.height/2;
				arrow= new Arrow(ox1, oy1, ox2, oy2);	
				arrow.setStrokeColor(actualPointNextColor);
				arrow.setHeadSize(2);
				actualCell.arrowToBottom = arrow;
				n[2] = setArrowName(slice_cell, arrow, n[2]);
				
					  if (n[0]==n[1]) {
						  Roi[] toRoitemp = new Roi[n[1]*2];						  
						  System.arraycopy(arrayRoi, 0, toRoitemp, 0, n[1]);
						  arrayRoi= toRoitemp;						 
						  n[1] *= 2;
					  }
					  arrayRoi[n[0]] = arrow;
					  n[0]++;
								
				if(direction == 1){
					actualCell.nextBottomCell.name = actualCell.name+"T";
				}
				else {
					actualCell.nextBottomCell.name = actualCell.name+"H";
				}													
				actualCell.nextBottomCell.setifLastCell(currentVectorSize);
			}			
			return new Object[]{n, arrayRoi};
		}
	}
	
	/** Adds the specified ROI to the list. The third argument ('n') will 
	be used to form the first part of the ROI label if it is >= 0. */
	public static int setArrowName(int slice, Roi roi, int n) {
		if (roi==null) return n;
		String label = getLabelArrow(slice, roi, n);
		roi.setName(label);
		return n+1;
	}

	private Vector<Integer> getAllCellSizeinVector(Ins_cellsVector[] cellsTimeIndex) 
	{
		Vector<Integer> v = new Vector<Integer>(100);
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			Ins_cellsVector actualChannel = cellsTimeIndex[i];			
			for(int a=1; a<=actualChannel.getCellVector().size(); a++)
			{
				Ins_cell actualCell = actualChannel.getCell(a);								
				if(actualCell == null || actualCell.parent==null)
					continue;
				v.add(actualCell.getLength());
			}
		}
		return v;
	}
	
	public static void addRoiToManager1(Roi roi, int slice)
	{
		Frame frame = WindowManager.getFrame("ROI Manager");										
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		RoiManager roiManager = (RoiManager)frame;	
		roi.setPosition(slice);
		roiManager.addRoi(roi);
	}
	
	public static void saveRoiToZip(Roi[][] rois, String path)
	{
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			for(int j=0;j<rois.length;j++)
			{
				Roi[] rois2 = rois[j];
				if(rois2 == null)
					continue;
				for (int i=0; i<rois2.length; i++) {
					Roi roi = rois2[i];
					String label = roi.getName();
					if(label == null)
					{
						label = getLabel(roi);
						roi.setName(label);			
						if(label == null)
							continue;
					}
					//if (true) System.out.println("saveMultiple: "+i+"  "+label+"  "+roi);
					if (!label.endsWith(".roi")) label += ".roi";
					try {
						zos.putNextEntry(new ZipEntry(label));
						re.write(roi);
						out.flush();
					} catch (Exception e) {
						continue;
					}					
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private boolean isMergedLine(Roi roi) {
		if(roi.getBounds().width > 1.5*roi_width && roi.getBounds().height >= minPeakDist)
			return true;
		else {
			return false;
		}
	}

	private boolean isHorizontalLine(Roi roi) {
		if(roi.getBounds().width > 1.5*roi_width && roi.getBounds().height < minPeakDist)
			return true;
		else {
			return false;
		}
	}
	
	private double centerMovement(Ins_cell baseCell, Ins_cell nextCell) {
		Roi lowerRoi = baseCell.getRoi();
		Roi nextRoi = nextCell.getRoi();
		int y1 = lowerRoi.getBounds().y;
		int y2 = nextRoi.getBounds().y;
		int l1 = lowerRoi.getBounds().height;
		int l2 = nextRoi.getBounds().height;
		double s = Math.abs(y1 + 0.5*l1 - y2 - 0.5*l2);
		return s>0?(double)s/(double)l1:0;
	}
	
	private double projectRatioLength(Ins_cell baseCell, Ins_cell nextCell) {
		Roi lowerRoi = baseCell.getRoi();
		Roi nextRoi = nextCell.getRoi();
		int y1 = lowerRoi.getBounds().y;
		int y2 = nextRoi.getBounds().y;
		int l1 = lowerRoi.getBounds().height;
		int l2 = nextRoi.getBounds().height;
		int minY = Math.min(y1, y2);
		int maxY = Math.max(y1+l1, y2+l2);
		int s = maxY-minY - Math.abs(y1-y2)-Math.abs(y1+l1-y2-l2);		
		return s>0?(double)s/(double)l1:0;
	}
	
	private double maxProjectRatio(Ins_cell lowerCell, Ins_cell nextCell) {
		Roi lowerRoi = lowerCell.getRoi();
		Roi nextRoi = nextCell.getRoi();
		int y1 = lowerRoi.getBounds().y;
		int y2 = nextRoi.getBounds().y;
		int l1 = lowerRoi.getBounds().height;
		int l2 = nextRoi.getBounds().height;
		int minY = Math.min(y1, y2);
		int maxY = Math.max(y1+l1, y2+l2);
		double s = maxY-minY - Math.abs(y1-y2)-Math.abs(y1+l1-y2-l2);		
		s = s>0?s:0;
		s = Math.max(s/(double)l1, s/(double)l2);
		return s;
	}
	
	public static Roi[] getRoiFromManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");										
		if (frame==null || !(frame instanceof RoiManager))
		{return null;}
		RoiManager roiManager = (RoiManager)frame;							
		Roi[] roisArrayRois = roiManager.getRoisAsArray();
		return roisArrayRois;
	}
	
	public static void addRoiToManager(Roi roi)
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
		{
			IJ.run("ROI Manager...");
		}
		frame = WindowManager.getFrame("ROI Manager");		
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		RoiManager roiManager = (RoiManager)frame;	
		if (roi==null) return;
		roiManager.addRoi(roi);
	}
	
	public static void deleteRoiToManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
		{
			IJ.run("ROI Manager...");
		}
		frame = WindowManager.getFrame("ROI Manager");		
//		if (frame==null || !(frame instanceof RoiManager))
//		{return;}
		RoiManager roiManager = (RoiManager)frame;	
		if(roiManager.getCount() != 0)
		{
			System.out.println(" roi manage run delete count : " + roiManager.getCount());
			roiManager.runCommand("delete");
		}
	}
	public static String getLabelArrow(int slice, Roi roi, int n) {				
		int yc  = 0;
		if (n>=0)
			yc=n;		
		if (yc<0) yc = 0;
		int digits = 4;				
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();
		ys = "000000" + yc;
		String label = ys.substring(ys.length()-digits) + "." + "Arrow";
		String zs = "000000" + slice;
		label = zs.substring(zs.length()-digits) + "." + label;
		roi.setPosition(slice);
		return label;
	}
	
	public static String getLabelArrow(int slice, Roi roi) {				
		Rectangle r = roi.getBounds();
		int xc = r.x + r.width/2;
		int yc = r.y + r.height/2;
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;		
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();
		xs = "000000" + xc;
		ys = "000000" + yc;
		String label = xs.substring(xs.length()-digits) + "." + ys.substring(ys.length()-digits) + "." + "Arrow";
		String zs = "000000" + slice;
		label = zs.substring(zs.length()-digits) + "." + label;
		roi.setPosition(slice);
		return label;
	}
	
	public static String getLabelPoint( Roi roi) {				
		Rectangle r = roi.getBounds();
		int slice = roi.getPosition();
		int xc = r.x;
		int yc = r.y;
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;		
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();
		xs = "000000" + xc;
		ys = "000000" + yc;
		String label = xs.substring(xs.length()-digits) + "." + ys.substring(ys.length()-digits) + "." + "Point";
		String zs = "000000" + slice;
		label = zs.substring(zs.length()-digits) + "." + label;
		roi.setPosition(slice);
		return label;
	}
	
	public static String getLabel(Roi roi) {
		int slice = roi.getPosition();
		Rectangle r = roi.getBounds();
		int xc = r.x + r.width/2;
		int yc = r.y + r.height/2;
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();		
		xs = "000000" + xc;
		ys = "000000" + yc;
		String label = ys.substring(ys.length()-digits) + "-" + xs.substring(xs.length()-digits);
		String zs = "000000" + slice;
		label = zs.substring(zs.length()-digits) + "-" + label;
		return label;
	}
	public static int computeTimeIndex(double x)
	{
		int timeIndex = (int)(x/(roi_width + blankwidth));//((int) x)/(roi_width + blankwidth);
		if(timeIndex < 0)
			timeIndex = 0;
		return timeIndex;
	}
	public RoiManager getRoiManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)						
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");
		if (frame==null || !(frame instanceof RoiManager))
			{return null;}
		RoiManager roiManager = (RoiManager)frame;
		roiManager.setVisible(true);	
		return roiManager;
	}

	public int getCellsCount() {
		return cell_counter;
	}
	
	public ArrayList<Ins_cell> getAllCells(){
		return cells;
	}
	
	public Ins_cell getCell(int index)
	{
		return cells.get(index);
	}

	public boolean[] getStateOfCellsTimeIndex() {
		boolean[] emptySlice = new boolean[cellsTimeIndex.length];
		Arrays.fill(emptySlice, false);
		for(int i=0;i<cellsTimeIndex.length;i++)
		{
			if(cellsTimeIndex[i] == null)
			{
				System.out.println("Lineage, empty slice : " + (i+1));
				emptySlice[i] = true;
			}
		}
		return emptySlice;
	}
	
	public static double[] getRowMedianProfile(Rectangle rect, ImageProcessor ip) {
		double[][] profile = new double[rect.height][rect.width];
		double[] aLine;
		ip.setInterpolate(false);
		for (int x=rect.x,j=0; x<rect.x+rect.width; x++,j++) {
			aLine = ip.getLine(x, rect.y, x, rect.y+rect.height-1);
			for (int i=0; i<rect.height; i++) {
				if (!Double.isNaN(aLine[i])) {
					profile[i][j] = aLine[i];
				}
			}
		}
		double[] profileMedian = new double[rect.height];
		for (int i=0; i<rect.height; i++)
		{
			Arrays.sort(profile[i]);
			double median;
			if (profile[i].length % 2 == 0)
			    median = ((double)profile[i][profile[i].length/2] + (double)profile[i][profile[i].length/2 - 1])/2;
			else
			    median = (double) profile[i][profile[i].length/2];
			profileMedian[i] = median;
		}
		return profileMedian;
	}

}
