import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
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
    private String groundTruth;

    public LeafAnalyzer( Roi roi_leaf, String cls )
    {
        this.roi_leaf = roi_leaf;
        this.contour = roi_leaf.getPolygon();
        this.groundTruth = cls;
    }

    public void analyze(ImagePlus imp) {
        //Roi roi_leaf = imp.getRoi();
        //WindowManager.setTempCurrentImage(imp);
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) 
            rm = new RoiManager();
        IJ.run("Interpolate", "interval=3 smooth");     // needed for moments calculation

        ResultsTable rt_temp = new ResultsTable();
        ResultsTable rt = ResultsTable.getResultsTable();

        /*// same as:
        IJ.run("Set Measurements...", "area centroid center perimeter bounding fit shape feret's skewness kurtosis display add redirect=None decimal=3");
        Analyzer.setResultsTable( rt2 );
        Analyzer.setPrecision( 3 );
        IJ.run("Measure");*/

        // explanations: see https://imagej.nih.gov/ij/docs/guide/146-30.html
        int measurements = Measurements.AREA +          // Area of selection in square pixels -> heading Area
                        Measurements.CENTROID +         // center point of selection (average of x and y coordinates of all pixels) -> X and Y
                        Measurements.CENTER_OF_MASS +    // brightness-weighted average of x and y coordinates (first order spatial moments) -> XM and YM
                        Measurements.PERIMETER +         // length of the outside boundary of selection -> Perim.
                        Measurements.RECT +              // smallest rectangle enclosing the selection -> BX, BY, Width and Height
                        Measurements.ELLIPSE +           // Fits an ellipse to selection -> Major, Minor and Angle (center = centroid, angle to x-axis)
                        Measurements.SHAPE_DESCRIPTORS + // shape descriptors: Circularity (Circ.), Aspect ratio (AR), Roundness (Round.), Solidity
                        Measurements.FERET +             // Feret’s diameter: longest distance between any two points along the selection boundary (maximum caliper) -> Feret, FeretAngle, MinFeret, FeretX and FeretY
                        Measurements.SKEWNESS +          // third order moment about the mean -> Skew.
                        Measurements.KURTOSIS +          // fourth order moment about the mean -> Kurt. 
                        Measurements.ADD_TO_OVERLAY +    // measured ROIs are automatically added to the image overlay
                        Measurements.LABELS;             // image name and selection label are recorded in the first column of the Results Table
        Analyzer an = new Analyzer(imp, measurements, rt_temp);
        //Analyzer an = new Analyzer(imp, measurements, rt);
        Analyzer.setPrecision( 3 );
        an.measure();

        int counter = rt_temp.getCounter();  //number of results
        if (counter==0) {
            //TODO:no results, handle that error here
        } else if (counter > 1) {
            // TODO
        }

        double area, circ, round, solid, majoraxis, minoraxis, skew, kurt, elliptic, ellipsarea = 0;

        for (int row=0; row<counter; row++) {
            circ = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Circ."), row);
            round = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Round"), row);
            solid = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Solidity"), row);
            skew = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Skew"), row);
            kurt = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Kurt"), row);

            majoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), row);
            minoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("MinFeret"), row);           
            area = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Area"), row);
            ellipsarea = Math.PI * majoraxis * minoraxis / 4;   // durch 4 teilen, weil nur die Hälfte der Achsen benötigt wird (Radius statt Durchmesser)
            elliptic = area / ellipsarea;
            
            // TODO:  Rauhigkeit = Umfang / Umfang der konv. Hülle

            rt.incrementCounter();
            rt.addValue( "Label", rt_temp.getLabel( row ) );
            if (this.groundTruth != "") rt.addValue( "Class", this.groundTruth );
            //rt.addValue( "Elongation", elong );
            rt.addValue( "Circularity", circ );
            rt.addValue( "Solidity", solid );
            rt.addValue( "Roundness", round );
            rt.addValue( "Skewness", skew );
            rt.addValue( "Kurtosis", kurt );
            rt.addValue( "Elliptic", elliptic );

            // TODO: add moments
        }


    }

    public void calcCCD() {
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
            if (dist > maxdist) {
                maxdist = dist;
                maxpoint = a;
            }
        }

        if (maxpoint != null) {
            Roi roi_mp = new Roi(new Rectangle((int)maxpoint.getX()-1, (int)maxpoint.getY()-1, 3, 3));
            roi_mp.setName( "Maxpoint" );
            //rm.add( imp_gray, roi_mp, 6 );
        }

        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot("Contour Distances","Contour Point","Distance",x,y);
        plot.setLimits(0,t3.npoints, 0, maxdist);
        plot.setLineWidth(2);

        plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot.setColor(Color.blue);
        plot.show();

    }

    public void findPetiole(ImagePlus imp_gray) {
        // copied from LeafJ
        IJ.log( "start find petiole" );
        ImageProcessor tip = imp_gray.getProcessor();

        tip.setAutoThreshold(ImageProcessor.ISODATA, ImageProcessor.OVER_UNDER_LUT );
        //tip.setAutoThreshold("Moments", false, ImageProcessor.OVER_UNDER_LUT );

        Calibration cal = imp_gray.getCalibration();
        ResultsTable rt_tmp = new ResultsTable();
        ResultsTable rt = ResultsTable.getResultsTable();
        RoiManager rm = RoiManager.getInstance();

        double minParticleSize = 4000;
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE
                                                   +ParticleAnalyzer.SHOW_RESULTS
                                                   //+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
                                                   ,Measurements.RECT+Measurements.ELLIPSE, rt_tmp, minParticleSize, Double.POSITIVE_INFINITY,0,1);
        pa.analyze(imp_gray,tip);   // TODO: nur Blatt messen, nicht alle Objekte im Bild


        leaf leafCurrent = new leaf();
        leafCurrent.setLeaf( rt_tmp, 0, cal ); //set initial attributes // TODO: nur aktuelle Ergebnisse hinzufügen
        leafCurrent.scanLeaf(tip);          //do a scan across the length to determine widths
        leafCurrent.findPetiole(tip);           //

        //timp.updateAndDraw();
        IJ.log("end find Petiole");
        leafCurrent.addPetioleToManager(imp_gray, tip, rm, 4);
        leafCurrent.addBladeToManager(imp_gray, tip, rm, 5);

        /*if (sd.saveRois) rm.runCommand("Save", imp.getShortTitle() + sd.getTruncatedDescription() + "_roi.zip");
        results.setHeadings(sd.getFieldNames());
        results.show();
        results.addResults(sd,rm,tip,timp);*/

        ResultsTable rt_temp = new ResultsTable();
        Analyzer petioleAnalyzer = new Analyzer(imp_gray,  Measurements.PERIMETER , rt_temp);
        petioleAnalyzer.measure();
        rt_temp.getValue("Perim.",rt_temp.getCounter()-1);
        rt.addValue( "Petiole Length", rt_temp.getValue("Perim.",rt_temp.getCounter()-1) );
        //rt.addValue( "Class", cls );

        rt.show( "Results" );

        //timp.close();
    }
}