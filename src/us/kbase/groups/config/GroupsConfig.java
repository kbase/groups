package us.kbase.groups.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.ini4j.Ini;
import org.productivity.java.syslog4j.SyslogIF;

import com.google.common.base.Optional;

import us.kbase.groups.service.SLF4JAutoLogger;
import us.kbase.groups.util.FileOpener;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.JsonServerSyslog.RpcInfo;
import us.kbase.common.service.JsonServerSyslog.SyslogOutput;

/** A configuration for the Groups software package. Loads the configuration from
 * the ini file section "groups" with the keys
 * 
 * <pre>
 * mongo-host
 * mongo-db
 * mongo-user
 * mongo-pwd
 * auth-url
 * workspace-url
 * dont-trust-x-ip-headers
 * </pre>
 * 
 * The last key is optional and instructs the server to ignore the X-Real-IP and X-Forwarded-For
 * headers if set to {@link #TRUE}.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class GroupsConfig {
	
	// we may want different configuration implementations for different environments. YAGNI for now.
	
	public static final String ENV_VAR_KB_DEP = "KB_DEPLOYMENT_CONFIG";
	
	private static final String LOG_NAME = "GroupsService";
	
	private static final String CFG_LOC = "groups";
	private static final String TEMP_KEY_CFG_FILE = "temp-key-config-file";
	
	private static final String KEY_MONGO_HOST = "mongo-host";
	private static final String KEY_MONGO_DB = "mongo-db";
	private static final String KEY_MONGO_USER = "mongo-user";
	private static final String KEY_MONGO_PWD = "mongo-pwd";
	private static final String KEY_AUTH_URL = "auth-url";
	private static final String KEY_WORKSPACE_URL = "workspace-url";
	private static final String KEY_IGNORE_IP_HEADERS = "dont-trust-x-ip-headers";
	
	public static final String TRUE = "true";
	
	private final String mongoHost;
	private final String mongoDB;
	private final Optional<String> mongoUser;
	private final Optional<char[]> mongoPwd;
	private final URL authURL;
	private final URL workspaceURL;
	private final SLF4JAutoLogger logger;
	private final boolean ignoreIPHeaders;

	/** Create a new configuration.
	 * 
	 * Loads the configuration from an ini file specified by the environmental variable
	 * {@link #ENV_VAR_KB_DEP}.
	 * The JVM system properties take precedence over environmental variables if both are
	 * present for a given key.
	 * @throws GroupsConfigurationException if the configuration is invalid.
	 */
	public GroupsConfig() throws GroupsConfigurationException {
		this(new FileOpener());
	}
	
	// for tests
	private GroupsConfig(final FileOpener fileOpener)
			throws GroupsConfigurationException {
		this(getConfigPathFromEnv(), false, fileOpener);
	}
	
	/** Create a new configuration.
	 *
	 * @param filepath the path to the ini file containing the configuration.
	 * @param nullLogger true to create a silent logger implementation.
	 * @throws GroupsConfigurationException if the configuration is invalid.
	 */
	public GroupsConfig(final Path filepath, final boolean nullLogger) 
			throws GroupsConfigurationException {
		this(filepath, nullLogger, new FileOpener());
	}
	
	// for tests
	private GroupsConfig(
			final Path filepath,
			final boolean nullLogger,
			final FileOpener fileOpener)
			throws GroupsConfigurationException {
		// the logger is configured in in the configuration class so that alternate environments with different configuration mechanisms can configure their own logger
		if (nullLogger) {
			logger = new NullLogger();
		} else {
			// may want to allow configuring the logger name, but YAGNI
			logger = new JsonServerSysLogAutoLogger(new JsonServerSyslog(LOG_NAME,
					//TODO KBASECOMMON allow null for the fake config prop arg
					"thisisafakekeythatshouldntexistihope",
					JsonServerSyslog.LOG_LEVEL_INFO, true));
		}
		final Map<String, String> cfg = getConfig(filepath, fileOpener);
		ignoreIPHeaders = TRUE.equals(getString(KEY_IGNORE_IP_HEADERS, cfg));
		authURL = getURL(KEY_AUTH_URL, cfg);
		workspaceURL = getURL(KEY_WORKSPACE_URL, cfg);
		mongoHost = getString(KEY_MONGO_HOST, cfg, true);
		mongoDB = getString(KEY_MONGO_DB, cfg, true);
		mongoUser = Optional.fromNullable(getString(KEY_MONGO_USER, cfg));
		Optional<String> mongop = Optional.fromNullable(getString(KEY_MONGO_PWD, cfg));
		if (mongoUser.isPresent() ^ mongop.isPresent()) {
			mongop = null; //GC
			throw new GroupsConfigurationException(String.format(
					"Must provide both %s and %s params in config file " +
					"%s section %s if MongoDB authentication is to be used",
					KEY_MONGO_USER, KEY_MONGO_PWD, cfg.get(TEMP_KEY_CFG_FILE), CFG_LOC));
		}
		mongoPwd = mongop.isPresent() ?
				Optional.of(mongop.get().toCharArray()) : Optional.absent();
		mongop = null; //GC
	}
	
	// returns null if no string
	private String getString(
			final String paramName,
			final Map<String, String> config)
			throws GroupsConfigurationException {
		return getString(paramName, config, false);
	}
	
	private String getString(
			final String paramName,
			final Map<String, String> config,
			final boolean except)
			throws GroupsConfigurationException {
		final String s = config.get(paramName);
		if (s != null && !s.trim().isEmpty()) {
			return s.trim();
		} else if (except) {
			throw new GroupsConfigurationException(String.format(
					"Required parameter %s not provided in configuration file %s, section %s",
					paramName, config.get(TEMP_KEY_CFG_FILE), CFG_LOC));
		} else {
			return null;
		}
	}
	
	private URL getURL(final String key, final Map<String, String> cfg)
			throws GroupsConfigurationException {
		final String url = getString(key, cfg, true);
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new GroupsConfigurationException(String.format(
					"Value %s of parameter %s in section %s of config " +
					"file %s is not a valid URL",
					url, key, CFG_LOC, cfg.get(TEMP_KEY_CFG_FILE)));
		}
	}
	
	private static Path getConfigPathFromEnv()
			throws GroupsConfigurationException {
		final String file = System.getProperty(ENV_VAR_KB_DEP) == null ?
					System.getenv(ENV_VAR_KB_DEP) : System.getProperty(ENV_VAR_KB_DEP);
		if (file == null || file.trim().isEmpty()) {
			throw new GroupsConfigurationException(String.format(
					"Could not find deployment configuration file from the " +
					"environment variable / system property %s",
					ENV_VAR_KB_DEP));
		}
		return Paths.get(file);
	}
	
	private Map<String, String> getConfig(final Path file, final FileOpener fileOpener)
			throws GroupsConfigurationException {
		final Ini ini;
		try (final InputStream is = fileOpener.open(file)){
			ini = new Ini(is);
		} catch (IOException ioe) {
			throw new GroupsConfigurationException(String.format(
					"Could not read configuration file %s: %s",
					file, ioe.getMessage()), ioe);
		}
		final Map<String, String> config = ini.get(CFG_LOC);
		if (config == null) {
			throw new GroupsConfigurationException(String.format(
					"No section %s in config file %s", CFG_LOC, file));
		}
		config.put(TEMP_KEY_CFG_FILE, file.toString());
		return config;
	}
	
	private static class NullLogger implements SLF4JAutoLogger {

		@Override
		public void setCallInfo(String method, String id, String ipAddress) {
			//  do nothing
		}

		@Override
		public String getCallID() {
			return null;
		}
	}
	
	// this is just too much of a pain to test, and testing manually is trivial.
	private static class JsonServerSysLogAutoLogger implements SLF4JAutoLogger {
		
		@SuppressWarnings("unused")
		private JsonServerSyslog logger; // keep a reference to avoid gc

		private JsonServerSysLogAutoLogger(final JsonServerSyslog logger) {
			super();
			this.logger = logger;
			logger.changeOutput(new SyslogOutput() {
				
				@Override
				public void logToSystem(
						final SyslogIF log,
						final int level,
						final String message) {
					System.out.println(message);
				}
				
			});
		}

		@Override
		public void setCallInfo(
				final String method,
				final String id,
				final String ipAddress) {
			final RpcInfo rpc = JsonServerSyslog.getCurrentRpcInfo();
			rpc.setId(id);
			rpc.setIp(ipAddress);
			rpc.setMethod(method);
		}

		@Override
		public String getCallID() {
			return JsonServerSyslog.getCurrentRpcInfo().getId();
		}
	}
	
	/** Get the MongoDB host, including the port if any.
	 * @return the host.
	 */
	public String getMongoHost() {
		return mongoHost;
	}

	/** Ge the MongoDB database to use.
	 * @return the database.
	 */
	public String getMongoDatabase() {
		return mongoDB;
	}

	/** Get the MongoDB user name, if any. If provided a password will also be provided.
	 * @return the user name
	 */
	public Optional<String> getMongoUser() {
		return mongoUser;
	}

	/** Get the MongoDB password, if any. If provided a user name will also be provided.
	 * @return the password.
	 */
	public Optional<char[]> getMongoPwd() {
		return mongoPwd;
	}
	
	/** Get the root url of the KBase authentication service.
	 * @return the url
	 */
	public URL getAuthURL() {
		return authURL;
	}
	
	/** Get the root url of the KBase workspace service.
	 * @return the url
	 */
	public URL getWorkspaceURL() {
		return workspaceURL;
	}
	
	/** Get a logger. The logger is expected to intercept SLF4J log events and log them
	 * appropriately. A reference to the logger must be maintained so that it is not garbage
	 * collected.
	 * @return the logger.
	 */
	public SLF4JAutoLogger getLogger() {
		return logger;
	}
	
	/** True if the X-Real-IP and X-Forwarded-For headers should be ignored.
	 * @return true to ignore IP headers.
	 */
	public boolean isIgnoreIPHeaders() {
		return ignoreIPHeaders;
	}
	
}
