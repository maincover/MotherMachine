package cellst.Image;

import ij.*;
import ij.gui.*;
import ij.process.*;

import java.awt.*;
import java.io.*;
import java.util.*;

import cellst.Main.*;

/**
 * A shapeList of ShapeFbt in a Hashmap (id -> ShapeFbt).
 *
 * This class is used to compute and save seeds and blobs.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class ShapeSet implements Serializable
{
  //==========================================================================
  //                               Attributes
  //==========================================================================  

  /**
   * HashMap containing all the ShapeFbts with as key their label.
   */
  protected HashMap<Integer, ShapeFbt> shapeList = new HashMap<Integer, ShapeFbt>();
  /**
   * Connection connectGraph of the ShapeSet. HashMap with as key each ShapeFbt
   * label and as value the set of its neighbors labels.
   */
  protected ConnectionGraph connectGraph;
  /**
   * List of possible cells in the shapeSet. A cell contains one or several
   * blobs.
   */
  protected ArrayList<Cell> possibleCells;
  protected ArrayList<Cell> deletedByUserCells = new ArrayList<Cell>();
  protected ArrayList<Cell> addedByUserCells = new ArrayList<Cell>();

  /**
   * Minimum cell Area.
   */
  protected double minCellArea;
  /**
   * Minimum blob area.
   */
  protected double minBlobArea;
  /**
   * Minimum border length with another blob ( if border too short, blobs aren't
   * linked ).
   */
  protected double minBorder;
  /**
   * Maximum number of blobs per cell.
   */
  protected int maxBlobsPerCell;

  /**
   * Maximum cell width.
   */
  protected double maxCellWidth;

  //==========================================================================
  //                               Constructor
  //========================================================================== 
  /**
   * ShapeSet Constructor. ConnectGraph and possibleCells are null.
   *
   * @param _minBlobArea
   * @param _minCellArea
   * @param _maxCellWidth
   * @param _minBorder
   * @param _maxBlobsPerCell
   */
  public ShapeSet(double _minBlobArea, double _minCellArea,
                  double _maxCellWidth, double _minBorder, int _maxBlobsPerCell)
  {
    minCellArea = _minCellArea;
    minBlobArea = _minBlobArea;
    maxCellWidth = _maxCellWidth;
    minBorder = _minBorder;
    maxBlobsPerCell = _maxBlobsPerCell;
  }

  /**
   * ShapeSet Constructor, parameters are updated with Fluo_Back_Tracker one.
   * ConnectGraph and possibleCells are null.
   *
   * @param fluobt
   */
  public ShapeSet(Fluo_Bac_Tracker fluobt)
  {
    updateParamFromFluobt(fluobt);
  }

  /**
   * Load a new ShapeSet from a file
   *
   * @param file path of the saved ShapeSet.
   */
  public ShapeSet(String file)
  {
    ShapeSet read = (ShapeSet) Utils.loadObject(file);

    this.shapeList = read.shapeList;
    this.connectGraph = read.connectGraph;
    this.possibleCells = read.possibleCells;
    minCellArea = read.minCellArea;
    minBlobArea = read.minBlobArea;
    maxCellWidth = read.maxCellWidth;
    minBorder = read.minBorder;
    maxBlobsPerCell = read.maxBlobsPerCell;
  }

  //==========================================================================  
  //                               Getters
  //==========================================================================
  /**
   * List getter.
   *
   * @return the HashMap containing the labels and the ShapeFbts of the
   * ShapeSet.
   */
  public HashMap<Integer, ShapeFbt> getList()
  {
    return shapeList;
  }

  /**
   * Connection center getter.
   *
   * @return center.
   */
  public ConnectionGraph getGraph()
  {
    return connectGraph;
  }

  /**
   * Compute an ImagePlus to visualize the ShapeSet.
   *
   * @param title title of the created imagePlus.
   * @param width width of the created imagePlus.
   * @param height height of the created imagePlus.
   * @return a Colored imagePlus black in background and colored in ShapeFbt.
   */
  public ImagePlus getImage(String title, int width, int height)
  {
    ColorProcessor colProc = getColorProcessor(width, height);

    return new ImagePlus(title, colProc);
  }

  /**
   * Compute an ImagePlus to visualize the ShapeSet.
   *
   * @param width width of the created imagePlus.
   * @param height height of the created imagePlus.
   * @return a Colored imagePlus black in background and colored in ShapeFbt.
   */
  public ImagePlus getImage(int width, int height)
  {
    return getImage("", width, height);
  }

  /**
   * Get the number of pixels in a ShapeFbt.
   *
   * @param label label of the ShapeFbt.
   * @return number of pixels.
   */
  public int getArea(int label)
  {
    return shapeList.get(label).getSize();
  }

  /**
   * Get one of the ShapeFbt from the ShapeSet.
   *
   * @param label label of ShapeFbt.
   * @return selected ShapeFbt.
   */
  public ShapeFbt getShape(int label)
  {
    return shapeList.get(label);
  }

  /**
   * Compute the labels of the ShapeSet. These labels are an array of int
   * corcenterponding to the image the ShapeSet is issued from. For each pixel
   * the array contains the label of its ShapeFbt or the background label.
   *
   * @param width width of the image the ShapeSet is issued from.
   * @param height height of the image the ShapeSet is issued from.
   * @return labels of the ShapeSet.
   */
  public int[] toLabels(int width, int height)
  {
    return Utils.ShapeSetToLabels(this, width, height, -1);
  }

  /**
   * possibleCells getter.
   *
   * @return possible cells shapeList.
   */
  public ArrayList<Cell> getPossibleCells()
  {
    return possibleCells;
  }

  /**
   * @return the addedByUserCells
   */
  public ArrayList<Cell> getAddedByUserCells()
  {
    return addedByUserCells;
  }

  /**
   * @return the deletedByUserCells
   */
  public ArrayList<Cell> getDeletedByUserCells()
  {
    return deletedByUserCells;
  }

  public HashSet<Point> getCellPixels(Cell cell)
  {
    HashSet<Point> res = new HashSet<Point>();

    for (int label : cell.labels)
    {
      res.addAll(getShape(label).pixels);
    }

    return res;
  }

  //==========================================================================
  //                               Setters
  //==========================================================================
  /**
   * Connection center setter.
   *
   * @param _graph new center.
   */
  public void setGraph(ConnectionGraph _graph)
  {
    connectGraph = _graph;
  }

  //==========================================================================
  //                              Public methods
  //==========================================================================
  public final void updateParamFromFluobt(Fluo_Bac_Tracker fluobt)
  {
    minCellArea = fluobt.getMinArea();
    minBlobArea = fluobt.getMinBlobArea();
    maxCellWidth = fluobt.getMaxWidth();
    minBorder = fluobt.getMinBlobBorder();
    maxBlobsPerCell = fluobt.getMaxNbBlobs();
  }

  public ShapeSet duplicate()
  {
    ShapeSet newShapeSet = new ShapeSet(minBlobArea, minCellArea, maxCellWidth,
                                        minBorder, maxBlobsPerCell);

    if (shapeList != null)
    {
      newShapeSet.shapeList = new HashMap<Integer, ShapeFbt>(shapeList);
    }
    if (possibleCells != null)
    {
      newShapeSet.possibleCells = new ArrayList<Cell>(possibleCells);
    }

    if (connectGraph != null)
    {
      newShapeSet.connectGraph = new ConnectionGraph(connectGraph);
    }

    return newShapeSet;
  }

  // -------------------------------------------------------------------------
  //               ShapeSet management (add or remove)
  // -------------------------------------------------------------------------
  /**
   * Add a point to one of the ShapeFbt of the ShapeSet. If the given label is
   * already pcenterent in the ShapeSet, the poitn is added to the
   * corcenterponding ShapeFbt. Else, a new ShapeFbt is created and added to the
   * ShapeSet with the given label and containing the point.
   *
   * @param label label of the ShapeFbt to update.
   * @param pt point to add to the ShapeFbt.
   */
  public void addPoint(int label, Point pt)
  {
    if (!shapeList.containsKey(label))
    {
      shapeList.put(label, new ShapeFbt());
    }

    ShapeFbt newpoints = shapeList.get(label);
    newpoints.add(pt);
    shapeList.put(label, newpoints);
  }

  public void addShape(ShapeFbt shape)
  {
    if (shape.center == null)
    {
      shape.setCenterAndRadius();
    }

    if (shape.boundary == null)
    {
      shape.updateBoundary();
    }

    // ======= Compute new label =============================================
    int label = Collections.max(shapeList.keySet()) + 1;

    // ======= Add shape to the lis ==========================================
    shapeList.put(label, shape);

    // ====== Update Connection graph ========================================
    if (connectGraph != null)
    {
      // --------------- for each other ShapeFbts ----------------------------
      //  compute the border length and add connection to the graph if needed.
      for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
      {
        ShapeFbt neighShape = entry.getValue();
        int neighLab = entry.getKey();

        double border = shape.border(neighShape);

        if (neighLab != label && border > minBorder)
        {
          connectGraph.putConnection(label, neighLab, border);
        }
      }
    }

    // ====== Update possible cells ==========================================
    if (possibleCells != null)
    {
      HashSet<Cell> cellSet = generateCellsFromStartBlob(new Cell(label));
      possibleCells.addAll(cellSet);
    }

  }

  /**
   * Remove a ShapeFbt from the ShapeSet.
   *
   * @param label label of the ShapeFbt to remove.
   */
  public void removeShape(int label)
  {
    shapeList.remove(label);

    // remove from connectGraph
    if (connectGraph != null)
    {
      connectGraph.remove(label);
    }

    // remove from possible cells
    if (possibleCells != null)
    {
      ArrayList<Cell> toRemove = new ArrayList<Cell>();
      for (Cell cell : possibleCells)
      {
        if (cell.contains(label))
        {
          toRemove.add(cell);
        }
      }

      for (Cell cell : toRemove)
      {
        possibleCells.remove(cell);
      }
    }
  }

  /**
   * Merge shape l1 in shape l2. Label l1 is removed and new merged shape will
   * have label l2.
   *
   * @param labToRemove
   * @param lab
   */
  public void merge(int labToRemove, int lab)
  {

    // ================= Check that both labels exists in ShapeSet ============
    if (!shapeList.containsKey(labToRemove) || !shapeList.containsKey(lab))
    {
      System.out.println(
          "Warning : At least one of the merged shapes doesn't exists. Nothing done.");
      return;
    }

    // ====================== Get first shape =================================
    ShapeFbt shape = getShape(labToRemove);

    // ================ Add all its pixels to second shape ====================
    for (Point pixel : shape.pixels)
    {
      addPoint(lab, pixel);
    }

    // ================ Update marged shape attributes ========================
    getShape(lab).setCenterAndRadius();
    getShape(lab).updateBoundary();

    // ================ Update Graph ==========================================
    if (connectGraph != null)
    {
      connectGraph.merge(labToRemove, lab);
    }

    // =============== Uate possible cells ====================================
    if (possibleCells != null)
    {
      for (Cell cell : possibleCells)
      {
        // if cell contains labToRemove, replace it by lab.
        // If cell contains labToRemove or lab, update its attrivbutes.
        if (cell.contains(labToRemove))
        {
          cell.remove(labToRemove);
          if (!cell.contains(lab))
          {
            cell.add(lab);
          }

          updateCellAttributes(cell);
        }
        else if (cell.contains(lab))
        {
          updateCellAttributes(cell);
        }
      }

      removeDuplicateCell();
    }

    removeShape(labToRemove);
  }

  /**
   * Compute all shapes boundaries.
   */
  public void updateBoundaries()
  {
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      entry.getValue().updateBoundary();
    }
  }

  /**
   * Compute all shapes centers and radius.
   */
  public void updateCenterAndRadius()
  {
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      entry.getValue().setCenterAndRadius();
    }
  }

  // -------------------------------------------------------------------------
  //               ShapeSet save
  // -------------------------------------------------------------------------
  /**
   * Save ShapeSet.
   *
   * @param file path of the file where to save ShapeSet.
   */
  public void save(String file)
  {
    Utils.saveObject(this, file);
  }

  // -------------------------------------------------------------------------
  //                 Neighboring management
  // -------------------------------------------------------------------------
  /**
   * Get Labels of the neighbouring blobs
   *
   * @param label
   * @return a set containing the neighbouring blobs labels
   */
  public Set<Integer> getNeighbourLabels(int label)
  {
    if (connectGraph == null)
    {
      System.out.println(
          "Error in ShapeSet.getNeighbourslabels : No graph computed yet.");
      return null;
    }

    return connectGraph.getNeighbours(label);
  }

  /**
   * Compute all borders length of a ShapeFbt with its neighbors. (use the
   * connection center).
   *
   * @param label label of the selected ShapeFbt.
   * @return an HashMap with as Key the neighboring ShapeFbt label and as value
   * the border length.
   */
  public HashMap<Integer, Double> getBordersLength(int label)
  {
    if (connectGraph == null)
    {
      System.out.println(
          "Error in ShapeSet.getBordersLength : No graph computed yet.");
      return null;
    }

    return connectGraph.getConnections(label);
  }

  /**
   * Compute connected connectGraph of the ShapeSet by looking which Shapes are
   * neighbors.
   */
  public void computeGraph()
  {
    updateCenterAndRadius();
    updateBoundaries();

    // ========================== Initiate connectGraph =======================
    ConnectionGraph res = new ConnectionGraph();
    ArrayList<Integer> done = new ArrayList<Integer>();

    // ========= For all points get its neighbors and borders length ==========
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      int lab = entry.getKey();
      ShapeFbt shape = entry.getValue();
      done.add(lab);

      // --------------- for each other ShapeFbts not already computed --------
      // test if it is a neighbor and compute the border length.
      for (Map.Entry<Integer, ShapeFbt> entry2 : shapeList.entrySet())
      {
        ShapeFbt neighShape = entry2.getValue();
        int neighLab = entry2.getKey();

        if (!done.contains(neighLab))
        {
          double border = shape.border(neighShape);

          if (border > minBorder)
          {
            res.putConnection(lab, neighLab, border);
          }
        }
      }
    }

    // ============= Set connectGraph as center =========
    connectGraph = res;
  }

  // ---------------------------------------------------------------------------
  //                 Functions related to blobs management 
  // ---------------------------------------------------------------------------
  /**
   * Remove invalid links from connectGraph. A link is invalid if the border
   * between the two blobs is inferior to minBorder, or if the union of the two
   * blobs has a width bigger than maxWidth (the resulting cell wouldn't be
   * valid).
   *
   */
  public void removeInvalidLinks()
  {
    // Init shapeList of Connexions to remove.
    ArrayList<Pair> toRemove = new ArrayList<Pair>();

    // For each connection, remove (add to remove list) it if its border is too
    // small or if the width of the union of the two blobs is too big.
    for (Map.Entry<Pair, Double> entry : connectGraph.entrySet())
    {
      Pair currPair = entry.getKey();

      if (entry.getValue() < minBorder)
      {
        toRemove.add(currPair);
      }
      else
      {
        double unionWidth = getShape(currPair.getS1()).getUnionWidth(getShape(
            currPair.getS2()));

        if (unionWidth > maxCellWidth)
        {
          toRemove.add(currPair);
        }
      }
    }

    // Remove all invalid connexions.
    for (Pair pairToRem : toRemove)
    {
      connectGraph.removeConnection(pairToRem);
    }
  }

  /**
   * Get rid of small blobs by merging them with the neighbor sharing the
   * largest border.
   *
   * @return a boolean indicating if at least one blob has been merge.
   */
  public boolean mergeSmall()
  {
    if (shapeList.isEmpty())
    {
      return false;
    }

    if (connectGraph == null)
    {
      System.out.println(
          "Error in ShapeSet.mergeSmall : No graph computed yet.");
      return false;
    }

    ShapeFbt s = shapeList.values().iterator().next();
    if (s.center == null || s.boundary == null)
    {
      updateCenterAndRadius();
      updateBoundaries();
    }

    removeInvalidLinks();
    ArrayList<Integer> smallLabels = new ArrayList<Integer>();

    // ============= shapeList all small blobs (< minArea) ==============
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      ShapeFbt blob = entry.getValue();
      int lab = entry.getKey();
      // if small blob
      if (blob.getSize() < minBlobArea)
      {
        smallLabels.add(lab);
      }
    }

    // if there is no small blobs, return false  
    if (smallLabels.isEmpty())
    {
      return false;
    }

    boolean changed = false;

    // ========== For each blobs with area inferior to minArea, =========
    // 1) find the neighboring blob sharing the largest border
    //    (if we don't find such a blob we do nothing)
    // 2) merge the two blobs
    for (int lab : smallLabels)
    {

      // if blobs has already been merged and is not small anymore, do nothing and continue.
      if (getArea(lab) > minBlobArea)
      {
        continue;
      }

      // compute all borders length (key is neighbor label and value is border length)
      HashMap<Integer, Double> borders = getBordersLength(lab);

      // ============ Find best neighbor ( 1) ) =========================
      // ------ If no neighbor with border large enough, do nothing --------
      if (!(borders.isEmpty() || Collections.max(borders.values()) < minBorder))
      {

        // ------ Else get neighbor with largest border --------
        double maxBorder = Collections.max(borders.values());
        int bestNeighLab = -1;

        for (Map.Entry<Integer, Double> neighEntry : borders.entrySet())
        {
          double bord = neighEntry.getValue();
          int neigh = neighEntry.getKey();

          if (maxBorder == bord)
          {
            bestNeighLab = neigh;
          }
        }

        //================ merge  the two blobs 2) ================
        System.out.println(lab + " merge in " + bestNeighLab);
        merge(lab, bestNeighLab);
        removeInvalidLinks();
        changed = true;
      }

    }

    return changed;
  }

  /**
   * Remove from ShapeSet all ShapeFbt too small to be a cell with no
   * neighboring ShapeFbt.
   *
   * @return
   */
  public boolean removeEdgeAndIsolated()
  {
    boolean changed = false;

    if (shapeList.isEmpty())
    {
      return false;
    }

    if (connectGraph == null)
    {
      System.out.println(
          "Error in ShapeSet.removeEdgeAndIsolated : No graph computed yet.");
      return false;
    }

    // <TODO: check comment>For each blob, compute its center radius and boundaries if it hasn't been done yet.
    ShapeFbt s = shapeList.values().iterator().next();
    if (s.center == null || s.boundary == null)
    {
      updateCenterAndRadius();
      updateBoundaries();
    }

    // Construct the list smallLabels of all the small blobs (< minCellArea) in
    // shapeList
    ArrayList<Integer> smallLabels = new ArrayList<Integer>();
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      ShapeFbt blob = entry.getValue();

      // If blob is small
      if (blob.getSize() < minCellArea)
      {
        smallLabels.add(entry.getKey());
      }
    }

    // If there is no small blobs, return  
    if (smallLabels.isEmpty())
    {
      return false;
    }

    // ========== For each blob with area smaller than minCellArea, =========
    // if it has no neighor, remove it, else if it has only 1 neighbour, merge it.
    for (int lab : smallLabels)
    {
      // if blobs has already been merged and is not small anymore, do nothing and continue.
      if (getArea(lab) > minCellArea)
      {
        continue;
      }

      // === compute all borders length === 
      // (key is neighbour label and value is border length)
      HashMap<Integer, Double> borders = getBordersLength(lab);

      // ==== If no neighbor, remove it ===
      if (borders.isEmpty())
      {
        System.out.println(lab + " removed");
        removeShape(lab);
        changed = true;
      }

      // === If only one neighbor, merge it ===
      if (borders.size() == 1)
      {
        int neigh = borders.keySet().iterator().next();
        System.out.println(lab + " merged in " + neigh);
        merge(lab, neigh);
        removeInvalidLinks();
        changed = true;
      }

    }

    updateCellsCertainity();
    return changed;
  }

  /**
   * Remove from ShapeSet those ShapeFbt whose width are too big to be part of a
   * cell.
   *
   */
  public void removeInvalidWidth()
  {
    ArrayList<Integer> toRemove = new ArrayList<Integer>();

    for (Integer label : shapeList.keySet())
    {
      Cell cell = new Cell(label);
      Double cellW = getCellWidth(cell);

      if (cellW > maxCellWidth)
      {
        toRemove.add(label);
      }

    }

    for (Integer label : toRemove)
    {
      removeShape(label);
    }
  }

  /**
   * Clean ShapeSet connectGraph. This means remove invalid links, merge small
   * blobs, remove isolated blobs too small to be a cell, merge edge blobs too
   * small to be a cell.
   *
   * @return
   */
  public boolean cleanGraph()
  {
    boolean res = false;

    removeInvalidWidth();
    removeInvalidLinks();

    int i = 0;
    boolean changed = true;
    while (changed)
    {
      if (i == 1)
      {
        res = true;
      }
      i++;

      changed = mergeSmall();
    }

    // === while removeEdgeAndIsolated function can't delete more small blobs ===
    // Remove small isolated blobs
    changed = true;
    i = 0;
    while (changed)
    {
      if (i == 1)
      {
        res = true;
      }
      i++;

      changed = removeEdgeAndIsolated();
    }

    return res;
  }

  // ---------------------------------------------------------------------------
  //                Graph Show
  // ---------------------------------------------------------------------------
  /**
   * Returns a colorProcessor showing blobs.
   *
   * @param width width of the image
   * @param height height of the image
   * @return
   */
  public ColorProcessor getColorProcessor(int width, int height)
  {
    ColorProcessor colProc = new ColorProcessor(width, height);

    Color[] palette = Utils.generateColors(getList().size());

    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        colProc.setColor(Color.BLACK);
        colProc.drawPixel(x, y);
      }
    }

    // For each seeds put grey level to >0
    int i = 0;
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      ShapeFbt currShape = entry.getValue();

      colProc.setColor(palette[i]);

      Iterator itr = currShape.iterator();
      while (itr.hasNext())
      {
        Point pt = (Point) itr.next();
        int x = pt.x;
        int y = pt.y;

        colProc.drawPixel(x, y);

      }

      i++;
    }

    return colProc;
  }
  
  // ---------------------------------------------------------------------------
  //                Graph Show
  // ---------------------------------------------------------------------------
  /**
   * Returns a colorProcessor showing blobs.
   *
   * @author xsong
   * @param width width of the image
   * @param height height of the image
   * @return a shortProcessor with label
   */
  public ShortProcessor converteLabelToShortProcessor( int width, int height )
  {
    ShortProcessor colProc = new ShortProcessor( width, height );

    for ( int x = 0; x < width; x++ )
    {
      for ( int y = 0; y < height; y++ )
      {
        colProc.set(x, y, 0);
      }
    }

    // For each seeds put grey level to >0
    int i = 1;
    for ( Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet() )
    {
      ShapeFbt currShape = entry.getValue();

      Iterator<Point> itr = currShape.iterator();
      while ( itr.hasNext() )
      {
        Point pt = itr.next();
        int x = pt.x;
        int y = pt.y;
        colProc.set(x, y, i);
      }
      i++;
    }
    return colProc;
  }
  
  /**
   * @author xsong
   * @param sp input obtained from getLabelProcessor function
   * @return idea is to get rid of neighbour point and generate binary image to be used by Roi manager, 
   */
  public ByteProcessor converteToByteProcessor(ShortProcessor sp)
  {
 	 int width = sp.getWidth();
 	 int height = sp.getHeight();
 	 ByteProcessor bp = new ByteProcessor(width,height);
 	 for ( int x = 0; x < width; x++ )
 	   {
 	     for ( int y = 0; y < height; y++ )
 	     {
 	    	if(sp.get(x, y)>0)
 	    		bp.set(x, y, 255);
 	    	else {
 	    		bp.set(x, y, 0);
 			}
 	     }
 	   }
 	 
 	 for(int x=0; x<width; x++)
 	 {
 		 for(int y=0; y<height; y++)
 		 {
 			 int label = sp.get(x, y);
 			 if(label == 0)
 				 continue;
 			 
 			 for(int xx=-1;xx<=1;xx++)
 				 for(int yy=-1;yy<=1;yy++)
 				 {
 					 if(x+xx<0 || x+xx>=width || y+yy<0 || y+yy>=height)
 						 continue;
 					 
 					 if(sp.get(x+xx, y+yy) == 0)
 						 continue;
 					 
 					 if( sp.get(x+xx, y+yy) != label)
 					 {
 						 bp.set(x, y, 0);
 						 break;
 					 }
 				 }
 		 }
 	 }
 	 return bp;
  }
  

  /**
   * Show connection center in an imagePlus.
   *
   * @param title
   * @param width
   * @param height
   * @return
   */
  public ImagePlus showGraph(String title, int width, int height)
  {
    ColorProcessor CP = getColorProcessorGraph(width, height);

    return new ImagePlus(title, CP);
  }

  /**
   * Show connection center in a ColorProcessor.
   *
   * @param width
   * @param height
   * @return
   */
  public ColorProcessor getColorProcessorGraph(int width, int height)
  {
    ColorProcessor CP = getColorProcessor(width, height);

    if (connectGraph == null || shapeList.isEmpty())
    {
      return CP;
    }

    updateCenterAndRadius();
    updateBoundaries();

    // == If connectGraph was computed show connections ===
    CP.setColor(Color.white);
    CP.setLineWidth(3);
    if (connectGraph != null)
    {
      for (Map.Entry<Pair, Double> entry : connectGraph.entrySet())
      {
        Pair currPair = entry.getKey();

        Point center1 = getShape(currPair.getS1()).center;
        Point center2 = getShape(currPair.getS2()).center;

        CP.drawLine(center1.x, center1.y, center2.x, center2.y);
      }
    }

    // === Draw mass centers of all shapes. ===
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      Point center = entry.getValue().center;

      int nbConnections = connectGraph.getConnections(entry.getKey()).size();

      if (nbConnections >= 3)
      {
        CP.setColor(Color.black);
      }
      else
      {
        CP.setColor(Color.white);
      }

      CP.setLineWidth(6);
      CP.drawDot(center.x, center.y);

      if (nbConnections >= 3)
      {
        CP.setColor(Color.red);
      }
      else
      {
        CP.setColor(Color.black);
      }
      CP.setLineWidth(4);
      CP.drawDot(center.x, center.y);
    }

    // == If posssible cells were computed show certain cells ===
    CP.setColor(Color.white);
    if (possibleCells != null)
    {
      for (Cell cell : possibleCells)
      {
        if (cell.isCertain())
        {
          ShapeFbt shape = new ShapeFbt();
          for (int lab : cell.labels)
          {
            ShapeFbt blob = getShape(lab);
            for (Point pt : blob.pixels)
            {
              shape.add(pt);
            }
          }
          shape.updateBoundary();

          for (Point pt : shape.boundary)
          {
            CP.drawPixel(pt.x, pt.y);
          }
        }
      }
    }

    return CP;
  }

  public ColorProcessor getColorProcessorCells(int width, int height)
  {
    // ========== If no cells computed, return normal colorprocessor ==========
    if (possibleCells == null)
    {
      return getColorProcessor(width, height);
    }

    // ========== Init color processor ========================================
    ColorProcessor colProc = new ColorProcessor(width, height);

    // ========== List all certains cells =====================================
    ArrayList<Cell> certainCells = new ArrayList<Cell>();
    for (Cell currCell : possibleCells)
    {
      if (currCell.isCertain())
      {
        certainCells.add(currCell);
      }
    }

    // ========= Init color palette ===========================================
    Color[] palette = Utils.generateColors(certainCells.size());

    // ========= fill color processor to black ================================
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        colProc.setColor(Color.BLACK);
        colProc.drawPixel(x, y);
      }
    }

    // === For each certain cell, color all its blobs with the same color. ====
    int i = 0;
    for (Cell currCell : certainCells)
    {
      // -------------- Change painting color ---------------------------------
      colProc.setColor(palette[i]);
      i++;

      // -------------- Paint all blobs in the cell ---------------------------
      for (int blobLabel : currCell.labels)
      {
        ShapeFbt currShape = getShape(blobLabel);

        for (Point pt : currShape.pixels)
        {
          colProc.drawPixel(pt.x, pt.y);
        }
      }
    }

    // ======= Paint all blobs borders in another color =======================
    colProc.setColor(Color.gray);
