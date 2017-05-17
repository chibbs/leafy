import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;

public class Batch_Analyzer {
    
    public static void main(String[] args) {
        String groundTruth = "";
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Leaf_Classification.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        /*
        // open sample
        //ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/populus_tremula/Populus_tremula_20_MEW2014.png");
        ImagePlus image = IJ.openImage("C:/Users/Laura/Dropbox/BA/Bilddatenbank/Laura/acer_platanoides/Acer_platanoides_3_MEW2014.png");
        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");*/
        
        // process folder
        String dir1 = IJ.getDirectory("Select source folder...");
        if (dir1==null) return;
        //System.out.println( dir1 );
        String[] list = new File(dir1).list();
        if (list==null) return;
        for (int i=0; i<list.length; i++) {
            IJ.showProgress(i, list.length);
            IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
            IJ.showStatus(i+"/"+list.length);
            boolean isDir = (new File(dir1+list[i])).isDirectory();
            if (!isDir && !list[i].startsWith(".")) {
                String filetype = list[i].split( "\\.(?=[^\\.]+$)" )[1];
                /*if (filetype in ["png", "jpg", "gif", "tif"]) {
                    
                }*/
                ImagePlus img = IJ.openImage(dir1+list[i]);
                if (img==null) continue;
                
                //cls = (new File(dir1+list[i])).getParentFile().getName();
                if (list[i].contains( "_")) {
                    groundTruth = list[i].split( "_" )[0] + " " + list[i].split( "_" )[1];
                } else {
                    groundTruth = "";
                }
                
                WindowManager.setTempCurrentImage(img);     // needed because image is not shown (no images open)

                // run the plugin
                img.show();
                IJ.runPlugIn("Leaf_Classification", groundTruth);
                //img.hide();
                //IJ.saveAs(format, dir2+list[i]);
                
            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");
        
        close_windows();
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.save( dir1 + "weka.csv" );
        IJ.run("Quit");
        System.exit( 0 );
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
