package cellst.Main;

import ij.IJ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import cellst.Enums.Step;
import cellst.Image.ImageFbt;
import cellst.Image.ShapeSet;
import cellst.Image.StackFbt;

/**
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class WorkingThr implements Runnable
{

  // ==========================================================================
  //                  Attributes
  // ==========================================================================
  /**
   * Messages box to manage thread progress.
   */
  ArrayBlockingQueue<int[]> msgBox;
  Semaphore sem;

  /**
   * Main Fluo_Bac_Tracker window.
   */
  protected Fluo_Bac_Tracker fluobt;
  /**
   * Beginning step.
   */
  protected Step startStep;
  /**
   * Ending step.
   */
  protected Step stopStep;
  /**
   * Current step.
   */
  protected Step currStep;

  /**
   * Slice to which applied computation, if 0 computation are applied to whole
   * movie.
   */
  protected int slice;

// ==========================================================================
  //                   CONSTRUCTOR
  // ==========================================================================
  /**
   * Working constructor.
   *
   * @param _fluobt main Fluo_Bac_Tracker window.
   * @param _startStep
   * @param _stopStep
   * @param _slice
   * @param _msgBox
   * @param _sem
   */
  public WorkingThr(Fluo_Bac_Tracker _fluobt,
                    Step _startStep,
                    Step _stopStep,
                    int _slice,
                    ArrayBlockingQueue<int[]> _msgBox,
                    Semaphore _sem)
  {
    // ====== inherited constructor =========
    super();

    initAttributes(_fluobt, _startStep, _stopStep, _slice, _msgBox, _sem);
  }

  protected final void initAttributes(Fluo_Bac_Tracker _fluobt,
                                      Step _startStep,
                                      Step _stopStep,
                                      int _slice,
                                      ArrayBlockingQueue<int[]> _msgBox,
                                      Semaphore _sem)
  {
    fluobt = _fluobt;
    startStep = _startStep;
    stopStep = _stopStep;
    slice = _slice;
    
    msgBox = _msgBox;
    sem = _sem;
  }

 

  /**
   * Override inherited run method. Run all treatments from the beginning step.
   */
  @Override
  public void run()
  {
    System.out.println("Working Thread started (" + Thread.currentThread().
        getId() + ")");

    // ============== Real computation ========================================
    try
    {
      //DEBUG time
      double tStart = System.currentTimeMillis();

//      int[] msg = new int[2];
//      msg[0] = slice;
      // ================= Begin processing at start step =====================
      // switch over start_step is used :
      // As the "break" statment occurs only at stop_step, processing compute all steps from start step to stop step.
      switch (startStep)
      {
        // ---------------- If start step is Rough background -----------------
        // compute rough background and continue.
        case ORIG:
        {
          currStep = Step.ROUGH_BACKGROUND;

          if (fluobt.isDoRoughB())
          {
            roughBackground();

            // Update current state of the computation
            fluobt.setStep(slice, currStep);

            // Tell the interface about the modification
            //msgBox.add(...)
            fluobt.upToDate = false;
          }

          // === If stop step is rough background ===
          // Stop processing by calling break.
          if (stopStep == Step.ROUGH_BACKGROUND)
          {
            break;
          }
        }

        // ---------------- If start step is denoising -----------------
        // denoise images and continue.
        case ROUGH_BACKGROUND:
        {
          currStep = Step.DENOIS;

          if (fluobt.isDoDenois())
          {
            denoising(sem);

            // Update current state of the computation
            fluobt.setStep(slice, currStep);

            // Tell the interface about the modification
            fluobt.upToDate = false;
          }

          // === If stop step is denoising ===
          // Stop processing by calling break.
          if (stopStep == Step.DENOIS)
          {
            break;
          }
        }

        // ---------------- If start step is Final background -----------------
        // compute final background and continue.
        case DENOIS:
        {
          currStep = Step.FINAL_BACKGROUND;

          if (fluobt.isDoFinalB())
          {
            finalBackground();

            // Update current state of the computation
            fluobt.setStep(slice, currStep);

            // Tell the interface about the modification
            fluobt.upToDate = false;
          }

          // === If stop step is final background ===
          // Stop processing by calling break.
          if (stopStep == Step.FINAL_BACKGROUND)
          {
            break;
          }
        }

        // ---------------- If start step is rencentering -----------------
        // recenter images and continue.
        case FINAL_BACKGROUND:
        {
          currStep = Step.RECENTER;

          if (fluobt.isDoRecenter())
          {
            recenter();

            // Update current state of the computation
            fluobt.setStep(slice, currStep);

            // Tell the interface about the modification
            fluobt.upToDate = false;
          }

          // === If stop step is recentering ===
          // Stop processing by calling break.
          if (stopStep == Step.RECENTER)
          {
            break;
          }
        }

        // ---------------- If start step is renormalizing -----------------
        // renormalize images and continue.
        case RECENTER:
        {
          currStep = Step.RENORM;

          if (fluobt.isDoRenorm())
          {
            renormalizing();

            // Update current state of the computation
            fluobt.setStep(slice, currStep);

            // Tell the interface about the modification
            fluobt.upToDate = false;
          }

          // === If stop step is renormalizing ===
          // Stop processing by calling break.
          if (stopStep == Step.RENORM)
          {
            break;
          }
        }

        // ---------------- If start step is seeds computing -----------------
        // Compute seeds and continue.
        case RENORM:
        {
          currStep = Step.SEEDS;

          seedsComputing();

          // Update current state of the computation
          fluobt.setStep(slice, currStep);

          // Tell the interface about the modification
          fluobt.upToDate = false;

          // === If stop step is seeds computing ===
          // Stop processing by calling break.
          if (stopStep == Step.SEEDS)
          {
            break;
          }
        }

        // ---------------- If start step is blobs computing -----------------
        // Compute blobs and continue.
        case SEEDS:
        {
          currStep = Step.BLOBS;

          blobsComputing();

          // Update current state of the computation
          fluobt.setStep(slice, currStep);

          // Tell the interface about the modification
          fluobt.upToDate = false;

          // === If stop step is blobs computing ===
          // Stop processing by calling break.
          if (stopStep == Step.BLOBS)
          {
            break;
          }
        }

        // ------------ If start step is final blobs computing ---------------
        // Clean and re-dilate blobs.
        case BLOBS:
        {
          currStep = Step.FINAL_BLOBS;

          blobsCleaning();

          // Update current state of the computation
          fluobt.setStep(slice, currStep);

          // Tell the interface about the modification
          fluobt.upToDate = false;

          // === If stop step is blobs cleaning ===
          // Stop processing by calling break.
          if (stopStep == Step.FINAL_BLOBS)
          {
            break;
          }
        }

      }

      //DEBUG time
      double tStop = System.currentTimeMillis();

      System.out.println("Working Thread stopped (" + Thread.currentThread().
          getId() + "): " + (tStop - tStart));
    }
    // ====================== If Thread is interrupted, stop it ==============
    catch (InterruptedException e)
    {
      System.out.println("Thread interrupted.");
      e.printStackTrace();
      fluobt.upToDate = true;
    }
  }

  // ==========================================================================
  //               Getters
  // ==========================================================================
  /**
   * Start step getter.
   *
   * @return start step.
   */
  public Step getStartStep()
  {
    return startStep;
  }

  /**
   * Stop step getter.
   *
   * @return stop step.
   */
  public Step getStopStep()
  {
    return stopStep;
  }

  /**
   * Current running step getter.
   *
   * @return current running step.
   */
  public Step getCurrStep()
  {
    return currStep;
  }


  public int getSlice()
  {
    return slice;
  }

  // ==========================================================================
  //               Setters
  // ==========================================================================


  // ==========================================================================
  //               Other methods
  // ==========================================================================
  /**
   * Compute rough backgrounds over original images stack.
   *
   * @throws java.lang.InterruptedException
   */
  protected void roughBackground() throws InterruptedException
  {
    // ======================= Load original stack ============================
    if (fluobt.getStepsList() == null)
    {
      IJ.showMessage("Original images not loaded yet.");
      Thread.currentThread().interrupt();
      Thread.sleep(0);
      return;
    }

    ImageFbt img = ImageFbt.load(fluobt.getOrigDir().resolve("slice_" + slice));
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }

    // ============= convert threshold from 255 max to stack max ==============
    int threshold = (int) img.grey255ToMax(fluobt.getRoughBThres());

    // ============= Compute its roughBackground ==============================
    // ------------ Copmpute rough background. --------------------------------
    int[] newBack = img.roughBackgroundMask(threshold,
                                            fluobt.getRoughBRadius() / fluobt.
        getZoom());

    // ============== Save new background =====================================
    Utils.saveObject(newBack, fluobt.getRoughBDir().resolve(
        fluobt.ROUGHB_NAMEFILE + "_" + slice).toString());

    // ============= Update attributes ========================================
    fluobt.setStep(slice, Step.ROUGH_BACKGROUND);

    // ============= set UpToDate to false ====================================
    fluobt.upToDate = false;

    // ============= Sleep to check if thread was not interrupted =============
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }
  }

  /**
   * Compute denoised image stack and save it.
   *
   * @throws java.lang.InterruptedException
   */
  protected void denoising(Semaphore sem) throws InterruptedException
  {
    // ======================= Load original stack ============================
    if (fluobt.getStepsList() == null)
    {
      IJ.showMessage("Original images not loaded yet.");
      Thread.currentThread().interrupt();
      Thread.sleep(0);
      return;
    }

    ImageFbt img = ImageFbt.load(fluobt.getOrigDir().resolve("slice_" + slice));

    // ==================== Set rough background ==============================
    int[] back = fluobt.getLastBackground(slice, Step.DENOIS);

    if (back != null)
    {
      img.setBackground(back);
    }

    // ============================ Denoise ImageFbt ==========================
    img.gtv_means_denoise(sem,
                          fluobt.getDenoiseLevel(),
                          fluobt.getDenoiseReduc(),
                          fluobt.getDenoiseNbPatch(),
                          (int) (fluobt.getDenoisePatchSize()),
                          (int) (fluobt.getDenoiseMaxDist()),
                          fluobt.getDenoiseEps(),
                          fluobt.getDenoiseMode().ordinal());

    // ================= Save denoised stack ==================================
    img.save(fluobt.getDenoisDir().resolve("slice_" + slice));
//    Utils.saveObject( img.getBackground(), fluobt.getDenoisDir().resolve( StackFbt.BACKGROUND_FILENAME + slice ).toString() );

    // =========== set check component of denoised images to true =============
    fluobt.setStep(slice, Step.DENOIS);

    // === set upToDate to false ===
    fluobt.upToDate = false;

    // === Sleep to check if thread was not interrupted ====
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }

  }

  /**
   * Computes final backgrounds over each slice of denoised or original image
   * stack.
   *
   * @throws InterruptedException thrown if thread is interrupted.
   */
  protected void finalBackground() throws InterruptedException
  {
    // ========================== Load denoised image =========================
    // If denoised image was not computed load original image.
    ImageFbt img = fluobt.getLastImage(slice, Step.FINAL_BACKGROUND);

    // ================= Compute final backgrounds ============================
    // ----------- Convert threshold from 255 max to stack max ----------- 
    int threshold = (int) img.grey255ToMax(fluobt.getBackThres());

    // -----------Compute new background ----------- 
    // Use rough background if already computed.
    int[] roughB = fluobt.getLastBackground(slice, Step.FINAL_BACKGROUND);
    int[] newBack;

    if (roughB != null)
    {
      newBack = img.backgroundMaskWithRoughB(!fluobt.getConn8(), threshold,
                                             roughB);
    }
    else
    {
      newBack = img.backgroundMask(!fluobt.getConn8(), threshold);
    }

    // ----------- Save new background -----------
    Utils.saveObject(newBack, fluobt.getBackgroundDir().resolve(
        fluobt.BACKGROUND_NAMEFILE + "_" + slice).toString());

    // ----------- Set check component of final background to true -----------
    fluobt.setStep(slice, Step.FINAL_BACKGROUND);

    // === set upToDate to false ===
    fluobt.upToDate = false;

  }

  /**
   * Recenter all images with colony center point.
   *
   * @throws java.lang.InterruptedException
   */
  protected void recenter() throws InterruptedException
  {
    // ========================== Load denoised image =========================
    // If denoised image was not computed load original image.
    ImageFbt img = fluobt.getLastImage(slice, Step.RECENTER);

    // ==================== Get last background ===============================
    int[] back = fluobt.getLastBackground(slice, Step.RECENTER);

    // ================ Recenter Image ========================================
    if (back == null)
    {
      return;
    }

    img.setBackground(back);

    // ----------- Recenter Image -----------
    img.recenter();

    // ----------- Save recentered stack and background ----- 
    img.save(fluobt.getRecenterDir().resolve("slice_" + slice));
    Utils.saveObject(img.getBackground(), fluobt.getRecenterDir().resolve(
        StackFbt.BACKGROUND_FILENAME + slice).toString());

    // ----------- Set recenter flag to true ----------
    fluobt.setStep(slice, Step.RECENTER);

    // ========== Set upToDate to false =====================================
    fluobt.upToDate = false;

  }

  /**
   * Compute renormalized images stack and save it.
   *
   * @throws InterruptedException thrown if thread is interrupted.
   */
  protected void renormalizing() throws InterruptedException
  {
    // ========================== Load denoised image =========================
    // If denoised image was not computed load original image.
    ImageFbt img = fluobt.getLastImage(slice, Step.RENORM);

    // ==================== Set last background ==============================
    int[] back = fluobt.getLastBackground(slice, Step.RENORM);
    if (back != null)
    {
      img.setBackground(back);
    }

    // ==================== Compute blobs from local min ======================
    // ----------- Compute min seed area in pixels = min cell area / 10 ----------- 
    int minSeedArea = (int) (fluobt.getMinArea() / (fluobt.getZoom() * fluobt.
                                                    getZoom() * 10));

    // ----------- Convert threshold from 255 max to stack max ----------- 
    int threshold = (int) img.grey255ToMax(fluobt.getRenormThres());

    // ----------- Compute blobs from local min ----------- 
    ShapeSet seeds = img.seedsFromLocalMin_Shape(minSeedArea, threshold, 1,
                                                 fluobt);

    // ==================== Renormalization ====================================
    // ----------- Renormalize stack ----------- 
    img.renormalize(seeds,
                    (int) (fluobt.getRenormSqSize() / fluobt.getZoom()),
                    fluobt.getRenormIter(),
                    (int) (fluobt.getRenormRadius() / fluobt.getZoom()));

    // ----------- Insert it in renormalized stack (initiated if needed) ------ 
    img.save(fluobt.getRenormDir().resolve("slice_" + slice));
    //    Utils.saveObject( img.getBackground(), fluobt.getRenormDir().resolve( StackFbt.BACKGROUND_FILENAME + slice ).toString() );

    // ----------- Set check component of renormalization to true ----------- 
    fluobt.setStep(slice, Step.RENORM);
    fluobt.upToDate = false;

    // === Sleep to check if thread was not interrupted ====
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }
  }

  /**
   * Compute blobs shapesets and save them.
   *
   * @throws java.lang.InterruptedException
   */
  protected void seedsComputing() throws InterruptedException
  {
    // ====================== Load last computed image ========================
    ImageFbt img = fluobt.getLastImage(slice, Step.SEEDS);

    int[] back = fluobt.getLastBackground(slice, Step.SEEDS);
    if (back != null)
    {
      img.setBackground(back);
    }

    // ============== Convert threshold from 255max to stack max ==============
    int threshold = (int) img.grey255ToMax(fluobt.getSeedsThres());

    // ============ Compute and save blobs  ===================================
    ShapeSet seeds = img.seedsFromBin_ShapeS(threshold, fluobt);
    seeds.save(fluobt.getSeedsDir().toString() + "/Seeds_" + Integer.toString(
        slice));

    // ========= Set check component of blobs to true =========================
    fluobt.setStep(slice, Step.SEEDS);
    fluobt.upToDate = false;

    // === Sleep to check if thread was not interrupted ====
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }

  }

  /**
   * Compute redilatedBlobs ShapeSets and save them. This method will use last
   * computed stack ( original, denoised or renormalized ) and blobs files.
   *
   * @throws InterruptedException thrown if thread is interrupted.
   */
  protected void blobsComputing() throws InterruptedException
  {
    if (fluobt.getStep(slice).compareTo(Step.SEEDS) < 0)
    {
      IJ.showMessage("seeds were not computed yet.");
      Thread.currentThread().interrupt();
      Thread.sleep(0);
      return;
    }

    // ====================== Load last computed image ========================
    ImageFbt img = fluobt.getLastImage(slice, Step.BLOBS);

    int[] back = fluobt.getLastBackground(slice, Step.BLOBS);
    if (back != null)
    {
      img.setBackground(back);
    }

    // =============== Dilate blobs to redilatedBlobs and save them ====================
    ShapeSet seeds = (ShapeSet) Utils.loadObject(
        fluobt.getSeedsDir() + "/Seeds_" + slice);

    float[] speed = img.computeViscosity(fluobt.getSpeedA(), fluobt.getSpeedB());
    ShapeSet blobs = img.dilate_ShapeS(seeds, speed, fluobt.getDilateIter(),
                                      fluobt);

    // ======================= Compute graph of redilatedBlobs =========================
    // === Compute  graph of connexions ===
    blobs.computeGraph();

    // === Save redilatedBlobs with graph ===
    blobs.save(fluobt.getBlobsDir() + "/Blobs_" + slice);
    // ========= Set check component of redilatedBlobs to true =========================
    fluobt.setStep(slice, Step.BLOBS);
    fluobt.upToDate = false;

    // === Sleep to check if thread was not interrupted ====
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }

  }

  /**
   * Compute final redilatedBlobs ShapeSets and save them.
   *
   * @throws InterruptedException thrown if thread is interrupted.
   */
  protected void blobsCleaning() throws InterruptedException
  {
    if (fluobt.getStep(slice).compareTo(Step.BLOBS) < 0)
    {
      IJ.showMessage("blobs were not computed yet.");
      Thread.currentThread().interrupt();
      Thread.sleep(0);
      return;
    }
//
//    int minBlobArea = ( int ) ( fluobt.getMinBlobArea() / ( fluobt.getZoom() * fluobt.getZoom() ) );
//    int minCellArea = ( int ) ( fluobt.getMinArea() / ( fluobt.getZoom() * fluobt.getZoom() ) );
//    double minBorder = fluobt.getMinBlobBorder() / fluobt.getZoom();
//    double maxWidth = fluobt.getMaxWidth() / fluobt.getZoom();
//

    // ====================== Load last computed image ========================
    ImageFbt img = fluobt.getLastImage(slice, Step.BLOBS);

    int[] back = fluobt.getLastBackground(slice, Step.BLOBS);
    if (back != null)
    {
      img.setBackground(back);
    }

    // =================== Moad blobs =========================================
    ShapeSet blobs = (ShapeSet) Utils.loadObject(
        fluobt.getBlobsDir() + "/Blobs_" + slice);

    // ======================= Clean Blobs ===========================
    // === Compute and clean graph of connexions ===
    // clean small Blobs by fusionning the with neighbor
    // Remove isolated Blobs too small to be a cell
    // merge edge Blobs too small to be a cell
    blobs.cleanGraph();

    // ====================== Redilate blobs ==================================
    float[] speed = img.computeViscosity(fluobt.getSpeedA() / 10, fluobt.
        getSpeedB());
    blobs = img.dilate_ShapeS(blobs, speed, fluobt.getCleanIter(), fluobt);

    // === Save cleaned blobs ===
    blobs.save(fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).toString());
    // ======== Set check component of final redilatedBlobs to true ===========
    fluobt.setStep(slice, Step.FINAL_BLOBS);
    fluobt.upToDate = false;

    // === Sleep to check if thread was not interrupted ====
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }

  }

  /**
   *
   * @throws InterruptedException
   */
  protected void genCells() throws InterruptedException
  {
    if (fluobt.getStep(slice).compareTo(Step.FINAL_BLOBS) < 0)
    {
      IJ.showMessage("final blobs were not computed yet.");
      Thread.currentThread().interrupt();
      Thread.sleep(0);
      return;
    }

    // === Load dilatedBlobs shapeset ===
    ShapeSet blobs = (ShapeSet) Utils.loadObject(fluobt.getfinalBlobsDir().
        resolve("Blobs_" + slice).toString());

//    int minCellArea = ( int ) ( fluobt.getMinArea() / ( fluobt.getZoom() * fluobt.getZoom() ) );
//    double maxWidth = fluobt.getMaxWidth() / fluobt.getZoom();
    // === Compute possible cells ===
    blobs.generateCells();

    // === Save final redilatedBlobs ===
    blobs.save(fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).toString());

    // === set upToDate to false ===
    fluobt.upToDate = false;

  }

}
