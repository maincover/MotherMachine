
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Arrow;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.gui.YesNoCancelDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.ImageCalculator;
import ij.plugin.Thresholder;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import ij.util.Java2;
import ij.util.Tools;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.GeneralPath;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import psf.PointSourceDetection;
import lineage.Ins_cell;
import lineage.Lineage;
import watershed2D.ImageBuilder;
import Denoise.ROF_Denoise;
import Fourier.shape.EllipticFD;
import Fourier.shape.EllipticFD_;
import Ij_Plugin.Ins_ParticleAnalyzer;
import Ij_Plugin.Ins_find_peaks;
import Stabilizer.Ins_Stabilizer_Log_Applier;
import Stabilizer.Ins_param;
import Threshold.Auto_Local_Threshold;
import Threshold.Auto_Threshold;
import cellst.Image.FftBandPassFilter;
import cellst.Image.ImageFbt;
import cellst.Main.Fluo_Bac_Tracker;



/**
 * http://rsb.info.nih.gov/ij/plugins/download/IP_Demo.java
 * @author xsong
 *
 */
public class Ins_seg_panel extends PlugInFrame implements ActionListener, ItemListener, KeyListener,ChangeListener, WindowListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JPanel panel;
	Label nameLabel;
	int previousID;
	static Frame instance;
	private ImagePlus imp;
	ImagePlus imp_ref;
	ImageProcessor ip_ref;	
	private ImagePlus wholeWImage;
	private ImagePlus impRearranged;
	private ImagePlus impRFP;
	private JCheckBox mEnabelAutoUpdate;
	public JCheckBox mChDirection;
	private JCheckBox mChNoProcessing;
	private JCheckBox mChRFP;
	private JCheckBox mConsiderAllFirstChannelCells;
	private JCheckBox mDetectAggregation1;
	private JCheckBox mModeSilence;
	private JCheckBox mBrightObject;
	private JCheckBox mSortByDate;	
	private JCheckBox m63x;
	private JCheckBox m100x;
	private boolean setM63x = true;
	
	JButton initializationButtion;
	JButton preprocessButton;
	JButton segLineageButton;
	JButton lineageButton;
	
	private JPanel 	mPnOption;
	public JTextField oChHeight;
	public JTextField oChInterLength;
	public JTextField oChNum;
	public JTextField oChStartX;	
	private JTextField oRootName;
	private JTextField oIntervalSlice;
	public JTextField oChStartY;	
	public JTextField oRoiWidth;	
	public JSlider oChRotSlider;
	
	private GridBagConstraints 	mConstraint = new GridBagConstraints();
	private GridBagLayout mLayout = new GridBagLayout();
	private boolean sortBydate = false;
	private boolean brightObjet = false;
	private boolean doSegmentationAfterCutting = false;
	private boolean doDenoise = false;
	private boolean doNormalizeSlice = false;
	private boolean doNormalizeStack = false;	
	private boolean doCombineCfp = false;
	private boolean doFilterSmallAgregate = true;
	private boolean doRunAll = true;
	private boolean doDenoiseBeforeCutting = false;	

	

	boolean roi1Selected = false;
	boolean roi2Selected = false;
	
	boolean aPressedOnce = false;
	boolean aPressedTwice = false;
	
	public static int stackSize = -1;
	public static int minPeakDist = 25;
	public static int widthImp = -1;
	public static int blankwidth = 6;
	public static int roi_width = 26;
	public static int channelNumber = 0;
	public static boolean runOnce = false;
	public static boolean addVirtualCell = false;
	public static boolean refinement = false;
	public static boolean mergeFirst = false;
	public static boolean outputNewBinary = true;
	//public static int startSlicePassDeadCell=0;
	public static int startSliceSuperpose1 = 5;
	public static int iterationTotal = 0;
	public static boolean writeResultTable = false;
	public static double filamentRatio = 2.0d;
	
	private String saveImgName = "";
	boolean mainFrameClosed;
	public double[] intensity;
	public Color filamentColor = new Color(159, 0, 255, 200);
	Ins_paramArray paramsRFP;
	private int shiftHeadSize = 0;
	boolean correctSegmentationBySeeds = false;
	boolean correctSegBySPF = false;
	boolean noLineageAfterCorrection = false;
	
	
	//public double pRFP_angle;

	
	public Ins_seg_panel() {
		super("Bacteria Analyzer @ U1001");		
		if (instance!=null) {
			instance.toFront();
			return;
		}
		Java2.setSystemLookAndFeel();	
		instance = this;
		//addKeyListener(IJ.getInstance());
		setLayout(new FlowLayout());
		panel = new JPanel();		
		nameLabel = new Label("Bacteria Analyzer @ U1001 v0.0",1);				
		panel.setLayout(mLayout);
		
//		addButton("Open Image");
//		addButton("Load ROIs");					
		Color color = new Color(100, 255, 255);
		initializationButtion = new JButton("Initialization (0)");
		initializationButtion.setToolTipText("Open images' folder to set parameters");
		initializationButtion.addActionListener(this);
		
		
		preprocessButton = new JButton("Preprocessing (1)");
		preprocessButton.setToolTipText("Cutting image to generate ss image");
		preprocessButton.setEnabled(false);
		preprocessButton.addActionListener(this);
		
		segLineageButton = new JButton("Seg (& lineage) (2)");
		segLineageButton.setToolTipText("Segmentation (then lineage)");
		segLineageButton.addActionListener(this);
		
		lineageButton = new JButton("Lineage (3)");
		lineageButton.setToolTipText("Independant lineage action");
		lineageButton.addActionListener(this);
		
		addComponent(panel, 0, 0, 1, 1, 4, initializationButtion);
		addComponent(panel, 0, 1, 1, 1, 4, preprocessButton);
		addComponent(panel, 1, 0, 1, 1, 4, segLineageButton);
		addComponent(panel, 1, 1, 1, 1, 4, lineageButton);
		addComponent(panel, 2, 0, 1, 1, 4, addButton("Stack to Images",color));		
		addComponent(panel, 2, 1, 1, 1, 4, addButton("Move ROIs slice"));
		addComponent(panel, 3, 0, 1, 1, 4, addButton("Rotate Image",color));
		addComponent(panel, 3, 1, 1, 1, 4, addButton("Save Image",color));
		addComponent(panel, 4, 0, 1, 1, 4, addButton("Macros",color));
		addComponent(panel, 4, 1, 1, 1, 4, addButton("Process",color));
		addComponent(panel, 5, 0, 1, 1, 4, addButton("Stack to slice",color));		
		addComponent(panel, 5, 1, 1, 1, 4, addButton("Open Image"));		
		addComponent(panel, 6, 0, 1, 1, 4, addButton("Fill roi",color));
		addComponent(panel, 6, 1, 1, 1, 4, addButton("Double&Triple stack",color));
		
		mEnabelAutoUpdate = new JCheckBox("Calibrate RFP image by saving memory", false);//Enable lineage update automatically				
		addComponent(panel, 7, 0, 1, 1, 2,mEnabelAutoUpdate);
		mEnabelAutoUpdate.addItemListener(this);
		
		m63x = new JCheckBox("ZEISS 63X", true);						
		m63x.addItemListener(this);
				
		m100x = new JCheckBox("NIKON 100X", false);				
		m100x.addItemListener(this);
		
		mSortByDate = new JCheckBox("Open images by acquisition time", sortBydate);
		addComponent(panel, 7, 1, 2, 1, 2,mSortByDate);
		mSortByDate.addItemListener(this);
        
    	mPnOption = new JPanel();
		mPnOption.setLayout(mLayout);		
        JPanel mPanelRadio = new JPanel();
        mPanelRadio.setLayout(mLayout);
        oChHeight = new JTextField("570",4);
        oChHeight.setEnabled(true);
        oChNum = new JTextField("22",4);
        oChNum.setEnabled(true);

        oChInterLength = new JTextField("110.6",4);
        oChInterLength.setEnabled(true);
        oChStartX = new JTextField("90",4);
        oChStartX.setEnabled(true);

        oRootName = new JTextField("A",4);
        oRootName.setEnabled(true);
        
        oIntervalSlice= new JTextField("1",4);
        oIntervalSlice.setEnabled(true);
        
        oChStartY= new JTextField("100",4);
        oChStartY.setEnabled(true);
        
        oRoiWidth= new JTextField(String.valueOf(roi_width),4);
        oRoiWidth.setEnabled(true);
        
        setM63xParameter();
        oChRotSlider = new JSlider(-80,80,0);
        oChRotSlider.setMinorTickSpacing(1);
        oChRotSlider.setSnapToTicks(true);
        oChRotSlider.setPaintTicks(true);
        oChRotSlider.setMajorTickSpacing(4);
        //Create the label table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        JLabel lowLabel = new JLabel("-8 degrees");
        lowLabel.setFont(new Font(null, Font.PLAIN, 12));
        JLabel highLabel = new JLabel("8 degrees");
        highLabel.setFont(new Font(null, Font.PLAIN, 12));
        labelTable.put( new Integer(-80), lowLabel);
        labelTable.put(new Integer(0), new JLabel("0"));
        labelTable.put( new Integer(80), highLabel);
        oChRotSlider.setLabelTable( labelTable );
        oChRotSlider.setPaintLabels(true);
        oChRotSlider.setEnabled(true);
        addComponent(panel, 8, 0, 2, 1, 4,oChRotSlider); 
        oChRotSlider.addChangeListener(this);
        addComponent(panel, 9, 0, 1, 1, 4,m63x);
        addComponent(panel, 9, 1, 1, 1, 4,m100x);
        
        addComponent(mPanelRadio, 1, 0, 1, 1, 4,new JLabel("Height: "));
        addComponent(mPanelRadio, 1, 1, 1, 1, 4,oChHeight);
        addComponent(mPanelRadio, 2, 0, 1, 1, 4,new JLabel("Number of channels:"));
        addComponent(mPanelRadio, 2, 1, 1, 1, 4,oChNum);
        addComponent(mPanelRadio, 4, 0, 1, 1, 4,new JLabel("Length between two channels: "));
        addComponent(mPanelRadio, 4, 1, 1, 1, 4,oChInterLength);
        addComponent(mPanelRadio, 5, 0, 1, 1, 4,new JLabel("Start X position:"));
        addComponent(mPanelRadio, 5, 1, 1, 1, 4,oChStartX);
        addComponent(mPanelRadio, 6, 0, 1, 1, 4,new JLabel("Root name:"));
        addComponent(mPanelRadio, 6, 1, 1, 1, 4,oRootName);
        addComponent(mPanelRadio, 7, 0, 1, 1, 4,new JLabel("Interval slice (RFP):"));
        addComponent(mPanelRadio, 7, 1, 1, 1, 4,oIntervalSlice);
        addComponent(mPanelRadio, 8, 0, 1, 1, 4,new JLabel("Start Y position :"));
        addComponent(mPanelRadio, 8, 1, 1, 1, 4,oChStartY);
        addComponent(mPanelRadio, 9, 0, 1, 1, 4,new JLabel("Roi width :"));
        addComponent(mPanelRadio, 9, 1, 1, 1, 4,oRoiWidth);
        JLabel optionTitle = new JLabel("Options");
        optionTitle.setHorizontalAlignment(JLabel.CENTER);        
        addComponent(mPnOption, 0, 0, 2, 1, 4, new JLabel(""));
        addComponent(mPnOption, 1, 0, 2, 1, 4, new JSeparator());
		addComponent(mPnOption, 2, 0, 2, 1, 4, optionTitle);
		addComponent(mPnOption, 3, 0, 2, 1, 4, mPanelRadio);
		mPnOption.setVisible(true);	
		      
        addComponent(panel, 11, 0, 2, 1, 4,mPnOption);
        mChDirection = new JCheckBox("Channel sealed off at top position", true);
        addComponent(panel, 12, 0, 2, 1, 4,mChDirection);        
        mChDirection.addItemListener(this);      
        mChNoProcessing = new JCheckBox("Open calibrated saved images (no processing needed)", false);
        addComponent(panel, 13, 0, 2, 1, 4,mChNoProcessing);        
        mChRFP = new JCheckBox("Result table with the cells' type", true);
        addComponent(panel, 14, 0, 2, 1, 4,mChRFP);      
        mConsiderAllFirstChannelCells = new JCheckBox("Considering all cells in the first channel", true);
        addComponent(panel, 15, 0, 2, 1, 4,mConsiderAllFirstChannelCells); 
        //mDetectAggregation = new JCheckBox("Detect aggregation", true);
        //addComponent(panel, 16, 0, 2, 1, 4,mDetectAggregation); 
        mModeSilence = new JCheckBox("Mode silence", false);
        addComponent(panel, 17, 0, 2, 1, 4,mModeSilence); 
        mBrightObject = new JCheckBox("Bright cell", false);
        addComponent(panel, 18, 0, 2, 1, 4,mBrightObject); 
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));		
		add(panel);
		pack();
		GUI.center(this);		
		this.setVisible(true);
		this.addWindowListener(this);
		mainFrameClosed = false;
	}
	
    public void windowClosing(WindowEvent e) {     	
    	if(!mainFrameClosed)
    	{
    		YesNoCancelDialog yesNO = new YesNoCancelDialog(null, "Close window", "Close bacterial analyzer?");
    		if(yesNO.yesPressed())    		
    		{    			
    			if(imp != null)
    				imp.close();
    			if(imp_ref != null)
    				imp_ref.close();
    			if(wholeWImage != null)
    				wholeWImage.close();
    			if(impRearranged!=null)
    				impRearranged.close();    			
    			this.dispose();
    			mainFrameClosed = true;    			
    		}
    		else {
    			 mainFrameClosed = false;    			 
    		}    		
    	}
    }

    
	/**
	 * Add a component in a panel
	 */
	private void addComponent(JPanel pn, int row, final int col, int width, final int height, int space, JComponent comp) {
		mConstraint.gridx = col;
		mConstraint.gridy = row;
		mConstraint.gridwidth = width;
		mConstraint.gridheight = height;
		mConstraint.anchor = GridBagConstraints.NORTHWEST;
		mConstraint.insets = new Insets(space, space, space, space);
		mConstraint.weightx = IJ.isMacintosh()?90:100;
		//mConstraint.fill = GridBagConstraints.HORIZONTAL;
		mConstraint.fill = GridBagConstraints.HORIZONTAL;
		mLayout.setConstraints(comp, mConstraint);
		pn.add(comp);
	}
	
	JButton addButton(String label) {
		JButton b = new JButton(label);		
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
		return b;
	}
	
	JButton addButton(String label, Color color) {
		JButton b = new JButton(label);
		b.setBackground(color);
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
		//panel.add(b);
		return b;
	}
	
	private void setM63xParameter()
	{
		m63x.setSelected(true);
		m100x.setSelected(false);
		setM63x = true;
		oChHeight.setText("300");
		oChNum.setText("18");
		oChInterLength.setText("72.8");	        
		oChStartX.setText("30");
		oRoiWidth.setText("24");
		oChStartY.setText("140");
	}
	
	private void setM100xParameter()
	{
		m100x.setSelected(true);
		m63x.setSelected(false);			
		setM63x = false;			
		oChHeight.setText("380");
		oChNum.setText("18");           
		oChInterLength.setText("75.9");
		//oChInterLength.setText("75.49");
		//oChInterLength.setText("76.411");
		oChStartX.setText("25");//32
		oRoiWidth.setText("32");
		oChStartY.setText("180");
	}
	

	
	
	public static ImagePlus watershedImgStac(ImagePlus img_Rearranged)
	{		
		if(img_Rearranged == null)
			return null;
		int type=img_Rearranged.getType();
		if(type != ImagePlus.GRAY8)
		{
			IJ.error("Image needs to be binaried");
			return null;
		}
		
		ImageStack imgStack= img_Rearranged.getImageStack();		
		for(int i=0;i<imgStack.getSize();i++)
		{
			ByteProcessor ip = (ByteProcessor)imgStack.getProcessor(i+1);
			ip.erode();			
			ip.dilate();		
			EDM edm=new EDM();		
			edm.setup("watershed", img_Rearranged);		
			edm.run(ip);			
		}
		return new ImagePlus(img_Rearranged.getTitle()+"-watershed", imgStack);		
	}
	
	
	public static void alignment()
	{
		
	}
	
	public static void dilate(ImageProcessor I, int[][] H)	
	{
		int ic = (H[0].length -1)/2;
		int jc = (H.length-1)/2;
		
		ImageProcessor tmp = I.createProcessor(I.getWidth(), I.getHeight());
		
		for(int j=0; j<H.length; j++)
		{
			for(int i=0; i<H[j].length; i++)
			{
				if(H[j][i] > 0)
					tmp.copyBits(I, i-ic, j-jc, Blitter.MAX);
			}
		}
		I.copyBits(tmp, 0, 0, Blitter.COPY);
	}
	
	@SuppressWarnings("rawtypes")
	/**
	 * two files are necessary, one of the binary Image , the other is the original image for computing the vertical edge,
	 * @param currentImp
	 * @return
	 */
	public static Vector[] watershedImp(ImagePlus currentImp)
	{	
		if(currentImp == null)
		{
			currentImp = IJ.openImage();			
		}
		if(currentImp == null)
			return null;
		
		currentImp.show();
		ImagePlus img_Rearranged = currentImp.duplicate();	
		img_Rearranged.setTitle("Imp binary");
		
		int type=img_Rearranged.getType();
		if(type != ImagePlus.GRAY8)
		{
			IJ.error("Image needs to be binaried");
			return null;
		}
		ByteProcessor ip = (ByteProcessor)img_Rearranged.getProcessor();
		img_Rearranged.getProcessor().invertLut();
		img_Rearranged.getProcessor().invert();
		//img_Rearranged.show();
		
		ImagePlus imp_beforeWL = img_Rearranged.duplicate();
		EDM edm = new EDM();
		edm.setup("watershed", img_Rearranged);		
		edm.run(ip);
		
		ImagePlus img_WL = img_Rearranged.duplicate();
		img_WL.getProcessor().copyBits(imp_beforeWL.getProcessor(), 0, 0, Blitter.DIFFERENCE);
		img_WL.setTitle("Imp WL");
		img_WL.getProcessor().invert();
		//img_WL.show();
		
		IJ.showMessage("Now select original image!");
		ImagePlus impOriginal = IJ.openImage();
		if(impOriginal == null || impOriginal.getWidth() != currentImp.getWidth() || impOriginal.getHeight() != currentImp.getHeight())
			return null;
		
		GaussianBlur gBlur = new GaussianBlur();
		gBlur.blurGaussian(impOriginal.getProcessor(), 1.2, 1.2, 0.01);
		ImageProcessor ip1 = impOriginal.getProcessor().duplicate();
		ip1.convolve(new float[]{-1,-1,-1,0,0,0,1,1,1}, 3, 3);
		ImageProcessor ip2 = impOriginal.getProcessor().duplicate();
		ip2.convolve(new float[]{1,1,1,0,0,0,-1,-1,-1}, 3, 3);
		ip1.copyBits(ip2, 0, 0, Blitter.ADD);
		impOriginal.getProcessor().copyBits(ip2, 0, 0,Blitter.COPY);
		impOriginal.show();
		
		//int[][] H_horizontal = {{0, 0, 0}, {1, 1, 1}, {0, 0, 0}};
		//dilate(impOriginal.getProcessor(), H_horizontal);
		ResultsTable rt = new ResultsTable();
		rt.reset();
		int options = 0; // set all PA options false
		int measurements = Measurements.RECT|Measurements.CENTROID;
		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(options, measurements, rt, 0, 2000);
		
		if(!pAnalyzer.analyze(img_WL, (ByteProcessor)img_WL.getProcessor())){				
			// the particle analyzer should produce a dialog on error, so don't make another one
			return null;
		}
		
		float[] x = rt.getColumn(ResultsTable.ROI_X);		
		float[] y = rt.getColumn(ResultsTable.ROI_Y);
		float[] width = rt.getColumn(ResultsTable.ROI_WIDTH);
		float[] height = rt.getColumn(ResultsTable.ROI_HEIGHT);
		float[] centroid_x = rt.getColumn(ResultsTable.X_CENTROID);
		float[] centroid_y = rt.getColumn(ResultsTable.Y_CENTROID);
		
		float[] mean = new float[x.length];
		int[] mean2 = new int[x.length];
		
		ImagePlus impMeanIntensity_otsu = IJ.createImage("Imp WLintensity", "8-bit", ip.getWidth(), ip.getHeight(), 1);
		ImagePlus impMeanIntensity_default = impMeanIntensity_otsu.duplicate();
		ImagePlus impMeanIntensity_yen = impMeanIntensity_otsu.duplicate();
		//ImagePlus impWL = IJ.createImage("Wline", "8-bit", ip.getWidth(), ip.getHeight(), 1);
		
		
		for(int j= 0; j< centroid_x.length;j++)
		{
			for(int shift = (int)centroid_x[j] - 6; shift <= (int)centroid_x[j] + 6; shift ++)
			{
				for(int shift_y = (int) centroid_y[j] - 3; shift_y <= centroid_y[j] + 3; shift_y ++)
				{
					if(impOriginal.getProcessor().getPixel(shift, shift_y) == 255)
						mean[j] = mean[j] + 0;
					else {
						mean[j] = mean[j] + impOriginal.getProcessor().getPixel(shift, shift_y);											
					}
				}
			}
		}

		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		
		for(int i=0; i<x.length; i++)
		{						
			if(mean[i] > max)
				max = mean[i];			
			if(mean[i] < min)
				min = mean[i];
		}
		
		for(int i=0; i<x.length; i++)
		{	
			mean2[i] = (int)(((mean[i]-min)/(max - min)) * 254);
		}

		int[] hist = new int[256];
		for (int i = 0; i < mean2.length; i++) {
			hist[mean2[i]] = hist[mean2[i]] + 1;
		}		
 		AutoThresholder thresholderAuto = new AutoThresholder();
 		
 		int threshold_otsu = thresholderAuto.getThreshold(Method.Otsu, hist);
 		int threshold_default = thresholderAuto.getThreshold(Method.Default, hist);
 		int threshold_yen = thresholderAuto.getThreshold(Method.Yen, hist);
 		System.out.println("Threshold default: " + threshold_default);
 		System.out.println("Threshold otsu: " + threshold_otsu);
 		System.out.println("Threshold yen: " + threshold_yen);
 		Vector<int[]>[] wl = new Vector[mean2.length];
		boolean[] drawS = new boolean[x.length];
 		int numWl = 0;
		ImageProcessor ip_WL = img_WL.getProcessor();		
		for(int px = 0; px < ip_WL.getWidth(); px ++)
			for(int py =0; py < ip_WL.getHeight(); py ++)
			{
				int v = ip_WL.get(px, py);
				if(v == 0)
				{
					for(int j= 0; j< x.length; j++)
					{						
						if( px >= x[j] && px <= x[j]+width[j] && py >= y[j] && py <= y[j] + height[j])
						{
							boolean ifBreak_default = false;
							boolean ifBreak_yen = false;
							boolean ifBreak_otsu = false;
							
							if(mean2[j] < threshold_default)
							{							
								impMeanIntensity_default.getProcessor().set(px, py, (byte)0);
								//ifBreak_default = true;
							}
							if(mean2[j] < threshold_yen)
							{							
								impMeanIntensity_yen.getProcessor().set(px, py, (byte)0);
								//ifBreak_yen = true;
							}							
							if(mean2[j] < threshold_otsu)
							{							
								impMeanIntensity_otsu.getProcessor().set(px, py, (byte)0);
								//ifBreak_otsu = true;
							}else {
								//impWL.getProcessor().set(px,py,(byte)100);
								if(!drawS[j])
								{
									numWl ++;
									drawS[j] = true;
								}

								if(wl[j] == null)
									wl[j] = new Vector<int[]>();
								wl[j].add(new int[]{px,py});
							}							
							if(ifBreak_default&&ifBreak_otsu&&ifBreak_yen)
								break;
						}
					}
				}
			}
		
		ImagePlus img_Rearranged_default = img_Rearranged.duplicate();
		ImagePlus img_Rearranged_Yen = img_Rearranged.duplicate();
		ImagePlus currentImp_default = currentImp.duplicate();
		ImagePlus currentImp_yen = currentImp.duplicate();
		ImagePlus currentImp_otsu = currentImp.duplicate();
		
		//impWL.show();		
		img_Rearranged.getProcessor().copyBits(impMeanIntensity_otsu.getProcessor(), 0, 0, Blitter.MIN);
		currentImp_otsu.getProcessor().copyBits(img_Rearranged.getProcessor(), 0, 0, Blitter.COPY);
		currentImp_otsu.getProcessor().invert();
		currentImp_otsu.setTitle("otsu");
		currentImp_otsu.show();
		
		
		
		img_Rearranged_default.getProcessor().copyBits(impMeanIntensity_default.getProcessor(), 0, 0, Blitter.MIN);
		currentImp_default.getProcessor().copyBits(img_Rearranged_default.getProcessor(), 0, 0, Blitter.COPY);
		currentImp_default.getProcessor().invert();
		currentImp_default.setTitle("default");
		currentImp_default.show();
		
		img_Rearranged_Yen.getProcessor().copyBits(impMeanIntensity_yen.getProcessor(), 0, 0, Blitter.MIN);
		currentImp_yen.getProcessor().copyBits(img_Rearranged_Yen.getProcessor(), 0, 0, Blitter.COPY);
		currentImp_yen.getProcessor().invert();
		currentImp_yen.setTitle("yen");
		currentImp_yen.show();
		
		
		IJ.showMessage(numWl+"/"+wl.length + " watershed lines have been saved!");
		return wl;		
	}
	

	
	
	
	public ArrayList<Roi> segmentationProcessRFPFromSeeds(ImagePlus impToseg)
	{
		if(impToseg == null)
			return null;		
		int interval = 1;
		String title = impToseg.getTitle().toLowerCase();
		if(title.indexOf("triple")!=-1)
			interval = 3;
		else if (title.indexOf("double")!=-1) {
			interval = 2;
		}else {
			interval = 1;
		}
		//impToseg.show();
		ImagePlus impToMark = impToseg;
		int width = impToMark.getWidth();
		int height = impToMark.getHeight();
		int ss = impToMark.getImageStackSize()/interval;
		String[] strs = new String[ss];
		for(int i=0; i<ss; i++)
		{
			strs[i] = "slice - " + String.valueOf(i+1);
		}
		//int minPeakDist = 25;
		boolean spfAsSeg = true;
		double sigma = 2.8;
		int sizeMin = 50;
		boolean[] bools = new boolean[ss];
		GenericDialog genericDialog = new GenericDialog("Segmentation");
		genericDialog.addNumericField("Number of original images : ", stackSize, 0);
		genericDialog.addMessage("Select slices to be segmented");
		genericDialog.addCheckbox("Use PSF as segmentation", spfAsSeg);
		genericDialog.addNumericField("sigma", sigma, 2);
		genericDialog.addNumericField("min cell size", sizeMin, 0);
		genericDialog.addCheckboxGroup(ss,1 ,strs, bools);
		if(!mModeSilence.isSelected())
		{
			genericDialog.showDialog();		
			if (genericDialog.wasCanceled()) return null;
		}
		spfAsSeg = genericDialog.getNextBoolean();
		stackSize = (int)genericDialog.getNextNumber();
		sigma = genericDialog.getNextNumber();		
		sizeMin = (int)genericDialog.getNextNumber();
		boolean all = true;
		for (int i=0; i<strs.length; i++) {
			if (strs[i].length()!=0) {
				bools[i] = genericDialog.getNextBoolean();
				if(bools[i])
					all = false;
				System.out.print(strs[i]+": "+bools[i] + " ");
			}
		}

		int totalSlice = impToMark.getStackSize();
		ImagePlus impDouble = impToseg;
		if(impDouble!=null)
		{
			impToMark = impDouble;	
		}
		FftBandPassFilter fftBandPassFilter = null; 
		if(doFilterSmallAgregate && !spfAsSeg)
		{				
			fftBandPassFilter = new FftBandPassFilter();			
		}

		if(width%stackSize !=0)
		{
			IJ.error("Wrong number of original images"+" width: " + width + " stacksize: "+stackSize);
			return null;
		}			
		roi_width = width/stackSize - 6;			
		int total = totalSlice*stackSize;
		int k= 1;
		IJ.run("ROI Manager...");
		
		ArrayList<Roi> rois = new ArrayList<Roi>();
		int n = 4;
		BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
		
		ImagePlus impPreprocessed = IJ.createImage("fft-"+impToMark.getTitle(), width, height, impToMark.getImageStackSize(), impToMark.getBitDepth());
		ImagePlus impPreprocessedSeeds;
		if(interval > 1)
			impPreprocessedSeeds = IJ.createImage("seeds-"+impToMark.getTitle(), width, height, impToMark.getImageStackSize(), impToMark.getBitDepth());
		else {
			impPreprocessedSeeds = IJ.createImage("seeds-Double-"+impToMark.getTitle(), width, height, impToMark.getImageStackSize()*2, impToMark.getBitDepth());
		}
		float[] viscosity = null;
		Fluo_Bac_Tracker fTracker = new Fluo_Bac_Tracker();
		
		//int sizeMin = 50;
		int sizeMax = 1000000;
		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);

		
		for(int s=1; s<=impToMark.getStackSize(); s = s + interval)
		{
			if(!bools[(s-1)/interval] && !all)
				continue;
			ImageProcessor ip = impToMark.getImageStack().getProcessor(s);
			ImageProcessor ipSeeds = Lineage.getSeedsFromPSF(ip,sigma);
			new ImagePlus("", ipSeeds).show();
			
			ImagePlus impFFt = IJ.createImage("fft", roi_width*n, height, 1, impToMark.getBitDepth());
			ImageProcessor ipFFt = impFFt.getProcessor();

			ImageProcessor ipPreprocessed = impPreprocessed.getImageStack().getProcessor(s);
			if(interval==2)
			{
				ImageProcessor ipFoci = impToMark.getImageStack().getProcessor(s+1);
				ipFoci = getGradientPSF(ipFoci);
				ipFoci.resetMinAndMax();
				impPreprocessed.getImageStack().getProcessor(s+1).insert(ipFoci.convertToShortProcessor(), 0, 0);
			}else if (interval == 3) {
				impPreprocessed.getImageStack().getProcessor(s+1).insert(impToMark.getImageStack().getProcessor(s+1), 0, 0);
				impPreprocessed.getImageStack().getProcessor(s+2).insert(impToMark.getImageStack().getProcessor(s+1), 0, 0);
			}
		
			ImagePlus impSegSlice = IJ.createImage("segSlice", "8-bit white", width, height, 1);
			ImageProcessor ipSegmented = impSegSlice.getProcessor();
			for(int slice=0;slice<stackSize;slice++)
			{
				if(spfAsSeg)
					continue;
				IJ.showStatus("slice: " + s + " timeIndex : " + slice);
				System.out.println("Segmentation - slice: " + s + " timeIndex : " + slice);
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
							ipFFt.setRoi(new Roi(a%n*(roi_width),0,roi_width,height));
							ipFFt.setValue(0);
							ipFFt.fill();
						}
					}
					if(doFilterSmallAgregate)
					{
						fftBandPassFilter.setup("fft band pass filter", impFFt);
						fftBandPassFilter.run(ipFFt);
						//ipFFt = ipFFt.convertToByte(true);
					}
				}
				IJ.showStatus(String.valueOf(k)+"/" + String.valueOf(total));
				int xx= (slice%n)*(roi_width);			
				Roi roiChannel = new Roi(xx, 0, roi_width, height);
				ipFFt.setRoi(roiChannel);
				ImageProcessor ipChannel = ipFFt.crop();
				
				if(doFilterSmallAgregate)
					bSubtracter.rollingBallBackground(ipChannel, 40, false, false, false, true, false);
				
				ipPreprocessed.insert(ipChannel, slice*(roi_width+6), 0);						
				ImagePlus impLocal = new ImagePlus(String.valueOf(xx), ipChannel.duplicate());
				
				ImagePlus impSeg;

				ImageProcessor ipChannelMask = impLocal.getProcessor();
				ipChannelMask = ipChannelMask.convertToByte(true);

				AutoThresholder thresholder = new AutoThresholder();
				int level = thresholder.getThreshold("Otsu", ipChannelMask.getHistogram());
				//				Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();						
				//				local_Threshold.exec(impLocal, "Otsu", 15, 0, 0, false);
				//				ipChannelMask.autoThreshold();
				ipChannelMask.threshold(level);
				ipChannelMask.dilate();
				ipChannelMask.erode();
				ipChannelMask.invert();
				ipSeeds.setRoi(slice*(roi_width+6), 0, roi_width, height);
				ImageProcessor ipChannelSeeds = ipSeeds.crop();
				ImageFbt impFbt = new ImageFbt(ipChannel, ipChannelMask);
				if(viscosity == null)
					viscosity = impFbt.computeViscosity(1, 25);

				boolean debug = s==3 && slice ==10;
				//debug = false;
				if(debug)
				{
					new ImagePlus("channel", ipChannel).show();
					new ImagePlus("channel-mask", ipChannelMask).show();
					new ImagePlus("channel-seeds", ipChannelSeeds).show();
				}				
				int[] seedsF;
				try {
					seedsF = impFbt.seedsFromBinary(0, ipChannelSeeds,10);
					if(seedsF == null)
						continue;
					int[] blobs = impFbt.dilate_ShapeF(seedsF,viscosity ,50, fTracker);
					impSeg  = impFbt.converteLabelToImageB(blobs,"");
					impSeg.getProcessor().threshold(127);// > 100 as the connected region
					impSeg.getProcessor().invert();
					ipSegmented.insert(impSeg.getProcessor(),slice*(roi_width+6), 0);
					if(debug)
					{
						impSeg.show();
					}
				} catch (InterruptedException e) {
					IJ.error("Dilatation segmentation failed");
				}

			}
			
			if(spfAsSeg)
			{
				ipSegmented.insert(ipSeeds, 0, 0);
				ipPreprocessed = ip;
			}
			
			if(getRoiManager().getCount() > 0)
				deleteRoiToManager();
			ipSegmented.autoThreshold();			
			pAnalyzer.analyze(impSegSlice,ipSegmented);
			
			int posRoi = s;
			if(interval == 1)
				posRoi = posRoi*2 - 1;
			
			if(getRoiManager().getCount() > 0)
			{
				Roi[] roisSlice = getRoiManager().getRoisAsArray();
				for(Roi r:roisSlice)
				{
					r.setPosition(posRoi);
					r.setName(null);
					rois.add(r);
				}
			}
			ipPreprocessed.resetMinAndMax();
			impPreprocessedSeeds.getImageStack().getProcessor(posRoi).insert(ipPreprocessed.duplicate(), 0, 0);
			impPreprocessedSeeds.getImageStack().getProcessor(posRoi+1).insert(ipPreprocessed.duplicate(), 0, 0);
			//ipPreprocessed.c;
		}		
		impPreprocessed.show();
		impPreprocessedSeeds.show();
		return rois;
	}
	
	

