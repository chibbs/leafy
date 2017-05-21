package net.larla.leafy.datamodel;
import java.util.ArrayList;

public class RadialDistances {
    ArrayList<Double> ccd;
    ArrayList<Double> normccd;
    private double maxdist;
    private double meandist;
    private double variance;
    private double sdev;
    private double normmean;
    private double normsdev;


    public RadialDistances(ArrayList<Double> ccd) {
	super();
	this.ccd = ccd;
    }
      

    public RadialDistances(ArrayList<Double> ccd, double maxdist, double meandist,
	    double variance, double sdev, ArrayList<Double> normccd, double normmean, double normsdev) {
	super();
	this.ccd = ccd;
	this.normccd = normccd;
	this.maxdist = maxdist;
	this.meandist = meandist;
	this.variance = variance;
	this.sdev = sdev;
	this.normmean = normmean;
	this.normsdev = normsdev;
    }
    public ArrayList<Double> getCcd() {
	return this.ccd;
    }
    public ArrayList<Double> getNormccd() {
	return this.normccd;
    }
    public double getMaxdist() {
	return this.maxdist;
    }
    public double getMeandist() {
	return this.meandist;
    }
    public double getVariance() {
	return this.variance;
    }
    public double getStddeviation() {
	return this.sdev;
    }
    public void calculateKeyfigures() {
	double normd;
	this.normmean = 0;
	double normvar = 0;
	this.normccd = new ArrayList<Double>();

	for (double d : this.ccd)
	    this.maxdist = this.maxdist < d ? d : this.maxdist;

	for (double d : this.ccd) {
	    normd = d / this.maxdist;
	    this.normccd.add(normd);
	    this.normmean += normd;
	}
	this.normmean /= this.normccd.size();

	for (double nd : this.normccd)
	    normvar = normvar + Math.pow(nd - normvar, 2);
	normvar /= this.normccd.size();
	this.normsdev = Math.sqrt( normvar );
    }
    public double getNormMean() {
	return normmean;
    }
    public double getNormSdev() {
	return normsdev;
    }



}
