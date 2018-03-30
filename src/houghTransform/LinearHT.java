package houghTransform;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


public class LinearHT {
	ImageProcessor ip;
	int xCtr, yCtr;
	int nAng;
	int nRad;
	int cRad;
	double dAng;
	double dRad;
	int[][] houghArray;
	
	public LinearHT(ImageProcessor ip, int nAng, int nRad) {
		this.ip = ip;
		this.xCtr = ip.getWidth()/2;
		this.yCtr = ip.getHeight()/2;
		this.nAng = nAng;
		this.dAng = Math.PI/nAng;
		
		this.nRad = nRad;
		this.cRad = nRad/2;
		double rMax = Math.sqrt(xCtr*xCtr + yCtr*yCtr);
		this.dRad = (2.0*rMax) / nRad;
		this.houghArray = new int[nAng][nRad];
		fillHoughAccumulator();
	}
	
	public LinearHT(ImageProcessor ip) {
		this.ip = ip;
		this.xCtr = ip.getWidth()/2;
		this.yCtr = ip.getHeight()/2;
		int nAng = 1800;
		double rMax = Math.sqrt(xCtr*xCtr + yCtr*yCtr);
		int nRad = (int)(2.0*rMax);
		this.nAng = nAng;
		this.dAng = Math.PI/nAng;
		this.nRad = nRad;
		this.cRad = nRad/2;
		this.dRad = (2.0*rMax) / nRad;
		this.houghArray = new int[nAng][nRad];
		fillHoughAccumulator();
	}
	
	void fillHoughAccumulator(){
		int h = ip.getHeight();
		int w = ip.getWidth();
		for(int v=0; v<h; v++)
			for(int u=0; u<w; u++)
			{
				if(ip.get(u,v)>0){
					doPixel(u,v);
				}
			}
	}
	
	void doPixel(int u, int v){
		int x = u - xCtr, y = v- yCtr;
		for(int i=0; i<nAng; i++)
		{
			double theta = dAng * i;
			int r = cRad + (int)Math.rint((x*Math.cos(theta) + y*Math.sin(theta))/dRad);
			if(r>=0 && r<nRad){
				houghArray[i][r]++;
			}
		}
	}
	
	public ImagePlus transformArrayToImp()
	{
		//int width = houghArray.length;
		int height = houghArray[0].length;
		ShortProcessor fp = new ShortProcessor(houghArray.length, houghArray[0].length);
		for(int u=0;u<fp.getWidth();u++)
			for(int v=0;v<fp.getHeight();v++)
			{
				fp.set(fp.getWidth()-u - 1, fp.getHeight()-v -1, houghArray[u][height - v-1]);
			}
		ImagePlus imp = new ImagePlus("HT", fp);
		return imp;
	}
	
	public static void main(String[] args)
	{
		ImageJ ij = new ImageJ();
		ij.setVisible(true);
		ImagePlus imp = IJ.openImage();
		LinearHT linearHT = new LinearHT(imp.getProcessor());
		ImagePlus impLH = linearHT.transformArrayToImp();
		impLH.show();
	}
}
