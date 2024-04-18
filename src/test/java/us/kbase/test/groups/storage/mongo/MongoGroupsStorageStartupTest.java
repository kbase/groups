package us.kbase.test.groups.storage.mongo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.exceptions.StorageInitException;
import us.kbase.groups.storage.mongo.MongoGroupsStorage;
import us.kbase.test.groups.MongoStorageTestManager;
import us.kbase.test.groups.TestCommon;

public class MongoGroupsStorageStartupTest {
	
	private static MongoStorageTestManager manager;

	@BeforeClass
	public static void setUp() throws Exception {
		manager = new MongoStorageTestManager("test_mongogroupsstorage");
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (manager != null) {
			manager.destroy();
		}
	}
	
	@Before
	public void clearDB() throws Exception {
		manager.reset();
	}
	
	
	@Test
	public void failConstructNulls() throws Exception {
		failMongoStart(null, set(), new NullPointerException("db"));
		failMongoStart(manager.db, null, new NullPointerException("types"));
		failMongoStart(manager.db, set(new ResourceType("t"), null), new NullPointerException(
				"Null item in collection types"));
	}
	@Test
	public void startUpAndCheckConfigDoc() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpAndCheckConfigDoc");
		new MongoGroupsStorage(db, set());
		final MongoCollection<Document> col = db.getCollection("config");
		assertThat("Only one config doc", col.countDocuments(), is(1L));
		final FindIterable<Document> c = col.find();
		final Document d = c.first();
		
		assertThat("correct config key & value", (String)d.get("schema"), is("schema"));
		assertThat("not in update", (Boolean)d.get("inupdate"), is(false));
		assertThat("schema v1", (Integer)d.get("schemaver"), is(1));
		
		//check startup works with the config object in place
		final MongoGroupsStorage ms = new MongoGroupsStorage(db, set());
		
		final GroupUser u = GroupUser.getBuilder(new UserName("u"), Instant.ofEpochMilli(10000))
				.build();
		ms.createGroup(Group.getBuilder(new GroupID("id"), new GroupName("name"), u,
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		assertThat("incorrect group", ms.getGroup(new GroupID("id")), is(
				Group.getBuilder(new GroupID("id"), new GroupName("name"), u,
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.build()));
	}
	
	@Test
	public void startUpWith2ConfigDocs() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWith2ConfigDocs");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", false)
				.append("schemaver", 1);
		
		db.getCollection("config").insertMany(Arrays.asList(m,
				// need to create a new document to create a new mongo _id
				new Document(m)));
		
		final Pattern errorPattern = Pattern.compile(
				"Failed to create index: Write failed with error code 11000 and error message " +
				"'(exception: )?E11000 duplicate key error (index|collection): " +
				"startUpWith2ConfigDocs.config( index: |\\.\\$)schema_1\\s+dup key: " +
				"\\{ : \"schema\" \\}'");
		try {
			new MongoGroupsStorage(db, set());
			fail("started mongo with bad config");
		} catch (StorageInitException e) {
			final Matcher match = errorPattern.matcher(e.getMessage());
			assertThat("exception did not match: \n" + e.getMessage(), match.matches(), is(true));
		}
	}
	
