package watershed2D;

public class SetOfMetrics {
	
	private double[] mMetrics=null;
	
	public SetOfMetrics(int nbOfMetrics)
	{
		mMetrics = new double[nbOfMetrics];
	}
	
	public double getMetric(int metric)
	{
		if (metric>=getMetricsNb() && metric<0)
		{
			System.out.println("Error! the index of the metric is not valid");
			return -1;
		}
		else
		{
			return (mMetrics[metric]);
		}
	}
	
	public void setMetric(double metricsValue, int ind)
	{
		if(ind>=getMetricsNb() && ind<0)
		{
			System.out.println("Error! the index of the metric is not valid");
		}
		else
		{
			mMetrics[ind] = metricsValue;
		}
	}
	
	public String toString(){

		String	s = "";
		
		for(int i=0; i<getMetricsNb(); i++){
			s += i+":" +mMetrics[i]+"  ";
		}
		
		return s;

	}
	
	public int getMetricsNb(){
		return mMetrics.length;
	}
	
}
