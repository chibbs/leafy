
public class RadialDistances {
    private double[] ccd;
    private double mindist;
    private double maxdist;
    private double meandist;
    private double variance;
    private double stddeviation;
    
    
    
    public RadialDistances(double[] ccd) {
	super();
	this.ccd = ccd;
    }
    public RadialDistances(double[] ccd, double mindist, double maxdist, double meandist, double variance,
	    double stddeviation) {
	super();
	this.ccd = ccd;
	this.mindist = mindist;
	this.maxdist = maxdist;
	this.meandist = meandist;
	this.variance = variance;
	this.stddeviation = stddeviation;
    }
    public double[] getCcd() {
        return ccd;
    }
    public void setCcd(double[] ccd) {
        this.ccd = ccd;
    }
    public double getMindist() {
        return mindist;
    }
    public double getMaxdist() {
        return maxdist;
    }
    public double getMeandist() {
        return meandist;
    }
    public double getVariance() {
        return variance;
    }
    public double getStddeviation() {
        return stddeviation;
    }
    public void calculateKeyfigures() {
	// TODO
    }

}
