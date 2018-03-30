package cellst.Image;

import static java.lang.Thread.sleep;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Arrow;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Filler;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import ij.util.Java2;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.BiPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;

import utilities.io_reader;
import Denoise.ROF_Denoise;
import Editor.Ins_editor;
import Ij_Plugin.Ins_ParticleAnalyzer;
import Threshold.Auto_Local_Threshold;
import Threshold.Auto_Threshold;
import Threshold.Auto_Threshold_v;
import cellst.Main.Fluo_Bac_Tracker;
import cellst.Main.Utils;


//############################################################################
//#                              Image Fbt class                             #
//############################################################################
/**
 * Fluo_Bac_Tracker Image class. It contains all the main methods of image
 * treatment as dilatation, erosion, non homogeneous dilatation ...
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class ImageFbt extends ShortProcessor implements Serializable
{
  // =========================================================================
  //                            Attributes
  // =========================================================================
  // For now most attributes are inherited from shortprocessor

  /**
   * Back ground mask ( 1 if in background, 0 if not )
   */
  protected int[] background = (int[]) null;
  /**
   * A static final background labelList.
   */
  protected static final int BACKGROUND_LABEL = -1;
  
  protected static final int BACKGROUND_MASK = -2;
  /**
   * Suffix to add to filename to get the file where to save backgrounds.
   */
  public static final String BACKGROUND_FILENAME = "background";
  
  final static int[] DIR_X_OFFSET = new int[] {  0,  1,  1,  1,  0, -1, -1, -1 };
  final static int[] DIR_Y_OFFSET = new int[] { -1, -1,  0,  1,  1,  1,  0, -1 };

  
  public static int startLabel = 50;
  public static int minHist = 20;
  // =========================================================================
  //                               Constructors
  // =========================================================================
  /**
   * ImageFbt Constructor : requires images dimensions and a background mask.
   * The image will be initialize to zero on each pixel.
   *
   * @param width Image width.
   * @param height Image height.
   * @param _background Background mask : array of int ( 1 if in background and
   * else 0 ); Background can be set to "(int[]) null" if it is of no use (ex :
   * temporary image). But in that case, errors can be raised if a method
   * needing a background is called : Background must be set before calling one
   * of these.
   *
   */
  public ImageFbt(int width, int height, int[] _background)
  {
    // === Herited constructor ===
    super(width, height);

    // === set background ===
    background = _background;

    // === Init all pixels to 0 ===
    int size = width * height;
    for (int i = 0; i < size; i++)
    {
      setf(i, 0.f);
    }
  }

  /**
   * ImageFbt Copy constructor. Create a new object ImageFbt exactly similar to
   * the original imageProcessor.
   *
   * @param orig ImageProcessor to copy.
   */
  public ImageFbt(ImageProcessor orig)
  {
    // === inherited constructor ===
    super(orig.getWidth(), orig.getHeight());

    // === Copy original pixels into new image ===
    int size = orig.getWidth() * orig.getHeight();
    for (int i = 0; i < size; i++)
    {
      setf(i, orig.getf(i));
    }

    // === if original image is an ImageFbt, copy its background ===
    if (orig instanceof ImageFbt)
    {
    	background = ((ImageFbt) orig).background;
    }	
  }
  
  
  
  /**
   * ImageFbt Copy constructor. Create a new object ImageFbt exactly similar to
   * the original imageProcessor.
   * @author xsong
   * @param orig ImageProcessor to copy.
   */
  public ImageFbt(ImageProcessor orig,int margeX, int margeY)
  {
    // === inherited constructor ===
    super(orig.getWidth(), orig.getHeight());

    // === Copy original pixels into new image ===
    int size = orig.getWidth() * orig.getHeight();
    for (int i = 0; i < size; i++)
    {
      setf(i, orig.getf(i));
    }

    // === if original image is an ImageFbt, copy its background ===
    if (orig instanceof ImageFbt)
    {
    	background = ((ImageFbt) orig).background;
    }else {
    	ImagePlus imp = IJ.createImage("Otsu mask background-0", "8-bit white", orig.getWidth(), orig.getHeight(), 1);
    	ImageProcessor ip = imp.getProcessor();
    	int level = Auto_Threshold_v.getThreshold("Otsu", orig);
    	//System.out.println("Background mask threshold level (Otsu) : " + level); 
    	background = new int[size];
    	int width = orig.getWidth();
    	for(int i=0; i<size ; i ++)
    	{
    		if(getf(i) > level*0.99)
    		{
    			ip.set(i, 0);
    			background[i] = BACKGROUND_MASK;
    		}
    		int x = i%width;
    		int y = i/width;
    		if(x<margeX || x>=width-margeX)
    			background[i] = BACKGROUND_MASK;
    		if(y<margeY)
    			background[i] = BACKGROUND_MASK;
    	}
    	//imp.show();
	}
  }
  
  /**
   * ImageFbt Copy constructor. Create a new object ImageFbt similar to
   * the original imageProcessor. using local autothreshold
   * @author xsong
   * @param orig ImageProcessor to copy.
   */
  public ImageFbt(ImageProcessor orig,int radius, int margeX, int margeY, double ratioThreshold)
  {
    // === inherited constructor ===
    super(orig.getWidth(), orig.getHeight());
	ImageProcessor ipNorm = normalisation(orig,ratioThreshold);
	//bp.invert();
    // === Copy original pixels into new image ===
    int size = orig.getWidth() * orig.getHeight();
    for (int i = 0; i < size; i++)
    {
      setf(i, ipNorm.getf(i));
    }

    // === if original image is an ImageFbt, copy its background ===
    if (orig instanceof ImageFbt)
    {
    	background = ((ImageFbt) orig).background;
    }else {
    	ImagePlus imp = IJ.createImage("Otsu mask background-0", "8-bit white", orig.getWidth(), orig.getHeight(), 1);
    	ImageProcessor ip = imp.getProcessor();
    	//imp.show();
    	Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();
    	//ImageProcessor bp = orig.convertToByte(true);    	
    	ImageProcessor bp = ipNorm.convertToByte(true);
    	//new ImagePlus("byte pro", bp).show();
    	//bp.invert();
    	//IJ.log(" local threshold otsu : start");
    	byte[] otsuPixels = local_Threshold.Mean(bp, radius, true);
    	//new ImagePlus("mean local", new ByteProcessor(orig.getWidth(), orig.getHeight(), otsuPixels)).show();
    	//IJ.log(" local threshold otsu : end");
    	//System.out.println("Background mask threshold level (Otsu) : " + level); 
    	background = new int[size];
    	int width = orig.getWidth();
    	for(int i=0; i<size ; i ++)
    	{
    		if(otsuPixels[i] == (byte)0)
    		{
    			ip.set(i, 0);
    			background[i] = BACKGROUND_MASK;
    		}
    		int x = i%width;
    		int y = i/width;
    		if(x<margeX || x>=width-margeX)
    			background[i] = BACKGROUND_MASK;
    		if(y<margeY)
    			background[i] = BACKGROUND_MASK;
    	}
    	
	}
  }
  
  /**
   * ImageFbt Copy constructor. Create a new object ImageFbt similar to
   * the original imageProcessor. using local autothreshold
   * @author xsong
   * @param orig ImageProcessor to copy.
   */
  public ImageFbt(ImageProcessor orig, ImageProcessor ipMASK)
  {
    // === inherited constructor ===
    super(orig.getWidth(), orig.getHeight());
    int size = orig.getWidth() * orig.getHeight();
    for (int i = 0; i < size; i++)
    {
      setf(i, orig.getf(i));
    }
    // === if original image is an ImageFbt, copy its background ===
    if (orig instanceof ImageFbt)
    {
    	background = ((ImageFbt) orig).background;
    }else {
    	background = new int[size];
    	byte[] maskPixels = (byte[])ipMASK.getPixels();
    	for(int i=0; i<size ; i ++)
    	{
    		if(maskPixels[i] == (byte)255)
    		{
    			background[i] = BACKGROUND_MASK;
    		}
    	}
    	
	}
  }
  
  /**
   * ImageFbt Copy constructor. Create a new object ImageFbt similar to
   * the original imageProcessor. using local autothreshold
   * @author xsong
   * @param orig ImageProcessor to copy.
   */
  public ImageFbt(ImageProcessor orig,int radius, int margeX, int margeY, double ratioThreshold,ImageProcessor mask)
  {
    // === inherited constructor ===
    super(orig.getWidth(), orig.getHeight());
	ImageProcessor ipNorm = normalisation(orig,ratioThreshold);
	//bp.invert();
    // === Copy original pixels into new image ===
    int size = orig.getWidth() * orig.getHeight();
    for (int i = 0; i < size; i++)
    {
      setf(i, ipNorm.getf(i));
    }

    // === if original image is an ImageFbt, copy its background ===
    if (orig instanceof ImageFbt)
    {
    	background = ((ImageFbt) orig).background;
    }else {
    	ImagePlus imp = IJ.createImage("Otsu mask background-0", "8-bit white", orig.getWidth(), orig.getHeight(), 1);
    	ImageProcessor ip = imp.getProcessor();
    	//imp.show();
    	Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();
    	//ImageProcessor bp = orig.convertToByte(true);    	
    	ImageProcessor bp = ipNorm.convertToByte(true);
    	//new ImagePlus("byte pro", bp).show();
    	bp.invert();
    	//IJ.log(" local threshold otsu : start");
    	byte[] otsuPixels = local_Threshold.Mean(bp, radius, true);
    	//new ImagePlus("otsu local", new ByteProcessor(orig.getWidth(), orig.getHeight(), otsuPixels)).show();
    	//IJ.log(" local threshold otsu : end");
    	//System.out.println("Background mask threshold level (Otsu) : " + level); 
    	background = new int[size];
    	int width = orig.getWidth();
    	for(int i=0; i<size ; i ++)
    	{
    		if(otsuPixels[i] == (byte)0)
    		{
    			ip.set(i, 0);
    			background[i] = BACKGROUND_MASK;
    		}
    		int x = i%width;
    		int y = i/width;
    		if(x<margeX || x>=width-margeX)
    			background[i] = BACKGROUND_MASK;
    		if(y<margeY)
    			background[i] = BACKGROUND_MASK;
    	}
    	
    	if(mask!=null && mask instanceof ByteProcessor && mask.getWidth()==orig.getWidth() && mask.getHeight()==orig.getHeight())
    	{
    		for(int i=0; i<size; i++)
    		{
    			if(mask.get(i)==0)
    				background[i] = BACKGROUND_MASK;
    		}
    	}
    		
	}
  }
  

  /**
   * ImageFbt Constructor with array of pixels. Background is initialize to 0 on
   * all picture (no background).
   *
   * @param width Width of the image.
   * @param height height of the image.
   * @param pixels array of pixels value.
   */
  public ImageFbt(int width, int height, short[] pixels)
  {
    // === inherited constructor ===
    super(width, height, pixels, null);

    // === Init background to 0 ===
//    initBackground();
  }

  /**
   * ImageFbt Constructor from a path. Background is initialize to 0 on all
   * picture (no background).
   *
   * @param path Path of the image to load.
   */
  public ImageFbt(String path)
  {
    // === Inherited constructor ===
    super((new ImagePlus(path)).getWidth(), (new ImagePlus(path)).
        getHeight());

    // === set pixels values === 
    ImageProcessor imp = new ImagePlus(path).getProcessor().convertToShort(
        true);
    setPixels(imp.getPixels());

    // === init background to 0 ===
//    initBackground();
  }

  public ImageFbt copy()
  {
    ImageFbt res = new ImageFbt(this);
    return res;
  }

  // =========================================================================
  //                             Public functions
  // =========================================================================
  // -------------------------------------------------------------------------
  //                 init background
  // -------------------------------------------------------------------------
  /**
   * Initialize background to 0 on all image. All pixels are in foreground.
   *
   */
  public final void initBackground()
  {
    int size = width * height;
    background = new int[size];
    for (int i = 0; i < size; i++)
    {
      background[i] = 0;
    }
  }

  // -------------------------------------------------------------------------
  //                  Save and load Image Fbt
  // -------------------------------------------------------------------------
  /**
   * Saves ImageFbt as byte[] in 'path'.
   *
   * This uses Files.write method.
   *
   * @param path
   */
  public void save(Path path)
  {
    ImagePlus IP = new ImagePlus("", this);
    FileSaver FS = new FileSaver(IP);

    try
    {
      Files.write(path, FS.serialize());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }

    // ================= Save background in 'savingPath' ==================
    String backfile = path.resolveSibling(
        path.getFileName() + "_" + BACKGROUND_FILENAME).toString();
    Utils.saveObject(background, backfile);

  }

  /**
   * Load ImageFbt from 'path', it should have been saved with ImageFbt.save
   * method.
   *
   * @param path
   * @return
   */
  public static ImageFbt load(Path path)
  {
    ImageFbt result;

    try
    {
      byte[] imgByte = Files.readAllBytes(path);
      ImagePlus IP = new Opener().deserialize(imgByte);
      result = new ImageFbt(IP.getProcessor());
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      return null;
    }

    try
    {
      int[] back = (int[]) Utils.loadObject(
          path.resolveSibling(
              path.getFileName() + "_" + BACKGROUND_FILENAME).toString());

      result.setBackground(back);
    }
    catch (Exception ex)
    {
    }

    return result;

  }

  // -------------------------------------------------------------------------
  //                  Show Image Fbt
  // -------------------------------------------------------------------------
  /**
   * Create an ImagePlus from the ImageFbt and show it.
   *
   * @param title title of the ImagePlus.
   */
  public void show(String title)
  {
    ImagePlus IP = new ImagePlus(title, new ImageFbt(this));
    IP.show();
  }

  // -------------------------------------------------------------------------
  //                  Test over coordinates or index
  // -------------------------------------------------------------------------
  /**
   * Test if a coordinate x is valid ( sup to 0 and inf to width ).
   *
   * @param x coordinate.
   * @return boolean true : x is valid , false : x is out of bounds.
   */
  public boolean isValidX(int x)
  {
    if (x >= 0 && x < width)
    {
      return true;
    }
    return false;
  }

  /**
   * Test if a coordinate y is valid ( sup to 0 and inf to height ).
   *
   * @param y coordinate.
   * @return boolean true : y is valid , false : y is out of bounds.
   */
  public boolean isValidY(int y)
  {
    return y >= 0 && y < height;
  }

  /**
   * Test if an index newInd is valid ( sup= 0 and inf= height*width ).
   *
   *
   * @param i index.
   * @return boolean true : newInd is valid , false : newInd is out of bounds.
   */
  public boolean isValidInd(int i)
  {
    return i >= 0 && i < width * height;

  }

  // -------------------------------------------------------------------------
  //             Conversions between index and coordinates
  // -------------------------------------------------------------------------
  /**
   * Conversion from index to 2D coordinates in an array.
   *
   * @param i index.
   * @return array of 2 int : coordinates.
   */
  public int[] indexToCoord(int i)
  {
    return Utils.indexToCoord(i, width, height);
  }

  /**
   * Conversion from 2D coordinates to index in an array.
   *
   * @param x row.
   * @param y column.
   * @return index.
   */
  public int coordToIndex(int x, int y)
  {
    return Utils.coordToIndex(x, y, width, height);
  }

  /**
   * Conversion from index to 2D coordinates in a Point.
   *
   * @param i index
   * @return Point with coordinates.
   */
  public Point indexToPoint(int i)
  {
    return Utils.indexToPoint(i, width, height);
  }

  /**
   * Conversion from 2D coordinates in a point to index in an array.
   *
   * @param p Point
   * @return index.
   */
  public int pointToIndex(Point p)
  {
    return Utils.pointToIndex(p, width, height);
  }

  // -------------------------------------------------------------------------
  //             Distance between 2 pixels
  // -------------------------------------------------------------------------
  /**
   * Squared distanceIndex between two pixels.
   *
   * @param x1 x of point 1.
   * @param x2 x of point 2.
   * @param y1 y of point 1.
   * @param y2 y of point 2.
   * @return squared distance.
   */
  public double distance(int x1, int x2, int y1, int y2)
  {
    return Utils.distance(x1, x2, y1, y2, width, height);
  }

  /**
   * Squared distanceIndex between two pixels.
   *
   * @param i1 index of point 1.
   * @param i2 index of point 2.
   * @return squared distance.
   */
  public double distance(int i1, int i2)
  {
    return Utils.distanceIndex(i1, i2, width, height);
  }
  
  /**
   * Squared distanceIndex between two pixels.
   *
   * @param i1 index of point 1.
   * @param i2 index of point 2.
   * @return squared distance.
   */
  public double distanceNeighbor(int i1, int direction)
  {
	  if(direction%2 == 0)
		  return 2.0d;
	  else {
		return 1.0d;
	}
  }

  // ---------------------------------------------------------------------------
  //      Conversion from 255 grey level to image min and max_grey values
  // ---------------------------------------------------------------------------
  /**
   * Convert n from [0-255] to [minValue()-max_greyValue()] scale.
   *
   * @param n grey level between 0 and 255.
   * @return corresponding pixel value between minValue and max_greyValue.
   */
  public double grey255ToMax(double n)
  {
    return Utils.grey255ToMax(n, maxValue(), minValue());
  }

  /**
   * Convert n from [minValue()-max_greyValue()] to [0-255] scale
   *
   * @param n pixel value between minValue and max_greyValue.
   * @return corresponding grey level between 0 and 255.
   */
  public double maxTogrey255(double n)
  {	  
    return Utils.maxTogrey255(n, maxValue(), minValue());
    
  }

  // -------------------------------------------------------------------------
  //              Neighborhood functions
  // -------------------------------------------------------------------------
  /**
   * To get all the valid neighbors indexes of this position.
   *
   * @param x x-coordinate of the position.
   * @param y y-coordinate of the position.
   * @param connected8 is there 4 or 8 neighbors.
   * @return shapeList of neighbors indexes.
   */
  public HashSet<Integer> getNeighIndex(int x, int y, boolean connected8)
  {
    return Utils.getNeighIndex(x, y, width, height, connected8);
  }

  /**
   * To get all the valid neighbors indexes of this position.
   *
   * @param i index of the position.
   * @param connected8 is there 4 or 8 neighbors.
   * @return shapeList of neighbors indexes.
   */
  public HashSet<Integer> getNeighIndex(int i, boolean connected8)
  {
    return Utils.getNeighIndex(i, width, height, connected8);
  }

  // -------------------------------------------------------------------------
  //              Erosion , dilatation , opening , closing
  // -------------------------------------------------------------------------
  // ----------------------- Erosion ------------------------------
  /**
   * Changes Image : Each pixel takes the minimum value of its neighborhood,
   * white objects are eroded.
   *
   * @param radius The neighbors are all the points in the circle of this
   * radius.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void erode(double radius) throws InterruptedException
  {
    // ========== Local Variables ==========
    Point[] nei = new Disc(radius).pixels; // Disc of radius r that will define neighborhood
    float[][] res = new float[width][height];

    // ========== Check that radius is at least 1 ==========
    if (radius < 1)
    {
      System.out.println(
          "WARNING in ImageFbt.erodilatation : radius must be at least 1. Nothing done to the image.");
      return;
    }

    // ========== For each pixel ==========
    for (int y = 0; y < height; y++)
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      for (int x = 0; x < width; x++)
      {
        float min_grey = getf(x, y);

        // ========= For each neighbour ==========
        for (Point currPt : nei)
        {
          int xx = x + currPt.x;
          int yy = y + currPt.y;

          // Checking limits
          if (isValidX(xx) && isValidY(yy))
          {
            float currgray = getf(xx, yy);

            // Erosion specific code
            if (currgray < min_grey)
            {
              min_grey = currgray;
            }
          }
        }

        // Set each pixel to limit of its neighborhood 
        res[x][y] = min_grey;
      }
    }
    setFloatArray(res);
  }

  // ----------------------- Dilatation ------------------------------
  /**
   * Changes images : Each pixel takes the max_grey value of its neighborhood,
   * white objects are dilated.
   *
   * @param radius The neighbors are all the points in the circle of this
   * radius.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  protected void dilate(double radius) throws InterruptedException
  {
    // ========== Local Variables ==========
    Point[] nei = new Disc(radius).pixels; // Disc of radius r that will define neighborhood
    float[][] res = new float[width][height];

    // ========== Check that radius is at least 1 ==========
    if (radius < 1)
    {
      System.out.println(
          "WARNING in ImageFbt.erodilatation : radius must be at least 1. Nothing done to the image.");
      return;
    }

    // ========== For each pixel ==========
    for (int y = 0; y < height; y++)
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      for (int x = 0; x < width; x++)
      {
        float max_grey = getf(x, y);

        // ========= For each neighbour ==========
        for (Point currPt : nei)
        {
          int xx = x + currPt.x;
          int yy = y + currPt.y;

          // Checking limits
          if (isValidX(xx) && isValidY(yy))
          {
            float currgray = getf(xx, yy);

            // Dilatation specific code
            if (currgray > max_grey)
            {
              max_grey = currgray;
            }
          }
        }

        // Set each pixel to limit of its neighborhood 
        res[x][y] = max_grey;
      }
    }

    setFloatArray(res);
  }

  // ------------------------------ Opening -----------------------------------
  /**
   * Opening : erosion + dilatation. This operator get rid of little white
   * objects.
   *
   * @param radius the neighbors are all the points in the circle of this
   * radius.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void opening(double radius) throws InterruptedException
  {
    //erosion
    erode(radius);
    //dilatation
    dilate(radius);
  }

  // ------------------------------ Closing -----------------------------------
  /**
   * Closing: dilatation + erosion. This operator get rid of little black
   * objects.
   *
   * @param radius the neighbors are all the points in the circle of this
   * radius.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void closing(double radius) throws InterruptedException
  {
    // dilatation
    dilate(radius);

    // erosion
    erode(radius);
  }

  // ------------ Geodesic dilatation -----------------------------------------
  /**
   * Computes geodesic dilatation of image.
   *
   * @param radius dilatation radius
   * @param boundImg bounding image
   * @throws InterruptedException
   */
  public void geodesicDilate(double radius, ImageFbt boundImg) throws
      InterruptedException
  {
    // =============== tag to know when it is over. ===========================
    boolean change = true;

    // =========== while stability is not reached =============================
    while (change)
    {
      // --------- set change flag to false -----------------------------------
      change = false;

      // --------------- dilate image -----------------------------------------
      dilate(radius);

      // ------------------- set each pixel to the minimum --------------------
      // between bound image and dilated image -
      int size = width * height;
      for (int i = 0; i < size; i++)
      {
        int min = Math.min(get(i), boundImg.get(i));

        if (min != get(i))
        {
          set(i, min);
          change = true;
        }
      }

    }
  }

  // -------------------------------------------------------------------------
  //                        Basic Operations
  // -------------------------------------------------------------------------
  // ---------------------------- invert --------------------------------------
  /**
   * Invert image. Each pixel value p is replace by (max_greyValue - p).
   */
  public void inversion()
  {
    int nbPix = width * height;
    // ========== For loop over all pixels invert gray level
    for (int i = 0; i < nbPix; i++)
    {
      setf(i, (float) (maxValue() - getf(i)));
    }
  }

  // ---------------------------- binarize -----------------------------------
  /**
   * Binarize image. Image will be set in black and white : pixels over a
   * max_greyit will be set to max_greyValue and others to minValue.
   *
   * @param threshold max value of gray value. Darker pixel will be set to black
   * and lighter to white
   */
  public void binarize(int threshold)
  {

    // ========== For loop over all pixels if > threshold 255 else 0
    int nbPix = width * height;
    for (int i = 0; i < nbPix; i++)
    {
      float currPix = getf(i);
      if (currPix > threshold)
      {
        setf(i, (float) maxValue());
      }
      else
      {
        setf(i, (float) minValue());
      }
    }

  }

  // -------------------------------------------------------------------------
  //              Compute background
  // -------------------------------------------------------------------------
  // ------------------------ Rough Background -------------------------------
  /**
   * Compute a rough background given a threshold value. First image is
   * binarized with the threshold. Then it is opened and dilated with the given
   * radius, 5 times. This gives a black and white image with interesting parts
   * in white and background in black. From this image we compute the background
   * mask and return it.
   *
   * @param thres binarization threshold.
   * @param radius radius of the opening and dilation.
   * @return an array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] roughBackgroundMask(int thres, double radius) throws
      InterruptedException
  {
    int[] mask = new int[width * height];

    // ======== Init temporary image by duplicating this =========
    ImageFbt tmp = new ImageFbt(this);

    // ======== Apply a simple smoothing on 3*3 patches
    tmp.smooth();

    // ========== Make it binary with threshold thres ==========
    tmp.binarize(thres);

    // ========== Close and dilate the image  ==========
    for (int i = 0; i < 5; i++)
    {
      tmp.closing(radius);
      tmp.dilate(radius);
    }

    // ========== Compute mask  ==========
    // if temporary image is black i.e. == 0
    // then it is background so mask[newInd] = 1,
    // else mask[newInd] = 0
    int size = width * height;

    for (int i = 0; i < size; i++)
    {
      if (tmp.get(i) == 0)
      {
        mask[i] = 1;
      }
      else
      {
        mask[i] = 0;
      }
    }

    // ========== return mask ==========
    return mask;
  }

  /**
   * Compute a rough background with autoThreshold. The threshold to max_greyit
   * the component is taken from the "Triangle dark" method of imageJ. First
   * image is binarized with the threshold. Then it is opened and dilated with
   * the given radius, 5 times. This gives a black and white image with
   * interesting parts in white and background in black. From this image we
   * compute the background mask and return it.
   *
   * @param radius radius of the opening and dilation.
   * @return an array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] roughBackgroundMaskAutoThresh(double radius) throws
      InterruptedException
  {
    setAutoThreshold("Triangle dark");
    int thres = (int) getMinThreshold();
    return roughBackgroundMask(thres, radius);
  }

  // ------------------------ Final Background -------------------------------
  /**
   * Compute background as the bordering connected component in the image. A
   * black border is added to the image. From the top left corner, we dilate the
   * background over the image. Dilatation is stopped by pixels with value over
   * threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param threshold grey level max_greyit of the background component. (it is
   * the grey level of the lighter area around the cells).
   * @return A array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] backgroundMask(boolean connected8, int threshold) throws
      InterruptedException
  {
    // ========== Init variables ==========
    HashSet<Integer> toTreat = new HashSet<Integer>(); // List of pixels to compute

    // =========== Work on an image with a black line added around it =======
    // this allows it to work even if there is a cell in the top left corner of the image
    ImageFbt im = new ImageFbt(width + 2, height + 2, (int[]) null);
    int size = (width + 2) * (height + 2);

    // background mask to 0 on the border and copy roughBackground elsewhere
    int mask[] = new int[size];

    for (int i = 0; i < size; i++)
    {
      int[] coord = im.indexToCoord(i);
      int x = coord[0];
      int y = coord[1];
      if (x == 0 || y == 0 || x == width + 1 || y == height + 1)
      {
        im.setf(x, y, 0);
      }
      else
      {
        im.setf(x, y, getf(x - 1, y - 1));
      }
      mask[i] = 0;
    }

    // =========== labelList as background the pixel of the top left corner =========== 
    mask[0] = 1;

    // =========== add the point to the shapeList to treat. =========== 
    toTreat.add(0);

    // =========== Untill there is no pixels to treat left, for each pixel in the toTreat shapeList =========== 
    // look at all its neighbors, 
    // add them to the component if they have a grey level not too different from the pixel ( < threshold ) 
    // and in that case add them to the toTreat shapeList.
    while (!toTreat.isEmpty())
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      // ---------- Get labelList of the pixel to treat and remove it from the toTreat shapeList ---------- 
      int currI = toTreat.iterator().next();
      toTreat.remove(currI);

      // ---------- Get its neighbors indexes ---------- 
      HashSet<Integer> neigh = im.getNeighIndex(currI, connected8);

      //  ---------- For each neighbor check its grey level and add it to the component if needed  ---------- 
      Iterator itr = neigh.iterator();
      while (itr.hasNext())
      {
        int neighI = (Integer) itr.next();

        if (mask[neighI] == 0)
        {
          // Compute difference of grey level between pixel and its neighbor 
          if (im.getf(neighI) < threshold)
          {
            //  add neighbor to composant 
            mask[neighI] = 1;
            //  add neighbor to toTreat 
            toTreat.add(neighI);
          }
        }
      }
    }

    // ========== Return mask corresponding to the image (without the black line ==========
    int[] finalMask = new int[height * width];
    for (int i = 0; i < size; i++)
    {

      int[] coord = im.indexToCoord(i);
      int x = coord[0];
      int y = coord[1];
      if (x != 0 && y != 0 && x != width + 1 && y != height + 1)
      {

        int ind = coordToIndex(x - 1, y - 1);
        finalMask[ind] = mask[i];
      }
    }

    return finalMask;
  }

  /**
   * Compute background as the bordering connected component in the image. A
   * black border is added to the image. From the top left corner, we dilate the
   * background over the image. The background is computed by taking in account
   * the given rough background mask : image parts already in background in this
   * rough mask are not computed again. Dilatation is stopped by pixels with
   * value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param threshold grey level max_greyit of the background component. (it is
   * the grey level of the lighter area around the cells).
   * @param roughMask a background mask computed from roughBackgroundmask.
   *
   *
   * progressBar.
   * @return A array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] backgroundMaskWithRoughB(boolean connected8, int threshold,
                                              int[] roughMask) throws
      InterruptedException
  {
    // ========== Init variables ==========
    HashSet<Integer> toTreat = new HashSet<Integer>(); // List of pixels to compute

    // =========== Work on an image with a black line added around it =======
    // this allows it to work even if there is a cell in the top left corner of the image
    ImageFbt im = new ImageFbt(width + 2, height + 2, (int[]) null);
    int size = (width + 2) * (height + 2);

    // background mask to 0 on the border and copy roughBackground elsewhere
    int mask[] = new int[size];

    for (int i = 0; i < size; i++)
    {
      int[] coord = im.indexToCoord(i);
      int x = coord[0];
      int y = coord[1];
      if (x == 0 || y == 0 || x == width + 1 || y == height + 1)
      {
        im.setf(x, y, 0);
        mask[i] = 0;
      }
      else
      {
        im.setf(x, y, getf(x - 1, y - 1));
        mask[i] = roughMask[coordToIndex(x - 1, y - 1)];
      }

    }

    // =========== labelList as background the pixel of the top left corner =========== 
    mask[0] = 1;

    // =========== add to the to treatlist all point in rough background with neighbor not in rough background. =========== 
    for (int i = 0; i < size; i++)
    {
      if (mask[i] == 1)
      {
        HashSet<Integer> neigh = im.getNeighIndex(i, connected8);
        Iterator itr = neigh.iterator();

        boolean added = false;
        while (itr.hasNext() && !added)
        {
          int ineigh = (Integer) itr.next();
          if (mask[ ineigh] == 0)
          {
            toTreat.add(i);
            added = true;
          }
        }
      }
    }

    // =========== Untill there is no pixels to treat left, for each pixel in the toTreat shapeList =========== 
    // look at all its neighbors, 
    // add them to the component if they have a grey level not too different from the pixel ( < threshold ) 
    // and in that case add them to the toTreat shapeList.
    while (!toTreat.isEmpty())
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      // ---------- Get labelList of the pixel to treat and remove it from the toTreat shapeList ---------- 
      int currI = toTreat.iterator().next();
      toTreat.remove(currI);

      // ---------- Get its neighbors indexes ---------- 
      HashSet<Integer> neigh = im.getNeighIndex(currI, connected8);

      //  ---------- For each neighbor check its grey level and add it to the component if needed  ---------- 
      Iterator itr = neigh.iterator();
      while (itr.hasNext())
      {
        int neighI = (Integer) itr.next();

        if (mask[neighI] == 0)
        {
          // Compute difference of grey level between pixel and its neighbor 
          if (im.getf(neighI) < threshold)
          {
            //  add neighbor to composant 
            mask[neighI] = 1;
            //  add neighbor to toTreat 
            toTreat.add(neighI);
          }
        }
      }

    }

    // ========== Return mask corresponding to the image (without the black line ==========
    int[] finalMask = new int[height * width];
    for (int i = 0; i < size; i++)
    {
      int[] coord = im.indexToCoord(i);
      int x = coord[0];
      int y = coord[1];
      if (x != 0 && y != 0 && x != width + 1 && y != height + 1)
      {
        int ind = coordToIndex(x - 1, y - 1);
        finalMask[ind] = mask[i];
      }
    }

    return finalMask;
  }

  /**
   * Compute background as the bordering connected component in the image. The
   * threshold to max_greyit the component is taken from the "Minimum dark"
   * method of imageJ. A black border is added to the image. From the top left
   * corner, we dilate the background over the image. The background is computed
   * by taking in account the given rough background mask : image parts already
   * in background in this rough mask are not computed again. Dilatation is
   * stopped by pixels with value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param roughMask a background mask computed from roughBackgroundmask.
   *
   *
   * progressBar.
   * @return A array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] backgroundMaskWithRoughB(boolean connected8,
                                              int[] roughMask) throws
      InterruptedException
  {
    setAutoThreshold("Triangle dark");
    int thres = (int) getMinThreshold();
    return backgroundMaskWithRoughB(connected8, thres, roughMask);
  }

  public static ImagePlus removeBackgroundMaskOnLM(ImageProcessor ipMASK, ImageProcessor ipLM)
  {
	  ByteProcessor bip = (ByteProcessor)ipMASK.convertToByte(false);
	  bip.invertLut();
	  Frame frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null)						
		  IJ.run("ROI Manager...");
	  frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null || !(frame instanceof RoiManager))
	  {return null;}
	  RoiManager roiManager = (RoiManager)frame;
	  ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER|ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, null, 100,  Double.POSITIVE_INFINITY);
	  particleAnalyzer.analyze(new ImagePlus("", bip));
	  Roi[] rois = roiManager.getRoisAsArray();
	  if(rois==null || rois.length<=0)
		  return null;

	  AutoThresholder aThresholder = new AutoThresholder();
	  int thres =(int)(aThresholder.getThreshold(AutoThresholder.Method.Default, ipLM.getHistogram()));
	  ipLM.threshold(thres);
	  int maxPoints = 50;
	  int[][] sp = new int[maxPoints][2];
	  int npoints = 0;
	  for(int x=0;x<ipLM.getWidth();x++)
		  for(int y=0;y<ipLM.getHeight();y++)
		  {
			  if(ipLM.get(x, y) > 0)
			  {
				  if (npoints==maxPoints) {
					  int[][] toTreatIndtemp = new int[maxPoints*2][2];
					  System.arraycopy(sp, 0, toTreatIndtemp, 0, maxPoints);
					  sp = toTreatIndtemp;
					  maxPoints *= 2;
				  }
				  sp[npoints][0] = x;
				  sp[npoints][1] = y;
				  npoints++;
			  }
		  }

	  ImagePlus impMASKLM = IJ.createImage("impMaskLM", "8-bit white", bip.getWidth(), bip.getHeight(),1);
	  ImageProcessor ipMASKLM = impMASKLM.getProcessor();
	  for(int i=0; i<rois.length;i++)
	  {
		  Roi r = rois[i];
		  for(int n=0;n<npoints;n++)
		  {
			  if(r.contains(sp[n][0], sp[n][1]))
			  {
				  ImageProcessor bipDup = bip.duplicate();
				  bipDup.setValue(255);
				  bipDup.setRoi(r);
				  ipMASKLM.setRoi(r);
				  ImageProcessor localOrig = ipMASKLM.crop();
				  bipDup.fillOutside(r);
				  ImageProcessor local = bipDup.crop();
				  for(int x=0;x<local.getWidth();x++)
					  for(int y=0;y<local.getHeight();y++)
					  {
						  if(local.get(x, y)<localOrig.get(x, y))
							  ipMASKLM.set(x+r.getBounds().x, y+r.getBounds().y, local.get(x, y));
					  }
			  }
		  }
	  }
	  //impMASKLM.getProcessor().autoThreshold();
	  return impMASKLM;
  }
  
  public void removeBackground(ImageProcessor impTrans, ImageProcessor ipFluo, int numDilation,boolean maxEntropy)
  {
	  //impTrans.invert(); // get bright object with dark edge
	  impTrans = impTrans.convertToByte(true);
	  AutoThresholder aThresholder = new AutoThresholder();
	  int thres = 0;
	  if(maxEntropy)
		  thres =(int)(aThresholder.getThreshold(AutoThresholder.Method.MaxEntropy, impTrans.getHistogram()));//int thres =(int)(aThresholder.getThreshold(AutoThresholder.Method.Default, impTrans.getHistogram())*0.9);
	  else {
		  thres =(int)(aThresholder.getThreshold(AutoThresholder.Method.Default, impTrans.getHistogram())*0.9);
	  }
	  System.out.println("MaxEntropy threshold : " + thres);
	  impTrans.threshold(thres);
	  //new ImagePlus("maxEntropy", impTrans).show();
	  	  
	  ByteProcessor bip = (ByteProcessor)impTrans.convertToByte(false);
	  bip.invert();
	  for(int i=0; i<numDilation;i++)
	  {
		  bip.dilate();
		  bip.erode();
	  }
	  //new ImagePlus("bip ", bip.duplicate()).show();
	  //impTrans.show();
	  Frame frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null)						
		  IJ.run("ROI Manager...");
	  frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null || !(frame instanceof RoiManager))
	  {return;}
	  RoiManager roiManager = (RoiManager)frame;
	  
	  
	  ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER|ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, null, 50,  Double.POSITIVE_INFINITY);
	  particleAnalyzer.analyze(new ImagePlus("", bip));
	  Roi[] rois = roiManager.getRoisAsArray();
	  if(rois==null || rois.length<=0)
		  return;

	  int maxArea = Integer.MIN_VALUE;
	  int maxIndex = 0;
	  for(int i=0; i<rois.length;i++)
	  {
		  impTrans.setRoi(rois[i]);
		  ImageStatistics iStatistics = ImageStatistics.getStatistics(impTrans, ImageStatistics.AREA, null);
		  if(iStatistics.area>maxArea)
		  {
			  maxArea = (int)iStatistics.area;
			  maxIndex = i;
		  }
	  }
	  
	  Roi roiMaxArea = rois[maxIndex];
	  impTrans.resetRoi();
	  impTrans.set(255);
	  impTrans.setRoi(roiMaxArea);
	  impTrans.setValue(0);
	  impTrans.fillOutside(roiMaxArea);

	  for(int x=0; x<ipFluo.getWidth();x++)
		  for(int y =0; y<ipFluo.getHeight(); y++)
		  {
			  if(impTrans.get(x, y) == 0)
			  {
				  ipFluo.set(x, y, 0);
				  background[x+y*ipFluo.getWidth()] = BACKGROUND_MASK;
			  }
		  }
	  deleteRoiInManager();	  
  }
  
  
  public static Roi[] removeBackground(ImageProcessor impTrans, int numDilation)
  {	  	 
	  int triAngleThres = Auto_Threshold_v.getThreshold("Triangle", impTrans);	  
	  impTrans.threshold((int)(triAngleThres*0.75));
	  //impTrans.threshold(2000);
	  ByteProcessor bip = (ByteProcessor)impTrans.convertToByte(false);
	  //bip.invert();
	  new ImagePlus("triangle threshold", bip).duplicate().show();
	  ImageProcessor ipBg = bip;
	  //ipBg.erode();
	  //ipBg.dilate();	  
	  ipBg.invert();
	  for(int i=1; i<=numDilation; i++)
		  ipBg.dilate();
	  
	  for(int i=1; i<=numDilation/2; i++)
		  ipBg.erode();

	  ImagePlus impBg = new ImagePlus("background estimate", ipBg);
	  impBg.show();
	  int options = Ins_ParticleAnalyzer.ADD_TO_MANAGER;
	  int measurements = Measurements.AREA|Measurements.RECT;
	  ResultsTable rt = new ResultsTable();
	  Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(options, measurements, rt, 1000, 1000000000);
	  Frame frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null)						
		  IJ.run("ROI Manager...");
	  frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null || !(frame instanceof RoiManager))
	  {return null;}
	  RoiManager roiManager = (RoiManager)frame;
	  roiManager.setVisible(true);		
	  Ins_ParticleAnalyzer.setRoiManager(roiManager);	  
	  pAnalyzer.analyze(impBg, ipBg);	  
	  return roiManager.getRoisAsArray();
  }
  
  /**
   * Compute background as the bordering connected component in the image. The
   * threshold to max_greyit the component is taken from the "Minimum dark"
   * method of imageJ. A black border is added to the image. From the top left
   * corner, we dilate the background over the image. Dilatation is stopped by
   * pixels with value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   *
   *
   * progressBar.
   * @return A array of int 1 if in background, 0 elsewhere.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public final int[] backgroundMask(boolean connected8)
      throws InterruptedException
  {
    setAutoThreshold("Triangle dark");
    int thres = (int) getMinThreshold();

    return backgroundMask(connected8, thres);
  }

  // -------------------------------------------------------------------------
  //                       Recenter Image
  // -------------------------------------------------------------------------
  /**
   * Return colony center as center point of all points not in background.
   *
   * @return
   */
  public Point colonyCenter()
  {
    if (background == null)
    {
      System.out.println(
          "Error in ImageFbt.colonyCenter : background not computed");
    }

    int sum = 0;
    Point res = new Point(0, 0);

    for (int i = 0; i < background.length; i++)
    {
      if (background(i) != 1)
      {
        sum++;
        int[] coord = indexToCoord(i);
        res.x += coord[0];
        res.y += coord[1];
      }
    }

    if (sum == 0)
    {
      return null;
    }

    res.x /= sum;
    res.y /= sum;

    return res;
  }

  /**
   * Recenter image around colony center. Compute colony center ( as center of
   * non-background pixels) and changes image so that this point is the center
   * of the image.
   *
   */
  public void recenter()
  {
    // ============= Compute colony center ====================================
    Point center = colonyCenter();

    // ============ compute difference from colony center to image center =====
    int dx = (int) (center.x - (double) width / 2.0);
    int dy = (int) (center.y - (double) height / 2.0);

    // ========== other variables =============================================
    int size = width * height;
    float[][] newPix = new float[width][height];
    int[] newBack = new int[size];

    // ============ For each pixel, move it to new position in newPix list ====
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        int newInd = coordToIndex(x, y);

        if (!isValidX(x + dx) || !isValidY(y + dy))
        {
          newPix[x][y] = 0;
          newBack[newInd] = 1;
        }
        else
        {
          int oldInd = coordToIndex(x + dx, y + dy);
          newPix[x][y] = getf(x + dx, y + dy);
          newBack[newInd] = background(oldInd);
        }
      }
    }

    setBackground(newBack);
    setFloatArray(newPix);
  }

  // -------------------------------------------------------------------------
  //                        Seeds computation
  // -------------------------------------------------------------------------
  /**
   * Compute seeds as all pixels smaller than the threshold value. Adjacent
   * pixels with smaller value than threshold are added in the same seed. Show
   * computation progress in a progressBarFbt.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param connected8 Are the seeds 4 or 8 connected.
   * @return mask to tell which pixel belongs to which seed.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public int[] seedsFromBinS(int threshold, boolean connected8) throws
      InterruptedException
  {

    int size = width * height;

    // init array of mask
    int[] label = new int[width * height];
    for (int i = 0; i < width * height; i++)
    {
      label[i] = BACKGROUND_LABEL;
    }

    // l labelList of next seed
    int l = BACKGROUND_LABEL + 1;

    // List of seeds to remove if they are not big enough ( area < 4 pixels)
    ArrayList<Integer> toRem = new ArrayList<Integer>();

    //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {

        // Check if thread is interrupted and raise exception if necessary
        Utils.checkThreadInterruption();

        int i = coordToIndex(x, y);
        float gray = this.getf(i);

        // if pixel is black and not labeled and not in the background
        if (gray < threshold && label[i] == BACKGROUND_LABEL && background(i)
                                                                == 0)
        {
          // Label the pixel
          label[i] = l;

          // count seed area.
          int area = 1;

          // Get its neighbors indexes
          HashSet<Integer> neigh = getNeighIndex(i, connected8);

          // --- Extends the seed to all neighboring points < threshold ---
          while (!neigh.isEmpty())
          {
            Integer ineigh = (Integer) neigh.iterator().next();

            // remove it from the neighbors shapeList
            neigh.remove(ineigh);

            // if neighbour is < to threshold and not labeled and not in background : 
            // it is add to the seeds and its neighbors add to the neighbors shapeList
            if (getf(ineigh) < threshold && label[ineigh] == BACKGROUND_LABEL
                && background(ineigh) == 0)
            {
              // add neighbor to the seed
              label[ineigh] = l;
              area++;

              // add its neighbors o the shapeList
              HashSet<Integer> neigh2 = getNeighIndex(ineigh, connected8);

              for (Integer ineigh2 : neigh2)
              {
                if (!neigh.contains(ineigh2))
                {
                  neigh.add(ineigh2);
                }
              }

            }
          }

          // if area is inferior to 4 pixels, set labelList as to remove
          if (area < 4) //changed by xsong, original one is 4
          {
            toRem.add(l);
          }

          //increment labelList to get to another seed
          l++;
        }
      }
    }

    // remove too small seeds.
    for (int i = 0; i < size; i++)
    {
      if (toRem.contains(label[i]))
      {
        label[i] = BACKGROUND_LABEL;
      }
    }

    return label;
  }
  
  // xsong
  // check pixel at (x,y), whether it is inside traced area
  private boolean inside(int x, int y,int threshold) {
	  if (x<0 || x>=width || y<0 || y>=height)
		  return false;
	  float value = getPixel(x, y);
	  return value < threshold;
  }
  
  // xsong
  // check pixel at (x,y), whether it is inside traced area
  private boolean inside(int x, int y) {
	  if (x<0 || x>=width || y<0 || y>=height)
		  return false;
	  return true;
  }

  // -------------------------------------------------------------------------
  //                        Seeds computation
  // -------------------------------------------------------------------------
  /**
   * Compute seeds as all pixels smaller than the threshold value. Adjacent
   * pixels with smaller value than threshold are added in the same seed. Show
   * computation progress in a progressBarFbt.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param connected8 Are the seeds 4 or 8 connected.
   * @return mask to tell which pixel belongs to which seed.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public int[] seedsFromBinF(int threshold, boolean connected8) throws
  InterruptedException
  {

	  int size = width * height;
	  
	  // init array of mask
	  int[] label = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  if(background[i] == BACKGROUND_MASK)
		  {
			  label[i] = BACKGROUND_MASK;
			  continue;
		  }
		  
		  label[i] = BACKGROUND_LABEL;
	  }

	  // l labelList of next seed
	  int l = 50;
	  int seedNumber = 0;

	  //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
	  for (int index = size-1; index >= 0; index--)
	  {
		  // Check if thread is interrupted and raise exception if necessary
		  //Utils.checkThreadInterruption();
		  if(label[index] != BACKGROUND_LABEL)
			  continue;

		  float gray = this.getf(index);
		  // if pixel is black and not labeled and not in the background
		  if (gray < threshold)
		  {
			  int x = index % width;
			  int y = index / width;
			  // Label the pixel
			  label[index] = l;
			  // count seed area.          
			  int npoints = 0;
			  int maxPoints = 50; // will be increased if necessary
			  /** The x-coordinates of the points in the neighbor area.
              A vertical boundary at x separates the pixels at x-1 and x. */
			  int[] xpoints = new int[maxPoints];
			  /** The y-coordinates of the points in the outline.
              A horizontal boundary at y separates the pixels at y-1 and y. */
			  int[] ypoints = new int[maxPoints];

			  xpoints[0] = x;
			  ypoints[0] = y;
			  npoints = 1;
			  int nTotreat = 1;
			  while(nTotreat > 0)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  for (int d=0; d<8; d++) 
					  {  
						  int x2 = xpoints[p]+DIR_X_OFFSET[d];
						  int y2 = ypoints[p]+DIR_Y_OFFSET[d];

						  if(!inside(x2, y2, threshold))
							  continue;

						  if(label[x2+y2*width] != BACKGROUND_LABEL)
							  continue;

						  if (npoints==maxPoints) {
							  int[] xtemp = new int[maxPoints*2];
							  int[] ytemp = new int[maxPoints*2];
							  System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
							  System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
							  xpoints = xtemp;
							  ypoints = ytemp;
							  maxPoints *= 2;
						  }
						  xpoints[npoints] = x2;
						  ypoints[npoints] = y2;
						  npoints++;
						  nTotreat++; // add one toTreat point
						  label[x2+y2*width] = l;
					  }
					  nTotreat = nTotreat - 1;//delete the number of toTreat point by one
				  }
			  }

			  if(npoints<=4)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  label[xpoints[p]+ypoints[p]*width] = BACKGROUND_LABEL;
				  }
			  }else {
				  l = l + 50;
				  seedNumber++;
			  }
		  }
	  }	  
	  //System.out.println("		Seeds (Cells) number : " + seedNumber);
	  //converteLabelToImageL(label, "seeds label imp");
	  return label;
  }
  
  
