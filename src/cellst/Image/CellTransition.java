package cellst.Image;

import java.io.*;

/**
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class CellTransition implements Comparable, Serializable
{

  // =========================================================================
  //                    Attributes
  // =========================================================================
  /**
   * Transition time. This of the ShapeSet containing the mother cell.
   */
  protected int time;

  /**
   * Mother cell.
   */
  private Cell mother;

  /**
   * Daughter cell.
   */
  private Cell daughter1;

  /**
   * Optional second daughter cell. If transition is not a division, it is null.
   */
  private Cell daughter2;

  // =========================================================================
  //                      Constructor
  // =========================================================================
  /**
   *
   * @param _t
   * @param _cell1
   * @param _cell2
   */
  public CellTransition(int _t, Cell _cell1, Cell _cell2)
  {
    time = _t;
    mother = _cell1;
    daughter1 = _cell2;
    daughter2 = null;
  }

  /**
   * Constructs a cell transition from '_cell1' at time 't' to 'cell2' and
   * 'cell3'. Daughters cells are ordered with compareTo method.
   *
   * @param _t
   * @param _cell1
   * @param _cell2
   * @param _cell3
   */
  public CellTransition(int _t, Cell _cell1, Cell _cell2, Cell _cell3)
  {
    time = _t;
    mother = _cell1;

    if (_cell3 == null)
    {
      daughter1 = _cell2;
      daughter2 = _cell3;
    }
    else if (_cell2.compareTo(_cell3) < 0)
    {
      daughter1 = _cell2;
      daughter2 = _cell3;
    }
    else if (_cell2.compareTo(_cell3) > 0)
    {
      daughter1 = _cell3;
      daughter2 = _cell2;
    }
    else
    {
      System.out.println(
          "Error in cell Transition constructor : Daughter cells are equals.");
      throw new IllegalArgumentException("Error in cell Transition constructor : Daughter cells are equals.");
//      daughter1 = _cell2;
    }
  }

  public CellTransition(CellTransition other)
  {
    time = other.time;
    mother = new Cell(other.mother);
    daughter1 = new Cell(other.daughter1);
    if (other.daughter2 != null)
    {
      daughter2 = new Cell(other.daughter2);
    }
    else
    {
      daughter2 = null;
    }
  }

  // =========================================================================
  //                       Getters
  // =========================================================================
  /**
   * Time getter.
   *
   * @return time : chronological parameter of the corresponding ShapeSet of
   * mother.
   */
  public int getT()
  {
    return time;
  }

  /**
   * mother cell getter.
   *
   * @return first cell.
   */
  public Cell getMother()
  {
    return mother;
  }

  /**
   * First daughter cell getter.
   *
   * @return second cell.
   */
  public Cell getDaughter1()
  {
    return daughter1;
  }

  /**
   * Second daughter cell getter.
   *
   * @return second daughter cell.
   */
  public Cell getDaughter2()
  {
    return daughter2;
  }
  
  // =========================================================================
  //                      Setters
  // =========================================================================

  /**
   * @param mother the mother to set
   */
  public void setMother(Cell mother)
  {
    this.mother = mother;
  }

  /**
   * @param daughter1 the daughter1 to set
   */
  public void setDaughter1(Cell daughter1)
  {
    this.daughter1 = daughter1;
  }

  /**
   * @param daughter2 the daughter2 to set
   */
  public void setDaughter2(Cell daughter2)
  {
    this.daughter2 = daughter2;
  }

  
  // =========================================================================
  //                 Public methods
  // =========================================================================
  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * Transitions with same time, same mother and same daughters are equals.
   * (even if the order is different)
   *
   * @param obj object to compare with.
   * @return
   */
  @Override
  public boolean equals(Object obj)
  {
    // ============ If "other" has same adress return true ====================
    if (this == obj)
    {
      return true;
    }

    // ============ If "other" is null return false ===========================
    if (obj == null)
    {
      return false;
    }

    // ========= If "other" is not a cellTransition return false ==============
    if (getClass() != obj.getClass())
    {
      return false;
    }

    // ===== If "other" has same time, mother and daughters return true =======
    final CellTransition other = (CellTransition) obj;
    if (other.mother.equals(mother) && other.time == time
        && sameDaughters(other))
    {
      return true;
    }

    // ======== Else return false =============================================
    return false;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return hashCode.
   */
  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + this.time;
    hash = 31 * hash + (this.mother != null ? this.mother.hashCode() : 0);

    int D1 = (this.daughter1 != null ? this.daughter1.hashCode() : 0);
    int D2 = (this.daughter2 != null ? this.daughter2.hashCode() : 0);

    hash = 31 * hash + D1 * D2;
    hash = 31 * hash + D1 + D2;
    return hash;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object is less than,
   * equal to, or greater than the specified object.
   *
   * Comparison is done with 1) time 2) mother cell. 3) daughter1. 4) daughter2.
   *
   *
   *
   * @param obj object to compare.
   * @return a negative integer, zero, or a positive integer as this object is
   * less than, equal to, or greater than the specified object.
   * @throws ClassCastException if the specified object's type prevents it from
   * being compared to this object.
   */
  @Override
  public int compareTo(Object obj)
  {
    // 1) Test equality
    // 2) Compare the cellTransitions times
    // 3) Compare the cellTransitions mother cells
    // 4) Compare the cellTransitions first daughter cells
    // 5) Compare the cellTransitions second daughter cells

    // =========== If obj is not a cellTransition, raise an error =============
    if (!(obj instanceof CellTransition))
    {
      throw new ClassCastException("Object is not a cellTransition.");
    }

    // 1) Test equality
    if (equals(obj))
    {
      return 0;
    }

    // ------------------------------------
    // 2) Compare the cellTransitions times
    // ------------------------------------
    CellTransition other = (CellTransition) obj;
    if (other.getT() > this.time)
    {
      return -1;
    }
    else if (other.getT() < this.time)
    {
      return 1;
    }
    else
    {
      // ======= Then compare the cellTransitions mother cells ===============
      int comp = mother.compareTo(other.mother);
      if (comp != 0)
      {
        return comp;
      }

      // 4) Compare the cellTransitions first daughter cells
      else
      {
        comp = daughter1.compareTo(other.daughter1);

        if (comp != 0)
        {
          return comp;
        }

        // 5) Compare the cellTransitions second daughter cells
        else
        {
          if (daughter2 == null && other.daughter2 == null)
          {
            return 0;
          }
          else if (daughter2 == null)
          {
            return -1;
          }
          else if (other.daughter2 == null)
          {
            return 1;
          }
          else
          {
            return daughter2.compareTo(other.daughter2);
          }
        }
      }
    }
  }

  /**
   * Method checking if a pair contains a certain cell.
   *
   * @param c cell to check.
   * @return True : the cell is in the pair. False : it is not.
   */
  public boolean contains(Cell c)
  {
    return (mother.equals(c) || daughter1.equals(c)
            || (daughter2 != null && daughter2.equals(c)));
  }

  /**
   * Method checking if a transition intersect with one of its cells a certain
   * cell.
   *
   * @param c cell to check.
   * @return True : the cell is intersect by the transition. False : it is not.
   */
  public boolean intersect(Cell c)
  {
    return (mother.intersect(c) || daughter1.intersect(c) || (daughter2 != null
                                                              && daughter2.
                                                              intersect(c)));
  }

  /**
   * If the old cell is in the pair, it is replace by the new cell. If the old
   * cell is not in the pair, nothing is done.
   *
   * @param oldC old cell to replace.
   * @param newC new cell to include.
   */
  public void replace(Cell oldC, Cell newC)
  {
    if (mother.equals(oldC))
    {
      setMother(newC);
    }
    if (daughter1.equals(oldC))
    {
      setDaughter1(newC);
    }
    if (daughter2 != null && daughter2.equals(oldC))
    {
      setDaughter2(newC);
    }
  }

  /**
   * Check if two cell transitions have the same time and daughters as cells.
   * Daughters order is not important.
   *
   * @param other other cellTransition.
   * @return boolean
   */
  public boolean sameDaughters(CellTransition other)
  {
    // ============= If transitions time are differents, return false =========
    if (time != other.time)
    {
      return false;
    }

    // ============ If both transitions are not divisions =====================
    // Just check that daughter1 are equals.
    if (daughter2 == null && other.daughter2 == null)
    {
      return daughter1.equals(other.daughter1);
    }

    // ============ If both transitions are divisions =========================
    // Check that either daughter1 = other.daughter1 and daughter2 = other.daughter2
    // Or daughter2 = other.daughter1 and daughter1 = other.daughter2
    // So that the order is not important. 
    else if (daughter2 != null && other.daughter2 != null)
    {
      if (daughter1.equals(other.daughter1) && daughter2.equals(other.daughter2))
      {
        return true;
      }
      else if (daughter1.equals(other.daughter2) && daughter2.equals(
          other.daughter1))
      {
        return true;
      }
    }

    // ====== If the conditions are not respected, return false ===============
    return false;

  }

  /**
   * Override toString method.
   *
   * @return a String describing the cellTransition.
   */
  @Override
  public String toString()
  {
    if (daughter2 != null)
    {
      return "[ " + time + " , " + mother.toString() + " => " + daughter1.
          toString() + " + " + daughter2.toString() + " ]";
    }
    else
    {
      return "[ " + time + " , " + mother.toString() + " => " + daughter1.
          toString() + " ]";
    }
  }

  /**
   * Update cells certainity after cell transition was removed.
   *
   * @param motherSet
   * @param daughtersSet
   */
  public void updateForRemove(ShapeSet motherSet, ShapeSet daughtersSet)
  {
    int motherIndex = motherSet.possibleCells.indexOf(mother);
    if (motherIndex >= 0)
    {
      setMother(motherSet.possibleCells.remove(motherIndex));
      mother.doneFor = false;
      mother.certain = (mother.doneBack || motherSet.isCellCertain(
                           mother));
      motherSet.possibleCells.add(mother);
    }

    int daughter1Index = daughtersSet.possibleCells.indexOf(daughter1);
    if (daughter1Index >= 0)
    {
      setDaughter1(daughtersSet.possibleCells.remove(daughter1Index));
      daughter1.doneBack = false;
      daughter1.certain = (daughter1.doneFor || daughtersSet.isCellCertain(
                             daughter1));
      daughtersSet.possibleCells.add(daughter1);
    }

    int daughter2Index = daughtersSet.possibleCells.indexOf(daughter2);
    if (daughter2Index >= 0)
    {
      setDaughter2(daughtersSet.possibleCells.remove(daughter2Index));
      daughter2.doneBack = false;
      daughter2.certain = (daughter2.doneFor || daughtersSet.isCellCertain(
                             daughter2));
      daughtersSet.possibleCells.add(daughter2);
    }

  }

}
