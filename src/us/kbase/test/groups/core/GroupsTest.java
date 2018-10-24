package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;

import org.junit.Test;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Notifications;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UUIDGenerator;
import us.kbase.groups.core.UserHandler;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.WorkspaceHandler;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.test.groups.TestCommon;

public class GroupsTest {
	
	private static final GroupCreationParams PARAMS;
	static {
		try {
			PARAMS = GroupCreationParams.getBuilder(
					new GroupID("i"), new GroupName("n")).build();
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests newb");
		}
	}

	private static TestMocks initTestMocks() throws Exception {
		final GroupsStorage storage = mock(GroupsStorage.class);
		final UserHandler uh = mock(UserHandler.class);
		final WorkspaceHandler wh = mock(WorkspaceHandler.class);
		final Notifications notis = mock(Notifications.class);
		final UUIDGenerator uuidGen = mock(UUIDGenerator.class);
		final Clock clock = mock(Clock.class);
		
		
		final Constructor<Groups> c = Groups.class.getDeclaredConstructor(
				GroupsStorage.class, UserHandler.class, WorkspaceHandler.class,
				Notifications.class, UUIDGenerator.class, Clock.class);
		c.setAccessible(true);
		final Groups instance = c.newInstance(storage, uh, wh, notis, uuidGen, clock);
		return new TestMocks(instance, storage, uh, wh, uuidGen, clock);
	}
	
	public static class TestMocks {
		
		public final Groups groups;
		public final GroupsStorage storage;
		public final UserHandler userHandler;
		public final WorkspaceHandler wsHandler;
		public final UUIDGenerator uuidGen;
		public final Clock clock;
		
		private TestMocks(
				final Groups groups,
				final GroupsStorage storage,
				final UserHandler userHandler,
				final WorkspaceHandler wsHandler,
				final UUIDGenerator uuidGen,
				final Clock clock) {
			this.groups = groups;
			this.storage = storage;
			this.userHandler = userHandler;
			this.wsHandler = wsHandler;
			this.uuidGen = uuidGen;
			this.clock = clock;
		}
	}
	
	@Test
	public void constructFail() throws Exception {
		final GroupsStorage s = mock(GroupsStorage.class);
		final UserHandler u = mock(UserHandler.class);
		final WorkspaceHandler w = mock(WorkspaceHandler.class);
		final Notifications n = mock(Notifications.class);
		
		failConstruct(null, u, w, n, new NullPointerException("storage"));
		failConstruct(s, null, w, n, new NullPointerException("userHandler"));
		failConstruct(s, u, null, n, new NullPointerException("wsHandler"));
		failConstruct(s, u, w, null, new NullPointerException("notifications"));
	}
	
	private void failConstruct(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final WorkspaceHandler wsHandler,
			final Notifications notifications,
			final Exception expected) {
		try {
			new Groups(storage, userHandler, wsHandler, notifications);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void createGroupMinimal() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		final Group ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build());
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		assertThat("incorrect group", ret, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build()));
	}
	
	@Test
	public void createGroupMaximal() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		final Group ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name"))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build());
		
		assertThat("incorrect group", ret, is(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build()));
	}
	
	@Test
	public void createGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;

		failCreateGroup(g, null, PARAMS, new NullPointerException("userToken"));
		failCreateGroup(g, new Token("t"), null, new NullPointerException("createParams"));
	}
	
	@Test
	public void createGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failCreateGroup(mocks.groups, new Token("token"), PARAMS, new InvalidTokenException());
	}
	
	@Test
	public void createGroupFailGroupExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		doThrow(new GroupExistsException("bar")).when(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build(),
				new GroupExistsException("bar"));
	}
	
	@Test
	public void createGroupFailMissingGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build(),
				new RuntimeException(
						"Just created a group and it's already gone. Something's really broken"));
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
	}
	
	private void failCreateGroup(
			final Groups g,
			final Token t,
			final GroupCreationParams p,
			final Exception expected) {
		try {
			g.createGroup(t, p);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
