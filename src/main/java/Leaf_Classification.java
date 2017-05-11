
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.List;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import imagingbook.pub.regions.Contour;
import imagingbook.pub.regions.ContourOverlay;
import imagingbook.pub.regions.RegionContourLabeling;
import imagingbook.pub.regions.RegionLabeling.BinaryRegion;
import imagingbook.pub.threshold.global.OtsuThresholder;

public class Leaf_Classification implements PlugInFilter {

    static boolean showContours = false;
    ImagePlus imp;
    
    @Override
    public int setup( String arg, ImagePlus imp )
    {
        this.imp = imp;
        return DOES_RGB; // this plugin accepts rgb/8-bit-grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {
        
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );
        ByteProcessor bp = cp.convertToByteProcessor();
        //(new ImagePlus(imp.getShortTitle() + " (gray)", bp)).show();
        
        /*bp.autoThreshold();
        bp.invert();*/
        
        int th = bp.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
        bp.invert();
        ImagePlus vorf = new ImagePlus("Vorverarbeitung (inverted)", bp);
        bp.threshold( th );
        
        
        vorf = new ImagePlus("Vorverarbeitung (binarized)", bp);
        //vorf.show();
        
        /*
        IJ.log("Apply morphologic filter: Close");
        bp.dilate();
        bp.erode();
        bp.dilate();*/
        
        vorf = new ImagePlus("Vorverarbeitung (closed)", bp);
        vorf.show();
        
        /*OtsuThresholder thr = new OtsuThresholder();
        int q = thr.getThreshold(bp);
        if (q >= 0) {
            IJ.log("threshold = " + q);
            bp.threshold(q);
        }
        else {
            IJ.showMessage("no threshold found");
        }  */     
        
        // Make sure we have a proper byte image:
        //ByteProcessor bp = (ByteProcessor) ip.convertToByteProcessor();
        
        // Create the region labeler / contour tracer:
        IJ.log( "Searching for leaf region..." );
        RegionContourLabeling segmenter = new RegionContourLabeling(bp);
        
        // Get the list of detected regions (sort by size):
        /*List<BinaryRegion> regions = segmenter.getRegions(true);
        if (regions.isEmpty()) {
            IJ.error("No regions detected!");
            return;
        }

        if (listRegions) {
            IJ.log("Detected regions: " + regions.size());
            for (BinaryRegion r: regions) {
                IJ.log(r.toString());
            }
        }
        
        if (listContourPoints) {
            // Get the outer contour of the largest region:
            BinaryRegion largestRegion = regions.get(0);
            Contour oc =  largestRegion.getOuterContour();
            IJ.log("Points along outer contour of largest region:");
            Point2D[] points = oc.getPointArray();
            for (int i = 0; i < points.length; i++) {
                Point2D p = points[i];
                IJ.log("Point " + i + ": " + p.toString());
            }
            
            // Get all inner contours of the largest region:
            List<Contour> ics = largestRegion.getInnerContours();
            IJ.log("Inner regions (holes): " + ics.size());
        }
        */
        
        // get leaf region
        BinaryRegion leaf = segmenter.getRegions( true ).get( 0 );
        IJ.log( "Leaf: " + leaf.toString() );
        
        Rectangle bb = leaf.getBoundingBox();
        
        bp.setRoi( bb );
        ImageProcessor ip3 = bp.crop();
        ImagePlus test3 = new ImagePlus("Test 3", ip3);
        test3.show();
        
        // Display the contours
            //ImageProcessor lip = segmenter.makeLabelImage(false);
            //ImagePlus lim = new ImagePlus("Region labels and contours", lip);
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
        
            /*Overlay oly = new ContourOverlay(segmenter);
            imp.setOverlay(oly);
            imp.show();*/

        
//      BinaryRegion r = segmenter.getRegions().get(0);
//      for (java.awt.Point p : r) {
//          
//      }
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