//-------------------------------------------------------------------------
 //                        Seeds computation
 // -------------------------------------------------------------------------
 /**
  * Compute seeds as all pixels smaller than the threshold value. Adjacent
  * pixels with smaller value than threshold are added in the same seed. Show
  * computation progress in a progressBarFbt.
  *
  * @param threshold Value determining seeds max_greyits.
  * @param connected8 Are the seeds 4 or 8 connected.
  * @return mask to tell which pixel belongs to which seed.
  * @throws InterruptedException thrown if current thread is stopped.
  */
 public int[] seedsFromMaximuFinderF_no_foci(double toleRatio,double tolerenceMax, double otsuRatio, ImageProcessor sp) throws
 InterruptedException
 {
	  int size = width * height;
	  // init array of mask
	  int[] label = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  if(background[i] == BACKGROUND_MASK)
		  {
			  label[i] = BACKGROUND_MASK;
			  continue;
		  }
		  label[i] = BACKGROUND_LABEL;
	  }
	  
	  MaximumFinder mFinder = new MaximumFinder();
	  sp = normalisation(sp, otsuRatio);
	  ImagePlus immp = new ImagePlus("normalised seeds ", sp);
	  IJ.save(immp, "d:/ns.tif");
	  //ShortProcessor sp = new ShortProcessor(getWidth(), getHeight(), (short[])getPixels(), getColorModel());
	  sp.invert();
	  int threshold = Auto_Threshold_v.getThreshold("Otsu", sp);
	  
	  if(toleRatio != 0.0d)
		  tolerenceMax = threshold*toleRatio;
	  System.out.println("		Maximum finder tolerence " + tolerenceMax);
	  ByteProcessor bip = mFinder.findMaxima(sp,tolerenceMax, ImageProcessor.NO_THRESHOLD, MaximumFinder.IN_TOLERANCE, true, false);
	  sp.invert();
	  bip.erode();

	  new ImagePlus("seeds ", bip).show();
	  // l labelList of next seed
	  int l = startLabel;
	  int seedNumber = 0;

	  byte[] grays = (byte[])bip.getPixels();
	  //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
	  for (int index = size-1; index >= 0; index--)
	  {
		  if(label[index] != BACKGROUND_LABEL)
			  continue;
		  
		  int gray = grays[index]&0xff;
		  // if pixel is black and not labeled and not in the background
		  if (gray == 255)
		  {
			  int x = index % width;
			  int y = index / width;
			  // Label the pixel
			  label[index] = l;
			  // count seed area.          
			  int npoints = 0;
			  int maxPoints = 50; // will be increased if necessary
			  /** The x-coordinates of the points in the neighbor area.
             A vertical boundary at x separates the pixels at x-1 and x. */
			  int[] xpoints = new int[maxPoints];
			  /** The y-coordinates of the points in the outline.
             A horizontal boundary at y separates the pixels at y-1 and y. */
			  int[] ypoints = new int[maxPoints];

			  xpoints[0] = x;
			  ypoints[0] = y;
			  npoints = 1;
			  int nTotreat = 1;
			  while(nTotreat > 0)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  for (int d=0; d<8; d++) 
					  {  
						  int x2 = xpoints[p]+DIR_X_OFFSET[d];
						  int y2 = ypoints[p]+DIR_Y_OFFSET[d];
						  
						  if(!inside(x2, y2))
							  continue;

						  if(label[x2+y2*width] != BACKGROUND_LABEL)
							  continue;
						  
						  if(grays[x2+y2*width]!=(byte)255)
							  continue;
						  
						  if (npoints==maxPoints) {
							  int[] xtemp = new int[maxPoints*2];
							  int[] ytemp = new int[maxPoints*2];
							  System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
							  System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
							  xpoints = xtemp;
							  ypoints = ytemp;
							  maxPoints *= 2;
						  }
						  xpoints[npoints] = x2;
						  ypoints[npoints] = y2;
						  npoints++;
						  nTotreat++; // add one toTreat point
						  label[x2+y2*width] = l;
					  }
					  nTotreat = nTotreat - 1;//delete the number of toTreat point by one
				  }
			  }

			  if(npoints<=10)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  label[xpoints[p]+ypoints[p]*width] = BACKGROUND_LABEL;
				  }
			  }else {
				  l = l + startLabel;
				  seedNumber++;
			  }
		  }
	  }
	  
	  //if(seedNumber > 10)
		  //return null;
	  System.out.println("		Seeds (Cells) number : " + seedNumber);
	  //converteLabelToImageL(label, "seeds label imp").show();
	  return label;
 }
 
 
