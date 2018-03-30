package normalisation;

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Hashtable;

import Ij_Plugin.Ins_ParticleAnalyzer;
import ij.plugin.ContrastEnhancer;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.*;
import ij.plugin.frame.RoiManager;

public class Ins_renormalisation implements PlugInFilter {
	ImagePlus imp;
	ImagePlus imp_original;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		this.imp_original = imp.duplicate();
		//imp.show();
		//imp.duplicate().show();
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {		
		//ip.invert();						
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		//ImagePlus imp2 = foregroundErodePoints(imp);		
		//ImageProcessor ip = imp2.getProcessor();
		//imp2.show();
		
		ImageProcessor ip_o = ip.duplicate();
		
		
		//ip.convertToShort(false);
		//ip.threshold(2000);
		//ip.autoThreshold();
		//ip.erode();
		imp.show();
		ByteProcessor ip_t = setThreshold((ShortProcessor)ip, (int)ip.getAutoThreshold()-200);
		//ip_t.erode();
		//ip_t.erode();
		ImagePlus imp_t= new ImagePlus("threshold ", ip_t);
		imp_t.show();
		
		imp.updateAndDraw();		
		//System.out.println(" max: "+ip.getMax()+ " min: "+ip.getMin());
		
		//adjustBackgroundHistogram(imp).show();
		
		Ins_ParticleAnalyzer pAnalyzer = new Ins_ParticleAnalyzer(1);
		pAnalyzer.setup("", imp_t);
		pAnalyzer.run(ip_t);				
		
		RoiManager roiManager = pAnalyzer.getRoiManager();				
		Hashtable rois=roiManager.getROIs();
		List list = roiManager.getList();		
		ImagePlus v_plus = IJ.createImage("v_plus", "16bits black", width, height, 1);
		ImagePlus v_minus = IJ.createImage("v_minus", "16bits black", width, height, 1);
		ImageProcessor ip_vPlus = v_plus.getProcessor();
		ImageProcessor ip_vMinus = v_minus.getProcessor();
		
		for(int i=0;i<list.getItemCount();i++)
		{
			
			Roi roi = (Roi) rois.get(list.getItem(i));
			Rectangle rc = roi.getBounds();
			//System.out.println("rc_x : " +rc.getMinX()+" rc_y : "+rc.getMaxX());
			int max = Integer.MIN_VALUE;
			int min = Integer.MAX_VALUE;
			int min_x = 0;
			int min_y = 0;
			int max_x = 0;
			int max_y = 0;
			for(int x=(int)rc.getMinX(); x<rc.getMaxX();x++)
			{
				for(int y=(int)rc.getMinY();y<rc.getMaxY();y++)
				{
					//System.out.println("x : " + x+ " y: " + y + " value: "+ ip_o.get(x, y));
					if (roi.contains(x, y))
					{
						if(max<ip_o.get(x, y))
						{
							max = ip_o.get(x, y);							
							ip_vMinus.putPixel(x, y, max);							
							System.out.println("x : " +x+ " y : "+y+ " max: "+ip_vMinus.get(x, y)+ " roi: "+(i+1));
							//							max_x = x;
//							max_y = y;
						}
						
						if(min>ip_o.get(x, y))
						{
							min = ip_o.get(x, y);
							min_x = x;
							min_y = y;
						}
					}
				}
			}
			//System.out.println("max_x : "+max_x + " max_y : "+ max_y + " max: "+max);
			//imp_max.getProcessor().set(max_x, max_y, max);
			//imp_min.getProcessor().set(min_x, min_y, min);		
			//imp_max.show();
			//imp_min.show();
			//IJ.wait(1000);
		}
		GaussianBlur gB = new GaussianBlur();
		gB.blur(ip_vMinus, 5);
		ImageProcessor ip_ = ip_vMinus.duplicate();
		ContrastEnhancer ce = new ContrastEnhancer();
		
		ShortProcessor sp= subStract((ShortProcessor)ip, (ShortProcessor)ip_vMinus);
		new ImagePlus("", sp).show();
		
		
		ce.stretchHistogram(ip_, 0);
		new ImagePlus("enhancer", ip_).show();
		
		IJ.save(new ImagePlus("vMinus", ip_vMinus), "E:/vMinus.tif");
		
		//v_minus.show();
		//gB.blur(imp_min.getProcessor(), 5);
		
		
		//ImageProcessor ip_ro = imp_original.getProcessor().duplicate();
		//int threshold = ip_ro.getAutoThreshold();
		
		//threshold
		
		
		
		
		//ImageCalculator ic = new ImageCalculator();
		
		//ImagePlus tmp = ic.run("subtract",imp_original, imp_max);
		//tmp.show();
		//imp_max.show();
		//imp_min.show();
		
		
		
		//ic.run("substract",imp_max, imp_min).show();
		//ImagePlus tmp = ic.run("divide", tmp, ic.run("substract",imp_max, imp_min));
		
		//tmp.show();

		//imp.updateAndDraw();
		
	}
	
