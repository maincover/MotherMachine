package watershed2D;

import ij.IJ;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;

public class HelpFrame extends JFrame {

	private GridBagLayout 		mLayout			= new GridBagLayout();
	private GridBagConstraints 	mConstraint		= new GridBagConstraints();
	
	
	
	public HelpFrame()
	{
		JPanel help = new JPanel();
		JTextField title1 = new JTextField();
		title1.setFont(new Font(null, Font.BOLD, 14));
		title1.setText("General description");
		help.setLayout(mLayout);
		addComponent(help, 0, 0, 1, 1, 4, title1);
		
        JPanel pnMain = new JPanel();
		pnMain.setLayout(mLayout);
		addComponent(pnMain, 0, 0, 2, 1, 4, help);

		pnMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(pnMain);
	}
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
}
