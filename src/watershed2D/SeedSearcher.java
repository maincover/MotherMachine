package watershed2D;

import ij.plugin.*;
import ij.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;


/**
 * This class search the seeds from an Image for a seeded watershed
 * 
 */
public class SeedSearcher {
	
	/**
	 * search the seeds in objects (object with bright peaks or object with uniform intensities)
	 * @param img Image source
	 * @param peaks true if objects have bright peaks
	 * @return array of pixels image with the seeds of objects
	 */
	public static byte[] searchForegroundSeeds1(ImagePlus img,ImagePlus imgBackground,boolean peaks){
		if(peaks==MainDialog.PEAKS)
			return foregroundBrightPeaks(img,imgBackground);
		else
			return foregroundMaxima(img);
	}

	/**
	 * search the seeds in objects with bright peaks
	 * @param img Image source
	 * @return array of pixels image with the seeds of objects
	 */
    public static byte[] foregroundBrightPeaks(ImagePlus img, ImagePlus imgBackground)
    {
    	int width = img.getWidth();
    	int height = img.getHeight();
        ResultsTable rt = new ResultsTable();
        ImagePlus imgDupli = new ImagePlus("duplicated image",img.getProcessor().duplicate());
        ImagePlus img2 = new ImagePlus("duplicated image",img.getProcessor().duplicate());
        img2.getProcessor().smooth();
        GaussianBlur gauss = new GaussianBlur();
        gauss.blur(imgDupli.getProcessor(),16);
        ImageCalculator imcalcul = new ImageCalculator();
        imcalcul.calculate("sub stack", img2, imgDupli);
        img2.getProcessor().invert();
        img2.getProcessor().autoThreshold();
        img2.getProcessor().erode();
        img2.getProcessor().medianFilter();
        img2.updateAndDraw();
    	
        ParticleAnalyzer particle;
		int measures=0;
		measures |= Measurements.CENTROID;
		Analyzer.setMeasurements(measures);
		int measure = Analyzer.getMeasurements();
		int options=0;
		options |= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		rt.reset();
		particle = new ParticleAnalyzer(options,measure,rt,1,500000);
		if (!particle.analyze(img2,img2.getProcessor()))
		{
			System.out.print("erreur lors de la detection des particules");	
		}
		int seeds_nb = rt.getColumn(6).length;
		byte[] foregroundSeeds;
		float[] x_center = rt.getColumn(6);
		float[] y_center = rt.getColumn(7);
		ImageProcessor foregroundSeedsImp = new ByteProcessor(width,height);
		for(int i=0;i<seeds_nb;i++)
		{
			foregroundSeedsImp.set((int)x_center[i],(int)y_center[i], 255);
		}

		ImagePlus foregroundSeedsImg = new ImagePlus("foregroundSeedsImg",foregroundSeedsImp);
		ImagePlus foregroundErodePoints = foregroundErodePoints(imgBackground);


        ImageCalculator imcalcul2 = new ImageCalculator();
        foregroundSeedsImg.duplicate().show();
        imcalcul2.calculate("add stack", foregroundSeedsImg, foregroundErodePoints);
        
        foregroundSeedsImg.show();
        
		foregroundSeeds = (byte[])foregroundSeedsImg.getProcessor().getPixels();
    	return foregroundSeeds;
    }
    
    private static ImagePlus foregroundErodePoints(ImagePlus img)
    {
		MaximumFinder fm = new MaximumFinder(); 
		ImageProcessor ip = img.getProcessor().duplicate();
		ImageProcessor ipDistanceMap = ImageBuilder.buildDistanceMap(new ImagePlus("segmented image",ip)).getProcessor();
		ip.invert();		
		
		ImagePlus imp = new ImagePlus("", ipDistanceMap);
		ImageConverter imageConverter = new ImageConverter(imp);
		imageConverter.convertToGray32();
		
		ByteProcessor ipBackgroundSeedsErodePoints = fm.findMaxima(imp.getProcessor(), 0.5*EDM.ONE, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, true);
		ImagePlus imgErode = new ImagePlus ("ipBackgroundSeedsErodePoints",ipBackgroundSeedsErodePoints);
		imgErode.show();
		//ImageBuilder.display(imgErode);
    	return imgErode;
    }
    
	/**
	 * search the seeds in objects with uniform intensities
	 * @param img Image source
	 * @return array of pixels image with the seeds of objects
	 */
    public static byte[] foregroundMaxima(ImagePlus img)
    {
    	ImagePlus imgDupli = new ImagePlus("foreground image",img.getProcessor().duplicate());
    	imgDupli.getProcessor().smooth();
    	ImageBuilder.adjustBackgroundHistogram(imgDupli);
    	MaximumFinder findMax = new MaximumFinder();
    	ByteProcessor maxIp = findMax.findMaxima(imgDupli.getProcessor(), 20, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, false);
		byte[] foregroundSeeds;
		foregroundSeeds = (byte[])maxIp.getPixels();
		return foregroundSeeds;
    }
    
//	/**
//	 * search the seeds of the background
//	 * @param img Image source
//	 * @return array of pixels image with the seeds of the background
//	 */
//    public static byte[] backgroundPixels1(ImagePlus img)
//    {
//    	ImagePlus imgDupli = new ImagePlus("background image",img.getProcessor().duplicate());
//    	ImageBuilder.adjustBackgroundHistogram(imgDupli);
//    	ImageProcessor ipBackgroundSeeds = imgDupli.getProcessor();
//    	
//    	imgDupli.duplicate().show();
//    	RankFilters rf = new RankFilters();
//    	rf.rank(ipBackgroundSeeds,3,RankFilters.MEDIAN);
//    	imgDupli.duplicate().show();
//    	
//    	ipBackgroundSeeds.threshold(ipBackgroundSeeds.getAutoThreshold()-(ipBackgroundSeeds.getAutoThreshold()/2));
//    	ipBackgroundSeeds.erode();
//    	ipBackgroundSeeds.invert();    	    	
//    	byte[] backgroundSeeds;
//    	backgroundSeeds = (byte[])ipBackgroundSeeds.getPixels();
//    	return backgroundSeeds;
//    }
//    
    
