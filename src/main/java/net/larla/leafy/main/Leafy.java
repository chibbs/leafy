package net.larla.leafy.main;

import java.awt.Frame;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.text.TextPanel;
import ij.text.TextWindow;
import net.larla.leafy.datatypes.Leaf;
import net.larla.leafy.datatypes.Tuple;

public class Leafy {

    static boolean showContours = true;
    String modelpath = "";
    boolean findPetiole = true;
    
    
    protected void createOverlay (Leaf l, String cls, ImagePlus imp) {
	// show name in image
		Roi roi_leaf = l.getContour();
		roi_leaf.setName(cls);
		imp.setRoi(roi_leaf);
		IJ.run("Add Selection...");
		IJ.run("Labels...", "color=white font=14 show use draw");
    }
    
    protected void showResults(String cls, ArrayList<Tuple> pl, String imagename) {
	String title = "Leafy - Results of classification";
	TextWindow tw;
	TextPanel tp;
	Frame f = WindowManager.getFrame(title);
	if (f==null || !(f instanceof TextWindow)) {
	    tw = new TextWindow(title,"",400,500);
	} else {
	    tw = (TextWindow) f;
	}
	tp = tw.getTextPanel();
	tp.append("Classified leaf in image " + imagename + "...");
	if (cls != "") {
	    tp.appendLine("This leaf belongs to plant class " + cls + ".");
	} else {
	    tp.appendLine("The plant class of this leaf could not be determined." );  
	}
	if (!pl.isEmpty()) {
	    tp.appendLine("Probabilities:");
	    for (Tuple t : pl) {
		tp.appendLine(t.s + ": \t" + t.d + " %");
	    }
	    tp.appendLine("");
	}
	tw.toFront();
    }
    
    protected void parseArg(String arg) {
	String[] args = arg.split(",");
	for(int i = 0; i < args.length; i++) {
	    switch (args[i]) {
	    case "custom":	    
		OpenDialog od = new OpenDialog("Select classifier...", OpenDialog.getLastDirectory(), "boosted.model");
		this.modelpath = od.getDirectory() + od.getFileName();
		break;
		/*case "genus":
	    this.modelpath = "genus";
	    break;
	case "species":
	    this.modelpath = "species";
	    break;*/
	    case "withoutpetiole":
		this.findPetiole = false;
		this.modelpath = "wopet";
		break;
	    }
	}
    }

}
