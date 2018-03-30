

import java.awt.image.*;
import ij.*;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** This class converts an ImageProcessor to another data type. */
public class Ins_stackTypeConverter {

	private static final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;
	private ImageStack is;
	private int type;
	boolean doScaling = true;
	private int size;
	int width, height;

	public Ins_stackTypeConverter(ImageStack is, boolean doScaling){

		try{
			this.is = is;
			this.size = is.getSize();
			this.doScaling = doScaling;
			ImageProcessor ip = is.getProcessor(1);
			
			if (ip instanceof ByteProcessor){
				type = BYTE;
				for(int i=1; i<=size; i++){
					ip = is.getProcessor(i);
					if ((ip instanceof ByteProcessor)==false)
						throw new InstantiationException();
				}
			}
			else if (ip instanceof ShortProcessor){
				type = SHORT;
				for(int i=1; i<=size; i++){
					ip = is.getProcessor(i);
					if ((ip instanceof ShortProcessor)==false)
						throw new InstantiationException();
				}
			}
			else if (ip instanceof FloatProcessor){
				type = FLOAT;
				for(int i=1; i<=size; i++){
					ip = is.getProcessor(i);
					if ((ip instanceof FloatProcessor)==false)
						throw new InstantiationException();
				}
			}
			else{
				type = RGB;
				for(int i=1; i<=size; i++){
					ip = is.getProcessor(i);
					if ((ip instanceof ColorProcessor)==false)
						throw new InstantiationException();
				}
			}
			
			width = is.getWidth();
			height = is.getHeight();
		}
		catch(InstantiationException e){
			System.out.println("Instanciation Error of StackTypeConverter : non uniform stack");
			System.exit(1);
		}

	}

	/** Converts processor to a ByteProcessor. */
	public ImageStack convertToByte() {
		switch (type) {
			case BYTE:
				return is;
			case SHORT:
				return convertShortToByte();
			case FLOAT:
				return convertFloatToByte();
			case RGB:
				return convertRGBToByte();
			default:
				return null;
		}
	}

	/** Converts a ShortProcessor to a ByteProcessor. */
	ImageStack convertShortToByte() {
		if (doScaling) {
			ImageStack stack = new ImageStack(width, height);
			int ipMin,ipMax;
			double scale = 1.0;
			int min = 65535;
			int max = 0;
			
			
			for(int k=1; k<=size; k++){
				ImageProcessor ip = is.getProcessor(k);
				((ShortProcessor)ip).resetMinAndMax();
				((ShortProcessor)ip).findMinAndMax();
				ipMin = (int)ip.getMin();
				ipMax = (int)ip.getMax();
				if(ipMin<min)
					min = ipMin;
				if(ipMax>max)
					max = ipMax;
			}
			if ((max-min)!=0.0)
				scale = 255.0/(max-min);

			double value;
			ImageProcessor ip;
			short[] pixels16;
			byte[] pixels8;
			for(int k=1; k<=size; k++){
				ip = is.getProcessor(k);
				pixels16 = (short[])ip.getPixels();
				pixels8 = new byte[width*height];
				for (int i=0; i<width*height; i++) {
					value = (pixels16[i]-min)*scale;
					if (value<0f) value = 0f;
					if (value>255f) value = 255f;
					pixels8[i] = (byte)Math.round(value);
				}
				stack.addSlice(is.getSliceLabel(k), new ByteProcessor(width, height, pixels8, ip.getColorModel()) );
			}
			return stack;
			
		} 
		else {
			ImageStack stack = new ImageStack(width, height);
			int value;
			for(int k=1; k<=size; k++){
				ImageProcessor ip = is.getProcessor(k);
				short[] pixels16 = (short[])ip.getPixels();
				byte[] pixels8 = new byte[width*height];
				for (int i=0; i<width*height; i++) {
					value = pixels16[i];
					if (value>255) value = 255;
					pixels8[i] = (byte)value;
				}
				stack.addSlice(is.getSliceLabel(k), new ByteProcessor(width, height, pixels8, ip.getColorModel()));
			}
			return stack;
		}
	}

