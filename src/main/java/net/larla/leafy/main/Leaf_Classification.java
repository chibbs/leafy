package net.larla.leafy.main;

import java.awt.Frame;
import java.util.ArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;
import net.larla.leafy.common.*;
import net.larla.leafy.datamodel.*;

public class Leaf_Classification implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    String modelpath = "";

    /**
     * 
     * @param arg	String "custom" means that a custom classifier should be used -> display file-open dialogue
     * @param imp	image to be filtered
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	if (arg.length() > 0 && arg.equals("custom")) {
	    OpenDialog od = new OpenDialog("Select classifier...", OpenDialog.getLastDirectory(), "boosted.model");
	    this.modelpath = od.getDirectory() + od.getFileName();
	}
	return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    /*
     * (non-Javadoc)
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {

	ImagePlus imp_bin = LeafPreprocessor.preprocess(this.imp);

	Roi roi_leaf = imp_bin.getRoi();
	this.imp.setRoi(roi_leaf, true);

	Leaf currentleaf = new LeafAnalyzer().analyze(imp, imp_bin, "?");
	//new LeafAnalyzer().findLeafAxis(currentleaf, "", "test");
	
	//ResultsTable.getResultsTable().show("Results");
	LeafClassifier lc = new LeafClassifier(this.modelpath);
	//Instances inst = lc.buildInstances(ResultsTable.getResultsTable());
	String cls = "";

	try {
	    cls = lc.predictSingle(currentleaf);
	    //IJ.log(lc.getProp(5).toString());
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	// show name in image
	roi_leaf = currentleaf.getContour();
	roi_leaf.setName(cls);
	this.imp.setRoi(roi_leaf);
	IJ.run("Add Selection...");
	IJ.run("Labels...", "color=white font=14 show use draw");
	
	
	String title = "Leafy - Results of classification";
	TextWindow tw;
	TextPanel tp;
	Frame f = WindowManager.getFrame(title);
	if (f==null || !(f instanceof TextWindow)) {
		tw = new TextWindow(title,"",400,500);
	} else {
		tw = (TextWindow) f;
	}
	tp = tw.getTextPanel();
	tp.appendLine("Your leaf belongs to plant genus " + cls + ".");
	tp.appendLine("");
	tp.appendLine("Possibilities:");
	ArrayList<Tuple> pl = lc.getProp();
	for (Tuple t : pl) {
	    tp.appendLine(t.s + ": \t" + t.d + " %");
	}
	tw.toFront();

	/*ImagePlus diagram = new LeafAnalyzer().getCCDplot(currentleaf.getCcd().getCcd(), currentleaf.getCcd().getMaxdist());
	diagram.show();*/
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
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Acer_campestre_3_MEW2014.png");	// rechts
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Quercus_petraea_16_MEW2014.png");	// links
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Salix_alba_30_MEW2014.png");	// quer
	ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/1258487290_0004.jpg");
	image.show();

	// run the plugin
	IJ.runPlugIn(clazz.getName(), "");
	//IJ.runPlugIn("Leaf_Classification", "");

    }

}
