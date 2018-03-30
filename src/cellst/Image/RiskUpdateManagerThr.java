/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Image;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author mvangkeosay
 */
public class RiskUpdateManagerThr extends Thread
{

  protected Semaphore sem;
  protected BlobSolver blobsolver;
  protected int time;
  protected ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.
      getRuntime().availableProcessors());

  public RiskUpdateManagerThr(BlobSolver _blobsolver, int _time)
  {
    super();
    blobsolver = _blobsolver;
    time = _time;
    sem = new Semaphore(1);
  }

  @Override
  public void run()
  {
    try
    {
      System.out.println("RiskUpdateManagerThr started");

      //DEBUG time
      double tStart = System.currentTimeMillis();

      // ============ Remove risk of 'time' from risksforward and backward ======
      // ------------------- Forward --------------------------------------------
      ArrayList<CellTransition> toRem = new ArrayList<CellTransition>();
      for (CellTransition trans : blobsolver.risksFwd.keySet())
      {
        if (trans.time == time - 1 || trans.time == time || trans.time == time
                                                                          + 1)
        {
          toRem.add(trans);
        }
      }

      blobsolver.risksFwd.keySet().removeAll(toRem);

      // ------------------- Backward --------------------------------------------
      toRem = new ArrayList<CellTransition>();
      for (CellTransition trans : blobsolver.risksBack.keySet())
      {
        if (trans.time == time - 1 || trans.time == time || trans.time == time
                                                                          + 1)
        {
          toRem.add(trans);
        }
      }

      blobsolver.risksBack.keySet().removeAll(toRem);

      // ================ For certain cell =========================
      // Add all transitions and divisions risks with both adjacent images to risks shapeList.
      // -------------- Compute back and forward from Slice time ----------------
      ShapeSet set = blobsolver.blobListOverMovie.get(time);

      for (Cell cell : set.possibleCells)
      {
        if (cell.certain)
        {
          workerPool.execute(new RiskWorkingThr(blobsolver, time, cell, true,
                                                sem));
          workerPool.execute(new RiskWorkingThr(blobsolver, time, cell, false,
                                                sem));
        }
      }

      // -------------- Compute backward and forward from Slice time + 1 -----------------
      if (blobsolver.blobListOverMovie.containsKey(time + 1))
      {
        set = blobsolver.blobListOverMovie.get(time + 1);

        for (Cell cell : set.possibleCells)
        {
          if (cell.isCertain())
          {
            workerPool.execute(new RiskWorkingThr(blobsolver, time + 1, cell,
                                                  true, sem));
            workerPool.execute(new RiskWorkingThr(blobsolver, time + 1, cell,
                                                  false, sem));
          }

        }
      }

      // -------------- Compute forward from Slice time - 1 -----------------
      if (blobsolver.blobListOverMovie.containsKey(time - 1))
      {
        set = blobsolver.blobListOverMovie.get(time - 1);

        for (Cell cell : set.possibleCells)
        {
          if (cell.isCertain())
          {
            workerPool.execute(new RiskWorkingThr(blobsolver, time - 1, cell,
                                                  true, sem));
          }
        }
      }

      // -------------- Compute backward from Slice time + 2 -----------------
      if (blobsolver.blobListOverMovie.containsKey(time + 2)
          && blobsolver.blobListOverMovie.
          containsKey(time + 1))
      {
        set = blobsolver.blobListOverMovie.get(time + 2);

        for (Cell cell : set.possibleCells)
        {
          if (cell.isCertain())
          {
            workerPool.execute(new RiskWorkingThr(blobsolver, time + 2, cell,
                                                  false,
                                                  sem));
          }
        }
      }

      // We have submitted all the tasks to be run, the executor service can be
      // shutdown. Thta means it will not accept further task submission but
      // will complete all submitted tasks.
      workerPool.shutdown();
      while (true)
      {
        workerPool.awaitTermination(1, TimeUnit.DAYS);
        if (workerPool.isTerminated())
        {
          break;
        }
      }

      //DEBUG time
      double tStop = System.currentTimeMillis();

      System.out.
          println("RiskUpdateManagerThr stopped : " + (tStop - tStart) + " ms");

    }
    catch (InterruptedException ex)
    {
      System.out.println("RiskUpdateManagerThr interrupted");
      ex.printStackTrace();
      workerPool.shutdownNow();
    }
  }

}