//	public ArrayList<Roi> segmentationProcessRFPbk(ImagePlus impToseg,Roi[] roisSPF)
//	{		
//		if(impToseg == null)
//			return null;
//		brightObjet = mBrightObject.isSelected();
//		int interval = 1;
//		String title = impToseg.getTitle().toLowerCase();
//		if(title.indexOf("triple")!=-1)
//			interval = 3;
//		else if (title.indexOf("double")!=-1) {
//			interval = 2;
//		}else {
//			interval = 1;
//		}
//		//impToseg.show();
//		ImagePlus impToMark = impToseg;
//		int width = impToMark.getWidth();
//		int height = impToMark.getHeight();
//		int ss = impToMark.getImageStackSize()/interval;
//		String[] strs = new String[ss];
//		for(int i=0; i<ss; i++)
//		{
//			strs[i] = "slice - " + String.valueOf(i+1);
//		}
//		int minPeakDist = 25;
//		boolean[] bools = new boolean[ss];
//		GenericDialog genericDialog = new GenericDialog("Segmentation");
//		genericDialog.addNumericField("Number of original images : ", stackSize, 0);
//		genericDialog.addNumericField("MinHeightDist (PeakFinder) : ", minPeakDist, 3);
//		genericDialog.addMessage("Select slices to be segmented");
//		genericDialog.addCheckboxGroup(ss,1 ,strs, bools);
//		if(!mModeSilence.isSelected())
//		{
//			genericDialog.showDialog();		
//			if (genericDialog.wasCanceled()) return null;
//		}
//		stackSize = (int)genericDialog.getNextNumber();
//		minPeakDist = (int)genericDialog.getNextNumber();
//		boolean all = true;
//		for (int i=0; i<strs.length; i++) {
//			if (strs[i].length()!=0) {
//				bools[i] = genericDialog.getNextBoolean();
//				if(bools[i])
//					all = false;
//				System.out.print(strs[i]+": "+bools[i] + " ");
//			}
//		}
//
//		int totalSlice = impToMark.getStackSize();
//		ImagePlus impDouble = impToseg;
//		if(impDouble!=null)
//		{
//			impToMark = impDouble;	
//		}
//		FftBandPassFilter fftBandPassFilter = null; 
//		if(doFilterSmallAgregate)
//		{				
//			fftBandPassFilter = new FftBandPassFilter();			
//		}
//
//		if(width%stackSize !=0)
//		{
//			IJ.error("Wrong number of original images");
//			return null;
//		}			
//		roi_width = width/stackSize - 6;			
//		int total = totalSlice*stackSize;
//		int k= 1;
//		IJ.run("ROI Manager...");
//		
//		ArrayList<Roi> rois = new ArrayList<Roi>();
//		int n = 4;
//		BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
//		
//		ImagePlus impPreprocessed = IJ.createImage("Preprocessed for seg", width, height, impToMark.getImageStackSize(), impToMark.getBitDepth());
//		
//		for(int s=1; s<=impToMark.getStackSize(); s = s + interval)
//		{
//			if(!bools[(s-1)/interval] && !all)
//				continue;
//			ImageProcessor ip = impToMark.getImageStack().getProcessor(s);					
//			ImagePlus impFFt = IJ.createImage("fft", roi_width*n, height, 1, 16);
//			ImageProcessor ipFFt = impFFt.getProcessor();
//			
//			Roi[] roisSlice = constructRoisSlice(roisSPF, s);
//			Ins_cell[] cells = constructCellsListFromStack(roisSlice, false);	
//			Ins_cellsVector cellsTimeIndex[] = null;
//			if(cells != null)
//			{
//				Arrays.sort(cells);
//				setCellsNumber(cells);
//				cellsTimeIndex = constructCellVectors(stackSize,cells);	
//			}
//			
//			ImageProcessor ipPreprocessed = impPreprocessed.getImageStack().getProcessor(s);
//			for(int slice=0;slice<cellsTimeIndex.length;slice++)
//			{
//				IJ.showStatus("slice: " + s + " current slice : " + slice);
//				if(slice%n == 0)
//				{
//					ipFFt = impFFt.getProcessor();
//
//					for(int a=slice; a<slice+n; a++)
//					{
//						int x= a*(roi_width+6);
//						Roi aroi = new Roi(x, 0, roi_width, height);
//						ip.setRoi(aroi);
//						ImageProcessor ip2 = ip.crop();
//						ipFFt.insert(ip2, a%n*(roi_width), 0);
//						if(a>=stackSize)
//						{
//							ipFFt.setRoi(new Roi(a%n*(roi_width),0,roi_width,height));
//							ipFFt.setValue(0);
//							ipFFt.fill();
//						}
//					}
//					if(doFilterSmallAgregate)
//					{
//						fftBandPassFilter.setup("fft band pass filter", impFFt);
//						fftBandPassFilter.run(ipFFt);
//						ipFFt = ipFFt.convertToByte(true);
//					}
//				}
//				IJ.showStatus(String.valueOf(k)+"/" + String.valueOf(total));
//				int xx= (slice%n)*(roi_width);			
//				Roi roiChannel = new Roi(xx, 0, roi_width, height);
//				ipFFt.setRoi(roiChannel);
//				ImageProcessor ipChannel = ipFFt.crop();
//				bSubtracter.rollingBallBackground(ipChannel, 40, false, false, false, true, false);
//				ImagePlus impLocal = new ImagePlus(String.valueOf(xx), ipChannel);
//				ImageProcessor ipLocal = impLocal.getProcessor();
//				ipLocal = ipLocal.convertToByte(true);
//				Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();						
//				local_Threshold.exec(impLocal, "Otsu", 15, 0, 0, false);
//				ipLocal.autoThreshold();
//				ipLocal.erode();
//				ipLocal.dilate();
//				ipPreprocessed.insert(ipLocal, slice*(roi_width+6), 0);
//				
//				int seg[][] = new int[15][2];
//				int p = 0;
//				Ins_cellsVector actualChannel = cellsTimeIndex[slice];
//				for(int c=1;c<=actualChannel.getCellVector().size();c++)
//				{
//					Ins_cell actualCell = actualChannel.getCell(c);					
//					if(actualCell == null)
//						break;
//					Roi roi = actualCell.getRoi();
//					if(roi instanceof Line || roi instanceof PointRoi || roi instanceof Arrow )
//						continue;
//					Ins_cell bottomCell = actualChannel.getCell(c+1);
//					Roi roiBottom = null;
//					if(bottomCell!=null)
//						roiBottom = bottomCell.getRoi();
//					if(c==1)
//					{
//						if(roiBottom!=null)
//							seg[p][1] = (int)0.5*(roiBottom.getBounds().y + roi.getBounds().y + roi.getBounds().height);
//						else {
//							seg[p][1] = height;
//						}
//					}else if(c<=actualChannel.getCellVector().size() - 1) {
//						Ins_cell topCell = actualChannel.getCell(c-1);
//						Roi roiTop = topCell.getRoi();
//						seg[p][0] =(int)0.5*(roiTop.getBounds().y + roiTop.getBounds().height + roi.getBounds().y);
//						seg[p][1] =(int)0.5*(roi.getBounds().y + roi.getBounds().height + roiBottom.getBounds().y);
//					}else {
//						Ins_cell topCell = actualChannel.getCell(c-1);
//						Roi roiTop = topCell.getRoi();
//						seg[p][0] =(int)0.5*(roiTop.getBounds().y + roiTop.getBounds().height + roi.getBounds().y);
//						seg[p][1] =height;
//					}
//					p = p +1;
//				}
//				int[][] tmp = new int[p][2];
//				System.arraycopy(seg, 0, tmp, 0, p);
//				seg = tmp;
//				
//				int[][] segWithWl = new int[seg.length][2];
//				System.arraycopy(seg, 0, segWithWl, 0, seg.length);
//				
////				for(int m=0;m<seg.length;m++)
////				{
////					Roi r = new Roi(0, seg[m][0], roi_width, seg[m][1] - seg[m][0] + 1);
////					ipLocal.setRoi(r);
////					ImageProcessor roiSeg = ipLocal.crop();
////				}
//				for(int i=0;i<seg.length;i++)
//				{
////					if(seg[i][1] - seg[i][0]<minPeakDist*0.6)
////						continue;
//					Roi roi2 = new Roi(slice*(roi_width+6), seg[i][0], roi_width, seg[i][1] - seg[i][0]);
//					roi2.setPosition(s);		
//					rois.add(roi2);
//				}
//			}
//		}
//		impPreprocessed.show();
//		return rois;
//	}
	
	private ImageProcessor getGradientPSF(ImageProcessor ip){
		double sigma = 2.5;
		boolean refineMaskLoG = false;
		boolean refineMaskValid = false;
		boolean whiteObject = true;
		PointSourceDetection pSD = new PointSourceDetection(new ImagePlus("laplacien", ip), sigma, refineMaskLoG, refineMaskValid,whiteObject);
		pSD.convolutions(false);		
		ImagePlus LoG = pSD.getLoGv();
		ImageProcessor ipLoGv = LoG.getProcessor();
		return ipLoGv;
	}
	
