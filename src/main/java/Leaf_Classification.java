
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ParticleAnalyzer;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.WaitForUserDialog;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.pub.regions.Contour;
import imagingbook.pub.regions.RegionContourLabeling;
import imagingbook.pub.regions.RegionLabeling.BinaryRegion;
import jnmaloof.leafj.LeafJ_;
import jnmaloof.leafj.LeafResults;
import jnmaloof.leafj.Sort2D;
import jnmaloof.leafj.leaf;
import jnmaloof.leafj.sampleDescription;

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
        
        // convert to grey scale using blue channel
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );    
        ByteProcessor bp = cp.convertToByteProcessor();
        ImagePlus vorf = new ImagePlus(imp.getShortTitle() + " (grayscale)", bp);
        vorf.show();
        
        int th = bp.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
        /*bp.invert();
        vorf.hide();
        vorf = new ImagePlus(imp.getShortTitle() + " (inverted)", bp);
        vorf.show();*/
        
        bp.threshold( th );     // background = white       -> IsoData algorithm
        bp.setBackgroundValue( 255 );   // used for rotate and scale
        //bp.setAutoThreshold(bp.ISODATA, bp.OVER_UNDER_LUT );
        vorf.hide();
        vorf = new ImagePlus(imp.getShortTitle() + " (binarized)", bp);
        vorf.show();
        
        IJ.log("Apply morphologic filter: Close");
        bp.dilate();
        bp.erode();
        // fill holes
        vorf.hide();
        vorf = new ImagePlus(imp.getShortTitle() + " (filtered)", bp);
        IJ.run(vorf, "Fill Holes", "");
        vorf.show();   
        
        // Create the region labeler / contour tracer:
        IJ.log( "Searching for leaf region..." );
        // invert picture (segmenter needs background to be black)
        ByteProcessor bpi = (ByteProcessor) bp.duplicate();
        bpi.invert();
        RegionContourLabeling segmenter = new RegionContourLabeling(bpi);
        
        // get leaf region (should be the biggest region, so first in list)
        BinaryRegion leaf = segmenter.getRegions( true ).get( 0 );
        IJ.log( "Leaf: " + leaf.toString() );
        
        // Display the contours
        Rectangle bb = leaf.getBoundingBox();
        Point2D centerpoint = leaf.getCenterPoint();
        Contour oc = leaf.getOuterContour();
        Overlay layer1 = new Overlay();
        BasicStroke stroke = new BasicStroke(1.0f);
        
        Shape s = oc.getPolygonPath();
        Roi roi = new ShapeRoi(s);
        roi.setName( "Leaf" );
        roi.setStrokeColor(Color.red);
        roi.setStroke(stroke);
        layer1.add(roi);
        
        Roi bbroi = new Roi(bb);
        bbroi.setName( "Bounding Box" );
        bbroi.setStrokeColor(Color.green);
        bbroi.setStroke(stroke);
        layer1.add(bbroi);
        
        Roi centerp = new Roi(new Rectangle((int)centerpoint.getX()-1, (int)centerpoint.getY()-1, 3, 3));
        centerp.setName( "Center" );
        centerp.setStrokeColor(Color.red);
        centerp.setStroke(stroke);
        layer1.add(centerp);
        
        IJ.log( "Cropping..." );
        vorf.hide();
        bp.setRoi( bb );
        //vorf = new ImagePlus(imp.getShortTitle() + " (processed)", bp.crop());
        vorf.show();
        
        imp.hide();
        imp.setOverlay(layer1);
        imp.show();
        
        RoiManager rm = new RoiManager();
        rm = RoiManager.getInstance();

        rm.add( imp, roi, 1 );
        rm.add( imp, bbroi, 2 );
        rm.add( imp, centerp, 3 );
        
        
        // find petiole
        findPetiole(cp.convertToByteProcessor(), vorf);
    }
    
    public void findPetiole(ImageProcessor tip, ImagePlus timp) {
        // copied from LeafJ
        IJ.log( "start find petiole" );
        
        tip.setAutoThreshold(ImageProcessor.ISODATA, ImageProcessor.OVER_UNDER_LUT );
        tip.setAutoThreshold("Moments", false, ImageProcessor.OVER_UNDER_LUT );

        Calibration cal = timp.getCalibration();
        ResultsTable rt = new ResultsTable();
        RoiManager rm = RoiManager.getInstance();
        
        double minParticleSize = 4000;
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE
            //+ParticleAnalyzer.SHOW_RESULTS
            //+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
            ,Measurements.RECT+Measurements.ELLIPSE, rt, minParticleSize, Double.POSITIVE_INFINITY,.15,1);
        pa.analyze(timp,tip);
        
        leaf leafCurrent = new leaf();
        leafCurrent.setLeaf( rt, 0, cal ); //set initial attributes
        leafCurrent.scanLeaf(tip);          //do a scan across the length to determine widths
        leafCurrent.findPetiole(tip);           //
        
        //timp.updateAndDraw();
        IJ.log("end find Petiole");
        leafCurrent.addPetioleToManager(timp, tip, rm, 0);
        leafCurrent.addBladeToManager(timp, tip, rm, 0);
            
        
        /*if (sd.saveRois) rm.runCommand("Save", imp.getShortTitle() + sd.getTruncatedDescription() + "_roi.zip");
        results.setHeadings(sd.getFieldNames());
        results.show();
        results.addResults(sd,rm,tip,timp);*/
        
        //timp.close();
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
		ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/populus_tremula/Populus_tremula_20_MEW2014.png");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
