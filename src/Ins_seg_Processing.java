import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;

import java.util.Arrays;

import Stabilizer.Ins_param;



public class Ins_seg_Processing {
	final ImagePlus img;
	final float[] channel_prefix_pos;
//	private StackProcessor sp;
//	private Vector<Ins_cropImagePlus> cropIms=null; 
	
	private Ins_param param;
	

	public Ins_seg_Processing(ImagePlus imp, Ins_param param_position)
	{
		this.img = imp;	
		this.channel_prefix_pos = param_position.getchannel_prefix_pos();
//		this.cropIms = new Vector<Ins_cropImagePlus>(channel_prefix_pos.length/2,2);
		this.param = param_position;
	}
	
	/**
	 * should modify the x and y for more precision in ImageStack ims = sp.crop((int)(x-4), y, 7, height_align-y);	
	 * @param ip
	 */
	public void cutSSImage()
	{
		int roi_width = param.getRoi_width();
//		ImagePlus imgSubBackground = img.duplicate();
		param.setX_x_deplace(new int[channel_prefix_pos.length-1]);
		int width = img.getWidth();
//		int[] relativeHeadPosition = param.getRelative_headPosition();
//		if(relativeHeadPosition == null)
//			relativeHeadPosition = new int[param.getChannelNumber()];
//		int height_align_min = param.getHeigth_align_min();		
		for(int i=0;i<channel_prefix_pos.length-1;i=i+2)
		{
			float x = (channel_prefix_pos[i]+channel_prefix_pos[i+1])/2;						
//			sp = new StackProcessor(imgSubBackground.duplicate().getImageStack(), null);
			int pos =(int)(x-roi_width/2);
			if(pos < 0)
				pos = 0;
			if(pos + roi_width> width)
				pos = width - roi_width;
//			ImageStack ims = sp.crop(pos, relativeHeadPosition[j],roi_width, height_align_min); //y and height_align-y should be the same with the previous one
			param.getX_x_deplace()[i] = pos;
			
//			ImagePlus tmp = new ImagePlus(img.getTitle()+String.valueOf(i/2+1), ims);				
//			cropIms.add(new Ins_cropImagePlus(tmp,i));			
		}
	}

	static private class MeanSlice implements Comparable{
        private int mIndex;
        private double mMean;
        
        public MeanSlice(int index, double mean){
            mIndex = index;
            mMean = mean;
        }
        public int compareTo(MeanSlice m){
            if(m.getMean()==mMean)
                return 0;
            else if(m.getMean()<mMean)
                return 1;
            else
                return -1;
        }
        public int compareTo(Object o) throws ClassCastException{
            
            if(o instanceof MeanSlice){
                return compareTo((MeanSlice)o);
            }
            else{
                throw new ClassCastException();
            }
        }
        public double getMean(){
            return mMean;
        }
        public int getIndex(){
            return mIndex;
        }
    }
	
	static public ImagePlus adjustZHistogram(ImagePlus img)
    {
        int width = img.getWidth();
        int height = img.getHeight();
        int depth = img.getStackSize();
        ImageStack ims = img.getStack();
        MeanSlice[] meanValueStackSorted = new MeanSlice[depth];
        for (int i=1; i<=depth;i++)
        {
            ImageStatistics stats = new ImagePlus("", ims.getProcessor(i)).getStatistics(Measurements.MEAN);
            meanValueStackSorted[i-1]=new MeanSlice(i,stats.mean);
        }
        Arrays.sort(meanValueStackSorted);
        int meanSlice = 0;
        int middle = (int)(Math.abs(depth/2));
        MeanSlice mSlice = meanValueStackSorted[middle];
        meanSlice = mSlice.getIndex();
        ImageProcessor ip = ims.getProcessor(meanSlice);
        ip=ip.convertToShort(false);
        ImageStatistics stats1 = new ImagePlus("",ip).getStatistics(Measurements.MEAN,Measurements.STD_DEV);
        ImageStack imsCopy=new ImageStack(width,height);
        for (int i=1; i<=img.getStackSize();i++)
        {
            short[] pixelTab2 = new short[width*height];
            short[] pixelTabResult = new short[width*height];
            ImageProcessor ip2 = ims.getProcessor(i).duplicate();
            ip2=ip2.convertToShort(false);
            ImageStatistics stats2 = new ImagePlus("",ip2).getStatistics(Measurements.MEAN,Measurements.STD_DEV);
            pixelTab2=(short[])ip2.getPixels();
            for (int x = 0; x<width; x++)
            {
                for (int y = 0; y<height; y++)
                {
                    short newPixelValue = (short)Math.round(((pixelTab2[x+y*width]- stats2.mean)*stats1.stdDev/stats2.stdDev)+stats1.mean);
                    if(newPixelValue<0)
                        newPixelValue=0;
                    pixelTabResult[x+y*width]=newPixelValue;
                }
            }
            ip2.setPixels(pixelTabResult);
            imsCopy.addSlice("", ip2);
        }
        
        ImagePlus resultImg = new ImagePlus(img.getTitle()+" (adjusted)", imsCopy);
        if(resultImg.getType()!=ImagePlus.GRAY8)
        {
        	StackConverter stkConvert = new StackConverter(resultImg);
        	stkConvert.convertToGray8();
        }
        return (resultImg);
    }
	
	

}





