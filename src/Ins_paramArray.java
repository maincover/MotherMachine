import houghTransform.LinearHT;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.io.OpenDialog;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import Stabilizer.Ins_param;
import Threshold.Auto_Threshold_v;



public class Ins_paramArray extends JPanel implements ActionListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static GridBagLayout mLayout = new GridBagLayout();
	static GridBagConstraints mConstraint = new GridBagConstraints();	
	static JPanel panel;	
	static JButton previousButton = new JButton("Previous");
	static JButton nextButton = new JButton("Next");
	static JButton applyParam = new JButton("ApplyFluo");
	static JButton saveButton = new JButton("Save");
	static JButton saveToFileButton = new JButton("Save to file...");
	static JButton loadFromFileButton = new JButton("Load from file...");
	static JButton autoButton = new JButton("Autoinitialization");
	static int currentIndex;	
	static ImagePlus[] impRef;	
	static Ins_seg_panel insPanel;	
	static ImagePlus impShow;
	static ImageProcessor ipRef;
	
	Ins_param[] paramsArray;
	private String path;
	
	String[] title;
	
	public Ins_paramArray(ImagePlus[] impRef, String path, Ins_seg_panel insPanel){
		currentIndex = 0;
		Ins_paramArray.impRef = impRef;
		Ins_paramArray.insPanel = insPanel;
		this.path = path;
		panel = new JPanel();   			
		previousButton.addActionListener(this);
		nextButton.addActionListener(this);
		applyParam.addActionListener(this);
		saveButton.addActionListener(this);
		saveToFileButton.addActionListener(this);
		loadFromFileButton.addActionListener(this);
		autoButton.addActionListener(this);
		panel.setLayout(mLayout);
		addComponent(panel, 0, 0, 1, 1, 3, previousButton);
		addComponent(panel, 0, 1, 1, 1, 3, nextButton);
		addComponent(panel, 0, 2, 1, 1, 3, saveButton);
		addComponent(panel, 0, 3, 1, 1, 3, autoButton);
		addComponent(panel, 1, 0, 1, 1, 3, saveToFileButton);
		addComponent(panel, 1, 1, 1, 1, 3, applyParam);
		addComponent(panel, 1, 2, 1, 1, 3, loadFromFileButton);
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));	
		add(panel);
		paramsArray = new Ins_param[impRef.length];
		title = new String[impRef.length];
		for(int i=0; i< paramsArray.length; i++)
		{			
			//impRef[i].getProcessor().flipVertical();
			title[i] = impRef[i].getTitle();			
			try {
				title[i] = title[i].substring(0, title[i].lastIndexOf("."));				
				title[i] = title[i].substring(title[i].lastIndexOf("xy"), title[i].lastIndexOf('t'));
				System.out.println(title[i]);
				//IJ.showMessage(title[i]);	
			} catch (Exception e) {
				try {									
					title[i] = title[i].substring(title[i].lastIndexOf('s'), title[i].lastIndexOf("_t"));	
					System.out.println(title[i]);
					//IJ.showMessage(title[i]);
				} catch (Exception e2) {
					title[i] = title[i].substring(0, title[i].lastIndexOf("_t"));;
					IJ.showMessage("wrong image name !"+ " title : " + title[i]);
				}				
			}			
			//paramsArray[i].pName = title[i];
		}
		for(int i=0; i<paramsArray.length;i++)
		{
			paramsArray[i] = new Ins_param();
			paramsArray[i].getPositionName();
		}
		try {
			impShow = new ImagePlus();
			impShow.setProcessor(impRef[currentIndex].getProcessor().duplicate());
			impShow.setTitle(impRef[currentIndex].getTitle());
			impShow.show();
		} catch (Exception e) {
		}
	}
	
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
	@Override
	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if(e.getSource() == autoButton)
		{
			ipRef = impRef[currentIndex].getProcessor().duplicate();
			Ins_param param = getInsPanelParams();
			if(!param.getSealedOffAtTop())
				ipRef.flipVertical();
			ImageProcessor ipRefNew = ipRef.duplicate();
			Ins_param pAuto = automatisation(ipRefNew);
			param.setAngle(pAuto.getAngle());
			param.setInterChannelLength(pAuto.getInterChannelLength());
			param.setStartX(pAuto.getStartX());
			param.setStartY(pAuto.getStartY());
			param.setChannelNumber(pAuto.getChannelNumber());			
			setInsPanelParams(param);
			drawGridLines(param);
		}

		if(label.equals("Previous"))
		{					
			if(currentIndex-1>=0)
			{
				impShow.close();
				ipRef = impRef[--currentIndex].getProcessor().duplicate();				
				impShow.setProcessor(ipRef);				
				impShow.setTitle(impRef[currentIndex].getTitle());
				impShow.show();
				insPanel.ip_ref = impRef[currentIndex].getProcessor();
				insPanel.imp_ref = null;
				if(paramsArray[currentIndex].savedOnce())
				{
					Ins_param param = getParams(currentIndex);
					setInsPanelParams(param);
				}else if (currentIndex-1 >=0 && paramsArray[currentIndex-1].savedOnce()) {
					Ins_param param = getParams(currentIndex-1);
					setInsPanelParams(param);
				}else if (currentIndex + 1 < paramsArray.length && paramsArray[currentIndex+1].savedOnce()) {
					Ins_param param = getParams(currentIndex+1);
					setInsPanelParams(param);
				}
				drawGridLines(getInsPanelParams());
			}			
			System.out.println("currentIndex : " + currentIndex);
		}
		
		if(label.equals("Next"))
		{
			if(currentIndex+1 < impRef.length)
			{
				impShow.close();								
				ipRef = impRef[++currentIndex].getProcessor().duplicate();
				impShow.setProcessor(ipRef);
				impShow.setTitle(impRef[currentIndex].getTitle());
				impShow.show();
				insPanel.ip_ref = impRef[currentIndex].getProcessor();
				insPanel.imp_ref = null;							
				if(paramsArray[currentIndex].savedOnce())
				{
					Ins_param param = getParams(currentIndex);
					setInsPanelParams(param);
				}else if (currentIndex -1 >= 0 && paramsArray[currentIndex-1].savedOnce()) {
					Ins_param param = getParams(currentIndex-1);
					setInsPanelParams(param);
				}else if (currentIndex+1 < impRef.length && paramsArray[currentIndex+1].savedOnce()) {
					Ins_param param = getParams(currentIndex+1);
					setInsPanelParams(param);
				}				
				drawGridLines(getInsPanelParams());
			}			
			System.out.println("currentIndex : " + currentIndex);
		}
		
		if(label.equals("Save"))
		{			
			ipRef = impRef[currentIndex].getProcessor().duplicate();
			Ins_param param = getInsPanelParams();
			if(!param.getSealedOffAtTop())
				ipRef.flipVertical();
			if(paramsArray[currentIndex] == null)				
				paramsArray[currentIndex] = new Ins_param();
			paramsArray[currentIndex].setAngle((double)insPanel.oChRotSlider.getValue()/10);			
			paramsArray[currentIndex].setHeight_align(Double.valueOf(insPanel.oChHeight.getText()));
			paramsArray[currentIndex].setInterChannelLength(Double.valueOf(insPanel.oChInterLength.getText()));
			paramsArray[currentIndex].setChannelNumber(Double.valueOf(insPanel.oChNum.getText()));
			paramsArray[currentIndex].compute_channel_prefix_pos(); // compute channel prefix position
			paramsArray[currentIndex].setStartX(Double.valueOf(insPanel.oChStartX.getText()));
			paramsArray[currentIndex].setStartY(Double.valueOf(insPanel.oChStartY.getText()));
			paramsArray[currentIndex].setRoi_width(Double.valueOf(insPanel.oRoiWidth.getText()));
			paramsArray[currentIndex].setBlank_width(Ins_seg_panel.blankwidth);
			paramsArray[currentIndex].setSavedOnce();
			paramsArray[currentIndex].sealedOffAtTop(insPanel.mChDirection.isSelected());		
			paramsArray[currentIndex].setPositionName(title[currentIndex]);
			drawGridLines(getParams(currentIndex));
			System.out.println(paramsArray[currentIndex]);
			IJ.log("save parameters into " + impRef[currentIndex].getTitle());
		}
		
		if(label.equals("Save to file..."))
		{		
			String defaultDir = OpenDialog.getDefaultDirectory();
			String title = "Save Params...";
			String defaultName = setExtension("params", ".out");
			ImageJ ij = IJ.getInstance();
			Frame parent = ij!=null?ij:new Frame();
			FileDialog fd = new FileDialog(parent, title, FileDialog.SAVE);
			if (defaultName!=null)
				fd.setFile(defaultName);
			if (defaultDir!=null)
				fd.setDirectory(defaultDir);
			fd.setVisible(true);
			fd.dispose();
			System.out.println("save file to : " + fd.getDirectory()+fd.getFile());
			try {
				FileOutputStream fos = new FileOutputStream(fd.getDirectory()+fd.getFile());
				ObjectOutputStream oos = new ObjectOutputStream(fos); 				
				for(int i=0; i<paramsArray.length; i++)
				{
					oos.writeObject(paramsArray[i]);
				}
				oos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if(label.equals("Load from file..."))
		{
			String title = "Load Params...";
			ImageJ ij = IJ.getInstance();
			Frame parent = ij!=null?ij:new Frame();
			FileDialog fd = new FileDialog(parent, title, FileDialog.LOAD);
			fd.setVisible(true);
			fd.dispose();
			System.out.println("load file from : " + fd.getDirectory()+fd.getFile());
			try {
				FileInputStream fis = new FileInputStream(fd.getDirectory()+fd.getFile());
				ObjectInputStream ois = new ObjectInputStream(fis); 
				
				if(paramsArray.length < 1)
					paramsArray = new Ins_param[50];
				int j = 0;
				for(int i=0; i<paramsArray.length; i++)
				{
					try {
						paramsArray[i] = (Ins_param) ois.readObject();
						System.out.println("load : " + paramsArray[i].toString());
						j = j + 1;
					} catch (Exception e2) {
						continue;
					}
				}
				Ins_param[] tmp = new Ins_param[j];
				System.arraycopy(paramsArray, 0, tmp, 0, j);
				paramsArray = tmp;
				ois.close();
				setInsPanelParams(getParams(0));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if(label.equals("ApplyFluo"))
		{
			Ins_param p0 = paramsArray[0];
			if(p0==null)
			{
				System.out.println("null parameter");
				return;
			}
			
			File fDirectory = getFolderPath();
			if(fDirectory == null)
				return;
			System.out.println("load fluo image at : " + fDirectory.getPath());
			String folderName = fDirectory.getName();
						
			for(Ins_param p : paramsArray)
			{
				File[] fFluo = listFileinFolder(p.getPositionName(), fDirectory.getPath());
				if(fFluo.length < 1)
				{
					   GenericDialog gd = new GenericDialog("Change Position name");
					   gd.addStringField("Position name", p.getPositionName());
					   gd.showDialog();
					      if (gd.wasCanceled()) return;
					   String posName = gd.getNextString();
					   fFluo = listFileinFolder(posName, fDirectory.getPath());
					   if(fFluo.length < 1)
					   {
						   IJ.showMessage("Position not found");
						   return;
					   }else {
						   p.setPositionName(posName);
					   }
				}
				ImagePlus impFluo = loadImpFromFileList(fFluo, folderName); 
				if(p.ready())
				{
					File rFile = new File(fDirectory.getPath()+File.separator+p.getPositionName());
					rFile.mkdirs();					
					ImagePlus impRFP = Ins_seg_panel.calibrateAndSegmentation(impFluo,p);					
					impRFP.show();
					IJ.save(impRFP, rFile.getPath()+File.separator+impRFP.getTitle()+".tif");
				}
			}			
		}
	}
	
	private ImagePlus loadImpFromFileList(File[] fFluo, String name){
		ImagePlus imp = IJ.openImage(fFluo[0].getPath());
		ImagePlus impFluo = IJ.createImage(name, imp.getWidth(), imp.getHeight(), fFluo.length, imp.getBitDepth());
		for(int i=0;i<fFluo.length;i++)
		{
			try {
				imp = IJ.openImage(fFluo[i].getPath());
				impFluo.getImageStack().getProcessor(i+1).insert(imp.getProcessor(), 0, 0);
			} catch (Exception e) {
			}
		}
		return impFluo;
	}

	public File[] listFileinFolder(final String regle, String path) {	    
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
	
	public File getFolderPath()
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
	    	return chooser.getSelectedFile();
	    }else {
	    	return null;
	    }
	}
	
	public static String setExtension(String name, String extension) {
		if (name==null || extension==null || extension.length()==0)
			return name;
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex>=0 && (name.length()-dotIndex)<=5) {
			if (dotIndex+1<name.length() && Character.isDigit(name.charAt(dotIndex+1)))
				name += extension;
			else
				name = name.substring(0, dotIndex) + extension;
		} else if (!name.endsWith(extension))
			name += extension;
		return name;
	}
	
	private Ins_param getParams(int i) {
		return paramsArray[i];
	}	
	
	private Ins_param getInsPanelParams()
	{
		Ins_param param = new Ins_param();
		param.setAngle((double)insPanel.oChRotSlider.getValue()/10);			
		param.setHeight_align(Double.valueOf(insPanel.oChHeight.getText()));
		param.setInterChannelLength(Double.valueOf(insPanel.oChInterLength.getText()));
		param.setChannelNumber(Double.valueOf(insPanel.oChNum.getText()));
		//param.compute_channel_prefix_pos(); // compute channel prefix position
		param.setStartX(Double.valueOf(insPanel.oChStartX.getText()));
		param.setStartY(Double.valueOf(insPanel.oChStartY.getText()));
		param.setRoi_width(Double.valueOf(insPanel.oRoiWidth.getText()));
		param.setBlank_width(Ins_seg_panel.blankwidth);
		param.setSavedOnce();
		param.sealedOffAtTop(insPanel.mChDirection.isSelected());
		return param;
	}

	private void setInsPanelParams(Ins_param param)
	{
		insPanel.oChRotSlider.setValue((int)(param.getAngle()*10));			
		insPanel.oChHeight.setText(String.valueOf(param.getHeight_align()));
		insPanel.oChInterLength.setText(String.valueOf(param.getInterChannelLength()));
		insPanel.oChNum.setText(String.valueOf(param.getChannelNumber()));
		insPanel.oChStartX.setText(String.valueOf(param.getStartX()));
		insPanel.oChStartY.setText(String.valueOf(param.getStartY()));
		insPanel.oRoiWidth.setText(String.valueOf(param.getRoi_width()));
		insPanel.mChDirection.setSelected(param.getSealedOffAtTop());		
	}
	
	
	
	void drawGridLines1(Ins_param param) {		
		impShow.setProcessor(ipRef); // as duplicate, otherwise it wonn't work
		if(!param.getSealedOffAtTop())
			ipRef.flipVertical();		
		impShow.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
		impShow.getProcessor().rotate(param.getAngle());		
		impShow.updateAndDraw();
		int lines = 1;
		ImageCanvas ic = impShow.getCanvas();
		if (ic==null) return;
		if (lines==0) {ic.setOverlay(null); return;}
		GeneralPath path = new GeneralPath();
		float width = impShow.getWidth();
		float height = impShow.getHeight();
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
		path.moveTo(param.getStartX(), 0f);
		path.lineTo(param.getStartX(), height);		
		path.moveTo(0f, param.getStartY());
		path.lineTo(width, param.getStartY());		
		//System.out.println("Angle : "+param.getAngle() + " start x : " + param.getStartX() + " start y : " + param.getStartY());		
		ic.getImage().setOverlay(path, new Color(0, 255, 255), null);
	}
	
	void drawGridLines(Ins_param param) {		
		impShow.setProcessor(ipRef); // as duplicate, otherwise it wonn't work
		ipRef.setInterpolationMethod(ImageProcessor.BILINEAR);
		ipRef.rotate(param.getAngle());		
		impShow.show();
		int lines = param.getChannelNumber() + 1;
		ImageCanvas ic = impShow.getCanvas();
		if (ic==null) return;
		if (lines==0) {ic.setOverlay(null); return;}
		GeneralPath path = new GeneralPath();		
		float xinc = param.getInterChannelLength();
		float yinc = param.getHeight_align();
		float xstart = param.getStartX();
		float ystart = param.getStartY();
		
		for (int i=0; i<lines; i++) {
			path.moveTo(xstart+xinc*i, ystart);
			path.lineTo(xstart+xinc*i, ystart + yinc);
		}
		path.moveTo(xstart, param.getStartY());
		path.lineTo(xstart+(lines-1)*xinc, param.getStartY());		
		ic.getImage().setOverlay(path, new Color(0, 255, 255), null);
	}
	
	private Ins_param automatisation(ImageProcessor ip)
	{
		int startY_ref = 0;
		int height = ip.getHeight();
		int width = ip.getWidth();
		ImagePlus[] eigenImp = (new featureJ.FJ_Structure()).getEigenImg(new ImagePlus("", ip),"8.0","3.0");
		ImagePlus eigenLargestImp = eigenImp[0];
		ShortProcessor spEigenLargest = eigenLargestImp.getProcessor().convertToShortProcessor();
		int level = Auto_Threshold_v.getThreshold("Mean", spEigenLargest);
		spEigenLargest.threshold(level);
		ByteProcessor bpEigenLargest = spEigenLargest.convertToByteProcessor();
		bpEigenLargest.autoThreshold();				
					
		double ratio = 0.2;
		int position_v = startY_ref;
		int position_v0 = startY_ref;
		for(int v=position_v0;v<height; v++)
		{
			int countZero = 0;
			if(v<0 || v>=height)
				continue;						
			for(int u=(int)ratio*width;u<width*(1-ratio);u++)
			{
				if(u<0 || u>= width)
					continue;							
				if(bpEigenLargest.get(u, v) == 255)
					countZero ++;
			}
			if (countZero > 100)
			{
				position_v = v;
				break;
			}
		}
		//System.out.println(" auto position y: " + position_v);
		float[] Gx=new float[]{
				-1,0,1,
				-2,0,2,
				-1,0,1
		};
		
		float[] Gx2=new float[]{
				1,0,-1,
				2,0,-2,
				1,0,-1
		};
		bpEigenLargest.setRoi(0, position_v, width, height - position_v);										
		ImageProcessor ip_y0 = bpEigenLargest.crop();
		ImageProcessor ip_y1 = ip_y0.duplicate();
		ip_y0.convolve(Gx, 3, 3);
		ip_y1.convolve(Gx2, 3, 3);
		
		if(true)
		{
			new ImagePlus("", ip).duplicate().show();
			eigenLargestImp.show();
			new ImagePlus("mean threshold", bpEigenLargest).show();
			new ImagePlus("ip_y0",ip_y0).show();
			new ImagePlus("ip_y1",ip_y1).show();
		}	
		
		double[] p0 = refinePosition(ip_y0);
		double[] p1 = refinePosition(ip_y1);
		if(p0 == null || p1==null)
			return null;
		double r0 = p0[0] > 900 ?(p0[0] - 1800.0)/10.0:(p0[0]/10.0);
		double r1 = p1[0] > 900 ?(p1[0] - 1800.0)/10.0:(p1[0]/10.0);
		
		double cLength0 = p0[1];
		double cLength1 = p1[1];
		
		double n0 = p0[2];
		double n1 = p1[2];

		double x0 = p0[3];
		double x1 = p0[3];
		Ins_param param = new Ins_param();
		param.setStartY(position_v);
		param.setAngle((r0+r1)*0.5);
		param.setInterChannelLength((cLength0+cLength1)*0.5);
		param.setChannelNumber(Math.min(n0, n1));
		param.setStartX(Math.max(x0, x1));
		System.out.println(param);
		return param;
		//return new double[]{position_v, (r0+r1)*0.5, (cLength0+cLength1)*0.5, Math.min(n0, n1), (x0+x1)*0.5};
	}
	
	
	private double[] refinePosition(ImageProcessor ip) {
		LinearHT linearHT = new LinearHT(ip);
		ImagePlus htImp = linearHT.transformArrayToImp();	
		htImp.show();
		MaximumFinder mFinder = new MaximumFinder();
		java.awt.Polygon polygon = mFinder.getMaxima(htImp.getProcessor(),150, false);
		int n = polygon.npoints;
		if(n < 1)
			return null;
		
		int startY = 0;
		int[] x = polygon.xpoints;
		int[] y = polygon.ypoints;
		
		int c0 = 0;
		int c1 = 0;
		
		int[] xc1 = new int[x.length];
		int[] yc1 = new int[x.length];
		int[] xc0 = new int[x.length];
		int[] yc0 = new int[x.length];
		for(int i=0;i<x.length;i++)
		{
			if(x[i]>900)
			{
				xc1[c1] = x[i];
				yc1[c1] = y[i];
				c1++;
			}else {
				xc0[c0] = x[i];
				yc0[c0] = y[i];
				c0++;
			}
		}
		double x_median = 0;
		if (c1 >= c0) {
			n = c1;
			int[] xNew = new int[c1];
			for(int i=0;i<xNew.length;i++)
			{
				xNew[i] = xc1[i];
			}
			x_median = computeMedian(xNew);			
		}else {
			n = c0;
			int[] xNew = new int[c0];
			for(int i=0;i<xNew.length;i++)
			{
				xNew[i] = xc0[i];
			}
			x_median = computeMedian(xNew);
		}
		
		double y_diff_meadian = 0;
		if (c1 >= c0) {
			int[] yNew = new int[c1];
			for(int i=0;i<yNew.length;i++)
			{
				yNew[i] = yc1[i];
			}
			Arrays.sort(yNew);
			startY = (int)(yNew[0]*0.5);
			int[] y_diff = new int[yNew.length-1];
			for(int i=0;i<y_diff.length;i++)
			{
				y_diff[i] = yNew[i+1] - yNew[i];
			}
			y_diff_meadian = computeMedian(y_diff);		
		}else {
			int[] yNew = new int[c0];
			for(int i=0;i<yNew.length;i++)
			{
				yNew[i] = yc0[i];
			}
			Arrays.sort(yNew);
			startY = (int)(yNew[0]*0.5);
			int[] y_diff = new int[yNew.length-1];			
			for(int i=0;i<y_diff.length;i++)
			{
				y_diff[i] = yNew[i+1] - yNew[i];
			}
			y_diff_meadian = computeMedian(y_diff);				
		}
		// n is number of possible channels, 
		return new double[]{x_median, y_diff_meadian,n,startY};
	}
	
	public double computeMedian(int[] array)
	{
		Arrays.sort(array);
		double median;
		if (array.length % 2 == 0)
			median = ((double)array[array.length/2] + (double)array[array.length/2 - 1])/2;
		else
			median = (double)array[array.length/2];
		return median;
	}

	public Ins_param[] getIns_paramsRFPs()
	{
		return paramsArray;
	}

	public String getDirectoryPath() {
		return path;
	}




}
