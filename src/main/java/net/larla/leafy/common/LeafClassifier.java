package net.larla.leafy.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;

import ij.IJ;
import ij.measure.ResultsTable;
import net.larla.leafy.datatypes.Leaf;
import net.larla.leafy.datatypes.Tuple;
import net.larla.leafy.helpers.*;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class LeafClassifier {

    public final static String GENUSCLASSIFIER = "model/j48genus.model";
    public final static String SPECIESCLASSIFIER = "model/j48species.model";
    public final static String WOPETCLASSIFIER = "model/j48wopet.model";
    public final static String DEFAULTMODEL = SPECIESCLASSIFIER;
    private Classifier classifier;
    private Instances header;		// classes and feature set
    private Instance testInst;

    public LeafClassifier(Classifier classifier, Instances header) {
	super();
	this.classifier = classifier;
	this.header = header;
    }

    public LeafClassifier (String path) {
	Vector<?> v = new Vector<Object>(2);

	// read model and header
	if (path.equals("") || path.equals("wopet")) {
	    InputStream is1;
	    if (path.equals("wopet")) {
		is1 = this.getClass().getClassLoader().getResourceAsStream(WOPETCLASSIFIER);
	    } else {
		// read default model from jar
		IJ.log("\tload default classifier");
		is1 = this.getClass().getClassLoader().getResourceAsStream(DEFAULTMODEL);
	    }
	    try {
		v = (Vector<?>) SerializationHelper.read(is1);
		is1.close();
	    } catch (Exception e) {
		e.printStackTrace();
		throw new UnsupportedOperationException("Classifier model could not be loaded.");
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
	    data.randomize(new java.util.Random(0));

            // Separate split into training and testing arrays
            int trainSize = (int) Math.round(data.numInstances() * 0.8);
            int testSize = data.numInstances() - trainSize;
            Instances train = new Instances(data, 0, trainSize);
            Instances test = new Instances(data, trainSize, testSize);
            
            ab.buildClassifier(train);
            Evaluation eval = new Evaluation(train); 
            eval.evaluateModel(ab, test);
            
            IJ.log("Training finished!");
            IJ.log("Percent correct: "+
                               Double.toString(eval.pctCorrect()));

	    ab.buildClassifier(data);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IllegalStateException("Classifier could not be built.");
	}
	
	LeafClassifier lc = new LeafClassifier(ab, new Instances(data, 0));

	return lc;
    }

    public String predictSingle(Leaf currentleaf) {

	IJ.log("Predicting...");
	String cls = "";
	double pred = -1.0;
	double actclassnum;
	String actclass = currentleaf.getLeafclass();
	if (this.classifier == null || this.header == null) {
	    throw new IllegalStateException("Classification not possible: no classifier present.");
	}

	Instances data = WekaHelper.buildInstances(ResultsTable.getResultsTable());	// TODO: not from table but from leaf!

	// output predictions
	for (int i1 = 0; i1 < data.numInstances(); i1++) {
	    Instance currFeature = data.instance(i1);
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
			    String label = currFeature.stringValue(att);
			    int index = this.header.attribute(n).indexOfValue(label);
			    if (index != -1)
				testInst.setValue(n, index);
			}
		    }
		    else if (att.isNumeric()) {
			testInst.setValue(n, currFeature.value(att));
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
    
    public ArrayList<Tuple> getProb() {
	// get probabilities
	double probabilities[];
	//ArrayList<?> topMatches = new ArrayList<Object>();
	ArrayList<Tuple> topMatches = new ArrayList<Tuple>();;
	// http://stackoverflow.com/questions/31405503/weka-how-to-use-classifier-in-java
	try {
	    probabilities = this.classifier.distributionForInstance(testInst);
	    IJ.log("Probabilities:");
	    for (int a = 0; a < probabilities.length; a++) {
		if (probabilities[a] != 0) {
		    probabilities[a] = probabilities[a] * 100;
		    probabilities[a] = Math.round(probabilities[a] * 100);
		    if (probabilities[a] != 0) {
			probabilities[a] /= 100;
			//IJ.log("\t\t" + inst.classAttribute().value(a) + ": " + propabilities[a]);
			topMatches.add(new Tuple(testInst.classAttribute().value(a), probabilities[a]));	
		    }
		}
	    }
	    Collections.sort(topMatches, Collections.reverseOrder());

	} catch (Exception e) {
	    // no handling needed
	}
	return topMatches;
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
