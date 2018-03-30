package cellst.Main;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.FolderOpener;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import cellst.Enums.BlobsDrawOpt;
import cellst.Image.ShapeFbt;
import cellst.Image.ShapeSet;

/**
 * Class regrouping all useful methods not attached to a class.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Utils
{

  private static final Random rand = new Random(5);

  // ===========================================================================
  //                  Test over coordinates or index
  // ===========================================================================
  /**
   * Test if a coordinate x is valid. x must be superior or equal to 0 and
   * inferior to width.
   *
   * @param x coordinate.
   * @return boolean true : x is valid , false : x is out of bounds.
   */
  public static boolean isValidX(int x, int width)
  {
    if (x >= 0 && x < width)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Test if a coordinate y is valid. y must be superior or equal to 0 and
   * inferior to height.
   *
   * @param y coordinate.
   * @return boolean true : y is valid , false : y is out of bounds.
   */
  public static boolean isValidY(int y, int height)
  {
    if (y >= 0 && y < height)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Test if an index i is valid. i must be superior or equal 0 and inferior to
   * height*width.
   *
   * @param i index.
   * @return boolean true : i is valid , false : i is out of bounds.
   */
  public static boolean isValidInd(int i, int width, int height)
  {
    if (i >= 0 && i < width * height)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  // ===========================================================================
  //             Conversions between index and coordinates
  // ===========================================================================
  /**
   * Conversion from index to 2D coordinates in an array corresponding to an
   * image.
   *
   * @param i index.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return array of 2 int : coordinates.
   */
  public static int[] indexToCoord(int i, int width, int height)
  {
    if (isValidInd(i, width, height))
    {
      int[] res =
      {
        i % width, i / width
      };
      return (res);

    }
    else
    {
      throw (new ArrayIndexOutOfBoundsException("( width = " + width
                                                + " * height = " + height
                                                + " ) = " + width * height
                                                + " : invalid index : " + i));
    }

  }

  /**
   * Conversion from 2D coordinates to index in an array corresponding to an
   * image.
   *
   * @param x x-coordinate.
   * @param y y-coordinate.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return corresponding index.
   */
  public static int coordToIndex(int x, int y, int width, int height)
  {
    if (isValidX(x, width) && isValidY(y, height))
    {
      return (y * width + x);
    }
    else
    {
      throw (new ArrayIndexOutOfBoundsException("width = " + width
                                                + " , height = " + height
                                                + " : invalid x or y : " + x
                                                + " , " + y));
    }

  }

  /**
   * Conversion from index to 2D coordinates in a Point corresponding to an
   * image.
   *
   * @param i index.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return Point with coordinates.
   */
  public static Point indexToPoint(int i, int width, int height)
  {
    if (isValidInd(i, width, height))
    {
      Point res = new Point(i % width, i / width);
      return (res);
    }
    else
    {
      throw (new ArrayIndexOutOfBoundsException("( width = " + width
                                                + " * height = " + height
                                                + " ) = " + width * height
                                                + " : invalid index : " + i));
    }

  }

  /**
   * Conversion from 2D coordinates to index in an array corresponding to an
   * image.
   *
   * @param p Point
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return corresponding index.
   */
  public static int pointToIndex(Point p, int width, int height)
  {
    if (isValidX(p.x, width) && isValidY(p.y, height))
    {
      return (p.y * width + p.x);
    }
    else
    {
      throw (new ArrayIndexOutOfBoundsException("width = " + width
                                                + " , height = " + height
                                                + " : invalid x or y : " + p.x
                                                + " , " + p.y));
    }

  }

  // ===========================================================================
  //              Finding neighbors functions
  // ===========================================================================
  /**
   * Find the neighboring points of a point (x,y). This method check if
   * neighbors are out of the corresponding image.
   *
   * @param x x coordinate.
   * @param y y coordinate.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @param connected8 8 or 4-connected.
   * @return the neighboring points of (x,y) in an image width * height.
   */
  public static HashSet<Point> getNeigh(int x, int y, int width, int height,
                                        boolean connected8)
  {
    HashSet<Point> res = new HashSet<Point>();

    if (x > 0)
    {
      res.add(new Point(x - 1, y));
    }
    if (y > 0)
    {
      res.add(new Point(x, y - 1));
    }
    if (x < width - 1)
    {
      res.add(new Point(x + 1, y));
    }
    if (y < height - 1)
    {
      res.add(new Point(x, y + 1));
    }

    if (connected8)
    {
      if (x > 0 && y > 0)
      {
        res.add(new Point(x - 1, y - 1));
      }
      if (x < width - 1 && y > 0)
      {
        res.add(new Point(x + 1, y - 1));
      }
      if (x > 0 && y < height - 1)
      {
        res.add(new Point(x - 1, y + 1));
      }
      if (x < width - 1 && y < height - 1)
      {
        res.add(new Point(x + 1, y + 1));
      }
    }

    return res;
  }

  /**
   * Find the neighboring points of a point (x,y). This method doesn't check if
   * neighbors are out of the corresponding image.
   *
   * @param x x coordinate.
   * @param y y coordinate.
   * @param connected8 8 or 4-connected.
   * @return the neighboring points of (x,y) in an image width * height.
   */
  public static HashSet<Point> getNeigh(int x, int y, boolean connected8)
  {
    return getNeigh(x, y, Integer.MAX_VALUE, Integer.MAX_VALUE, connected8);
  }

  /**
   * Find the neighboring points of a point of index 'index'. This method check
   * if neighbors are out of the corresponding image.
   *
   * @param index index of the point.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @param connected8 8 or 4-connected.
   * @return the neighboring points of point of index 'index' in an image width
   * * height.
   */
  public static HashSet<Point> getNeigh(int index, int width, int height,
                                        boolean connected8)
  {
    int[] couple = indexToCoord(index, width, height);
    return getNeigh(couple[0], couple[1], width, height, connected8);
  }

  /**
   * Find the neighboring indexes of a point ( x, y ). This method check if
   * neighbors are out of the corresponding image.
   *
   * @param x x coordinate.
   * @param y y coordinate.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @param connected8 8 or 4-connected.
   * @return the neighboring indexes of point of index 'index' in an image width
   * * height.
   */
  public static HashSet<Integer> getNeighIndex(int x, int y, int width,
                                               int height, boolean connected8)
  {
    HashSet<Integer> res = new HashSet<Integer>();

    HashSet<Point> neighCoord = getNeigh(x, y, width, height, connected8);
    Iterator itr = neighCoord.iterator();

    while (itr.hasNext())
    {
      Point coord = (Point) itr.next();
      res.add(coordToIndex(coord.x, coord.y, width, height));
    }

    return res;
  }

  /**
   * Find the neighboring indexes of a point of index 'i'. This method check if
   * neighbors are out of the corresponding image.
   *
   * @param i index of the point
   * @param width width of the image
   * @param height height of the image
   * @param connected8 8- or 4-connected
   * @return the neighboring indexes of point of index 'i' in an image width *
   * height
   */
  public static HashSet<Integer> getNeighIndex(int i, int width, int height,
                                               boolean connected8)
  {
    int[] coord = indexToCoord(i, width, height);

    return getNeighIndex(coord[0], coord[1], width, height, connected8);
  }

  // ===========================================================================
  //      Conversion from 255 grey level to others min and max value
  // ===========================================================================
  /**
   * Convert a grey level from [0, 255] scale to [min, max] scale.
   *
   * @param n grey level.
   * @param max maximum of new scale.
   * @param min minimum of new scale.
   * @return new grey level in new scale.
   */
  public static double grey255ToMax(double n, double max, double min)
  {
    return (n / 255.) * (max - min) + min;
  }

  /**
   * Convert a grey level from [min, max] scale to [0, 255] scale.
   *
   * @param n grey level.
   * @param max maximum of old scale.
   * @param min minimum of old scale.
   * @return new grey level in new scale [0, 255].
   */
  public static double maxTogrey255(double n, double max, double min)
  {
    return ((n - min) / (max - min)) * 255;
  }

  // ===========================================================================
  //             Distance between 2 points
  // ===========================================================================
  /**
   * Squared distance between two points using their coordinates. This method
   * check that points are in the corresponding image of size width * height.
   *
   * @param x1 x of point 1.
   * @param x2 x of point 2.
   * @param y1 y of point 1.
   * @param y2 y of point 2.
   * @param width x max.
   * @param height y max.
   * @return squared distance.
   */
  public static double distance(int x1, int x2, int y1, int y2, int width,
                                int height)
  {
    if (isValidX(x1, width) && isValidX(x2, width) && isValidY(y1, height)
        && isValidY(y2, height))
    {
      return ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
    else
    {
      throw (new ArrayIndexOutOfBoundsException());
    }
  }

  /**
   * Squared distance between two points using their indexes. This method check
   * that points are in the corresponding image of size width * height.
   *
   * @param i1 index of point 1.
   * @param i2 index of point 2.
   * @param width x max.
   * @param height y max.
   * @return squared distance.
   */
  public static double distanceIndex(int i1, int i2, int width, int height)
  {
    int[] c1 = indexToCoord(i1, width, height);
    int[] c2 = indexToCoord(i2, width, height);
    return distance(c1[0], c2[0], c1[1], c2[1], width, height);
  }

  /**
   * Squared distanceIndex between two points. This method doesn't check that
   * points are in the corresponding image of size width * height.
   *
   * @param x1 x of point 1.
   * @param x2 x of point 2.
   * @param y1 y of point 1.
   * @param y2 y of point 2.
   * @return squared distance.
   */
  public static double distance(int x1, int x2, int y1, int y2)
  {
    return distance(x1, x2, y1, y2, Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  // ===========================================================================
  //             Conversion between ShapeSet and int[]
  // ===========================================================================
  /**
   * Convert a ShapeSet to an array of sortedLabels.
   *
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return corresponding Array (int[]).
   */
  public static int[] ShapeSetToLabels(ShapeSet orig, int width, int height,
                                       int background_label)
  {
    int[] res = new int[width * height];

    for (int w = 0; w < width; w++)
    {
      for (int h = 0; h < height; h++)
      {
        res[Utils.coordToIndex(w, h, width, height)] = background_label;
      }
    }

    for (Map.Entry<Integer, ShapeFbt> entry : orig.getList().entrySet())
    {
      ShapeFbt currShape = entry.getValue();
      int label = entry.getKey();

      Iterator itr = currShape.iterator();

      while (itr.hasNext())
      {
        Point curr = (Point) itr.next();
        res[Utils.coordToIndex(curr.x, curr.y, width, height)] = label;
      }
    }

    return res;
  }

  /**
   * Convert an array of sortedLabels to a shapeSet.
   *
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return corresponding ShapeSet.
   */
  public static ShapeSet LabelsToShapeSet(int[] orig, int width, int height,
                                          int background_label,
                                          double _minBlobArea,
                                          double _minCellArea,
                                          double _maxCellWidth,
                                          double _minBorder,
                                          int _maxBlobsPerCell)
  {
    ShapeSet res = new ShapeSet(_minBlobArea, _minCellArea, _maxCellWidth,
                                _minBorder, _maxBlobsPerCell);

    for (int w = 0; w < width; w++)
    {
      for (int h = 0; h < height; h++)
      {
        int lab = orig[Utils.coordToIndex(w, h, width, height)];

        if (lab != background_label)
        {
          res.addPoint(lab, new Point(w, h));
        }
      }
    }

    return res;
  }

  /**
   * Convert an array of sortedLabels to a shapeSet.
   *
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @return corresponding ShapeSet.
   */
  public static ShapeSet LabelsToShapeSet(int[] orig, int width, int height,
                                          int background_label,
                                          Fluo_Bac_Tracker fluobt)
  {
    int minBlobArea = (int) (fluobt.getMinBlobArea() / (fluobt.getZoom()
                                                        * fluobt.getZoom()));
    int minCellArea = (int) (fluobt.getMinArea() / (fluobt.getZoom() * fluobt.
                                                    getZoom()));
    double minBorder = fluobt.getMinBlobBorder() / fluobt.getZoom();
    double maxWidth = fluobt.getMaxWidth() / fluobt.getZoom();

    return LabelsToShapeSet(orig, width, height, background_label,
                            minBlobArea, minCellArea,
                            maxWidth, minBorder, fluobt.getMaxNbBlobs());
  }

  // ===========================================================================
  //               Sort ImageStack by sortedLabels names
  // ===========================================================================
  /**
   * If labels are numeroted, sort stack by labels numbers.
   *
   * @param origin
   * @return
   */
  public static ImageStack sortByLabels(ImageStack origin)
  {
    // ================ Get original stack size and labels ====================
    int size = origin.getSize();
    String[] labels = origin.getSliceLabels();

    // ================ Init an HashMap of slice numerotation =================
    //  Key is numerotation of the slice, value is its position in the stack
    HashMap<Integer, Integer> numbers = new HashMap<Integer, Integer>(size);

    // =============== init an array of integers ==============================
    // that will contain sorted numerotation of slices 
    int[] sortedNumbers = new int[size];

    // ============= For each slice of origin stack ===========================
    // 1 ) get slice label
    // 2 ) compute slice numerotation from slice label ( number finishing the slice)
    //     if no numerotation, return origin.
    // 3 ) Update numbers and sortedNumbers 
    for (int i = 0; i < size; i++)
    {
      // ------------------ 1 ) get slice label -------------------------------
      String currLabel = labels[i];

      // ----------- 2 ) compute slice numerotation ---------------------------
      // from slice label ( number finishing the slice) 
      int nb;
      try
      {
        nb = Integer.parseInt(currLabel.replaceFirst("^.*\\D", ""));
      }
      // if no numerotation, return origin.
      catch (NumberFormatException ex)
      {
        System.out.println(
            "Warning in Utils.sortByLabels : labels are not numeroted.");
        return origin;
      }

      // ---------------- 3 ) Update numbers and sortedNumbers ---------------- 
      numbers.put(nb, i);
      sortedNumbers[ i] = nb;
    }

    // =========== Sort sortedNumbers array ===================================
    Arrays.sort(sortedNumbers);

    // ========= init sorted stack ============================================
    ImageStack sortedStack = new ImageStack(origin.getWidth(), origin.
        getHeight(), size);

    // =========== Add all slices of origin stack in sortedStack ==============
    //             at their sorted position
    for (int i = 0; i < size; i++)
    {
      int nb = sortedNumbers[i];
      int pos = numbers.get(nb);

      sortedStack.addSlice(origin.getProcessor(pos + 1));
      sortedStack.setSliceLabel(labels[ pos], i + 1);
    }

    // ================ Return sorted stack ===================================
    return sortedStack;
  }

  // ===========================================================================
  //               Open ImageStack with decided numbr of slices
  // ===========================================================================
  public static ImagePlus openImages(Path path, String title, int size)
  {
    ImagePlus IP = FolderOpener.open(path.toString());

    if (IP == null)
    {
      System.out.println(
          "Warning in Utils.openImages : coulnd't load images returned null");
      return null;
    }

    IP.setTitle(title);
    ImageStack loaded = IP.getImageStack();

    // ===== if the loaded stack is bigger than ask, raise an exception =======
    if (loaded.getSize() > size)
    {
      System.out.println(
          "Error in Utils.openImages : More slices than asked size. Returned null.");
      return null;
    }

    // =========== Else replace missing slices with null =====================
    else if (loaded.getSize() < size)
    {
      int nb = 0;

      // ----- insert null processors inside loaded stack if needed -----------
      int loadedSize = loaded.getSize();
      int added = 0;
      ImageProcessor nullProc = new ShortProcessor(loaded.getWidth(), loaded.
          getHeight());

      for (int i = 1; i <= loadedSize; i++)
      {
        String name = loaded.getSliceLabel(i);
        nb = Integer.parseInt(name.replaceFirst("^.*\\D", ""));

        for (int j = i + added; j < nb; j++)
        {
          loaded.addSlice("slice_" + i, nullProc, i - 1);
          added++;
        }
      }

      // --- insert null processors at the end of loaded stack if needed ------
      int init = loaded.getSize() + 1;
      for (int i = init; i <= size; i++)
      {
        loaded.addSlice("slice_" + i, nullProc, i - 1);
      }
    }

    return IP;

  }

  // ===========================================================================
  //               Get ImagePlus from ShapeSet path
  // ===========================================================================
  public static ImagePlus ImagePlusFromShapeSets(int width, int height,
                                                 String title, Path path,
                                                 BlobsDrawOpt drawOpt)
  {
    // ---------- Get all files in the blobs path ----------
    File[] list = path.toFile().listFiles();

    // ---------- For each of these files ----------
    // check if they contains the title given and add them to the stack in right order.
    // --- Init image stack ---
    ImageStack IS = new ImageStack(width, height);

    // --- Init list of names of files to add to the stack ---
    ArrayList<String> nameList = new ArrayList<String>();
    for (File currFile : list)
    {
      String name = currFile.getName();
      if (name.contains(title + "_"))
      {
        nameList.add(name);
      }
    }

    if (nameList.isEmpty())
    {
      return new ImagePlus();
    }

    // --- For each name of path to add load corresponding shape set and add it to stack ---
    boolean first = true;

    for (String filename : nameList)
    {
      // Load shapeSet
      Path currPath = path.resolve(filename);

      ShapeSet shapeS;
      try
      {
        shapeS = (ShapeSet) Utils.loadObject(currPath.toString());
        
        if( shapeS == null )
        {
          return null;
        }

        // Get corresponding ColorProcessor
        ColorProcessor CP = null;
        switch (drawOpt)
        {
          case BLOBS:
            CP = shapeS.getColorProcessor(width, height);
            break;
          case GRAPH:
            CP = shapeS.getColorProcessorGraph(width, height);
            break;
          case CELLS:
            CP = shapeS.getColorProcessorCells(width, height);
            break;

        }

        // If it is the first image processor we add to the stack
        // we first update Stack so it correspond to imageprocessor width and height 
        if (first)
        {
          IS.update(CP);
          first = false;
        }

        // Add the color processor to the stack.
        IS.addSlice(filename, CP);

      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        return null;
      }

    }

    // ------- Sort stack  with labels ------------
    IS = Utils.sortByLabels(IS);

    ImagePlus IP = new ImagePlus(title, IS);
    return IP;
  }

  public static ImagePlus ImagePlusFromShapeSets(int width, int height,
                                                 String title, Path path,
                                                 int size,
                                                 BlobsDrawOpt drawOpt)
  {
    ImagePlus img = ImagePlusFromShapeSets(width, height, title, path, drawOpt);
    ImageStack loaded = img.getImageStack();

    int loadedSize = loaded.getSize();

    if (loadedSize == 0)
    {
      System.out.println(
          "Warning  in Utils.ImagePlusFromShapeSet : empty stack.");
      return null;
    }
    if (loadedSize == size)
    {
      return img;
    }
    else if (loadedSize > size)
    {
      System.out.println(
          "Error in Utils.ImagePlusFromShapeSet : More shapeSets than asked size.");
      return null;
    }
    else
    {
      ImageStack stack = new ImageStack(loaded.getWidth(), loaded.getHeight());

      // ====== Add all slices in the StackFbt ====== 
      for (int i = 1; i < loadedSize + 1; i++)
      {
        stack.addSlice(loaded.getProcessor(i));
      }

      int nb = 0;

      for (int i = 1; i <= loadedSize; i++)
      {
        String name = loaded.getSliceLabel(i);
        nb = Integer.parseInt(name.replaceFirst("^.*\\D", ""));

        for (int j = i; j < nb; j++)
        {
          ColorProcessor proc = new ColorProcessor(width, height);
          stack.addSlice("slice_" + i, proc, i - 1);
        }
      }

      for (int i = nb + 1; i <= size; i++)
      {
        ColorProcessor proc = new ColorProcessor(width, height);
        stack.addSlice("slice_" + i, proc, i - 1);
      }

      return new ImagePlus(title, stack);

    }

  }

  // ===========================================================================
  //             Conversion between int[] and ByteProcessor
  // ===========================================================================
  /**
   * Automatically generate an array of 'n' colors.
   *
   * @param n number of colors.
   * @return an array of 'n' colors.
   */
  public static Color[] generateColors(int n)
  {
    Color[] cols = new Color[n];
    for (int i = 0; i < n; i++)
    {
      cols[i] = Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
    }

    Collections.shuffle(Arrays.asList(cols), rand);
    return cols;
  }

  /**
   * Convert an array of sortedLabels corresponding to an image to a
   * ColorProcessor. Each label will be fill in the ColorProcessor by a
   * different color.
   *
   * @param orig array of sortedLabels.
   * @param width corresponding image width.
   * @param height corresponding image height.
   * @param background_label background label to be set in black.
   * @return corresponding colorProcessor.
   */
  public static ColorProcessor LabelsToImageProc(int[] orig, int width,
                                                 int height,
                                                 int background_label)
  {
    ColorProcessor proc = new ColorProcessor(width, height);

    int max = orig[0];
    for (int i : orig)
    {
      if (max < i)
      {
        max = i;
      }
    }
    Color[] cols = generateColors(max + 1);

    int nbpix = width * height;

    for (int i = 0; i < nbpix; i++)
    {
      int[] coord = indexToCoord(i, width, height);

      if (orig[i] == background_label)
      {
        proc.setColor(Color.BLACK);
        proc.drawPixel(coord[0], coord[1]);
      }
      else
      {
        proc.setColor(cols[orig[i]]);
        proc.drawPixel(coord[0], coord[1]);
      }
    }

    return proc;
  }

  // ===========================================================================
  //             save and load objects
  // ===========================================================================
  /**
   * Save any serializable object in a file.
   *
   * @param obj serializable object.
   * @param file saving file.
   */
  public static void saveObject(Object obj, String file)
  {
    try
    {
      FileOutputStream fos = new FileOutputStream(file);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(obj);
      oos.flush();
      oos.close();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

  }

  /**
   * Load an object from a file.
   *
   * @param file loading file.
   * @return loaded object.
   */
  public static Object loadObject(String file)
  {
    try
    {
      Object read;
      FileInputStream fis = new FileInputStream(file);
      ObjectInputStream ois = new ObjectInputStream(fis);
      read = ois.readObject();

      ois.close();
      return read;
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return null;
    }
  }

  // ===========================================================================
  //             Clear or delete a path
  // ===========================================================================
  /**
   * Delete all files and subdirectories in a directory.
   *
   * @param pathFile directory to clear.
   */
  public static void clearPath(File pathFile)
  {
    // ============== For each file or directory in the path ==================
    // 1) Clear it if it's a directory
    // 2) Delete it
    File[] list = pathFile.listFiles();
    for (int i = 0; i < list.length; i++)
    {
      File currFile = list[i];

      // 1) Clear it if it's a directory
      if (currFile.isDirectory())
      {
        clearPath(currFile);
      }

      // 2) Delete it
      currFile.delete();
    }
  }

  /**
   * Delete a directory.
   *
   * @param pathFile Directory to delete.
   */
  public static void deletePath(File pathFile)
  {
    clearPath(pathFile);
    pathFile.delete();
  }

  // ===========================================================================
  //             Copy a directory
  // ===========================================================================
  /**
   * Copy all files and subdirectories from a path to another.
   *
   * @param orig
   * @param target
   */
  public static void copyDir(Path orig, Path target)
  {
    // ================ Check that orig directory exists ======================
    // If not raise an exception
    if (Files.exists(orig))
    {

      if (!Files.exists(target))
      {
        try
        {
          Files.createDirectories(target);
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
          return;
        }
      }

      File[] origList = orig.toFile().listFiles();
      for (File currFile : origList)
      {
        if (currFile.isDirectory())
        {
          copyDir(currFile.toPath(), target.resolve(currFile.getName()));
        }
        else
        {
          try
          {
            Files.copy(currFile.toPath(), target.resolve(currFile.getName()),
                       StandardCopyOption.REPLACE_EXISTING);
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
            return;
          }
        }
      }
    }
    //=============== If orig directory doesn't exists raise an exception. =====
    else
    {
      System.out.println(
          "Error in Utils.CopyDir : Couldn't copy directory - original directory doesn't exists.");
    }
  }

  // ===========================================================================
  //             Other methods with directories
  // ===========================================================================
  public static boolean isEmpty(Path dir) throws NotDirectoryException
  {
    if (Files.isDirectory(dir))
    {
      if (dir.toFile().list().length == 0)
      {
        return true;
      }
      else
      {
        return false;
      }
    }

    throw new NotDirectoryException(dir + " is not a directory.");
  }

  // ===========================================================================
  //               Probalbilities laws
  // ===========================================================================
  /**
   * Gaussian probability law.
   *
   */
  public static double gauss(double x, double mean, double dev)
  {
    return (1 / (dev * Math.sqrt(2 * Math.PI))) * Math.exp(-(x - mean) * (x
                                                                          - mean)
                                                           / (2 * dev * dev));
  }

  /**
   * Laplace probability law.
   */
  public static double laplace(double x, double mean, double scale)
  {
    return (1 / (2 * scale)) * Math.exp(-Math.abs(x - mean) / scale);
  }

  /**
   * Exponential probability law.
   *
   * @param x
   * @param lambda exponential lawx parameter.
   * @return probability
   */
  public static double exponential(double x, double lambda)
  {
    if (x >= 0)
    {
      return lambda * Math.exp(-lambda * x);
    }
    else
    {
      return 0;
    }
  }

  // ==========================================================================
  //                  Mixing color processors 
  // ==========================================================================
  public static ColorProcessor MixColorProcessors(ColorProcessor Cproc1,
                                                  ColorProcessor Cproc2)
  {
    // =============== Init variables =========================================
    int width = Cproc1.getWidth();
    int height = Cproc1.getHeight();
    int nbPix = width * height;

    if (width != Cproc2.getWidth() || height != Cproc2.getHeight())
    {
      System.out.println(
          "Error in Utils.MixColorProcessors : processors don't have the same dimensions.");
      return null;
    }

    ColorProcessor newCP = new ColorProcessor(width, height);

    // ========== For each color channel of these color processors ============
    //    a ) Get color channel float processors.
    //    b ) Merge normalized and blobs processor for this channel in a new float processor.
    //    c ) Add the merged float processor to a new color processor
    for (int channel = 0; channel < Cproc2.getNChannels(); channel++)
    {
      //    a ) Get color channel float processors.
      FloatProcessor blobsFP = Cproc1.toFloat(channel, null);
      FloatProcessor renormFP = Cproc2.toFloat(channel, null);
      FloatProcessor newFP = new FloatProcessor(width, height);

      float[] blobsPixels = (float[]) blobsFP.getPixels();
      float[] renormPixels = (float[]) renormFP.getPixels();
      float[] newPixels = new float[nbPix];

      //    b ) Merge normalized and blobs processor for this channel in a new float processor.
      for (int pixIndex = 0; pixIndex < nbPix; pixIndex++)
      {
        float weight1 = .6f;
        float weight2 = 1.f - weight1;
        newPixels[pixIndex] = weight1 * blobsPixels[pixIndex] + weight2
                                                                * renormPixels[pixIndex];
      }

      //    c ) Add the merged float processor to a new color processor
      newFP.setPixels(newPixels);
      newCP.setPixels(channel, newFP);
    }

    // ============== return newCP color processor ============================
    return newCP;
  }

  public static ImageStack MixStacks(ImageStack stack1, ImageStack stack2)
  {
    if( stack1 == null || stack2 == null )
    {
      return null;
    }

    // --- Mixed Images and blobs or seeds Stack ---
    //      to get final toShow ImagePLus
    int size = stack1.getSize();
    int width = stack1.getWidth();
    int height = stack1.getHeight();

    if (size != stack2.getSize() || width != stack2.getWidth() || height
                                                                  != stack2.
        getHeight())
    {
      System.err.println(
          "Error in Utils.MixStacks, stacks don't have same dimensions : returns null.");
      return null;
    }

    ImageStack colorStack = new ImageStack(width, height, size);
    for (int i = 1; i <= size; i++)
    {
      ImageProcessor iProc = Utils.MixColorProcessors(
          (ColorProcessor)stack1.getProcessor(i).convertToRGB(),
          (ColorProcessor)stack2.getProcessor(i).convertToRGB());

      colorStack.addSlice(iProc);
    }

    return colorStack;
  }

  // ==========================================================================
  //               Angles operations
  // ==========================================================================
  /**
   * Normalizes an angle so it is between PI and -PI.
   *
   * @param theta angle to normalize
   * @return
   */
  public static double normalizedAngle(double theta)
  {
    double res = theta;

    while (res >= Math.PI)
    {
      res -= 2 * Math.PI;
    }
    while (res < -Math.PI)
    {
      res += 2 * Math.PI;
    }

    return res;
  }

  /**
   * Computes mean angle of two given angles.
   *
   * @param theta1
   * @param theta2
   * @return
   */
  public static double meanAngles(double theta1, double theta2)
  {
    if (Math.abs(theta1 - theta2) <= Math.PI)
    {
      return (theta1 + theta2) / 2;
    }
    else if (theta1 < theta2)
    {
      theta1 = theta1 + 2 * Math.PI;
    }
    else if (theta1 > theta2)
    {
      theta1 = theta1 - 2 * Math.PI;
    }
    return (theta1 + theta2) / 2;
  }

  // ==========================================================================
  //               Thread interruption management
  // ==========================================================================
  /**
   * Checks if thread is interrupted : if it is interrupted, sleep so that
   * Thread will be able to raise the InterruptedException.
   *
   * @throws InterruptedException
   */
  public static void checkThreadInterruption() throws InterruptedException
  {
    if (Thread.currentThread().isInterrupted())
    {
      Thread.sleep(0);
    }
  }

  // ==========================================================================
  //               Start a second jvm
  // ==========================================================================
//  public static void startSecondJVM(String javaPath, String javaClassPath,
//                                    String newMainClassName) throws IOException,
//                                                                    InterruptedException
//  {
//    ProcessBuilder processBuilder = new ProcessBuilder(javaPath, "-cp", 
//                                                       javaClassPath,
//                                                       newMainClassName);
//    Process process = processBuilder.start();
//    process.waitFor();
//
//    try
//    { 
//      Runtime r = Runtime.getRuntime();
//      Process shell = r.exec(cmd);
//
//      final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(shell.
//          getOutputStream()));
//      final BufferedReader br = new BufferedReader(new InputStreamReader(shell.
//          getInputStream()));
//
//// lecture clavier
//      listener = new Thread()
//      {
//        public void run()
//        {
//          BufferedReader in = new BufferedReader(
//              new InputStreamReader(System.in));
//          String s = "";
//
//          while (listener == Thread.currentThread())
//          {
//            try
//            {
//              s = in.readLine();
//              bw.write(s + "n");
//              bw.flush();
//            }
//            catch (Exception ex)
//            {
//              ex.printStackTrace();
//            }
//          }
//        }
//      };
//      listener.start();
//
//// lecture resultat commande
//      while ((strinp = br.readLine()) != null)
//      {
//        System.out.println(strinp);
//      }
//
//      bw.close();
//      br.close();
//    }
//    catch (Exception ex)
//    {
//      ex.printStackTrace();
//    }
//
//  }
//
//  public static void startSecondJVM(String newMainClassName) throws IOException,
//                                                                    InterruptedException
//  {
//    String separator = System.getProperty("file.separator");
//    String classpath = System.getProperty("java.class.path");
//    String path = System.getProperty("java.home")
//                  + separator + "bin" + separator + "java";
//    startSecondJVM(path, classpath, newMainClassName);
//  }
}
