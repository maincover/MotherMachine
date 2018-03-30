package panel;
import java.awt.Frame;
import java.awt.List;
import java.util.Hashtable;

import javax.swing.JFrame;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

public class Ins_Test_ProcessFrame implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		Frame frame = WindowManager.getFrame("ROI Manager");					
		if (frame==null || !(frame instanceof RoiManager))
			{return;}
		RoiManager roiManager = (RoiManager)frame;			
		List list = roiManager.getList();
		Hashtable<String, Roi> rois = roiManager.getROIs();			
		if(list.getItemCount() != 2)
		{
			IJ.showMessage("Need 2 ROI in the manager");
			return;
		}
		Ins_processFrame processFrame = new Ins_processFrame();
		processFrame.setOpaque(true);	
		JFrame frame2 = new JFrame("Bacterial analyzer");
		frame2.setContentPane(processFrame);
		frame2.pack();
		frame2.setResizable(false);
		frame2.setVisible(true);
	}
}

