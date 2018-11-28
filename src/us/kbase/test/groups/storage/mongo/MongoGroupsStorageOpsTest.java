package us.kbase.test.groups.storage.mongo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;
import static us.kbase.test.groups.TestCommon.assertLogEventsCorrect;
import static us.kbase.test.groups.TestCommon.inst;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequest.Builder;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;
import us.kbase.groups.storage.mongo.MongoGroupsStorage;
import us.kbase.test.groups.TestCommon.LogEvent;
import us.kbase.test.groups.MongoStorageTestManager;
import us.kbase.test.groups.TestCommon;

public class MongoGroupsStorageOpsTest {

	private static MongoStorageTestManager manager;
	private static List<ILoggingEvent> logEvents;
	private static Path TEMP_DIR;
	private static Path EMPTY_FILE_MSH;
	
	@BeforeClass
	public static void setUp() throws Exception {
		logEvents = TestCommon.setUpSLF4JTestLoggerAppender("us.kbase.groups");
		manager = new MongoStorageTestManager("test_mongogroupsstorage");
		TEMP_DIR = TestCommon.getTempDir().resolve("StorageTest_" + UUID.randomUUID().toString());
		Files.createDirectories(TEMP_DIR);
		EMPTY_FILE_MSH = TEMP_DIR.resolve(UUID.randomUUID().toString() + ".msh");
		Files.createFile(EMPTY_FILE_MSH);
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (manager != null) {
			manager.destroy();
		}
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void before() throws Exception {
		manager.reset();
		logEvents.clear();
	}
	
	private <T> List<T> list(@SuppressWarnings("unchecked") T... items) {
		return Arrays.asList(items);
	}
	
	@Test
	public void createAndGetGroupAndExistsMinimal() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("uname"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
						.build()));
		
