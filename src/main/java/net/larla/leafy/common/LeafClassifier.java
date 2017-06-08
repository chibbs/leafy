package net.larla.leafy.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.measure.ResultsTable;
import net.larla.leafy.datamodel.Leaf;
import net.larla.leafy.datamodel.Tuple;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class LeafClassifier {

    public final static String FILENAME = "model/j48default.model";
    private Classifier classifier;
    private Instances header;
    private Instance testInst;

    public LeafClassifier(Classifier classifier, Instances header) {
	super();
	this.classifier = classifier;
	this.header = header;
    }

    public LeafClassifier (String path) {
	Vector<?> v = new Vector<Object>(2);

	// read model and header
	if (path == "") {
	    // read default model from jar
	    IJ.log("\tload default classifier");
	    InputStream is1 = this.getClass().getClassLoader().getResourceAsStream(FILENAME);
	    try {
		v = (Vector<?>) SerializationHelper.read(is1);
	    } catch (Exception e) {
		e.printStackTrace();
		throw new UnsupportedOperationException("Classifier model could not be loaded.");
	    }

	    try {
		is1.close();
	    } catch (IOException e) {
		// handling not needed
		e.printStackTrace();
	    }
	} else {
	    // read custom model from file system

	    try {
		v = (Vector<?>) SerializationHelper.read(path);
	    } catch (Exception e) {
		e.printStackTrace();
		throw new UnsupportedOperationException("Classifier model could not be loaded.");
	    }

	    IJ.log("\tload classifier from " + path);
	}
	this.classifier = (Classifier) v.get(0);
	this.header = (Instances) v.get(1);

    }

    public static LeafClassifier train(String datapath) {
	IJ.log("Training classifier with data from " + datapath + "...");

	// load training data from csv
	DataSource source;
	Instances data;
	try {
	    source = new DataSource(datapath);
	    data = source.getDataSet();
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IllegalArgumentException("Training data could not be loaded.");
	}
	data.setClassIndex(0);
	//data.setClassIndex(data.numAttributes() - 1);

	// train Tree
	AdaBoostM1 ab = new AdaBoostM1();
	ab.setClassifier(new J48());
	ab.setNumIterations(100);
	try {
	    ab.buildClassifier(data);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IllegalStateException("Classifier could not be built.");
	}
	IJ.log("Training finished!");

	LeafClassifier lc = new LeafClassifier(ab, new Instances(data, 0));

	return lc;
    }

    public String predictSingle(Leaf currentleaf) {
	// TODO: error handling in try/catch-Blöcken!!!

	IJ.log("Predicting...");
	String cls = "";
	double[] propabilities = null;
	double pred = -1.0;
	double actclassnum;
	String actclass = currentleaf.getLeafclass();
	if (this.classifier == null || this.header == null) {
	    throw new IllegalStateException("Classification not possible: no classifier present.");
	}

	Instances data = buildInstances(ResultsTable.getResultsTable());	// TODO: not from table but from leaf!

	// output predictions
	for (int i1 = 0; i1 < data.numInstances(); i1++) {
	    Instance curr = data.instance(i1);
	    // create an instance for the classifier that fits the training data
	    // Instances object returned here might differ slightly from the one
	    // used during training the classifier, e.g., different order of
	    // nominal values, different number of attributes.
	    testInst = new DenseInstance(this.header.numAttributes());
	    testInst.setDataset(this.header);
	    for (int n = 0; n < this.header.numAttributes(); n++) {
		Attribute att = data.attribute(this.header.attribute(n).name());
		// original attribute is also present in the current dataset
		if (att != null) {
		    if (att.isNominal()) {
			// is this label also in the original data?
			// Note:
			// "numValues() > 0" is only used to avoid problems with nominal
			// attributes that have 0 labels, which can easily happen with
			// data loaded from a database
			if ((this.header.attribute(n).numValues() > 0) && (att.numValues() > 0)) {
			    String label = curr.stringValue(att);
			    int index = this.header.attribute(n).indexOfValue(label);
			    if (index != -1)
				testInst.setValue(n, index);
			}
		    }
		    else if (att.isNumeric()) {
			testInst.setValue(n, curr.value(att));
		    }
		    else {
			throw new IllegalStateException("Unhandled attribute type! " + att.toString());
		    }
		}
	    }

	    // predict class
	    try {
		pred = this.classifier.classifyInstance(testInst);
	    } catch (Exception e) {
		e.printStackTrace();
		throw new UnsupportedOperationException("Classification not possible");
	    }
	    cls = this.header.classAttribute().value((int) pred);
	    actclassnum = testInst.classValue();
	    if (Double.isNaN(actclassnum))
		actclassnum = -1;
	    IJ.log("actual: " + actclass + " (" + (int)actclassnum + "), predicted: " + cls + " (" + (int)pred + ")");

	}

	IJ.log("Predicting finished!");
	return cls;
    }
    
    public ArrayList<Tuple> getProp() {
	// get probabilities
	double propabilities[];
	//ArrayList<?> topMatches = new ArrayList<Object>();
	ArrayList<Tuple> topMatches = new ArrayList<Tuple>();;
	    // TODO: auslagern in Methode
	    // http://stackoverflow.com/questions/31405503/weka-how-to-use-classifier-in-java
	    try {
		propabilities = this.classifier.distributionForInstance(testInst);
		IJ.log("Propabilities:");
		for (int a = 0; a < propabilities.length; a++) {
		    if (propabilities[a] != 0) {
			propabilities[a] = propabilities[a] * 100000;
			propabilities[a] = Math.round(propabilities[a]);
			if (propabilities[a] != 0) {
        			propabilities[a] /= 1000;
        			//IJ.log("\t\t" + inst.classAttribute().value(a) + ": " + propabilities[a]);
        			topMatches.add(new Tuple(testInst.classAttribute().value(a), propabilities[a]));	
			}
		    }
		}
		Collections.sort(topMatches, Collections.reverseOrder());
		
	    } catch (Exception e) {
		// no handling needed
		e.printStackTrace();
	    }
	    return topMatches;
    }
   

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

    public void saveModel(String modelpath) {
	// save model + header
	Vector<Object> v = new Vector<Object>();
	v.add(this.classifier);
	v.add(this.header);
	try {
	    SerializationHelper.write(modelpath, v);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new UnsupportedOperationException("Classifier model could not be saved.");
	}

	IJ.log("\tWrote classifier to " + modelpath);
    }

}
