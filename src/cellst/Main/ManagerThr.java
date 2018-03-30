package cellst.Main;

import java.util.concurrent.*;

import cellst.Enums.Step;

/**
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class ManagerThr extends Thread
{
  // ==========================================================================
  //                  Attributes
  // ==========================================================================
  /**
   * Messages box to manage thread progress.
   */
  ArrayBlockingQueue<int[]> msgBox;
  Semaphore sem;

  /**
   * Main Fluo_Bac_Tracker window.
   */
  protected Fluo_Bac_Tracker fluobt;
  /**
   * Beginning step.
   */
  protected Step startStep;
  /**
   * Ending step.
   */
  protected Step stopStep;
  /**
   * Current step.
   */
  protected Step currStep;


  protected int slice;

  /**
   * Thread array and numer of threads.
   */
  protected ExecutorService workerPool = null;

// ==========================================================================
  //                   CONSTRUCTOR
  // ==========================================================================
  /**
   * AllThr constructor.
   *
   * @param _fluobt main Fluo_Bac_Tracker window.
   * @param _startStep
   * @param _stopStep
   * @param _slice
   */
  public ManagerThr(Fluo_Bac_Tracker _fluobt,
                    Step _startStep,
                    Step _stopStep,
                    int _slice)
  {
    // ====== inherited constructor =========
    super();

    // ====== init attributes =========
    fluobt = _fluobt;
    startStep = _startStep;
    stopStep = _stopStep;
    slice = _slice;

  }

 

  // ==========================================================================
  //                   Run method
  // ==========================================================================
  /**
   * Override inherited run method. Run all treatments from the beginning step.
   */
  @Override
  public void run()
  {
    System.out.println("Manager Thread started : ");

    // ============ Initializations =============
    msgBox = new ArrayBlockingQueue<int[]>(fluobt.getISize());
    sem = new Semaphore(1);

    // ============== Open a waiting progressBar to show user it's working ====
    WaitThr waitThread = new WaitThr(this);
    waitThread.start();

    // ============== Real computation ========================================
    try
    {
      //DEBUG time
      double tStart = System.currentTimeMillis();

      // Initialize current computation step for each slice to process
      if (slice == -1)
      {
        for (int i = 1; i <= fluobt.getISize(); i++)
        {
//          System.err.println(fluobt);
//          System.err.println(fluobt.stepsList);
          fluobt.setStep(i, startStep);
        }
      }
      else
      {
        fluobt.setStep(slice, startStep);
      }

      // Submit worker tasks
      if (slice == -1)
      {
        workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < fluobt.getISize(); i++)
        {
          
          workerPool.execute(new WorkingThr(fluobt,
                                            startStep, stopStep,
                                            i + 1,
                                            msgBox, sem));
        }
      }
      else
      {
        workerPool = Executors.newSingleThreadExecutor();
        workerPool.execute(new WorkingThr(fluobt, startStep, stopStep, slice,
                                          msgBox, sem));
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

      // Wait for progress messages from the workers and process them
//      while (true)
//      {
//        int[] msg = msgBox.take();
//
//        System.out.println("Message = [" + msg[0] + ", " + msg[1] + "]");
//        if (slice == -1)
//        {
//          fluobt.stepsList[msg[0] - 1] = Step.values()[msg[1]];
//        }
//        else
//        {
//          fluobt.stepsList[0] = Step.values()[msg[1]];
//        }
//
//        // Tell the interface about the modification
//        upToDate = false;
//
//        // Test whether every task has finished
//        if (workerPool.isTerminated())
//        {
//          break;
//        }
//        else
//        {
//          System.out.println("not term");
//        }
//        
//        if (workerPool.)
//        {
//          System.out.println("not term");
//        }
//      }
      //DEBUG time
      double tStop = System.currentTimeMillis();

      System.out.println("Manager Thread stopped : " + (tStop - tStart));
    } // ====================== If Thread is interrupted, stop it ==============
    catch (InterruptedException e)
    {
      System.out.println("Thread interrupted.");
      interrupt();
      waitThread.interrupt();
    }
  }

  @Override
  public void interrupt()
  {
    super.interrupt();

    if (workerPool != null)
    {
      workerPool.shutdownNow();
    }
  }

  // ==========================================================================
  //                     GETTERS
  // ==========================================================================
  // ==========================================================================
  //                     SETTERS
  // ==========================================================================
}
