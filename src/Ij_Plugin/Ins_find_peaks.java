package Ij_Plugin;

import ij.gui.GenericDialog;
import ij.plugin.filter.MaximumFinder;
import ij.process.FloatProcessor;

public class Ins_find_peaks {

	/* Fit_Polynomial.bsh
	 * IJ BAR: https://github.com/tferr/Scripts#scripts
	 *
	 * Returns the local maxima and minima of an ImageJ plot. Requires ImageJ 1.48d or newer.
	 * See http://fiji.sc/Find_Peaks for details.
	 * Tiago Ferreira, v1.0.3 2014.06.19
	 */


	public double tolerance = 0;
	public double threshold = 50;
	public double minPeakDistance = 20;
	double minMaximaValue = Double.NaN;
	double maxMinimaValue = Double.NaN;
	
	public Ins_find_peaks(int tolerance, int minPeakDist)
	{
		this.tolerance = tolerance;
		this.minPeakDistance = minPeakDist;
	}

	int[] findPositions(double[] values, boolean minima) {				
		float[] values_sort = new float[values.length];
		for (int i = 0 ; i < values.length; i++)
		{
			values_sort[i] = (float) values[i];
		}
		
		//System.arraycopy(values, 0, values_sort, 0, values.length); // if a problem occurs, change to cast from double to float		
		int[] positions = null;		
		byte[] maxIp = null;
		FloatProcessor ip = new FloatProcessor(values_sort.length, 1, values_sort);
		//new ImagePlus("", ip).show();
		MaximumFinder maxFinder = new MaximumFinder();
		if (minima)
		{
			ip.invert();
			maxIp = (byte[])maxFinder.findMaxima(ip, tolerance, threshold, MaximumFinder.SINGLE_POINTS, false, false).getPixels();
		}
		else
			maxIp = (byte[])maxFinder.findMaxima(ip, tolerance, threshold, MaximumFinder.SINGLE_POINTS, false, false).getPixels();
		
		if(maxIp == null)
			return null;
		positions = new int[maxIp.length];
		for(int i=0;i<positions.length;i++)
		{
			positions[i] = i;
		}
		
		int size = 0;
		for(int i=0;i<positions.length;i++)
		{			
			if(maxIp[i] != (byte)255)
			{
				values_sort[i] = 0;				
			}else {
				size ++;
			}
		}
		quicksort(values_sort,positions);		
		int[] newPos = new int[size];
		System.arraycopy(positions, positions.length-size, newPos, 0, size);		
		return newPos;
	}

	int[] findMaxima(double[] values) {
		return findPositions(values, false);
	}

	int[] findMinima(double[] values) {
		return findPositions(values, true);
	}

	double[] getPeakValue(double[] values, int[] positions) {
		int size = positions.length;
		double[] cc = new double[size];
		
		int j=0;
		for (int i=0; i<size; i++)
		{		
			cc[j] = values[ positions[i]];
			j = j + 1;
		}
				
		return cc;
	}

	boolean prompt() {
		GenericDialog gd = new GenericDialog("Find Local Maxima/Minima...");
		gd.addNumericField("Min._peak_amplitude:", tolerance, 2);
		gd.addNumericField("Min._peak_distance:", minPeakDistance, 2);
		gd.addNumericField("Min._value of maxima:", minMaximaValue, 2, 6, "(NaN: no filtering)");
		gd.addNumericField("Max._value of minima:", maxMinimaValue, 2, 6, "(NaN: no filtering)");

		gd.addHelp("http://fiji.sc/Find_Peaks");
		//gd.showDialog();
		//boolean dialogOKed = (gd.wasCanceled()) ? false : true;
		tolerance = gd.getNextNumber();
		minPeakDistance = gd.getNextNumber();
		minMaximaValue = gd.getNextNumber();
		maxMinimaValue = gd.getNextNumber();
		return true;
	}



	// min peak distance instead of original trimpeakDistance,
	// Beginning with the largest peak, the algorithm ignores all identified peaks not separated by more than the value of 'MINPEAKDISTANCE'
	// You can use the 'MINPEAKDISTANCE' option to specify that the algorithm ignore small peaks that occur in the neighborhood of a larger peak
	int[] trimPeakDistance(int[] peakPosition, int[] index) {
		int size = peakPosition.length;
		boolean[] ignorePositions = new boolean[size];		
		
		int[] temp = new int[size];
		int newsize = 0;
		for (int i=size-1; i>=0; i--) {
			int pos1 = peakPosition[i];
			if(ignorePositions[i])
				continue;			
			for (int j=i-1; j>=0; j--) {
				if(ignorePositions[j]) // avoid twice
					continue;
				int pos2 = peakPosition[j];
				if (Math.abs(index[pos2] - index[pos1]) < minPeakDistance)
				{
					ignorePositions[j] = true; //including itself
				}
			}			
			temp[newsize++] = pos1;			
		}
		int[] newpositions = new int[newsize];
		for (int i=0; i<newsize; i++)
			newpositions[i] = temp[i];
		return newpositions;
	}