//	public static ImageProcessor getSeedsFromPSF(ImageProcessor ip) {
//		double sigma = 2.5;
//		boolean refineMaskLoG = false;
//		boolean refineMaskValid = false;
//		boolean whiteObject = true;
//		PointSourceDetection pSD = new PointSourceDetection(new ImagePlus("maskV", ip), sigma, refineMaskLoG, refineMaskValid,whiteObject);
//		pSD.process(false);		
//		ImagePlus mask = pSD.getMaskV();
//		ImageProcessor ipSeeds = mask.getProcessor();
//		ipSeeds = ipSeeds.convertToByteProcessor();
//		ipSeeds.invert();
//		ipSeeds.autoThreshold();
//		ImagePlus impSeeds = new ImagePlus("maskV", ipSeeds);
//		//impSeeds.show();
//		int sizeMin = 10;
//		int sizeMax = 1000000;
//		ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
//		pAnalyzer.analyze(impSeeds,ipSeeds);
//		Roi[] rois = getRoiFromManager();
//		ImagePlus imp = IJ.createImage("", "8-bit white", mask.getWidth(), mask.getHeight(), 1);
//		ImageProcessor ipParticle = imp.getProcessor();
//		ipParticle.setValue(0);;
//		for(int i=0;i<rois.length;i++)
//		{
//			if(rois[i].getBounds().height <= ip.getHeight()*0.06667)//1/15
//				continue;
//			ipParticle.fill(rois[i]);
//		}
//		return ipParticle;
//	}
//	
	

	public ArrayList<Roi> segmentationProcessTrans(ImagePlus impToseg)
	{		
		if(impToseg == null)
			return null;

		//FftBandPassFilter.largeDia = 40.0d;
		
		brightObjet = mBrightObject.isSelected();
		int interval = 1;
		String title = impToseg.getTitle().toLowerCase();
		if(title.indexOf("triple")!=-1)
			interval = 3;
		else if (title.indexOf("double")!=-1) {
			interval = 2;
		}else {
			interval = 1;
		}
		//impToseg.show();
		ImagePlus impToMark = impToseg;
		int width = impToMark.getWidth();
		int height = impToMark.getHeight();
		int ss = impToMark.getImageStackSize()/interval;
		String[] strs = new String[ss];
		for(int i=0; i<ss; i++)
		{
			strs[i] = "slice - " + String.valueOf(i+1);
		}
		int tolerance = 85;
		int minPeakDist = 16;
		int meanValueCell = 150;
		//int startY = 30;
		if(brightObjet)
		{	
			tolerance = 20;
			meanValueCell = 254;
		}
		boolean[] bools = new boolean[ss];
		GenericDialog genericDialog = new GenericDialog("Segmentation");
		genericDialog.addNumericField("Number of original images : ", stackSize, 0);
		genericDialog.addNumericField("Tolerence (PeakFinder) : ", tolerance, 3);
		genericDialog.addNumericField("MinHeightDist (PeakFinder) : ", minPeakDist, 3);
		genericDialog.addNumericField("Mean value cell : ", meanValueCell, 3);
		genericDialog.addCheckbox("FFT band pass", doFilterSmallAgregate);
		genericDialog.addMessage("Select slices to be segmented");

		genericDialog.addCheckboxGroup(ss,1 ,strs, bools);

		if(!mModeSilence.isSelected())
		{
			genericDialog.showDialog();		
			if (genericDialog.wasCanceled()) return null;
		}
		stackSize = (int)genericDialog.getNextNumber();
		tolerance = (int)genericDialog.getNextNumber();
		minPeakDist = (int)genericDialog.getNextNumber();
		meanValueCell = (int)genericDialog.getNextNumber();
		boolean fourierReconstruction = false;
		//startY = (int)genericDialog.getNextNumber();
		//seedsTolerence = genericDialog.getNextNumber();

		boolean all = true;
		doFilterSmallAgregate = genericDialog.getNextBoolean();
		for (int i=0; i<strs.length; i++) {
			if (strs[i].length()!=0) {
				bools[i] = genericDialog.getNextBoolean();
				if(bools[i])
					all = false;
				System.out.print(strs[i]+": "+bools[i] + " ");
			}
		}

		int totalSlice = impToMark.getStackSize();
		ImagePlus impDouble = impToseg;//doubleStack(impToseg[0], impToseg[1]);
		if(impDouble!=null)
		{
			//impDouble.show();
			impToMark = impDouble;	
		}


		FftBandPassFilter fftBandPassFilter = null; 
//		if(brightObjet)
//			FftBandPassFilter.filterLargeDia = 40;
		if(doFilterSmallAgregate)
		{				
			fftBandPassFilter = new FftBandPassFilter();			
		}

		if(width%stackSize !=0)
		{
			IJ.error("Wrong number of original images"+" width: " + width + " stacksize: " + stackSize);
			return null;
		}			
		roi_width = width/stackSize - 6;			

		int total = totalSlice*stackSize;
		//		ImageStack imsRes = ImageStack.create(width, height, ss, 8);
		int k= 1;

		IJ.run("ROI Manager...");

		ArrayList<Roi> rois = new ArrayList<Roi>();
		int n = 4;
		BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
		for(int s=1; s<=impToMark.getStackSize(); s = s + interval)
		{
			if(!bools[(s-1)/interval] && !all)
				continue;
			ImageProcessor ip = impToMark.getImageStack().getProcessor(s);		
			//				AutoThresholder autoThresholder = new AutoThresholder();			
			ImagePlus impFFt = IJ.createImage("fft", roi_width*n, height, 1, 16);
			ImageProcessor ipFFt = impFFt.getProcessor();
			for(int slice=0; slice<stackSize;slice++,k++)
			{
				IJ.showStatus("slice: " + s + " current slice : " + slice);
				if(slice%n == 0)
				{
					if(slice+n > stackSize)
						n = stackSize - slice;
					
					ipFFt = impFFt.getProcessor();
					for(int a=slice; a<slice+n; a++)
					{
						int x= a*(roi_width+6);
						Roi roi = new Roi(x, 0, roi_width, height);
						ip.setRoi(roi);
						ImageProcessor ip2 = ip.crop();
						ipFFt.insert(ip2, a%n*(roi_width), 0);
					}
					if(doFilterSmallAgregate)
					{
						fftBandPassFilter.setup("fft band pass filter", impFFt);
						fftBandPassFilter.run(ipFFt);
						ipFFt = ipFFt.convertToByte(true);
					}
					
				}

				boolean debug = false;//slice == 2118/(roi_width+6);

				IJ.showStatus(String.valueOf(k)+"/" + String.valueOf(total));
				int xx= (slice%n)*(roi_width);			
				Roi roi = new Roi(xx, 0, roi_width, height);

				ipFFt.setRoi(roi);
				ImageProcessor ip2 = ipFFt.crop();
				bSubtracter.rollingBallBackground(ip2, 40, false, false, false, true, false);
				

				if(brightObjet)
				{				
					//ip2.invert();
					ImagePlus impLocal = new ImagePlus(String.valueOf(xx), ip2);
					ImageProcessor ipLocal = impLocal.getProcessor();
					ImageProcessor ipLocalOrig = ipLocal.duplicate();
					ipLocal = ipLocal.convertToByte(true);
					Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();						
					local_Threshold.exec(impLocal, "Otsu", 15, 0, 0, false);
					ipLocal.autoThreshold();
					ipLocal.erode();
					//						ipLocal.erode();
					ipLocal.dilate();
					//						ipLocal.dilate();

					if(debug)
					{
						IJ.save(new ImagePlus("", ipLocal),"d:/localThreshold.tif");
						IJ.save(new ImagePlus("", ipLocalOrig), "d:/localOrig.tif");
					}

					int xMin = Integer.MAX_VALUE;
					int xMax = Integer.MIN_VALUE;

					for(int x=0;x<ipLocal.getWidth();x++)
						for(int y=0;y<ipLocal.getHeight()*0.75;y++)//not until the end, because a lot of noise
						{								
							if(ipLocal.get(x, y) == 0)
							{
								if(x < xMin)
									xMin = x;
								if(x > xMax)
									xMax = x;
							}
						}
					


					if(debug)
						System.out.println("\n"+"xMin : " + xMin + " xMax : " + xMax);;
						int seg[][] = new int[15][2];
						int p = 0;
						for(int y=0;y<ipLocal.getHeight();y++)
						{
							for(int x=0;x<ipLocal.getWidth();x++)
							{
								if(p >= seg.length)
									break;
								if(ipLocal.get(x, y) == 0)
								{	

									seg[p][0] = y;									
									for(int y2 =y + 1; y2<ipLocal.getHeight(); y2++)
									{
										boolean foundCell = false;
										for(int x2 = 0;x2<ipLocal.getWidth(); x2++)
										{
											if(ipLocal.get(x2, y2) == 0)
											{	
												foundCell = true;
												break;
											}
										}
										if(!foundCell || y2 == ipLocal.getHeight() - 1)
										{
											seg[p][1] = y2 - 1;
											y = y2;
											if(debug)
												System.out.println("seg p: " + p + " seg[p][0] " + seg[p][0] + " seg[p][1] " + seg[p][1]);
											break;
										}
									}
									p++;
									break;
								}
							}

						}

						int[][] tmp = new int[p][2];
						System.arraycopy(seg, 0, tmp, 0, p);
						seg = tmp;

						int[][] segWithWl = new int[seg.length][2];
						System.arraycopy(seg, 0, segWithWl, 0, seg.length);


						for(int m=0;m<seg.length;m++)
						{
							Roi r = new Roi(xMin, seg[m][0], xMax - xMin + 1, seg[m][1] - seg[m][0] + 1);
							ipLocal.setRoi(r);
							ImageProcessor ipLocalRoi0 = ipLocal.crop();
							ImageProcessor ipLocalRoi1 = ipLocal.crop();							
							if(fourierReconstruction)
							{
								if(debug)
									IJ.save(new ImagePlus("", ipLocalRoi1), "d:/fouriershapeBefore-"+String.valueOf(m)+".tif");
								int startX = 0,startY = 0;
								Wand wand = new Wand(ipLocalRoi1);
								for(int x=0;x<ipLocalRoi1.getWidth();x++)
									for(int y=0;y<ipLocalRoi1.getHeight();y++)
									{
										if(ipLocalRoi1.get(x, y) == 0)
										{
											startX = x;
											startY = y;
											break;
										}
									}
								wand.autoOutline(startX, startY);
								int[] xpoints = wand.xpoints;
								int[] ypoints = wand.ypoints;
								int npoints = wand.npoints;
								double[] x = new double[npoints];
								double[] y = new double[npoints];
								for (int i = 0; i <npoints; i++){
									x[i] = (double) (xpoints[i]);
									y[i] = (double) (ypoints[i]); 
								}
								EllipticFD ellipticFD = new EllipticFD(x, y, 16);
								Roi roiRe = EllipticFD_.getReconstructionRoi(ellipticFD);
								ipLocalRoi1.setValue(255);
								ipLocalRoi1.fill(new Roi(0, 0, ipLocalRoi1.getWidth(), ipLocalRoi1.getHeight()));								
								ipLocalRoi1.setValue(0);
								ipLocalRoi1.fill(roiRe);
								ipLocalRoi0 = ipLocalRoi1.duplicate();
							}

							EDM edm = new EDM();
							edm.setup("watershed", new ImagePlus("", ipLocalRoi1));		
							edm.run(ipLocalRoi1);				
							ipLocalRoi0.copyBits(ipLocalRoi1, 0, 0, Blitter.DIFFERENCE);
							ipLocalRoi0.invert();
							if(debug)
							{
								IJ.save(new ImagePlus("", ipLocalRoi1), "d:/fouriershapeAfter-"+String.valueOf(m)+".tif");
								IJ.save(new ImagePlus("", ipLocalRoi0), "d:/fouriershapeWatershed-"+String.valueOf(m)+".tif");
							}
							int wlNumber = 0;
							int[] wlPos = new int[15];
							for(int y=0;y<ipLocalRoi0.getHeight();y++)
							{

								for(int x=0;x<ipLocalRoi0.getWidth();x++)
								{
									if(wlNumber >= wlPos.length)
										break;

									if(ipLocalRoi0.get(x, y) != 0)
										continue;									

									int nextY = y;
									int sumNextY = y;
									int n1 = 1;
									for(int yy = y + 1; yy<ipLocalRoi0.getHeight(); yy++)
									{
										boolean foundNextMin = false;
										for(int x2 = 0; x2<ipLocalRoi0.getWidth(); x2++)
										{
											if(ipLocalRoi0.get(x2, yy) == 0)
											{
												foundNextMin = true;
												nextY = yy;
												sumNextY += yy;
												n1++;
												break;
											}
										}
										if(!foundNextMin)
										{
											break;
										}
									}									
									wlPos[wlNumber] = (int)((float)sumNextY/(float)n1);
									if(debug)
										System.out.println(" watershed line : "+wlNumber + " y: " + y + " line n1: " + n1+ " line y : " + wlPos[wlNumber]);									
									wlNumber++;
									y = nextY;									
									break;
								}
							}

							if(wlNumber == 0)
								continue;

							ipLocalOrig.setRoi(r);
							ImageProcessor ipLocalRoiOrig = ipLocalOrig.crop();		
							ipLocalRoiOrig.invert();	
							if(debug)
								IJ.save(new ImagePlus("", ipLocalRoiOrig), "d:/ipLocalProfile.tif");
							double[] profile = getProfile1(ipLocalRoiOrig,debug,String.valueOf(m));							
							Ins_find_peaks peakFinder = new Ins_find_peaks(tolerance, minPeakDist);				
							Object[] out = peakFinder.findPeaks(profile, true, 6);
							int position[] = (int[])out[0]; 
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
								continue;

							//if(debug)
							System.out.println("peak found length: "+ position.length + " watershed line length : " + wlNumber);							
							int[] correctWlPos = new int[10];
							int c = 0;
							for(int j=0;j < position.length;j++)
							{
								int yPeak = position[j];
								for(int j1=0; j1<wlPos.length;j1++)
								{
									//if(debug)
									if(Math.abs(yPeak-wlPos[j1]) <= 5) //peak position superposed with watershed line, then this is correct watershedLine
									{
										//System.out.println("peak found j : "+ j + " peak : " + yPeak + " waterhshed line: " + wlPos[j1]);
										correctWlPos[c] = wlPos[j1]  + seg[m][0]; //wlPos[j1] yPeak 
										c++;
									}
								}
							}

							if(c==0) //not found any superposed watershed line and peak position
								continue;

							int[][] newSegPos = new int[c+1][2];
							newSegPos[0] = new int[]{seg[m][0], correctWlPos[0]};
							newSegPos[c] = new int[]{correctWlPos[c-1],seg[m][1]};

							for(int j=1;j<c;j++)
							{
								newSegPos[j] = new int[]{correctWlPos[j-1], correctWlPos[j]};
							}

							int[][] segTmp = new int[seg.length + newSegPos.length - 1][2]; //current m is divided into new seg
							//System.out.println("(1)seg length : " + seg.length + " m : " + m);
							System.arraycopy(seg,0 , segTmp, 0, m);
							//if(debug)
							//System.out.println("(2)newSegPos length : " + newSegPos.length + " m : " + m);
							System.arraycopy(newSegPos, 0, segTmp, m, newSegPos.length);
							//System.out.println("(3)newSegPos length : " + newSegPos.length + " m : " + m);
							System.arraycopy(seg, m+1 , segTmp, m+newSegPos.length, seg.length - m - 1);
							seg = segTmp;
							m = m + 1 + newSegPos.length; 
						}

						for(int i=0;i<seg.length;i++)
						{
							if(seg[i][1] - seg[i][0]<minPeakDist*0.6)
								continue;
							Roi roi2 = new Roi(slice*(roi_width+6), seg[i][0], roi_width, seg[i][1] - seg[i][0]);
							roi2.setPosition(s);
							ipLocalOrig.setRoi(new Roi(xMin, roi2.getBounds().y, xMax - xMin, roi2.getBounds().getHeight()));		
							ImageProcessor ipSeg = ipLocalOrig.crop();
							if(debug)
								IJ.save(new ImagePlus("", ipSeg), "d:/"+"ipLocalMean"+String.valueOf(i)+".tif");
							ImageStatistics iStatistics = ImageStatistics.getStatistics(ipSeg, ImageStatistics.MEAN, null);
							if(iStatistics.mean <= 20)//if(iStatistics.mean <= 100)
								continue;
							roi2.setPosition(s);
							rois.add(roi2);
						}
				}else {
					ImageProcessor ip2Dup = ip2.duplicate();
					BackgroundSubtracter bgSubtracter = new BackgroundSubtracter();
					bgSubtracter.rollingBallBackground(ip2Dup, 10, false, false, false, false, false);
					ImagePlus[] eigenImp = (new featureJ.FJ_Structure()).getEigenImg(new ImagePlus("", ip2Dup),"1.0","3.0");
					ImagePlus impPlotProfile = eigenImp[1]; //smallest Eigen Image
					
					double ratio = 0.3;
					
					//impPlotProfile.setRoi(0, 0, roi_width, height);
					//ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);//sdfqhsdfk
					//double[] profile = pPlot.getProfile();
//					impPlotProfile = convertTo8bits(impPlotProfile,false);
//					if(s==3 && slice == 708/(roi_width + 6))
//					{						
//						ImagePlus ip2Temp = new ImagePlus("Debug - eigenSmall", impPlotProfile.getProcessor());
//						//ip2Temp.setRoi((int)(Math.ceil(roi_width*(1-ratio)*0.5)), 0, (int)(roi_width*ratio), height);
//						//ip2Temp.getProcessor().fill();
//						ip2Temp.show();
////						new ImagePlus("Debug - orig", ip2).show();
//					}
					double[] profile = Ins_seg_preprocessing.getRowMedianProfile(new Rectangle((int)Math.ceil(roi_width*(1-ratio)*0.5), 0, (int)(roi_width*ratio), height), impPlotProfile.getProcessor());
					double[] profile1 = new double[profile.length];
					for(int p=0; p<profile.length; p++)
					{
						double sum = 0;
						for(int q=p-1; q<p+2; q++)
						{
							if(q<0 || q>=profile.length)
								continue;
							sum = sum + profile[q];
						}
						profile1[p] = sum;					
					}
					profile = profile1;
					Ins_find_peaks peakFinder = new Ins_find_peaks(tolerance, minPeakDist);				
					Object[] out = peakFinder.findPeaks(profile, true, 6);
					int position[] = (int[])out[0]; 
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
						position = new int[]{height};
					}else if (position.length == 1) {
						position = new int[]{position[0], height};
					}
					System.out.println("Segmentation at channel - " + s + " slice-" + slice);

					int tLevel = 0;
					for(int j=0;j < position.length;j++)
					{						
						Roi roi2;
						int pos = 0;
						if(j==0)
						{
							roi2 = new Roi(slice*(roi_width+6), 0, roi_width, position[j]); //roi2 = new Roi(x, 0, roi_width, position[j]);						
							if(position.length > 1)
							{
								pos = position[1] - position[0];
								Roi roi3 = new Roi(slice*(roi_width+6), position[0], roi_width, pos,1);//roi2 = new Roi(x, position[j-1], roi_width, pos,1);
								ip.setRoi(new Roi(roi3.getBounds().x + 5, roi3.getBounds().y-5, roi3.getBounds().width-10, roi3.getBounds().height+10));		
								ImageProcessor ipSeg = ip.crop();
								ipSeg = ipSeg.convertToByte(true);	
								tLevel = (int)(Auto_Threshold.getThreshold("Otsu", ipSeg)*1.0);//*1.16
								System.out.println("		slice : " + slice + " default threshold : " + tLevel +" width : " + ipSeg.getWidth() + " height : " + ipSeg.getHeight());
							}

						}else {
							pos = position[j] - position[j-1];
							roi2 = new Roi(slice*(roi_width+6), position[j-1], roi_width, pos,1);//roi2 = new Roi(x, position[j-1], roi_width, pos,1);
						}
						Roi roiRefine = null;
						if(mBrightObject.isSelected())
							roiRefine = refineRoiRFP(roi2,ip,slice*(roi_width+6),j==0,false,tLevel);//refineRoiRFP(roi2,ip,slice*(roi_width+6),j==0,false,tLevel);
						else {
							//roiRefine = refineRoiTrans(roi2,ip,slice*(roi_width+6),j==0,false,tLevel);//refineRoiRFP(roi2,ip,slice*(roi_width+6),j==0,false,tLevel);
						}

						if(roiRefine != null)
							roi2 = roiRefine;

						//ratio = 0.1;
						if(roi2 != null)
						{
//							ImageProcessor bp = ip2.convertToShort(true);
//							bp = bp.convertToByte(true);
							
//							ImageProcessor bp3 = ip2.convertToByte(true);
							
//							ImagePlus imp8 = convertTo8bits(new ImagePlus("debug-ip2", ip2));
//							ip2.setRoi(3, 0, ip2.getWidth()-6, ip2.getHeight());
//							ip2 = ip2.crop();
							ImageProcessor bp =convertTo8bits(new ImagePlus("debug-ip2", ip2),true).getProcessor();
							bp.setRoi((int)(Math.ceil(roi_width*0.5-1)), (int)roi2.getBounds().y, 2, (int)roi2.getBounds().getHeight());								
							ImageProcessor ipSeg = bp.crop();
//							if(s==3 && (slice == 177/(roi_width + 6)))
//							{
//
//								new ImagePlus("debug-ip2", bp).show();
////								imp8.show();								
//								ImageProcessor ip2Temp = bp.duplicate();
//								ip2Temp.setRoi((int)(Math.ceil(roi_width*0.5-1)), (int)roi2.getBounds().y, 2, (int)roi2.getBounds().getHeight());
//								ip2Temp.fill();
//								ImageStatistics iStatistics1 = ImageStatistics.getStatistics(ipSeg, ImageStatistics.MEDIAN, null);
//								System.out.println("j :"+  j +" " +  iStatistics1.median);
//								new ImagePlus("", ip2Temp).show();
//							}
							ImageStatistics iStatistics = ImageStatistics.getStatistics(ipSeg, ImageStatistics.MEDIAN, null);
							
							if(iStatistics.median <= meanValueCell) {
								roi2.setPosition(s);
								rois.add(roi2);
							}
						}
					}

					IJ.showStatus("Segmentation at channel - " + s + " slice-" + slice);
				}
			}
		}
		return rois;
	}
	public static ImagePlus convertTo8bits(ImagePlus img16, boolean adjustBG)
	{

		if(img16.getType()!= ImagePlus.GRAY8) {
			ImageProcessor ip = img16.getProcessor().duplicate();
			ContrastEnhancer ce = new ContrastEnhancer();
			ce.stretchHistogram(ip, 0.0);
			ImagePlus img8 = new ImagePlus(img16.getTitle()+" 8 bits", ip);
			ImageConverter ic = new ImageConverter(img8);
			ic.convertToGray8();
			if(adjustBG)
				img8 = adjustBackgroundHistogram(img8);			
			return img8;
		}
		else{			
			return img16;
		}
	}
	
	/**
	 * adjust histogram of gray value (~remove a part of noise)
	 * @param img ImagePlus of an image
	 * @return ImagePlus this same image with an adjusted histogram
	 */
	public static ImagePlus adjustBackgroundHistogram(ImagePlus img)
	{
		int[] histosorted = img.getProcessor().getHistogram();
		int[] histo = histosorted.clone();
		Arrays.sort(histosorted);
		int backgroundThreshold=0;
		for (int i=0;i<histo.length;i++)
		{
			if (histosorted[histosorted.length-2]==histo[i])
				backgroundThreshold=i;                                            
		}
		img.getProcessor().setMinAndMax(backgroundThreshold, img.getProcessor().getMax());
		int[] table = new int[256];
		int min = (int)img.getProcessor().getMin();
		int max = (int)img.getProcessor().getMax();
		for (int i=0; i<256; i++) {
			if (i<=min)
				table[i] = 0;
			else if (i>=max)
				table[i] = 255;
			else
				table[i] = (int)(((double)(i-min)/(max-min))*255);
		}
		img.getProcessor().applyTable(table);
		return img;
	}
	

	
	
	public double[] getProfile(ImageProcessor ipLocalRoiOrig,boolean debug,String m) {
		Convolver convolver = new Convolver();
		ImageProcessor ip2up = ipLocalRoiOrig.duplicate();
		ImageProcessor ip2down = ipLocalRoiOrig.duplicate();
		convolver.setNormalize(true);
		convolver.convolve(ip2up, new float[]{-1,-2,-1,0,0,0,1,2,1}, 3, 3);
		convolver.convolve(ip2down, new float[]{1,2,1,0,0,0,-1,-2,-1}, 3, 3);
		ImageCalculator imageCalculator = new ImageCalculator();
		ImagePlus impMax = imageCalculator.run("max 32", new ImagePlus("",ip2up), new ImagePlus("",ip2down));
		ImagePlus impPlotProfile = new ImagePlus("", impMax.getProcessor().convertToByte(true));
		if(debug)
			IJ.save(impPlotProfile, "d:/impPlotprofile-"+m+".tif");
		impPlotProfile.setRoi(0,0,ip2up.getWidth(),ip2down.getHeight());
		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);//sdfqhsdfk
		double[] profile = pPlot.getProfile();
		double[] profile1 = new double[profile.length];
		for(int p=0; p<profile.length; p++)
		{
			double sum = 0;
			for(int q=p-1; q<p+2; q++)
			{
				if(q<0 || q>=profile.length)
					continue;
				sum = sum + profile[q];
			}
			profile1[p] = sum;					
		}
		profile = profile1;
		return profile;
	}
	
	private double[] getProfile1(ImageProcessor ipLocalRoiOrig,boolean debug,String m) {
		ImagePlus impPlotProfile = new ImagePlus("", ipLocalRoiOrig.convertToByte(true));
		if(debug)
			IJ.save(impPlotProfile, "d:/impPlotprofile-"+m+".tif");
		impPlotProfile.setRoi(0,0,ipLocalRoiOrig.getWidth(),ipLocalRoiOrig.getHeight());
		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);
		double[] profile = pPlot.getProfile();
		return profile;
	}


	public static Roi refineRoi(Roi roi, ImageProcessor ip,int roiX, boolean head, boolean debug)
	{		
		Roi roi2 = null;
		ip.setRoi(roi);
		int width = roi.getBounds().width;
		ImageProcessor ipSeg = ip.crop();						
		ipSeg = ipSeg.convertToByte(true);		
		ipSeg.setRoi(width/2-5, 0, 10, ipSeg.getHeight());//ipSeg.setRoi(6, 0, roi_width-12, ipSeg.getHeight());
		ipSeg = ipSeg.crop();
		if(debug)
			new ImagePlus(String.valueOf(roi.getBounds().x), ipSeg.duplicate()).show();

		int tLevel = (int)(Auto_Threshold.getThreshold("Otsu", ipSeg)*1.16);
		ipSeg.threshold(tLevel);
		ipSeg.erode();
		ipSeg.dilate();
		
		if(debug)
			new ImagePlus(String.valueOf(roi.getBounds().y), ipSeg.duplicate()).show();
		

		int top = -1;
		int bottom = -1;

		boolean satisfaiedLength = false;
		int numdarkMax = Integer.MIN_VALUE;
		int topMin = Integer.MAX_VALUE;
		for(int xx=0; xx<ipSeg.getWidth(); xx++)
		{
			int numdark = 0;
			top = -1;
			for(int y = 0; y<ipSeg.getHeight(); y++)
			{					
				int grey = ipSeg.get(xx, y);
				if(grey == 0 )
				{
					if(top == -1)
						top = y;
					numdark ++;
				}else if (numdark<10) {
					top = -1;
					numdark = 0;
				}
			}
			if(numdark > numdarkMax)
			{
				numdarkMax = numdark;
				if(top != -1 && top < topMin)
					topMin = top;					
			}
		}
		if(numdarkMax > 7)
		{
			satisfaiedLength = true;
		}


		top = topMin;
		int numdark = 0;
		if(satisfaiedLength)
		{
			int xx = ipSeg.getWidth()/2;
			while( top > 0 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, top -1) == 0 || ipSeg.get(xx-1, top-1) == 0 || ipSeg.get(xx+1, top -1) == 0))
			{
				if(ipSeg.get(xx, top - 1) == 0)
				{

				}else if (ipSeg.get(xx - 1, top -1) == 0) {
					xx = xx - 1;
				}else if (ipSeg.get(xx + 1, top -1) == 0) {
					xx = xx + 1;
				}
				top = top-1;
			}


			numdarkMax = Integer.MIN_VALUE;
			int bottomMax = Integer.MIN_VALUE;

			numdark = 0;							
			for(int xxx=0; xxx<ipSeg.getWidth(); xxx++)
			{
				for(int y=ipSeg.getHeight() - 1; y>=0; y--)
				{
					int grey = ipSeg.get(xxx, y);
					if(grey == 0)
					{
						if(bottom == -1)
							bottom = y;
						numdark ++;
					}else if(numdark<5) {
						bottom = -1;
						numdark = 0;
					}

				}
				if(numdark > numdarkMax)
				{
					numdarkMax = numdark;
					if(bottom != -1 && bottomMax < bottom)
						bottomMax = bottom;					
				}								
			}

			bottom = bottomMax;
			xx = ipSeg.getWidth()/2;
			while( bottom < ipSeg.getHeight() - 1 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, bottom + 1) == 0 || ipSeg.get(xx-1, bottom  + 1) == 0 || ipSeg.get(xx+1, bottom + 1) == 0)) // to be verified
			{
				if(ipSeg.get(xx, bottom + 1) == 0)
				{

				}else if(ipSeg.get(xx-1, bottom + 1) == 0) {
					xx = xx - 1;
				}else if (ipSeg.get(xx + 1, bottom + 1) == 0) {
					xx = xx + 1;
				}
				bottom = bottom + 1;
			}

			if(satisfaiedLength)						
				bottom = bottom + 1 < ipSeg.getHeight()? bottom+1:ipSeg.getHeight();
			else {
				bottom = ipSeg.getHeight();
			}

						
			if(bottom - top > minPeakDist)
			{
				if(head)
				{
					System.out.println("top : " + top + " bottom : " + bottom);
					roi2 = new Roi(roiX, roi.getBounds().y+top-4, roi_width, bottom - top + 4);
				}
				else {
					roi2 = new Roi(roiX, roi.getBounds().y+ top, roi_width, bottom - top + 1,1);
				}
			}else {
				roi2 = null;
			}
		}
		return roi2;

	}
	
	public static Roi refineRoiTrans(Roi roi, ImageProcessor ip,int roiX,boolean head, boolean debug, int level)
	{		
		Roi roi2 = null;
		ip.setRoi(new Roi(roi.getBounds().x + 5, roi.getBounds().y, roi.getBounds().width-10, roi.getBounds().height));		
		ImageProcessor ipSeg = ip.crop();
		ipSeg = ipSeg.convertToByte(true);
		
		int tLevel = 0;
		if(head && level > 0)
		{
			tLevel = (int)(level * 1.16);
			if(debug)
				new ImagePlus(String.valueOf(tLevel), ipSeg.duplicate()).show();
			//System.out.println("threshold value by bottom cell is : " + tLevel);
		}else {
			tLevel = (int)(Auto_Threshold.getThreshold("Otsu", ipSeg)*1.1);//*1.16
		}
		if(tLevel > 120)
			tLevel = 120;
		
		if(debug)
			new ImagePlus(String.valueOf(tLevel), ipSeg.duplicate()).show();
		
		int numdark = 0;
		int top = -1;
		for(int y = 0; y<ipSeg.getHeight(); y++)
		{
			int[] grey = new int[ipSeg.getWidth()];//ipSeg.get(xx, y);
			for(int xx=0; xx<ipSeg.getWidth(); xx++)
			{
				grey[xx] = ipSeg.get(xx,y);
			}
			Arrays.sort(grey);
			int greyM = grey[ipSeg.getWidth()/4-1];
			if(greyM <= tLevel )
			{
				if(top == -1)
					top = y;
				numdark ++;
			}else if (numdark<minPeakDist*0.7) {
				top = -1;
				numdark = 0;
			}
			if(debug)
				System.out.println("x : " + roi.getBounds().x + " y : " + y + " grey : " + greyM + " tLevel : " + tLevel + " top : " + top + " numdark : " + numdark);
		}		
		if(top!=-1)
		{						
			roi2 = new Roi(roiX, roi.getBounds().y + top, roi_width, numdark);
		}
		return roi2;
	}
	
	public static Roi refineRoiRFP(Roi roi, ImageProcessor ip,int roiX,boolean head, boolean debug, int level)
	{		
		Roi roi2 = null;
		ip.setRoi(new Roi(roi.getBounds().x, roi.getBounds().y, roi.getBounds().width, roi.getBounds().height));		
		ImageProcessor ipSeg = ip.crop();
		ipSeg = ipSeg.convertToByte(true);
		
		int tLevel = (int)(Auto_Threshold.getThreshold("Otsu", ipSeg)*1.1);//*1.16
		tLevel = tLevel>level?tLevel:level;
		
		if(debug)
			new ImagePlus(String.valueOf(tLevel), ipSeg.duplicate()).show();
		
		int numdark = 0;
		int top = -1;
		for(int y = 0; y<ipSeg.getHeight(); y++)
		{
			int[] grey = new int[ipSeg.getWidth()];//ipSeg.get(xx, y);
			for(int xx=0; xx<ipSeg.getWidth(); xx++)
			{
				grey[xx] = ipSeg.get(xx,y);
			}
			Arrays.sort(grey);
			int greyM = grey[ipSeg.getWidth()/6-1];
			if(greyM <= tLevel )
			{
				if(top == -1)
					top = y;
				numdark ++;
			}else if (numdark<minPeakDist*0.5) {
				top = -1;
				numdark = 0;
			}
			if(debug)
				System.out.println("x : " + roi.getBounds().x + " y : " + y + " grey : " + greyM + " tLevel : " + tLevel + " top : " + top + " numdark : " + numdark);
		}
		System.out.println("x : " + roi.getBounds().x + " top : " + top);
		if(top!=-1)
		{						
			roi2 = new Roi(roiX, roi.getBounds().y + top, roi_width, numdark);
		}
		return roi2;
	}
	
	public static Roi refineRoiRFPOld(Roi roi, ImageProcessor ip,int roiX, boolean head, boolean debug, int level)
	{		
		Roi roi2 = null;
		ip.setRoi(new Roi(roi.getBounds().x + 13, roi.getBounds().y, roi.getBounds().width-26, roi.getBounds().height));		
		ImageProcessor ipSeg = ip.crop();
		ipSeg = ipSeg.convertToByte(true);
		
		int tLevel = 0;
		if(head && level > 0)
		{
			tLevel = (int)(level*1.12);
			if(debug)
				new ImagePlus(String.valueOf(level), ipSeg.duplicate()).show();
			//System.out.println("threshold value by bottom cell is : " + tLevel);
		}
		else {
			tLevel = (int)(Auto_Threshold.getThreshold("MaxEntropy", ipSeg)*1.1);//*1.16
		}
		ipSeg.threshold(tLevel);
		ipSeg.erode();
		ipSeg.dilate();
		ipSeg.setRoi(0, 0, ipSeg.getWidth(), ipSeg.getHeight());//ipSeg.setRoi(6, 0, roi_width-12, ipSeg.getHeight());//ipSeg.setRoi(ipSeg.getWidth()/2-3, 0, 5, ipSeg.getHeight());//ipSeg.setRoi(6, 0, roi_width-12, ipSeg.getHeight());
		ipSeg = ipSeg.crop();		
		
		
		if(debug)
			new ImagePlus(String.valueOf(tLevel), ipSeg.duplicate()).show();
		

		int top = -1;
		int bottom = -1;
		boolean satisfaiedLength = false;
		int numdarkMax = Integer.MIN_VALUE;
		int topMin = Integer.MAX_VALUE;
		for(int xx=0; xx<ipSeg.getWidth(); xx++)
		{
			int numdark = 0;
			top = -1;
			for(int y = 0; y<ipSeg.getHeight(); y++)
			{					
				int grey = ipSeg.get(xx, y);
				if(grey == 0 )
				{
					if(top == -1)
						top = y;
					numdark ++;
				}else if (numdark<minPeakDist*0.5) {
					top = -1;
					numdark = 0;
				}
			}
			if(numdark > numdarkMax)
			{
				numdarkMax = numdark;
				if(top != -1 && top < topMin)
					topMin = top;					
			}
		}
		if(numdarkMax > minPeakDist*0.5)
		{
			satisfaiedLength = true;
		}


		top = topMin;
		int numdark = 0;
		if(satisfaiedLength)
		{
//			int xx = ipSeg.getWidth()/2;
//			while( top > 0 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, top -1) == 0 || ipSeg.get(xx-1, top-1) == 0 || ipSeg.get(xx+1, top -1) == 0))
//			{
//				if(ipSeg.get(xx, top - 1) == 0)
//				{
//
//				}else if (ipSeg.get(xx - 1, top -1) == 0) {
//					xx = xx - 1;
//				}else if (ipSeg.get(xx + 1, top -1) == 0) {
//					xx = xx + 1;
//				}
//				top = top-1;
//			}


			numdarkMax = Integer.MIN_VALUE;
			int bottomMax = Integer.MIN_VALUE;
										
			for(int xxx=0; xxx<ipSeg.getWidth(); xxx++)
			{
				numdark = 0;
				bottom = -1;
				for(int y=ipSeg.getHeight() - 1; y>=0; y--)
				{
					int grey = ipSeg.get(xxx, y);
					if(grey == 0)
					{
						if(bottom == -1)
							bottom = y;
						numdark ++;
					}else if(numdark<minPeakDist*0.5) {
						bottom = -1;
						numdark = 0;
					}

				}
				if(numdark > numdarkMax)
				{
					numdarkMax = numdark;
					if(bottom != -1 && bottomMax < bottom)
						bottomMax = bottom;					
				}								
			}
			bottom = bottomMax;
//			xx = ipSeg.getWidth()/2;
//			while( bottom < ipSeg.getHeight() - 1 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, bottom + 1) == 0 || ipSeg.get(xx-1, bottom  + 1) == 0 || ipSeg.get(xx+1, bottom + 1) == 0)) // to be verified
//			{
//				if(ipSeg.get(xx, bottom + 1) == 0)
//				{
//
//				}else if(ipSeg.get(xx-1, bottom + 1) == 0) {
//					xx = xx - 1;
//				}else if (ipSeg.get(xx + 1, bottom + 1) == 0) {
//					xx = xx + 1;
//				}
//				bottom = bottom + 1;
//			}
			
			if(numdarkMax > minPeakDist*0.5)
			{
				satisfaiedLength = true;
			}

			if(satisfaiedLength)						
				bottom = bottom < ipSeg.getHeight()? bottom:ipSeg.getHeight();
			else {
				bottom = ipSeg.getHeight();
			}
						
			if(bottom - top > minPeakDist)
			{
				if(head)
				{
					System.out.println("top : " + top + " bottom : " + bottom);
					roi2 = new Roi(roiX, roi.getBounds().y + top-2, roi_width, bottom - top + 2);
				}
				else {
					roi2 = new Roi(roiX, roi.getBounds().y + top, roi_width, bottom - top);
				} 
			}else {
				roi2 = null;
			}
		}
		return roi2;

	}
	
	
	public static Roi refineRoiHead(Roi roi, ImageProcessor ip,int roiX, boolean head, boolean debug)
	{		
		Roi roi2 = null;
		ip.setRoi(roi);
		ImageProcessor ipSeg = ip.crop();						
		ipSeg = ipSeg.convertToByte(true);		
		ipSeg.setRoi(6, 0, roi_width-12, ipSeg.getHeight());
		ipSeg = ipSeg.crop();
		if(debug)
			new ImagePlus(String.valueOf(roi.getBounds().x), ipSeg.duplicate()).show();

		int tLevel = (int)(Auto_Threshold.getThreshold("Otsu", ipSeg)*1.16);
		ipSeg.threshold(tLevel);
		ipSeg.erode();
		ipSeg.dilate();
		
		if(debug)
			new ImagePlus(String.valueOf(roi.getBounds().y), ipSeg.duplicate()).show();
		

		int top = -1;
		int bottom = -1;

		boolean satisfaiedLength = false;

		int numdarkMax = Integer.MIN_VALUE;
		int topMin = Integer.MAX_VALUE;
		for(int xx=0; xx<ipSeg.getWidth(); xx++)
		{
			int numdark = 0;
			top = -1;
			for(int y = 0; y<ipSeg.getHeight(); y++)
			{					
				int grey = ipSeg.get(xx, y);
				if(grey == 0 )
				{
					if(top == -1)
						top = y;
					numdark ++;
				}else if (numdark<5) {
					top = -1;
					numdark = 0;
				}
			}
			if(numdark > numdarkMax)
			{
				numdarkMax = numdark;
				if(top != -1 && top < topMin)
					topMin = top;					
			}
		}
		if(numdarkMax > 7)
		{
			satisfaiedLength = true;
		}


		top = topMin;
		int numdark = 0;
		if(satisfaiedLength)
		{
			int xx = ipSeg.getWidth()/2;
			while( top > 0 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, top -1) == 0 || ipSeg.get(xx-1, top-1) == 0 || ipSeg.get(xx+1, top -1) == 0))
			{
				if(ipSeg.get(xx, top - 1) == 0)
				{

				}else if (ipSeg.get(xx - 1, top -1) == 0) {
					xx = xx - 1;
				}else if (ipSeg.get(xx + 1, top -1) == 0) {
					xx = xx + 1;
				}
				top = top-1;
			}


			numdarkMax = Integer.MIN_VALUE;
			int bottomMax = Integer.MIN_VALUE;

			numdark = 0;							
			for(int xxx=0; xxx<ipSeg.getWidth(); xxx++)
			{
				for(int y=ipSeg.getHeight() - 1; y>=0; y--)
				{
					int grey = ipSeg.get(xxx, y);
					if(grey == 0)
					{
						if(bottom == -1)
							bottom = y;
						numdark ++;
					}else if(numdark<5) {
						bottom = -1;
						numdark = 0;
					}

				}
				if(numdark > numdarkMax)
				{
					numdarkMax = numdark;
					if(bottom != -1 && bottomMax < bottom)
						bottomMax = bottom;					
				}								
			}

			bottom = bottomMax;
			xx = ipSeg.getWidth()/2;
			while( bottom < ipSeg.getHeight() - 1 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, bottom + 1) == 0 || ipSeg.get(xx-1, bottom  + 1) == 0 || ipSeg.get(xx+1, bottom + 1) == 0)) // to be verified
			{
				if(ipSeg.get(xx, bottom + 1) == 0)
				{

				}else if(ipSeg.get(xx-1, bottom + 1) == 0) {
					xx = xx - 1;
				}else if (ipSeg.get(xx + 1, bottom + 1) == 0) {
					xx = xx + 1;
				}
				bottom = bottom + 1;
			}

			if(satisfaiedLength)						
				bottom = bottom + 1 < ipSeg.getHeight()? bottom+1:ipSeg.getHeight();
			else {
				bottom = ipSeg.getHeight();
			}

						
			if(bottom - top > minPeakDist)
			{
				if(head)
				{
					System.out.println("top : " + top + " bottom : " + bottom);
					roi2 = new Roi(roiX, roi.getBounds().y+top-4, roi_width, bottom - top + 4);
				}
				else {
					roi2 = new Roi(roiX, roi.getBounds().y+ top, roi_width, bottom - top + 1,1);
				}
			}else {
				roi2 = null;
			}
		}
		return roi2;

	}
	
	
	public void makeMovie(ImagePlus impToseg, String path)
	{		
		if(impToseg == null)
			return;
		//impToseg.show();
		ImagePlus impToMark = impToseg;
		int width = impToMark.getWidth();
		int height = impToMark.getHeight();
		int ss = impToMark.getImageStackSize();
		parseImageTitle(impToMark);
		String[] strs = new String[ss];
		for(int i=0; i<ss; i++)
		{
			strs[i] = "slice - " + String.valueOf(i+1);
		}
		boolean[] bools = new boolean[ss];
		GenericDialog genericDialog = new GenericDialog("Make movie");
		genericDialog.addNumericField("Number of original images : ", stackSize, 0);
		genericDialog.addNumericField("First cell dead at : ", 6314, 0);
		genericDialog.addNumericField("cell1 y : ", 22, 0);
		genericDialog.addNumericField("Second cell dead at : ", 12105, 0);
		genericDialog.addNumericField("cell2 y : ", 66, 0);
		genericDialog.addNumericField("Third cell dead at : ", 16669, 0);
		genericDialog.addNumericField("cell3 y : ", 108, 0);
		genericDialog.addMessage("Select slices to be segmented");

		genericDialog.addCheckboxGroup(impToMark.getStackSize(),1 ,strs, bools);


		genericDialog.showDialog();		
		if (genericDialog.wasCanceled()) return;
		stackSize = (int)genericDialog.getNextNumber();
		int cell1 = (int)genericDialog.getNextNumber();
		cell1 = cell1/(roi_width + 6);
//		int y1 = (int)genericDialog.getNextNumber();
		int cell2 = (int)genericDialog.getNextNumber();
		cell2 = cell2/(roi_width + 6);
//		int y2 = (int)genericDialog.getNextNumber();
		int cell3 = (int)genericDialog.getNextNumber();
		cell3 = cell3/(roi_width + 6);
//		int y3 = (int)genericDialog.getNextNumber();
		
		int y0 = 110;
		
		boolean all = true;
		
		for (int i=0; i<strs.length; i++) {
			if (strs[i].length()!=0) {
				bools[i] = genericDialog.getNextBoolean();
				if(bools[i])
					all = false;
				System.out.print(strs[i]+": "+bools[i] + " ");
			}
		}
		
		int totalSlice = impToMark.getStackSize();
		ImagePlus impDouble = impToseg;//doubleStack(impToseg[0], impToseg[1]);
		if(impDouble!=null)
		{
			//impDouble.show();
			impToMark = impDouble;	
		}
		
		if(width%stackSize !=0)
		{
			IJ.error("Wrong number of original images");
			return;
		}			
		roi_width = width/stackSize - 6;			

		int total = totalSlice*stackSize;
		int k= 1;
		
		
		int n = 10;
		int halfn = n - 2;
		int yshift = roi_width;
		for(int s=1; s<=impToMark.getStackSize(); s++)
		{
			if(!bools[s-1] && !all)
				continue;
			ImageProcessor ip;
			ip = impToMark.getImageStack().getProcessor(s);				
			
			for(int slice=0; slice<stackSize;slice++,k++)
			{
				ImagePlus impRGB = IJ.createImage(String.valueOf(slice), "RGB white", roi_width*n, height+yshift, 1); 
						//IJ.createImage("movie", roi_width*n, height+roi_width, 1, 16);
				ColorProcessor ipFFt = (ColorProcessor)impRGB.getProcessor();
				int x= slice*(roi_width+6);
				Roi roi = new Roi(x, 0, roi_width, height);
				ip.setRoi(roi);
				ImageProcessor ip2 = ip.crop();
				ip2 = ip2.convertToByte(true);
				ip2 = ip2.convertToRGB();
				
				ipFFt.insert(ip2, halfn*(roi_width), roi_width);
				String label = String.valueOf(slice*4)+" minutes";
				ipFFt.setColor(new Color(0, 0, 0));
				ipFFt.setFont(new Font("TimesRoman", Font.PLAIN, 15));
				ipFFt.drawString(label, (int)(halfn*0.5*roi_width), (int)(roi_width));

				IJ.showStatus(String.valueOf(k)+"/" + String.valueOf(total));
				
				
				if(slice <= 4)
				{			
					ipFFt.setColor(new Color(0, 0, 0));
					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y0, halfn*roi_width, y0);
					arrowRoi.setStrokeColor(new Color(0, 0, 0));
					ipFFt.draw(arrowRoi);
					ipFFt.drawString("Early daughter cell", (int)(halfn*roi_width - 7*roi_width),(int)(y0 + yshift*0.4));
				}
				
				if(slice > 4 )
				{			
					y0 = 50;
					if(slice > 226)
						ipFFt.setColor(new Color(255, 0, 0));
					else {
						ipFFt.setColor(new Color(0, 0, 0));
					}
					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y0, halfn*roi_width, y0);					
					ipFFt.draw(arrowRoi);
					ipFFt.drawString("Early daughter cell", (int)(halfn*roi_width - 7*roi_width),(int)(y0 + yshift*0.4));
				}
				
				if(slice > 226 )
				{			
					y0 = 85;
					if(slice > 433)
						ipFFt.setColor(new Color(255, 0, 0));
					else {
						ipFFt.setColor(new Color(0, 0, 0));
					}					
					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y0, halfn*roi_width, y0);
					arrowRoi.setStrokeColor(new Color(0, 0, 0));
					ipFFt.draw(arrowRoi);
					ipFFt.drawString("Late  daughter cell", (int)(halfn*roi_width - 7*roi_width), (int)(y0 + yshift*0.4));
				}
				
				if(slice > 433 )
				{			
					y0 = 133;
					if(slice > 611)
						ipFFt.setColor(new Color(255, 0, 0));
					else {
						ipFFt.setColor(new Color(0, 0, 0));
					}
					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y0, halfn*roi_width, y0);
					arrowRoi.setStrokeColor(new Color(0, 0, 0));
					ipFFt.draw(arrowRoi);
					ipFFt.drawString("Second generation \n late daughter cell", (int)(halfn*roi_width - 7.2*roi_width),(int)(y0 + yshift*0.4));
				}