	/** Converts a FloatProcessor to a ByteProcessor. */
	ImageStack convertFloatToByte() {
		if (doScaling) {
			
			ImageStack stack = new ImageStack(width, height);
			double ipMin,ipMax;
			double scale = 1.0;
			double min = Float.MAX_VALUE;;
			double max = Float.MIN_VALUE;
			
			for(int k=1; k<=size; k++){
				ImageProcessor ip = is.getProcessor(k);
				((FloatProcessor)ip).resetMinAndMax();
				((FloatProcessor)ip).findMinAndMax();
				ipMin = ip.getMin();
				ipMax = ip.getMax();
				if(ipMin<min)
					min = ipMin;
				if(ipMax>max)
					max = ipMax;
			}
			if ((max-min)!=0.0)
				scale = 255.0/(max-min);

			double value;
			ImageProcessor ip;
			float[] pixels32;
			byte[] pixels8;
			for(int k=1; k<=size; k++){
				ip = is.getProcessor(k);
				pixels32 = (float[])ip.getPixels();
				pixels8 = new byte[width*height];
				for (int i=0; i<width*height; i++) {
					value = (pixels32[i]-min)*scale;
					if (value<0f) value = 0f;
					if (value>255f) value = 255f;
					pixels8[i] = (byte)Math.round(value);
				}
				stack.addSlice(is.getSliceLabel(k), new ByteProcessor(width, height, pixels8, ip.getColorModel()) );
			}
			
			return stack;
			
		} else {
			ImageStack stack = new ImageStack(width, height);
			float value;
			ImageProcessor ip;
			float[] pixels32;
			byte[] pixels8;
			for(int k=1; k<=size; k++){
				ip = is.getProcessor(k);
				pixels32 = (float[])ip.getPixels();
				pixels8 = new byte[width*height];
				for (int i=0; i<width*height; i++) {
					value = pixels32[i];
					if (value<0f) value = 0f;
					if (value>255f) value = 255f;
					pixels8[i] = (byte)Math.round(value);
				}
				stack.addSlice(is.getSliceLabel(k),new ByteProcessor(width, height, pixels8, ip.getColorModel()) );
			}
			return stack;
		}
	}

	/** Converts a ColorProcessor to a ByteProcessor. 
		The pixels are converted to grayscale using the formula
		g=r/3+g/3+b/3. Call ColorProcessor.setWeightingFactors() 
		to do weighted conversions. */
	ImageStack convertRGBToByte() {
	
		ImageStack stack = new ImageStack(width, height);
		int c, r, g, b;
		int[] pixels32;
		byte[] pixels8;
		
		for(int k=1; k<=size; k++){
			ImageProcessor ip = is.getProcessor(k);
			//get RGB pixels
			pixels32 = (int[])ip.getPixels();
			
			//convert to grayscale
			double[] w = ColorProcessor.getWeightingFactors();
			double rw=w[0], gw=w[1], bw=w[2];
			pixels8 = new byte[width*height];
			for (int i=0; i < width*height; i++) {
				c = pixels32[i];
				r = (c&0xff0000)>>16;
				g = (c&0xff00)>>8;
				b = c&0xff;
				pixels8[i] = (byte)(r*rw + g*gw + b*bw + 0.5);
			}
			stack.addSlice(is.getSliceLabel(k), new ByteProcessor(width, height, pixels8, null));
		}
		return stack;
	}
	
	/** Converts processor to a ShortProcessor. */
	public ImageStack convertToShort() {
		switch (type) {
			case BYTE:
				return convertByteToShort();
			case SHORT:
				return is;
			case FLOAT:
				return convertFloatToShort();
			case RGB:
				ImageStack tmp = convertRGBToByte();
				Ins_stackTypeConverter sc = new Ins_stackTypeConverter(tmp,doScaling);
				return sc.convertByteToShort();
			default:
				return null;
		}
	}

	/** Converts a ByteProcessor to a ShortProcessor. */
	ImageStack convertByteToShort() {
		
		ImageStack stack = new ImageStack(width, height);
		for(int k=1; k<=size; k++){
			ImageProcessor ip = is.getProcessor(k);
			byte[] pixels8 = (byte[])ip.getPixels();
			short[] pixels16 = new short[width * height];
			for (int i=0; i<width*height; i++) {
				pixels16[i] = (short)(pixels8[i]&0xff);
			}
			stack.addSlice(is.getSliceLabel(k), new ShortProcessor(width, height, pixels16, ip.getColorModel()) );
		}
		return stack;
	}

