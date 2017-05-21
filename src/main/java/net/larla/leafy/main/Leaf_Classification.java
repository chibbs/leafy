package net.larla.leafy.main;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.larla.leafy.common.LeafAnalyzer;
import net.larla.leafy.common.LeafClassifier;
import net.larla.leafy.common.LeafPreprocessor;
import net.larla.leafy.datamodel.Leaf;

public class Leaf_Classification implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    String modelpath = "";

    @Override
    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	if (arg.length() > 0 && arg.equals("custom")) {
	    OpenDialog od = new OpenDialog("Select classifier...", OpenDialog.getLastDirectory(), "boosted.model");
	    this.modelpath = od.getDirectory() + od.getFileName();
	}
	return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {

	ImagePlus imp_bin = LeafPreprocessor.preprocess(this.imp);

	Roi roi_leaf = imp_bin.getRoi();
	this.imp.setRoi(roi_leaf, true);

	Leaf currentleaf = new LeafAnalyzer().analyze(imp, imp_bin, "?");
	
	ResultsTable.getResultsTable().show("Results");
	LeafClassifier lc = new LeafClassifier(this.modelpath);
	//Instances inst = lc.buildInstances(ResultsTable.getResultsTable());
	String cls = "";

	try {
	    cls = lc.predictSingle(currentleaf);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	roi_leaf = this.imp.getRoi();
	roi_leaf.setName(cls);
	this.imp.setRoi(roi_leaf);
	IJ.run("Add Selection...");
	IJ.run("Labels...", "color=white font=14 show use draw");

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
	String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length() + clazz.getPackage().getName().length());
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
	//IJ.runPlugIn("Leaf_Classification", "");

    }

}
