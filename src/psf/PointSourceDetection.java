package psf;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.special.Erf;


public class PointSourceDetection implements PlugInFilter{
	ImagePlus img;
	ImagePlus imgXT;
	double[] imgV;
	double[] imgLMV;
	double[] imgXTV;
	double[] imgLoGV; // defined in convolution
	double[] rssV;
	double[] allMaxV;
	int[] maskV;
	double[] fgV;
	double[] fuV;
	double[] fu2V;
	double[] a_estV;
	double[] c_estV;
	int width;
	int height;
	int widthV;
	int heightV;
	double sigma;
	double alpha = 0.05;
	boolean mask;
	boolean fitMixtures;
	boolean maxMixtures;
	boolean removeRedundant;
	double redundancyRadius = 0.25;
	boolean prefilter = true;
	boolean refineMaskLoG = true;
	boolean refineMaskValid = true;
	boolean keepUnMatched = true;
	String mode = "xyAc";
	int w = 0;
	double[] x;
	double[] g;
	double[][] g2d;
	int numel;
	double gsum;
	double g2sum;
	double[] u;
	
	public PointSourceDetection(ImagePlus img,double sigma,boolean refineMaskLoG,boolean refineMaskValid,boolean whiteObject)
	{
		this.img = img;
		if(!whiteObject)
			this.img.getProcessor().invert();
		this.sigma = sigma;
		this.refineMaskLoG = refineMaskLoG;
		this.refineMaskValid = refineMaskValid;
	}
	
	public void initializeGaussianKernel()
	{
		w = (int)Math.ceil(sigma*4);
		x = new double[2*w+1];
		g = new double[2*w+1];
		u = new double[2*w+1];
		for(int i=-w,j=0;i<=w;i++,j++)
		{
			x[j] = i; 
			g[j] = Math.exp(-i*i/(2*sigma*sigma));
			u[j] = 1;
			//System.out.println("g " + j + " : " + g[j]);
		}
		
	}
	
	/*
	 * convolution adn laplacian of Gaussian
	 */
	public void convolutions(boolean debug)
	{
		System.out.println("SPF process");
		initializeGaussianKernel();
		ImageProcessor ipXT = padarrayXT(img.getProcessor(), w);
		boolean notFloat = !(ipXT instanceof FloatProcessor);
		if (notFloat) {
			if (ipXT instanceof ColorProcessor) 
				throw new IllegalArgumentException("RGB images not supported");
			TypeConverter tc = new TypeConverter(ipXT, false);
			ipXT= tc.convertToFloat(null);
		}
		float[] pixelsF = (float[])ipXT.getPixels();
		double[] pixelsD = toDoubleArray(pixelsF);
		width = ipXT.getWidth();
		height = ipXT.getHeight();
		double[] fg = conv2(g,g,pixelsD,width,height);
		double[] fu = conv2(u,u, pixelsD, width, height);
		double[] pixelsSquare=toMultiplyArray(pixelsD, pixelsD);
		double[] fu2 = conv2(u,u, pixelsSquare, width, height);
		
		fgV = toValidArr(fg,g.length,width,height);
		fuV = toValidArr(fu,g.length,width,height);
		fu2V = toValidArr(fu2,g.length,width,height);
		//Laplacian of Gaussian
		double[] xsquare = toMultiplyArray(x, x);
		double[] gx2 = toMultiplyArray(g, xsquare);
		double sigma2 = Math.pow(sigma, 2);
		double sigma4 = Math.pow(sigma, 4);
		double[] pixelsL1 = conv2(g, gx2, pixelsD, width, height);
		double[] pixelsL1V = toValidArr(pixelsL1, g.length, width, height);
		double[] pixelsL2 = conv2(gx2, g, pixelsD, width, height);
		double[] pixelsL2V = toValidArr(pixelsL2, g.length, width, height);
		imgLoGV = toDivideValue(toSubstracArray(toDivideValue(toMultiplyValue(fgV, 2),sigma2),toDivideValue(toAddArray(pixelsL1V, pixelsL2V), sigma4)), 2*Math.PI*sigma2);
		widthV = width - g.length +1;
		heightV = height - g.length + 1;
		//all local max
		allMaxV = locmax2d(imgLoGV, widthV, heightV, (int)(2*Math.ceil(sigma)+1), false);
		if(debug)
			toPrintToImp(imgLoGV, widthV, heightV, "imgLoGV");
	}
	
