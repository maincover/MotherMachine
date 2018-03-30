/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 *
 * @author mvangkeosay
 */
public class RiskWorkingThr extends Thread
{

  // ==========================================================================
  //            ATTRIBUTES 
  // ==========================================================================
  private int time;
  private Cell cell;
  private BlobSolver blobsolver;
  private boolean forward;
  private Semaphore sem;
  boolean verbose = false; // TODO removed after debug

  // ==========================================================================
  //            CONSTRUCTOR 
  // ==========================================================================
  public RiskWorkingThr(BlobSolver _blobsolver, int _time, Cell _cell,
                        boolean _forward, Semaphore _sem)
  {
    super();

    blobsolver = _blobsolver;
    time = _time;
    cell = _cell;
    forward = _forward;
    sem = _sem;
  }

  // ==========================================================================
  //            RUN 
  // ==========================================================================
  @Override
  public void run()
  {
    try
    {
      double tStart = 0;
      if (verbose)
      {
        System.out.println("RisksWorkingThr started (" + currentThread().getId()
                           + ")");
        System.out.println("cell = "+cell.toString()+", time = "+time);
        //DEBUG time
        tStart = System.currentTimeMillis();
      }

      if (forward && blobsolver.blobListOverMovie.containsKey(time + 1))
      {
        HashMap<CellTransition, Double> forwardRes = blobsolver.risksForward(time,
                                                                          cell);
        sem.acquire();
        blobsolver.risksFwd.putAll(forwardRes);
        sem.release();
        

      }

      else if (!forward && blobsolver.blobListOverMovie.containsKey(time - 1))
      {
        HashMap<CellTransition, Double> backwardRes = blobsolver.
            risksBackward(time,
                          cell);
        sem.acquire();
        blobsolver.risksBack.putAll(backwardRes);
        sem.release();
        

      }

      if (verbose)
      {
        //DEBUG time
        double tStop = System.currentTimeMillis();

        System.out.println("Risk WorkingThr stopped (" + currentThread().getId()
                           + ") : " + (tStop - tStart) + " ms");
      }
    }
    catch (InterruptedException ex)
    {
      System.out.println("RisksWorkingThr interrupted (" + currentThread().
          getId() + ")");
      ex.printStackTrace();
    }
  }

  // ==========================================================================
  //            GETTERS
  // ==========================================================================
  /**
   * @return the time
   */
  public int getTime()
  {
    return time;
  }

  /**
   * @return the cell
   */
  public Cell getCell()
  {
    return cell;
  }

  /**
   * @return the blobsolver
   */
  public BlobSolver getBlobsolver()
  {
    return blobsolver;
  }

  // ==========================================================================
  //            SETTERS 
  // ==========================================================================
  /**
   * @param time the time to set
   */
  public void setTime(int time)
  {
    this.time = time;
  }

  /**
   * @param cell the cell to set
   */
  public void setCell(Cell cell)
  {
    this.cell = cell;
  }

  /**
   * @param blobsolver the blobsolver to set
   */
  public void setBlobsolver(BlobSolver blobsolver)
  {
    this.blobsolver = blobsolver;
  }

}
