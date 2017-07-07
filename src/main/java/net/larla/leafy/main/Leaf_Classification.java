package net.larla.leafy.main;

import java.util.ArrayList;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.larla.leafy.common.LeafAnalyzer;
import net.larla.leafy.common.LeafClassifier;
import net.larla.leafy.common.LeafPreprocessor;
import net.larla.leafy.datatypes.Leaf;
import net.larla.leafy.datatypes.Tuple;

public class Leaf_Classification extends Leafy implements PlugInFilter {
    ImagePlus imp;

    /**
     * 
     * @param arg	String "custom" means that a custom classifier should be used -> display file-open dialogue
     * @param imp	image to be filtered
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	this.parseArg(arg);
	this.imp = imp;
	return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    /*
     * (non-Javadoc)
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {

	ImagePlus imp_bin = LeafPreprocessor.preprocess(this.imp);

	this.imp.setRoi(imp_bin.getRoi(), true);
	
	int anOptions = ((verbose)?1:0) * LeafAnalyzer.VERBOSEMODE +
			((findPetiole)?1:0) * LeafAnalyzer.FINDPETIOLE +
			LeafAnalyzer.USEROIMANAGER+ 
		    	((this.saveoverlayimg)?1:0) * LeafAnalyzer.SAVEOVERLAYIMG+ 
		    	((this.saveccdplot)?1:0) * LeafAnalyzer.SAVECCD;
	Leaf currentleaf = new LeafAnalyzer(anOptions).analyze(imp, imp_bin, "?");
	LeafClassifier lc = new LeafClassifier(this.modelpath);
	String cls = "";

	cls = lc.predictSingle(currentleaf);

	ArrayList<Tuple> pl = lc.getProb();
	
	// show name in image
	this.createOverlay(currentleaf, cls, this.imp);
	
	// show results
	this.showResults(cls, pl, this.imp.getShortTitle());
	

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
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Ulmus_minor_24_MEW2014 - Kopie.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Quercus_petraea_56_MEW2014 - Kopie.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Salix_alba_6_MEW2014.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Alnus_glutinosa_18_MEW2014.png");
	ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Acer_campestre_6_MEW2014.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Platanus_hispanica_20_MEW2014.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/Testbilder/Testbild2.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Desktop/test.png");
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura1/Training/Populus_alba_81_MEW2014.png");	// alt, ohne Stiel
	//ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura2/Test/Populus_alba_81_MEW2014.png");
	image.show();

	// run the plugin
	IJ.runPlugIn(clazz.getName(), "");
	//IJ.runPlugIn("Leaf_Classification", "");

    }
    
    

}
