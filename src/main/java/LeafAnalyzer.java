import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PolygonRoi;
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

    public void analyze(Leaf leaf) {
	ResultsTable rt_temp = new ResultsTable();
	double area, convexlength, perim, majoraxis, minoraxis, ellipsarea;

	ImagePlus imp = leaf.getImg();
	WindowManager.setTempCurrentImage(imp);

	IJ.run("Interpolate", "interval=1 smooth");     // needed for moments calculation

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
	Analyzer.setPrecision( 3 );
	an.measure();

	int counter = rt_temp.getCounter();  //number of results
	if (counter==0) {
	    //TODO:no results, handle that error here
	} else if (counter > 1) {
	    // TODO
	}

	leaf.setCircularity(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Circ."), 0));
	leaf.setRoundness(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Round"), 0));
	leaf.setSolidity(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Solidity"), 0));
	leaf.setSkewness(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Skew"), 0));
	leaf.setKurtosis(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Kurt"), 0));

	majoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), 0);
	minoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("MinFeret"), 0); 
	perim = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Perim."), 0);
	area = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Area"), 0);
	ellipsarea = Math.PI * majoraxis * minoraxis / 4;   // durch 4 teilen, weil nur die Hälfte der Achsen benötigt wird (Radius statt Durchmesser)
	leaf.setElliptic(area / ellipsarea);
	leaf.setMaxcaliper(majoraxis);
	leaf.setMincaliper(minoraxis);
	leaf.setArea(area);
	leaf.setPerimeter(perim);
	
	// use centroid as center point (average x and y of all pixels in region (center of mass requires grayscale pic)
	leaf.setCentroidX(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("X"), 0));
	leaf.setCentroidY(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Y"), 0));


	// Rauhigkeit = Umfang / Umfang der konv. Hülle
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
	leaf.setConvexity(perim / convexlength);


	// TODO: add moments

	WindowManager.setTempCurrentImage(null);

    }

    public void calcCCD(Leaf leaf) {
        // calculate ccd
	double maxdist = 0;
        double meandist = 0; 
        double nmeandist = 0;
        double vardist = 0;
        double nvardist = 0;
        double stdevdist, nstdevdist, har, har2;
	Roi roi_leaf = leaf.getContour();
	int pointcount = 0;
        double dist, normdist;
        ArrayList<Double> ccd = new ArrayList<Double>();
        ArrayList<Double> normccd = new ArrayList<Double>();
        int contourpoints = roi_leaf.getPolygon().npoints;
        Polygon polygon = roi_leaf.getPolygon();
        for (int i = 0; i < contourpoints; i++) {
        //for (Point p : roi_leaf) {
            Point p = new Point(polygon.xpoints[i], polygon.ypoints[i]) ;
            dist = Math.sqrt( Math.pow(p.getX() - leaf.getCentroidX(), 2) + Math.pow( p.getY() - leaf.getCentroidY(), 2 ) );
            ccd.add( dist );
            maxdist = dist > maxdist ? dist : maxdist;
            meandist += dist;
            pointcount++;	// = regionpoints
        }
        meandist /= (double) pointcount;
        // Varianz berechnen -> TODO: gleich oben mitberechnen
        for (double d : ccd) {
            vardist = vardist + Math.pow(d - meandist, 2);
            normdist = d / maxdist;
            normccd.add(normdist);
            nmeandist += normdist;
        }
        vardist /= (double) pointcount;
        stdevdist = Math.sqrt( vardist );
        nmeandist /= (double) pointcount;
        
        for (double nd : normccd) {
            nvardist += Math.pow(nd - nmeandist, 2);
        }
        nvardist /= (double) pointcount;
        nstdevdist = Math.sqrt( nvardist );
        
        har = meandist / stdevdist;
        har2 = stdevdist / meandist;
        
        RadialDistances rd = new RadialDistances(ccd, maxdist, meandist, vardist, stdevdist, normccd, nmeandist, nstdevdist);
        leaf.setCcd(rd);
        leaf.setHaralick1(har);
        leaf.setHaralick2(har2);
        
        
        /*
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
	*/
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

    public void fillResultsTable(Leaf leaf) {
    	ResultsTable rt = ResultsTable.getResultsTable();
    	rt.showRowNumbers( false );
    	rt.incrementCounter();
        rt.addValue( "Label", leaf.getTitle() );
        rt.addValue( "Circularity", leaf.getCircularity() );
        rt.addValue( "Roundness", leaf.getRoundness() );
        rt.addValue( "Solidity", leaf.getSolidity() );
        rt.addValue( "Convexity", leaf.getConvexity() );
        rt.addValue( "Skewness", leaf.getSkewness() );
        rt.addValue( "Kurtosis", leaf.getKurtosis() );
        rt.addValue( "Elliptic", leaf.getElliptic() );
        //results.addValue( "Haralick1", leaf.getHaralick1() );
        rt.addValue( "Haralick2", leaf.getHaralick2());
        rt.addValue( "nMeanDist", leaf.getCcd().getNormMean());
        rt.addValue( "nDistSD", leaf.getCcd().getNormSdev());
        if (leaf.getLeafclass() != "") rt.addValue( "Class", leaf.getLeafclass() );

    }
}