		assertThat("incorrect group exists", manager.storage.getGroupExists(new GroupID("gid")),
				is(true));
		assertThat("incorrect group exists", manager.storage.getGroupExists(new GroupID("gid1")),
				is(false));
	}
	
	@Test
	public void createAndGetGroupMaximal() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.PROJECT)
				.withDescription("desc")
				.withMember(new UserName("foo"))
				.withMember(new UserName("bar"))
				.withAdministrator(new UserName("a1"))
				.withAdministrator(new UserName("a3"))
				.withResource(new ResourceType("t"), new ResourceDescriptor(new ResourceID("r")))
				.withResource(new ResourceType("t"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.withResource(new ResourceType("x"), new ResourceDescriptor(new ResourceID("y")))
				.withResource(new ResourceType("x"), new ResourceDescriptor(
						new ResourceAdministrativeID("b"),
						new ResourceID("z")))
				.withCustomField(new NumberedCustomField("foo-83"), "bar")
				.withCustomField(new NumberedCustomField("whoo"), "whee")
				.build());
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("uname"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
						.withType(GroupType.PROJECT)
						.withDescription("desc")
						.withMember(new UserName("foo"))
						.withMember(new UserName("bar"))
						.withAdministrator(new UserName("a1"))
						.withAdministrator(new UserName("a3"))
						.withResource(new ResourceType("t"), new ResourceDescriptor(
								new ResourceID("r")))
						.withResource(new ResourceType("t"), new ResourceDescriptor(
								new ResourceAdministrativeID("a"),
								new ResourceID("b")))
						.withResource(new ResourceType("x"), new ResourceDescriptor(
								new ResourceID("y")))
						.withResource(new ResourceType("x"), new ResourceDescriptor(
								new ResourceAdministrativeID("b"),
								new ResourceID("z")))
						.withCustomField(new NumberedCustomField("foo-83"), "bar")
						.withCustomField(new NumberedCustomField("whoo"), "whee")
						.build()));
	}
	
	@Test
	public void createGroupFail() throws Exception {
		failCreateGroup(null, new NullPointerException("group"));
		
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		
		failCreateGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name1"), new UserName("uname1"),
				new CreateAndModTimes(Instant.ofEpochMilli(21000), Instant.ofEpochMilli(31000)))
				.build(),
				new GroupExistsException("gid"));
	}
	
	private void failCreateGroup(final Group g, final Exception expected) {
		try {
			manager.storage.createGroup(g);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupFail() throws Exception {
		failGetGroup(null, new NullPointerException("groupID"));
		
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		failGetGroup(new GroupID("gid1"), new NoSuchGroupException("gid1"));
	}
	
	private void failGetGroup(final GroupID id, final Exception expected) {
		try {
			manager.storage.getGroup(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupExistsFail() throws Exception {
		try {
			manager.storage.getGroupExists(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("groupID"));
		}
	}
	
	@Test
	public void illegalGroupDataInDB() throws Exception {
		// just test each type of exception. Not testing every possible exception that could be
		// thrown.
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		manager.db.getCollection("groups").updateOne(new Document("id", "gid"),
				new Document("$set", new Document("name", "")));
		
		failGetGroup(new GroupID("gid"), new GroupsStorageException(
				"Unexpected value in database: 30000 Missing input parameter: group name"));
		
		manager.db.getCollection("groups").updateOne(new Document("id", "gid"),
				new Document("$set", new Document("name", "foo").append("own", "a*b")));
	
		failGetGroup(new GroupID("gid"), new GroupsStorageException(
				"Unexpected value in database: 30010 Illegal user name: " +
				"Illegal character in user name a*b: *"));
		
		manager.db.getCollection("groups").updateOne(new Document("id", "gid"),
				new Document("$set", new Document("own", "a").append("type", "TEEM")));
	
		failGetGroup(new GroupID("gid"), new GroupsStorageException(
				"Unexpected value in database: No enum constant " +
				"us.kbase.groups.core.GroupType.TEEM"));
	}
	
	@Test
	public void updateGroupNoUpdate() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build();
		manager.storage.createGroup(g);
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(new GroupID("gid")).build(),
				inst(40000));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(g));
	}
	
	@Test
	public void updateGroupMinimal() throws Exception {
		// update a field at a time
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("newname")).build(),
				inst(40000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(40000)))
				.build()));
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withType(GroupType.TEAM).build(),
				inst(60000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(60000)))
				.withType(GroupType.TEAM)
				.build()));
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("my desc")).build())
				.build(),
				inst(50000)); // naughty naughty
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(50000)))
				.withType(GroupType.TEAM)
				.withDescription("my desc")
				.build()));
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove()).build())
				.build(),
				inst(567842));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(567842)))
				.withType(GroupType.TEAM)
				.build()));

		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-1"),
								StringField.from("yay!"))
						.build())
				.build(),
				inst(90000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(90000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo-1"), "yay!")
				.build()));
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-1"), StringField.remove())
						.build())
				.build(),
				inst(100000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(100000)))
				.withType(GroupType.TEAM)
				.build()));
	}
	
	@Test
	public void updateCustomFields() throws Exception {
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.build());
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-1"),
								StringField.from("valfoo"))
						.withCustomField(new NumberedCustomField("bar"),
								StringField.remove())
						.withCustomField(new NumberedCustomField("baz"),
								StringField.noAction())
						.withCustomField(new NumberedCustomField("foo-2"),
								StringField.from("valfoo2"))
						.withCustomField(new NumberedCustomField("foo-3"),
								StringField.from("valfoo3"))
						.build())
				.build(),
				inst(90000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(90000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo-1"), "valfoo")
				.withCustomField(new NumberedCustomField("foo-2"), "valfoo2")
				.withCustomField(new NumberedCustomField("foo-3"), "valfoo3")
				.build()));
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-1"),
								StringField.remove())
						.withCustomField(new NumberedCustomField("foo-3"),
								StringField.from("valfoo42"))
						.withCustomField(new NumberedCustomField("bar"),
								StringField.remove())
						.withCustomField(new NumberedCustomField("baz"),
								StringField.noAction())
						.withCustomField(new NumberedCustomField("foo-2"),
								StringField.noAction())
						.build())
				.build(),
				inst(100000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(100000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo-2"), "valfoo2")
				.withCustomField(new NumberedCustomField("foo-3"), "valfoo42")
				.build()));
	}
	
	@Test
	public void updateGroupMaximal() throws Exception {
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("my desc")
				.withCustomField(new NumberedCustomField("foo-2"), "valfoo2")
				.withCustomField(new NumberedCustomField("foo-3"), "valfoo42")
				.build());
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("newname"))
				.withType(GroupType.PROJECT)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("other desc"))
						.withCustomField(new NumberedCustomField("foo-2"), StringField.remove())
						.withCustomField(new NumberedCustomField("foo-3"), StringField.from("meh"))
						.build())
				.build(),
				inst(30001));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("newname"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30001)))
				.withType(GroupType.PROJECT)
				.withDescription("other desc")
				.withCustomField(new NumberedCustomField("foo-3"), "meh")
				.build()));
	}
	
	@Test
	public void updateGroupNoopMinimalNoAction() throws Exception {
		updateGroupNoopMinimal(StringField.noAction());
	}
	
	@Test
	public void updateGroupNoopMinimalRemove() throws Exception {
		updateGroupNoopMinimal(StringField.remove());
	}

	private void updateGroupNoopMinimal(final StringField action) throws Exception {
		// tests updating a group with identical contents. Ensure the mod date isn't set.
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.build());
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(action)
						.withCustomField(new NumberedCustomField("yay"), action)
						.build())
				.build(),
				inst(50000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.build()));
	}
	
	@Test
	public void updateGroupNoopMaximal() throws Exception {
		// tests updating a group with identical contents. Ensure the mod date isn't set.
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("my desc")
				.withCustomField(new NumberedCustomField("thatlast"), "test was a bit rude")
				.withCustomField(new NumberedCustomField("yesi-1"), "agree it was a bit")
				.build());
		
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("name"))
				.withType(GroupType.TEAM)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("my desc"))
						.withCustomField(new NumberedCustomField("thatlast"),
								StringField.from("test was a bit rude"))
						.withCustomField(new NumberedCustomField("yesi-1"),
								StringField.from("agree it was a bit"))
						.build())
				.build(),
				inst(50000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("my desc")
				.withCustomField(new NumberedCustomField("thatlast"), "test was a bit rude")
				.withCustomField(new NumberedCustomField("yesi-1"), "agree it was a bit")
				.build()));
	}
	
	@Test
	public void updateGroupSingleDifferentField() throws Exception {
		final GroupID gid = new GroupID("gid");
		manager.storage.createGroup(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("my desc")
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build());
		
		// remove desc
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("name"))
				.withType(GroupType.TEAM)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove())
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.build())
				.build(),
				inst(50000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(50000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build()));
		
		// removing again should not update the mod time
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("name"))
				.withType(GroupType.TEAM)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove())
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.build())
				.build(),
				inst(760000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(50000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build()));
		
		// new name
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("new name"))
				.withType(GroupType.TEAM)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.noAction())
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.build())
				.build(),
				inst(60000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("new name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(60000)))
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build()));
		
		// new desc
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("new name"))
				.withType(GroupType.TEAM)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("yay!"))
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.build())
				.build(),
				inst(80000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("new name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(80000)))
				.withType(GroupType.TEAM)
				.withDescription("yay!")
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build()));
		
		// new type
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("new name"))
				.withType(GroupType.PROJECT)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("yay!"))
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.build())
				.build(),
				inst(90000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("new name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(90000)))
				.withType(GroupType.PROJECT)
				.withDescription("yay!")
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build()));
		
		//new custom field
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("new name"))
				.withType(GroupType.PROJECT)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("yay!"))
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.withCustomField(new NumberedCustomField("baz"), StringField.from("bat"))
						.build())
				.build(),
				inst(100000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("new name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(100000)))
				.withType(GroupType.PROJECT)
				.withDescription("yay!")
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.withCustomField(new NumberedCustomField("baz"), "bat")
				.build()));
		
		//remove custom field
		manager.storage.updateGroup(GroupUpdateParams.getBuilder(gid)
				.withName(new GroupName("new name"))
				.withType(GroupType.PROJECT)
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("yay!"))
						.withCustomField(new NumberedCustomField("foo"), StringField.remove())
						.withCustomField(new NumberedCustomField("baz"), StringField.from("bat"))
						.build())
				.build(),
				inst(100000));
		
		assertThat("incorrect group", manager.storage.getGroup(gid), is(Group.getBuilder(
				gid, new GroupName("new name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(100000)))
				.withType(GroupType.PROJECT)
				.withDescription("yay!")
				.withCustomField(new NumberedCustomField("baz"), "bat")
				.build()));
	}
	
	@Test
	public void updateGroupFailNulls() throws Exception {
		failUpdateGroup(null, inst(1), new NullPointerException("update"));
		failUpdateGroup(GroupUpdateParams.getBuilder(new GroupID("id")).build(), null,
				new NullPointerException("modDate"));
	}
	
	@Test
	public void updateGroupFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("uname"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.build());
		
		failUpdateGroup(GroupUpdateParams.getBuilder(new GroupID("gid1"))
				.withName(new GroupName("my name")).build(),
				inst(56000), new NoSuchGroupException("gid1"));
	}

	private void failUpdateGroup(
			final GroupUpdateParams p,
			final Instant i,
			final Exception expected) {
		try {
			manager.storage.updateGroup(p, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupsEmpty() throws Exception {
		assertThat("incorrect get groups",
				manager.storage.getGroups(GetGroupsParams.getBuilder().build()),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getGroupsDefaultParams() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("aid"), new GroupName("name1"), new UserName("uname1"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(10000)))
				.withType(GroupType.PROJECT)
				.withDescription("desc1")
				.withMember(new UserName("foo1"))
				.withMember(new UserName("bar1"))
				.withAdministrator(new UserName("admin"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("42")))
				.build());
		
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("fid"), new GroupName("name2"), new UserName("uname2"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("desc2")
				.withMember(new UserName("foo2"))
				.build());
		
		assertThat("incorrect get group", manager.storage.getGroups(
				GetGroupsParams.getBuilder().build()), is(Arrays.asList(
						Group.getBuilder(
								new GroupID("aid"), new GroupName("name1"), new UserName("uname1"),
								new CreateAndModTimes(Instant.ofEpochMilli(10000),
										Instant.ofEpochMilli(10000)))
								.withType(GroupType.PROJECT)
								.withDescription("desc1")
								.withMember(new UserName("foo1"))
								.withMember(new UserName("bar1"))
								.withAdministrator(new UserName("admin"))
								.withResource(new ResourceType("workspace"),
										new ResourceDescriptor(new ResourceID("42")))
								.build(),
						Group.getBuilder(
								new GroupID("fid"), new GroupName("name2"), new UserName("uname2"),
								new CreateAndModTimes(Instant.ofEpochMilli(20000),
										Instant.ofEpochMilli(30000)))
								.withType(GroupType.TEAM)
								.withDescription("desc2")
								.withMember(new UserName("foo2"))
								.build(),
						Group.getBuilder(
								new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
								new CreateAndModTimes(Instant.ofEpochMilli(40000),
										Instant.ofEpochMilli(50000)))
								.build()
						)));
	}
	
	@Test
	public void getGroupsSortAndExclude() throws Exception {
		final Group ga = Group.getBuilder(
				new GroupID("gida"), new GroupName("na"), new UserName("n"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build();
		final Group gb = Group.getBuilder(
				new GroupID("gidb"), new GroupName("na"), new UserName("n"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build();
		final Group gd = Group.getBuilder(
				new GroupID("gidd"), new GroupName("na"), new UserName("n"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build();
		manager.storage.createGroup(ga);
		manager.storage.createGroup(gd);
		manager.storage.createGroup(gb);
		
		checkGroupsList(GetGroupsParams.getBuilder().build(), list(ga, gb, gd));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableSortAscending(true)
				.build(),
				list(ga, gb, gd));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableSortAscending(false)
				.build(),
				list(gd, gb, ga));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("gida")
				.build(),
				list(gb, gd));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("gidb")
				.build(),
				list(gd));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("gidc")
				.build(),
				list(gd));
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("gidd")
				.build(),
				list());
		checkGroupsList(GetGroupsParams.getBuilder()
				.withNullableSortAscending(false)
				.withNullableExcludeUpTo("gidc")
				.build(),
				list(gb, ga));
	}

	@Test
	public void getGroupsLimit() throws Exception {
		for (int i = 1; i < 202; i++) {
			final String id = String.format("g%03d", i);
			manager.storage.createGroup(Group.getBuilder(
					new GroupID(id), new GroupName("g" + 1), new UserName("n"),
					new CreateAndModTimes(inst(1000))).build());
		}
		
		assertGroupListCorrect(null, 1, 100);
		assertGroupListCorrect("fzzz", 1, 100);
		assertGroupListCorrect("g0", 1, 100);
		assertGroupListCorrect("g000", 1, 100);
		assertGroupListCorrect("g001", 2, 100);
		assertGroupListCorrect("g099", 100, 100);
		assertGroupListCorrect("g100", 101, 100);
		assertGroupListCorrect("g101", 102, 100);
		assertGroupListCorrect("g102", 103, 99);
		assertGroupListCorrect("g149", 150, 52);
		assertGroupListCorrect("g199", 200, 2);
		assertGroupListCorrect("g200", 201, 1);
		assertGroupListCorrect("g201", 201, 0);
		assertGroupListCorrect("g300", 201, 0);
	}
	
	private <T> void assertGroupListCorrect(
			final String excludeUpTo,
			final int start,
			final int size)
			throws Exception {
		final List<Group> res = manager.storage.getGroups(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo(excludeUpTo).build());
		assertThat("incorrect size", res.size(), is(size));
		int i = start;
		for (final Group g: res) {
			final String id = String.format("g%03d", i);
			assertThat("incorrect group", g.getGroupID().getName(), is(id));
			i++;
		}
	}

	
	private void checkGroupsList(final GetGroupsParams p, final List<Group> expected)
			throws GroupsStorageException {
		assertThat("incorrect groups", manager.storage.getGroups(p), is(expected));
	}
	
	@Test
	public void getGroupsFail() throws Exception {
		try {
			manager.storage.getGroups(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("params"));
		}
		
	}
	
	@Test
	public void addMember() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		manager.storage.addMember(new GroupID("gid"), new UserName("foo"), inst(70000));
		manager.storage.addMember(new GroupID("gid"), new UserName("bar"), inst(80000));
		
		assertThat("incorrect add member result", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(80000)))
						.withMember(new UserName("foo"))
						.withMember(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void addMemberFailNulls() throws Exception {
		failAddMember(null, new UserName("f"), inst(1), new NullPointerException("groupID"));
		failAddMember(new GroupID("g"), null, inst(1), new NullPointerException("member"));
		failAddMember(new GroupID("g"), new UserName("f"), null,
				new NullPointerException("modDate"));
	}
	
	@Test
	public void addMemberFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddMember(new GroupID("gid1"), new UserName("foo"), inst(1),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void assertModificationTimeIs(final GroupID groupID, final Instant modDate)
			throws Exception {
		assertThat("incorrect mod time", manager.storage.getGroup(groupID).getModificationDate(),
				is(modDate));
	}

	@Test
	public void addMemberFailExists() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("foo"), inst(1),
				new UserIsMemberException("User foo is already a member of group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void addMemberFailOwner() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(60000)))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("uname3"), inst(1),
				new UserIsMemberException("User uname3 is the owner of group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(60000));
	}
	
	@Test
	public void addMemberFailAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("admin"), inst(1),
				new UserIsMemberException("User admin is an administrator of group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failAddMember(
			final GroupID gid,
			final UserName member,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.addMember(gid, member, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("user"))
				.withMember(new UserName("bar"))
				.build());
		
		manager.storage.addAdmin(new GroupID("gid"), new UserName("foo"), inst(75000));
		// test that adding an admin that is already a member removes from member list
		manager.storage.addAdmin(new GroupID("gid"), new UserName("bar"), inst(85000));
		
		assertThat("incorrect add admin result", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(85000)))
						.withMember(new UserName("user"))
						.withAdministrator(new UserName("foo"))
						.withAdministrator(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void addAdminFailNulls() throws Exception {
		failAddAdmin(null, new UserName("f"), inst(1), new NullPointerException("groupID"));
		failAddAdmin(new GroupID("g"), null, inst(1), new NullPointerException("admin"));
		failAddAdmin(new GroupID("g"), new UserName("f"), null,
				new NullPointerException("modDate"));
	}
	
	@Test
	public void addAdminFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddAdmin(new GroupID("gid1"), new UserName("foo"), inst(1),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void addAdminFailOwner() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddAdmin(new GroupID("gid"), new UserName("uname3"), inst(1),
				new UserIsMemberException("User uname3 is the owner of group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void addAdminFailAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failAddAdmin(new GroupID("gid"), new UserName("admin"), inst(1),
				new UserIsMemberException("User admin is already an administrator of group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failAddAdmin(
			final GroupID gid,
			final UserName admin,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.addAdmin(gid, admin, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeMember() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.build());
		
		manager.storage.removeMember(new GroupID("gid"), new UserName("foo"), inst(76000));
		manager.storage.removeMember(new GroupID("gid"), new UserName("baz"), inst(82000));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(82000)))
						.withMember(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void removeMemberFailNulls() throws Exception {
		failRemoveMember(null, new UserName("f"), inst(1), new NullPointerException("groupID"));
		failRemoveMember(new GroupID("g"), null, inst(1), new NullPointerException("member"));
		failRemoveMember(new GroupID("g"), new UserName("u"), null,
				new NullPointerException("modDate"));
	}
	
	@Test
	public void removeMemberFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.build());
		
		failRemoveMember(new GroupID("gid1"), new UserName("foo"), inst(1),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void removeMemberFailNoSuchUser() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.withAdministrator(new UserName("bar"))
				.build());
		
		failRemoveMember(new GroupID("gid"), new UserName("bar"), inst(1), new NoSuchUserException(
				"No member bar in group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failRemoveMember(
			final GroupID gid,
			final UserName member,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.removeMember(gid, member, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void demoteAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.withAdministrator(new UserName("bar"))
				.withAdministrator(new UserName("baz"))
				.withAdministrator(new UserName("bat"))
				.build());
		
		manager.storage.demoteAdmin(new GroupID("gid"), new UserName("bar"), inst(90000));
		manager.storage.demoteAdmin(new GroupID("gid"), new UserName("bat"), inst(80000));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(80000)))
						.withMember(new UserName("foo"))
						.withMember(new UserName("bar"))
						.withMember(new UserName("bat"))
						.withAdministrator(new UserName("baz"))
						.build()));
	}
	
	@Test
	public void demoteAdminFailNulls() throws Exception {
		failDemoteAdmin(null, new UserName("f"), inst(1), new NullPointerException("groupID"));
		failDemoteAdmin(new GroupID("g"), null, inst(1), new NullPointerException("admin"));
		failDemoteAdmin(new GroupID("g"), new UserName("u"), null,
				new NullPointerException("modDate"));
	}
	
	@Test
	public void demoteAdminFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("foo"))
				.build());
		
		failDemoteAdmin(new GroupID("gid1"), new UserName("foo"), inst(1),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void demoteAdminFailNoSuchAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("foo"))
				.withMember(new UserName("bar"))
				.build());
		
		failDemoteAdmin(new GroupID("gid"), new UserName("bar"), inst(1), new NoSuchUserException(
				"No administrator bar in group gid"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failDemoteAdmin(
			final GroupID gid,
			final UserName member,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.demoteAdmin(gid, member, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addResource() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b")))
				.build());
		
		manager.storage.addResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceDescriptor(new ResourceAdministrativeID("a"), new ResourceID("c")),
				inst(55000));
		manager.storage.addResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceDescriptor(new ResourceAdministrativeID("b"), new ResourceID("d")),
				inst(65000));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(65000)))
						.withResource(new ResourceType("ws"), new ResourceDescriptor(
								new ResourceAdministrativeID("a"), new ResourceID("b")))
						.withResource(new ResourceType("ws"), new ResourceDescriptor(
								new ResourceAdministrativeID("a"), new ResourceID("c")))
						.withResource(new ResourceType("ws"), new ResourceDescriptor(
								new ResourceAdministrativeID("b"), new ResourceID("d")))
						.build()));
	}
	
	@Test
	public void addResourceFailNulls() throws Exception {
		final GroupID g = new GroupID("g");
		final ResourceType t = new ResourceType("t");
		final ResourceDescriptor d = new ResourceDescriptor(new ResourceID("i"));
		
		failAddResource(null, t, d, inst(1), new NullPointerException("groupID"));
		failAddResource(g, null, d, inst(1), new NullPointerException("type"));
		failAddResource(g, t, null, inst(1), new NullPointerException("resource"));
		failAddResource(g, t, d, null, new NullPointerException("modDate"));
	}
	
	@Test
	public void addResourceFailNoGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddResource(
				new GroupID("gid1"),
				new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("a")),
				inst(60000),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void addResourceFailResourceExists() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b")))
				.build());
		
		failAddResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b")),
				inst(32),
				new ResourceExistsException("ws b"));
		
		failAddResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceDescriptor(
						new ResourceAdministrativeID("c"), new ResourceID("b")),
				inst(32),
				new ResourceExistsException("ws b"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failAddResource(
			final GroupID g,
			final ResourceType t,
			final ResourceDescriptor d,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.addResource(g, t, d, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeResource() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b")))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("c")))
				.build());
		
		manager.storage.removeResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceID("c"),
				inst(109200));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(109200)))
						.withResource(new ResourceType("ws"), new ResourceDescriptor(
								new ResourceAdministrativeID("a"), new ResourceID("b")))
						.build()));
		
		manager.storage.removeResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceID("b"),
				inst(119200));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(119200)))
						.build()));
	}
	
	@Test
	public void removeResourceFailNulls() throws Exception {
		final GroupID g = new GroupID("g");
		final ResourceType t = new ResourceType("t");
		final ResourceID d = new ResourceID("i");
		
		failRemoveResource(null, t, d, inst(1), new NullPointerException("groupID"));
		failRemoveResource(g, null, d, inst(1), new NullPointerException("type"));
		failRemoveResource(g, t, null, inst(1), new NullPointerException("resource"));
		failRemoveResource(g, t, d, null, new NullPointerException("modDate"));
	}
	
	@Test
	public void removeResourceFailNoGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failRemoveResource(
				new GroupID("gid1"),
				new ResourceType("t"),
				new ResourceID("id"),
				inst(1),
				new NoSuchGroupException("gid1"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	@Test
	public void removeResourceFailNoResource() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b")))
				.build());
		
		failRemoveResource(
				new GroupID("gid"),
				new ResourceType("ws"),
				new ResourceID("c"),
				inst(1),
				new NoSuchResourceException("Group gid does not include ws c"));
		
		assertModificationTimeIs(new GroupID("gid"), inst(50000));
	}
	
	private void failRemoveResource(
			final GroupID g,
			final ResourceType t,
			final ResourceID d,
			final Instant modDate,
			final Exception expected) {
		try {
			manager.storage.removeResource(g, t, d, modDate);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void storeAndGetRequestMinimal() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)),
				is(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.build())
						.build()));
	}
	
	@Test
	public void storeAndGetRequestMaximal() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foobar"), new UserName("barfoo"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(40000), Instant.ofEpochMilli(60000))
					.withModificationTime(Instant.ofEpochMilli(50000))
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("githubrepo"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("kbase"),
						new ResourceID("kbase/groups")))
				.withStatus(GroupRequestStatus.from(
						GroupRequestStatusType.DENIED, new UserName("whee"), "jerkface"))
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)),
				is(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foobar"), new UserName("barfoo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(40000), Instant.ofEpochMilli(60000))
						.withModificationTime(Instant.ofEpochMilli(50000))
						.build())
					.withType(RequestType.INVITE)
					.withResourceType(new ResourceType("githubrepo"))
					.withResource(new ResourceDescriptor(new ResourceAdministrativeID("kbase"),
							new ResourceID("kbase/groups")))
					.withStatus(GroupRequestStatus.denied(new UserName("whee"), "jerkface"))
					.build()));
	}
	
	@Test
	public void storeRequestWithIdenticalCharacteristicString() throws Exception {
		// tests that storage with identical characteristic strings works as long as
		// there's only one open request
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final UUID id3 = UUID.randomUUID();
		final UUID id4 = UUID.randomUUID();
		final UUID id5 = UUID.randomUUID();
		manager.storage.storeRequest(getBuilder(id1).build());
		manager.storage.storeRequest(getBuilder(id2).withStatus(GroupRequestStatus.canceled())
				.build());
		manager.storage.storeRequest(getBuilder(id3).withStatus(GroupRequestStatus.expired())
				.build());
		manager.storage.storeRequest(getBuilder(id4).withStatus(
				GroupRequestStatus.accepted(new UserName("u")))
				.build());
		manager.storage.storeRequest(getBuilder(id5).withStatus(
				GroupRequestStatus.denied(new UserName("u"), "r")).build());
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id1)), is(
				getBuilder(id1).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id2)), is(
				getBuilder(id2).withStatus(GroupRequestStatus.canceled()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id3)), is(
				getBuilder(id3).withStatus(GroupRequestStatus.expired()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id4)), is(
				getBuilder(id4).withStatus(GroupRequestStatus.accepted(new UserName("u"))).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id5)), is(
				getBuilder(id5).withStatus(GroupRequestStatus.denied(new UserName("u"), "r")).build()));
	}

	private Builder getBuilder(final UUID id)
			throws Exception {
		return GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("yay"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("foo"),
						new ResourceID("bar")));
	}
	
	@Test
	public void storeRequestsWithSimilarCharacteristicStrings() throws Exception {
		// tests that request with nearly, but not exactly, identical characteristic strings
		// can be saved
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final UUID id3 = UUID.randomUUID();
		final UUID id4 = UUID.randomUUID();
		final UUID id5 = UUID.randomUUID();
		final UUID id6 = UUID.randomUUID();
		final CreateModAndExpireTimes times = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
				.build();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.build());
		
		// with different request type
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.withType(RequestType.INVITE)
				.build());
		
		// with different resource type
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withResourceType(new ResourceType("foo"))
				.build());
		
		// with different resource
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
				// res admin ID is not included in the char string since for a given resource ID,
				// the admin ID is always the same
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("yay"),
						new ResourceID("yo")))
				.build());
				
		
		// with different group
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id5), new GroupID("fooo"), new UserName("bar"), times)
				.build());
		
		// with different user
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id6), new GroupID("foo"), new UserName("barr"), times)
				.build());
		
		// check results
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id1)), is(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
						.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id2)), is(
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
						.withType(RequestType.INVITE)
						.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id3)), is(
				GroupRequest.getBuilder(
						new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
						.withResourceType(new ResourceType("foo"))
						.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id4)), is(
				GroupRequest.getBuilder(
						new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("yay"),
								new ResourceID("yo")))
						.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id5)), is(
				GroupRequest.getBuilder(
						new RequestID(id5), new GroupID("fooo"), new UserName("bar"), times)
						.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id6)), is(
				GroupRequest.getBuilder(
						new RequestID(id6), new GroupID("foo"), new UserName("barr"), times)
						.build()));
	}
	
	@Test
	public void storeRequestFailNull() throws Exception {
		failStoreRequest(null, new NullPointerException("request"));
	}
	
	@Test
	public void storeRequestFailDuplicateID() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build());
		
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo1"), new UserName("bar1"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(30000), Instant.ofEpochMilli(40000))
				.build())
			.build();
		
		failStoreRequest(request, new IllegalArgumentException(String.format(
				"ID %s already exists in the database. The programmer is responsible for " +
				"maintaining unique IDs.",
				id.toString())));
	}
	
	@Test
	public void storeRequestFailEquivalentRequest() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo1"), new UserName("bar1"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("foo"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("aid"),
						new ResourceID("rid")))
				.build());
		
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("bar1"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(30000), Instant.ofEpochMilli(40000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("foo"))
				// this should never actually happen. rid -> aid mapping should be immutable.
				// however, we test here to ensure the RAID is not included in the char string.
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("aid2"),
						new ResourceID("rid")))
				.build();
		
		failStoreRequest(request, new RequestExistsException(String.format(
				"Request exists with ID: %s", id.toString())));
	}
	
	private void failStoreRequest(final GroupRequest request, final Exception expected) {
		
		try {
			manager.storage.storeRequest(request);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void getRequestFail() throws Exception {
		failGetRequest(null, new NullPointerException("requestID"));
		
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("bar1"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build());
		
		final UUID id = UUID.randomUUID();
		failGetRequest(new RequestID(id), new NoSuchRequestException(id.toString()));
	}
	
	@Test
	public void illegalRequestDataInDB() throws Exception {
		// just test each type of exception. Not testing every possible exception that could be
		// thrown.
		
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar1"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build());
		
		manager.db.getCollection("requests").updateOne(new Document("id", id.toString()),
				new Document("$set", new Document("gid", "")));
		
		failGetRequest(new RequestID(id), new GroupsStorageException(
				"Unexpected value in database: 30000 Missing input parameter: group id"));
		
		manager.db.getCollection("requests").updateOne(new Document("id", id.toString()),
				new Document("$set", new Document("gid", "foo").append("requester", "a*b")));
	
		failGetRequest(new RequestID(id), new GroupsStorageException(
				"Unexpected value in database: 30010 Illegal user name: " +
				"Illegal character in user name a*b: *"));
		
		manager.db.getCollection("requests").updateOne(new Document("id", id.toString()),
				new Document("$set", new Document("requester", "a")
						.append("type", "KICK_FROM_GROUP")));
	
		failGetRequest(new RequestID(id), new GroupsStorageException(
				"Unexpected value in database: No enum constant " +
				"us.kbase.groups.core.request.RequestType.KICK_FROM_GROUP"));
	}
	
	
	private void failGetRequest(final RequestID id, final Exception expected) {
		try {
			manager.storage.getRequest(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsByRequester() throws Exception {
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		
		final GroupRequest first = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(50000), forever)
							.withModificationTime(Instant.ofEpochMilli(120000))
							.build())
				.build();
		final GroupRequest second = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo2"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(40000), forever)
							.withModificationTime(Instant.ofEpochMilli(130000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("whee")))
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo3"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m1"),
						new ResourceID("m1.n")))
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo4"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("2")))
				.build();
		final GroupRequest target = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("targ"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("user"))
				.withResource(new ResourceDescriptor(new ResourceID("bar")))
				.build();
		final GroupRequest otheruser = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("other"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.build();
		final GroupRequest closed = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("closed"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(inst(160000))
							.build())
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		manager.storage.storeRequest(fourth);
		manager.storage.storeRequest(target);
		manager.storage.storeRequest(otheruser);
		manager.storage.storeRequest(first);
		manager.storage.storeRequest(closed);
		manager.storage.storeRequest(third);
		manager.storage.storeRequest(second);
		
		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"), p),
				is(Arrays.asList(first, second, third, fourth)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.build()),
				is(Arrays.asList(third, fourth)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(third, fourth, closed)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(120000))
								.build()),
				is(Arrays.asList(second, third, fourth)));
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(129999))
								.build()),
				is(Arrays.asList(second, third, fourth)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"), 
						GetRequestsParams.getBuilder().withNullableSortAscending(false).build()),
				is(Arrays.asList(fourth, third, second, first)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"), 
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(closed, fourth, third, second, first)));
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar"), 
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableExcludeUpTo(inst(140000))
								.build()),
				is(Arrays.asList(second, first)));

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("baz"), p),
				is(Arrays.asList(otheruser)));
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bat"), p),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsByRequesterHitLimit() throws Exception {
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		final List<ResourceType> rtypes = Arrays.asList(new ResourceType("user"),
				new ResourceType("workspace"), new ResourceType("catalogmethod"));
		final List<ResourceDescriptor> resources = Arrays.asList(
				ResourceDescriptor.from(new UserName("target")),
				new ResourceDescriptor(new ResourceID("1")),
				new ResourceDescriptor(new ResourceAdministrativeID("m"), new ResourceID("m.m")));
		
		for (int i = 1; i < 202; i++) {
			final GroupRequest req = makeRequestForLimitTests(
					forever, i, new GroupID("n" + i), new UserName("name"),
					RequestType.values()[i % RequestType.values().length],
					rtypes.get(i % 3),
					resources.get(i % 3));
			manager.storage.storeRequest(req);
		}
		
		// these should not show up
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("name1"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1030000))
						.build())
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("name1"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1130000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("2")))
				.build());
		
		assertRequestListCorrect(
				r -> r.getGroupID().getName(),
				(s, p) -> s.getRequestsByRequester(new UserName("name"), p));
	}
	
	@Test
	public void getRequestsByRequesterFail() throws Exception {
		failGetRequestsByRequester(null, GetRequestsParams.getBuilder().build(),
				new NullPointerException("requester"));
		failGetRequestsByRequester(new UserName("u"), null, new NullPointerException("params"));
	}
	
	private void failGetRequestsByRequester(
			final UserName requester,
			final GetRequestsParams params,
			final Exception expected) {
		try {
			manager.storage.getRequestsByRequester(requester, params);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsByTarget() throws Exception {
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		
		final GroupRequest first = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(50000), forever)
							.withModificationTime(Instant.ofEpochMilli(120000))
							.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("bar")))
				.build();
		final GroupRequest second = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo2"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(40000), forever)
							.withModificationTime(Instant.ofEpochMilli(130000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.n")))
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo3"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("bar")))
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo4"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("1")))
				.build();
		final GroupRequest fifth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo5"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(160000))
							.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("bar")))
				.build();
		final GroupRequest user = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("use"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.build();
		final GroupRequest requestws = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("reqws"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("1")))
				.build();
		final GroupRequest requestcat = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("reqcat"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.n")))
				.build();
		final GroupRequest othertarget = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("other"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
							.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("bat")))
				.build();
		final GroupRequest otherws = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("otherws"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(125000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("2")))
				.build();
		final GroupRequest othercat = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("othercat"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(135000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m2"),
						new ResourceID("m2.n")))
				.build();
		final GroupRequest closed = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("closed"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(inst(170000))
							.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("bar")))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		final GroupRequest closedws = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("closed"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(inst(110000))
							.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("1")))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		manager.storage.storeRequest(fourth);
		manager.storage.storeRequest(user);
		manager.storage.storeRequest(closedws);
		manager.storage.storeRequest(requestcat);
		manager.storage.storeRequest(othertarget);
		manager.storage.storeRequest(first);
		manager.storage.storeRequest(fifth);
		manager.storage.storeRequest(requestws);
		manager.storage.storeRequest(othercat);
		manager.storage.storeRequest(closed);
		manager.storage.storeRequest(otherws);
		manager.storage.storeRequest(third);
		manager.storage.storeRequest(second);

		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		final Map<ResourceType, Set<ResourceAdministrativeID>> mt = Collections.emptyMap();
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"), mt, p),
				is(Arrays.asList(first, third, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(
						new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1"))),
						p),
				is(Arrays.asList(first, third, fourth, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						p),
				is(Arrays.asList(first, second, third, fourth, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.build()),
				is(Arrays.asList(third, fourth, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(third, fourth, fifth, closed)));

		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(120000))
								.build()),
				is(Arrays.asList(second, third, fourth, fifth)));

		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(129999))
								.build()),
				is(Arrays.asList(second, third, fourth, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.build()),
				is(Arrays.asList(fifth, fourth, third, second, first)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(closed, fifth, fourth, third, second, first, closedws)));

		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"))),
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableExcludeUpTo(inst(140000))
								.build()),
				is(Arrays.asList(second, first)));

		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1"),
										new ResourceAdministrativeID("2")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m"),
										new ResourceAdministrativeID("m2"))),
						p),
				is(Arrays.asList(first, otherws, second, othercat, third, fourth, fifth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bat"), mt, p),
				is(Arrays.asList(othertarget)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bat"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("2")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m2"))),
						p),
				is(Arrays.asList(othertarget, otherws, othercat)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("baz"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("3")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m3"))),
						p),
				is(Collections.emptyList()));
	}
	
	private interface FnExcept<T, R> {
		R apply(T t) throws Exception;
	}
	
	@Test
	public void getRequestsByTargetHitLimit() throws Exception {
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		final List<ResourceType> rtypes = Arrays.asList(new ResourceType("user"),
				new ResourceType("workspace"), new ResourceType("catalogmethod"));
		final List<FnExcept<Integer, ResourceDescriptor>> intToResource = Arrays.asList(
				i -> ResourceDescriptor.from(new UserName("target")),
				i -> new ResourceDescriptor(new ResourceID(((i % 4) + 1) + "")),
				i -> new ResourceDescriptor(new ResourceAdministrativeID("m" + ((i % 4) + 1)),
						new ResourceID("m" + ((i % 4) + 1) + ".m")));
		
		for (int i = 1; i < 202; i++) {
			final GroupRequest req = makeRequestForLimitTests(
					forever, i, new GroupID("n" + i), new UserName("name"),
					RequestType.INVITE,
					rtypes.get(i % 3),
					intToResource.get(i % 3).apply(i));
			manager.storage.storeRequest(req);
		}
		
		// these should not show up
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("name1"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1030000))
						.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("target1")))
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("name1"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1130000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("5")))
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("name1"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1130000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m5"),
						new ResourceID("m5.m")))
				.build());
		
		assertRequestListCorrect(
				r -> r.getGroupID().getName(),
				(s, p) -> s.getRequestsByTarget(
						new UserName("target"),
						ImmutableMap.of(new ResourceType("workspace"),
								set(new ResourceAdministrativeID("1"),
										new ResourceAdministrativeID("2"),
										new ResourceAdministrativeID("3"),
										new ResourceAdministrativeID("4")),
								new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("m1"),
										new ResourceAdministrativeID("m2"),
										new ResourceAdministrativeID("m3"),
										new ResourceAdministrativeID("m4"))),
						p));
	}
	
	@Test
	public void getRequestsByTargetFailBadArgs() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		failGetRequestsByTarget(null, Collections.emptyMap(), p,
				new NullPointerException("target"));
		failGetRequestsByTarget(new UserName("u"), null, p,
				new NullPointerException("resources"));
		
		final ResourceAdministrativeID raid = new ResourceAdministrativeID("a");
		final Map<ResourceType, Set<ResourceAdministrativeID>> resources = new HashMap<>();
		resources.put(new ResourceType("t"), set(raid));
		resources.put(null, set(raid));
		failGetRequestsByTarget(new UserName("u"), resources, p,
				new NullPointerException("null key in resources"));
		
		resources.remove(null);
		resources.put(new ResourceType("v"), null);
		failGetRequestsByTarget(new UserName("u"), resources, p,
				new NullPointerException("resources key v value"));
		
		resources.put(new ResourceType("v"), set());
		failGetRequestsByTarget(new UserName("u"), resources, p,
				new IllegalArgumentException("No resource IDs for key v"));
		
		resources.put(new ResourceType("v"), set(raid, null));
		failGetRequestsByTarget(new UserName("u"), resources, p,
				new NullPointerException("Null item in collection resources key v value"));
		
		failGetRequestsByTarget(new UserName("u"), Collections.emptyMap(), null,
				new NullPointerException("params"));
	}
	
	private void failGetRequestsByTarget(
			final UserName target,
			final Map<ResourceType, Set<ResourceAdministrativeID>> resources,
			final GetRequestsParams params,
			final Exception expected) {
		try {
			manager.storage.getRequestsByTarget(target, resources, params);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsByGroup() throws Exception {
		// tests including open/closed groups, sort direction, and skipping by date.
		// does not test limit.
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		
		final GroupRequest first = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar1"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(50000), forever)
							.withModificationTime(Instant.ofEpochMilli(120000))
							.build())
				.build();
		final GroupRequest second = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar2"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(40000), forever)
							.withModificationTime(Instant.ofEpochMilli(130000))
							.build())
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("45")))
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar3"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.m")))
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar4"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("42")))
				.build();
		// any request with a target user is excluded
		final GroupRequest target = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("foo")))
				.build();
		// same for invite workspace requests
		final GroupRequest targetws = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("86")))
				.build();
		// and invite method requests
		final GroupRequest targetcat = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.m")))
				.build();
		final GroupRequest othergroup = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("other"), new UserName("other"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.build();
		final GroupRequest closed = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("closed"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
							.withModificationTime(inst(160000))
							.build())
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		manager.storage.storeRequest(fourth);
		manager.storage.storeRequest(target);
		manager.storage.storeRequest(othergroup);
		manager.storage.storeRequest(first);
		manager.storage.storeRequest(targetws);
		manager.storage.storeRequest(closed);
		manager.storage.storeRequest(targetcat);
		manager.storage.storeRequest(third);
		manager.storage.storeRequest(second);

		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"), p), is(
						Arrays.asList(first, second, third, fourth)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.build()),
				is(Arrays.asList(third, fourth)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(130000))
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(third, fourth, closed)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(120000))
								.build()),
				is(Arrays.asList(second, third, fourth)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableExcludeUpTo(inst(129999))
								.build()),
				is(Arrays.asList(second, third, fourth)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder().withNullableSortAscending(false).build()),
				is(Arrays.asList(fourth, third, second, first)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableIncludeClosed(true)
								.build()),
				is(Arrays.asList(closed, fourth, third, second, first)));
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo"),
						GetRequestsParams.getBuilder()
								.withNullableSortAscending(false)
								.withNullableExcludeUpTo(inst(140000))
								.build()),
				is(Arrays.asList(second, first)));
		
		
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("other"), p),
				is(Arrays.asList(othergroup)));
		
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("baz"), p),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsByGroupHitLimit() throws Exception {
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		
		final List<ResourceType> rtypes = Arrays.asList(new ResourceType("user"),
				new ResourceType("workspace"), new ResourceType("catalogmethod"));
		final List<ResourceDescriptor> resources = Arrays.asList(
				ResourceDescriptor.from(new UserName("target")),
				new ResourceDescriptor(new ResourceID("1")),
				new ResourceDescriptor(new ResourceAdministrativeID("m"), new ResourceID("m.m")));
		
		for (int i = 1; i < 202; i++) {
			final GroupRequest req = makeRequestForLimitTests(
					forever, i, new GroupID("gid"), new UserName("n" + i),
					RequestType.REQUEST,
					rtypes.get(i % 3),
					resources.get(i % 3));
			manager.storage.storeRequest(req);
		}
		
		// these should not show up
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1030000))
						.build())
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid1"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(inst(999999), forever)
						.withModificationTime(inst(1130000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("2")))
				.build());
		
		assertRequestListCorrect(
				r -> r.getRequester().getName(),
				(s, p) -> s.getRequestsByGroup(new GroupID("gid"), p));
	}

	private void assertRequestListCorrect(
			final Function<GroupRequest, String> getCheckString,
			final BiFnExcept<GroupsStorage, GetRequestsParams, List<GroupRequest>> getRequests)
			throws Exception {
		assertRequestListCorrect(null, 1, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(1000000), 1, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(1009999), 1, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(1010000), 2, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(1990001), 100, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(1999999), 100, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(2000000), 101, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(2019999), 102, 100, getCheckString, getRequests);
		assertRequestListCorrect(inst(2020000), 103, 99, getCheckString, getRequests);
		assertRequestListCorrect(inst(2499999), 150, 52, getCheckString, getRequests);
		assertRequestListCorrect(inst(2999999), 200, 2, getCheckString, getRequests);
		assertRequestListCorrect(inst(3000000), 201, 1, getCheckString, getRequests);
		assertRequestListCorrect(inst(3000001), 201, 1, getCheckString, getRequests);
		assertRequestListCorrect(inst(3010000), 201, 0, getCheckString, getRequests);
		assertRequestListCorrect(inst(4000000), 201, 0, getCheckString, getRequests);
	}
	
	private interface BiFnExcept<T, U, R> {
		
		R apply(T t, U u) throws Exception;
	}

	private <T> void assertRequestListCorrect(
			final Instant excludeUpTo,
			final int start,
			final int size,
			final Function<GroupRequest, String> getCheckString,
			final BiFnExcept<GroupsStorage, GetRequestsParams, List<GroupRequest>> getRequests)
			throws Exception {
		final List<GroupRequest> res = getRequests.apply(
				manager.storage, GetRequestsParams.getBuilder()
						.withNullableIncludeClosed(true)
						.withNullableExcludeUpTo(excludeUpTo)
						.build());
		assertThat("incorrect size", res.size(), is(size));
		int i = start;
		for (final GroupRequest r: res) {
			assertThat("incorrect request", getCheckString.apply(r), is("n" + i));
			i++;
		}
	}

	private GroupRequest makeRequestForLimitTests(
			final Instant forever,
			final int i,
			final GroupID gid,
			final UserName requester,
			final RequestType type,
			final ResourceType restype,
			final ResourceDescriptor resource)
			throws Exception {
		final GroupRequestStatusType st = GroupRequestStatusType.values()
				[i % GroupRequestStatusType.values().length];
		final GroupRequest req = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), gid, requester,
				CreateModAndExpireTimes.getBuilder(inst(1000000 - (10000 * i)), forever)
						.withModificationTime(inst(1000000 + (10000 * i)))
						.build())
				.withStatus(GroupRequestStatus.from(st, new UserName("c"), "r"))
				.withType(type)
				.withResourceType(restype)
				.withResource(resource)
				.build();
		return req;
	}
	
	@Test
	public void getRequestsByGroupFail() throws Exception {
		failGetRequestsByGroup(null, GetRequestsParams.getBuilder().build(),
				new NullPointerException("groupID"));
		failGetRequestsByGroup(new GroupID("id"), null, new NullPointerException("params"));
	}
	
	private void failGetRequestsByGroup(
			final GroupID id,
			final GetRequestsParams p,
			final Exception expected) {
		
		try {
			manager.storage.getRequestsByGroup(id, p);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void closeRequestCancel() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		manager.storage.closeRequest(new RequestID(id), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(25000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)), is(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.withModificationTime(Instant.ofEpochMilli(25000))
									.build())
						.withStatus(GroupRequestStatus.canceled())
						.build()));
	}
	
	@Test
	public void closeRequestAccept() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		manager.storage.closeRequest(new RequestID(id),
				GroupRequestStatus.accepted(new UserName("a")), Instant.ofEpochMilli(25000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)), is(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.withModificationTime(Instant.ofEpochMilli(25000))
									.build())
						.withStatus(GroupRequestStatus.accepted(new UserName("a")))
						.build()));
	}
	
	@Test
	public void closeRequestDeny() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		manager.storage.closeRequest(new RequestID(id),
				GroupRequestStatus.denied(new UserName("a"), "r"), Instant.ofEpochMilli(25000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)), is(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.withModificationTime(Instant.ofEpochMilli(25000))
									.build())
						.withStatus(GroupRequestStatus.denied(new UserName("a"), "r"))
						.build()));
	}
	
	@Test
	public void closeRequestDenyNoReason() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		manager.storage.closeRequest(new RequestID(id),
				GroupRequestStatus.denied(new UserName("a"), null), Instant.ofEpochMilli(25000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)), is(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.withModificationTime(Instant.ofEpochMilli(25000))
									.build())
						.withStatus(GroupRequestStatus.denied(new UserName("a"), null))
						.build()));
	}
	
	@Test
	public void closeRequestAndCreateNew() throws Exception {
		// tests that the characteristic string for the request is removed so that
		// the request can be remade if desired.
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		manager.storage.closeRequest(new RequestID(id), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(25000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)), is(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.withModificationTime(Instant.ofEpochMilli(25000))
									.build())
						.withStatus(GroupRequestStatus.canceled())
						.build()));
		
		final UUID newID = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(newID), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(newID)), is(
				GroupRequest.getBuilder(
						new RequestID(newID), new GroupID("foo"), new UserName("bar"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
									.build())
						.build()));
	}
	
	@Test
	public void closeRequestFailNulls() throws Exception {
		final RequestID id = new RequestID(UUID.randomUUID());
		final GroupRequestStatus s = GroupRequestStatus.canceled();
		final Instant m = Instant.ofEpochMilli(10000);
		
		failCloseRequest(null, s, m, new NullPointerException("requestID"));
		failCloseRequest(id, null, m, new NullPointerException("newStatus"));
		failCloseRequest(id, s, null, new NullPointerException("modificationTime"));
	}
	
	@Test
	public void closeRequestFailOpen() throws Exception {
		failCloseRequest(new RequestID(UUID.randomUUID()), GroupRequestStatus.open(),
				Instant.ofEpochMilli(10000), new IllegalArgumentException(
						"newStatus cannot be OPEN"));
	}
	
	@Test
	public void closeRequestFailNoSuchID() throws Exception {
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.build());
		
		final UUID newID = UUID.randomUUID();
		failCloseRequest(new RequestID(newID), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(10000), new NoSuchRequestException(
						"No open request with ID " + newID.toString()));
	}
	
	@Test
	public void closeRequestFailAlreadyClosed() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.withStatus(GroupRequestStatus.canceled())
				.build());
		
		failCloseRequest(new RequestID(id), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(10000), new NoSuchRequestException(
						"No open request with ID " + id.toString()));
	}
	
	private void failCloseRequest(
			final RequestID id,
			final GroupRequestStatus status,
			final Instant mod,
			final Exception expected) {
		try {
			manager.storage.closeRequest(id, status, mod);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void expireRequests() throws Exception {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final UUID id3 = UUID.randomUUID();
		final UUID id4 = UUID.randomUUID();
		// won't expire - expire time in future
		final GroupRequest gr1 = GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(inst(20000), inst(40001)).build())
				.build();
		manager.storage.storeRequest(gr1);
		// won't expire - already closed
		final GroupRequest gr2 = GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(inst(20000), inst(30000)).build())
				.withStatus(GroupRequestStatus.canceled())
				.build();
		manager.storage.storeRequest(gr2);
		// will expire
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bat"),
					CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000)).build())
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id4), new GroupID("foo"), new UserName("bae"),
					CreateModAndExpireTimes.getBuilder(inst(20000), inst(30000)).build())
				.build());
		
		manager.storage.expireRequests(inst(40000));
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id1)), is(gr1));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id2)), is(gr2));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id3)),
				is(GroupRequest.getBuilder(
						new RequestID(id3), new GroupID("foo"), new UserName("bat"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000))
								.withModificationTime(inst(40000))
								.build())
						.withStatus(GroupRequestStatus.expired())
						.build()));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id4)),
				is(GroupRequest.getBuilder(
						new RequestID(id4), new GroupID("foo"), new UserName("bae"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(30000))
								.withModificationTime(inst(40000))
								.build())
						.withStatus(GroupRequestStatus.expired())
					.build()));
		
		// ensure the request characteristic string is removed
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bat"),
					CreateModAndExpireTimes.getBuilder(inst(20000), inst(30000)).build())
				.build());
	}
	
	@Test
	public void expireRequestsFail() throws Exception {
		try {
			manager.storage.expireRequests(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("expireTime"));
		}
	}
	
	@Test
	public void expireAgent() throws Exception {
		// also tests that stopping the agent multiple times in succession has no effect.
		final MongoGroupsStorage s = manager.storage;
		final Clock clock = manager.clockMock;
		final Instant now = Instant.ofEpochMilli(50000);
		when(clock.instant()).thenReturn(now);
		assertThat("incorrect agent running", s.isExpirationAgentRunning(), is(true));
		s.stopExpirationAgent(); // stop the default agent
		assertThat("incorrect agent running", s.isExpirationAgentRunning(), is(false));
		s.stopExpirationAgent();
		assertThat("incorrect agent running", s.isExpirationAgentRunning(), is(false));
		// to be expired immediately
		final UUID id1 = UUID.randomUUID();
		final GroupRequest gr1 = GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bat"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000)).build())
				.build();
		final GroupRequest gr1ex = GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bat"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000))
								.withModificationTime(inst(50000))
								.build())
						.withStatus(GroupRequestStatus.expired())
				.build();
		// safe data
		final UUID id2 = UUID.randomUUID();
		final GroupRequest gr2 = GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("baz"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(80000)).build())
				.build();
		// to be expired later
		final UUID id3 = UUID.randomUUID();
		final GroupRequest gr3 = GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000)).build())
				.build();
		final GroupRequest gr3ex = GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"),
						CreateModAndExpireTimes.getBuilder(inst(20000), inst(40000))
								.withModificationTime(inst(50000))
								.build())
						.withStatus(GroupRequestStatus.expired())
				.build();

		logEvents.clear();
		manager.storage.storeRequest(gr1);
		manager.storage.storeRequest(gr2);
		s.startExpirationAgent(1); // runs the agent immediately, so hold off a sec
		Thread.sleep(100); // let the agent finish
		assertThat("incorrect agent running", s.isExpirationAgentRunning(), is(true));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id1)), is(gr1ex));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id2)), is(gr2));
		
		manager.storage.storeRequest(gr3);
		assertThat("incorrect agent running", s.isExpirationAgentRunning(), is(true));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id1)), is(gr1ex));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id2)), is(gr2));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id3)), is(gr3));

		Thread.sleep(400);
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id1)), is(gr1ex));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id2)), is(gr2));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id3)), is(gr3));

		Thread.sleep(600);
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id1)), is(gr1ex));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id2)), is(gr2));
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id3)), is(gr3ex));

		assertLogEventsCorrect(logEvents,
				new LogEvent(Level.INFO, "Running expiration agent",
						MongoGroupsStorage.class.getName() + "$ExpirationAgent"),
				new LogEvent(Level.INFO, "Running expiration agent",
						MongoGroupsStorage.class.getName() + "$ExpirationAgent"));
	}
	
	@Test
	public void startReaperFail() {
		final MongoGroupsStorage s = manager.storage;
		failStartExpirationAgent(s, 1, new IllegalArgumentException(
				"The expiration agent is already running"));
		
		s.stopExpirationAgent();
		failStartExpirationAgent(s, 0, new IllegalArgumentException(
				"periodInSeconds must be > 0"));
		
		s.startExpirationAgent(1000);
		failStartExpirationAgent(s, 1, new IllegalArgumentException(
				"The expiration agent is already running"));
	}
	
	private void failStartExpirationAgent(
			final MongoGroupsStorage storage,
			final long period, 
			final Exception expected) {
		try {
			storage.startExpirationAgent(period);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
