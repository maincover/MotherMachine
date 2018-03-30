package panel;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;



/**
 * this class implements the result window
 * where it is possible to see the different result images
 */

public class Ins_processFrame extends JPanel
						   implements ActionListener, PlugInFilter{

	private static final long serialVersionUID = 1L;	

	private GridBagLayout mLayout = new GridBagLayout();
	private GridBagConstraints mConstraint = new GridBagConstraints();
	private JButton mBtnApply = null;
	private JButton mBtnSetPosition = null;
	private Roi roi1 = null;
	private Roi roi2 = null;
	int[] xy_roi1 = new int[2];
	int[] wh_roi1 = new int[2];
	int[] xy_roi2 = new int[2];
	int[] wh_roi2 = new int[2];
	
	int[] xy_roi1_after = new int[2];
	int[] wh_roi1_after = new int[2];
	int[] xy_roi2_after = new int[2];
	int[] wh_roi2_after = new int[2];
	
	List list;
	Hashtable<String, Roi> rois;
	
	
	public Ins_processFrame(){
		
        JLabel text1 = new JLabel("Drag the ROI and Apply :");         
        mBtnApply = new JButton("Apply");
        mBtnApply.setHorizontalAlignment(JLabel.CENTER);
        mBtnApply.addActionListener(this);
        
        mBtnSetPosition = new JButton("Set Position");
        mBtnSetPosition.setHorizontalAlignment(JLabel.CENTER);
        mBtnSetPosition.addActionListener(this);
        
        JPanel pnMain = new JPanel();
		pnMain.setLayout(mLayout);
        addComponent(pnMain, 0, 0, 1, 1, 4, text1);		
		addComponent(pnMain, 1, 0, 1, 2, 4, mBtnSetPosition);
		addComponent(pnMain, 1, 1, 1, 2, 4, mBtnApply);		
		
		pnMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));		
		add(pnMain);
		IJ.showMessage("Class initialized!");
		
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
		mConstraint.fill = GridBagConstraints.HORIZONTAL;
		mLayout.setConstraints(comp, mConstraint);
		pn.add(comp);
	}
	
	
	
	public static void main(String[] args) {
		
		ImagePlus trans = IJ.openImage();
		ImagePlus rfp = IJ.openImage();
		
		if(trans == null || rfp == null)
			return;
		
		rfp.show();
		trans.show();
		IJ.runPlugIn(trans, "ij.plugin.frame.ContrastAdjuster", ""); 
		
		Frame frame = WindowManager.getFrame("ROI Manager");
		if(frame == null)
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");		
		if (frame==null || !(frame instanceof RoiManager))
			{return;}
		frame.setVisible(true);		
		RoiManager roiManager = (RoiManager)frame;			
//		List list = roiManager.getList();
//		Hashtable<String, Roi> rois = roiManager.getROIs();			
//		if(list.getItemCount() != 2)
//		{
//			IJ.showMessage("Need 2 ROI in the manager");
//			return;
//		}
		Ins_processFrame processFrame = new Ins_processFrame();
		processFrame.setOpaque(true);	
		JFrame frame2 = new JFrame("Bacterial analyzer");
		frame2.setContentPane(processFrame);
		frame2.pack();
		frame2.setResizable(false);
		frame2.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mBtnApply) {
			IJ.showMessage("Apply Start!");
			if(roi1 == null || roi2 == null)
			{
				IJ.showMessage("Set Position at first! Then charge the ROIs");
				return;
			}
			roi1 = rois.get(list.getItem(0));
			roi2 = rois.get(list.getItem(1));
			setXYandWH(xy_roi1_after,wh_roi1_after, xy_roi2_after, wh_roi2_after);
			double w_ratio2by1 =(double) wh_roi2[0]/(double)wh_roi1[0];
			double h_ratio2by1 =w_ratio2by1;//(double) wh_roi2[1]/(double)wh_roi1[1];
			double diffx, diffy, diffw, diffh;			
			
			if(wh_roi1[0]!=wh_roi1_after[0] || wh_roi1[1]!=wh_roi1_after[1] || xy_roi1[0]!=xy_roi1_after[0] || xy_roi1[1]!=xy_roi1_after[1])
			{
				IJ.showMessage("roi1 : " +roi1.getName()+ " changed First!");
				diffx = xy_roi1_after[0] - xy_roi1[0];
				diffy = xy_roi1_after[1] - xy_roi1[1];
				diffw = wh_roi1_after[0] - wh_roi1[0];
				//diffh = wh_roi1_after[1] - wh_roi1[1];				
				String name = roi2.getName();				
				System.out.println("-----------------");
				IJ.showMessage("Apply!" + " roi2.x: " + xy_roi2[0] + " roi2.y: " + xy_roi2[1] + " roi2.w: "+wh_roi2[0] + " roi2.h: " + wh_roi2[1]);
				roi2 = new Roi((int)(xy_roi2[0] + diffx*w_ratio2by1),(int)( xy_roi2[1] + diffy*h_ratio2by1),(int)( wh_roi2[0] + diffw*w_ratio2by1),(int)(wh_roi1_after[1]*h_ratio2by1)); //(int)( wh_roi2[1] + diffh*h_ratio2by1)
				IJ.showMessage("Apply!" + " roi2.x: " + roi2.getBounds().x + " roi2.y: " + roi2.getBounds().y + " roi1.w: "+roi2.getBounds().width + " roi2.h: " + roi2.getBounds().height);
				System.out.println("-----------------");
				list.remove(1);
				list.add(name,1);
				roi2.setName(name);
				rois.put(name, roi2);
				System.out.println("Roi2 :" + name + " changed After!");
			}
			
			else if(wh_roi2[0]!=wh_roi2_after[0] || wh_roi2[1]!=wh_roi2_after[1] || xy_roi2[0]!=xy_roi2_after[0] || xy_roi2[1]!=xy_roi2_after[1]) 
			{	
				IJ.showMessage("roi2 label : " + roi2.getName() + " changed!");				
				diffx = xy_roi2_after[0] - xy_roi2[0];
				diffy = xy_roi2_after[1] - xy_roi2[1];
				diffw = wh_roi2_after[0] - wh_roi2[0];
				diffh = wh_roi2_after[1] - wh_roi2[1];
				String name = roi1.getName();				
				roi1 = new Roi((int)(xy_roi1[0] + diffx*w_ratio2by1),(int)( xy_roi1[1] + diffy*h_ratio2by1),(int)( wh_roi1[0] + diffw*w_ratio2by1),(int)( wh_roi1[1] + diffh*h_ratio2by1));				
				list.remove(0);
				list.add(name,0);
				roi1.setName(name);
				rois.put(name, roi1);
				System.out.println("Roi1 :" + name + " changed!");								
			}
			
			this.roi1 = rois.get(list.getItem(0));
			this.roi2 = rois.get(list.getItem(1));			
		}
		
		
		
		if (e.getSource() == mBtnSetPosition) {			
			Frame frame = WindowManager.getFrame("ROI Manager");
			if(frame == null)
				IJ.run("ROI Manager...");
			frame = WindowManager.getFrame("ROI Manager");		
			if (frame==null || !(frame instanceof RoiManager))
				{return;}
			frame.setVisible(true);		
			RoiManager roiManager = (RoiManager)frame;	
			list = roiManager.getList();
			rois = roiManager.getROIs();
			if(list.getItemCount() != 2)
			{
				IJ.showMessage("No Roi charged in ROI Manager! Please charge two Roi and launch!");
				return;
			}			
			this.roi1 = rois.get(list.getItem(0));
			this.roi2 = rois.get(list.getItem(1));
			setXYandWH(xy_roi1,wh_roi1,xy_roi2,wh_roi2);
			setXYandWH(xy_roi1_after,wh_roi1_after, xy_roi2_after, wh_roi2_after);		
			
			IJ.showMessage("Position Set!" + " roi1.x: " + xy_roi1[0] + " roi1.y: " + xy_roi1[1] + " roi1.w: "+wh_roi1[0] + " roi1.h: " + wh_roi1[1]);
			IJ.showMessage("Position Set!" + " roi2.x: " + xy_roi2[0] + " roi2.y: " + xy_roi2[1] + " roi1.w: "+wh_roi2[0] + " roi1.h: " + wh_roi2[1]);
			System.out.println("=================================================");
		}
	}
	
	private void setXYandWH(int[] xy_roi1, int[] wh_roi1, int[] xy_roi2, int[] wh_roi2)
	{
		xy_roi1[0] = roi1.getBounds().x;
		xy_roi1[1] = roi1.getBounds().y;
		wh_roi1[0] = roi1.getBounds().width;
		wh_roi1[1] = roi1.getBounds().height;			
					
		xy_roi2[0] = roi2.getBounds().x;
		xy_roi2[1] = roi2.getBounds().y;
		wh_roi2[0] = roi2.getBounds().width;
		wh_roi2[1] = roi2.getBounds().height;			

	}





	@Override
	public int setup(String arg, ImagePlus imp) {		
		return DOES_ALL;
	}



	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
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
		//Ins_processFrame processFrame = new Ins_processFrame(rois.get(list.getItem(0)), rois.get(list.getItem(1)));		
		//processFrame.setVisible(true);
	}	
}

























