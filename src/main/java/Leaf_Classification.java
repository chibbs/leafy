
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ParticleAnalyzer;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
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
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
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
    String cls = "";
    
    @Override
    public int setup( String arg, ImagePlus imp )
    {
        this.imp = imp;
        if (arg.length() > 0) cls = arg;
        return DOES_RGB + DOES_8G; // this plugin accepts rgb images and 8-bit grayscale images
    }

    @Override
    public void run(ImageProcessor ip) {
        
        
        
        // convert to gray scale using blue channel
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );    
        ByteProcessor bp_gray = cp.convertToByteProcessor();
        ImagePlus imp_gray = new ImagePlus(imp.getShortTitle() + " (grayscale)", bp_gray);
        imp_gray.show();
        
        int th = bp_gray.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
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
        imp_bin.updateAndDraw();
        //IJ.run(imp_bin, "Fill Holes", "");
        
        ResultsTable rt_temp = new ResultsTable();
        // https://imagej.nih.gov/ij/developer/source/ij/plugin/filter/ParticleAnalyzer.java.html
        ParticleAnalyzer pat = new ParticleAnalyzer(
                                 //ParticleAnalyzer.ADD_TO_MANAGER+
                                 //ParticleAnalyzer.SHOW_OVERLAY_OUTLINES+
                                 ParticleAnalyzer.INCLUDE_HOLES+
                                 ParticleAnalyzer.RECORD_STARTS, 
                                 Measurements.AREA, rt_temp, 10, Double.POSITIVE_INFINITY, 0, 1);
        pat.analyze( imp_bin );
        
        int counter = rt_temp.getCounter();  //number of results
        if (counter==0) {
          //TODO:no results, handle that error here
        }
        int maxrow = 0;
        if (counter > 1) {
            int col = rt_temp.getColumnIndex("Area");
            double area, maxarea = 0;
            
            for (int row=0; row<counter; row++) {
              area = rt_temp.getValueAsDouble(col, row); //all the Area values
              if (area > maxarea) {
                  maxarea = area;
                  maxrow = row;
              }
            }
        }
        Point stp = new Point((int)rt_temp.getValueAsDouble(rt_temp.getColumnIndex("XStart"), maxrow), (int)rt_temp.getValueAsDouble(rt_temp.getColumnIndex("YStart"), maxrow));
        IJ.doWand(stp.x, stp.y);
        
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) 
            rm = new RoiManager();
        Roi roi_leaf = imp_bin.getRoi();
        roi_leaf.setName( "Leaf" );
        rm.add( imp, roi_leaf, 0);
        //rm.add( imp, imp_bin.getRoi(), 0 );
        
        IJ.run("Measure");
        
        /*
        //draw
        double[] points = roi_leaf.getContourCentroid();
        Polygon t = roi_leaf.getPolygon();
        FloatPolygon t3 = roi_leaf.getInterpolatedPolygon();
        
        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot("Test","x","y",t3.ypoints,t3.xpoints);
        //Plot p = new Plot()
        plot.setLimits(0,2000, 0, 2000);
        plot.setLineWidth(2);

        plot.setColor(Color.black);
        plot.changeFont(new Font("Helvetica", Font.PLAIN, 24));
        plot.addLabel(0.15, 0.95, "This is a label");

        plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot.setColor(Color.blue);
        plot.show();
        */
        
        // find petiole
        findPetiole(cp.convertToByteProcessor(), imp_gray);
        
        // calculate ccd
        double dist;
        ArrayList<Double> ccd = new ArrayList<Double>();
        Polygon t3 = roi_leaf.getPolygon( );
        double[] x = new double[t3.npoints];
        double[] y = new double[t3.npoints];
        double maxdist = 0;
        Point maxpoint = null;
        Point a;
        Point centerpoint = new Point((int)roi_leaf.getContourCentroid()[0], (int)roi_leaf.getContourCentroid()[1]);
        
        for (int i=0;i<t3.npoints;i++) {
           a = new Point(t3.xpoints[i], t3.ypoints[i]) ;
           dist = Math.sqrt( Math.pow(a.getX() - centerpoint.getX(), 2) + Math.pow( a.getY() - centerpoint.getY(), 2 ) );
           ccd.add( dist );
           x[i] = i;
           y[i] = dist;
           //maxdist = (dist > maxdist) ? dist : maxdist;
           if (dist > maxdist) {
               maxdist = dist;
               maxpoint = a;
           }
        }
        
        if (maxpoint != null) {
            Roi roi_mp = new Roi(new Rectangle((int)maxpoint.getX()-1, (int)maxpoint.getY()-1, 3, 3));
            roi_mp.setName( "Maxpoint" );
            rm.add( imp, roi_mp, 6 );
        }
        
        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot(imp.getShortTitle() + " Contour Distances","Contour Point","Distance",x,y);
        plot.setLimits(0,t3.npoints, 0, maxdist);
        plot.setLineWidth(2);
        
     // add label
        /*
        plot.setColor(Color.black);
        plot.changeFont(new Font("Helvetica", Font.PLAIN, 24));
        plot.addLabel(0.15, 0.95, "This is a label");*/

        plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot.setColor(Color.blue);
        plot.show();
         
        
        //imp.hide();
        //rm.setVisible(false);
        //imp_bin.hide();
    }
    
    public void findPetiole(ImageProcessor tip, ImagePlus timp) {
        // copied from LeafJ
        IJ.log( "start find petiole" );
        
        tip.setAutoThreshold(ImageProcessor.ISODATA, ImageProcessor.OVER_UNDER_LUT );
        tip.setAutoThreshold("Moments", false, ImageProcessor.OVER_UNDER_LUT );

        Calibration cal = timp.getCalibration();
        ResultsTable rt_tmp = new ResultsTable();
        ResultsTable rt = ResultsTable.getResultsTable();
        RoiManager rm = RoiManager.getInstance();
        
        double minParticleSize = 4000;
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE
            +ParticleAnalyzer.SHOW_RESULTS
            //+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
            ,Measurements.RECT+Measurements.ELLIPSE, rt_tmp, minParticleSize, Double.POSITIVE_INFINITY,0,1);
        pa.analyze(timp,tip);   // TODO: nur Blatt messen, nicht alle Objekte im Bild
        
        
        leaf leafCurrent = new leaf();
        leafCurrent.setLeaf( rt_tmp, 0, cal ); //set initial attributes // TODO: nur aktuelle Ergebnisse hinzufügen
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
        rt.addValue( "Class", cls );
        
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

		
        // open sample
        //ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/populus_tremula/Populus_tremula_20_MEW2014.png");
        //ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/acer_platanoides/Acer_platanoides_3_MEW2014.png");
        ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/quercus_petraea/Quercus_petraea_13_MEW2014.png");
        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
        
        
 
    }
	
	
    
    static ImagePlus convertToGrayscale(ImagePlus img) {
        ImagePlus img2 = img.createImagePlus();
        img2.setProcessor(img.getTitle(), img.getProcessor().convertToByte(true));
        return img2;
    
	}
}
