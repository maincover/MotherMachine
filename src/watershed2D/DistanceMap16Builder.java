package watershed2D;

import ij.IJ;
import ij.plugin.filter.EDM;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * This class extends the Euclidean Distance Map (EDM)
 * it allow to build an distance map of a binary image
 */
public class DistanceMap16Builder extends EDM{
	

	/**
	 * build a distance map (ShortProcessor) with a binary image and the pixels of object's borders
	 * @param ip ImageProcessor of a binary image
	 * @param borders array of pixels representing the borders of objects
	 * @return ShortProcessor representing the distance map
	 */
	public ShortProcessor make16bitEDM(ImageProcessor ip, int[] borders) {
        int xmax, ymax;
        int offset, rowsize;
        
        IJ.showStatus("Generating EDM");
        int width = ip.getWidth();
        int height = ip.getHeight();
        rowsize = width;
        xmax    = width - 2;
        ymax    = height - 2;
        ShortProcessor ip16 = (ShortProcessor)ip.convertToShort(false);
        ip16.multiply(128); //foreground pixels set (almost) as high as possible for signed short
        short[] image16 = (short[])ip16.getPixels();
        
        if(borders!=null){
        	for(int i=0; i<borders.length; i++){
        		image16[borders[i]] = EDM.ONE;
        	}
        }
        	
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                offset = x + y * rowsize;
                if (image16[offset] > 0) {
                    if ((x<=1) || (x>=xmax) || (y<=1) || (y>=ymax))
                        setEdgeVal(offset, rowsize, image16, x, y, xmax, ymax);
                    else
                        setVal(offset, rowsize, image16);
                }
            } // for x
        } // for y
        
        for (int y=height-1; y>=0; y--) {
            for (int x=width-1; x>=0; x--) {
                offset = x + y * rowsize;
                if (image16[offset] > 0) {
                    if ((x<=1) || (x>=xmax) || (y<=1) || (y>=ymax))
                        setEdgeVal(offset, rowsize, image16, x, y, xmax, ymax);
                    else
                        setVal(offset, rowsize, image16);
                }
            } // for x
        } // for y
        //(new ImagePlus("EDM16", ip16.duplicate())).show();
        
        return ip16;
    } // make16bitEDM(ip)
	
	 void setVal(int offset, int rowsize, short[] image16) {
	        int  v;
	        int r1  = offset - rowsize - rowsize - 2;
	        int r2  = r1 + rowsize;
	        int r3  = r2 + rowsize;
	        int r4  = r3 + rowsize;
	        int r5  = r4 + rowsize;
	        int min = 32767;
	        
	        v = image16[r2 + 2] + ONE;
	        if (v < min)
	            min = v;
	        v = image16[r3 + 1] + ONE;
	        if (v < min)
	            min = v;
	        v = image16[r3 + 3] + ONE;
	        if (v < min)
	            min = v;
	        v = image16[r4 + 2] + ONE;
	        if (v < min)
	            min = v;
	        
	        v = image16[r2 + 1] + SQRT2;
	        if (v < min)
	            min = v;
	        v = image16[r2 + 3] + SQRT2;
	        if (v < min)
	            min = v;
	        v = image16[r4 + 1] + SQRT2;
	        if (v < min)
	            min = v;
	        v = image16[r4 + 3] + SQRT2;
	        if (v < min)
	            min = v;
	        
	        v = image16[r1 + 1] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r1 + 3] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r2 + 4] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r4 + 4] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r5 + 3] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r5 + 1] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r4] + SQRT5;
	        if (v < min)
	            min = v;
	        v = image16[r2] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        image16[offset] = (short)min;
	        
	    } // setValue()
	    
	    void setEdgeVal(int offset, int rowsize, short[] image16, int x, int y, int xmax, int ymax) {
	        int  v;
	        int r1 = offset - rowsize - rowsize - 2;
	        int r2 = r1 + rowsize;
	        int r3 = r2 + rowsize;
	        int r4 = r3 + rowsize;
	        int r5 = r4 + rowsize;
	        int min = 32767;
	        int offimage = image16[r3 + 2];
	        
	        if (y<1)
	            v = offimage + ONE;
	        else
	            v = image16[r2 + 2] + ONE;
	        if (v < min)
	            min = v;
	        
	        if (x<1)
	            v = offimage + ONE;
	        else
	            v = image16[r3 + 1] + ONE;
	        if (v < min)
	            min = v;
	        
	        if (x>xmax)
	            v = offimage + ONE;
	        else
	            v = image16[r3 + 3] + ONE;
	        if (v < min)
	            min = v;
	        
	        if (y>ymax)
	            v = offimage + ONE;
	        else
	            v = image16[r4 + 2] + ONE;
	        if (v < min)
	            min = v;
	        
	        if ((x<1) || (y<1))
	            v = offimage + SQRT2;
	        else
	            v = image16[r2 + 1] + SQRT2;
	        if (v < min)
	            min = v;
	        
	        if ((x>xmax) || (y<1))
	            v = offimage + SQRT2;
	        else
	            v = image16[r2 + 3] + SQRT2;
	        if (v < min)
	            min = v;
	        
	        if ((x<1) || (y>ymax))
	            v = offimage + SQRT2;
	        else
	            v = image16[r4 + 1] + SQRT2;
	        if (v < min)
	            min = v;
	        
	        if ((x>xmax) || (y>ymax))
	            v = offimage + SQRT2;
	        else
	            v = image16[r4 + 3] + SQRT2;
	        if (v < min)
	            min = v;
	        
	        if ((x<1) || (y<=1))
	            v = offimage + SQRT5;
	        else
	            v = image16[r1 + 1] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x>xmax) || (y<=1))
	            v = offimage + SQRT5;
	        else
	            v = image16[r1 + 3] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x>=xmax) || (y<1))
	            v = offimage + SQRT5;
	        else
	            v = image16[r2 + 4] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x>=xmax) || (y>ymax))
	            v = offimage + SQRT5;
	        else
	            v = image16[r4 + 4] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x>xmax) || (y>=ymax))
	            v = offimage + SQRT5;
	        else
	            v = image16[r5 + 3] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x<1) || (y>=ymax))
	            v = offimage + SQRT5;
	        else
	            v = image16[r5 + 1] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x<=1) || (y>ymax))
	            v = offimage + SQRT5;
	        else
	            v = image16[r4] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        if ((x<=1) || (y<1))
	            v = offimage + SQRT5;
	        else
	            v = image16[r2] + SQRT5;
	        if (v < min)
	            min = v;
	        
	        image16[offset] = (short)min;
	        
	    } // setEdgeValue()
	
}