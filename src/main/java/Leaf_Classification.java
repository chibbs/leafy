
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Leaf_Classification
    implements PlugInFilter
{

    @Override
    public int setup( String arg, ImagePlus imp )
    {
        return DOES_8G; // this plugin accepts 8-bit-grayscale images
    }

    @Override
    public void run( ImageProcessor ip )
    {
        int M = ip.getWidth();
        int N = ip.getHeight();
        
        // iterate
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                int p = ip.getPixel( u, v );
                ip.putPixel( u, v, 255-p );
            }
        }

    }
    

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Leaf_Classification.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		//String pluginsDir = "C:/Users/Laura/Desktop/ImageJ.app/plugins/";
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/Tree_Rings.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
