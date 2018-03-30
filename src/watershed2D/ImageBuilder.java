package watershed2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.EDM;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;////////////////

import java.util.Arrays;


/**
 * This class allow to create image (ImagePlus) of differente type 
 * (background image, gradient image etc...)
 */
public class ImageBuilder {
	
	public static int area = 10;

	/**
	 * build a gradient image from the source image
	 * @param img ImagePlus of the source image
	 * @param peaks true if there are bright peaks in the objects
	 * @return ImagePlus of the gradient image
	 */
	public static ImagePlus gradientImage(ImagePlus img,boolean peaks){
		if(peaks==MainDialog.PEAKS)
			return buildGradientImageWithPeaks(img);
		else
			return buildGradientImageWithoutPeaks(img);
	}

	/**
	 * convert a 16-bits-image to an 8-bits-image
	 * @param img16 ImagePlus of a 16-bits image
	 * @return the converted image to 8-bits
	 */
	public static ImagePlus convertTo8bits(ImagePlus img16)
	{

		if(img16.getType()== ImagePlus.GRAY16) {

			ImageProcessor ip = img16.getProcessor().duplicate();
			ContrastEnhancer ce = new ContrastEnhancer();
			ce.stretchHistogram(ip, 0);

			ImagePlus img8 = new ImagePlus(img16.getTitle()+" 8 bits", ip);
			ImageConverter ic = new ImageConverter(img8);
			ic.convertToGray8();

			return img8;
		}
		else{
			IJ.showMessage("impossible conversion from 16 to 8 bits because the image type isn't 16-bits");
			return null;
		}
	}

	public static ImagePlus adjustZBackgroundHistogram8(final ImagePlus imp)
	{
		
		if(!(imp.getProcessor() instanceof ShortProcessor))
			return null;
		
		//ImagePlus impRes = IJ.createImage(imp.getTitle()+"-normalised", "8-bit black", imp.getWidth(), imp.getHeight(), imp.getStackSize());
		
		int size = imp.getStackSize();
		int maxIndex = -1; // background
		int[] minIndex = new int[size];
		
		for(int s=0; s<size; s++)
		{
			int maxValue = Integer.MIN_VALUE;			
			minIndex[s] = Integer.MIN_VALUE;			
			ImageProcessor sp = imp.getImageStack().getProcessor(s+1);
			int[] hist = sp.getHistogram();			
			for(int i = hist.length-1; i>=0; i--)
			{
				if(hist[i] > 50)
				{					
					if(i > maxIndex)
						maxIndex = i;		
					//System.out.println("i " + i);
					break;
				}
			}			
			for(int i = 0; i<hist.length; i++)
			{
				if(i==0)
					continue;
				
				if(hist[i] > maxValue)
				{
					maxValue = hist[i];
					minIndex[s] = i;
					//System.out.println("i " + i + " hist : " + hist[i]);
				}
			}
		}
		
		ImageStack ims = new ImageStack(imp.getWidth(), imp.getHeight());
		for(int ss=0; ss<size; ss++)
		{
			ImageProcessor sp = imp.getImageStack().getProcessor(ss+1);			
			short[] pixels16orig = (short[])sp.getPixels();
			int length = pixels16orig.length;
			byte[] pixels8 = new byte[length];			
			double min = minIndex[ss];
			double max = maxIndex;			
			double scale;
			if ((max-min)==0.0)
				scale = 1.0;
			else
				scale = 255.0/(max-min);
			double value;
			//System.out.println(" maxIndex " + maxIndex + " minIndex " + min +  " scale : " + scale);
			
			for (int i=0; i<length; i++) {
				value = ((pixels16orig[i]&0xffff)-min)*scale;				
				if (value<0) value = 0;
				if (value>255) value = 255;
				//System.out.println(" pixel value i " + i + " " + value);
				pixels8[i] = (byte)(value+0.5);				
			}
			//new ImagePlus(""+s, new ByteProcessor(imp.getWidth(), imp.getHeight(), pixels8)).show();
			ims.addSlice(new ByteProcessor(imp.getWidth(), imp.getHeight(), pixels8));
		}
		ImagePlus impRes = new ImagePlus("", ims);
	    return impRes;
	}
	
