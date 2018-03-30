package cellst.Image;

import java.io.*;


/**
 * Class containing a pair of integer. 
 * Used to manage connections graph.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Pair implements Serializable
{
  // =========================================================================
  //                    Attributes
  // =========================================================================
  /**
   * First integer.
   */
  protected int S1;
  /**
   * Second integer.
   */
  protected int S2;

  // =========================================================================
  //                      Constructor
  // =========================================================================
  /**
   * Pair constructor.
   *
   * @param s1 first integer.
   * @param s2 second integer.
   */
  public Pair( int s1, int s2 )
  {
    super();
    this.S1 = s1;
    this.S2 = s2;
  }

  // =========================================================================
  //                       Getters
  // =========================================================================
  /**
   * First integer getter.
   *
   * @return first integer.
   */
  public int getS1()
  {
    return S1;
  }

  /**
   * Second integer getter.
   *
   * @return second integer.
   */
  public int getS2()
  {
    return S2;
  }

  /**
   * If the pair contains 's', it returns the other label of the pair, else it returns null.
   * 
   * @param s label to search in the pair.
   * @return other label in the pair, if 's' is in it, else null.
   */
  public Integer getOther( int s )
  {
    if ( S1 == s )
    {
      return S2;
    }
    if ( S2 == s )
    {
      return S1;
    }
    return null;
  }

  // =========================================================================
  //                      Setters
  // =========================================================================
  // =========================================================================
  //                 Public methods
  // =========================================================================
  /**
   * Override equals methods so that two pairs with same labels (event if the order is different) are equals.
   * 
   * @param obj object to compare with.
   * @return 
   */
  @Override
  public boolean equals( Object obj )
  {

    if ( this == obj )
    {
      return true;
    }

    if ( obj == null )
    {
      return false;
    }

    if ( getClass() != obj.getClass() )
    {
      return false;
    }


    final Pair other = ( Pair ) obj;
    if ( other.contains( S1 ) && other.contains( S2 ) )
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Override HashCode so that two pairs with same labels (event if the order is different) will have the same hashCode.
   * 
   * @return hashCode.
   */
  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 53 * hash + ( this.S1 + this.S2 );
    hash = 53 * hash + ( this.S1 * this.S2 );
    return hash;
  }

  /**
   * Method checking if a pair contains a certain int.
   *
   * @param s integer to check.
   * @return True : the integer is in the pair. False : it is not.
   */
  public boolean contains( int s )
  {
    return ( S1 == s || S2 == s );
  }

  /**
   * If the old integer is in the pair, it is replace by the new integer. If the
   * old integer is not in the pair, nothing is done.
   *
   * @param oldS old integer to replace.
   * @param newS new integer to include.
   */
  public void replace( int oldS, int newS )
  {
    if ( S1 == oldS )
    {
      S1 = newS;
    }
    if ( S2 == oldS )
    {
      S2 = newS;
    }

  }

}

