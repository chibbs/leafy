package net.larla.leafy.datamodel;

public class RadialDistances {
    double[] ccd;
    double[] normccd;
    private double maxdist;
    private double meandist;
    private double variance;
    private double sdev;
    private double normmean;
    private double normsdev;


    public RadialDistances(double[] ccd) {
	super();
	this.ccd = ccd;
    }
      

    public RadialDistances(double[] ccd, double maxdist, double meandist,
	    double variance, double sdev, double[] normccd, double normmean, double normsdev) {
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
    public double[] getCcd() {
	return this.ccd;
    }
    public double[] getNormccd() {
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
	this.normccd = new double[this.ccd.length];

	for (double d : this.ccd) {
	    if (this.maxdist < d) {
		this.maxdist =  d;
	    }
	}

	for (int i = 0; i < this.ccd.length; i++) {
	    normd = this.ccd[i] / this.maxdist;
	    this.normccd[i] = (normd);
	    this.normmean += normd;
	}
	this.normmean /= this.normccd.length;

	for (double nd : this.normccd)
	    normvar = normvar + Math.pow(nd - normvar, 2);
	normvar /= this.normccd.length;
	this.normsdev = Math.sqrt( normvar );
    }
    public double getNormMean() {
	return normmean;
    }
    public double getNormSdev() {
	return normsdev;
    }

}