	/**
	 * search the seeds of the background 
	 * no smooth by median filter is necessary
	 * @param img Image source
	 * @return array of pixels image with the seeds of the background
	 */
    public static byte[] backgroundPixels1(ImagePlus img)
    {
    	ImagePlus imgDupli = new ImagePlus("background image",img.getProcessor().duplicate());
    	ImageProcessor ipBackgroundSeeds = imgDupli.getProcessor();    	    	    	
    	ipBackgroundSeeds.threshold(ipBackgroundSeeds.getAutoThreshold());
    	ipBackgroundSeeds.erode();
    	ipBackgroundSeeds.invert();    	    
    	//imgDupli.show();
    	byte[] backgroundSeeds;
    	backgroundSeeds = (byte[])ipBackgroundSeeds.getPixels();
    	return backgroundSeeds;
    }

    
    
    /**
	 * search the seeds in objects with bright peaks
	 * @param img Image source
	 * @return array of pixels image with the seeds of objects
	 */
    public static byte[] foregroundSeedsByUser(ImagePlus img)
    {
    	int width = img.getWidth();
    	int height = img.getHeight();
        ResultsTable rt = new ResultsTable();

        ImagePlus img2 = new ImagePlus("duplicated image",img.getProcessor().duplicate());
    	
        ParticleAnalyzer particle;
		int measures=0;
		measures |= Measurements.CENTROID;
		Analyzer.setMeasurements(measures);
		int measure = Analyzer.getMeasurements();
		int options=0;
		//options |= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		rt.reset();
		particle = new ParticleAnalyzer(options,measure,rt,30,500000);
		if (!particle.analyze(img2,img2.getProcessor()))
		{
			IJ.showMessage("Foreground seeds will be automatically chosen");	
			return null;
		}
		int seeds_nb = rt.getColumn(6).length;
		byte[] foregroundSeeds;
		float[] x_center = rt.getColumn(6);
		float[] y_center = rt.getColumn(7);
		ImageProcessor foregroundSeedsImp = new ByteProcessor(width,height);
		for(int i=0;i<seeds_nb;i++)
		{
			foregroundSeedsImp.set((int)x_center[i],(int)y_center[i], 255);
		}
		ImagePlus foregroundSeedsImg = new ImagePlus("foregroundSeedsImg",foregroundSeedsImp);
		foregroundSeedsImg.show();        
		foregroundSeeds = (byte[])foregroundSeedsImg.getProcessor().getPixels();
    	return foregroundSeeds;
    }
    
    
    
    
    
	/**
	 * search the seeds on the objects of a distance map
	 * @param img distance map image (16 bit)
	 * @return array of pixels image with the seeds of the objects (distance map image)
	 */
	public static byte[] foregroundSeeds2(ImagePlus imgDistance)
	{	
		ImageProcessor ip = imgDistance.getProcessor().duplicate();
		MaximumFinder fm = new MaximumFinder(); 		
		ImagePlus imp = new ImagePlus("", ip);
		ImageConverter imageConverter = new ImageConverter(imp);
		imageConverter.convertToGray32();
		//imp.show();			
		ByteProcessor maxIp = fm.findMaxima(imp.getProcessor(), 0.5*EDM.ONE, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, true, true);		
    	byte[] foregroundSeeds2;
    	foregroundSeeds2 = (byte[])maxIp.getPixels();
		return foregroundSeeds2;
	}

	/**
	 * search the seeds of the background (with a distance map image)
	 * @param img distance map image
	 * @return array of pixels image with the seeds of the background
	 */
	public static byte[] backgroundSeeds2(int[] labels)
	{
		byte[] backgroundSeeds2 = new byte[labels.length];
		for(int i=0; i<backgroundSeeds2.length; i++){
			if(labels[i]==2)
				backgroundSeeds2[i]=(byte)255;
		}
		return backgroundSeeds2;
	}
    /*public static byte[] backgroundPixels1(ImagePlus img){
    	
    	ImageStatistics is = img.getStatistics(Measurements.MIN_MAX);
    	System.out.println("Min : "+is.min+"    Max : "+is.max+"    Mean : "+is.mean);
    	double max = is.max;
    	
    	ImageProcessor ip = img.getProcessor().duplicate();
    	
    	int[] histo = ip.getHistogram();
    	double hMax=0;
    	int indMax=0;
    	for(int i=0; i<histo.length; i++){
    		if(histo[i]>hMax){
    			hMax=histo[i];
    			indMax=i;
    		}
    	}
    	System.out.println("Histo Max : "+ max +"   ind Max : "+ indMax);
    	
    	double a = hMax/(indMax-max);
    	double b = -1;
    	double c = -max*a;

    	System.out.println("a : "+ a +"   c : "+ c);

    	double dist;
    	double distanceMax = 0;
    	int t = indMax;
    	for(int i=indMax; i<max; i++){
    		
    		dist = Math.abs( (a*i + b*histo[i] + c) / ( Math.sqrt(a*a+b*b) ) );
    		
    		if(dist>distanceMax){
    			distanceMax=dist;
    			t = i;
    		}
    			
    	}
    	ip.threshold(t);
    	ip.invert();
    	System.out.println("distance Max : "+ distanceMax +"   t : "+ t);
        byte[] backgroundSeeds = (byte[])ip.getPixels();
        return backgroundSeeds;
    }*/

}