//				if(slice >= cell1)
//				{			
//					ipFFt.setColor(new Color(255, 0, 0));
//					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y1, halfn*roi_width, y1 - yshift*0.5);
//					arrowRoi.setStrokeColor(new Color(255, 0, 0));
//					ipFFt.draw(arrowRoi);
//				}
//				
//				if(slice >= cell2)
//				{	
//					ipFFt.setColor(new Color(255, 0, 0));
//					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y2, halfn*roi_width, y2+yshift);
//					arrowRoi.setStrokeColor(new Color(255, 0, 0));
//					ipFFt.draw(arrowRoi);
//				}
//				
//				if(slice >= cell3)
//				{					
//					ipFFt.setColor(new Color(255, 0, 0));
//					Arrow arrowRoi = new Arrow(halfn*roi_width - 1.5*roi_width, y3, halfn*roi_width, y3+yshift);
//					arrowRoi.setStrokeColor(new Color(255, 0, 0));
//					ipFFt.draw(arrowRoi);
//				}
					
				IJ.save(impRGB, path+slice+".tif");
				System.out.println("save rgb image - " + s + " slice-" + slice);
				
			}
		}
		
		
		
	}
	
	
	public void meanImage(ImagePlus impToseg, String path)
	{		
		if(impToseg == null)
			return;
		//impToseg.show();
		ImagePlus impToMark = impToseg;
		int width = impToMark.getWidth();
		int ss = impToMark.getImageStackSize();
		String[] strs = new String[ss];
		for(int i=0; i<ss; i++)
		{
			strs[i] = "slice - " + String.valueOf(i+1);
		}
		boolean[] bools = new boolean[ss];
		GenericDialog genericDialog = new GenericDialog("Make movie");
		genericDialog.addNumericField("Number of original images : ", stackSize, 0);
		
		genericDialog.addNumericField("inter number : ", 20, 0);
		genericDialog.addNumericField("height : ", 50, 0);
		genericDialog.addMessage("Select slices to be segmented");
		genericDialog.addCheckboxGroup(impToMark.getStackSize(),1 ,strs, bools);


		genericDialog.showDialog();		
		if (genericDialog.wasCanceled()) return;
		stackSize = (int)genericDialog.getNextNumber();
		
		int cell1 = (int)genericDialog.getNextNumber();
		int y = (int)genericDialog.getNextNumber();
		
		boolean all = true;
		
		for (int i=0; i<strs.length; i++) {
			if (strs[i].length()!=0) {
				bools[i] = genericDialog.getNextBoolean();
				if(bools[i])
					all = false;
				System.out.print(strs[i]+": "+bools[i] + " ");
			}
		}
		
		int totalSlice = impToMark.getStackSize();
		ImagePlus impDouble = impToseg;//doubleStack(impToseg[0], impToseg[1]);
		if(impDouble!=null)
		{
			//impDouble.show();
			impToMark = impDouble;	
		}
		
		if(width%stackSize !=0)
		{
			IJ.error("Wrong number of original images");
			return;
		}			
		roi_width = width/stackSize - 6;			

		int total = totalSlice*stackSize;
		int k= 1;
		
		

		for(int s=1; s<=impToMark.getStackSize(); s++)
		{
			if(!bools[s-1] && !all)
				continue;
			ImageProcessor ip;
			ip = impToMark.getImageStack().getProcessor(s);				
			
			for(int slice=0; slice<stackSize;slice=slice+cell1,k++)
			{
				
				ImagePlus impMean = null; 
				
				ImageCalculator imCalculator = new ImageCalculator();
			
				for(int s2 = slice; s2<slice+cell1;s2 = s2 + 2)
				{					
					System.out.println("process slice : " + s2);
					if(s2 + 2 >= stackSize)
						break;
					
					int x= s2*(roi_width+6);
					Roi roi = new Roi(x, 0, roi_width, y);
					ip.setRoi(roi);
					ImageProcessor ip2 = ip.crop();				
					
					x= (s2+1)*(roi_width+6);
					roi = new Roi(x, 0, roi_width, y);
					ip.setRoi(roi);
					ImageProcessor ip3 = ip.crop();									
					
					if(s2 == slice)						
						impMean = imCalculator.run("max create", new ImagePlus("",ip2), new ImagePlus("", ip3));
					else {
						ImagePlus impMean23 = imCalculator.run("max create", new ImagePlus("",ip2), new ImagePlus("", ip3));
						//impMean23.show();
						impMean = imCalculator.run("max create", impMean, impMean23);						
					}
					//impMean.show();
					
				}
				//impMean.getProcessor().multiply(1.0/cell1);
				IJ.showStatus(String.valueOf(k)+"/" + String.valueOf(total));				
				IJ.save(impMean, path+slice+".tif");
				//System.out.println("save mean image - " + s + " slice-" + (slice+1));
				
			}
		}
		
		
		
	}
	
	
	public int[] getPeakOnProfile(ImageProcessor ip, int tolerance, int minPeakDist)
	{
		ImagePlus impPlotProfile = new ImagePlus("", ip);		
		impPlotProfile.setRoi(ip.getWidth()/4+1, 0, ip.getWidth()/2, ip.getHeight());
		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);
		double[] profile = pPlot.getProfile();
		Ins_find_peaks peakFinder = new Ins_find_peaks(tolerance, minPeakDist);				
		Object[] out = peakFinder.findPeaks(profile);
		int position[] = (int[])out[0];
		if(position.length>0)
			return position;
		else {
			return null;
		}
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
	
	private void parseImageTitle(ImagePlus imp)
	{
		try {			
			String impName = imp.getTitle();
			String[] name = impName.split("-ss-");				
			name = name[1].split("-roi-");
			stackSize = Integer.valueOf(name[0]);		
			name = impName.split("-roi-");
			name = name[1].split(".tif");
			if(impName.contains("-sx-"))
				name = name[0].split("-sx-");
			roi_width = Integer.valueOf(name[0]);
			widthImp = imp.getWidth();
		} catch (ArrayIndexOutOfBoundsException e2) {
			IJ.showMessage("Image name doesn't include necessary information! (ss-?-roi-?)");					
		}	
	}

	// 1->1 2->3 4->7 put 2 and -1
	// to come back, put 0.5 and 0.5
	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if (label.equals("Move ROIs slice")) {
			Frame frame = WindowManager.getFrame("ROI Manager");
			if (frame==null)						
				IJ.run("ROI Manager...");
			frame = WindowManager.getFrame("ROI Manager");
			if (frame==null || !(frame instanceof RoiManager))
				{return;}
			RoiManager roiManager = (RoiManager)frame;
			roiManager.setVisible(true);	
			
			int deletedSlice = -1;
			GenericDialog genericDialog = new GenericDialog("Move Roi Position");
			genericDialog.addNumericField("Delete all rois on slice", deletedSlice, 1);
			
			
			genericDialog.addMessage("for moving 1->1, 2->3, 4->7... put 2 and -1, for moving back, put 0.5 and 0.5");
			genericDialog.addNumericField("Multiply roi slice by ", 1, 1);
			genericDialog.addNumericField("Increase roi slice by ", 1, 1);
			genericDialog.addCheckbox("Transform AP old to AP new", false);
			genericDialog.addNumericField("arithmetic progression", 2, 0);
			genericDialog.addNumericField("to new arithmetic progression", 3, 0);

			genericDialog.showDialog();
			if(genericDialog.wasCanceled())
			{
				return;
			}
			deletedSlice = (int)genericDialog.getNextNumber();
			double multiply = genericDialog.getNextNumber();
			double m = genericDialog.getNextNumber();
			boolean nextChoice = genericDialog.getNextBoolean();
			int ap1 = (int)genericDialog.getNextNumber();
			int apNew = (int)genericDialog.getNextNumber();
			Roi[] rois = roiManager.getRoisAsArray();
			deleteRoiToManager();
			
			if(deletedSlice >= 0)
			{
				for(Roi r:rois)
				{
					int s = r.getPosition();
					if(s == deletedSlice)
						continue;
//					r.setName(null);
//					r.setPosition(s);
					roiManager.addRoi(r);
				}
				return;
			}
			
			if(!nextChoice)
			for(Roi r:rois)
			{
				int s = r.getPosition();
				s = (int)(s*multiply + m);
				r.setName(null);
				r.setPosition(s);
				roiManager.addRoi(r);
			}else {
				for(Roi r:rois)
				{
					int s = r.getPosition();
					int s1 = 1 + ((s-1)/ap1)*apNew + (s-1)%ap1;
					r.setPosition(s1);
					//System.out.println(" s : " + s + " s1 : " + s1);
					roiManager.addRoi(r);
				}
			}
		}
		
		if(e.getSource() == initializationButtion)
		{
			Object[] objs = getRefImages();
			if(objs == null)
				return;
			ImagePlus[] impRefArray =(ImagePlus[])objs[0];
			paramsRFP = new Ins_paramArray(impRefArray,(String)objs[1],this);			
			paramsRFP.setOpaque(true);	
			JFrame frame = new JFrame("Save Parameters");
			frame.setContentPane(paramsRFP);
			frame.pack();
			frame.setResizable(true);
			frame.setVisible(true);
			preprocessButton.setEnabled(true);
		}
		
		if (label.equals("Open Image")){
			ImagePlusAndTime impAndTime=openFDialog_stack(null,1,sortBydate);
			if (mChNoProcessing.isSelected())
			{
				impRearranged = impAndTime.imp;
				imp = impAndTime.imp;
				if(impRearranged == null)
				{
					IJ.showMessage("No image opened !");
					return;
				}
				impRearranged.show();
				try {
					saveImgName = impRearranged.getTitle();
					String[] name = saveImgName.split("-ss-");				
					name = name[1].split("-roi-");
					stackSize = Integer.valueOf(name[0]);		
					name = saveImgName.split("-roi-");
					name = name[1].split(".tif");
					roi_width = Integer.valueOf(name[0]);
					widthImp = impRearranged.getWidth();
				} catch (ArrayIndexOutOfBoundsException e2) {
					IJ.showMessage("Image name doesn't include necessary information! (ss-?-roi-?)");					
				}				
				return;
			}
			imp = impAndTime.imp;
			
			if(imp == null)
				return;
			imp.show();
			String zoom = impAndTime.zoom;
			if(zoom.contains("63x"))
			{
				setM63xParameter();
			}else if (zoom.contains("100x")) {
				setM100xParameter();
			}else {
				IJ.showMessage("These images are not aquired by 100x or 63x, please input the parameters manually");
			}
			
			if(impAndTime.oneFileOpened && mChNoProcessing.isSelected())
			{
			}else {
				imp_ref = new ImagePlus("img_ref", imp.getProcessor().duplicate());
				imp_ref.show();
				imp_ref.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
				ip_ref = imp_ref.getProcessor().createProcessor(imp_ref.getWidth(), imp_ref.getHeight()); // create reference ip_ref
				ip_ref.setPixels(imp_ref.getProcessor().getPixelsCopy());
			}
			widthImp = imp.getWidth();
			stackSize = impAndTime.getStacksize();			
			imp.getCanvas().addKeyListener(this);			
		}
		
		if (e.getSource() == lineageButton) {			
			lineageProcess(null,mModeSilence.isSelected());
		}

		if (label.equals("Stack to slice")) {
			ImagePlus imp = WindowManager.getCurrentImage();
			if(imp==null)
			{
				imp = IJ.openImage();
				if(imp == null)
					return;
			}
			String rename = imp.getImageStack().getShortSliceLabel(1);
			rename = rename.substring(0, rename.lastIndexOf("t")+1);
			GenericDialog genericDialog = new GenericDialog("Change file name");
			genericDialog.addStringField("Image Name",rename);
			genericDialog.showDialog();
			if(genericDialog.wasCanceled())
				return;
			rename = genericDialog.getNextString();
			Java2.setSystemLookAndFeel();
			ImageStack ims = imp.getImageStack();
			String pathDirectory = IJ.getDirectory("image");
			if(pathDirectory == null)
				pathDirectory = "D:";
		    File directory=new File(pathDirectory);	    
			JFileChooser  chooser = new JFileChooser(); 
		    chooser.setCurrentDirectory(directory);
		    chooser.setDialogTitle("Choose folder");
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
		    	pathDirectory = chooser.getSelectedFile().getPath();
		    }else {
				return;
			}
		    pathDirectory = pathDirectory + File.separator;
		   
			for(int s=1; s<ims.getSize(); s++)
			{			
				String title = rename; 
				title = title+String.valueOf(s);
				ImageProcessor ip = ims.getProcessor(s);			
				IJ.save(new ImagePlus("", ip), pathDirectory+title+".tif");
				System.out.println(pathDirectory+title+".tif");
			}
//			String pathDirectory = IJ.getDirectory("image");
//			if(pathDirectory == null)
//				pathDirectory = "D:";
//			File directory=new File(pathDirectory);	    
//			JFileChooser  chooser = new JFileChooser(); 
//			chooser.setCurrentDirectory(directory);
//			chooser.setDialogTitle("Choose folder");
//			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//			if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
//				pathDirectory = chooser.getSelectedFile().getPath();
//			}else {
//				return;
//			}
//			pathDirectory = pathDirectory + File.separator;
//			makeMovie(imp,pathDirectory);
		}
		
		if (label.equals("Macros")) {
			GenericDialog genericDialog = new GenericDialog("Diverse function");
			genericDialog.addCheckbox("Save each slice of stack to cell(odd)&foci(even) file", false);
			genericDialog.addCheckbox("Generate cell-mask or foci-LM by selecting two folder images", false);			
			genericDialog.addCheckbox("Threshold only in rectangular roi, value 100", false);
			genericDialog.addCheckbox("Refine foci by big aggerate mask in odd slice", false);
			genericDialog.showDialog();
			if(genericDialog.wasCanceled())
				return;
			boolean saveToFile = genericDialog.getNextBoolean();
			boolean combinetwoFolder = genericDialog.getNextBoolean();			
			boolean thresholdRoi = genericDialog.getNextBoolean();
			boolean refineFociRoi = genericDialog.getNextBoolean();
			
			if(saveToFile)
			{
				OpenDialog od = new OpenDialog("Open", "");
				String dir = od.getDirectory();
				String name = od.getFileName();
				String path = "";
				if (name==null)
					return;
				else
					path =  dir+name;			
				Opener opener = new Opener();
				ImagePlus imp = opener.openImage(path);

				String impname = imp.getTitle();
				ImageStack ims = imp.getImageStack();

				String dir1 = dir + "/cell/orig";
				System.out.println(dir1);
				File f = new File(dir1);
				f.mkdirs();
				for(int s=1;s<=ims.getSize();s=s+2)
				{
					ImageProcessor ip = ims.getProcessor(s);
					String spath = dir1 + "/"+impname+"-s"+ String.valueOf(s)+".tif";
					//spath = dir1 + "/"+ims.getShortSliceLabel(s)+".tif";
					System.out.println("saved : " + spath);
					IJ.save(new ImagePlus("", ip), spath);
				}
				
				String dir2 = dir +"/foci/orig";
				System.out.println(dir2);
				f = new File(dir2);
				f.mkdirs();				
				for(int s=2;s<=ims.getSize();s=s+2)
				{
					ImageProcessor ip = ims.getProcessor(s);
//					ip.invert();
					String spath = dir2 + "/"+impname+"-s"+ String.valueOf(s)+".tif";
					//spath = dir2 + "/"+ims.getShortSliceLabel(s)+".tif";
					System.out.println("saved : " + spath);
					IJ.save(new ImagePlus("", ip), spath);
					
				}
			}else if (combinetwoFolder){
				GenericDialog genericDialog2 = new GenericDialog("Select type");
				genericDialog2.addCheckbox("cell", false);
				genericDialog2.addCheckbox("foci", false);
				genericDialog2.showDialog();
				if(genericDialog2.wasCanceled())
					return;
				boolean cell = genericDialog2.getNextBoolean();
				boolean foci = genericDialog2.getNextBoolean();
				
				if(!cell && !foci || (cell && foci))
				{
					IJ.showMessage("Cell or Foci should be selected");
					return;
				}
				
				File[] f1 = openFolderDialog(1);
				File[] f2 = openFolderDialog(1);
				if(f1.length != f2.length)
				{
					IJ.showMessage("files' number are not identical");
					return;
				}
				ImagePlus im = IJ.openImage(f1[0].getPath());
				ImagePlus imp = IJ.createImage("foci-mask","8-bit black", im.getWidth(), im.getHeight(), f1.length*2);
				ImageStack ims = imp.getImageStack();
				
				int sizeMin = cell?20:0;
				int sizeMax = cell?10000:50;
				ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
				pAnalyzer.analyze(imp);
				Frame frame = WindowManager.getFrame("ROI Manager");
				if (frame==null)
				{
					IJ.run("ROI Manager...");
				}
				frame = WindowManager.getFrame("ROI Manager");
				if(foci)
				{	
					for(int i=0,s=2;i<f1.length;i++,s=s+2)
					{
						ImageProcessor ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);
						ip.threshold(Auto_Threshold.MaxEntropy(ip.getHistogram()));
						ip.invert();
						//ims.setProcessor(ip, s);
						imp.setSlice(s);
						if(!pAnalyzer.analyze(imp, ip)){				
							// the particle analyzer should produce a dialog on error, so don't make another one
							continue;
						}
						ims.getProcessor(s-1).insert(IJ.openImage(f1[i].getPath()).getProcessor().convertToByte(true), 0, 0);
					}
					imp.show();					
					imp = IJ.createImage("foci-LM", im.getWidth(), im.getHeight(), f1.length*2, 8);
					ims = imp.getImageStack();
					for(int i=0,s=1;i<f1.length;i++,s=s+2)
					{
						ImageProcessor ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);											
						ip = IJ.openImage(f1[i].getPath()).getProcessor().convertToByte(true);
						ims.setProcessor(ip, s);
						ims.setProcessor(ip, s+1);
					}
					imp.show();
					Roi[] rois = getRoiFromManager();
					Roi[] pRois = new Roi[rois.length];				
					for(int i=0;i<rois.length;i++)
					{
						if(rois[i] instanceof PointRoi || rois[i] instanceof OvalRoi){
							pRois[i] = rois[i];
						}else {						
							pRois[i] = new PointRoi(rois[i].getBounds().x, rois[i].getBounds().y);
						}
						pRois[i].setPosition(rois[i].getPosition());
						pRois[i].setName(rois[i].getName());
					}
					if(getRoiManager().getCount() > 0)
						deleteRoiToManager();			
					for(int m=0; m<pRois.length; m++)
					{
						if(pRois[m] == null)
							continue;
						addRoiToManager(pRois[m]);
					}
					
					
				}else if (cell) {
					for(int i=0,s=1;i<f1.length;i++,s=s+2)
					{
						ImageProcessor ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);
						ip.threshold(128);
						ip.invertLut();
						//open process to separate one cell
						ip.erode();
						ip.dilate();
						ims.getProcessor(s).insert(ip, 0, 0);
						imp.setSlice(s);
						if(!pAnalyzer.analyze(imp, ip)){				
							// the particle analyzer should produce a dialog on error, so don't make another one
							continue;
						}
					}
					imp.show();
					imp = IJ.createImage("cell-mask", im.getWidth(), im.getHeight(), f1.length*2, 8);
					ims = imp.getImageStack();
					for(int i=0,s=1;i<f1.length;i++,s=s+2)
					{
						ImageProcessor ip = IJ.openImage(f1[i].getPath()).getProcessor().convertToByte(true);					
						ims.setProcessor(ip, s);
						ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);
						ip.threshold(128);
						ip.invertLut();
						ip.erode();
						ip.dilate();
						ims.setProcessor(ip, s+1);
					}
					imp.show();
				}
				
			}else if(thresholdRoi)
			{
				ImagePlus imp = WindowManager.getCurrentImage();
				if(imp == null)
					imp = IJ.openImage();
				if(imp == null)
					return;
				int level = 99;
				Roi[] rois = getRoiFromManager();
				if(rois == null)
					return;
				for(int i=0; i<rois.length; i++)
				{
					Roi roi = rois[i];
					if(roi instanceof PointRoi || roi instanceof Line)
						continue;
					int position = roi.getPosition();
					ImageProcessor ip = imp.getImageStack().getProcessor(position);
					ip.setRoi(roi);
					ImageProcessor ipRoi = ip.crop();
					ipRoi.threshold(level);
					ip.insert(ipRoi, roi.getBounds().x, roi.getBounds().y);
				}
				System.out.println("local roi threshold done!");
				imp.updateAndDraw();
			}else if (refineFociRoi) {
				if(getRoiManager().getCount() == 0)
					return;
				Roi[] rois = getRoiFromManager();
				if(rois == null)
					return;
				ImagePlus imp = WindowManager.getCurrentImage();
				ImageStack ims = imp.getImageStack();
				ArrayList<Roi> roiNew = new ArrayList<Roi>();
				
				
				for(int i=0; i<rois.length; i++)
					roiNew.add(rois[i]);
				
				for(int s=1; s<=ims.getSize();s=s+2)
				{
					System.out.println("Current slice : " + s);
					Roi[] roiMask = constructRoisSliceAll(rois, s);
					if(roiMask == null)
						continue;
					Roi[] roiFoci = constructRoisSliceAll(rois, s+1);
					ImageProcessor ip = ims.getProcessor(s);
				
					for(int i=0; i<roiMask.length;i++)
					{
						System.out.println("foci mask " + i + "/" + roiMask.length);
						Roi mask = roiMask[i];
						int nInMask = 0;
						Roi[] roisToTreat = new Roi[5];
						for(int j=0; j<roiFoci.length; j++)
						{
							Roi foci = roiFoci[j];
							if(!(foci instanceof PointRoi))
								continue;
							if(mask.getBounds().contains(foci.getBounds().x, foci.getBounds().y))
							{
								roisToTreat[nInMask] = foci;
								nInMask++;
							}
						}
						if (nInMask == 0) {//no foci found or more than one foci is found, there should be one new PointRoi
							System.out.println(" 	no foci in the mask");
							ip.setRoi(mask);
							ImageStatistics iStatistics = ImageStatistics.getStatistics(ip, Measurements.CENTER_OF_MASS, null);
							PointRoi pRoi = new PointRoi(iStatistics.xCenterOfMass,iStatistics.yCenterOfMass);
							pRoi.setPosition(s+1);
							roiNew.add(pRoi);							
						}else if (nInMask > 1){
							
							System.out.println(" 	find more than one foci in the mask " + nInMask);
							for(int j=0;j<nInMask;j++)
							{
								if(roisToTreat[j].getBounds().x > 14682 && roisToTreat[j].getPosition() == 10 && roisToTreat[j].getBounds().x < 14684)
									System.out.println("index: " + roiNew.indexOf(roisToTreat[j]));
								System.out.println("to remove : " + roisToTreat[j].getBounds().x +"-"+roisToTreat[j].getPosition() + "-" + roisToTreat[j].getBounds().y +"-index-"+ roiNew.indexOf(roisToTreat[j]));
								int index = -1;
								for(int r=0; r<roiNew.size();r++)
								{
									if(roisToTreat[j].equals(roiNew.get(r)) && roisToTreat[j].getPosition() == roiNew.get(r).getPosition())
										index = r;
								}
								if(index >= 0)
									roiNew.remove(index);
							}
							ip.setRoi(mask);
							ImageStatistics iStatistics = ImageStatistics.getStatistics(ip, Measurements.CENTER_OF_MASS, null);
							PointRoi pRoi = new PointRoi(iStatistics.xCenterOfMass,iStatistics.yCenterOfMass);
							pRoi.setPosition(s+1);
							roiNew.add(pRoi);
							if(pRoi.getBounds().x > 14680 && pRoi.getPosition() == 10 && pRoi.getBounds().x < 14682)
								System.out.println("nInmask " + nInMask);
							
						}
						roiNew.remove(mask);
					}				
				}
				
				if(getRoiManager().getCount() > 0)
					deleteRoiToManager();

				for(int m=0; m<roiNew.size(); m++)
				{
					addRoiToManager(roiNew.get(m));
				}
			}
		}
				
							
		if (label.equals("Process"))
		{
			GenericDialog gDialog = new GenericDialog("option");
			gDialog.addCheckbox("Write to result table", writeResultTable);
			gDialog.addNumericField("Resize head cell size by adding", shiftHeadSize, 0);
			gDialog.addNumericField("Detect filament (ratio*median_cell_size)", filamentRatio,2);
			gDialog.showDialog();
			if(gDialog.wasCanceled())
				return;
			writeResultTable = gDialog.getNextBoolean();
			shiftHeadSize = (int)gDialog.getNextNumber();
			filamentRatio = gDialog.getNextNumber();
			iterationTotal = 0;			
			File[][] impTosegFile = getImageAndRoiZipFile();
			if(impTosegFile==null)
				return;
			for(int i=0;i<impTosegFile.length;i++)
			{				
				File[] f = impTosegFile[i];
				if(f==null)
					continue;
				ImagePlus impToseg = IJ.openImage(f[0].getPath());
				if(impToseg==null)
				{
					IJ.showMessage("No open image");
					return;
				}
				impToseg.show();
				parseImageTitle(impToseg);
				if(getRoiManager().getCount() > 0)
					deleteRoiToManager();
				openRoiZip(f[1].getAbsolutePath());
				lineageProcess(f[1].getParent(),mModeSilence.isSelected());				
			}
		}
		
		if (e.getSource() == preprocessButton)
		{										
			if(doRunAll)
			{
				if(paramsRFP == null)
				{
					IJ.showMessage("Parameters not initialized, please lauch Parametration before");
					return;
				}
				Ins_param[] pArray = paramsRFP.getIns_paramsRFPs();
				String path = paramsRFP.getDirectoryPath();				
				for(int p=0; p<pArray.length; p++)
				{
					if(!pArray[p].savedOnce())
						continue;					
					Ins_param param_position = pArray[p];										
					String outputDirectoryPath = path + Prefs.separator + param_position.getPositionName()+"-sx-"+String.valueOf(param_position.getStartX());		
					File dir = new File (outputDirectoryPath);
					dir.mkdir();
					System.out.println("mkdir : " + dir.getPath());	
					IJ.log("mkdir : " + dir.getPath() + " total params : " + pArray.length);
					File[] files= openFolderDialog(1,mSortByDate.isSelected(),param_position.getPositionName(), path);
					if(files==null)
						continue;				
					imp = load(files, param_position.getPositionName(), false);
					stackSize = imp.getStackSize();
					if(!param_position.getSealedOffAtTop())
					{
						if(imp != null)
							flipVertical(imp);
					}
					System.out.println("rotate angle : " + param_position.getAngle());
					if(param_position.getAngle()!=0.0d)		
						rotateImage(imp, param_position.getAngle());					
					param_position.compute_channel_prefix_pos();								
					channelNumber = param_position.getChannelNumber();
					
//					int startX = param_position.getStartX();
//					int startY = param_position.getStartY();				
					roi_width = param_position.getRoi_width();
					//posName = imp.getTitle();
					
					boolean memorySave = mEnabelAutoUpdate.isSelected();
					ProcessRunner iRunner=new ProcessRunner(imp, param_position);
					iRunner.runCommand(memorySave);
					param_position.setReady(true);
					ImagePlus impRFPs = null;
					impRFP = load(files, param_position.getPositionName(), false);
					if(impRFP == null)
						continue;

					saveImgName = param_position.getPositionName()+"-ss-"+imp.getStackSize()+"-roi-"+roi_width+"-sx-"+String.valueOf(param_position.getStartX());					
					if(param_position.ready())
					{
						impRFPs = calibrateAndSegmentation(impRFP, param_position);							
						impRFPs.setTitle(saveImgName);
						String savePath = outputDirectoryPath +Prefs.separator+"Original-"+ saveImgName+".tif";						
						IJ.save(impRFPs,savePath);						
					}else {
						IJ.error("No calibration parameters!");
					}
					System.gc();					
				}
			}else {
				Object[] objs= openFolderDialog(1,true,"");
				if(objs==null)
					return;
				File[][] files = (File[][])objs[0];
//				String[] folderName = (String[])objs[1];
				if(files == null)
					return;
				File[] transFiles = files[0];
				imp_ref = load(transFiles[0]);
				imp_ref.show();
			}
		}
		
		if (label.equals("Save Image")) {
			ImagePlus imp_save= WindowManager.getCurrentImage();
			if(imp_save==null)
				return;			
			FileSaver fs = new FileSaver(imp_save);
			fs.saveAsTiff();
		}
		if (label.equals("Rotate Image")) {
			imp_ref.getWindow().setAlwaysOnTop(false);			
			if (imp==null) return;	
		}
		
		if (label.equals("Stack to Images"))
		{
			ImagePlus imp = WindowManager.getCurrentImage();
			if(imp==null)
			{
				imp = IJ.openImage();
				if(imp == null)
					return;
			}
			Java2.setSystemLookAndFeel();
			ImageStack ims = imp.getImageStack();
			String pathDirectory = IJ.getDirectory("image");
			if(pathDirectory == null)
				pathDirectory = "D:";
		    File directory=new File(pathDirectory);	    
			JFileChooser  chooser = new JFileChooser(); 
		    chooser.setCurrentDirectory(directory);
		    chooser.setDialogTitle("Choose folder");
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
		    	pathDirectory = chooser.getSelectedFile().getPath();
		    }else {
				return;
			}
		    pathDirectory = pathDirectory + File.separator;
		    GenericDialog genericDialog = new GenericDialog("Stack to images");
		    genericDialog.addStringField("Position name", ims.getSliceLabel(1));
		    genericDialog.showDialog();
		    if(genericDialog.wasCanceled())
		    	return;
		    String title1 = genericDialog.getNextString();
			for(int s=1; s<ims.getSize(); s++)
			{			 
				String title = title1;
				title = title+String.valueOf(s);
				ImageProcessor ip = ims.getProcessor(s);			
				IJ.save(new ImagePlus("", ip), pathDirectory+title+".tif");
				System.out.println(pathDirectory+title+".tif");
			}