	public static ImagePlus adjustZBackgroundHistogram16(final ImagePlus imp)
	{
		
		if(!(imp.getProcessor() instanceof ShortProcessor))
			return null;
		
		//ImagePlus impRes = IJ.createImage(imp.getTitle()+"-normalised", "8-bit black", imp.getWidth(), imp.getHeight(), imp.getStackSize());
		
		int size = imp.getStackSize();
		int maxIndex = -1; // background
		int[] minIndex = new int[size];
		
		for(int s=0; s<size; s++)
		{
			int maxValue = Integer.MIN_VALUE;			
			minIndex[s] = Integer.MIN_VALUE;			
			ImageProcessor sp = imp.getImageStack().getProcessor(s+1);
			int[] hist = sp.getHistogram();			
			for(int i = hist.length-1; i>=0; i--)
			{
				if(hist[i] > 50)
				{					
					if(i > maxIndex)
						maxIndex = i;		
					//System.out.println("i " + i);
					break;
				}
			}			
			for(int i = 0; i<hist.length; i++)
			{
				if(i==0)
					continue;
				
				if(hist[i] > maxValue)
				{
					maxValue = hist[i];
					minIndex[s] = i;
					//System.out.println("i " + i + " hist : " + hist[i]);
				}
			}
		}
		
		ImageStack ims = new ImageStack(imp.getWidth(), imp.getHeight());
		for(int ss=0; ss<size; ss++)
		{
			ImageProcessor sp = imp.getImageStack().getProcessor(ss+1);			
			short[] pixels16orig = (short[])sp.getPixels();
			int length = pixels16orig.length;
			short[] pixels16 = new short[length];			
			double min = minIndex[ss];
			double max = maxIndex;			
			double scale;
			if ((max-min)==0.0)
				scale = 1.0;
			else
				scale = 65535.0/(max-min);
			double value;
			//System.out.println(" maxIndex " + maxIndex + " minIndex " + min +  " scale : " + scale);
			
			for (int i=0; i<length; i++) {
				value = ((pixels16orig[i]&0xffff)-min)*scale;				
				if (value<0) value = 0;
				if (value>65535) value = 65535;
				//System.out.println(" pixel value i " + i + " " + value);
				pixels16[i] = (short)(value+0.5);				
			}
			//new ImagePlus(""+s, new ByteProcessor(imp.getWidth(), imp.getHeight(), pixels8)).show();
			ims.addSlice(new ShortProcessor(imp.getWidth(), imp.getHeight(), pixels16,imp.getProcessor().getColorModel()));
		}
		ImagePlus impRes = new ImagePlus("", ims);
	    return impRes;
	}

	/**
	 * build a distance map image
	 * @param b_image ImagePlus of a binary image
	 * @param int array of the pixels of borders objects
	 * @return ImagePlus of the distance map
	 */
	public static ImagePlus buildDistanceMap(ImagePlus b_image)
	{
		ImageProcessor b_ip = b_image.getProcessor().duplicate();
		//BinaryFiller bf = new BinaryFiller();
		//bf.run(b_ip);
		b_ip.invert();
		EDM edm = new EDM();

		edm.setup("", b_image);

		ImagePlus im = new ImagePlus("Distance Map",edm.make16bitEDM(b_ip));

		im.updateAndDraw();
		
		return im;

		/* */
	}

	/**
	 * build a distance map image
	 * @param b_image ImagePlus of a binary image
	 * @param int array of the pixels of borders objects
	 * @param name title of the image
	 * @return ImagePlus of the distance map
	 */
	public static ImagePlus buildDistanceMap(ImagePlus b_image, String name)
	{
		ImagePlus im = buildDistanceMap(b_image);
		if(im!=null)  im.setTitle(name);
		return im;
	}

	/**
	 * build a gradient image from an image without bright peaks in the objects
	 * @param img ImagePlus of the source image
	 * @return ImagePlus the gradient image
	 */
	public static ImagePlus buildGradientImageWithoutPeaks(ImagePlus img)
	{
		ImagePlus imgGrad = new ImagePlus("Gradient Magnitude Image", img.getProcessor().duplicate());
		imgGrad.getProcessor().smooth();
		adjustBackgroundHistogram(imgGrad);
		imgGrad.getProcessor().findEdges();
		return imgGrad;
	}

