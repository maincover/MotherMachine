package panel;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.util.Java2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

public class Ins_Watershed_MergingFrame extends JPanel implements AdjustmentListener,ActionListener{
    JPanel panel;
    JScrollBar tolerenceSlider;
    GridBagConstraints c;
    GridBagLayout gridbag;
 	int maxSliderValue = 60;
	int minSliderValue=-60;
	private GridBagLayout mLayout = new GridBagLayout();
	private GridBagConstraints mConstraint = new GridBagConstraints();
	JCheckBox previewCheckBox;
	JLabel messageArea;
	JLabel lable_tolerence;
	JTextField oHeight;
	
	ImagePlus impBinaryOriginal;//original binary image
	ImagePlus impOriginal;//for computing the edge image
	ImagePlus[] img_WL;
	ImagePlus img_Cavanas;
	ImagePlus[] imp_beforeWL;
	Vector<int[]>[] wl;
	
	int[][] hist;
	float[][] x;		
	float[][] y;
	float[][] width;
	float[][] height;
	int[][] mean2;
	int[] threshold;
	boolean previewing;
	float tolerance = 0;
	Boolean stack = false;
	int stackSize = 1;
	
	
	public Ins_Watershed_MergingFrame(Vector<int[]>[] wLine) {
		this.wl = wLine;
	}

