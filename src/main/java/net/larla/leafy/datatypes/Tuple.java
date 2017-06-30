package net.larla.leafy.datatypes;

public class Tuple implements Comparable<Tuple> {
    @Override
    public String toString() {
	return "Tuple [s=" + s + ", d=" + d + "]";
    }
    public String s;
    public double d;
    public Tuple(String s, double d) {
	super();
	this.s = s;
	this.d = d;
    }
    
    @Override
    public int compareTo(Tuple t2) {
	if (t2.d > this.d)
	    return -1;
	if (this.d > t2.d)
	    return 1;
	return 0;
    }
    


    

}