//			System.out.println("substract background");
//			File[] files = listFilesinSelectedFolder();		
//			if(files == null)
//				return;
//			String saveName = files[0].getName();
//			saveName = saveName.substring(0, saveName.indexOf("-"));
//			System.out.println("save file name : " + saveName);
//			String[] name = files[0].getName().split("-ss-");				
//			name = name[1].split("-roi-");
//			stackSize = Integer.valueOf(name[0]);		
//			name = files[0].getName().split("-roi-");
//			name = name[1].split(".tif");
//			roi_width = Integer.valueOf(name[0]);
//			saveName = saveName+"-ss-"+String.valueOf(stackSize)+"-roi-"+String.valueOf(roi_width);			
//			DirectoryChooser.setDefaultDirectory(IJ.getDirectory("home"));
//			DirectoryChooser directoryChooser = new DirectoryChooser("Select output folder");				
//			String outputDirectoryPath = directoryChooser.getDirectory();
//			if(outputDirectoryPath == null)
//			{
//				IJ.showMessage("No output folder selected");
//				return;
//			}
//			
//			int k = 0;
//			
//			for(File f:files)
//			{
//				ImagePlus imp =  IJ.openImage(f.getAbsolutePath());		
//				int width = imp.getWidth();
//				int height = imp.getHeight();
//				int total = files.length*stackSize*imp.getImageStackSize();
//				ImageStack imStack = imp.getImageStack();
//				ImagePlus impSub = IJ.createImage(imp.getTitle()+"-bg", width, height, imStack.getSize(), imp.getBitDepth());
//				for(int j=0;j<imStack.getSize();j++)
//				{
//					ImageProcessor ip = imStack.getProcessor(j+1);
//					String subTitle = imStack.getSliceLabel(j+1);
//					ImageProcessor ipSub = IJ.createImage("ipMask", "16-bit black", imp.getWidth(), imp.getHeight(), 1).getProcessor();
//					for(int i=0; i<stackSize;i++,k++)
//					{
//						System.out.println(String.valueOf(k)+"/" + total + " finished!");
//						IJ.showProgress((double)k/(double)total);
//						int x= i*(roi_width+6);			
//						Roi roi = new Roi(x, 0, roi_width, imp.getHeight());
//						ip.setRoi(roi);
//						ImageProcessor orig = ip.crop();					
//						BackgroundSubtracter bSubtracter = new BackgroundSubtracter();
//						bSubtracter.rollingBallBackground(orig, roi_width, false, false, false, true, true);
//						ipSub.insert(orig, x, 0);
//					}
//					impSub.getImageStack().setProcessor(ipSub, j+1);
//					impSub.getImageStack().setSliceLabel(subTitle, j+1);
//				}			
//				String savePath = outputDirectoryPath + impSub.getTitle()+".tif";						
//				IJ.save(impSub,savePath);
//			}
			
		}
		
		if (e.getSource() == segLineageButton)
		{
			GenericDialog gDialog = new GenericDialog("Segmentation");
			gDialog.addCheckbox("Fluorescence by SPF seeds image", false);
			gDialog.showDialog();
			if(gDialog.wasCanceled())
				return;
			boolean useSPF = gDialog.getNextBoolean();			
			File[] impTosegFile = getSegmentationImageFile();
			if(impTosegFile==null)
			{
				ImagePlus impToseg = WindowManager.getCurrentImage();
				if(impToseg==null)
				{
					impToseg = IJ.openImage();
					if(impToseg == null)
						return;
				}
				//System.out.println("path of image to process : " +impToseg.getOriginalFileInfo().directory);
				impToseg.show();
				parseImageTitle(impToseg);
				ArrayList<Roi> rois;
				if(useSPF)
				{
					rois = segmentationProcessRFPFromSeeds(impToseg);	
				}else {
					rois = segmentationProcessTrans(impToseg);
				}
				if(rois == null)
					return;				
				if(getRoiManager().getCount() > 0)
					deleteRoiToManager();
				for(Roi roi : rois)
				{
					addRoiToManager(roi);
				}
				String path = null;
				if(impToseg.getOriginalFileInfo() != null)
					path = impToseg.getOriginalFileInfo().directory;
				lineageProcess(path,mModeSilence.isSelected());
				return;
			}
				

			for(int i=0;i<impTosegFile.length;i++)
			{
				File f = impTosegFile[i];
				if(f==null)
					continue;

				ImagePlus impToseg = IJ.openImage(f.getPath());
				if(impToseg==null)
				{
					IJ.showMessage("No open image");
					return;
				}
				impToseg.show();
				parseImageTitle(impToseg);
				ArrayList<Roi> rois = null;
				if(useSPF)
				{
					rois = segmentationProcessRFPFromSeeds(impToseg);	
				}else {
					rois = segmentationProcessTrans(impToseg);
				}
				if(rois == null || rois.isEmpty())
					return;
				if(getRoiManager().getCount() > 0)
					deleteRoiToManager();
				for(Roi roi : rois)
				{
					addRoiToManager(roi);
				}
				lineageProcess(f.getParent(),mModeSilence.isSelected());
			}
		}

		
		if(label.equals("Fill roi"))
		{
			String pathDirectory = IJ.getDirectory("image");
			if(pathDirectory == null)
				pathDirectory = "D:";
		    File directory=new File(pathDirectory);	    
			JFileChooser  chooser = new JFileChooser(); 
		    chooser.setCurrentDirectory(directory);
		    chooser.setDialogTitle("Choose folder");
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
		    	File file = chooser.getSelectedFile();
		    	FileFilter filter = new FileFilter() {
					public boolean accept(File f) {
						if(f.isDirectory())
		    				return true;
		    			else {
		    				return false;
		    			}
					}
				};	    	
		    	File[] subfolders = file.listFiles(filter);
		    	File[] impFiles = new File[subfolders.length];
		    	for(int i=0; i<subfolders.length; i++)
		    	{
		    		if(!subfolders[i].isDirectory())
		    			continue;
		    		FilenameFilter filterN = new FilenameFilter() {
			    		public boolean accept(File dir, String name) {
			    			if(name.contains("ToOriginal")&& name.contains(".zip"))
			    				return true;
			    			else {
			    				return false;
			    			}					
			    		}
			    	};
			    	File[] impF = subfolders[i].listFiles(filterN);
			    	if(impF.length < 1)
			    		continue;
			    	impFiles[i] = impF[0];			    	
		    	}
		    	
		    	File[] zipRoi = new File[subfolders.length];
		    	for(int i=0; i<subfolders.length; i++)
		    	{
		    		if(!subfolders[i].isDirectory())
		    			continue;
		    		
		    		FilenameFilter filterN = new FilenameFilter() {
			    		public boolean accept(File dir, String name) {
			    			if(name.contains("lineage-")&& name.contains(".zip"))
			    				return true;
			    			else {
			    				return false;
			    			}					
			    		}
			    	};
			    	File[] impF = subfolders[i].listFiles(filterN);
			    	if(impF.length < 1)
			    		continue;
			    	zipRoi[i] = impF[0];			    	
		    	}
		    		
		    	for(int k = 0; k<impFiles.length; k++)
		    	{
		    		File f = impFiles[k];
		    		File fZip =zipRoi[k];
		    		if(f==null)
		    			continue;
		    		
		    		System.out.println(f.getParent() + " - " + fZip.getParent());
		    		
		    		if(!f.getParent().equals(fZip.getParent()))
		    			continue;
		    		
		    		File impF = unZipIt(f.getPath(),f.getParent());
		    		ImagePlus imp = IJ.openImage(impF.getAbsolutePath());		        	
		    		parseImageTitle(imp);
		    		openRoiZip(fZip.getPath());
		    		Roi[] rois = getRoiFromManager();
		    		ImagePlus imp2 = IJ.createImage("imporig", imp.getWidth(), imp.getHeight(), imp.getImageStackSize(), 32);
		    		FftBandPassFilter fftBandPassFilter = new FftBandPassFilter();
		    		fftBandPassFilter.setup("", imp);
		    		boolean bandPass = true;
		    		int n = stackSize;
		    		for(int s=1; s<=imp.getStackSize(); s = s + 1)//imp.getStackSize();
		    		{
		    			Roi[] roiSlice = null;
		    			if(s%3 == 1)
		    			{
		    				roiSlice = constructRoisSlice(rois, s + 1);
		    				if(roiSlice == null)
		    					roiSlice = constructRoisSlice(rois, s + 2);
		    			}else if (s%3 == 2) {
		    				roiSlice = constructRoisSlice(rois, s);
		    				if(roiSlice == null)
		    					roiSlice = constructRoisSlice(rois, s + 1);
						}else if (s%3 == 0) {
		    				roiSlice = constructRoisSlice(rois, s);
		    				if(roiSlice == null)
		    					roiSlice = constructRoisSlice(rois, s - 1);
						}
		    			if(roiSlice == null)
		    				continue;
		    			
		    			ImagePlus maskImp = getMaskFromRoi(roiSlice, imp.getWidth(), imp.getHeight());
		    			//maskImp.show();
		    			
		    			ImageProcessor ip = imp.getImageStack().getProcessor(s);
		    			ImageProcessor ip_orig = imp.getImageStack().getProcessor(s);
		    			
		    			ImagePlus impFFt = IJ.createImage("fft", imp.getWidth(), imp.getHeight(), 1, 32);
		    			ImagePlus impOrigRoi = IJ.createImage("origRoi", imp.getWidth(), imp.getHeight(), 1, 32);
		    			ImagePlus impOrig = IJ.createImage("orig", imp.getWidth(), imp.getHeight(), 1, 32);
		    			ImageProcessor ipFFt = null;// = impFFt.getProcessor();
		    			ImageProcessor ipOrigRoi = null;// = impFFt.getProcessor();
		    			ImageProcessor ipOrig = null;// = impFFt.getProcessor();
		    			
		    			for(int slice=0; slice<stackSize;slice++)
		    			{		    				
		    				if(slice%n == 0)
		    				{
		    					ipFFt = impFFt.getProcessor();
		    					ipOrigRoi = impOrigRoi.getProcessor();	
		    					ipOrig = impOrig.getProcessor();
		    					ImageProcessor ipRoiFill = maskImp.getProcessor();		    					
		    					for(int a=slice; a<slice+n; a++)
		    					{
//		    						if(a == slice + n -1)
//		    						{
//		    							System.out.println(" a : " + a);
//		    						}
		    						if(a>=stackSize)
		    						{
		    							continue;
		    						}
		    						int x= a*(roi_width+6);
		    						Roi roi = new Roi(x, 0, roi_width, imp.getHeight());
		    						ip.setRoi(roi);
		    						ImageProcessor ip2 = ip.crop();
		    						ImageProcessor ip2Orig = ip.crop();
		    						
		    						ipRoiFill.setRoi(roi);
		    						ImageProcessor ip2Mask = ipRoiFill.crop();
		    						
		    						ip2Mask.dilate();
		    						ip2Mask.dilate();
		    					
		    						
		    						
//		    						if(a < 10)
//		    						{
//		    							new ImagePlus(String.valueOf(a) + " mask", ip2Mask).show();
//		    							new ImagePlus(String.valueOf(a) + " ip2 ", ip2).show();
//		    						}
		    						
		    						double mean = 0;
		    						int meanN = 0;
		    						for(int xx=0;xx<ip2Mask.getWidth();xx++)
		    						{
		    							for(int yy=0; yy<ip2Mask.getHeight();yy++)
		    							{
		    								if(ip2Mask.get(xx, yy) == 0)
		    									continue;		    								
		    								mean = mean + ip2.getf(xx, yy);
		    								meanN++;
		    							}
		    						}
		    						if(meanN != 0)
		    						{
		    							mean = mean / meanN;
		    							for(int xx=0;xx<ip2Mask.getWidth();xx++)
		    							{
		    								for(int yy=0; yy<ip2Mask.getHeight();yy++)
		    								{
		    									if(ip2Mask.get(xx, yy) == 0)
		    										ip2.setf(xx, yy, (float)mean); ;
		    								}
		    							}
		    						}
		    						ipFFt.insert(ip2, a%n*(roi_width), 0);
		    						ipOrigRoi.insert(ip2, a%n*(roi_width), 0);
		    						ipOrig.insert(ip2Orig, a%n*(roi_width), 0);
		    					}
		    					fftBandPassFilter.setup("fft band pass filter", impFFt);
		    					fftBandPassFilter.run(ipFFt);		    					
//		    					if(slice == 0 && true)
//		    					{
//		    						new ImagePlus("ipFFT", ipFFt).show();
//		    						new ImagePlus("ipOrigRoi", ipOrigRoi).show();
//		    						new ImagePlus("ipOrig", ipOrig).show();
//		    					}
		    				}

		    				int xx= (slice%n)*(roi_width);			
		    				Roi roi = new Roi(xx, 0, roi_width, imp.getHeight());
		    				impFFt.setRoi(roi);			    				
		    				ImageStatistics iStatistics = ImageStatistics.getStatistics(ipFFt, Measurements.MEAN, null);
		    				double meanFFT = iStatistics.mean;
		    				
		    				impOrigRoi.setRoi(roi);		    				
		    				iStatistics = ImageStatistics.getStatistics(ipOrigRoi, Measurements.MEAN, null);
		    				double meanOrig = iStatistics.mean;
		    				double diff = meanOrig - meanFFT;
		    				
		    				impOrig.setRoi(roi);
		    				
		    				//System.out.println("slice: " + s + " current slice : " + slice + " diff : " + diff);
		    				
		    				ImageProcessor ipCrop;
		    				if(bandPass)
		    				{
		    					ipCrop = impOrig.getProcessor().crop();
		    					ipCrop.add(0-diff);
		    				}else {
		    					ipCrop = impOrig.getProcessor().crop();
							}
	    					ip_orig.insert(ipCrop, slice * (roi_width + 6), 0);
		    			}
		    			imp2.getImageStack().getProcessor(s).insert(ip_orig, 0, 0);
		    		}
		    		IJ.save(imp2, impF.getParent()+File.separator+"fft-"+imp.getTitle()+".tif");
		    		System.out.println("image to be segmented and lineaged : " + impF.getAbsolutePath());	 
		    		deleteRoiToManager();
		    	}
		    	
		    }
		}
			
