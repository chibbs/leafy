import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Selection;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import jnmaloof.leafj.leaf;

public class LeafAnalyzer {

    public void analyze(Leaf leaf) {
        //Roi roi_leaf = imp.getRoi();
	ImagePlus imp = leaf.getImg();
        WindowManager.setTempCurrentImage(imp);
        /*RoiManager rm = RoiManager.getInstance();
        if (rm == null) 
            rm = new RoiManager();*/
        IJ.run("Interpolate", "interval=1 smooth");     // needed for moments calculation

        ResultsTable rt_temp = new ResultsTable();
        rt_temp.showRowNumbers( false );
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

        double area, convexlength, convexity, perim, circ, round, solid, majoraxis, minoraxis, skew, kurt, elliptic, ellipsarea = 0;

            circ = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Circ."), 0);
            round = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Round"), 0);
            solid = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Solidity"), 0);
            skew = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Skew"), 0);
            kurt = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Kurt"), 0);

            majoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), 0);
            minoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("MinFeret"), 0); 
            perim = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Perim."), 0);
            area = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Area"), 0);
            ellipsarea = Math.PI * majoraxis * minoraxis / 4;   // durch 4 teilen, weil nur die Hälfte der Achsen benötigt wird (Radius statt Durchmesser)
            elliptic = area / ellipsarea;
            
            // TODO:  Rauhigkeit = Umfang / Umfang der konv. Hülle
            Roi roi = imp.getRoi();
            Polygon hull = roi.getConvexHull();
            PolygonRoi roi_hull = new PolygonRoi(hull, Roi.TRACED_ROI);
            imp.killRoi();
            imp.setRoi( roi_hull, true );
            //imp.show();
            rt_temp.reset();
            an = new Analyzer(imp, Measurements.PERIMETER + Measurements.LABELS, rt_temp);
            an.measure();
            imp.killRoi();
            imp.setRoi(roi);
            counter = rt_temp.getCounter();
            if (rt_temp.getCounter()==0) {
                //TODO:no results, handle that error here
            }
            convexlength = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Perim."), 0);
            convexity =  perim / convexlength;

            rt.incrementCounter();
            rt.addValue( "Label", rt_temp.getLabel( 0 ) );
            if (leaf.getLeafclass() != "") rt.addValue( "Class", leaf.getLeafclass() );
            //rt.addValue( "Elongation", elong );
            rt.addValue( "Circularity", circ );
            rt.addValue( "Roundness", round );
            rt.addValue( "Solidity", solid );
            rt.addValue( "Convexity", convexity );
            rt.addValue( "Skewness", skew );
            rt.addValue( "Kurtosis", kurt );
            rt.addValue( "Elliptic", elliptic );
            rt.show( "Results" );
            // TODO: add moments
        
            WindowManager.setTempCurrentImage(null);

    }

    public void calcCCD(Leaf leaf) {
        // calculate ccd
	Roi roi_leaf = leaf.getContour();
        double dist;
        ArrayList<Double> ccd = new ArrayList<Double>();
        //Polygon t3 = roi_leaf.getPolygon( );
        Polygon t3 = roi_leaf.getPolygon();
        int pointcount = t3.npoints;
        double[] x = new double[t3.npoints];
        double[] y = new double[t3.npoints];
        double maxdist = 0;
        double mindist = Double.MAX_VALUE;
        double meandist = 0;
        double vardist = 0;
        double stdevdist = 0;
        double har = 0, har2 = 0;
        Point maxpoint = null;
        Point a;
        Point centerpoint = new Point((int)roi_leaf.getContourCentroid()[0], (int)roi_leaf.getContourCentroid()[1]);

        for (int i=0;i<pointcount;i++) {
            a = new Point(t3.xpoints[i], t3.ypoints[i]) ;
            dist = Math.sqrt( Math.pow(a.getX() - centerpoint.getX(), 2) + Math.pow( a.getY() - centerpoint.getY(), 2 ) );
            ccd.add( dist );
            x[i] = i;
            y[i] = dist;
            if (dist > maxdist) {
                maxdist = dist;
                maxpoint = a;
            }
            mindist = dist < mindist ? dist : mindist;
            meandist += dist;
        }
        meandist /= (double) pointcount;
        // Varianz berechnen -> TODO: gleich oben mitberechnen
        for (int i = 0; i < y.length; i++) {
            double temp = y[i] - meandist;
            double temp2 =  Math.pow(y[i] - meandist, 2);
            vardist = vardist + Math.pow(y[i] - meandist, 2);
        }
        vardist /= (double) pointcount;
        stdevdist = Math.sqrt( vardist );
        har = meandist / stdevdist;
        har2 = stdevdist / meandist;
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.addValue( "Haralick", har );
        rt.addValue( "Haralick2", har2 );
        rt.show( "Results" );

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
        ImageProcessor ipp = plot.getProcessor();
        plot.show();
        ImagePlus impp = new ImagePlus("Plot", ipp);
        WindowManager.setTempCurrentImage(impp);
        IJ.save( "C:/Users/Laura/Desktop/test.png" );
        
        // Histogram with bins
        int bins = 360;
        int b;
        int values = y.length;
        double[] H = new double[bins];
        double[] Hx = Arrays.copyOfRange(x, 0, bins);
        double normval;
        double maxcount = 0;
        // Histogramm with Binning
        for (int i = 0; i < values; i++) {
            normval = (y[i] - mindist) / (maxdist - mindist);
            //b = (int) Math.floor( bins * normval / values );
            //b = (int) Math.floor( y[i] * (double)bins / values );
            b = (int) Math.floor( normval * (bins - 1) );
            H[b]++;
            maxcount = H[b] > maxcount ? H[b] : maxcount;
        }
        for (int i = 0; i < H.length; i++) {
            H[i] = H[i] / maxcount * 100;
        }
        
        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot2 = new Plot("CCD Histogram","Distance","Count in %",Hx,H);
        plot2.setLimits(0,bins, 0, 100);
        plot2.setLineWidth(2);

        plot2.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot2.setColor(Color.blue);
        plot2.show();

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