	// marina's image has sometimes two peaks, because of the vertical edges are added together, then have two peaks, but cann't say the biggest peak is the best, so better to take the center of the two peaks
	// the distTwoPeak should be small as 5 pixels,
	
	int[] trimPeakDistance(int[] peakPosition, int[] index, int distTwoPeak) {
		int size = peakPosition.length;
		boolean[] ignorePositions = new boolean[size];		
		
		int[] temp = new int[size];
		int newsize = 0;
		for (int i=size-1; i>=0; i--) {
			int pos1 = peakPosition[i];
			if(ignorePositions[i])
				continue;			
			for (int j=i-1; j>=0; j--) {
				if(ignorePositions[j]) // avoid twice
					continue;
				int pos2 = peakPosition[j];
				if (Math.abs(index[pos2] - index[pos1]) < minPeakDistance )
				{
					ignorePositions[j] = true; //including itself
					if(Math.abs(index[pos2] - index[pos1])<=distTwoPeak)
					{
						//System.out.println("find two close peaks pos 1 " + pos1 + " pos 2 : "  + pos2);
						pos1 = (int)( (pos2 + pos1)*0.5);
					}
				}
			}			
			temp[newsize++] = pos1;			
		}
		int[] newpositions = new int[newsize];
		for (int i=0; i<newsize; i++)
			newpositions[i] = temp[i];
		return newpositions;
	}
	
	public Object[] findPeaks(double[] profile) {
		if (!prompt()) return null;
		int[] index = new int[profile.length];
		for(int i=0; i<profile.length; i++)
			index[i] = i;
		int[] peakPosition = findMaxima(profile);		
		if (minPeakDistance>0) {
			peakPosition = trimPeakDistance(peakPosition, index);			
		}
		double[] peakValue = getPeakValue(profile, peakPosition);
		return new Object[]{peakPosition,peakValue};
	}
	
	/**
	 * 
	 * @param profile
	 * @param centerTwoPeaks if is true, the dist will be considered
	 * @param dist two close peaks, then take the center
	 * @return
	 */
	public Object[] findPeaks(double[] profile, boolean centerTwoPeaks, int dist) {
		if (!prompt()) return null;
		int[] index = new int[profile.length];
		for(int i=0; i<profile.length; i++)
			index[i] = i;
		int[] peakPosition = findMaxima(profile);		
		if (minPeakDistance>0) {
			if(centerTwoPeaks)
				peakPosition = trimPeakDistance(peakPosition, index, dist);
			else {
				peakPosition = trimPeakDistance(peakPosition, index);
			}
		}
		double[] peakValue = getPeakValue(profile, peakPosition);
		return new Object[]{peakPosition,peakValue};
	}
	
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
	
	public static void main(String[] arg)
	{
//		ImagePlus imp = IJ.openImage();
//		ImageProcessor ip =imp.getProcessor();
//		imp.show();
//		
//		FftBandPassFilter fftBandPassFilter = null; 
//		fftBandPassFilter = new FftBandPassFilter();
//		fftBandPassFilter.setup("fft band pass filter", imp);
//
//		int roi_width = imp.getWidth();
//		int height = imp.getHeight();
//
//
//		fftBandPassFilter.run(ip);
//		imp.updateAndDraw();
//		ip = ip.convertToByte(true);
//		ImagePlus impPlotProfile = new ImagePlus("", ip);
//		impPlotProfile.show();
//		//impPlotProfile.setRoi(0, 0, roi_width, height);
//		impPlotProfile.setRoi(roi_width/4+1, 0, roi_width/2, height);
//		ProfilePlot pPlot = new ProfilePlot(impPlotProfile, true);
//		double[] profile = pPlot.getProfile();
//		
//		float[] profile_f = new float[profile.length];
//		double[] index = new double[profile.length];
//		for(int i=0;i<index.length;i++)
//		{
//			index[i] = i;
//			profile_f[i] = (float)profile[i];
//		}
//		Ins_find_peaks peakFinder = new Ins_find_peaks(40, 30);
//		Object[] out = peakFinder.findPeaks(index, profile_f);
//
//		int position[] = (int[])out[0];
//		double peaks[] = (double[])out[1];
//		
//		for(int i=0;i<peaks.length;i++)
//		{
//			if(position[i] < 30 || position[i] > index.length - 30)
//				continue;
//			ip.setValue(0);
//			ip.drawLine(0, (int)position[i], roi_width, (int)position[i]);
//			System.out.println("Position i : " + i + " - "+ position[i] + " - max - " + peaks[i]);
//		}
//		
	}

}
