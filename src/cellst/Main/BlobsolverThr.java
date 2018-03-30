/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Main;

import cellst.Image.*;

/**
 *
 * @author Maagali Vangkeosay, David Parsons
 */
public class BlobsolverThr extends Thread
{

  // ==========================================================================
  //                  ATTRIBUTES
  // ==========================================================================
  Fluo_Bac_Tracker fluobt;

  // ==========================================================================
  //                  CONSTRUCTORS
  // ==========================================================================
  public BlobsolverThr(Fluo_Bac_Tracker fluobt)
  {
    super();
    this.fluobt = fluobt;
  }

  // ==========================================================================
  //                  RUN
  // ==========================================================================
  @Override
  public void run()
  {
    // ============== Open a waiting progressBar to show user it's working ====
    WaitThr waitThread = new WaitThr(this);
    waitThread.start();

      // ================== Create blob solver ==================================
      BlobSolver blobSolv = new BlobSolver(
          fluobt.getfinalBlobsDir(),
          "Blobs_",
          fluobt);

      // ================== Construct lineage ===================================
      blobSolv.constructLineage();

      // ======================= Save ===========================================
      blobSolv.saveFiles(fluobt.getblobSolvDir());
  }
}
