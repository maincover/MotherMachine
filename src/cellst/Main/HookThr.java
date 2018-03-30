package cellst.Main;

import ij.gui.YesNoCancelDialog;
import ij.io.DirectoryChooser;

import java.awt.Frame;
import java.io.IOException;
import java.nio.file.*;

import cellst.DirManagement.*;
import cellst.Interface.WaitingDialog;

/**
 * Thread started when jvm is shutting down to delete all temporary directories.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class HookThr extends Thread
{

  // ==========================================================================
  //                   ATTRIBUTES
  // ==========================================================================
  Fluo_Bac_Tracker fluobt;
  Frame frame;

  // ==========================================================================
  //                   CONSTRUCTOR
  // ==========================================================================
  /**
   * Creates Hook thread to be started as JVM shutdown..
   *
   * @param _fluobt main Fluo_Bac_Tracker window.
   */
  public HookThr(Fluo_Bac_Tracker _fluobt)
  {
    // ====== inherited constructor =========
    super();

    // ====== init attributes =========
    fluobt = _fluobt;

    frame = new Frame("end");
  }

  // ==========================================================================
  //                   Run method
  // ==========================================================================
  /**
   * Run method : delete all temporary directories. Register properties.
   */
  @Override
  public void run()
  {

    // ============= Remove all temporary directories. =======================
    try
    {
      Files.walkFileTree(fluobt.getOrigDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getRoughBDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getDenoisDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getBackgroundDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getRecenterDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getRenormDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getSeedsDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getBlobsDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getfinalBlobsDir(), new VisitorDelete());
      Files.walkFileTree(fluobt.getblobSolvDir(), new VisitorDelete());
    }
    catch (IOException ex)
    {
      System.out.println(ex.toString() + " : " + ex.getLocalizedMessage());
    }

    // ==================== Save properties ===================================
    Object[] list = new Object[1];
    list[0] = fluobt.workingDir.toString();
    Utils.saveObject(list, "./Fluobt_properties");
  }
}
