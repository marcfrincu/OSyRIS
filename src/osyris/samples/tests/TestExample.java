package osyris.samples.tests;
import osyris.samples.MyOSyRISwf;

/**
 * This class is used to test the MyOSyRISwf workflow engine as run from within
 * a command line application
 * 
 * @author Marc Frincu
 * @since 2009
 */
public class TestExample {

	/**
	 * Main entry point in the application
	 * 
	 * @param args
	 *            the list of arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.out.println("Invalid number of parameters. Usage:\n");
			System.out
					.println("osyris.workflow.MyOSyRISwf ruleFileNameAndPath settingsFileNameAndPath parentWorkflowId fromDB");
		} else {
			// create a new OSyRIS workflow instance
			MyOSyRISwf workflow = new MyOSyRISwf(args[0], args[1], args[2], Boolean.parseBoolean(args[3]));
			// and execute it
			workflow.execute();
		}
	}
}
