package net.larla.leafy.main;
import java.io.*;
import java.util.*;

import ij.*;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import net.larla.leafy.common.*;
import net.larla.leafy.datatypes.*;
import net.larla.leafy.helpers.*;

public class Batch_Analyzer extends Leafy implements PlugIn {

    @Override
    public void run( String arg ) {
	this.parseArg(arg);
	//LeafClassifier lc = new LeafClassifier();
	String groundTruth = "";
	// process folder
	String trainfolder = IJ.getDirectory("Select folder with training images...");
	if (trainfolder==null) 
	    return;
	String arffpath = trainfolder + "_leaf.arff";
	File f = new File(arffpath);
	HashMap<String,String> classmap = FileHelper.file2Map(trainfolder+"_classes.csv");

	if(!f.exists() || f.isDirectory()) {  // TODO: Teste ob arff-Datei schon existiert
	    String[] imglist = new File(trainfolder).list();
	    if (imglist==null) return;
	    for (int i=0; i<imglist.length; i++) {
		IJ.showProgress(i, imglist.length);
		//IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
		IJ.showStatus(i+"/"+imglist.length);
		boolean isDir = (new File(trainfolder+imglist[i])).isDirectory();
		if (!isDir && !imglist[i].startsWith(".")) {
		    IJ.log("Image: " + trainfolder+imglist[i]);
		    ImagePlus img = IJ.openImage(trainfolder+imglist[i]);
		    if (img==null) continue;

		    if (classmap.containsKey(imglist[i])) {
			groundTruth = classmap.get(imglist[i]);
		    } else {
			if (imglist[i].contains( "_")) {
			    groundTruth = imglist[i].split( "_" )[0] + " " + imglist[i].split( "_" )[1];	// Klasse = Gattung und Art
			    //groundTruth = list[i].split( "_" )[0];					// Klasse = Gattung
			    classmap.put(imglist[i], groundTruth);
			} else {
			    groundTruth = "";
			}  
		    }

		    WindowManager.setTempCurrentImage(img);     // needed because image is not shown (no images open)

		    // run the plugin
		    ImagePlus imp_bin = LeafPreprocessor.preprocess(img);

		    img.setRoi(imp_bin.getRoi(), true);

		    int anOptions = (findPetiole)?1:0 * LeafAnalyzer.FINDPETIOLE;
		    LeafAnalyzer la = new LeafAnalyzer(anOptions);
		    Leaf currentleaf = la.analyze(img, imp_bin, groundTruth);
		    //la.saveCCDplot(currentleaf, trainfolder + "ccd/, img.getShortTitle());
		    //la.saveOverlayImg(currentleaf, trainfolder + "ccd/", img);

		}
	    }
	    IJ.showProgress(1.0);
	    IJ.showStatus("");

	    close_windows();

	    ResultsTable rt = ResultsTable.getResultsTable();
	    //rt.save( trainfolder + "_leaf.csv");
	    FileHelper.writeToFile(FileHelper.map2String(classmap),trainfolder+"_classes.csv");
	    WekaHelper.table2arff(rt, arffpath);
	} 
	
	// train classifier and serialize
	SaveDialog sd = new SaveDialog("Save new classifier...", "myClassifier", ".model");
	String dir4 = sd.getDirectory() + sd.getFileName();
	if (dir4.contains("null")) {
	    //rt.show("Results");
	    return;
	}

	LeafClassifier customClassifier = LeafClassifier.train(arffpath);
	
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
	IJ.runPlugIn(clazz.getName(), "withoutpetiole");

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