	/**
	 * build a gradient image from an image with bright peaks in the objects
	 * @param img ImagePlus of the source image
	 * @return ImagePlus the gradient image
	 */
	/*public static ImagePlus buildGradientImageWithPeaksForMerging2(ImagePlus img)
	{
		ImagePlus imgGrad = new ImagePlus("Gradient Magnitude Image", img.getProcessor().duplicate());
		imgGrad.getProcessor().smooth();
		ImagePlus imgSmooth = new ImagePlus("Smoothed image",img.getProcessor().duplicate());
		ImagePlus imgMask = new ImagePlus("Mask image",img.getProcessor().duplicate());
		imgMask.getProcessor().smooth();
		GaussianBlur gauss = new GaussianBlur();
		gauss.blur(imgSmooth.getProcessor(),16);
		ImageCalculator imcalcul = new ImageCalculator();
		imcalcul.calculate("sub stack", imgMask, imgSmooth);
		imgMask.getProcessor().autoThreshold();
		ImagePlus imgMaskAndPoints = new ImagePlus("Mask and Points Image", imgMask.getProcessor().duplicate());
		imcalcul.calculate("AND", imgMaskAndPoints, imgSmooth);
		ImagePlus imgWithoutPoints = new ImagePlus("Original Image with black holes", imgGrad.getProcessor().duplicate());
		imgMask.getProcessor().invert();
		imcalcul.calculate("AND", imgWithoutPoints, imgMask);
		ImagePlus imgFinal = new ImagePlus("Image original with smoothed peaks", imgWithoutPoints.getProcessor().duplicate());
		imcalcul.calculate("add stack", imgFinal, imgMaskAndPoints);
		ImagePlus imgFinal2 = new ImagePlus("gradient Image", imgFinal.getProcessor().duplicate());
		GaussianBlur gauss2 = new GaussianBlur();
		gauss2.blur(imgFinal2.getProcessor(),2);
		imgFinal2.getProcessor().findEdges();
		adjustBackgroundHistogram(imgFinal2);
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.stretchHistogram(imgFinal2.getProcessor(), 0);
		//imgFinal2.show();
		return imgFinal2;
	}*/


	/**
	 * build a gradient image from an image with bright peaks in the objects
	 * @param img ImagePlus of the source image
	 * @return ImagePlus the gradient image
	 */
	public static ImagePlus buildGradientImageWithPeaksForMerging2(ImagePlus img)
	{
		ImageProcessor ip1 = img.getProcessor().duplicate();
		ip1.smooth();
		ImagePlus imgGrad = new ImagePlus("Gradient Magnitude Image", ip1);

		ImageProcessor ip2 = img.getProcessor().duplicate();
		GaussianBlur g = new GaussianBlur();
		g.blur(ip2,16);
		ImagePlus imgBlur = new ImagePlus("bluring image",ip2);

		ImageCalculator ic = new ImageCalculator();
		ic.calculate("Min", imgGrad, imgBlur);

		ip1.findEdges();
		ContrastEnhancer cEnh = new ContrastEnhancer();
		cEnh.stretchHistogram(ip1, 0);

		return imgGrad;

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
		img.getProcessor().setMinAndMax(backgroundThreshold, img.getProcessor().getMax());
		int[] table = new int[256];
		int min = (int)img.getProcessor().getMin();
		int max = (int)img.getProcessor().getMax();
		for (int i=0; i<256; i++) {
			if (i<=min)
				table[i] = 0;
			else if (i>=max)
				table[i] = 255;
			else
				table[i] = (int)(((double)(i-min)/(max-min))*255);
		}
		img.getProcessor().applyTable(table);
		return img;
	}
	
	
	public static ShortProcessor adjustBackgroundHistogram(ImageProcessor sp)
	{
		if(!(sp instanceof ShortProcessor))
			return null;
		int[] hist = sp.getHistogram();
		int maxCount = Integer.MIN_VALUE;
		int maxIndex = -1; // background
		int minIndex = -1;
		for(int i = hist.length-1; i>=0; i--)
		{
			if(maxCount<hist[i])
			{
				maxCount = hist[i];
				maxIndex = i;
			}
		}
		for(int i = hist.length-1; i>=0; i--)
		{
			if(hist[i] > area)
			{
				minIndex = i;
				break;
			}
		}
		short[] pixels16orig = (short[])sp.getPixels();
		int size = pixels16orig.length;
		short[] pixels16 = new short[size];
		double min = maxIndex;
		double max = minIndex;
		double scale;
		if ((max-min)==0.0)
			scale = 1.0;
		else
			scale = 65535.0/(max-min);
		double value;
		//System.out.println("maxCount " + maxCount + " maxIndex " + maxIndex + " minIndex " + minIndex +  " scale : " + scale);
		
		for (int i=0; i<size; i++) {
			value = ((pixels16orig[i]&0xffff)-min)*scale;
			if (value<0.0) value = 0.0;
			if (value>65535.0) value = 65535.0;
			pixels16[i] = (short)(value+0.5);
		}
	    return new ShortProcessor(sp.getWidth(), sp.getHeight(), pixels16, sp.getColorModel());
		
	}
	
