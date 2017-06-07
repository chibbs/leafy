package jnmaloof.leafj;
/*	ImageJ plug-in to measure leaves
	This version is to work on one image at a time
	User should select an ROI containing multiple leaves
	plug-in will then find leaves and measure
	
	Julin Maloof
	Plant Biology, University of California, Davis
	jnmaloof@ucdavis.edu
	
	modified from hypocotyl_multi02.java
	version 0.1 Oct 07, 2010
*/

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.plugin.frame.*;

import java.util.Arrays;
import java.util.Comparator;


public class LeafJ_ implements PlugInFilter {
	ImagePlus imp;
	public int verbose = 0;
	

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {

		double minThreshold = ip.getMinThreshold();
		double maxThreshold = ip.getMaxThreshold();
		sampleDescription sd = new sampleDescription();
		LeafResults results = new LeafResults(imp.getShortTitle() + "_measurements.txt");

		
		if (minThreshold == ImageProcessor.NO_THRESHOLD) {
			IJ.showMessage("A thresholded image is required.");
			return;
		}
		
		sd.setDescription(imp.getTitle()); //get user input for sample description and add file name	
		
		ImagePlus timp = new ImagePlus("temporary",ip.crop()); //temporary ImagePlus to hold current ROI
		Calibration cal = timp.getCalibration();
		ImageProcessor tip = timp.getProcessor(); //ImageProcessor associated with timp
		tip.setThreshold(minThreshold,maxThreshold,ImageProcessor.NO_LUT_UPDATE);
		ResultsTable rt = new ResultsTable();
		RoiManager rm = new RoiManager();
		rm = RoiManager.getInstance();
		if (rm.getCount()>0) {
			//if a ROI window is already open, this is necessary
			rm.runCommand("Select All");
			rm.runCommand("Delete");
		}
			
		double minParticleSize = 4000;
		
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE
			//+ParticleAnalyzer.SHOW_RESULTS
			//+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
			,Measurements.RECT+Measurements.ELLIPSE, rt, minParticleSize, Double.POSITIVE_INFINITY,.15,1);
		
		
		IJ.log("Threshold: " + IJ.d2s(maxThreshold));
	
		pa.analyze(timp,tip);
		
		if (verbose > 2) IJ.showMessage("leaves found");
		timp.show();		
		timp.updateAndDraw();
		
		//IJ.setColumnHeadings("Plant	Top	Bottom");
		
		leaf[] leaves = new leaf[rt.getCounter()];	//an array of leaves
		int[][] xpos = new int[rt.getCounter()][];	//an array to hold leaf position
		Comparator<int[]> sorter = new Sort2D(1);	//to sort the xpos[][]

		//need to sort leaves based on x position
		//first go through the results table to get x position
		//then sort before setting up leaves and processing
		
		//Find boundary x position for each leaf
		for (int leafCurrent = 0; leafCurrent < rt.getCounter();leafCurrent++) {
			xpos[leafCurrent] = new int[] {leafCurrent, (int) rt.getValue("BX",leafCurrent)};
		}
		
		//sort the array based on x position
		Arrays.sort(xpos,sorter);

	
		//set and process leaves
		for (int i = 0; i < xpos.length;i++) {	//Move through the leaves
			int leafCurrent = xpos[i][0];
			if (verbose > 0) IJ.log("leaf multi. i = " + IJ.d2s(i,0) + ". leaf current = " + IJ.d2s(leafCurrent,0));
			if (verbose > 2) IJ.showMessage("setting leaf attributes for leaf: " + IJ.d2s(leafCurrent,0));
			leaves[leafCurrent] = new leaf();
			leaves[leafCurrent].setLeaf(rt, leafCurrent,cal);		//set initial attributes
	
			leaves[leafCurrent].scanLeaf(tip);			//do a scan across the length to determine widths
			leaves[leafCurrent].findPetiole(tip);			//
			if (verbose >2) {
				timp.updateAndDraw();
				IJ.showMessage("end find Petiole");
			}
			leaves[leafCurrent].addPetioleToManager(timp, tip, rm, i);
			leaves[leafCurrent].addBladeToManager(timp, tip, rm, i);
		
		}//for leafCurrent
		//timp = WindowManager.getCurrentImage();
		timp.setProcessor("results",tip);
		timp.setCalibration(imp.getCalibration());
		ImageCanvas ic = new ImageCanvas(timp);
		ic.setShowAllROIs(true);
		timp.updateAndDraw();
		
		//allow user to make adjustments to ROI
		WaitForUserDialog waitForAdjust = new WaitForUserDialog("Please adjust ROIs and press OK when done");
		waitForAdjust.show();		
		
		//now I need to fill up a results table and include the sample description data.
		//hmmm looks like results table can only take numeric values
		//use TextPanel class
		
		if (sd.saveRois) rm.runCommand("Save", imp.getShortTitle() + sd.getTruncatedDescription() + "_roi.zip");
		results.setHeadings(sd.getFieldNames());
		results.show();
		results.addResults(sd,rm,tip,timp);
		
		timp.close();
		
		//if (verbose > 0) IJ.showMessage("done");


	} //run

	}//class
	
	//note: could extend this whole thing by making an array of sumArrays
	//this would allow processing across slices
	//(might not be needed if one passed the previous value along to
	//the validate method)

