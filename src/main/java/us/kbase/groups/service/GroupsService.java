package us.kbase.groups.service;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import us.kbase.groups.build.GroupsBuilder;
import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.config.GroupsConfigurationException;
import us.kbase.groups.core.Groups;
import us.kbase.groups.service.exceptions.ExceptionHandler;
import us.kbase.groups.storage.exceptions.StorageInitException;

public class GroupsService extends ResourceConfig {
	
	//TODO TEST
	//TODO JAVADOC
	
	private static MongoClient mc;
	@SuppressWarnings("unused")
	private final SLF4JAutoLogger logger; //keep a reference to prevent GC
	
	public GroupsService()
			throws StorageInitException, GroupsConfigurationException {
		//TODO ZLATER CONFIG Get the class name from environment & load if we need alternate config mechanism
		final GroupsConfig cfg = new GroupsConfig();
		
		quietLogger();
		logger = cfg.getLogger();
		try {
			buildApp(cfg);
		} catch (StorageInitException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to initialize storage engine: " + e.getMessage(), e);
			throw e;
		}
	}

	private void quietLogger() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
				.setLevel(Level.INFO);
	}

	private void buildApp(
			final GroupsConfig c)
			throws StorageInitException, GroupsConfigurationException {
		final GroupsBuilder gb;
		synchronized(this) {
			if (mc == null) {
				gb = new GroupsBuilder(c);
				mc = gb.getMongoClient();
			} else {
				gb = new GroupsBuilder(c, mc);
			}
		}
		packages("us.kbase.groups.service.api");
		register(JacksonFeature.class);
		register(LoggingFilter.class);
		register(ExceptionHandler.class);
		final Groups g = gb.getGroups();
		register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(c).to(GroupsConfig.class);
				bind(g).to(Groups.class);
				bind(c.getLogger()).to(SLF4JAutoLogger.class);
			}
		});
	}
	
	static void shutdown() {
		mc.close();
	}
}
