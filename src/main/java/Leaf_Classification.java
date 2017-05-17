
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import jnmaloof.leafj.leaf;

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
          
        ByteProcessor bp_gray = LeafPreprocessor.convertToGray(ip);
        ImagePlus imp_gray = new ImagePlus(imp.getShortTitle() + " (grayscale)", bp_gray);
        //imp_gray.show();
        
        ByteProcessor bp_bin = LeafPreprocessor.convertToBinary(bp_gray);
        //imp_gray.hide();
        ImagePlus imp_bin = new ImagePlus(imp.getShortTitle() + " (binarized)", bp_bin);
        imp_bin.show();
        
        LeafPreprocessor.smoothBinary( imp_bin );
        /*
        imp = LeafPreprocessor.cropImage(imp, imp_bin.getRoi());    // Reihenfolge!
        imp_gray = LeafPreprocessor.cropImage(imp_gray, imp_bin.getRoi());    // Reihenfolge!
        imp_bin = LeafPreprocessor.cropImage(imp_bin);
        bp_bin = (ByteProcessor) imp_bin.getProcessor();
        
        imp_bin.show();
        IJ.run( "Create Selection" );
        */
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) 
            rm = new RoiManager();
        
        Roi roi_leaf = imp_bin.getRoi();
        roi_leaf.setName( "Leaf" );
        rm.add( imp, roi_leaf, 0);
        
        IJ.run("Measure");
        
        // find petiole
        findPetiole(imp_gray);
        
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

        plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot.setColor(Color.blue);
        plot.show();
         
        
        //imp.hide();
        //rm.setVisible(false);
        //imp_bin.hide();
    }
    
    public void findPetiole(ImagePlus timp) {
        // copied from LeafJ
        IJ.log( "start find petiole" );
        ImageProcessor tip = timp.getProcessor();
        
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
        ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/acer_platanoides/Acer_platanoides_3_MEW2014.png");
        //ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/quercus_petraea/Quercus_petraea_13_MEW2014.png");
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