//-------------------------------------------------------------------------
//                        Seeds computation
// -------------------------------------------------------------------------
/**
 * Compute seeds as all pixels smaller than the threshold value. Adjacent
 * pixels with smaller value than threshold are added in the same seed. Show
 * computation progress in a progressBarFbt.
 *
 * @param threshold Value determining seeds max_greyits.
 * @param connected8 Are the seeds 4 or 8 connected.
 * @return mask to tell which pixel belongs to which seed.
 * @throws InterruptedException thrown if current thread is stopped.
 */
public int[] seedsFromErodeStep(int erodeNumber, ImageProcessor sp, double raitoNorm) throws
InterruptedException
{
	  int size = width * height;
	  // init array of mask
	  int[] label = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  if(background[i] == BACKGROUND_MASK)
		  {
			  label[i] = BACKGROUND_MASK;
			  continue;
		  }
		  label[i] = BACKGROUND_LABEL;
	  }
	  
	  //MaximumFinder mFinder = new MaximumFinder();
	  sp = normalisation(sp, raitoNorm);
	  ImagePlus immp = new ImagePlus("normalised seeds ", sp);
	  IJ.save(immp, "d:/ns.tif");
	  //ShortProcessor sp = new ShortProcessor(getWidth(), getHeight(), (short[])getPixels(), getColorModel());
	  
	  sp.invert();
	  Auto_Local_Threshold auto_Local_Threshold = new Auto_Local_Threshold();
	  byte[] otsuPixels =auto_Local_Threshold.Otsu(sp.convertToByte(true), 15, true);
	  ByteProcessor bip = new ByteProcessor(width, height, otsuPixels);
	  bip.invert();
	  
	  for(int i=1; i<=erodeNumber;i++)
		  bip.erode();
	  
	  bip.invert();
	  //ImagePlus impSeeds =  new ImagePlus("seeds ", bip);
	  //IJ.save(impSeeds, "d:/impSeeds.tif");
	  // l labelList of next seed
	  int l = startLabel;
	  int seedNumber = 0;

	  byte[] grays = (byte[])bip.getPixels();
	  //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
	  for (int index = 0; index < size; index++)
	  {
		  if(label[index] != BACKGROUND_LABEL)
			  continue;
		  
		  int gray = grays[index]&0xff;
		  // if pixel is black and not labeled and not in the background
		  if (gray == 255)
		  {
			  int x = index % width;
			  int y = index / width;
			  // Label the pixel
			  label[index] = l;
			  // count seed area.          
			  int npoints = 0;
			  int maxPoints = 50; // will be increased if necessary
			  /** The x-coordinates of the points in the neighbor area.
            A vertical boundary at x separates the pixels at x-1 and x. */
			  int[] xpoints = new int[maxPoints];
			  /** The y-coordinates of the points in the outline.
            A horizontal boundary at y separates the pixels at y-1 and y. */
			  int[] ypoints = new int[maxPoints];

			  xpoints[0] = x;
			  ypoints[0] = y;
			  npoints = 1;
			  int nTotreat = 1;
			  while(nTotreat > 0)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  for (int d=0; d<8; d++) 
					  {  
						  int x2 = xpoints[p]+DIR_X_OFFSET[d];
						  int y2 = ypoints[p]+DIR_Y_OFFSET[d];
						  
						  if(!inside(x2, y2))
							  continue;

						  if(label[x2+y2*width] != BACKGROUND_LABEL)
							  continue;
						  
						  if(grays[x2+y2*width]!=(byte)255)
							  continue;
						  
						  if (npoints==maxPoints) {
							  int[] xtemp = new int[maxPoints*2];
							  int[] ytemp = new int[maxPoints*2];
							  System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
							  System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
							  xpoints = xtemp;
							  ypoints = ytemp;
							  maxPoints *= 2;
						  }
						  xpoints[npoints] = x2;
						  ypoints[npoints] = y2;
						  npoints++;
						  nTotreat++; // add one toTreat point
						  label[x2+y2*width] = l;
					  }
					  nTotreat = nTotreat - 1;//delete the number of toTreat point by one
				  }
			  }

			  if(npoints<=10)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  label[xpoints[p]+ypoints[p]*width] = BACKGROUND_LABEL;
				  }				  
			  }else {
				  l = l + startLabel;
				  seedNumber++;
			  }
		  }
	  }
	  
	  //if(seedNumber > 20)
		  //return null;
	  System.out.println("		Seeds (Cells) number : " + seedNumber);
	  converteLabelToImageL(label, "seeds label imp").show();;
	  //IJ.save(seedsLabel, "d:/seedslabel.tif");
	  
	  return label;
}

public int[] seedsFromErodeStepWithoutBinary(int erodeNumber, ByteProcessor bip) throws
InterruptedException
{
	  int size = width * height;
	  // init array of mask
	  int[] label = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  if(background[i] == BACKGROUND_MASK)
		  {
			  label[i] = BACKGROUND_MASK;
			  continue;
		  }
		  label[i] = BACKGROUND_LABEL;
	  }

	  bip.invert();
	  
	  for(int i=1; i<=erodeNumber;i++)
		  bip.erode();

	  bip.invert();
	  ImagePlus impSeeds =  new ImagePlus("seeds ", bip);
	  //impSeeds.show();
	  //IJ.save(impSeeds, "d:/impSeeds.tif");
	  // l labelList of next seed
	  int l = startLabel;
	  int seedNumber = 0;

	  byte[] grays = (byte[])bip.getPixels();
	  //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
	  for (int index = 0; index < size; index++)
	  {
		  if(label[index] != BACKGROUND_LABEL)
			  continue;
		  
		  int gray = grays[index]&0xff;
		  // if pixel is black and not labeled and not in the background
		  if (gray == 255)
		  {
			  int x = index % width;
			  int y = index / width;
			  // Label the pixel
			  label[index] = l;
			  // count seed area.          
			  int npoints = 0;
			  int maxPoints = 50; // will be increased if necessary
			  /** The x-coordinates of the points in the neighbor area.
            A vertical boundary at x separates the pixels at x-1 and x. */
			  int[] xpoints = new int[maxPoints];
			  /** The y-coordinates of the points in the outline.
            A horizontal boundary at y separates the pixels at y-1 and y. */
			  int[] ypoints = new int[maxPoints];

			  xpoints[0] = x;
			  ypoints[0] = y;
			  npoints = 1;
			  int nTotreat = 1;
			  while(nTotreat > 0)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  for (int d=0; d<8; d++) 
					  {  
						  int x2 = xpoints[p]+DIR_X_OFFSET[d];
						  int y2 = ypoints[p]+DIR_Y_OFFSET[d];
						  
						  if(!inside(x2, y2))
							  continue;

						  if(label[x2+y2*width] != BACKGROUND_LABEL)
							  continue;
						  
						  if(grays[x2+y2*width]!=(byte)255)
							  continue;
						  
						  if (npoints==maxPoints) {
							  int[] xtemp = new int[maxPoints*2];
							  int[] ytemp = new int[maxPoints*2];
							  System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
							  System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
							  xpoints = xtemp;
							  ypoints = ytemp;
							  maxPoints *= 2;
						  }
						  xpoints[npoints] = x2;
						  ypoints[npoints] = y2;
						  npoints++;
						  nTotreat++; // add one toTreat point
						  label[x2+y2*width] = l;
					  }
					  nTotreat = nTotreat - 1;//delete the number of toTreat point by one
				  }
			  }

			  if(npoints<=10)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  label[xpoints[p]+ypoints[p]*width] = BACKGROUND_LABEL;
				  }				  
			  }else {
				  l = l + startLabel;
				  seedNumber++;
			  }
		  }
	  }
	  
	  System.out.println("		Seeds (Cells) number : " + seedNumber);
	  if(seedNumber > 1000)
	  {
		  System.out.println("		Seeds (Cells) number : " + seedNumber + " more than 500, so too much noised");
		  return null;
	  }
		  
	  
	  //ImagePlus impSe = converteLabelToImageL(label, "seeds label imp");
	  
	  
	  return label;
}

public int[] seedsFromErodeStep(int erodeNumber, ByteProcessor bip, int minimumPixelNumberbySeed) throws
InterruptedException
{
	  int size = width * height;
	  // init array of mask
	  int[] label = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  label[i] = BACKGROUND_LABEL;
	  }

//	  bip.invert();
	  
	  for(int i=1; i<=erodeNumber;i++)
		  bip.erode();

//	  bip.invert();
//	  ImagePlus impSeeds =  new ImagePlus("seeds ", bip);
//	  IJ.save(impSeeds, "d:/impSeeds.tif");
	  // l labelList of next seed
	  int l = startLabel;
	  int seedNumber = 0;

	  byte[] grays = (byte[])bip.getPixels();
	  //------------------- For each pixel not labeled and dark enough : create another seed ---------------------------------
	  for (int index = 0; index < size; index++)
	  {
		  if(label[index] != BACKGROUND_LABEL)
			  continue;
		  
		  int gray = grays[index]&0xff;
		  // if pixel is black and not labeled and not in the background
		  if (gray == 0)
		  {
			  int x = index % width;
			  int y = index / width;
			  // Label the pixel
			  label[index] = l;
			  // count seed area.          
			  int npoints = 0;
			  int maxPoints = 50; // will be increased if necessary
			  /** The x-coordinates of the points in the neighbor area.
            A vertical boundary at x separates the pixels at x-1 and x. */
			  int[] xpoints = new int[maxPoints];
			  /** The y-coordinates of the points in the outline.
            A horizontal boundary at y separates the pixels at y-1 and y. */
			  int[] ypoints = new int[maxPoints];

			  xpoints[0] = x;
			  ypoints[0] = y;
			  npoints = 1;
			  int nTotreat = 1;
			  while(nTotreat > 0)
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  for (int d=0; d<8; d++) 
					  {  
						  int x2 = xpoints[p]+DIR_X_OFFSET[d];
						  int y2 = ypoints[p]+DIR_Y_OFFSET[d];
						  
						  if(!inside(x2, y2))
							  continue;

						  if(label[x2+y2*width] != BACKGROUND_LABEL)
							  continue;
						  
						  if(grays[x2+y2*width]!=(byte)0)
							  continue;
						  
						  if (npoints==maxPoints) {
							  int[] xtemp = new int[maxPoints*2];
							  int[] ytemp = new int[maxPoints*2];
							  System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
							  System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
							  xpoints = xtemp;
							  ypoints = ytemp;
							  maxPoints *= 2;
						  }
						  xpoints[npoints] = x2;
						  ypoints[npoints] = y2;
						  npoints++;
						  nTotreat++; // add one toTreat point
						  label[x2+y2*width] = l;
					  }
					  nTotreat = nTotreat - 1;//delete the number of toTreat point by one
				  }
			  }

			  if(npoints<=minimumPixelNumberbySeed)//seeds pixels number too small,
			  {
				  for(int p=0;p<npoints;p++)
				  {
					  label[xpoints[p]+ypoints[p]*width] = BACKGROUND_LABEL;
				  }				  
			  }else {
				  l = l + startLabel;
				  seedNumber++;
			  }
		  }
	  }
	  
	  // init array of mask
	  int[] label2 = new int[size];
	  for (int i = 0; i < size; i++)
	  {
		  if(background[i] == BACKGROUND_MASK)
		  {
			  label2[i] = BACKGROUND_MASK;
			  continue;
		  }
		  if(label[i] > 0) 
		  {
			  label2[i] = label[i];
			  continue;
		  }		  
		  label2[i] = BACKGROUND_LABEL;
		  background[i] = BACKGROUND_LABEL;
	  }
	  
	  System.out.println("		Seeds number : " + seedNumber);
	  if(seedNumber > 1500 || seedNumber == 0)
	  {
		  System.out.println("Invalid seeds number : " + seedNumber);
		  return null;
	  }
		  
	  
