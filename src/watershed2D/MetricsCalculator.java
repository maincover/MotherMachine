package watershed2D;

import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
import ij.plugin.Selection;
import ij.process.ColorProcessor;
import ij.process.ImageStatistics;

public class MetricsCalculator {
	
	/*private static float analyzeRegion(int column, int measure, Roi roi,ImagePlus img)
	{
		img.setRoi(roi);
		float analyzeMeasure=0;
		ResultsTable rt = new ResultsTable();
		Analyzer analyzer = new Analyzer(img,measure,rt);
		analyzer.run(img.getProcessor());
		analyzeMeasure = rt.getColumn(column)[0];
		return analyzeMeasure;
	}
	
	public static float circularity(Roi roi,ImagePlus img)
	{
		float objectCircularity=analyzeRegion(ResultsTable.CIRCULARITY,Measurements.CIRCULARITY,roi,img);
		return objectCircularity;
	}*/
	
	public static double perimeter(Roi roi,ImagePlus img)
	{
		//float objectPerimeter=analyzeRegion(ResultsTable.PERIMETER, Measurements.PERIMETER,roi,img);
		double objectPerimeter=roi.getLength();
		return objectPerimeter;
	}
	
	public static double meanGrayValue(Roi roi,ImagePlus img)
	{
		img.setRoi(roi);
		img.getProcessor().setRoi(roi);
		//float objectMeanGreyValue=analyzeRegion(ResultsTable.MEAN, Measurements.MEAN,roi,img);	
		ImageStatistics iStats = ImageStatistics.getStatistics(img.getProcessor(),Measurements.MEAN,null);
		double objectMeanGreyValue=iStats.mean;
		return objectMeanGreyValue;
	}
	
	public static double area(Roi roi,ImagePlus img)
	{
		img.setRoi(roi);
		img.getProcessor().setRoi(roi);
		ImageStatistics iStats = ImageStatistics.getStatistics(img.getProcessor(),Measurements.AREA,img.getCalibration());
		double objectArea=iStats.pixelCount;
		//float objectArea=analyzeRegion(ResultsTable.AREA, Measurements.AREA,roi,img);

		//display ROI
		img.getRoi().update(false, false);
		
		return objectArea;
	}
	
	public static double circularity(Roi roi,ImagePlus img)
	{
		double circularity=0;
		//double area = analyzeRegion(ResultsTable.AREA, Measurements.AREA,roi,img);
		double area = area(roi,img);
		//double perimeter = analyzeRegion(ResultsTable.PERIMETER, Measurements.PERIMETER,roi,img);
		double perimeter = perimeter(roi,img);
		circularity=(4*(float)Math.PI*Math.abs(area))/(perimeter*perimeter);
		return circularity;
	}
	// ratio of the area object and area of the convex hull
	public static double convexity(Roi roi,ImagePlus img)
	{
		double objectConvexity;
		double area = area(roi,img);
		
		Selection selection = new Selection();
		WindowManager.setTempCurrentImage(img);
		selection.run("hull");
		double areaConvexHull = area(img.getRoi(),img);
		
		objectConvexity = area/areaConvexHull;
		
		return objectConvexity;
	}
	
	public static float meanRadius(Roi roi,ImagePlus img)
	{
		float objectsResult=0;
	
		return objectsResult;
	}
	public static float eccentricity(float  objectsPixels,ImagePlus img)
	{
		float objectsResult=0;
		
		return objectsResult;
	}
}


