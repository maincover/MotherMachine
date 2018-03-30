package watershed2D;

import ij.IJ;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * this class implements the result window
 * where it is possible to see the different result images
 */

public class ResultsWindow extends JPanel
						   implements ActionListener{

	private static final long serialVersionUID = 1L;
	WatershedData mWData = null;
	private static final int RESULTS = 0;
	private static final int WL = 1;
	private static final int COLORED_REGIONS = 2;
	private static final int BINARY_REGIONS = 3;
	private static final int SOURCE_IMAGE = 4;
	private static final int WATERSHED_IMAGE = 5;
	
	private String[] mImagesList = {"Results Image",
									"Watershed Lines",
									"Colored Regions", 
									"Binary Regions Image",
									"Source Image",
									"Watershed source Image"
									};
	
	
	private JComboBox mImagesComboBox = new JComboBox(mImagesList);
	private GridBagLayout mLayout = new GridBagLayout();
	private GridBagConstraints mConstraint = new GridBagConstraints();
	private JButton mBtnSHOW = null;
	
	/**
	 * constructor -> build the result window
	 * @param wData watershed data
	 */
	public ResultsWindow(WatershedData wData){
		
		// data of results
		mWData = wData;
        
        // static text
        JLabel text1 = new JLabel("Result Images :");
       
        //Create the combo box
        mImagesComboBox.setSelectedIndex(0);
		

        // Create the button OK
        mBtnSHOW = new JButton("show");
        mBtnSHOW.setHorizontalAlignment(JLabel.CENTER);
        mBtnSHOW.addActionListener(this);
        

        JPanel pnMain = new JPanel();
		pnMain.setLayout(mLayout);
        addComponent(pnMain, 0, 0, 1, 1, 4, text1);
		addComponent(pnMain, 1, 0, 1, 2, 4, mImagesComboBox);
		addComponent(pnMain, 1, 1, 1, 2, 4, mBtnSHOW);

		pnMain.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		add(pnMain);

        
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
	
	
	/** 
     * Listens to the combo box. 
     */
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == mBtnSHOW) {
			int selection = (int)mImagesComboBox.getSelectedIndex();
			
			switch(selection){
				case RESULTS :
					mWData.displayResult(false);
				break;
				
				case WL :
					mWData.displayWL(true);
				break;
				
				case COLORED_REGIONS :
					mWData.displayRegion(true);
				break;
				
				case BINARY_REGIONS :
					mWData.displaySegmentedRegions(false);
				break;
				
				case SOURCE_IMAGE :
					mWData.displaySource();
				break;

				case WATERSHED_IMAGE :
					ImageBuilder.display(mWData.getWatershedImageUsed());
				break;
			}  
		}
	}
}
