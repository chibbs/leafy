package net.larla.leafy.common;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import jnmaloof.leafj.leaf;
import net.larla.leafy.datatypes.*;
import net.larla.leafy.helpers.FileHelper;

public class LeafAnalyzer {
    public static final int 	VERBOSEMODE = 1,
	    			FINDPETIOLE = 2,
	    			USEROIMANAGER = 4,
	    			SAVEOVERLAYIMG = 8,
	    			SAVECCD = 16;
    private int settings;
    
    public LeafAnalyzer(int op) {
	this.settings = op;
    }
    public LeafAnalyzer() {
	this.settings = 0;
    }

    public Leaf analyze(ImagePlus imp, ImagePlus mask, String groundTruth) {

	Leaf currentleaf = new Leaf(imp, imp.getShortTitle(), groundTruth, mask.getRoi(), mask);
	findRegions(currentleaf);	// save all rois to currentleaf
	if (isSet(USEROIMANAGER))
	    addRoisToManager(currentleaf);
	calculateFeatures(currentleaf);
	calcCCD(currentleaf);
	fillResultsTable(currentleaf);
	if (isSet(SAVEOVERLAYIMG)) {
	    FileInfo fi = imp.getOriginalFileInfo();
	    String imgDir = fi.directory + "res/";
	    saveOverlayImg(currentleaf, imgDir, imp);
	}
	if (isSet(SAVECCD)) {
	    FileInfo fi = imp.getOriginalFileInfo();
	    String imgDir = fi.directory + "res/";
	    saveCCDplot(currentleaf, imgDir, imp.getShortTitle()+ "_ccd.png");
	}
	return currentleaf;
    }

    public void findRegions(Leaf leaf) {
	ImagePlus imp = leaf.getImg();
	WindowManager.setTempCurrentImage(imp);

	//IJ.run("Interpolate", "interval=1 smooth");     // needed for moments calculation

	Roi roi = imp.getRoi();	
	Polygon hull = roi.getConvexHull();
	PolygonRoi roi_hull = new PolygonRoi(hull, Roi.TRACED_ROI);
	roi_hull.setName("Convex Hull");
	roi_hull.setStrokeColor(Color.MAGENTA);
	leaf.setHullroi(roi_hull);

	Roi bbroi = new Roi(roi.getBounds());
	bbroi.setName("Bounding Box");
	bbroi.setStrokeColor(Color.BLUE);
	leaf.setBbroi(bbroi);

	findLeafAxis(leaf);	// depends on contour
	if (isSet(LeafAnalyzer.FINDPETIOLE)) {
	    findPetiole(leaf);	// depends on convexhull

	    Roi bladeroi = leaf.getBladeroi();
	    if (bladeroi != null) {
		Polygon bladep = leaf.getBladeroi().getConvexHull();
		PolygonRoi bladehull = new PolygonRoi(bladep, Roi.TRACED_ROI);
		bladehull.setName("Blade Convex Hull");
		bladehull.setStrokeColor(Color.MAGENTA);
		leaf.setBladehullroi(bladehull);
	    }

	    if (leaf.getBladeroi() == null || leaf.getPetioleroi() == null) {
		IJ.log("Blade/Petiole not found!");
	    }
	}
    }

    public void addRoisToManager(Leaf leaf) {
	RoiManager rm = RoiManager.getInstance();
	if (rm == null)
	    rm = new RoiManager();
	ImagePlus imp = leaf.getImg();

	rm.add(imp, leaf.getContour(), 1);
	rm.add(imp, leaf.getBbroi(), 2);
	rm.add(imp, leaf.getHullroi(), 3);
	rm.add(imp, leaf.getLeafaxis(), 4);
	if (isSet(LeafAnalyzer.FINDPETIOLE)) {
	    if (leaf.getBladeroi() != null)
		rm.add(imp, leaf.getBladeroi(), 6);
	    if (leaf.getPetioleroi() != null)
		rm.add(imp, leaf.getPetioleroi(), 5);
	    if (leaf.getBladehullroi() != null)
		rm.add(imp, leaf.getBladehullroi(), 7);
	}
    }

