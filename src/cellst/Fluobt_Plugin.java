package cellst;
import ij.*;
import ij.io.*;
import ij.plugin.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cellst.Interface.*;
import cellst.Interface.Preprocessings.MainWorkFlowFrame;
import cellst.Main.*;

/**
 * Main plugin file. Fluobt_Plugin is segmenting and tracking cells from
 * microscopic images. Preprocessings are available ( denoising, recentering,
 * renormalizing). Then an over-segmentation is perform to get blobs. Cells are
 * tracked and segmented from blobs.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Fluobt_Plugin implements PlugIn
{

  /**
   * Launches the Fluo_Bac_Tracker plugin.
   *
   * @param arg
   */
  @Override
  public void run(String arg)
  {
    // ============ If working directory was saved, ask to use it =============
    Path properties = Paths.get("./Fluobt_properties");
    Path workingDir = Paths.get(System.getProperty("java.io.tmpdir"));
    if (Files.exists(properties))
    {
      try
      {
        Object[] list = (Object[]) Utils.loadObject(properties.toString());

        int i = 0;
        workingDir = Paths.get((String) list[ i++]);
      }
      catch (Exception ex)
      {
        IJ.showMessage(
            "Error loading last workingDir, temporary directory used.");
        workingDir = Paths.get(System.getProperty("java.io.tmpdir"));
      }
    }

    // ==================== Launch plugin =====================================
    Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker(workingDir);
    InterfaceControl control = new InterfaceControl(fluobt);
    MainWorkFlowFrame mainFrame = MainWorkFlowFrame.create(control);
    fluobt.addObserver(mainFrame);
    mainFrame.setVisible(true);
   
  }

  /**
   * Main method for debugging.
   *
   * For debugging, it is convenient to have a method that starts ImageJ and
   * calls the plugin, e.g. after setting breakpoints.
   *
   * @param args unused
   * @throws java.lang.InterruptedException
   */
  public static void main(String[] args) throws InterruptedException
  {
    ImageJ ImJ = new ImageJ();

    IJ.runPlugIn(Fluobt_Plugin.class.getName(), "");
  }

}
