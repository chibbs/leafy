package net.larla.leafy.datamodel;
import ij.ImagePlus;
import ij.gui.Roi;

public class Leaf {
    private ImagePlus img;
    private String title;
    private String leafclass;
    private ImagePlus mask;
    private Roi contour;
    private Roi hullroi;
    private Roi bbroi;
    private Roi petioleroi;
    private Roi bladeroi;
    private Roi ellipseroi;
    private RadialDistances ccd;
    private double circularity;
    private double roundness;
    private double solidity;
    private double skewness;
    private double kurtosis;
    private double maxcaliper;
    private double mincaliper;
    private double area;
    private double perimeter;
    private double convexity;
    private double elliptic;
    private double haralick1;
    private double haralick2;
    private double centroidX;
    private double centroidY;
    private double petioleratio;

    public Roi getBbroi() {
        return bbroi;
    }

    public void setBbroi(Roi bbroi) {
        this.bbroi = bbroi;
    }

    public Roi getPetioleroi() {
        return petioleroi;
    }

    public void setPetioleroi(Roi petioleroi) {
        this.petioleroi = petioleroi;
    }

    public double getPetioleratio() {
        return petioleratio;
    }

    public void setPetioleratio(double petioleratio) {
        this.petioleratio = petioleratio;
    }

    public Leaf(ImagePlus img, String title, String leafclass) {
	this.img = img;
	this.title = title;
	this.leafclass = leafclass;
    }
    
    public Leaf(ImagePlus img, String title, String leafclass, Roi roi, ImagePlus mask) {
	this.img = img;
	this.title = title;
	this.leafclass = leafclass;
	this.contour = roi;
	this.setMask(mask);
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

    public ImagePlus getMask() {
	return mask;
    }

    public void setMask(ImagePlus mask) {
	if (mask.getType() != 0)
	    throw new IllegalArgumentException("Masks must be 8bit grayscale images!");
	this.mask = mask;
    }

    public double getConvexity() {
	return convexity;
    }

    public void setConvexity(double convexity) {
	this.convexity = convexity;
    }

    public double getElliptic() {
	return elliptic;
    }

    public void setElliptic(double elliptic) {
	this.elliptic = elliptic;
    }

    public double getHaralick1() {
	return haralick1;
    }

    public void setHaralick1(double haralick1) {
	this.haralick1 = haralick1;
    }

    public double getHaralick2() {
	return haralick2;
    }

    public void setHaralick2(double haralick2) {
	this.haralick2 = haralick2;
    }

    public RadialDistances getCcd() {
	return ccd;
    }

    public void setCcd(RadialDistances ccd) {
	this.ccd = ccd;
    }

    public double getCentroidX() {
	return centroidX;
    }

    public void setCentroidX(double centroidX) {
	this.centroidX = centroidX;
    }

    public double getCentroidY() {
	return centroidY;
    }

    public void setCentroidY(double centroidY) {
	this.centroidY = centroidY;
    }
    
    public Roi getBladeroi() {
        return bladeroi;
    }

    public void setBladeroi(Roi bladeroi) {
        this.bladeroi = bladeroi;
    }

}
