/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.Interface.BlobSolving;

import ij.*;
import ij.gui.*;

import java.awt.*;

import cellst.Image.*;
import cellst.Main.*;

/**
 * Dialog allowing user to add a new blob to BlobSolver with control over grey
 * threshold and area.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class AddBlobDialog extends javax.swing.JDialog
{

  // ==========================================================================
  //                          CONSTRUCTOR
  // ==========================================================================
  /**
   * Creates new form AddBlobDialog.
   *
   * @param parent the owner of the dialog or null if this dialog has no owner
   * @param modal specifes whether dialog blocks user input to other top-level
   * windows when shown. If false, the dialog is MODELESS; if true, the modality
   * type property is set to DEFAULT_MODALITY_TYPE
   */
  public AddBlobDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    initComponents();
  }

  // ==========================================================================
  //                       GENERATED CODE 
  // ==========================================================================
  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    jLabel1 = new javax.swing.JLabel();
    jScrollBarSpeedB = new javax.swing.JScrollBar();
    jButtonFinish = new javax.swing.JButton();
    jButtonCancel = new javax.swing.JButton();
    jLabelSpeedB = new javax.swing.JLabel();
    jScrollBarMaxIter = new javax.swing.JScrollBar();
    jLabel3 = new javax.swing.JLabel();
    jLabelMaxIter = new javax.swing.JLabel();
    jLabelSpeedA = new javax.swing.JLabel();
    jScrollBarSpeedA = new javax.swing.JScrollBar();
    jButtonPropagate = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    addComponentListener(new java.awt.event.ComponentAdapter()
    {
      public void componentShown(java.awt.event.ComponentEvent evt)
      {
        formComponentShown(evt);
      }
    });

    jLabel1.setText("Viscosity parameters");

    jScrollBarSpeedB.setMaximum(255);
    jScrollBarSpeedB.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
    jScrollBarSpeedB.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    jScrollBarSpeedB.addAdjustmentListener(new java.awt.event.AdjustmentListener()
    {
      public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt)
      {
        jScrollBarSpeedBAdjustmentValueChanged(evt);
      }
    });

    jButtonFinish.setText("Finish");
    jButtonFinish.setPreferredSize(new java.awt.Dimension(100, 30));
    jButtonFinish.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButtonFinishActionPerformed(evt);
      }
    });

    jButtonCancel.setText("Cancel");
    jButtonCancel.setPreferredSize(new java.awt.Dimension(100, 30));
    jButtonCancel.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButtonCancelActionPerformed(evt);
      }
    });

    jLabelSpeedB.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabelSpeedB.setText("0");

    jScrollBarMaxIter.setMaximum(2000);
    jScrollBarMaxIter.setMinimum(1);
    jScrollBarMaxIter.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
    jScrollBarMaxIter.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    jScrollBarMaxIter.addAdjustmentListener(new java.awt.event.AdjustmentListener()
    {
      public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt)
      {
        jScrollBarSpeedBAdjustmentValueChanged(evt);
      }
    });

    jLabel3.setText("Max dilatation iteration");

    jLabelMaxIter.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabelMaxIter.setText("0");

    jLabelSpeedA.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabelSpeedA.setText("0");

    jScrollBarSpeedA.setMaximum(1000);
    jScrollBarSpeedA.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
    jScrollBarSpeedA.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    jScrollBarSpeedA.addAdjustmentListener(new java.awt.event.AdjustmentListener()
    {
      public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt)
      {
        jScrollBarSpeedBAdjustmentValueChanged(evt);
      }
    });

    jButtonPropagate.setText("Validate and Propagate to next slice");
    jButtonPropagate.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButtonPropagateActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jButtonFinish, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButtonPropagate)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(jLabelMaxIter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addGap(12, 12, 12)
        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      .addGroup(layout.createSequentialGroup()
        .addGap(12, 12, 12)
        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      .addGroup(layout.createSequentialGroup()
        .addGap(12, 12, 12)
        .addComponent(jScrollBarSpeedB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGap(12, 12, 12))
      .addGroup(layout.createSequentialGroup()
        .addGap(12, 12, 12)
        .addComponent(jScrollBarMaxIter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGap(12, 12, 12))
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollBarSpeedA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabelSpeedA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabelSpeedB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(jScrollBarSpeedA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabelSpeedA)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(jScrollBarSpeedB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabelSpeedB)
        .addGap(18, 18, 18)
        .addComponent(jLabel3)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(jScrollBarMaxIter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabelMaxIter)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jButtonFinish, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jButtonPropagate))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  // ==========================================================================
  //                    ACTIONS MANAGEMENT METHODS
  // ==========================================================================
  /**
   * OK button pressed : add the blob to BlobSolver.
   *
   * @param evt
   */
  private void jButtonFinishActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonFinishActionPerformed
  {//GEN-HEADEREND:event_jButtonFinishActionPerformed
    // ================ Get parent frame and fluobt ===========================
    BlobsCheckFrame parent = (BlobsCheckFrame) getParent();
    Fluo_Bac_Tracker fluobt = parent.getFluobt();

    // =============== Add blob to the parent frame final blobs ================
    String blobsPath = fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).
        toString();

    blobsSet.addShape(blob);

    Utils.saveObject(blobsSet, blobsPath);

    // ======= reinit imageBlobs roi in parent frame ==========================
    parent.getImageBlobs().setRoi(null);
    parent.preview();

    // ======= Close this dialog ==============================================
    dispose();

  }//GEN-LAST:event_jButtonFinishActionPerformed

  /**
   * One of the scrollbars is changed : recompute blob with new parameters.
   *
   * @param evt
   */
  private void jScrollBarSpeedBAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt)//GEN-FIRST:event_jScrollBarSpeedBAdjustmentValueChanged
  {//GEN-HEADEREND:event_jScrollBarSpeedBAdjustmentValueChanged
    // ================= get new parameters ===================================
    double speedA = jScrollBarSpeedA.getValue() / 100.D;
    double speedB = jScrollBarSpeedB.getValue();
    int maxIter = jScrollBarMaxIter.getValue();

    // ============== Update labels ===========================================
    jLabelSpeedB.setText("" + speedB);
    jLabelMaxIter.setText("" + maxIter);
    jLabelSpeedA.setText("" + speedA);

    // ============== Init variables ==========================================
    BlobsCheckFrame parent = ((BlobsCheckFrame) getParent());
    Fluo_Bac_Tracker fluobt = parent.getFluobt();

    // ============== Compute new blob =======================================
    blob = renormIFbt.BlobFromPoint(pos, blobsSet, maxIter,
                                    speedA, speedB, fluobt.getConn8());

    // ============== If statring point was already in a blob =================
    //        Tell user en close window.
    if (blob == null)
    {
      dispose();

      IJ.showMessage(
          "Starting point of dilatation is already in a blob\n or in background.");

      parent.getImageBlobs().setRoi(null);
      parent.preview();
      return;
    }

    // ============== Compute new roi to show blob ============================
    int length = blob.getPixels().size();

    int[] coordx = new int[length];
    int[] coordy = new int[length];
    int i = 0;
    for (Point pix : blob.getPixels())
    {
      coordx[i] = pix.x;
      coordy[i] = pix.y;

      i++;
    }

    Roi blobRoi = new PointRoi(coordx, coordy, length);

    // ============= Show new computed blob ===================================
    parent.getImageBlobs().setRoi(blobRoi);

    parent.getImageBlobs().setBlobsOrSeedsImage(slice, blobsSet.
        getColorProcessorGraph(fluobt.getIWidth(), fluobt.getIHeight()));
  }//GEN-LAST:event_jScrollBarSpeedBAdjustmentValueChanged

  /**
   * Cancel button pressed : close dialog and do nothing.
   *
   * @param evt
   */
  private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonCancelActionPerformed
  {//GEN-HEADEREND:event_jButtonCancelActionPerformed
    // ======= reinit imageBlobs roi in parent frame ==========================
    BlobsCheckFrame parent = (BlobsCheckFrame) getParent();
    parent.getImageBlobs().setRoi(null);

    // ======= Close this dialog ==============================================
    dispose();
  }//GEN-LAST:event_jButtonCancelActionPerformed

  /**
   * Propagate Button pressed : add the blob to BlobSolver, go to next slide and
   * try to a similar blob at the same place.
   *
   * @param evt
   */
  private void jButtonPropagateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonPropagateActionPerformed
  {//GEN-HEADEREND:event_jButtonPropagateActionPerformed
    // ================ Get parent frame and fluobt ===========================
    BlobsCheckFrame parent = (BlobsCheckFrame) getParent();
    Fluo_Bac_Tracker fluobt = parent.getFluobt();

    // ======== If this is last slice of stack, call finish button method =====
    if (slice >= fluobt.getISize())
    {
      jButtonFinishActionPerformed(null);

      return;
    }

    // =============== Add blob to the parent frame final blobs ================
    String blobsPath = fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).
        toString();

    blobsSet.addShape(blob);

    Utils.saveObject(blobsSet, blobsPath);

    // ============== Add same blob over several images =======================
    slice++;
    parent.setSlice(slice);

    renormIFbt = ImageFbt.load(fluobt.getRenormDir().resolve("slice_" + slice));

    blobsPath = fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).toString();
    blobsSet = (ShapeSet) Utils.loadObject(blobsPath);

    jScrollBarSpeedBAdjustmentValueChanged(null);

  }//GEN-LAST:event_jButtonPropagateActionPerformed

  /**
   * When dialog is shown : place the dialog at the good position and compute at
   * first the potential added blob.
   *
   * @param evt
   */
  private void formComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_formComponentShown
  {//GEN-HEADEREND:event_formComponentShown
    BlobsCheckFrame parentFrame = (BlobsCheckFrame) getParent();

    // ========== Put window at the mouse place ===============================
    Point location = parentFrame.getMousePosition();
    if (location != null)
    {
      setLocation(location);
    }

    // =========== Get slice =================================================
    slice = parentFrame.getSlice();

    // ============ Get renormalized image ====================================
    Fluo_Bac_Tracker fluobt = parentFrame.getFluobt();
    renormIFbt = ImageFbt.load(fluobt.getRenormDir().resolve("slice_" + slice));

    // =========== Get blobs shapeSet ========================================
    String blobsPath = fluobt.getfinalBlobsDir().resolve("Blobs_" + slice).
        toString();
    blobsSet = (ShapeSet) Utils.loadObject(blobsPath);

    // =========== Compute closest local minimum from mouse position ========
    Point localMin = renormIFbt.FindNearlocalMin(pos, 1000);

    // --------- if local minimum not found use mouse position ----------------
    if (localMin == null)
    {
      System.out.println("Couldn't find local minimum.");
    }
    else
    {
      // --------- if localMin is in blob or in background ---------------------------
      //           use mouse position as local min
      int index = renormIFbt.pointToIndex(pos);
      int[] labelList = Utils.ShapeSetToLabels(blobsSet,
                                               fluobt.getIWidth(), fluobt.
          getIHeight(), -1);
      if (renormIFbt.getBackground()[ index] == 0 && labelList[ index] != -1)
      {
        pos = localMin;
      }
    }

    // ====================== Update scrollbars values =======================
    jScrollBarMaxIter.setValue((int) (fluobt.getMinArea() * 2 / (fluobt.
                                                                 getZoom()
                                                                 * fluobt.
                                                                 getZoom())));
    jScrollBarSpeedA.setValue((int) (100 * fluobt.getSpeedA()));
    jScrollBarSpeedB.setValue((int) fluobt.getSpeedB());
  }//GEN-LAST:event_formComponentShown

  // ==========================================================================
  //                       SETTERS
  // ==========================================================================
  public void setPos(Point _pos)
  {
    pos = _pos;
  }

  // ==========================================================================
  //                       MAIN - GENERATED
  // ==========================================================================
  /**
   *
   * @param args the command line arguments
   */
  public static void main(String args[])
  {
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
     */
    try
    {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.
          getInstalledLookAndFeels())
      {
        if ("Nimbus".equals(info.getName()))
        {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    }
    catch (ClassNotFoundException ex)
    {
      java.util.logging.Logger.getLogger(AddBlobDialog.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    }
    catch (InstantiationException ex)
    {
      java.util.logging.Logger.getLogger(AddBlobDialog.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    }
    catch (IllegalAccessException ex)
    {
      java.util.logging.Logger.getLogger(AddBlobDialog.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    }
    catch (javax.swing.UnsupportedLookAndFeelException ex)
    {
      java.util.logging.Logger.getLogger(AddBlobDialog.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the dialog */
    java.awt.EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        AddBlobDialog dialog = new AddBlobDialog(new javax.swing.JFrame(), true);
        dialog.addWindowListener(new java.awt.event.WindowAdapter()
        {
          @Override
          public void windowClosing(java.awt.event.WindowEvent e)
          {
            System.exit(0);
          }
        });
        dialog.setVisible(true);
      }
    });
  }
  // ==========================================================================
  //                       PERSONNAL ATTRIBUTES
  // ==========================================================================
  /**
   * Number of the slice in renormalized stack the blob is add to.
   */
  private int slice;
  /**
   * Local minimum found next to the position of the mouse when it was clicked
   * to add a blob.
   */
  private Point pos;
  /**
   * New blob to add.
   */
  private ShapeFbt blob;
  /**
   * Renormalized ImageFbt of the corresponding slice.
   */
  private ImageFbt renormIFbt;
  /**
   * BlobsSet in the renormalized image.
   */
  private ShapeSet blobsSet;
  // ==========================================================================
  //                       GENERATED ATTRIBUTES 
  // ==========================================================================
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButtonCancel;
  private javax.swing.JButton jButtonFinish;
  private javax.swing.JButton jButtonPropagate;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabelMaxIter;
  private javax.swing.JLabel jLabelSpeedA;
  private javax.swing.JLabel jLabelSpeedB;
  private javax.swing.JScrollBar jScrollBarMaxIter;
  private javax.swing.JScrollBar jScrollBarSpeedA;
  private javax.swing.JScrollBar jScrollBarSpeedB;
  // End of variables declaration//GEN-END:variables
}
