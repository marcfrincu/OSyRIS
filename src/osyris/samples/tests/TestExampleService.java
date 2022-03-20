package osyris.samples.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

/**
 * This class is used to test the MyOSyRISwf workflow engine example run as a
 * service
 * 
 * @author Marc Frincu
 * @since 2008
 * 
 */
public class TestExampleService {

	static String SILKfilename = "silk/test.silk";
	static String serviceURL = "http://localhost:8080/axis/osyris/OSyRISWorkflowExample.jws";

	/**
	 * The main entry point in the application
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		final String data = TestExampleService
				.getCmdsFromFile(TestExampleService.SILKfilename);

		// Send the SILK workflow to the service
		final String id = TestExampleService.callService(
				TestExampleService.serviceURL, data);
		System.out.println("Workflow id: " + id);

		// Additional functionality for retrieving the result or status of the
		// workflow can be implemented in the same way
	}

	/**
	 * This method is responsible for calling the OSyRIS Web Service example
	 * 
	 * @param endpoint
	 *            the service URL
	 * @param data
	 *            the rules
	 * @return the result of the invocation
	 * @throws Exception
	 */
	public static String callService(String endpoint, String data)
			throws Exception {
		String method = "call";
		Service service = new Service();

		Call call = (Call) service.createCall();
		call.setTargetEndpointAddress(new java.net.URL(endpoint));
		call.setOperationName(method);
		call.addParameter("xml", XMLType.XSD_STRING, ParameterMode.IN);
		call.setReturnType(XMLType.XSD_STRING);

		Object parameters[] = new Object[] { data };

		return (String) call.invoke(parameters);
	}

	/**
	 * Reads a command file and returns its contents.
	 * 
	 * @param filename
	 *            the SILK filename
	 * @return the file content
	 * @throws IOException
	 */
	private static String getCmdsFromFile(String filename) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(filename));
		StringBuilder fileContent = new StringBuilder();

		try {
			String line = null;
			/*
			 * readLine returns the content of a line MINUS the newline. it
			 * returns null only for the END of the stream. it returns an empty
			 * String if two newlines appear in a row.
			 */
			while ((line = input.readLine()) != null) {
				fileContent.append(line);
				fileContent.append(System.getProperty("line.separator"));
			}

		} finally {
			input.close();
		}
		return fileContent.toString();
	}
}
