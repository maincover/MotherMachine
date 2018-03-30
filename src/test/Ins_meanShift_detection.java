package test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProfilePlot;
import ij.plugin.Profiler;
import ij.process.ImageProcessor;
import ij.util.Tools;

import java.awt.Dimension;
import java.util.ArrayList;

public class Ins_meanShift_detection {
	
	public double[] meanShiftVector(double[] profile, double sigmax, double sigmav)
	{
		double[] d = new double[profile.length];
		double[] v_smooth = new double[profile.length];
		double[] labelx = new double[profile.length];
		//double[] xkplus1 = new double[profile.length];
		for(int i=0;i<profile.length;i++)
		{
			labelx[i] = i;
			double x = i;			
			double v = profile[i];
			double diff = 0;
			int iteration = 0;
			double smoothv = 0;
			double x_meanshift = x;
			do {
				double[][] regionProfile = refineProfile(x,profile,sigmax);
				double[] evaluation = evaluateMS(x,v,regionProfile, sigmax,sigmav);
				diff = Math.abs(evaluation[0] - x);
				x = evaluation[0];
				smoothv = evaluation[1];
				x_meanshift = evaluation[0];
				iteration++;
			} while (diff > 0.05);
			d[i] = x_meanshift - i;
			profile[i] = smoothv;
			v_smooth[i] = smoothv;
		}
		Plot plot = getPlot(profile);
		plot.show();		
		Plot plot2 = getPlot(d);		
		plot2.show();
		return d;
	}

	private double[] evaluateMS(double xc, double vc, double[][] profile, double sigmax, double sigmav) {
		double sumNumeratorX = 0.0d;
		double sumNumeratorV = 0.0d;
		double sumDenomitor = 0.0d;
		sigmax = sigmax * sigmax * 2;
		sigmav = sigmav * sigmav * 2;
		for(int i=0; i<profile.length;i++)
		{
			double x = profile[i][0];
			double v = profile[i][1];
			double tmp = 0;
			if(vc-v != 0)			
				tmp = Math.exp(-(xc-x)*(xc-x)/sigmax)*Math.exp(-(vc-v)*(vc-v)/sigmav);
			sumNumeratorX = sumNumeratorX+x*tmp;
			sumNumeratorV = sumNumeratorV+v*tmp;			
			sumDenomitor = sumDenomitor+tmp;
		}
		// TODO Auto-generated method stub
		if(sumDenomitor != 0)
			return new double[]{sumNumeratorX/sumDenomitor, sumNumeratorV/sumDenomitor};
		else {
			return new double[]{xc, vc};
		}
	}

	private double[][] refineProfile(double xc, double[] profile,
			double sigmax) {
		ArrayList<double[]> list = new ArrayList<double[]>();
		for(int i=0; i<profile.length; i++)
		{
			double x = i;
			double v = profile[i];
			
			if(Math.sqrt((x-xc)*(x-xc)) <= (int)(3*sigmax))
			{
				list.add(new double[]{x,v});
			}
		}
		return list.toArray(new double[list.size()][]);
	}
	
	public double[] getProfile(int[] p0, int[] p1, ImagePlus imp)
	{
		imp.setRoi(new Line(p0[0], p0[1], p1[0], p1[1]));;
		ProfilePlot profilePlot = new ProfilePlot(imp);
		profilePlot.createWindow();
		return profilePlot.getProfile();
	}
	
	public Plot getPlot(double[] profile) {		
		if (profile==null)
			return null;
		float[] xValues = null;
		String xLabel = "Distance ("+"pix"+")";
  		int n = profile.length;
  		if (xValues==null) {
			xValues = new float[n];
			for (int i=0; i<n; i++)
				xValues[i] = (float)(i*1);
		}
        float[] yValues = new float[n];
        for (int i=0; i<n; i++)
        	yValues[i] = (float)profile[i];
		Plot plot = new Plot("Plot of profile", xLabel, "v", xValues, yValues);
		return plot;
	}
	
	public static void main(String[] args)
	{
		ImagePlus imp = IJ.openImage("d:/profile.tif");
		imp.show();
		int[] p0 = new int[]{140,160};
		int[] p1 = new int[]{280,160};
		Ins_meanShift_detection meanShift_detection = new Ins_meanShift_detection();
		double[] profile = meanShift_detection.getProfile(p0, p1, imp);
		double sigmax = 20;
		double sigmav = 5;
		//double[] profileMirror = mirrorProfile(sigmax,profile);
		meanShift_detection.meanShiftVector(profile, sigmax, sigmav);
	}

	private static double[] mirrorProfile(double sigmax, double[] profile) {
		int n = (int)(3*sigmax);
		double[] mirror = new double[profile.length + 2*n];
		for (int i = 0; i < n; i++) {
			if(n-i -1 < profile.length)
				mirror[i] = profile[n-i - 1];
		}
		
		for (int i = n; i < n + profile.length; i++) {			
			mirror[i] = profile[i-n];
		}
		
		for (int i = n+profile.length, k =0; i < mirror.length; i++,k++) {
			if(profile.length - 1 - k > 0)
				mirror[i] = profile[profile.length - 1 - k];
		}
		return mirror;
		
	}
}