	public int setup(String arg, ImagePlus imp) {   	    		    	
		if(imp == null)
		{
			impBinaryOriginal = IJ.openImage();
		}else {
			impBinaryOriginal = imp.duplicate();
		}
		
		
		if(impBinaryOriginal == null)
			return 0;
		
		
		if(impBinaryOriginal.getStackSize() > 1)
		{
			stackSize = impBinaryOriginal.getStackSize();
			stack = true;
		}
		
		//impBinaryOriginal.show();			
		//ImagePlus img_Rearranged = currentImp.duplicate();	
		impBinaryOriginal.setTitle("Binary");
		int type=impBinaryOriginal.getType();
		if(type != ImagePlus.GRAY8)
		{
			IJ.error("Image needs to be binaried");
			return 0;
		}		
		impBinaryOriginal.duplicate().show();
		IJ.showMessage("Select original image!");
		this.impOriginal = IJ.openImage();
		if(impOriginal == null || impOriginal.getWidth() != impBinaryOriginal.getWidth() || impOriginal.getHeight() != impBinaryOriginal.getHeight())
		{
			IJ.showMessage("Image null or size not correct!");
			return 0;
		}
		
		if(impOriginal.getImageStackSize()!=impBinaryOriginal.getImageStackSize())
		{
			IJ.showMessage("Wrong stack size!");
			return 0;
		}
		
		hist = new int[stackSize][256];
		img_WL = new ImagePlus[stackSize];
		imp_beforeWL= new ImagePlus[stackSize];
		threshold = new int[stackSize];
		x = new float[stackSize][];
		y = new float[stackSize][];
		width = new float[stackSize][];
		height = new float[stackSize][];
		mean2 = new int[stackSize][];
		
		int tolerence_Number = 0;
		for(int s=1; s<=stackSize;s++)
		{
			ByteProcessor ip = (ByteProcessor)impBinaryOriginal.getImageStack().getProcessor(s);
			//ip.invertLut();
			//ip.invert();		
			imp_beforeWL[s-1] = new ImagePlus("", ip.duplicate());
			EDM edm = new EDM();
			edm.setup("watershed", impBinaryOriginal);
			edm.run(ip);		
			img_WL[s-1] = new ImagePlus("", ip.duplicate());
			//impBinaryOriginal.duplicate().show();

			img_WL[s-1].getProcessor().copyBits(imp_beforeWL[s-1].getProcessor(), 0, 0, Blitter.DIFFERENCE);
			img_WL[s-1].setTitle("Imp WL");
			img_WL[s-1].getProcessor().invert();
			//img_WL[s-1].show();			

			//imp_beforeWL[s-1].getProcessor().invert();
			//imp_beforeWL[s-1].getProcessor().invertLut();

			GaussianBlur gBlur = new GaussianBlur();
			gBlur.blurGaussian(impOriginal.getImageStack().getProcessor(s), 1.2, 1.2, 0.01);
			ImageProcessor ip1 = impOriginal.getImageStack().getProcessor(s).duplicate();
			ip1.convolve(new float[]{-1,-2,-1,1,2,1,0,0,0}, 3, 3);
			ImageProcessor ip2 = impOriginal.getImageStack().getProcessor(s).duplicate();
			ip2.convolve(new float[]{0,0,0,1,2,1,-1,-2,-1}, 3, 3);			
			ip1.copyBits(ip2, 0, 0, Blitter.ADD);			
			impOriginal.getImageStack().getProcessor(s).copyBits(ip1, 0, 0,Blitter.COPY);
			//impOriginal.show();

			ResultsTable rt = new ResultsTable();
			rt.reset();
			int options = 0; // set all PA options false
			int measurements = Measurements.RECT|Measurements.CENTROID;
			ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(options, measurements, rt, 0, 2000);
			img_WL[s-1].getProcessor().invert();
			if(!pAnalyzer.analyze(img_WL[s-1], (ByteProcessor)img_WL[s-1].getProcessor())){				
				// the particle analyzer should produce a dialog on error, so don't make another one
				return 0;
			}
			x[s-1] = rt.getColumn(ResultsTable.ROI_X);		
			y[s-1] = rt.getColumn(ResultsTable.ROI_Y);
			width[s-1] = rt.getColumn(ResultsTable.ROI_WIDTH);
			height[s-1] = rt.getColumn(ResultsTable.ROI_HEIGHT);
			float[] centroid_x = rt.getColumn(ResultsTable.X_CENTROID);
			float[] centroid_y = rt.getColumn(ResultsTable.Y_CENTROID);			
			
			if(x[s-1]==null)
				continue;
			mean2[s-1] = new int[x[s-1].length];			
			tolerence_Number = tolerence_Number+1;
			float[] mean = new float[x[s-1].length];
			//mean2 = new int[stackSize][x[s-1].length];
			
			for(int j= 0; j< centroid_x.length;j++)
			{
				int numInside = 0;
				for(int shift = (int)(centroid_x[j] - 6); shift <= centroid_x[j] + 6; shift ++)
				{
					for(int shift_y = (int) (centroid_y[j] - 2); shift_y <= centroid_y[j] + 2; shift_y ++)
					{
						if(impOriginal.getImageStack().getProcessor(s).getPixel(shift, shift_y) == 255)
							mean[j] = mean[j] + 0;
						else {
							mean[j] = mean[j] + impOriginal.getImageStack().getProcessor(s).getPixel(shift, shift_y);
							numInside++;
						}
					}
				}
				mean[j] = numInside>0?mean[j]/numInside:0;
				//System.out.println("mean["+j+"]: "+mean[j]);
			}


			float max = Float.MIN_VALUE;
			float min = Float.MAX_VALUE;

			for(int i=0; i<x[s-1].length; i++)
			{						
				if(mean[i] > max)
					max = mean[i];			
				if(mean[i] < min)
					min = mean[i];
			}
			for(int i=0; i<x[s-1].length; i++)
			{	
				mean2[s-1][i] = (int)(((mean[i]-min)/(max - min)) * 254);
			}

			for (int i = 0; i < mean2[s-1].length; i++) {
				hist[s-1][mean2[s-1][i]] = hist[s-1][mean2[s-1][i]] + 1;
			}
		}
		
		AutoThresholder autoThresholder = new AutoThresholder();
		
		for(int s=1; s<=stackSize;s++)
		{
			threshold[s-1]  = autoThresholder.getThreshold(Method.Otsu, hist[s-1]);
//			if(threshold != 254)
//				tolerance += threshold; 
//			System.out.println("Threshold : "+s+" :" + threshold);
		}
		//tolerance = tolerance/tolerence_Number;
		//System.out.println("tolerance : " + tolerance);
		
		
		img_Cavanas = IJ.createImage("Imp WLintensity", "8-bit",impBinaryOriginal.getWidth(),impBinaryOriginal.getHeight(), stackSize);
		panel = new JPanel();
		Java2.setSystemLookAndFeel();	
		panel.setLayout(mLayout);
		tolerenceSlider = new JScrollBar(Scrollbar.HORIZONTAL, (int)0, 1, minSliderValue, maxSliderValue);
		tolerenceSlider.setPreferredSize(new  Dimension(100, 20));
		tolerenceSlider.addAdjustmentListener(this);
		
		
		messageArea= new JLabel("? line");
		lable_tolerence = new JLabel(String.valueOf(tolerenceSlider.getValue()));
		previewCheckBox = new JCheckBox("Preview");
		previewCheckBox.addActionListener(this);
		
		oHeight= new JTextField(String.valueOf(impBinaryOriginal.getHeight()/10),4);
		oHeight.setEnabled(true);

		addComponent(panel, 0, 0, 1, 1, 4, new Label("Tolerence"));
		addComponent(panel, 0, 1, 1, 1, 4, tolerenceSlider);		
		addComponent(panel, 1, 0, 1, 1, 4, previewCheckBox);
		addComponent(panel, 1, 1, 1, 1, 4, lable_tolerence);
		addComponent(panel, 2, 0, 1, 1, 4, new JLabel("Height"));
		addComponent(panel, 2, 1, 1, 1, 4, oHeight);
		addComponent(panel, 3, 0, 1, 1, 4, messageArea);
		
		
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));	
		add(panel);
		return 0;
	}
	
	
	
	public void watershedImp(int tolerence)
	{		
		if(!img_Cavanas.isVisible())
		{
			img_Cavanas = IJ.createImage("Imp WLintensity", "8-bit", impBinaryOriginal.getWidth(), impBinaryOriginal.getHeight(), stackSize);
			img_Cavanas.show();
		}else {
			//img_Cavanas = IJ.createImage("Imp WLintensity", "8-bit", impBinaryOriginal.getWidth(), impBinaryOriginal.getHeight(), stackSize);
			for(int s=1; s<=stackSize;s++)
			{				
				byte[] pixels = (byte[])img_Cavanas.getImageStack().getProcessor(s).getPixels();
				for(int i=0;i<pixels.length;i++)
					pixels[i] = (byte)255;
			}
		}
		
		int numWl = 0;
		for(int s=1; s<=stackSize;s++)
		{
			//img_WL[s-1].show();
			//int threshold_otsu = threshold;
			//Vector<int[]>[] wl = new Vector[mean2[s-1].length];
			if(mean2[s-1]==null)
				continue;
			ImageProcessor ip_WL = img_WL[s-1].getProcessor();
			
//			for(int i=0;i<x[s-1].length;i++)
//			{
//				if(mean2[s-1][i] >= threshold[s-1]+tolerence)
//					img_Cavanas.getImageStack().getProcessor(s).set((int)x[s-1][i], (int)y[s-1][i], 0);			
//			
//			}
			
			
			for(int px = 0; px < ip_WL.getWidth(); px ++)
				for(int py =Integer.valueOf(oHeight.getText()); py < ip_WL.getHeight(); py ++)//
				{
					int v = ip_WL.get(px, py);
					if(v == 255)
					{
						for(int j= 0; j< x[s-1].length; j++)
						{						
							if(px >= x[s-1][j] && px <= x[s-1][j]+width[s-1][j] && py >= y[s-1][j] && py <= y[s-1][j] + height[s-1][j])
							{
								if(mean2[s-1][j] >= threshold[s-1]+tolerence)
								{							
									img_Cavanas.getImageStack().getProcessor(s).set(px, py, 0);									
									numWl ++;
									break;
								}				
							}
						}
					}
				}
			//System.out.println("threshold "+s+" : " + (threshold[s-1]));
			img_Cavanas.getImageStack().getProcessor(s).copyBits(imp_beforeWL[s-1].getProcessor(), 0, 0, Blitter.MIN);		 
			img_Cavanas.getImageStack().getProcessor(s).invert();
//			img_Cavanas.getImageStack().getProcessor(s).invertLut();
//			img_Cavanas.getProcessor().invert();
			//this.wl = wl;s
		}
		img_Cavanas.setTitle("Watershed binary");	
		img_Cavanas.updateAndDraw();
		imp_beforeWL[0].show();
		messageArea.setText(numWl + " wlines!");		
	}
	/**
	 * Add a component in a panel
	 */
	private void addComponent(JPanel pn, int row, final int col, int width, final int height, int space, Component comp) {
		mConstraint.gridx = col;
		mConstraint.gridy = row;
		mConstraint.gridwidth = width;
		mConstraint.gridheight = height;
		mConstraint.anchor = GridBagConstraints.NORTHWEST;
		mConstraint.insets = new Insets(space, space, space, space);
		mConstraint.weightx = IJ.isMacintosh()?90:100;
		mConstraint.fill = GridBagConstraints.HORIZONTAL;
		mLayout.setConstraints(comp, mConstraint);
		pn.add(comp);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == previewCheckBox)
		{
			if(previewCheckBox.isSelected())
			{
				previewing = true;							
				watershedImp((int)tolerance);
			}
			else {
				previewing = false;
			}
		}
	}
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		// TODO Auto-generated method stub
		Object source = e.getSource();
		if (source==tolerenceSlider)			
			tolerance = tolerenceSlider.getValue();
		else {
			return;
		}			
		lable_tolerence.setText(String.valueOf(tolerance));	
		if(previewing == true)
			watershedImp((int)tolerance);
	}
}
