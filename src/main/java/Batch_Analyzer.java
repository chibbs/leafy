import java.io.File;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

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
                IJ.runPlugIn("Leaf_Classification", groundTruth);
                //IJ.saveAs(format, dir2+list[i]);

            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");

        close_windows();
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.save( dir1 + "weka.csv" );
    }

    public static void main(String[] args) {

        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Batch_Analyzer.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        //run the plugin
        IJ.runPlugIn(clazz.getName(), "");


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