	private double[] locmax2d(double[] img, int width, int height, int w, boolean keepFlat)
	{
		if(w%2 == 0)
			w = w + 1;
		int numE1 = w*w;
		int[] mask = new int[numE1];
		for(int i=0;i<mask.length;i++)
			mask[i] = 1;
		double[] fImg = ordfilt2(img, height, width, numE1, mask,w);
		if(!keepFlat)
		{
			double[] fImg2 = ordfilt2(img, height, width, numE1-1, mask, w);
			fImg = toArrayXor(fImg, fImg2);
		}
		// take only those positions where the max filter and the original image value
		// are equal -> this is a local maximum
		fImg = toArrayXNor(fImg, img);
		// set image border to zero
		toArrayBorderZero(fImg, height, width, w);
		return fImg;
	}
	
	private void toArrayBorderZero(double[] fImg, int height, int width,
			int w) 
	{
		assert w%2 == 1;
		int b = (w-1)/2;
		for(int i=0;i<fImg.length;i++)
		{
			int c = i%width;
			int r = i/width;
			if(c<b || c>=width-b || r<b || r>=height-b)
				fImg[i] = 0;
		}
	}

	private double[] toArrayXNor(double[] fImg, double[] fImg2) {
		assert fImg.length == fImg2.length;
		double[] arr1 = new double[fImg.length];
		System.arraycopy(fImg, 0, arr1, 0, arr1.length);
		for(int i=0; i<fImg.length;i++)
			if(fImg[i]!=fImg2[i])
				arr1[i] = 0;
		return arr1;
	}
	
	private double[] toArrayXor(double[] fImg, double[] fImg2) {
		assert fImg.length == fImg2.length;
		double[] arr1 = new double[fImg.length];
		System.arraycopy(fImg, 0, arr1, 0, arr1.length);
		for(int i=0; i<fImg.length;i++)
			if(fImg[i]==fImg2[i])
				arr1[i] = 0;
		return arr1;
	}

	private double[] ordfilt2(double[] img, int height, int width,
			int numE1, int[] domain, int w) {
		assert img.length == height * width;
		assert domain.length == w*w;
		int[][] dirOffset = toDirOffset(domain, w);
		int[] dirOffsetR = dirOffset[0];
		int[] dirOffsetC = dirOffset[1];
		int nNeighbor = dirOffset[0].length; 
		double[] ret = new double[img.length];
		System.arraycopy(img, 0, ret, 0, ret.length);
		
		for(int i=0;i<img.length;i++)
		{
			int c = i%width;
			int r = i/width;
			double[] arr = new double[nNeighbor];
			for(int v=0; v<nNeighbor;v++)
			{
				int rn = r + dirOffsetR[v];
				int cn = c + dirOffsetC[v];
				if(isValid(rn,cn, height, width))
					arr[v] = img[rn*width+cn];
			}
			Arrays.sort(arr);
			ret[i] = arr[numE1-1];	
		}
		return ret;
	}

	private boolean isValid(int rn, int cn, int height, int width) {
		return rn<height && cn<width && rn>=0 && cn>=0;
	}

	private int[][] toDirOffset(int[] domain, int w){
		assert domain.length == w*w;
		int cR = (int)Math.floor(w*0.5);
		int cC = (int)Math.floor(w*0.5);
		int[][] ret = new int[2][domain.length];
		for(int i=0;i<domain.length;i++)
		{
			ret[0][i] = i%w - cR;
			ret[1][i] = i/w - cC;
		}
		return ret;
	}

	public void get2dKenel()
	{
		int mLength = g.length;
		gsum = 0.0d;
		g2sum = 0.0d;
		g2d = new double[mLength][mLength];
		for(int i = 0; i < mLength; i++) {         // rows from m1
			for(int j = 0; j < mLength; j++) {     // columns from m2
				g2d[i][j] = g[i] * g[j];
				gsum += g2d[i][j];
				g2sum += g2d[i][j]*g2d[i][j];
			}
        }
		numel = g.length * g.length;
	}
	
