import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import jnmaloof.leafj.leaf;

public class LeafAnalyzer {
    private Polygon contour;
    private Roi roi_leaf;
    
    public LeafAnalyzer( Roi roi_leaf )
    {
        this.roi_leaf = roi_leaf;
        this.contour = roi_leaf.getPolygon();
    }

    public void analyze(ImagePlus imp) {
        //Roi roi_leaf = imp.getRoi();
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) 
            rm = new RoiManager();
        
      //IJ.run("Measure");
        IJ.runPlugIn("Measure", "ij.plugin.filter.Analyzer", "");
        
        // find petiole
        //findPetiole(imp_gray);
        findPetiole(imp);
        
        // calculate ccd
        double dist;
        ArrayList<Double> ccd = new ArrayList<Double>();
        //Polygon t3 = roi_leaf.getPolygon( );
        Polygon t3 = this.contour;
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
    }
    
    public static void findPetiole(ImagePlus timp) {
        // copied from LeafJ
        IJ.log( "start find petiole" );
        ImageProcessor tip = timp.getProcessor();
        
        tip.setAutoThreshold(ImageProcessor.ISODATA, ImageProcessor.OVER_UNDER_LUT );
        //tip.setAutoThreshold("Moments", false, ImageProcessor.OVER_UNDER_LUT );

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
        Analyzer petioleAnalyzer = new Analyzer(timp,  Measurements.PERIMETER , rt_temp);
        petioleAnalyzer.measure();
        rt_temp.getValue("Perim.",rt_temp.getCounter()-1);
        rt.addValue( "Petiole Length", rt_temp.getValue("Perim.",rt_temp.getCounter()-1) );
        //rt.addValue( "Class", cls );
        
        rt.show( "Results" );
        
        //timp.close();
        
        
    }

}