	public static ImageStack adjustBackgroundHistogramZ(ImageStack ims)
	{
		if(!(ims.getProcessor(1) instanceof ShortProcessor))
			return null;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		
		ImageStack ims2 = ImageStack.create(ims.getWidth(), ims.getHeight(), ims.getSize(), 16);
		for(int i=0; i<ims.getSize(); i++)
		{
			ImageProcessor ip = ims.getProcessor(i+1);
			
			if(min > ip.getMin())
			{
				min = ip.getMin();
			}
			
			int[] hist = ip.getHistogram();
			
			int firstMax = Integer.MIN_VALUE;
			for(int j=hist.length-1; j>=0; j--)
			{
				if(hist[j] > 10)
				{
					firstMax = j;
					break;
				}
			}
			if(max < firstMax)
				max = firstMax;
		}
		System.out.println(" max : " + max + " min : " + min );
		double scale;
		if ((max-min)==0.0)
			scale = 1.0;
		else
			scale = 65535.0/(max-min);
		double value;
		
		int size = ims.getWidth()*ims.getHeight();
		for(int i=0; i<ims.getSize(); i++)
		{
			ImageProcessor ip = ims.getProcessor(i+1).duplicate();
			short[] pixels = (short[])ip.getPixels();
			for (int j=0; j<size; j++) {
				value = ((pixels[j]&0xffff)-min)*scale;
				if (value<0.0) value = 0.0;
				if (value>65535.0) value = 65535.0;
				pixels[j] = (short)(value+0.5);
			}
			ims2.setPixels(pixels, i+1);
		}
		return ims2;
	}

	/**
	 * build a gradient image from an image with bright peaks in the objects
	 * using for the flooding of the first watershed
	 * @param img ImagePlus of the source image
	 * @return ImagePlus the gradient image
	 */
	public static ImagePlus buildGradientImageWithPeaks(ImagePlus img)
	{
		ImagePlus imgGrad = new ImagePlus("Gradient Magnitude Image", img.getProcessor().duplicate());
		imgGrad.getProcessor().smooth();
		int[] histosorted = imgGrad.getProcessor().getHistogram();
		int[] histo = histosorted.clone();
		Arrays.sort(histosorted);
		int backgroundThreshold=0;
		for (int i=0;i<histo.length;i++)
		{
			if (histosorted[histosorted.length-1]==histo[i])
				backgroundThreshold=i;                                            
		}
		imgGrad.getProcessor().setMinAndMax(backgroundThreshold, imgGrad.getProcessor().getAutoThreshold());
		int[] table = new int[256];
		int min = (int)imgGrad.getProcessor().getMin();
		int max = (int)imgGrad.getProcessor().getMax();
		for (int i=0; i<256; i++) {
			if (i<=min)
				table[i] = 0;
			else if (i>=max)
				table[i] = 255;
			else
				table[i] = (int)(((double)(i-min)/(max-min))*255);
		}
		imgGrad.getProcessor ().applyTable(table);
		imgGrad.getProcessor().setMinAndMax(0, 255);
		imgGrad.getProcessor().findEdges();
		imgGrad.updateAndDraw();
		return imgGrad;
	}

	/**
	 * Build the image of the watershed results
	 * @param wData watershed data
	 * @return ImagePlus binary image after the watershed
	 */
	public static ImagePlus buildSegmentedRegionImage(WatershedData wData)
	{
		if(wData!=null && wData.isDone()){

			int width = wData.getWidth();
			int height = wData.getHeight();
			byte[] pixels = new byte[width*height];

			int[] labels = wData.getLabels();
			int index;
			for(int i=0; i<width ; i++){
				for(int j=0; j<height ; j++){

					index = i+j*width;

					if(labels[index]==2){
						// background pixel
						pixels[index]=(byte)0;
					}
					else {
						if(labels[index]>2){
							if(labels[index]%2==0){
								// object pixel
								pixels[index]=(byte)255;
							}
							else{
								// label odd => label not definitive => ERROR
								System.out.println("ERROR : label odd ("+(labels[index])+") at pixel : "+i+","+j);
							}
						}else{
							// label of no region
							System.out.println("ERROR : label of no region ("+(labels[index])+") at pixel : "+i+","+j);
						}
					}
				}
			}

			// coordinates of pixels of watershed line
			int[][] pixlist = wData.getSetOfWL().getPixelList(false);

			if(pixlist!=null)
				// for each pixel of WL
				for(int i=0; i<pixlist.length; i++){
					pixels[pixlist[i][0] + pixlist[i][1]*width] = 0;
				}		

			ByteProcessor bp = new ByteProcessor(width, height, pixels,null);
			bp.invert();
			ImagePlus segmentedRegionImage = new ImagePlus("Segmented Regions",bp);


			return segmentedRegionImage;
		}
		else{
			return null;
		}
	}