	public void solutionToLS()
	{
		a_estV = toDivideValue(toSubstracArray(fgV, toDivideValue(toMultiplyValue(fuV, gsum), numel)), g2sum-gsum*gsum/numel);
		c_estV = toDivideValue(toSubstracArray(fuV, toMultiplyValue(a_estV, gsum)), numel);
	}
	
	/*
	 * m(:) same to matlab, first is the column then row
	 */
	public double[] toArrMatrix(double[][] m)
	{
		assert m!=null;
		double[] mResult = new double[m.length*m[0].length];
		int k=0;
		for(int i=0;i<m[0].length;i++)
			for(int j=0;j<m.length;j++)
				mResult[k++] = m[j][i];
		return mResult;
	}
	
	/*
	 * J = [g(:) ones(n,1)]; % g_dA g_dc
	 */
	public double[][] toTableGone(double[][] g, double v)
	{
		double[] gArr = toArrMatrix(g);
		double[][] mTable = new double[gArr.length][2];
		for(int i=0;i<gArr.length;i++)
		{
			mTable[i][0] = gArr[i];
			mTable[i][1] = v;
		}
		return mTable;
	}
	
	public static double[][] transposeMatrix(double [][] m){
		double[][] temp = new double[m[0].length][m.length];
		for (int i = 0; i < m.length; i++)
			for (int j = 0; j < m[0].length; j++)
				temp[j][i] = m[i][j];
		return temp;
	}
	
	public static double[][] multiplyByMatrix(double[][] m1, double[][] m2) {
		int m1ColLength = m1[0].length; // m1 columns length
		int m2RowLength = m2.length;    // m2 rows length
		assert m1ColLength == m2RowLength;		
		int mRRowLength = m1.length;    // m result rows length
		int mRColLength = m2[0].length; // m result columns length
		double[][] mResult = new double[mRRowLength][mRColLength];
		for(int i = 0; i < mRRowLength; i++) {         // rows from m1
			for(int j = 0; j < mRColLength; j++) {     // columns from m2
				for(int k = 0; k < m1ColLength; k++) { // columns from m1
					mResult[i][j] += m1[i][k] * m2[k][j];
				}
			}
		}
		return mResult;
	}
	
	public void toPrintMatrix(double[][] d)
	{
		for(int i=0;i<d.length;i++)
		{
			String toPrintRow = "";
			for(int j=0;j<d[0].length;j++)
			{
				toPrintRow += "("+i+","+j+")"+ d[i][j]+"\t"; 
			}
			System.out.println(toPrintRow);
		}
	}
	
	public void toPrintToImp(double[] d, int width, int height,String title)
	{
		ImageProcessor ip = new FloatProcessor(toMatrixF(d, width, height));
		if(title == null)
			title = "new";
		new ImagePlus(title, ip).show();
	}
	
	public void toPrintToImp(int[] d, int width, int height,String title)
	{
		ImageProcessor ip = new FloatProcessor(toMatrixF(d, width, height));
		if(title == null)
			title = "new";
		new ImagePlus(title, ip).show();
	}
	
