package cellst.Main;

import ij.*;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import cellst.DirManagement.*;
import cellst.Enums.BlobsDrawOpt;
import cellst.Enums.Step;
import cellst.Image.*;

/**
 * Main plugin Class.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Fluo_Bac_Tracker extends Observable
{

  // ===========================================================================
  //                             Enums
  // ===========================================================================
  /**
   * Possibles aggregations modes for gtv_mean denoising.
   */
  public enum AggMode
  {

    NONE,
    BETTER_PSNR,
    LESS_ARTEFACT;

    public static String[] getNames()
    {
      String[] res =
      {
        NONE.name(), BETTER_PSNR.name(), LESS_ARTEFACT.name()
      };
      return res;
    }
  };

  // ==========================================================================
  //                      Attributes
  // ==========================================================================
  // --------------------------------------------------------------------------
  //                           Images attributes
  // --------------------------------------------------------------------------
  // -------------- Current stepsList -----------------------------------------
  private Step[] stepsList;

  /**
   * Flag used in interface to know when to update previews.
   */
  protected boolean upToDate = true;

  // ---------------- Do steps flags ------------------------------------------
  /**
   * Do rough background step flag.
   */
  private boolean doRoughB = true;
  /**
   * Do denois step flag.
   */
  private boolean doDenois = true;
  /**
   * Do final background step flag.
   */
  private boolean doFinalB = true;
  /**
   * Do recenter step flag.
   */
  private boolean doRecenter = true;
  /**
   * Do renormalize step flag.
   */
  private boolean doRenorm = true;

  // ---------------- Results files names -------------------------------------
  /**
   * File name to save original image stack.
   */
  public final String ORIG_NAMEFILE = "result_original";
  /**
   * File name to save rough backgrounds.
   */
  public final String ROUGHB_NAMEFILE = "result_roughBack";
  /**
   * File name to save denoised image stack.
   */
  public final String DENOIS_NAMEFILE = "result_denoised";
  /**
   * File name to save final backgrounds.
   */
  public final String BACKGROUND_NAMEFILE = "result_background";
  /**
   * File name to save recentered image stack and background.
   */
  public final String RENORM_NAMEFILE = "result_renormalized";
  /**
   * File name to save recentered image stack.
   */
  public final String RECENTER_NAMEFILE = "result_recentered";
  /**
   * File name to save seeds ShapeSets.
   */
  public final String SEEDS_NAMEFILE = "result_seeds";
  /**
   * File name to save blobs ShapeSets.
   */
  public final String BLOBS_NAMEFILE = "result_blobs";
  /**
   * File name to save final blobs ShapeSets.
   */
  public final String FINALBLOBS_NAMEFILE = "result_finalBlobs";
  /**
   * File name to save BlobSolver.
   */
  public final String BLOBSOLVER_NAMEFILE = "result_blobSolver";
  // ---------------- Temporary Results directories ----------------------------
  protected Path workingDir;

  /**
   * Temporary directory to save original images.
   */
  protected Path origDir;
  /**
   * Temporary directory to save rough Background.
   */
  protected Path roughBDir;
  /**
   * Temporary directory to save denoised images.
   */
  protected Path denoisDir;
  /**
   * Temporary directory to save final background.
   */
  protected Path backgroundDir;
  /**
   * Temporary directory to save recentered images.
   */
  protected Path recenterDir;
  /**
   * Temporary directory to save renormalized images.
   */
  protected Path renormDir;
  /**
   * Temporary directory to save seeds ShapeSets.
   */
  protected Path seedsDir;
  /**
   * Temporary directory to save blobs ShapeSets.
   */
  protected Path blobsDir;
  /**
   * Temporary directory to save final blobs Shapesets.
   */
  protected Path finalBlobsDir;
  /**
   * Temporary directory to save blobsolver.
   */
  protected Path blobsolverDir;
  // ---------------- Results paths --------------------------------------------
  /**
   * Path to save all results.
   */
  protected String pathRes;
  /**
   * Path to save parameters.
   */
  protected String pathParam;
  /**
   * File to save parameters.
   */
  protected String fileParam;
  // ---------------- Images params --------------------------------------------
  /**
   * Images zoom.
   */
  protected double zoom = 0.064;
  /**
   * Images auto-threshold.
   */
  protected double autoThres;
  /**
   * Images width.
   */
  protected int iWidth;
  /**
   * Images height.
   */
  protected int iHeight;
  /**
   * Images slices number.
   */
  protected int iSize;
  /**
   * ShapeSets neighboring connections (4 or 8). True : 8 connected. False 4
   * connected.
   */
  protected boolean conn8 = false;
  // ---------------------------------------------------------------------------
  //                     Cells attributes (in micromètres)
  // ---------------------------------------------------------------------------
  /**
   * Cells minimum area.
   */
  protected double minArea = 1.23;
  /**
   * Cells maximum width.
   */
  protected double maxWidth = 1.28;

  public boolean isUpToDate()
  {
    return upToDate;
  }

  public void setUpToDate(boolean upToDate)
  {
    this.upToDate = upToDate;
  }
  // ---------------------------------------------------------------------------
  //                     Denoising step Options
  // ---------------------------------------------------------------------------
  /**
   * rough background threshold parameter.
   */
  protected double roughBThres = 4.5;
  /**
   * rough Background radius of closing and dilating.
   */
  protected double roughBRadius = 0.320;
  /**
   * Denoising level parameter.
   */
  protected double denoiseLevel = 20.D;
  /**
   * Denoising reduction parameter.
   */
  protected double denoiseReduc = 0.1D;
  /**
   * Denoising patchs number parameter.
   */
  protected int denoiseNbPatch = 5;
  /**
   * Denoising patch size parameter (pixels).
   */
  protected int denoisePatchSize = 25;
  /**
   * Denoising maximum distance parameter (pixels).
   */
  protected double denoiseMaxDist = 10;
  /**
   * Denoising epsilon parameter.
   */
  protected double denoiseEps = 0.01D;
  /**
   * Denoising aggregation mode parameter.
   */
  protected AggMode denoiseMode = AggMode.BETTER_PSNR;
  // ---------------------------------------------------------------------------
  //                     Renormalization step Options
  // ---------------------------------------------------------------------------
  /**
   * Final background threshold parameter.
   */
  protected double backThres = 4.5;
  /**
   * Renormalization threshold parameter.
   */
  protected double renormThres = 4.5;
  /**
   * Renormalization radius parameter.
   */
  protected double renormRadius = 0.576;
  /**
   * Renormalization convolution square size parameter.
   */
  protected double renormSqSize = 1.28;
  /**
   * Renormalization interations number parameter.
   */
  protected int renormIter = 3;
  // ---------------------------------------------------------------------------
  //                     Seeds step Options
  // ---------------------------------------------------------------------------
  /**
   * Seeds threshold parameter.
   */
  protected double seedsThres = 20;
  // ---------------------------------------------------------------------------
  //                     Dilate step Options
  // ---------------------------------------------------------------------------
  /**
   * Speed first parameter (a) : 1.0 + exp( a * ( grey level - b ) ).
   */
  protected double speedA = 1;
  /**
   * Speed first parameter (a) : 1.0 + exp( a * ( grey level - b ) ).
   */
  protected double speedB = 155.;
  /**
   * Dilatation iterations number parameter.
   */
  protected int dilateIter = 6;
  // ---------------------------------------------------------------------------
  //                     Cleaning step Options
  // ---------------------------------------------------------------------------
  /**
   * Minimum area of a blob.
   */
  protected double minBlobArea = minArea / 3;
  /**
   * Minimum length of the border between two blobs to consider them neighbors.
   */
  protected double minBlobBorder = maxWidth / 3;
  /**
   * Cleaning iterations number parameters.
   */
  protected int cleanIter = 2;
  // ---------------------------------------------------------------------------
  //                     Gencells and Blobsolv step Options
  // ---------------------------------------------------------------------------
  /**
   * Maximum number of blobs a cell can contain.
   */
  protected int maxNbBlobs = 7;
  /**
   * Gencells and blobsolver button
   */
  protected Button buGenCell;

  /**
   * Probability for a cell to divide between two images.
   */
  private double blobsolvPdiv = 0.059;
  /**
   * Maximum distance between center points of two linked (mother/daughter) cell
   * in images.
   */
  private double blobsolvMaxDistBtwLinkedCells = 30;
  /**
   * Log of cell growth rate.
   */
  private double blobsolvGrowRateLog = 0.06;

  /**
   * Cell growth standard deviation.
   */
  private double blobsolvGrowStdDev = 0.04;

  /**
   * Orientation parameter.
   */
  private double blobsolvOrientationRate = 30;

  /**
   * Horizontal moving parameter.
   */
  private double blobsolvMovingRateX = 0.02;

  /**
   * Vertical moving parameter.
   */
  private double blobsolvMovingRateY = 0.0;

  /**
   * Moving scale parameter.
   */
  private double blobsolvMovingScale = 0.02;

  /**
   * Movie time scale.
   */
  private double blobsolvTimescale = 1.;

  // ===========================================================================
  //                              Constructor
  // ===========================================================================
  /**
   * Fluo_Bac_Tracker constructor. Initialize all GUI.
   *
   */
  public Fluo_Bac_Tracker(Path _workingDir)
  {
    // Call PlugInFrame constructor
    super();

    // ============ Initialize temporary directories ===========================
    workingDir = _workingDir;
    try
    {
      origDir = Files.createTempDirectory(_workingDir, ORIG_NAMEFILE);
      roughBDir = Files.createTempDirectory(_workingDir, ROUGHB_NAMEFILE);
      denoisDir = Files.createTempDirectory(_workingDir, DENOIS_NAMEFILE);
      backgroundDir = Files.
          createTempDirectory(_workingDir, BACKGROUND_NAMEFILE);
      recenterDir = Files.createTempDirectory(_workingDir, RECENTER_NAMEFILE);
      renormDir = Files.createTempDirectory(_workingDir, RENORM_NAMEFILE);
      seedsDir = Files.createTempDirectory(_workingDir, SEEDS_NAMEFILE);
      blobsDir = Files.createTempDirectory(_workingDir, BLOBS_NAMEFILE);
      finalBlobsDir = Files.
          createTempDirectory(_workingDir, FINALBLOBS_NAMEFILE);
      blobsolverDir = Files.
          createTempDirectory(_workingDir, BLOBSOLVER_NAMEFILE);
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      System.exit(0);
    }

    // ========= Add a shutdown hook to delete temp directories on exit ========
    Runtime.getRuntime().addShutdownHook(new HookThr(this));

    // ============ Init stepsList to avoid null exceptions ===================
    stepsList = null;
  }

  /**
   * Fluo_Bac_Tracker constructor. Initialize all GUI.
   *
   */
  public Fluo_Bac_Tracker()
  {
    // Call PlugInFrame constructor
    super();

    // ============ Initialize temporary directories ===========================
    workingDir = Paths.get(System.getProperty("java.io.tmpdir"));
    try
    {
      origDir = Files.createTempDirectory(workingDir, ORIG_NAMEFILE);
      roughBDir = Files.createTempDirectory(workingDir, ROUGHB_NAMEFILE);
      denoisDir = Files.createTempDirectory(workingDir, DENOIS_NAMEFILE);
      backgroundDir = Files.createTempDirectory(workingDir, BACKGROUND_NAMEFILE);
      recenterDir = Files.createTempDirectory(workingDir, RECENTER_NAMEFILE);
      renormDir = Files.createTempDirectory(workingDir, RENORM_NAMEFILE);
      seedsDir = Files.createTempDirectory(workingDir, SEEDS_NAMEFILE);
      blobsDir = Files.createTempDirectory(workingDir, BLOBS_NAMEFILE);
      finalBlobsDir = Files.createTempDirectory(workingDir, FINALBLOBS_NAMEFILE);
      blobsolverDir = Files.createTempDirectory(workingDir, BLOBSOLVER_NAMEFILE);
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      System.exit(0);
    }

    // ========= Add a shutdown hook to delete temp directories on exit ========
    Runtime.getRuntime().addShutdownHook(new HookThr(this));
  }

  // ==========================================================================
  //                                   Getters 
  // ==========================================================================
  /**
   * Working directory getter.
   *
   * @return wworking directory.
   */
  public Path getWorkingDir()
  {
    return workingDir;
  }
  
  public Step[] getStepsList()
  {
    return stepsList;
  }

  public Step getStep(int slice)
  {
    return stepsList[slice - 1];
  }

  /**
   * @return the doRoughB
   */
  public boolean isDoRoughB()
  {
    return doRoughB;
  }

  /**
   * @return the doDenois
   */
  public boolean isDoDenois()
  {
    return doDenois;
  }

  /**
   * @return the doFinalB
   */
  public boolean isDoFinalB()
  {
    return doFinalB;
  }

  /**
   * @return the doRecenter
   */
  public boolean isDoRecenter()
  {
    return doRecenter;
  }

  /**
   * @return the doRenorm
   */
  public boolean isDoRenorm()
  {
    return doRenorm;
  }

  // --------------------- Temporary directories ------------------------------
  /**
   * Returns temporary directory where original images are stored.
   */
  public Path getOrigDir()
  {
    return origDir;
  }

  /**
   * Returns temporary directory where rough backgrounds are stored.
   */
  public Path getRoughBDir()
  {
    return roughBDir;
  }

  /**
   * Returns temporary directory where denoised images are stored.
   */
  public Path getDenoisDir()
  {
    return denoisDir;
  }

  /**
   * Returns temporary directory where final backgrounds are stored.
   */
  public Path getBackgroundDir()
  {
    return backgroundDir;
  }

  /**
   * Returns temporary directory where recentered images are stored.
   */
  public Path getRecenterDir()
  {
    return recenterDir;
  }

  /**
   * Returns temporary directory where renormalized images are stored.
   */
  public Path getRenormDir()
  {
    return renormDir;
  }

  /**
   * Returns temporary directory where seeds ShapeSets are stored.
   */
  public Path getSeedsDir()
  {
    return seedsDir;
  }

  /**
   * Returns temporary directory where blobs ShapeSets are stored.
   */
  public Path getBlobsDir()
  {
    return blobsDir;
  }

  /**
   * Returns temporary directory where final blobs ShapeSets are stored.
   */
  public Path getfinalBlobsDir()
  {
    return finalBlobsDir;
  }

  /**
   * Returns temporary directory where blobsolver is stored.
   */
  public Path getblobSolvDir()
  {
    return blobsolverDir;
  }

  // --------------------- Images getters -------------------------------------
  /**
   * Image zoom getter.
   *
   * @return image zoom.
   */
  public double getZoom()
  {
    return zoom;
  }

  /**
   * Results path getter.
   *
   * @return results path.
   */
  public String getPathRes()
  {
    return pathRes;
  }

  /**
   * Parameters path getter.
   *
   * @return Parameters path.
   */
  public String getPathParam()
  {
    return pathParam;
  }

  /**
   * Parameters file getter.
   *
   * @return Parameters file.
   */
  public String getFileParam()
  {
    return fileParam;
  }

  /**
   * Auto computed threshold getter.
   *
   * @return image threshold.
   */
  public double getThres()
  {
    return autoThres;
  }

  /**
   * Image width getter.
   *
   * @return Image width.
   */
  public int getIWidth()
  {
    return iWidth;
  }

  /**
   * Image height getter.
   *
   * @return Image height.
   */
  public int getIHeight()
  {
    return iHeight;
  }

  /**
   * Image slices number getter.
   *
   * @return Image slices number.
   */
  public int getISize()
  {
    return iSize;
  }

  /**
   * ShapeSet connection (4 or 8) getter.
   *
   * @return Connection type.
   */
  public boolean getConn8()
  {
    return conn8;
  }

  // --------------------- Cell getters ---------------------------------------
  /**
   * Cells minimum area getter.
   *
   * @return cells minimum area.
   */
  public double getMinArea()
  {
    return minArea;
  }

  /**
   * Cells maximum width getter.
   *
   * @return cells maximum width.
   */
  public double getMaxWidth()
  {
    return maxWidth;
  }

  // --------------------- Denoising getters ----------------------------------
  /**
   * Rough background threshold getter.
   *
   * @return rough background threshold.
   */
  public double getRoughBThres()
  {
    return roughBThres;
  }

  /**
   * Rough background radius getter.
   *
   * @return rough background radius.
   */
  public double getRoughBRadius()
  {
    return roughBRadius;
  }

  /**
   * Denoising level parameter getter.
   *
   * @return denoising level parameter.
   */
  public double getDenoiseLevel()
  {
    return denoiseLevel;
  }

  /**
   * Denoising reduction parameter getter.
   *
   * @return denoising reduction parameter.
   */
  public double getDenoiseReduc()
  {
    return denoiseReduc;
  }

  /**
   * Denoising patchs number parameter getter.
   *
   * @return denoising patchs number parameter.
   */
  public int getDenoiseNbPatch()
  {
    return denoiseNbPatch;
  }

  /**
   * Denoising patch size parameter getter.
   *
   * @return denoising patch size parameter.
   */
  public double getDenoisePatchSize()
  {
    return denoisePatchSize;
  }

  /**
   * Denoising maximum distance parameter getter.
   *
   * @return denoising maximum distance parameter.
   */
  public double getDenoiseMaxDist()
  {
    return denoiseMaxDist;
  }

  /**
   * Denoising epsilon parameter getter.
   *
   * @return denoising epsilon parameter.
   */
  public double getDenoiseEps()
  {
    return denoiseEps;
  }

  /**
   * Denoising aggregation mode parameter getter.
   *
   * @return denoising aggregation mode parameter.
   */
  public AggMode getDenoiseMode()
  {
    return denoiseMode;
  }

  // --------------------- Renormalization getters ----------------------------
  /**
   * Final Background threshold getter.
   *
   * @return final background threshold.
   */
  public double getBackThres()
  {
    return backThres;
  }

  /**
   * Renormalization radius getter.
   *
   * @return renormalization radius.
   */
  public double getRenormRadius()
  {
    return renormRadius;
  }

  /**
   * Renormalization convolution square size getter.
   *
   * @return renormalization convolution square size.
   */
  public double getRenormSqSize()
  {
    return renormSqSize;
  }

  /**
   * Renormalization iterations number getter.
   *
   * @return renormalization iterations number.
   */
  public int getRenormIter()
  {
    return renormIter;
  }

  /**
   * Renormalization threshold getter.
   *
   * @return renormalization threshold.
   */
  public double getRenormThres()
  {
    return renormThres;
  }

  // --------------------- Seeds getters ----------------------------
  /**
   * Seeds threshold getter.
   *
   * @return seeds threshold.
   */
  public double getSeedsThres()
  {
    return seedsThres;
  }

  // --------------------- Dilatation getters ---------------------------------
  /**
   * Speed first parameter getter.
   *
   * @return speed first parameter.
   */
  public double getSpeedA()
  {
    return speedA;
  }

  /**
   * Speed second parameter getter.
   *
   * @return speed second parameter.
   */
  public double getSpeedB()
  {
    return speedB;
  }

  /**
   * Dilatation iterations number getter.
   *
   * @return dilatation iterations number.
   */
  public int getDilateIter()
  {
    return dilateIter;
  }

  // --------------------- Cleaning getters -----------------------------------
  /**
   * Minimum blob area getter.
   *
   * @return minimum blob area.
   */
  public double getMinBlobArea()
  {
    return minBlobArea;
  }

  /**
   * Minimum border length between two neighboring blobs getter.
   *
   * @return minimum neighboring blobs border length.
   */
  public double getMinBlobBorder()
  {
    return minBlobBorder;
  }

  /**
   * Cleaning iterations number getter.
   *
   * @return cleaning iterations number.
   */
  public int getCleanIter()
  {
    return cleanIter;
  }

  // --------------- Gencells and Blobsolver getters --------------------------
  /**
   * Maximum number of blobs per cell getter.
   *
   * @return maximum blobs number.
   */
  public int getMaxNbBlobs()
  {
    return maxNbBlobs;
  }

  /**
   * Gencell and blobsolver button getter.
   *
   * @return buGenCell.
   */
  public Button getBuGenCell()
  {
    return buGenCell;
  }

  /**
   * @return the blobsolvPdiv
   */
  public double getBlobsolvPdiv()
  {
    return blobsolvPdiv;
  }

  /**
   * @return the blobsolvMaxDistBtwLinkedCells
   */
  public double getBlobsolvMaxDistBtwLinkedCells()
  {
    return blobsolvMaxDistBtwLinkedCells;
  }

  /**
   * @return the blobsolvGrowRateLog
   */
  public double getBlobsolvGrowRateLog()
  {
    return blobsolvGrowRateLog;
  }

  /**
   * @return the blobsolvGrowStdDev
   */
  public double getBlobsolvGrowStdDev()
  {
    return blobsolvGrowStdDev;
  }

  /**
   * @return the blobsolvOrientationRate
   */
  public double getBlobsolvOrientationRate()
  {
    return blobsolvOrientationRate;
  }

  /**
   * @return the blobsolvMovingRateX
   */
  public double getBlobsolvMovingRateX()
  {
    return blobsolvMovingRateX;
  }

  /**
   * @return the blobsolvMovingRateY
   */
  public double getBlobsolvMovingRateY()
  {
    return blobsolvMovingRateY;
  }

  /**
   * @return the blobsolvMovingScale
   */
  public double getBlobsolvMovingScale()
  {
    return blobsolvMovingScale;
  }

  /**
   * @return the blobsolvTimescale
   */
  public double getBlobsolvTimescale()
  {
    return blobsolvTimescale;
  }

  // ==========================================================================
  //                                   Setters
  // ==========================================================================
  // -------------------- StepsList -------------------------------------------
  public void setStep(int slice, Step step)
  {
    stepsList[slice - 1] = step;
    setChanged();
    notifyObservers(step);
  }

  /**
   * @param doRoughB the doRoughB to set
   */
  public void setDoRoughB(boolean doRoughB)
  {
    this.doRoughB = doRoughB;
  }

  /**
   * @param doDenois the doDenois to set
   */
  public void setDoDenois(boolean doDenois)
  {
    this.doDenois = doDenois;
  }

  /**
   * @param doFinalB the doFinalB to set
   */
  public void setDoFinalB(boolean doFinalB)
  {
    this.doFinalB = doFinalB;
  }

  /**
   * @param doRecenter the doRecenter to set
   */
  public void setDoRecenter(boolean doRecenter)
  {
    this.doRecenter = doRecenter;
  }

  /**
   * @param doRenorm the doRenorm to set
   */
  public void setDoRenorm(boolean doRenorm)
  {
    this.doRenorm = doRenorm;
  }

  // --------------------- Images setters -------------------------------------
  /**
   * Images zoom setter.
   *
   * @param zoo new zoom value.
   */
  public void setZoom(double zoo)
  {
    zoom = zoo;
  }

  /**
   * Path to save results setter.
   *
   * @param path new saving path.
   */
  public void setPathRes(String path)
  {
    pathRes = path;
  }

  /**
   * Path to save parameters setter.
   *
   * @param path new saving path.
   */
  public void setPathParam(String path)
  {
    pathParam = path;
  }

  /**
   * File to save parameters setter.
   *
   * @param file new saving file.
   */
  public void setFileParam(String file)
  {
    fileParam = file;
  }

  /**
   * Images auto-threshold setter.
   *
   * @param _thres new threshold.
   */
  public void setThres(double _thres)
  {
    autoThres = _thres;
  }

  /**
   * Images width setter.
   *
   * @param _w new width.
   */
  public void setIWidth(int _w)
  {
    iWidth = _w;
  }

  /**
   * Images height setter.
   *
   * @param _h new height.
   */
  public void setIHeight(int _h)
  {
    iHeight = _h;
  }

  /**
   * Images slices number setter.
   *
   * @param _s new slices number.
   */
  public void setISize(int _s)
  {
    iSize = _s;
  }

  /**
   * ShapeSets neighboring connection type (4 or 8 connected). True : 8
   * connected. False 4 connected.
   *
   * @param _conn new connection type.
   */
  public void setConn8(boolean _conn)
  {
    conn8 = _conn;
  }

  // --------------------- Cell setters ---------------------------------------
  /**
   * Cells minimum area setter.
   *
   * @param minA new minimum area.
   */
  public void setMinArea(double minA)
  {
    minArea = minA;
  }

  /**
   * Cells maximum width setter.
   *
   * @param maxW new maximum width.
   */
  public void setMaxWidth(double maxW)
  {
    maxWidth = maxW;
  }

  // --------------------- Denoising setters -----------------------------------
  /**
   * Rough background threshold setter.
   *
   * @param _roughThres new rough background threshold.
   */
  public void setRoughBThres(double _roughThres)
  {
    roughBThres = _roughThres;
  }

  /**
   * Rough background radius setter.
   *
   * @param _roughRad new rough background radius.
   */
  public void setRoughBRadius(double _roughRad)
  {
    roughBRadius = _roughRad;
  }

  /**
   * Denoising level parameter setter.
   *
   * @param _level new denoising level parameter.
   */
  public void setDenoiseLevel(double _level)
  {
    denoiseLevel = _level;
  }

  /**
   * Denoising reduction parameter setter.
   *
   * @param _reduc new denoising reduction parameter.
   */
  public void setDenoiseReduc(double _reduc)
  {
    denoiseReduc = _reduc;
  }

  /**
   * Denoising patchs number parameter setter.
   *
   * @param _nbPatch new denoising patchs number parameter.
   */
  public void setDenoiseNbPatch(int _nbPatch)
  {
    denoiseNbPatch = _nbPatch;
  }

  /**
   * Denoising patchs size parameter setter.
   *
   * @param _patchSize new denoising patch size parameter.
   */
  public void setDenoisePatchSize(int _patchSize)
  {
    denoisePatchSize = _patchSize;
  }

  /**
   * Denoising maximum distance parameter setter.
   *
   * @param _maxDist new denoising maximum distance parameter.
   */
  public void setDenoiseMaxDist(double _maxDist)
  {
    denoiseMaxDist = _maxDist;
  }

  /**
   * Denoising epsilon parameter setter.
   *
   * @param _eps new denoising epsilon parameter.
   */
  public void setDenoiseEps(double _eps)
  {
    denoiseEps = _eps;
  }

  /**
   * Denoising aggregation mode parameter setter.
   *
   * @param _mode new denoising aggregation mode parameter.
   */
  public void setDenoiseMode(AggMode _mode)
  {
    denoiseMode = _mode;
  }

  // --------------------- Renormalization setters ----------------------------
  /**
   * Final background threshold setter.
   *
   * @param _thres new final background threshold.
   */
  public void setBackThres(double _thres)
  {
    backThres = _thres;
  }

  /**
   * Renormalization radius setter.
   *
   * @param _rad new renormalization radius.
   */
  public void setRenormRadius(double _rad)
  {
    renormRadius = _rad;
  }

  /**
   * Renormalization convolution square size setter.
   *
   * @param _size new renormalization convolution square size.
   */
  public void setRenormSqSize(double _size)
  {
    renormSqSize = _size;
  }

  /**
   * Renormalization interations number setter.
   *
   * @param _iter new renormalization iterations number.
   */
  public void setRenormIter(int _iter)
  {
    renormIter = _iter;
  }

  /**
   * Renormalization threshold setter.
   *
   * @param _thres new renormalization threshold.
   */
  public void setRenormThres(double _thres)
  {
    renormThres = _thres;
  }

  // --------------------- Seeds setters --------------------------------------
  /**
   * Seeds threshold setter.
   *
   * @param _thres new seeds threshold.
   */
  public void setSeedsThres(double _thres)
  {
    seedsThres = _thres;
  }

  // --------------------- Dilatation setters ---------------------------------
  /**
   * Speed first parameter setter.
   *
   * @param _speed new speed first parameter.
   */
  public void setSpeedA(double _speed)
  {
    speedA = _speed;
  }

  /**
   * Speed second parameter setter.
   *
   * @param _speed new speed second parameter.
   */
  public void setSpeedB(double _speed)
  {
    speedB = _speed;
  }

  /**
   * Dilatation iterations number setter.
   *
   * @param _iter new dilatation iterations number.
   */
  public void setDilateIter(int _iter)
  {
    dilateIter = _iter;
  }

  // --------------------- Cleaning setters -----------------------------------
  /**
   * Minimum blob area setter.
   *
   * @param _area new minimum blob area.
   */
  public void setMinBlobArea(double _area)
  {
    minBlobArea = _area;
  }

  /**
   * Minimum neighboring blobs border length setter.
   *
   * @param _bord new minimum neighboring blobs border length.
   */
  public void setMinBlobBorder(double _bord)
  {
    minBlobBorder = _bord;
  }

  /**
   * Cleaning iterations number setter.
   *
   * @param _iter new cleaning iterations number.
   */
  public void setCleanIter(int _iter)
  {
    cleanIter = _iter;
  }

  // --------------- Gencells and Blobsolver setters --------------------------
  /**
   * Maximum number of blobs per cell setter.
   *
   * @param _maxBlobs new maximum blobs number.
   */
  public void setMaxNbBlobs(int _maxBlobs)
  {
    maxNbBlobs = _maxBlobs;
  }

  /**
   * @param blobsolvPdiv the blobsolvPdiv to set
   */
  public void setBlobsolvPdiv(double blobsolvPdiv)
  {
    this.blobsolvPdiv = blobsolvPdiv;
  }

  /**
   * @param blobsolvMaxDistBtwLinkedCells the blobsolvMaxDistBtwLinkedCells to set
   */
  public void setBlobsolvMaxDistBtwLinkedCells(
      double blobsolvMaxDistBtwLinkedCells)
  {
    this.blobsolvMaxDistBtwLinkedCells = blobsolvMaxDistBtwLinkedCells;
  }

  /**
   * @param blobsolvGrowRateLog the blobsolvGrowRateLog to set
   */
  public void setBlobsolvGrowRateLog(double blobsolvGrowRateLog)
  {
    this.blobsolvGrowRateLog = blobsolvGrowRateLog;
  }

  /**
   * @param blobsolvGrowStdDev the blobsolvGrowStdDev to set
   */
  public void setBlobsolvGrowStdDev(double blobsolvGrowStdDev)
  {
    this.blobsolvGrowStdDev = blobsolvGrowStdDev;
  }

  /**
   * @param blobsolvOrientationRate the blobsolvOrientationRate to set
   */
  public void setBlobsolvOrientationRate(double blobsolvOrientationRate)
  {
    this.blobsolvOrientationRate = blobsolvOrientationRate;
  }

  /**
   * @param blobsolvMovingRateX the blobsolvMovingRateX to set
   */
  public void setBlobsolvMovingRateX(double blobsolvMovingRateX)
  {
    this.blobsolvMovingRateX = blobsolvMovingRateX;
  }

  /**
   * @param blobsolvMovingRateY the blobsolvMovingRateY to set
   */
  public void setBlobsolvMovingRateY(double blobsolvMovingRateY)
  {
    this.blobsolvMovingRateY = blobsolvMovingRateY;
  }

  /**
   * @param blobsolvMovingScale the blobsolvMovingScale to set
   */
  public void setBlobsolvMovingScale(double blobsolvMovingScale)
  {
    this.blobsolvMovingScale = blobsolvMovingScale;
  }

  /**
   * @param blobsolvTimescale the blobsolvTimescale to set
   */
  public void setBlobsolvTimescale(double blobsolvTimescale)
  {
    this.blobsolvTimescale = blobsolvTimescale;
  }

  // ===========================================================================
  //                      Public methods
  // ===========================================================================
  // --------------------------------------------------------------------------
  //                          Save and load Parameters
  // --------------------------------------------------------------------------
  /**
   * Saving parameters method. All plugin parameters are saved in a file.
   *
   * @param Path file where to save the parameters.
   */
  public void saveParam(String Path)
  {
    Object[] list =
    {
      zoom,
      // Cells attributes (in micromètres)
      minArea,
      maxWidth,
      // skipping steps,
      doRoughB,
      doDenois,
      doFinalB,
      doRecenter,
      doRenorm,
      // Denoising step Options
      roughBThres,
      denoiseLevel,
      denoiseReduc,
      denoiseNbPatch,
      denoisePatchSize,
      denoiseMaxDist,
      denoiseEps,
      denoiseMode,
      // Renormalization step Options
      backThres,
      renormThres,
      renormRadius,
      renormSqSize,
      renormIter,
      // Seeds step Options
      seedsThres,
      // Dilate step SeedsOptions
      speedA,
      speedB,
      dilateIter,
      // Cleaning step SeedsOptions
      minBlobArea,
      minBlobBorder,
      cleanIter
    };

    Utils.saveObject(list, Path);
  }

  /**
   * Load parameters from a file.
   *
   * @param Path loading parameters file.
   */
  public void loadParam(String Path)
  {
    Object[] list = (Object[]) Utils.loadObject(Path);

    int i = 0;
    setZoom((Double) list[ i++]);

    // Cells attributes (in micromètres)
    setMinArea((Double) list[ i++]);
    setMaxWidth((Double) list[ i++]);

    // Skipping steps 
    doRoughB = (Boolean) list[ i++];
    doDenois = (Boolean) list[ i++];
    doFinalB = (Boolean) list[ i++];
    doRecenter = (Boolean) list[ i++];
    doRenorm = (Boolean) list[ i++];
    
    // Denoising step Options
    setRoughBThres((Double) list[ i++]);
    setDenoiseLevel((Double) list[ i++]);
    setDenoiseReduc((Double) list[ i++]);
    setDenoiseNbPatch((Integer) list[ i++]);
    setDenoisePatchSize((Integer) list[ i++]);
    setDenoiseMaxDist((Double) list[ i++]);
    setDenoiseEps((Double) list[ i++]);
    setDenoiseMode((AggMode) list[ i++]);

    // Renormalization step Options
    setBackThres((Double) list[ i++]);
    setRenormThres((Double) list[ i++]);
    setRenormRadius((Double) list[ i++]);
    setRenormSqSize((Double) list[ i++]);
    setRenormIter((Integer) list[ i++]);

    // Seeds step Options
    setSeedsThres((Double) list[ i++]);

    // Dilate step SeedsOptions
    setSpeedA((Double) list[ i++]);
    setSpeedB((Double) list[ i++]);
    setDilateIter((Integer) list[ i++]);

    // Cleaning step SeedsOptions
    setMinBlobArea((Double) list[ i++]);
    setMinBlobBorder((Double) list[ i++]);
    setCleanIter((Integer) list[ i++]);

  }

  // --------------------------------------------------------------------------
  //                          Save and load Results
  // --------------------------------------------------------------------------
  /**
   * Saves all results and parameters into 'path'.
   *
   * @param path Path where to save results.
   */
  public void save(String path)
  {
    // ============ Create results directory if needed ========================
    Path resultDir = Paths.get(path);
    try
    {
      Files.createDirectories(resultDir);
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // ============ Intitiate subdirectories paths  ===========================
    Path origPath = resultDir.resolve(ORIG_NAMEFILE);
    Path roughBPath = resultDir.resolve(ROUGHB_NAMEFILE);
    Path denoisPath = resultDir.resolve(DENOIS_NAMEFILE);
    Path backgroundPath = resultDir.resolve(BACKGROUND_NAMEFILE);
    Path recenterPath = resultDir.resolve(RECENTER_NAMEFILE);
    Path renormPath = resultDir.resolve(RENORM_NAMEFILE);
    Path seedsPath = resultDir.resolve(SEEDS_NAMEFILE);
    Path blobsPath = resultDir.resolve(BLOBS_NAMEFILE);
    Path finalBlobsPath = resultDir.resolve(FINALBLOBS_NAMEFILE);
    Path blobsolverPath = resultDir.resolve(BLOBSOLVER_NAMEFILE);

    // ============= Delete results that could have been there ================
    try
    {
      if (Files.exists(origPath))
      {
        Files.walkFileTree(origPath, new VisitorClear());
      }

      if (Files.exists(roughBPath))
      {
        Files.walkFileTree(roughBPath, new VisitorClear());
      }

      if (Files.exists(denoisPath))
      {
        Files.walkFileTree(denoisPath, new VisitorClear());
      }

      if (Files.exists(backgroundPath))
      {
        Files.walkFileTree(backgroundPath, new VisitorClear());
      }

      if (Files.exists(recenterPath))
      {
        Files.walkFileTree(recenterPath, new VisitorClear());
      }

      if (Files.exists(renormPath))
      {
        Files.walkFileTree(renormPath, new VisitorClear());
      }

      if (Files.exists(seedsPath))
      {
        Files.walkFileTree(seedsPath, new VisitorClear());
      }

      if (Files.exists(blobsPath))
      {
        Files.walkFileTree(blobsPath, new VisitorClear());
      }

      if (Files.exists(finalBlobsPath))
      {
        Files.walkFileTree(finalBlobsPath, new VisitorClear());
      }

      if (Files.exists(blobsolverPath))
      {
        Files.walkFileTree(blobsolverPath, new VisitorClear());
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // ============= Copy results from temporary files ========================
    try
    {
      Files.walkFileTree(origDir, new VisitorCopy(origDir, origPath));
      Files.walkFileTree(roughBDir, new VisitorCopy(roughBDir, roughBPath));
      Files.walkFileTree(denoisDir, new VisitorCopy(denoisDir, denoisPath));
      Files.walkFileTree(backgroundDir, new VisitorCopy(backgroundDir,
                                                        backgroundPath));
      Files.
          walkFileTree(recenterDir, new VisitorCopy(recenterDir, recenterPath));
      Files.walkFileTree(renormDir, new VisitorCopy(renormDir, renormPath));
      Files.walkFileTree(seedsDir, new VisitorCopy(seedsDir, seedsPath));
      Files.walkFileTree(blobsDir, new VisitorCopy(blobsDir, blobsPath));
      Files.walkFileTree(finalBlobsDir, new VisitorCopy(finalBlobsDir,
                                                        finalBlobsPath));
      Files.walkFileTree(blobsolverDir, new VisitorCopy(blobsolverDir,
                                                        blobsolverPath));
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // ============= Save parameters ==========================================
    saveParam(path + "/.param");
  }

  /**
   * Loads results and parameters from 'path'.
   *
   * @param dir Path to load results.
   */
  public void load(Path dir)
  {
    // ===== Check that all loading results directories exist =================

    // ------------ Intitiate subdirectories paths  ---------------------------
    Path origPath = dir.resolve(ORIG_NAMEFILE);
    Path roughBPath = dir.resolve(ROUGHB_NAMEFILE);
    Path denoisPath = dir.resolve(DENOIS_NAMEFILE);
    Path backgroundPath = dir.resolve(BACKGROUND_NAMEFILE);
    Path recenterPath = dir.resolve(RECENTER_NAMEFILE);
    Path renormPath = dir.resolve(RENORM_NAMEFILE);
    Path seedsPath = dir.resolve(SEEDS_NAMEFILE);
    Path blobsPath = dir.resolve(BLOBS_NAMEFILE);
    Path finalBlobsPath = dir.resolve(FINALBLOBS_NAMEFILE);
    Path blobsolverPath = dir.resolve(BLOBSOLVER_NAMEFILE);
    Path paramPath = dir.resolve(".param");

    // ----- Check that all directories exist ---------------------------------
    if (!(Files.exists(dir)
          && Files.exists(origPath)
          && Files.exists(roughBPath)
          && Files.exists(denoisPath)
          && Files.exists(backgroundPath)
          && Files.exists(recenterPath)
          && Files.exists(renormPath)
          && Files.exists(seedsPath)
          && Files.exists(blobsPath)
          && Files.exists(finalBlobsPath)
          && Files.exists(blobsolverPath)
          && Files.exists(paramPath)))
    {
      System.out.println(
          "Error in Fluo_Bac_Tracker.load : One or several results directories missing.");
      return;
    }

    // ============ Set results path to new directory =========================
    pathRes = dir.toString();

    // ================ COPY RESULTS IN TEMP DIR ==============================
    // -------- Delete precedents result from temporary directories. ----------
    clearTemporaryFiles();

    // ------------- Copy results to temporary files ------------------------
    try
    {
      Files.walkFileTree(origPath, new VisitorCopy(origPath, origDir));
      Files.walkFileTree(roughBPath, new VisitorCopy(roughBPath, roughBDir));
      Files.walkFileTree(denoisPath, new VisitorCopy(denoisPath, denoisDir));
      Files.walkFileTree(backgroundPath, new VisitorCopy(backgroundPath,
                                                         backgroundDir));
      Files.walkFileTree(recenterPath,
                         new VisitorCopy(recenterPath, recenterDir));
      Files.walkFileTree(renormPath, new VisitorCopy(renormPath, renormDir));
      Files.walkFileTree(seedsPath, new VisitorCopy(seedsPath, seedsDir));
      Files.walkFileTree(blobsPath, new VisitorCopy(blobsPath, blobsDir));
      Files.walkFileTree(finalBlobsPath, new VisitorCopy(finalBlobsPath,
                                                         finalBlobsDir));
      Files.walkFileTree(blobsolverPath, new VisitorCopy(blobsolverPath,
                                                         blobsolverDir));
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // ------------- Load parameters ------------------------------------------
    loadParam(paramPath.toString());

    // =============== Update Progression and image parameters ================
    String[] files = origDir.toFile().list();

    int filesNb = files.length;
    if (filesNb % 2 == 1)
    {
      System.out.println(
          " Error in Fluo_Bac_Tracker.load : unpair number of files in origDir");
      return;
    }

    iSize = filesNb / 2;

    ImageFbt iFbt = null;
    for (int i = 0; i < filesNb; i++)
    {
      Path path = origDir.resolve(files[i]);

      if (path.getFileName().toString().replaceAll("[0-9]", "").equals("slice_"))
      {
        iFbt = ImageFbt.load(path);
        break;
      }
    }

    if (iFbt == null)
    {
      System.out.println(" Error in Fluo_Bac_Tracker.load : no original images");
      return;
    }

    iHeight = iFbt.getHeight();
    iWidth = iFbt.getWidth();

    updateStepsListFromFiles();
  }

  // --------------------------------------------------------------------------
  //     Check if last results are saved
  // --------------------------------------------------------------------------
  /**
   * Checks if last results computed have been saved.
   *
   * @return true : last results were saved, false : there is unsaved results.
   */
  public boolean isSaved()
  {
    boolean saved = true;

    // ================ If there is no results path ==========================
    // If there is no images loaded yet, then there is nothing to save : return true.
    // Else, if there are files to save, return false.
    if (pathRes == null)
    {
      File origDirFile = origDir.toFile();
      int filesNb = origDirFile.list().length;

      return filesNb == 0;
    }

    // ========== Init list of the temporary directories =====================
    Path resultsPath = Paths.get(pathRes);

    Path[] resultsDir = new Path[10];
    resultsDir[0] = resultsPath.resolve(ORIG_NAMEFILE);
    resultsDir[1] = resultsPath.resolve(ROUGHB_NAMEFILE);
    resultsDir[2] = resultsPath.resolve(DENOIS_NAMEFILE);
    resultsDir[3] = resultsPath.resolve(BACKGROUND_NAMEFILE);
    resultsDir[4] = resultsPath.resolve(RECENTER_NAMEFILE);
    resultsDir[5] = resultsPath.resolve(RENORM_NAMEFILE);
    resultsDir[6] = resultsPath.resolve(SEEDS_NAMEFILE);
    resultsDir[7] = resultsPath.resolve(BLOBS_NAMEFILE);
    resultsDir[8] = resultsPath.resolve(FINALBLOBS_NAMEFILE);
    resultsDir[9] = resultsPath.resolve(BLOBSOLVER_NAMEFILE);

    // ========== Init list of the results directories ========================
    Path[] tempDir = new Path[10];
    tempDir[0] = origDir;
    tempDir[1] = roughBDir;
    tempDir[2] = denoisDir;
    tempDir[3] = backgroundDir;
    tempDir[4] = recenterDir;
    tempDir[5] = renormDir;
    tempDir[6] = seedsDir;
    tempDir[7] = blobsDir;
    tempDir[8] = finalBlobsDir;
    tempDir[9] = blobsolverDir;

    // ===================== For each directory ===============================
    // Compare all files between temporary and results directory with a VisitorCompare
    // If at least one file has changed or is not present, return false.
    // If all files are present and unchanged, true will be return.
    VisitorCompare comparator;
    for (int i = 0; i < 10; i++)
    {
      comparator = new VisitorCompare(tempDir[i], resultsDir[i]);

      try
      {
        Files.walkFileTree(tempDir[i], comparator);
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
        saved = false;
        break;
      }

      if (comparator.arePathsDiff)
      {
        saved = false;
        break;
      }

    }

    return saved;
  }

  // --------------------------------------------------------------------------
  //     Change WorkingDir
  // --------------------------------------------------------------------------
  public void changeWorkingDir(Path newWorkDir)
  {
    Path newOrigDir, newRoughBDir, newDenoisDir, newBackgroundDir,
        newRecenterDir, newRenormDir, newSeedsDir, newBlobsDir,
        newFinalBlobsDir, newBlobsolverDir;

    try
    {
      // ============ Initialize temporary directories ===========================
      newOrigDir = Files.createTempDirectory(newWorkDir, ORIG_NAMEFILE);
      newRoughBDir = Files.createTempDirectory(newWorkDir, ROUGHB_NAMEFILE);
      newDenoisDir = Files.createTempDirectory(newWorkDir, DENOIS_NAMEFILE);
      newBackgroundDir = Files.createTempDirectory(newWorkDir,
                                                   BACKGROUND_NAMEFILE);
      newRecenterDir = Files.createTempDirectory(newWorkDir, RECENTER_NAMEFILE);
      newRenormDir = Files.createTempDirectory(newWorkDir, RENORM_NAMEFILE);
      newSeedsDir = Files.createTempDirectory(newWorkDir, SEEDS_NAMEFILE);
      newBlobsDir = Files.createTempDirectory(newWorkDir, BLOBS_NAMEFILE);
      newFinalBlobsDir = Files.createTempDirectory(newWorkDir,
                                                   FINALBLOBS_NAMEFILE);
      newBlobsolverDir = Files.createTempDirectory(newWorkDir,
                                                   BLOBSOLVER_NAMEFILE);

      // ============= Copy results from temporary files ========================
      Files.walkFileTree(origDir, new VisitorCopy(origDir, newOrigDir));
      Files.walkFileTree(roughBDir, new VisitorCopy(roughBDir, newRoughBDir));
      Files.walkFileTree(denoisDir, new VisitorCopy(denoisDir, newDenoisDir));
      Files.walkFileTree(backgroundDir, new VisitorCopy(backgroundDir,
                                                        newBackgroundDir));
      Files.walkFileTree(recenterDir, new VisitorCopy(recenterDir,
                                                      newRecenterDir));
      Files.walkFileTree(renormDir, new VisitorCopy(renormDir, newRenormDir));
      Files.walkFileTree(seedsDir, new VisitorCopy(seedsDir, newSeedsDir));
      Files.walkFileTree(blobsDir, new VisitorCopy(blobsDir, newBlobsDir));
      Files.walkFileTree(finalBlobsDir, new VisitorCopy(finalBlobsDir,
                                                        newFinalBlobsDir));
      Files.walkFileTree(blobsolverDir, new VisitorCopy(blobsolverDir,
                                                        newBlobsolverDir));
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      IJ.showMessage("Couldn't change working directory : " + ex.
          getLocalizedMessage());
      return;
    }

    // ============= Save parameters ==========================================
    saveParam(newWorkDir + "/.param");

    // ============= Delete old working directories ===========================
    try
    {
      Files.walkFileTree(origDir, new VisitorDelete());
      Files.walkFileTree(roughBDir, new VisitorDelete());
      Files.walkFileTree(denoisDir, new VisitorDelete());
      Files.walkFileTree(backgroundDir, new VisitorDelete());
      Files.walkFileTree(recenterDir, new VisitorDelete());
      Files.walkFileTree(renormDir, new VisitorDelete());
      Files.walkFileTree(seedsDir, new VisitorDelete());
      Files.walkFileTree(blobsDir, new VisitorDelete());
      Files.walkFileTree(finalBlobsDir, new VisitorDelete());
      Files.walkFileTree(blobsolverDir, new VisitorDelete());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      IJ.showMessage("Warning : Couldn't delete old working directories in : "
                     + workingDir);
    }

    // ============= Put new working directories ==============================
    workingDir = newWorkDir;
    origDir = newOrigDir;
    roughBDir = newRoughBDir;
    denoisDir = newDenoisDir;
    backgroundDir = newBackgroundDir;
    recenterDir = newRecenterDir;
    renormDir = newRenormDir;
    seedsDir = newSeedsDir;
    blobsDir = newBlobsDir;
    finalBlobsDir = newFinalBlobsDir;
    blobsolverDir = newBlobsolverDir;
  }

  // --------------------------------------------------------------------------
  //     Get further step computed stack image
  // --------------------------------------------------------------------------
  public ImageFbt getLastImage(int slice, Step maxStep)
  {
    Step sliceStep = getStep(slice);

    // ==== If renormalized image is computed return it ====
    if (isDoRenorm() && sliceStep.compareTo(Step.RENORM) >= 0
        && maxStep.compareTo(Step.RENORM) > 0)
    {
      return ImageFbt.load(getRenormDir().resolve("slice_" + slice));
    }
    // ==== Else if recentered image is computed return it ===
    else if (isDoRecenter() && sliceStep.compareTo(Step.RECENTER) >= 0
             && maxStep.compareTo(Step.RECENTER) > 0)
    {
      return ImageFbt.load(getRecenterDir().resolve("slice_" + slice));
    }
    // ==== Else if denoised images are computed return it ===
    else if (isDoDenois() && sliceStep.compareTo(Step.DENOIS) >= 0
             && maxStep.compareTo(Step.DENOIS) > 0)
    {
      return ImageFbt.load(getDenoisDir().resolve("slice_" + slice));
    }
    // ============ Else return original image  =================
    else if (sliceStep.compareTo(Step.ORIG) >= 0
             && maxStep.compareTo(Step.ORIG) > 0)
    {
      return ImageFbt.load(getOrigDir().resolve("slice_" + slice));
    }
    else
    {
      IJ.showMessage("Original images not loaded yet.");
      Thread.currentThread().interrupt();
      return null;
    }
  }

  /**
   * Returns path to renormalized imageStack, if they aren't computed returns
   * path to recentered ImageStack, then to denoised ImageStack then to original
   * ImageStack. If even original image aren't loaded, returns null.
   *
   * @param maxStep
   * @return
   * @throws java.lang.InterruptedException
   */
  public ImageStack getLastStack(Step maxStep)
  {
    ImageStack res = new ImageStack(iWidth, iHeight);

    for (int i = 1; i <= iSize; i++)
    {
      ImageFbt img = getLastImage(i, maxStep);

      if (img == null)
      {
        return null;
      }

      res.addSlice(null, img, i - 1);
    }

    return res;
  }

  /**
   * return for this slice last computed background.
   *
   * @param slice
   * @param maxStep
   * @return
   * @throws Exception
   */
  public int[] getLastBackground(int slice, Step maxStep)
  {
    Step sliceStep = getStep(slice);

    // === If recentered backgrounds where computed return them === 
    if (isDoRecenter() && sliceStep.compareTo(Step.RECENTER) >= 0
        && maxStep.compareTo(Step.RECENTER) >= 0)
    {
      Path absolutePath = getRecenterDir().resolve(
          "slice_" + slice + "_" + ImageFbt.BACKGROUND_FILENAME);
      return (int[]) Utils.loadObject(absolutePath.toString());
    }
    // ==== Else If  backgrounds are computed return them ====
    else if (isDoFinalB() && sliceStep.compareTo(Step.FINAL_BACKGROUND) >= 0
             && maxStep.compareTo(Step.FINAL_BACKGROUND) >= 0)
    {
      Path absolutePath = getBackgroundDir().resolve(
          BACKGROUND_NAMEFILE + "_" + slice);
      return (int[]) Utils.loadObject(absolutePath.toString());
    }
    // ==== Else if rough backgrounds are computed return them  ===
    else if (isDoRoughB() && sliceStep.compareTo(Step.ROUGH_BACKGROUND) >= 0
             && maxStep.compareTo(Step.ROUGH_BACKGROUND) >= 0)
    {
      Path absolutePath = getRoughBDir().resolve(
          ROUGHB_NAMEFILE + "_" + slice);
      return (int[]) Utils.loadObject(absolutePath.toString());
    }
    // ============ Else return null =================
    else
    {
      return null;
    }
  }

  /**
   * return for each slice last computed background.
   *
   * @param maxStep
   * @return
   */
  public ArrayList<int[]> getLastBackground(Step maxStep)
  {
    ArrayList<int[]> newBack = new ArrayList<int[]>(getISize());
    for (int i = 1; i <= iSize; i++)
    {
      newBack.add(i - 1, getLastBackground(i, maxStep));

    }
    return newBack;
  }

  // --------------------------------------------------------------------------
  //     Load Image Stack from shapeSets path with fluobt dimensions
  // --------------------------------------------------------------------------
  public ImagePlus ImagePlusFromShapeSets(String title, Path path,
                                          BlobsDrawOpt drawOpt)
  {
    return Utils.ImagePlusFromShapeSets(getIWidth(), getIHeight(), title, path,
                                        drawOpt);
  }

  public ImagePlus ImagePlusFromShapeSets(String title, Path path, int size,
                                          BlobsDrawOpt drawOpt)
  {
    return Utils.ImagePlusFromShapeSets(getIWidth(), getIHeight(), title, path,
                                        size, drawOpt);
  }

  // --------------------------------------------------------------------------
  //              Update Fluobt according to original images
  // --------------------------------------------------------------------------
  /**
   * Updates Fluo_bac_Tracker according to the provided StackFbt.
   */
  public void updateToStack(StackFbt sFbt)
  {
    try
    {
      // =========== Update images dimension ==================================
      iWidth = sFbt.getWidth();
      iHeight = sFbt.getHeight();
      iSize = sFbt.getSize();

      // ============= Reinit stepsList =======================================
      stepsList = new Step[iSize];
      for (int i = 1; i <= iSize; i++)
      {
        stepsList[i - 1] = Step.ORIG;
      }

      setChanged();
      notifyObservers(Step.ORIG);

      // ================== Compute automatic threshold =======================
      double thres = 0;
      for (int i = 1; i <= iSize; i++)
      {
        ImageFbt iFbt = sFbt.getImageFbt(1);
        iFbt.setAutoThreshold("Triangle dark");
        thres += iFbt.maxTogrey255(iFbt.getMinThreshold());
      }

      autoThres = thres / (double) iSize;

    }
    catch (Exception ex)
    {   
      System.out.println("Couldn't update Fluo_Bac_Tracker with original images");
      ex.printStackTrace();
    }
  }

  // --------------------------------------------------------------------------
  //              Manage Progression
  // --------------------------------------------------------------------------
  public Step getMovieStep()
  {
    if (stepsList == null)
    {
      return null;
    }

    Step moviestep = Step.FINAL_BLOBS;
    for (Step currStep : stepsList)
    {
      if (currStep.compareTo(moviestep) <= 0)
      {
        moviestep = currStep;
      }
    }

    return moviestep;
  }

  /**
   * Update ProgressionList and booleans according to temporary directories
   * results.
   */
  public void updateStepsListFromFiles()
  {

    // ================= Init Variables =======================================
    stepsList = new Step[iSize];

    // ============== Update progressionList ==================================
    // For each slice, check (in this order) 
    // if final blobs, blobs and seeds were computed
    // if it was renormalized and recentered 
    // if final background was computed
    // if it was denoised
    // if rough background was computed
    // if it was loaded
    // And update progression list corresponding element.
    for (int i = 1; i <= iSize; i++)
    {
      // -------------- BLOBSOLVER step ---------------------------------------
      if (Files.exists(blobsolverDir.resolve("Set_" + i)))
      {
        stepsList[i - 1] = Step.BLOBSOLVER;
      }
      // -------------- FINALBLOBS step ---------------------------------------
      if (Files.exists(finalBlobsDir.resolve("Blobs_" + i)))
      {
        stepsList[i - 1] = Step.FINAL_BLOBS;
      }
      // ---------------- BLOBS Step ------------------------------------------
      else if (Files.exists(blobsDir.resolve("Blobs_" + i)))
      {
        stepsList[i - 1] = Step.BLOBS;
      }
      // ---------------- SEEDS Step ------------------------------------------
      else if (Files.exists(seedsDir.resolve("Seeds_" + i)))
      {
        stepsList[i - 1] = Step.SEEDS;
      }
      // -------------- RENORMALIZE Step --------------------------------------
      else if (Files.exists(renormDir.resolve("slice_" + i)))
      {
        stepsList[i - 1] = Step.RENORM;
      }
      // ---------------- RECENTER Step ---------------------------------------
      else if (Files.exists(recenterDir.resolve("slice_" + i)))
      {
        stepsList[i - 1] = Step.RECENTER;
      }
      // -------------- FINAL BACKGROUND Step ---------------------------------
      else if (Files.
          exists(backgroundDir.resolve(BACKGROUND_NAMEFILE + "_" + i)))
      {
        stepsList[i - 1] = Step.FINAL_BACKGROUND;
      }
      // ----------------- DENOISE Step ---------------------------------------
      else if (Files.exists(denoisDir.resolve("slice_" + i)))
      {
        stepsList[i - 1] = Step.DENOIS;
      }
      // ----------------- ROUGH BACKGROUND Step ------------------------------
      else if (Files.exists(roughBDir.resolve(ROUGHB_NAMEFILE + "_" + i)))
      {
        stepsList[i - 1] = Step.ROUGH_BACKGROUND;
      }
      // ---------------- ORIGINAL Step ---------------------------------------
      else if (Files.exists(origDir.resolve("slice_" + i)))
      {
        stepsList[i - 1] = Step.ORIG;
      }
    }

    notifyMovieStep();

  }

  public double getProgress()
  {
    if (stepsList == null)
    {
      return 0;
    }

    double progress = 0;
    double oneSlice = 100.D / (double) iSize;

    for (Step currStep : stepsList)
    {
      if (currStep.equals(Step.FINAL_BLOBS))
      {
        progress += oneSlice;
      }
      else if (currStep.equals(Step.BLOBS))
      {
        progress += oneSlice * 0.9;
      }
      else if (currStep.equals(Step.SEEDS))
      {
        progress += oneSlice * 0.8;
      }
      else if (currStep.equals(Step.RENORM))
      {
        progress += oneSlice * 0.7;
      }
      else if (currStep.equals(Step.RECENTER))
      {
        progress += oneSlice * 0.5;
      }
      else if (currStep.equals(Step.FINAL_BACKGROUND))
      {
        progress += oneSlice * 0.4;
      }
      else if (currStep.equals(Step.DENOIS))
      {
        progress += oneSlice * 0.3;
      }
      else if (currStep.equals(Step.ROUGH_BACKGROUND))
      {
        progress += oneSlice * 0.1;
      }
    }

    return progress;
  }

  /**
   * Returns true if a blobSolver is correctly computed in blobsolverDir, else
   * returns false.
   *
   * @return
   */
  public boolean isBlobSolverComputed()
  {
    boolean allSets = true;

    for (int i = 1; i <= iSize; i++)
    {
      allSets = allSets && Files.exists(blobsolverDir.resolve("Set_" + i));
    }

    if (Files.exists(blobsolverDir.resolve("RisksFor"))
        && Files.exists(blobsolverDir.resolve("RisksBack"))
        && Files.exists(blobsolverDir.resolve("Parameters"))
        && Files.exists(blobsolverDir.resolve("Lineage"))
        && allSets)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  // --------------------------------------------------------------------------
  //              Manage Directories
  // --------------------------------------------------------------------------
  /**
   * Clear all temporary results directories.
   *
   */
  public void clearTemporaryFiles()
  {
    try
    {
      Files.walkFileTree(origDir, new VisitorClear());
      Files.walkFileTree(roughBDir, new VisitorClear());
      Files.walkFileTree(denoisDir, new VisitorClear());
      Files.walkFileTree(backgroundDir, new VisitorClear());
      Files.walkFileTree(recenterDir, new VisitorClear());
      Files.walkFileTree(renormDir, new VisitorClear());
      Files.walkFileTree(seedsDir, new VisitorClear());
      Files.walkFileTree(blobsDir, new VisitorClear());
      Files.walkFileTree(finalBlobsDir, new VisitorClear());
      Files.walkFileTree(blobsolverDir, new VisitorClear());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  // --------------------------------------------------------------------------
  //              Observable methods
  // --------------------------------------------------------------------------
  public void notifyMovieStep()
  {
    Step step = getMovieStep();

    switch (step)
    {
      case BLOBSOLVER:
        setChanged();
        notifyObservers(Step.BLOBSOLVER);
      case FINAL_BLOBS:
        setChanged();
        notifyObservers(Step.FINAL_BLOBS);
      case BLOBS:
        setChanged();
        notifyObservers(Step.BLOBS);
      case SEEDS:
        setChanged();
        notifyObservers(Step.SEEDS);
      case RENORM:
        setChanged();
        notifyObservers(Step.RENORM);
      case RECENTER:
        setChanged();
        notifyObservers(Step.RECENTER);
      case FINAL_BACKGROUND:
        setChanged();
        notifyObservers(Step.FINAL_BACKGROUND);
      case DENOIS:
        setChanged();
        notifyObservers(Step.DENOIS);
      case ROUGH_BACKGROUND:
        setChanged();
        notifyObservers(Step.ROUGH_BACKGROUND);
      case ORIG:
        setChanged();
        notifyObservers(Step.ORIG);
    }
  }

}