	@Test
	public void startUpWithBadSchemaVersion() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWithBadSchemaVersion");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", false)
				.append("schemaver", 4);
		
		db.getCollection("config").insertOne(m);
		
		failMongoStart(db, new StorageInitException(
				"Incompatible database schema. Server is v1, DB is v4"));
	}
	
	@Test
	public void startUpWithUpdateInProgress() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWithUpdateInProgress");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", true)
				.append("schemaver", 1);
		
		db.getCollection("config").insertOne(m);
		
		failMongoStart(db, new StorageInitException(
				"The database is in the middle of an update from v1 of the " +
				"schema. Aborting startup."));
	}
	
	private void failMongoStart(final MongoDatabase db, final Exception exp) throws Exception {
		failMongoStart(db, set(), exp);
	}
	
	private void failMongoStart(
			final MongoDatabase db,
			final Collection<ResourceType> types,
			final Exception exp) throws Exception {
		try {
			new MongoGroupsStorage(db, types);
			fail("started mongo with bad config");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exp);
		}
	}
	
	/* The following tests ensure that all indexes are created correctly. The collection names
	 * are tested so that if a new collection is added the test will fail without altering 
	 * said test, at which time the coder will hopefully read this notice and add index tests
	 * for the new collection.
	 */
	
	@Test
	public void checkCollectionNames() throws Exception {
		final Set<String> names = new HashSet<>();
		final Set<String> expected = set(
				"config",
				"requests",
				"groups");
		if (manager.includeSystemIndexes) {
			expected.add("system.indexes");
		}
		// this is annoying. MongoIterator has two forEach methods with different signatures
		// and so which one to call is ambiguous for lambda expressions.
		manager.db.listCollectionNames().forEach((Consumer<String>) names::add);
		assertThat("incorrect collection names", names, is(expected));
	}
	
	@Test
	public void indexesConfig() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("config").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		assertThat("incorrect indexes", indexes, is(set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("schema", 1))
						.append("name", "schema_1")
						.append("ns", "test_mongogroupsstorage.config"),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", "test_mongogroupsstorage.config")
				)));
	}
	
	@Test
	public void indexesGroups() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("groups").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		assertThat("incorrect indexes", indexes, is(getDefaultGroupsIndexes()));
	}

	private Set<Document> getDefaultGroupsIndexes() {
		final String col = "test_mongogroupsstorage.groups";
		return set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("id", 1))
						.append("name", "id_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("own", 1).append("id", 1))
						.append("name", "own_1_id_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("admin", 1).append("id", 1))
						.append("name", "admin_1_id_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("priv", 1).append("id", 1))
						.append("name", "priv_1_id_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("memb.user", 1).append("id", 1))
						.append("name", "memb.user_1_id_1")
						.append("ns", col)
				);
	}
	
	//TODO NOW check indexes are getting used
	@Test
	public void indexesGroupsWithResourceTypes() throws Exception {
		manager.db.drop();
		try {
			new MongoGroupsStorage(
					manager.db, Arrays.asList(new ResourceType("t1"), new ResourceType("t2")));
			final Set<Document> indexes = new HashSet<>();
			manager.db.getCollection("groups").listIndexes()
					.forEach((Consumer<Document>) indexes::add);
			
			final String col = "test_mongogroupsstorage.groups";
			final Set<Document> expected = getDefaultGroupsIndexes();
			for (final String t: set("t1", "t2")) {
				final String resKey = "resources." + t + ".rid";
				expected.addAll(set(
						new Document("v", manager.indexVer)
								.append("key", new Document(resKey, 1)
										.append("own", 1)
										.append("id", 1))
								.append("name", resKey + "_1_own_1_id_1")
								.append("ns", col),
						new Document("v", manager.indexVer)
								.append("key", new Document(resKey, 1)
										.append("id", 1))
								.append("name", resKey + "_1_id_1")
								.append("ns", col),
						new Document("v", manager.indexVer)
								.append("key", new Document(resKey, 1)
										.append("priv", 1)
										.append("id", 1))
								.append("name", resKey + "_1_priv_1_id_1")
								.append("ns", col)
						));
			}
			assertThat("incorrect indexes", indexes, is(expected));
		} finally {
			manager.db.drop();
		}
		
	}
	
	@Test
	public void indexesRequests() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("requests").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		final String col = "test_mongogroupsstorage.requests";
		assertThat("incorrect indexes", indexes, is(set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("id", 1))
						.append("name", "id_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("gid", 1).append("type", 1).append("mod", 1))
						.append("name", "gid_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("gid", 1)
								.append("status", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "gid_1_status_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("requester", 1).append("mod", 1))
						.append("name", "requester_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("requester", 1)
								.append("status", 1)
								.append("mod", 1))
						.append("name", "requester_1_status_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resaid", 1)
								.append("restype", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resaid_1_restype_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resaid", 1)
								.append("restype", 1)
								.append("status", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resaid_1_restype_1_status_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("restype", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_restype_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("restype", 1)
								.append("status", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_restype_1_status_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("requester", 1)
								.append("restype", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_requester_1_restype_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("requester", 1)
								.append("restype", 1)
								.append("status", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_requester_1_restype_1_status_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("gid", 1)
								.append("restype", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_gid_1_restype_1_type_1_mod_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("resrid", 1)
								.append("gid", 1)
								.append("restype", 1)
								.append("status", 1)
								.append("type", 1)
								.append("mod", 1))
						.append("name", "resrid_1_gid_1_restype_1_status_1_type_1_mod_1")
						.append("ns", col),		
						
				new Document("v", manager.indexVer)
						.append("key", new Document("expire", 1))
						.append("name", "expire_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("sparse", true)
						.append("key", new Document("charstr", 1))
						.append("name", "charstr_1")
						.append("ns", col),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", col)
				)));
	}
}