	public void prefilter(boolean preFilter, boolean debug)
	{
		if(preFilter)
		{
			double[][] j = toTableGone(g2d, 1.0d);
			double[][] jT = transposeMatrix(j);
			double[][] jTj = multiplyByMatrix(jT, j);
			RealMatrix jTjm = MatrixUtils.createRealMatrix(jTj);
			RealMatrix jTjIm = new LUDecomposition(jTjm).getSolver().getInverse();
			if(debug)
			{
				double[][] d= jTjIm.getData();
				toPrintMatrix(d);
			}
			//f_c = fu2 - 2*c_est.*fu + n*c_est.^2; % f-c
			//RSS = A_est.^2*g2sum - 2*A_est.*(fg - c_est*gsum) + f_c;
			double[] f_cV = toAddArray(toSubstracArray(fu2V, toMultiplyValue(toMultiplyArray(c_estV, fuV), 2)), toMultiplyValue(toMultiplyArray(c_estV, c_estV), numel));
			rssV = toAddArray(toSubstracArray(toMultiplyValue(toMultiplyArray(a_estV, a_estV), g2sum), toMultiplyValue(toMultiplyArray(a_estV, toSubstracArray(fgV, toMultiplyValue(c_estV, gsum))), 2)), f_cV);
			rssV = toArrayPositive(rssV);
			if(debug)
				toPrintToImp(rssV, widthV, heightV, "rssV");
			double[] sigma_e2V = toDivideValue(rssV, numel-3);
			double[] sigma_AV = toSquareRootArray(toMultiplyValue(sigma_e2V, jTjIm.getData()[0][0]));
			double[] sigma_resV = toSquareRootArray(toDivideValue(rssV, numel-1));
			
			NormalDistribution nDistribution = new NormalDistribution(0, 1.0d);
			double kLevel = nDistribution.inverseCumulativeProbability(1-alpha*0.5);
			if(debug)
				System.out.println("kLevel : " + kLevel + " erf : " +Erf.erfInv(2 * 0.975-1));
			double[] se_sigma_cV = toMultiplyValue(toDivideValue(sigma_resV, Math.sqrt(2*(numel-1))), kLevel);
			double[] df2V = toMultiplyValue(toDivideArray(toPowerArray(toAddArray(toPowerArray(sigma_AV, 2), toPowerArray(se_sigma_cV, 2)),2), toAddArray(toPowerArray(sigma_AV, 4), toPowerArray(se_sigma_cV, 4))), numel-1);
			double[] scombV = toPowerArray(toDivideValue(toAddArray(toPowerArray(sigma_AV, 2), toPowerArray(se_sigma_cV, 2)), numel), 0.5);
			double[] tV = toDivideArray(toSubstracArray(a_estV, toMultiplyValue(sigma_resV, kLevel)), scombV);
			if(debug)
				toPrintToImp(tV, widthV, heightV, "tV");
			double[] pV = tdcf(toArrayMirror(tV), df2V);
			
			if(debug)
				toPrintToImp(df2V, widthV, heightV, "df2V");
			if(debug)
				toPrintToImp(c_estV, widthV, heightV, "c_estV");
			maskV = toArrayMaskThreshold(pV, 0.05,true);
		}else {
			maskV = toTrueArray(widthV, heightV);
		}
	}
	
	/**
	 * TCDF   Student's T cumulative distribution function (cdf). no consideration of  cauchy | normal | nans,
	 * no lower or upper for more precision
	 * @param xarr value
	 * @param varr degrees of freedom
	 * @return
	 */
	public double[] tdcf(double[] xarr, double[] varr)
	{
		assert xarr.length == varr.length;
		double[] ret = new double[xarr.length];
		for(int i=0;i<xarr.length;i++)
		{
			double x =xarr[i];
			double degreesOfFreedom = varr[i];
			if (x == 0) {
				ret[i] = 0.5;
			} else {
				double t =
						Beta.regularizedBeta(
								degreesOfFreedom / (degreesOfFreedom + (x * x)),
								0.5 * degreesOfFreedom,
								0.5);
				if (x < 0.0) {
					ret[i] = 0.5 * t;
				} else {
					ret[i] = 1.0 - 0.5 * t;
				}
			}
		}

		return ret;
	}
	
	
	public void process(boolean debug)
	{
		convolutions(debug);
		get2dKenel();
		solutionToLS();
		prefilter(true,debug);
		//local maxima above threshold in image domain
		imgLMV = toMultiplyArray(allMaxV, maskV);
		Pstruct pStruct = getpStruct(debug);
	}
	
	public ImagePlus getLoGv()
	{
		ImageProcessor ip = new FloatProcessor(toMatrixF(imgLoGV, widthV, heightV));
		return new ImagePlus("LoGV", ip);
	}
	
	public ImagePlus getRssV()
	{
		ImageProcessor ip = new FloatProcessor(toMatrixF(rssV, widthV, heightV));
		return new ImagePlus("rssV", ip);
	}
	
	public ImagePlus getMaskV()
	{
		ImageProcessor ip = new FloatProcessor(toMatrixF(maskV, widthV, heightV));
		return new ImagePlus("maskV", ip);
	}
	
