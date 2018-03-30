package Stabilizer;
/**
    Image stabilizer log applier plugin for ImageJ.

    This plugin reapplies the transformations logged by  Image_Stabilizer  to
    another stack.

    Authors:  Kang Li     (kangli AT cs.cmu.edu)
              Steven Kang (sskang AT andrew.cmu.edu)

    Requires: ImageJ 1.38q or later & JRE 5.0 or later

    Installation:
      Download Image_Stabilizer_Log_Applier.java to the plugins folder or its
      subfolder.  Compile and run  it using Plugins/Compile  and Run. Restart
      ImageJ and there will be a new  "Image Stabilizer Log Applier"  command
      in the Plugins menu or its submenu.

    History:
        2009/01/11: First version
*/

/**
Copyright (C) 2009 Kang Li and Steven Kang. All rights reserved.

Permission to use, copy, modify, and distribute this software for any purpose
without fee is hereby granted,  provided that this entire notice  is included
in all copies of any software which is or includes a copy or modification  of
this software  and in  all copies  of the  supporting documentation  for such
software. Any for profit use of this software is expressly forbidden  without
first obtaining the explicit consent of the author.

THIS  SOFTWARE IS  BEING PROVIDED  "AS IS",  WITHOUT ANY  EXPRESS OR  IMPLIED
WARRANTY.  IN PARTICULAR,  THE AUTHOR  DOES NOT  MAKE ANY  REPRESENTATION OR
WARRANTY OF ANY KIND CONCERNING  THE MERCHANTABILITY OF THIS SOFTWARE  OR ITS
FITNESS FOR ANY PARTICULAR PURPOSE.
*/

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.Editor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;

public class Ins_Stabilizer_Log_Applier implements PlugInFilter {

    static final int TRANSLATION = 0;
    static final int AFFINE = 1;

    ImagePlus      imp = null;
    ImageStack     stack = null;
    ImageStack     stackOut = null;
    String         outputDir = null;
    boolean        stackVirtual = false;
    boolean        outputNewStack = false;
    int            transform = TRANSLATION;
    

   
    
    Ins_param pRFP;
    
    public int setup(String arg, ImagePlus imp) {
        IJ.register(Ins_Stabilizer_Log_Applier.class);
        this.imp = imp;
        return DOES_ALL|STACK_REQUIRED;
    }
    
    public void setuppRFP(Ins_param pRFP)
    {
    	this.pRFP = pRFP;
    }


    public void run(ImageProcessor ip) {
        stack = imp.getStack();

        if (stack.isVirtual()) {
            boolean ok = IJ.showMessageWithCancel(
                "Image Stabilizer",
                "You are using a virtual stack.\n" +
                "You will be asked to choose an output directory to save the stablized images.\n" +
                "If you did not intend to use a virtual stack, or if you want to view the stablized images in ImageJ directly,\n" +
                "please reload the image sequence with the 'Use Virtual Stack' option unchecked.");
            if (!ok) return;
            DirectoryChooser dc = new DirectoryChooser("Output Directory");
            outputDir = dc.getDirectory();
            if (outputDir == null || outputDir.length() == 0)
                return;
            File file = new File(outputDir);
            VirtualStack virtualStack = (VirtualStack)stack;
            String stackDir = virtualStack.getDirectory();
            if (null != stackDir) {
                File stackFile = new File(stackDir);
                try {
                    file = file.getCanonicalFile();
                    stackFile = stackFile.getCanonicalFile();
                }
                catch (IOException e) {
                    IJ.error("Could not get canonical file path.");
                    return;
                }
                if (file.equals(stackFile)) {
                    IJ.error("Output directory must be difference from the stack directory.");
                    return;
                }
            }
            stackVirtual = true;
            outputNewStack = true;
        }


        if (!stackVirtual && !showDialog(ip))
            return;

        int current = imp.getCurrentSlice();
        ImageProcessor ipRef = stack.getProcessor(current);

        if (outputNewStack)
            stackOut = new ImageStack(ip.getWidth(), ip.getHeight());

        showProgress(0.0);
        if (!IJ.escapePressed())
            process(ipRef);

        if (!outputNewStack) // in-place processing
            imp.updateAndDraw();
        else if (stackOut.getSize() > 0) {
            // Create new image using the new stack.
            ImagePlus impOut = new ImagePlus(
                "Stablized " + imp.getShortTitle(), stackOut);
            impOut.setStack(null, stackOut);

            // Display the new stacks.
            //impOut.show();
        }
    }


    boolean showDialog(ImageProcessor ip) {
        GenericDialog gd = new GenericDialog("Image Stabilizer");
        if (transform == AFFINE)
            gd.addMessage("-- Applying Affine Image Stabilization --");
        else
            gd.addMessage("-- Applying Translation Image Stabilization --");
        gd.addCheckbox("Output_to_a_New_Stack", false);
        //gd.showDialog();
        //if (gd.wasCanceled())
            //return false;
        outputNewStack = gd.getNextBoolean();
        return true;
    }


    void showProgress(double percent) {
        if (!stackVirtual)
            IJ.showProgress(percent);
    }