//	  ImagePlus impSe = converteLabelToImageL(label2, "seeds label imp");
//	  impSe.show();
//	  IJ.save(impSe, "d:/seedsLabelImp.tif");
	  return label2;
}

  /**
   * Compute seeds with threshold and compute corresponding shapeSet.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param fluobt Fluo_Back_Tracker the parameters are taken from.
   * @return
   * @throws InterruptedException
   */
  public ShapeSet seedsFromBin_ShapeS(int threshold,
                                     Fluo_Bac_Tracker fluobt) throws
      InterruptedException
  {
    int[] label = seedsFromBinS(threshold, fluobt.getConn8());
    return Utils.LabelsToShapeSet(label, width, height, BACKGROUND_LABEL,
                                  fluobt);
  }
  
  /**
   * Compute seeds with threshold and compute corresponding shapeSet.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param fluobt Fluo_Back_Tracker the parameters are taken from.
   * @return
   * @throws InterruptedException
   */
  public int[] seedsFromBin_ShapeF(int threshold,
                                     Fluo_Bac_Tracker fluobt) throws
      InterruptedException
  {
    int[] label = seedsFromBinF(threshold, fluobt.getConn8());
    return label;
  }
  
  
  /**
   * Compute seeds with threshold and compute corresponding shapeSet.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param fluobt Fluo_Back_Tracker the parameters are taken from.
   * @return
   * @throws InterruptedException
   */
  public int[] seedsFromBin_ShapeF_noFoci(double toleRatio,double tolerenceMaxF, double otsuRatio, ImageProcessor sp) throws
      InterruptedException
  {
    int[] label = seedsFromMaximuFinderF_no_foci(toleRatio, tolerenceMaxF, otsuRatio, sp);
    return label;
  }

  /**
   * Compute seeds with threshold and compute corresponding shapeSet.
   *
   * @param threshold Value determining seeds max_greyits.
   * @param fluobt Fluo_Back_Tracker the parameters are taken from.
   * @return
   * @throws InterruptedException
   */
  public int[] seedsFromErode(int erodeNumber, ImageProcessor sp,double ratioNorm) throws
      InterruptedException
  {
    int[] label = seedsFromErodeStep(erodeNumber, sp,ratioNorm);
    return label;
  }
  
  public int[] seedsFromErode(int erodeNumber, ByteProcessor sp, int minimumPixelNumberBySeeds) throws
  InterruptedException
  {
	  int[] label = seedsFromErodeStep(erodeNumber, sp,minimumPixelNumberBySeeds );
	  return label;
  }
  
  public int[] seedsFromErodeWithoutBinary(int erodeNumber, ByteProcessor sp) throws
  InterruptedException
  {
	  int[] label = seedsFromErodeStepWithoutBinary(erodeNumber, sp);
	  return label;
  }
  /**
   * Other method to get the seeds by finding local minimums dark enough
   * (inferior to greyMax). Then seeds are grown around these minimums by
   * increasing the grey threshold of grey step until the seed area is superior
   * to MinArea.
   *
   *
   * @param MinSeedsArea Minimum area in pixels² of a seed.
   * @param maxGrey Maximum value of a local minimum to be consider as a seed.
   * @param greystep Increase value of grey level threshold.
   * @param connected8 Are the seeds 4 or 8 connected.
   *
   *
   * progressBar.
   * @return mask to tell which pixel belongs to which seed.
   */
  public int[] seedsFromLocalMin(int MinSeedsArea, int maxGrey, int greystep,
                                 boolean connected8)
  {

    //===================== init array of mask ===============================
    int nbPix = width * height;
    int[] label = new int[nbPix];
    for (int i = 0; i < nbPix; i++)
    {
      label[i] = BACKGROUND_LABEL;
    }

    //====================== l labelList of next seed ==============================
    int l = BACKGROUND_LABEL + 1;

    // ================  For each suitable pixel create a new seed =============
    // pixel should be unlabeled and local min  and not in the background
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        int i = coordToIndex(x, y);

        if (label[i] == BACKGROUND_LABEL && is_local_min(x, y) && getf(i)
                                                                  < maxGrey
            && background(i) == 0)
        {
          // ------------ Init the seed ----------------------------------------

          // labelList the pixel with the new labelList l
          label[i] = l;

          // init area to 1
          int area = 1;

          // shapeList to keep track of the border around the seed
          HashSet<Integer> border = new HashSet<Integer>();
          HashSet<Integer> init = getNeighIndex(i, connected8);

          for (Integer ibord : init)
          {
            if (background(ibord) == 0)
            {
              border.add(ibord);
            }
          }

          // grey level threshold that will be increased is init to the gray level of the local min
          int grey = get(x, y);

          // ------------ Grow the seed ----------------------------------------
          int added = 0;
          // by increasing its grey level threshold of greystep until area >= minArea
          while (area < MinSeedsArea && !border.isEmpty() && grey < maxGrey
                 && added < MinSeedsArea)
          {
            // List of point to treat 
            // it is empty once all points neighboring the seed and < to grey have been add to the seed
            HashSet<Integer> toTreat = new HashSet<Integer>(border);

            added = 0;
            while (!toTreat.isEmpty() && added < MinSeedsArea)
            {

              // === Solve first point to treat ===
              Integer icurr = (Integer) toTreat.iterator().next();

              // === remove pixel from the shapeList ===
              toTreat.remove(icurr);

              // === If pixel is in the background don't take it in account  ===
              //  If pixel is  sup to grey don't take it in account
              //  If pixel is already in the seed don't take it in account
              if (background(icurr) == 0 && getf(icurr) < grey && label[icurr]
                                                                  != l)
              {
                // === treat point ===
                // If it is labeled : merge the two neighbouring seeds
                // If not add just the point to the seed and its neighbors to the toTreat shapeList.
                if (label[icurr] != BACKGROUND_LABEL)
                {
                  // labeled = merge the seeds
                  int touchSeedLab = label[icurr];

                  for (int k = 0; k < nbPix; k++)
                  {
                    if (label[k] == touchSeedLab)
                    {
                      label[k] = l;
                      area++;
                      added++;
                    }
                  }
                }
                else
                {
                  // unlabeled = add it to the seed
                  label[icurr] = l;
                  area++;
                  added++;

                  // add its neighbors to the getNeigh shapeList if they aren't in background
                  HashSet<Integer> neigh = getNeighIndex(icurr, connected8);
                  for (Integer ineigh : neigh)
                  {
                    if (!toTreat.contains(ineigh) && background(ineigh) == 0
                        && label[ineigh] != l)
                    {
                      toTreat.add(ineigh);
                    }
                  }

                }

                // === UPDATE border ===
                // remove pixel added from it and add its neighbors that aren't in the seed already
                border.remove(icurr);

                HashSet<Integer> neigh = getNeighIndex(icurr, connected8);
                for (Integer ineigh : neigh)
                {
                  if (!border.contains(ineigh) && label[ineigh] != l
                      && background(ineigh) == 0)
                  {
                    border.add(ineigh);
                  }
                }

              }

            }

            // === increasing grey threshold ===
            grey += greystep;
          }

          // --------- increment labelList to next seed  --------------
          l++;
        }

      }
    }

    return label;
  }

  /**
   * Compute seeds with local minima and compute corresponding shapeSet.
   *
   * @param MinSeedsArea Minimum area in pixels² of a seed.
   * @param maxGrey Maximum value of a local minimum to be consider as a seed.
   * @param greystep Increase value of grey level threshold.
   * @param fluobt Fluo_Back_tracker the parmeters are taken from.
   * @return
   */
  public ShapeSet seedsFromLocalMin_Shape(int MinSeedsArea, int maxGrey,
                                          int greystep, Fluo_Bac_Tracker fluobt)
  {
    int[] label = seedsFromLocalMin(MinSeedsArea, maxGrey, greystep, fluobt.
        getConn8());
    return Utils.LabelsToShapeSet(label, width, height, BACKGROUND_LABEL,
                                  fluobt);
  }

  // -------------------------------------------------------------------------
  //                Local extremums
  // -------------------------------------------------------------------------
  /**
   * This function checks if the pixel (x,y) is a local minimum or max_greyimum
   * considering its 8 neighbors.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param min true : searching minimum false searching max_greyimum
   * @return true if (x, y) is a local minimum, false otherwise
   */
  public boolean is_local_extremum(int x, int y, boolean min)
  {
    // grey level of the pixel
    float grey = getf(x, y);

    // pixel must be inferior/superior or equal to all its neighbours
    boolean compAll = true;

    // pixel must be strictly inferior/superior to at least one of its neighbours
    boolean compOne = false;

    HashSet<Integer> neigh = getNeighIndex(x, y, true);
    // for each neighbour
    for (Integer neighInd : neigh)
    {
      // get neighbour grey level
      float neighGrey = getf(neighInd);

      // we check that the neighbour greylevel is inferior/superior or equal to grey
      if (min)
      {
        compAll = compAll && (grey <= neighGrey);
      }
      else
      {
        compAll = compAll && (grey >= neighGrey);
      }

      // compOne is set to true if the neighbour grey level is strictly inferior/superior to grey 
      if (min)
      {
        compOne = compOne || (grey < neighGrey);
      }
      else
      {
        compOne = compOne || (grey > neighGrey);
      }
    }

    return (compAll && compOne);
  }

  /**
   * This function checks if the pixel (x,y) is a local minimum considering its
   * 8 neighbors.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @return true if (x, y) is a local minimum, false otherwise
   */
  public boolean is_local_min(int x, int y)
  {
    return is_local_extremum(x, y, true);
  }

  /**
   *
   * This function checks if the pixel (x,y) is a local max_greyimum considering
   * its 8 neighbors.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @return true if (x, y) is a local max_greyimum, false otherwise
   */
  public boolean is_local_max(int x, int y)
  {
    return is_local_extremum(x, y, false);
  }

  /**
   * Find a local minimum from a point in the image.
   *
   * Starting from the orig point, we move to its darkest neighbor then to the
   * darkest neighbor of this new point, etc. Until a local minimum or a
   * max_greyimum step number was reach. If max_greyimum step number was reach
   * before a local minimum was found, it returns null.
   *
   *
   * @param orig Starting point to find minimum.
   * @param iterMax Maximum step number.
   * @return local minimum or null.
   */
  public Point FindNearlocalMin(Point orig, int iterMax)
  {
    // =========== Init variables =============================================
    Point currPt = orig;
    int iter = 0;

    // ======= While current point is not a minimum ===========================
    //            and maximum step number not reach 
    // 1 ) Increment iteration number
    // 2 ) Compute neighbors indexes of current point
    // 3 ) Find darkest (minimum grey level) neighbor
    // 4 ) Set this neighbor as current point
    while (!is_local_min(currPt.x, currPt.y) && iter < iterMax)
    {
      // ----------- 1 ) Increment iteration number ---------------------------
      iter++;

      // -------- 2 ) Compute neighbors indexes of current point --------------
      HashSet<Integer> neighIndList = getNeighIndex(currPt.x, currPt.y, true);

      // --------- 3 ) Find darkest (minimum grey level) neighbor -------------
      int minIndex = neighIndList.iterator().next();
      for (int currNeighIndex : neighIndList)
      {
        if (getf(minIndex) > getf(currNeighIndex))
        {
          minIndex = currNeighIndex;
        }
      }

      // ---------- 4 ) Set this neighbor as current point --------------------
      currPt = indexToPoint(minIndex);
    }

    // =============== If maximum iteration number was reach return null ======
    if (iter == iterMax)
    {
      System.out.println("Couldn't find near local minimum.");
      return orig;
    }

    // ========== else return local minimum ===================================
    return currPt;

  }

  /**
   * Find a local minimum from a point in a blob ShapeFbt.
   *
   * Starting from the orig point in the blob, we move to its darkest neighbor
   * that is in the blob then to the darkest neighbor in the blob of this new
   * point, etc. Until a local minimum or a max_greyimum step number was reach.
   * If max_greyimum step number was reach before a local minimum was found, it
   * returns null.
   *
   * @param blob ShapeFbt in which to find the local minimum.
   * @param orig Starting point to find minimum.
   * @param iterMax Maximum step number.
   * @return local minimum or null.
   */
  public Point slice(ShapeFbt blob, Point orig, int iterMax)
  {

    // ================== Check that orig point is in the blob ================
    //    if not, raise an out of bound exception.
    HashSet<Point> pixels = blob.getPixels();
    if (!pixels.contains(orig))
    {
      throw new IndexOutOfBoundsException(
          "Error in ImageFbt.FindNearLocalMinBlob : origin point not in blob.");
    }

    // =========== Init variables =============================================
    Point currPt = orig;
    int iter = 0;

    // ======= While current point is not a minimum ===========================
    //            and maximum step number not reach 
    // 1 ) Increment iteration number
    // 2 ) Compute neighbors indexes of current point
    // 3 ) Find darkest (minimum grey level) neighbor in the blob
    // 4 ) Set this neighbor as current point
    while (!is_local_min(currPt.x, currPt.y) && iter < iterMax)
    {
      // ----------- 1 ) Increment iteration number ---------------------------
      iter++;

      // -------- 2 ) Compute neighbors indexes of current point --------------
      HashSet<Integer> neighIndList = getNeighIndex(currPt.x, currPt.y, true);

      // ----- 3 ) Find darkest (minimum grey level) neighbor in the blob -----
      int minIndex = neighIndList.iterator().next();
      for (int currNeighIndex : neighIndList)
      {
        if (pixels.contains(indexToPoint(currNeighIndex))
            && getf(minIndex) > getf(currNeighIndex))
        {
          minIndex = currNeighIndex;
        }
      }

      // ---------- 4 ) Set this neighbor as current point --------------------
      currPt = indexToPoint(minIndex);
    }

    // =============== If maximum iteration number was reach return null ======
    if (iter == iterMax)
    {
      System.out.println("Couldn't find near local minimum.");
      return orig;
    }

    // ========== else return local minimum ===================================
    return currPt;

  }

  // -------------------------------------------------------------------------
  //                        Renormalization
  // -------------------------------------------------------------------------
  // ------------------------ Convolution ------------------------------------
  /**
   * Convolution using as kernel a square of width 'size'*2+1 Each result pixel
   * is the average of the pixels around its position in original image. Average
   * value is first compute on lines then on columns.
   *
   * @param size use to compute kernel square border size.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void squareConvol(int size) throws InterruptedException
  {
    float[][] tmp = new float[width][height];
    int maxCol = width - 1;
    int maxLin = height - 1;

    // ========================== Line filter ==================================
    // for each pixel
    int line, col;
    for (col = 0; col < width; col++)
    {
      // Column of the first pixel to take in account 
      int firstCol = Math.max(0, col - size);

      // Column of the last pixel to take in account 
      int lastCol = Math.min(col + size, maxCol);

      for (line = 0; line < height; line++)
      {

        // Check if thread is interrupted and raise exception if necessary
        Utils.checkThreadInterruption();

        float sum = 0;
        float norm = 0;

        // for each of the pixel to take in account
        int x;
        for (x = firstCol; x <= lastCol; x++)
        {
          // sum up the grey levels
          sum += getf(x, line);
          norm++;
        }

        // set average value to the temporary image
        tmp[col][line] = sum / norm;
      }
    }

    // ============================== Column filter ============================
    /*
     * mask pixel is the average of the "size" pixels of the tmp image around it
     * in its column
     */
    // for each pixel
    for (line = 0; line < height; line++)
    {
      // Line of the first pixel to take in account 
      int firstLin = Math.max(0, line - size);

      // Line of the last pixel to take in account 
      int lastLin = Math.min(line + size, maxLin);

      for (col = 0; col < width; col++)
      {
        float sum = 0;
        float norm = 0;

        // for each of the pixel to take in account
        for (int y = firstLin; y <= lastLin; y++)
        {
          // sum up the grey levels
          sum += tmp[col][y];
          norm++;
        }

        // set average value to the mask image
        setf(col, line, sum / norm);
      }
    }

  }

  // -------------------------- Extrapolation --------------------------------
  /**
   * From some points given by a mask, extrapolate to the whole image with a
   * squareConvolution. Show progress in a progressBarFbt.
   *
   * @param mask shapeList of float : 0. if point not to take in account and >
   * 0. if one of the point to extrapolate
   * @param size Size of the square in the square Convolution (see squareConvol
   * function)
   * @param iter Number of convolutions applied to the image.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void extrapolate(float[] mask, int size, int iter) throws
      InterruptedException
  {

    // ======== Init result image and needed temporary images ==========
    ImageFbt tmp1 = new ImageFbt(width, height, background);
    ImageFbt tmp2 = new ImageFbt(width, height, background);

    // =========  update temporary images ==========
    // For each pixel of the image, if it is known (not in background and not to 0)
    // tmp 1 is set to the mask value
    // tmp2 is set to 1000 
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        int index = coordToIndex(x, y);
        if (mask[index] < 1.e-6 || background(index) == 1) // Non connu
        {
          tmp1.setf(x, y, 0);
          tmp2.setf(x, y, 0);
        }
        else // connu
        {
          tmp1.setf(x, y, mask[index]);
          tmp2.setf(x, y, 1000);
        }
      }
    }

    // ========== Apply a square convolution to temporary images ==========
    for (int i = 0; i < iter; i++)
    {
      tmp1.squareConvol(size);
      tmp2.squareConvol(size);
    }

    // ========== Update result image using temporary images =========
    // result grey = tmp1 * 1000 / tmp2
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        float currTmp2Pix = tmp2.getf(x, y);
        if (currTmp2Pix > 1.e-6)
        {
          float currTmp1Pix = tmp1.getf(x, y);
          setf(x, y, (float) Math.min(maxValue(), currTmp1Pix * 1000.f
                                                  / currTmp2Pix));
        }
        else
        {
          setf(x, y, 0);
        }
      }
    }
  }

  // -------------------------- Low mask -------------------------------------
  /**
   * Compute a mask containing seeds values in seeds and 0 elsewhere. Used to
   * renormalize image. Show progress in a progressBarFbt.
   *
   * @param seeds ShapeSet of the seeds.
   *
   *
   * progressBar.
   * @return a mask (float[]) with seeds grey level if the pixel is in a seed
   * and 0 if not.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public float[] lowMask(ShapeSet seeds) throws InterruptedException
  {
    float[] mask = new float[width * height];

    // ====== init mask to 0. ======
    int i;
    int size = width * height;
    for (i = 0; i < size; i++)
    {
      mask[i] = 0.0f;
    }

    // ====== For each pixel in a seed, put its grey level in the mask. ======
    for (ShapeFbt currSeed : seeds.shapeList.values())
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      for (Point pt : currSeed.pixels)
      {
        int x = pt.x;
        int y = pt.y;

        mask[coordToIndex(x, y)] = getf(x, y);
      }
    }

    return mask;
  }

  // -------------------------- High mask ------------------------------------
  /**
   * Compute a mask containing in seeds the value of grey level of the cell
   * border. This value is obtained by sampling grey level in the seed
   * neighborhood, approximately at the cell radius distance. For more precision
   * see Maël Primet phd "Probabilistic methods for point tracking and
   * biological analysis" (2.2.1). Show progress in a progressBarFbt.
   *
   * @param seeds ShapeSet of the seeds.
   * @param radius a bit more than cell radius to sample grey level around seed.
   *
   *
   * progressBar.
   * @return a mask (float[]) with cell border grey level if the pixel is in a
   * seed and 0 if not.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public float[] highMask(ShapeSet seeds, int radius) throws
      InterruptedException
  {
    ImageFbt tmp = new ImageFbt(this);
    tmp.closing(radius);

    return tmp.lowMask(seeds);
  }

  // ------------------------ Renormalization --------------------------------
  /**
   * Renormalize picture. Images from microscopy can have regions a bit darker
   * and others a bit lighter. This method treat image so that grey level is
   * homogeneous over all image. It compute a low and a high mask from given
   * seeds. Convolution is applied to those two masks. Then image is renormalize
   * given these local minima and max_greyima. Show progress in a
   * progressBarFbt.
   *
   * @param seeds ShapeSet of the seeds.
   * @param convolSize Size of the square in the square Convolution (see
   * squareConvol function)
   * @param iter
   * @param highRadius a bit more than cell radius to sample grey level around
   * seed. (see highMask method).
   *
   *
   * progressBar.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void renormalize(ShapeSet seeds, int convolSize, int iter,
                          int highRadius) throws InterruptedException
  {
    // ================== Variables =========================================
    int min = (int) grey255ToMax(20);
    int max = (int) grey255ToMax(80);

    // =========== Compute low and high mask =========
    // low mask : array of float with 0 if not in a seed and grey level of original image if in seed
    float[] lowMask = lowMask(seeds);
    // high mask : array of float with 0 if not in a seed and elsewhere
    // maximum over the seed of minimums in each pixels of the seeds 
    // of maximum of grey level in a disc of radius "highradius" containing the pixel
    float[] highMask = highMask(seeds, highRadius);

    // ========== Extrapolate high and low mask over the whole image =========
    ImageFbt low = new ImageFbt(this);
    low.extrapolate(lowMask, convolSize, iter);
    ImageFbt high = new ImageFbt(this);
    high.extrapolate(highMask, convolSize, iter);

    // ========== Apply the renormalization to each pixel ============
    // new grey = min + (max_grey- min) * (( old grey - low) / (high - low) ) 
    // max_grey and min : chosen values to get grey levels over the whole possible range.
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        // if in background, the grey value is let to 0
        int index = coordToIndex(x, y);
        if (background(index) == 0)
        {

          // Check if thread is interrupted and raise exception if necessary
          Utils.checkThreadInterruption();

          // --- Compute new grey  level for the pixel ---
          float ratio = (getf(x, y) - low.getf(x, y)) / (high.getf(x, y) - low.
                                                         getf(x, y));

          float newf = (float) (min + (max - min) * ratio);

          // --- If new values is superior to the maximum possible value or inferior to the minimum possible value ---
          // It is set to maximum or minimum value
          if (newf > maxValue())
          {
            newf = (float) maxValue();
          }
          else if (newf < minValue())
          {
            newf = (float) minValue() + 1;
          }

          // --- Set the new grey level ---
          setf(x, y, newf);
        }
        else
        {
          // --- If background, set to 0 ---
          setf(x, y, 0);
        }
      }
    }

  }

  // -------------------------------------------------------------------------
  //                        Non homogeneous Dilatation 
  // -------------------------------------------------------------------------
  /**
   * Dilatation non homogeneous to dilate the blobs from seeds.
   *
   * @param seed array of seeds to start the dilatation from
   * @param viscosity array of viscosity of dilatation over the image
   * @param iter_max number of iterations max_greyimum as a max_greyit to the
   * dilatation computation.
   * @param connected8 are the seeds and blobs 4 or 8 connected.
   * @return
   * @throws java.lang.InterruptedException
   */
  public int[] dilateS(ShapeSet seed, float[] viscosity, int iter_max,
                      boolean connected8) throws InterruptedException
  {
    // ========================== Variables ===================================
    // labelList array : array containing wich point is in wich blob
    int[] label = new int[width * height];

    // frozen array set points that are fixed to a labelList definitively
    boolean[] frozen = new boolean[width * height];

    // Two ArrayLists of points to treat (to keep track of where to dilate next)
    // toTreatList is shapeList of indexes of points
    // toTreatDist is shapeList of distances from its seed
    ArrayList<Integer> toTreatInd = new ArrayList<Integer>();
    ArrayList<Float> toTreatDist = new ArrayList<Float>();

    // ========================== Initialization ==============================
    // ---------- Initialisation of mask labelList and frozen shapeList -----------
    for (int i = 0; i < width * height; i++)
    {
      // labelList is set to BACKGROUND_LABEL
      label[i] = BACKGROUND_LABEL;

      //  frozen is set to true in background and false elsewhere
      frozen[i] = (background(i) == 1);
    }

    // --------- Init toTreat shapeList and labelList shapeList from seeds --------
    // === Update seeds  center radius and boundaries ===
    seed.updateCenterAndRadius();
    seed.updateBoundaries();

    // === Use each seed to init toTreat toTreatDist and labels ===
    for (Map.Entry<Integer, ShapeFbt> entry : seed.shapeList.entrySet())
    {
      int currLabel = entry.getKey();
      ShapeFbt currSeed = entry.getValue();

      // --- Init seeds labelList ---
      // For each seeds point , set frozen to true and labelList to seed's labelList
      for (Point pt : currSeed.pixels)
      {
        int currInd = coordToIndex(pt.x, pt.y);

        label[ currInd] = currLabel;
        frozen[ currInd] = true;
      }

      // --- Initialisation of array toTreat with points bordering seeds ---
      for (Point pt : currSeed.boundary)
      {
        int currInd = coordToIndex(pt.x, pt.y);

        // distance to the seed is init at 0
        toTreatInd.add(currInd);
        toTreatDist.add(0.0f);
      }
    }

    // ============================ Computation ================================
    // Number of iteration and number of points to treat untill next iteration
    int nbIter = 0;
    int nbToNextIter = toTreatInd.size();

    // ------------- Until max_grey iteration number or to Treat empty -------------
    // do a dilatation step
    while (!toTreatInd.isEmpty() && nbIter < iter_max)
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      // === Manage iteration count ===
      if (nbToNextIter == 0)
      {
        nbIter++;
        System.out.println(nbIter);
        nbToNextIter = toTreatInd.size();
      }

      // === Dilatation step ===
      Object[] res = dilateStepS(label, toTreatInd, toTreatDist, frozen,
                                viscosity, connected8);

      label = (int[]) res[0];
      toTreatInd = (ArrayList<Integer>) res[1];
      toTreatDist = (ArrayList<Float>) res[2];
      frozen = (boolean[]) res[3];

      // === decrement nbToNextIter ===
      nbToNextIter--;
    }

    // ==================== return labelList ======================================
    return label;
  }
  
  //-------------------------------------------------------------------------
  //                        Non homogeneous Dilatation 
  // -------------------------------------------------------------------------
  /**
   * Dilatation non homogeneous to dilate the blobs from seeds.
   *
   * @param seed array of seeds to start the dilatation from
   * @param viscosity array of viscosity of dilatation over the image
   * @param iter_max number of iterations max_greyimum as a max_greyit to the
   * dilatation computation.
   * @param connected8 are the seeds and blobs 4 or 8 connected.
   * @return
   * @throws java.lang.InterruptedException
   */
  public int[] dilateF(int[] seed, float[] viscosity, final int iter_max,
		  boolean connected8) throws InterruptedException
		  {
	  // ========================== Variables ===================================
	  // labelList array : array containing wich point is in wich blob
	  int size = width*height;
	  if(size!=seed.length)
		  return null;

	  int npoints = 0;
	  int nTotreat = 0;
	  int maxPoints = 50; // will be increased if necessary
	  int[] toTreatInd = new int[maxPoints];//updateCenterAndRadius(seed);
	  
	  float[] toTreatDist = new float[maxPoints];

	  for(int i=size-1; i>=0; i--)
	  {
		  int seedLabel = seed[i];
		  
		  if(seedLabel==BACKGROUND_MASK) // do not consider background and BACKGROUNDLABEL
			  continue;
		  
		  if(seedLabel<=0) // do not consider background and BACKGROUNDLABEL
			  continue;
		  
		  if(seedLabel%2==0)
		  {			  
			  int x = i%width;
			  int y = i/width;
			  for (int d=0; d<8; d++) 
			  {  
				  int x2 = x+DIR_X_OFFSET[d];
				  int y2 = y+DIR_Y_OFFSET[d];

				  if(!inside(x2, y2))
					  continue;
				  
				  int pos = x2 + y2*width;

				  if(seed[pos] == seedLabel || seed[pos]!=BACKGROUND_LABEL)
					  continue;
				  
				  if(seed[pos]==BACKGROUND_MASK)
					  continue;
				  
				  if (npoints==maxPoints) {
					  int[] toTreatIndtemp = new int[maxPoints*2];
					  float[] toTreatDisttemp = new float[maxPoints*2];
					  System.arraycopy(toTreatInd, 0, toTreatIndtemp, 0, maxPoints);
					  System.arraycopy(toTreatDist, 0, toTreatDisttemp, 0, maxPoints);
					  toTreatInd = toTreatIndtemp;
					  toTreatDist = toTreatDisttemp;
					  maxPoints *= 2;
				  }
				  toTreatInd[npoints] = pos;
				  toTreatDist[npoints] = 0.0f;
				  npoints++;
				  nTotreat++; // add one toTreat point
				  seed[pos] = seedLabel - 1; //-1                     
			  }
		  }
	  }
	  
	  //quicksort(toTreatDist, toTreatInd);
/*	  for(int i=0; i<nTotreat; i++)
	  {
		  System.out.println(toTreatInd[i]);
	  }*/
	  
	  int npointsToTreat = npoints;
	  
	  //System.out.println("		inition npoints: "+npoints);

	  // ============================ Computation ================================
	  // Number of iteration and number of points to treat untill next iteration
	  int nbIter = 0;
	  // ------------- Until max_grey iteration number or to Treat empty -------------
	  // do a dilatation step
	  int nbToNextIter = nTotreat-1;
	  while (npoints>1 && nbIter < iter_max)
	  {
		  // Check if thread is interrupted and raise exception if necessary
		  if(IJ.escapePressed())
			  break;

		  // === Manage iteration count ===
		  if (nbToNextIter == 0)
		  {
			  nbIter++;
			  //System.out.println("		iteration " +nbIter);
			  nbToNextIter = npoints;
		  }

		  // === Dilatation step ===
		  Object[] res = dilateStepF(seed, toTreatInd, toTreatDist,npoints,npointsToTreat, maxPoints,viscosity, connected8);

		  toTreatInd =  (int[])res[0];
		  toTreatDist = (float[]) res[1];
		  npoints = ((Integer) res[2]).intValue();
		  npointsToTreat = ((Integer) res[3]).intValue();
		  maxPoints = ((Integer) res[4]).intValue();
		  
		  //System.out.println("		npoints: "+npoints + " nbIteration: " + nbToNextIter);
		  // === decrement nbToNextIter ===
		  nbToNextIter--;
	  }

   // ==================== return labelList ======================================
   return seed;
 }

  
  //************************************************************
  public static void quicksort(float[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}

	// quicksort a[left] to a[right]
	public static void quicksort(float[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(float[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

	// is x < y ?
	private static boolean less(float x, float y) {
	    return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(float[] a, int[] index, int i, int j) {
	    float swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}
	///////////////////
	
	
	


  /**
   * A step of non homogeneous dilatation.
   *
   *
   * @param label list of seed labelList for each pixel.
   * @param toTreatInd ArrayList of Index of pixels to dilate
   * @param toTreatDist ArrayList of Distance to seed of pixels to dilate.
   * @param frozen List of boolean to know which pixel was already computed.
   * @param viscosity viscosity of the image
   * @param connected8 are blobs 8 connected ?
   * @return
   */
  public Object[] dilateStepS(int[] label,
                             ArrayList<Integer> toTreatInd,
                             ArrayList<Float> toTreatDist,
                             boolean[] frozen,
                             float[] viscosity,
                             boolean connected8)
  {
    // ======== pixel with min distance to its seed of the HashMap toTreat ====
    float currDist = Collections.min(toTreatDist);
    int currPos = toTreatDist.indexOf(currDist);
    int currInd = toTreatInd.get(currPos);

    //froze it
    frozen[currInd] = true;

    // ============== for each neighbor of this pixel =========================
    // 1 ) If neighbor is frozen do nothing
    // 2 ) Compute distance of neighbor to the seed
    // 3 ) If neighbor is in toTreat ( == has already a labelList / seed )
    //     3a ) get distance of neighbor to its current seed.
    //     3b ) if the distance to the new seed is smaller, the neighbour get this new labelList / seed
    // 4 ) Else, it get the new seed labelList and is add to toTreat lists
    HashSet<Integer> neigh = getNeighIndex(currInd, connected8);
    for (int neighInd : neigh)
    {

      // -------------- 1 ) If neighbor is frozen do nothing ------------------
      if (frozen[neighInd] || label[neighInd] == label[currInd])
      {
        continue;
      }

      // -------- 2 ) Compute distance of neighbor to the seed ----------------
      float newD = currDist + (float) distance(currInd, neighInd) * 0.5f
                              * (viscosity[neighInd] + viscosity[currInd]);

      // --- 3 ) If neighbor is in toTreat ( == has already a labelList / seed ) --
      //     3a ) get distance of neighbor to its current seed.
      //     3b ) if the distance to the new seed is smaller, 
      //          the neighbour get this new labelList / seed
//      if ( toTreatInd.contains( neighInd ) )
      if (label[neighInd] != BACKGROUND_LABEL)
      {
        // === 3a ) get distance of neighbor to its current seed. ===
        int neighPos = toTreatInd.indexOf(neighInd);
        float d = toTreatDist.get(neighPos);

        // === 3b ) if the distance to the new seed is smaller, ===
        //          the neighbour get this new labelList / seed
        if (newD < d)
        {
          label[neighInd] = label[currInd];

          toTreatDist.set(neighPos, newD);
        }
      }
      // --- 4 ) Else, it get the new seed labelList and is add to toTreat lists --
      else
      {
        // it get the seed labelList
        label[neighInd] = label[currInd];

        // It is add to toTreat shapeList
        toTreatInd.add(neighInd);
        toTreatDist.add(newD);
      }

    }

    // ==============  remove treated pixel from toTreat ======================
    toTreatInd.remove(currPos);
    toTreatDist.remove(currPos);

    // ============== Return results for next step ============================
    Object[] res =
    {
      label, toTreatInd, toTreatDist, frozen
    };
    return res;
  }
  
  /**
   * A step of non homogeneous dilatation.
   *
   *
   * @param label list of seed labelList for each pixel.
   * @param toTreatInd ArrayList of Index of pixels to dilate
   * @param toTreatDist ArrayList of Distance to seed of pixels to dilate.
   * @param frozen List of boolean to know which pixel was already computed.
   * @param viscosity viscosity of the image
   * @param connected8 are blobs 8 connected ?
   * @return
   */
  public Object[] dilateStepF(int[] seed,
                             int[] toTreatInd,
                             float[] toTreatDist,
                             int npoints,
                             int npointsToTreat,
                             int maxPoints,
                             float[] viscosity,
                             boolean connected8)
  {
    // ======== pixel with min distance to its seed of the HashMap toTreat ====
	  float currDist = Float.MAX_VALUE;
	  int currentPoint = 0;
	  int currInd = toTreatInd[0];
	  for(int i=0; i<npoints; i++)
	  {
		  if(currDist > toTreatDist[i])
		  {
			  currInd = toTreatInd[i];
			  currDist = toTreatDist[i];			  
			  currentPoint = i;
		  }
	  }
	  //System.out.println(currDist);
	  
	  int x = currInd%width;
	  int y = currInd/width;
	  
	  for (int d=0; d<8; d++) 
	  {  
		  int x2 = x+DIR_X_OFFSET[d];
		  int y2 = y+DIR_Y_OFFSET[d];

		  if(!inside(x2, y2))
			  continue;

		  int neighInd = x2 + y2*width;

		  if(seed[neighInd] >0 || seed[neighInd] == seed[currInd])
			  continue;

	      if(seed[neighInd]==BACKGROUND_MASK)
	    	  continue;

	      // -------- 2 ) Compute distance of neighbor to the seed ----------------
	      float newD = currDist + (float) distanceNeighbor(currInd, d) * 0.5f
	                              * (viscosity[neighInd] + viscosity[currInd]);
	      
	      //System.out.println("		new D : " + newD + " Current D : " + currDist + " viscosity[currInd] : "+ viscosity[currInd] + " distanceNeighbor(currInd, d) " + distanceNeighbor(currInd, d) );
	      if (seed[neighInd] != BACKGROUND_LABEL)
	      {
	    	  int neighPos = -1;
	    	  for(int i=0; i<npoints;i++)
	    	  {
	    		  if(toTreatInd[i] == neighInd)
	    		  {
	    			neighPos = i;
	    			break;
	    		  }
	    	  }
	          float dist = toTreatDist[neighPos];
	          
	          System.out.println("Rober neighbor");
	          // === 3b ) if the distance to the new seed is smaller, ===
	          //          the neighbour get this new labelList / seed
	          if (newD < dist)
	          {
	            seed[neighInd] = seed[currInd];
	            toTreatDist[neighPos] = newD;
	          }
	      }else {
	          // it get the seed labelList
	          seed[neighInd] = seed[currInd];//+2
	          if (npointsToTreat == maxPoints) {
				  int[] toTreatIndtemp = new int[maxPoints*2];
				  float[] toTreatDisttemp = new float[maxPoints*2];
				  System.arraycopy(toTreatInd, 0, toTreatIndtemp, 0, maxPoints);
				  System.arraycopy(toTreatDist, 0, toTreatDisttemp, 0, maxPoints);
				  toTreatInd = toTreatIndtemp;
				  toTreatDist = toTreatDisttemp;
				  maxPoints *= 2;
			  }
			  toTreatInd[npoints] = neighInd;
			  toTreatDist[npoints] = newD;
			  npointsToTreat++; // not as npoints,no decrease
			  npoints++;
			  //System.out.println("			npoint in dilateStepF : " + npoints);
		  }
	  }
	  
	  int len = toTreatInd.length;
	  int[] toTreatIndtemp = new int[len];
	  float[] toTreatDisttemp = new float[len];
	  System.arraycopy(toTreatInd, 0, toTreatIndtemp, 0, currentPoint);
	  System.arraycopy(toTreatInd, currentPoint+1, toTreatIndtemp, currentPoint,len - currentPoint-1);
	  
	  System.arraycopy(toTreatDist, 0, toTreatDisttemp, 0, currentPoint);
	  System.arraycopy(toTreatDist, currentPoint+1, toTreatDisttemp, currentPoint,len - currentPoint-1);
	  
	  toTreatInd = toTreatIndtemp;
	  toTreatDist = toTreatDisttemp;
	  
	  npoints = npoints - 1;

    // ============== Return results for next step ============================
    Object[] res =
    {
      toTreatInd, toTreatDist, npoints, npointsToTreat, maxPoints
    };
    return res;
  }

  /**
   * Compute non-homogeneous dilatation and compute ShapeSet of blobs from it.
   *
   * @param seed array of seeds to start the dilatation from.
   * @param viscosity array of viscosity of dilatation over the image.
   * @param iter_max number of iterations max_greyimum as a max_greyit to the
   * dilatation computation.
   * @param fluobt
   * @return ShapeSet of blobs.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ShapeSet dilate_ShapeS(ShapeSet seed, float[] viscosity, int iter_max,
                               Fluo_Bac_Tracker fluobt) throws
      InterruptedException
  {
    int[] label = dilateS(seed, viscosity, iter_max, fluobt.getConn8());
    ShapeSet res = Utils.LabelsToShapeSet(label, width, height,
                                          BACKGROUND_LABEL, fluobt);
    res.possibleCells = seed.possibleCells;
    res.connectGraph = seed.connectGraph;
    return res;
  }
  
  /**
   * Compute non-homogeneous dilatation and compute ShapeSet of blobs from it.
   *
   * @param seed array of seeds to start the dilatation from.
   * @param viscosity array of viscosity of dilatation over the image.
   * @param iter_max number of iterations max_greyimum as a max_greyit to the
   * dilatation computation.
   * @param fluobt
   * @return ShapeSet of blobs.
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public int[] dilate_ShapeF(int[] seed, float[] viscosity, int iter_max,
                               Fluo_Bac_Tracker fluobt) throws
      InterruptedException
  {
    int[] label = dilateF(seed, viscosity, iter_max, fluobt.getConn8());
    //ImagePlus imp = converteLabelToImage(label, "Dilation result");
    //ShapeSet res = Utils.LabelsToShapeSet(label, width, height,BACKGROUND_LABEL, fluobt);
    //res.possibleCells = seed.possibleCells;
    //res.connectGraph = seed.connectGraph;
    return label;
  }
  
  public ByteProcessor autoLocalThreshold(ImageProcessor sp, double ratioNormalisation)
  {
	  sp = normalisation(sp, ratioNormalisation);
	  Auto_Local_Threshold local_Threshold = new Auto_Local_Threshold();    	
  	  ImageProcessor bp = sp.convertToByte(true);
  	  bp.invert();
  	  byte[] meanPixels = local_Threshold.Mean(bp, 15, true);
  	  return new ByteProcessor(width, height, meanPixels);
  }
  
  public ImagePlus converteLabelToImageL(int[] label, String title)
  {
	  if(label.length!=width*height)
		  return null;
	  ImagePlus imp = IJ.createImage(title, "16-bit black", width, height, 1);
	  ImageProcessor ip = imp.getProcessor();
	  int maxLabel = Integer.MIN_VALUE;
	  for(int i=0; i<label.length;i++)
	  {
		  if(label[i] == BACKGROUND_MASK)
		  {
			  ip.set(i, 5);
			  continue;
		  }
		  
		  if(label[i] == BACKGROUND_LABEL)
		  {
			  ip.set(i, 4705);
			  continue;
		  }
		  ip.set(i, label[i]);		  
		  if(maxLabel < label[i])
			  maxLabel = label[i];
	  }
	  imp.setDisplayRange(0, maxLabel);
	  //imp.show();
      
	  //FileSaver fs = new FileSaver(imp);
	  //fs.saveAsTiff();
	  return imp;
  }
  
  public ImagePlus converteLabelToImageB(int[] label, String title)
  {
	  if(label.length!=width*height)
		  return null;
	  ImagePlus imp = IJ.createImage(title, "8-bit black", width, height, 1);
	  ImageProcessor ip = imp.getProcessor();

	  for(int i=0; i<label.length;i++)
	  {
		  int seedLabel = label[i];
		  
		  if(seedLabel == BACKGROUND_MASK)
		  {
			  ip.set(i, 0);
			  continue;
		  }
		  
		  if(seedLabel == BACKGROUND_LABEL)
		  {
			  ip.set(i, 0);
			  continue;
		  }
		  
		  ip.set(i, 255);
		  
		  int x = i%width;
		  int y = i/width;
		  
		  for (int d=0; d<8; d++) 
		  {  
			  int x2 = x+DIR_X_OFFSET[d];
			  int y2 = y+DIR_Y_OFFSET[d];

			  if(!inside(x2, y2))
				  continue;
			  
			  int pos = x2 + y2*width;

			  if(label[pos] == seedLabel || label[pos] == seedLabel -1 || seedLabel == label[pos] - 1)
				  continue;
			  
			  if(label[pos]==BACKGROUND_LABEL || label[pos]==BACKGROUND_MASK)
				  continue;
			  
			  ip.set(i, 100);              
		  }
	  }

	  //imp.show();
      
	  //FileSaver fs = new FileSaver(imp);
	  //fs.saveAsTiff();
	  return imp;
  }
  
  public ImagePlus mergeSmallCell(final int[] labelc, String title, int minArea)
  {
	  int[] label = labelc.clone();
	  if(label.length!=width*height)
		  return null;	  
	  
	  int maxPoints = 20;
	  int toTreatPoints = 0;
	  int[] area = new int[maxPoints]; // start from 10 cells
	  int[] labelIndex = new int[maxPoints];
	  for(int i=0; i<label.length; i++)
	  {
		  int seedLabel = labelc[i];
		  if(seedLabel == BACKGROUND_MASK)
		  {
			  continue;
		  }
		  
		  if(seedLabel == BACKGROUND_LABEL)
		  {
			  continue;
		  }
		  //System.out.println("seed label : " + seedLabel);
		  
		  toTreatPoints = (seedLabel+1)/startLabel - 1;
		  
		  label[i] = toTreatPoints;
		  
		  if(toTreatPoints == maxPoints)
		  {
			  int[] toTreatIndtemp = new int[maxPoints*2];					  
			  System.arraycopy(area, 0, toTreatIndtemp, 0, maxPoints);			  
			  area = toTreatIndtemp;
			  int[] toTreatIndextemp = new int[maxPoints*2];					  
			  System.arraycopy(labelIndex, 0, toTreatIndextemp, 0, maxPoints);			  
			  labelIndex = toTreatIndextemp;
			  maxPoints *= 2;
		  }
		  area[toTreatPoints] ++;
		  labelIndex[toTreatPoints] = label[i];
	  }
	  
	  int[] labelSmallCell = new int[area.length];
	  int k=0;
	  for(int i=0; i<area.length; i++)	  
	  {
		  if(area[i] == 0)
			  continue;
		  
		  if(area[i] <= minArea)
		  {
			  labelSmallCell[k] = labelIndex[i];			  
			  //System.out.println("Small cell found, surface of cell i : " + (i+1) + " : " + area[i] + " label index : " + labelIndex[i]);
			  k++;
		  }
	  }
	  
	  // merging step
	  //int[] mergedLabel = label.clone();
	  
	  for(int aIndex = 0; aIndex<k; aIndex++)
	  {
		  int labelSC = labelSmallCell[aIndex];
		  int[] toTreatSCpoints = new int[2];		  
		  int[] toTreatMergeCellLabel = new int[2];
		  
		  for(int i=0; i<label.length; i++)
		  {
			  int seedLabel = label[i];			  
			  if(seedLabel != labelSC && seedLabel != labelSC+1)
			  {
				  continue;
			  }
			  
			  int x = i%width;
			  int y = i/width;
			  
			  for (int d=0; d<8; d++) 
			  {  
				  int x2 = x+DIR_X_OFFSET[d];
				  int y2 = y+DIR_Y_OFFSET[d];
				  if(!inside(x2, y2))
					  continue;				  
				  int pos = x2 + y2*width;
				  if(label[pos] == seedLabel)
					  continue;
				  
				  if(label[pos]==BACKGROUND_LABEL || label[pos]==BACKGROUND_MASK)
					  continue;
				  
				  if(label[pos] > seedLabel)
				  {
					  toTreatMergeCellLabel[0] = label[pos];
					  toTreatSCpoints[0] ++;
				  }
				  else {
					  toTreatMergeCellLabel[1] = label[pos];
					  toTreatSCpoints[1] ++;
				  }
			  }			  
		  }
		  
		  int toMergeLabel;
		  
		  if (toTreatSCpoints[0]==0 && toTreatSCpoints[1] > 0) {
			  toMergeLabel = toTreatMergeCellLabel[1];
		  }else if (toTreatSCpoints[1]==0 && toTreatSCpoints[0] > 0) {
			  toMergeLabel = toTreatMergeCellLabel[0];
		  }else if(toTreatSCpoints[1]>0 && toTreatSCpoints[0] > 0){
			  toMergeLabel = toTreatSCpoints[0] <= toTreatSCpoints[1]? toTreatMergeCellLabel[0]:toTreatMergeCellLabel[1];
		  }
		  else {
			  toMergeLabel = -1;
		  }
		  //System.out.println("current label : " + labelSC + " be replaced by label : " + toTreatSCpoints[0] + "-"+toTreatSCpoints[1]);
		  if(toMergeLabel == -1)
			  continue;
		  
		  //System.out.println("		Connected with big cell, current label " + labelSC + " will be replaced by label " + toMergeLabel);
		  for(int i=0; i<label.length; i++)
		  {
			  int seedLabel = label[i];			  
			  if(seedLabel == labelSC)
			  {
				 label[i] = toMergeLabel;
			  }
		  }		  
	  }	  
	  
	  ImagePlus imp = IJ.createImage(title, "8-bit black", width, height, 1);
	  ImageProcessor ip = imp.getProcessor();

	  for(int i=0; i<label.length;i++)
	  {
		  int seedLabel = label[i];
		  
		  if(seedLabel == BACKGROUND_MASK)
		  {
			  ip.set(i, 0);
			  continue;
		  }
		  
		  if(seedLabel == BACKGROUND_LABEL)
		  {
			  ip.set(i, 50);
			  continue;
		  }
		  
		  ip.set(i, 255);
		  
		  int x = i%width;
		  int y = i/width;
		  
		  for (int d=0; d<8; d++) 
		  {  
			  int x2 = x+DIR_X_OFFSET[d];
			  int y2 = y+DIR_Y_OFFSET[d];

			  if(!inside(x2, y2))
				  continue;
			  
			  int pos = x2 + y2*width;

			  if(label[pos] == seedLabel)
				  continue;
			  
			  if(label[pos]==BACKGROUND_LABEL || label[pos]==BACKGROUND_MASK)
				  continue;
			  
			  ip.set(i, 100);              
		  }
	  }
	  //imp.show();
	  //FileSaver fs = new FileSaver(imp);
	  //fs.saveAsTiff();
	  return imp;
  }
  
  public ImagePlus mergeSmallCell(final int[] labelc, String title, int minArea, boolean mergingIntensity)
  {
	  int[] label = labelc.clone();
	  if(label.length!=width*height)
		  return null;	  
	  
	  int maxPoints = 20;
	  int toTreatPoints = 0;
	  int[] area = new int[10]; // start from 10 cells
	  int[] labelIndex = new int[10];
	  for(int i=0; i<label.length; i++)
	  {
		  int seedLabel = labelc[i];
		  if(seedLabel == BACKGROUND_MASK)
		  {
			  continue;
		  }
		  
		  if(seedLabel == BACKGROUND_LABEL)
		  {
			  continue;
		  }
		  //System.out.println("seed label : " + seedLabel);
		  
		  toTreatPoints = (seedLabel+1)/startLabel - 1;
		  
		  label[i] = toTreatPoints;
		  
		  if(toTreatPoints == maxPoints)
		  {
			  int[] toTreatIndtemp = new int[maxPoints*2];					  
			  System.arraycopy(area, 0, toTreatIndtemp, 0, maxPoints);			  
			  area = toTreatIndtemp;
			  int[] toTreatIndextemp = new int[maxPoints*2];					  
			  System.arraycopy(labelIndex, 0, toTreatIndextemp, 0, maxPoints);			  
			  labelIndex = toTreatIndextemp;
			  maxPoints *= 2;
		  }
		  area[toTreatPoints] ++;
		  labelIndex[toTreatPoints] = label[i];
	  }
	  
	  int[] labelSmallCell = new int[area.length];
	  int k=0;
	  for(int i=0; i<area.length; i++)	  
	  {
		  if(area[i] == 0)
			  continue;
		  
		  if(area[i] <= minArea)
		  {
			  labelSmallCell[k] = labelIndex[i];			  
			  System.out.println("Small cell found, surface of cell i : " + (i+1) + " : " + area[i] + " label index : " + labelIndex[i]);
			  k++;
		  }
	  }
	  
	  // merging step
	  //int[] mergedLabel = label.clone();
	  
	  for(int aIndex = 0; aIndex<k; aIndex++)
	  {
		  int labelSC = labelSmallCell[aIndex];
		  int[] toTreatSCpoints = new int[2];		  
		  int[] toTreatMergeCellLabel = new int[2];
		  
		  for(int i=0; i<label.length; i++)
		  {
			  int seedLabel = label[i];			  
			  if(seedLabel != labelSC && seedLabel != labelSC+1)
			  {
				  continue;
			  }
			  
			  int x = i%width;
			  int y = i/width;
			  
			  for (int d=0; d<8; d++) 
			  {  
				  int x2 = x+DIR_X_OFFSET[d];
				  int y2 = y+DIR_Y_OFFSET[d];
				  if(!inside(x2, y2))
					  continue;				  
				  int pos = x2 + y2*width;
				  if(label[pos] == seedLabel)
					  continue;
				  
				  if(label[pos]==BACKGROUND_LABEL || label[pos]==BACKGROUND_MASK)
					  continue;
				  
				  if(label[pos] > seedLabel)
				  {
					  toTreatMergeCellLabel[0] = label[pos];
					  toTreatSCpoints[0] ++;
				  }
				  else {
					  toTreatMergeCellLabel[1] = label[pos];
					  toTreatSCpoints[1] ++;
				  }
			  }			  
		  }
		  
		  int toMergeLabel;
		  
		  if (toTreatSCpoints[0]==0 && toTreatSCpoints[1] > 0) {
			  toMergeLabel = toTreatMergeCellLabel[1];
		  }else if (toTreatSCpoints[1]==0 && toTreatSCpoints[0] > 0) {
			  toMergeLabel = toTreatMergeCellLabel[0];
		  }else if(toTreatSCpoints[1]>0 && toTreatSCpoints[0] > 0){
			  toMergeLabel = toTreatSCpoints[0] <= toTreatSCpoints[1]? toTreatMergeCellLabel[0]:toTreatMergeCellLabel[1];
		  }
		  else {
			  toMergeLabel = -1;
		  }
		  //System.out.println("current label : " + labelSC + " be replaced by label : " + toTreatSCpoints[0] + "-"+toTreatSCpoints[1]);
		  if(toMergeLabel == -1)
			  continue;
		  
		  System.out.println("		Connected with big cell, current label " + labelSC + " will be replaced by label " + toMergeLabel);
		  for(int i=0; i<label.length; i++)
		  {
			  int seedLabel = label[i];			  
			  if(seedLabel == labelSC)
			  {
				 label[i] = toMergeLabel;
			  }
		  }		  
	  }	  
	  
	  ImagePlus imp = IJ.createImage(title, "8-bit black", width, height, 1);
	  ImageProcessor ip = imp.getProcessor();
	  
	  int thresholde = 0;
	  if(mergingIntensity)
		  thresholde = Auto_Threshold_v.Otsu(this.getHistogram());

	  for(int i=0; i<label.length;i++)
	  {
		  int seedLabel = label[i];
		  
		  if(seedLabel == BACKGROUND_MASK)
		  {
			  ip.set(i, 0);
			  continue;
		  }
		  
		  if(seedLabel == BACKGROUND_LABEL)
		  {
			  ip.set(i, 50);
			  continue;
		  }
		  
		  ip.set(i, 255);
		  
		  int x = i%width;
		  int y = i/width;
		  
		  for (int d=0; d<8; d++) 
		  {  
			  int x2 = x+DIR_X_OFFSET[d];
			  int y2 = y+DIR_Y_OFFSET[d];

			  if(!inside(x2, y2))
				  continue;
			  
			  int pos = x2 + y2*width;

			  if(label[pos] == seedLabel)
				  continue;
			  
			  if(label[pos]==BACKGROUND_LABEL || label[pos]==BACKGROUND_MASK)
				  continue;
			  
			  if(mergingIntensity && this.getf(i)<0.3*thresholde)
			  {
				  ip.set(i, 255);
				  System.out.println("	merge intensity " + this.getf(i) + " threshold : " + thresholde);
			  }
			  else {
				  ip.set(i, 100);
			  }
		  }
	  }
	  //imp.show();
	  //FileSaver fs = new FileSaver(imp);
	  //fs.saveAsTiff();
	  return imp;
  }


  /**
   * computeViscosity Function to compute viscosity of dilatation over the
   * image.
   *
   * @param a first parameter.
   * @param b second parameter.
   * @return 1.0 + exp( a*( ( gray level )-b ) ).
   */
  public float[] computeViscosity(double a, double b)
  {
    float[] res = new float[width * height];

    int i;
    byte[] greys255 = (byte[])this.convertToByte(false).getPixels();
    for (i = 0; i < width * height; i++)
    {
      //res[i] = ( float ) ( 1. - 1. / ( 1.0 + Math.exp( -a * ( maxTogrey255( this.getf( i ) ) - b ) ) ) );
      res[i] = (float) (1.0 + Math.exp(
                        a * ((greys255[i]&0xff) - b)));
      
      //res[i] = greys255[i]*greys255[i];
      //if(greys255[i] < 30)
    	  //System.out.println(res[i] + " max to grey " + greys255[i]);
    }

    return res;
  }
  
  public float[] maxTogrey()
  {
	  
	  int[] hist = getHistogram();
	  //int min = 0;
	  int max = 65535;
	  
	  int min = (int)(Auto_Threshold_v.Otsu(hist)*0.6);
	  
	  System.out.println(" otsu : " + min);
			  
	  /*for(int i=0; i<hist.length ;i++)
	  {
		  if(hist[i] > minHist )
		  {			  
			  min = i;
			  break;
		  }
	  }*/
	  
	  for(int i=hist.length-1; i>=0; i--)
	  {
		  if(hist[i]>0)
		  {			  
			  max = i;
			  break;
		  }
	  }
	  float scale = (float) (255.0f/(max-min));
	  //System.out.println("scale : " + scale + " max : " + max + " min : " + min);
	  float[] res = new float[width*height];
	  for(int i=0; i<width*height ; i++)
	  {		  
		  float value = (this.getf(i)-min)*scale;
		  if (value<0) value = 0;
		  if (value>255) value = 255;
			//System.out.println(" pixel value i " + i + " " + value);
		  res[i] = value + 0.5f;		
	  }
	  //FloatProcessor fp = new FloatProcessor(width, height, res);
	  //new ImagePlus("float", fp).show();
	  return res;
  }
  
  private ShortProcessor normalisation(ImageProcessor ip, double ratio)
  {	  
	  int[] hist = ip.getHistogram();
	  //int min = 0;
	  int max = 65535;
	  int min = (int)(Auto_Threshold_v.Otsu(hist)*ratio);
	  //System.out.println(" otsu : " + min);
	  
	  for(int i=hist.length-1; i>=0; i--)
	  {
		  if(hist[i] > 0)
		  {			  
			  max = i;
			  break;
		  }
	  }
	  float scale = (float) (65535.0f/(max-min));

	  short[] res = new short[width*height];
	  for(int i=0; i<width*height ; i++)
	  {		  
		  float value = (ip.getf(i)-min)*scale;
		  if (value<0) value = 0;
		  if (value>65535) value = 65535;
		  //System.out.println(" pixel value i " + i + " " + value);
		  res[i] = (short)(value + 0.5);
	  }
	  ShortProcessor sp = new ShortProcessor(width, height, res,null);
	  ImagePlus nImp = new ImagePlus("normalised float", sp);
	  IJ.save(nImp,"d:/normalised.tif");
	  return sp;
  }

  
  
  /**
   * Creates a new blob by dilating from a point.
   *
   * @param seed Point to dilate from
   * @param previewsblobs Blobs already computed.
   * @param iter_max nb of iteration of dilatation max_greyimum.
   * @param speedA first parameter for viscosity computation
   * @param speedB second parameter for viscosity computation
   * @param connected8 is blob 8 connected.
   * @return
   */
  public ShapeFbt BlobFromPoint(Point seed, ShapeSet previewsblobs,
                                int iter_max,
                                double speedA, double speedB,
                                boolean connected8)
  {

    // =================== Compute new blob label =============================
    int new_label = Collections.max(previewsblobs.shapeList.keySet()) + 1;

    // ================ convert previewsBlobs Shapeset to label list ==========
    int[] labelList = Utils.ShapeSetToLabels(previewsblobs, width, height,
                                             BACKGROUND_LABEL);

    // ====== If starting point is already in a blob or in background, ========
    //                       return null
    int seedIndex = pointToIndex(seed);
    if (background(seedIndex) == 1 || labelList[ seedIndex] != BACKGROUND_LABEL)
    {
      return null;
    }

    // ================= add new seed poit to the list ========================
    labelList[ pointToIndex(seed)] = new_label;

    // ================ Init totreat lists and add new seed point to them =====
    // ArrayList of Index of pixels to dilate
    ArrayList<Integer> toTreatInd = new ArrayList<Integer>();

    // toTreatDist ArrayList of Distance to seed of pixels to dilate.
    ArrayList<Float> toTreatDist = new ArrayList<Float>();

    toTreatInd.add(pointToIndex(seed));
    toTreatDist.add(0.0f);

    // ========== Init frozen boolean list ===================================
    // true : in background or already in blob
    // false : seed can dilate to this pixel
    int size = width * height;
    boolean[] frozen = new boolean[size];
    for (int i = 0; i < size; i++)
    {
      frozen[i] = (background(i) == 1) || (labelList[i] != BACKGROUND_LABEL);
    }

    // =================== Compute viscosity ==================================
    float[] viscosity = computeViscosity(speedA, speedB);

    for (int i = 0; i < iter_max; i++)
    {
      if (toTreatInd.isEmpty())
      {
        break;
      }

      Object[] res = dilateStepS(labelList, toTreatInd, toTreatDist, frozen,
                                viscosity, connected8);

      labelList = (int[]) res[0];
      toTreatInd = (ArrayList<Integer>) res[1];
      toTreatDist = (ArrayList<Float>) res[2];
      frozen = (boolean[]) res[3];

    }

    ShapeFbt blob = new ShapeFbt();
    for (int i = 0; i < size; i++)
    {
      if (labelList[i] == new_label)
      {
        blob.add(indexToPoint(i));
      }
    }

    blob.setCenterAndRadius();
    blob.updateBoundary();

    return blob;

  }

  /**
   * Taking a blobs ShapeSet, divide one of the blob in it by dilating two
   * starting pixels in this blob.
   *
   * @param startPtIndex1 First starting pixel index
   * @param startPtIndex2 Second starting pixel index
   * @param previewsBlobs ShapeSet of blobs before division
   * @param label Label of the blob to divide
   * @param speedA First viscosity parameter
   * @param speedB Second viscosity parameter
   * @param connected8 are ShapeFbt 8 connected.
   *
   * @return new ShapeSet with divided blob.
   */
  public ShapeSet divideBlob(int startPtIndex1, int startPtIndex2,
                             ShapeSet previewsBlobs,
                             int label, double speedA, double speedB,
                             boolean connected8)
  {
    // =========== init result shapeSet =======================================
    ShapeSet resultBlobs = previewsBlobs.duplicate();

    // =========== Get blob to divide =========================================
    // --------------- get blob -----------------------------------------------
    ShapeFbt blob = resultBlobs.getShape(label);

    // -------------- if blob wasan't found, return null ----------------------
    if (blob == null)
    {
      System.out.println(
          "Error in ImageFbt.divideBlob : blob doesn't exists in Blobs ShapeSet");
      return null;
    }

    // ------------- Update center, radius and boundary of the blob -----------
    blob.setCenterAndRadius();
    blob.updateBoundary();

    // ------------- Remove it from result shapeSet ---------------------------
    resultBlobs.removeShape(label);

    // ============ If starting points are not in blob, return null ===========
    if (!(blob.pixels.contains(indexToPoint(startPtIndex1))
          && blob.pixels.contains(indexToPoint(startPtIndex2))))
    {
      return null;
    }

    // ================== Find the two points in blob border ==================
    int nbPix = width * height;

    // ------------------ Compute the labels of the two futur blobs -----------
    int[] labelList = resultBlobs.toLabels(width, height);
    int newLabel = Collections.max(resultBlobs.getList().keySet()) + 1;
    labelList[ startPtIndex1] = newLabel;
    labelList[ startPtIndex2] = newLabel + 1;

    // -------------- Init toTreat lists with starting points -----------------
    ArrayList<Integer> toTreatInd = new ArrayList<Integer>();
    ArrayList<Float> toTreatDist = new ArrayList<Float>();
    toTreatInd.add(startPtIndex1);
    toTreatDist.add(0.0f);
    toTreatInd.add(startPtIndex2);
    toTreatDist.add(0.0f);

    // ------------ init frozen list ------------------------------------------
    // All pixels are frozen except the ones in the blob to divide. 
    boolean[] frozen = new boolean[nbPix];

    // === init all pixel as frozen ===
    for (int i = 0; i < nbPix; i++)
    {
      frozen[i] = true;
    }

    // === unfroze the ones in the blob to divide ===
    for (Point currPt : blob.getPixels())
    {
      int currInd = pointToIndex(currPt);
      frozen[currInd] = false;
    }

    // === froze starting points ===
    frozen[startPtIndex1] = true;
    frozen[startPtIndex2] = true;

    // ===================== Compute viscosity ===============================
    float[] viscosity = computeViscosity(speedA, speedB);

    // ================= Dilate the two new blobs ============================
    // untill they fill the whole old blob.
    while (!toTreatInd.isEmpty())
    {
      Object[] stepResult = dilateStepS(labelList, toTreatInd, toTreatDist,
                                       frozen, viscosity, connected8);
      labelList = (int[]) stepResult[0];
      toTreatInd = (ArrayList<Integer>) stepResult[1];
      toTreatDist = (ArrayList<Float>) stepResult[2];
      frozen = (boolean[]) stepResult[3];
    }

    // ============== get the two new blobs from the labels list ==============
    ShapeFbt divBlob1 = new ShapeFbt();
    ShapeFbt divBlob2 = new ShapeFbt();

    for (int i = 0; i < nbPix; i++)
    {
      if (labelList[i] == newLabel)
      {
        divBlob1.add(indexToPoint(i));
      }
      else if (labelList[i] == newLabel + 1)
      {
        divBlob2.add(indexToPoint(i));
      }
    }

    // ================== Update their centers, radii and boundary ============
    divBlob1.setCenterAndRadius();
    divBlob2.setCenterAndRadius();
    divBlob1.updateBoundary();
    divBlob2.updateBoundary();

    // =============== Add them to result blobs ShapeSet ======================
    resultBlobs.addShape(divBlob1);
    resultBlobs.addShape(divBlob2);

    // ======== Return result blobs ShapeSet ==================================
    return resultBlobs;
  }

  /**
   * Taking a blobs ShapeSet, divide one of the blob in it by dilating two
   * starting pixels in this blob.
   *
   * Starting pixels are found by computing points in blob border that are the
   * most distant from one another and find their nearest local minimum. If
   * these points aren't found, it returns null.
   *
   * @param previewsBlobs ShapeSet of blobs before division
   * @param label Label of the blob to divide
   * @param speedA First viscosity parameter
   * @param speedB Second viscosity parameter
   * @param connected8 are ShapeFbt 8 connected.
   * @return new ShapeSet with divided blob.
   */
  public ShapeSet autoDivideBlob(ShapeSet previewsBlobs, int label,
                                 double speedA, double speedB,
                                 boolean connected8)
  {
    // =========== Get blob to divide =========================================
    // --------------- get blob -----------------------------------------------
    ShapeFbt blob = previewsBlobs.getShape(label);

    // -------------- if blob wasan't found, return null ----------------------
    if (blob == null)
    {
      System.out.println(
          "Error in ImageFbt.autoDivideBlob : blob doesn't exists in Blobs ShapeSet");
      return null;
    }

    // ------------- Update center, radius and boundary of the blob -----------
    blob.setCenterAndRadius();
    blob.updateBoundary();

    // ================== Find the two points in blob border ==================
    //               That are the most distant from one each other.
    // ----------- init variables --------------------------------------------
    HashSet<Point> border = blob.getBoundary();
    HashSet<Point> done = new HashSet<Point>();
    double maxDist = 0.;
    Point vertex1 = null;
    Point vertex2 = null;

    // ---------------- For each couple of point in blob border --------------
    // 1 ) Compute distance between points
    // 2 ) register max_grey distance and points if needed 
    for (Point point1 : border)
    {
      // add point1 to list of done points.
      done.add(point1);

      for (Point point2 : border)
      {
        // if point2 was already computed, do nothing.
        if (done.contains(point2))
        {
          continue;
        }

        // === 1 ) Compute distance between points ===
        double dist = point1.distance(point2);

        // 2 ) === register max_grey distance and points if needed ===
        if (dist > maxDist)
        {
          maxDist = dist;
          vertex1 = point1;
          vertex2 = point2;
        }
      }
    }

    // ========= If most distant points weren't found, return null ============
    if (vertex1 == null || vertex2 == null)
    {
      return null;
    }

    // ========= find nearest minimum local from points =======================
    Point localMin1 = slice(blob, vertex1, 1000);
    Point localMin2 = slice(blob, vertex2, 1000);

    // ===================== Check that local minimums were found =============
    // If not, return null.
    int localMin1Ind;
    if (blob.pixels.contains(localMin1))
    {
      localMin1Ind = pointToIndex(localMin1);
    }
    else
    {
      System.out.println(
          "ImageFbt.autoDivideBlob : couldn't find local minimum.");
      return null;
    }

    int localMin2Ind;
    if (blob.pixels.contains(localMin1))
    {
      localMin2Ind = pointToIndex(localMin2);
    }
    else
    {
      System.out.println(
          "ImageFbt.autoDivideBlob : couldn't find local minimum.");
      return null;
    }

    // ======== return divide blob starting from local minimums computed. =====
    return divideBlob(localMin1Ind, localMin2Ind, previewsBlobs, label, speedA,
                      speedB, connected8);
  }

  // -------------------------------------------------------------------------
  //                GTV Means coded by Lionel Moisan
  // -------------------------------------------------------------------------
  //---------- one iteration for TV denoising ----------
  // returns an upper bound of ||u^n-\bar{u}||^2 
  public double energy_evol_max(double u[], int ofs, double px[], double py[],
                                int nx, int ny, double lambda, double step)
      throws InterruptedException
  {
    double d, E, gx, gy, norm;
    int x, y, adr;

    // compute u from (px,py)
    for (adr = y = 0; y < ny; y++)
    {
      for (x = 0; x < nx; x++, adr++)
      {
        d = -px[adr] - py[adr];

        if (x > 0)
        {
          d += px[adr - 1];
        }

        if (y > 0)
        {
          d += py[adr - nx];
        }

        u[ofs + adr] = (double) u[adr] + d;
      }
    }

    // update (px,py) from u
    E = 0.D;
    for (adr = y = 0; y < ny; y++)
    {
      // Check if thread is interrupted and raise exception if necessary
      Utils.checkThreadInterruption();

      for (x = 0; x < nx; x++, adr++)
      {
        gx = ((x < nx - 1) ? (u[ofs + adr + 1] - u[ofs + adr]) : 0.D);
        gy = ((y < ny - 1) ? (u[ofs + adr + nx] - u[ofs + adr]) : 0.D);
        E += px[adr] * gx + py[adr] * gy + 0.5 * lambda * Math.sqrt(
            gx * gx + gy * gy);
        gx = px[adr] - step * gx;
        gy = py[adr] - step * gy;
        norm = Math.sqrt(gx * gx + gy * gy);
        px[adr] = gx / Math.max(1., norm * 2. / lambda);
        py[adr] = gy / Math.max(1., norm * 2. / lambda);
      }
    }
    return (E);
  }

  public void gtv_means_denoise(Semaphore sem, double l, double r, int n,
		  int patchSize, int d, double eps, int g) throws
		  InterruptedException
		  {
	  int xx, yy, i, scale, adr, adrs;
	  int adrp, xp, yp, xp1, xp2, yp1, yp2;
	  double sum, threshold, dist, f, E;
	  boolean finished;

	  //    // default parameters
	  //    double l = 20.D;
	  //    double r = 0.1D;
	  //    int n = 5;
	  //    int patchSize = 25;
	  //    int d = 10;
	  //    double eps = 0.01D;
	  //    int g = 1;
	  double dlambda = 1.;
	  int nlambda = 10;

	  // Check that patchSize < width and height
	  if (patchSize > width || patchSize > height)
	  {
		  System.out.println(
				  "Error in ImageFbt.gtv_mean_denoise : patch size is bigger than image.");
		  return;
	  }

	  int patchRadius = (patchSize - 1) / 2;
	  int patchRadiusSquared = patchRadius * patchRadius;

	  int patchNbPixels = 0;
	  int x, y;
	  for (x = -patchRadius; x <= patchRadius; x++)
	  {
		  for (y = -patchRadius; y <= patchRadius; y++)
		  {
			  if (x * x + y * y < patchRadiusSquared)
			  {
				  patchNbPixels++;
			  }
		  }
	  }

	  /*
	   * We will have to enlarge the image to deal properly with the borders,
	   * For now, we will just compute the size of this enlarged image
	   */
	  int enlargedImgWidth = width + 2 * patchRadius;
	  int enlargedImgHeight = height + 2 * patchRadius;
	  int enlargedImgNbPixels = enlargedImgWidth * enlargedImgHeight;

	  /*
	   * This portion of the code (from here to the end of the method) is critical
	   * regarding memory consumption, it requires approx.
	   * 3 * patchNbPixels + (3 * nlambda + 8) * enlargedImgNbPixels * 4 bytes
	   * Since patchNbPixels < enlargedImgNbPixels, an upper bound of the required
	   * memory is (3 * nlambda + 11) * enlargedImgNbPixels * 4 bytes
	   *
	   * Because of the undeterministic nature of multithreading, we will double
	   * this requirement and check for its availability before proceeding.
	   */
	  int requiredMemory = 2 * (3 * nlambda + 11) * enlargedImgNbPixels * 4;
	  boolean memOK = false;

	  double[] enlargedImg = null;
	  int[] dadr = null;
	  double[] px = null;
	  double[] py = null;
	  float[] cumulatedPatchValues = null;
	  double[] pnew = null;
	  double[] dout = null;
	  double[] doutw = null;

	  while (!memOK)
	  {
		  sem.acquire();
		  Runtime.getRuntime().gc();
		  if (requiredMemory < Runtime.getRuntime().freeMemory() + Runtime.
				  getRuntime().maxMemory() - Runtime.getRuntime().totalMemory())
		  {
			  enlargedImg = new double[enlargedImgNbPixels * nlambda];
			  dadr = new int[patchNbPixels];
			  px = new double[enlargedImgNbPixels];
			  py = new double[enlargedImgNbPixels];
			  cumulatedPatchValues = new float[enlargedImgNbPixels * nlambda];
			  pnew = new double[patchNbPixels];
			  dout = new double[enlargedImgNbPixels];
			  doutw = new double[enlargedImgNbPixels];
			  memOK = true;
		  }
		  sem.release();
		  sleep(1000);
	  }

	  /*
	   * Create the enlarged image
	   */
	  for (y = 0; y < enlargedImgHeight; y++)
	  {
		  yy = y - patchRadius;
		  if (yy < 0)
		  {
			  yy = -yy;
		  }
		  if (yy >= height)
		  {
			  yy = height * 2 - 2 - yy;
		  }
		  for (x = 0; x < enlargedImgWidth; x++)
		  {
			  xx = x - patchRadius;
			  if (xx < 0)
			  {
				  xx = -xx;
			  }
			  if (xx >= width)
			  {
				  xx = width * 2 - 2 - xx;
			  }

			  enlargedImg[y * enlargedImgWidth + x] = (double) getf(xx, yy); // scale 0
		  }
	  }

	  /*
	   * build patch adressing (dadr array)
	   */
	  i = 0;
	  for (y = -patchRadius; y <= patchRadius; y++)
	  {
		  for (x = -patchRadius; x <= patchRadius; x++)
		  {
			  if (x * x + y * y < patchRadius * patchRadius)
			  {
				  dadr[i++] = y * enlargedImgWidth + x;
			  }
		  }
	  }

	  /*
	   * pre-preprocessing TV
	   */
	  // IJ.log( "Pre-processing: TV denoising\n" );
	  for (scale = 1; scale < nlambda; scale++)
	  {
		  // IJ.log( "scale " + ( scale + 1 ) + "/" + nlambda );
		  // Total Variation Denoising
		  do
		  {
			  E = energy_evol_max(enlargedImg, scale * enlargedImgNbPixels, px, py,
					  enlargedImgWidth, enlargedImgHeight,
					  dlambda * (double) scale, 0.249);
		  }
		  while (E >= eps * (double) (enlargedImgNbPixels));
	  }

	  /*
	   * compute cumulated patch values (for acceleration)
	   */
	  for (scale = 0; scale < nlambda; scale++)
	  {
		  for (y = patchRadius; y < enlargedImgHeight - patchRadius; y++)
		  {
			  for (x = patchRadius; x < enlargedImgWidth - patchRadius; x++)
			  {
				  adr = scale * enlargedImgNbPixels + y * enlargedImgWidth + x;
				  f = 0.;
				  for (i = 0; i < patchNbPixels; i++)
				  {
					  f += enlargedImg[adr + dadr[i]];
				  }
				  cumulatedPatchValues[adr] = (float) f;
			  }
		  }
	  }
	  threshold = (double) patchNbPixels * 2. * l * l * (1. + 1.88 * Math.sqrt(
			  2.
			  / (double) patchNbPixels));

	  /*
	   * patch denoising
	   */
	  //    IJ.log( "Patch denoising\n" );
	  sum = 0.D; // Useless but avoids "uninitialized value" warnings
	  for (x = patchRadius; x < enlargedImgWidth - patchRadius; x++)
	  {
		  // Check if thread is interrupted and raise exception if necessary
		  Utils.checkThreadInterruption();

		  for (y = patchRadius; y < enlargedImgHeight - patchRadius; y++)
		  {
			  // COMPUTE ONLY IF NOT IN BACKGROUND
			  if (background == null 
					  || background(coordToIndex(x - patchRadius, y - patchRadius)) == 0)
			  {
				  adr = y * enlargedImgWidth + x;

				  /*
				   * loop on scales
				   */
				  finished = false;
				  for (scale = 0; !finished && scale < nlambda; scale++)
				  {

					  adrs = enlargedImgNbPixels * scale;
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  pnew[i] = 0.;
					  }
					  sum = 0.D;

					  /*
					   * compute patch distances
					   */
					  xp1 = Math.max(x - d, patchRadius);
					  xp2 = Math.min(x + d, enlargedImgWidth - 1 - patchRadius);
					  yp1 = Math.max(y - d, patchRadius);
					  yp2 = Math.min(y + d, enlargedImgHeight - 1 - patchRadius);
					  for (xp = xp1; xp <= xp2; xp++)
					  {
						  for (yp = yp1; yp <= yp2; yp++)
						  {
							  adrp = yp * enlargedImgWidth + xp;
							  f = (double) (cumulatedPatchValues[adrs + adrp]
									  - cumulatedPatchValues[adrs + adr]);
							  if (f * f < threshold * (double) patchNbPixels)
							  {
								  dist = 0.;
								  for (i = 0; i < patchNbPixels && dist < threshold; i++)
								  {
									  f = enlargedImg[adrs + adrp + dadr[i]] - enlargedImg[adrs
									                                                       + adr
									                                                       + dadr[i]];
									  dist += f * f;
								  }
								  if (dist < threshold)
								  {
									  for (i = 0; i < patchNbPixels; i++)
									  {
										  pnew[i] += enlargedImg[adrs + adrp + dadr[i]];
									  }
									  sum += 1.;
								  }
							  }
						  }
					  }
					  finished = ((sum >= (double) (n) * (1. - (double) scale * dlambda
							  * r)) || (scale == nlambda
							  - 1));
				  }
				  /*
				   * update
				   */
				  switch (g)
				  {
				  case 0: /*
				   * no aggregation
				   */
					  dout[adr] += pnew[patchNbPixels / 2] / sum;
					  doutw[adr] += 1.;
					  break;
				  case 1: /*
				   * original aggregation (SIIMS paper)
				   */
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  dout[adr + dadr[i]] += pnew[i] / sum;
						  doutw[adr + dadr[i]] += 1.;
					  }
					  break;
				  case 2: /*
				   * reduce artifacts but decrease PSNR
				   */
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  dout[adr + dadr[i]] += pnew[i];
						  doutw[adr + dadr[i]] += sum;
					  }
					  break;
				  }
			  }
		  }
	  }

	  /*
	   * build output
	   */
	  setMinAndMax(getMin(), getMax());
	  for (y = 0; y < height; y++)
	  {
		  for (x = 0; x < width; x++)
		  {
			  adr = (y + patchRadius) * enlargedImgWidth + x + patchRadius;
			  setf(x, y, (float) (doutw[adr] == 0. ? 0. : dout[adr] / doutw[adr]));
		  }
	  }
		  }

  /**
   * a default parameter gtv denoise
   * @param sem
   * @throws InterruptedException
   */
  public void gtv_means_denoise(Semaphore sem) throws
		  InterruptedException
		  {
	  int xx, yy, i, scale, adr, adrs;
	  int adrp, xp, yp, xp1, xp2, yp1, yp2;
	  double sum, threshold, dist, f, E;
	  boolean finished;

	  //    // default parameters
	      double l = 20.D;
	      double r = 0.1D;
	      int n = 5;
	      int patchSize = 25;
	      int d = 10;
	      double eps = 0.01D;
	      int g = 0;
	  double dlambda = 1.;
	  int nlambda = 10;

	  // Check that patchSize < width and height
	  if (patchSize > width || patchSize > height)
	  {
		  System.out.println(
				  "Error in ImageFbt.gtv_mean_denoise : patch size is bigger than image.");
		  return;
	  }

	  int patchRadius = (patchSize - 1) / 2;
	  int patchRadiusSquared = patchRadius * patchRadius;

	  int patchNbPixels = 0;
	  int x, y;
	  for (x = -patchRadius; x <= patchRadius; x++)
	  {
		  for (y = -patchRadius; y <= patchRadius; y++)
		  {
			  if (x * x + y * y < patchRadiusSquared)
			  {
				  patchNbPixels++;
			  }
		  }
	  }

	  /*
	   * We will have to enlarge the image to deal properly with the borders,
	   * For now, we will just compute the size of this enlarged image
	   */
	  int enlargedImgWidth = width + 2 * patchRadius;
	  int enlargedImgHeight = height + 2 * patchRadius;
	  int enlargedImgNbPixels = enlargedImgWidth * enlargedImgHeight;

	  /*
	   * This portion of the code (from here to the end of the method) is critical
	   * regarding memory consumption, it requires approx.
	   * 3 * patchNbPixels + (3 * nlambda + 8) * enlargedImgNbPixels * 4 bytes
	   * Since patchNbPixels < enlargedImgNbPixels, an upper bound of the required
	   * memory is (3 * nlambda + 11) * enlargedImgNbPixels * 4 bytes
	   *
	   * Because of the undeterministic nature of multithreading, we will double
	   * this requirement and check for its availability before proceeding.
	   */
	  int requiredMemory = 2 * (3 * nlambda + 11) * enlargedImgNbPixels * 4;
	  boolean memOK = false;

	  double[] enlargedImg = null;
	  int[] dadr = null;
	  double[] px = null;
	  double[] py = null;
	  float[] cumulatedPatchValues = null;
	  double[] pnew = null;
	  double[] dout = null;
	  double[] doutw = null;

	  while (!memOK)
	  {
		  sem.acquire();
		  Runtime.getRuntime().gc();
		  if (requiredMemory < Runtime.getRuntime().freeMemory() + Runtime.
				  getRuntime().maxMemory() - Runtime.getRuntime().totalMemory())
		  {
			  enlargedImg = new double[enlargedImgNbPixels * nlambda];
			  dadr = new int[patchNbPixels];
			  px = new double[enlargedImgNbPixels];
			  py = new double[enlargedImgNbPixels];
			  cumulatedPatchValues = new float[enlargedImgNbPixels * nlambda];
			  pnew = new double[patchNbPixels];
			  dout = new double[enlargedImgNbPixels];
			  doutw = new double[enlargedImgNbPixels];
			  memOK = true;
		  }
		  sem.release();
		  sleep(1000);
	  }

	  /*
	   * Create the enlarged image
	   */
	  for (y = 0; y < enlargedImgHeight; y++)
	  {
		  yy = y - patchRadius;
		  if (yy < 0)
		  {
			  yy = -yy;
		  }
		  if (yy >= height)
		  {
			  yy = height * 2 - 2 - yy;
		  }
		  for (x = 0; x < enlargedImgWidth; x++)
		  {
			  xx = x - patchRadius;
			  if (xx < 0)
			  {
				  xx = -xx;
			  }
			  if (xx >= width)
			  {
				  xx = width * 2 - 2 - xx;
			  }

			  enlargedImg[y * enlargedImgWidth + x] = (double) getf(xx, yy); // scale 0
		  }
	  }

	  /*
	   * build patch adressing (dadr array)
	   */
	  i = 0;
	  for (y = -patchRadius; y <= patchRadius; y++)
	  {
		  for (x = -patchRadius; x <= patchRadius; x++)
		  {
			  if (x * x + y * y < patchRadius * patchRadius)
			  {
				  dadr[i++] = y * enlargedImgWidth + x;
			  }
		  }
	  }

	  /*
	   * pre-preprocessing TV
	   */
	  // IJ.log( "Pre-processing: TV denoising\n" );
	  for (scale = 1; scale < nlambda; scale++)
	  {
		  // IJ.log( "scale " + ( scale + 1 ) + "/" + nlambda );
		  // Total Variation Denoising
		  do
		  {
			  E = energy_evol_max(enlargedImg, scale * enlargedImgNbPixels, px, py,
					  enlargedImgWidth, enlargedImgHeight,
					  dlambda * (double) scale, 0.249);
		  }
		  while (E >= eps * (double) (enlargedImgNbPixels));
	  }

	  /*
	   * compute cumulated patch values (for acceleration)
	   */
	  for (scale = 0; scale < nlambda; scale++)
	  {
		  for (y = patchRadius; y < enlargedImgHeight - patchRadius; y++)
		  {
			  for (x = patchRadius; x < enlargedImgWidth - patchRadius; x++)
			  {
				  adr = scale * enlargedImgNbPixels + y * enlargedImgWidth + x;
				  f = 0.;
				  for (i = 0; i < patchNbPixels; i++)
				  {
					  f += enlargedImg[adr + dadr[i]];
				  }
				  cumulatedPatchValues[adr] = (float) f;
			  }
		  }
	  }
	  threshold = (double) patchNbPixels * 2. * l * l * (1. + 1.88 * Math.sqrt(
			  2.
			  / (double) patchNbPixels));

	  /*
	   * patch denoising
	   */
	   //IJ.log( "Patch denoising\n" );
	  sum = 0.D; // Useless but avoids "uninitialized value" warnings
	  for (x = patchRadius; x < enlargedImgWidth - patchRadius; x++)
	  {
		  // Check if thread is interrupted and raise exception if necessary
		  Utils.checkThreadInterruption();

		  for (y = patchRadius; y < enlargedImgHeight - patchRadius; y++)
		  {
			  // COMPUTE ONLY IF NOT IN BACKGROUND
			  if (background == null 
					  || background(coordToIndex(x - patchRadius, y - patchRadius)) == 0)
			  {
				  adr = y * enlargedImgWidth + x;

				  /*
				   * loop on scales
				   */
				  finished = false;
				  for (scale = 0; !finished && scale < nlambda; scale++)
				  {

					  adrs = enlargedImgNbPixels * scale;
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  pnew[i] = 0.;
					  }
					  sum = 0.D;

					  /*
					   * compute patch distances
					   */
					  xp1 = Math.max(x - d, patchRadius);
					  xp2 = Math.min(x + d, enlargedImgWidth - 1 - patchRadius);
					  yp1 = Math.max(y - d, patchRadius);
					  yp2 = Math.min(y + d, enlargedImgHeight - 1 - patchRadius);
					  for (xp = xp1; xp <= xp2; xp++)
					  {
						  for (yp = yp1; yp <= yp2; yp++)
						  {
							  adrp = yp * enlargedImgWidth + xp;
							  f = (double) (cumulatedPatchValues[adrs + adrp]
									  - cumulatedPatchValues[adrs + adr]);
							  if (f * f < threshold * (double) patchNbPixels)
							  {
								  dist = 0.;
								  for (i = 0; i < patchNbPixels && dist < threshold; i++)
								  {
									  f = enlargedImg[adrs + adrp + dadr[i]] - enlargedImg[adrs
									                                                       + adr
									                                                       + dadr[i]];
									  dist += f * f;
								  }
								  if (dist < threshold)
								  {
									  for (i = 0; i < patchNbPixels; i++)
									  {
										  pnew[i] += enlargedImg[adrs + adrp + dadr[i]];
									  }
									  sum += 1.;
								  }
							  }
						  }
					  }
					  finished = ((sum >= (double) (n) * (1. - (double) scale * dlambda
							  * r)) || (scale == nlambda
							  - 1));
				  }
				  /*
				   * update
				   */
				  switch (g)
				  {
				  case 0: /*
				   * no aggregation
				   */
					  dout[adr] += pnew[patchNbPixels / 2] / sum;
					  doutw[adr] += 1.;
					  break;
				  case 1: /*
				   * original aggregation (SIIMS paper)
				   */
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  dout[adr + dadr[i]] += pnew[i] / sum;
						  doutw[adr + dadr[i]] += 1.;
					  }
					  break;
				  case 2: /*
				   * reduce artifacts but decrease PSNR
				   */
					  for (i = 0; i < patchNbPixels; i++)
					  {
						  dout[adr + dadr[i]] += pnew[i];
						  doutw[adr + dadr[i]] += sum;
					  }
					  break;
				  }
			  }
		  }
	  }

	  /*
	   * build output
	   */
	  setMinAndMax(getMin(), getMax());
	  for (y = 0; y < height; y++)
	  {
		  for (x = 0; x < width; x++)
		  {
			  adr = (y + patchRadius) * enlargedImgWidth + x + patchRadius;
			  setf(x, y, (float) (doutw[adr] == 0. ? 0. : dout[adr] / doutw[adr]));
		  }
	  }
		  }
  
  
  // =========================================================================
  //                            GETTERS
  // =========================================================================
  /**
   * Background getter.
   *
   * @return background.
   */
  public int[] getBackground()
  {
    return background;
  }

  /**
   * Gives background value for pixel of index 'index'. If background is null,
   * returns 0.
   *
   * @param index pixel index.
   * @return
   */
  public int background(int index)
  {
    try
    {
      return background[index];
    }
    catch (NullPointerException e)
    {
      return 0;
    }
  }

  // =========================================================================
  //                            SETTERS
  // =========================================================================
  /**
   * Background setter.
   *
   * @param _back New background.
   */
  public void setBackground(int[] _back)
  {
    background = _back;
  }
  
  
  // test colony segmentation for antoine
  public static void main(String[] args)
  {
	  run();
  }
  
  public static void run()
  {
	  GenericDialog genericDialog = new GenericDialog("Macros");
	  genericDialog.addCheckbox("Segmentation based on mask-LM or seeds", false);
	  genericDialog.addCheckbox("Generate foci-LM by selecting two folder images", false);			
	  genericDialog.addCheckbox("Triple images to stack, rfp-gfp-trans", false);
	  genericDialog.addCheckbox("Remove unwanted pointRoi by adding oval or rectangular", false);
	  genericDialog.addCheckbox("Generate result table", false);
	  genericDialog.showDialog();
	  if(genericDialog.wasCanceled())
		  return;
	  boolean seg = genericDialog.getNextBoolean();
	  boolean fociLM = genericDialog.getNextBoolean();			
	  boolean toStack = genericDialog.getNextBoolean();
	  boolean refineRoi = genericDialog.getNextBoolean();
	  boolean resultT = genericDialog.getNextBoolean();
	  
	  if(seg)
	  {
		  IJ.showMessage("Select the folder which contains 'orig', 'LM', 'mask', 'A'"
		  		+ "\r\n"+ "Then select if segmentation from seeds or not (for the first time)");
		  maskLMbasedSegmentation();
	  }else if (fociLM) {
		  lmBasedFociFinder();
	  }else if (toStack) {
		  tripleFolderToStack();
	  }else if (refineRoi) {
		  refineRoi();
	  }else if (resultT) {
		  resultTable();
	  }
  }
  
  public static void resultTable()
  {
	  ImagePlus imp = WindowManager.getCurrentImage();
	  if(imp == null)
		  imp = IJ.openImage();
	  if(imp == null)
		  return;
	  Roi[] rois = getRoiFromManager();
	  ImageStack ims = imp.getImageStack();
	  
	  int mOption =  Measurements.AREA|Measurements.MEAN|Measurements.CENTROID|Measurements.MEDIAN|Measurements.MIN_MAX;
	  Ins_editor insEditor = new Ins_editor();
	  
	  insEditor.append("cell_label"+"\t"+
			  "pos_x"+"\t"+
			  "pos_y"+"\t"+
			  "min(rfp)"+"\t"+
			  "max(rfp)"+"\t"+
			  "median(rfp)"+"\t"+
			  "mean(rfp)"+"\t"+
			  "min(yfp)"+"\t"+
			  "max(yfp)"+"\t"+
			  "median(yfp)"+"\t"+
			  "mean(yfp)"+"\t"+
			  "area"+"\t"+
			  "foci"+"\t"+
			  "area(foci)"+"\t"+
			  "mean(foci)"+"\t"+
			  "max(foci)"+
			  "\r\n");

	  
	  for(int slice = 1; slice<=imp.getStackSize(); slice = slice + 3)
	  {
		  Roi[] roiCell = constructRoisSlice(rois, slice+2);
		  if (roiCell == null) {
			  continue;			
		  }
		  Roi[] roiFoci = constructRoisSlice(rois,slice+1);
		  ImageProcessor rfp = ims.getProcessor(slice);
		  ImageProcessor yfp = ims.getProcessor(slice+1);		  
		  for(int i=0;i<roiCell.length;i++)
		  {			  
			  rfp.setRoi(roiCell[i]);
			  yfp.setRoi(roiCell[i]);
			  ImageStatistics statsCell = ImageStatistics.getStatistics(rfp, mOption, null);				  
			  ImageStatistics statsCellgfp = ImageStatistics.getStatistics(yfp, mOption, null);
			  String res = roiCell[i].getName()+"\t"+statsCell.xCentroid+"\t"+statsCell.yCentroid+"\t"+statsCell.min+"\t"+statsCell.max+"\t"+statsCell.median+"\t"+statsCell.mean+"\t"+statsCellgfp.min+"\t"+statsCellgfp.max+"\t"+statsCellgfp.median+"\t"+statsCellgfp.mean+"\t" +statsCell.area+"\t";			  
			  Rectangle r = roiCell[i].getBounds();
			  roiCell[i].setPosition(slice+1);
			  int foci = 0;
			  boolean found = false;
			  if(roiFoci == null)
				  insEditor.append(res + "\r\n");			  

			  if(roiFoci == null)
				  continue;
			  for(int j=0;j<roiFoci.length;j++)
			  {				  				  
				  if(r.contains(roiFoci[j].getBounds().x+roiFoci[j].getBounds().width*0.5, roiFoci[j].getBounds().y+roiFoci[j].getBounds().height*0.5))
				  {
					  foci = foci + 1;
					  yfp.setRoi(roiFoci[j]);
					  ImageStatistics statsFociyfp = ImageStatistics.getStatistics(yfp, mOption, null);
					  String res1 = res + String.valueOf(foci) + "\t" + statsFociyfp.area +"\t"+ statsFociyfp.mean + "\t"+statsFociyfp.max + "\r\n";
					  insEditor.append(res1);
					  found = true;
					  roiFoci[j].setPosition(slice + 2);
				  }
			  }
			  if(!found)
				  insEditor.append(res + "\r\n");			
		  }
	  }
	  insEditor.save();
	  
  }
  
  public static File[] openFolderDialog(int interval) {
		String pathDirectory = Prefs.getDefaultDirectory();;
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	System.out.println("getCurrentDirectory(): " 
	    			+  chooser.getCurrentDirectory());
	    	System.out.println("getSelectedFile() : " 
	    			+  chooser.getSelectedFile());
	    	
		    File file = chooser.getSelectedFile();
		    
		    FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if((name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff")))
						return true;
					else {
						return false;
					}					
				}
			};
			
			File[] files = null;
			String pathSubFolder = file.getPath();
			File subFolder = new File(pathSubFolder);
			files = subFolder.listFiles(filter);
		    	if(files!=null)
		    	Arrays.sort(files, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('s') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });

			    for(File f : files) {
			    	System.out.println("Selected file " + f.getPath());
			    }
			    
		    return files;
	    }else {
	    	System.out.println("No Selection ");
	    	return null;
	    }
	}
  
  
  public static Roi[] getRoiFromManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");										
		if (frame==null || !(frame instanceof RoiManager))
		{return null;}
		RoiManager roiManager = (RoiManager)frame;		
		Roi[] roisArrayRois = roiManager.getRoisAsArray();
		return roisArrayRois;
	}
  
	public static RoiManager getRoiManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)						
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");
		if (frame==null || !(frame instanceof RoiManager))
			{return null;}
		RoiManager roiManager = (RoiManager)frame;
		roiManager.setVisible(true);	
		return roiManager;
	}
	
	public static void deleteRoiToManager()
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
		{
			IJ.run("ROI Manager...");
		}
		frame = WindowManager.getFrame("ROI Manager");		
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		RoiManager roiManager = (RoiManager)frame;		
		roiManager.runCommand("delete");
		
	}
	public static void addRoiToManager(Roi roi)
	{
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
		{
			IJ.run("ROI Manager...");
		}
		frame = WindowManager.getFrame("ROI Manager");		
		if (frame==null || !(frame instanceof RoiManager))
		{return;}
		RoiManager roiManager = (RoiManager)frame;	
		if (roi==null) return;
		roiManager.addRoi(roi);
	}
	
	
	public static void tripleFolderToStack()
	{
		  File[] f1 = openFolderDialog(1);
		  File[] f2 = openFolderDialog(1);
		  if(f1.length != f2.length)
		  {
			  IJ.showMessage("files' number are not identical");
			  return;
		  }
		  ImagePlus im = IJ.openImage(f1[0].getPath());
		  ImagePlus imp = IJ.createImage("Triple-RFP-YFP-TRANS","16-bit black", im.getWidth(), im.getHeight(), f1.length*3);
		  ImageStack ims = imp.getImageStack();
		  for(int i=0, s=1;i<f1.length;i++,s=s+3)
		  {
			  ims.getProcessor(s).insert(IJ.openImage(f1[i].getPath()).getProcessor(), 0, 0);
			  ims.setSliceLabel("RFP-"+ String.valueOf(i+1), s);
			  ims.getProcessor(s+1).insert(IJ.openImage(f2[i].getPath()).getProcessor(), 0, 0);
			  ims.setSliceLabel("YFP-"+ String.valueOf(i+1), s+1);
			  ims.getProcessor(s+2).insert(IJ.openImage(f1[i].getPath()).getProcessor(), 0, 0);
			  ims.setSliceLabel("TRANS-"+ String.valueOf(i+1), s+2);
		  }
		  imp.show();
	}
	
	public static Roi[] constructRoisSliceAll(Roi[] roiArray, int slice)
	{
		int count = roiArray.length;
		Roi[] currentRois = new Roi[count];		
		int j=0;
		for (int i=0; i<count; i++) {	
			Roi cellRoi = roiArray[i];
			if(cellRoi.getPosition() != slice)
				continue;
			currentRois[j] = cellRoi;
			j++;
		}
		if(j==0)
			return null;
		Roi[] sliceRois = new Roi[j];
		System.arraycopy(currentRois, 0, sliceRois, 0, j);
		return sliceRois;
	}
	
	public static void refineRoi()
	{
		if(getRoiManager().getCount() == 0)
			return;
		Roi[] rois = getRoiFromManager();
		if(rois == null)
			return;
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack ims = imp.getImageStack();
		ArrayList<Roi> roiNew = new ArrayList<Roi>();
		
		for(int i=0; i<rois.length; i++)
			roiNew.add(rois[i]);
		
		for(int s=1; s<=ims.getSize();s=s+3)
		{
			System.out.println("Current slice : " + s);
			Roi[] roiFoci = constructRoisSliceAll(rois, s+1);
			for(int i=0; i<roiFoci.length;i++)
			{
				System.out.println("foci mask " + i + "/" + roiFoci.length);
				Roi mask = roiFoci[i];
				if(mask instanceof PointRoi)
					continue;

				int nInMask = 0;
				Roi[] roisToTreat = new Roi[500];
				for(int j=0; j<roiFoci.length; j++)
				{
					Roi foci = roiFoci[j];
					if(!(foci instanceof PointRoi))
						continue;
					if(mask.getBounds().contains(foci.getBounds().x, foci.getBounds().y))
					{
						roisToTreat[nInMask] = foci;
						nInMask++;
					}
				}
				
				if (nInMask > 0){
					System.out.println(" 	find more than one foci in the mask " + nInMask);
					for(int j=0;j<nInMask;j++)
					{
						int index = -1;
						for(int r=0; r<roiNew.size();r++)
						{
							if(roisToTreat[j].equals(roiNew.get(r)) && roisToTreat[j].getPosition() == roiNew.get(r).getPosition())
								index = r;
						}
						if(index >= 0)
							roiNew.remove(index);
					}
				}
				roiNew.remove(mask);
			}				
		}
		
		if(getRoiManager().getCount() > 0)
			deleteRoiToManager();

		for(int m=0; m<roiNew.size(); m++)
		{
			addRoiToManager(roiNew.get(m));
		}
	
	}
	
  public static void lmBasedFociFinder()
  {
	  File[] f1 = openFolderDialog(1);
	  File[] f2 = openFolderDialog(1);
	  if(f1.length != f2.length)
	  {
		  IJ.showMessage("files' number are not identical");
		  return;
	  }
	  ImagePlus im = IJ.openImage(f1[0].getPath());
	  ImagePlus imp = IJ.createImage("foci-mask","8-bit black", im.getWidth(), im.getHeight(), f1.length*3);
	  ImageStack ims = imp.getImageStack();

	  int sizeMin = 0;
	  int sizeMax = 50;

	  ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET|ParticleAnalyzer.ADD_TO_MANAGER,0,null,sizeMin,sizeMax);
	  pAnalyzer.analyze(imp);
	  Frame frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null)
	  {
		  IJ.run("ROI Manager...");
	  }
	  frame = WindowManager.getFrame("ROI Manager");

	  for(int i=0,s=2;i<f1.length;i++,s=s+3)
	  {
		  ImageProcessor ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);
		  ip.threshold((int)(Auto_Threshold.MaxEntropy(ip.getHistogram())*1.3));
		  ip.invert();
		  //ims.setProcessor(ip, s);
		  imp.setSlice(s);
		  if(!pAnalyzer.analyze(imp, ip)){				
			  // the particle analyzer should produce a dialog on error, so don't make another one
			  continue;
		  }
		  ims.getProcessor(s).insert(IJ.openImage(f1[i].getPath()).getProcessor().convertToByte(true), 0, 0);
	  }
	  imp.show();					