	private Pstruct getpStruct(boolean debug) {
		double sumImgLM = toValueSumArray(imgLMV);
		if(sumImgLM == 0.0d)
			return null;
		if(debug)
			System.out.println("sum(imgLM) : " + sumImgLM);
		
		if(refineMaskLoG)
		{
			double logThreshold = toValueMin(imgLoGV, imgLMV);
			if(debug)
				System.out.println("logThreshold : " + logThreshold);
			int[] logMask = toArrayMaskThreshold(imgLoGV, logThreshold, false);
			maskV = toArrayMaskOr(maskV, logMask);
		}
		//re-select local maxima
		imgLMV = toMultiplyArray(allMaxV, maskV);
		if(debug)
		{
			toPrintToImp(imgLMV, widthV, heightV, "imgLMV");
			toPrintToImp(maskV, widthV, heightV, "maskV");
		}
		
		//% apply exclusion mask Not useful
		
		
		
		
		Pstruct pstruct = new Pstruct();
		return pstruct;
	}



	private int[] toArrayMaskOr(int[] maskV, int[] logMask) {
		assert maskV.length == logMask.length;
		int[] ret = new int[maskV.length];
		for(int i=0;i<maskV.length;i++)
		{
			if(maskV[i]==0 && logMask[i]==0)
				ret[i] = 0;
			else {
				ret[i] = 1;
			}
		}
		return ret;
	}

	private double toValueMin(double[] imgLoGV, double[] imgLMV) {
		assert imgLoGV.length == imgLMV.length;
		double min = Double.MAX_VALUE;
		for(int i=0;i<imgLoGV.length;i++)
		{
			double vLM = imgLMV[i];
			if(vLM != 0.0d)
			{
				double vLoG =imgLoGV[i];
				if(vLoG < min)
				{
					min = vLoG;
				}
			}
		}
		return min;
	}

	private double toValueSumArray(double[] arr) {
		double ret = 0.0d;
		for(int i=0;i<arr.length;i++)
			ret+=arr[i];
		return ret;
	}

	private int[] toTrueArray(int width, int height)
	{
		int[] tArr = new int[width*height];
		for(int i=0;i<tArr.length;i++)
			tArr[i] = 1;
		return tArr;
	}
	
	private int[] toArrayMaskThreshold(double[] arr1, double v, boolean less)
	{
		assert arr1!=null;
		int[] arr = new int[arr1.length];
		for(int i=0;i<arr1.length;i++)
			if(less)
			{
				if(arr1[i]<v)
					arr[i] = 1;
				else {
					arr[i] = 0;
				}
			}else {
				if(arr1[i]>=v)
					arr[i] = 1;
				else {
					arr[i] = 0;
				}
			}
		return arr;
	}

	private double[] toArrayMirror(double[] arr1)
	{
		assert arr1!=null;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
				arr[i] = 0-arr1[i];
		return arr;
	}
	
	private double[] toArrayPositive(double[] arr1)
	{
		assert arr1!=null;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			if(arr1[i]<0.0d)
				arr[i] = 0.0d;
			else {
				arr[i] = arr1[i];
			}
		return arr;
	}
	
	private double[] toSquareRootArray(double[] arr1) {
		assert arr1!=null;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = Math.sqrt(arr1[i]);
		return arr;
	}
	
