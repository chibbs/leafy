package net.larla.leafy.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class FileHelper {

    public static String map2String (HashMap<String, String> myMap) {
	String eol = System.getProperty("line.separator");
	StringBuilder builder = new StringBuilder();
	for (Map.Entry<String, String> kvp : myMap.entrySet()) {
	    builder.append(kvp.getKey());
	    builder.append(";");
	    builder.append(kvp.getValue());
	    //builder.append("\r\n");
	    builder.append(eol);
	}

	String content = builder.toString().trim();
	return content;
    }

    public static void writeToFile(String text, String path) {
	try{
	    PrintWriter writer = new PrintWriter(path, "UTF-8");
	    writer.println(text);
	    writer.close();
	} catch (IOException e) {
	    // do something
	}
    }

    public static HashMap<String, String> file2Map(String path) {
	HashMap<String, String> result = new HashMap<String, String>();

	FileReader fr;
	BufferedReader br = null;
	try {
	    fr = new FileReader(path);
	    br = new BufferedReader(fr);
	    String zeile = br.readLine();
	    while( zeile != null )
	    {
		String[] parts = zeile.split(";");
		result.put(parts[0], parts[1]);

		zeile = br.readLine();
	    }

	    br.close();

	} catch (FileNotFoundException e) {

	} catch (IOException e) {

	} 


	return result;
    }

    public static boolean checkDir(String dirname) {
	// https://www.java-forum.org/thema/wie-kann-ich-schauen-ob-ein-ordner-vorhanden-ist.568/
	File reviewdir = new File(dirname);
	if (reviewdir.exists())    // Überprüfen, ob es den Ordner gibt
	{
	    return true;
	}
	else
	{
	    if (reviewdir.mkdir())    // Erstellen des Ordners
	    {
		return true;
	    }
	    else
	    {
		return false;
	    }
	}
    }

}
