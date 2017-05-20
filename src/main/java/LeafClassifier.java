import weka.classifiers.*;
import weka.classifiers.trees.*;
import weka.core.*;
import weka.experiment.*;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.ArrayList;
import java.util.Vector;

public class LeafClassifier {
    
    public void test(String path) throws Exception {
	/*J48 tree;
	    tree = (J48) weka.core.SerializationHelper.read("../resources/J48boosted.model");

	
	//load new dataset
			DataSource source1 = new DataSource("/home/likewise-open/ACADEMIC/csstnns/Desktop/qdb-unknown.arff");
			Instances testDataset = source1.getDataSet();	
			//set class index to the last attribute
			testDataset.setClassIndex(testDataset.numAttributes()-1);
			
			//get class double value for first instance
			double actualValue = testDataset.instance(0).classValue();
			//get Instance object of first instance
			Instance newInst = testDataset.instance(0);
			Instance n = new Instance(3);
			//call classifyInstance, which returns a double value for the class
			double predSMO = tree.classifyInstance(newInst);

	System.out.println(actualValue+", "+predSMO);*/
    }
    
    public final static String FILENAME = "src/main/resources/j48example.save";
    
    public void train(String path) throws Exception {
      System.out.println("Training...");

      // load training data from database
      /*InstanceQuery query = new InstanceQuery();
      query.setDatabaseURL(URL);
      query.setUsername(USER);
      query.setPassword(PASSWORD);
      query.setQuery("select * from Results0");
      Instances data = query.retrieveInstances();*/
      DataSource source = new DataSource(path);
      Instances data = source.getDataSet();
      data.setClassIndex(0);
      //data.setClassIndex(data.numAttributes() - 1);

      // train M5P
      J48 tree = new J48();
      // further options...
      tree.buildClassifier(data);

      // save model + header
      Vector v = new Vector();
      v.add(tree);
      v.add(new Instances(data, 0));
      SerializationHelper.write(FILENAME, v);

      System.out.println("Training finished!");
    }

    @SuppressWarnings("rawtypes")
    public void predict(String path) throws Exception {
	// https://weka.wikispaces.com/Use+Weka+in+your+Java+code#Classifying%20instances
      System.out.println("Predicting...");

      // load data from database that needs predicting

     DataSource source = new DataSource(path);
      Instances data = source.getDataSet();
      data.setClassIndex(1);
      //data.setClassIndex(data.numAttributes() - 1);

      // read model and header
      Vector v = (Vector) SerializationHelper.read(FILENAME);
      Classifier cl = (Classifier) v.get(0);
      Instances header = (Instances) v.get(1);

      // output predictions
      System.out.println("actual -> predicted");
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
              throw new IllegalStateException("Unhandled attribute type!");
            }
          }
        }

        // predict class
        double pred = cl.classifyInstance(inst);
        String cls = inst.classAttribute().value((int) pred);
        System.out.println(inst.classValue() + " -> " + pred + " (" + cls + ")");
      }

      System.out.println("Predicting finished!");
    }

 
    public void predictSingle(String path) throws Exception {
	      System.out.println("Predicting...");

	      ArrayList<Attribute>      atts;
	      ArrayList<Attribute>      attsRel;
	      ArrayList<Attribute>      attVals;
	      ArrayList<Attribute>      attValsRel;
	      Instances       data;
	      Instances       dataRel;
	      double[]        vals;
	      double[]        valsRel;
	      int             i;
	  
	      
	      // 1. set up attributes
	      atts = new ArrayList<Attribute>();
	      // - numeric
	      atts.add(new Attribute("att1"));
	      // - nominal
	      attVals = new ArrayList<Attribute>();
	      for (i = 0; i < 5; i++)
	        attVals.add(new Attribute("val" + (i+1)));
	      atts.add(new Attribute("att2", 123));
	      // - string
	      atts.add(new Attribute("att3"));
	      // - date
	      atts.add(new Attribute("att4", "yyyy-MM-dd"));
	      // - relational
	      attsRel = new ArrayList<Attribute>();
	      // -- numeric
	      attsRel.add(new Attribute("att5.1"));
	      // -- nominal
	      attValsRel = new ArrayList<Attribute>();
	      for (i = 0; i < 5; i++)
	        attValsRel.add(new Attribute("val5." + (i+1)));
	      attsRel.add(new Attribute("att5.2", 567));
	      dataRel = new Instances("att5", attsRel, 0);
	      atts.add(new Attribute("att5", dataRel, 0));
	  
	      // 2. create Instances object
	      data = new Instances("MyRelation", atts, 0);
	  
	      // 3. fill with data
	      // first instance
	      vals = new double[data.numAttributes()];
	      // - numeric
	      vals[0] = Math.PI;
	      // - nominal
	      vals[1] = attVals.indexOf("val3");
	      // - string
	      vals[2] = data.attribute(2).addStringValue("This is a string!");
	      // - date
	      vals[3] = data.attribute(3).parseDate("2001-11-09");
	      // - relational
	      dataRel = new Instances(data.attribute(4).relation(), 0);
	      // -- first instance
	      valsRel = new double[2];
	      valsRel[0] = Math.PI + 1;
	      valsRel[1] = attValsRel.indexOf("val5.3");
	      dataRel.add(new DenseInstance(1.0, valsRel));
	      // -- second instance
	      valsRel = new double[2];
	      valsRel[0] = Math.PI + 2;
	      valsRel[1] = attValsRel.indexOf("val5.2");
	      dataRel.add(new DenseInstance(1.0, valsRel));
	      vals[4] = data.attribute(4).addRelation(dataRel);
	      // add
	      data.add(new DenseInstance(1.0, vals));
	      
	   // 4. output data
	      System.out.println(data);

	      // read model and header
	      Vector v = (Vector) SerializationHelper.read(FILENAME);
	      Classifier cl = (Classifier) v.get(0);
	      Instances header = (Instances) v.get(1);

	      // output predictions
	      System.out.println("actual -> predicted");
	      for (int i = 0; i < data.numInstances(); i++) {
	        Instance curr = data.instance(i);
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
	              throw new IllegalStateException("Unhandled attribute type!");
	            }
	          }
	        }

	        // predict class
	        double pred = cl.classifyInstance(inst);
	        String cls = inst.classAttribute().value((int) pred);
	        System.out.println(inst.classValue() + " -> " + pred + " (" + cls + ")");
	      }

	      System.out.println("Predicting finished!");
	    }
	

}
