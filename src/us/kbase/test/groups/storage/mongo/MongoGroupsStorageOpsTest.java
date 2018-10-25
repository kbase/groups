package us.kbase.test.groups.storage.mongo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.storage.exceptions.GroupsStorageException;
import us.kbase.test.groups.MongoStorageTestManager;
import us.kbase.test.groups.TestCommon;

public class MongoGroupsStorageOpsTest {

	private static MongoStorageTestManager manager;
	private static List<ILoggingEvent> logEvents;
	private static Path TEMP_DIR;
	private static Path EMPTY_FILE_MSH;
	
	@BeforeClass
	public static void setUp() throws Exception {
		logEvents = TestCommon.setUpSLF4JTestLoggerAppender("us.kbase.assemblyhomology");
		manager = new MongoStorageTestManager("test_mongoahstorage");
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
	
	@Test
	public void createAndGetGroupMinimal() throws Exception {
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
	public void getGroups() throws Exception {
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
				.build());
		
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("fid"), new GroupName("name2"), new UserName("uname2"),
				new CreateAndModTimes(Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withType(GroupType.TEAM)
				.withDescription("desc2")
				.withMember(new UserName("foo2"))
				.build());
		
		assertThat("incorrect get group", manager.storage.getGroups(), is(Arrays.asList(
				Group.getBuilder(
						new GroupID("aid"), new GroupName("name1"), new UserName("uname1"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000),
								Instant.ofEpochMilli(10000)))
						.withType(GroupType.PROJECT)
						.withDescription("desc1")
						.withMember(new UserName("foo1"))
						.withMember(new UserName("bar1"))
						.withAdministrator(new UserName("admin"))
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
	public void getGroupsEmpty() throws Exception {
		assertThat("incorrect get groups", manager.storage.getGroups(),
				is(Collections.emptyList()));
	}
	
	@Test
	public void addMember() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		manager.storage.addMember(new GroupID("gid"), new UserName("foo"));
		manager.storage.addMember(new GroupID("gid"), new UserName("bar"));
		
		assertThat("incorrect add member result", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(50000)))
						.withMember(new UserName("foo"))
						.withMember(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void addMemberFailNulls() throws Exception {
		failAddMember(null, new UserName("f"), new NullPointerException("groupID"));
		failAddMember(new GroupID("g"), null, new NullPointerException("member"));
	}
	
	@Test
	public void addMemberFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddMember(new GroupID("gid1"), new UserName("foo"), new NoSuchGroupException("gid1"));
	}
	
	@Test
	public void addMemberFailExists() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("foo"),
				new UserIsMemberException("User foo is already a member of group gid"));
	}
	
	@Test
	public void addMemberFailOwner() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("uname3"),
				new UserIsMemberException("User uname3 is the owner of group gid"));
	}
	
	@Test
	public void addMemberFailAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failAddMember(new GroupID("gid"), new UserName("admin"),
				new UserIsMemberException("User admin is an administrator of group gid"));
	}
	
	private void failAddMember(
			final GroupID gid,
			final UserName member,
			final Exception expected) {
		try {
			manager.storage.addMember(gid, member);
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
		
		manager.storage.addAdmin(new GroupID("gid"), new UserName("foo"));
		// test that adding an admin that is already a member removes from member list
		manager.storage.addAdmin(new GroupID("gid"), new UserName("bar"));
		
		assertThat("incorrect add admin result", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(50000)))
						.withMember(new UserName("user"))
						.withAdministrator(new UserName("foo"))
						.withAdministrator(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void addAdminFailNulls() throws Exception {
		failAddAdmin(null, new UserName("f"), new NullPointerException("groupID"));
		failAddAdmin(new GroupID("g"), null, new NullPointerException("admin"));
	}
	
	@Test
	public void addAdminFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddAdmin(new GroupID("gid1"), new UserName("foo"), new NoSuchGroupException("gid1"));
	}
	
	@Test
	public void addAdminFailOwner() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.build());
		
		failAddAdmin(new GroupID("gid"), new UserName("uname3"),
				new UserIsMemberException("User uname3 is the owner of group gid"));
	}
	
	@Test
	public void addAdminFailAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failAddAdmin(new GroupID("gid"), new UserName("admin"),
				new UserIsMemberException("User admin is already an administrator of group gid"));
	}
	
	private void failAddAdmin(
			final GroupID gid,
			final UserName admin,
			final Exception expected) {
		try {
			manager.storage.addAdmin(gid, admin);
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
		
		manager.storage.removeMember(new GroupID("gid"), new UserName("foo"));
		manager.storage.removeMember(new GroupID("gid"), new UserName("baz"));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(50000)))
						.withMember(new UserName("bar"))
						.build()));
	}
	
	@Test
	public void removeMemberFailNulls() throws Exception {
		failRemoveMember(null, new UserName("f"), new NullPointerException("groupID"));
		failRemoveMember(new GroupID("g"), null, new NullPointerException("member"));
	}
	
	@Test
	public void removeMemberFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.build());
		
		failRemoveMember(new GroupID("gid1"), new UserName("foo"),
				new NoSuchGroupException("gid1"));
	}
	
	@Test
	public void removeMemberFailNoSuchUser() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withMember(new UserName("foo"))
				.withAdministrator(new UserName("bar"))
				.build());
		
		failRemoveMember(new GroupID("gid"), new UserName("bar"), new NoSuchUserException(
				"No member bar in group gid"));
	}
	
	private void failRemoveMember(
			final GroupID gid,
			final UserName member,
			final Exception expected) {
		try {
			manager.storage.removeMember(gid, member);
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
		
		manager.storage.demoteAdmin(new GroupID("gid"), new UserName("bar"));
		manager.storage.demoteAdmin(new GroupID("gid"), new UserName("bat"));
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")), is(
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
						new CreateAndModTimes(Instant.ofEpochMilli(40000),
								Instant.ofEpochMilli(50000)))
						.withMember(new UserName("foo"))
						.withMember(new UserName("bar"))
						.withMember(new UserName("bat"))
						.withAdministrator(new UserName("baz"))
						.build()));
	}
	
	@Test
	public void demoteAdminFailNulls() throws Exception {
		failDemoteAdmin(null, new UserName("f"), new NullPointerException("groupID"));
		failDemoteAdmin(new GroupID("g"), null, new NullPointerException("admin"));
	}
	
	@Test
	public void demoteAdminFailNoSuchGroup() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("foo"))
				.build());
		
		failDemoteAdmin(new GroupID("gid1"), new UserName("foo"),
				new NoSuchGroupException("gid1"));
	}
	
	@Test
	public void demoteAdminFailNoSuchAdmin() throws Exception {
		manager.storage.createGroup(Group.getBuilder(
				new GroupID("gid"), new GroupName("name3"), new UserName("uname3"),
				new CreateAndModTimes(Instant.ofEpochMilli(40000), Instant.ofEpochMilli(50000)))
				.withAdministrator(new UserName("foo"))
				.withMember(new UserName("bar"))
				.build());
		
		failDemoteAdmin(new GroupID("gid"), new UserName("bar"), new NoSuchUserException(
				"No administrator bar in group gid"));
	}
	
	private void failDemoteAdmin(
			final GroupID gid,
			final UserName member,
			final Exception expected) {
		try {
			manager.storage.demoteAdmin(gid, member);
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
	public void storeAndGetRequestMinimalWithTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
							.build())
				.withInviteToGroup(new UserName("target"))
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(new RequestID(id)),
				is(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("bar"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
								.build())
						.withInviteToGroup(new UserName("target"))
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
				.withInviteToGroup(new UserName("target"))
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
					.withInviteToGroup(new UserName("target"))
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
		final CreateModAndExpireTimes times = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
				.build();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bar"), times).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.canceled()).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.expired()).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.accepted(new UserName("u"))).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id5), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.denied(new UserName("u"), "r")).build());
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id1)), is(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id2)), is(
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.canceled()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id3)), is(
				GroupRequest.getBuilder(
						new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.expired()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id4)), is(
				GroupRequest.getBuilder(
						new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.accepted(new UserName("u"))).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id5)), is(
				GroupRequest.getBuilder(
						new RequestID(id5), new GroupID("foo"), new UserName("bar"), times)
				.withStatus(GroupRequestStatus.denied(new UserName("u"), "r")).build()));
	}
	
	@Test
	public void storeRequestWithIdenticalCharacteristicStringAndTarget() throws Exception {
		// tests that storage with identical characteristic strings works as long as
		// there's only one open request
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final UUID id3 = UUID.randomUUID();
		final UUID id4 = UUID.randomUUID();
		final UUID id5 = UUID.randomUUID();
		final CreateModAndExpireTimes times = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
				.build();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.canceled()).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.expired()).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.accepted(new UserName("u"))).build());
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id5), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.denied(new UserName("u"), "r")).build());
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id1)), is(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id2)), is(
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.canceled()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id3)), is(
				GroupRequest.getBuilder(
						new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.expired()).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id4)), is(
				GroupRequest.getBuilder(
						new RequestID(id4), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.accepted(new UserName("u"))).build()));
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id5)), is(
				GroupRequest.getBuilder(
						new RequestID(id5), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.denied(new UserName("u"), "r")).build()));
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
		final CreateModAndExpireTimes times = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
				.build();
		manager.storage.storeRequest(GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build());
		
		// with no target - implies different type
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.build());
		
		// with different target
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("tarjeh"))
				.build());
		
		// with different group
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id4), new GroupID("fooo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build());
		
		// with different user
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id5), new GroupID("foo"), new UserName("barr"), times)
				.withInviteToGroup(new UserName("target"))
				.build());
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id1)), is(
				GroupRequest.getBuilder(
						new RequestID(id1), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id2)), is(
				GroupRequest.getBuilder(
						new RequestID(id2), new GroupID("foo"), new UserName("bar"), times)
				.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id3)), is(
				GroupRequest.getBuilder(
						new RequestID(id3), new GroupID("foo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("tarjeh"))
				.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id4)), is(
				GroupRequest.getBuilder(
						new RequestID(id4), new GroupID("fooo"), new UserName("bar"), times)
				.withInviteToGroup(new UserName("target"))
				.build()));
		
		assertThat("incorrect group", manager.storage.getRequest(new RequestID(id5)), is(
				GroupRequest.getBuilder(
						new RequestID(id5), new GroupID("foo"), new UserName("barr"), times)
				.withInviteToGroup(new UserName("target"))
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
	public void storeRequestFailEquivalentRequestNoTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build());
		
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(30000), Instant.ofEpochMilli(40000))
				.build())
			.build();
		
		failStoreRequest(request, new RequestExistsException(String.format(
				"Request exists with ID: %s", id.toString())));
	}
	
	@Test
	public void storeRequestFailEquivalentRequestWithTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("foo1"), new UserName("bar1"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.withInviteToGroup(new UserName("baz1"))
				.build());
		
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("bar1"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(30000), Instant.ofEpochMilli(40000))
				.build())
				.withInviteToGroup(new UserName("baz1"))
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
				"us.kbase.groups.core.request.GroupRequestType.KICK_FROM_GROUP"));
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
	public void getRequestsByRequesterOpenState() throws Exception {
		// as of writing this test, only the open state is supported
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
				.withInviteToGroup(new UserName("whee"))
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo3"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo4"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.build();
		final GroupRequest target = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("targ"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withInviteToGroup(new UserName("bar"))
				.build();
		final GroupRequest otheruser = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("other"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.build();
		final GroupRequest closed = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("closed"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
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

		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bar")), is(
						Arrays.asList(first, second, third, fourth)));
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("baz")), is(
						Arrays.asList(otheruser)));
		
		assertThat("incorrect get by requester",
				manager.storage.getRequestsByRequester(new UserName("bat")), is(
						Collections.emptyList()));
	}
	
	@Test
	public void failGetRequestsByRequester() {
		try {
			manager.storage.getRequestsByRequester(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("requester"));
		}
	}
	
	@Test
	public void getRequestsByTargetOpenState() throws Exception {
		// as of writing this test, only the open state is supported
		final Instant forever = Instant.ofEpochMilli(1000000000000000L);
		
		final GroupRequest first = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo1"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(50000), forever)
							.withModificationTime(Instant.ofEpochMilli(120000))
							.build())
				.withInviteToGroup(new UserName("bar"))
				.build();
		final GroupRequest second = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo2"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(40000), forever)
							.withModificationTime(Instant.ofEpochMilli(130000))
							.build())
				.withInviteToGroup(new UserName("bar"))
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo3"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.withInviteToGroup(new UserName("bar"))
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo4"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.withInviteToGroup(new UserName("bar"))
				.build();
		final GroupRequest user = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("use"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.build();
		final GroupRequest othertarget = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("other"), new UserName("baz"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
							.build())
				.withInviteToGroup(new UserName("bat"))
				.build();
		final GroupRequest closed = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("closed"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withInviteToGroup(new UserName("bar"))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		manager.storage.storeRequest(fourth);
		manager.storage.storeRequest(user);
		manager.storage.storeRequest(othertarget);
		manager.storage.storeRequest(first);
		manager.storage.storeRequest(closed);
		manager.storage.storeRequest(third);
		manager.storage.storeRequest(second);

		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bar")), is(
						Arrays.asList(first, second, third, fourth)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("bat")), is(
						Arrays.asList(othertarget)));
		
		assertThat("incorrect get by target",
				manager.storage.getRequestsByTarget(new UserName("baz")), is(
						Collections.emptyList()));
	}
	
	@Test
	public void failGetRequestsByTarget() {
		try {
			manager.storage.getRequestsByTarget(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("target"));
		}
	}
	
	@Test
	public void getRequestsByGroupOpenState() throws Exception {
		// as of writing this test, only the open state is supported
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
				.build();
		final GroupRequest third = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar3"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(30000), forever)
							.withModificationTime(Instant.ofEpochMilli(140000))
							.build())
				.build();
		final GroupRequest fourth = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("bar4"),
					CreateModAndExpireTimes.getBuilder(Instant.ofEpochMilli(20000), forever)
							.withModificationTime(Instant.ofEpochMilli(150000))
							.build())
				.build();
		// any request with a target is excluded
		final GroupRequest target = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("foo"), new UserName("whee"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), forever)
					.build())
				.withInviteToGroup(new UserName("foo"))
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
					.build())
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		manager.storage.storeRequest(fourth);
		manager.storage.storeRequest(target);
		manager.storage.storeRequest(othergroup);
		manager.storage.storeRequest(first);
		manager.storage.storeRequest(closed);
		manager.storage.storeRequest(third);
		manager.storage.storeRequest(second);

		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("foo")), is(
						Arrays.asList(first, second, third, fourth)));
		
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("other")), is(
						Arrays.asList(othergroup)));
		
		assertThat("incorrect get by group",
				manager.storage.getRequestsByGroup(new GroupID("baz")), is(
						Collections.emptyList()));
	}
	
	@Test
	public void failGetRequestsByGroup() {
		try {
			manager.storage.getRequestsByGroup(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("groupID"));
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
	
}
