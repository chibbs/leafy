import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import weka.core.Instances;

public class Batch_Analyzer implements PlugIn {

    @Override
    public void run( String arg ) {
        String groundTruth = "";
        // process folder
        String dir1 = IJ.getDirectory("Select source folder...");
        if (dir1==null) return;
        String[] list = new File(dir1).list();
        if (list==null) return;
        for (int i=0; i<list.length; i++) {
            IJ.showProgress(i, list.length);
            IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
            IJ.showStatus(i+"/"+list.length);
            boolean isDir = (new File(dir1+list[i])).isDirectory();
            if (!isDir && !list[i].startsWith(".")) {
                ImagePlus img = IJ.openImage(dir1+list[i]);
                if (img==null) continue;

                if (list[i].contains( "_")) {
                    groundTruth = list[i].split( "_" )[0] + " " + list[i].split( "_" )[1];
                } else {
                    groundTruth = "";
                }

                 WindowManager.setTempCurrentImage(img);     // needed because image is not shown (no images open)

                // run the plugin
                //IJ.runPlugIn("Leaf_Classification", groundTruth);
                //IJ.saveAs(format, dir2+list[i]);
                ImagePlus imp_bin = LeafPreprocessor.preprocess(img);

        	Roi roi_leaf = imp_bin.getRoi();
        	img.setRoi(roi_leaf, true);
        	
        	Leaf currentleaf = new Leaf(img, img.getShortTitle(), groundTruth, roi_leaf, imp_bin);

        	LeafAnalyzer la = new LeafAnalyzer();	// TODO: Options übergeben
        	la.analyze(currentleaf);
        	la.calcCCD(currentleaf);
        	la.fillResultsTable(currentleaf);

            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");

        close_windows();
        String dir2 = dir1 + "leaf.csv";
        String dir3 = dir1 + "leaf.arff";
        //LeafResultsTable rt = (LeafResultsTable) ResultsTable.getResultsTable();
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.save( dir2);
        //rt.saveAsArff(dir1 + "leaf.arff");
        LeafClassifier lc = new LeafClassifier();
        Instances inst = lc.buildInstances(rt);
     // save weka data
        
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(dir3));
	    writer.write(inst.toString());
	    writer.newLine();
	        writer.flush();
	        writer.close();
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
        
        try {
	    lc.train(dir2);
	    //lc.buildInstances(rt);
	    //lc.predictSingle(dir1 + "predict.csv");
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void main(String[] args) {

        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Batch_Analyzer.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        //String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        //System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        //run the plugin
        IJ.runPlugIn(clazz.getName(), "");


        IJ.run("Quit");
        //System.exit( 0 );
    }

    public static void close_windows() {
        //http://imagej.1557.x6.nabble.com/Re-Plugin-Command-To-Close-Window-Without-quot-Save-Changes-quot-Dialog-td3683293.html
        ImagePlus img;
        while (null != WindowManager.getCurrentImage()) {
            img = WindowManager.getCurrentImage();
            img.changes = false;
            img.close();
        }
    }
}