	public ShortProcessor subStract(ShortProcessor ip1, ShortProcessor ip2)
	{
		short[] p1 = (short [])ip1.getPixels();
		short[] p2 = (short [])ip2.getPixels();		
		if(p1.length != p2.length)
			return null;				
		ShortProcessor sp = (ShortProcessor)ip1.createProcessor(ip1.getWidth(), ip1.getHeight());
		short[] p3 = (short[])sp.getPixels();
		for(int i=0; i<p1.length;i++)
		{
			p3[i] = (short)(p1[i]&0xffff - p2[i]&0xffff);
		}
		
		sp.setPixels(p3);
		return sp;
	}
	
	public ByteProcessor setThreshold(ShortProcessor ip, int thres)
	{
		ByteProcessor ip_t = new ByteProcessor(ip.getWidth(), ip.getHeight());
		short[] pixels = (short [])ip.getPixels();
		byte[] pixels2 = new byte[pixels.length];
		for(int i=0 ; i< pixels.length; i++)
		{
			if((pixels[i]&0xffff) < thres)
				pixels2[i] = 0;
			else {
				pixels2[i] = (byte) 255;
			}
		}
		ip_t.setPixels(pixels2);
		return ip_t;
	}
	
	
	/**
	 * adjust histogram of gray value (~remove a part of noise)
	 * @param img ImagePlus of an image
	 * @return ImagePlus this same image with an adjusted histogram
	 */
	public static ImagePlus adjustBackgroundHistogram(ImagePlus img)
	{
		int[] histosorted = img.getProcessor().getHistogram();
		int[] histo = histosorted.clone();
		Arrays.sort(histosorted);
		int backgroundThreshold=0;
		for (int i=0;i<histo.length;i++)
		{
			if (histosorted[histosorted.length-1]==histo[i])
				backgroundThreshold=i;                                            
		}
		//System.out.println("backgroundThreshold "+backgroundThreshold);
		img.getProcessor().setMinAndMax(backgroundThreshold/2+20,4000);
		int[] table = new int[256];
		int min = (int)img.getProcessor().getMin();
		int max = (int)img.getProcessor().getMax();
		//System.out.println("max: "+max);
		for (int i=0; i<256; i++) {
			if (i<=min)
				table[i] = 0;
			else if (i>=max)
				table[i] = 65535;
			else
				table[i] = (int)(((double)(i-min)/(max-min))*65535);
		}
		//img.getProcessor ().applyTable(table);
		return img;
	}
	
	
    public static ImagePlus foregroundErodePoints(ImagePlus img)
    {
		MaximumFinder fm = new MaximumFinder(); 
		ImageProcessor ip = img.getProcessor().duplicate();
		//ImageProcessor ipDistanceMap = ImageBuilder.buildDistanceMap(new ImagePlus("segmented image",ip)).getProcessor();
		ip.invert();		
		ByteProcessor ipBackgroundSeedsErodePoints = fm.findMaxima(ip, 1, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, true, false);
		ImagePlus imgErode = new ImagePlus ("ipBackgroundSeedsErodePoints",ipBackgroundSeedsErodePoints);
		
    	return imgErode;
    }
    
    public static void main(String[] args)
    {
    	Ins_renormalisation ins_renormalisation = new Ins_renormalisation();
    	ImagePlus imp = IJ.openImage("E:/test.tif");
    	//imp.show();
    	ins_renormalisation.setup("", imp);
    	ins_renormalisation.run(imp.getProcessor());
    }
}