	private double[] toPowerArray(double[] arr1, double power) {
		assert arr1!=null;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = Math.pow(arr1[i], power);
		return arr;
	}

	
	private double[] toSubstracArray(double[] arr1, double[] arr2) {
		assert arr1.length == arr2.length;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]-arr2[i];
		return arr;
	}
	private double[] toAddArray(double[] arr1, double[] arr2) {
		assert arr1.length == arr2.length;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]+arr2[i];
		return arr;
	}
	private double[] toDivideArray(double[] arr1, double[] arr2) {
		assert arr1.length == arr2.length;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]/arr2[i];
		return arr;
	}
	private double[] toDivideValue(double[] arr1, double value) {
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]/value;
		return arr;
	}
	private double[] toMultiplyValue(double[] arr1, double value) {
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]*value;
		return arr;
	}

	private double[] toMultiplyArray(double[] arr1, double[] arr2) {
		assert arr1.length == arr2.length;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]*arr2[i];
		return arr;
	}
	
	private double[] toMultiplyArray(double[] arr1, int[] arr2) {
		assert arr1.length == arr2.length;
		double[] arr = new double[arr1.length];
		for(int i=0;i<arr1.length;i++)
			arr[i] = arr1[i]*arr2[i];
		return arr;
	}
	
	private double[] toValidArr(double[] arr, int length, int width, int height) {
		assert width*height == arr.length : "width*height!=arr.length";		
		double[] arr1d = new double[(width-length+1)*(height-length+1)];
		for(int i=0,j=0;i<arr.length;i++)
		{
			int x = i%width;
			int y = i/width;
			if(x<length/2 || y<length/2 || x>width-length/2-1 || y>height-length/2-1)
				continue;
			arr1d[j++] = arr[i];
		}
		return arr1d;
	}

	/*
	 * this is for imageProcessor show, not correct for malatb matrix
	 */
	double[][] toMatrixD(double[] arr, int width, int height)
	{
		assert width*height == arr.length : "width*height!=arr.length";		
		double[][] arr2d = new double[width][height];
		for(int i=0;i<arr.length;i++)
		{
			arr2d[i/width][i%width] = arr[i];
		}
		return arr2d;
	}
	
	/*
	 * this is for imageProcessor show, not correct for malatb matrix
	 */
	float[][] toMatrixF(double[] arr, int width, int height)
	{
		assert width*height == arr.length : "width*height!=arr.length";		
		float[][] arr2d = new float[width][height];
		for(int i=0;i<arr.length;i++)
		{
			arr2d[i%width][i/width] = (float)arr[i];
		}
		return arr2d;
	}
	
	float[][] toMatrixF(int[] arr, int width, int height)
	{
		assert width*height == arr.length : "width*height!=arr.length";		
		float[][] arr2d = new float[width][height];
		for(int i=0;i<arr.length;i++)
		{
			arr2d[i%width][i/width] = (float)arr[i];
		}
		return arr2d;
	}

	
	float[] toFloatArray(double[] arr) {
		  if (arr == null) return null;
		  int n = arr.length;
		  float[] ret = new float[n];
		  for (int i = 0; i < n; i++) {
		    ret[i] = (float)arr[i];
		  }
		  return ret;
	}
	
	public double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