	/**
	 * Build the image of the watershed results
	 * @param wData watershed data
	 * @param name name of the image
	 * @return binary regions image
	 */
	public static ImagePlus buildSegmentedRegionImage(WatershedData wData, String name)
	{
		ImagePlus im = buildSegmentedRegionImage(wData);
		if(im!=null)  im.setTitle(name);
		return im;
	}

	/**
	 * Build the image of the watershed results (image source + watershed lines)
	 * @param wData watershed data
	 * @param img byte array of the image source pixels 
	 * @return ImagePlus results image
	 */
	public static ImagePlus buildResultImage(WatershedData wData, byte[] img)
	{
		if(wData!=null && wData.isDone())
		{
		// region labels tab?
		int[] labels = wData.getLabels();
		// coordinates of pixels of watershed line
		int[][] pixlist = wData.getSetOfWL().getPixelList(true);

		int width = wData.getWidth();
		int height = wData.getHeight();

		ColorProcessor cp = new ColorProcessor(width, height);

		// region colors tab
		byte[][] regionColor = new byte[wData.getNbSeeds()][3];


		regionColor[0][0] = (byte)255;
		regionColor[0][1] = (byte)255;
		regionColor[0][2] = (byte)255;
		for(int i=1; i<regionColor.length; i++){
			regionColor[i][0] = (byte)(Math.random()*255);
			regionColor[i][1] = (byte)(Math.random()*255);
			regionColor[i][2] = (byte)(Math.random()*255);
		}

		int label, index, regionIndex;

		byte[] red = img.clone();
		byte[] green = img.clone();
		byte[] blue = img.clone();


		// for each pixel of WL
		for(int i=0; i<pixlist.length; i++){
			index = pixlist[i][0]+ pixlist[i][1]*width;
			// label associated to the pixel
			label = labels[index];
			regionIndex = (label/2) - 1;
			// pixel of a region : the color of the region is associated
			red[index]=(byte)regionColor[regionIndex][0];
			green[index]=(byte)regionColor[regionIndex][1];
			blue[index]=(byte)regionColor[regionIndex][2];

		}

		cp.setRGB(red, green, blue);
		ImagePlus damsImage = new ImagePlus("Dams image", cp);

		return damsImage;

		}
		else{
			return null;
		}
	}

	/**
	 * Build the image of the watershed results (image source + watershed lines)
	 * @param wData watershed data
	 * @param img byte array of the image source pixels 
	 * @param name title of the result image
	 * @return ImagePlus results image
	 */
	public static ImagePlus buildResultImage(WatershedData wData, byte[] img, String name)
	{
		ImagePlus im = buildResultImage(wData, img);
		if(im!=null)  im.setTitle(name);
		return im;
	}

	/**
	 * Build the binary image of watershed lines
	 * @param wData watershed data
	 * @return ImagePlus lines image
	 */
	public static ImagePlus buildWLImage(WatershedData wData){
		if(wData!=null && wData.isDone()){

			int width = wData.getWidth();
			int height = wData.getHeight();

			// coordinates of pixels of watershed line
			int[][] pixlist = wData.getSetOfWL().getPixelList(true);
			byte[] pixels = new byte[width*height];

			// for each pixel of WL
			for(int i=0; i<pixlist.length; i++){
				pixels[pixlist[i][0]+ pixlist[i][1]*width]=(byte)255;
			}


			ByteProcessor bp = new ByteProcessor(width, height, pixels,null);
			ImagePlus WLImage = new ImagePlus("Watershed Lines",bp);


			return WLImage;
		}
		else{
			return null;
		}
	}

	/**
	 * Build the binary image of watershed lines
	 * @param wData watershed data
	 * @param name name of the image
	 * @return watershed lines image
	 */
	public static ImagePlus buildWLImage(WatershedData wData, String name)
	{
		ImagePlus im = buildWLImage(wData);
		if(im!=null)  im.setTitle(name);
		return im;
	}

