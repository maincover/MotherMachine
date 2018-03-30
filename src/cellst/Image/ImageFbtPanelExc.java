package cellst.Image;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.util.Java2;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ImageFbtPanelExc extends JPanel implements ActionListener, PlugInFilter{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static GridBagLayout mLayout = new GridBagLayout();
	static GridBagConstraints mConstraint = new GridBagConstraints();	
	static JPanel panel;	
	static JButton bMacros = new JButton("Macros");

	public ImageFbtPanelExc() {
		panel = new JPanel();
		panel.setLayout(mLayout);
		bMacros.addActionListener(this);
		addComponent(panel, 0, 0, 1, 1, 3, bMacros);
		panel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));	
		add(panel);

	}
	
	public static void main(String[] args)
	{
		ImageJ ij = new ImageJ();
		Java2.setSystemLookAndFeel();
		ImageFbtPanelExc iFbtPanel = new ImageFbtPanelExc();
		iFbtPanel.setOpaque(true);	
		JFrame frame = new JFrame("Macros");
		frame.setContentPane(iFbtPanel);
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);	
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
		if(label.equals("Macros"))
		{					
			ImageFbt.run();
		}
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		return 0;
	}

	@Override
	public void run(ImageProcessor ip) {
		Java2.setSystemLookAndFeel();
		ImageFbtPanelExc iFbtPanel = new ImageFbtPanelExc();
		iFbtPanel.setOpaque(true);	
		JFrame frame = new JFrame("Macros");
		frame.setContentPane(iFbtPanel);
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);	
	}

}
