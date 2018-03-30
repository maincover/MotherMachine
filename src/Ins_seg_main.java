import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;




public class Ins_seg_main implements PlugIn {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ImageJ ij = new ImageJ();
		Ins_seg_panel ins_panel = new Ins_seg_panel();
		ins_panel.setVisible(true);
		
	}

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		if (arg.equals("about"))
		{showAbout(); return;}
		if (arg.equals("uninstall"))
		{showUnintallDialog(); return;}
		if (arg.equals("run"))
		{
			Ins_seg_panel ins_seg_panel = new Ins_seg_panel();
			ins_seg_panel.setVisible(true);
			return;
		}
	}
	
	public void showUnintallDialog() {
		IJ.showMessage("Bacteria Analyzer", 
            		"To uninstall this plugin, move Bacteria_Analyzer.jar out\n"
			+"of the plugins folder and restart ImageJ.");
    }

	public void showAbout() {
		IJ.showMessage("Microfluidic Bacteria Analyzer", "This plugin executes a complete automatic E.coli segmentation and lineage\n"
			+ "process for mother machine platforme, see http://jun.ucsd.edu/ for more information.\n"
			+ "The programme can be used for non-commercial purpose.\n"
			+ "All rights reserved @ Xiaohu SONG, Inserm U1001, Paris."
			);
	}
}
