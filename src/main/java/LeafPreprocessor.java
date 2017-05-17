import java.awt.Point;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class LeafPreprocessor {

    public static ByteProcessor convertToGray(ImageProcessor ip) {
        // convert to gray scale using blue channel
        ColorProcessor cp = (ColorProcessor) ip;
        cp.setRGBWeights( 0, 0, 1 );    
        ByteProcessor bp_gray = cp.convertToByteProcessor();
        //ImagePlus imp_gray = new ImagePlus(imp.getShortTitle() + " (grayscale)", bp_gray);
        //return imp_gray;
        return bp_gray;
    }

    public static ByteProcessor convertToBinary(ImageProcessor bp_gray) {
        // TODO: Check ob grau oder RGB und convertToGray ggf. aufrufen
        int th = bp_gray.getAutoThreshold();
        IJ.log( "Creating binary image... Threshold: " + th );
        ByteProcessor bp_bin = (ByteProcessor) bp_gray.duplicate();
        bp_bin.threshold( th );     // background = white (needed for erode/dilate)      -> IsoData algorithm

        //bp_bin.setBackgroundValue( 255 );   // used for rotate and scale
        //bp_bin.setAutoThreshold(bp.ISODATA, bp.OVER_UNDER_LUT );

        //ImagePlus imp_bin = new ImagePlus(imp.getShortTitle() + " (binarized)", bp_bin);
        return bp_bin;
    }

    public static void smoothBinary(ImagePlus imp_bin){
        IJ.log("Apply morphologic filter: Close");
        ByteProcessor bp_bin = (ByteProcessor) imp_bin.getProcessor();
        bp_bin.dilate();
        bp_bin.erode();
        // fill holes
        imp_bin.updateAndDraw();
        IJ.run(imp_bin, "Fill Holes", "");

        IJ.setBackgroundColor( 255, 255, 255 );

        // clear pixels outside leaf
        Roi roi_leaf = getLeafRoi(imp_bin);
        bp_bin.fillOutside( roi_leaf );
        imp_bin.updateAndDraw();
    }

    public static PolygonRoi getLeafRoi(ImagePlus imp_bin){
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
        Point stp = new Point((int)rt_temp.getValueAsDouble(rt_temp.getColumnIndex("XStart"), maxrow), 
                              (int)rt_temp.getValueAsDouble(rt_temp.getColumnIndex("YStart"), maxrow));
        IJ.doWand(stp.x, stp.y);
        Roi r = imp_bin.getRoi();
        if(r instanceof PolygonRoi){
            return (PolygonRoi) r;
        } else {
            return null;    //TODO
        }
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
