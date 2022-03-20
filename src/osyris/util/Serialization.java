package osyris.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class holds methods for converting Java objects to byte arrays and vice versa
 * @author Marc Frincu
 * @since 2010
 *
 */
public class Serialization {

	/**
	 * Converts a Java object to a byte array
	 * @param obj the Java object
	 * @return the byte array
	 * @throws java.io.IOException
	 */
	public static byte[] getBytes(Object obj) throws java.io.IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
	
		oos.writeObject(obj);
		
		oos.flush();
		oos.close();
		bos.close();
		
		return bos.toByteArray();
	}
	
	/**
	 * Converts a byte array to a Java object.
	 * @param bytea the byte array
	 * @return the Java object
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static Object getObject(byte[] bytea) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytea);
		ObjectInputStream ois = new ObjectInputStream(bis);
		 
		final Object object = ois.readObject();
		 
		bis.close();
		ois.close();
		 
		return object;
	}
	
	/**
	 * Writes a Java object to a given file
	 * @param object the Java object
	 * @param filePath the file path
	 * @param wfId the workflow ID
	 * @throws IOException
	 */
	public static void toFile(Object object, String filePath, String wfId) throws IOException {
		File outFile = new File(filePath + File.pathSeparator + wfId + ".tmp");
		FileOutputStream fout = new FileOutputStream(outFile);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(object);
		oos.close();
	}
	
	/**
	 * Reads a Java object from a given file
	 * @param filePath the file path
	 * @param wfId the workflow ID
	 * @return the Java object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object fromFile(String filePath, String wfId) throws IOException, 
																	ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filePath + File.pathSeparator + wfId + ".tmp");
		
		ObjectInputStream ois = new ObjectInputStream(fis);
		final Object object = ois.readObject();
		ois.close();
		
		return object;
	}
}
