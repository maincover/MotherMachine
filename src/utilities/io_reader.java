package utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.Opener;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;

import javax.swing.JFileChooser;

public class io_reader {
	public static Object[] openFolderDialog(int interval,boolean sortBydate) {
		String pathDirectory = IJ.getDirectory("current");
		if(pathDirectory == null)
			pathDirectory = "D:";
	    File directory=new File(pathDirectory);
	    
		JFileChooser  chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(directory);
	    chooser.setDialogTitle("Choose folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    
	    
	    if (chooser.showOpenDialog(IJ.getInstance()) == JFileChooser.APPROVE_OPTION) { 
	    	System.out.println("getCurrentDirectory(): " 
	    			+  chooser.getCurrentDirectory());
	    	System.out.println("getSelectedFile() : " 
	    			+  chooser.getSelectedFile());
	    	
	    	
	    	
		    File file = chooser.getSelectedFile();
		    String[] names = file.list();
		    FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if(name.contains(".tif") || name.contains(".TIF") || name.contains(".TIFF") || name.contains(".tiff"))
						return true;
					else {
						return false;
					}					
				}
			};
			
			File[] filesORIG = null;
			File[] filesSEG = null;
			File[] filesMASK = null;
			File[] filesLM = null;
			String[] nameFolder = new String[5];
			nameFolder[4] = chooser.getSelectedFile().getName();
			//System.out.println("parent folder : " + nameFolder[4]);
		    for(String name : names)
		    {
		    	String pathSubFolder = file.getPath() +Prefs.getFileSeparator()+ name;		    	
		        if (new File(pathSubFolder).isDirectory())
		        {
		            System.out.println("folder-" + name);
		            if(name.contains("ORIG")||name.contains("orig"))
		            {
			        	nameFolder[0] = name;
		            	File subFolder = new File(pathSubFolder);
		            	filesORIG = subFolder.listFiles(filter);
		            }else if (name.contains("SEG")||name.contains("seg")) {
			        	nameFolder[1] = name;
		            	File subFolder = new File(pathSubFolder);
		            	filesSEG = subFolder.listFiles(filter);		            	
					}else if (name.contains("MASK")||name.contains("mask")) {
			        	nameFolder[2] = name;
						File subFolder = new File(pathSubFolder);
		            	filesMASK = subFolder.listFiles(filter);
					}else if (name.contains("LM")||name.contains("lm")) {
			        	nameFolder[3] = name;
						File subFolder = new File(pathSubFolder);
		            	filesLM = subFolder.listFiles(filter);
					}else {
						
					}
		        }else {
		        	//System.out.println("file-" + name);
				}
		    }
		    
		    if(sortBydate)
		    {
		    	if(filesORIG!=null)
		    	 Arrays.sort(filesORIG, new Comparator<File>() {
				    	@Override
				    	public int compare(File o1, File o2) {
				    		long n1 = getAcuquisitionTime(o1);
				    		long n2 = getAcuquisitionTime(o2);
				    		return safeLongToInt(n1 - n2);
				    	}
				    });

				    for(File f : filesORIG) {
				    	System.out.println("sorted " + f.getPath());
				    }
				    
				    if(filesSEG!=null)
				    Arrays.sort(filesSEG, new Comparator<File>() {
				    	@Override
				    	public int compare(File o1, File o2) {
				    		long n1 = getAcuquisitionTime(o1);
				    		long n2 = getAcuquisitionTime(o2);
				    		return safeLongToInt(n1 - n2);
				    	}
				    });
				    
				    if(filesMASK!=null)
				    Arrays.sort(filesMASK, new Comparator<File>() {
				    	@Override
				    	public int compare(File o1, File o2) {
				    		long n1 = getAcuquisitionTime(o1);
				    		long n2 = getAcuquisitionTime(o2);
				    		return safeLongToInt(n1 - n2);
				    	}
				    });
				    
				    if(filesLM!=null)
				    Arrays.sort(filesLM, new Comparator<File>() {
				    	@Override
				    	public int compare(File o1, File o2) {
				    		long n1 = getAcuquisitionTime(o1);
				    		long n2 = getAcuquisitionTime(o2);
				    		return safeLongToInt(n1 - n2);
				    	}
				    });
		    }else {
		    	if(filesORIG!=null)
		    	Arrays.sort(filesORIG, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('t') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });

			    if(filesSEG!=null)
			    Arrays.sort(filesSEG, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('_') + 2;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });
			    
/*			    for(File f : filesCFP) {
			    	System.out.println("Open file " + f.getPath());
			    }*/
			    
			    if(filesMASK!=null)
			    Arrays.sort(filesMASK, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('t') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });
			    
			    if(filesLM!=null)
			    Arrays.sort(filesLM, new Comparator<File>() {
			    	@Override
			    	public int compare(File o1, File o2) {
			    		int n1 = extractNumber(o1.getName());
			    		int n2 = extractNumber(o2.getName());
			    		return n1 - n2;
			    	}
			    	private int extractNumber(String name) {
			    		int i = 0;
			    		try {
			    			int s = name.lastIndexOf('t') + 1;
			    			int e = name.lastIndexOf('.');
			    			String number = name.substring(s, e);		    			
			    			i = Integer.parseInt(number);
			    		} catch(Exception e) {
			    			i = 0; // if filename does not match the format
			    			// then default to 0
			    		}
			    		return i;
			    	}
			    });
			}
		    return new Object[]{new File[][]{filesORIG,filesSEG,filesMASK,filesLM},nameFolder};
	    }else {
	    	System.out.println("No Selection ");
	    	return null;
	    }
	}
	public static long getAcuquisitionTime(File f) {		
		//if ((name == null) || (name == "")) return 0;			
		Opener fo = new Opener(); 
		ImagePlus imp = fo.openImage(f.getPath());
		Properties properties = imp.getProperties();
		Date date = null;
		if(properties != null)
		{
			String p = properties.toString();
			String t = null;
			if (p.contains("acquisition-time-local")) {
				t = p.substring(p.indexOf("acquisition-time-local")+43,p.indexOf("acquisition-time-local")+60);
				//System.out.println(t);
			}else {
				return f.lastModified();
			}	
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");				    						
			try {    
				date = formatter.parse(t);
				//System.out.println(name+" "+date.toString());
			} catch (ParseException e) {    
				e.printStackTrace();    
			}
			return date.getTime();
		}else {
			return f.lastModified();
		}		 
	}
	
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException
			(l + " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}
}
