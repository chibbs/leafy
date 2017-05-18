
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

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

	/*RoiManager rm = RoiManager.getInstance();
	if (rm == null)
	    rm = new RoiManager();
	if (roi_leaf != null) {
	    roi_leaf.setName("Leaf");
	    rm.add(this.imp, roi_leaf, 0);
	}*/


	LeafAnalyzer la = new LeafAnalyzer(roi_leaf, this.cls);
	la.analyze(imp_bin);
	la.calcCCD();
	// la.findPetiole( imp_gray ); // TODO: imageplus entfernen und nur mit roi messen

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
