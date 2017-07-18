import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import net.larla.leafy.common.LeafAnalyzer;
import net.larla.leafy.common.LeafClassifier;
import net.larla.leafy.common.LeafPreprocessor;
import net.larla.leafy.helpers.FileHelper;
import net.larla.leafy.helpers.WekaHelper;
import net.larla.leafy.main.Batch_Analyzer;
import net.larla.leafy.main.Leaf_Classification;


public class TimeTest {

    @Test
    public void timeMeasureSingle() {
	// TODO Auto-generated method stub
	long time;
	SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:SS");
	Date date = new Date();

	for (int n = 0; n < 100;n++) {

	    time = -System.currentTimeMillis();
	    //
	    // ganz lange operation...
	    Leaf_Classification.main(null);
	    //
	    time += System.currentTimeMillis();
	    date.setTime(time);
	    System.out.println(sdf.format(date));

	}
	Assert.assertTrue(true);
    }

    @Test
    public void timeMeasureTraining() {
	long time;
	SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:SS");
	Date date = new Date();

	for (int n = 0; n < 10;n++) {
	    time = -System.currentTimeMillis();
	    //
	    // ganz lange operation...
	    String dir = "C:\\Users\\Laura\\Documents\\Studium\\Semester_07\\Bilddatenbank\\offline\\Laura neu\\ohneStiel - Kopie\\";
	    String arffpath = dir + "_leaf.arff";
	    String dir4 = dir + "test.model";
	    LeafClassifier customClassifier = LeafClassifier.train(arffpath, null);

	    customClassifier.saveModel(dir4);

	    time += System.currentTimeMillis();
	    date.setTime(time);
	    System.out.println(sdf.format(date));
	}

	Assert.assertTrue(true);
    }

    @Test
    public void timeMeasureBatchWopet() {
	long time;
	Date date = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:SS");

	time = -System.currentTimeMillis();
	//
	// ganz lange operation...
	startBatch(0);
	ImagePlus img;
	while (null != WindowManager.getCurrentImage()) {
	    img = WindowManager.getCurrentImage();
	    img.changes = false;
	    img.close();
	}
	IJ.run("Quit");

	time += System.currentTimeMillis();
	date.setTime(time);
	System.out.println(sdf.format(date));


	Assert.assertTrue(true);
    }

    @Test
    public void timeMeasureBatchPet() {
	// TODO Auto-generated method stub
	long time;
	Date date = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:SS");

	time = -System.currentTimeMillis();
	//
	// ganz lange operation...
	startBatch(1);
	//
	time += System.currentTimeMillis();
	date.setTime(time);
	System.out.println(sdf.format(date));


	Assert.assertTrue(true);
    }

    private void startBatch(int mode){
	// set the plugins.dir property to make the plugin appear in the Plugins menu
	String trainfolder;
	Boolean findPetiole = true;
	Class<?> clazz = Batch_Analyzer.class;
	String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length() + clazz.getPackage().getName().length());

	System.setProperty("plugins.dir", pluginsDir);

	// start ImageJ
	new ImageJ();

	//run the plugin
	if (mode == 0) {
	    trainfolder = "C:\\Users\\Laura\\Documents\\Studium\\Semester_07\\Bilddatenbank\\offline\\Laura neu\\ohneStiel - Kopie\\";
	    findPetiole = false;
	} else {
	    trainfolder = "C:\\Users\\Laura\\Documents\\Studium\\Semester_07\\Bilddatenbank\\offline\\Laura neu\\Stiel - Kopie\\";
	}
	String groundTruth = "";
	// process folder

	String arffpath = trainfolder + "_leaf.arff";
	File f = new File(arffpath);
	HashMap<String,String> classmap = FileHelper.file2Map(trainfolder+"_classes.csv");
	String[] imglist = new File(trainfolder).list();
	if (imglist==null) return;
	for (int i=0; i<imglist.length; i++) {
	    IJ.showProgress(i, imglist.length);
	    //IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
	    IJ.showStatus(i+"/"+imglist.length);
	    boolean isDir = (new File(trainfolder+imglist[i])).isDirectory();
	    if (!isDir && !imglist[i].startsWith(".")) {
		IJ.log("Image: " + trainfolder+imglist[i]);
		ImagePlus img = IJ.openImage(trainfolder+imglist[i]);
		if (img==null) continue;

		if (classmap.containsKey(imglist[i])) {
		    groundTruth = classmap.get(imglist[i]);
		} else {
		    if (imglist[i].contains( "_")) {
			groundTruth = imglist[i].split( "_" )[0] + " " + imglist[i].split( "_" )[1];	// Klasse = Gattung und Art
			//groundTruth = list[i].split( "_" )[0];						// Klasse = Gattung
			classmap.put(imglist[i], groundTruth);
		    } else {
			groundTruth = "";
		    }  
		}

		WindowManager.setTempCurrentImage(img);     // needed because image is not shown (no images open)

		// run the plugin
		ImagePlus imp_bin = LeafPreprocessor.preprocess(img);

		img.setRoi(imp_bin.getRoi(), true);

		int anOptions = ((findPetiole)?1:0) * LeafAnalyzer.FINDPETIOLE;
		LeafAnalyzer la = new LeafAnalyzer(anOptions);
		la.analyze(img, imp_bin, groundTruth);
	    }
	}
	IJ.showProgress(1.0);
	IJ.showStatus("");

	Batch_Analyzer.close_windows();

	ResultsTable rt = ResultsTable.getResultsTable();
	//rt.save( trainfolder + "_leaf.csv");
	FileHelper.writeToFile(FileHelper.map2String(classmap),trainfolder+"_classes.csv");
	WekaHelper.table2arff(rt, arffpath);


	IJ.run("Quit");
	//System.exit( 0 );
    }

}
