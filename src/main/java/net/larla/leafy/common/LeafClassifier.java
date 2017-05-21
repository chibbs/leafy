package net.larla.leafy.common;
import weka.classifiers.*;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.util.*;

import ij.IJ;
import ij.measure.ResultsTable;

public class LeafClassifier {

    public final static String FILENAME = "model/j48default.model";

    public void train(String csvpath, String modelpath) throws Exception {
	System.out.println("Training...");

	// load training data from csv
	DataSource source = new DataSource(csvpath);
	Instances data = source.getDataSet();
	data.setClassIndex(0);
	//data.setClassIndex(data.numAttributes() - 1);
	String path = modelpath;

	// train Tree
	AdaBoostM1 ab = new AdaBoostM1();
	ab.setClassifier(new J48());
	ab.setNumIterations(100);
	ab.buildClassifier(data);

	// save model + header
	Vector<RevisionHandler> v = new Vector<RevisionHandler>();
	v.add(ab);
	v.add(new Instances(data, 0));
	SerializationHelper.write(path, v);

	System.out.println("Training finished!\nWrote classifier to " + modelpath);
    }

    public String predictSingle(Instances data) throws Exception {
	System.out.println("Predicting...");
	InputStream is1 = getClass().getClassLoader().getResourceAsStream(FILENAME);
	
	String cls = "?";

	// read model and header
	Vector<?> v = (Vector<?>) SerializationHelper.read(is1);
	Classifier cl = (Classifier) v.get(0);
	Instances header = (Instances) v.get(1);
	is1.close();

	// output predictions
	for (int i1 = 0; i1 < data.numInstances(); i1++) {
	    Instance curr = data.instance(i1);
	    // create an instance for the classifier that fits the training data
	    // Instances object returned here might differ slightly from the one
	    // used during training the classifier, e.g., different order of
	    // nominal values, different number of attributes.
	    Instance inst = new DenseInstance(header.numAttributes());
	    inst.setDataset(header);
	    for (int n = 0; n < header.numAttributes(); n++) {
		Attribute att = data.attribute(header.attribute(n).name());
		// original attribute is also present in the current dataset
		if (att != null) {
		    if (att.isNominal()) {
			// is this label also in the original data?
			// Note:
			// "numValues() > 0" is only used to avoid problems with nominal 
			// attributes that have 0 labels, which can easily happen with
			// data loaded from a database
			if ((header.attribute(n).numValues() > 0) && (att.numValues() > 0)) {
			    String label = curr.stringValue(att);
			    int index = header.attribute(n).indexOfValue(label);
			    if (index != -1)
				inst.setValue(n, index);
			}
		    }
		    else if (att.isNumeric()) {
			inst.setValue(n, curr.value(att));
		    }
		    else {
			throw new IllegalStateException("Unhandled attribute type! " + att.toString());
		    }
		}
	    }

	    // predict class
	    double pred = cl.classifyInstance(inst);
	    cls = inst.classAttribute().value((int) pred);
	    //System.out.println(inst.classValue() + " -> " + pred + " (" + cls + ")");

	    //System.out.print("ID: " + inst.value(0));
	    System.out.print(", actual: " + data.classAttribute().value((int) inst.classValue()));
	    System.out.println(", predicted: " + inst.classAttribute().value((int) pred));
	    
	    
	    // get probabilities
	    // http://stackoverflow.com/questions/31405503/weka-how-to-use-classifier-in-java
	    double[] propabilities = cl.distributionForInstance(inst);
	    System.out.println("Propabilities:");
	    for (int a = 0; a < propabilities.length; a++) {
		if (propabilities[a] != 0)
		    System.out.println(inst.classAttribute().value(a) + ": " + propabilities[a]);
	    }

	}

	System.out.println("Predicting finished!");
	return cls;
    }
    public String predictSingle(Instances data, String modelpath) throws Exception {
	System.out.println("Predicting...");
	Vector<?> v = new Vector<Object>(2);
	String cls = "?";
	
	// read model and header
	if (modelpath == "") {
	    // read default model from jar
	    IJ.log("load default classifier");
        	InputStream is1 = getClass().getClassLoader().getResourceAsStream(FILENAME);
        	v = (Vector<?>) SerializationHelper.read(is1);
        	is1.close();
	} else {
	    // read custom model from file system
	    v = (Vector<?>) SerializationHelper.read(modelpath);
	    IJ.log("load classifier from " + modelpath);
	}
	Classifier cl = (Classifier) v.get(0);
	Instances header = (Instances) v.get(1);
	

	// output predictions
	for (int i1 = 0; i1 < data.numInstances(); i1++) {
	    Instance curr = data.instance(i1);
	    // create an instance for the classifier that fits the training data
	    // Instances object returned here might differ slightly from the one
	    // used during training the classifier, e.g., different order of
	    // nominal values, different number of attributes.
	    Instance inst = new DenseInstance(header.numAttributes());
	    inst.setDataset(header);
	    for (int n = 0; n < header.numAttributes(); n++) {
		Attribute att = data.attribute(header.attribute(n).name());
		// original attribute is also present in the current dataset
		if (att != null) {
		    if (att.isNominal()) {
			// is this label also in the original data?
			// Note:
			// "numValues() > 0" is only used to avoid problems with nominal 
			// attributes that have 0 labels, which can easily happen with
			// data loaded from a database
			if ((header.attribute(n).numValues() > 0) && (att.numValues() > 0)) {
			    String label = curr.stringValue(att);
			    int index = header.attribute(n).indexOfValue(label);
			    if (index != -1)
				inst.setValue(n, index);
			}
		    }
		    else if (att.isNumeric()) {
			inst.setValue(n, curr.value(att));
		    }
		    else {
			throw new IllegalStateException("Unhandled attribute type! " + att.toString());
		    }
		}
	    }

	    // predict class
	    double pred = cl.classifyInstance(inst);
	    cls = inst.classAttribute().value((int) pred);
	    //System.out.println(inst.classValue() + " -> " + pred + " (" + cls + ")");

	    //System.out.print("ID: " + inst.value(0));
	    System.out.print(", actual: " + header.classAttribute().value((int) inst.classValue()));	// TODO: fix problem with incompatible headers -> IndexOutOfBoundsException after custom call
	    System.out.println(", predicted: " + inst.classAttribute().value((int) pred));
	    
	    
	    // get probabilities
	    // http://stackoverflow.com/questions/31405503/weka-how-to-use-classifier-in-java
	    double[] propabilities = cl.distributionForInstance(inst);
	    System.out.println("Propabilities:");
	    for (int a = 0; a < propabilities.length; a++) {
		if (propabilities[a] != 0)
		    System.out.println(inst.classAttribute().value(a) + ": " + propabilities[a]);
	    }

	}

	System.out.println("Predicting finished!");
	return cls;
    }

    public Instances buildInstances(ResultsTable rttmp) {
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

}
