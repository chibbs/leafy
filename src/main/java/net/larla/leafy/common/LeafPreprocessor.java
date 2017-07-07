package net.larla.leafy.common;
import java.awt.Point;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.*;

public class LeafPreprocessor {

    public static ImagePlus preprocess (ImagePlus imp) {
	int type = imp.getType();
	String title = imp.getShortTitle();
	ImagePlus imp_bin;
	ImagePlus imp_gray;
	
	if (type == 4) {	// RGB image
	    imp_gray = LeafPreprocessor.convertToGray(imp, title);
	} else {
	    imp_gray = imp;
	}
	
	imp_bin = LeafPreprocessor.convertToBinary(imp_gray, title);
	LeafPreprocessor.smoothBinary(imp_bin);
	
	//imp = LeafPreprocessor.cropImage(imp, imp_bin.getRoi()); //Reihenfolge! 
	//imp_gray = LeafPreprocessor.cropImage(imp_gray,
	//imp_bin.getRoi()); // Reihenfolge! 
	//imp_bin = LeafPreprocessor.cropImage(imp_bin); 
	//bp_bin = (ByteProcessor) imp_bin.getProcessor();
	//IJ.run( "Create Selection" );
	
	return imp_bin;
    }

    public static ImagePlus convertToGray(ImagePlus imp, String title) {
        ImageProcessor ip = imp.getProcessor();
        // convert to gray scale using blue channel
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );    
        ByteProcessor bp_gray = cp.convertToByteProcessor();
        ImagePlus imp_gray = new ImagePlus(title + " (grayscale)", bp_gray);
        return imp_gray;
    }

    public static ImagePlus convertToBinary(ImagePlus imp_gray, String title) {
        ImageProcessor bp_gray = imp_gray.getProcessor();
        
        int th = bp_gray.getAutoThreshold();
        //IJ.log( "Creating binary image... Threshold: " + th );
        ByteProcessor bp_bin = (ByteProcessor) bp_gray.duplicate();
        bp_bin.threshold( th );     // background = white (needed for erode/dilate)      -> IsoData algorithm

        //bp_bin.setBackgroundValue( 255 );   // used for rotate and scale
        //bp_bin.setAutoThreshold(bp.ISODATA, bp.OVER_UNDER_LUT );

        ImagePlus imp_bin = new ImagePlus(title + " (binarized)", bp_bin);
        return imp_bin;
    }

    public static void smoothBinary(ImagePlus imp_bin){
        WindowManager.setTempCurrentImage(imp_bin);
        //IJ.log("Apply morphologic filter: Close");
        ByteProcessor bp_bin = (ByteProcessor) imp_bin.getProcessor();
        //imp_bin.show();
        
        // fill holes
        IJ.run(imp_bin, "Fill Holes", "");
        
        bp_bin.erode();
        bp_bin.dilate();

        IJ.setBackgroundColor( 255, 255, 255 );
        //imp_bin.updateAndDraw();

        // clear pixels outside leaf
        Roi roi_leaf = getLeafRoi(imp_bin);
        
        bp_bin.fillOutside( roi_leaf );
        //imp_bin.updateAndDraw();
        imp_bin.setRoi(roi_leaf);
        //imp_bin.show();
    }

    public static PolygonRoi getLeafRoi(ImagePlus imp_bin){
	// attention: 4-neighbourhood may cause problems with wand (start point of particle may not be connected) -> problem when using morphologic closing
	// 8-neighbourhood may cause problems with blade segmentation (line between petiole and blade is too narrow, intersection may not be recognized)
	int minThresh, maxThresh;
	double maxarea = 0;
	
	// analyze particles and find biggest
	ResultsTable rt_temp = new ResultsTable();
	ParticleAnalyzer pat = new ParticleAnalyzer(
		ParticleAnalyzer.INCLUDE_HOLES+
		ParticleAnalyzer.RECORD_STARTS, 
		Measurements.AREA + Measurements.CENTROID, 
		rt_temp, 10, Double.POSITIVE_INFINITY, 0, 1);
	pat.analyze( imp_bin );

	int counter = rt_temp.getCounter();  //number of results
	if (counter==0) {
	    throw new IllegalStateException("Segmentation Error: No Regions Not Found!");
	}
	int maxrow = 0;
	if (counter > 1) {
	    int col = rt_temp.getColumnIndex("Area");

	    for (int row=0; row<counter; row++) {
		double area = rt_temp.getValueAsDouble(col, row); //all the Area values
		if (area > maxarea) {
		    maxarea = area;
		    maxrow = row;
		}
	    }
	}
	
	// use start point of biggest region to trace contour
	Point stp = new Point(
		(int)rt_temp.getValueAsDouble(
			rt_temp.getColumnIndex("XStart"), maxrow), 
		(int)rt_temp.getValueAsDouble(
			rt_temp.getColumnIndex("YStart"), maxrow));
	Roi r = imp_bin.getRoi();

	ImageProcessor ip = imp_bin.getProcessor();
	Wand w = new Wand(ip);
	int thresh = ip.getAutoThreshold();
	double bg = ip.getBackgroundValue();
	if (thresh > bg) {
	    minThresh = thresh;
	    maxThresh = 255;
	} else {
	    minThresh = 0;
	    maxThresh = thresh;
	}
	w.autoOutline(stp.x, 
		stp.y,
		minThresh,
		maxThresh,
		Wand.FOUR_CONNECTED);		// FOUR nehmen: EIGHT ist komisch, macht teilweise mehr Punkte, teilweise genauso viel wie FOUR; außerdem wird InterpolatedPolygon verwendet, um wirklich alle Pixel zu bekommen und das liefert eh ViererNachbarschaft...

	// create ROI from contour points
	if (w.npoints > 0) {
	    r = new PolygonRoi(w.xpoints,w.ypoints,w.npoints,Roi.TRACED_ROI);
	    r.setName("Leaf");
	} else {
	    // no region found
	    throw new IllegalStateException("Segmentation Error: Leaf Region Not Found!");
	}
	return (PolygonRoi) r;

    }

    public static ImagePlus cropImage(ImagePlus imp){
        imp.hide(); // wichtig, sonst funktioniert crop() nicht!
        ImagePlus imp_crop = imp.crop();
        imp_crop.setTitle( imp.getTitle() );
        imp_crop.show();
        //IJ.run( "Create Selection" );
        return imp_crop;
    }

    public static ImagePlus cropImage(ImagePlus imp, Roi roi){
        imp.setRoi( roi );
        return cropImage(imp);
    }

}
