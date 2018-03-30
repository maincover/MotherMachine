package watershed2D;

import ij.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;




/**
 * this class implements the user interface
 */
public class MainDialog extends JFrame
                          implements ActionListener, WindowListener, ItemListener {

	private static final long serialVersionUID = 1L;
	
	private GridBagLayout 		mLayout			= new GridBagLayout();
	private GridBagConstraints 	mConstraint		= new GridBagConstraints();
	private JButton 			mBtnSTART;
	//private JComboBox 			mConnexityComboBox;
	private JPanel 				mComboBoxPanel;
	private static String 		mTitle = "V1 Watershed Segmentation";
	private static String 		mSTART = "Start";
	private static String 		mSTOP = "Stop";
	private Processing			mProcess;
	private boolean 			mNoImage = false;
	
	private JLabel 				labelMinSizeObject;
	private JLabel 				labelMaxSizeObject;
	
	private JTextField 			mTF;
	private JTextField 			mTF2;
	private JTextField 			mTF3;
	
	private	ButtonGroup 		bg;
	
	private JRadioButton 		mRButton1;
	private JRadioButton 		mRButton2;
	
	private JButton 			mBtnOPTION;
	private JButton 			mBtnHelp;
	private JCheckBox			mChkProgress;
	private JCheckBox			mChkSizeMerging;
	private JCheckBox			mChkSizeExcluding;
	private JComboBox 			mObjectBackgroundComboBox;
	private JComboBox 			mObjectDetailsComboBox;
	private JPanel 				mPnOption;
	private JPanel 				mPanelRadio;
	private JPanel 				mPanelSize;
	private JSlider 			mSlider;
	private JSlider 			mSlider2;
	private static String 		mNotOPTION = "Options >>";
	private static String 		mOPTION = "Options <<";
	
	private boolean[] 			mParamChoice;
	// choice 0
	public static boolean		BRIGHT = true;
	public static boolean		DARK = false;
	// choice 1
	public static boolean		PEAKS = true;
	public static boolean		UNIFORM = false;
	// choice 2
	public static boolean		PROGRESSION = true;
	public static boolean		NO_PROGRESSION = false;
	// choice 3
	public static boolean		CONNEXITY_4 = true;
	public static boolean		CONNEXITY_8 = false;
	// sensitivity ladder
	public static int			sensitivity_MIN = 0;
	public static int			sensitivity_MAX = 10;
	
	
	/**
	 * constructor -> build all the user interface
	 * @param title String of the window tiltle
	 */
    public MainDialog(String title) {
    	
    	super(title);
    	
    	mParamChoice = new boolean[6];
    	
		
        JLabel labelBackground = new JLabel("Object & Background intensity :");
		//Create the combo box
        String[] objectColor = new String[2];
        objectColor[0] = "Bright objects/Dark background";
        objectColor[1] = "Dark objects/Bright background";
        mObjectBackgroundComboBox = new JComboBox(objectColor);
        mObjectBackgroundComboBox.setSelectedIndex(0);
        mObjectBackgroundComboBox.setFont(new Font(null, Font.PLAIN, 12));
		
        JLabel labelObject = new JLabel("Intensities variations on objects :");
		//Create the combo box
        String[] objectDetails = new String[2];
        objectDetails[0] = "Object with bright peaks";
        objectDetails[1] = "Object with uniform intensities";
        mObjectDetailsComboBox = new JComboBox(objectDetails);
        mObjectDetailsComboBox.setSelectedIndex(0);
        mObjectDetailsComboBox.setFont(new Font(null, Font.PLAIN, 12));
		
        
        mChkProgress = new JCheckBox("Show progression messages", false);
		
        // combo boxes panel
        mComboBoxPanel = new JPanel();
        mComboBoxPanel.setLayout(mLayout);
        
        //addComponent(mComboBoxPanel, 0, 0, 1, 1, 4, labelImage);
       // addComponent(mComboBoxPanel, 0, 1, 1, 1, 4, mBtnUpDate);
        addComponent(mComboBoxPanel, 2, 0, 2, 1, 4, labelBackground);
        addComponent(mComboBoxPanel, 3, 0, 2, 1, 4, mObjectBackgroundComboBox);
        addComponent(mComboBoxPanel, 4, 0, 2, 1, 4, labelObject);
        addComponent(mComboBoxPanel, 5, 0, 2, 1, 4, mObjectDetailsComboBox);
        addComponent(mComboBoxPanel, 6, 0, 2, 1, 4,mChkProgress);
        
        
        //Create the button OK
        mBtnSTART = new JButton(mSTART);
        mBtnSTART.setHorizontalAlignment(JLabel.CENTER);
        mBtnSTART.addActionListener(this);
        
        //Create the button option
        mBtnOPTION = new JButton(mNotOPTION);
        mBtnOPTION.setHorizontalAlignment(JLabel.CENTER);
        mBtnOPTION.addActionListener(this);
        
        //Create the button help
        mBtnHelp = new JButton("?");
        mBtnHelp.setHorizontalAlignment(JLabel.CENTER);
        mBtnHelp.addActionListener(this);
        
        

		//Create the combo box
        /*String[] connexityChoice = {"4-connected", "8-connected" };
        mConnexityComboBox = new JComboBox(connexityChoice);
        mConnexityComboBox.setSelectedIndex(1);
        mConnexityComboBox.setFont(new Font(null, Font.PLAIN, 12));*/
        
        JLabel labelSlider = new JLabel("First merging :");
        
        mSlider = new JSlider(sensitivity_MIN,sensitivity_MAX,8);
        mSlider.setMinorTickSpacing(1);
        mSlider.setSnapToTicks(true);
        mSlider.setPaintTicks(true);
        mSlider.setMajorTickSpacing(4);
        //Create the label table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        JLabel lowLabel = new JLabel("Low");
        lowLabel.setFont(new Font(null, Font.PLAIN, 12));
        JLabel highLabel = new JLabel("High");
        highLabel.setFont(new Font(null, Font.PLAIN, 12));
        labelTable.put( new Integer( 0 ), lowLabel);
        labelTable.put( new Integer( 10 ), highLabel);
        mSlider.setLabelTable( labelTable );
        mSlider.setPaintLabels(true);
        mSlider.setEnabled(true);
        
        mSlider2 = new JSlider(sensitivity_MIN,sensitivity_MAX,2);
        mSlider2.setMinorTickSpacing(1);
        mSlider2.setSnapToTicks(true);
        mSlider2.setPaintTicks(true);
        mSlider2.setMajorTickSpacing(4);
        //Create the label table
        Hashtable<Integer, JLabel> labelTable2 = new Hashtable<Integer, JLabel>();
        JLabel lowLabel2 = new JLabel("Low");
        lowLabel2.setFont(new Font(null, Font.PLAIN, 12));
        JLabel highLabel2 = new JLabel("High");
        highLabel2.setFont(new Font(null, Font.PLAIN, 12));
        labelTable2.put( new Integer( 0 ), lowLabel2);
        labelTable2.put( new Integer( 10 ), highLabel2);
        mSlider2.setLabelTable( labelTable2 );
        mSlider2.setPaintLabels(true);
        
        mChkSizeMerging = new JCheckBox("Merging on size criterion", true);
        mChkSizeMerging.addItemListener(this);
        
        mPanelRadio = new JPanel();
        
        mRButton1 = new JRadioButton("   Automatic",true);
        mRButton2 = new JRadioButton("   Objects size : ",false);
        
        mRButton1.addItemListener(this);
        mRButton2.addItemListener(this);
        bg = new ButtonGroup();
        bg.add(mRButton1);
        bg.add(mRButton2);
        mTF = new JTextField(4);
  	  	mRButton1.setEnabled(true);
  	  	mRButton2.setEnabled(true);
  	  	mTF.setEnabled(false);
        
        mPanelRadio.setLayout(mLayout);
        addComponent(mPanelRadio, 0, 0, 1, 1, 4,mRButton1);
        addComponent(mPanelRadio, 1, 0, 1, 1, 4,mRButton2);
        addComponent(mPanelRadio, 1, 1, 1, 1, 4,mTF);
        
        
        mPanelSize = new JPanel();
        mChkSizeExcluding = new JCheckBox("Exclude objects on size criterion", false);
        mChkSizeExcluding.addItemListener(this);
        labelMinSizeObject = new JLabel("	min size (area) :");
        mTF2 = new JTextField(4);
        labelMaxSizeObject = new JLabel("	max size (area) :");
        mTF3 = new JTextField(4);
        labelMinSizeObject.setEnabled(false);
        labelMaxSizeObject.setEnabled(false);
        mTF2.setEnabled(false);
        mTF3.setEnabled(false);
        
        mPanelSize.setLayout(mLayout);
        addComponent(mPanelSize, 0, 0, 1, 1, 4,labelMinSizeObject);
        addComponent(mPanelSize, 0, 1, 1, 1, 4,mTF2);
        addComponent(mPanelSize, 1, 0, 1, 1, 4,labelMaxSizeObject);
        addComponent(mPanelSize, 1, 1, 1, 1, 4,mTF3);
        
        
        
        
        
        //
        
        
        JLabel optionTitle = new JLabel("Options");
        optionTitle.setHorizontalAlignment(JLabel.CENTER);

		mPnOption = new JPanel();
		mPnOption.setLayout(mLayout);
        addComponent(mPnOption, 0, 0, 1, 1, 4, new JLabel(""));
        addComponent(mPnOption, 1, 0, 1, 1, 4, new JSeparator());
		addComponent(mPnOption, 2, 0, 1, 1, 4, optionTitle);
		//addComponent(mPnOption, 3, 0, 1, 1, 4, mConnexityComboBox);
		addComponent(mPnOption, 3, 0, 1, 1, 4, labelSlider);
		addComponent(mPnOption, 4, 0, 1, 1, 4, mSlider);
		addComponent(mPnOption, 5, 0, 1, 1, 4, new JLabel("Second merging :"));
		addComponent(mPnOption, 6, 0, 1, 1, 4, mSlider2);
		addComponent(mPnOption, 7, 0, 1, 1, 4, mChkSizeMerging);
		addComponent(mPnOption, 8, 0, 1, 1, 4, mPanelRadio);
		addComponent(mPnOption, 9, 0, 1, 1, 4, mChkSizeExcluding);
		addComponent(mPnOption, 10, 0, 1, 1, 4, mPanelSize);
		
		mPnOption.setVisible(false);
		
        JPanel pnMain = new JPanel();
		pnMain.setLayout(mLayout);
		addComponent(pnMain, 0, 0, 3, 1, 4, mComboBoxPanel);
		addComponent(pnMain, 1, 0, 3, 1, 4, new JSeparator());
		addComponent(pnMain, 2, 0, 1, 1, 4, mBtnHelp);
		addComponent(pnMain, 2, 1, 1, 1, 4, mBtnOPTION);
		addComponent(pnMain, 2, 2, 1, 1, 4, mBtnSTART);
		addComponent(pnMain, 3, 0, 3, 1, 4,mPnOption);
		
		

		pnMain.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(pnMain);

        
    
    }
    /**
     * listener of the differents state of items
     */
    public void itemStateChanged(ItemEvent evt) 
    {
      Object source = evt.getSource();
      if(mChkSizeMerging.isSelected())
      {
    	  mRButton1.setEnabled(true);
    	  mRButton2.setEnabled(true);
    	  mTF.setEnabled(false);
      }
      else
      {
    	  mRButton1.setEnabled(false);
    	  mRButton2.setEnabled(false);
    	  mTF.setEnabled(false);
      }
      if (source == mRButton1)
      {
    	  mTF.setEnabled(false);
    	  //mRButton1.setSelected(true);
    	 // mRButton2.setSelected(false);
      }
      if (source == mRButton2)
      {
    	  mTF.setEnabled(true);
    	  //mRButton2.setSelected(true);
    	  //mRButton1.setSelected(false);
      }
      if (mChkSizeExcluding.isSelected())
      {
          labelMinSizeObject.setEnabled(true);
          labelMaxSizeObject.setEnabled(true);
          mTF2.setEnabled(true);
          mTF3.setEnabled(true);
      }
      else 
      {
          labelMinSizeObject.setEnabled(false);
          labelMaxSizeObject.setEnabled(false);
          mTF2.setEnabled(false);
          mTF3.setEnabled(false);
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
		mConstraint.fill = GridBagConstraints.HORIZONTAL;
		mLayout.setConstraints(comp, mConstraint);
		pn.add(comp);
	}
	
   
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
    	
        //Create and set up the content pane.
        MainDialog dialog = new MainDialog(mTitle);
        
    	if(dialog.isNoImage()==false){
	        //Display the window.
    		dialog.pack();
    		dialog.setResizable(false);
    		dialog.setVisible(true);
    	}
    }
    
    public static void main(String[] args) {
    	
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
        
    }
    
    
    
    
	/**
	 * true if the Maindialog object is instantiated without image, else false
	 * @return true if the Maindialog object is instantiated without image, else false
	 */
	public boolean isNoImage() {
		return mNoImage;
	}
	
	
	/**
	 * called when the window is closed -> stop the watershed process (thread)
	 */
    public void stopProcess(){
    	
    	if(mProcess!=null){
			mProcess.shutDown();
			while(mProcess.isStopped()==false);
		}
    }
    
    
    /** 
     * Listens the action events
     */
    public void actionPerformed(ActionEvent e) {
    	    	
    	if(e.getSource() == mBtnSTART){ 
    		
    		if(mBtnSTART.getText()==mSTART){
    		// button START
	            //int ind1 = (int)mSourceImageList.getSelectedIndex();
	            //ImagePlus impS = WindowManager.getCurrentImage();
	            
	            
	            ImagePlus impS = IJ.openImage();
	            
	            if (impS==null){
	                IJ.showMessage("Error"+"\n"+"no opened image...");
	                return;
	            }
	            
	            mBtnSTART.setText(mSTOP);
	    		
	            
	            // choosen option
	            
	            // choice 0
	            if(mObjectBackgroundComboBox.getSelectedIndex() == 0)
	            	mParamChoice[0]=BRIGHT;
	            else
	            	mParamChoice[0]=DARK;
	            // choice 1
	            if(mObjectDetailsComboBox.getSelectedIndex() == 0)
	            	mParamChoice[1]=PEAKS;
	            else
	            	mParamChoice[1]=UNIFORM;
	            // choice 2
	            mParamChoice[2]=mChkProgress.isSelected();
	            // choice 3
	            /*if(mConnexityComboBox.getSelectedIndex() == 0)
	            	mParamChoice[3]=CONNEXITY_4;
	            else*/
	            	mParamChoice[3]=CONNEXITY_8;
	            mParamChoice[4]=mChkSizeMerging.isSelected();
	            int objectSize=-1;
	            if (mChkSizeMerging.isSelected()&& mRButton2.isSelected())
	            {
	            	try{
	            		objectSize = Integer.parseInt(mTF.getText());
	            	}
            		catch(NumberFormatException e2){
            			IJ.showMessage("Warning! Enter an integer value for \"Objects size\"");
            			mBtnSTART.setText(mSTART);
            			return;
            		}
	            	if(objectSize<0)
	            	{
	            		IJ.showMessage("Warning! \"Objects size\" value must be positive");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            }
	            int firstMergingSensitivity = mSlider.getValue();
	            int secondMergingSensitivity = mSlider2.getValue();
	            int minObjectSize=0;
	            int maxObjectSize=0;
	            mParamChoice[5]=mChkSizeExcluding.isSelected();
	            if(mChkSizeExcluding.isSelected())
	            {
	            	try{
	            		minObjectSize = Integer.parseInt(mTF2.getText());
	            	}
	            	catch(NumberFormatException e1){
	            		IJ.showMessage("Warning! Enter an integer value for \"min size\"");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            	try{
	            		maxObjectSize = Integer.parseInt(mTF3.getText());
	            	}
	            	catch(NumberFormatException e2){
	            		IJ.showMessage("Warning! Enter an integer value for \"max size\"");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            	if(minObjectSize<0)
	            	{
	            		IJ.showMessage("Warning! \"min size\" value must be positive");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            	if(maxObjectSize<0)
	            	{
	            		IJ.showMessage("Warning! \"max size\" value must be positive");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            	if(maxObjectSize<minObjectSize)
	            	{
	            		IJ.showMessage("Warning! \"max size\" value must be superior to \"min size\" value");
	            		mBtnSTART.setText(mSTART);
	            		return;
	            	}
	            }
	            // process launching
	            mProcess = new Processing(impS,null, null, mParamChoice, firstMergingSensitivity,secondMergingSensitivity,objectSize,minObjectSize,maxObjectSize,mBtnSTART,mSTART);
	            mProcess.start();
	            IJ.showStatus("End");
	            
	            
    		}
    		else if(mBtnSTART.getText()==mSTOP){
    		// button STOP
    			if(mProcess!=null){
    				mProcess.shutDown();
    				while(mProcess.isStopped()==false);
    			}
    			mProcess=null;
	            mBtnSTART.setText(mSTART);
    		}
    	}
    	else if(e.getSource() == mBtnHelp)
    	{
    		IJ.showMessage("Help"+"\n"+" "+"\n"+
    						"\"Object & Background intensity :\"  ->  Choose if objects to segment are bright or dark"+"\n"+
    						"\"Intensities variations on objects :\"  ->  Choose if objects have an uniform intensities or bright peaks"+"\n"+
    						"\"Show progression messages\"  ->  Allow to follow the watershed progression"+"\n"+" "+"\n"+
    						"\"Options >>\"  ->  More options for informed users"+"\n"+
    						"\"4-connected/8-connected\"  ->  Choose the connexity of segmented objects"+"\n"+
    						"\"First merging :\"  ->  Choose the sensibility of the first watershed (intensity criterion)"+"\n"+
    						"\"Second merging :\"  ->  Choose the sensibility of the second watershed (shape criterion) :"+"\n"+
    						"                \"low\" -> High segmentation"+"\n"+
    						"               \"high\" -> High merging"+"\n"+" "+"\n"+
    						"\"Merging on size criterion\"  ->  Merge the small objects with its neighbors"+"\n"+
    						"            \"Automatic\"  ->  Automatic determination of the objects size (use for the merging on size criterion)"+"\n"+
    						"          \"Objects size\" ->  Enter the mean size of an object (use for the merging on size criterion)"+"\n"+" "+"\n"+
    						"\"Exclude objects on size criterion\"  ->  Exclude the object with a size smaller or higher than the limit size enter by the user"+"\n"+
    						"            \"min size (area)\"  ->  minimum size of the objects (size = area in pixels)"+"\n"+
    						"            \"max size (area)\"  ->  maximun size of the objects (size = area in pixels)");
    	}
    	else if(mBtnOPTION.getText()==mNotOPTION){	
			mPnOption.setVisible(true);
			this.pack();
			mBtnOPTION.setText(mOPTION);
		}
		else if(mBtnOPTION.getText()==mOPTION){	
			mPnOption.setVisible(false);
			this.pack();
			mBtnOPTION.setText(mNotOPTION);
		}
    }
    
    
    
	/**
	* Implements the windowActivated method for the WindowListener.
	*/
	public void windowActivated(WindowEvent e) {
	}
		
	/**
	* Implements the windowClosing method for the WindowListener.
	*/
	public void windowClosing(WindowEvent e) {
		stopProcess();
	}
		
	/**
	* Implements the windowClosed method for the WindowListener.
	*/
	public void windowClosed(WindowEvent e) {
	}

	/**
	* Implements the windowDeactivated method for the WindowListener.
	*/
	public void windowDeactivated(WindowEvent e) {
	}

	/**
	* Implements the windowDeiconified method for the WindowListener.
	*/
	public void windowDeiconified(WindowEvent e){
	}

	/**
	* Implements the windowIconified method for the WindowListener.
	*/
	public void windowIconified(WindowEvent e){
	}

	/**
	* Implements the windowOpened method for the WindowListener.
	*/
	public void windowOpened(WindowEvent e){
	}
	

}
