import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.plugin.filter.Convolver;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;

import java.util.Arrays;


public class Ins_seg_imageBuilderStack {
	public static final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;	
	
	public static ImagePlus convertedStackType(ImagePlus im, int type, boolean doScaling){
		
		Ins_stackTypeConverter sc = new Ins_stackTypeConverter(im.getImageStack(), doScaling);
		switch (type){
			case FLOAT:
				return new ImagePlus(im.getTitle(), sc.convertToFloat(null));
			case BYTE:	
				return new ImagePlus(im.getTitle(), sc.convertToByte());
			case SHORT:
				return new ImagePlus(im.getTitle(), sc.convertToShort());
			case RGB:
				return new ImagePlus(im.getTitle(), sc.convertToRGB());
			default :
				return null;
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
        if(resultImg.getType()!=ImagePlus.GRAY16)
        {
        	StackConverter stkConvert = new StackConverter(resultImg);
        	stkConvert.convertToGray16();
        }
        return (resultImg);
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


	/**
	 * 
	 * @param im
	 * @param H 1d or 2d filter kernel could be reformed by kw and kh
	 * @param kw 
	 * @param kh
	 * @return
	 */
	public static ImagePlus convolveFilter(ImagePlus im, float[] H, int kw, int kh){
		if (kw*kh!=H.length) 
			throw new Error("Kernel length not verified"); 
		ImagePlus img=null;
		if(im!=null){
			if(im.getStackSize()==1){
				img = new ImagePlus(im.getTitle(), im.getProcessor().duplicate());
				return img;
			}
			else{ //Stack				
				img = new ImagePlus(im.getTitle(),im.duplicate().getImageStack());
				for(int i=1;i<=img.getImageStackSize();i++)
				{
					ImageProcessor ip=img.getImageStack().getProcessor(i);
					Convolver cv=new Convolver();
					cv.convolve(ip, H, kw, kh);				
					img.getImageStack().getProcessor(i).insert(ip, 0, 0);
				}
				return img;
			}
		}else {
			return null;
		}
	}	


	
	
	

	

	
	/**
	 * 需要加入一个确认对话框 目的是为了确定中线的位置是不是在中间
	 * @param ip
	 * @param direction
	 * @param interval
	 * @return index of sorted maximum histogram by interval 
	 */
	
	
	public static int[] positionMaxhisto(ImageProcessor ip, int direction, int interval, float thres_h,float thres_v){
		int[] position=new int[2];
		int w=ip.getWidth();
		int h=ip.getHeight();	
		int hist[];
		switch (direction) {
		case 1:	
			hist = new int[ip.getWidth()];		
			for(int u=0;u<w;u++)
				for(int v=0;v<h; v++)
				{
					hist[u] = hist[u]+ip.get(u, v); 
				}
			float[] hist_interval=getArray_interval(hist,interval);			
			
			//---------------------------------------------
			//for getting the indices array and float array above the threshold
			int[] indices=new int[hist_interval.length]; 
			float[] values=new float[hist_interval.length];
			int indices_size=0;
			for(int i=0;i<hist_interval.length;i++)
			{
				if(hist_interval[i]>=thres_v)
				{
					indices[i]=i;
					values[i]=hist_interval[i];
					indices_size++;
				}else {
					indices[i]=-1;
				}
			}			
			int[] indices_=new int[indices_size];
			float[] values_=new float[indices_size];
			int k=0;
			for(int i=0;i<hist_interval.length;i++)
			{
				if(indices[i]!=-1)
				{
					indices_[k]=i;
					values_[k]=values[i];
					k++;
				}
			}//-----------------------------------------------		
			if(k==0)
				throw new Error("Threshold set too high (Try to decrease this level)");
			
			position[0]=indices_[0];
			position[1]=indices_[indices_.length-1];					
			break;
		case 2:			
			hist = new int[ip.getHeight()];			
			for(int v=0;v<h; v++)
				for(int u=0;u<w;u++)
				{
					hist[v] = hist[v]+ip.get(u, v); 
				}
			float[] hist_interval_c2=getArray_interval(hist,interval);			
			
			//---------------------------------------------
			//for getting the indices array and float array above the threshold
			int[] indices_c2=new int[hist_interval_c2.length]; 
			float[] values_c2=new float[hist_interval_c2.length];
			int indices_size_c2=0;
			for(int i=0;i<hist_interval_c2.length;i++)
			{
				if(hist_interval_c2[i]>=thres_h)
				{
					indices_c2[i]=i;
					values_c2[i]=hist_interval_c2[i];
					indices_size_c2++;
				}else {
					indices_c2[i]=-1;
				}
			}			
			int[] indices_c2_=new int[indices_size_c2];
			float[] values_c2_=new float[indices_size_c2];
			int k_c2=0;
			for(int i=0;i<hist_interval_c2.length;i++)
			{
				if(indices_c2[i]!=-1)
				{
					indices_c2_[k_c2]=i;
					values_c2_[k_c2]=values_c2[i];
					k_c2++;
				}
			}//-----------------------------------------------			
			position[0]=indices_c2_[0];
			position[1]=indices_c2_[indices_c2_.length-1];			
			break;


		default:
			break;
		}
		return position;
	}
	
	
	
	public static int[] positionMaxhisto_horizontal(ImageProcessor ip, int interval, float threshold, int pos0, int pos1){
		int[] position=new int[2];
		int w=ip.getWidth();
		int hist[];

		hist = new int[ip.getWidth()];		
		for(int u=0;u<w;u++)
			for(int v=pos0;v<=pos1; v++)
			{
				hist[u] = hist[u]+ip.get(u, v); 
			}
		float[] hist_interval=getArray_interval(hist,interval);			

		//---------------------------------------------
		//for getting the indices array and float array above the threshold
		int[] indices=new int[hist_interval.length]; 
		float[] values=new float[hist_interval.length];
		int indices_size=0;
		for(int i=0;i<hist_interval.length;i++)
		{
			if(hist_interval[i]>=threshold)
			{
				indices[i]=i;
				values[i]=hist_interval[i];
				indices_size++;
			}else {
				indices[i]=-1;
			}
		}			
		int[] indices_=new int[indices_size];
		float[] values_=new float[indices_size];
		int k=0;
		for(int i=0;i<hist_interval.length;i++)
		{
			if(indices[i]!=-1)
			{
				indices_[k]=i;
				values_[k]=values[i];
				k++;
			}
		}//-----------------------------------------------		
		if(k==0)
			throw new Error("Threshold set too high (Try to decrease this level)");

		position[0]=indices_[0];
		position[1]=indices_[indices_.length-1];					
		
		return position;
	}

	
	/**
	 * 
	 * @param ar histogram computed already
	 * @param interval interval pixels default should be impair.
	 * @return
	 */
	public static int getMaxItemIndex_interval(int [] ar, int interval){
		int r = 0;
		int local_max=0;
		for(int x=interval;x<ar.length-interval;x++){			
			int comp_max = 0;
			for(int j=0-interval; j<=interval; j++)
			{
				comp_max=comp_max+ar[x+j];
			}
			if (comp_max>local_max){
				local_max=comp_max;
				r = x;
			}			
		}
		return r-interval;		
	}
	
	/**
	 * 
	 * @param ar histogram computed already
	 * @param interval interval pixels default should be impair.
	 * @return
	 */
	public static int[] getArray_interval_ind(int [] ar, int interval){
		float[] main=new float[ar.length];		
		int[] index=new int[ar.length];
		for(int x=interval;x<ar.length-interval;x++){			
			int comp_max = 0;
			for(int j=0-interval; j<=interval; j++)
			{
				comp_max=comp_max+ar[x+j];
			}
			main[x] = comp_max;
			index[x] = x-interval;
		}
		quicksort(main, index);		
		return index;		
	}
	
	/**
	 * 
	 * @param ar histogram computed already
	 * @param interval interval pixels default should be impair.
	 * @return
	 */
	public static float[] getArray_interval(int [] ar, int interval){
		float[] main=new float[ar.length];				
		for(int x=interval;x<ar.length-interval;x++){			
			int comp_max = 0;
			for(int j=0-interval; j<=interval; j++)
			{
				comp_max=comp_max+ar[x+j];
			}
			main[x] = comp_max;			
		}
		return main;		
	}
	
	//--------------------------------------------------------------------------
	//quick sort
	public static void quicksort(float[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}

	// quicksort a[left] to a[right]
	public static void quicksort(float[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(float[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

	// is x < y ?
	private static boolean less(float x, float y) {
	    return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(float[] a, int[] index, int i, int j) {
	    float swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}	
	//---------------------------------------------------------------
	
	public static int getMaxItemIndex(int [] ar){
		int r = 0;
		for(int x=1;x<ar.length;x++){
			r = ar[r]>=ar[x]?r:x;
		}
		return r;
	} 
	
	public static int[] getThresholdItemIndex(int [] ar, double d){
		int r = 0;		
		for(int x=1;x<ar.length;x++){
			r = ar[r]>=ar[x]?r:x;			
		}
		int i[]=new int[2];		
		for(int x=0;x<ar.length;x++){
			if (ar[x]>(d*ar[r]))
			{
				i[0]=x;
				break;
			}
		}		
		for(int x=ar.length-1;x>=0;x--){
			if (ar[x]>(d*ar[r]))
			{
				i[1]=x;
				break;
			}
		}		
		return i;
	} 
	
	/**
	 * Display an ImagePlus by duplication
	 * @param im ImagePlus of the image to display
	 */
	public static void display(ImageProcessor ip, String name){
		if(ip!=null){
			new ImagePlus(name, ip).show();
		}
	}
	
	/**
	 * Display an ImagePlus by duplication
	 * @param im ImagePlus of the image to display
	 */
	public static void display(ImagePlus im){
		if(im!=null){
			if(im.getStackSize()==1){
				ImagePlus img = new ImagePlus(im.getTitle(), im.getProcessor().duplicate());
				img.show();
			}
			else{ //Stack
				ImagePlus img = new ImagePlus(im.getTitle(), im.duplicate().getImageStack());
				img.show();
			}
		}
	}
	
	/**
	 * Display an ImagePlus by duplication
	 * @param im ImagePlus of the image to display
	 * @param name title of the displayed window
	 */
	public static void display(ImagePlus im, String name){
		if(im!=null){
			if(im.getStackSize()==1){
				ImagePlus img = new ImagePlus(name, im.getProcessor().duplicate());
				img.show();
			}
			else{ //Stack
				ImagePlus img = new ImagePlus(name, im.duplicate().getImageStack());
				img.show();
			}
		}
	}
}
