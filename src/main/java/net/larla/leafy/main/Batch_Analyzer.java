package net.larla.leafy.main;
import java.awt.Color;
import java.io.*;
import java.util.*;
import ij.*;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import net.larla.leafy.common.*;
import net.larla.leafy.datamodel.*;
import weka.core.Instances;

public class Batch_Analyzer implements PlugIn {

    @Override
    public void run( String arg ) {
	//LeafClassifier lc = new LeafClassifier();
	String groundTruth = "";
	// process folder
	String dir1 = IJ.getDirectory("Select folder with training images...");
	if (dir1==null) 
	    return;
	String dir3 = dir1 + "leaf.arff";
	File f = new File(dir3);
	HashMap<String,String> classmap = new HashMap<String, String>();

	try{
	    File toRead=new File(dir1+"classes.txt");
	    FileInputStream fis=new FileInputStream(toRead);
	    ObjectInputStream ois=new ObjectInputStream(fis);

	    classmap = (HashMap<String,String>)ois.readObject();

	    ois.close();
	    fis.close();
	    //print All data in MAP
	    for(Map.Entry<String,String> m :classmap.entrySet()){
		System.out.println(m.getKey()+" : "+m.getValue());
	    }
	}catch(Exception e){}

	if(!f.exists() || f.isDirectory()) {  // TODO: Teste ob arff-Datei schon existiert
	    String[] list = new File(dir1).list();
	    if (list==null) return;
	    for (int i=0; i<list.length; i++) {
		IJ.showProgress(i, list.length);
		//IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
		IJ.showStatus(i+"/"+list.length);
		boolean isDir = (new File(dir1+list[i])).isDirectory();
		if (!isDir && !list[i].startsWith(".")) {
		    IJ.log("Image: " + dir1+list[i]);
		    ImagePlus img = IJ.openImage(dir1+list[i]);
		    if (img==null) continue;

		    if (classmap.containsKey(list[i])) {
			groundTruth = classmap.get(list[i]);
		    } else {
			if (list[i].contains( "_")) {
			    //groundTruth = list[i].split( "_" )[0] + " " + list[i].split( "_" )[1];	// Klasse = Gattung und Art
			    groundTruth = list[i].split( "_" )[0];					// Klasse = Gattung
			    classmap.put(list[i], groundTruth);
			} else {
			    groundTruth = "";
			}  
		    }

		    WindowManager.setTempCurrentImage(img);     // needed because image is not shown (no images open)

		    // run the plugin
		    ImagePlus imp_bin = LeafPreprocessor.preprocess(img);

		    Roi roi_leaf = imp_bin.getRoi();
		    img.setRoi(roi_leaf, true);

		    Leaf currentleaf = new Leaf(img, img.getShortTitle(), groundTruth, roi_leaf, imp_bin);

		    LeafAnalyzer la = new LeafAnalyzer();	// TODO: Options übergeben
		    la.calculateFeatures(currentleaf);
		    la.findLeafAxis(currentleaf);
		    la.findPetiole(currentleaf);
		    
		    img.setOverlay(currentleaf.getPetioleroi(), Color.YELLOW, 3, null);
		    img = img.flatten();
		    img.setOverlay(currentleaf.getLeafaxis(), Color.GREEN, 3, null);
		    FileSaver fs = new FileSaver(img.flatten());
		    fs.saveAsJpeg(dir1 + "ccd/" + img.getShortTitle() + "_axis.jpg");
		    la.calcCCD(currentleaf);
		    la.saveCCDplot(currentleaf, dir1, img.getShortTitle());
		    la.fillResultsTable(currentleaf);

		}
	    }
	    IJ.showProgress(1.0);
	    IJ.showStatus("");

	    close_windows();
	    String dir2 = dir1 + "leaf.csv";

	    ResultsTable rt = ResultsTable.getResultsTable();
	    rt.save( dir2);
	    try{
		File fileOne=new File(dir1+"classes.txt");
		FileOutputStream fos=new FileOutputStream(fileOne);
		ObjectOutputStream oos=new ObjectOutputStream(fos);

		oos.writeObject(classmap);
		oos.flush();
		oos.close();
		fos.close();
	    }catch(Exception e){}
	    Instances inst = LeafClassifier.buildInstances(rt);

	    // save weka data
	    try {
		BufferedWriter writer = new BufferedWriter(
			new FileWriter(dir3));
		writer.write(inst.toString());
		writer.newLine();
		writer.flush();
		writer.close();
	    } catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	} 
	SaveDialog sd = new SaveDialog("Save new classifier...", "myClassifier", ".model");
	String dir4 = sd.getDirectory() + sd.getFileName();
	if (dir4.contains("null")) {
	    //rt.show("Results");
	    return;
	}

	LeafClassifier customClassifier = LeafClassifier.train(dir3);
	customClassifier.saveModel(dir4);

    }

    public static void main(String[] args) {

	// set the plugins.dir property to make the plugin appear in the Plugins menu
	Class<?> clazz = Batch_Analyzer.class;
	String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length() + clazz.getPackage().getName().length());

	System.setProperty("plugins.dir", pluginsDir);

	// start ImageJ
	new ImageJ();

	//run the plugin
	IJ.runPlugIn(clazz.getName(), "");


	IJ.run("Quit");
	System.exit( 0 );
    }

    public static void close_windows() {
	//http://imagej.1557.x6.nabble.com/Re-Plugin-Command-To-Close-Window-Without-quot-Save-Changes-quot-Dialog-td3683293.html
	ImagePlus img;
	while (null != WindowManager.getCurrentImage()) {
	    img = WindowManager.getCurrentImage();
	    img.changes = false;
	    img.close();
	}
    }
}
