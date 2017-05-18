import ij.ImagePlus;
import ij.gui.Roi;

public class Leaf {
    private ImagePlus img;
    private String title;
    private String leafclass;
    // private ImagePlus mask;
    private Roi contour;
    private Roi hullroi;
    private Roi ellipseroi;
    private double centroid;
    private double[] ccd;
    private double circularity;
    private double roundness;
    private double solidity;
    private double skewness;
    private double kurtosis;
    private double maxcaliper;
    private double mincaliper;
    private double area;
    private double perimeter;

    public Leaf(ImagePlus img, String title, String leafclass) {
	this.img = img;
	this.title = title;
	this.leafclass = leafclass;
    }

    public ImagePlus getImg() {
	return this.img;
    }

    public void setImg(ImagePlus img) {
	this.img = img;
    }

    public String getTitle() {
	return this.title;
    }

    public void setTitle(String title) {
	this.title = title;
    }

    public String getLeafclass() {
	return this.leafclass;
    }

    public void setLeafclass(String leafclass) {
	this.leafclass = leafclass;
    }

    public Roi getContour() {
	return this.contour;
    }

    public void setContour(Roi contour) {
	this.contour = contour;
    }

    public Roi getHullroi() {
	return this.hullroi;
    }

    public void setHullroi(Roi hullroi) {
	this.hullroi = hullroi;
    }

    public Roi getEllipseroi() {
	return this.ellipseroi;
    }

    public void setEllipseroi(Roi ellipseroi) {
	this.ellipseroi = ellipseroi;
    }

    public double getCentroid() {
	return this.centroid;
    }

    public void setCentroid(double centroid) {
	this.centroid = centroid;
    }

    public double[] getCcd() {
	return this.ccd;
    }

    public void setCcd(double[] ccd) {
	this.ccd = ccd;
    }

    public double getCircularity() {
	return this.circularity;
    }

    public void setCircularity(double circularity) {
	this.circularity = circularity;
    }

    public double getRoundness() {
	return this.roundness;
    }

    public void setRoundness(double roundness) {
	this.roundness = roundness;
    }

    public double getSolidity() {
	return this.solidity;
    }

    public void setSolidity(double solidity) {
	this.solidity = solidity;
    }

    public double getSkewness() {
	return this.skewness;
    }

    public void setSkewness(double skewness) {
	this.skewness = skewness;
    }

    public double getKurtosis() {
	return this.kurtosis;
    }

    public void setKurtosis(double kurtosis) {
	this.kurtosis = kurtosis;
    }

    public double getMaxcaliper() {
	return this.maxcaliper;
    }

    public void setMaxcaliper(double maxcaliper) {
	this.maxcaliper = maxcaliper;
    }

    public double getMincaliper() {
	return this.mincaliper;
    }

    public void setMincaliper(double mincaliper) {
	this.mincaliper = mincaliper;
    }

    public double getArea() {
	return this.area;
    }

    public void setArea(double area) {
	this.area = area;
    }

    public double getPerimeter() {
	return this.perimeter;
    }

    public void setPerimeter(double perimeter) {
	this.perimeter = perimeter;
    }

}