    void process(ImageProcessor ipRef)
    {
        int width = ipRef.getWidth();
        int height = ipRef.getHeight();
        int stackSize = stack.getSize();
        int intervalSlices = pRFP.getIntervalSlices();
        
        for (int slice=1; slice <= stackSize; slice++) {
            if (IJ.escapePressed()) 
                break;            
            int interval = 0;
            double[][] wp = pRFP.getWp()[((slice-1)/intervalSlices)*intervalSlices];
            System.out.println("calibrate-stabilize-slice : " +((slice-1)/intervalSlices)*intervalSlices);
            
            if (slice < 1 || slice > stackSize) {
                IJ.showStatus("Skipping slice " + slice + "...");
                continue;
            }
            
            String label = stack.getSliceLabel(slice);
            IJ.showStatus("Stabilizing " + slice + "/" + stackSize + 
                " ... (Press 'ESC' to Cancel)");
            ImageProcessor ip = stack.getProcessor(slice);
            ImageProcessor ipFloat = ip.convertToFloat();

            if (ip instanceof ColorProcessor) {
                ColorProcessor ipColorOut = new ColorProcessor(width, height);

                if (transform == AFFINE)
                    warpColorAffine(ipColorOut, (ColorProcessor)ip, wp);
                else
                    warpColorTranslation(ipColorOut, (ColorProcessor)ip, wp);

                if (stackOut == null) {
                    if (!stackVirtual)
                        stack.setPixels(ipColorOut.getPixels(), slice);
                    else
                        saveImage(ipColorOut, slice);
                }
                else if (interval < 0)
                    stackOut.addSlice(label, ipColorOut, 0);
                else
                    stackOut.addSlice(label, ipColorOut);
            }
            else {

                FloatProcessor ipFloatOut = new FloatProcessor(width, height);

                if (transform == AFFINE)
                    warpAffine(ipFloatOut, ipFloat, wp);
                else
                    warpTranslation(ipFloatOut, ipFloat, wp);

                if (ip instanceof ByteProcessor) {
                    ImageProcessor ipByteOut = ipFloatOut.convertToByte(false);
                    if (stackOut == null) {
                        if (!stackVirtual)
                            stack.setPixels(ipByteOut.getPixels(), slice);
                        else
                            saveImage(ipByteOut, slice);
                    }
                    else if (interval < 0)
                        stackOut.addSlice(label, ipByteOut, 0);
                    else
                        stackOut.addSlice(label, ipByteOut);
                }
                else if (ip instanceof ShortProcessor) {
                    ImageProcessor ipShortOut = ipFloatOut.convertToShort(false);
                    if (stackOut == null) {
                        if (!stackVirtual)
                            stack.setPixels(ipShortOut.getPixels(), slice);
                        else
                            saveImage(ipShortOut, slice);
                    }
                    else if (interval < 0)
                        stackOut.addSlice(label, ipShortOut, 0);
                    else
                        stackOut.addSlice(label, ipShortOut);
                }
                else {
                    if (stackOut == null) {
                        if (!stackVirtual)
                            stack.setPixels(ipFloatOut.getPixels(), slice);
                        else
                            saveImage(ipFloatOut, slice);
                    }
                    else if (interval < 0)
                        stackOut.addSlice(label, ipFloatOut, 0);
                    else
                        stackOut.addSlice(label, ipFloatOut);
                }
            }

            showProgress(slice / (double)stackSize);
        }
    }


    void saveImage(ImageProcessor ip, int slice) {
        VirtualStack virtualStack = (VirtualStack)stack;
        String fileName = null;
        try {
            fileName = virtualStack.getFileName(slice);
        }
        catch (NullPointerException e) {
            // do nothing...
        }
        if (null == fileName || fileName.length() == 0) {
            String title = imp.getTitle();
            String baseName = getBaseName(title);
            Object[] args = { Integer.valueOf(slice) };
            fileName = baseName + String.format("%05d", args) + ".tif";
        }
        else {
            boolean dup = false;
            if (slice > 1 && virtualStack.getFileName(slice - 1) == fileName)
                dup = true; // duplicated name
            String baseName = getBaseName(fileName);
            if (!dup)
                fileName = baseName + ".tif";
            else {
                Object[] args = { Integer.valueOf(slice) };
                fileName = baseName + String.format("%05d", args) + ".tif";
            }
        }
        FileSaver fs = new FileSaver(new ImagePlus(fileName, ip));
        fs.saveAsTiff(outputDir + File.separator + fileName);
    }


    String getBaseName(String fileName) {
        String baseName = fileName;
        int index = fileName.lastIndexOf('.');
        if (index >= 0)
            baseName = fileName.substring(0, index);
        return baseName;
    }


    void warpAffine(ImageProcessor ipOut,
                    ImageProcessor ip,
                    double[][]     wp)
    {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpColorAffine(ImageProcessor ipOut,
                         ColorProcessor ip,
                         double[][]     wp)
    {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = (1.0 + wp[0][0]) * x + wp[0][1] * y + wp[0][2];
                double yy = wp[1][0] * x + (1.0 + wp[1][1]) * y + wp[1][2];
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpTranslation(ImageProcessor ipOut,
                         ImageProcessor ip,
                         double[][]     wp)
    {
        float[] outPixels = (float[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double xx = x + wp[0][0];
                double yy = y + wp[1][0];
                outPixels[p] = (float)ip.getInterpolatedPixel(xx, yy);
                ++p;
            } // x
        } // y
    }


    void warpColorTranslation(ImageProcessor ipOut,
                              ColorProcessor ip,
                              double[][]     wp)
    {
        int[] outPixels = (int[])ipOut.getPixels();
        int width = ipOut.getWidth();
        int height = ipOut.getHeight();
        for (int p = 0, y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                outPixels[p] = (int)ip.getInterpolatedRGBPixel(x + wp[0][0], y + wp[1][0]);
                ++p;
            } // x
        } // y
    }
    
   
}