//    for ( ShapeFbt currShape : shapeList.values() )
//    {
    for (Map.Entry<Integer, ShapeFbt> entry : shapeList.entrySet())
    {
      ShapeFbt currShape = entry.getValue();
      int label = entry.getKey();

      for (Point pt : currShape.boundary)
      {
        colProc.drawPixel(pt.x, pt.y);
      }

      // DEBUG 
//      colProc.setColor( Color.white );
//      currShape.setCenter();
//      colProc.setLineWidth(2);
//      colProc.drawString( ""+label, currShape.center.x - 5 , currShape.center.y - 5  );
//      colProc.setLineWidth(1);
//      // \DEBUG
    }

    // ================== return colorProcessor ==============================
    return colProc;
  }

  // -------------------------------------------------------------------------
  //                       Generate cells
  // -------------------------------------------------------------------------
  /**
   * Describe all possible cells.
   *
   * @return String describing all possible cells.
   */
  public String possibleCellsToString()
  {
    String res = "( ";

    for (Cell cell : possibleCells)
    {
      res += cell.toString();
    }

    res += " )";

    return res;
  }

  /**
   * Compute cell center point and area and put it in its attributes.
   *
   * @param cell cell to use.
   */
  public void updateCellCenterAndArea(Cell cell)
  {
    // === Init center point and area ===
    Point center = new Point(0, 0);

    // === For each blob in cell ===
    // Update center point and area.
    HashSet<Point> cellPixs = getCellPixels(cell);
    int area = cellPixs.size();

    for (Point currPt : cellPixs)
    {
      center.x += currPt.x;
      center.y += currPt.y;
    }

    // === Finish to compute center point ===
    center.x = Math.round((float) center.x / (float) area);
    center.y = Math.round((float) center.y / (float) area);

    cell.center = center;
    cell.area = area;
  }

  public void updateCellOrientationVariables(Cell cell)
  {
    // Computation taken from Mael Primet code in C++. 
    double a = 0, b = 0, c = 0;

    HashSet<Point> cellPixs = getCellPixels(cell);
    for (Point currPt : cellPixs)
    {
      int x = (int) (currPt.x - cell.center.x);
      int y = (int) (currPt.y - cell.center.y);
      a += x * x;
      b += x * y;
      c += y * y;
    }

    a /= cell.area;
    b /= cell.area;
    c /= cell.area;

    // ===================== Update cell attributes =========================
    double[] var =
    {
      a, b, c
    };
    cell.setOrientationVariables(var);
  }

  /**
   * Compute cell orientation and put it in its attributes. WARNING : this
   * depends on center and area of the cell, so they must be updated. Code taken
   * from mael primet.
   *
   * @param cell cell to use.
   */
  public void updateCellOrientation(Cell cell)
  {
    updateCellOrientationVariables(cell);
    cell.UpdateOrientationFromVariables();
  }

  public void updateCellWidth(Cell cell)
  {
    cell.setWidth(getCellWidth(cell));
  }

  /**
   * Update a cell center point, area and orientation. GetCellCenter,
   * getCellArea and getCellOrientation are not use to be more efficient in
   * computing time.
   *
   * @param cell
   */
  public void updateCellAttributes(Cell cell)
  {
    // =================== center point and area ============================
    updateCellCenterAndArea(cell);

    //================== Orientation ========================================
    updateCellOrientation(cell);

    // ================ Width ===============================================
    updateCellWidth(cell);

  }

  /**
   * Updates attributes of all possible cells.
   *
   */
  public void updateAllCellsAttributes()
  {
    for (Cell cell : possibleCells)
    {
      updateCellAttributes(cell);
    }

  }

