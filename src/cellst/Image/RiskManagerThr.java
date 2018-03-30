/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Image;

import java.util.*;
import java.util.concurrent.*;

import cellst.Main.*;

/**
 * Until there is no more transition to choose 1) Try to solve all that is
 * possible from certains cells. 2) Do a step with all cells certain or not to
 * choose next currTrans.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class RiskManagerThr extends Thread
{

  // ==========================================================================
  //                  Attributes
  // ==========================================================================
  protected Semaphore sem;
  protected BlobSolver blobsolver;
  protected ExecutorService workerPool;

  // ==========================================================================
  //                  Constructor
  // ==========================================================================
  public RiskManagerThr(BlobSolver _blobsolver)
  {
    super();
    blobsolver = _blobsolver;
    sem = new Semaphore(1);
  }

  // ==========================================================================
  //                 Run
  // ==========================================================================
  @Override
  public void run()
  {
    // ============== Open a waiting progressBar to show user it's working ====
    WaitThr waitThread = new WaitThr(this);
    waitThread.start();

    try
    {
      System.out.println("RiskManagerThr started");

      //DEBUG time
      double tStart = System.currentTimeMillis();
      
      boolean changed = true;
      while (changed)
      {

        System.out.println(
            " =============== Compute certain risks ============= ");
        // 1) Try to solve all that is possible from certains cells.
        computeRisks(true);

        while (changed)
        {
          System.out.println("    =============== Solve step =============== ");
          changed = solveStep();
        }

        // 2) Do a step with all cells certain or not to choose next currTrans.
        System.out.println(" =============== Compute risks =============== ");

        computeRisks(false);

        System.out.println(" =============== Solve step =============== ");
        changed = solveStep();

      }

      //DEBUG time
      double tStop = System.currentTimeMillis();

      System.out.
          println("Risk ManagerThr stopped : " + (tStop - tStart) + " ms");

    }
    catch (InterruptedException ex)
    {
      System.out.println("RisksManagerThr interrupted");
      ex.printStackTrace();

      waitThread.interrupt();
      workerPool.shutdownNow();
    }
  }

  // ==========================================================================
  //                  Other methods
  // ==========================================================================
  // -------------------------------------------------------------------------
  //    Choose currTrans with lowest risk
  // -------------------------------------------------------------------------
  /**
   * Choose the lowest risk currTrans from 'risks' and add it to lineage.
   *
   * @return True : one cellTransition was chosen. False : their is no more
   * cellTransition to choose or only impossible ones (infinite risk).
   * @throws java.lang.InterruptedException
   */
  public boolean solveStep() throws InterruptedException
  {
    Utils.checkThreadInterruption();

    // =========== Init local variables ======================================
    double minRisk = Double.POSITIVE_INFINITY;
    CellTransition minRiskTrans = null;

    // =========== Search minimum risk currTrans ============================
    for (Map.Entry<CellTransition, Double> entry : blobsolver.risksFwd.
        entrySet())
    {
      if (entry.getValue() <= minRisk)
      {
        minRisk = entry.getValue();
        minRiskTrans = entry.getKey();
      }
    }

    for (Map.Entry<CellTransition, Double> entry : blobsolver.risksBack.
        entrySet())
    {
      if (entry.getValue() <= minRisk)
      {
        minRisk = entry.getValue();
        minRiskTrans = entry.getKey();
      }
    }

    // ========== If minimum risk doesn't exist ( no more risks ) ===========
    //       return false.
    if (minRiskTrans == null)
    {
      return false;
    }

    System.out.println(
        "minRisk : " + minRisk + "(" + minRiskTrans.toString() + ")");

    // =========== Else get minimum risk currTrans variables =================
    Cell cell1 = minRiskTrans.getMother();
    Cell cell2 = minRiskTrans.getDaughter1();
    Cell cell3 = minRiskTrans.getDaughter2();
    int time = minRiskTrans.time;

    // ========= Update certainity and lineage in currTrans cells ============
    cell1.setCertain(true);
    cell2.setCertain(true);
    cell1.doneFor = true;
    cell2.doneBack = true;
    if (cell3 != null)
    {
      cell3.setCertain(true);
      cell3.doneBack = true;
    }

    // ============ Add the Transition to lineage. ============================
    CellTransition newTrans = new CellTransition(time, cell1, cell2, cell3);
    blobsolver.lineage.add(newTrans);

    // ============ Update Possibles cells ==================
    updatePossibleCellsAfterStep(newTrans);

    // -------------------------Update Risks. ---------------------------------
    RiskUpdateManagerThr run = new RiskUpdateManagerThr(blobsolver, time);
    run.start();
    run.join();

    // ====== Return true =======
    return true;
  }

  // -------------------------------------------------------------------------
  //   Update Possible cells
  // -------------------------------------------------------------------------
  /**
   * Updates possible cells list after a solving step ( adding a currTrans to
   * lineage ). 1 ) Remove cells that intersect new certains cells. 2 )Simplify
   * if one blob can't be a cell by itself and has only one link left.
   *
   * @param trans
   */
  public void updatePossibleCellsAfterStep(CellTransition trans)
  {
    // ================== 1) =================================================
    //  Remove cells that intersect new certains cells.
    ArrayList<Cell> toRemove = new ArrayList<Cell>();

    // --- remove all cells intersecting cell1  from 'time' possible cells currSet
    ShapeSet motherSet = blobsolver.blobListOverMovie.get(trans.time);
    ArrayList<Cell> motherPossCells = motherSet.possibleCells;
    for (Cell currCell : motherPossCells)
    {
      if (!currCell.equals(trans.getMother()) && currCell.intersect(trans.getMother()))
      {
        toRemove.add(currCell);
      }
    }

    motherPossCells.removeAll(toRemove);

    // --- remove all cells intersectingcell2 or cell3 -----------------------
    //           from 'time + 1' possible cells currSet
    toRemove = new ArrayList<Cell>();
    ShapeSet daughtersSet = blobsolver.blobListOverMovie.get(trans.time + 1);
    ArrayList<Cell> daughtersPossCells = daughtersSet.possibleCells;
    for (Cell currCell : daughtersPossCells)
    {
      if (!currCell.equals(trans.getDaughter1()) && currCell.intersect(
          trans.getDaughter1()))
      {
        toRemove.add(currCell);
      }
      else if (trans.getDaughter2() != null && !currCell.equals(trans.getDaughter2())
               && currCell.
          intersect(trans.getDaughter2()))
      {
        toRemove.add(currCell);
      }
    }

    daughtersPossCells.removeAll(toRemove);

    // ================= 2) ===================================================
    // Simplify if one blob can't be a cell by itself and has only one link left.
  /*
     * toRemove = new ArrayList<Cell>(); for( int lab : trans.getMother().labels ) {
     * Set<Integer> neighs = motherSet.getNeighbourLabels(lab);
     *
     * for( Integer currNeigh : neighs ) { Set<Integer> links =
     * motherSet.getNeighbourLabels(currNeigh ); int sumLink = 0; int onlyLink =
     * 0; for( Integer currLink : links ) { if( !trans.getMother().contains(currLink)
     * ) { sumLink ++; onlyLink = currLink; } }
     *
     * if( sumLink == 1 ) { for( Cell currCell : motherPossCells) { if(
     * currCell.contains( onlyLink ) && !currCell.contains( currNeigh )) {
     * toRemove.add(currCell); } }
     *
     * motherPossCells.removeAll(toRemove); } } }
     *
     * toRemove = new ArrayList<Cell>(); for( int lab : trans.getDaughter1().labels )
     * { Set<Integer> neighs = daughtersSet.getNeighbourLabels(lab);
     *
     * for( Integer currNeigh : neighs ) { Set<Integer> links =
     * daughtersSet.getNeighbourLabels(currNeigh ); int sumLink = 0; int
     * onlyLink = 0; for( Integer currLink : links ) { if(
     * !trans.getDaughter1().contains(currLink) ) { sumLink ++; onlyLink = currLink;
     * } }
     *
     * if( sumLink == 1 ) { for( Cell currCell : daughtersPossCells) { if(
     * currCell.contains( onlyLink ) && !currCell.contains( currNeigh )) {
     * toRemove.add(currCell); } }
     *
     * daughtersPossCells.removeAll(toRemove); } } }
     *
     * if( trans.getDaughter2() == null ) { return; }
     *
     * toRemove = new ArrayList<Cell>(); for( int lab : trans.getDaughter2().labels )
     * { Set<Integer> neighs = daughtersSet.getNeighbourLabels(lab);
     *
     * for( Integer currNeigh : neighs ) { Set<Integer> links =
     * daughtersSet.getNeighbourLabels(currNeigh ); int sumLink = 0; int
     * onlyLink = 0; for( Integer currLink : links ) { if(
     * !trans.getDaughter2().contains(currLink) ) { sumLink ++; onlyLink = currLink;
     * } }
     *
     * if( sumLink == 1 ) { for( Cell currCell : daughtersPossCells) { if(
     * currCell.contains( onlyLink ) && !currCell.contains( currNeigh )) {
     * toRemove.add(currCell); } }
     *
     * daughtersPossCells.removeAll(toRemove); } } }
     */
  }

  // -------------------------------------------------------------------------
  //   Compute risk for all cells : certains or not
  // -------------------------------------------------------------------------
  /**
   * Compute all currTranss and divisions risks for all cells and save them in
   * the risks attribute.
   *
   * @param certain If certain, only certains cells risks are computed.
   * @throws java.lang.InterruptedException
   */
  public void computeRisks(boolean certain) throws InterruptedException
  {

    workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().
        availableProcessors());

    blobsolver.risksFwd = new TreeMap<CellTransition, Double>();
    blobsolver.risksBack = new TreeMap<CellTransition, Double>();

    // For each cell (or only certain cells if 'certain' is true),
    // Add all the risks (transitions and divisions) with both adjacent images
    // (backwards and forward).
    Set<Map.Entry<Integer, ShapeSet>> entrySet = blobsolver.blobListOverMovie.
        entrySet();
    for (Map.Entry<Integer, ShapeSet> entry : entrySet)
    {
      ShapeSet currImageBlobs = entry.getValue();
      int time = entry.getKey();

      for (Cell cell : currImageBlobs.possibleCells)
      {
        if (!certain || cell.isCertain())
        {
          workerPool.execute(new RiskWorkingThr(blobsolver, time, cell, true,
                                                sem));
          workerPool.execute(new RiskWorkingThr(blobsolver, time, cell, false,
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

  }

}