//	public static void testConvolutions()
//	{
//		ImagePlus imp = IJ.openImage();
//		PointSourceDetection pSD = new PointSourceDetection(imp, 2.5, true, true);
//		pSD.process(true);
//	}
	

	/*
	 * Two dimensional convolution.
	 *  C = CONV2(H1, H2, A) first convolves each column of A with the vector
%   H1 and then convolves each row of the result with the vector H2.  If
%   n1 = length(H1), n2 = length(H2), and [mc,nc] = size(C) then
%   mc = max([ma+n1-1,ma,n1]) and nc = max([na+n2-1,na,n2]).
%   CONV2(H1, H2, A) is equivalent to CONV2(H1(:)*H2(:).', A) up to
%   round-off.
	 */
	private double[] conv2(double[] g1, double[] g2, double[] pixels, int width, int height) {
		double[] pixelsOutput = new double[pixels.length];
		System.arraycopy(pixels, 0, pixelsOutput, 0, pixels.length);
		float[] g11 = new float[g1.length];
		float[] g22 = new float[g2.length];
		for(int i=0;i<g11.length;i++)
			g11[i] = (float)g1[i];
		for(int i=0;i<g22.length;i++)
			g22[i] = (float)g2[i];
		pixelsOutput = convolveFloat1D(pixelsOutput,g11,1,g11.length, width, height);
		pixelsOutput = convolveFloat1D(pixelsOutput,g22,g22.length,1, width, height);
		return pixelsOutput;
	}
	
	public double[] convolveFloat1D(double[] pixels2, float[] kernel, int kw, int kh, int width, int height) {
		int x1 = 0;
		int y1 = 0;
		int x2 = width;
		int y2 = height;
		int uc = kw/2;    
		int vc = kh/2;
		double[] pixels = new double[pixels2.length];
		double[] pixelsCopy = new double[pixels2.length];
		System.arraycopy(pixels2, 0, pixelsCopy, 0, pixels2.length);
		
		boolean vertical = kw==1;
		double sum;
		int offset, i;
		boolean edgePixel;
		int xedge = width-uc;
		int yedge = height-vc;
		for(int y=y1; y<y2; y++) {
			for(int x=x1; x<x2; x++) {
				sum = 0.0;
				i = 0;
				if (vertical) {
					edgePixel = y<vc || y>=yedge;
					offset = x+(y-vc)*width;
					for(int v=-vc; v<=vc; v++) {
						if (edgePixel){
							continue;
						}else{
							sum += pixelsCopy[offset+uc]*kernel[i++];
						}
						offset += width;
					}
				} else {
					edgePixel = x<uc || x>=xedge;
					offset = x+(y-vc)*width;
					for(int u = -uc; u<=uc; u++) {
						if (edgePixel)
							continue;
						else
							sum += pixelsCopy[offset+u]*kernel[i++];
					}
				}
				pixels[x+y*width] = (float)(sum);
			}
    	}
		return pixels;
    }
	

	public static ImageProcessor padarrayXT(ImageProcessor ip, int w) {		
		int[][] aIdx = getPaddingIndices(new int[]{ip.getHeight(),ip.getWidth()}, new int[]{w,w}); 
		ImageProcessor ipXT = transformMatlabIdxToIJ(aIdx, ip);
		return ipXT;
	}
	
	public static void testPadarrayXT()
	{
		ImagePlus imp = IJ.openImage();
		ImageProcessor ipXT = PointSourceDetection.padarrayXT(imp.getProcessor(), 5);
		ImagePlus impXT = new ImagePlus("impXT", ipXT);
		impXT.show();
	}
	
	public static ImageProcessor transformMatlabIdxToIJ(int[][] aIdx, ImageProcessor ip) {
		int[] row = aIdx[0];
		int[] column = aIdx[1];
		ImageProcessor ipXT = ip.createProcessor(column.length, row.length);
		for(int x=0;x<column.length;x++)
			for(int y=0;y<row.length;y++)
			{
				ipXT.set(x, y, ip.get(column[x]-1,row[y]-1));
			}
		return ipXT;
	}

	/*
	 * matlab getPaddingIndices function, use direction 'both', and method 'symmetric'
	 */
	public static int[][] getPaddingIndices(int[] aSize,int[] padSize)
	{
		//not considering if padSize.length is bigger than aSize
		int numDims = padSize.length;
		int[][] idx = new int[numDims][];
		int[] dimNums;
		for(int k=0; k<numDims;k++)
		{
			int m = aSize[k];
			int div = 0;
			if(m>1){
				dimNums = new int[2*m-2];
				div = 2*m-2;
				for(int j=1;j<=m;j++)
					dimNums[j-1] = j;
				
				for(int j=m+1, l=m-1;j<=div;j++,l--)
					dimNums[j-1] = l;
				
			}else {
					dimNums = new int[]{1,1};
					div = 2;
			}
			int p = padSize[k];
			idx[k] = new int[m+2*p];
			for(int j=-p,n=0; j<=m+p-1;j++,n++)
				idx[k][n] = dimNums[j-div*Math.floorDiv(j, div)];
		}
		return idx;
	}
	
	public static void testGetPaddingIndices()
	{
		int[][] aIdx = PointSourceDetection.getPaddingIndices(new int[]{60,50}, new int[]{5,5});
		for(int i=0; i<aIdx.length;i++)
			for (int j = 0; j < aIdx[i].length; j++) {
				System.out.println("aIdx cell " + i + " value : "+ aIdx[i][j]);
			}
	}
	
	public static void main(String[] args)
	{
		//PointSourceDetection.testGetPaddingIndices();
		ImageJ ij = new ImageJ();
		//PointSourceDetection.testPadarrayXT();
		ImagePlus img = IJ.openImage();
		double sigma = 3.2;
		boolean refineMaskLoG = false;
		boolean refineMaskValid = false;
		boolean whiteObject = true;
		PointSourceDetection pSD = new PointSourceDetection(img, sigma, refineMaskLoG, refineMaskValid,whiteObject);
		pSD.process(true);
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		
		return 0;
	}

	@Override
	public void run(ImageProcessor ip) {
		
		
	}
	
	
	
}
