package us.kbase.test.groups.storage.mongo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
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
				.withType(GroupType.project)
				.withDescription("desc")
				.build());
		
		assertThat("incorrect group", manager.storage.getGroup(new GroupID("gid")),
				is(Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("uname"),
						new CreateAndModTimes(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
						.withType(GroupType.project)
						.withDescription("desc")
						.build()));
	}
	
	@Test
	public void storeAndGetRequestMinimal() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				id, new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(UUID.fromString(id.toString())),
				is(GroupRequest.getBuilder(
				id, new GroupID("foo"), new UserName("bar"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
					.build())
				.build()));
	}
	
	@Test
	public void storeAndGetRequestMaximal() throws Exception {
		final UUID id = UUID.randomUUID();
		manager.storage.storeRequest(GroupRequest.getBuilder(
				id, new GroupID("foobar"), new UserName("barfoo"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(40000), Instant.ofEpochMilli(60000))
					.withModificationTime(Instant.ofEpochMilli(50000))
					.build())
				.withInviteToGroup(new UserName("target"))
				.withStatus(GroupRequestStatus.ACCEPTED)
				.build());
		
		assertThat("incorrect request", manager.storage.getRequest(UUID.fromString(id.toString())),
				is(GroupRequest.getBuilder(
						id, new GroupID("foobar"), new UserName("barfoo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(40000), Instant.ofEpochMilli(60000))
						.withModificationTime(Instant.ofEpochMilli(50000))
						.build())
					.withInviteToGroup(new UserName("target"))
					.withStatus(GroupRequestStatus.ACCEPTED)
					.build()));
	}
	
}
