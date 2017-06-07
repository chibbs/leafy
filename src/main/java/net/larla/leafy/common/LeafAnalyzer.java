package net.larla.leafy.common;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import jnmaloof.leafj.leaf;
import net.larla.leafy.datamodel.*;

public class LeafAnalyzer {

    public Leaf analyze(ImagePlus imp, ImagePlus mask, String groundTruth) {

	Leaf currentleaf = new Leaf(imp, imp.getShortTitle(), groundTruth, mask.getRoi(), mask);
	runAnalyzer(currentleaf);
	calcCCD(currentleaf);
	fillResultsTable(currentleaf);

	return currentleaf;
    }

    public void runAnalyzer(Leaf leaf) {
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
	
	// find petiole
	rt_temp = findPetiole(leaf, rt_temp);

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
	//int contourpoints = roi_leaf.getPolygon().npoints;
	Polygon polygon = roi_leaf.getPolygon();
	double[] ccd = new double[polygon.npoints];
	double[] normccd = new double[ccd.length];
	for (int i = 0; i < ccd.length; i++) {
	    //for (Point p : roi_leaf) {
	    Point p = new Point(polygon.xpoints[i], polygon.ypoints[i]) ;
	    dist = Math.sqrt( Math.pow(p.getX() - leaf.getCentroidX(), 2) + Math.pow( p.getY() - leaf.getCentroidY(), 2 ) );
	    ccd[i] = dist;
	    maxdist = dist > maxdist ? dist : maxdist;
	    meandist += dist;
	    pointcount++;	// = regionpoints
	}
	meandist /= (double) pointcount;
	// Varianz berechnen -> TODO: gleich oben mitberechnen
	for (int i = 0; i < ccd.length; i++) {
	    vardist = vardist + Math.pow(ccd[i] - meandist, 2);
	    normdist = ccd[i] / maxdist;
	    normccd[i] = normdist;
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

    }


    public void fillResultsTable(Leaf leaf) {
	ResultsTable rt = ResultsTable.getResultsTable();
	rt.showRowNumbers( false );
	rt.incrementCounter();
	//rt.addValue( "Label", leaf.getTitle() );
	if (leaf.getLeafclass() != "") {
	    rt.addValue( "Class", leaf.getLeafclass() );
	} else {
	    rt.addValue( "Class", "?" );
	}
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



    }

    public ImagePlus getCCDplot(double[] distances, double max){
	PlotWindow.noGridLines = false; // draw grid lines
	double[] y = distances;
	double[] x = new double[y.length];
	for (int i = 0; i < y.length; i++) {
	    x[i] = 1.0 * i;
	    i++;
	}
	Plot plot = new Plot("Contour Distances","Contour Point","Distance",x,y);
	plot.setLimits(0,y.length, 0, max);
	plot.setLineWidth(2);
	plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
	plot.setColor(Color.blue);
	ImageProcessor ipp = plot.getProcessor();
	//plot.show();
	ImagePlus impp = new ImagePlus("Plot", ipp);
	return impp;

    }

    public void saveCCDplot(Leaf leaf, String dir, String filename) {
	ImagePlus impp = getCCDplot(leaf.getCcd().getNormccd(), 100d);

	WindowManager.setTempCurrentImage(impp);
	// TODO: make directory, if not exist
	IJ.save( dir + "ccd/" + filename + "-ccd.png" );
    }

    public void findLeafAxis(Leaf leaf, String dir, String filename) {
	RadialDistances rd = leaf.getCcd();
	double maxvalue = rd.getMaxdist();
	double threshold = rd.getMeandist();
	double[] points = rd.getCcd();
	double[] nms = new double[points.length];
	int section = points.length / 6;
	int index;

	for (int i = 0; i < points.length; i++) {
	    if (points[i] > threshold) {
		for (int j = i - section; j < i + section; j++) {
		    index = j < 0 ? points.length + j : j;	// negativer Überlauf
		    index = index >= points.length ? index - points.length : index;	// positiver Überlauf
		    if (points[index] > 0 && points[index] <= points[i]) {
			points[index] = 0d;

		    }
		}
	    }
	}
	ImagePlus impp = getCCDplot(points, maxvalue);
	//WindowManager.setTempCurrentImage(impp);
	// TODO: make directory, if not exist
	//IJ.save( dir + "ccd/" + filename + "-ccdmax.png" );
	impp.show();
    }

    public ResultsTable findPetiole(Leaf leaf, ResultsTable rt_temp) {
	// copied from LeafJ
	//IJ.log( "start find petiole" );
	
	
	RoiManager rm = RoiManager.getInstance();
	if (rm == null)
	    rm = new RoiManager();
	ResultsTable rt_res = new ResultsTable();

	ImagePlus imp = leaf.getMask();
	//imp.show();
	Calibration cal = imp.getCalibration();
	ImageProcessor tip = imp.getProcessor();
	WindowManager.setTempCurrentImage(imp);
	
	

	tip.setThreshold(0, 128, 3);
	//tip.setAutoThreshold(ImageProcessor.ISODATA, ImageProcessor.OVER_UNDER_LUT );
	//tip.setAutoThreshold("Moments", false, ImageProcessor.OVER_UNDER_LUT );
	//WindowManager.setTempCurrentImage(imp);


	leaf leafCurrent = new leaf();
	leafCurrent.setLeaf( rt_temp, 0, cal ); //set initial attributes // TODO: nur aktuelle Ergebnisse hinzufügen
	leafCurrent.scanLeaf(tip);          //do a scan across the length to determine widths
	leafCurrent.findPetiole(tip);           //

	//timp.updateAndDraw();
	IJ.log("end find Petiole");
	leafCurrent.addPetioleToManager(imp, tip, rm, 4);
	leafCurrent.addBladeToManager(imp, tip, rm, 5);

	ResultsTable rt_temp1 = new ResultsTable();
	Analyzer petioleAnalyzer = new Analyzer(imp,  Measurements.PERIMETER , rt_temp1);
	petioleAnalyzer.measure();
	
	rt_temp.addValue( "Petiole Length", rt_temp1.getValue("Perim.",rt_temp1.getCounter()-1) );

	return rt_temp;

	//timp.close();
    }
}