//			Frame frame = WindowManager.getFrame("ROI Manager");					
//			if (frame==null || !(frame instanceof RoiManager))
//				{return;}
//			RoiManager roiManager = (RoiManager)frame;
//			ImagePlus imp = WindowManager.getCurrentImage();
//			ImagePlus impFilled = IJ.createImage(imp.getTitle()+"-mean", imp.getWidth(), imp.getHeight(), imp.getStackSize(), imp.getBitDepth());
//			ImageStack ims = imp.getImageStack();
//			ImageStack imsFilled = impFilled.getImageStack();			
//			Roi[] roisArrayRois = roiManager.getRoisAsArray();
//			ImageStatistics iStatistics = new ImageStatistics();
//			int mOptions = Measurements.MEAN;
//			for(Roi roi:roisArrayRois)
//			{
//				int slice = roi.getPosition();
//				ImageProcessor ip;
//				if(slice == 0)
//					ip = imp.getProcessor();
//				else {
//					ip = ims.getProcessor(slice);
//				}
//				ip.resetRoi();
//				ip.setRoi(roi);
//				iStatistics = ImageStatistics.getStatistics(ip, mOptions, null);
//				double mean = iStatistics.mean;				
//				ImageProcessor ipFilled;
//				if(slice == 0)
//				{
//					ipFilled = impFilled.getProcessor();
//				}else {
//					ipFilled = imsFilled.getProcessor(slice);
//				}
//				ipFilled.setValue(mean);
//				ipFilled.fillPolygon(roi.getPolygon());				
//			}
//			impFilled.show();
		
		
		
		
		if(label.equals("Double&Triple stack"))
		{					
			
			GenericDialog genericDialog = new GenericDialog("Double & Triple stack");
			genericDialog.addCheckbox("Triple stack", false);
			genericDialog.showDialog();
			if(genericDialog.wasCanceled())
			{
				return;
			}
			boolean triple = genericDialog.getNextBoolean();
			
		    ImagePlus imp = null;
			if(imp == null)
			{
				imp = IJ.openImage();
				if(imp == null)
					return;
			}
			
			ImagePlus imp2 = IJ.openImage();
			if(imp2==null)
				return;
			
			ImagePlus imp3 = null; 
			if(triple)
			{
				imp3 = IJ.openImage();
				if(imp3 == null)
					return;
			}
			
			ImagePlus impRes;
			if(triple)
			{
				impRes = tripleStack(imp, imp2, imp3);
			}else {
				impRes = doubleStack(imp, imp2, false);				
			}
			
			if(impRes!=null)
				impRes.show();
		}
	}
	
    private ImagePlus getMaskFromRoi(Roi[] roiSlice, int width, int height) {
    	ImagePlus imp = IJ.createImage("fillRoi", "8-bit white", width, height, 1);
    	imp.getProcessor().setValue(0);
    	for(Roi r:roiSlice)
    		imp.getProcessor().fill(r);
		return imp;
	}

	public File unZipIt(String zipFile, String outputFolder){
    	byte[] buffer = new byte[1024];
    	try{
    		//create output directory is not exists
    		File folder = new File(outputFolder);
    		if(!folder.exists()){
    			folder.mkdir();
    		}
    		//get the zip file content
    		ZipInputStream zis = 
    				new ZipInputStream(new FileInputStream(zipFile));
    		//get the zipped file list entry
    		ZipEntry ze = zis.getNextEntry();
    		File newFile = null;
    		while(ze!=null){
    			String fileName = ze.getName();
    			newFile = new File(outputFolder + File.separator + fileName);
    			System.out.println("file unzip : "+ newFile.getAbsoluteFile());    			
    			//create all non exists folders
    			//else you will hit FileNotFoundException for compressed folder
    			new File(newFile.getParent()).mkdirs();

    			FileOutputStream fos = new FileOutputStream(newFile);             

    			int len;
    			while ((len = zis.read(buffer)) > 0) {
    				fos.write(buffer, 0, len);
    			}

    			fos.close();   
    			ze = zis.getNextEntry();
    		}
    		zis.closeEntry();
    		zis.close();
    		
    		System.out.println("Done");
    		return newFile;
    	}catch(IOException ex){
    		ex.printStackTrace();
    		return null;
    	}
    }    
   


	
	public ImagePlus combineImp(ImagePlus[] imp,String title)
	{
		for(ImagePlus im:imp)
		{
			if(im==null)
				return null;
		}
		
		int stacksize = imp[0].getStackSize();		
		ImagePlus impDouble;		
		if(imp[0].getProcessor() instanceof ShortProcessor)
			impDouble = IJ.createImage(title, "16-bit black", imp[0].getWidth(), imp[0].getHeight(), stacksize*imp.length);
		else {
			impDouble = IJ.createImage(title, "8-bit black", imp[0].getWidth(), imp[0].getHeight(), stacksize*imp.length);
		}		
		for(int i=1; i<=imp.length*stacksize;i=i+imp.length)
		{
			for(int k=0;k<imp.length;k++)
			{
				impDouble.getImageStack().getProcessor(i+k).insert(imp[k].getImageStack().getProcessor((i-1)/imp.length + 1), 0, 0);
				if(k==0)
					impDouble.getImageStack().setSliceLabel("YFP-" + String.valueOf((i-1)/imp.length + 1), i+k);
				else if (k==1) {
					impDouble.getImageStack().setSliceLabel("CFP-"+ String.valueOf((i-1)/imp.length + 1), i+k);
				}else if (k==2) {
					impDouble.getImageStack().setSliceLabel("RFP-"+ String.valueOf((i-1)/imp.length + 1), i+k);
				}
			}
		}
		return impDouble;
	}
	
	private ImagePlus doubleStack(ImagePlus imp1, ImagePlus imp2, boolean movieMode)
	{			
		int stacksize = imp1.getStackSize();
		ImagePlus impDouble;		
		if(!movieMode)
		{
			if(imp1 == null || imp2 == null || imp1.getWidth() != imp2.getWidth() || imp1.getHeight() != imp2.getHeight() || imp1.getStackSize() != imp2.getStackSize())
				return null;
			if(imp1.getProcessor() instanceof FloatProcessor || imp2.getProcessor() instanceof FloatProcessor)
			{
				impDouble = IJ.createImage("double stack", "32-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*2);
			}else if(imp1.getProcessor() instanceof ShortProcessor || imp2.getProcessor() instanceof ShortProcessor)
				impDouble = IJ.createImage("double stack", "16-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*2);
			else {
				impDouble = IJ.createImage("double stack", "8-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*2);
			}
			for(int i=1; i<=2*stacksize;i=i+2)
			{
				impDouble.getImageStack().getProcessor(i).insert(imp1.getImageStack().getProcessor((i+1)/2), 0, 0);
				impDouble.getImageStack().getProcessor(i+1).insert(imp2.getImageStack().getProcessor((i+1)/2), 0, 0);			
			}		
			impDouble.setTitle("Double-"+imp1.getTitle());
			return impDouble;
		}else {
			if(imp1 == null || imp2 == null || imp1.getStackSize() != imp2.getStackSize())
				return null;
			
			int width = imp1.getWidth() + imp2.getWidth();
			int height = Math.max(imp1.getHeight(), imp2.getHeight());
			
			if(imp1.getProcessor() instanceof FloatProcessor || imp2.getProcessor() instanceof FloatProcessor)
			{
				impDouble = IJ.createImage("double stack", "32-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*2);
			}else if(imp1.getProcessor() instanceof ByteProcessor || imp2.getProcessor() instanceof ByteProcessor)
				impDouble = IJ.createImage("double stack", "8-bit black", width, height, stacksize);
			else {
				impDouble = IJ.createImage("double stack", "16-bit black", width, height, stacksize);	
			}
			for(int i=1; i<=stacksize;i++)
			{
				impDouble.getImageStack().getProcessor(i).insert(imp1.getImageStack().getProcessor(i).convertToByte(true), 0, 0);
				impDouble.getImageStack().getProcessor(i).insert(imp2.getImageStack().getProcessor(i).convertToByte(true), imp1.getWidth(), 0);							
			}
			return impDouble;
		}
		
	}
	
	public ImagePlus tripleStack(ImagePlus imp1, ImagePlus imp2, ImagePlus imp3)
	{			
		int stacksize = imp1.getStackSize();
		ImagePlus impTriple;		

		if(imp1 == null || imp2 == null || imp1.getWidth() != imp2.getWidth() || imp1.getHeight() != imp2.getHeight() || imp1.getStackSize() != imp2.getStackSize())
			return null;
		if(imp1.getProcessor() instanceof FloatProcessor || imp2.getProcessor() instanceof FloatProcessor)
		{
			impTriple = IJ.createImage("double stack", "32-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*3);
		}else if(imp1.getProcessor() instanceof ShortProcessor || imp2.getProcessor() instanceof ShortProcessor)
			impTriple = IJ.createImage("double stack", "16-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*3);
		else {
			impTriple = IJ.createImage("double stack", "8-bit black", imp1.getWidth(), imp1.getHeight(), stacksize*3);
		}
		for(int i=1; i<=3*stacksize;i=i+3)
		{
			impTriple.getImageStack().getProcessor(i).insert(imp1.getImageStack().getProcessor((i+2)/3), 0, 0);
			impTriple.getImageStack().getProcessor(i+1).insert(imp2.getImageStack().getProcessor((i+2)/3), 0, 0);	
			impTriple.getImageStack().getProcessor(i+2).insert(imp3.getImageStack().getProcessor((i+2)/3), 0, 0);	
		}	
		impTriple.setTitle("Triple"+imp1.getTitle());
		return impTriple;

	}
	
	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		
		if(e.getSource() == mEnabelAutoUpdate)
		{
			if (mEnabelAutoUpdate.isSelected() )
			{
				//updateLineage = true;
			}else {
				//updateLineage = false;			
			}
		}	
		
		if(e.getSource() == mSortByDate)
		{
			if (mSortByDate.isSelected())
			{
				System.out.println("sort files by time");
				sortBydate = true;
			}else {
				System.out.println("sort files by name");
				sortBydate = false;				
			}
		}
		
		if(e.getSource() == m63x)
			if(m63x.isSelected() && !setM63x)
			{
				setM63xParameter();
			}

		if(e.getSource() == m100x)
			if(m100x.isSelected() && setM63x)
			{
				setM100xParameter();
			}

		if(e.getSource()==mChDirection)
		{
//			if(!mChDirection.isSelected())
//			{
////				IJ.showStatus("Image flipped!");
////				if(WindowManager.getCurrentImage()!=null)
////					flipVertical(WindowManager.getCurrentImage());
//				//ip_ref = imp.getProcessor().duplicate();
//			}
		}
	      
	}

	 public static ImagePlus mergingRGB(ImagePlus imp1, ImagePlus imp2, ImagePlus imp3)
	 {
		 if(imp1==null ||imp2==null || imp3 == null)
			 return null;
		 int size = imp1.getStackSize();
		 int width = imp1.getWidth();
		 int height = imp1.getHeight();
		 ImagePlus impRGB = IJ.createImage(imp1.getTitle()+"-"+imp2.getTitle()+"-"+imp3.getTitle(), "RGB", imp1.getWidth(), imp2.getHeight()+20, size);
		 ImagePlus imp1N = imp1;
		 ImagePlus imp2N = imp2;
		 ImagePlus imp3N = imp3;




		 for(int i=1; i<=size;i++)
		 {
			 ImageProcessor ip1 = imp1N.getImageStack().getProcessor(i).convertToByte(true);
			 ImageProcessor ip2 = imp2N.getImageStack().getProcessor(i).convertToByte(true);
			 ImageProcessor ip3 = imp3N.getImageStack().getProcessor(i).convertToByte(true);
			 ColorProcessor cp = new ColorProcessor(width, height);
			 cp.setRGB((byte[])ip1.getPixels(), (byte[])ip2.getPixels(), (byte[])ip3.getPixels());    		
			 impRGB.getImageStack().getProcessor(i).insert(cp, 0, 0);;    		
		 }
		 return impRGB;
	 }

	 
	
	public static ImagePlus load(File f) {
		if ((f == null) || (f.getName() == "")) return null;			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(f.getPath());
		return imp; 
	}
	
	public static ImagePlus load(File[] f, String stackName, boolean doDenoise) {		
		if ((f == null)) return null;			
		Opener fo = new Opener();
		ImagePlus imp = fo.openImage(f[0].getPath());
		if(imp == null) return null;
		ImageStack ims = new ImageStack(imp.getWidth(), imp.getHeight());
		int i=1; 
		int s=1;
		for(File file : f)
		{
			imp = fo.openImage(file.getPath());
			if(imp==null)
				continue;
			ImageProcessor sp = imp.getProcessor();
			
			String name = imp.getTitle();
			if(doDenoise)
			{
				System.out.println("Calibrate rfp : denoising " + file.getName());
				IJ.showStatus("Denoise : " + i+"/"+f.length);
				FloatProcessor ipFloat = (FloatProcessor)sp.convertToFloat();
				ROF_Denoise.denoise(ipFloat, 25);
				sp = (ShortProcessor)ipFloat.convertToShort(false);
				i++;
			}
			String nameSlice;
			try {
				nameSlice = name.substring(name.lastIndexOf('t'), name.lastIndexOf('.'));
			} catch (Exception e) {
				nameSlice = String.valueOf(s);
			}
			ims.addSlice(nameSlice, sp);
			s++;
		}
		return new ImagePlus(stackName, ims); 
	}
	
	
	public File[] listFilesinSelectedFolder()
	{
		String pathDirectory = IJ.getDirectory("current");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) {
	    	FilenameFilter filter = new FilenameFilter() {
	    		public boolean accept(File dir, String name) {
	    			if(name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff"))
	    				return true;
	    			else {
	    				return false;
	    			}					
	    		}
	    	};	    
	    	File subFolder = chooser.getSelectedFile();
	    	File[] files = subFolder.listFiles(filter);
	    	return files;
	    }else {
	    	return null;
	    }

	}
	
	public File[] getSegmentationImageFile()
	{
		String pathDirectory = Prefs.getDefaultDirectory();
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	File file = chooser.getSelectedFile();
	    	FileFilter filter = new FileFilter() {
				public boolean accept(File f) {
					if(f.isDirectory())
	    				return true;
	    			else {
	    				return false;
	    			}
				}
			};	    	
	    	File[] subfolders = file.listFiles(filter);
	    	File[] impFiles = new File[subfolders.length];
	    	for(int i=0; i<subfolders.length; i++)
	    	{
	    		if(!subfolders[i].isDirectory())
	    			continue;
	    		
	    		FilenameFilter filterN = new FilenameFilter() {
		    		public boolean accept(File dir, String name) {
		    			if(name.contains("-ss-")&& (name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff")))
		    				return true;
		    			else {
		    				return false;
		    			}
		    		}
		    	};
		    	File[] impF = subfolders[i].listFiles(filterN);
		    	if(impF.length != 1)
		    	{
		    		IJ.showMessage("More than one files named -ss- found, please clean the selected folder");
		    		continue;
		    	}
		    	impFiles[i] = impF[0];		    	
	    	}
	    		    	
	    	for(File f : impFiles)
	    	{
	    		if(f==null)
	    			continue;
	    		System.out.println("image to be segmented and lineaged : " + f.getAbsolutePath());	    		
	    	}
	    	return impFiles;
	    }else {
	    	return null;
	    }
	}
	
	public File[][] getImageAndRoiZipFile()
	{
		String pathDirectory = IJ.getDirectory("image");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	File file = chooser.getSelectedFile();
	    	FileFilter filter = new FileFilter() {
				public boolean accept(File f) {
					if(f.isDirectory())
	    				return true;
	    			else {
	    				return false;
	    			}
				}
			};	    	
	    	File[] subfolders = file.listFiles(filter);
	    	File[][] impFiles = new File[subfolders.length][2];
	    	int k = 0;
	    	for(int i=0; i<subfolders.length; i++)
	    	{
	    		if(!subfolders[i].isDirectory())
	    			continue;
	    		
	    		FilenameFilter filterN = new FilenameFilter() {
		    		public boolean accept(File dir, String name) {
		    			if((name.contains("-ss-")&& (name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff"))&& !name.contains(".zip"))
		    					|| (name.contains("-ss-")&& name.contains(".zip")&& name.contains("corrected")))
		    				return true;
		    			else {
		    				return false;
		    			}					
		    		}
		    	};
		    	File[] impF = subfolders[i].listFiles(filterN);
		    	if(impF.length != 2)
		    		continue;
		    	
		    	if(impF[0].getName().contains(".zip"))
		    	{
		    		File tmp = impF[0];
		    		impF[0] = impF[1];
		    		impF[1] = tmp;
		    	}
		    	impFiles[k] = impF;		    	
		    	k = k + 1;		    	
	    	}
	    	
	    	if(k < 1)
	    		return null;
	    	File[][] temp = new File[k][2];
	    	System.arraycopy(impFiles, 0, temp, 0, k);
	    	
//	    	for(File f : impFiles)
//	    	{
//	    		if(f==null)
//	    			continue;
//	    		System.out.println("image to be segmented and lineaged : " + f.getAbsolutePath());	    		
//	    	}
	    	return temp;
	    }else {
	    	return null;
	    }
	}
	
	public Object[] getRefImages()
	{
		String pathDirectory = IJ.getDirectory("current");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    GenericDialog gD = new GenericDialog("Input reference image number");
	    //gD.addNumericField("Input the reference image number", 20, 0);
	    gD.addStringField("Input the reference image name, ex, _t10.", "_t10.");
	    gD.showDialog();
	    if(gD.wasCanceled())
	    	return null;
	    String n = gD.getNextString();
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	File file = chooser.getSelectedFile();
	    	FilenameFilter filter = new FilenameFilter() {
	    		public boolean accept(File dir, String name) {
	    			if(((name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff"))))
	    				return true;
	    			else {
	    				return false;
	    			}					
	    		}
	    	};
	    	
	    	String[] names = file.list(filter);
	    	
	    	String[] ref_names = new String[names.length];
	    	int j = 0;
	    	for(int i=0 ; i< names.length; i++)
	    	{
	    		if (names[i].indexOf(n)>0)
	    			ref_names[j++] = names[i];
	    	}
	    	
	    	String[] names_new = new String[j];
	    	System.arraycopy(ref_names, 0, names_new, 0, j);
	    	
	    	Arrays.sort(names_new, new Comparator<String>() {
	    		public int compare(String arg0, String arg1) {
	    			int n1 = extractNumber(arg0);
	    			int n2 = extractNumber(arg1);
	    			return n1 - n2;
	    		}
	    		private int extractNumber(String name) {
	    			int i = 0;
	    			try {
	    				int s = name.lastIndexOf("_s") + 2;
	    				int e = name.lastIndexOf("_t");
	    				String number = name.substring(s, e);
	    				int i0 = Integer.parseInt(number);
	    				
    					int s1 = name.lastIndexOf("_t") + 2;
	    				int e1 = name.lastIndexOf('.');
	    				String number1 = name.substring(s1, e1);		    			
	    				int i1 = Integer.parseInt(number1);

	    				i = i0*100000+ i1;
	    			} catch(Exception e) {
	    				i = 0;
	    			}
	    			return i;
	    		}
	    	});

	    	ImagePlus[] imp = new ImagePlus[names_new.length];
	    	int i=0;
	    	for(String name : names_new)
	    	{
	    		String path = file.getPath() +Prefs.getFileSeparator()+ name;
	    		System.out.println("reference image path : " + path);
	    		imp[i++] = IJ.openImage(path);
	    	}
	    	return new Object[]{imp, file.getPath()};
	    }else {
	    	return null;
	    }
	    
	}
	
	private int extractNumber(String name) {
		int i = 0;
		name = name.substring(0, name.lastIndexOf('.'));
		try {
			int s = name.lastIndexOf('t') + 1;			
			String number = name.substring(s, name.length());		    			
			i = Integer.parseInt(number);
		} catch(Exception e) {
			try {
				int s = name.lastIndexOf('s') + 1;
				int e1 = name.lastIndexOf("_t");
				String number = name.substring(s, e1);		    			
				i = Integer.parseInt(number);
			} catch (Exception e2) {
				i=0;
			}
		}
		return i;
	}
	
	public File[] openFolderDialog(int interval,boolean sortBydate, final String regle) {
		String pathDirectory = IJ.getDirectory("current");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	System.out.println("getCurrentDirectory(): " 
	    			+  chooser.getCurrentDirectory());
	    	System.out.println("getSelectedFile() : " 
	    			+  chooser.getSelectedFile());
	    	
		    File file = chooser.getSelectedFile();
		    
		    FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if(name.contains(regle)&& (name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff")))
						return true;
					else {
						return false;
					}					
				}
			};
			
			File[] files = null;
			String pathSubFolder = file.getPath();
			File subFolder = new File(pathSubFolder);
			files = subFolder.listFiles(filter);
		    	if(files!=null)
		    	Arrays.sort(files, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('t') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });

			    for(File f : files) {
			    	System.out.println("Selected file " + f.getPath());
			    }
			    
		    return files;
	    }else {
	    	System.out.println("No Selection ");
	    	return null;
	    }
	}
	
	public File[] openFolderDialog(int interval) {
		String pathDirectory = Prefs.getDefaultDirectory();;
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	System.out.println("getCurrentDirectory(): " 
	    			+  chooser.getCurrentDirectory());
	    	System.out.println("getSelectedFile() : " 
	    			+  chooser.getSelectedFile());
	    	
		    File file = chooser.getSelectedFile();
		    
		    FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if((name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff")))
						return true;
					else {
						return false;
					}					
				}
			};
			
			File[] files = null;
			String pathSubFolder = file.getPath();
			File subFolder = new File(pathSubFolder);
			files = subFolder.listFiles(filter);
		    	if(files!=null)
		    	Arrays.sort(files, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('s') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });

			    for(File f : files) {
			    	System.out.println("Selected file " + f.getPath());
			    }
			    
		    return files;
	    }else {
	    	System.out.println("No Selection ");
	    	return null;
	    }
	}
	
	public File[] openFolderDialog(int interval,boolean sortBydate, final String regle, String path) {	    
	    if (path!=null) { 
	    	File file = new File(path);
	    	FilenameFilter filter = new FilenameFilter() {
	    		public boolean accept(File dir, String name) {
	    			if(name.contains(regle)&& (name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff")))
	    				return true;
	    			else {
	    				return false;
	    			}					
	    		}
	    	};
	    	File[] filesTrans = null;
	    	String pathSubFolder = file.getPath();
	    	File subFolder = new File(pathSubFolder);
	    	filesTrans = subFolder.listFiles(filter);

	    	if(filesTrans!=null)
	    		Arrays.sort(filesTrans, new Comparator<File>() {
	    			@Override
	    			public int compare(File o1, File o2) {
	    				int n1 = extractNumber(o1.getName());
	    				int n2 = extractNumber(o2.getName());
	    				return n1 - n2;
	    			}
	    			private int extractNumber(String name) {
	    				int i = 0;
	    				try {
	    					
	    					int e = name.lastIndexOf('.');	    					
	    					int s = name.substring(0, e).lastIndexOf('t') + 1;
	    					String number = name.substring(s, e);		    			
	    					i = Integer.parseInt(number);
	    				} catch(Exception e) {
	    					i = 0; // if filename does not match the format
	    					// then default to 0
	    				}
	    				return i;
	    			}
	    		});

	    	for(File f : filesTrans) {
	    		System.out.println("Open file " + f.getPath());
	    	}
	    	return filesTrans;
	    }else {
	    	System.out.println("No Selection ");
	    	return null;
	    }
	}
	
	
	public static long getAcuquisitionTime(File f) {		
		//if ((name == null) || (name == "")) return 0;			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(f.getPath());
		Properties properties = imp.getProperties();
		Date date = null;
		if(properties != null)
		{
			String p = properties.toString();
			String t = null;
			if (p.contains("acquisition-time-local")) {
				t = p.substring(p.indexOf("acquisition-time-local")+43,p.indexOf("acquisition-time-local")+60);
				//System.out.println(t);
			}else {
				return f.lastModified();
			}	
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");				    						
			try {    
				date = formatter.parse(t);
				//System.out.println(name+" "+date.toString());
			} catch (ParseException e) {    
				e.printStackTrace();    
			}
			return date.getTime();
		}else {
			return f.lastModified();
		}		 
	}
	
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException
			(l + " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_D){			
			   //delete();
		}
		if(e.getKeyCode() == KeyEvent.VK_C){
			   //delete(true);
		}
		if(e.getKeyCode()== KeyEvent.VK_V)
		{
			if(!aPressedOnce)
			{
				WindowManager.getCurrentImage().getCanvas().setShowAllROIs(false);			
				WindowManager.getCurrentImage().draw();
				aPressedOnce = true;				
			}else
			{
				WindowManager.getCurrentImage().getCanvas().setShowAllROIs(true);			
				WindowManager.getCurrentImage().draw();
				aPressedOnce = false;
			}
		}
		
		if(e.getKeyCode() == KeyEvent.VK_ALT)
		{
		/*	Frame frame = WindowManager.getFrame("ROI Manager");					
			if (frame==null || !(frame instanceof RoiManager))
				{return;}
			RoiManager roiManager = (RoiManager)frame;			
			Roi roi = null;
			if(WindowManager.getCurrentImage() != null)
				roi = WindowManager.getCurrentImage().getRoi();
			else {
				return;
			}
			if(roi == null)
				return;			
			roi = new Roi(new Rectangle((computeTimeIndex(roi.getBounds().x))*(roi_width+blankwidth), roi.getBounds().y, roi_width, roi.getBounds().height));			
			roiManager.addRoi(roi);
			if (updateLineage)
			{
				lineageAnalyze(false,false);
				//setRoisVisible();
			}*/
		}
		
		
	/*	if(e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			if(list == null)
				return;
			int count = list.getItemCount();
			index_roi2 = list.getSelectedIndex();	
			if(index_roi2 == -1)
				return;
			System.out.println("Roi2 selected : "+list.getItem(index_roi2));
			IJ.showStatus("Roi2 selected : "+list.getItem(index_roi2));
			for (int i=count-1; i>=0; i--) {				
				if (index_roi2==i)
					roi2Selected = true;
			}
			
			if(roi1Selected&&roi2Selected && index_roi1!=index_roi2)
			{
				merge(index_roi1,index_roi2);
			}			
			roi1Selected = false;
			roi2Selected = false;
		}

		if(e.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			if(list == null || rois == null)
			{
				Frame frame = WindowManager.getFrame("ROI Manager");					
				if (frame==null || !(frame instanceof RoiManager))
					{return;}
				RoiManager roiManager = (RoiManager)frame;
				list = roiManager.getList();
				rois = roiManager.getROIs();
			}	
			if(list == null || rois == null)
			{
				return;
			}					
			int count = list.getItemCount();						
			index_roi1 = list.getSelectedIndex();
			if(index_roi1 == -1)
				return;				
			System.out.println("Roi1 selected : "+list.getItem(index_roi1));
			IJ.showStatus("Roi1 selected : "+list.getItem(index_roi1));
			for (int i=count-1; i>=0; i--) {				
				if (index_roi1==i)
					roi1Selected = true;
			}
		}*/
	}	
	
/*	public static int computeTimeIndex(int x)
	{
		int timeIndex = -1;
		float width = Ins_seg_panel.widthImp- Ins_seg_panel.blankwidth;		
		float inter_channel = width/(float)Ins_seg_panel.stackSize;						
		if(x <= roi_width*0.5 + inter_channel/2)
			timeIndex = 1;
		else if (x >= width-roi_width*0.5-inter_channel/2) {
			timeIndex = Ins_seg_panel.stackSize;			
		}else {
			for(int i=2; i<=Ins_seg_panel.stackSize-1; i=i+1)
			{		
				if(x >= ((i-1)*inter_channel + roi_width*0.5 - 0.5*inter_channel) && x <((i-1)*inter_channel + roi_width*0.5 + 0.5*inter_channel)){
					timeIndex = i;
					break;
				}
			}
		}
		//System.out.println("x : "+x +"time index : " + timeIndex + " Stack size : " +stackSize + " width_img " + width );
		timeIndex = timeIndex-1;
		return timeIndex;
	}*/
	
	public static int computeTimeIndex(double x)
	{
		int timeIndex = ((int) x)/(roi_width + blankwidth);
		return timeIndex;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	@Override
	public void keyPressed(KeyEvent e) {
		//System.out.println("delete pressed ! ");
			
	}
	
	
	protected void measureRoi(Roi roiRFP, ImageProcessor ip2, int measurements, RoiPropeties rP) {
		ip2.resetRoi();
		ip2.setRoi(roiRFP);
		ImageStatistics stats = ImageStatistics.getStatistics(ip2, measurements, null);
		rP.perimeter = roiRFP.getLength();
		double circularity = rP.perimeter==0.0?0.0:4.0*Math.PI*(stats.pixelCount/(rP.perimeter*rP.perimeter));				
		if (circularity>1.0) circularity = 1.0;		
		rP.xCentroid = stats.xCentroid;
		rP.yCentroid = stats.yCentroid;
		rP.circularity = circularity;
		rP.meanIntensity = stats.mean;
		rP.sumIntensity = stats.mean*stats.area;
		rP.area = stats.area;
		rP.length = stats.roiHeight;
	}


	
	/**
	 * 
	 * @param list
	 * @param rois
	 * @return construct the cells from the list and rois precomputed
	 */
//	public Ins_cell[] constructCellsListFromStackMarina(List list, Hashtable<String, Roi> rois,int slice, boolean fluorence, boolean openChannel)
//	{
//		boolean skeletonize = mModeSilence.isSelected();
//		//RoiPropeties rPFC = new RoiPropeties();
//		
//		Ins_cell[] cells= new Ins_cell[list.getItemCount()];	
//		int count = list.getItemCount();
//		int j = 0;				
//
//		//slice_cell = imp.getStackSize() -1;
//		int mOption = Measurements.CENTROID|Measurements.MEAN|Measurements.AREA;
//		
//		ImageProcessor ipSkeleton = null;
//		if(skeletonize)
//		{
//			ipSkeleton =WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
//			ipSkeleton = ipSkeleton.convertToRGB();
//		}
//		if(fluorence)
//		{
//			ImageProcessor ip = WindowManager.getCurrentImage().getImageStack().getProcessor(slice);			
//			for(int i=list.getItemCount()-1; i>=0; i--)
//			{
//				String label = list.getItem(i);
//				Roi roi = (Roi)rois.get(label);
//				
//				if(roi.getPosition() != slice)
//					continue;
//				
//				if(mChRFP.isSelected()){
//					if(!(roi instanceof Arrow || roi instanceof PointRoi))
//					{
//						Foci[] focies = new Foci[10];
//						boolean manulTrackingCell = false;
//						int numFoci = 0;
//						Comet[] comets = null;// new Comet[10];
//						//int numComet = 0;
//						ip.resetRoi();
//						ip.setRoi(roi);							
//						ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);
//						
//						for(int id = list.getItemCount()-1;id>=0 ; id--)
//						{
//							String labelRFP = list.getItem(id);
//							Roi roiRFP = (Roi) rois.get(labelRFP);
//							if(!(roiRFP instanceof PointRoi))
//								continue;
//							
//							if(!openChannel)
//							{
//								if(roiRFP.getPosition() == slice)
//								{
//									PointRoi roiPoint = (PointRoi) rois.get(labelRFP);
//									int numPoints = roiPoint.getPolygon().npoints;
//									for(int p=0;p<numPoints;p++)
//									{
//										int x = roiRFP.getPolygon().xpoints[p];
//										int y = roiRFP.getPolygon().ypoints[p];
//										if(y >= roi.getBounds().y - 1 && y <= (roi.getBounds().y + roi.getBounds().height + 1) && x >= roi.getBounds().x-3 && x <= (roi.getBounds().x + roi.getBounds().width+3))										
//										{
//											Foci foci = new Foci();																
//											foci.center[0] = x;
//											foci.center[1] = y;
//											foci.reletiveCenterToCell = new double[2];
//											foci.reletiveCenterToCell[0] = (foci.center[0]-statsCell.xCentroid)/statsCell.roiWidth;
//											foci.reletiveCenterToCell[1] = (foci.center[1]-statsCell.yCentroid)/statsCell.roiHeight;										
//											foci.meanIntensity = (double)(ip.getPixel(x-1, y-1) + ip.getPixel(x, y-1) + ip.getPixel(x+1, y-1) + 
//													ip.getPixel(x-1, y) + ip.getPixel(x, y) + ip.getPixel(x+1, y) +
//													ip.getPixel(x-1, y+1) + ip.getPixel(x, y+1) + ip.getPixel(x+1, y+1))/9.0;										
//											foci.label = labelRFP;
//											focies[numFoci] = foci;
//											numFoci++;
//											//IJ.showMessage("roi label: "+label + " point label: " + labelRFP);
//										}
//									}
//								}
//							}else {
//								if(roiRFP.getPosition() == slice)
//								{
//									PointRoi roiPoint = (PointRoi) rois.get(labelRFP);
//									int numPoints = roiPoint.getPolygon().npoints;
//									for(int p=0;p<numPoints;p++)
//									{
//										int x = roiRFP.getPolygon().xpoints[p];
//										int y = roiRFP.getPolygon().ypoints[p];
//										if(y >= roi.getBounds().y - 1 && y <= (roi.getBounds().y + roi.getBounds().height + 1) && x >= roi.getBounds().x-3 && x <= (roi.getBounds().x + roi.getBounds().width+3))										
//										{
//											Foci foci = new Foci();																
//											foci.center[0] = x;
//											foci.center[1] = y;
//											foci.reletiveCenterToCell = new double[2];
//											foci.reletiveCenterToCell[0] = (foci.center[0]-statsCell.xCentroid)/statsCell.roiWidth;
//											foci.reletiveCenterToCell[1] = (foci.center[1]-statsCell.yCentroid)/statsCell.roiHeight;										
//											foci.meanIntensity = (double)(ip.getPixel(x-1, y-1) + ip.getPixel(x, y-1) + ip.getPixel(x+1, y-1) + 
//													ip.getPixel(x-1, y) + ip.getPixel(x, y) + ip.getPixel(x+1, y) +
//													ip.getPixel(x-1, y+1) + ip.getPixel(x, y+1) + ip.getPixel(x+1, y+1))/9.0;										
//											foci.label = labelRFP;
//											focies[numFoci] = foci;
//											numFoci++;
//											//IJ.showMessage("roi label: "+label + " point label: " + labelRFP);
//										}
//									}
//								}else if (roiRFP.getPosition() == slice) {
//									PointRoi roiPoint = (PointRoi) rois.get(labelRFP);
//									if(roi.getBounds().contains(roiPoint.getPolygon().xpoints[0],roiPoint.getPolygon().ypoints[0] ))
//									{
//										manulTrackingCell = true;
//									}									
//								}
//							}
//						}
//						
//						cells[j]= new Ins_cell(roi);
//						cells[j].manulTrackingCell = manulTrackingCell;
//						
//						
//						if(skeletonize)
//						{
//							roi.setImage(null);
//							ByteProcessor bIp = (ByteProcessor)roi.getMask();
//							bIp.invertLut();
//							ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
//							bIp.skeletonize();
//							cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
//						}
//						j = j+1;
//					}
//
//				}
////				else {	//only trans binary image is used for lineage 
////					if(!(roi instanceof Arrow))
////					{			
////						cells[j]= new Ins_cell(label, 1,roi.getBounds().height, roi, null, null,new int[]{roi.getBounds().x,roi.getBounds().y},0,0,0,0,false);				
////						j = j+1;
////					}
////				}
//			}
//			if(skeletonize)
//				new ImagePlus("Skeleton imp", ipSkeleton).show();
//		}else {
//			ImageProcessor ip = WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
//			for(int i=0 ; i< count; i++)
//			{
//				String label = list.getItem(i);
//				Roi roi = (Roi)rois.get(label);
//				
//				if(roi.getPosition() != slice)
//					continue;
//				
//				if(openChannel)
//				{
//					if(!(roi instanceof Arrow))
//					{	
//						boolean manulTrackingCell = false;
//						for(int id = list.getItemCount()-1;id>=0 ; id--)
//						{
//							String labelRFP = list.getItem(id);
//							Roi roiRFP = (Roi) rois.get(labelRFP);
//							if(!(roiRFP instanceof PointRoi))
//								continue;
//							if (roiRFP.getPosition() == slice) {
//								PointRoi roiPoint = (PointRoi) rois.get(labelRFP);								
//								if(roi.getBounds().contains(roiPoint.getPolygon().xpoints[0],roiPoint.getPolygon().ypoints[0] ))
//								{
//									manulTrackingCell = true;
//								}
//							}
//						}
//						cells[j].manulTrackingCell = manulTrackingCell;
//						
//						ip.resetRoi();
//						ip.setRoi(roi);							
//						ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);						
//						cells[j]= new Ins_cell(roi);				
//						if(skeletonize)
//						{
//							roi.setImage(null);
//							ByteProcessor bIp = (ByteProcessor)roi.getMask();
//							bIp.invertLut();
//							ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
//							bIp.skeletonize();
//							cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
//						}
//						j = j+1;
//					}
//				}else {
//					if(!(roi instanceof Arrow))
//					{	
//						ip.resetRoi();
//						ip.setRoi(roi);							
//						ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);						
//						cells[j]= new Ins_cell(roi);				
//						if(skeletonize)
//						{
//							roi.setImage(null);
//							ByteProcessor bIp = (ByteProcessor)roi.getMask();
//							bIp.invertLut();
//							ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
//							bIp.skeletonize();
//							cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
//						}
//						j = j+1;
//					}
//				}
//				
//			}
//			if(skeletonize)
//				new ImagePlus("Skeleton imp", ipSkeleton).show();
//		}
//		Ins_cell[] cells2 = new Ins_cell[j];
//		for(int k=0;k<j;k++)
//		{
//			cells2[k] = cells[k];
//			//System.out.println(cells2[k]);
//		}		
//		return cells2;
//	}
	
	
	
	/**
	 * 
	 * @param list
	 * @param rois
	 * @return special one for AS
	 */
	public Ins_cell[] constructCellsListFromStackYifan(List list, Hashtable<String, Roi> rois,int slice, boolean fluorence, boolean openChannel, int volumeSize)
	{
		boolean skeletonize = mModeSilence.isSelected();
		Ins_cell[] cells= new Ins_cell[list.getItemCount()];
		
		int count = list.getItemCount();
		int j = 0;				

		ImageProcessor ipSkeleton = null;
		if(skeletonize)
		{
			ipSkeleton =WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
			ipSkeleton = ipSkeleton.convertToRGB();
		}
		if(fluorence)
		{
			ImageProcessor ip = WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
			ImageProcessor ip2 = WindowManager.getCurrentImage().getImageStack().getProcessor(slice+1);
			ImageProcessor ip3 = WindowManager.getCurrentImage().getImageStack().getProcessor(slice+2);
			for(int i=list.getItemCount()-1; i>=0; i--)
			{
				String label = list.getItem(i);
				Roi roi = (Roi)rois.get(label);

				if(roi.getPosition() != slice)
					continue;

				if(mChRFP.isSelected()){
					if(!(roi instanceof Arrow || roi instanceof PointRoi))
					{
						ip.resetRoi();
						ip.setRoi(roi);
						ip2.resetRoi();
						ip2.setRoi(roi);
						ip3.resetRoi();
						ip3.setRoi(roi);
						
						
						//cells[j]= new Ins_cell(label, slice, roi.getBounds().height, roi, new int[]{roi.getBounds().x,roi.getBounds().y}, statsCell.mean, statsCell.area, statsCell2.mean, statsCell2.area, statsCell3.mean, statsCell3.area);

						if(skeletonize)
						{
							roi.setImage(null);
							ByteProcessor bIp = (ByteProcessor)roi.getMask();
							bIp.invertLut();
							ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
							bIp.skeletonize();
							cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
						}

						j = j+1;
					}
				}
			}
			if(skeletonize)
				new ImagePlus("Skeleton imp", ipSkeleton).show();
		}else {
			ImageProcessor ip = WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
			for(int i=0 ; i< count; i++)
			{
				String label = list.getItem(i);
				Roi roi = (Roi)rois.get(label);

				if(roi.getPosition() != slice )
					continue;

				if(!(roi instanceof Arrow))
				{	
					ip.resetRoi();
					ip.setRoi(roi);							
					cells[j]= new Ins_cell(roi);				
					if(skeletonize)
					{
						roi.setImage(null);
						ByteProcessor bIp = (ByteProcessor)roi.getMask();
						bIp.invertLut();
						ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
						bIp.skeletonize();
						cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
					}
					j = j+1;
				}
			}
			if(skeletonize)
				new ImagePlus("Skeleton imp", ipSkeleton).show();
		}
		
		
		//return Arrays.copyOfRange(cells, 0, j-1);
		Ins_cell[] cells2 = new Ins_cell[j];
		for(int k=0;k<j;k++)
		{
			cells2[k] = cells[k];
		}		
		return cells2;
	}
	
	
	/**
	 * 
	 * @param list
	 * @param rois
	 * @return construct the cells from the list and rois precomputed
	 */
	public Object[] constructCellsListFromStackCombine(Roi[] rois,int slice, boolean fluorence)
	{
		boolean skeletonize = mModeSilence.isSelected();
		int count = rois.length;
		Ins_cell[] cells_slice1= new Ins_cell[count];	
		int j = 0;
		int mOption = 0;
		mOption = mOption|Measurements.CENTROID|Measurements.MEAN|Measurements.AREA;

		ImageProcessor ipSkeleton = null;
		if(skeletonize)
		{
			ipSkeleton =WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
			ipSkeleton = ipSkeleton.convertToRGB();
		}
		ImageStack ims = WindowManager.getCurrentImage().getImageStack();

		ImageProcessor ip = ims.getProcessor(slice);
		//ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
		for(int i=0 ; i< count; i++)
		{					
			Roi roi = rois[i];
			if(roi instanceof Arrow)
				continue;				
			int slice1 = roi.getPosition();
			if(slice1 != slice)
				continue;
			cells_slice1[j]= new Ins_cell(roi);
			if(fluorence)
			{
				ip.resetRoi();
				ip.setRoi(roi);							
				ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
				int index = cells_slice1[j].setAreaToAreaArray(statsCell.area);
				cells_slice1[j].setIntensityToMeanIntensityArray(statsCell.mean, index);
				cells_slice1[j].setCentroid(statsCell.xCentroid, statsCell.yCentroid);;
			}
			j = j+1;
		}

		for(int i=0 ; i< count; i++)
		{					
			Roi roi = rois[i];
			if(roi instanceof Arrow)
				continue;				
			int slice1 = roi.getPosition();
			if(slice1 != slice + 1)
				continue;

			int x2 = roi.getBounds().x;
			int y2 = roi.getBounds().y;
			int width2 = roi.getBounds().width;
			int height2 = roi.getBounds().height;

			boolean superpose = false;
			int areaSuperpose = 0;
			for(int ii=0; ii<count; ii++)
			{
				Roi roi2 = rois[ii];
				int slice2 = roi2.getPosition();
				if(slice2!= slice)
					continue;										
				int x1 = roi2.getBounds().x;
				int y1 = roi2.getBounds().y;
				int width1 = roi2.getBounds().width;
				int height1 = roi2.getBounds().height;
				for(int x=x2; x<x2+width2;x++)
				{
					for(int y=y2; y<y2+height2; y++)
					{
						if(x>=x1&&y>=y1&&x<(x1+width1)&&y<(y1+height1))
						{
							areaSuperpose++;
						}
					}
				}					
				if(areaSuperpose >= width1*height1*0.5)
				{
					superpose = true;
					break;
				}
			}
			if(!superpose)
			{
				Roi roiInSlice1 = (Roi)roi.clone();
				roiInSlice1.setPosition(slice);
				roiInSlice1.setName(null);
				addRoiToManager(roiInSlice1);
				//System.out.println("no superposed roi 2 : " + roi.getName());
				cells_slice1[j]= new Ins_cell(roiInSlice1);
				
				if(fluorence)
				{					
					ip.resetRoi();
					ip.setRoi(roi);							
					ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
					int index = cells_slice1[j].setAreaToAreaArray(statsCell.area);
					cells_slice1[j].setIntensityToMeanIntensityArray(statsCell.mean, index);
					cells_slice1[j].setCentroid(statsCell.xCentroid, statsCell.yCentroid);;
				}
				j = j+1;
			}
		}
		if(skeletonize)
			new ImagePlus("Skeleton imp", ipSkeleton).show();

		Ins_cell[] cells2 = new Ins_cell[j];
		for(int k=0;k<j;k++)
		{
			cells2[k] = cells_slice1[k];
			//System.out.println("			cell name : " + cells[k].getRoi().getName());
		}

//--------------------------------------------------------------------------------------------


		Ins_cell[] cells_slice2= new Ins_cell[count];	
		j = 0;
		if(skeletonize)
		{
			ipSkeleton =WindowManager.getCurrentImage().getImageStack().getProcessor(slice);
			ipSkeleton = ipSkeleton.convertToRGB();
		}

		ip = ims.getProcessor(slice+1);
		//ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
		for(int i=0 ; i< count; i++)
		{					
			Roi roi = rois[i];
			if(roi instanceof Arrow)
				continue;				
			int slice1 = roi.getPosition();
			if(slice1 != slice+1)
				continue;
			cells_slice2[j]= new Ins_cell(roi);
			if(fluorence)
			{
				ip.resetRoi();
				ip.setRoi(roi);							
				ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
				int index = cells_slice2[j].setAreaToAreaArray(statsCell.area);
				cells_slice2[j].setIntensityToMeanIntensityArray(statsCell.mean, index);
				cells_slice2[j].setCentroid(statsCell.xCentroid, statsCell.yCentroid);;
			}
			j = j+1;
		}

		for(int i=0 ; i< count; i++)
		{
			Roi roi = rois[i];
			if(roi instanceof Arrow)
				continue;				
			int slice1 = roi.getPosition();
			if(slice1 != slice)
				continue;

			int x2 = roi.getBounds().x;
			int y2 = roi.getBounds().y;
			int width2 = roi.getBounds().width;
			int height2 = roi.getBounds().height;

			boolean superpose = false;
			int areaSuperpose = 0;
			for(int ii=0; ii<count; ii++)
			{
				Roi roi2 = rois[ii];
				int slice2 = roi2.getPosition();
				if(slice2!= slice + 1)
					continue;										
				int x1 = roi2.getBounds().x;
				int y1 = roi2.getBounds().y;
				int width1 = roi2.getBounds().width;
				int height1 = roi2.getBounds().height;
				for(int x=x2; x<x2+width2;x++)
				{
					for(int y=y2; y<y2+height2; y++)
					{
						if(x>=x1&&y>=y1&&x<(x1+width1)&&y<(y1+height1))
						{
							areaSuperpose++;
						}
					}
				}					
				if(areaSuperpose >= width1*height1*0.5)
				{
					superpose = true;
					break;
				}
			}
			if(!superpose)
			{
				Roi roiInSlice1 = (Roi)roi.clone();
				roiInSlice1.setPosition(slice+1);
				roiInSlice1.setName(null);
				addRoiToManager(roiInSlice1);
				//System.out.println("no superposed roi 2 : " + roi.getName());
				cells_slice2[j]= new Ins_cell(roiInSlice1);	
				if(fluorence)
				{
					ip.resetRoi();
					ip.setRoi(roi);							
					ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);		
					int index = cells_slice2[j].setAreaToAreaArray(statsCell.area);
					cells_slice2[j].setIntensityToMeanIntensityArray(statsCell.mean, index);
					cells_slice2[j].setCentroid(statsCell.xCentroid, statsCell.yCentroid);;
				}
				j = j+1;
			}
		}
		if(skeletonize)
			new ImagePlus("Skeleton imp", ipSkeleton).show();
		
		Ins_cell[] cells2_s2 = new Ins_cell[j];
		for(int k=0;k<j;k++)
		{
			cells2_s2[k] = cells_slice2[k];
		}
		return new Object[]{cells2, cells2_s2};
	}
	
		
	/**
	 * 
	 * @param list
	 * @param rois
	 * @return construct the cells from the list and rois precomputed
	 *//*
	private Ins_cell[] constructCellsfromList(List list, Hashtable<String, Roi> rois, boolean fluorence)
	{
		boolean skeletonize = mSkeletonize.isSelected();
		double thresCircular=Double.valueOf(oThresCircularity.getText());
		double areaFraction=Double.valueOf(oIntervalSlice.getText());
		//RoiPropeties rPFC = new RoiPropeties();
		System.out.println("slice selector dialog");
		ImagePlus imp= WindowManager.getCurrentImage();
		GenericDialog genericDialog= new GenericDialog("Slice selector");
		genericDialog.addNumericField("Slice : ", slice_cell, 0);
		genericDialog.showDialog();
        if (genericDialog.wasCanceled())
            slice_cell = imp.getStackSize() - 1;
        slice_cell = (int)genericDialog.getNextNumber();
        if(slice_cell > imp.getStackSize() - 1)
        	slice_cell = imp.getStackSize() - 1;
        if(slice_cell < 0)
        	slice_cell = 1;
		
		Ins_cell[] cells= new Ins_cell[list.getItemCount()];	
		int count = list.getItemCount();
		int j = 0;				

		//slice_cell = imp.getStackSize() -1;
		int mOption = 0;
		mOption = mOption|Measurements.CENTROID|Measurements.MEAN|Measurements.AREA;
		ImageProcessor ipSkeleton = imp.getImageStack().getProcessor(slice_cell);
		ipSkeleton = ipSkeleton.convertToRGB();
		if(fluorence)
		{
			ImageProcessor ip = imp.getImageStack().getProcessor(imp.getStackSize());			
			for(int i=list.getItemCount()-1; i>=0; i--)
			{
				String label = list.getItem(i);
				Roi roi = (Roi)rois.get(label);				
				if(mChRFP.isSelected()){
					if(roi.getPosition() == slice_cell)//if(roi.getPosition() == imp.getStackSize()-1)
					{
						if(!(roi instanceof Arrow))
						{
							Foci[] focies = new Foci[10];
							boolean manulTrackingCell = false;
							int numFoci = 0;
							Comet[] comets = new Comet[10];
							//int numComet = 0;
							
							ip.resetRoi();
							ip.setRoi(roi);							
							ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);
							
							for(int id = list.getItemCount()-1;id>=0 ; id--)
							{
								String labelRFP = list.getItem(id);
								Roi roiRFP = (Roi) rois.get(labelRFP);
								if(!(roiRFP instanceof PointRoi))
									continue;

								if(roiRFP.getPosition() == imp.getStackSize())
								{
									PointRoi roiPoint = (PointRoi) rois.get(labelRFP);
									if(roiPoint.getPolygon().ypoints[0] >= roi.getBounds().y - 1 && roiPoint.getPolygon().ypoints[0] <= (roi.getBounds().y + roi.getBounds().height + 1) && roiPoint.getPolygon().xpoints[0] >= roi.getBounds().x-2 && roiPoint.getPolygon().xpoints[0] <= (roi.getBounds().x + roi.getBounds().width+2) )										
									//if(roi.getBounds().contains(roiPoint.getPolygon().xpoints[0],roiPoint.getPolygon().ypoints[0] ))
									{										
										Foci foci = new Foci();						
										int x = roiPoint.getPolygon().xpoints[0];
										foci.center[0] = x;
										int y = roiPoint.getPolygon().ypoints[0];
										foci.center[1] = y;										
										foci.reletiveCenterToCell = new double[2];
										foci.reletiveCenterToCell[0] = (foci.center[0]-statsCell.xCentroid)/statsCell.roiWidth;
										foci.reletiveCenterToCell[1] = (foci.center[1]-statsCell.yCentroid)/statsCell.roiHeight;										
										foci.meanIntensity = (double)(ip.getPixel(x-1, y-1) + ip.getPixel(x, y-1) + ip.getPixel(x+1, y-1) + 
												ip.getPixel(x-1, y) + ip.getPixel(x, y) + ip.getPixel(x+1, y) +
												ip.getPixel(x-1, y+1) + ip.getPixel(x, y+1) + ip.getPixel(x+1, y+1))/9.0;										
										foci.label = labelRFP;
										focies[numFoci] = foci;
										numFoci++;
										//IJ.showMessage("roi label: "+label + " point label: " + labelRFP);
									}									
								}else if (roiRFP.getPosition() == roi.getPosition()-1) {
									PointRoi roiPoint = (PointRoi) rois.get(labelRFP);
									if(roi.getBounds().contains(roiPoint.getPolygon().xpoints[0],roiPoint.getPolygon().ypoints[0] ))
									{
										manulTrackingCell = true;
									}									
								}
							}						
							cells[j]= new Ins_cell(1, (int)statsCell.roiHeight, roi ,focies, comets,statsCell.mean*statsCell.area, statsCell.area, statsCell.xCentroid, statsCell.yCentroid, true);
							cells[j].manulTrackingCell = manulTrackingCell;
							if(skeletonize)
							{
								roi.setImage(null);
								ByteProcessor bIp = (ByteProcessor)roi.getMask();
								bIp.invertLut();
								ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
								bIp.skeletonize();
								cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
							}
							j = j+1;
						}
					}
				}
//				else {	//only trans binary image is used for lineage 
//					if(!(roi instanceof Arrow))
//					{			
//						cells[j]= new Ins_cell(label, 1,roi.getBounds().height, roi, null, null,new int[]{roi.getBounds().x,roi.getBounds().y},0,0,0,0,false);				
//						j = j+1;
//					}
//				}
			}
			if(skeletonize)
				new ImagePlus("Skeleton imp", ipSkeleton).show();
		}else {
			ImageProcessor ip = imp.getImageStack().getProcessor(imp.getStackSize());
			for(int i=0 ; i< count; i++)
			{
				String label = list.getItem(i);
				Roi roi = (Roi)rois.get(label);	
				if(roi.getPosition() == slice_cell)
				{
					if(!(roi instanceof Arrow))
					{	
//						boolean manulTrackingCell = false;
//						for(int id = list.getItemCount()-1;id>=0 ; id--)
//						{
//							String labelRFP = list.getItem(id);
//							Roi roiRFP = (Roi) rois.get(labelRFP);
//							if(!(roiRFP instanceof PointRoi))
//								continue;
//							if (roiRFP.getPosition() <= imp.getStackSize()-2) {
//								PointRoi roiPoint = (PointRoi) rois.get(labelRFP);								
//								if(roi.getBounds().contains(roiPoint.getPolygon().xpoints[0],roiPoint.getPolygon().ypoints[0] ))
//								{
//									manulTrackingCell = true;
//								}								
//							}
//						}
//						cells[j].manulTrackingCell = manulTrackingCell;
						
						ip.resetRoi();
						ip.setRoi(roi);							
						ImageStatistics statsCell = ImageStatistics.getStatistics(ip, mOption, null);
						
						cells[j]= new Ins_cell(1,roi.getBounds().height, roi, null, null,0,statsCell.area,statsCell.xCentroid,statsCell.yCentroid,false);				
						if(skeletonize)
						{
							roi.setImage(null);
							ByteProcessor bIp = (ByteProcessor)roi.getMask();
							bIp.invertLut();
							ImagePlus binaryImp =new ImagePlus(list.getItem(i), bIp.duplicate()); 
							bIp.skeletonize();
							cells[j].setSkeletonLength(getSkeletonLength(new ImagePlus(list.getItem(i)+"skeleton", bIp), binaryImp,true,ipSkeleton,roi.getBounds().x,roi.getBounds().y));
						}
						j = j+1;
					}
				}
			}
			if(skeletonize)
				new ImagePlus("Skeleton imp", ipSkeleton).show();
		}
				
		Ins_cell[] cells2 = new Ins_cell[j];
		for(int k=0;k<j;k++)
		{
			cells2[k] = cells[k];
			//System.out.println(cells2[k]);
		}		
		return cells2;
	}*/
	

	/**
	 * 
	 * @param skeletonImp skeletonized image
	 * @param binaryImp binary image
	 * @param showSkeleton
	 * @param ip image to draw the skeleton
	 * @param x
	 * @param y
	 * @return
	 */
	public double getSkeletonLength(ImagePlus skeletonImp,ImagePlus binaryImp, boolean showSkeleton, ImageProcessor ip, int x, int y)
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
		double maxDistance = -1.0;
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
		if(showSkeleton && ip!=null)
		{
			//cpBranchesOnBinary.invertLut();		
			if(!(ip instanceof ColorProcessor))
				ip = ip.convertToRGB();			
			ip.insert(cpBranchesOnBinary, x, y);
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

	
	
	
	
	void selectProcessingParams()
	{
		GenericDialog dialog = new GenericDialog("Process parameters");
		
		dialog.addCheckbox("Combine RFP", doCombineCfp);
		dialog.addCheckbox("Normalization", doNormalizeSlice );
		dialog.addCheckbox("Normalization Stack", doNormalizeStack);
		dialog.addCheckbox("Denoise ROF", doDenoise);
		dialog.addCheckbox("Segmentation", doSegmentationAfterCutting);
	
		dialog.showDialog();
	    if (dialog.wasCanceled()) return;
	    doCombineCfp = dialog.getNextBoolean();
	    doNormalizeSlice = dialog.getNextBoolean();
	    doNormalizeStack = dialog.getNextBoolean();
	    doDenoise = dialog.getNextBoolean();
	    doSegmentationAfterCutting = dialog.getNextBoolean();  
	}
	
	
	boolean selectBatchParams()
	{
		GenericDialog dialog = new GenericDialog("Batch parameters");		
		dialog.addCheckbox("Run all", doRunAll);
		//dialog.addCheckbox("Combine RFP", doCombineCfp);
		dialog.addCheckbox("Normalization Slice", doNormalizeSlice );
		dialog.addCheckbox("Normalization Stack", doNormalizeStack);
		dialog.addCheckbox("Denoise ROF", doDenoiseBeforeCutting);
		dialog.addCheckbox("Segmentation", doSegmentationAfterCutting);			
		dialog.showDialog();
	    if (dialog.wasCanceled()) return false;
	    doRunAll = dialog.getNextBoolean();
	    //doCombineCfp = dialog.getNextBoolean();
	    doNormalizeSlice = dialog.getNextBoolean();
	    doNormalizeStack = dialog.getNextBoolean();
	    doDenoiseBeforeCutting = dialog.getNextBoolean();
	    doSegmentationAfterCutting = dialog.getNextBoolean();
	    return true;
	}
	
	public static void rotateImage(ImagePlus imp, double angle) {
		IJ.showStatus("Rotating image angle : " + angle);
		ImageStack stack = imp.getImageStack();
		int ssize = imp.getStackSize();
		if(ssize > 1)			
			for(int j=1;j<=ssize;j++)
			{			
				IJ.showStatus("Rotating image: "+j+"/"+ssize);
				System.out.println("Rotating image: "+j+"/"+ssize);
				ImageProcessor ip = stack.getProcessor(j);
				ip.setInterpolationMethod(ImageProcessor.BILINEAR);
				ip.setBackgroundValue(0);
				ip.rotate(angle);
			}
		else {
				ImageProcessor ip = imp.getProcessor();
				ip.setInterpolationMethod(ImageProcessor.BILINEAR);
				ip.setBackgroundValue(0);
				ip.rotate(angle);			
			}
		imp.updateAndDraw();		
	}
	
	
	public static void flipVertical(ImagePlus imp) {		
		if(imp.getStackSize() > 1)
			for(int j=1;j<=imp.getStackSize();j++)
			{			
				ImageProcessor ip = imp.getImageStack().getProcessor(j);
				ip.flipVertical();			
			}else {
				imp.getProcessor().flipVertical();
			}
		imp.updateAndDraw();		
	}
	
//	/**
//	 * classic result table
//	 */
//	public void writeCellstoRT(String rootName)
//	{
//		if(WindowManager.getCurrentImage() == null)
//			return;
//		
//		WindowManager.getCurrentImage().getCanvas().addKeyListener(this);						
//		//boolean orderByChannel = mEnableChannel.isSelected();
//		boolean roiManagerClosed = false;
//		Frame frame = WindowManager.getFrame("ROI Manager");
//		if (frame==null)				
//		{
//			roiManagerClosed = true;
//			IJ.run("ROI Manager...");
//		}				
//		frame = WindowManager.getFrame("ROI Manager");										
//		if (frame==null || !(frame instanceof RoiManager))
//		{return;}
//		RoiManager roiManager = (RoiManager)frame;					
//		roiManager.addKeyListener(this);
//		
//		if(!roiManagerClosed)
//		{
//			list = roiManager.getList();
//			rois = roiManager.getROIs();
//		}
//		if(roiManagerClosed && rois != null && list != null)
//		{					
//			for(int i=0;i<rois.size();i++)
//			{
//				roiManager.add(new ImagePlus(), (Roi)rois.get(list.getItem(i)), i);						
//			}
//		}
//		int count = roiManager.getList().getItemCount();
//		if(count == 0)
//			return;				
//		roiManager.setVisible(true);											
//		list.setMultipleMode(true);	
//		
//		Ins_editor logEditor = new Ins_editor();
//		logEditor.setVisible(true);
//		lineageAnalyze(logEditor, rootName);
////		for(int j=0 ; j< cells.length; j++)
////		{
////			logEditor.append(cells[j].toString());
////		}
////		list.addKeyListener(this);
//	}
	
	
//	/**
//	 * for Anne Sophie's RCP image
//	 * @param idCell
//	 */
//	public Roi[][] writeCellstoRT(String path)
//	{
//		if(WindowManager.getCurrentImage() == null)
//		{
//			ImagePlus impCurrent = IJ.openImage();
//			if(impCurrent == null)
//				return null;
//			else {
//				impCurrent.show();
//			}
//		}
//		Frame frame = WindowManager.getFrame("ROI Manager");
//		if (frame==null)
//		{
//			IJ.run("ROI Manager...");
//		}
//		
//		frame = WindowManager.getFrame("ROI Manager");										
//		if (frame==null || !(frame instanceof RoiManager))
//		{return null;}
//		RoiManager roiManager = (RoiManager)frame;					
//		roiManager.addKeyListener(this);
//		
//		Roi[] roisArrayRois = roiManager.getRoisAsArray();
//		int count = roisArrayRois.length;
//		if(count == 0)
//		{
//			return null;
//		}
//		roiManager.setVisible(true);	
//		GenericDialog genericDialog = new GenericDialog("Information");
//		if(stackSize<=0)
//			genericDialog.addNumericField("Stack size : ", WindowManager.getCurrentImage().getWidth()/(roi_width+6), 0);
//		else {
//			genericDialog.addNumericField("Stack size : ", stackSize, 0);
//		}
//		
//		
//		ImagePlus impToLineage = WindowManager.getCurrentImage();
//		int interval = 1;
//		String title = impToLineage.getTitle().toLowerCase();
//		if(title.indexOf("triple")!=-1)
//			interval = 3;
//		else if (title.indexOf("double")!=-1) {
//			interval = 2;
//		}else {
//			interval = 1;
//		}
//		
//		genericDialog.addNumericField("Roi width : ", roi_width, 0);
//		genericDialog.addNumericField("Image width",impToLineage.getWidth(), 0);
//		genericDialog.addNumericField("Channel number", impToLineage.getImageStackSize()/interval, 0);
//		genericDialog.addCheckbox("Competition merge first", mergeFirst);
//		genericDialog.addNumericField("Iteration (correct error cell)",iterationTotal,0);
//		genericDialog.addCheckbox("Add virtual cell(if no ROI)", addVirtualCell);
//
//		//genericDialog.addCheckbox("Refinement of top cell size", refinement);
//		genericDialog.addNumericField("Filament ratio to normal", filamentRatio,2);
//		//genericDialog.addCheckbox("Output new stack binary", outputNewBinary);
//		genericDialog.addCheckbox("Save Result Table", writeResultTable);
//		
//		if(mSkeletonize.isSelected())
//		{
//			genericDialog.showDialog();
//			if (genericDialog.wasCanceled()) return null;
//		}
//		stackSize = (int)genericDialog.getNextNumber();
//		roi_width = (int)genericDialog.getNextNumber();
//		widthImp = (int)genericDialog.getNextNumber();
//		channelNumber = (int)genericDialog.getNextNumber();
//		mergeFirst = genericDialog.getNextBoolean();
//		iterationTotal = (int)genericDialog.getNextNumber();
//		addVirtualCell = genericDialog.getNextBoolean();
//		filamentRatio = genericDialog.getNextNumber();
//		
//		//refinement = genericDialog.getNextBoolean();		
//		//outputNewBinary = genericDialog.getNextBoolean();
//		outputNewBinary = false;
//		writeResultTable = genericDialog.getNextBoolean();
//		
//		if(impToLineage.getImageStackSize() % channelNumber !=0)
//		{
//			IJ.showMessage("Wrong channel number!");
//			return null;
//		}
//		return lineage(writeResultTable,path,false, roisArrayRois,iterationTotal, filamentRatio,impToLineage);
//		
//	}
	
	/**
	 * for Anne Sophie's RCP image
	 * @param idCell
	 */
	public void lineageProcess(String path, boolean modeSilence)
	{
		if(WindowManager.getCurrentImage() == null)
		{
			ImagePlus impCurrent = IJ.openImage();
			if(impCurrent == null)
				return;
			else {
				impCurrent.show();
			}
		}
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
		{
			IJ.run("ROI Manager...");
		}
		frame = WindowManager.getFrame("ROI Manager");										
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		RoiManager roiManager = (RoiManager)frame;
		Roi[] roisArrayRois = roiManager.getRoisAsArray();
		int count = roisArrayRois.length;
		if(count == 0)
		{
			return;
		}
		roiManager.setVisible(true);
		
		ImagePlus impToLineage = WindowManager.getCurrentImage();
		//int stackSize = 0;
		//int roi_width = Lineage.roi_width;
		double filamentRatio = 2.4;		
		boolean allCellsInFirstChannel = false;

		if(mConsiderAllFirstChannelCells.isSelected())
			allCellsInFirstChannel = true;		
		boolean writeResultTable = false;
		parseImageTitle(impToLineage);
		/*try {			
			String impName = impToLineage.getTitle();
			String[] name = impName.split("-ss-");				
			name = name[1].split("-roi-");
			stackSize = Integer.valueOf(name[0]);		
			if(impName.contains("-sx-"))
				name = name[0].split("-sx-");
			name = name[1].split(".tif");
			roi_width = Integer.valueOf(name[0]);
		} catch (ArrayIndexOutOfBoundsException e2) {
			IJ.showMessage("Image name doesn't include necessary information! (ss-?-roi-?)");					
		}*/
		//correctSegBySeedsOnly=correctSegmentationBySeeds;
		
		GenericDialog genericDialog = new GenericDialog("Information");
		if(stackSize<=0)
			genericDialog.addNumericField("Stack size : ", WindowManager.getCurrentImage().getWidth()/(roi_width+6), 0);
		else {
			genericDialog.addNumericField("Stack size : ", stackSize, 0);
		}
		
		int interval = 1;
		String title = impToLineage.getTitle().toLowerCase();
		if(title.indexOf("triple")!=-1)
			interval = 3;
		else if (title.indexOf("double")!=-1) {
			interval = 2;
		}else {
			interval = 1;
		}
		boolean detectFoci = false;
		boolean addVitrualCell = false;
		boolean mergeFirst = false;
		boolean evaluation = false;
		boolean mergeCells = false;
		int iterTotal = 0;
		double sigma = 2.8;
		if(!runOnce)
			channelNumber = impToLineage.getImageStackSize()/interval;		
		genericDialog.addNumericField("Roi width : ", roi_width, 0);
		genericDialog.addNumericField("Number of slices for lineage", channelNumber, 0);
		genericDialog.addCheckbox("Detect aggregation", detectFoci);
		genericDialog.addNumericField("Filament ratio to normal", filamentRatio,2);
		genericDialog.addCheckbox("Consider all cells in the first channel", allCellsInFirstChannel);
		genericDialog.addCheckbox("Save Result Table", writeResultTable);
		genericDialog.addCheckbox("Add virtual cell", addVitrualCell);
		genericDialog.addCheckbox("Merge first", mergeFirst);
		genericDialog.addNumericField("Iteration steps", iterTotal, 0);		
		genericDialog.addCheckbox("Evaluation", evaluation);		
		genericDialog.addCheckbox("Correct segmentation from SEEDS-MASK", correctSegmentationBySeeds);
		genericDialog.addCheckbox("Correct segmentation from SEEDS-MASK (PSF)", correctSegBySPF);
		genericDialog.addNumericField("sigma", sigma, 2);
		genericDialog.addCheckbox("No lineage after correction", noLineageAfterCorrection);
		genericDialog.addCheckbox("Merge cells slice cfp-rfp (NO LINEAGE)", mergeCells);
		
		if(!modeSilence || correctSegmentationBySeeds)
		{
			genericDialog.showDialog();		
			if (genericDialog.wasCanceled()) return;
			runOnce = true;
		}
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
		evaluation = genericDialog.getNextBoolean();
		correctSegmentationBySeeds = genericDialog.getNextBoolean();
		correctSegBySPF = genericDialog.getNextBoolean();
		sigma = genericDialog.getNextNumber();
		noLineageAfterCorrection = genericDialog.getNextBoolean();		
		mergeCells = genericDialog.getNextBoolean();		
		if(correctSegBySPF) // if the two checkbox are selected, only the second one is considered
			correctSegmentationBySeeds = false;
		Lineage lineage = new Lineage(impToLineage, stackSize, channelNumber,roi_width, sigma, filamentRatio, allCellsInFirstChannel, writeResultTable, addVitrualCell, mergeFirst, iterTotal, path, detectFoci,evaluation,false, correctSegmentationBySeeds,correctSegBySPF,noLineageAfterCorrection, mergeCells);				
		lineage.run("lineage");
	}
	


	
/*	private void doCombineThenLineage(int slice1, int slice2, int slice3, Roi[] roiArray,Roi[][] roisArrayToManager, int iterTotal, ImagePlus redirectImp,boolean writeToResult, String outputDirectoryPath)
	{
		
		//boolean allNull = false;
		//ImageProcessor ip = cImp.getImageStack().getProcessor(slice2);				
		Roi[] roisSlice1 = constructRoisSlice(roiArray, slice1);
		Ins_cellsVector cellsTimeIndex1[] = null;
		if(roisSlice1!=null)
		{
			Ins_cell[] cells1 = constructCellsListFromStack(roisSlice1);
			if(cells1 != null)
			{
				Arrays.sort(cells1);
				setCellsNumber(cells1);
				cellsTimeIndex1 = constructCellVectors(stackSize,cells1);	
			}
		}				
		Ins_cellsVector[] cellsTimeIndex = null;
		int increment = Math.abs(slice3 - slice2);
		if(increment == 1)
		{
			Roi[] roisSlice2 = constructRoisSlice(roiArray, slice2);
			Ins_cellsVector cellsTimeIndex2[] = null;
			if(roisSlice2!=null)
			{
				Ins_cell[] cells2 = constructCellsListFromStack(roisSlice2);
				if(cells2 != null)
				{
					Arrays.sort(cells2);
					setCellsNumber(cells2);
					cellsTimeIndex2 = constructCellVectors(stackSize,cells2);
					if(cellsTimeIndex1 != null)
						cellsTimeIndex = combineCellsTimeIndex(cellsTimeIndex2,cellsTimeIndex1,slice2);
					else {
						cellsTimeIndex = cellsTimeIndex2;
					}
				}
			}
			Roi[] roisSlice3 = constructRoisSlice(roiArray, slice3);
			if(roisSlice3!=null)
			{
				Ins_cell[] cells3 = constructCellsListFromStack(roisSlice3);
				if(cells3 != null)
				{
					Arrays.sort(cells3);
					setCellsNumber(cells3);
					Ins_cellsVector cellsTimeIndex3[] = constructCellVectors(stackSize,cells3);	
					cellsTimeIndex = combineCellsTimeIndex(cellsTimeIndex, cellsTimeIndex3,slice2);
				}
			}			
			if(cellsTimeIndex == null)
				return;
			
			IJ.log("-------------------ligneage running slice : " + slice2+"--------------------");	
		}else {
			cellsTimeIndex = cellsTimeIndex1;
		}
		//System.out.println("	Number of cells ");		
		
		if(cellsTimeIndex == null)
			return;
		
		Ins_cell rootCell = cellsTimeIndex[0].getCell(1);
		if(rootCell == null)
			return;
		
		rName= oRootName.getText();
		rootCell.setName(rName);
		Ins_cell rootParent =new Ins_cell(-1, -1, null, null,null,0,0,0,0,false); 
		rootCell.parent = rootParent;

		if(mConsiderAllFirstChannelCells.isSelected()){
			String name = "New";
			for(int a=2;a<=cellsTimeIndex[0].getCellVector().size();a++)
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
				cellsTimeIndex[0].getCell(a).setName(name);
				cellsTimeIndex[0].getCell(a).parent = new Ins_cell(-1, -1, null, null,null,0,0,0,0,false);
			}
		}

	/*	Ins_editor logEditor = new Ins_editor();
		logEditor.setTitle("slice-"+slice2);
		if(writeToResult){
			logEditor.setVisible(true);
			if(mChRFP.isSelected())
			{
					logEditor.append("cell_label"+"\t"+
						"last_cell" + "\t"+	 // check if it's the last cell add
						"name"+"\t" +
						"timeIndex"+"\t"+
						"time"+"\t"+
						"cell_num"+"\t"+
						"length"+"\t"+
						"skeletonLength"+"\t"+
						"sumIntensity" + "\t"+
						"cell_meanIntensity"  + "\t"+
						"cell_area" + "\t"+		
						"xCentroid"+"\t"+
						"yCentroid"+"\t"+
						"type"+ "\t"+
						"cellDividedFromPrevious"+ "\t"+
						"F_C" + "\t"
						+ "F_total"+ "\t" // new delete the original one
						+ "C_total"+ "\t" // new
						+ "agg_label" + "\t"
						+ "agg_area"+ "\t" 
						+ "agg_meanIntensity"+ "\t" 
						+ "agg_radius"+ "\t"
						+ "agg_center_x" + "\t"
						+ "agg_center_y" + "\t"
						+ "agg_relativeCenter_x"+"\t"
						+ "agg_relativeCenter_y"	
						+ "\t"+ "\r"+"\n");
				logEditor.append(rootCell.toString());
				// for Yifan
				logEditor.append("cell_label"+"\t"+
						"last_cell" + "\t"+	 // check if it's the last cell add
						"name"+"\t" +
						"timeIndex"+"\t"+
//						"time"+"\t"+
						"cell_num"+"\t"+
						"death_state"+"\t"+
						"length"+"\t"+
//						"skeletonLength"+"\t"+
//						"sumIntensity" + "\t"+
						"xCenter"+"\t"+
						"yCenter"+"\t"+
						"cell_area" + "\t"+
						"meanIntensity"  + "\t"+							
						"medianIntensity"  + "\t"+
						"cell_area2" + "\t"+
						"cell_meanIntensity3"  + "\t"+
						"cell_area3" + "\t"							
						"\t"+ "\r"+"\n");
				//logEditor.append(rootCell.toStringYifan());
			}
			else {
				logEditor.append("cell_label"+"\t"+
						"last_cell" + "\t"+	 // check if it's the last cell add
						"name"+"\t" +
						"timeIndex"+"\t"+
						"time"+"\t"+
						"cell_num"+"\t"+
						"length"+"\t"+
						"skeletonLength"+"\t"+
						"xCentroid"+"\t"+
						"yCentroid"
						+ "\t"+ "\r"+"\n");
				//logEditor.append(rootCell.toString2());
			}
		}
	    
		Vector<Ins_cell> errorCellsVector = new Vector<Ins_cell>(10);			
		roisArrayToManager[slice2-1] = lineageCellsTimeIndex(cellsTimeIndex, errorCellsVector, true,roisArrayToManager[slice2-1]);
		correctErrorCell(errorCellsVector,cellsTimeIndex,false, false);
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
					green = false;
					red = true;
				}else {
					green = true;
					red = false;
				}
			}
			System.out.println("------------------- iteration : "+ iterNumber);
			correctErrorCell(errorCellsVector,cellsTimeIndex,green, red);
			cellsTimeIndex = reinitialCellsTimeIndex(cellsTimeIndex);
			errorCellsVector = new Vector<Ins_cell>(10);			
			roisArrayToManager[slice2-1] = lineageCellsTimeIndex(cellsTimeIndex, errorCellsVector, true,roisArrayToManager[slice2-1]);
			iterNumber++;
		}
		System.out.println("----------------error cells' number: " + errorCellsVector.size());
		if(writeToResult){
			if(slice2!=slice3)
			{
				int sliceMin = Math.min(slice2, slice3);
				String name = "";
				if(slice2 == sliceMin)
					name = "cfpLineage";
				else {
					name = "rfpLineage";
				}
				int slice = sliceMin/3+1;
				for(int i=sliceMin-1;i<=sliceMin+1;i++)
				{
					String nameL = "";
					ImageProcessor ip = redirectImp.getImageStack().getProcessor(i);				
					if(i==sliceMin-1)
						nameL = "slice-" + String.valueOf(slice) + "-" + "YFP-" +name; 
					else if (i==sliceMin) {
						nameL = "slice-" + String.valueOf(slice) + "-" + "CFP-" +name;
					}else if (i==sliceMin+1) {
						nameL = "slice-" + String.valueOf(slice) + "-" + "RFP-" +name;
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
					wrtieResultTologEditor(cellsTimeIndex, logEditor,ip);
					logEditor.setPath(outputDirectoryPath+nameL+".txt");
					System.out.println("save file : " + outputDirectoryPath+nameL+".txt");
					logEditor.save();
				}			
			}else {
				String nameL = String.valueOf(slice1);
				ImageProcessor ip = redirectImp.getImageStack().getProcessor(slice1);	
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
				wrtieResultTologEditor(cellsTimeIndex, logEditor,ip);
				logEditor.setPath(outputDirectoryPath+nameL+".txt");
				System.out.println("save file : " + outputDirectoryPath+nameL+".txt");
				logEditor.save();
			}
		}
		IJ.showStatus(slice2+" "+"done!");
	}
*/
	
	
	
	
	
	
	
	

	

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

//	public void refineCellSize1(Ins_cellsVector[] cellsTimeIndex,ImageProcessor ip) 
//	{
//		int a = 1;//get the first cell on the top of the channel
//		for(int i=0;i<cellsTimeIndex.length;i++)
//		{
//			Ins_cellsVector actualChannel = cellsTimeIndex[i];
//			Ins_cell actualCell = actualChannel.getCell(a);
//			if(actualCell == null || actualCell.parent==null)
//				continue;
//			
//			Vector<Integer> y_refine = new Vector<Integer>(5);
//			
//			System.out.println("Start Refine from : " + actualCell.getName());
//			while(actualCell!=null)
//			{
//				int top = findRefinePosition(actualCell.getRoi(), ip);
//				y_refine.add(top);				
//				i=actualCell.getTimeIndex();
//				System.out.println("	Refine process : "+ " Current cell name : " + actualCell.getName() + " find top y : " + top + " timeIndex : "+ i);
//				actualCell = actualCell.nextCell;				
//			}			
//			Integer[] y = new Integer[y_refine.size()]; 
//			y = y_refine.toArray(y);
//			Arrays.sort(y);
//					
//			int yMedian = y[(int)(y.length*0.5)];
//			System.out.println("	Median y found : " + yMedian);
//			
//			actualCell = actualChannel.getCell(a);
//			while(actualCell!=null)
//			{
//				refineCellSize(actualCell,yMedian);		
//				actualCell = actualCell.nextCell;
//			}
//		}
//	}
	
	
//	public void refineCellSizeByCorrelation(Ins_cellsVector[] cellsTimeIndex,ImageProcessor ip) 
//	{
//		int a = 1;//get the first cell on the top of the channel
//		for(int i=0;i<cellsTimeIndex.length;i++)
//		{
//			Ins_cellsVector actualChannel = cellsTimeIndex[i];
//			Ins_cell actualCell = actualChannel.getCell(a);
//			if(actualCell == null || actualCell.parent==null)
//				continue;
//			
//			//Vector<Integer> y_refine = new Vector<Integer>(5);
//			
//			System.out.println("Start Refine from : " + actualCell.getName());
//			
//			Vector<int[]> refinePos = new Vector<int[]>();
//			Ins_cell nextCell = actualCell.nextCell;
//			while(actualCell!=null)
//			{				
//				int top = findRefinePosition(actualCell.getRoi(), ip);
//				int delay = 0;
//				if(nextCell!=null)
//					delay = peakCrossCorrelation1(actualCell.getRoi(), nextCell.getRoi(), ip);
//				refinePos.add(new int[]{top, delay});
////				y_refine.add(top);				
//				i=actualCell.getTimeIndex();
//				if(i*(roi_width+6) == 10836)
//					System.out.println("debug");
//				if(nextCell!=null)
//					System.out.println("	Refine process : "+ " Current cell name : " + actualCell.getLabel() + " find top y : " + top + " x : "+ i*(roi_width+6) + " nextCell name: " + nextCell.getLabel() + " delay : " + delay);
//				actualCell = actualCell.nextCell;
//				if(actualCell!=null)
//					nextCell = actualCell.nextCell;
//			}
//			
//			actualCell = actualChannel.getCell(a);
//			int pos = refinePos.get(0)[0];
//			int k = 0;
//			while(actualCell!=null)
//			{
//				if(k>0)
//					pos = pos + refinePos.get(k-1)[1];				
//				pos = pos - 1;
//				if(pos<0 || refinePos.get(k)[0] <= 0)
//					pos = 0;				
//				refineCellSize(actualCell,pos);
//				actualCell = actualCell.nextCell;
//				k++;
//			}
//		}
//	}
	
//	private void refineCellSize(Ins_cell actualCell, int yMedian) 
//	{
//		Roi roi = actualCell.getRoi();
//		int x = actualCell.getPosition()[0];
//		int width = roi.getBounds().y;
//		int height = roi.getBounds().height;		
//		Roi roi2 = new Roi(x, yMedian, width, height - yMedian);
//		roi2.setPosition(roi.getPosition());
//		roi2.setName(roi.getName());		
//		actualCell.setRoi(roi2);
//	}
//
////	// the ip should be 
//	private int peakCrossCorrelation1(Roi roi1, Roi roi2, ImageProcessor ip)
//	{
//		ImagePlus impPlotProfile = new ImagePlus("profile", ip);
//		int height = (int)Math.min(roi1.getBounds().getHeight(), roi2.getBounds().getHeight());
//		height = 60;
//		impPlotProfile.setRoi(roi1.getBounds().x, roi1.getBounds().y, roi_width, height/2);		
//		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);
//		double[] profile1 = pPlot.getProfile();
//		
//		impPlotProfile.setRoi(roi2.getBounds().x, roi2.getBounds().y, roi_width, height/2);
//		ProfilePlot pPlot2 = new ProfilePlot(impPlotProfile, true);
//		double[] profile2 = pPlot2.getProfile();
//		
//		cutoffWhitePart(profile1,profile2);
//		return findMaxDelay(profile1,profile2);
//		
//	}
	
//	private void cutoffWhitePart(double[] profile1, double[] profile2) {
//		
//		int n = 0;
//		for(int i=2; i<profile1.length;i++)
//		{			
//			double v1 = profile1[i];
//			double v2 = profile2[i];
//			
//			if(v1!=0 && v2/v1 >= 1.25)
//			{
//				profile2[i] = 0;
//				n++;
//			}else {
//				break;
//			}
//		}
//		
//		if(n>0)
//		{
//			profile2[0] = 0;
//			profile2[1] = 0;
//			n = n + 2;
//		}
//		
//		for(int i= profile1.length-1; i>=profile1.length-n; i--)
//			profile1[i] = 0;
//		
//		if(n == 0)
//		{
//			for(int i=2; i<profile1.length;i++)
//			{			
//				double v1 = profile1[i];
//				double v2 = profile2[i];
//
//				if(v2!=0 && v1/v2 >= 1.25)
//				{
//					profile1[i] = 0;
//					n++;
//				}else {
//					break;
//				}
//			}
//
//			if(n>0)
//			{
//				profile2[0] = 0;
//				profile2[1] = 0;
//				n = n + 2;
//			}
//
//			for(int i= profile2.length-1; i>=profile2.length-n; i--)
//				profile2[i] = 0;
//
//		}
//	
//	}
//
//	private int findMaxDelay(double[] profile1, double[] profile2) {
//		int i,j;
//		double mx,my,sx,sy,sxy,denom;
//		/* Calculate the mean of the two series x[], y[] */
//		mx = 0;
//		my = 0;   
//		int n = profile1.length;
//		for (i=0;i<n;i++) {
//			mx += profile1[i];
//			my += profile2[i];
//		}
//		mx /= n;
//		my /= n;
//
//		/* Calculate the denominator */
//		sx = 0;
//		sy = 0;
//		for (i=0;i<n;i++) {
//			sx += (profile1[i] - mx) * (profile1[i] - mx);
//			sy += (profile2[i] - my) * (profile2[i] - my);
//		}
//		denom = Math.sqrt(sx*sy);
//		int maxdelay = 10;
//		double[] r = new double[2*maxdelay];
//		int maxDelay = 0;
//		/* Calculate the correlation series */
//		double max = Double.MIN_VALUE;
//		for (int delay=-maxdelay;delay<maxdelay;delay++) {
//			sxy = 0;
//			for (i=0;i<n;i++) {
//				j = i + delay;
//				if (j < 0 || j >= n)
//					continue;
//				else
//					sxy += (profile1[i] - mx) * (profile2[j] - my);
//				/* Or should it be (?)
//		         if (j < 0 || j >= n)
//		            sxy += (x[i] - mx) * (-my);
//		         else
//		            sxy += (x[i] - mx) * (y[j] - my);
//				 */
//			}
//			r[delay+maxdelay]=sxy/denom;
//			if(r[delay+maxdelay] > max)
//			{
//				max = r[delay + maxdelay];
//				maxDelay = delay;
//			}
//			/* r is the correlation coefficient at "delay" */
//		}
//		
//		return maxDelay;
//	}
//
//	private int findRefinePosition(Roi roi, ImageProcessor ip) 
//	{		
//		AutoThresholder autoThresholder = new AutoThresholder();	
//		ip.setRoi(roi);
//		ImageProcessor ipSeg = ip.crop();						
//		ipSeg = ipSeg.convertToByte(true);
//		ipSeg.setRoi(2, 0, roi_width-4, ipSeg.getHeight());
//		ipSeg = ipSeg.crop();
//
//		int otsuLevel = (int)(autoThresholder.getThreshold(Method.MaxEntropy, ipSeg.getHistogram())*1.1);
//
//		ipSeg.threshold(otsuLevel);
//		ipSeg.erode();
//		ipSeg.dilate();
//		
//		int top = -1;		
//		int numdark = 0;					
//		boolean satisfaiedLength = false;
//		for(int y = 0; y<ipSeg.getHeight(); y++)
//		{
//			int grey = ipSeg.get(ipSeg.getWidth()/2, y);
//			if(grey == 0 )
//			{
//				if(top == -1)
//					top = y;
//				numdark ++;
//			}else {
//				top = -1;
//				numdark = 0;
//			}
//			if(numdark > minPeakDist/2)
//			{
//				satisfaiedLength = true;
//				break;
//			}
//		}
//		
//		if(satisfaiedLength)
//		{
//			int xx = ipSeg.getWidth()/2;
//			while( top > 0 && xx > 0 && xx < ipSeg.getWidth() - 1 && (ipSeg.get(xx, top -1) == 0 || ipSeg.get(xx-1, top-1) == 0 || ipSeg.get(xx+1, top -1) == 0))
//			{
//				if(ipSeg.get(xx, top - 1) == 0)
//				{
//
//				}else if (ipSeg.get(xx-1, top -1) == 0) {
//					xx = xx - 1;
//				}else if (ipSeg.get(xx + 1, top -1) == 0) {
//					xx = xx + 1;
//				}
//				top = top-1;
//			}
//
//			//			if(j==0)
//			//				top = 0;
//
//			//top = top -1 > 0 ? top-1:0;							
//
//			top = 0;
//			ip.setRoi(roi);
//			ImageProcessor ipSeg2 = ip.crop();						
//			ipSeg2 = ipSeg2.convertToByte(true);
//			ipSeg2.setRoi(0, 0, roi_width, ipSeg2.getHeight());
//			ipSeg2 = ipSeg2.crop();				
//
//			//				if(slice == 9337/(roi_width + 6))
//			//				{
//			//					//new ImagePlus("", ipSeg2.duplicate()).show();
//			//					IJ.save(new ImagePlus("", ipSeg2.duplicate()), "d:/a.tif");
//			//					//new ImagePlus("", ipSeg2).show();
//			//				}
//
//			otsuLevel = (int)(autoThresholder.getThreshold(Method.IsoData, ipSeg2.getHistogram()));
//			ipSeg2.threshold(otsuLevel);	
//			//				ipSeg2.dilate();
//			//				ipSeg2.erode();
//
//
//
//			//				if(slice == 34/(roi_width + 6))
//			//				{
//			//					//new ImagePlus("", ipSeg2.duplicate()).show();
//			//					IJ.save(new ImagePlus("", ipSeg2), "d:/t.tif");
//			//					//new ImagePlus("", ipSeg2).show();
//			//				}
//
//			int hole = 0;
//			int countWhiteStable = 0;
//			for(int yy=0; yy<ipSeg2.getHeight(); yy++)
//			{
//				int countWhite = 0;																	
//				boolean crossCenter = false;									
//				for(int xxx =0; xxx<ipSeg2.getWidth();xxx++)
//				{										
//					if (ipSeg2.get(xxx, yy)== 255) {
//						countWhite = 1;
//						int x1 = xxx;
//						while(x1<ipSeg2.getWidth() - 1)
//						{												
//							x1++;
//							if(ipSeg2.get(x1, yy) == 255) // if back to 255
//							{
//								countWhite++;													
//								if(x1 == 0.5*ipSeg2.getWidth())
//								{
//									int left = 0;
//									int right = 0;
//									int x3 = x1;
//									while(x3<ipSeg2.getWidth()-1)
//									{
//										x3++;
//										if(ipSeg2.get(x3, yy) == 255)
//											right++;						
//									}
//									x3 = x1;
//									while(x3>0)
//									{
//										x3--;
//										if(ipSeg2.get(x3, yy) == 255)
//											left++;															
//									}
//									if(Math.abs(right-left)<4 && right > 2 && left > 2)														
//										crossCenter = true;
//								}
//							}else {
//								int x2 = x1;
//								while(x2<ipSeg2.getWidth() - 1)
//								{
//									x2++;
//									if(ipSeg2.get(x2, yy)==255)
//									{
//										hole++;
//										countWhite = 0;
//										countWhiteStable = 0;															
//										break;
//									}
//								}
//								break;
//							}
//						}
//
//						if(countWhite>=5)
//							countWhiteStable++;
//
//						break;
//					}
//				}
//				if(hole == 1 && yy>=5)
//				{
//					top = yy - 1;
//					break;
//				}else if(hole >= 2)
//				{
//					top = yy - 2;
//					break;
//				}else if (countWhiteStable > 1 && !crossCenter) {
//					top = yy;
//					break;
//				}else {
//					continue;
//				}
//				//			if(j==0)
//				//				roi2 = new Roi(slice*(roi_width+6), top, roi_width, bottom - top + 1);
//				//			else {
//				//				roi2 = new Roi(slice*(roi_width+6), position[j-1]+ top, roi_width, bottom - top + 1,1);
//			}
//			return top;
//		}else {
//			return 0;
//		}
//	}

	
	
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
			return null;
		Roi[] sliceRois = new Roi[j];
		System.arraycopy(currentRois, 0, sliceRois, 0, j);
		return sliceRois;
	}
	
	// deep clone constructing the rois
	private Roi[] constructRoisSliceAll(Roi[] roiArray, int slice)
	{
		int count = roiArray.length;
		Roi[] currentRois = new Roi[count];		
		int j=0;
		for (int i=0; i<count; i++) {	
			Roi cellRoi = roiArray[i];
			if(cellRoi.getPosition() != slice)
				continue;
			currentRois[j] = cellRoi;
			j++;
		}
		if(j==0)
			return null;
		Roi[] sliceRois = new Roi[j];
		System.arraycopy(currentRois, 0, sliceRois, 0, j);
		return sliceRois;
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
			// 
			e.printStackTrace();
		} catch (IOException e) {
			// 
			e.printStackTrace();
		}
	}



	public static ImagePlus calibrateAndSegmentation(ImagePlus impRFP2, Ins_param param) {
		if(!param.getSealedOffAtTop())
		{
			flipVertical(impRFP2);
		}
		if(param.getAngle()!=0)
		{
			rotateImage(impRFP2, param.getAngle());						
			impRFP2.updateAndDraw();
		}
		ImagePlus impRFP2cropOriginal = new ImagePlus();
		ImageStack stackRFPOriginal = new ImageStack(param.getWidth_align(), param.getHeight_align(), impRFP2.getStackSize());
		for (int i=1; i<=impRFP2.getStackSize(); i++) {
			IJ.showStatus("Cropping cellst " + i + "/" + impRFP2.getStackSize() + 
					" ... (Press 'ESC' to Cancel)");
			System.out.println("Cropping " + i + "/" + impRFP2.getStackSize() + 
					" ... (Press 'ESC' to Cancel)");;
			if (IJ.escapePressed())
				break;
			ImageProcessor ip1 = impRFP2.getImageStack().getProcessor(i);						
			ip1.setRoi(param.getPosition_l()[((i-1)/param.getIntervalSlices())*param.getIntervalSlices()], param.getPosition_h()[((i-1)/param.getIntervalSlices())*param.getIntervalSlices()], param.getWidth_align(), param.getHeight_align());	// 1 for more precision depends on experiments
			ImageProcessor ip2 = ip1.crop();						
			stackRFPOriginal.setPixels(ip2.duplicate().getPixels(), i);	
			stackRFPOriginal.setSliceLabel(impRFP2.getImageStack().getShortSliceLabel(i), i);
		}		
		impRFP2cropOriginal.setStack(stackRFPOriginal);
		impRFP2cropOriginal.setTitle(impRFP2.getTitle());		
		if(param.getStabilized())
		{
			Ins_Stabilizer_Log_Applier stabilizerLog = new Ins_Stabilizer_Log_Applier();
			stabilizerLog.setuppRFP(param);
			stabilizerLog.setup("", impRFP2cropOriginal);
			stabilizerLog.run(impRFP2cropOriginal.getProcessor());
		}	
		impRFP2cropOriginal.show();
		ImagePlus rearrangedRFPOriginal = param.toSSImage(impRFP2cropOriginal);
		return rearrangedRFPOriginal;	
	}
	
	
	
	/** Adds the specified ROI to the list. The third argument ('n') will 
	be used to form the first part of the ROI label if it is >= 0. */
	public static int setArrowName1(int slice, Roi roi, int n) {
		if (roi==null) return n;
		String label = getLabel(slice, roi, n);
		roi.setName(label);
		return n+1;
	}
	
	public static Roi[] getRoiFromManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");										
		if (frame==null || !(frame instanceof RoiManager))
		{return null;}
		RoiManager roiManager = (RoiManager)frame;					
		//roiManager.addKeyListener(this);		
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
	
	void openRoiZip(String path) { 
		ZipInputStream in = null; 
		ByteArrayOutputStream out; 
		int nRois = 0; 
		try { 
			in = new ZipInputStream(new FileInputStream(path)); 
			byte[] buf = new byte[1024]; 
			int len; 
			ZipEntry entry = in.getNextEntry(); 
			while (entry!=null) { 
				String name = entry.getName();
				if (name.endsWith(".roi")) { 
					out = new ByteArrayOutputStream(); 
					while ((len = in.read(buf)) > 0) 
						out.write(buf, 0, len); 
					out.close(); 
					byte[] bytes = out.toByteArray(); 
					RoiDecoder rd = new RoiDecoder(bytes, name); 
					Roi roi = rd.getRoi(); 
					if (roi!=null) { 
						addRoiToManager(roi);
						nRois++;
					} 
				} 
				entry = in.getNextEntry(); 
			} 
			in.close(); 
		} catch (IOException e) {System.out.println(e.toString());} 
		if(nRois==0)
			System.out.println("This ZIP archive does not appear to contain \".roi\" files");
		updateShowAll();
	} 
	
	public void deleteRoiToManager()
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
		if(roiManager.getCount() != 0)
		{
			System.out.println(" panel delete roi : " + roiManager.getCount());
			roiManager.runCommand("delete");
		}
	}
	
/*	*//** Adds the specified ROI to the list. The third argument ('n') will 
	be used to form the first part of the ROI label if it is >= 0. *//*
	public void addArrowRoi(int slice, Roi roi, int n) {
		if (roi==null) return;
		String label = roi.getName();
		if (label==null)
			label = getLabel(slice, roi, n);
		if (label==null) return;
		list.add(label);
		roi.setName(label);
		rois.put(label, (Roi)roi.clone());
	}
*/	
	
	
	
//	String getLabel(ImagePlus imp, Roi roi, int n) {
//		Rectangle r = roi.getBounds();
//		int xc = r.x + r.width/2;
//		int yc = r.y + r.height/2;
//		if (n>=0)
//			{xc = yc; yc=n;}
//		if (xc<0) xc = 0;
//		if (yc<0) yc = 0;
//		int digits = 4;
//		String xs = "" + xc;
//		if (xs.length()>digits) digits = xs.length();
//		String ys = "" + yc;
//		if (ys.length()>digits) digits = ys.length();
//		if (digits==4 && imp!=null && imp.getStackSize()>=10000) digits = 5;
//		xs = "000000" + xc;
//		ys = "000000" + yc;
//		String label = ys.substring(ys.length()-digits) + "-" + xs.substring(xs.length()-digits);
//		if (imp!=null && imp.getStackSize()>1) {
//			int slice = roi.getPosition();
//			if (slice==0)
//				slice = imp.getCurrentSlice();
//			String zs = "000000" + slice;
//			label = zs.substring(zs.length()-digits) + "-" + label;
//			roi.setPosition(slice);
//		}
//		return label;
//	}
	
	
	public static String getLabel(int slice, Roi roi, int n) {				
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


	
	
	void updateShowAll() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) return;
		ImageCanvas ic = imp.getCanvas();
		if (ic!=null && ic.getShowAllROIs())
			imp.draw();
	}
	
	/*private int[] getSelectedIndexes() {
			return list.getSelectedIndexes();
	}
	
	int[] getAllIndexes() {
		int count = list.getItemCount();
		int[] indexes = new int[count];
		for (int i=0; i<count; i++)
			indexes[i] = i;
		return indexes;
	}*/




	@Override
	public void stateChanged(ChangeEvent e) {
		// 
		if(e.getSource() == oChRotSlider)
		{			
			if(imp_ref==null)
			{
				imp_ref = WindowManager.getCurrentImage();				
				if(imp_ref != null)
				{
					imp_ref.show();
					ip_ref = imp_ref.getProcessor().duplicate();
//					if(ip_ref==null)
//						ip_ref = imp_ref.getProcessor().duplicate();
				}
			}

			if(imp_ref != null)
			{
				if(!imp_ref.isVisible())
					imp_ref.show();
				//imp_ref.getWindow().setAlwaysOnTop(true);
				imp_ref.setProcessor(ip_ref.duplicate()); // as duplicate, otherwise it wonn't work
				imp_ref.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
				IJ.showStatus("Angle : "+ (double)oChRotSlider.getValue()/10);
				System.out.println("Angle : "+(double)oChRotSlider.getValue()/10);
				imp_ref.getProcessor().rotate((double)oChRotSlider.getValue()/10);
//				drawGridLines(6);
				imp_ref.updateAndDraw();
				IJ.showStatus("Rotate degree "+(double)oChRotSlider.getValue()/10);
			}
			
			//pRFP_angle = (double)((double)oChRotSlider.getValue()/10);
		}
	}
	
	void drawGridLines(int lines) {
		ImageCanvas ic = imp_ref.getCanvas();
		if (ic==null) return;
		if (lines==0) {ic.setVisible(true);; return;}
		GeneralPath path = new GeneralPath();
		float width = imp_ref.getWidth();
		float height = imp_ref.getHeight();
		float xinc = width/lines;
		float yinc = height/lines;
		float xstart = xinc/2f;
		float ystart = yinc/2f;
		for (int i=0; i<lines; i++) {
			path.moveTo(xstart+xinc*i, 0f);
			path.lineTo(xstart+xinc*i, height);
			path.moveTo(0f, ystart+yinc*i);
			path.lineTo(width, ystart+yinc*i);
		}
		ic.setVisible(true);
//		ic.setDisplayList(path, null, null);
	}
	
	
	public ImagePlusAndTime openFDialog_stack(ImagePlus imp, int interval,boolean sortBydate) {
		String pathDirectory = IJ.getDirectory("current");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    JFileChooser fc = new JFileChooser();		
	    try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return null;}
		fc.setMultiSelectionEnabled(true);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Tiff file", "tif", "tiff");
		fc.setFileFilter(filter);		
		if (directory!=null)
			fc.setCurrentDirectory(directory);			
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return new ImagePlusAndTime(null, null,null, -1, false);
		
		boolean oneFileOpened;		
		int stackSize = -1;
		
		File[] files = fc.getSelectedFiles();			
		if (files.length==0) {
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}						
		String directory_ = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();			
		String name_0 = files[0].getName();	
		String[] time2;
		String stackName = "s";
		String zoom = "";
		
		if(files.length == 1)
		{
			imp = load(directory_, name_0);
			time2 = new String[imp.getImageStackSize()];
			for(int i=0;i<imp.getStackSize();i++)
			{
				String[] nameSlice;
				//System.out.println(imp.getImageStack().getSliceLabel(i+1));
				if(imp.getImageStack().getSliceLabel(i+1) != null)
					if(imp.getImageStack().getSliceLabel(i+1).contains("-ms-"))
					{
						nameSlice = imp.getImageStack().getSliceLabel(i+1).split("-ms-");
						time2[i] = nameSlice[1];
					}else {
						time2[i] = String.valueOf(i);
					}
				else
					time2[i] = "NO-TIME";
			}

			try {
				stackName = name_0.substring(0, name_0.indexOf("_Ins"));
			} catch (Exception e) {
				stackName = "s";
			}
			oneFileOpened = true;
		}
		else
		{				
			ImageStack stack=new ImageStack(load(directory_, name_0).getWidth(),load(directory_, name_0).getHeight());
			String[] name=new String[files.length];
			String[] time=new String[files.length];				
			int ii=0;							  			
			for (int i=0; i<files.length; i=i+interval) {
				name[ii] = files[i].getName();
				if(getAcuquisitionTime(directory_, name[ii]) != 0)
					files[i].setLastModified(getAcuquisitionTime(directory_, name[ii]));										
				ii++;
			}			
			zoom = getZoomCoefficient(directory_,name[0]);
			
			if(sortBydate)
			{
				sortFilesByTime(files);
			}
			ii = 0;
			for (int i=0; i<files.length; i=i+interval) {
				name[ii] = files[i].getName();
				time[ii] = String.valueOf(getAcuquisitionTime(directory_, name[ii]));		
				ii++;
			}
						
			String[] name2=new String[ii];
			
			try {
				stackName = name[0].substring(0,name[0].indexOf("_t"));
			} catch (IndexOutOfBoundsException e) {
				stackName = "s";
			}
			
			time2=new String[ii];
			for(int i=0;i<ii;i++)
			{
				name2[i]=name[i];
				time2[i]=time[i];
			}
			String[] list; 
			if(!sortBydate)
			{
				list = sortFilesByName(name2);
			}					
			else {
				list = name2;
			}
			
			for (int i=0; i<list.length; i++) {
				System.out.println("Adding slice:" +(i+1)+"/"+list.length+"		"+ list[i]);
				IJ.showStatus("Adding slice: "+(i+1)+"/"+list.length +"		"+ list[i]);
				ImageProcessor ipPath = load(directory_, list[i]).getProcessor();
				if(doDenoise)
				{
					FloatProcessor ipFloat = (FloatProcessor)ipPath.convertToFloat();
					ROF_Denoise.denoise(ipFloat, 25);
					ROF_Denoise.denoise(ipFloat, 25);
					ShortProcessor sp = (ShortProcessor)ipFloat.convertToShort(false);
					if(doNormalizeSlice)
						sp = ImageBuilder.adjustBackgroundHistogram(sp);
					if(sp == null)
						break;
					stack.addSlice(list[i]+"-ms-"+time2[i], sp);
				}else {
					stack.addSlice(list[i]+"-ms-"+time2[i], ipPath);
				}
			}
			imp=new ImagePlus(stackName+"_Ins_Original", stack);
			stackSize = imp.getStackSize();	
			oneFileOpened = false;
		}
		ImagePlusAndTime impWithPropeties = new ImagePlusAndTime(imp, time2, stackName, stackSize, oneFileOpened);
		impWithPropeties.setZoom(zoom);		
		return impWithPropeties;
	}
	
	public static void sortFilesByTime(File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {			
				if (o1.lastModified() <= o2.lastModified()) {
					return -1;
				} else {
					return 1;
				}
			}
		});	
	}
	
    public static String[] sortFilesByName(String[] list) {
        int listLength = list.length;
        int first = listLength>1?1:0;
        if ((list[first].length()==list[listLength-1].length())&&(list[first].length()==list[listLength/2].length()))
        {ij.util.StringSorter.sort(list); return list;}
        int maxDigits = 15;
        String[] list2 = null;
        char ch;
        for (int i=0; i<listLength; i++) {
            int len = list[i].length();
            String num = "";
            for (int j=0; j<len; j++) {
                ch = list[i].charAt(j);
                if (ch>=48&&ch<=57) num += ch;
            }
            if (list2==null) list2 = new String[listLength];
            num = "000000000000000" + num; // prepend maxDigits leading zeroes
            num = num.substring(num.length()-maxDigits);
            list2[i] = num + list[i];
            
            System.out.println("Sort by name : "+ list2[i]);
        }
        if (list2!=null) {
            ij.util.StringSorter.sort(list2);
            for (int i=0; i<listLength; i++)
                list2[i] = list2[i].substring(maxDigits);
            return list2;
        } else {
            ij.util.StringSorter.sort(list);
            return list;
        }
    }
	
	public static ImagePlus load(String directory, String name) {		
		if ((name == null) || (name == "")) return null;			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(directory+name);
		return imp; 
	}
	
	public static long getAcuquisitionTime(String directory, String name) {		
		if ((name == null) || (name == "")) return 0;			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(directory+name);
		Properties properties = imp.getProperties();
		Date date = null;
		if(properties != null)
		{
			String p = properties.toString();
			String t = null;
			if (p.contains("acquisition-time-local")) {
				t = p.substring(p.indexOf("acquisition-time-local")+43,p.indexOf("acquisition-time-local")+60);
				//System.out.println(t);
			}			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");				    						
			try {    
				date = formatter.parse(t);
				//System.out.println(name+" "+date.toString());
			} catch (ParseException e) {    
				e.printStackTrace();    
			}
			return date.getTime();
		}else {
			return 0;
		}
		 
	}
	
	
	public static String getZoomCoefficient(String directory, String name) {		
		if ((name == null) || (name == "")) return "";			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(directory+name);
		Properties properties = imp.getProperties();
		
		if(properties != null)
		{
			String p = properties.toString();
			String t = null;
			if (p.contains("_MagSetting_")) {
				t = p.substring(p.indexOf("_MagSetting_")+35,p.indexOf("_MagSetting_")+39);
				return t;
			}else {
				return "";
			}
			
		}else {
			return "";
		}
		 
	}
	


}



