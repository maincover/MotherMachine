package Stabilizer;



import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;

import java.io.Serializable;

public class Ins_param implements Serializable{
	private static final long serialVersionUID = 1L;
		private double angle = 0;
		private int[] position_h;
		private int height_align;
		private int height_align_min;
		
		private int width_align;
		private int[] position_l;
		private boolean stabilized = true;
		private double[][][] wp;
		private boolean ready;
		private int roi_width;
		private int[][] y;
		private float[] channel_prefix_pos;
		private int[] relative_headPosition;
		private int[] x_deplace;
		private int blank_width;
		private int intervalSlices = 1;
		private int start_x;
		private int start_y;
		private float interChannelLength;
		private int channelNumber;
		private boolean savedOnce;
		private boolean sealedOffTop = true;
		private String pName;


		public void compute_channel_prefix_pos()
		{
			channel_prefix_pos=new float[2*channelNumber];		
			channel_prefix_pos[0]=0;		
			for(int i=2; i < channel_prefix_pos.length;i=i+2)
			{
				channel_prefix_pos[i] = channel_prefix_pos[i-2]+interChannelLength;
			}
			for(int i=1; i < channel_prefix_pos.length;i=i+2)
			{
				channel_prefix_pos[i] = channel_prefix_pos[i-1]+roi_width; 
			}
			int width_align=(int)(channel_prefix_pos[channel_prefix_pos.length-1]-channel_prefix_pos[0]);
			setWidth_algin(width_align);
		}
		
		public int getStartX()
		{
			return start_x;
		}
		
		public int getStartY()
		{
			return start_y;
		}
		
		public void setStartX(double x)
		{
			start_x = (int)x;
		}
		
		public void setStartY(double y)
		{
			start_y = (int)y;
		}

		public int getIntervalSlices()
		{
			return intervalSlices;
		}
		
		public void setIntervalSlices(int pIntervalSlices)
		{
			if(pIntervalSlices<1)
				intervalSlices = 1;
			intervalSlices = pIntervalSlices;
		}
		
		public int getRoi_width()
		{
			return roi_width;
		}
		
		public void setRoi_width(double pRoi_width)
		{
			roi_width = (int)pRoi_width;
		}
		
		public boolean ready()
		{
			return ready;
		}
		
		public void setReady(boolean ifReady)
		{
			ready = ifReady;
		}
		
		public double getAngle()
		{
			return angle;
		}
		
		public void setAngle(double pAngle)
		{
			angle = pAngle;
		}
		
		public int[] getPosition_l()
		{
			return position_l;
		}
		
		public void setPosition_l(int[] pPosition_l)
		{
			position_l = pPosition_l;
		}
		
		public int[] getPosition_h()
		{
			return position_h;
		}
		
		public void setPosition_h(int[] pPosition_h)
		{
			position_h = pPosition_h;
		}
		
		public int getWidth_align()
		{
			return width_align;
		}
		
		public void setWidth_algin(int pWidth_align)
		{
			width_align = pWidth_align;
		}
		
		public int getHeight_align()
		{
			return height_align;
		}
		
		public void setHeight_align(double pHeight_align)
		{
			height_align = (int)pHeight_align;
		}
		
		public void setHeight_align_min(int pHeight_align_min)
		{
			height_align_min = pHeight_align_min;
		}
		
		public int getHeigth_align_min()
		{
			return height_align_min;
		}
		
		public int[][] getY()
		{
			return y;
		}
		
		public void setY(int[][] y)
		{
			this.y = y;
		}
		
		public float[] getchannel_prefix_pos()
		{
			return channel_prefix_pos;
		}
		
		public void setChannel_prefix_pos(float[] pChannel_prefix_pos)
		{
			channel_prefix_pos = pChannel_prefix_pos;
		}
		
		public void setX_x_deplace(int[] pX_x_deplace)
		{
			x_deplace = pX_x_deplace;
		}
		
		public int[] getX_x_deplace()
		{
			return x_deplace;
		}
		
		public int getBlank_width()
		{
			return blank_width;
		}
		
		public void setBlank_width(int pInter)
		{
			blank_width = pInter;
		}
		
		
		public void setStabilization(boolean stabilized)
		{
			this.stabilized = stabilized;
		}
		
		public boolean getStabilized()
		{
			return stabilized;
		}
		
		public void setWp(double[][][] pWp)
		{
			wp = pWp;
		}
		
		public double[][][] getWp()
		{
			return wp;
		}
		
		public ImagePlus toSSImage(ImagePlus img) 
		{
			if(channel_prefix_pos == null)
				return null;
			int depth = img.getStackSize();
			int height_ = img.getHeight();
			int width = depth*(roi_width + blank_width);		
			ImageStack ims = new ImageStack(width, height_align_min, channel_prefix_pos.length/2);
			int slice = 1;			
			int channel = 0;
			
			for(int i=0,j=0;i<channel_prefix_pos.length-1;i=i+2,j++)
			{
				StackProcessor sp = new StackProcessor(img.getImageStack().duplicate(), null);
				ImageProcessor ip = img.getProcessor().createProcessor(width,height_);
				ImageStack originalStack = sp.crop(x_deplace[i], relative_headPosition[j] , roi_width, height_align_min);
				int x = 0;
				for(int k=1; k <= depth; k++)
				{
					ImageProcessor ip_RFP = originalStack.getProcessor(k);
					ip.insert(ip_RFP, x, 0);
					x=x+roi_width+blank_width;						
				}
				ims.setPixels(ip.getPixels(), slice);
				slice = slice + 1;
				channel = channel + 1;
			}
			return new ImagePlus(img.getTitle()+"-"+getPositionName()+"-ss-"+String.valueOf(img.getImageStackSize())+"-roi-"+getRoi_width(), ims);
		}
		


		public float getInterChannelLength()
		{			
			return interChannelLength;
		}
		public void setInterChannelLength(double valueOf) {
			this.interChannelLength = (float)valueOf;
		}

		public void setChannelNumber(double valueOf) {
			this.channelNumber = (int)valueOf;
		}


		public int getChannelNumber() {
			return channelNumber;
		}

		public void setSavedOnce() {
			savedOnce = true;			
		}
		
		public boolean savedOnce(){
			return savedOnce;
		}

		public boolean getSealedOffAtTop() {
			return sealedOffTop ;
		}
		
		public void sealedOffAtTop(boolean flip)
		{
			sealedOffTop = flip;
		}

		public String getPositionName() {
			return pName;
		}
		
		public void setPositionName(String pName){
			this.pName = pName;
		}

	
		public String toString()
		{			
			return "position : " + pName + " rotation : " + angle + " start_x : " + start_x + " start_y : " + start_y + " channel number : " + channelNumber + " Inter channel length : " + interChannelLength;			
		}

		public void setRelative_headPosition(int[] relative_headPosition) {
			this.relative_headPosition = relative_headPosition;			
		}
		
//		public int[] getRelative_headPosition1()
//		{
//			return relative_headPosition;
//		}

	
}