//  /**
//   * Approximate the main orientation of the elongation of the cell, and tell if
//   * the orientation is significant or not.
//   */
//  public double getCellOrientation( Cell cell )
//  {
//
//    double size = 0.0;
//    Point center = getCellCenter( cell );
//
//    /* matrix is | a b |
//     *           | b c | */
//    double a = 0, b = 0, c = 0;
//
//
//    for ( int label : cell.labels )
//    {
//      size += getArea( label );
//
//      ShapeFbt shape = getShape( label );
//      for ( Point currPt : shape.pixels )
//      {
//        int x = ( int ) ( currPt.x - center.x );
//        int y = ( int ) ( currPt.y - center.y );
//        a += x * x;
//        b += x * y;
//        c += y * y;
//      }
//    }
//
//
//    a /= size;
//    b /= size;
//    c /= size;
//
//    /* eigenvalues equation is lam^2 - (a+c)*lam + ac - b^2 = 0 */
//    double B = -a - c, C = a * c - b * b;
//    double sq_delta = Math.sqrt( B * B - 4 * 1 * C );
//    double pe1 = ( -B - sq_delta ) * 0.5,
//            pe2 = ( -B + sq_delta ) * 0.5;
//    double e1;
//    if ( Math.abs( pe1 ) > Math.abs( pe2 ) )
//    {
//      e1 = pe1;
//    }
//    else
//    {
//      e1 = pe2;
//    }
//
//    /* first eigenvector */
//    double ux, uy;
//
//    if ( Math.abs( b ) > 0.01 )
//    {
//      ux = 1;
//      uy = ( e1 - a ) / b;
//    }
//    else if ( Math.abs( a - e1 ) < 0.01 )
//    {
//      ux = 1;
//      uy = 0;
//    }
//    else
//    {
//      ux = 0;
//      uy = 1;
//    }
//    double theta = Math.atan2( uy, ux );
//
//    return theta;
//  }
  /**
   * Compute cell width as width of the minimum annulus enclosing it.
   *
   * @param cell List of blobs labels contained in the cell.
   * @return cell width.
   */
  public double getCellWidth(Cell cell)
  {
    /* Comuting the width of the minimal-area enclosing annulus is
     * equivalent to solving the problem :
     *   Data : (x1,y1) ... (xn,yn)
     *   Variables : x,y,r^2,R^2
     *   Problem :
     *     minimize R - r
     *     under the constraints
     *       For all i = 1..n, r^2 <= (xi - x)^2 + (yi-y)^2 <= R^2
     *
     * This problem is complex to solve in all it's generality, therefore
     * we do an exhaustive search on a limited number of points, we
     * search the center of the annulus in a cone orthogonal to the
     * orientation of the blob. We discretize the cone by taking only a
     * small number of lines in the cone, and only a small number of
     * points on those lines. This seems to give a relatively good
     * approximation.
     *
     * We also only search the minimal-width annulus that encloses the
     * border pixels of the shape (to lighten the computation time),
     * and therefore we only use annulus centers that are outside the
     * shape.
     */

    if (cell.center == null || Double.isNaN(cell.orientation))
    {
      updateCellAttributes(cell);
    }

    double theta = cell.getOrientation();
    Point center = cell.getCenter();

    double x, y, r = 0, R = 1E15;
    double cur_x, cur_y, cur_r, cur_R;

    for (double dt = -Math.PI / 8; dt <= Math.PI / 8; dt += Math.PI / 32)
    {
      double ux = Math.cos(theta + dt + Math.PI / 2.);
      double uy = Math.sin(theta + dt + Math.PI / 2.);

      for (double t = -200; t <= 200; t += 10)
      {
        /* XXX
         * We check that |t| >= 20 to avoid centers of annulus inside
         * the shape. This is not a really good method to avoid the
         * center being inside the shape. If it's problematic, and
         * that we can afford the cost, we can check the annulus
         * against all points in the shape rather than only on border
         * pixels, this wouldn't require the center to be outside of
         * the shape */
        if (Math.abs(t) < 20)
        {
          continue;
        }

        cur_x = t * ux + center.x;
        cur_y = t * uy + center.y;

        cur_r = 1E15;
        cur_R = 0;

        // For all border pixels of cells, update v ariables.  
        for (int label : cell.labels)
        {
          ShapeFbt shape = getShape(label);

          for (Point currPixel : shape.boundary)
          {
            double px = (double) currPixel.x - cur_x;
            double py = (double) currPixel.y - cur_y;
            double d = Math.sqrt(px * px + py * py);
            cur_r = Math.min(cur_r, d);
            cur_R = Math.max(cur_R, d);
          }
        }

        if (cur_R - cur_r < R - r)
        {
          R = cur_R;
          r = cur_r;
          x = cur_x;
          y = cur_y;
        }
      }
    }

    double width = R - r;
    return width;
  }

  /**
   * Check if cell is valid. It means : It is not empty, it has not too many
   * blobs, its area is superior to minimum area, its width is inferior to
   * maximum width and it was not delete by user.
   *
   * @param cell cell to check. (shapeList of blobs label)
   * @return true : cell is valid, false : cell is not valid.
   */
  public boolean isCellValid(Cell cell)
  {
    // DEBUG 28/01
    boolean debugVerb = false;
    String log = "Is cell valid ? " + cell.toString() + "\n";

    // if added by user, return tru
    for (Cell addCell : addedByUserCells)
    {
      if (cell.contains(addCell) && addCell.contains(cell))
      {
        if (debugVerb)
        {
          log += "Added by user : VALID\n";
          System.out.println(log);
        }
        return true;
      }
    }

    if (cell.isEmpty())
    {
      if (debugVerb)
      {
        log += "Empty : INVALID \n";
        System.out.println(log);
      }
      return false;
    }

    // if deleted by user, return false
    for (Cell delCell : deletedByUserCells)
    {
      if (cell.labels.containsAll(delCell.labels))
      {
        if (debugVerb)
        {
          log += "Deleted by user : INVALID\n";
          System.out.println(log);
        }
        return false;
      }
    }

    // If one of the blobs doesn't exists, return false.
    for (int label : cell.labels)
    {
      if (!shapeList.containsKey(label))
      {
        if (debugVerb)
        {
          log += "One of the blobs doesn't exists (" + label + ") : INVALID\n";
          System.out.println(log);
        }
        return false;
      }
    }

    /* only allows cells with blob[0]->label < blob[last]->label in order
     * not to add two times the same cell */
//    if (cell.get(0) - cell.get(cell.blobsNb() - 1) > 0)
//    {
//      if (debugVerb)
//      {
//        log += "Last blob inf to first blob : INVALID\n";
//        System.out.println(log);
//      }
//      return false;
//    }
    if (cell.width > maxCellWidth)
    {
      if (debugVerb)
      {
        log += "Cell width (" + cell.width + ") superior to max cellWidth ("
               + maxCellWidth + ") : INVALID";
        System.out.println(log);
      }
      return false;
    }

//    else if (cell.area < minCellArea)
//    {
//      if (debugVerb)
//      {
//        log += "Cell area (" + cell.area + ") inferior to min cellArea ("
//               + minCellArea + ") : INVALID\n";
//        System.out.println(log);
//      }
//      return false;
//    }

//    if (debugVerb)
//    {
//      log += "VALID\n";
//      System.out.println(log);
//    }
    return true;
  }

  /**
   * Removes from possible cells all cells with blob[0]->label <
   * blob[last]->label in order not to add two times the same cell and 
   * cells with too small area.
   */
  public void removeSmallOrBadOrderedCells()
  {
    ArrayList<Cell> toKeep = new ArrayList<Cell>();

    for (Cell cell : possibleCells)
    {
      // if blob[0]->label < blob[last]->label remove it
      if (cell.get(0) - cell.get(cell.blobsNb() - 1) > 0)
      {
        continue;
      }

      // if too small area, remove it
      if (cell.area < minCellArea)
      {
        continue;
      }
      
      toKeep.add(cell);
    }

    possibleCells = toKeep;
  }

  /**
   * Check if a cell is certain. A cell is certain if it contains only one blob
   * without neighbors.
   *
   *
   * @param cell cell to check.
   * @return true : cell is certain, false it is not.
   */
  public boolean isCellCertain(Cell cell)
  {
    // if added by user, return tru
    for (Cell addCell : addedByUserCells)
    {
      if (cell.contains(addCell) && addCell.contains(cell))
      {
        return true;
      }
    }

    if (cell.blobsNb() != 1)
    {
      return false;
    }

    Set<Integer> neighs = getNeighbourLabels(cell.get(0));
    return neighs.isEmpty();
  }

  /**
   * Update all cells certain flag. A cell is certain if it has no neighbor and
   * only one blob. WARNING : this function only put cell from not certain to
   * certain; Once a cell is certain it stays certain.
   *
   */
  public void updateCellsCertainity()
  {
    // =========== If possible cells were not computed yet ====================
    // Do nothing.
    if (possibleCells == null)
    {
      return;
    }

    // ========================== Variables ===================================
    // List of cells to remove (if it contains a blob that is a certain cell)
    ArrayList<Cell> toRemove = new ArrayList<Cell>();

    // =========== For each possible cell =====================================
    // 1) Check if it is certain ( one blob without neighbor) and update its flag.
    // 2) If it is certain, mark all other cells containing it to remove.
    for (Cell cell : possibleCells)
    {
      // ------- 1) Check certainity, if certain set flag to true -----------
      if (isCellCertain(cell))
      {
        if (!cell.isCertain())
        {
          cell.setCertain(true);
        }

        // === 2) Mark other cells containing the certain cell unique label to be removed. === 
        int index = cell.get(0);
        for (Cell cell2 : possibleCells)
        {
          if (cell2 != cell && cell2.contains(index))
          {
            toRemove.add(cell2);
          }
        }
      }

    }

    // =============== Remove all marked cells ================================
    for (Cell cell : toRemove)
    {
      possibleCells.remove(cell);
    }

  }

  /**
   * creates a pointRoi containinq all border points of the 'currCell'
   *
   * @param currCell cell we want to make a roi from.
   * @return pointRoi
   */
  public PointRoi getRoiFromCell(Cell currCell)
  {
    // =========== Init result roi ============================================
    PointRoi cellRoi = null;

    for (int currLabel : currCell.getLabels())
    {
      // --- Load currBlob ---
      ShapeFbt currBlob = getShape(currLabel);

      // --- Compute its center radius and boundary ---
      currBlob.setCenterAndRadius();
      currBlob.updateBoundary();

      // --- 2 ) Compute roi representing border of the cell. ---
      for (Point pix : currBlob.getBoundary())
      {
        if (cellRoi == null)
        {
          cellRoi = new PointRoi(pix.x, pix.y);
        }
        else
        {
          cellRoi = cellRoi.addPoint(pix.x, pix.y);
        }
      }
    }

    return cellRoi;
  }

  /**
   * From a starting candidate cell (shapeList of blob labels), compute all
   * possible cells containing it.
   *
   * @param start starting cell.
   * @return HashSet of all possible cells containing start.
   */
  public HashSet<Cell> generateCellsFromStartBlob(Cell start)
  {
    // =================  Init result HashSet. ================================
    HashSet<Cell> res = new HashSet<Cell>();

    // =============== If starting cell contains ==============================
    //         already maximum number of blobs, return null. 
    if (start.blobsNb() >= maxBlobsPerCell)
    {
      return null;
    }

    // =============== Get the neighbours of the last blob ====================
    //                   (at the extremity of the cell) 
    // and for each neighbour, check if it can be added to the cell to make
    // another potential cell.
    int last = start.get(start.blobsNb() - 1);
    Set<Integer> neighs = getNeighbourLabels(last);

    for (int newLab : neighs)
    {
      // ------ Get the starting cell  ----------------------------------------
      Cell newCell = new Cell(start);

      // ------ If blob is already in the starting cell, ----------------------
      //       continue to the next neighbor blob.
      if (newCell.contains(newLab))
      {
        continue;
      }

      // ------- Else add the blob to the cell. -------------------------------
      newCell.add(newLab);
      updateCellAttributes(newCell);

      // ------ If the new cell is valid, add it to the result shapeList. -----
      // and continue to extend this new cell to get all possible cells from 
      //     it as starting point.
      // Add these to the result shapeList.
      if (isCellValid(newCell))
      {
        // add newcell to results.
        res.add(newCell);

        // add all possible extensions of newcell to results.
        HashSet<Cell> neighsPoss = generateCellsFromStartBlob(newCell);
        if (neighsPoss != null)
        {
          res.addAll(neighsPoss);
        }
      }
    }

    // ============== Return results shapeList ================================
    return res;
  }

  public HashSet<Cell> generateCellsContainingCell(Cell start)
  {

    // ================= Get all vali cells beginning from start cell =========
    HashSet<Cell> res = generateCellsFromStartBlob(start);

    // =============== If starting cell contains ==============================
    //         already maximum number of blobs, return null. 
    if (res == null)
    {
      return null;
    }

    // =============== Get the neighbours of the first blob ====================
    //                   (at the extremity of the cell) 
    // and for each neighbour, check if it can be added to the cell to make
    // another potential starting cell.
    int first = start.get(0);
    Set<Integer> neighs = getNeighbourLabels(first);

    for (int newLab : neighs)
    {
      // ------ Get the starting cell  ----------------------------------------
      Cell newCell = new Cell(start);

      // ------ If blob is already in the starting cell, ----------------------
      //       continue to the next neighbor blob.
      if (newCell.contains(newLab))
      {
        continue;
      }

      // ------- Else add the blob to the cell. -------------------------------
      newCell.add(newLab);

      // ------ If the new cell is valid, add it to the result shapeList. -----
      // and continue to extend this new cell to get all possible cells from 
      //     it as starting point.
      // Add these to the result shapeList.
      if (isCellValid(newCell))
      {
        // add newcell to results.
        res.addAll(generateCellsFromStartBlob(newCell));

        // add all possible extensions of newcell to results.
        HashSet<Cell> neighsPoss = generateCellsContainingCell(newCell);
        if (neighsPoss != null)
        {
          res.addAll(neighsPoss);
        }
      }
    }

    // ============== Return results shapeList ================================
    return res;

  }

  /**
   * Recomputes cells keeping certains cells as they are.
   *
   * This allows to upadet possible cells after user edited, added or deleted a
   * cell.
   *
   */
  public void reComputeCells()
  {
    // ============= Register certain cells of the ShapeSet ===================
    ArrayList<Cell> certainCells = new ArrayList<Cell>();

    for (Cell currCell : possibleCells)
    {
      if (currCell.isCertain() && !deletedByUserCells.contains(currCell))
      {
        certainCells.add(currCell);
      }
    }

    // =========== regenerate cells ===========================================
    generateCells();

    // =============== Remove cells intersecting a certain cell ===============
    ArrayList<Cell> toRem = new ArrayList<Cell>();
    for (Cell currCell : possibleCells)
    {
      for (Cell certainCell : certainCells)
      {
        if (currCell.intersect(certainCell))
        {
          toRem.add(currCell);
        }
      }
    }

    possibleCells.removeAll(toRem);

    // ===== Add the previously registered certains cells to possible cells === 
    possibleCells.addAll(certainCells);

    // ========= Update cells attributes ======================================
    updateAllCellsAttributes();

  }

  /**
   * Generate all possible cells in the ShapeSet.
   *
   * Update their certain flag.
   *
   */
  public void generateCells()
  {
    ArrayList<Cell> res = new ArrayList<Cell>();

    // For each blob, generate all the candidate cells that contain the blob
    // and add them to center.
    for (int label : shapeList.keySet())
    {
      // If the blob is a possible cell, add it to center
      Cell blob = new Cell(label);
      updateCellAttributes(blob);
      if (isCellValid(blob))
      {
        res.add(blob);
      }

      // Generate all the possible combinations of this blob with others and
      // add the candidate cells to the list
      HashSet<Cell> newCells = generateCellsFromStartBlob(blob);
      if (newCells != null)
      {
        for (Cell cell : newCells)
        {
          res.add(cell);
        }
      }

    }

    possibleCells = res;
    removeSmallOrBadOrderedCells();
    updateCellsCertainity();
    removeDuplicateCell();
    updateAllCellsAttributes();
    
    //DEBUG 30/01
//    for( Cell cell : possibleCells)
//    {
//      System.out.println(cell.toString());
//    }
  }

  /**
   * Check if a cell is intersecting another certain cell in the ShapeSet.
   *
   * @param cell cell to check.
   * @return true : it intersect a certain cell. false : it doesn't.
   */
  public boolean intersectCertain(Cell cell)
  {
    for (Cell certCell : possibleCells)
    {
      if (certCell.certain && cell.intersect(certCell) && !cell.equals(
          certCell))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * Remove cells with same blobs but not same certainity in possibleCells :
   * only the certain one is kept.
   */
  public void removeDuplicateCell()
  {
    // ============== Init variables =========================================
    HashSet<Cell> done = new HashSet<Cell>();
    ArrayList<Cell> toRemove = new ArrayList<Cell>();

    // ============== For each possible cell =================================
    // ---- 1) If it was already checked, continue ----
    // ---- 2) For each cell2 in possibleCells ----
    // 2a) if cell and cell2 have the same blobs, remove one of the cells
    //        with priority for the uncertain one.
    // 2b) if cell and cell2 are equal : first time, don't do anything, 
    //        after that remove duplicates.
    for (Cell cell : possibleCells)
    {
      // ---- 1) If it was already checked, continue ----
      if (done.contains(cell))
      {
        continue;
      }

      // ---- 2) For each cell2 in possibleCells ----
      int sum = 0;
      for (Cell cell2 : possibleCells)
      {
        // 2a) if cell and cell2 have the same blobs, remove one of the cells
        //        with priority for the uncertain one.
        if (cell.contains(cell2) && cell2.contains(cell) && !(cell.certain
                                                              == cell2.certain))
        {
          done.add(cell);
          done.add(cell2);

          if (cell.certain)
          {
            toRemove.add(cell2);
          }
          else
          {
            toRemove.add(cell);
          }
        }
        // 2b) if cell and cell2 are equal : first time, don't do anything, 
        //        after that remove duplicates.
        else if (cell.equals(cell2))
        {
          done.add(cell);
          if (sum == 0)
          {
            sum++;
          }
          else
          {
            toRemove.add(cell2);
          }
        }
      }
    }

    // ============== Remove all cells listed to be remove ====================
    for (Cell cell : toRemove)
    {
      possibleCells.remove(cell);
    }
  }

  // -------------------------------------------------------------------------
  // --- Correction methods used once the lineage has been construct
  // -------------------------------------------------------------------------
  /**
   * Edit a cell in possibleCells list by adding or deleting a blob. It then
   * recomputes cells accordingly.
   *
   * @param cell
   * @param blobLab
   * @return true if cell was edited, false if not
   */
  public boolean editCell(Cell cell, int blobLab)
  {
    // ======== if cell is not in this slice, return ==========================
    if (!possibleCells.contains(cell))
    {
      System.out.println("Error in Blobsolver.editCell : cell " + cell.
          toString() + " was not in ShapeSet possible cells.");
      return false;
    }

    // =============== if cell contains blobLab, ==============================
    // 1 ) remove it 
    // 2 ) mark cell has unlinked (donefor and doneBack false) and certain
    if (cell.contains(blobLab))
    {
      possibleCells.remove(cell);

      // 1 ) remove it 
      cell.remove(blobLab);

      // 2 ) mark cell has unlinked (donefor and doneBack false) and certain
      cell.certain = true;
      cell.doneBack = false;
      cell.doneFor = false;

      possibleCells.add(cell);
    }
    // ======= if cell doesn't contains blobLab, ==============================
    // 1 ) If it is in another certain cell, fo nothing and return
    // 2 ) Else, add it to cell 
    // 3 ) mark cell has unlinked (donefor and doneBack false) and certain
    else
    {
      // 1 ) If it is in another certain cell, fo nothing and return
      for (Cell currCell : possibleCells)
      {
        if (currCell.isCertain() && currCell.contains(blobLab))
        {
          System.out.println("Blob is already in a certain cell.");
          return false;
        }
      }

      // 2 ) add it to cell 
      possibleCells.remove(cell);
      cell.add(blobLab);

      // 3 ) mark cell has unlinked (donefor and doneBack false) and certain
      cell.certain = true;
      cell.doneBack = false;
      cell.doneFor = false;

      possibleCells.add(cell);
    }

    reComputeCells();
    return true;

  }

}
