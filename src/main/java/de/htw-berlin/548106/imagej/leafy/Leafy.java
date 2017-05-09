
import java.io.File;

import net.imagej.Dataset;
import net.imagej.ImageJ;

/** Loads and displays a dataset using the ImageJ API. */
public class Leafy {

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();

        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");

        // load the dataset
        final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

        // display the dataset
        ij.ui().show(dataset);
    }

}
