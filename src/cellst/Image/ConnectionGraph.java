package cellst.Image;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Graph of connections between two ShapeFbt in a ShapeSet.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class ConnectionGraph implements Serializable
{

  //==========================================================================
  //                               Variables
  //==========================================================================
  /**
   * HashMap of connections : as key a pair of the connected ShapeFbt labels, as
   * value their border length.
   */
  protected HashMap<Pair, Double> graph = new HashMap<Pair, Double>();

  //==========================================================================
  //                               Constructor
  //==========================================================================
  /**
   * ConnectionGraph constructor. Create an empty graph.
   */
  public ConnectionGraph()
  {
  }

  /**
   * ConnectionGraph copy constructor.
   *
   * @param connG graph to copy.
   */
  public ConnectionGraph(ConnectionGraph connG)
  {
    graph = connG.graph;
  }

  //==========================================================================
  //                               Getters
  //==========================================================================
  public double get(Pair key)
  {
    return graph.get(key);
  }

  //==========================================================================
  //                               Setters
  //==========================================================================
  //==========================================================================
  //                              Public methods
  //==========================================================================
  /**
   * Check if graph contains the given pair.
   *
   * @param pair pair to check.
   * @return true : the pair is in the graph. False : it is not.
   */
  public boolean contains(Pair pair)
  {
    boolean b2 = false;

    for (Map.Entry<Pair, Double> entry : graph.entrySet())
    {
      if (entry.getKey().equals(pair))
      {
        b2 = true;
      }
    }

    return b2;
  }

  /**
   * Get entry set of the connection graph.
   *
   * @return entry set of the connection graph.
   */
  public Set<Map.Entry<Pair, Double>> entrySet()
  {
    return graph.entrySet();
  }

  /**
   * Remove all connections containing the label 's'.
   *
   * @param s label to remove.
   */
  public void remove(int s)
  {
    // === Init to remove list ===
    ArrayList<Pair> toRemove = new ArrayList<Pair>();

    // === Put all Pairs containing s in the toRemove list ===
    for (Map.Entry<Pair, Double> entry : graph.entrySet())
    {
      Pair currPair = entry.getKey();
      if (currPair.contains(s))
      {
        toRemove.add(currPair);
      }
    }

    // === remove all pairs of toRemove list from the graph. ===
    for (Pair pairToRem : toRemove)
    {
      graph.remove(pairToRem);
    }
  }

  /**
   * Add a connection to the graph. If the connection already existed, replace
   * border length.
   *
   * @param S1 first ShapeFbt label of the connection.
   * @param S2 second ShapeFbt label of the connection
   * @param border border length of the connection.
   */
  public void putConnection(int S1, int S2, double border)
  {
    if (S1 == S2)
    {
      throw new IllegalArgumentException("Can't connect a blob to itself.");
    }
    else
    {
      graph.put(new Pair(S1, S2), border);
    }
  }

  /**
   * Remove a connection from the graph.
   *
   * @param S1 first ShapeFbt label of the connection.
   * @param S2 second ShapeFbt label of the connection.
   */
  public void removeConnection(int S1, int S2)
  {
    graph.remove(new Pair(S1, S2));
  }

  /**
   * Remove a connection from the graph.
   *
   * @param pair Pair of labels to remove.
   */
  public void removeConnection(Pair pair)
  {
    graph.remove(pair);
  }

  /**
   * Method searching for all connections of a label 's' and its borders
   * lengths.
   *
   * @param s label to search.
   * @return An HashMap containing as key neighbors label of ShapeFbt with label
   * 's' and as value the borders lengths.
   */
  public HashMap<Integer, Double> getConnections(int s)
  {
    // ======================== Init result HashMap ===========================
    HashMap<Integer, Double> res = new HashMap<Integer, Double>();

    // ======== For each connection, if s is in the connection Pair ===========
    // Add the other connection label to the result HashMap and the border length as value.
    for (Map.Entry<Pair, Double> entry : graph.entrySet())
    {
      Pair currPair = entry.getKey();

      // === if s is in the connection ===
      // Add the neighbor label and the border length to the hashMap.
      if (currPair.getS1() == s)
      {
        res.put(currPair.getS2(), entry.getValue());
      }
      else if (currPair.getS2() == s)
      {
        res.put(currPair.getS1(), entry.getValue());
      }

    }

    return res;
  }

  /**
   * Returns a Set containing the labels of all the neighbours of node 's'
   *
   * @param s label to search.
   * @return A Set containing the labels of all the neighbours of node 's'
   */
  public Set<Integer> getNeighbours(int s)
  {
    // ======================== Init result HashMap ===========================
    Set<Integer> res = new CopyOnWriteArraySet<Integer>();

    // ======== For each connection, if s is in the connection Pair, ===========
    // add the other label to the result.
    for (Pair pair : graph.keySet())
    {
      // === if s is in the connection ===
      // Add the neighbor label and the border length to the hashMap.
      if (pair.getS1() == s)
      {
        res.add(pair.getS2());
      }
      else if (pair.getS2() == s)
      {
        res.add(pair.getS1());
      }

    }

    return res;
  }

  /**
   * Merge ShapeFbt with label 'toRemove' to ShapeFbt with label 'toMerge' in
   * connections graph. New merged ShapeFbt will keep label 'toMerge'.
   *
   * @param toRemove label of the first ShapeFbt to merge.
   * @param toMerge label of the second ShapeFbt to merge.
   */
  public void merge(int toRemove, int toMerge)
  {
    // ============== init toPut HashMap ======================================
    // This map will register all connections to change in the merging.
    HashMap<Integer, Double> toPut = new HashMap<Integer, Double>();

    // ======= remove connection between the two ShapeFbt to merge. ===========
    removeConnection(toRemove, toMerge);

    // =============  For each pair containing the label toRemove. ============
    // If label toMerge was alredy connected to the other label in the pair. 
    //    This means that the two merged ShapeFbt were both connected to this third ShapeFbt.
    //    Register this connection in toPut list with a new border length : 
    //    new border will be the sum of the two old merged ShapeFbts.
    // Else, only ShapeFbt with label labToRemove was connected to this third 
    //    ShapeFbt. So it is registered in the toPut list (with new label toMerge instead of toRemove)
    //    with the same border.
    for (Map.Entry<Pair, Double> entry : graph.entrySet())
    {
      Pair currPair = entry.getKey();
      Integer otherLab = currPair.getOther(toRemove);

      if (otherLab != null)
      {
        Pair otherPair = new Pair(toMerge, otherLab);
        double newBord = graph.get(currPair);

        if (graph.containsKey(otherPair))
        {
          newBord += graph.get(otherPair);
        }

        toPut.put(otherLab, newBord);
      }
    }

    // ========= Remove all connections containing toRemove label =============
    remove(toRemove);

    // ========= Put all registered connections from toPut list ===============
    for (Map.Entry<Integer, Double> entry : toPut.entrySet())
    {
      putConnection(toMerge, entry.getKey(), entry.getValue());
    }
  }

}
