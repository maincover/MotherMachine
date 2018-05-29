package lineage;
import java.util.Arrays;
import java.util.Vector;


public class Ins_cellsVector {
	private Vector<Ins_cell> cells;
	//private boolean ifTwoCellsMunalTracking = false;
	
	private int trackingCellsNum;
	private Ins_cell[] trackingCells;
	
	private int timeIndex;
	private String time="-1";//the acquisition time ms
	//protected int depth;
	//protected boolean[] ifDivideNext1;
	
	
	
	public Ins_cellsVector(int timeIndex)
	{
		
		this.timeIndex = timeIndex;
		this.cells = new Vector<Ins_cell>(5);
		//this.depth = 10;
		//this.ifDivideNext = new boolean[depth];
/*		this.trackingCellsNum = 0;
		this.trackingCells =  new Ins_cell[2];
*//*		for(int i=0;i<depth;i++)
			ifDivideNext[i]=false;
*/	}
	
	
	
	public Vector<Ins_cell> getCellVector()
	{
		return cells;
	}
	
//	public int getSlice()
//	{
//		return slice;
//	}
	
	public int getTimeIndex()
	{
		return timeIndex;
	}
	
	public void insertCell(Ins_cell cell)
	{
		cell.setTime(time);
		cells.add(cell);		
		Ins_cell[] cellsArray = cells.toArray(new Ins_cell[cells.size()]);
		Arrays.sort(cellsArray);
		cells = new Vector<Ins_cell>(cellsArray.length);
		int num = 0;
		for(int i=0 ; i< cellsArray.length; i++)
		{				
			num = num + 1;
			cellsArray[i].setCellNumber(num);
			cells.add(cellsArray[i]);
		}
	}
	
	public void insertCell(int cellNum, Ins_cell cell)
	{
		cells.add(cellNum-1, cell);
		cell.setTime(time);		
		for(Ins_cell cell2 : cells)
		{
			if(cell2.getCellNumber()>=cellNum)
			{
				cell2.setCellNumber(cell2.getCellNumber()+1);
			}
		}
		cell.setCellNumber(cellNum);
	}
	
	public void removeCell(Ins_cell cell)
	{
		boolean removed = cells.remove(cell);
		if(!removed)
		{
			System.out.println("cell is not removed, cell position: " + cell.getRoi().getPosition() + " x: " + cell.getRoi().getBounds().x + " y: " + cell.getRoi().getBounds().y);
			return;
		}
		Ins_cell[] cellsArray = cells.toArray(new Ins_cell[cells.size()]);
		Arrays.sort(cellsArray);
		cells = new Vector<Ins_cell>(cellsArray.length);
		int num = 0;
		for(int i=0 ; i< cellsArray.length; i++)
		{				
			num = num + 1;
			cellsArray[i].setCellNumber(num);
			cells.add(cellsArray[i]);
		}
	}
	
	
	
	public Ins_cell getCell(int cellNum) 
	{				
		if(cellNum <= cells.size() && cellNum>0)
			return cells.get(cellNum-1);
		else {
			return null;
		}
	}


/*	public void setDividing(int depthIndex, boolean ifDivideNext) {
		// TODO Auto-generated method stub
		if(depthIndex > this.depth)
		{
			IJ.error("Depth is more than "+this.depth);
			return;
		}		
		this.ifDivideNext[depthIndex] = ifDivideNext;		
	}*/


	public void setTime(String time) {
		// TODO Auto-generated method stub
		this.time = time;
	}

	public void insertTrackingCells(Ins_cell trackingCell)
	{
		trackingCells[trackingCellsNum] = trackingCell;
		trackingCellsNum ++;
	}
	
	public int getTrackingCellsNum()
	{
		return trackingCellsNum;
	}
	
	public boolean ifHave2TrackingCells()
	{
		return trackingCellsNum == 2;
	}
	
	public Ins_cell[] getTrackingCells() {
		// TODO Auto-generated method stub
		return trackingCells;
	}
	
	
}
