/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Image;

import ij.*;
import ij.io.*;
import ij.process.*;

import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import cellst.Main.*;

/**
 * ImageStack containing several ImageFbt. This class contains the whole
 * sequence of bacteria images.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class StackFbt extends ImageStack
{

  // =======================================================================
  //                             Attributes
  // =======================================================================
  /**
   * Suffix to add to filename to get the file where to save backgrounds.
   */
  public static final String BACKGROUND_FILENAME = "background_";
  /**
   * Background masks list ( 1 if in background, 0 if not).
   */
  protected ArrayList<int[]> background = new ArrayList<int[]>();
  /**
   * Static background label.
   */
  static final int BACKGROUND_LABEL = -1;

  // =======================================================================
  //                                 Enums
  // =======================================================================
  // =======================================================================
  //                               Constructors
  // =======================================================================
  /**
   * StackFbt constructor. Inherited from ImageStack.
   */
  public StackFbt()
  {
    super();
  }

  /**
   * StackFbt constructor. Inherited from ImageStack.
   *
   * @param width images width.
   * @param height images height.
   */
  public StackFbt(int width, int height)
  {
    super(width, height);
  }

  /**
   * StackFbt constructor. Inherited from ImageStack.
   *
   * @param width images width.
   * @param height images height.
   * @param cm color model of the images.
   */
  public StackFbt(int width, int height, ColorModel cm)
  {
    super(width, height, cm);
  }

  /**
   * StackFbt constructor. Inherited from ImageStack.
   *
   * @param width images width.
   * @param height images height.
   * @param size number of slices.
   */
  public StackFbt(int width, int height, int size)
  {
    super(width, height, size);
  }

  // =======================================================================
  //                            Accessors: getters
  // =======================================================================
  /**
   * ImageFbt getter.
   *
   * @param n slice number of the ImageFbt wanted.
   * @return ImageFbt contained in the nth slice of the StackFbt.
   */
  public ImageFbt getImageFbt(int n)
  {
    ImageFbt ip;

    if (n < 1 || n > getSize())
    {
      throw new IllegalArgumentException("Slice index out of range : " + n);
    }

    if (getSize() == 0)
    {
      return null;
    }

    if (!background.isEmpty())
    {
      ip = new ImageFbt(getWidth(), getHeight(), background.get(n - 1));
    }
    else
    {
      ip = new ImageFbt(getWidth(), getHeight(), (int[]) null);
    }

    ip.setPixels(getPixels(n));

    return ip;
  }

  /**
   * Backgrounds getter.
   *
   * @return an ArrayList of backgrounds masks (int[]) of each slice.
   */
  public ArrayList<int[]> getBackground()
  {
    return background;
  }

  // =======================================================================
  //                            Accessors: setters
  // =======================================================================
  /**
   * Backgrounds setter. Replace each background of each slice with the given
   * backgrounds list.
   *
   * @param _back new backgrounds list.
   */
  public void setBackground(ArrayList<int[]> _back)
  {
    background = _back;
  }

  // =======================================================================
  //                              Public Methods
  // =======================================================================
  // -----------------------------------------------------------------------
  //                    Create a Stack Static methods
  // -----------------------------------------------------------------------
  /**
   * Creates a StackFbt from an ImageStack.
   *
   * @param orig
   * @return
   */
  public static StackFbt fromImageStack(ImageStack orig)
  {
    boolean isStackFbt = orig instanceof StackFbt;

    // ===== herited constructor =====
    StackFbt newStack = new StackFbt(orig.getWidth(), orig.getHeight());

    // ====== Add all slices in the StackFbt ====== 
    int size = orig.getSize();
    for (int i = 1; i < size + 1; i++)
    {
      newStack.addSlice(orig.getSliceLabel(i),
                        new ImageFbt(orig.getProcessor(i)));
    }

    if (isStackFbt)
    {
      newStack.background = ((StackFbt) orig).getBackground();
    }

    return newStack;
  }

  /**
   * Create an StackFbt from a path, basename and suffix (tiff, rim, jpg ...).
   * This static method will search all files in path containing the basename
   * and finishing by the suffix and add it to the stack.
   *
   * @param path path where to search images.
   * @param basename files must contain basename in their title.
   * @param suffix files must finish by the suffix.
   * @return the new StackFbt.
   */
  public static StackFbt open(String path, String basename, String suffix)
  {
    // ====== Open files in the given path ====== 
    File[] files = new File(path).listFiles();

    // ====== List of files to add to the stack ====== 
    ArrayList<String> paths = new ArrayList<String>();
    for (File file : files)
    {
      String name = file.getName();
      if (name.contains(basename) && name.endsWith(suffix))
      {
        paths.add(file.getPath());
      }
    }

    // ====== If no files corresponds, raise an error ====== 
    if (paths.isEmpty())
    {
      System.out.println("No corresponding images");
      return null;
    }

    // ====== Get width and height of images ====== 
    ImagePlus firstIP = new ImagePlus(paths.get(0));
    int width = firstIP.getWidth();
    int height = firstIP.getHeight();

    // ====== Init result StackFbt ====== 
    StackFbt res = new StackFbt(width, height);

    // ====== Add each file to our Stack ====== 
    for (String currPath : paths)
    {

      ImageFbt iFbt = new ImageFbt(currPath);

      String name = currPath.substring(currPath.lastIndexOf("/") + 1);
      name = name.substring(1, name.lastIndexOf("."));

      res.addSlice(name, iFbt);
    }

    // ==================  sort stack =========================================
    res = res.sort();

    // ====== Return StackFbt ====== 
    return res;
  }

  /**
   * Create an StackFbt from a path and basename. This static method will search
   * all files in path containing the basename and add it to the stack.
   *
   * @param path path where to search images.
   * @param basename files must contain basename in their title.
   * @return the new StackFbt.
   */
  public static StackFbt open(String path, String basename)
  {
    return open(path, basename, "");
  }

  /**
   * Create an StackFbt from a path. This static method will add all files in
   * path to the stack.
   *
   * @param path path where to search images.
   * @return the new StackFbt.
   */
  public static StackFbt open(String path)
  {
    return open(path, "", "");
  }

  public static StackFbt openStack(String file)
  {
    ImagePlus ImP = new ImagePlus(file);
    ImageStack imStack = ImP.getStack();
    StackFbt stack = StackFbt.fromImageStack(imStack);

    return stack;
  }

  // -----------------------------------------------------------------------
  //                   Save and  Load a Stack  methods
  // -----------------------------------------------------------------------
  /**
   * Save StackFbt in a file. In fact Stack is saved in a file and backgrounds
   * are saved in a second file with name file+BACKGROUND_FILENAME.
   *
   * @param file file where to save the stack.
   */
  public void save(String file)
  {
    ImagePlus IP = new ImagePlus("", this);

    FileSaver FS = new FileSaver(IP);

    byte[] img = FS.serialize();
    Utils.saveObject(img, file);

    String backfile = file + "_" + BACKGROUND_FILENAME;
    Utils.saveObject(background, backfile);
  }

  /**
   * Saves all StackFbt's slices and the background into 'path'.
   *
   * This allows to save several small files rather than one big file as in
   * save(String file) method.
   *
   * @param savingPath path where to save StackFbt
   */
  public void saveImages(Path savingPath)
  {

    // ===== If savingPath doesn't exists yet : create it =====================
    if (!Files.exists(savingPath))
    {
      try
      {
        Files.createDirectories(savingPath);
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        return;
      }
    }
    // ===== If savingPath exists but is not a directory raise an exception ===
    else if (!Files.isDirectory(savingPath))
    {
      System.out.println("Error in StackFbt.saveImages : " + savingPath
                         + " exists and is not a directory.");
    }

    // ==== Save each non null slice of the stack in 'savingPath' =============
    int size = getSize();
    for (int i = 1; i <= size; i++)
    {
      ImageFbt currImg = getImageFbt(i);

      if (currImg != null && currImg.getPixels() != null)
      {
        currImg.save(savingPath.resolve("slice_" + i));

//        // ================= Save background in 'savingPath' ==================
//        int[] back = currImg.getBackground();
//        String backfile = savingPath.resolve( BACKGROUND_FILENAME+ i ).toString();
//        Utils.saveObject( back, backfile );
      }
    }

  }

  /**
   * Load a stackFbt from a stack file. In fact Stack is loaded from a file and
   * backgrounds are loaded from a second file with name
   * file+BACKGROUND_FILENAME.
   *
   * @param file file where to load the stack.
   * @return the loaded StackFbt.
   */
  public static StackFbt load(String file)
  {
    byte[] img = (byte[]) Utils.loadObject(file);
    ImagePlus IP = new Opener().deserialize(img);

    ImageStack IS = IP.getStack();
    StackFbt res = StackFbt.fromImageStack(IS);

    String backfile = file + "_" + BACKGROUND_FILENAME;
    ArrayList<int[]> back = (ArrayList<int[]>) Utils.loadObject(backfile);

    res.setBackground(back);

    return res;
  }

  /**
   * Load a stackFbt from several imageFbt files and a background file generated
   * by saveImages method.
   *
   * @param loadingPath path to load the StackFbt.
   * @return
   */
  public static StackFbt loadImages(Path loadingPath)
  {
    // ====== Open files in the given path ====== 
    File[] files = loadingPath.toFile().listFiles();

    // ====== List of files to add to the stack ====== 
    ArrayList<String> paths = new ArrayList<String>();
    for (File file : files)
    {
      String name = file.getName();
      if (name.replaceAll("[0-9]", "").equals("slice_"))
      {
        paths.add(file.getPath());
      }
    }

    // ====== If no files corresponds, raise an error ====== 
    if (paths.isEmpty())
    {
      System.out.println("No corresponding images");
      return null;
    }

    // ====== Get width and height of images ====== 
    byte[] firstImgByte;
    try
    {
      firstImgByte = Files.readAllBytes(Paths.get(paths.get(0)));
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      return null;
    }

    ImagePlus firstIP = new Opener().deserialize(firstImgByte);
    int width = firstIP.getWidth();
    int height = firstIP.getHeight();

    // ====== Init result StackFbt ====== 
    StackFbt res = new StackFbt(width, height);
    res.background = new ArrayList<int[]>();

    // ====== Add each file to our Stack ====== 
    for (String currPath : paths)
    {

      ImageFbt iFbt = ImageFbt.load(Paths.get(currPath));

      String name = currPath.substring(currPath.lastIndexOf("/") + 1);

      res.addSlice(name, iFbt);
      res.background.set(res.getSize() - 1, iFbt.getBackground());

    }

    res = res.sort();

    // ====== Return StackFbt ====== 
    return res;
  }

  /**
   * Loads a StackFbt with 'size' number of slice, saved with saveImage. If all
   * the slices were not saved, add null ImageProcessor.
   *
   * @param loadingPath
   * @param size
   * @return
   */
  public static StackFbt loadImages(Path loadingPath, int size)
  {
    //==================== Load stackFbt without size constraint ==============
    StackFbt loaded = loadImages(loadingPath);

    if (loaded == null)
    {
      System.out.println("Warning in StackFbt.loadImages : No images.");
      return null;
    }

    // ===== if the loaded stack is bigger than ask, raise an exception =======
    if (loaded.getSize() > size)
    {
      System.out.println(
          "Error in StackFbt.loadImages : More slices than asked size.");
      return null;
    }

    // ==== if it is already the good size, return it ========================
    else if (loaded.getSize() == size)
    {
      return loaded;
    }

    // =========== Else replace missing slices with null =====================
    else
    {
      StackFbt res = StackFbt.fromImageStack(loaded);
      int nb = 0;

      // ----- insert null processors inside loaded stack if needed -----------
      int loadedSize = loaded.getSize();
      int added = 0;
      for (int i = 1; i <= loadedSize; i++)
      {
        String name = loaded.getSliceLabel(i);
        nb = Integer.parseInt(name.replaceFirst("^.*\\D", ""));

        for (int j = i + added; j < nb; j++)
        {
          res.addSlice("slice_" + i, null, i - 1);
          added++;
        }
      }

      // --- insert null processors at the end of loaded stack if needed ------
      for (int i = nb + 1; i <= size; i++)
      {
        res.addSlice("slice_" + i, null, i - 1);
      }

      return res;

    }

  }

  // -----------------------------------------------------------------------
  //                    Sort   method
  // -----------------------------------------------------------------------
  public StackFbt sort()
  {
    // ================ Get size and labels ==================================
    int size = getSize();
    String[] labels = getSliceLabels();

    // ================ Init an HashMap of slice numerotation =================
    //  Key is numerotation of the slice, value is its position in the stack
    HashMap<Integer, Integer> numbers = new HashMap<Integer, Integer>(size);

    // =============== init an array of integers ==============================
    // that will contain sorted numerotation of slices 
    int[] sortedNumbers = new int[size];

    // ============= For each slice of this stack =============================
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
      // if no numerotation, return null.
      catch (NumberFormatException ex)
      {
        System.out.println(
            "Warning in StackFbt.sort : labels are not numeroted.");
        return null;
      }

      // ---------------- 3 ) Update numbers and sortedNumbers ---------------- 
      numbers.put(nb, i);
      sortedNumbers[ i] = nb;
    }

    // =========== Sort sortedNumbers array ===================================
    Arrays.sort(sortedNumbers);

    // ========= init sorted stack ============================================
    StackFbt sortedStack = new StackFbt(getWidth(), getHeight(), size);
    sortedStack.background = new ArrayList<int[]>(size);

    // =========== Add all slices of stack in sortedStack =====================
    //             at their sorted position
    for (int i = 0; i < size; i++)
    {
      int nb = sortedNumbers[i];
      int pos = numbers.get(nb);

      sortedStack.addSlice(getProcessor(pos + 1));
      sortedStack.setSliceLabel(labels[ pos], i + 1);
      sortedStack.background.add(i, background.get(pos));
    }

    // ================ Return sorted stack ===================================
    return sortedStack;
  }

  // -----------------------------------------------------------------------
  //                    Show   method
  // -----------------------------------------------------------------------
  /**
   * Show an imagePlus construct from this stack.
   *
   * @param title title of the shown imagePlus.
   */
  public final void show(String title)
  {
    StackFbt st = StackFbt.fromImageStack(this);

    ImagePlus IP = new ImagePlus(title, st);
    IP.show();

  }

  // -----------------------------------------------------------------------
  //                   Convert grey levels
  // -----------------------------------------------------------------------
  /**
   * Convert n from [0-255] to [minValue()-maxValue()] scale.
   *
   * @param n grey level between 0 and 255.
   * @return corresponding pixel value between minValue and maxValue.
   */
  public double grey255ToMax(double n)
  {
    double max = 0;
    double min = 0;
    int nbSlice = getSize();

    for (int i = 1; i < nbSlice + 1; i++)
    {
      ImageProcessor Ip = getProcessor(i);
      max += Ip.maxValue();
      min += Ip.minValue();
    }
    max /= nbSlice;
    min /= nbSlice;

    return Utils.grey255ToMax(n, max, min);
  }

  /**
   * Convert n from [minValue()-maxValue()] to [0-255] scale
   *
   * @param n pixel value between minValue and maxValue.
   * @return corresponding grey level between 0 and 255.
   */
  public double maxTogrey255(double n)
  {
    double max = 0;
    double min = 0;
    int nbSlice = getSize();

    for (int i = 1; i < nbSlice + 1; i++)
    {
      ImageProcessor Ip = getProcessor(i);
      max += Ip.maxValue();
      min += Ip.minValue();
    }
    max /= nbSlice;
    min /= nbSlice;

    return Utils.maxTogrey255(n, max, min);
  }

  // -----------------------------------------------------------------------
  //                  Add and remove imageFbt to stack
  // -----------------------------------------------------------------------
  /**
   * Adds the image in 'ip' to the stack following slice 'n'. Adds the slice to
   * the beginning of the stack if 'n' is zero. If the 'ip' is not an ImageFbt,
   * nothing is done.
   *
   * @param label label of the slice to be created.
   * @param imp ImageProcessor to add ( must be an ImageFbt ).
   * @param n slice number after which new slice is added.
   */
  @Override
  public void addSlice(String label, ImageProcessor imp, int n)
  {
    if (imp instanceof ImageFbt)
    {
      super.addSlice(label, imp, n);
    }
    else if (imp == null)
    {
      imp = new ImageFbt(getWidth(), getHeight(), (int[]) null);

      super.addSlice(label, imp, n);
    }
    else
    {
      throw new IllegalArgumentException(
          "Error : Not null nor an imageFbt, ImageProcessor not added");
    }
  }

  /**
   * Adds the image in 'ip' to the stack following slice 'n'., setting the
   * string 'sliceLabel' as the slice metadata. If the 'ip' is not an ImageFbt,
   * nothing is done.
   *
   * @param label of the slice to be created.
   * @param imp ImageProcessor to add ( must be an ImageFbt ).
   */
  @Override
  public void addSlice(String label, ImageProcessor imp)
  {
    if (imp instanceof ImageFbt)
    {
      super.addSlice(label, imp);

      background.add(((ImageFbt) imp).background);
    }
    else if (imp == null)
    {
      imp = new ImageFbt(getWidth(), getHeight(), (int[]) null);

      super.addSlice(label, imp);
      background.add((int[]) null);
    }
    else
    {
      throw new IllegalArgumentException(
          "Error : Not null nor an imageFbt, ImageProcessor not added");
    }
  }

  /**
   * Adds the image in 'ip' to the end of the stack. If the 'ip' is not an
   * ImageFbt, nothing is done.
   *
   * @param imp ImageProcessor to add ( must be an ImageFbt ).
   */
  @Override
  public void addSlice(ImageProcessor imp)
  {
    String label = "slice " + (getSize() + 1);
    addSlice(label, imp);
  }

  /**
   * Adds the image in 'ip' to the end of the stack, setting the string
   * 'sliceLabel' as the slice metadata. If the 'ip' is not an ImageFbt, nothing
   * is done. Specified background is add as ImageFbt background.
   *
   * @param label of the slice to be created.
   * @param imp ImageProcessor to add ( must be an ImageFbt ).
   * @param n slice number after which new slice is added.
   * @param _back background of the ImageFbt to add.
   */
  public void addSlice(String label, ImageProcessor imp, int n, int[] _back)
  {
    addSlice(label, imp, n);
    background.set(n, _back);
  }

  /**
   * Deletes the specified slice, were n in [ 1, nslices ].
   *
   * @param n number of slice to remove.
   */
  @Override
  public void deleteSlice(int n)
  {
    super.deleteSlice(n);

    background.remove(n);
  }

  // -----------------------------------------------------------------------
  //                     Images treatement
  // -----------------------------------------------------------------------
  /**
   * Resize StackFbt and return result. Warning, this doesn't copy and resize
   * backgrounds.
   *
   * @param newW new width.
   * @param newH new height.
   * @return new resized StackFbt.
   */
  public StackFbt resize(int newW, int newH)
  {
    StackFbt res = new StackFbt(newW, newH);

    for (int i = 1; i < getSize() + 1; i++)
    {
      ImageFbt im = getImageFbt(i);
      ImageProcessor ip = new ImageFbt(im.resize(newW, newH));
      res.addSlice(ip);
    }

    return res;
  }

  /**
   * Compute for all slices a rough background. First images are binarized with
   * the threshold. Then they are opened and dilated with radius 5, 5 times.
   * This gives black and white images with interesting parts in white and
   * background in black. From this images we compute the background masks and
   * return them.
   *
   * @param thres binarization threshold.
   * @param radius
   *
   *
   * @return An arrayList of each slice background mask ( array of int 1 if in
   * background, 0 elsewhere ).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> roughBackgroundMasks(int thres, double radius) throws
      InterruptedException
  {
    ArrayList<int[]> res = new ArrayList<int[]>();

    // prB.setTitle( "Computing rough background masks." );
    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {
      ImageFbt IFbt = this.getImageFbt(i);
      res.add(IFbt.roughBackgroundMask(thres, radius));
      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

    return res;
  }

  /**
   * Compute for all slices a rough backgroun with autoThreshold. The threshold
   * to limit the component is taken from the "Minimum dark" method of imageJ.
   * First images are binarized with the threshold. Then they are opened and
   * dilated with radius 5, 5 times. This gives black and white images with
   * interesting parts in white and background in black. From this images we
   * compute the background masks and return them.
   *
   * @param radius
   * @return An arrayList of each slice background mask ( array of int 1 if in
   * background, 0 elsewhere ).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> roughBackgroundMasks(double radius) throws
      InterruptedException
  {
    ImageFbt iFbt = getImageFbt(1);
    iFbt.setAutoThreshold("Triangle dark");
    int thres = (int) iFbt.getMinThreshold();

    return roughBackgroundMasks(thres, radius /*, prB, maxPerc */);
  }

  /**
   * Denoising all ImageFbt of the StackFbt. Coded by Lionel Moisan.
   *
   * @param sem
   * @param l
   * @param r
   * @param n
   * @param s
   * @param d
   * @param eps
   * @param g
   *
   *
   *
   * @return the denoised StackFbt.
   * @throws java.lang.InterruptedException Exception thrown if current thread
   * is stopped or an error occurred in denoising.
   */
  public StackFbt denoise(Semaphore sem, double l, double r, int n, int s, int d,
                          double eps, int g) throws InterruptedException
  {
    StackFbt res = StackFbt.fromImageStack(this);

    int size = res.getSize();
    for (int i = 1; i < size + 1; i++)
    {
      ImageFbt IFbt = this.getImageFbt(i);
      IFbt.gtv_means_denoise(sem, l, r, n, s, d, eps, g);
      res.setPixels(IFbt.getPixels(), i);
      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

    return res;
  }

  /**
   * Compute all final backgrounds as the bordering connected component in the
   * image. A black border is added to each image. From the top left corner, we
   * dilate the background over each image. Dilatation is stopped by pixels with
   * value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param threshold grey level limit of the background component. (it is the
   * grey level of the lighter area around the cells).
   *
   *
   *
   * @return an ArrayList of each slice background masks (array of int 1 if in
   * background, 0 elsewhere).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> backgroundMasks(boolean connected8, int threshold)
      throws InterruptedException
  {
    ArrayList<int[]> res = new ArrayList<int[]>();

    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {
      ImageFbt IFbt = getImageFbt(i);
      res.add(IFbt.
          backgroundMask(connected8, threshold /*, prB, maxPerc / ( double ) size */));
      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

    return res;
  }

  /**
   * Compute all final backgrounds as the bordering connected component in the
   * image with autoThreshold. The threshold to limit the component is taken
   * from the "Minimum dark" method of imageJ. A black border is added to each
   * image. From the top left corner, we dilate the background over each image.
   * Dilatation is stopped by pixels with value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   *
   *
   *
   * @return an ArrayList of each slice background masks (array of int 1 if in
   * background, 0 elsewhere).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> backgroundMasks(boolean connected8) throws
      InterruptedException
  {
    ImageFbt iFbt = getImageFbt(1);
    iFbt.setAutoThreshold("Triangle dark");
    int thres = (int) iFbt.getMinThreshold();

    return backgroundMasks(connected8, thres/*, prB, maxPerc*/);
  }

  /**
   * Compute all final backgrounds as the bordering connected component in the
   * image. A black border is added to each image. From the top left corner, we
   * dilate the background over each image. Dilatation is stopped by pixels with
   * value over threshold. The background is computed by taking in account the
   * given rough background masks : images parts already in background in this
   * rough masks are not computed again. Dilatation is stopped by pixels with
   * value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param threshold grey level limit of the background component. (it is the
   * grey level of the lighter area around the cells).
   * @param roughMasks background masks computed from roughBackgroundmasks.
   *
   *
   *
   * @return an ArrayList of each slice background masks (array of int 1 if in
   * background, 0 elsewhere).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> backgroundMasks(boolean connected8, int threshold,
                                          ArrayList<int[]> roughMasks) throws
      InterruptedException
  {
    ArrayList<int[]> res = new ArrayList<int[]>();

    /*if ( prB != null )
     {
     prB.setTitle( "Computing final masks." );
     }*/
    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {
      ImageFbt IFbt = getImageFbt(i);

      res.add(IFbt.backgroundMaskWithRoughB(connected8, threshold, roughMasks.
          get(i - 1) /*, prB, maxPerc / ( double ) size */));

      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

    return res;
  }

  /**
   * Compute all final backgrounds as the bordering connected component in the
   * image with autoThreshold. The threshold to limit the component is taken
   * from the "Minimum dark" method of imageJ. A black border is added to each
   * image. From the top left corner, we dilate the background over each image.
   * Dilatation is stopped by pixels with value over threshold. The background
   * is computed by taking in account the given rough background masks : images
   * parts already in background in this rough masks are not computed again.
   * Dilatation is stopped by pixels with value over threshold.
   *
   * @param connected8 is background 4 or 8 connected.
   * @param roughMasks background masks computed from roughBackgroundmasks.
   *
   *
   *
   * @return an ArrayList of each slice background masks (array of int 1 if in
   * background, 0 elsewhere).
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public ArrayList<int[]> backgroundMasks(boolean connected8,
                                          ArrayList<int[]> roughMasks) throws
      InterruptedException
  {
    ImageFbt iFbt = getImageFbt(1);
    iFbt.setAutoThreshold("Triangle dark");
    int thres = (int) iFbt.getMinThreshold();

    return backgroundMasks(connected8, thres, roughMasks /*, prB, maxPerc */);
  }

  /**
   * Recenter all slices around colony center.
   *
   * @return
   */
  public StackFbt recenter()
  {

    StackFbt res = StackFbt.fromImageStack(this);

    for (int i = 1; i < getSize() + 1; i++)
    {
      ImageFbt imFbt = res.getImageFbt(i);
      imFbt.recenter();
      res.setPixels(imFbt.getPixels(), i);
      res.background.set(i - 1, imFbt.background);
    }

    return res;
  }

  /**
   * Compute, for all slices, seeds from local min and save them. Computed
   * ShapeSet of seeds are saved in path directory under names : Seeds_1,
   * Seeds_2, ...
   *
   * @param path directory where to save the seeds.
   * @param minSeedArea Minimum area in pixels² of a seed.
   * @param maxGrey Maximum value of a local minimum to be consider as a seed.
   * @param greyStep Increase value of grey level threshold.
   * @param fluobt
   */
  public void saveSeedsFromLocalMin(String path, int minSeedArea, int maxGrey,
                                    int greyStep, Fluo_Bac_Tracker fluobt)
  {
    File seedsPath = new File(path);
    seedsPath.mkdir();

    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {
      ImageFbt IFbt = this.getImageFbt(i);

      ShapeSet seeds = IFbt.seedsFromLocalMin_Shape(minSeedArea, maxGrey,
                                                    greyStep, fluobt);
      seeds.save(path + "/Seeds_" + Integer.toString(i));
    }
  }

  /**
   * Compute, for all slices, seeds from binarization and save them. Computed
   * ShapeSet of seeds are saved in path directory under names : Seeds_1,
   * Seeds_2, ...
   *
   * @param path directory where to save the seeds.
   * @param threshold Value determining seeds limits.
   * @param fluobt
   * @throws InterruptedException
   */
  public void saveSeedsFromBin(String path, int threshold,
                               Fluo_Bac_Tracker fluobt) throws
      InterruptedException
  {
    File seedsPath = new File(path);
    seedsPath.mkdir();

    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {

      ImageFbt IFbt = this.getImageFbt(i);
      ShapeSet seeds = IFbt.seedsFromBin_ShapeS(threshold, fluobt);
      seeds.save(path + "/Seeds_" + Integer.toString(i));
    }
  }

  /**
   * renormalize all slices using saved seeds.
   *
   * @param seedsFiles names of seeds files.
   * @param convolSize convolution square used.
   * @param iter nb of iteration in convolution.
   * @param highRadius more than cell radius.
   * @return a StackFbt with renormalized slices.
   * @throws java.lang.InterruptedException
   */
  public StackFbt renormalize(String[] seedsFiles, int convolSize, int iter,
                              int highRadius) throws InterruptedException
  {
    StackFbt res = StackFbt.fromImageStack(this);

    /*if ( prB != null )
     {
     prB.setTitle( "renormalizing images." );
     }*/
    int size = getSize();
    for (int i = 0; i < size; i++)
    {
      String seedsName = seedsFiles[i];
      int nb = Integer.parseInt(seedsName.replaceFirst("^.*\\D", ""));

      ImageFbt IFbt = this.getImageFbt(nb);
      ShapeSet seeds = (ShapeSet) Utils.loadObject(seedsName);
      IFbt.
          renormalize(seeds, convolSize, iter, highRadius /*, prB, maxPerc / ( double ) size */);
      res.setPixels(IFbt.getPixels(), nb);

      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

    return res;
  }

  /**
   * Compute non homogeneous dilatation from seeds to blobs over all slices of
   * the StackFbt.
   *
   * @param savePath path where to save blobs ShapeSet.
   * @param speedA first speed parameter. (see ImageFbt.ComputeSpeed).
   * @param speedB second speed parameter. (see ImageFbt.ComputeSpeed).
   * @param iterMax number of iterations maximum as a limit to the dilatation
   * computation.
   * @param seedsFiles Array of File containing the seeds ShapeSets.
   * @param fluobt Fluo_Back_Tracker from which parameters are taken
   * @throws InterruptedException thrown if current thread is stopped.
   */
  public void dilate(String savePath, double speedA, double speedB,
                     int iterMax, String[] seedsFiles,
                     Fluo_Bac_Tracker fluobt) throws InterruptedException
  {

    File blobsFile = new File(savePath);
    blobsFile.mkdir();

    int size = getSize();
    for (int i = 0; i < size; i++)
    {
      String name = seedsFiles[i];
      int nb = Integer.parseInt(name.replaceFirst("^.*\\D", ""));
      ShapeSet seeds = (ShapeSet) Utils.loadObject(name);

      ImageFbt IFbt = this.getImageFbt(nb);

      float[] speed = IFbt.computeViscosity(speedA, speedB);
      // TODO test
      ShapeSet blobs = IFbt.dilate_ShapeS(seeds, speed, iterMax, fluobt);

      blobs.save(savePath + "/Blobs_" + nb);
    }

  }

  // -----------------------------------------------------------------------
  //                     Generating cells
  // -----------------------------------------------------------------------
  /**
   * TODO
   *
   * @param ShapeSetFiles
   * @param maxBlobNb
   * @param maxCellWidth
   * @param minArea
   * @param connected8
   * @throws InterruptedException
   */
  public void gencells(File[] ShapeSetFiles, int maxBlobNb, double maxCellWidth,
                       double minArea, boolean connected8) throws
      InterruptedException
  {
    int size = getSize();
    for (int i = 1; i < size + 1; i++)
    {
      String path = ShapeSetFiles[i - 1].getAbsolutePath();
      ShapeSet blobs = (ShapeSet) Utils.loadObject(path);
      blobs.generateCells();
      Utils.saveObject(blobs, path);

      if (Thread.currentThread().isInterrupted())
      {
        Thread.sleep(0);
      }
    }

  }
  // =======================================================================
  //                              Protected Methods
  // =======================================================================
}
