package net.larla.leafy.main;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.larla.leafy.common.*;
import net.larla.leafy.datamodel.*;
import weka.core.Instances;

public class Leaf_Classification implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    String cls = "";

    @Override
    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	if (arg.length() > 0)
	    this.cls = arg;
	return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {

	ImagePlus imp_bin = LeafPreprocessor.preprocess(this.imp);

	Roi roi_leaf = imp_bin.getRoi();
	this.imp.setRoi(roi_leaf, true);
	
	Leaf currentleaf = new Leaf(imp, imp.getShortTitle(), cls, roi_leaf, imp_bin);

	/*RoiManager rm = RoiManager.getInstance();
	if (rm == null)
	    rm = new RoiManager();
	if (roi_leaf != null) {
	    roi_leaf.setName("Leaf");
	    rm.add(this.imp, roi_leaf, 0);
	}*/


	LeafAnalyzer la = new LeafAnalyzer();	// TODO: Options übergeben
	la.analyze(currentleaf);
	la.calcCCD(currentleaf);
	// la.findPetiole( imp_gray ); // TODO: imageplus entfernen und nur mit roi messen
	la.fillResultsTable(currentleaf);
	//ResultsTable.getResultsTable().show("Results");
	LeafClassifier lc = new LeafClassifier();
	Instances inst = lc.buildInstances(ResultsTable.getResultsTable());
	String cls = "";
	try {
	    cls = lc.predictSingle(inst);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	roi_leaf = this.imp.getRoi();
	roi_leaf.setName(cls);
	this.imp.setRoi(roi_leaf);
	IJ.run("Add Selection...");
	IJ.run("Labels...", "color=white font=14 show use draw");
	
	/*
	// Create empty instance with three attribute values 
	 Instance inst = new DenseInstance(3); 

	 // Set instance's values for the attributes "length", "weight", and "position"
	 inst.setValue(0, 5.3); 
	 inst.setValue(1, 300); 
	 inst.setValue(2, "first");*/

	// imp.hide();
	// rm.setVisible(false);
	// imp_bin.hide();
    }

    /**
     * Main method for debugging.
     *
     * For debugging, it is convenient to have a method that starts ImageJ,
     * loads an image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args
     *            unused
     */
    public static void main(String[] args) {
	// set the plugins.dir property to make the plugin appear in the Plugins
	// menu
	Class<?> clazz = Leaf_Classification.class;
	String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	String pluginsDir = url.substring("file:".length(),
		url.length() - clazz.getName().length() - ".class".length());
	System.setProperty("plugins.dir", pluginsDir);

	// start ImageJ
	new ImageJ();

	// open sample
	// ImagePlus image =
	// IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/populus_tremula/Populus_tremula_20_MEW2014.png");
	// ImagePlus image =
	// IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/acer_platanoides/Acer_platanoides_3_MEW2014.png");
	// ImagePlus image =
	// IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/quercus_petraea/Quercus_petraea_13_MEW2014.png");
	ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder2/Betula_pubescens_16_MEW2014.png");
	image.show();

	// run the plugin
	IJ.runPlugIn(clazz.getName(), "");

    }

}