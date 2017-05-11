
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.pub.regions.Contour;
import imagingbook.pub.regions.RegionContourLabeling;
import imagingbook.pub.regions.RegionLabeling.BinaryRegion;

public class Leaf_Classification implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    
    @Override
    public int setup( String arg, ImagePlus imp )
    {
        this.imp = imp;
        return DOES_RGB; // this plugin accepts rgb images
    }

    @Override
    public void run(ImageProcessor ip) {
        
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );
        ByteProcessor bp = cp.convertToByteProcessor();
        
        int th = bp.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
        bp.invert();
        ImagePlus vorf = new ImagePlus("Vorverarbeitung (inverted)", bp);
        bp.threshold( th );
        
        
        vorf = new ImagePlus("Vorverarbeitung (binarized)", bp);
        
        /*
        IJ.log("Apply morphologic filter: Close");
        bp.dilate();
        bp.erode();
        bp.dilate();*/
        
        vorf = new ImagePlus("Vorverarbeitung (closed)", bp);
        vorf.show();
        
        // Create the region labeler / contour tracer:
        IJ.log( "Searching for leaf region..." );
        RegionContourLabeling segmenter = new RegionContourLabeling(bp);
        
        // get leaf region (should be the biggest region, so first in list)
        BinaryRegion leaf = segmenter.getRegions( true ).get( 0 );
        IJ.log( "Leaf: " + leaf.toString() );
        
        Rectangle bb = leaf.getBoundingBox();
        
        bp.setRoi( bb );
        ImageProcessor ip3 = bp.crop();
        ImagePlus test3 = new ImagePlus("Test 3", ip3);
        test3.show();
        
        // Display the contours
        Contour oc = leaf.getOuterContour();
        Overlay test = new Overlay();
        BasicStroke stroke = new BasicStroke(1.0f);
        
        Shape s = oc.getPolygonPath();
        Roi roi = new ShapeRoi(s);
        roi.setName( "Leaf" );
        roi.setStrokeColor(Color.red);
        roi.setStroke(stroke);
        test.add(roi);
        
        Roi bbroi = new Roi(bb);
        bbroi.setName( "Bounding Box" );
        bbroi.setStrokeColor(Color.green);
        bbroi.setStroke(stroke);
        test.add(bbroi);
        
        imp.hide();
        imp.setOverlay(test);
        imp.show();
        
        bp.setRoi( bbroi );
        ip = bp.crop();
        
    }
    

    

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Leaf_Classification.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("https://images.blogthings.com/theautumnleaftest/leaf-1.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
