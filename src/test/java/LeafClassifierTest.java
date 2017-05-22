import org.junit.Assert;
import org.junit.Test;

import ij.measure.ResultsTable;
import net.larla.leafy.common.LeafClassifier;
import weka.core.Instances;

public class LeafClassifierTest {

    public LeafClassifierTest() {
	// TODO Auto-generated constructor stub
    }

    @Test
    public void shouldBuildInstancesSimple() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Heading1", 0.1d );
	rt_test.addValue( "Heading2", 0.5786574839);

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);
	} catch (Exception e) {
	    System.out.println(e);
	}
	Assert.assertNotNull(inst);
    }

    @Test
    public void shouldBuildInstancesWithClass() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "Class1" );
	rt_test.addValue( "Heading1", 0.1d );
	rt_test.addValue( "Heading2", 0.5786574839);

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);
	    Assert.assertTrue(inst.classIndex() == 0);
	} catch (Exception e) {
	    System.out.println(e);
	    Assert.assertNotNull(inst);
	}

    }
    
    @Test
    public void shouldBuildInstancesWithClass2() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "Class1" );
	rt_test.addValue( "Heading1", 0.1d );
	rt_test.addValue( "Heading2", 0.5786574839);
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "?" );
	rt_test.addValue( "Heading1", 56 );
	rt_test.addValue( "Heading2", 0);

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);
	    Assert.assertTrue(inst.classIndex() == 0);
	} catch (Exception e) {
	    System.out.println(e);
	    Assert.assertNotNull(inst);
	}

    }
    
    @Test
    public void shouldBuildInstancesWithLabel() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Label", "Image1" );
	rt_test.addValue( "Heading1", 0.1d );
	rt_test.addValue( "Heading2", 0.5786574839);
	rt_test.incrementCounter();
	rt_test.addValue( "Label", "Image2" );
	rt_test.addValue( "Heading1", 56 );
	rt_test.addValue( "Heading2", 0);

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);

	} catch (Exception e) {
	    System.out.println(e);
	    
	}
	Assert.assertNotNull(inst);

    }
    
    @Test
    public void shouldBuildInstancesWithMissingValues() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "c1" );
	rt_test.addValue( "Heading2", 0.5786574839);
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "c2" );
	rt_test.addValue( "Heading1", 56 );

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);
	} catch (Exception e) {
	    System.out.println(e);
	}
	Assert.assertNotNull(inst);
    }
    
    @Test
    public void shouldBuildInstancesWithMissingClass() {
	Instances inst = null;

	// create Test Table
	ResultsTable rt_test = new ResultsTable();
	rt_test.showRowNumbers( false );
	rt_test.incrementCounter();
	rt_test.addValue( "Class", "Class1" );
	rt_test.addValue( "Heading1", 0.1d );
	rt_test.addValue( "Heading2", 0.5786574839);
	rt_test.incrementCounter();
	rt_test.addValue( "Heading1", 56 );
	rt_test.addValue( "Heading2", 0);

	// build Instances
	try {
	    inst = LeafClassifier.buildInstances(rt_test);
	} catch (Exception e) {
	    System.out.println(e);
	}
	Assert.assertNotNull(inst);

    }

}
