/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Interface;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;

import java.awt.*;
import java.nio.file.*;
import java.util.*;

import cellst.Enums.*;
import cellst.Image.*;
import cellst.Main.*;

/**
 * Interface controler : it is called by interface components to do action on
 * the Fluo_Back_Tracker model (as save, import, ...).
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class InterfaceControl
{
  // ==========================================================================
  //                   Attributes
  // ==========================================================================
  /**
   * Fluo_Bac_Tracker : main model.
   */
  protected Fluo_Bac_Tracker fluobt;

  private Thread run;

  // ==========================================================================
  //                    Constructors
  // ==========================================================================
  /**
   * Creates the Interface controler
   *
   * @param _fluobt main model
   */
  public InterfaceControl(Fluo_Bac_Tracker _fluobt)
  {
    fluobt = _fluobt;
    run = new Thread();
  }

  // ==========================================================================
  //                     Setter
  // ==========================================================================
  /**
   * @param fluobt the fluobt to set
   */
  public void setFluobt(Fluo_Bac_Tracker fluobt)
  {
    this.fluobt = fluobt;
  }

  // ==========================================================================
  //                     Getter
  // ==========================================================================
  /**
   * @return the fluobt
   */
  public Fluo_Bac_Tracker getFluobt()
  {
    return fluobt;
  }

  /**
   * @return the run
   */
  public Thread getRun()
  {
    return run;
  }

  // ==========================================================================
  //                     Public methods
  // ==========================================================================
  // --------------------------------------------------------------------------
  // --              actions Methods
  // --------------------------------------------------------------------------
  // ----------------- SAVE AND LOAD ------------------------------------------
  /**
   * Saves all fluobt results in results path. If results path wasn"t yet
   * define, ask user tu define it.
   *
   * @param frame Frame that called the save action.
   */
  public void save(Frame frame)
  {
    // ============== If no result path was defined ===========================
    // Let user choose it.
    if (fluobt.getPathRes() == null)
    {
      saveAs(frame);

      // If didn't choose any directory (result path still null), do nothing.
      if (fluobt.getPathRes() == null)
      {
        return;
      }
    }

    // ======== Show waiting bar ==============================================
    WaitingDialog waitDial = new WaitingDialog(frame, false);
    waitDial.setVisible(true);

    // =============== Save results in result path ============================
    try
    {
      fluobt.save(fluobt.getPathRes());
    }
    catch (Exception ex)
    {
      // do nothing
    }

    System.out.println("saved");

    // ======== Close waiting bar ==============================================
    waitDial.dispose();
  }

  /**
   * Saves all fluobt results in a new results path defined by user.
   *
   * @param frame Frame that called the save action.
   */
  public void saveAs(Frame frame)
  {
    // ================= Let User choose his/her results path =================
    DirectoryChooser dirChoos = new DirectoryChooser(
        "Choose the results directory");
    String dir = dirChoos.getDirectory();

    // -------------- If dialog was cancelled, stopRun method -------------------
    if (dir == null)
    {
      return;
    }

    // -------------- Update result path -------------------------------------
    fluobt.setPathRes(dir);

    // ================ Save results ==========================================
    save(frame);
  }

  /**
   * Loads results from a former results path.
   *
   * @param frame Frame that called the load action.
   */
  public void load(Frame frame)
  {
    // ===================== Choose directory =================================
    DirectoryChooser dirChoos = new DirectoryChooser(
        "Choose the results directory");
    String dir = dirChoos.getDirectory();

    // ------------ If dialog was cancelled stopRun method -----------------------
    if (dir == null)
    {
      return;
    }

    // ======== Show waiting bar ==============================================
//    WaitingDialog waitDial = new WaitingDialog(frame, false);
//    waitDial.setVisible(true);
    WaitThr waitBar = new WaitThr(Thread.currentThread());
    waitBar.start();

    // ========= Load results  ================================================
    try
    {
      fluobt.load(Paths.get(dir));
      System.out.println("loaded");
    }
    catch (Exception ex)
    {
      System.out.println("loading exception");
    }

    // ======== Close waiting bar ==============================================
//    waitDial.dispose();
    waitBar.interrupt();

  }

  // ========================== IMPORT ========================================
  /**
   * Import a sequence of images (that share a common title) or an ImageStack,
   * creates original ImageFbt from it and save them.
   *
   * @param frame Frame that called the import action.
   */
  public void importImgs(Frame frame)
  {
    // ================= Let User choose his/her images =======================
    OpenDialog opDiag = new OpenDialog("Open","Choose one of the images.");
    String dir = opDiag.getDirectory();
    String name = opDiag.getFileName();

    // ========== If dialog was cancelled, return =============================
    if (dir == null || name == null)
    {
      return;
    }

    // ======== Show waiting dialog ===========================================
    WaitingDialog waitDial = new WaitingDialog(frame, false);
    waitDial.setVisible(true);

    // ================== Open Stack or images ================================
    // --------------------- If it is a Stack ---------------------------------
    StackFbt newStack;

    newStack = StackFbt.openStack(dir + name);

    // ------------ If it is images -------------------------------------------
    if (newStack.getSize() == 1)
    {
      // Delete extension from name
      int i = name.lastIndexOf(".");
      if (i != -1)
      {
        name = name.substring(0, i);
      }

      // Delete numbers at the end of the name
      name = name.replaceAll("[0-9]+$", "");

      // Open stack to check it is valid stack file 
      newStack = StackFbt.open(dir, name);
    }

    // ===== Delete precedents result from temporary directories. =============
    fluobt.clearTemporaryFiles();

    // ====================== Save new stackFbt. ==============================
    newStack.saveImages(fluobt.getOrigDir());

    // =================== Update images attributes in fluobt. ==============
    fluobt.updateToStack(newStack);

    // ================== Update options ====================================
    fluobt.setRoughBThres(fluobt.getThres());
    fluobt.setBackThres(fluobt.getThres());

    // ======== Close waiting bar ===========================================
    waitDial.dispose();
  }

  // ========================== QUIT ==========================================
  /**
   * Check if last results are saved and if not, ask user if they want to save
   * them.
   *
   * @param frame Frame that called the quit action.
   */
  public void quit(Frame frame)
  {
    // =========== If results aren't saved ====================================
    // ask user if they want to save them.
    if (!fluobt.isSaved())
    {
      // ---------------- Show YES/NO/CANCEL dialog options -------------------
      YesNoCancelDialog dialog = new YesNoCancelDialog(frame,
                                                       "Do you want to save ?",
                                                       "Do you want to save your current work ?");

      // ----------------- If user want to save, save results -----------------
      if (dialog.yesPressed())
      {
        save(frame);
      }
      // ---------------- If user cancel quit action, return ------------------
      else if (dialog.cancelPressed())
      {
        return;
      }
    }

    // ===================== Exit system ======================================
    System.exit(0);
  }

  // ====================== PREVIEW ===========================================
  /**
   * Update an imagePanel by setting it's image to 'imagesStack' and its
   * blobsOrSeedsStack to 'blobsOrSeedsStack'.
   *
   * @param imagesStack ImageStack to put in previewPanel
   * @param blobsOrSeedsStack imageStack of seeds or blobs to put in
   * previewPAnel
   * @param previewPanel imagePanel to update
   * @param withResize are the images resize when put in previewPanel
   * @param slice slice of the preview panel
   * @param drawOpt Option to draw either just blobs, connection graph or cells.
   */
  public void preview(ImageStack imagesStack, ImageStack blobsOrSeedsStack,
                      ImagePanel previewPanel,
                      boolean withResize,
                      int slice,
                      BlobsDrawOpt drawOpt)
  {
    // ================ Update Image panel main imagerStack ===================
    // With resize 
    if (withResize)
    {
      previewPanel.setImageWithResize(imagesStack, slice);
    }
    // Or without resize
    else
    {
      previewPanel.setImage(imagesStack, slice);
    }

    // ============== Update Image panel seedsOrBlobs image ===================
    // (if no seeds or blobs : it is null)
    previewPanel.setBlobsOrSeedsImage(blobsOrSeedsStack);
  }

  /**
   * Update an imagePanel by setting it's image to 'imagesStack' and its
   * blobsOrSeedsStack to 'blobsOrSeedsStack'.
   *
   * @param imagesStack ImageStack to put in previewPanel
   * @param blobsOrSeedsStack imageStack of seeds or blobs to put in
   * previewPAnel
   * @param previewPanel imagePanel to update
   * @param withResize are the images resize when put in previewPanel
   * @param slice slice of the preview panel
   */
  public void preview(ImageStack imagesStack, ImageStack blobsOrSeedsStack,
                      ImagePanel previewPanel,
                      boolean withResize,
                      int slice)
  {
    preview(imagesStack, blobsOrSeedsStack, previewPanel, withResize, slice,
            BlobsDrawOpt.BLOBS);
  }

  /**
   * Previews images of one 'step' of the process and put them in the given
   * ImagePanel.
   *
   * @param previewPanel ImagePanel to update with this step preview
   * @param withResize true : resize images to the panel size, false : don't
   * resize
   * @param step Step of the process we want to preview
   * @param drawOpt What do we draw if previewing seeds or blobs : blobs,
   * connection graph or cells. If preview is not about seeds or blobs, this
   * option has no use (can be null).
   */
  public void preview(ImagePanel previewPanel, boolean withResize, Step step,
                      BlobsDrawOpt drawOpt)
  {
    // ===================== Get Images path we are going to use ==============
    //                        Or seeds or blobs path and title.
    Object[] res = getShowOrPreviewUseful(step);
    String type = (String) res[0];
    Path path = (Path) res[1];
    String title = (String) res[2];

    // ================= Init variables =======================================
    ImageStack imageStack;
    ImageStack blobsOrSeedsStack = null;

    // ============== If preview is not about seeds or blobs ==================
    //  Open imagePlus from the path and get its Stack
    if (type.equals("images"))
    {
      imageStack = getImagesStack(path, step);
      if (imageStack == null)
      {
        return;
      }
    }

    // ============== If preview is about seeds or blobs ======================
    else if (type.equals("seedsOrBlobs"))
    {
      ImageStack[] stacksArray = getSeedsOrBlobsStacks(path, step, title,
                                                       drawOpt);
      imageStack = stacksArray[0];
      blobsOrSeedsStack = stacksArray[1];
    }

    // =========== If preview is about background =============================
    else if (type.equals("background"))
    {
      imageStack = getBackgroundStack(path, step, title);
    }

    // =========== If preview is about blobsolver =============================
    else
    {
      ImageStack[] stacksArray = getBlobSolverStacks(path);
      imageStack = stacksArray[0];
      blobsOrSeedsStack = stacksArray[1];
    }

    // ========= If preview is about  original image ==========================
    //   Set ImagePanel slice to 1.
    //   Else keep last ImagePanel slice.
    // This is to show first slice when loading new images.
    int slice = previewPanel.getSlice();
    if (step.equals(Step.ORIG))
    {
      slice = 1;
    }

    // =========== Update Imagepreview ========================================
    preview(imageStack, blobsOrSeedsStack,
            previewPanel, withResize, slice,
            drawOpt);
  }

  /**
   * Previews images of one 'step' of the process and put them in the given
   * ImagePanel.
   *
   * @param previewPanel ImagePanel to update with this step preview
   * @param withResize true : resize images to the panel size, false : don't
   * resize
   * @param step Step of the process we want to preview
   */
  public void preview(ImagePanel previewPanel, boolean withResize, Step step)
  {
    preview(previewPanel, withResize, step, BlobsDrawOpt.BLOBS);
  }

  // ===================== SHOW ===============================================
  /**
   * Opens an ImagePlus corresponding to the given process 'step' and shows it.
   *
   * @param step Step of the process we want to show
   * @param drawOpt What do we draw if showing seeds or blobs : blobs,
   * connection graph or cells. If show is not about seeds or blobs, this option
   * has no use (can be null).
   */
  public void show(Step step, BlobsDrawOpt drawOpt)
  {
    // ===================== Get Images path we are going to use ==============
    //                        Or seeds or blobs path and title.
    Object[] res = getShowOrPreviewUseful(step);
    String type = (String) res[0];
    Path path = (Path) res[1];
    String title = (String) res[2];

    // ====================== Init variables =================================
    ImagePlus toshow;

    // ======= If show is an images step  =====================================
    // (not background nor seeds or blobs computing)
    // Simply open ImagePlus of ImagePath
    if (type.equals("images"))
    {
      ImageStack imStack = getImagesStack(path, step);

      if (imStack == null)
      {
        return;
      }

      toshow = new ImagePlus(step.name(), imStack);
    }

    // ======= If show is about seeds or blobs computing ======================
    // Mixed last computed images with blobs or seeds images
    else if (type.equals("seedsOrBlobs"))
    {
      ImageStack[] stacks = getSeedsOrBlobsStacks(path, step,
                                                  title, drawOpt);
      ImageStack seedsOrBlobsStack = Utils.MixStacks(stacks[0], stacks[1]);
      
      if( seedsOrBlobsStack == null )
      {
        return;
      }
      toshow = new ImagePlus(step.name(), seedsOrBlobsStack);
    }

    // ======= If show is about background ====================================
    else if (type.equals("background"))
    {
      ImageStack imStack = getBackgroundStack(path, step, title);
      
      if( imStack == null)
      {
        return;
      }
      
      toshow = new ImagePlus(step.name(),imStack );
    }

    // ======= If show is about blobsolver ====================================
    else
    {
      ImageStack[] stacksArray = getBlobSolverStacks(path);
      toshow = new ImagePlus(step.name(), Utils.MixStacks(stacksArray[0],
                                                          stacksArray[1]));
    }

    // ====================== Show ============================================
    toshow.show();
  }

  /**
   * Opens an ImagePlus corresponding to the given process 'step' and shows it.
   *
   * @param step Step of the process we want to show
   */
  public void show(Step step)
  {
    show(step, BlobsDrawOpt.BLOBS);
  }

  // ====================== RUN ===============================================
  public void run(Step startStep, Step stopStep, int slice)
  {
    if (stopStep.equals(Step.BLOBSOLVER))
    {
      if (startStep.compareTo(Step.FINAL_BLOBS) < 0)
      {
        run(startStep, Step.FINAL_BLOBS, slice);
        try
        {
          run.join();
        }
        catch (InterruptedException ex)
        {
          ex.printStackTrace();
          return;
        }
      }

//      run = new BlobsolverThr(fluobt);
//      run.start();
      // ------------------ Create blob solver --------------------------------
      BlobSolver blobSolv = new BlobSolver(
          fluobt.getfinalBlobsDir(),
          "Blobs_",
          fluobt);

      try
      {
        ChooseBlobsolverParam(blobSolv);

        // ------------------ Construct lineage ---------------------------------
        blobSolv.constructLineage();

        // ----------------------- Save -----------------------------------------
        blobSolv.saveFiles(fluobt.getblobSolvDir());
      }
      catch (InterruptedException ex)
      {
      }

    }
    else
    {
      run.interrupt();

      run = new ManagerThr(fluobt,
                           startStep,
                           stopStep,
                           slice);

      run.start();
    }
  }

  public void runAll(Step startStep, Step stopStep)
  {
    run(startStep, stopStep, -1);
  }

  public void stopRun()
  {
    run.interrupt();
  }

  // --------------------------------------------------------------------------
  // --              other Methods
  // --------------------------------------------------------------------------
  // ===================== SHOW AND PREVIEW ===================================
  /**
   * Gives Images path of the given 'step' if it is not about seeds or blobs,
   * else gives seeds or blobs path and title.
   *
   * @param step Step of the process
   * @return an array of Objects containing : - a string giving the type of step
   * we are dealing with ( images, background or seedsOrBlobs) - path of the
   * 'step' containing either images or seeds or blobs - the title of files
   * containg seeds or blobs or background. If it is an images step, it will be
   * null.
   */
  private Object[] getShowOrPreviewUseful(Step step)
  {
    // ===================== Init variables ===================================
    String type = null;
    Path path = null;
    String title = null;

    // ================= Switch on step =======================================
    // For each step gets corresponding path and if necessary 
    // seeds or blobs title
    switch (step)
    {
      case ORIG:
        path = fluobt.getOrigDir();
        type = "images";
        break;

      case ROUGH_BACKGROUND:
        path = fluobt.getRoughBDir();
        title = fluobt.ROUGHB_NAMEFILE;
        type = "background";
        break;

      case DENOIS:
        path = fluobt.getDenoisDir();
        type = "images";
        break;

      case FINAL_BACKGROUND:
        path = fluobt.getBackgroundDir();
        title = fluobt.BACKGROUND_NAMEFILE;
        type = "background";
        break;

      case RECENTER:
        path = fluobt.getRecenterDir();
        type = "images";
        break;

      case RENORM:
        path = fluobt.getRenormDir();
        type = "images";
        break;

      case SEEDS:
        path = fluobt.getSeedsDir();
        title = "Seeds";
        type = "seedsOrBlobs";
        break;

      case BLOBS:
        path = fluobt.getBlobsDir();
        title = "Blobs";
        type = "seedsOrBlobs";
        break;

      case FINAL_BLOBS:
        path = fluobt.getfinalBlobsDir();
        title = "Blobs";
        type = "seedsOrBlobs";
        break;

      case BLOBSOLVER:
        path = fluobt.getblobSolvDir();
        title = "slice";
        type = "blobsolver";
    }

    // ==================== returns computed path and title ===================
    Object[] res =
    {
      type,
      path,
      title
    };

    return res;
  }

  /**
   * Changes 'image' by drawing in white all pixels in background.
   *
   * @param image ImageStack to modify.
   * @param background list of backgrounds of each slice. A background is an
   * array of int. For each pixel if background = 0 it is not in background else
   * if background = 1 it is in background.
   */
  private void setBackgroundWhite(ImageStack image,
                                  ArrayList<int[]> background)
  {
    if (background == null || background.isEmpty())
    {
      return;
    }
    

    int size = image.getSize();
    int width = image.getWidth();
    int height = image.getHeight();
    int pixNb = width * height;

    if( size != background.size() )
    {
      System.err.println("Error in InterfaceControl.setBackgroundWhite : Background and image have not same slice number.");
      return;
    }
    
    for (int slice = 0; slice < size; slice++)
    {
      int[] currBack = background.get(slice);

      if (currBack == null || currBack.length != pixNb)
      {
        continue;
      }

      ImageProcessor iProc = image.getProcessor(slice + 1);
      iProc.setColor(Color.white);

      for (int index = 0; index < pixNb; index++)
      {
        if (currBack[ index] == 1)
        {
          Point pixel = Utils.indexToPoint(index, width, height);
          iProc.drawPixel(pixel.x, pixel.y);
        }
      }
    }

  }

  /**
   * Gives ImageStack of the images at the given 'step' of preprocessing. This
   * should be use for steps computing images (and not background or seeds or
   * blobs).
   *
   * @param path Path were the images can be found.
   * @param step step of the images
   * @return
   */
  private ImageStack getImagesStack(Path path, Step step)
  {
    // open imagePLus
    ImagePlus toshowIP = Utils.openImages(path, "", fluobt.getISize());

    // if there is no image in the path, throw exception
    if (toshowIP == null)
    {
      System.err.println(
          "Error in InterfaceControl.getImagesStack : files missing in " + path);
      return null;
    }

    ImageStack toShowIS = toshowIP.getImageStack();
    ArrayList<int[]> back = fluobt.getLastBackground(step);
    setBackgroundWhite(toShowIS, back);

    return toShowIS;
  }

  /**
   * Gives ImageStack of the seeds or blobs at the given 'step' of processing.
   * This should be use for steps computing seeds or blobs (and not background
   * or images).
   *
   * @param path Path were the seeds or blobs chapeSet can be found.
   * @param step step of processing
   * @param title commun name of the shapeSets files
   * @param drawOpt option to draw blobs or connection graph or cells
   * @return
   * @throws InterruptedException
   */
  private ImageStack[] getSeedsOrBlobsStacks(Path path, Step step, String title,
                                             BlobsDrawOpt drawOpt)
  {
    // --- Get last computed images ---
    ImageStack imageStack = fluobt.getLastStack(step);

    ArrayList<int[]> back = fluobt.getLastBackground(step);
    setBackgroundWhite(imageStack, back);

    // --- Get ImageStack from seeds or blobs ---
    ImagePlus blobsOrSeedsIP = fluobt.ImagePlusFromShapeSets(title,
                                                             path,
                                                             fluobt.getISize(),
                                                             drawOpt);

    ImageStack blobsOrSeedsStack;
    if (blobsOrSeedsIP == null)
    {
      blobsOrSeedsStack = null;
    }
    else
    {
      blobsOrSeedsStack = blobsOrSeedsIP.getImageStack();
    }

    ImageStack[] res =
    {
      imageStack, blobsOrSeedsStack
    };
    return res;
  }

  /**
   * Gives ImageStack of the background at the given 'step' of processing. This
   * should be use for steps computing background (and not seeds or blobs or
   * images).
   *
   * @param path Path were the backgrounds can be found.
   * @param step step of processing
   * @param title commun name of the background files
   * @return
   */
  private ImageStack getBackgroundStack(Path path, Step step, String title)
  {
    // -------------------------- Load background ---------------------------
    int size = fluobt.getISize();
    ArrayList<int[]> back = new ArrayList<int[]>(size);

    for (int i = 1; i <= size; i++)
    {
      try
      {
        int[] currBack = (int[]) Utils.loadObject(path.
            resolve(title + "_" + i).toString());
        back.add(i - 1, currBack);
      }
      catch (Exception ex)
      {
        // do nothing
      }

    }

    // --- Get last computed images ---
    ImageStack imageStack = fluobt.getLastStack(step);
    
    if( imageStack.getSize() == 0 )
    {
      return null;
    }

    // ------------- Put in white every pixel in background ---------------
    setBackgroundWhite(imageStack, back);

    // -------------------------- Show ImagePlus --------------------------
    return imageStack;
  }

  /**
   * Gives ImageStack of the blobsolver.
   *
   * @param path Path were the blobsolver can be found.
   * @return
   */
  private ImageStack[] getBlobSolverStacks(Path path)
  {
    ImageStack[] res = new ImageStack[2];

    res[0] = fluobt.getLastStack(Step.BLOBSOLVER);
    setBackgroundWhite(res[0], fluobt.getLastBackground(Step.BLOBSOLVER));

    BlobSolver blobsolver = BlobSolver.loadFiles(path);
    blobsolver.updateDrawingColors();
    res[1] = blobsolver.getLinImg();

    return res;
  }

  private void ChooseBlobsolverParam(BlobSolver blobSolv) throws
      InterruptedException
  {
    // ============== Create a dialog to change options =======================
    GenericDialog dialog = new GenericDialog("Blobsolving Options");
    dialog.addNumericField("Division probability (per sec)",
                           blobSolv.getpDiv(), 3);
    dialog.addNumericField("Maximum distance between two linked cells : ",
                           blobSolv.getMaxDistBetweenLinkedCells(), 3);
    dialog.addNumericField("Log of cell growth rate : ",
                           blobSolv.getGrowRateLog(), 3);
    dialog.addNumericField("Cell growth standard deviation : ",
                           blobSolv.getGrowStdDev(), 3);
    dialog.addNumericField("Orientation rate : ",
                           blobSolv.getOrientationRate(), 3);
    dialog.addNumericField("Moving scale : ",
                           blobSolv.getMovingScale(), 3);
    dialog.addNumericField("Moving rate X : ", blobSolv.getMovingRateX(), 3);
    dialog.addNumericField("Moving rate Y : ", blobSolv.getMovingRateY(), 3);
    dialog.addNumericField("time between two frame (sec) : ",
                           blobSolv.getTimescale(), 3);

    // ============================ Show the Dialog ===========================
    dialog.showDialog();

    // ======== If dialog canceled return without doing anything ==============
    if (dialog.wasCanceled())
    {
      throw new InterruptedException("cancelled");
    }

    // ============== Else get back user new options ==========================
    double newPdiv = dialog.getNextNumber();
    double newMaxDistBtwCells = dialog.getNextNumber();
    double newGrowRateLog = dialog.getNextNumber();
    double newGrowStdDev = dialog.getNextNumber();
    double newOrientationRate = dialog.getNextNumber();
    double newMovingScale = dialog.getNextNumber();
    double newMovingRateX = dialog.getNextNumber();
    double newMovingRateY = dialog.getNextNumber();
    double newTimeScale = dialog.getNextNumber();

    // ==== If invalid numbers are found,  return without doing anything  =====
    if (dialog.invalidNumber())
    {
      IJ.showMessage("Invalid numbers");
      return;
    }

    // ============== Else update getFluobt() attributes with new options  =========
    fluobt.setBlobsolvPdiv(newPdiv);
    fluobt.setBlobsolvMaxDistBtwLinkedCells(newMaxDistBtwCells);
    fluobt.setBlobsolvGrowRateLog(newGrowRateLog);
    fluobt.setBlobsolvGrowStdDev(newGrowStdDev);
    fluobt.setBlobsolvOrientationRate(newOrientationRate);
    fluobt.setBlobsolvMovingScale(newMovingScale);
    fluobt.setBlobsolvMovingRateX(newMovingRateX);
    fluobt.setBlobsolvMovingRateY(newMovingRateY);
    fluobt.setBlobsolvTimescale(newTimeScale);
    blobSolv.init_attributes(fluobt);
  }

}