//	  imp = IJ.createImage("foci-LM", im.getWidth(), im.getHeight(), f1.length*3, 8);
//	  ims = imp.getImageStack();
//	  for(int i=0,s=1;i<f1.length;i++,s=s+3)
//	  {
//		  ImageProcessor ip = IJ.openImage(f2[i].getPath()).getProcessor().convertToByte(false);											
//		  ip = IJ.openImage(f1[i].getPath()).getProcessor().convertToByte(true);
//		  ims.setProcessor(ip, s);
//		  ims.setProcessor(ip, s+1);
//		  ims.setProcessor(ip, s+2);
//	  }
//	  imp.show();
	  Roi[] rois = getRoiFromManager();
	  Roi[] pRois = new Roi[rois.length];				
	  for(int i=0;i<rois.length;i++)
	  {
		  if(rois[i] instanceof PointRoi || rois[i] instanceof OvalRoi){
			  pRois[i] = rois[i];
		  }else {						
			  pRois[i] = new PointRoi(rois[i].getBounds().x, rois[i].getBounds().y);
		  }
		  pRois[i].setPosition(rois[i].getPosition());
		  pRois[i].setName(rois[i].getName());
	  }
	  if(getRoiManager().getCount() > 0)
		  deleteRoiToManager();			
	  for(int m=0; m<pRois.length; m++)
	  {
		  if(pRois[m] == null)
			  continue;
		  addRoiToManager(pRois[m]);
	  }

  }
  
  public static void clearoutside()
  {
	  Java2.setSystemLookAndFeel();	
	  Object[] files = io_reader.openFolderDialog(1, false);
	  File[][] filesAll = (File[][])files[0];
	  File[] cfpFiles = (File[])filesAll[1];
	  
	  DirectoryChooser directoryChooser = new DirectoryChooser("Select output folder");
	  String outputDirectoryPath = directoryChooser.getDirectory();
	  if(outputDirectoryPath == null)
	  {		  
		  IJ.showMessage("No output folder selected");
		  return;
	  }
	  
	  for(int i=0;i<cfpFiles.length;i++)
	  {
		  System.out.println("image path : " + cfpFiles[i].getName());		  
		  ImagePlus impSeg = IJ.openImage(cfpFiles[i].getPath());

		  ImageProcessor ip = impSeg.getProcessor().duplicate();
		  ip.threshold(51);
		  ip.dilate();
		  ip.dilate();
		  ip.dilate();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();
		  ip.erode();

		  ip.invert();
		  ImagePlus impBinary = new ImagePlus("binary", ip);
		  impBinary.show();

		  int options = Ins_ParticleAnalyzer.ADD_TO_MANAGER;
		  int measurements = Measurements.AREA|Measurements.RECT;
		  ResultsTable rt = new ResultsTable();
		  Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(options, measurements, rt, 2000, 1000000000);
		  Frame frame = WindowManager.getFrame("ROI Manager");
		  if (frame==null)						
			  IJ.run("ROI Manager...");
		  frame = WindowManager.getFrame("ROI Manager");
		  if (frame==null || !(frame instanceof RoiManager))
		  {return;}
		  RoiManager roiManager = (RoiManager)frame;
		  roiManager.setVisible(true);		
		  Ins_ParticleAnalyzer.setRoiManager(roiManager);	  
		  pAnalyzer.analyze(impBinary, ip);

		  float[] areas = rt.getColumn(ResultsTable.AREA);
		  Roi[] rois = roiManager.getRoisAsArray();
		  if(rois.length>=1)
		  {
			  impSeg.getProcessor().setRoi(rois[0]);
			  impSeg.getProcessor().setBackgroundValue(0);
			  impSeg.getProcessor().fillOutside(rois[0]);			  
			  roiManager.runCommand("delete");
			  impSeg.show();
			  String savePath = outputDirectoryPath + cfpFiles[i].getName();
			  IJ.save(impSeg,savePath);
		  }
	  }
  }
  
  public static void segmentation()
  {
	  Java2.setSystemLookAndFeel();	
	  Object[] files = io_reader.openFolderDialog(1, false);
	  File[][] filesAll = (File[][])files[0];
	  File[] rfpFilesFiles = (File[])filesAll[2];
	  
	  DirectoryChooser directoryChooser = new DirectoryChooser("Select output folder");
	  String outputDirectoryPath = directoryChooser.getDirectory();
	  if(outputDirectoryPath == null)
	  {		  
		  IJ.showMessage("No output folder selected");
		  return;
	  }
	  
	  for(int i=0;i<rfpFilesFiles.length;i++)
	  {
		  System.out.println("image path : " + rfpFilesFiles[i].getName());		  
		  ImagePlus imp = IJ.openImage(rfpFilesFiles[i].getPath());
		  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
		  ROF_Denoise.denoise(ipFluo, 25);	  
		  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
		  
		  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
		  //IJ.save(imp,"d:/denoised.tif");
		  //imp.show();
		  FftBandPassFilter fftFilter = new FftBandPassFilter();
		  fftFilter.setup("", imp);
		  ImageProcessor ip = imp.getProcessor();
		  ip.invert();
		  
		  double ratioNorm = 0.1d;
		  //ImageFbt impFbt = new ImageFbt(ip);
		  
		  ImageFbt impFbt = new ImageFbt(ip, 30, 0, 0, ratioNorm);
		  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
		  int[] seedsF;
		  
		  try {
			  fftFilter.run(ip);
			  //IJ.save(imp,"d:/fft.tif");
			  //seedsF = impFbt.seedsFromBin_ShapeF(0.001, ip);
			  //seedsF = impFbt.seedsFromErode(2, ip, ratioNorm);
			  //seedsF = impFbt.seedsFromBin_ShapeF_noFoci(0.3,40000, ratioNorm,ip);
			  //seedsF = impFbt.seedsFromBin_ShapeF((int)(level*0.8), fluobt);
			  //int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
			  
			  //impFbt.converteLabelToImage(blobs, null).show();
			  //impFbt.converteLabelToImageB(blobs,"segmentation label").show();
			  ImagePlus impTrans = IJ.openImage(rfpFilesFiles[i].getPath());
			  impFbt.removeBackground(impTrans.getProcessor(), ip,5,false);		  
			  ByteProcessor byteProcessor = impFbt.autoLocalThreshold(ip, ratioNorm);		  
			  ImagePlus impSe = new ImagePlus("auto mean threshold", byteProcessor);		  
			  //IJ.save(impSe, "d:/seedsLabel.tif");
			  seedsF = impFbt.seedsFromErode(1, (ByteProcessor)byteProcessor,50);
			  if(seedsF == null)
				  continue;
			  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
			  ImagePlus impSegmentation  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
			  
			  
			  
			  
			  
			  
			  String savePath = outputDirectoryPath + rfpFilesFiles[i].getName();
			  IJ.save(impSegmentation,savePath);
			  //impSegmentation.show();
			  //impFbt.mergeSmallCell(blobs,"byte label",300, false).show();
		  }catch (InterruptedException e) {
			//  e.printStackTrace();
		  }
	  }
  }
  
  
  public static void getROISTriangleThenSegmentation()
  {
	  ImagePlus imp = IJ.openImage();	  
	  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
	  ROF_Denoise.denoise(ipFluo, 25);	  
	  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
	  ImageProcessor ipDenoise = ipFluoS.duplicate();
	  
	  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
	  FftBandPassFilter fftFilter = new FftBandPassFilter();
	  fftFilter.setup("", imp);
	  ImageProcessor ip = imp.getProcessor();	  
	  fftFilter.run(ip);
	  imp.show();
	  Roi[] rois = removeBackground(ip,12);
	  
	  //segmentationClearoutsideTrans(rois, ipDenoise);
	  
	  //ByteProcessor byteProcessor = impFbt.autoLocalThreshold(ip, ratioNorm);		  
	  //ImagePlus impSe = new ImagePlus("auto mean threshold", byteProcessor);		  
	  //seedsF = impFbt.seedsFromErode(1, (ByteProcessor)byteProcessor);
  }
  
  public static void segmentationClearoutsideTrans(Roi[] rois, ImageProcessor ip)
  {
	  ImagePlus impOut = IJ.createImage("output", "8-bit black", ip.getWidth(), ip.getHeight(), 1);
	  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
	  for(int i=0; i<rois.length; i++)
	  {		  
		  Roi roi = rois[i];
		  ImageProcessor roiMask = roi.getMask();
		  //roiMask.dilate();
		  //roiMask.dilate();
		  //new ImagePlus("roiMask", roiMask).show();	  
		  System.out.println("Working roi : " + roi.getName());
		  int xloc = roi.getBounds().x;
		  int yloc = roi.getBounds().y;		  
		  ip.setRoi(roi);
		  ImageProcessor ipRoi = ip.crop();
		  ImagePlus impRoi = new ImagePlus("roi", ipRoi);
		  ImageFbt impFbt = new ImageFbt(ipRoi, 30, 0, 0, 0.1,roiMask);		  
		  
		  FftBandPassFilter fftFilter = new FftBandPassFilter();
		  fftFilter.setup("", impRoi);
		  fftFilter.run(ipRoi);
		  ByteProcessor byteProcessor = impFbt.autoLocalThreshold(ipRoi, 0.1);
		  //ImagePlus impSe = new ImagePlus("thresLocal", byteProcessor);
		  //new ImagePlus("fft", byteProcessor).show();
		try {
			int[] seedsF = impFbt.seedsFromErode(2, (ByteProcessor)byteProcessor,50);
			 if(seedsF == null)
				  continue;
			  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
			  ImagePlus impSegmentation  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
			  impOut.getProcessor().insert(impSegmentation.getProcessor(), xloc, yloc);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	  }
	  impOut.show();
  }
  
  
  public static void maskLMbasedSegmentation()
  {
	  Java2.setSystemLookAndFeel();	
	  Object[] files = io_reader.openFolderDialog(1, false);
	  File[][] filesAll = (File[][])files[0];
	  File[] origFilesFiles = (File[])filesAll[0];
	  File[] binaryFiles = (File[])filesAll[1];
	  File[] maskFiles = (File[])filesAll[2];
	  File[] lmFiles = (File[])filesAll[3];

	  File file = new File(origFilesFiles[0].getParentFile().getParent()+File.separator+"SEG");
	  file.mkdirs();
	  String outputDirectoryPath= file.getAbsolutePath();

	  boolean fromBinary = false;
	  GenericDialog genericDialog = new GenericDialog("Result Table");
	  genericDialog.addCheckbox("From binary", fromBinary);
	  genericDialog.showDialog();
	  if(genericDialog.wasCanceled())
		  return;
	  fromBinary = genericDialog.getNextBoolean();

	  ImagePlus imp0 = IJ.openImage(origFilesFiles[0].getPath());
	  ImagePlus bImpRES = IJ.createImage("", "8-bit white", imp0.getWidth(), imp0.getHeight(), origFilesFiles.length*3);
	  Roi[][] rois = new Roi[origFilesFiles.length][];

	  boolean debug = false;

	  for(int i=0,k=1;i<origFilesFiles.length;i++,k=k+3)//rfpFilesFiles.length//origFilesFiles.length	  
	  {
		  System.out.println("image name : " + origFilesFiles[i].getName());		  
		  ImagePlus imp = IJ.openImage(origFilesFiles[i].getPath());

		  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
		  ROF_Denoise.denoise(ipFluo, 25);	  
		  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
		  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
		  FftBandPassFilter fftFilter = new FftBandPassFilter();
		  fftFilter.setLargeSmallFilterSize(10, 3);
		  fftFilter.setup("", imp);
		  ImageProcessor ip = imp.getProcessor();
		  //ImageProcessor ipTostack = ip.duplicate();
		  double ratioNorm = 0.1d;
		  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
		  int[] seedsF;		  
		  try {
			  fftFilter.run(ip);
			  if(debug)
				  new ImagePlus("fft band pass", ip).show();			  
			  if(fromBinary)
			  {
				  ImagePlus seeds = IJ.openImage(binaryFiles[0].getPath());
				  ImageProcessor ipMask = seeds.getImageStack().getProcessor(k);
				  ImageFbt impFbt = new ImageFbt(ip, ipMask);
				  ImageProcessor ipSeeds = seeds.getImageStack().getProcessor(k+1);
				  
				  seedsF = impFbt.seedsFromBinary(0, ipSeeds,50);
				  if(seedsF == null)
					  continue;
				  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
				  ImagePlus impSeg  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
				  if(debug)
				  {
					  impSeg.show();					  
					  IJ.save(impSeg, "d:/segmentation.tif");
				  }
				  Frame frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null)						
					  IJ.run("ROI Manager...");
				  frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null || !(frame instanceof RoiManager))
				  {return;}
				  RoiManager roiManager = (RoiManager)frame;
				  ImageProcessor bp = impSeg.getProcessor();
				  bp.threshold(101);
				  bp.invert();
				  rois[i] = writeToRT(bp, k, roiManager);	
				  bImpRES.getImageStack().getProcessor(k).insert(ipMask, 0, 0);
				  bImpRES.getImageStack().getProcessor(k+1).insert(bp, 0, 0);
				  bImpRES.getImageStack().getProcessor(k+2).insert(ip.convertToByte(true), 0, 0);
				  bImpRES.getImageStack().setSliceLabel("mask", k);
				  bImpRES.getImageStack().setSliceLabel("seeds", k+1);
				  bImpRES.getImageStack().setSliceLabel(imp.getTitle(), k+2);

			  }else {
				  ImagePlus impMASK = IJ.openImage(maskFiles[i].getPath());
				  ImagePlus impLM = IJ.openImage(lmFiles[i].getPath());
				  ImageProcessor ipMaskLM = removeBackgroundMaskOnLM(impMASK.getProcessor(),impLM.getProcessor()).getProcessor();				  				  
				  bImpRES.getImageStack().getProcessor(k).insert(ipMaskLM, 0, 0);
				  bImpRES.getImageStack().getProcessor(k+1).insert(ipMaskLM, 0, 0);
				  bImpRES.getImageStack().getProcessor(k+2).insert(ip.convertToByte(true), 0, 0);
				  bImpRES.getImageStack().setSliceLabel("mask", k);
				  bImpRES.getImageStack().setSliceLabel("seeds", k+1);
				  bImpRES.getImageStack().setSliceLabel(imp.getTitle(), k+2);
			  }
			  
		  }catch (InterruptedException e) {
			  //  e.printStackTrace();
		  }
	  }
	  bImpRES.show();
	  String savePath = outputDirectoryPath +File.separator+ "seg-orig.tif";
	  IJ.save(bImpRES,savePath);
	  String savePathRoi = outputDirectoryPath +File.separator+ "roiset.zip";
	  saveRoiToZip(rois,savePathRoi);


	  //			  
	  //			  
	  //			  
	  //		if (fromBinary) {
	  //		  ImagePlus seeds = IJ.openImage(binaryFiles[0].getPath());
	  //		  if(seeds == null)
	  //		  {
	  //			  IJ.showMessage("No seeds image found!");
	  //			  return;
	  //		  }
	  //		  for(int i=0,k=1;i<origFilesFiles.length;i++,k=k+2)//rfpFilesFiles.length	  
	  //		  {
	  //			  System.out.println("image name : " + origFilesFiles[i].getName());		  
	  //			  ImagePlus imp = IJ.openImage(origFilesFiles[i].getPath());
	  //			  ImageProcessor ipRfp = imp.getProcessor().duplicate();
	  //
	  //			  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
	  //			  ROF_Denoise.denoise(ipFluo, 25);	  
	  //			  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
	  //			  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
	  //			  FftBandPassFilter fftFilter = new FftBandPassFilter();
	  //			  fftFilter.setLargeSmallFilterSize(10, 3);
	  //			  fftFilter.setup("", imp);
	  //			  ImageProcessor ip = imp.getProcessor();
	  //			  double ratioNorm = 0.1d;
	  //			  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
	  //			  int[] seedsF;		  
	  //			  try {
	  //				  fftFilter.run(ip);
	  ////				  ImageProcessor ipTostack = ip.duplicate();
	  //				  if(debug)
	  //					  new ImagePlus("fft band pass", ip).show();			  
	  //				  ImageFbt impFbt = new ImageFbt(ip, 15, 0, 0, ratioNorm);
	  //				  ipRfp.medianFilter();
	  //				  if(removeBg)
	  //				  {
	  //					  if(i<maxEntropyEnd-1)
	  //						  impFbt.removeBackground(ipRfp, ip,1,true);
	  //					  else {
	  //						  impFbt.removeBackground(ipRfp, ip,1,false);
	  //					  }
	  //				  }				  
	  //				  ip.invert();
	  //				  // seeds come from the 
	  //				  ImageProcessor ipSeeds = seeds.getImageStack().getProcessor(k);//IJ.openImage(binaryFiles[i].getPath());
	  //				  ImageProcessor ipModified = seeds.getImageStack().getProcessor(k+1);
	  //				  ipSeeds = projectIpToSeedsIp(ipModified, ipSeeds);
	  //				  //ipSeeds.invert();
	  //				  ipSeeds.threshold(5);
	  //				  seedsF = impFbt.seedsFromBinary(0, ipSeeds);			  
	  //				  
	  //				  if(seedsF == null)
	  //					  continue;
	  //				  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
	  //				  ImagePlus impSegmentation  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
	  //				  if(debug)
	  //				  {
	  //					  impSegmentation.duplicate().show();
	  //					  //return;
	  //				  }
	  //				  Frame frame = WindowManager.getFrame("ROI Manager");
	  //				  if (frame==null)						
	  //					  IJ.run("ROI Manager...");
	  //				  frame = WindowManager.getFrame("ROI Manager");
	  //				  if (frame==null || !(frame instanceof RoiManager))
	  //				  {return;}
	  //				  RoiManager roiManager = (RoiManager)frame;
	  //
	  //				  //impSeg.show();				  				 
	  //				  ImageProcessor bp = impSegmentation.getProcessor();
	  //				  //bp.threshold(0);				  
	  //				  int level = 30;
	  //				  ImageProcessor ipNumerise = numerizeCellByRFP(bp, ip, roiManager, level);
	  //				  deleteRoiInManager();
	  //				  ImageProcessor ipN = ipNumerise.duplicate();
	  //				  ipN.threshold(level);				  
	  //				  ipN.invert();				  
	  //				  rois[i] = writeToRT(ipN, k, roiManager);	
	  //				  bPTrans.getImageStack().setSliceLabel(imp.getTitle(), k);
	  //				  bPTrans.getImageStack().getProcessor(k).insert(ipNumerise, 0, 0);
	  //				  ip.invert();
	  //				  bPTrans.getImageStack().getProcessor(k+1).insert(ip.convertToByte(true), 0, 0);				  				  
	  //				  ipN = numerizeCell(ipN,roiManager);
	  //				  ImagePlus impN = new ImagePlus("numerised", ipN);
	  //				  deleteRoiInManager();
	  //				  if(debug)
	  //					  impN.show();		  
	  //				  String savePath = outputDirectoryPath2 + File.separator + origFilesFiles[i].getName();
	  //				  IJ.save(impN, savePath);
	  //			  }catch (InterruptedException e) {
	  //				  //  e.printStackTrace();
	  //			  }
	  //		  }
	  //	  }
	  //	  bPTrans.show();
	  //	  String savePath = outputDirectoryPath +File.separator+ "seg-rfp.tif";
	  //	  IJ.save(bPTrans,savePath);
	  //	  String savePathRoi = outputDirectoryPath +File.separator+ "roiset.zip";
	  //	  saveRoiToZip(rois,savePathRoi);
  }
  // selecte the folder which includes rfp, then selecte/create folder1, then selecte/create folder 2
  // param can be changed : threshold method in  impFbt.removeBackground, threshold level in numerizeCellByRFP,
  // new method to remove background without Trans image, but pay attention to the rfp image using method of threshold.
  public static void segmentationClearoutsideNumerise()
  {
	  Java2.setSystemLookAndFeel();	
	  Object[] files = io_reader.openFolderDialog(1, false);
	  File[][] filesAll = (File[][])files[0];
	  File[] origFilesFiles = (File[])filesAll[0];
	  File[] binaryFiles = (File[])filesAll[1];
	  File[] maskFiles = (File[])filesAll[2];
	  File[] lmFiles = (File[])filesAll[3];
	  //File[] transFiles = (File[])filesAll[0];
	  
	  File file = new File(origFilesFiles[0].getParentFile().getParent()+File.separator+"SEG");
	  file.mkdirs();
	  String outputDirectoryPath= file.getAbsolutePath();
	  
	  file = new File(origFilesFiles[0].getParentFile().getParent()+File.separator+"RES2");
	  file.mkdirs();
	  String outputDirectoryPath2= file.getAbsolutePath();

	  boolean fromBinary = false;
	  boolean removeBg = true;
	  int maxEntropyEnd = 70;
	  //boolean fromManager = false;
	  GenericDialog genericDialog = new GenericDialog("Result Table");
	  genericDialog.addCheckbox("From binary", fromBinary);
	  genericDialog.addCheckbox("Remove background", removeBg);
	  genericDialog.addNumericField("Max entropy ending slice", maxEntropyEnd,0);
	  genericDialog.showDialog();
	  
	  if(genericDialog.wasCanceled())
		  return;
	  fromBinary = genericDialog.getNextBoolean();
	  removeBg = genericDialog.getNextBoolean();
	  maxEntropyEnd = (int)genericDialog.getNextNumber();
	  //fromManager = genericDialog.getNextBoolean();

	  ImagePlus imp0 = IJ.openImage(origFilesFiles[0].getPath());
	  ImagePlus bPTrans = IJ.createImage("", "8-bit white", imp0.getWidth(), imp0.getHeight(), origFilesFiles.length*2);
	  Roi[][] rois = new Roi[origFilesFiles.length][];
	  
	  boolean debug = false;
	  if(!fromBinary)
	  {
		  for(int i=0,k=1;i<origFilesFiles.length;i++,k=k+2)//rfpFilesFiles.length	  
		  {
			  System.out.println("image name : " + origFilesFiles[i].getName());		  
			  ImagePlus imp = IJ.openImage(origFilesFiles[i].getPath());
			  ImageProcessor ipRfp = imp.getProcessor().duplicate();

			  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
			  ROF_Denoise.denoise(ipFluo, 25);	  
			  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
			  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
			  FftBandPassFilter fftFilter = new FftBandPassFilter();
			  fftFilter.setLargeSmallFilterSize(10, 3);
			  fftFilter.setup("", imp);
			  ImageProcessor ip = imp.getProcessor();
			  //ImageProcessor ipTostack = ip.duplicate();
			  double ratioNorm = 0.1d;
			  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
			  int[] seedsF;		  
			  try {
				  fftFilter.run(ip);				  
//				  ImageProcessor ipTostack = ip.duplicate();
				  if(debug)
					  new ImagePlus("fft band pass", ip).show();			  
				  ImageFbt impFbt = new ImageFbt(ip, 15, 0, 0, ratioNorm);

				  
				  /*			  ImagePlus impTrans = IJ.openImage(transFiles[i].getPath());			  
			  BackgroundSubtracter backgroundSubtracter  = new BackgroundSubtracter();
			  backgroundSubtracter.rollingBallBackground(impTrans.getProcessor(), 2, false, false, true, true, false);
			  fftFilter.setup("", impTrans);
			  fftFilter.setLargeSmallFilterSize(40, 5);
			  fftFilter.run(impTrans.getProcessor());
			  if(debug)
				  impTrans.duplicate().show();*/

				  ipRfp.medianFilter();
				  if(removeBg)
				  {
					  if(i<maxEntropyEnd-1)
						  impFbt.removeBackground(ipRfp, ip,1,true);
					  else {
						  impFbt.removeBackground(ipRfp, ip,1,false);
					  }
				  }
				  ip.invert();
				  ByteProcessor byteProcessor = impFbt.autoLocalThreshold(ip, ratioNorm);				  
				  //ImagePlus impSe = new ImagePlus("local_mean", byteProcessor);
				  //impSe.show();
				  //IJ.save(impSe, "d:/seedsLabel.tif");
				  seedsF = impFbt.seedsFromBinary(1, (ByteProcessor)byteProcessor,50);
				  if(seedsF == null)
					  continue;
				  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
				  ImagePlus impSegmentation  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
				  ImagePlus impSeg = impSegmentation;			  
				  if(debug)
				  {
					  impSeg.show();					  
					  IJ.save(impSeg, "d:/segmentation.tif");
					  //return;
				  }
				  //ImageProcessor ipS = impSeg.getProcessor().duplicate();
				  //ipS.threshold(20);
				  //ipS.invert();

				  Frame frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null)						
					  IJ.run("ROI Manager...");
				  frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null || !(frame instanceof RoiManager))
				  {return;}
				  RoiManager roiManager = (RoiManager)frame;

				  //impSeg.show();				  				 
				  ImageProcessor bp = impSeg.getProcessor();
				  //bp.threshold(0);				  
				  int level = 30;
				  ImageProcessor ipNumerise = numerizeCellByRFP(bp, ip, roiManager, level);
				  deleteRoiInManager();
				  ImageProcessor ipN = ipNumerise.duplicate();
				  ipN.threshold(level);				  
				  ipN.invert();				  
				  rois[i] = writeToRT(ipN, k, roiManager);	
				  bPTrans.getImageStack().setSliceLabel(imp.getTitle(), k);
				  bPTrans.getImageStack().getProcessor(k).insert(ipNumerise, 0, 0);
				  ip.invert();
				  bPTrans.getImageStack().getProcessor(k+1).insert(ip.convertToByte(true), 0, 0);				  				  
				  ipN = numerizeCell(ipN,roiManager);
				  ImagePlus impN = new ImagePlus("numerised", ipN);
				  deleteRoiInManager();
				  if(debug)
					  impN.show();		  
				  String savePath = outputDirectoryPath2 + File.separator + origFilesFiles[i].getName();
				  IJ.save(impN, savePath);
			  }catch (InterruptedException e) {
				  //  e.printStackTrace();
			  }
		  }
	  }else if (fromBinary) {
		  ImagePlus seeds = IJ.openImage(binaryFiles[0].getPath());
		  if(seeds == null)
		  {
			  IJ.showMessage("No seeds image found!");
			  return;
		  }
		  for(int i=0,k=1;i<origFilesFiles.length;i++,k=k+2)//rfpFilesFiles.length	  
		  {
			  System.out.println("image name : " + origFilesFiles[i].getName());		  
			  ImagePlus imp = IJ.openImage(origFilesFiles[i].getPath());
			  ImageProcessor ipRfp = imp.getProcessor().duplicate();

			  FloatProcessor ipFluo = (FloatProcessor)imp.getProcessor().convertToFloat();
			  ROF_Denoise.denoise(ipFluo, 25);	  
			  ImageProcessor ipFluoS = ipFluo.convertToShort(true);
			  imp = new ImagePlus(imp.getTitle()+"-denoised", ipFluoS);
			  FftBandPassFilter fftFilter = new FftBandPassFilter();
			  fftFilter.setLargeSmallFilterSize(10, 3);
			  fftFilter.setup("", imp);
			  ImageProcessor ip = imp.getProcessor();
			  double ratioNorm = 0.1d;
			  Fluo_Bac_Tracker fluobt = new Fluo_Bac_Tracker();
			  int[] seedsF;		  
			  try {
				  fftFilter.run(ip);
//				  ImageProcessor ipTostack = ip.duplicate();
				  if(debug)
					  new ImagePlus("fft band pass", ip).show();			  
				  ImageFbt impFbt = new ImageFbt(ip, 15, 0, 0, ratioNorm);
				  ipRfp.medianFilter();
				  if(removeBg)
				  {
					  if(i<maxEntropyEnd-1)
						  impFbt.removeBackground(ipRfp, ip,1,true);
					  else {
						  impFbt.removeBackground(ipRfp, ip,1,false);
					  }
				  }				  
				  ip.invert();
				  // seeds come from the 
				  ImageProcessor ipSeeds = seeds.getImageStack().getProcessor(k);//IJ.openImage(binaryFiles[i].getPath());
				  ImageProcessor ipModified = seeds.getImageStack().getProcessor(k+1);
				  ipSeeds = projectIpToSeedsIp(ipModified, ipSeeds);
				  //ipSeeds.invert();
				  ipSeeds.threshold(5);
				  seedsF = impFbt.seedsFromBinary(0, ipSeeds,50);			  
				  
				  if(seedsF == null)
					  continue;
				  int[] blobs = impFbt.dilate_ShapeF(seedsF, impFbt.computeViscosity(1, 25),50, fluobt);
				  ImagePlus impSegmentation  = impFbt.converteLabelToImageB(blobs,"segmentation "+String.valueOf(i+1));
				  if(debug)
				  {
					  impSegmentation.duplicate().show();
					  //return;
				  }
				  Frame frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null)						
					  IJ.run("ROI Manager...");
				  frame = WindowManager.getFrame("ROI Manager");
				  if (frame==null || !(frame instanceof RoiManager))
				  {return;}
				  RoiManager roiManager = (RoiManager)frame;

				  //impSeg.show();				  				 
				  ImageProcessor bp = impSegmentation.getProcessor();
				  //bp.threshold(0);				  
				  int level = 30;
				  ImageProcessor ipNumerise = numerizeCellByRFP(bp, ip, roiManager, level);
				  deleteRoiInManager();
				  ImageProcessor ipN = ipNumerise.duplicate();
				  ipN.threshold(level);				  
				  ipN.invert();				  
				  rois[i] = writeToRT(ipN, k, roiManager);	
				  bPTrans.getImageStack().setSliceLabel(imp.getTitle(), k);
				  bPTrans.getImageStack().getProcessor(k).insert(ipNumerise, 0, 0);
				  ip.invert();
				  bPTrans.getImageStack().getProcessor(k+1).insert(ip.convertToByte(true), 0, 0);				  				  
				  ipN = numerizeCell(ipN,roiManager);
				  ImagePlus impN = new ImagePlus("numerised", ipN);
				  deleteRoiInManager();
				  if(debug)
					  impN.show();		  
				  String savePath = outputDirectoryPath2 + File.separator + origFilesFiles[i].getName();
				  IJ.save(impN, savePath);
			  }catch (InterruptedException e) {
				  //  e.printStackTrace();
			  }
		  }
	  }
	  bPTrans.show();
	  String savePath = outputDirectoryPath +File.separator+ "seg-rfp.tif";
	  IJ.save(bPTrans,savePath);
	  String savePathRoi = outputDirectoryPath +File.separator+ "roiset.zip";
	  saveRoiToZip(rois,savePathRoi);
  }
  
  private static ImageProcessor projectIpToSeedsIp(ImageProcessor ipMask,
		ImageProcessor ipSeeds) {
	  
	  ImageProcessor ip = IJ.createImage("new seeds", "8-bit black", ipSeeds.getWidth(), ipSeeds.getHeight(), 1).getProcessor();
	  
	  for(int i=0;i<ip.getWidth();i++)
		  for(int j=0;j<ip.getHeight();j++)
		  {
			  if(ipMask.get(i, j) == 255)
			  {
				  ip.set(i, j, 255);
				  continue;
			  }
			  ip.set(i, j, ipSeeds.get(i, j));
		  }
	return ip;
}

