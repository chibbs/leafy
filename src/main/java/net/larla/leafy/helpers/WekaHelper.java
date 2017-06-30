package net.larla.leafy.helpers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import ij.measure.ResultsTable;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class WekaHelper {

    public static Instances buildInstances(ResultsTable rttmp) {
	ArrayList<Attribute>      atts;
	ArrayList<String>      attVals = new ArrayList<String>();
	Instances       data;
	double[]        vals;
	int             i;
	int colindex;
	double numval;
	String strval;
	HashSet<String> classvalues = new HashSet<String>();

	// 1. set up attributes
	atts = new ArrayList<Attribute>();
	String[] headings = rttmp.getHeadings();
	int classindex = rttmp.getColumnIndex("Class");
	if (classindex != ResultsTable.COLUMN_NOT_FOUND) {
	    for (int r = 0; r < rttmp.getCounter(); r++)
		classvalues.add(rttmp.getStringValue(classindex, r));
	    classvalues.add("?");
	}

	//vals = new double[data.numAttributes()];
	int attnum = rttmp.getLastColumn() + 1;
	vals = new double[attnum];
	i = 0;
	for (String heading: headings) {
	    colindex = rttmp.getColumnIndex(heading);	// index of column in table!
	    numval = rttmp.getValueAsDouble(colindex, 0);
	    if (Double.isNaN(numval)) {
		strval = rttmp.getStringValue(colindex, 0);
		if (heading == "Class") {
		    attVals = new ArrayList<String>();
		    for (String val : classvalues)
			attVals.add(val);
		    atts.add(new Attribute("Class", attVals));
		} else {
		    atts.add(new Attribute(heading, true));
		}
	    } else {
		atts.add(new Attribute(heading));
	    }
	    i++;
	}
	// 2. create Instances object
	data = new Instances("MyRelation", atts, 0);
	if (classindex >= 0) {
	    data.setClassIndex(classindex);
	} else {
	    data.setClassIndex(data.numAttributes() - 1);
	}

	for (int row = 0; row < rttmp.getCounter(); row++) {
	    vals = new double[data.numAttributes()];
	    i = 0;
	    for (String heading: headings) {
		colindex = rttmp.getColumnIndex(heading);	// index of column in table!
		numval = rttmp.getValueAsDouble(colindex, row);
		if (Double.isNaN(numval)) {
		    strval = rttmp.getStringValue(colindex, row);
		    if (data.attribute(i).isNominal()) {


			vals[i] = attVals.indexOf(strval);
		    } else if (data.attribute(i).isString()) {
			vals[i] = data.attribute(i).addStringValue(strval);
		    }
		} else {
		    vals[i] = numval;
		}
		i++;
	    }
	    // add
	    data.add(new DenseInstance(1.0, vals));
	}

	return data;
    }
    
    public static void table2arff(ResultsTable rt, String arffpath) {
	    Instances inst = WekaHelper.buildInstances(rt);

	    // save weka data
	    try {
		BufferedWriter writer = new BufferedWriter(
			new FileWriter(arffpath));
		writer.write(inst.toString());
		writer.newLine();
		writer.flush();
		writer.close();
	    } catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
    }
    
    

}
