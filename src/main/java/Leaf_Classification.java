
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
import ij.plugin.filter.Analyzer;
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
        return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {
        
        // convert to grey scale using blue channel
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );    
        ByteProcessor bp_gray = cp.convertToByteProcessor();
        ImagePlus imp_gray = new ImagePlus(imp.getShortTitle() + " (grayscale)", bp_gray);
        imp_gray.show();
        
        int th = bp_gray.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
        /*bp_gray.invert();
        imp_gray.hide();
        imp_gray = new ImagePlus(imp.getShortTitle() + " (inverted)", bp_gray);
        imp_gray.show();*/
        
        ByteProcessor bp_bin = cp.convertToByteProcessor();
        bp_bin.threshold( th );     // background = white       -> IsoData algorithm
        bp_bin.setBackgroundValue( 255 );   // used for rotate and scale
        //bp_bin.setAutoThreshold(bp.ISODATA, bp.OVER_UNDER_LUT );
        imp_gray.hide();
        ImagePlus imp_bin = new ImagePlus(imp.getShortTitle() + " (binarized)", bp_bin);
        imp_bin.show();
        
        IJ.log("Apply morphologic filter: Close");
        bp_bin.dilate();
        bp_bin.erode();
        // fill holes
        //imp_bin.hide();
        //imp_bin = new ImagePlus(imp.getShortTitle() + " (filtered)", bp_gray);
        imp_bin.updateAndDraw();
        IJ.run(imp_bin, "Fill Holes", "");
        //imp_bin.show();   
        
        // Create the region labeler / contour tracer:
        IJ.log( "Searching for leaf region..." );
        // invert picture (segmenter needs background to be black)
        ByteProcessor bp_bin_inv = (ByteProcessor) bp_bin.duplicate();
        bp_bin_inv.invert();
        RegionContourLabeling segmenter = new RegionContourLabeling(bp_bin_inv); // TODO: cite (see javadoc)
        
        // get leaf region (should be the biggest region, so first in list)
        BinaryRegion leaf_region = segmenter.getRegions( true ).get( 0 );
        IJ.log( "Leaf: " + leaf_region.toString() );
        
        // Display the contours
        Rectangle bb = leaf_region.getBoundingBox();
        Point2D centerpoint = leaf_region.getCenterPoint();
        Contour oc = leaf_region.getOuterContour();
        
        Shape s = oc.getPolygonPath();
        Roi roi_shp = new ShapeRoi(s);
        roi_shp.setName( "Leaf" );
        
        Roi roi_bb = new Roi(bb);
        roi_bb.setName( "Bounding Box" );

        Roi roi_cp = new Roi(new Rectangle((int)centerpoint.getX()-1, (int)centerpoint.getY()-1, 3, 3));
        roi_cp.setName( "Center" );
        
        RoiManager rm = new RoiManager();
        rm = RoiManager.getInstance();

        rm.add( imp, roi_shp, 1 );
        rm.add( imp, roi_bb, 2 );
        rm.add( imp, roi_cp, 3 );  
        
      //IJ.log( "Cropping..." );
        bp_bin.setRoi( bb );
        /*imp_bin.hide();
        imp_bin = new ImagePlus(imp_bin.getShortTitle(), bp_bin.crop());
        imp_bin.show();
        
        ip.setRoi( bb );
        bp_gray.setRoi( bb );
        //imp_bin = bp_bin.crop();
        ip = ip.crop();
        bp_gray.setRoi( bb );
        bp_gray.crop();
        imp_bin.updateAndDraw();
        imp.updateAndDraw();*/
        
        // find petiole
        findPetiole(cp.convertToByteProcessor(), imp_gray);
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
            +ParticleAnalyzer.SHOW_RESULTS
            //+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
            ,Measurements.RECT+Measurements.ELLIPSE, rt, minParticleSize, Double.POSITIVE_INFINITY,.15,1);
        pa.analyze(timp,tip);
        
        leaf leafCurrent = new leaf();
        leafCurrent.setLeaf( rt, 0, cal ); //set initial attributes
        leafCurrent.scanLeaf(tip);          //do a scan across the length to determine widths
        leafCurrent.findPetiole(tip);           //
        
        //timp.updateAndDraw();
        IJ.log("end find Petiole");
        leafCurrent.addPetioleToManager(timp, tip, rm, 4);
        leafCurrent.addBladeToManager(timp, tip, rm, 5);
        
        /*if (sd.saveRois) rm.runCommand("Save", imp.getShortTitle() + sd.getTruncatedDescription() + "_roi.zip");
        results.setHeadings(sd.getFieldNames());
        results.show();
        results.addResults(sd,rm,tip,timp);*/
        
        ResultsTable rt_temp = new ResultsTable();
        Analyzer petioleAnalyzer = new Analyzer(imp,  Measurements.PERIMETER , rt_temp);
        petioleAnalyzer.measure();
        rt_temp.getValue("Perim.",rt_temp.getCounter()-1);
        rt.addValue( "Petiole Length", rt_temp.getValue("Perim.",rt_temp.getCounter()-1) );
        
        rt.show( "Results" );
        
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