public int[] seedsFromBinary(int erodeNumber, ImageProcessor seedsIp, int minimumPixelNumberBySeeds)throws
  InterruptedException
  {	  
	  //	  new ImagePlus("seeds orig", seedsIp.duplicate()).show();
	  int[] label = seedsFromErodeStep(erodeNumber, (ByteProcessor)seedsIp,minimumPixelNumberBySeeds);
	  return label;
  }

public static void saveRoiToZip(Roi[][] rois, String path)
  {
	  try {
		  ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
		  DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
		  RoiEncoder re = new RoiEncoder(out);
		  for(int j=0;j<rois.length;j++)
		  {
			  Roi[] rois2 = rois[j];
			  if(rois2 == null)
				  continue;
			  for (int i=0; i<rois2.length; i++) {
				  Roi roi = rois2[i];
				  String label = roi.getName();
				  if(label == null)
				  {
					  label = getLabel(roi);
					  roi.setName(label);			
					  if(label == null)
						  continue;
				  }
				  if (false) System.out.println("saveMultiple: "+i+"  "+label+"  "+roi);
				  if (!label.endsWith(".roi")) label += ".roi";
				  try {
					  zos.putNextEntry(new ZipEntry(label));
					  re.write(roi);
					  out.flush();
				  } catch (Exception e) {
					  continue;
				  }					
			  }
		  }
		  System.out.println("save rois to : " + path);
		  out.close();
	  } catch (FileNotFoundException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  } catch (IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  }
  }
  public static String getLabel(Roi roi) {
		int slice = roi.getPosition();
		Rectangle r = roi.getBounds();
		int xc = r.x + r.width/2;
		int yc = r.y + r.height/2;
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();		
		xs = "000000" + xc;
		ys = "000000" + yc;
		String label = ys.substring(ys.length()-digits) + "-" + xs.substring(xs.length()-digits);
		String zs = "000000" + slice;
		label = zs.substring(zs.length()-digits) + "-" + label;
		return label;
	}
  
  public static Roi[] writeToRT(ImageProcessor bp,int slice,RoiManager roiManager)
  {	  
	  deleteRoiInManager();	  
	  int options = Ins_ParticleAnalyzer.ADD_TO_MANAGER|Ins_ParticleAnalyzer.CLEAR_WORKSHEET;
	  int measurements = Measurements.AREA|Measurements.RECT;
	  ResultsTable rt = new ResultsTable();
	  Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(options, measurements, rt, 50, 1000000000);
	  ImagePlus imageBinary = new ImagePlus("impNN", bp);
	  pAnalyzer.analyze(imageBinary, bp);
	  if(roiManager.getCount()>0)
	  {		  
		  Roi[] rois = roiManager.getRoisAsArray();		  
		  for(int i=0;i<rois.length;i++)
		  {			  
			  rois[i].setPosition(slice+2);
			  rois[i].setName(null);
		  }
		  System.out.println("cell's number is : " + rois.length);
		  return rois;
	  }else {
		  return null;
	  }
  }
  
  public static Roi[] constructRoisSlice(Roi[] roiArray, int slice)
  {
	  int count = roiArray.length;
	  Roi[] currentRois = new Roi[count];		
	  int j=0;
	  for (int i=0; i<count; i++) {	
		  Roi cellRoi = roiArray[i];
		  if(cellRoi.getPosition() != slice)
			  continue;
		  if (!(cellRoi instanceof Arrow)) {
			  currentRois[j] = cellRoi;
			  j++;
		  }
	  }
	  if(j==0)
		  return null;
	  Roi[] sliceRois = new Roi[j];
	  System.arraycopy(currentRois, 0, sliceRois, 0, j);
	  return sliceRois;
  }
  
  public static void deleteRoiInManager()
  {
	  Frame frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null)						
		  IJ.run("ROI Manager...");
	  frame = WindowManager.getFrame("ROI Manager");
	  if (frame==null || !(frame instanceof RoiManager))
	  {return;}
	  RoiManager roiManager = (RoiManager)frame;
	  if(roiManager.getCount()>0)
		  roiManager.runCommand("delete");		
  }
  
  public static ImageProcessor numerizeCell(ImageProcessor bp, RoiManager roiManager)
  {	  
	  deleteRoiInManager();
	  int options = Ins_ParticleAnalyzer.ADD_TO_MANAGER|Ins_ParticleAnalyzer.CLEAR_WORKSHEET;
	  int measurements = Measurements.AREA|Measurements.RECT;
	  ResultsTable rt = new ResultsTable();
	  Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(options, measurements, rt, 50, 1000000000);
	  Ins_ParticleAnalyzer.setRoiManager(roiManager);
	  ImagePlus imageBinary = new ImagePlus("impNN", bp);
	  pAnalyzer.analyze(imageBinary, bp);
	  	  
	  ImagePlus imp = IJ.createImage("numerised imp", "8-bit black",imageBinary.getWidth(), imageBinary.getHeight(), 1);
	  ImageProcessor ip = imp.getProcessor();	  
	  
	  if(roiManager.getCount()>0)
	  {
		  Roi[] rois = roiManager.getRoisAsArray();
		  for(int i=0,j=1;i<rois.length;i++,j=j+1)
		  {
			  ip.setRoi(rois[i]);
			  ip.setValue(j);;
			  ip.fill(rois[i]);;
		  }
	  }
	  return ip;
  }
  
  public static ImageProcessor numerizeCellByRFP(ImageProcessor bp, ImageProcessor rfp, RoiManager roiManager, int level)
  {	  
	  bp.autoThreshold();	  
	  bp.invert();
	  int options = Ins_ParticleAnalyzer.ADD_TO_MANAGER|Ins_ParticleAnalyzer.CLEAR_WORKSHEET;
	  int measurements = Measurements.AREA;
	  ResultsTable rt = new ResultsTable();
	  Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(options, measurements, rt, 50, 15000);	  
	  Ins_ParticleAnalyzer.setRoiManager(roiManager);
	  ImagePlus imageBinary = new ImagePlus("", bp);
	  pAnalyzer.analyze(imageBinary, bp);
	  
	  ImagePlus imp = IJ.createImage("numerised imp", "8-bit black",imageBinary.getWidth(), imageBinary.getHeight(), 1);
	  ImageProcessor ip = imp.getProcessor();
	  rfp = rfp.convertToByte(true);
	  rfp.invert();
	  int mOption = Measurements.MEDIAN|Measurements.MIN_MAX;
	  
	  if(roiManager.getCount()>0)
	  {
		  Roi[] rois = roiManager.getRoisAsArray();	  
		  for(int i=0,j=1;i<rois.length;i++,j=j+1)
		  {
			  rfp.setRoi(rois[i]);
			  ImageStatistics statsCell = ImageStatistics.getStatistics(rfp, mOption, null);	
			  ip.setRoi(rois[i]);
			  if(statsCell.max>level) // level means the roi is considered as Cell if the max value is larger than the level
			  {
				  ip.setValue(statsCell.median);
				  ip.fill(rois[i]);
			  }
		  }
	  }
	  return ip;
  }
  
  
  
  public static void numerizeCellinFolder()
  {	  
	  ImagePlus imp = IJ.openImage();
	  ImageStack ims = imp.getImageStack();
	  ImagePlus impNumerise = IJ.createImage("numerised imp", "8-bit black", imp.getWidth(), imp.getHeight(), ims.getSize());
	  ImageStack imsNumerise = impNumerise.getImageStack();
	  for(int i=0; i<ims.getSize();i++)
	  {
		  ImageProcessor ip = ims.getProcessor(i+1);
		  ip.threshold(101);
		  ImageProcessor ipNumerise = numerizeCell(ip,null);
		  //new ImagePlus("", ipNumerise).show();
		  imsNumerise.getProcessor(i+1).insert(ipNumerise, 0, 0);
	  }
	  impNumerise.show();
	  IJ.save(impNumerise, "d:/numerisedImp.tif");
  }
  
}
