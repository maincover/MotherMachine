/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Main;

import cellst.Interface.WaitingDialog;

/**
 *
 * @author mvangkeosay
 */
public class WaitThr extends Thread
{

  // ==========================================================================
  //   ATTRIBUTES
  // ==========================================================================
  private Thread linkedThr;

  // ==========================================================================
  //   CONSTRUCTOR
  // ==========================================================================
  public WaitThr(Thread _link)
  {
    super();
    linkedThr = _link;
  }

  // ==========================================================================
  //   RUN METHOD
  // ==========================================================================
  @Override
  public void run()
  {
    WaitingDialog waitFrame = new WaitingDialog(null, false);
    waitFrame.setRun(linkedThr);
    
    try
    {

      waitFrame.setVisible(true);

      while( linkedThr.isAlive() )
      {
        sleep(1000);
      }
      
      waitFrame.dispose();
    }
    catch (InterruptedException ex)
    {
      System.out.println("Interrupted exception.");
      waitFrame.dispose();
    }

  }
}
