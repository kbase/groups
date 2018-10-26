package us.kbase.groups.build;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.config.GroupsConfigurationException;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.UserHandler;
import us.kbase.groups.core.WorkspaceHandler;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;
import us.kbase.groups.notifications.SLF4JNotifier;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.StorageInitException;
import us.kbase.groups.storage.mongo.MongoGroupsStorage;
import us.kbase.groups.userhandler.KBaseUserHandler;
import us.kbase.groups.workspaceHandler.SDKClientWorkspaceHandler;
import us.kbase.workspace.WorkspaceClient;

/** A class for building a {@link Groups} instance given a {@link GroupsConfig}
 * configuration instance.
 * @author gaprice@lbl.gov
 *
 */
public class GroupsBuilder {

	//TODO TEST
	
	private final MongoClient mc;
	private final Groups groups;
	private final GroupsStorage storage;
	
	/** Build a groups instance.
	 * @param cfg the configuration to build to.
	 * @throws StorageInitException if the storage system could not be initialized.
	 * @throws GroupsConfigurationException if the application could not be built from the 
	 * configuration.
	 */
	public GroupsBuilder(final GroupsConfig cfg)
			throws StorageInitException, GroupsConfigurationException {
		checkNotNull(cfg, "cfg");
		mc = buildMongo(cfg);
		storage = buildStorage(cfg, mc);
		groups = buildGroups(cfg, storage);
	}
	
	/** Build a groups instance with a previously existing MongoDB client. MongoDB
	 * recommends creating only one client per process. The client must have been retrieved
	 * from {@link #getMongoClient()}.
	 * @param cfg the configuration to build to.
	 * @param mc the MongoDB client.
	 * @throws StorageInitException if the storage system could not be initialized.
	 * @throws GroupsConfigurationException if the application could not be built from the 
	 * configuration.
	 */
	public GroupsBuilder(final GroupsConfig cfg, final MongoClient mc)
			throws StorageInitException, GroupsConfigurationException {
		checkNotNull(cfg, "cfg");
		checkNotNull(mc, "mc");
		this.mc = mc;
		storage = buildStorage(cfg, mc);
		groups = buildGroups(cfg, storage);
	}
	
	private MongoClient buildMongo(final GroupsConfig c) throws StorageInitException {
		//TODO ZLATER MONGO handle shards & replica sets
		try {
			if (c.getMongoUser().isPresent()) {
				final List<MongoCredential> creds = Arrays.asList(MongoCredential.createCredential(
						c.getMongoUser().get(), c.getMongoDatabase(), c.getMongoPwd().get()));
				// unclear if and when it's safe to clear the password
				return new MongoClient(new ServerAddress(c.getMongoHost()), creds);
			} else {
				return new MongoClient(new ServerAddress(c.getMongoHost()));
			}
		} catch (MongoException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to connect to MongoDB: " + e.getMessage(), e);
			throw new StorageInitException("Failed to connect to MongoDB: " + e.getMessage(), e);
		}
	}
	
	private Groups buildGroups(final GroupsConfig c, final GroupsStorage storage)
			throws StorageInitException, GroupsConfigurationException {
		final UserHandler uh;
		final WorkspaceHandler wh;
		// these may need changes if we want to allow alternate implementations. YAGNI for now.
		try {
			uh = new KBaseUserHandler(
					c.getAuthURL(), c.getWorkspaceAdminToken(), c.isAllowInsecureURLs());
		} catch (IOException | URISyntaxException | AuthenticationException e) {
			throw new GroupsConfigurationException(
					"Failed to create KBase user handler for auth service: " + e.getMessage(), e);
		}
		
		try {
			final WorkspaceClient client = new WorkspaceClient(
					c.getWorkspaceURL(),
					new AuthToken(c.getWorkspaceAdminToken().getToken(), "<fake>"));
			client.setIsInsecureHttpConnectionAllowed(c.isAllowInsecureURLs());
			wh = new SDKClientWorkspaceHandler(client);
		} catch (IOException | UnauthorizedException | WorkspaceHandlerException e) {
			throw new GroupsConfigurationException(
					"Failed to create workspace handler: " + e.getMessage(), e);
		}
		// TODO NOTIFICATIONS replace with actual implementation
		return new Groups(storage, uh, wh, new SLF4JNotifier());
	}

	private GroupsStorage buildStorage(
			final GroupsConfig c,
			final MongoClient mc)
			throws StorageInitException {
		final MongoDatabase db;
		try {
			db = mc.getDatabase(c.getMongoDatabase());
		} catch (MongoException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to get database from MongoDB: " + e.getMessage(), e);
			throw new StorageInitException("Failed to get database from MongoDB: " +
					e.getMessage(), e);
		}
		//TODO TEST authenticate to db, write actual test with authentication
		return new MongoGroupsStorage(db);
	}
	
	/** Get the mongo client associated with the groups instance.
	 * @return the mongo client.
	 */
	public MongoClient getMongoClient() {
		return mc;
	}

	/** Get the groups instance.
	 * @return the groups instance.
	 */
	public Groups getGroups() {
		return groups;
	}
	
	/** Get the storage system for the groups instance.
	 * @return the storage system.
	 */
	public GroupsStorage getStorage() {
		return storage;
	}
}
