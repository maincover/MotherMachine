package cellst.Image;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.process.*;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.*;

import cellst.DirManagement.*;
import cellst.Main.*;

/**
 * Class solving cells segmentation and tracking.
 *
 * It contains all blobs lists over the movie and their possibleCells. From this
 * it computes currTranss and divisions risks in the movie and constructs the
 * lineage.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class BlobSolver implements Serializable
{

  //==========================================================================
  //                               Attributes
  //==========================================================================
  /**
   * Blobs of each image throughout the whole movie.
   */
  protected HashMap<Integer, ShapeSet> blobListOverMovie;
  /**
   * All risks of possible currTranss computed in forward direction.
   */
  protected TreeMap<CellTransition, Double> risksFwd;
  /**
   * All risks of possible currTranss computed in backward direction.
   */
  protected TreeMap<CellTransition, Double> risksBack;
  /**
   * Chosen transitions list to describe cells lineage.
   */
  protected LinkedHashSet<CellTransition> lineage = new LinkedHashSet<CellTransition>();
  /**
   * Width of the corresponding images.
   */
  protected int width;
  /**
   * Height of the corresponding images.
   */
  protected int height;
  /**
   * Probability for a cell to divide between two images.
   */
  private double pDiv = 0.059;
  /**
   * Maximum distance between center points of two linked (mother/mother) cell
   * in images.
   */
  private double maxDistBtwLinkedCells = 30;
  /**
   * Log of cell growth rate.
   */
  private double growRateLog = 0.06;

  /**
   * Cell growth standard deviation.
   */
  private double growStdDev = 0.04;

  /**
   * Orientation parameter.
   */
  private double orientationRate = 30;

  /**
   * Horizontal moving parameter.
   */
  private double movingRateX = 0.02;

  /**
   * Vertical moving parameter.
   */
  private double movingRateY = 0.0;

  /**
   * Moving scale parameter.
   */
  private double movingScale = 0.02;

  /**
   * Movie time scale.
   */
  private double timescale = 1.;

  /**
   * Map of colors to use for drawing each cell.
   */
  protected HashMap<Cell, Color>[] drawingColors;

  /**
   * Colors palette.
   */
  protected Color[] colorPalette;

  protected double probaThreshold = 1E-3;

  /**
   * Flags
   */
  public final static boolean FORWARD = true;
  public final static boolean BACKWARD = false;

  //==========================================================================
  //                               Constructor
  //========================================================================== 
  /**
   * BlobSolver constructor.
   */
  public BlobSolver()
  {
    blobListOverMovie = new HashMap<Integer, ShapeSet>();
    risksFwd = new TreeMap<CellTransition, Double>();
    risksBack = new TreeMap<CellTransition, Double>();

  }

  /**
   * BlobSolver constructor.
   *
   * @param _w
   * @param _h
   * @param _pDiv
   */
  public BlobSolver(Fluo_Bac_Tracker fluobt)
  {
    blobListOverMovie = new HashMap<Integer, ShapeSet>();
    risksFwd = new TreeMap<CellTransition, Double>();
    risksBack = new TreeMap<CellTransition, Double>();

    // ================== init attributes =====================================
    init_attributes(fluobt);

  }

  /**
   * BlobSolver constructor. Load all ShapeSet saved in a 'path' under ids
   * 'baseid'+number. ex : /Blobs/Blobs_1, /Blobs/Blobs_2 ...
   *
   * @param path path where to load the ShapeSets.
   * @param basename Base id of the ShapeSets files.
   * @param _w images width.
   * @param _h images height.
   * @param _pDiv
   */
  public BlobSolver(Path path, final String basename, Fluo_Bac_Tracker fluobt)
  {

    // ==================== List and sort files from ShapeSets path ==========
    File directory = path.toFile();
    File[] files = directory.listFiles(new NameFilter(basename));
    Arrays.sort(files);

    // ============ Init blobListOverMovie ====================================
    blobListOverMovie = new HashMap<Integer, ShapeSet>(files.length);

    // ============= For each files in shapeSets directory ====================
    //             Load shapeSet and add it to blobListOverMovie
    //             if it finish by a number insert it at this number place.
    int size = files.length;
    for (int i = 0; i < size; i++)
    {
      File currFile = files[i];
      ShapeSet shapeS = new ShapeSet(currFile.getAbsolutePath());
      int index = Integer.parseInt(currFile.getName().replace(basename, ""));
      blobListOverMovie.put(index, shapeS);
    }

    // ================ init risks ============================================
    risksFwd = new TreeMap<CellTransition, Double>();
    risksBack = new TreeMap<CellTransition, Double>();

    // ================== init attributes =====================================
    init_attributes(fluobt);
  }

  public void init_attributes(Fluo_Bac_Tracker fluobt)
  {
    width = fluobt.getIWidth();
    height = fluobt.getIHeight();

    pDiv = fluobt.getBlobsolvPdiv();
    maxDistBtwLinkedCells = fluobt.getBlobsolvMaxDistBtwLinkedCells();

    growRateLog = fluobt.getBlobsolvGrowRateLog();
    growStdDev = fluobt.getBlobsolvGrowStdDev();

    orientationRate = fluobt.getBlobsolvOrientationRate();

    movingRateX = fluobt.getBlobsolvMovingRateX();
    movingRateY = fluobt.getBlobsolvMovingRateY();
    movingScale = fluobt.getBlobsolvMovingScale();

    timescale = fluobt.getBlobsolvTimescale();
  }

  //==========================================================================
  //                               Getters
  //==========================================================================
  /**
   * blobListOverMovie getter.
   *
   * @return blobListOverMovie HashMap
   */
  public HashMap<Integer, ShapeSet> getBlobListOverMovie()
  {
    return blobListOverMovie;
  }

  /**
   * Lineage getter.
   *
   * @return Lineage TreeSet.
   */
  public LinkedHashSet<CellTransition> getLineage()
  {
    return lineage;
  }

  /**
   * Risks forward getter.
   *
   * @return Risks forward .
   */
  public TreeMap<CellTransition, Double> getRisksFwd()
  {
    return risksFwd;
  }

  /**
   * Risks backward getter.
   *
   * @return Risks backward .
   */
  public TreeMap<CellTransition, Double> getRisksBack()
  {
    return risksBack;
  }

  public HashSet<Point> getCellPoints(int time, Cell cell)
  {
    return getBlobListOverMovie().get(time).getCellPixels(cell);
  }

  /**
   * @return the pDiv
   */
  public double getpDiv()
  {
    return pDiv;
  }

  /**
   * @return the maxDistBtwLinkedCells
   */
  public double getMaxDistBetweenLinkedCells()
  {
    return maxDistBtwLinkedCells;
  }

  /**
   * @return the growRateLog
   */
  public double getGrowRateLog()
  {
    return growRateLog;
  }

  /**
   * @return the growStdDev
   */
  public double getGrowStdDev()
  {
    return growStdDev;
  }

  /**
   * @return the orientationRate
   */
  public double getOrientationRate()
  {
    return orientationRate;
  }

  /**
   * @return the movingRateX
   */
  public double getMovingRateX()
  {
    return movingRateX;
  }

  /**
   * @return the movingRateY
   */
  public double getMovingRateY()
  {
    return movingRateY;
  }

  /**
   * @return the movingScale
   */
  public double getMovingScale()
  {
    return movingScale;
  }

  /**
   * @return the timescale
   */
  public double getTimescale()
  {
    return timescale;
  }

  //==========================================================================
  //                               Setters
  //==========================================================================
  /**
   * @param pDiv the pDiv to motherSet
   */
  public void setpDiv(double pDiv)
  {
    this.pDiv = pDiv;
  }

  /**
   * @param maxDistBetweenLinkedCells the maxDistBtwLinkedCells to motherSet
   */
  public void setMaxDistBetweenLinkedCells(double maxDistBetweenLinkedCells)
  {
    this.maxDistBtwLinkedCells = maxDistBetweenLinkedCells;
  }

  /**
   * @param growRateLog the growRateLog to motherSet
   */
  public void setGrowRateLog(double growRateLog)
  {
    this.growRateLog = growRateLog;
  }

  /**
   * @param growStdDev the growStdDev to motherSet
   */
  public void setGrowStdDev(double growStdDev)
  {
    this.growStdDev = growStdDev;
  }

  /**
   * @param orientationRate the orientationRate to motherSet
   */
  public void setOrientationRate(double orientationRate)
  {
    this.orientationRate = orientationRate;
  }

  /**
   * @param movingRateX the movingRateX to motherSet
   */
  public void setMovingRateX(double movingRateX)
  {
    this.movingRateX = movingRateX;
  }

  /**
   * @param movingRateY the movingRateY to motherSet
   */
  public void setMovingRateY(double movingRateY)
  {
    this.movingRateY = movingRateY;
  }

  /**
   * @param movingScale the movingScale to motherSet
   */
  public void setMovingScale(double movingScale)
  {
    this.movingScale = movingScale;
  }

  /**
   * @param timescale the timescale to motherSet
   */
  public void setTimescale(double timescale)
  {
    this.timescale = timescale;
  }

  //==========================================================================
  //                              Public methods
  //==========================================================================
  /**
   * Duplicates this BlobSolver.
   *
   * @return new blobSolver
   */
  public BlobSolver duplicate()
  {
    BlobSolver res = new BlobSolver();
    res.width = width;
    res.height = height;
    res.blobListOverMovie = new HashMap<Integer, ShapeSet>(blobListOverMovie);
    res.risksBack = new TreeMap<CellTransition, Double>(risksBack);
    res.risksFwd = new TreeMap<CellTransition, Double>(risksFwd);
    res.lineage = new LinkedHashSet<CellTransition>(lineage);
    res.pDiv = pDiv;
    res.maxDistBtwLinkedCells = maxDistBtwLinkedCells;
    res.growRateLog = growRateLog;
    res.growStdDev = growStdDev;
    res.orientationRate = orientationRate;
    res.movingScale = movingScale;
    res.movingRateX = movingRateX;
    res.movingRateY = movingRateY;
    res.timescale = timescale;
    return res;
  }

  // -------------------------------------------------------------------------
  //               Save and load methods
  // -------------------------------------------------------------------------
  /**
   * Save the BlobSolver in 'file'.
   *
   * @param file path and id of the file to save.
   */
  public void save(String file)
  {
    Utils.saveObject(this, file);
  }

  /**
   * Load a BlobSolver from a file.
   *
   * @param file file where to load the BlobSolver.
   * @return BlobSolver.
   */
  public static BlobSolver load(String file)
  {
    return (BlobSolver) Utils.loadObject(file);
  }

  /**
   * Save BlobSolver in several files in 'path'. Warning : if another BlobSolver
   * was already saved in 'path' it will be erased.
   *
   * @param path
   */
  public void saveFiles(Path path)
  {
    try
    {
      // ================= Create directory if it doesn't exists ================
      Files.createDirectories(path);
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      return;
    }

    // ================= clean it =============================================
    try
    {
      Files.walkFileTree(path, new VisitorClear());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      return;
    }

    // ===================== Save all ShapeSets ===============================
    for (Map.Entry<Integer, ShapeSet> entry : blobListOverMovie.entrySet())
    {
      entry.getValue().save(path + "/Set_" + entry.getKey());
    }

    // ===================== Save risks =======================================
    Utils.saveObject(risksFwd, path + "/RisksFor");
    Utils.saveObject(risksBack, path + "/RisksBack");

    // ===================== Save lineage =====================================
    Utils.saveObject(lineage, path + "/Lineage");

    // ===================== Other parameters =================================
    Object[] list =
    {
      width, height,
      pDiv,
      maxDistBtwLinkedCells,
      growRateLog,
      growStdDev,
      orientationRate,
      movingScale,
      movingRateX,
      movingRateY,
      timescale
    };
    Utils.saveObject(list, path + "/Parameters");
  }

  /**
   * Load BlobSolver from a path where it was saved with saveFiles.
   *
   * @param path path from which to load.
   * @return new BlobSolver.
   */
  public static BlobSolver loadFiles(Path path)
  {
    // ================= Check that directory exists ==========================
    File dir = path.toFile();
    if (!dir.exists())
    {
      System.out.
          println("Error in BlobSolver.loadFiles : Path doesn't exist.");
      return null;
    }

    try
    {
      // ================= Init all attributes ==================================
      HashMap<Integer, ShapeSet> _sets = new HashMap<Integer, ShapeSet>();
      TreeMap<CellTransition, Double> _riFor = null;
      TreeMap<CellTransition, Double> _riBack = null;
      LinkedHashSet<CellTransition> _lin = null;
      int _w = (int) Double.NaN;
      int _h = (int) Double.NaN;
      double _Pdiv = Double.NaN;
      double _MaxDistBtwCells = Double.NaN;
      double _GrowRateLog = Double.NaN;
      double _GrowStdDev = Double.NaN;
      double _OrienationRate = Double.NaN;
      double _MovingScale = Double.NaN;
      double _MovingRateX = Double.NaN;
      double _MovingRateY = Double.NaN;
      double _TimeScale = Double.NaN;

      // ================================= Load Files ===========================
      File[] fileList = dir.listFiles();

      for (File file : fileList)
      {
        String name = file.getName();

        // ------------- If file begin by Set_, load shapeSet -------------------
        if (name.startsWith("Set_"))
        {
          int nb = Integer.parseInt(name.replaceFirst("^.*\\D", ""));
          ShapeSet set = (ShapeSet) Utils.loadObject(file.getAbsolutePath());
          _sets.put(nb, set);

        }
        // ------------------------- Parameters ---------------------------------
        else if ("Parameters".equals(name))
        {
          Object[] list = (Object[]) Utils.loadObject(file.getAbsolutePath());
          int i = 0;
          _w = (Integer) list[i++];
          _h = (Integer) list[i++];
          _Pdiv = (Double) list[i++];
          _MaxDistBtwCells = (Double) list[i++];
          _GrowRateLog = (Double) list[i++];
          _GrowStdDev = (Double) list[i++];
          _OrienationRate = (Double) list[i++];
          _MovingScale = (Double) list[i++];
          _MovingRateX = (Double) list[i++];
          _MovingRateY = (Double) list[i++];
          _TimeScale = (Double) list[i++];
        }
        // ------------------------- Risks forward ------------------------------
        else if ("RisksFor".equals(name))
        {
          _riFor = (TreeMap<CellTransition, Double>) Utils.loadObject(file.
              getAbsolutePath());
        }
        // ------------------------- Risks backward -----------------------------
        else if ("RisksBack".equals(name))
        {
          _riBack = (TreeMap<CellTransition, Double>) Utils.loadObject(file.
              getAbsolutePath());
        }
        // ------------------------- Lineage ------------------------------------
        else if ("Lineage".equals(name))
        {
          _lin = (LinkedHashSet<CellTransition>) Utils.loadObject(file.
              getAbsolutePath());
        }

      }

      // =============== If a file was missing, return null =====================
      if (_lin == null || _riFor == null || _riBack == null
          || Double.isNaN(_w) || Double.isNaN(_h) || Double.isNaN(_Pdiv))
      {
        System.out.
            println("Error in BlobSolver.loadFiles : Some files missing.");
        return null;
      }

      // =============== Else compute blobsolver and return it ==================
      BlobSolver blobSolv = new BlobSolver();

      blobSolv.width = _w;
      blobSolv.height = _h;
      blobSolv.lineage = _lin;
      blobSolv.risksFwd = _riFor;
      blobSolv.risksBack = _riBack;
      blobSolv.blobListOverMovie = _sets;

      blobSolv.pDiv = _Pdiv;
      blobSolv.maxDistBtwLinkedCells = _MaxDistBtwCells;
      blobSolv.growRateLog = _GrowRateLog;
      blobSolv.growStdDev = _GrowStdDev;
      blobSolv.orientationRate = _OrienationRate;
      blobSolv.movingScale = _MovingScale;
      blobSolv.movingRateX = _MovingRateX;
      blobSolv.movingRateY = _MovingRateY;
      blobSolv.timescale = _TimeScale;

      return blobSolv;

    }
    catch (ClassCastException ex)
    {
      System.out.
          println("Error in BlobSolver.loadFiles : ClassCast exception.");
      return null;
    }

  }

  // -------------------------------------------------------------------------
  //              ShapeSet methods applied to the blobListOverMovie 
  // -------------------------------------------------------------------------
  /**
   * Update all parameters of BlobSolver by taking them from a given
   * Fluo_Bac_Tracker.
   *
   * @param fluobt
   */
  public void updateParamFromFluobt(Fluo_Bac_Tracker fluobt)
  {
    init_attributes(fluobt);

    for (ShapeSet currSet : blobListOverMovie.values())
    {
      currSet.updateParamFromFluobt(fluobt);
    }
  }

  /**
   * Update all centers and radius of all ShapeFbt of evere ShapeSet in
   * 'blobListOverMovie'.
   */
  public void updateCentersAndRadii()
  {
    for (ShapeSet currSet : blobListOverMovie.values())
    {
      currSet.updateCenterAndRadius();
    }
  }

  /**
   * Update all boundaries of all ShapeFbt of evere ShapeSet in
   * 'blobListOverMovie'.
   */
  public void updateBoundaries()
  {
    for (ShapeSet currSet : blobListOverMovie.values())
    {
      currSet.updateBoundaries();
    }
  }

  /**
   * Compute connexions connectGraph for each ShapeSet.
   *
   */
  public void computeGraphs()
  {
    for (ShapeSet currSet : blobListOverMovie.values())
    {
      currSet.computeGraph();
    }
  }

  /**
   * Update possible cells certain flag for each ShapeSets. WARNING this only
   * put some cells to certain. It doesn't put to uncertain a certain cell.
   *
   */
  public void updateCertainity()
  {
    for (ShapeSet currSet : blobListOverMovie.values())
    {
      currSet.updateCellsCertainity();
    }
  }

  /**
   * Clean connectGraph of each ShapeSet.
   */
  public boolean cleanGraphs()
  {
    boolean changed = false;
    for (ShapeSet currSet : blobListOverMovie.values())
    {
      changed = changed || currSet.cleanGraph();
    }
    return changed;
  }

  /**
   * Compute all possible cells for each ShapeSet.
   */
  public void generateCells()
  {
    for (ShapeSet currImageBlobs : blobListOverMovie.values())
    {
      currImageBlobs.generateCells();
    }
  }

  /**
   * Show stack representing all blobListOverMovie and their connected
   * connectGraph.
   *
   * @return
   */
  public ImagePlus getImGraphs()
  {
    // ========== Init imageStack ==========
    ImageStack stack = new ImageStack(width, height);

    // ========== For each ShapeSet, get connectGraph imagePlus  ==========
    //                  and add it to the stack
    for (Map.Entry<Integer, ShapeSet> currImageEntry : blobListOverMovie.
        entrySet())
    {
      int t = currImageEntry.getKey();
      ShapeSet currImageBlobs = currImageEntry.getValue();

      ImagePlus IP = currImageBlobs.showGraph("" + t, width, height);

      stack.addSlice("slice_" + t, IP.getProcessor(), Math.
          min(stack.getSize(), t - 1));
    }

    // ========= Show stack ==========
    ImagePlus res = new ImagePlus("", stack);
    return res;
  }

  /**
   * Show stack representing all blobListOverMovie and their lineage.
   *
   * @return
   */
  @Deprecated
  public ImagePlus getImLin()
  {
    // ========== Init imageStack ==========
    ImageStack stack = new ImageStack(width, height);

    // ========== For each ShapeSet, get its imagePlus ==========
    //              Draw lineage.
    for (Map.Entry<Integer, ShapeSet> entry : blobListOverMovie.entrySet())
    {
      // --------- Variables ----------
      ShapeSet set1 = entry.getValue();
      int t = entry.getKey();

      // --------- get ShapeSet imagePlus ----------
      ImagePlus IP = set1.getImage("" + t, width, height);

      // --------- Init a roiManager ----------
      // 1) Draw  mother cell centers.
      // 2) Draw lines between mother cell centers and mother cell centers.
      // 3) Draw borders of certain cells in white.
      RoiManager roiM = new RoiManager();

      for (CellTransition trans : lineage)
      {
//        ShapeSet set2 = blobListOverMovie.get( trans.time );

        if (trans.time == t - 1)
        {
          Point mothCent = trans.getMother().center; //set2.getCellCenter( trans.mother );
          Point daught1Cent = trans.getDaughter1().center; //set1.getCellCenter( trans.mother );

          // === 1) ===
          Roi roi1 = new PointRoi(mothCent.x, mothCent.y);
          roiM.add(IP, roi1, 0);

          Roi roi2 = new Line(mothCent.x, mothCent.y, daught1Cent.x,
                              daught1Cent.y);
          roiM.add(IP, roi2, 0);

          // === 2) ===
          if (trans.getDaughter2() != null)
          {
            Point daught2Cent = trans.getDaughter2().center; // set1.getCellCenter( trans.sister );
            Roi roi3 = new Line(mothCent.x, mothCent.y, daught2Cent.x,
                                daught2Cent.y);
            roiM.add(IP, roi3, 0);
          }

        }
      }

      // === 3)  ===
      // For each certain cell from possibleCells, draw white boundaries.
      if (set1.possibleCells != null)
      {
        for (Cell cell : set1.possibleCells)
        {
          if (cell.isCertain())
          {

            // Compute a ShapeFbt containing all blobs of the cell,
            ShapeFbt shape = new ShapeFbt();
            for (int lab : cell.labels)
            {
              ShapeFbt blob = set1.getShape(lab);
              for (Point pt : blob.pixels)
              {
                shape.add(pt);
              }
            }

            // compute its boundary
            shape.updateBoundary();

            // Draw its boundary
            IP.getProcessor().setColor(Color.white);
            for (Point pt : shape.boundary)
            {
              IP.getProcessor().drawPixel(pt.x, pt.y);
            }
          }
        }
      }

      // === Move Rois to Overlay and flatten the image so its part of it.
      roiM.moveRoisToOverlay(IP);
      IP.getOverlay().setFillColor(Color.white);
      IP.getOverlay().setStrokeColor(Color.white);
      IP = IP.flatten();

      // === Close roiManager ===
      roiM.close();

      // === Add image to the stack. ===
      stack.addSlice("slice_" + t, IP.getProcessor(), Math.
          min(stack.getSize(), t - 1));
    }

    ImagePlus res = new ImagePlus("", stack);
    return res;
  }

  // -------------------------------------------------------------------------
  //                     Transition and division likelyhood
  // -------------------------------------------------------------------------
  public double areaProbability(int t, Cell mother, Cell daughter)
  {
    // =============== Compute cells area probability ========================
    // This is probability of two cell to be mother/daughter according to their areas.
    double area1 = mother.area;
    double area2 = daughter.area;

    return Utils.gauss(Math.log((area2 / area1) / getTimescale()),
                       getGrowRateLog(), getGrowStdDev());
  }

  public double orientationProbability(int t, Cell mother, Cell daughter)
  {
    double orient1 = mother.orientation;
    double orient2 = daughter.orientation;

    // Compute orientation difference between the two cells.
    double theta1 = orient1 - orient2;

    while (theta1 >= Math.PI)
    {
      theta1 -= 2.0 * Math.PI;
    }
    while (theta1 < -Math.PI)
    {
      theta1 += 2.0 * Math.PI;
    }
    theta1 = Math.abs(theta1);

    double theta2 = orient1 - (orient2 + Math.PI);
    while (theta2 >= Math.PI)
    {
      theta2 -= 2.0 * Math.PI;
    }
    while (theta2 < -Math.PI)
    {
      theta2 += 2.0 * Math.PI;
    }
    theta2 = Math.abs(theta2);

    double orient = Math.min(theta1, theta2) / getTimescale();

    // Probability follows an exponential law over this difference.
    return Utils.exponential(orient, getOrientationRate());
  }

  public double distanceProbability(int t, Cell mother, Cell daughter)
  {
    Point center1 = mother.center; // set1.getCellCenter( mother );
    Point center2 = daughter.center; // set2.getCellCenter( mother );

    // compute cells colony center (images must have been recentered.
    Point centerIm = new Point(width / 2, height / 2);

    // Compute distance between mother and colony center.
    // This will be important : cell speed depends on its distance to the colony center.
    double distAbs = centerIm.distance(center1);

    // If cell is not too near the colony center, compute renormalized speed in function of distance.
    // Compute probability with laplace law.
    if (distAbs > 20)
    {
      double ux = (center1.x - centerIm.x) / distAbs;
      double uy = (center1.y - centerIm.y) / distAbs;
      double dx = center2.x - center1.x;
      double dy = center2.y - center1.y;

      double speed_x = (dx * ux + dy * uy) / (distAbs * getTimescale());
      double speed_y = (-dx * uy + dy * ux) / (distAbs * getTimescale());

      double PdistX = Utils.laplace(speed_x, getMovingRateX(), getMovingScale());
      double PdistY = Utils.laplace(speed_y, getMovingRateY(), getMovingScale());
      return PdistX * PdistY;
    }
    // Else use a gaussian law.
    else
    {
      double speed = center1.distance(center2) / getTimescale();
      return Utils.gauss(speed, 1.7, 1);
    }
  }

  /**
   * Probability that 'mother' at time 'time' is also 'mother' at time t + 1.
   *
   * @param t first time.
   * @param mother first cell to compare.
   * @param daughter second cell to compare.
   * @return probability.
   */
  public double transitionLikelyhood(int t, Cell mother, Cell daughter)
  {
    // TIme scale for now at 1. Time passed between two images

//    // ====== Check that mother cell exists in their SHapeSet ========
//    if (!blobListOverMovie.get(t).possibleCells.contains(mother))
//    {
//      System.out.println(
//        "Error in BlobSolver.transitionLikelyHood : " + mother.toString() + " doesn't exists in slice " + t);
//      return Double.NaN;
//    }
    // ================= Compute cells Orientations probability ===============
    // This is probability of two cell to be mother/daughter according to their orientations.
    // Compute orientation of the two cells.
    double Porient = orientationProbability(t, mother, daughter);

    // =============== Compute cells area probability ========================
    // This is probability of two cell to be mother/daughter according to their areas.
    double Parea = areaProbability(t, mother, daughter);

    // =============== Compute cells Distance probability =====================
    // This is probability of two cell to be mother/daughter according to their positions.
    double Pdist = distanceProbability(t, mother, daughter);

    if (t == 1 && mother.contains(143) && daughter.contains(147))
    {

      String log = " !!!!!!!!!!!! Probability [ " + t + " , " + mother.
          toString() + " => " + daughter.toString() + " ]\n";
      log += "Distance = " + Pdist + "\n";
      log += "Orientation = " + Porient + "\n";
      log += "Area = " + Parea + "\n";
      log += "Proba = " + (1 - getpDiv()) * Pdist * Porient * Parea + "\n";

      System.out.println(log);
    }

    // ========================= Main formula =================================
    return (1 - getpDiv()) * Pdist * Porient * Parea;

  }

  /**
   * Compute probability for cell mother to divide into mother1 and and sister
   * in next ShapeSet.
   *
   * @param time time of mother ShapeSet.
   * @param mother mother cell.
   * @param daughter1 first mother cell.
   * @param daughter2 second mother cell.
   * @return probability for cell mother to divide into mother1 and and sister
   * in next ShapeSet.
   */
  public double divisionLikelyhood(int time, Cell mother, Cell daughter1,
                                   Cell daughter2)
  {

    // =========== Check that cell exist in corresponding ShapeSet ===========
    /*
     * if (!blobListOverMovie.get(time).possibleCells.contains(mother)) {
     * System.out.println("Error in BlobSolver.divisionLikelyhood : " + mother.
     * toString() + " not present in slice " + time); return Double.NaN; }
     *
     * if (!daughterSet.possibleCells.contains(mother)) { System.out.println(
     * "Error in BlobSolver.divisionLikelyhood : " + mother.toString() + "
     * not present in slice " + (time + 1)); return Double.NaN; }
     *
     * if (sister != null) { if
     * (!daughterSet.possibleCells.contains(sister)) { System.out.println(
     * "Error in BlobSolver.divisionLikelyhood : " + sister. toString() + "
     * not present in slice " + (time + 1)); return Double.NaN; }
     *
     * if (mother.equals(sister)) { System.out.println( "Error in
     * BlobSolver.divisionLikelyhood : Daughter cells are equals."); return
     * Double.NaN; } }
     */
    // ============== Compute a cell fusionning the two daughters. ============
    Cell daughter = new Cell(daughter1);
    for (int lab : daughter2.labels)
    {
      daughter.add(lab);
    }

    // ============ Update cell attributes ===================================
    daughter.area = daughter1.area + daughter2.area;

    daughter.center.x = (int) Math.round((daughter1.center.x * daughter1.area
                                          + daughter2.center.x * daughter2.area)
                                         / daughter.area);
    daughter.center.y = (int) Math.round((daughter1.center.y * daughter1.area
                                          + daughter2.center.y * daughter2.area)
                                         / daughter.area);

    double[] orientVar1 = daughter1.orientationVariables;
    double[] orientVar2 = daughter2.orientationVariables;
    double[] orientVar = new double[3];
    orientVar[0] = (orientVar1[0] * daughter1.area + orientVar2[0]
                                                     * daughter2.area)
                   / daughter.area;
    orientVar[1] = (orientVar1[1] * daughter1.area + orientVar2[1]
                                                     * daughter2.area)
                   / daughter.area;
    orientVar[2] = (orientVar1[2] * daughter1.area + orientVar2[2]
                                                     * daughter2.area)
                   / daughter.area;

    daughter.setOrientationVariables(orientVar);
    daughter.UpdateOrientationFromVariables();

    // ======================== Main formula. =================================
    double proba = transitionLikelyhood(time, mother, daughter);

    return proba * getpDiv() / (1 - getpDiv());
  }

  // -------------------------------------------------------------------------
  // Transition and division probabilities for one cell and an adjacent image
  // -------------------------------------------------------------------------
  /**
   * Compute all currTranss probabilities of a cell with one of the
   * chronologically adjacent ShapeSet.
   *
   * @param time time of the ShapeSet containing cell.
   * @param cell cell to use.
   * @param forward if true compute probabilities with next shapeSet else
   * compute probabilities with last ShapeSet
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans probability.
   */
  public HashMap<CellTransition, Double> transitions(int time, Cell cell,
                                                     boolean forward)
  {
    if (forward)
    {
      return transitionsForward(time, cell);
    }
    else
    {
      return transitionsBackward(time, cell);
    }
  }

  /**
   * Compute all divisions probabilities of a cell with one of the adjacent
   * ShapeSet.
   *
   * @param time time of cell cell ShapeSet.
   * @param cell cell cell.
   * @param forward if true compute probabilities with next shapeSet else
   * compute probabilities with last ShapeSet
   * @return HashMap with as key array of the two mother cells and as value the
   * division probability.
   */
  public HashMap<CellTransition, Double> divisions(int time, Cell cell,
                                                   boolean forward)
  {
    if (forward)
    {
      return divisionsForward(time, cell);
    }
    else
    {
      return divisionsBackward(time, cell);
    }
  }

  // -------------------- Forward probabilities -------------------------------
  /**
   * Compute all currTranss probabilities of a cell with the next
   * chronologically adjacent ShapeSet.
   *
   * @param time time of the ShapeSet containing cell.
   * @param cell cell to use.
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans probability.
   */
  public HashMap<CellTransition, Double> transitionsForward(int time, Cell cell)
  {
    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();

//    Point center = blobListOverMovie.get( time ).getCellCenter( cell );
    // ============ For each possible cell of adjacent ShapeSet =====================
    // Compute currTrans probability with cell and add it to the result hashMap.
    ShapeSet set = blobListOverMovie.get(time + 1);

    // === If Adjacent ShapeSet doesn'time exists return null ===
    if (set == null)
    {
      System.out.println(
          "Error in BlobSolver.transitions : Slice " + (time + 1)
          + " doesn't exists.");
      return null;
    }

    // === For each possible daugter ===
    // 1) Check if it doesn'time have a mother yet
    // 2) Check if it is not too far from cell
    // 3) Compute proba and add it to res shapeList 
    for (Cell daughter : set.possibleCells)
    {
      // 1) Check if it doesn'time have a mother yet
      // 2) Check if it is not too far from cell
      if (daughter.doneBack
          || daughter.center.distance(cell.center)
             > getMaxDistBetweenLinkedCells())
      {
        continue;
      }

      // 3) Compute proba and add it to res shapeList 
      double proba = transitionLikelyhood(time, cell, daughter);
      if (proba > 0)
      {
        res.put(new CellTransition(time, cell, daughter), proba);
      }
    }

    // =================== Return HashMap =====================================
    return res;
  }

  /**
   * Compute all divisions probabilities of a cell with the next adjacent
   * ShapeSet.
   *
   * @param time time of cell cell ShapeSet.
   * @param cell cell cell.
   * @return HashMap with as key array of the two mother cells and as value the
   * division probability.
   */
  public HashMap<CellTransition, Double> divisionsForward(int time, Cell cell)
  {
    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();

//    Point center = blobListOverMovie.get( time ).getCellCenter( cell );
    // ============ For each possible cell of adjacent ShapeSet =====================
    // Compute currTrans probability with cell and add it to the result hashMap.
    // ----------------------- FORWARD ----------------------------------------
    ArrayList<Cell> done = new ArrayList<Cell>();

    ShapeSet set = blobListOverMovie.get(time + 1);

    // === If adjacent set1 doesn'time exists return null ===
    if (set == null)
    {
      System.out.println(
          "Error in BlobSolver.divisions : Slice " + (time + 1)
          + " doesn't exists.");
      return null;
    }

    // === For all possible couple of daughters ===
    // 1) Check that they don't have a mother
    // 2) Check if they are not too far from cell
    // 3) Check that they don't intersect each other
    // 4) Compute the division probability and add it to res shapeList.
    for (Cell daughter1 : set.possibleCells)
    {
      done.add(daughter1);

      // 1) Check that they don't have a mother
      // 2) Check if they are not too far from cell
      if (daughter1.doneBack || daughter1.center.distance(cell.center)
                                > getMaxDistBetweenLinkedCells())
      {
        continue;
      }

      for (Cell daughter2 : set.possibleCells)
      {
        // 1) Check that they don't have a mother
        // 2) Check if they are not too far from cell
        if (daughter2.doneBack || daughter2.center.distance(cell.center)
                                  > getMaxDistBetweenLinkedCells())
        {
          continue;
        }

        Point meanCenter = new Point();
        meanCenter.x = (daughter1.center.x + daughter2.center.x) / 2;
        meanCenter.y = (daughter1.center.y + daughter2.center.y) / 2;

        if (meanCenter.distance(cell.center) > getMaxDistBetweenLinkedCells())
        {
          continue;
        }

        // 3) Check that they don't intersect each other
        if (!daughter1.intersect(daughter2) && !done.contains(daughter2))
        {
          // 4) Compute the division probability and add it to res shapeList.
          double proba = divisionLikelyhood(time, cell, daughter1, daughter2);
          if (proba > 0)
          {
            res.put(new CellTransition(time, cell, daughter1, daughter2),
                    proba);
          }
        }
      }
    }

    // Return HashMap
    return res;
  }

  // -------------------- Backward probabilities ------------------------------
  /**
   * Compute all transition probabilities of a cell with the last adjacent
   * ShapeSet.
   *
   * @param time time of the ShapeSet containing cell.
   * @param cell cell to use.
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans probability.
   */
  public HashMap<CellTransition, Double> transitionsBackward(int time, Cell cell)
  {
    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();

    // ========== If Adjacent ShapeSet doesn'time exists return null ==========
    ShapeSet set = blobListOverMovie.get(time - 1);
    if (set == null)
    {
      System.out.println(
          "Error in BlobSolver.transitions : Slice " + (time - 1)
          + " doesn't exists.");
      return null;
    }

    // ================== For each possible mother ============================
    // 1) Check if it doesn'time have a daughter yet
    // 2) Check if it is not too far from cell
    // 3) Compute proba and add it to res shapeList 
    for (Cell mother : set.possibleCells)
    {
      // -------------- 1) Check if it doesn't have a daughter yet ------------
      // -------------- 2) Check if it is not too far from cell ---------------
      if (mother.doneFor
          || mother.center.distance(cell.center)
             > getMaxDistBetweenLinkedCells())
      {
        continue;
      }

      // --------- 3) Compute proba and add it to res shapeList ---------------
      double proba = transitionLikelyhood(time, mother, cell);
      if (proba > 0)
      {
        res.put(new CellTransition(time, mother, cell), proba);
      }
    }

    // =================== Return HashMap =====================================
    return res;
  }

  /**
   * Compute all divisions probabilities of a cell with the last adjacent
   * ShapeSet.
   *
   * @param time time of cell cell ShapeSet.
   * @param cell cell cell.
   * @return HashMap with as key array of the two mother cells and as value the
   * division probability.
   */
  public HashMap<CellTransition, Double> divisionsBackward(int time, Cell cell)
  {
    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();

    // ============ If adjacent set doesn't exists return null ================
    ShapeSet motherSet = blobListOverMovie.get(time - 1);
    ShapeSet daughtersSet = blobListOverMovie.get(time);

    if (motherSet == null)
    {
      System.out.println(
          "Error in BlobSolver.divisions : Slice " + (time - 1)
          + " doesn't exists.");
      return null;
    }

    // =================== For all possible mother ============================
    // 1) Check that it doesn't have daughter(s)
    // 2) For all possible sister
    //    2a) Check it has no mother yet and it doesn't intersect cell
    //    2b) Check daughters aren't too far from mother
    //    2c) Compute the division probabilities and add it to res shapeList
    for (Cell mother : motherSet.possibleCells)
    {
      // ---------------- 1) Check that they don't have a mother --------------
      if (mother.doneBack)
      {
        continue;
      }

      // ---------------- 2) For all possible sister --------------------------
      for (Cell sister : daughtersSet.possibleCells)
      {
        // === 2a) Check it has no mother yet and it doesn't intersect cell
        if (sister.doneBack || sister.intersect(cell))
        {
          continue;
        }

        // === 2b) Check daughters aren't too far from mother
        Point meanCenter = new Point();
        meanCenter.x = (int) ((cell.center.x * cell.area + sister.center.x
                                                           * sister.area)
                              / (cell.area + sister.area));
        meanCenter.y = (int) ((cell.center.y * cell.area + sister.center.y
                                                           * sister.area)
                              / (cell.area + sister.area));

        if (meanCenter.distance(cell.center) > getMaxDistBetweenLinkedCells())
        {
          continue;
        }

        // === 2c) Compute the division probability and add it to res shapeList.
        double proba = divisionLikelyhood(time, mother, cell, sister);
        if (proba > 0)
        {
          res.put(new CellTransition(time, mother, cell, sister),
                  proba);
        }

      }
    }

    // ======================================================================== 
    return res;
  }

  // -------------------------------------------------------------------------
  //         Transition and division risks
  // -------------------------------------------------------------------------
  /**
   * Compute risk of transition from mother in ShapeSet t1 to daughter. This
   * risk is computed either with all transisions and divisions probabilities of
   * mother cell, or all transisions and divisions probabilities of daughter1
   * cell
   *
   * @param t time of mother cell ShapeSet.
   * @param mother mother cell.
   * @param daughter first daughter cell.
   * @param forward If true risk is computed with all transisions and divisions
   * probabilities of mother cell Else risk is computed with all transisions and
   * divisions probabilities of daughter1 cell
   * @return risks of transition.
   */
  public double transitionRisk(int t, Cell mother, Cell daughter,
                               boolean forward)
  {
    return divisionRisk(t, mother, daughter, null, forward);
  }

  /**
   * Compute risk of division from mother in ShapeSet t1 to daughter1 and
   * daughter2 in next ShapeSet. This risk is computed either with all
   * transisions and divisions probabilities of mother cell, or all transisions
   * and divisions probabilities of daughter1 cell
   *
   * @param t time of mother cell ShapeSet.
   * @param mother mother cell.
   * @param daughter1 first daughter cell.
   * @param daughter2 second daughter cell.
   * @param forward If true risk is computed with all transisions and divisions
   * probabilities of mother cell Else risk is computed with all transisions and
   * divisions probabilities of daughter1 cell
   * @return risks of division.
   */
  public double divisionRisk(int t, Cell mother, Cell daughter1, Cell daughter2,
                             boolean forward)
  {
    // DEBUG 30/01
    boolean debugVerb = t == 1 && mother.contains(143) && daughter2 == null;
    String log = "!!!!!!!!!!!!!!!!!!!!!!!!! Risks (forward : " + forward
                 + ") !!!!!!!!!!!!!!!!!!!!!!!!!\n";
    log += "[ " + t + " , " + mother.toString() + " => " + daughter1.toString()
           + "\n";

    // =========== Check that cell exist in corresponding ShapeSet ===========
    if (!blobListOverMovie.get(t).possibleCells.contains(mother))
    {
      System.out.println("Error in BlobSolver.divisionRisk : " + mother.
          toString() + " not present in slice " + t);
      return Double.NaN;
    }

    if (!blobListOverMovie.get(t + 1).possibleCells.contains(daughter1))
    {
      System.out.println("Error in BlobSolver.divisionRisk : " + daughter1.
          toString() + " not present in slice " + (t + 1));
      return Double.NaN;
    }

    if (daughter2 != null)
    {
      if (!blobListOverMovie.get(t + 1).possibleCells.contains(daughter2))
      {
        System.out.println("Error in BlobSolver.divisionRisk : " + daughter2.
            toString() + " not present in slice " + (t + 1));
        return Double.NaN;
      }

      if (daughter1.equals(daughter2))
      {
        System.out.println(
            "Error in BlobSolver.divisionRisk : Daughter cells are equals.");
        return Double.NaN;
      }
    }

    // =========== Compute all divisions probabilities of mother =============
    HashMap<CellTransition, Double> divs;
    HashMap<CellTransition, Double> trans;

    if (forward)
    {
      divs = divisions(t, mother, FORWARD);
      trans = transitions(t, mother, FORWARD);
    }
    else
    {
      divs = divisions(t + 1, daughter1, BACKWARD);
      trans = transitions(t + 1, daughter1, BACKWARD);
    }

    trans.putAll(divs);

    // ========== Get maximum currTrans probability of mother to ==============
    //               any cell but mother in next ShapeSet.
    double max = Double.NEGATIVE_INFINITY;
    double proba = Double.NEGATIVE_INFINITY;
    // DEBUG 30/01
    CellTransition debugMaxTrans = null;

    for (Map.Entry<CellTransition, Double> entry : trans.entrySet())
    {
      CellTransition curTrans = entry.getKey();
      double val = entry.getValue();

      if (!curTrans.equals(
          new CellTransition(t, mother, daughter1, daughter2)))
      {
        if (val > max)
        {
          max = val;

          // DEBUG 30 / 01
          debugMaxTrans = curTrans;
        }
      }
      else
      {
        proba = val;
      }
    }

    if (debugVerb)
    {
      log += "Transition proba = " + proba + "\n";
    }

    // ========================= Main formula =================================
    //  maximum division probability of mother to any couple of cells but the current one
    //  divided by probability of currTrans from mother to current couple of mother.
    if (proba < probaThreshold)
    {
      if (debugVerb)
      {
        log += "Proba too small, risk = infinite\n";
        System.out.println(log);
      }
      return Double.POSITIVE_INFINITY;
    }

    if (Double.isInfinite(proba))
    {
      if (debugVerb)
      {
        log += "Proba is infinite, risk = infinite\n";
        System.out.println(log);
      }
      return Double.POSITIVE_INFINITY;
    }

    if (Double.isInfinite(max))
    {
      if (debugVerb)
      {
        log += "Max is infinite, risk = 0\n";
        System.out.println(log);
      }
      return 0;
    }

    if (debugVerb)
    {
      log += "Second Max Transistion = " + debugMaxTrans.toString() + " : "
             + max + "\n";
      log += "Risk = " + max / proba;
      System.out.println(log);

    }

    return max / proba;
  }

  // -------------------------------------------------------------------------
  // Transition and division risks for one cell and an adjacent image
  // -------------------------------------------------------------------------
  /**
   * Compute all risks of currTrans or division of a cell with the next
   * ShapeSet.
   *
   * @param time time of ShapeSet.
   * @param cell cell to compute.
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans currTransRisk.
   * @throws java.lang.InterruptedException
   */
  public HashMap<CellTransition, Double> risksForward(
      int time,
      Cell cell) throws InterruptedException
  {
    // ================= Check that cell exists ===============================
    if (!blobListOverMovie.get(time).possibleCells.contains(cell))
    {
      System.out.println("Error in BlobSolver.risksForward : " + cell.
          toString() + " doesn't exists in slice " + time);
      return null;
    }

    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();
    ShapeSet set = blobListOverMovie.get(time + 1);

    // =========== If cell already has a mother, do nothing  =================
    if (cell.doneFor)
    {
      return res;
    }

    // ==== If this was the last image in the movie, throw Exception  ========
    if (set == null)
    {
      System.out.println(
          "Error in BlobSolver.risksForward : Slice " + (time + 1)
          + " doesn't exists.");
      return null;
    }

    // ======= Get cell center point ==========================================
    // ======= For each possible cell with no parent in the next image, =====
    // Compute the corresponding currTrans risk and add it to res.
    HashSet<Cell> done = new HashSet<Cell>();
    for (Cell daughter1 : set.possibleCells)
    {
      Utils.checkThreadInterruption();

      done.add(daughter1);

      // mother
      // - must not have a mother 
      // -  nor be too far from cell
      if (daughter1.doneBack || cell.center.distance(daughter1.center)
                                > getMaxDistBetweenLinkedCells())
      {
        continue;
      }

      // Compute the risk for the current cell to correspond to mother in
      // the next image
      double risk = transitionRisk(time, cell, daughter1, FORWARD);
      if (!Double.isInfinite(risk))
      {
        res.put(new CellTransition(time, cell, daughter1), risk);
      }

      // ========= For each possible couple of cells of ShapeSet time + 1 =====
      // Compute division Risk with cell and add it to the result hashMap.
      for (Cell daughter2 : set.possibleCells)
      {
        // Daughter2 must not 
        // - intersect mother 
        // - nor have a mother 
        if (daughter1.intersect(daughter2)
            || done.contains(daughter2)
            || daughter2.doneBack)
        {
          continue;
        }

        // - nor be too far from cell
//        Point daughter2Center = currSet.getCellCenter( sister );
        if (cell.center.distance(daughter2.center)
            > getMaxDistBetweenLinkedCells())
        {
          continue;
        }

        double risk2 = divisionRisk(time, cell, daughter1, daughter2, FORWARD);
        if (!Double.isInfinite(risk2))
        {
          res.
              put(new CellTransition(time, cell, daughter1, daughter2),
                  risk2);
        }

      }
    }

    return res;
  }

  /**
   * Compute all risks of currTrans or division of a cell with the last
   * ShapeSet.
   *
   * @param time time of ShapeSet.
   * @param cell cell to compute.
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans currTransRisk.
   * @throws java.lang.InterruptedException
   */
  public HashMap<CellTransition, Double> risksBackward(
      int time, Cell cell) throws InterruptedException
  {
    // DEBUG 28/01
    boolean debugVerb = false; // (time == 5 && cell.contains(310));
    String log = "";
    if (debugVerb)
    {
      log += " ========= Risks backward : " + cell.toString()
             + " ==================\n";
    }

    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = new HashMap<CellTransition, Double>();

    // ================== If cell has a mother yet do nothing =================
    if (cell.doneBack)
    {
//      if (debugVerb)
//      {
//        log += "Cell already done back.\n";
//      }
      return res;
    }

    // =========== If last ShapeSet doesn'time exists throw exception ==========
    ShapeSet set = blobListOverMovie.get(time - 1);
    if (set == null)
    {
      System.out.println(
          "Error in BlobSolver.risksBackward : Slice " + (time - 1)
          + " doesn't exists.");
    }

    // ======= Get cell center point ==========================================
//    Point center = blobListOverMovie.get( time ).getCellCenter( cell );
    // ============ For each possible mother in ShapeSet time - 1 =============
    // 1) Check that mother does not 
    //      - intersect a certain cell, 
    //      - nor have a mother
    //      - nor is too far from cell
    // 2) Compute currTrans risk and add it to res shapeList
    for (Cell mother : set.possibleCells)
    {
//      if (debugVerb)
//      {
//        log += " -------- Possible mother : " + mother.toString()
//               + " ---------\n";
//      }

      Utils.checkThreadInterruption();

      // 1) Check that mother does not 
      //      - intersect a certain cell, 
      //      - nor have a mother
      //      - nor is too far from cell
//      Point motherCenter = currSet.getCellCenter( mother );
      if (mother.doneFor || cell.center.distance(mother.center)
                            > getMaxDistBetweenLinkedCells())
      {
//        if (debugVerb)
//        {
//          log += "Continue called : \n";
//          log += "     - mother.doneFor : " + mother.doneFor + "\n";
//          log += "     - distance with cell : " + cell.center.distance(
//              mother.center) + "\n";
//        }
        continue;
      }

      //2) Compute currTrans risk and add it to res shapeList
      double risk = transitionRisk(time - 1, mother, cell, BACKWARD);
      if (!Double.isInfinite(risk))
      {
        res.put(new CellTransition(time - 1, mother, cell), risk);

        if (debugVerb)
        {
          log += " -------- Possible mother : " + mother.toString()
                 + " ---------\n";
          log += "risk = " + risk + "\n";
        }
      }
//      else if (debugVerb)
//      {
//        log += "Infinite risk : " + risk + "\n";
//      }

      // ---------- For each sister possible in ShapeSet time ---------------
      // 1) Check that sister does not 
      //          - intersect a certain cell, 
      //          - nor have a mother, 
      //          - nor intersect cell
      //          - nor is too far from cell
      // 2) Compute division risk with mother and add it to res shapeList
      for (Cell sister : blobListOverMovie.get(time).possibleCells)
      {
//        if (debugVerb)
//        {
//          log += "=== Cell sister : " + sister.toString() + " === \n";
//        }

        // 1) Check that sister does not 
        //          - intersect a certain cell, 
        //          - nor have a mother, 
        //          - nor intersect cell 
        if (sister.doneBack
            || sister.intersect(cell))
        {
//          if (debugVerb)
//          {
//            log += "Continue called : \n";
//            log += "     - sister doneBack : " + sister.doneBack + "\n";
//            log += "     - sister intersect cell : " + sister.intersect(cell)
//                   + "\n";
//          }
          continue;
        }

        // if sisters cells are too far from cell. 
        Point sisterCenter = new Point();
        sisterCenter.x = (int) ((cell.center.x * cell.area + sister.center.x
                                                             * sister.area)
                                / (cell.area + sister.area));
        sisterCenter.y = (int) ((cell.center.y * cell.area + sister.center.y
                                                             * sister.area)
                                / (cell.area + sister.area));
        if (sisterCenter.distance(mother.center)
            > getMaxDistBetweenLinkedCells())
        {
//          if (debugVerb)
//          {
//            log += "Continue called : \n";
//            log += "     - sisters center too far from center cell : "
//                   + sisterCenter.distance(mother.center)
//                   + "\n";
//          }
          continue;
        }

        // 2)
        double risk2 = divisionRisk(time - 1, mother, cell, sister, BACKWARD);
        if (!Double.isInfinite(risk2))
        {
          res.put(new CellTransition(time - 1, mother, cell, sister), risk2);

          if (debugVerb)
          {
            log += "=== Cell sister : " + sister.toString() + " === \n";
            log += "risk = " + risk2 + "\n";
          }
        }
//        else if (debugVerb)
//        {
//          log += "Infinite risk : " + risk2 + "\n";
//        }
      }
    }

    if (debugVerb)
    {
      System.out.println(log);
    }

    // =================== Return HashMap =====================================
    return res;
  }

  /**
   * Compute all risks of currTrans or division of a cell with one of the
   * chronologically adjacent ShapeSet.
   *
   * @param time time of ShapeSet.
   * @param cell cell to compute.
   * @return HashMap with as key cell implied in currTrans and as value the
   * currTrans currTransRisk.
   * @throws java.lang.InterruptedException
   */
  public HashMap<CellTransition, Double> risks(int time, Cell cell) throws
      InterruptedException
  {
    // ================== Init result HashMap =================================
    HashMap<CellTransition, Double> res = risksForward(time, cell);

    HashMap<CellTransition, Double> back = risksBackward(time, cell);

    // =================== Add backward and forward ===========================
    for (Map.Entry<CellTransition, Double> entry : back.entrySet())
    {
      res.put(entry.getKey(), entry.getValue());
    }

    // =================== Return HashMap =====================================
    return res;
  }

  // -------------------------------------------------------------------------
  //   Update risk
  // -------------------------------------------------------------------------
  /**
   * Update risks after a currTrans of time 'time' was add to lineage. update
   * risks between time -1, time , time + 1 .
   *
   * Removes all currTranss between time -1, time , time + 1. Recompute all this
   * currTranss from certain cells in those slices.
   *
   * @param time
   */
  public void updateRisks(int time)
  {
    RiskUpdateManagerThr run = new RiskUpdateManagerThr(this, time);
    run.start();
    try
    {
      run.join();
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }

  }

  // -------------------------------------------------------------------------
  //    Main method to construct lineage.
  // -------------------------------------------------------------------------
  public void constructLineage()
  {
    // ========== Update all ShapeSets center radius and boundaries ===========
    System.out.println(
        " =============== Update centers and boundaries. =============== ");
    updateCentersAndRadii();
    updateBoundaries();

    // ====== Generate possible cells and compute transitions risks ===========
    System.out.println(" =============== generate cells =============== ");
    generateCells();

    // ====== Until there is no more currTrans to choose ===========
    // 1) Try to solve all that is possible from certains cells.
    // 2) Do a step with all cells certain or not to choose next currTrans.
    computeBestTransitions();

  }

  public void computeBestTransitions()
  {
    RiskManagerThr run = new RiskManagerThr(this);
    run.start();

    try
    {
      run.join();
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
  }

  // -------------------------------------------------------------------------
  //    Merging methods
  // -------------------------------------------------------------------------
  /**
   * Merge two blobs in a risks treeMap.
   *
   * @param risks TreeMap containing cellTransitions as keys.
   * @param time time of the ShapeSet containing the two blobs to merge.
   * @param labToRem blobLab to merge and remove.
   * @param lab blobLab to merge and keep.
   */
  public void mergeRisks(TreeMap<CellTransition, Double> risks, int time,
                         int labToRem, int lab)
  {
    // =========== Init Lists of CellTransitions to remove and to add to the risks TreeSet =========
    ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
    HashMap<CellTransition, Double> toAdd = new HashMap<CellTransition, Double>();

    // =========== For each currTrans of risks ==============================
    // 1) If currTrans is of time 'time', 
    //    a) if mother contains labToRemove or lab, add it to remove shapeList. 
    //    b) if mother contains labToRemove and lab, add a currTrans with only 
    //       lab into add shapeList.
    // 2) If currTrans is of time 'time - 1', 
    //    a) if one of the daughters contains labToRemove or lab, add it to 
    //       remove shapeList. 
    //    b) if one ofthe daughters contains labToRemove and lab, add a 
    //       currTrans with only lab into add shapeList.
    for (Map.Entry<CellTransition, Double> entry : risks.entrySet())
    {
      CellTransition trans = entry.getKey();
      double val = entry.getValue();

      if (trans.time == time)
      {
        boolean containsLabToRemove = trans.getMother().labels.contains(labToRem);
        boolean containsLab = trans.getMother().labels.contains(lab);

        // --------------- 1) a) ---------------------------------------------
        // If currTrans is of time 'time' and mother contains labToRemove or 
        // lab, add it to remove shapeList. 
        if (containsLabToRemove && containsLab)
        {
          toRem.add(trans);
        }

        // --------------- 1) b) ---------------------------------------------
        // If currTrans is of time 'time' and mother contains labToRemove and 
        // lab, add a currTrans with only lab into add shapeList.
        if (containsLabToRemove && containsLab)
        {
          // === Compute a new currTrans with only lab. ===
          CellTransition newTrans = new CellTransition(trans);
          newTrans.getMother().remove(labToRem);

          // === Add new currTrans to risksToAdd shapeList.
          toAdd.put(newTrans, val);
        }

      }
      else if (trans.time == time - 1)
      {
        boolean containsLabToRemove1 = trans.getDaughter1().labels.
            contains(labToRem);
        boolean containsLab1 = trans.getDaughter1().labels.contains(lab);
        boolean containsBoth1 = containsLab1 && containsLabToRemove1;
        boolean containsOnlyOne1 = !containsBoth1 && (containsLab1
                                                      || containsLabToRemove1);

        boolean containsLabToRemove2 = false;
        boolean containsLab2 = false;
        boolean containsBoth2 = false;
        boolean containsOnlyOne2 = false;

        if (trans.getDaughter2() != null)
        {
          containsLabToRemove2 = trans.getDaughter2().labels.contains(labToRem);
          containsLab2 = trans.getDaughter2().labels.contains(lab);
          containsBoth2 = containsLab2 && containsLabToRemove2;
          containsOnlyOne2 = !containsBoth2 && (containsLab2
                                                || containsLabToRemove2);
        }

        // ------------- 2) a) -----------------------------------------------------
        // If currTrans is of time 'time - 1', and one of the daughters 
        // contains labToRemove or lab, add it to remove shapeList. 
        if (containsLab1 || containsLabToRemove1
            || containsLab2 || containsLabToRemove2)
        {
          // === Add currTrans to torem shapeList. ===
          toRem.add(trans);
        }

        // ------------- 2) b) -----------------------------------------------------
        // If currTrans is of time 'time - 1', and one of the daughters 
        // contains labToRemove and lab, add currTrans with only lab into add
        // shapeList.
        if ((containsBoth1 && !containsOnlyOne2)
            || (containsBoth2 && !containsOnlyOne1))
        {

          // === Compute new currTrans with labToRem replaced by lab. ===
          CellTransition newTrans = new CellTransition(trans);

          // --- remove labToRem in mother ---
          if (containsLabToRemove1)
          {
            newTrans.getDaughter1().remove(labToRem);
          }

          // --- If needed, remove labToRem in sister ---
          if (trans.getDaughter2() != null && containsLabToRemove2)
          {
            newTrans.getDaughter2().remove(labToRem);
          }

          // === Add new currTrans to risksToAdd shapeList.
          toAdd.put(newTrans, val);
        }
      }
    }

    // =========== Remove all transitions of toRem shapeList from risks =======
    risks.keySet().removeAll(toRem);
//    for ( CellTransition trans : toRem )
//    {
//      risks.remove( trans );
//    }

    // ============ Add all transitions of risksToAdd shapeList to risks ======
    risks.putAll(toAdd);

  }

  /**
   * Merge two blobs in lineage.
   *
   * @param time time of the ShapeSet containing the two blobs to merge.
   * @param labToRem blobLab to merge and remove.
   * @param lab blobLab to merge and keep.
   */
  public void mergeLin(int time, int labToRem, int lab)
  {
    // =========== Init Lists of CellTransitions to remove and to add to the lineage =========
    ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
    ArrayList<CellTransition> toAdd = new ArrayList<CellTransition>();

    // =========== For each currTrans of lineage ==============================
    // 1) If currTrans is of time 'time', if mother contains labToRemove, 
    //    add it to remove shapeList and add a currTrans with labToRem replaced by lab in to add shapeList.
    // 2) If currTrans is of time 'time - 1', if one of the daughters contains labToRemove, 
    //    add currTrans to remove shapeList and add a currTrans with labToRem replaced by lab in to add shapeList.
    for (CellTransition trans : lineage)
    {

      // ------------- 1) -----------------------------------------------------
      if (trans.time == time)
      {
        if (trans.getMother().labels.contains(labToRem))
        {
          // === Add currTrans torem shapeList. ===
          toRem.add(trans);

          // === Compute new currTrans with labToRem replaced by lab. ===
          CellTransition newTrans = new CellTransition(trans);
          newTrans.getMother().remove(labToRem);
          if (!newTrans.getMother().labels.contains(lab))
          {
            newTrans.getMother().add(lab);
          }

          // === Add new currTrans to risksToAdd shapeList ===
          toAdd.add(newTrans);
        }
      }
      // ------------- 2) -----------------------------------------------------
      else if (trans.time == time - 1)
      {
        // ============= If Daughter1 contains labToRem =============
        // Add currTrans to remove shapeList and compute new currTrans to add,
        // with labToRem replaced by lab in mother and sister if needed.
        if (trans.getDaughter1().labels.contains(labToRem))
        {
          // === Add currTrans to torem shapeList. ===
          toRem.add(trans);

          // === Compute new currTrans with labToRem replaced by lab. ===
          CellTransition newTrans = new CellTransition(trans);

          // --- replace labToRem in mother ---
          newTrans.getDaughter1().remove(labToRem);
          if (!newTrans.getDaughter1().labels.contains(lab))
          {
            newTrans.getDaughter1().add(lab);
          }

          // --- If needed, replace labToRem in sister ---
          if (trans.getDaughter2() != null && trans.getDaughter2().labels.contains(
              labToRem))
          {
            newTrans.getDaughter2().remove(labToRem);
            if (!newTrans.getDaughter2().labels.contains(lab))
            {
              newTrans.getDaughter2().add(lab);
            }
          }

          // === Add new currTrans to risksToAdd shapeList.
          toAdd.add(newTrans);
        }
        // ============= If Daughter1 boesn't contains labToRem =============
        //                but Daughter2 do.
        // Add currTrans to remove shapeList and compute new currTrans to add,
        // with labToRem replaced by lab in sister.
        else if (trans.getDaughter2() != null && trans.getDaughter2().labels.contains(
            labToRem))
        {
          // === Add currTrans to torem shapeList. ===          
          toRem.add(trans);

          // === Compute new currTrans with labToRem replaced by lab. ===
          CellTransition newTrans = new CellTransition(trans);
          newTrans.getDaughter2().remove(labToRem);
          if (!newTrans.getDaughter2().labels.contains(lab))
          {
            newTrans.getDaughter2().add(lab);
          }

          // === Add new currTrans to risksToAdd shapeList.
          toAdd.add(newTrans);
        }
      }
    }

    // ======== Remove all transitions of toRem shapeList from lineage ============
    lineage.removeAll(toRem);

//    for ( CellTransition trans : toRem )
//    {
//      lineage.remove( trans );
//    }
    // =========== Add all transitions of toRem shapeList to lineage ==============
    lineage.addAll(toAdd);

//    for ( CellTransition trans : toAdd )
//    {
//      lineage.add( trans );
//    }
  }

  /**
   * Merge two blobs in a ShapeSet and update lineage and risks accordingly.
   *
   * @param time time of the ShapeSet containing the two blobs to merge.
   * @param labToRem blobLab to merge and remove.
   * @param lab blobLab to merge and keep.
   */
  public void merge(int time, int labToRem, int lab)
  {
    ShapeSet set = blobListOverMovie.get(time);

    mergeLin(time, labToRem, lab);
    mergeRisks(risksFwd, time, labToRem, lab);
    mergeRisks(risksBack, time, labToRem, lab);

    set.merge(labToRem, lab);
  }

  /**
   * Merge in ShapeSets blobListOverMovie that are part of the same certain
   * cell.
   */
  public void mergeCertain()
  {

    // ================ For each ShapeSet of blobListOverMovie ========================
    // Merge blobs in certain cell 
    for (Map.Entry<Integer, ShapeSet> entry : blobListOverMovie.entrySet())
    {
      ShapeSet set = entry.getValue();
      int time = entry.getKey();

      // --------------- Init mergeFrom and mergeTo shapeList ----------------------------
      ArrayList<Integer> mergefrom = new ArrayList<Integer>();
      ArrayList<Integer> mergeto = new ArrayList<Integer>();

      // -------------- For each certain cell in ShapeSet ---------------------
      // Put in MergeFrom and mergeTo all blobs to be merge.
      for (Cell cell : set.possibleCells)
      {
        if (cell.certain && cell.blobsNb() > 1)
        {
          Iterator itr = cell.iterator();

          // === 'First' blob of the cell is record ===
          // to be the one all other blobs will be merge into.
          int first = (Integer) itr.next();

          // === For all other blobs of the cell ===
          // Add it to the mergeFrom shapeList, and add first to the mergeTo shapeList.
          while (itr.hasNext())
          {
            int lab = (Integer) itr.next();
            mergefrom.add(lab);
            mergeto.add(first);
          }
        }
      }

      // --- Merge all blobs in mergeFrom into its recorded first blob in mergeTo -------
      int size = mergefrom.size();
      for (int i = 0; i < size; i++)
      {
        int from = mergefrom.get(i);
        int to = mergeto.get(i);

        // Merge it.
        merge(time, from, to);

        // As a certain cell, remove all its connections with neighbors.
        set.connectGraph.remove(to);
      }
    }
  }

  // -------------------------------------------------------------------------
  //    toString methods
  // -------------------------------------------------------------------------
  /**
   * Describe all the currTrans risks.
   *
   * @return String describing all currTrans risks.
   */
  public String risksToString()
  {
    String res = "FORWARD : \n\n";

    for (Map.Entry<CellTransition, Double> entry : risksFwd.entrySet())
    {
      CellTransition currPair = entry.getKey();
      res += currPair.toString() + " : " + entry.getValue() + "\n";
    }

    res += "BACKWARD : \n\n";

    for (Map.Entry<CellTransition, Double> entry : risksBack.entrySet())
    {
      CellTransition currPair = entry.getKey();
      res += currPair.toString() + " : " + entry.getValue() + "\n";
    }

    return res;
  }

  // -------------------------------------------------------------------------
  //  Remove edges once graph connexion have been changed
  // -------------------------------------------------------------------------
  /**
   * Mergeg blobs that are too small to be a cell and have only one neighbor
   * with their neighbor.
   *
   * @param time
   * @param minCellArea
   * @return
   */
  public boolean removeEdges(int time, double minCellArea)
  {
    boolean changed = false;

    ArrayList<Integer> smallLabels = new ArrayList<Integer>();
    ShapeSet set = blobListOverMovie.get(time);

    // ============= list all small blobs ( inf to minCellArea) ==============
    for (Map.Entry<Integer, ShapeFbt> entry : set.shapeList.entrySet())
    {
      ShapeFbt blob = entry.getValue();
      int lab = entry.getKey();
      // if small blob
      if (blob.getSize() < minCellArea)
      {
        smallLabels.add(lab);
      }
    }

    // if there is no small blob, return  
    if (smallLabels.isEmpty())
    {
      return false;
    }

    // ========== For each blob with area inferior to minCellArea, =========
    // if it has one neighbor merge it.
    Iterator<Integer> itr = smallLabels.iterator();
    while (itr.hasNext())
    {
      int lab = (int) itr.next();

      // if blob has already been merged and is not small anymore, do nothing and continue.
      if (set.getArea(lab) > minCellArea)
      {
        continue;
      }

      // === compute all borders length === 
      // (key is neighbor blobLab and value is border length)
      HashMap<Integer, Double> borders = set.getBordersLength(lab);

      // === If only one neighbor, merge it ===
      if (borders.size() == 1)
      {
        int neigh = borders.keySet().iterator().next();
        //System.out.println( lab + " merged in " + neigh );
        merge(time, lab, neigh);
        set.removeInvalidLinks();
        changed = true;
      }

    }

    set.updateCellsCertainity();
    return changed;
  }

  // -------------------------------------------------------------------------
  // --- Correction methods used once the lineage has been construct
  // -------------------------------------------------------------------------
  public void editCell(int time, Cell cell, int blobLab) throws
      InterruptedException
  {
    // ======== if cell is not in this slice, return ==========================

    ShapeSet blobsSet = getBlobListOverMovie().get(time);
    ArrayList<Cell> cellsList = blobsSet.getPossibleCells();

    if (!(cell.isEmpty() || cellsList.contains(cell)))
    {
      System.out.println("Error in Blobsolver.editCell : cell " + cell.
          toString() + " was not in slice " + time + ".");
      return;
    }

    // ============ Edit cell in blobs ShapeSet possibleCells =================
    Cell oldCell = new Cell(cell);

    // ------------- if cell contains blobLab, --------------------------------
    // 1 ) remove it 
    // 2 ) mark cell has unlinked (donefor and doneBack false) and certain
    if (cell.contains(blobLab))
    {
      blobsSet.possibleCells.remove(cell);

      // 1 ) remove it 
      cell.remove(blobLab);

      if (cell.isEmpty())
      {
        blobsSet.deletedByUserCells.add(oldCell);
        blobsSet.addedByUserCells.remove(oldCell);
      }
      else
      {

        // 2 ) mark cell has unlinked (donefor and doneBack false) and certain
        cell.certain = true;
        cell.doneBack = false;
        cell.doneFor = false;

        Cell newCell = new Cell(cell);
        blobsSet.possibleCells.add(newCell);
        blobsSet.addedByUserCells.add(newCell);
        blobsSet.deletedByUserCells.remove(cell);
      }
    }
    // ------- if cell doesn't contains blobLab, ------------------------------
    // 1 ) If it is in another certain cell, fo nothing and return
    // 2 ) Else, add it to cell 
    // 3 ) mark cell has unlinked (donefor and doneBack false) and certain
    else
    {
      // 1 ) If it is in another certain cell, remove label from it.
      for (Cell currCell : blobsSet.possibleCells)
      {
        if (currCell.isCertain() && currCell.contains(blobLab))
        {
          blobsSet.possibleCells.remove(currCell);

          updateLineageAfterCellEdit(time, currCell);

          break;
        }
      }

      // 2 ) add it to cell 
      blobsSet.possibleCells.remove(cell);
      cell.add(blobLab);

      // 3 ) mark cell has unlinked (donefor and doneBack false) and certain
      cell.certain = true;
      cell.doneBack = false;
      cell.doneFor = false;

      Cell newCell = new Cell(cell);
      blobsSet.possibleCells.add(newCell);
      blobsSet.addedByUserCells.add(newCell);
      blobsSet.deletedByUserCells.remove(cell);
    }

    // ==================== Edit cell in lineage ==============================
//    updateLineageAfterCellEdit(time, cell);
    updateLineageAfterCellEdit(time, oldCell);
    blobsSet.reComputeCells();

    // ================== Edit cell in risks =================================
    updateRisks(time);

    // ============= recomputes transitions ===================================
    //   that can be compute in this new configuration 
    computeBestTransitions();

  }

  /**
   * Update lineage after a 'cell' was changed.
   *
   * Removes all currTrans containing 'cell'. Updates drawingColorsFor and
   * drawingColorsBack flags of cells. Recomputes currTranss that can be compute
   * with this new configuration.
   *
   * @param time slice the cell is in.
   * @param cell cell that was changed.
   */
  public void updateLineageAfterCellEdit(int time, Cell cell)
  {
    ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
    boolean delete = false;
    for (CellTransition trans : lineage)
    {
      if ((trans.time == time && trans.getMother().equals(cell))
          || (trans.time == time - 1 && trans.getDaughter1().equals(cell))
          || (trans.time == time - 1 && trans.getDaughter2() != null
              && trans.getDaughter2().equals(cell)))
      {
        delete = true;
      }

      if (delete)
      {
        toRem.add(trans);
      }

    }

    // ============ remove marked transitions from lineage ====================
    lineage.removeAll(toRem);

    for (CellTransition trans : toRem)
    {
      trans.updateForRemove(blobListOverMovie.get(trans.time),
                            blobListOverMovie.get(trans.time + 1));
    }

  }

  /**
   * Add a new cell to slice 'time' and update risks and lineage with it.
   *
   * @param time
   * @param newCell
   */
  public void addCell(int time, Cell newCell) throws InterruptedException
  {
    // ======== if cell is in this slice, return ==========================
    ShapeSet blobsSet = getBlobListOverMovie().get(time);
    ArrayList<Cell> cellsList = blobsSet.getPossibleCells();

    if (cellsList.contains(newCell))
    {
      System.out.println("Error in Blobsolver.editCell : cell " + newCell.
          toString() + " already in slice " + time + ".");
      return;
    }

    // ============ Add cell in blobs ShapeSet possibleCells =================
    blobsSet.updateCellAttributes(newCell);
    blobsSet.possibleCells.add(newCell);
    blobsSet.addedByUserCells.add(newCell);
    blobsSet.deletedByUserCells.remove(newCell);

    // ==================== Edit cell in lineage ==============================
    blobsSet.reComputeCells();

    // ================== Edit cell in risks =================================
    updateRisks(time);

    // ============= recomputes transitions ===================================
    //   that can be compute in this new configuration 
    computeBestTransitions();
  }

  public void deleteCell(int time, Cell cell) throws InterruptedException
  {
    // ======== if cell is not in this slice, return ==========================
    ShapeSet blobsSet = getBlobListOverMovie().get(time);
    ArrayList<Cell> cellsList = blobsSet.getPossibleCells();

    if (!cellsList.contains(cell))
    {
      System.out.println("Error in Blobsolver.editCell : cell " + cell.
          toString() + " was not in slice " + time + ".");
      return;
    }

    // ============ remove cell in blobs ShapeSet possibleCells =================
    blobsSet.updateCellAttributes(cell);
    blobsSet.possibleCells.remove(cell);
    blobsSet.addedByUserCells.remove(cell);
    blobsSet.deletedByUserCells.add(cell);

    // ==================== Edit cell in lineage ==============================
    updateLineageAfterCellEdit(time, cell);
    blobsSet.reComputeCells();

    // ================== Edit cell in risks =================================
    updateRisks(time);

    // ============= recomputes transitions ===================================
    //   that can be compute in this new configuration 
    computeBestTransitions();

  }

  /**
   * Edit a currTrans in lineage, update concerned cells and recompute best
   * currTranss.
   *
   * @param transition
   * @param newMother
   */
  public void editTransitionMother(CellTransition transition, Cell newMother)
      throws InterruptedException
  {


    // ======== If newMother is not a certain cell, return ====================
    ArrayList<Cell> currCells = getBlobListOverMovie().get(transition.time).possibleCells;
    if (!(newMother.certain && currCells.contains(newMother)))
    {
      return;
    }

    // ============= Delete currTrans from lineage ===========================
    deleteTransition(transition);

    // ========= if there are other transitions with ==========================
    //              newMother cell, delete them
    ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
    for (CellTransition currTrans : lineage)
    {
      if (currTrans.getMother().equals(newMother))
      {
        toRem.add(currTrans);
      }
    }

    for (CellTransition toRemTrans : toRem)
    {
      deleteTransition(toRemTrans);
    }

    // ========= Add new currTrans to lineage ================================
    CellTransition newTrans = new CellTransition(transition);
    newTrans.setMother(newMother);
    addTransition(newTrans);

    // ========== recompute risks and best transitions ========================
    updateRisks(transition.time);
    computeBestTransitions();

  }

  /**
   * TODO
   *
   * @param transition
   * @param newDaughter1
   * @param newDaughter2
   */
  public void editTransitionDaughter(CellTransition transition,
                                     Cell newDaughter1, Cell newDaughter2)
      throws InterruptedException
  {
    // ======== If currTrans is not in lineage, return =======================
//    if (!lineage.contains(transition))
//    {
//      return;
//    }

    // ======== If new daughters are not certains cells, return ===============
    ArrayList<Cell> currCells = getBlobListOverMovie().get(transition.time + 1).possibleCells;
    if (!(newDaughter1.certain && currCells.contains(newDaughter1)))
    {
      return;
    }

    if (newDaughter2 != null && !(newDaughter1.certain && currCells.contains(
                                  newDaughter1)))
    {
      return;
    }

    // ============= Delete currTrans from lineage ===========================
    if (lineage.contains(transition))
    {
      deleteTransition(transition);
    }
    
    // ========= if there are other transitions with ==========================
    //              new daughters cells, delete them
    ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
    for (CellTransition currTrans : lineage)
    {
      if (currTrans.getDaughter1().equals(newDaughter1))
      {
        toRem.add(currTrans);
      }
      else if (newDaughter2 != null && currTrans.getDaughter2() != null
               && newDaughter2.equals(currTrans.getDaughter2()))
      {
        toRem.add(currTrans);
      }
    }

    for (CellTransition toRemTrans : toRem)
    {
      deleteTransition(toRemTrans);
    }

    // ========= Add new currTrans to lineage ================================
    CellTransition newTrans = new CellTransition(transition);
    newTrans.setDaughter1(newDaughter1);
    newTrans.setDaughter2(newDaughter2);
    addTransition(newTrans);

    // ========== recompute risks and best transitions ========================
    updateRisks(transition.time + 1);
    computeBestTransitions();
  }

  /**
   * Delete a currTrans to lineage and update cells drawingColorsFor and
   * drawingColorsBackFlags in possibleCells lists.
   *
   * @param transition
   */
  public void deleteTransition(CellTransition transition)
  {
    if (!lineage.contains(transition))
    {
      return;
    }

    // ========= Init variables ===============================================
    ShapeSet currSet = getBlobListOverMovie().get(transition.time);
    ArrayList<Cell> currCells = currSet.possibleCells;
    ShapeSet daughterSet = getBlobListOverMovie().get(transition.time + 1);
    ArrayList<Cell> daughterCells = daughterSet.possibleCells;

    // ============= Delete currTrans from lineage ===========================
    lineage.remove(transition);
    currCells.get(currCells.indexOf(transition.getMother())).doneFor = false;
    daughterCells.get(daughterCells.indexOf(transition.getDaughter1())).doneBack = false;
    if (transition.getDaughter2() != null)
    {
      daughterCells.get(daughterCells.indexOf(transition.getDaughter2())).doneBack = false;
    }

  }

  /**
   * Add a currTrans to lineage and update cells drawingColorsFor and
   * drawingColorsBackFlags in possibleCells lists.
   *
   * @param transition
   */
  public void addTransition(CellTransition transition)
  {
    // ========= Init variables ===============================================
    ShapeSet currSet = getBlobListOverMovie().get(transition.time);
    ArrayList<Cell> currCells = currSet.possibleCells;
    ShapeSet daughterSet = getBlobListOverMovie().get(transition.time + 1);
    ArrayList<Cell> daughterCells = daughterSet.possibleCells;

    // ============= Delete currTrans from lineage ===========================
    lineage.add(transition);
    currCells.get(currCells.indexOf(transition.getMother())).doneFor = true;
    daughterCells.get(daughterCells.indexOf(transition.getDaughter1())).doneBack = true;
    if (transition.getDaughter2() != null)
    {
      daughterCells.get(daughterCells.indexOf(transition.getDaughter2())).doneBack = true;
    }
  }

  // -------------------------------------------------------------------------
  // ---  Show BlobSolver cells
  // -------------------------------------------------------------------------
  public void updateDrawingColors()
  {
    // =============== Variables ==============================================
    int nbCol = 0;

    ArrayList<Cell> isolatedCertainCells = new ArrayList<Cell>();
    ArrayList<Integer> isolatedCertainCellsTime = new ArrayList<Integer>();

    int size = blobListOverMovie.size();

    // ============================== List of cells colors ====================
    drawingColors = new HashMap[size];

    for (int i = 0; i < size; i++)
    {
      drawingColors[i] = new HashMap<Cell, Color>();
    }

    // ========== List all certains cells not in lineage ======================
    for (Map.Entry< Integer, ShapeSet> entry : blobListOverMovie.entrySet())
    {
      ShapeSet set = entry.getValue();
      int time = entry.getKey();

      for (Cell currCell : set.possibleCells)
      {
        if (currCell.isCertain())
        {
          if (!currCell.doneBack)
          {
            nbCol++;
//            System.out.println("Add color "+nbCol+" for new mother or isolated : "+ currCell.toString() );

            if (!currCell.doneFor)
            {
              isolatedCertainCells.add(currCell);
              isolatedCertainCellsTime.add(time);
            }
          }
        }
      }
    }

    for (CellTransition trans : lineage)
    {
      if (trans.getDaughter2() != null)
      {
        nbCol++;
//        System.out.println("Add color "+nbCol+" for new sister : "+ trans.sister.toString() );
      }
    }

    // ========= Init color colorPalette with number of certain cells ==============
    colorPalette = Utils.generateColors(nbCol);

    // =============== For each isolated certain cell, ========================
    //               record it with a new color.
    int isolatedNb = isolatedCertainCells.size();
    for (int i = 0; i < isolatedNb; i++)
    {
      Cell currCell = isolatedCertainCells.get(i);
      int currTime = isolatedCertainCellsTime.get(i);

      System.out.println("" + i + ", Colored " + currTime + ", " + currCell.
          toString());

      drawingColors[currTime - 1].put(currCell, colorPalette[i]);
    }

    // ========= Record all other certains cells according to lineage. =========
    int colorIndex = isolatedNb;

    // -------------- For each transition in lineage, -------------------------
    // 1 ) If mother were already in one of the drawingColors keep the color for mother
    // 2 ) Else, use a new color
    // 3 ) if sister exists give it a new color.
    TreeSet<CellTransition> sortedLineage = new TreeSet<CellTransition>(lineage);
    for (CellTransition currTrans : sortedLineage)
    {
      // === 1 ) If mother  were already in one of the drawingColors ===
      // keep the color for mother
      Color currCol;
      HashMap<Cell, Color> motherDoneMap = drawingColors[currTrans.time - 1];
      HashMap<Cell, Color> daughtersDoneMap = drawingColors[currTrans.time];
      if (motherDoneMap.containsKey(currTrans.getMother()))
      {
        currCol = motherDoneMap.get(currTrans.getMother());

        if (daughtersDoneMap.put(currTrans.getDaughter1(), currCol) != null)
        {
          System.out.println("Error : " + currTrans.getDaughter1().toString()
                             + " already colored.");
        }
      }
      // === 2 ) Else, use a new color ===
      else
      {

//        if( currTrans..doneBack)
//        {
//          System.err.println("Error ! mother is doneBack : " + currTrans.toString());
//          System.err.println("Shouldn't be new color.");
//          continue;
//        }
        currCol = colorPalette[colorIndex];
        colorIndex++;

        if (motherDoneMap.put(currTrans.getMother(), currCol) != null)
        {
          System.out.println("Error : " + currTrans.getMother().toString()
                             + " already colored.");
        }

        if (daughtersDoneMap.put(currTrans.getDaughter1(), currCol) != null)
        {
          System.out.println("Error : " + currTrans.getDaughter1().toString()
                             + " already colored.");
        }
      }

      // === 3 ) if sister exists give it a new color. ===
      if (currTrans.getDaughter2() != null)
      {

        if (daughtersDoneMap.
            put(currTrans.getDaughter2(), colorPalette[colorIndex]) != null)
        {
          System.out.println("Error : " + currTrans.getDaughter2().toString()
                             + " already colored.");
        }
        colorIndex++;

      }
    }

  }

  /**
   * Construct an ImageStack representing certains cells, with color according
   * to lineage.
   *
   * @return imageStack
   */
  public ImageStack getLinImg()
  {

    // ========== Init res Image Stack ========================================
    int size = blobListOverMovie.size();
    ImageStack res = new ImageStack(width, height);

    // ---------------- fill it with black ColorProcessor --------------------
    // === fill a color processor to black ===
    ColorProcessor blackColProc = new ColorProcessor(width, height);
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        blackColProc.setColor(Color.BLACK);
        blackColProc.drawPixel(x, y);
      }
    }

    // === fill res with duplicates of black processor ===
    for (int i = 0; i < size; i++)
    {
      res.addSlice(blackColProc.duplicate());
    }

    // ========== Paint all cells with color taken in drawingColors. ==========
    for (int i = 0; i < size; i++)
    {
      ImageProcessor currProc = res.getProcessor(i + 1);
      HashMap<Cell, Color> currMap = drawingColors[i];
      ShapeSet currSet = blobListOverMovie.get(i + 1);

      for (Map.Entry<Cell, Color> entry : currMap.entrySet())
      {
        Cell currCell = entry.getKey();
        Color currColor = entry.getValue();
        currProc.setColor(currColor);

        for (int blobLab : currCell.labels)
        {
          ShapeFbt blob = currSet.getShape(blobLab);
          for (Point pix : blob.pixels)
          {
            currProc.drawPixel(pix.x, pix.y);
          }
        }
      }
    }

    // ======= Paint all blobs borders in gray color ==========================
    for (Map.Entry<Integer, ShapeSet> entryListOverMovie : blobListOverMovie.
        entrySet())
    {
      ShapeSet shapeList = entryListOverMovie.getValue();
      ImageProcessor currProc = res.getProcessor(entryListOverMovie.getKey());
      currProc.setColor(Color.gray);

      for (Map.Entry<Integer, ShapeFbt> entryShape : shapeList.shapeList.
          entrySet())
      {
        ShapeFbt currShape = entryShape.getValue();

        for (Point pt : currShape.boundary)
        {
          currProc.drawPixel(pt.x, pt.y);
        }
      }
    }

    return res;
  }

  // -------------------------------------------------------------------------
  // ---  Show results
  // -------------------------------------------------------------------------
  /**
   * Add a result row in results table.
   *
   * @param results result table
   * @param id id of the cell
   * @param divs divisions of the cell ( H for head, T for tail )
   * @param length length of the cell
   * @param width width of the cell
   * @param frame frame number
   * @param x center x position of the cell
   * @param y center y position of the cell
   * @param orientation orientation of the cell
   * @param labels blobs labels of the cell
   */
  protected void addResult(ResultsTable results, int id, String divs,
                           double length,
                           double width, int frame, int x, int y,
                           double orientation, String labels)
  {
    results.incrementCounter();
    results.addValue("id", id);
    results.addLabel("divisions", divs);
    results.addValue("length", length);
    results.addValue("width", width);
    results.addValue("frame", frame);
    results.addValue("x", x);
    results.addValue("y", y);
    results.addValue("orientation", orientation);
    results.addLabel("labels", labels);
  }

  /**
   * Add a result row in results table.
   *
   * @param results results table
   * @param id id of the cell
   * @param divs string registering divisions ( H = head, T = tail)
   * @param cell cell
   * @param frame frame number
   */
  protected void addResult(ResultsTable results, int id, String divs, Cell cell,
                           int frame)
  {
    addResult(results, id, divs, 0, cell.width, frame, cell.center.x,
              cell.center.y, cell.orientation, cell.toString());
  }

  /**
   * Put lineage informations in a results table. Each row describes a cell in a
   * given frame. Cell id, length, width, frame number, center position,
   * orientation, blobs labels.
   *
   * @return resultsTable
   */
  public ResultsTable getResults()
  {
    // ====================== Init Variables ==================================
    // results table
    ResultsTable results = new ResultsTable();
    // cell id
    int name = -1;

    // ======================= get lineage copy ===============================
    TreeSet<CellTransition> transitionsList = new TreeSet<CellTransition>(
        lineage);

    // ============= While lineage copy still contains transition =============
    // 1 ) Get first transition
    // 2 ) Check if mother cell is already in result
    // 3 ) If mother cell not in results, add it
    // 4 ) Add mother(s) cell(s) to results
    while (!transitionsList.isEmpty())
    {
      // -------------- 1 ) Get first transition ------------------------------
      CellTransition currTrans = transitionsList.first();

      // ----------- 2 ) Check if mother cell is already in result ------------
      String divs = "";
      boolean isMotherInResults = false;
      for (int row = 0; row < results.getCounter(); row++)
      {
        if (results.getValue("frame", row) == currTrans.time)
            //&& results.getValue("labels", row).equals(currTrans.getMother().
            //toString()))
        {
          isMotherInResults = true;
          name = (int) results.getValue("id", row);
          //divs = results.getValue("divisions", row);
          break;
        }
      }

      // ----------- 3 ) If mother cell not in results, add it ----------------
      if (!isMotherInResults)
      {
        name++;
        addResult(results, name, "", currTrans.getMother(), currTrans.time);
      }

      // ------- 4 ) Add mother(s) cell(s) to results -----------------------
      // First one gets the tag H for head
      // Second one gets the tag T for tail
      if (currTrans.getDaughter2() == null)
      {
        addResult(results, name, divs, currTrans.getDaughter1(), currTrans.time + 1);
      }
      else
      {
        addResult(results, name, divs + "H", currTrans.getDaughter1(), currTrans.time
                                                                  + 1);
        addResult(results, name, divs + "T", currTrans.getDaughter2(), currTrans.time
                                                                  + 1);
      }

      transitionsList.remove(currTrans);

    }

    // ============ Return results ============================================
    return results;
  }

  // -------------------------------------------------------------------------
  // ---  Skelets
  // -------------------------------------------------------------------------
  /**
   * Constructs list of cells segmented by blobsolver as an HashMap. The results
   * map has as key a int array with first element cell id and second frame
   * number of the cell, and as value an HashSet of Point contained in the cell.
   *
   * @return HashMap listing all segmented cells.
   */
  public HashMap< int[], HashSet<Point>> getLineageCellsList()
  {
    // ====================== Init Variables ==================================
    HashMap< int[], HashSet<Point>> results = new HashMap<int[], HashSet<Point>>();
    ArrayList<Cell> done = new ArrayList<Cell>();
    // cell id
    int id = -1;

    // ======================= get lineage copy ===============================
    TreeSet<CellTransition> transitionsList = new TreeSet<CellTransition>(
        lineage);

    // ============= While lineage copy still contains transition =============
    // 1 ) Get first transition
    // 2 ) If mother cell not in results, add it
    // 3 ) Add mother(s) cell(s) to results
    // 4 ) remove transition from transitions list
    while (!transitionsList.isEmpty())
    {
      // -------------- 1 ) Get first transition ------------------------------
      CellTransition currTrans = transitionsList.first();

      // ----------- 2 ) If mother cell not in results, add it ----------------
      if (!done.contains(currTrans.getMother()))
      {
        // increment cell id .
        id++;
        // create key with id and time
        int[] key =
        {
          id, currTrans.time
        };
        // put mother cell in results and in done list.
        results.put(key, getCellPoints(currTrans.time, currTrans.getMother()));
        done.add(currTrans.getMother());
      }

      // ------- 3 ) Add mother(s) cell(s) to results -----------------------
      // increment id 
      id++;
      // create key with id and time
      int[] key =
      {
        id, currTrans.time + 1
      };
      // put mother cell in results and in done list.
      results.put(key, getCellPoints(currTrans.time + 1, currTrans.getDaughter1()));
      done.add(currTrans.getDaughter1());

      // if sister is not null, add it too.
      if (currTrans.getDaughter2() != null)
      {
        // increment id 
        id++;
        // create key with id and time
        int[] key2 =
        {
          id, currTrans.time + 1
        };
        // put mother cell in results and in done list.
        results.
            put(key2, getCellPoints(currTrans.time + 1, currTrans.getDaughter2()));
        done.add(currTrans.getDaughter2());
      }

      // ----------- 4 ) remove transition from transitions list --------------
      transitionsList.remove(currTrans);

    }

    // returns results
    return results;
  }

  /**
   * Constructs an ImageFbt corresponding to a frame 'time' with pixel value = 0
   * if not in a cell and pixel value = id cell when pixel in cell.
   *
   * @param time frame number
   * @return ImageFbt
   */
  public ImageFbt getLineageCellsImg(int time)
  {
    // ================= Initialisation of variables ==========================
    int size = width * height;
    short[] zeros = new short[size];
    for (int i = 0; i < size; i++)
    {
      zeros[i] = 0;
    }

    ImageFbt res = new ImageFbt(width, height, zeros);
    int label = 1;
    boolean add;

    // ================= For each transition of lineage =======================
    // 1 ) if transition is time 'time' : add mother cell pixels to ImageFbt results
    // 2 ) else if transition if time 'time - 1' : add mother(s) cell(s) to 
    // ImageFbt results. 
    for (CellTransition currTrans : lineage)
    {
      // ----------- 1 ) if transition is time 'time' -------------------------
      // add mother cell pixels to ImageFbt results
      if (currTrans.time == time)
      {
        add = false;

        // put all pixels in  mother cell to the value 'label'.
        for (Point currPix : getCellPoints(time, currTrans.getMother()))
        {
          if (res.get(currPix.x, currPix.y) == 0)
          {
            res.set(currPix.x, currPix.y, label);
            add = true;
          }
        }

        // if at least one pixel was in cell, increment 'label'.
        if (add == true)
        {
          label++;
        }
      }

      // ---------------- 2 ) else if transition if time 'time - 1' -----------
      // add mother(s) cell(s) to ImageFbt results. 
      else if (currTrans.time == time - 1)
      {
        add = false;
        // put all pixels in  mother cell to the value 'label'.
        for (Point currPix : getCellPoints(time, currTrans.getDaughter1()))
        {
          if (res.get(currPix.x, currPix.y) == 0)
          {
            res.set(currPix.x, currPix.y, label);
            add = true;
          }
        }

        // if at least one pixel was in cell, increment 'label'.
        if (add == true)
        {
          label++;
        }

        // --- if sister is not null add it too. ---
        if (currTrans.getDaughter2() != null)
        {
          add = false;
          // put all pixels in  mother cell to the value 'label'.
          for (Point currPix : getCellPoints(time, currTrans.getDaughter2()))
          {
            if (res.get(currPix.x, currPix.y) == 0)
            {
              res.set(currPix.x, currPix.y, label);
              add = true;
            }
          }

          // if at least one pixel was in cell, increment 'label'.
          if (add == true)
          {
            label++;
          }
        }
      }
    }

    // ============================== return results ==========================
    return res;
  }

  /**
   * Takes an ImageFbt produced by getLineageCellsImg and fill eventual holes
   * that could be in cells. This is done transforming image in binary image and
   * eroding it.
   *
   * @param cellsImg ImageFbt produced by getLineageCellsImg
   * @param erosionRadius radius used for erosion
   * @return array of ImageFbt with holes filled : first is image with cells
   * labels second is binary image.
   * @throws InterruptedException
   */
  public ImageFbt[] fillHolesInCells(ImageFbt cellsImg, double erosionRadius)
      throws InterruptedException
  {
    // =========== Variables =================================================
    // original cells image copy
    ImageFbt fillCellsImg = new ImageFbt(cellsImg);
    // init binary image
    ImageFbt binImg = new ImageFbt(width, height, (int[]) null);
    // image pixels size
    int size = width * height;

    // ------------ construct Binary image ------------------------------------
    // 0 -> pixel in cell, 1 -> pixel not in cell
    for (int i = 0; i < size; i++)
    {
      int currLab = cellsImg.get(i);

      if (currLab != 0)
      {
        binImg.set(i, 0);
      }
      else
      {
        binImg.set(i, 1);
      }
    }

    // ================= Fill eventual holes in cells ======================== 
    // copy binary image
    ImageFbt fillBinImg = new ImageFbt(binImg);
    // erode fillBinImg
    fillBinImg.erode(erosionRadius);
    // do a geodesic dilatation on fillBinImg.
    fillBinImg.geodesicDilate(erosionRadius, binImg);

    // ============= For each pixel ===========================================
    // 1 ) inverse fillBinImg pixel ( 0->1 and 1->0 )
    // 2 )
    for (int i = 0; i < size; i++)
    {

      // --------- 1 ) inverse fillBinImg pixel ( 0->1 and 1->0 ) -------------
      // it means that for now on 0 -> not in cell, 1 -> in cell
      if (fillBinImg.get(i) == 0)
      {
        fillBinImg.set(i, 1);
      }
      else
      {
        fillBinImg.set(i, 0);
      }

      // ------ 2 ) if pixel is registered as not in cell in labels image 
      // but not in cell in binary image, find neighbor label and put it to the
      // pixel in labels image.
      if (fillCellsImg.get(i) == 0 && fillBinImg.get(i) == 1)
      {
        // get pixel point.
        Point pixel = Utils.indexToPoint(i, width, height);

        // if right neighbor is labeled take its label
        if (fillCellsImg.get(pixel.x + 1, pixel.y) != 0)
        {
          fillCellsImg.set(i, fillCellsImg.get(pixel.x + 1, pixel.y));
        }
        // if bottom neighbor is labeled take its label
        else if (fillCellsImg.get(pixel.x, pixel.y + 1) != 0)
        {
          fillCellsImg.set(i, cellsImg.get(pixel.x, pixel.y + 1));
        }
        // if left neighbor is labeled take its label
        else if (fillCellsImg.get(pixel.x - 1, pixel.y) != 0)
        {
          fillCellsImg.set(i, cellsImg.get(pixel.x - 1, pixel.y));
        }
        // if top neighbor is labeled take its label
        else if (fillCellsImg.get(pixel.x, pixel.y - 1) != 0)
        {
          fillCellsImg.set(i, fillCellsImg.get(pixel.x, pixel.y - 1));
        }

      }

    }

    // =========== Return filled images ======================================
    ImageFbt[] res =
    {
      fillCellsImg, fillBinImg
    };
    return res;
  }

  /**
   * Constructs morphological skelets of segmented cells in frame 'time'.
   *
   * Note : erosion radius is 1 in Mael Primet Code.
   *
   * @param time frame number
   * @param erosionRadius radius of erosion to fill holes in cells.
   * @param skeletThreshold threshold of pixels in skelet or not.
   * @return HashMap : key -> cell id, value -> ArrayList of pixels indexes.
   * @throws InterruptedException
   */
  public HashMap<Integer, ArrayList<Integer>> morphologicalSkelet(int time,
                                                                  double erosionRadius,
                                                                  int skeletThreshold)
      throws InterruptedException
  {
    int size = width * height;

    // =============== Get labels image of cells ============================== 
    ImageFbt cellsLabelsImg = getLineageCellsImg(time);

    // ============== Get maximum label ( = nb of cells) ======================
    int maxLab = 0;
    for (int i = 0; i < size; i++)
    {
      if (cellsLabelsImg.get(i) > maxLab)
      {
        maxLab = cellsLabelsImg.get(i);
      }
    }

    // ============== if no cells, return ===================================
    if (maxLab == 0)
    {
      return null;
    }

    //  =========== fill eventual holes in cells ==============================
    ImageFbt[] fillres = fillHolesInCells(cellsLabelsImg, erosionRadius);
    ImageFbt fillCellsImg = fillres[0];
    ImageFbt fillBinImg = fillres[1];

    //  ====================== init eroded image ==============================
    // copy of filled binary image
    ImageFbt cellsImgEroded = new ImageFbt(fillBinImg);

    // ===================  init skeleton img to 0 ============================
    short[] zeros = new short[size];
    for (int i = 0; i < size; i++)
    {
      zeros[i] = 0;
    }
    ImageFbt skeletImg = new ImageFbt(width, height, zeros);

    // ============= MAIN LOOP ================================================
    while (true)
    {

      // -------------- erode image -------------------------------------------
      cellsImgEroded.erode(erosionRadius);

      // -------------- If image is all black, break --------------------------
      boolean noCells = true;
      for (int i = 0; i < size; i++)
      {
        if (cellsImgEroded.get(i) != 0)
        {
          noCells = false;
          break;
        }
      }

      if (noCells)
      {
        break;
      }

      // -------------- dilate image = cellsImgTmp ----------------------------
      ImageFbt cellsImgTmp = new ImageFbt(cellsImgEroded);
      cellsImgTmp.dilate(erosionRadius);

      // ----------- remove opened image from old image -----------------------
      // and multiply the value by i
      // to get skelet image
      for (int i = 0; i < size; i++)
      {
        int val = i * (fillBinImg.get(i) - cellsImgTmp.get(i));
        skeletImg.set(i, skeletImg.get(i) + val);
      }

      // ---------- Set fillBinImg = eroded image to continue loop ------------
      fillBinImg = cellsImgEroded.copy();

    }

    // ======================= List skelets points ============================
    HashMap<Integer, ArrayList<Integer>> skelList
                                         = new HashMap<Integer, ArrayList<Integer>>();

    // --------------- For each skelet init Point indexes List ----------------
    for (int i = 1; i <= maxLab; i++)
    {
      skelList.put(i, new ArrayList<Integer>());
    }

    // ------------- For each pixels in skelet image --------------------------
    // If pixel > skeletThreshold, add it to skelet point list.
    for (int i = 0; i < size; i++)
    {
      if (skeletImg.get(i) > skeletThreshold)
      {
        int key = fillCellsImg.get(i);

        if (key != 0)
        {
          ArrayList<Integer> currskel = (ArrayList<Integer>) (skelList.get(key));
          currskel.add(i);
        }
      }
    }

    // =============== returns skelet list ===================================
    return skelList;
  }

  /**
   * Computes morphological skelet of all frames and show them in an ImageStack.
   *
   * @param erosionRadius radius of erosion to fill holes in cells.
   * @param skeletThreshold threshold of pixels in skelet or not.
   * @throws InterruptedException
   */
  public void showSkelet(double erosionRadius, int skeletThreshold) throws
      InterruptedException
  {
    // ================= Init variables =======================================
    int frameNb = getBlobListOverMovie().size();
    ImageStack toShow = new ImageStack(width, height);

    // ================ For each frame, =======================================
    // 1 ) computes morphological skelets
    // 2 ) computes colorProcessor showing skelets and add it to imageStack
    for (int i = 1; i <= frameNb; i++)
    {
      // ------- 1 ) computes morphological skelets --------------------------
      HashMap<Integer, ArrayList<Integer>> skelRes = morphologicalSkelet(i,
                                                                         erosionRadius,
                                                                         skeletThreshold);

      // -- 2 ) computes colorProcessor showing skelets -----------------------
      // and add it to imageStack
      ColorProcessor skelCP = new ColorProcessor(width, height);
      colorPalette = Utils.generateColors(skelRes.size());

      // === for each skelet ===
      // Color each pixels
      for (Map.Entry<Integer, ArrayList<Integer>> entry : skelRes.entrySet())
      {
        int lab = entry.getKey();
        ArrayList<Integer> indexes = entry.getValue();

        // get color
        skelCP.setColor(colorPalette[lab - 1]);

        // draw pixels
        for (int index : indexes)
        {
          Point coord = Utils.indexToPoint(index, width, height);
          skelCP.drawPixel(coord.x, coord.y);
        }

      }

      // add image to skelet ImageStack
      toShow.addSlice(skelCP);
    }

    // =================== Show ImageStack ====================================
    ImagePlus toShowIP = new ImagePlus("Skelets", toShow);
    toShowIP.show();
  }

  // ---------------- skelet vectorisation ------------------------------------
  /**
   * Vectorizes morphological skelet to get a single line skelet.
   *
   * @param minPointsNb minimum point number in a skelet.
   * @param currSkel pixels indexes list of morpholigical skelet
   * @return vectorize skelet
   */
  public ArrayList<Integer> skeletVectorisation(int minPointsNb,
                                                ArrayList<Integer> currSkel)
  {
    // ============= If skelet is too small return it unchanged ===============
    if (currSkel.size() < minPointsNb)
    {
      System.out.println("Warning : small number of point in skelet ");
      return new ArrayList<Integer>(currSkel);
    }
    // ========== Else, vectorize it ==========================================
    // 1 ) delete isolated points, then register current points number
    // 2 ) extract branch from skelwithoutisolated
    // 3 ) If branch too small, redo this 5 times max until finding a good branch,
    //       or not enough points left.
    else
    {

      // === 1 ) delete isolated points, then register current points number ==
      ArrayList<Integer> skelWithoutIsolated = deleteIsolatedPoints(currSkel);
      int nskel = skelWithoutIsolated.size();

      // ======== 2 ) extract branch from skelwithoutisolated =================
      ArrayList<Integer> branch = extractSkeletBranch(skelWithoutIsolated);

      // =========  3 ) If branch too small, redo this ========================
      // 5 times max until finding a good branch, or not enough points left.
      int iter = 0;
      while (branch.size() < nskel / 2)
      {
        // delete isolated and register number of points left.
        skelWithoutIsolated = deleteIsolatedPoints(skelWithoutIsolated);
        nskel = skelWithoutIsolated.size();

        // Remove already selected points in earlier branches 
        skelWithoutIsolated.removeAll(branch);

        // If more than 5 times or not enough point left, return currSkel.
        if (iter > 5 || skelWithoutIsolated.size() < nskel / 2)
        {
          return currSkel;
        }

        // extract branch
        branch = extractSkeletBranch(skelWithoutIsolated);
        iter++;
      }

      // return branch
      return branch;

    }

  }

  /**
   * Removes from an array list of Points indexes the points that don't have any
   * neighbors in the list.
   *
   * @param skel list of points indexes.
   * @return new list without isolated points.
   */
  public ArrayList<Integer> deleteIsolatedPoints(ArrayList<Integer> skel)
  {
    // =========== Init variable ==============================================
    ArrayList<Integer> results = new ArrayList<Integer>();

    // ======= For each index =================================================
    // 1 ) computes its neighbors
    // 2 ) check if skel contains one of its neighbor, if it does, add it to result.
    for (int currIndex : skel)
    {
      boolean hasNeighbor = false;

      // ---------------- 1 ) computes its neighbors --------------------------
      HashSet<Integer> neighbors = Utils.getNeighIndex(currIndex, width, height,
                                                       true); // connect 8

      // ---------- 2 ) check if skel contains one of its neighbor ------------
      for (int currNeigh : neighbors)
      {
        if (skel.contains(currNeigh))
        {
          hasNeighbor = true;
          break;
        }
      }

      // if it does, add it to result.
      if (hasNeighbor)
      {
        results.add(currIndex);
      }
    }

    return results;
  }

  /**
   * Extract branch ( line of point ) from a skelet.
   *
   * @param skel pixels indexes list of skelet.
   * @return pixels indexes list of extracted branch.
   */
  public ArrayList<Integer> extractSkeletBranch(ArrayList<Integer> skel)
  {
    ArrayList<Point> resultsPt = new ArrayList<Point>();

    // =========== Convert to Points ========================================
    ArrayList<Point> pointsList = new ArrayList<Point>();
    for (int currIndex : skel)
    {
      pointsList.add(Utils.indexToPoint(currIndex, width, height));
    }

    // ============== gravity center ========================================
    Point center = new Point(0, 0);
    for (Point pt : pointsList)
    {
      center.x += pt.x;
      center.y += pt.y;
    }

    center.x /= pointsList.size();
    center.y /= pointsList.size();

    // =============== Computes branch ========================================
    // -------------- Find an extrimity point ---------------------------------
    Point newPt = pointsList.get(0);
    Point extremityPt = null;
    double maxDist = 0;

    for (Point pt : pointsList)
    {
      double currDist = newPt.distance(pt);

      if (currDist > maxDist)
      {
        maxDist = currDist;
        extremityPt = pt;
      }
    }

    pointsList.remove(extremityPt);
    resultsPt.add(extremityPt);
    double maxStep = extremityPt.distance(center);

    // ---------- extract branch from extremity -------------------------------
    double centerTheta = Math.atan2(center.y - extremityPt.y, center.x
                                                              - extremityPt.x);
    double selectedTheta = new Double(centerTheta);
    double oldTheta = new Double(centerTheta);
    double oldTheta2 = new Double(centerTheta);
    double deltaThetaOld = 0;
    double angleM3 = 0;
    double angleM6 = 0;
    Point selectedPt = null;
    int nreject = 0;

    int k = 0;
    while (true)
    {
      k++;

      double selectedDist = Double.MAX_VALUE;

      for (Point pt : pointsList)
      {
        boolean select = false;

        double currDist = extremityPt.distance(pt);
        double currTheta = Math.
            atan2(pt.y - extremityPt.y, pt.x - extremityPt.x);

        if (currDist < selectedDist)
        {
          select = true;
        }
        else if (currDist == selectedDist)
        {

          double normCurrTheta = Utils.normalizedAngle(currTheta);
          normCurrTheta = Math.abs(normCurrTheta);

          double diffTmpTheta = Utils.normalizedAngle(selectedTheta - oldTheta);
          diffTmpTheta = Math.abs(diffTmpTheta);

          if ((normCurrTheta - oldTheta) < diffTmpTheta)
          {
            select = true;
          }
          else if ((normCurrTheta - oldTheta) == diffTmpTheta)
          {
            double diffcenterTheta = Utils.normalizedAngle(centerTheta
                                                           - oldTheta);
            diffcenterTheta = Math.abs(diffcenterTheta);

            if ((normCurrTheta - centerTheta) < diffcenterTheta)
            {
              select = true;
            }
          }
        }

        if (select)
        {
          selectedDist = currDist;
          selectedTheta = currTheta;
          selectedPt = pt;
        }
      }

      // remove point from list : 
      pointsList.remove(selectedPt);

      // TODO Lot of tests to reject or accpte point
      if (k > 5)
      {
        Point pt3 = resultsPt.get(resultsPt.size() - 3);
        Point pt6 = resultsPt.get(resultsPt.size() - 6);
        angleM3 = Math.atan2(selectedPt.y - pt3.y, selectedPt.x - pt3.x);
        angleM6 = Math.atan2(selectedPt.y - pt6.y, selectedPt.x - pt6.x);
      }

      // point must not be further than half the cell 
      if (selectedDist > maxStep || pointsList.isEmpty() || nreject > 10)
      {
        break;
      }

      // points must not be too close to other selected points.
      boolean tooClose = false;
      for (Point otherPt : resultsPt)
      {
        double dist = selectedPt.distance(otherPt);
        if (dist < selectedDist)
        {
          tooClose = true;
          break;
        }
      }

      if (tooClose)
      {
        nreject++;
        k--;
        continue;
      }

      // If orientation is too different from other points, reject
      double testAngle = Utils.normalizedAngle(oldTheta2 - selectedTheta);
      testAngle = Math.abs(testAngle);

      if (testAngle > Math.PI / 2)
      {
        nreject++;
        k--;
        continue;
      }

      // Demi-tour : reject
      if (k > 2)
      {
        testAngle = Utils.normalizedAngle(selectedTheta - oldTheta);
        if (deltaThetaOld == testAngle && Math.abs(deltaThetaOld) == Math.PI / 2)
        {
          nreject++;
          k--;
          continue;
        }

        // Demi-tour 2 : reject
        testAngle = Math.abs(deltaThetaOld + selectedTheta - oldTheta);
        if (testAngle == 0.75 * Math.PI)
        {
          nreject++;
          k--;
          continue;
        }
      }

      testAngle = Math.abs(Utils.normalizedAngle(selectedTheta - oldTheta));

      if (testAngle == 0.75 * Math.PI)
      {
        nreject++;
        k--;
        continue;
      }

      testAngle = Math.abs(Utils.normalizedAngle(angleM3 - angleM6));
      if (testAngle > Math.PI / 2)
      {
        nreject++;
        k--;
        continue;
      }

      // accept point
      nreject = 0;
      deltaThetaOld = Utils.normalizedAngle(selectedTheta - oldTheta);

      if (k == 1)
      {
        oldTheta2 = selectedTheta;
      }
      else
      {
        oldTheta2 = Utils.meanAngles(selectedTheta, oldTheta);
      }
      oldTheta = selectedTheta;

      extremityPt = selectedPt;
      resultsPt.add(selectedPt);

    }

    ArrayList<Integer> results = new ArrayList<Integer>();

    for (Point pt : resultsPt)
    {
      results.add(Utils.pointToIndex(pt, width, height));
    }

    return results;
  }

  /**
   * Computes morphological skelet of all frames, vecotrize them and show them
   * in an ImageStack.
   *
   * @param erosionRadius radius of erosion to fill holes in cells.
   * @param skeletThreshold threshold of pixels in skelet or not.
   * @throws InterruptedException
   */
  public void showVectSkelet(double erosionRadius, int skeletThreshold) throws
      InterruptedException
  {
    // ================= Init variables =======================================
    int frameNb = getBlobListOverMovie().size();
    ImageStack toShow = new ImageStack(width, height);

    // ================ For each frame, =======================================
    // 1 ) computes morphological skelets, vectorize it
    // 2 ) computes colorProcessor showing skelets and add it to imageStack
    for (int i = 1; i <= frameNb; i++)
    {
      // ------- 1 ) computes morphological skelets --------------------------
      HashMap<Integer, ArrayList<Integer>> skelRes = morphologicalSkelet(i,
                                                                         erosionRadius,
                                                                         skeletThreshold);

      // -- 2 ) computes colorProcessor showing skelets -----------------------
      // and add it to imageStack
      ColorProcessor skelCP = new ColorProcessor(width, height);
      colorPalette = Utils.generateColors(skelRes.size());

      // === for each skelet ===
      // Vectorize it and Color each pixels
      for (Map.Entry<Integer, ArrayList<Integer>> entry : skelRes.entrySet())
      {
        int lab = entry.getKey();
        ArrayList<Integer> indexes = entry.getValue();
        indexes = skeletVectorisation(8, indexes);

        // get color
        skelCP.setColor(colorPalette[lab - 1]);

        // draw pixels
        for (int index : indexes)
        {
          Point coord = Utils.indexToPoint(index, width, height);
          skelCP.drawPixel(coord.x, coord.y);
        }

      }

      // add image to skelet ImageStack
      toShow.addSlice(skelCP);
    }

    // =================== Show ImageStack ====================================
    ImagePlus toShowIP = new ImagePlus("Skelets", toShow);
    toShowIP.show();
  }
}