	/** Converts a FloatProcessor to a ShortProcessor. */
	ImageStack convertFloatToShort() {
		
		ImageStack stack = new ImageStack(width, height);
		double ipMin,ipMax;
		double scale = 1.0;
		double min = Float.MAX_VALUE;
		double max = Float.MIN_VALUE;
		if (doScaling){
			for(int k=1; k<=size; k++){
				ImageProcessor ip = is.getProcessor(k);
				//((FloatProcessor)ip).resetMinAndMax();
				//((FloatProcessor)ip).findMinAndMax();
				ipMin = ip.getMin();
				ipMax = ip.getMax();
				
				if(ipMin < min)
					min = ipMin;
				
				if(ipMax>max)
					max = ipMax;
			}
			if ((max-min)!=0.0)
				scale = 65535.0/(max-min);
			
			//System.out.println("scale : 32-> 16 : "+ scale);
		}
		
		double value;
		for(int k=1; k<=size; k++){
			ImageProcessor ip = is.getProcessor(k);
			float[] pixels32 = (float[])ip.getPixels();
			short[] pixels16 = new short[width*height];
			for (int i=0; i<width*height; i++) {
				if (doScaling)
					{
						value = (pixels32[i]-min)*scale;
						//System.out.println("pixel32 i :" + pixels32[i]);
						//System.out.println("value after 32->16 i :" + value + " scale: " + scale +" max: " + max + " min: "+ min);
					}
				else
					value = pixels32[i];
				if (value<0.0) value = 0.0;
				if (value>65535.0) value = 65535.0;
				pixels16[i] = (short)value;
			}
			stack.addSlice(is.getSliceLabel(k), new ShortProcessor(width, height, pixels16, ip.getColorModel()) );
		}
		
		return stack;
	}

	/** Converts processor to a FloatProcessor. */
	public ImageStack convertToFloat(float[] ctable) {
		switch (type) {
			case BYTE:
				return convertByteToFloat(ctable);
			case SHORT:
				return convertShortToFloat(ctable);
			case FLOAT:
				return is;
			case RGB:
				ImageStack tmp = convertRGBToByte();
				Ins_stackTypeConverter sc = new Ins_stackTypeConverter(tmp,doScaling);
				return sc.convertByteToFloat(null);
			default:
				return null;
		}
	}

	/** Converts a ByteProcessor to a FloatProcessor. Applies a
		calibration function if the calibration table is not null.
		@see ImageProcessor.setCalibrationTable
	 */
	ImageStack convertByteToFloat(float[] cTable) {
		
		ImageStack stack = new ImageStack(width, height);
		byte[] pixels8;
		float[] pixels32;
		for(int k=1; k<=size; k++){
			ImageProcessor ip = is.getProcessor(k);
			pixels8 = (byte[])ip.getPixels();
			pixels32 = new float[width*height];
			if (cTable!=null && cTable.length==256)
				for (int i=0; i<width*height; i++)
					pixels32[i] = cTable[pixels8[i]&255];
			else
				for (int i=0; i<width*height; i++)
					pixels32[i] = pixels8[i]&255;
		    ColorModel cm = ip.getColorModel();
			stack.addSlice(is.getSliceLabel(k), new FloatProcessor(width, height, pixels32, cm) );
		}
		return stack;
	    
	}

	/** Converts a ShortProcessor to a FloatProcessor. Applies a
		calibration function if the calibration table is not null.
		@see ImageProcessor.setCalibrationTable
	 */
	ImageStack convertShortToFloat(float[] cTable) {

		ImageStack stack = new ImageStack(width, height);
		short[] pixels16;
		float[] pixels32;
		for(int k=1; k<=size; k++){
			ImageProcessor ip = is.getProcessor(k);
			pixels16 = (short[])ip.getPixels();
			pixels32 = new float[width*height];
			if (cTable!=null && cTable.length==65536)
				for (int i=0; i<width*height; i++)
					pixels32[i] = cTable[pixels16[i]&0xffff];
			else
				for (int i=0; i<width*height; i++)
					pixels32[i] = pixels16[i]&0xffff;
		    ColorModel cm = ip.getColorModel();
			stack.addSlice(is.getSliceLabel(k), new FloatProcessor(width, height, pixels32, cm) );
		}
		return stack;
		
	}
	
	/** Converts processor to a ColorProcessor. */
	public ImageStack convertToRGB() {
		if (type==RGB)
			return is;
		else {
			ImageStack tmp=null;
			switch (type) {
				case BYTE:
					tmp = is;
				break;
				case SHORT:
					tmp = convertShortToByte();
				break;
				case FLOAT:
					tmp = convertFloatToByte();
				break;
			}
			
			ImageStack stack = new ImageStack(width, height);
			ImageProcessor ip;
			for(int k=1; k<=size; k++){
				ip = tmp.getProcessor(k);
				stack.addSlice(is.getSliceLabel(k), new ColorProcessor(ip.createImage()) );
			}
			return stack;
		}
	}

}
