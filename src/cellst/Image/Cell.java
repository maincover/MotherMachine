package cellst.Image;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.*;

/**
 * Possible cell class. Containing labels of the hypothetical cell and its flags
 * ( isCertain ...). It can also keep cell attributes (width, orientaiton,
 * doneFor, doneBack, center, drawing color ...) but these attributes must be
 * manually updated in ShapeSet class.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Cell implements Serializable, Comparable
{

  // =========================================================================
  // ===                      Attributes
  // =========================================================================
  /**
   * Certain cell flag.
   */
  protected boolean certain = false;

  /**
   * List of labels of the blobs contained in the cell.
   */
  protected ArrayList<Integer> labels = new ArrayList<Integer>();

  /**
   * Flag true if cell has been attribute a mother cell, false if not.
   */
  protected boolean doneFor;

  /**
   * Flag true if cell has been attribute one(two) daughter(s) cell(s), false if
   * not.
   */
  protected boolean doneBack;

  /**
   * cell orientation.
   */
  protected double orientation;
  
  protected double[] orientationVariables;

  /**
   * Cell area.
   */
  protected double area;

  /**
   * cell center point.
   */
  protected Point center;

  /**
   * Color to draw cell
   */
  protected Color color = Color.black;

  /**
   * Cell width
   */
  protected double width;

  // =========================================================================
  // ===                      Constructors
  // =========================================================================
  /**
   * Cell constructor.
   *
   * @param label first blob of the cell.
   */
  public Cell(int label)
  {
    labels.add(label);
  }

  /**
   * Cell constructor.
   *
   * @param label first blob of the cell.
   * @param cert is the cell certain.
   */
  public Cell(int label, boolean cert)
  {
    labels.add(label);
    certain = cert;
  }

  /**
   * Cell constructor.
   *
   * @param labelList list of blobs in the cell.
   */
  public Cell(ArrayList<Integer> labelList)
  {
    labels.addAll(labelList);
  }

  /**
   * Cell copy constructor.
   *
   * @param orig cell to copy.
   */
  public Cell(Cell orig)
  {
    labels = new ArrayList<Integer>(orig.labels);

    certain = orig.certain;
    doneFor = orig.doneFor;
    doneBack = orig.doneBack;
    area = orig.area;

    if (orig.center != null)
    {
      center = new Point(orig.center);
    }

    orientation = orig.orientation;
    orientationVariables = orig.orientationVariables;
    width = orig.width;
    color = orig.color;
    

  }

  // =========================================================================
  // ===                        Getters
  // =========================================================================
  /**
   * Returns a boolean indicating if cell already has a daughter cell or not.
   *
   * @return True if cell has a daughter cell, False elsewhere.
   */
  public boolean isDoneForward()
  {
    return doneFor;
  }

  /**
   * Returns a boolean indicating if cell already has a mother cell or not.
   *
   * @return True if cell has a mother cell, False elsewhere.
   */
  public boolean isDoneBackward()
  {
    return doneBack;
  }

  /**
   * Check if the cell is certain or not.
   *
   * @return true : cell is certain; false : it is not.
   */
  public boolean isCertain()
  {
    return certain;
  }

  /**
   * Blobs labels getter.
   *
   * @return ArrayList of blobs labels.
   */
  public ArrayList<Integer> getLabels()
  {
    return labels;
  }

  /**
   * Get label at index 'index' in the list.
   *
   * @param index index of the label to get.
   * @return label at index 'index'
   */
  public int get(int index)
  {
    return labels.get(index);
  }

  /**
   *
   * @return number of blobs in the cell.
   */
  public int blobsNb()
  {
    return labels.size();
  }

  /**
   *
   * @return iterator for the blobs label list.
   */
  public Iterator iterator()
  {
    return labels.iterator();
  }

  /**
   * Is the cell empty ?.
   *
   * @return
   */
  public boolean isEmpty()
  {
    return labels.isEmpty();
  }

  /**
   * returns cell area.
   *
   * @return
   */
  public double getArea()
  {
    return area;
  }

  /**
   * Returns cell orientation.
   *
   * @return
   */
  public double getOrientation()
  {
    return orientation;
  }
  
  public double[] getOrientationVariables()
  {
    return orientationVariables;
  }

  /**
   * Returns cell Center point.
   *
   * @return cell Center point.
   */
  public Point getCenter()
  {
    return center;
  }

  /**
   * Cell color getter.
   *
   * @return cell color.
   */
  public Color getColor()
  {
    return color;
  }

  /**
   * Cell width getter.
   *
   * @return cell width.
   */
  public double getWidth()
  {
    return width;
  }

  // =========================================================================
  // ===                        Setters
  // =========================================================================
  /**
   * Set a cell to certain or uncertain.
   *
   * @param cert boolean certain or not certain.
   */
  public void setCertain(boolean cert)
  {
    certain = cert;
  }

  /**
   * Changes cell area value.
   *
   * @param _area
   */
  public void setArea(double _area)
  {
    area = _area;
  }

  /**
   * Changes cell orientation value.
   *
   * @param _orientation
   */
  public void setOrientation(double _orientation)
  {
    orientation = _orientation;
  }
  
  public void setOrientationVariables(double[] _orientationVariables)
  {
    orientationVariables = _orientationVariables;
  }

  /**
   * Changes cell center point.
   *
   * @param _center new cell Center point
   */
  public void setCenter(Point _center)
  {
    center = _center;
  }

  /**
   * Changes doneFor boolean.
   *
   * @param _done
   */
  public void setDoneForward(boolean _done)
  {
    doneFor = _done;
  }

  /**
   * Changes doneBack boolean.
   *
   * @param _done
   */
  public void setDoneBackward(boolean _done)
  {
    doneBack = _done;
  }

  /**
   * Changes color to draw the cell.
   *
   * @param _col new Color.
   */
  public void setColor(Color _col)
  {
    color = _col;
  }

  /**
   * Change cell width.
   *
   * @param _width new cell width.
   */
  public void setWidth(double _width)
  {
    width = _width;
  }

  // =========================================================================
  // ===                        Public methods
  // =========================================================================
  /**
   * Override toString method.
   *
   * @return a String of the form (blobID, blobID, ..., isCertain)
   */
  @Override
  public String toString()
  {
    String res = "( ";
    for (int lab : labels)
    {
      res += lab + " / ";
    }

//    res = res.replaceAll( ", $", "" );
    res += certain + " )";

    return res;
  }

  // -------------------------------------------------------------------------
  //                         Comparison methods
  // -------------------------------------------------------------------------
  /**
   * Indicates whether some other object is "equal to" this one. Two cells are
   * equals if they got the same blobs labels (order is not important).
   *
   * @param obj object to compare.
   * @return boolean objects are equals or not.
   */
  @Override
  public boolean equals(Object obj)
  {
    // ============ If "obj" has same adress return true ======================
    if (this == obj)
    {
      return true;
    }

    // ============ If "obj" is null return false =============================
    if (obj == null)
    {
      return false;
    }

    // ================ If "obj" is not a cell return false ===================
    if (getClass() != obj.getClass())
    {
      return false;
    }

    // ============= If "obj" has same blobs labels, return true ==============
    //  ie : if "obj has same blobs labels number and contains all labels of this object.
    final Cell other = (Cell) obj;
    if (other.certain == certain)
    {
      if (other.labels.size() == labels.size())
      {
        return labels.containsAll(other.labels);
      }
    }

//      for( int lab : labels )
//      {
//        if (! other.contains( lab ) )
//        {
//          return false;
//        }
//      }
//      
//      return true;
    // ============= If "obj" has not same blobs labels, return false =========
    return false;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return hashCode
   */
  @Override
  public int hashCode()
  {
    int hash = 5;
    hash = 67 * hash + (this.certain ? 1 : 0);

    if (labels != null)
    {
      int size = labels.size();
      for (int i = 0; i < size; i++)
      {
        hash = 67 * hash + this.labels.get(i);
      }
    }

    return hash;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object is less than,
   * equal to, or greater than the specified object.
   *
   * Cells are ordered by : 1 ) value of their first blob label. 2 ) value of
   * their second blob label. 3 ) ... 4 ) their 'certain' flag ( if they have
   * exactly the same blobs labels)
   *
   * @param t
   * @return
   */
  @Override
  public int compareTo(Object t)
  {

    // =================== First obvious tests  ===============================
    if (!(t instanceof Cell))
    {
      throw new UnsupportedOperationException("Object is not a cell.");
    }

    if (equals(t))
    {
      return 0;
    }

    // =================== First obvious tests  ===============================
    Cell other = (Cell) t;

    // --------------------- Compare blobs labels in cells --------------------
    int size = labels.size();
    for (int i = 0; i < size; i++)
    {

      // === if other cell has less blobs, it is superior ===
      if (i >= other.blobsNb())
      {
        return -1;
      }

      // === Compare each blobs label ===
      if (labels.get(i) > other.labels.get(i))
      {
        return 1;
      }
      else if (labels.get(i) < other.labels.get(i))
      {
        return -1;
      }
    }

    // === if cell has less blobs than the other, it is superior ===
    if (size < other.labels.size())
    {
      return 1;
    }

    // --------------------- Compare certain flags ----------------------------
    if (certain && !other.certain)
    {
      return 1;
    }
    else if (!certain && other.certain)
    {
      return -1;
    }
    else
    {
      return 0;
    }
  }

  // -------------------------------------------------------------------------
  //                         CONTAIN AND INTERSECT methods
  // -------------------------------------------------------------------------
  /**
   * Check if the cell contains the blob of label 'lab'.
   *
   * @param lab label of the blob to check.
   * @return true : the cell contains the blobs; false : the cell doesn't
   * contains the blob.
   */
  public boolean contains(int lab)
  {
    return labels.contains(lab);
  }

  /**
   * Check if the cell contains another cell. This happens if all the blobs of
   * the other cell are contained in this one. Equal cells contain each other.
   *
   * @param other cell to check.
   * @return true : other is contained in the cell; false : it is not.
   */
  public boolean contains(Cell other)
  {
    for (int lab : other.labels)
    {
      if (!labels.contains(lab))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the cell intersects another cell : they have at least one blob in
   * common. Equal cells intersects each other.
   *
   * @param other
   * @return
   */
  public boolean intersect(Cell other)
  {
    for (int lab : other.labels)
    {
      if (labels.contains(lab))
      {
        return true;
      }
    }
    return false;
  }

  // -------------------------------------------------------------------------
  //                       ADD OR REMOVE BLOBS
  // -------------------------------------------------------------------------
  /**
   * Ad a blob of label 'lab' to the cell.
   *
   * @param lab
   */
  public void add(int lab)
  {
    if (!contains(lab))
    {
      labels.add(lab);
    }
  }

  /**
   * Remove a blob of label 'lab' from the cell.
   *
   * @param lab
   */
  public void remove(int lab)
  {
    labels.remove((Integer) lab);
  }

  
  
  public void UpdateOrientationFromVariables()
  {
    double a  = orientationVariables[0];
    double b  = orientationVariables[1];
    double c  = orientationVariables[2];
    
    /* eigenvalues equation is lam^2 - (a+c)*lam + ac - b^2 = 0 */
    double B = -a - c;
    double C = a * c - b * b;
    double sq_delta = Math.sqrt(B * B - 4 * 1 * C);
    double pe1 = (-B - sq_delta) * 0.5,
        pe2 = (-B + sq_delta) * 0.5;
    double e1;
    if (Math.abs(pe1) > Math.abs(pe2))
    {
      e1 = pe1;
    }
    else
    {
      e1 = pe2;
    }

    /* first eigenvector */
    double ux, uy;

    if (Math.abs(b) > 0.01)
    {
      ux = 1;
      uy = (e1 - a) / b;
    }
    else if (Math.abs(a - e1) < 0.01)
    {
      ux = 1;
      uy = 0;
    }
    else
    {
      ux = 0;
      uy = 1;
    }
    
    orientation = Math.atan2(uy, ux);
  }
}
