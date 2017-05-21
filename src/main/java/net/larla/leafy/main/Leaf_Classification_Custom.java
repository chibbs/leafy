package net.larla.leafy.main;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.larla.leafy.common.*;
import net.larla.leafy.datamodel.*;
import weka.core.Instances;

public class Leaf_Classification_Custom implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    String modelpath = "";

    @Override
    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {
	OpenDialog od = new OpenDialog("Select classifier...", OpenDialog.getLastDirectory(), "myCustom.model");
        String modelfilepath = od.getDirectory() + od.getFileName();
        this.imp = new ImagePlus(this.imp.getShortTitle(), ip);
	IJ.runPlugIn("net.larla.leafy.main.Leaf_Classification", modelfilepath);
	
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
	// set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Leaf_Classification_Custom.class;
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

        //run the plugin
        IJ.runPlugIn(clazz.getName(), "");

    }

}