	/**
	 * build coloured regions image
	 * @param wData watershed data
	 * @return coloured regions image
	 */
	public static ImagePlus buildRegionImage(WatershedData wData){

		if(wData!=null && wData.isDone()){
			int width = wData.getWidth();
			int height = wData.getHeight();
			ColorProcessor cpRegion = new ColorProcessor(width, height);
			ImagePlus impRegion = new ImagePlus("Coloured Regions",cpRegion);
			int[] labels = wData.getLabels();
			int[][]  regionColor = wData.getRegionColor();

			int[] whiteColor = new int[3];
			whiteColor[0]=255;
			whiteColor[1]=255;
			whiteColor[2]=255;

			int[] redColor = new int[3];
			redColor[0]=255;
			redColor[1]=0;
			redColor[2]=0;

			for(int i=0; i<width ; i++){
				for(int j=0; j<height ; j++){
					if(labels[i+j*width]==2)
						// background pixel
						cpRegion.putPixel(i, j, whiteColor);
					else if(labels[i+j*width]>2){

						if(labels[i+j*width]%2==0)
							// object pixel
							cpRegion.putPixel(i, j, regionColor[ (labels[i+j*width]/2) - 1 ]);
						else{
							// label odd => label not definitive => ERROR
							System.out.println("ERROR : label odd ("+(labels[i+j*width])+") at pixel : "+i+","+j);
						}
					}
				}
			}
			return impRegion;
		}
		else{
			return null;
		}

	}
	/**
	 * build coloured regions image
	 * @param wData watershed data
	 * @return coloured regions image
	 */
	/*public static ImagePlus buildRegionImage(WatershedData wData){/////////////////

		if(wData!=null && wData.isDone()){
			int width = wData.getWidth();
			int height = wData.getHeight();
			ShortProcessor cpRegion = new ShortProcessor(width, height);
			ImagePlus impRegion = new ImagePlus("Coloured Regions",cpRegion);
			int[] labels = wData.getLabels();
			//int[][]  regionColor = wData.getRegionColor();
			int collisionLabel = wData.getCollisionLabel();

			int[] whiteColor = new int[3];
			whiteColor[0]=255;
			whiteColor[1]=255;
			whiteColor[2]=255;

			int[] redColor = new int[3];
			redColor[0]=255;
			redColor[1]=0;
			redColor[2]=0;
			int color=0;
			for(int i=0; i<width ; i++){
				for(int j=0; j<height ; j++){
					if(labels[i+j*width]==2)
						// background pixel
						cpRegion.putPixel(i, j, 0);
					else if(labels[i+j*width]>2 && labels[i+j*width]<collisionLabel){

						if(labels[i+j*width]%2==0)
							// object pixel
							cpRegion.putPixel(i, j, labels[i+j*width]);
						else{
							// label odd => label not definitive => ERROR
							System.out.println("ERROR : label odd ("+(labels[i+j*width])+") at pixel : "+i+","+j);
						}
					}
					else
						// collision pixel
						cpRegion.putPixel(i, j, redColor);
				}
			}
			return impRegion;
		}
		else{
			return null;
		}

	}*/


	/**
	 * Build the strength of watershed lines image
	 * @param wData watershed data
	 * @param name name of the image
	 */
	public static ImagePlus buildRegionImage(WatershedData wData, String name){
		ImagePlus im = buildRegionImage(wData);
		if(im!=null)  im.setTitle(name);
		return im;
	}



	/**
	 * Display an ImagePlus by duplication
	 * @param im ImagePlus to display
	 */
	public static void display(ImagePlus im){
		ImagePlus img = new ImagePlus(im.getTitle(), im.getProcessor().duplicate());
		img.show();
	}

	/**
	 * Display an ImagePlus by duplication
	 * @param im ImagePlus to display
	 */
	public static void display(String name, int width, int height, byte[] pix){
		ImagePlus img = new ImagePlus(name, new ByteProcessor(width,height,pix,null));
		img.show();
	}

	
	public static void main(String[] args)
	{
		ImagePlus imp = IJ.openImage();
		imp.show();
		
		ImageStack ims = imp.getImageStack();
		ImageStack ims2 = ImageBuilder.adjustBackgroundHistogramZ(ims);
		//ShortProcessor sp = ImageBuilder.adjustBackgroundHistogram((ShortProcessor)imp.getProcessor());
		
		new ImagePlus("", ims2).show();
		
		
	}



}


