package us.kbase.test.groups.storage.mongo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
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
	
	// TODO TEST add more tests for create and get group /request
	
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
				.build());
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("uname"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
						.withType(GroupType.PROJECT)
						.withDescription("desc")
						.build()));
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
	
	//TODO TEST that saving requests with similar but not identical characteristics works
	//TODO TEST that saving requests with identical characteristics but with open vs. closed states works
	
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
	
}