class RoiPropeties{	
	public double length;
	double area;
	double circularity;
	double yCentroid;
	double xCentroid;
	double sumIntensity;
	double meanIntensity;
	double perimeter;
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

class ImagePlusAndTime{
	ImagePlus imp;
	String[] time;
	String posName;
	String zoom;
	boolean oneFileOpened;	
	int stackSize;
	
	public ImagePlusAndTime(ImagePlus imp, String[] time, String namePos, int stackSize, boolean oneFileOpened) {
		this.imp = imp;
		this.time = time;
		this.posName = namePos;
		this.stackSize = stackSize;
		this.oneFileOpened = oneFileOpened;
	}
	
	public int getStacksize() {
		// 
		return stackSize;
	}

	public void setZoom(String zoom)
	{
		this.zoom = zoom;
	}
	
	public void setTime(String[] time)
	{
		this.time = time;
	}
	
}

class ProcessRunner{

	private ImagePlus imp;
	
	private Ins_seg_preprocessing mProcess;
//	private Vector<Ins_cropImagePlus> cropImages;
	List list;
	Hashtable<String, Roi> rois;
	private ImagePlus wholeWImage;
	private ImagePlus imp_rearranged;
	private Ins_ParticleAnalyzer pAnalyzer=null;
	private int slice = -1;
	private String posName="s";
	private Ins_param param_position;
	
	
	public int getSliceIndex() {
		return slice;
	}

