package osyris.util.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Class handling the <i>system.properties</i> file for the OSyRIS engine
 * 
 * @author Marc Frincu
 * @since 2010
 */
public class SystemSettings {

	public static void main(String args[]) {
		SystemSettings settings = SystemSettings.getSystemSettings();
		settings.loadProperties("system.properties");
	}

	/**
	 * The keys of the properties in the system.properties file.
	 */
	private enum PropertyKeys {
		database, 
		username, 
		password, 
		silk_file_location, 
		drl_file_location, 
		log_file_location, 
		executor_class, 
		executor_timeout_limit, 
		fire_parallel_rules, 
		engine_timeout_limit, 
		mq_username,
		mq_password,
		mq_virtual_host,
		mq_host_name,
		mq_port_number,
		drl_package_name,
		container_solution_name,
		mq_msg_batch_size
	};

	private String database, username, password, silkFileLocation,
			drlFileLocation, logFileLocation, executorClass, mq_username, 
			mq_password, mq_virtual_host, mq_host_name,
			drlPackageName, container_solution_name;

	private long executorTimeoutLimit, engineTimeoutLimit;
	private int  mq_port_number,mq_msg_batch_size;
	
	private boolean fireParallelRules, brokerQueueIsTransacted;

	public int getMq_msg_batch_size() {
		return this.mq_msg_batch_size;
	}
	
	public String getContainer_solution_name() {
		return this.container_solution_name;
	}
	
	public String getDrlPackageName() {
		return this.drlPackageName;
	}
	
	public String getMq_username() {
		return mq_username;
	}

	public String getMq_password() {
		return mq_password;
	}

	public String getMq_virtual_host() {
		return mq_virtual_host;
	}

	public String getMq_host_name() {
		return mq_host_name;
	}

	public int getMq_port_number() {
		return mq_port_number;
	}

	public boolean getIsMessageQueueTransacted() {
		return this.brokerQueueIsTransacted;
	}

	public String getDatabase() {
		return this.database;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getSilkFileLocation() {
		return this.silkFileLocation;
	}

	public String getDrlFileLocation() {
		return this.drlFileLocation;
	}

	public String getLogFileLocation() {
		return this.logFileLocation;
	}

	public String getExecutorClass() {
		return this.executorClass;
	}

	public long getExecutorTimeout() {
		return this.executorTimeoutLimit;
	}

	public long getEngineTimeoutLimit() {
		return this.engineTimeoutLimit;
	}

	public boolean getFireParallelRules() {
		return this.fireParallelRules;
	}

	/**
	 * The system settings object.
	 */
	private static SystemSettings settings = null;

	/**
	 * Private constructor.
	 */
	private SystemSettings() {
	}

	/**
	 * Returns the system settings object.
	 * <p>
	 * 
	 * @return the system settings object
	 */
	public static SystemSettings getSystemSettings() {
		if (settings == null) {
			settings = new SystemSettings();
		}
		return settings;
	}

	/**
	 * This method loads the properties from the property file
	 * "system.properties" previously added to the class path either explicit or
	 * implicit by being part of a .jar file added to the CLASSPATH.
	 * <p>
	 * <i>Called from SAPSCIEnce and SAPlatform main methods</i>
	 * 
	 * @param propertiesFilePath
	 *            path to properties file
	 */
	public void loadProperties(String propertiesFilePath) {

		Properties props = new Properties();

		// URL url = SystemSettings.class.getClassLoader().getResource(
		// propertiesFilePath);
		// if (url == null) {
		// throw new MissingResourceException(
		// "Unable to load the properties file." + " File not found: "
		// + url, null, null);
		// }
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(propertiesFilePath));
		} catch (IOException ioe) {
			throw new MissingResourceException(
					"Unable to load the properties file." + " File not found: "
							+ propertiesFilePath, null, null);
		}

		try {
			props.load(br);
		} catch (IOException e) {
			throw new MissingResourceException(
					"The properties file cannot be accessed.", null, null);
		}

		validateFile(props);

		this.database = props.getProperty(PropertyKeys.database.toString());
		this.username = props.getProperty(PropertyKeys.username.toString());
		this.password = props.getProperty(PropertyKeys.password.toString());
		this.silkFileLocation = props
				.getProperty(PropertyKeys.silk_file_location.toString());
		this.drlFileLocation = props.getProperty(PropertyKeys.drl_file_location
				.toString());
		this.logFileLocation = props.getProperty(PropertyKeys.log_file_location
				.toString());
		this.executorClass = props.getProperty(PropertyKeys.executor_class
				.toString());
		this.fireParallelRules = Boolean.parseBoolean(props
				.getProperty(PropertyKeys.fire_parallel_rules.toString()));
		this.executorTimeoutLimit = Long.parseLong(props
				.getProperty(PropertyKeys.executor_timeout_limit.toString()));
		this.engineTimeoutLimit = Long.parseLong(props
				.getProperty(PropertyKeys.engine_timeout_limit.toString()));
		this.mq_virtual_host = props.getProperty(
				PropertyKeys.mq_virtual_host.toString());
		this.mq_username = props.getProperty(
				PropertyKeys.mq_username.toString());
		this.mq_password = props.getProperty(
				PropertyKeys.mq_password.toString());
		this.mq_host_name = props.getProperty(
				PropertyKeys.mq_host_name.toString());
		this.mq_port_number = Integer.parseInt(props.getProperty(
				PropertyKeys.mq_port_number.toString()));
		this.mq_msg_batch_size = Integer.parseInt(props.getProperty(
				PropertyKeys.mq_msg_batch_size.toString()));
		this.drlPackageName = props.getProperty(PropertyKeys.drl_package_name.toString());
		this.container_solution_name = props.getProperty(PropertyKeys.container_solution_name.toString());
	}

	/**
	 * Logs an error message and throws a MissingResourceException.
	 * <p>
	 * 
	 * @param mssg
	 *            the message
	 * @param e
	 *            any exception that may have caused the error
	 */
	private void error(String mssg, Exception e) {
		Logger logger = Logger.getLogger(SystemSettings.class.getPackage()
				.getName());

		logger.error(mssg, e);
		throw new MissingResourceException(mssg, null, null);
	}

	/**
	 * Validates the properties file.
	 * <p>
	 * 
	 * @param props
	 *            the properties object
	 * @return <code>true</code> if all entries are valid, <code>false</code>
	 *         otherwise
	 */
	private boolean validateFile(Properties props) {
		String loc = SystemSettings.class.getSimpleName()
				+ ".validateFile() - ";

		String keyName, keyValue;

		PropertyKeys[] properties = PropertyKeys.values();
		for (PropertyKeys pk : properties) {
			keyName = pk.name();
			keyValue = props.getProperty(keyName);
			if (((keyValue == null) || (keyValue.trim().compareTo("") == 0))) {
				error(loc + "Missing or illegal value in settings file"
						+ " for key: " + keyName, null);
				return false;
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		throw new UnsupportedOperationException();
	}
}
