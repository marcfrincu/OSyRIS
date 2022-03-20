package osyris.samples;

import osyris.workflow.OSyRISwf;

/**
 * Sample class showing how we can extend the standard OSyRISwf class
 * 
 * @author Marc Frincu
 * @since 2009
 */
public class MyOSyRISwf extends OSyRISwf {

	public MyOSyRISwf(String rulefile, String settingsFile, String parrentWfID, boolean fromDB) throws Exception {
		super(rulefile, settingsFile, parrentWfID, fromDB);
	}

	// This workflow does not implement any functionality distinct from the
	// standard OSyRIS engine but it could.
	// If this is the case just add your own methods here
}