	ProcessRunner(ImagePlus imp_in,Ins_param pRFP) {
		this.imp = imp_in;
		this.posName = pRFP.getPositionName();				
		this.param_position = pRFP;		
	}
	
	
	public List getList()
	{
		return list;
	}
	
	public Hashtable<String, Roi> getRois()
	{
		return rois;
	}
	
	public Ins_ParticleAnalyzer getPAnalyzer()
	{
		return pAnalyzer;
	}
	
//	public Vector<Ins_cropImagePlus> getRoiImages()
//	{
//		return cropImages;
//	}
	
	public ImagePlus getWholeWImage()
	{
		return wholeWImage;
	}
	
	public ImagePlus getImageRearranged()
	{
		return imp_rearranged;
	}
	
	public ImagePlus getImage() {
		return this.imp;
	}
	
	public <T> void runCommand(boolean memorySave) {
            mProcess = new Ins_seg_preprocessing(imp,param_position); 
            mProcess.buildImages_preProcess(posName);                        
            imp = mProcess.getCurrentImage();
            Ins_seg_Processing ins_process=new Ins_seg_Processing(imp, param_position);
            ins_process.cutSSImage();				
	}
		
	
	/** Returns the slice number associated with the specified name,
	or -1 if the name does not include a slice number. */
	public static int getSliceNumber(String label) {
		int slice = -1;
		if (label.length()>=14 && label.charAt(4)=='-' && label.charAt(9)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,4),-1);
		else if (label.length()>=17 && label.charAt(5)=='-' && label.charAt(11)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,5),-1);
		else if (label.length()>=20 && label.charAt(6)=='-' && label.charAt(13)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,6),-1);
		return slice;
	}

	/** Returns the slice number associated with the specified ROI or name,
	or -1 if the ROI or name does not include a slice number. */
	public static int getSliceNumber(Roi roi, String label) {
		int slice = roi!=null?roi.getPosition():-1;
		if (slice==0)
			slice=-1;
		if (slice==-1)
			slice = getSliceNumber(label);
		return slice;
	}

	
	void macro2(ImagePlus imp, ImageProcessor ip) {
		IJ.resetEscape();
		double scale = 1, m = 1.2;
		for (int i=0; i<20; i++) {
			ip.reset();
			scale *= m;
			ip.scale(scale, scale);
			imp.updateAndDraw();
			if (IJ.escapePressed()) return;
			IJ.wait(10);
		}
		for (int i=0; i <20; i++) {
			ip.reset();
			scale /= m;
			ip.scale(scale, scale);
			imp.updateAndDraw();
			if (IJ.escapePressed()) return;
			IJ.wait(10);
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
	int straightNumber = 1;
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
			//System.out.println("same branch merging");
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
		if(position==1 || position==3 || position==7 || position==9)
			diagonalNumber++;
		else {
			straightNumber++;
		}
		branchPixels.add(p);
	}
	
	
	public void addPixelToExtension(int[] p, int position)
	{		
		if(position==1 || position==3 || position==7 || position==9)
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