    public void calculateFeatures(Leaf leaf) {
   	ResultsTable rt_temp = new ResultsTable();
   	Analyzer an;
   	Analyzer.setPrecision( 3 );
   	int counter;
   	double area, convexarea, convexperim, perim, majoraxis, minoraxis, ellipsarea, 
   		leafwidth, leafheight, angle, petiolelength, petioleratio;

   	ImagePlus imp = leaf.getImg();
   	WindowManager.setTempCurrentImage(imp);


   	// Berechnungen mit der konvexen Hülle
   	imp.setRoi( leaf.getHullroi(), true );
   	rt_temp.reset();
   	an = new Analyzer(imp, 
   		//Measurements.PERIMETER + 
   		Measurements.FERET +
   		Measurements.CENTROID +         // center point of selection (average of x and y coordinates of all pixels) -> X and Y
   		Measurements.CENTER_OF_MASS +    // brightness-weighted average of x and y coordinates (first order spatial moments) -> XM and YM
   		Measurements.LABELS, 
   		rt_temp);
   	an.measure();

   	counter = rt_temp.getCounter();
   	if (rt_temp.getCounter()==0) {
   	    //TODO:no results, handle that error here
   	}
   	leafheight = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), rt_temp.getCounter()-1);
   	//leafwidth = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("MinFeret"), rt_temp.getCounter()-1);
   	//angle = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("FeretAngle"), rt_temp.getCounter()-1);


   	// Berechnungen des Stiels
   	Roi pr = leaf.getPetioleroi();
   	if (pr != null) {
   	    imp.setRoi(pr, true);
   	    rt_temp.reset();
   	    an = new Analyzer(imp,  Measurements.PERIMETER + Measurements.LABELS,  rt_temp);
   	    an.measure();
   	    petiolelength = rt_temp.getValue("Perim.",rt_temp.getCounter()-1);
   	    petioleratio = petiolelength / leafheight;
   	    leaf.setPetioleratio(petioleratio);
   	}

   	// Berechnungen der Blattspreite
   	Roi br = leaf.getBladeroi();
   	if (br != null) {
   	    imp.setRoi(leaf.getBladeroi(), true);
   	} else {
   	    imp.setRoi(leaf.getContour(), true);
   	}
   	rt_temp.reset();
   	//IJ.run("Interpolate", "interval=1 smooth");     // needed for moments calculation
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
   	an = new Analyzer(imp, measurements, rt_temp);
   	an.measure();
   	/*// same as:
                   IJ.run("Set Measurements...", "area centroid center perimeter bounding fit shape feret's skewness kurtosis display add redirect=None decimal=3");
                   Analyzer.setResultsTable( rt_temp );
                   IJ.run("Measure");*/


   	counter = rt_temp.getCounter();  //number of results
   	if (counter==0) {
   	    //TODO:no results, handle that error here
   	    IJ.log("No results.");
   	} else if (counter > 1) {
   	    // TODO
   	}

   	majoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), rt_temp.getCounter()-1);
   	perim = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Perim."), rt_temp.getCounter()-1);
   	area = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Area"), rt_temp.getCounter()-1);
   	
   	leaf.setCircularity(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Circ."), rt_temp.getCounter()-1));	// Kompaktheit -> Kreis 0.99 im Test
   	leaf.setRoundness(4 * area / Math.PI / Math.pow(majoraxis,2));	// Rundheit
   	leaf.setSolidity(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Solidity"), rt_temp.getCounter()-1));	// Wert fast genauso wie selbst berechnet
   	// use centroid as center point (average x and y of all pixels in region (center of mass requires grayscale pic)
   	leaf.setCentroidX(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("X"), rt_temp.getCounter()-1));
   	leaf.setCentroidY(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Y"), rt_temp.getCounter()-1));
   	
   	majoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Feret"), rt_temp.getCounter()-1);
	minoraxis = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("MinFeret"), rt_temp.getCounter()-1);
	ellipsarea = Math.PI * majoraxis * minoraxis / 4;   // durch 4 teilen, weil nur die Hälfte der Achsen benötigt wird (Radius statt Durchmesser)	// TODO: Elongiertheit 1 - klein / groß
	leaf.setElliptic(area / ellipsarea);
   	
   	
   	// zu prüfen
   	//leaf.setSkewness(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Skew"), rt_temp.getCounter()-1));
   	//leaf.setKurtosis(rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Kurt"), rt_temp.getCounter()-1));
   	/*IJ.log("Skewness IJ: " + leaf.getSkewness());
   	IJ.log("Kurtosis IJ: " + leaf.getKurtosis());*/
   	
   	leaf.setArea(area);
   	leaf.setPerimeter(perim);


   	Roi bhr = leaf.getBladehullroi();
   	if (bhr != null) {
           	imp.setRoi(leaf.getBladehullroi(), true);
   	} else {
   	    imp.setRoi(leaf.getHullroi(), true);
   	}
           	rt_temp.reset();
           	an = new Analyzer(imp,  Measurements.PERIMETER + Measurements.AREA + Measurements.LABELS,  rt_temp);
           	an.measure();
           	convexperim = rt_temp.getValueAsDouble(rt_temp.getColumnIndex("Perim."), rt_temp.getCounter()-1);
           	leaf.setConvexity(convexperim / perim);
   	

   	// TODO: add moments



       }
    
    public void calcCCD(Leaf leaf) {
	// calculate ccd
	double maxdist = 0;
	double meandist = 0; 			// 1. Konturmoment = Mittelwert m1 = Radius
	double ampvar = 0;
	double skew = 0;
	double kurt = 0;
	double M3 = 0, M4 = 0, M5 = 0;
	double var2 = 0;			//2. zentrales Konturmoment = Varianz M2 = sigma^2
	double normvar2 = 0;
	double nmeandist = 0;
	Roi roi_leaf = leaf.getBladeroi();
	if (roi_leaf == null)
	    roi_leaf = leaf.getContour();
	FloatPolygon polygon = roi_leaf.getInterpolatedPolygon();	// get all points of contour (4-connected) (integers as floats)
	double[] ccd = new double[polygon.npoints - 1];			// erster und letzter Punkt sind gleich!!!
	double pointcount = 0;
	
	
	// calculate ccd and get max and mean distance
	for (int i = 0; i < ccd.length; i++) {
	    double dist = Math.sqrt( Math.pow( (polygon.xpoints[i]- leaf.getCentroidX()), 2 ) + Math.pow( (polygon.ypoints[i] - leaf.getCentroidY()), 2 ) );
	    ccd[i] = dist;
	    maxdist = dist > maxdist ? dist : maxdist;
	    meandist += dist;
	    var2 += (dist * dist);
	    if (var2 == Double.POSITIVE_INFINITY)
		throw new ArithmeticException("Overflow");
	    pointcount += 1;					// = regionpoints
	}
	meandist /= pointcount;
	var2 = var2 / pointcount - Math.pow(meandist, 2);	// Effiziente Berechnung der Varianz über Verschiebungssatz
	
	// Momente berechnen
	for (int i = 0; i < ccd.length; i++) {
	    M3 += (Math.pow(ccd[i] - meandist, 3));
	    M4 += (Math.pow(ccd[i] - meandist, 4));
	    M5 += (Math.pow(ccd[i] - meandist, 5));
	    
	    double normdist = ccd[i] / maxdist;
	    normvar2 += (normdist * normdist);
	    nmeandist += normdist;
	    
	    if (M3 == Double.POSITIVE_INFINITY || M4 == Double.POSITIVE_INFINITY || M5 == Double.POSITIVE_INFINITY)
		throw new ArithmeticException("Overflow");
	}
	nmeandist /= pointcount;
	normvar2 = (normvar2 / pointcount) - Math.pow(nmeandist, 2);
	M3 /= pointcount;
	M4 /= pointcount;
	M5 /= pointcount;
	ampvar = Math.sqrt(var2) / meandist;
	skew = M3 / Math.sqrt( Math.pow(var2, 3));
	kurt = M4 / Math.sqrt( Math.pow(var2, 4));
	
	
	leaf.setSkewness(skew);
	leaf.setKurtosis(kurt);
	leaf.setAmpvar(ampvar);
	leaf.setRadratio(nmeandist);
	leaf.setNormvar(normvar2);
	/*
	IJ.log("Momente:");
	IJ.log("Mittelwert:   " + meandist); 	// nicht skalierungsinv.
	IJ.log("Varianz:   " + var2); 		// Kreis: Wert nahe 0, aber nicht skalierungsinvariant!
	IJ.log("AmpVar:   " + ampvar);
	IJ.log("Skewness:   " + skew);
	IJ.log("Kurtosis:   " + kurt);
	IJ.log("norm.Mittelwert:   " + nmeandist);
	IJ.log("norm.Varianz:   " + normvar2); */
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
	//rt.addValue( "Haralick2", leaf.getHaralick2());
	//rt.addValue( "nMeanDist", leaf.getCcd().getNormMean());
	//rt.addValue( "nDistSD", leaf.getCcd().getNormSdev());
	if (isSet(LeafAnalyzer.FINDPETIOLE)) {
	    rt.addValue( "Petioleratio", leaf.getPetioleratio() );
	}
	//rt.addValue("nMeanDist", leaf.getRadratio());
	rt.addValue( "ampVar", leaf.getAmpvar());
	//rt.addValue( "normVar", leaf.getNormvar());



    }

    public ImagePlus getCCDplot(double[] distances, double max){
	PlotWindow.noGridLines = false; // draw grid lines
	double[] y = distances;
	double[] x = new double[y.length];
	for (int i = 0; i < y.length; i++) {
	    x[i] = 1.0 * i;
	}
	Plot plot = new Plot("Contour Distances","Contour Point","Distance",x,y);
	plot.setLimits(0,x.length, 0, max);
	plot.setLineWidth(2);
	plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
	plot.setColor(Color.blue);
	ImageProcessor ipp = plot.getProcessor();
	//plot.show();
	ImagePlus impp = new ImagePlus("Plot", ipp);
	return impp;

    }

    public boolean saveCCDplot(Leaf leaf, String dir, String filename) {
	if (FileHelper.checkDir(dir)) {
	    ImagePlus impp = getCCDplot(leaf.getCcd().getNormccd(), 1d);	// TODO: überprüfen ob CCD überhaupt noch da ist
	    FileSaver fs = new FileSaver(impp);
	    if (fs.saveAsPng(dir + "/" + filename)) {
		return true;
	    }
	}
	return false;
    }
    
    public boolean saveOverlayImg (Leaf currentleaf, String dir, ImagePlus imp) {
	// Vergleichsbild speichern
	    if (FileHelper.checkDir(dir)) {
		ImagePlus img = new ImagePlus(imp.getShortTitle() + " (Axis)", imp.getProcessor());
		if (currentleaf.getPetioleroi() != null)
		    img.setOverlay(currentleaf.getPetioleroi(), Color.CYAN, 3, null);
		img = img.flatten();
		if (currentleaf.getLeafaxis() != null)
		    img.setOverlay(currentleaf.getLeafaxis(), Color.GREEN, 3, null);
		img = img.flatten();
		if (currentleaf.getBladeroi() != null)
		    img.setOverlay(currentleaf.getBladeroi(), Color.RED, 3, null);
		FileSaver fs = new FileSaver(img.flatten());
		if (fs.saveAsJpeg(dir + "/" + img.getShortTitle() + "_axis.jpg")) {
		    return true;
		}
	    }
	    return false;
    }

    public void findLeafAxis(Leaf leaf) {
	ImagePlus imp = leaf.getMask();
	Roi roi = leaf.getContour();
	/*RoiManager rm = RoiManager.getInstance();
	if (rm == null)
	    rm = new RoiManager();*/

	imp.setRoi(roi);
	roi.setName("Leaf");
	ResultsTable rt_temp2 = new ResultsTable();
	Analyzer leafAnalyzer = new Analyzer(imp,  Measurements.FERET , rt_temp2);
	leafAnalyzer.measure();
	double leafHeight = rt_temp2.getValue("Feret",rt_temp2.getCounter()-1);
	double feretAngle = rt_temp2.getValue("FeretAngle",rt_temp2.getCounter()-1);
	double feretX = rt_temp2.getValue("FeretX",rt_temp2.getCounter()-1);
	double feretY = rt_temp2.getValue("FeretY",rt_temp2.getCounter()-1);
	double feretAngleRad = Math.toRadians(feretAngle);
	double x2 = 0;
	double y2 = 0;

	if (feretAngle <= 90) {
	    x2 = feretX + leafHeight * Math.cos(feretAngleRad);
	    y2 = feretY - leafHeight * Math.sin(feretAngleRad);
	} else {
	    x2 = feretX + leafHeight * Math.cos(Math.PI - feretAngleRad);
	    y2 = feretY + leafHeight * Math.sin(feretAngleRad);
	}
	Roi ln = new Line(feretX, feretY, x2, y2);
	ln.setName("LeafAxis");
	ln.setStrokeColor(Color.GREEN);
	leaf.setLeafaxis(ln);

	//rm.add(imp, ln, 0);
    }

    /**
     * 
     * @param leaf
     */
    public void findPetiole(Leaf leaf) {
	// copied from LeafJ
	ResultsTable rt_temp = new ResultsTable();

	ImagePlus imp = leaf.getMask();
	//imp.show();
	Calibration cal = imp.getCalibration();
	ImageProcessor tip = imp.getProcessor();
	WindowManager.setTempCurrentImage(imp);

	Roi roi = leaf.getContour();
	PolygonRoi convexhull = (PolygonRoi) leaf.getHullroi();
	if (convexhull == null) {
	    Polygon hull = roi.getConvexHull();
	    convexhull = new PolygonRoi(hull, Roi.TRACED_ROI);
	}
	imp.killRoi();
	imp.setRoi( convexhull, false );


	Analyzer an = new Analyzer(imp, 
		Measurements.RECT +
		Measurements.ELLIPSE + 
		//Measurements.FERET + 
		Measurements.LABELS, 
		rt_temp);
	an.measure();

	tip.setThreshold(0, 128, 3);	// TODO: change 3 to OVER_UNDER_LUT

	leaf leafCurrent = new leaf();
	leafCurrent.setLeaf( rt_temp, 0, cal ); //set initial attributes
	leafCurrent.scanLeaf(tip);          //do a scan across the length to determine widths
	leafCurrent.findPetiole(tip);           //

	PolygonRoi bladeRoi = leafCurrent.getBladeROI(imp, tip);
	if (bladeRoi != null)
	    bladeRoi.setStrokeColor(Color.RED);
	PolygonRoi petioleRoi = leafCurrent.getPetioleROI(imp, tip);
	if (petioleRoi != null)
	    petioleRoi.setStrokeColor(Color.YELLOW);

	leaf.setBladeroi(bladeRoi);
	leaf.setPetioleroi(petioleRoi);
	//imp.hide();
    }
    
    private boolean isSet(int op) {
	if( (this.settings & op) != 0 ){
	    return true;
	} else {
	return false;
	}
    }
}