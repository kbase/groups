package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;
import static us.kbase.test.groups.TestCommon.inst;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UUIDGenerator;
import us.kbase.groups.core.UserHandler;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ClosedRequestException;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchResourceTypeException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.FieldValidators;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.test.groups.TestCommon;

public class GroupsTest {
	
	// probably some of these tests could be DRYed up a bit. This is left as an exercise for
	// the reader.
	
	private static final GroupCreationParams PARAMS;
	static {
		try {
			PARAMS = GroupCreationParams.getBuilder(
					new GroupID("i"), new GroupName("n")).build();
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests newb", e);
		}
	}
	
	private static TestMocks initTestMocks() throws Exception {
		final GroupsStorage storage = mock(GroupsStorage.class);
		final UserHandler uh = mock(UserHandler.class);
		final ResourceHandler wh = mock(ResourceHandler.class);
		final ResourceHandler ch = mock(ResourceHandler.class);
		final FieldValidators val = mock(FieldValidators.class);
		final Notifications notis = mock(Notifications.class);
		final UUIDGenerator uuidGen = mock(UUIDGenerator.class);
		final Clock clock = mock(Clock.class);
		
		
		final Constructor<Groups> c = Groups.class.getDeclaredConstructor(
				GroupsStorage.class, UserHandler.class, Map.class, FieldValidators.class,
				Notifications.class, UUIDGenerator.class, Clock.class);
		c.setAccessible(true);
		final Groups instance = c.newInstance(
				storage,
				uh,
				ImmutableMap.of(
						new ResourceType("workspace"), wh,
						new ResourceType("catalogmethod"), ch),
				val,
				notis,
				uuidGen,
				clock);
		return new TestMocks(instance, storage, uh, wh, ch, val, notis, uuidGen, clock);
	}
	
	public static class TestMocks {
		
		public final Groups groups;
		public final GroupsStorage storage;
		public final UserHandler userHandler;
		public final ResourceHandler wsHandler;
		public final ResourceHandler catHandler;
		public final FieldValidators validators;
		public final Notifications notifs;
		public final UUIDGenerator uuidGen;
		public final Clock clock;
		
		private TestMocks(
				final Groups groups,
				final GroupsStorage storage,
				final UserHandler userHandler,
				final ResourceHandler wsHandler,
				final ResourceHandler catHandler,
				final FieldValidators validators,
				final Notifications notifs,
				final UUIDGenerator uuidGen,
				final Clock clock) {
			this.groups = groups;
			this.storage = storage;
			this.userHandler = userHandler;
			this.wsHandler = wsHandler;
			this.catHandler = catHandler;
			this.validators = validators;
			this.notifs = notifs;
			this.uuidGen = uuidGen;
			this.clock = clock;
		}
	}
	
	@Test
	public void constructFail() throws Exception {
		final GroupsStorage s = mock(GroupsStorage.class);
		final UserHandler u = mock(UserHandler.class);
		final ResourceHandler rh = mock(ResourceHandler.class);
		final Map<ResourceType, ResourceHandler> h = Collections.emptyMap();
		final FieldValidators v = mock(FieldValidators.class);
		final Notifications n = mock(Notifications.class);
		
		failConstruct(null, u, h, v, n, new NullPointerException("storage"));
		failConstruct(s, null, h, v, n, new NullPointerException("userHandler"));
		failConstruct(s, u, null, v, n, new NullPointerException("resourceHandlers"));
		failConstruct(s, u, h, null, n, new NullPointerException("validators"));
		failConstruct(s, u, h, v, null, new NullPointerException("notifications"));
		
		failConstruct(s, u, ImmutableMap.of(
				new ResourceType("ws"), rh,
				new ResourceType("user"), rh),
				v, n,
				new IllegalArgumentException(
						"resourceHandlers cannot contain built in type user"));
	}
	
	private void failConstruct(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final Map<ResourceType, ResourceHandler> handlers,
			final FieldValidators validators,
			final Notifications notifications,
			final Exception expected) {
		try {
			new Groups(storage, userHandler, handlers, validators, notifications);
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
		
		final GroupView ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name")).build());
		
		verifyZeroInteractions(mocks.validators);
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		
		assertThat("incorrect group", ret, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build(), new UserName("foo"))
				.withStandardView(true)
				.withResourceType(new ResourceType("workspace"))
				.withResourceType(new ResourceType("catalogmethod"))
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
				.withCustomField(new NumberedCustomField("foo-26"), "yay")
				.build());
		
		final GroupView ret = mocks.groups.createGroup(new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("desc"))
						.withCustomField(new NumberedCustomField("foo-26"),
								StringField.from("yay"))
						.withCustomField(new NumberedCustomField("a"), StringField.remove())
						.withCustomField(new NumberedCustomField("b"), StringField.noAction())
						.build())
				.withType(GroupType.TEAM)
				.build());
		
		verify(mocks.validators).validate(new NumberedCustomField("foo-26"), "yay");
		verifyNoMoreInteractions(mocks.validators);
		
		verify(mocks.storage).createGroup(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo-26"), "yay")
				.build());
		
		assertThat("incorrect group", ret, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.withCustomField(new NumberedCustomField("foo-26"), "yay")
				.build(), new UserName("foo"))
				.withStandardView(true)
				.withResourceType(new ResourceType("workspace"))
				.withResourceType(new ResourceType("catalogmethod"))
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
	public void createGroupFailNoField() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		doThrow(new NoSuchCustomFieldException("var"))
				.when(mocks.validators).validate(new NumberedCustomField("var"), "7");
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("var"), StringField.from("7"))
						.build())
				.build(),
				new NoSuchCustomFieldException("var"));
	}

	// test illegal parameter on update
	@Test
	public void createGroupFailMissingField() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		doThrow(new MissingParameterException("foo"))
				.when(mocks.validators).validate(new NumberedCustomField("var"), "7");
		
		failCreateGroup(mocks.groups, new Token("token"), GroupCreationParams
				.getBuilder(new GroupID("bar"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("var"), StringField.from("7"))
						.build())
				.build(),
				new RuntimeException(
						"This should be impossible. Please turn reality off and on again"));
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
	
	@Test
	public void updateGroupNoop() throws Exception {
		final TestMocks mocks = initTestMocks();
		mocks.groups.updateGroup(new Token("toketoke"), GroupUpdateParams
				.getBuilder(new GroupID("gid"))
				.build());
		
		verifyZeroInteractions(mocks.userHandler);
		verifyZeroInteractions(mocks.storage);
		verifyZeroInteractions(mocks.validators);
	}
	
	@Test
	public void updateGroupOwner() throws Exception {
		updateGroup(new UserName("own"));
	}
	
	@Test
	public void updateGroupAdmin() throws Exception {
		updateGroup(new UserName("admin"));
	}

	private void updateGroup(final UserName user) throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("toketoke"))).thenReturn(user);
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(30000));
		
		mocks.groups.updateGroup(new Token("toketoke"), GroupUpdateParams
				.getBuilder(new GroupID("gid"))
				.withName(new GroupName("new name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-26"),
								StringField.from("yay"))
						.withCustomField(new NumberedCustomField("a"), StringField.remove())
						.withCustomField(new NumberedCustomField("b"), StringField.noAction())
						.build())
				.build());
		
		verify(mocks.validators).validate(new NumberedCustomField("foo-26"), "yay");
		verifyNoMoreInteractions(mocks.validators);
		
		verify(mocks.storage).updateGroup(GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withName(new GroupName("new name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo-26"),
								StringField.from("yay"))
						.withCustomField(new NumberedCustomField("a"), StringField.remove())
						.withCustomField(new NumberedCustomField("b"), StringField.noAction())
						.build())
				.build(),
				inst(30000));
	}
	
	@Test
	public void updateGroupFailNulls() throws Exception {
		final Groups g = initTestMocks().groups;
		
		failUpdateGroup(g, null, GroupUpdateParams.getBuilder(new GroupID("i")).build(),
				new NullPointerException("userToken"));
		failUpdateGroup(g, new Token("t"), null, new NullPointerException("updateParams"));
	}
	
	@Test
	public void updateGroupFailMissingField() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		doThrow(new MissingParameterException("foo"))
				.when(mocks.validators).validate(new NumberedCustomField("var"), "7");
		
		failUpdateGroup(mocks.groups, new Token("token"), GroupUpdateParams
				.getBuilder(new GroupID("bar"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("var"), StringField.from("7"))
						.build())
				.build(),
				new RuntimeException(
						"This should be impossible. Please turn reality off and on again"));
	}
	
	@Test
	public void updateGroupFailIllegalField() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		doThrow(new IllegalParameterException("foo"))
				.when(mocks.validators).validate(new NumberedCustomField("var"), "7");
		
		failUpdateGroup(mocks.groups, new Token("token"), GroupUpdateParams
				.getBuilder(new GroupID("bar"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("var"), StringField.from("7"))
						.build())
				.build(),
				new IllegalParameterException("foo"));
	}
	
	@Test
	public void updateGroupFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("toketoke"))).thenReturn(new UserName("mem"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withAdministrator(new UserName("admin"))
				.withMember(new UserName("mem"))
				.build());
		
		failUpdateGroup(mocks.groups, new Token("toketoke"),
				GroupUpdateParams.getBuilder(new GroupID("gid")).withType(GroupType.TEAM).build(),
				new UnauthorizedException("User mem may not administrate group gid"));
	}
	
	private void failUpdateGroup(
			final Groups g,
			final Token t,
			final GroupUpdateParams p,
			final Exception expected) {
		try {
			g.updateGroup(t, p);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void getGroupNoTokenNoResources() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.withMember(new UserName("baz"))
				.build());

		final GroupView g = mocks.groups.getGroup(null, new GroupID("bar"));
		
		assertThat("incorrect group", g, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build(), null)
				.withStandardView(true)
				.withResourceType(new ResourceType("workspace"))
				.withResourceType(new ResourceType("catalogmethod"))
				.build()));
	}
	
	@Test
	public void getGroupNoToken() throws Exception {
		// can get public resources
		// tests that there's still a resource entry in the group view if missing from the group
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.withMember(new UserName("baz"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("92")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("86")))
				.build());

		when(mocks.wsHandler.getResourceInformation(
				null, set(new ResourceID("92"), new ResourceID("86")), true))
				.thenReturn(ResourceInformationSet.getBuilder(null)
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"public", true)
						.build());
		
		final GroupView g = mocks.groups.getGroup(null, new GroupID("bar"));
		
		assertThat("incorrect group", g, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("desc")
				.withType(GroupType.TEAM)
				.build(), null)
				.withStandardView(true)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceType("workspace"), ResourceInformationSet
						.getBuilder(null)
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"public", true)
						.build())
				.build()));
	}
	
	@Test
	public void getGroupNonMemberToken() throws Exception {
		// can get public and admin'd resources
		// tests that there's still a resource entry in the group view if missing from the group
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withMember(new UserName("baz"))
				.withAdministrator(new UserName("whoo"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("92")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("57")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("86")))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("whee"));

		when(mocks.wsHandler.getResourceInformation(
				new UserName("whee"), set(new ResourceID("92"), new ResourceID("57"),
						new ResourceID("86")), true))
				.thenReturn(ResourceInformationSet.getBuilder(new UserName("whee"))
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("57")),
								"name", "my ws2")
						.build());
		
		final GroupView g = mocks.groups.getGroup(new Token("token"), new GroupID("bar"));
		
		assertThat("incorrect group", g, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withAdministrator(new UserName("whoo"))
				.build(), new UserName("whee"))
				.withStandardView(true)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceType("workspace"), ResourceInformationSet
						.getBuilder(new UserName("whee"))
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("57")),
								"name", "my ws2")
						.build())
				.build()));
	}
	
	@Test
	public void getGroupMemberTokenWithNonexistentResources() throws Exception {
		// tests non existent resource code
		// can get all resources
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("other desc")
				.withType(GroupType.PROJECT)
				.withMember(new UserName("baz"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("92")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("6")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("57")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(
								new ResourceAdministrativeID("mod1"),
								new ResourceID("mod1.meth1")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(
								new ResourceAdministrativeID("mod2"),
								new ResourceID("mod2.meth2")))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("baz"));
		when(mocks.wsHandler.getResourceInformation(
				new UserName("baz"), set(new ResourceID("92"), new ResourceID("6"),
						new ResourceID("57")), false))
				.thenReturn(ResourceInformationSet.getBuilder(new UserName("baz"))
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("6")),
								"name", "my other ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("57")),
								"name", "my ws2")
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("34")))
						// will throw error, should ignore
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("86")))
						.build());
		when(mocks.catHandler.getResourceInformation(
				new UserName("baz"), set(new ResourceID("mod1.meth1"),
						new ResourceID("mod2.meth2")), false))
				.thenReturn(ResourceInformationSet.getBuilder(new UserName("baz"))
						.withResourceDescriptor(new ResourceDescriptor(
								new ResourceAdministrativeID("mod2"),
								new ResourceID("mod2.meth2")))
						.build());
		when(mocks.clock.instant()).thenReturn(inst(5600));
		doThrow(new NoSuchResourceException("86")).when(mocks.storage)
				.removeResource(new GroupID("bar"), new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("86")), inst(5600));
		
		
		final GroupView g = mocks.groups.getGroup(new Token("token"), new GroupID("bar"));
		
		verify(mocks.storage).removeResource(new GroupID("bar"), new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("34")), inst(5600));
		
		assertThat("incorrect group", g, is(GroupView.getBuilder(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withDescription("other desc")
				.withType(GroupType.PROJECT)
				.withMember(new UserName("baz"))
				.build(), new UserName("baz"))
				.withStandardView(true)
				.withResource(new ResourceType("workspace"), ResourceInformationSet
						.getBuilder(new UserName("baz"))
						.withResourceField(new ResourceDescriptor(new ResourceID("92")),
								"name", "my ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("6")),
								"name", "my other ws")
						.withResourceField(new ResourceDescriptor(new ResourceID("57")),
								"name", "my ws2")
						//TODO NNOW remove nonexistant resources from view
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("34")))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("86")))
						.build())
				.withResource(new ResourceType("catalogmethod"), ResourceInformationSet
						.getBuilder(new UserName("baz"))
						.withResourceDescriptor(new ResourceDescriptor(
								new ResourceAdministrativeID("mod2"),
								new ResourceID("mod2.meth2")))
						.build())
				.build()));
	}
	
	@Test
	public void getGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failGetGroup(mocks.groups, null, new GroupID("bar"), new NoSuchGroupException("bar"));
	}
	
	@Test
	public void getGroupFailAuthException() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build());
		when(mocks.userHandler.getUser(new Token("token")))
				.thenThrow(new AuthenticationException(
						ErrorType.AUTHENTICATION_FAILED, "oh hecky darn"));
		
		failGetGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "oh hecky darn"));
	}
	
	@Test
	public void getGroupFailMissingResourceType() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("baz"))
				.withResource(new ResourceType("workspyce"),
						new ResourceDescriptor(new ResourceID("not a ws id")))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("baz"));
		
		failGetGroup(mocks.groups, new Token("token"), new GroupID("bar"), new RuntimeException(
				"Group bar has workspyce data without a configured handler"));
	}
	
	@Test
	public void getGroupFailIllegalResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("baz"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("not a ws id")))
				.build());
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("baz"));
		when(mocks.wsHandler.getResourceInformation(
				new UserName("baz"), set(new ResourceID("not a ws id")), false))
				.thenThrow(new IllegalResourceIDException("oh heck"));
		
		failGetGroup(mocks.groups, new Token("token"), new GroupID("bar"), new RuntimeException(
				"Illegal data associated with group bar: 30030 Illegal resource ID: oh heck"));
	}
	
	private void failGetGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final Exception expected) {
		try {
			g.getGroup(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroupExists(new GroupID("g1"))).thenReturn(true);
		when(mocks.storage.getGroupExists(new GroupID("g2"))).thenReturn(false);
		
		assertThat("incorrect group exists", mocks.groups.getGroupExists(new GroupID("g1")),
				is(true));
		assertThat("incorrect group exists", mocks.groups.getGroupExists(new GroupID("g2")),
				is(false));
	}
	
	@Test
	public void getGroupExistsFail() throws Exception {
		try {
			initTestMocks().groups.getGroupExists(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("groupID"));
		}
	}
	
	@Test
	public void getGroupsEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroups(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("ex")
				.withNullableSortAscending(false)
				.build()))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect groups", mocks.groups.getGroups(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("ex")
				.withNullableSortAscending(false)
				.build()),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getGroups() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.storage.getGroups(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("someex")
				.build()))
				.thenReturn(Arrays.asList(
						Group.getBuilder(
								new GroupID("id1"), new GroupName("name1"), new UserName("u1"),
								new CreateAndModTimes(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
								.build(),
						Group.getBuilder(
								new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
								new CreateAndModTimes(Instant.ofEpochMilli(10000)))
								.withDescription("desc")
								.withType(GroupType.PROJECT)
								.withMember(new UserName("whee"))
								.withAdministrator(new UserName("whoo"))
								.build()
						));
		
		assertThat("incorrect groups", mocks.groups.getGroups(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("someex")
				.build()),
				is(Arrays.asList(
						GroupView.getBuilder(Group.getBuilder(
								new GroupID("id1"), new GroupName("name1"), new UserName("u1"),
								new CreateAndModTimes(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
								.build(), null)
								.build(),
						GroupView.getBuilder(Group.getBuilder(
								new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
								new CreateAndModTimes(Instant.ofEpochMilli(10000)))
								.withDescription("desc")
								.withType(GroupType.PROJECT)
								.withAdministrator(new UserName("whoo"))
								.build(), null)
								.build())
						));
	}

	@Test
	public void getGroupsFail() throws Exception {
		try {
			initTestMocks().groups.getGroups(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("params"));
		}
	}
	
	@Test
	public void requestGroupMembership() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final GroupRequest req = mocks.groups.requestGroupMembership(
				new Token("token"), new GroupID("bar"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("own")),
				Group.getBuilder(
						new GroupID("bar"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.build()
				);
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.build()
				));
	}
	
	@Test
	public void requestGroupMembershipFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failRequestGroupMembership(g, null, new GroupID("i"),
				new NullPointerException("userToken"));
		failRequestGroupMembership(g, new Token("t"), null,
				new NullPointerException("groupID"));
	}
	
	@Test
	public void requestGroupMembershipFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("i"),
				new InvalidTokenException());
	}
	
	@Test
	public void requestGroupMembershipFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("foo"));
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new NoSuchGroupException("bar"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsOwner() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserIsMemberException("User own is already a member of group bar"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserIsMemberException("User admin is already a member of group bar"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("u3"));
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failRequestGroupMembership(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserIsMemberException("User u3 is already a member of group bar"));
	}
	
	private void failRequestGroupMembership(
			final Groups g,
			final Token t,
			final GroupID i,
			final Exception expected) {
		try {
			g.requestGroupMembership(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void inviteUserToGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final GroupRequest req = mocks.groups.inviteUserToGroup(
				new Token("token"), new GroupID("bar"), new UserName("foo"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("admin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("foo")))
				.build());
		
		verify(mocks.notifs).notify(
				Arrays.asList(new UserName("foo")),
				Group.getBuilder(
						new GroupID("bar"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.withAdministrator(new UserName("admin"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("admin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("foo")))
						.build()
				);
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("bar"), new UserName("admin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
						.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("foo")))
				.build()
				));
	}
	
	@Test
	public void inviteUserToGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final GroupID i = new GroupID("i");
		final Token t = new Token("t");
		final UserName u = new UserName("u");
		
		failInviteUserToGroup(g, null, i, u, new NullPointerException("userToken"));
		failInviteUserToGroup(g, t, null, u, new NullPointerException("groupID"));
		failInviteUserToGroup(g, t, i, null, new NullPointerException("newMember"));
	}
	
	@Test
	public void inviteUserToGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("i"),
				new UserName("u"), new InvalidTokenException());
	}
	
	@Test
	public void inviteUserToGroupFailInvalidUser() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(false);
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("i"),
				new UserName("foo"), new NoSuchUserException("foo"));
	}
	
	@Test
	public void inviteUserToGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar")))
				.thenThrow(new NoSuchGroupException("bar"));
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new NoSuchGroupException("bar"));
	}
	
	@Test
	public void inviteUserToGroupFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notown"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new UnauthorizedException(
						"User notown may not administrate group bar"));
	}
	
	@Test
	public void inviteUserToGroupFailIsMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("u1"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("u1"), new UserIsMemberException(
						"User u1 is already a member of group bar"));
	}
	
	@Test
	public void inviteUserToGroupFailIsAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.userHandler.isValidUser(new UserName("admin"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("admin"), new UserIsMemberException(
						"User admin is already a member of group bar"));
	}
	
	@Test
	public void inviteUserToGroupFailRequestExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.userHandler.isValidUser(new UserName("foo"))).thenReturn(true);
		when(mocks.storage.getGroup(new GroupID("bar"))).thenReturn(Group.getBuilder(
				new GroupID("bar"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		doThrow(new RequestExistsException("someid")).when(mocks.storage).storeRequest(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("bar"), new UserName("own"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(1209610000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("foo")))
						.build()
				);
		
		failInviteUserToGroup(mocks.groups, new Token("token"), new GroupID("bar"),
				new UserName("foo"), new RequestExistsException("someid"));
	}
	
	private void failInviteUserToGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final UserName invite,
			final Exception expected) {
		try {
			g.inviteUserToGroup(t, i, invite);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestMembershipCreatorOpen() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b,
				set(GroupRequestUserAction.CANCEL));
	}
	
	@Test
	public void getRequestMembershipCreatorClosed() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withStatus(GroupRequestStatus.canceled()),
				set());
	}
	
	@Test
	public void getRequestMembershipAdminOpen() throws Exception {
		getRequest(
				new UserName("own"),
				b -> b,
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	}
	
	@Test
	public void getRequestMembershipAdminClosed() throws Exception {
		getRequest(
				new UserName("admin"),
				b -> b.withStatus(GroupRequestStatus.expired()),
				set());
	}
	
	@Test
	public void getRequestResourceCreatorOpen() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("78"))),
				set(GroupRequestUserAction.CANCEL));
	}
	
	@Test
	public void getRequestResourceCreatorClosed() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("78")))
						.withStatus(GroupRequestStatus.canceled()),
				set());
	}
	
	@Test
	public void getRequestResourceAdminOpen() throws Exception {
		getRequest(
				new UserName("own"),
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("78"))),
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	}
	
	@Test
	public void getRequestResourceAdminClosed() throws Exception {
		getRequest(
				new UserName("admin"),
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("78")))
						.withStatus(GroupRequestStatus.expired()),
				set());
	}
	
	@Test
	public void getRequestInviteCreatorOpen() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite"))),
				set(GroupRequestUserAction.CANCEL));
	}
	
	@Test
	public void getRequestInviteCreatorClosed() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.withStatus(GroupRequestStatus.canceled()),
				set());
	}
	
	@Test
	public void getRequestInviteTargetOpen() throws Exception {
		getRequest(
				new UserName("invite"),
				b -> b.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite"))),
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	}
	
	@Test
	public void getRequestInviteTargetClosed() throws Exception {
		getRequest(
				new UserName("invite"),
				b -> b.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.withStatus(GroupRequestStatus.expired()),
				set());
	}
	
	@Test
	public void getRequestInviteResourceCreatorOpen() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("87"))),
				set(GroupRequestUserAction.CANCEL));
	}
	
	@Test
	public void getRequestInviteResourceCreatorClosed() throws Exception {
		getRequest(
				new UserName("user"),
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("87")))
						.withStatus(GroupRequestStatus.canceled()),
				set());
	}
	
	@Test
	public void getRequestInviteResourceTargetOpen() throws Exception {
		getRequest(
				new UserName("wsadmin"),
				b -> b.withType(RequestType.INVITE)
					.withResourceType(new ResourceType("workspace"))
					.withResource(new ResourceDescriptor(new ResourceID("87"))),
				set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	}
	
	@Test
	public void getRequestInviteResourceTargetClosed() throws Exception {
		getRequest(
				new UserName("wsadmin"),
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("87")))
						.withStatus(GroupRequestStatus.expired()),
				set());
	}
	
	private void getRequest(
			final UserName user,
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> buildFn,
			final Set<GroupRequestUserAction> expectedActions)
			throws Exception {
		
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(user);
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(buildFn.apply(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("87"), new UserName("wsadmin")))
				.thenReturn(true);
		
		final GroupRequestWithActions req = mocks.groups.getRequest(
				new Token("token"), new RequestID(id));
		
		assertThat("incorrect request", req, is(new GroupRequestWithActions(
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
						.build(),
				expectedActions)));
	}
	
	@Test
	public void getRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final UUID id = UUID.randomUUID();
		
		failGetRequest(g, null, new RequestID(id), new NullPointerException("userToken"));
		failGetRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}
	
	@Test
	public void getRequestFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(UUID.randomUUID()),
				new InvalidTokenException());
	}
	
	@Test
	public void getRequestFailNoSuchRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenThrow(
				new NoSuchRequestException(id.toString()));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new NoSuchRequestException(id.toString()));
	}
	
	@Test
	public void getRequestFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("invite")))
				.withStatus(GroupRequestStatus.expired())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid")))
				.thenThrow(new NoSuchGroupException("gid"));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Request %s's group doesn't exist: 50000 No such group: gid", id)));
	}
	
	@Test
	public void getRequestFailMembershipCantView() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("requester"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User user may not access request " + id));
	}
	
	@Test
	public void getRequestFailInviteCantViewAsAdmin() throws Exception {
		// see notes in the getRequest subfunction code about this.
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("invite")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User own may not access request " + id));
	}
	
	@Test
	public void getRequestFailRequestResourceCantView() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("requester"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("67")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User user may not access request " + id));
	}
	
	@Test
	public void getRequestFailInviteResourceNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("96")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("96"), new UserName("someuser")))
				.thenReturn(false);
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User someuser may not access request " + id));
	}
	
	@Test
	public void getRequestFailInviteResourceNoSuchResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("96")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("96"), new UserName("someuser")))
				.thenThrow(new NoSuchResourceException("foo"));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User someuser may not access request " + id));
	}
	
	@Test
	public void getRequestFailInviteResourceIllegalValue() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("mod"),
						new ResourceID("mod.meth")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.isAdministrator(
				new ResourceID("mod.meth"), new UserName("someuser")))
				.thenThrow(new IllegalResourceIDException("foo"));
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Illegal value stored in request %s: 30030 Illegal resource ID: foo",
						id.toString())));
	}
	
	@Test
	public void getRequestFailInviteUserIllegalUserName() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResource(new ResourceDescriptor(new ResourceID("bad*user")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id), new RuntimeException(
				String.format("Invalid data in request %s: 30010 Illegal user name: " +
				"Illegal character in user name bad*user: *",
				id.toString())));
	}
	
	@Test
	public void getRequestFailInviteResourceNoSuchResourceType() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("caterlawgmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("mod"),
						new ResourceID("mod.meth")))
				.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failGetRequest(mocks.groups, new Token("token"), new RequestID(id), new RuntimeException(
				"No handler configured for resource type caterlawgmethod in request " +
				id.toString()));
	}
	
	private void failGetRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.getRequest(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsForRequesterEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequestsByRequester(
				new UserName("user"), GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(5600))
						.build()))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForRequester(
				new Token("token"), GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(5600))
						.build()),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForRequester() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequestsByRequester(
				new UserName("user"), GetRequestsParams.getBuilder()
						.withNullableIncludeClosed(true)
						.withNullableSortAscending(false)
						.build()))
				.thenReturn(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withType(RequestType.INVITE)
								.withResource(ResourceDescriptor.from(new UserName("invite")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
						));
				
		assertThat("incorrect requests", mocks.groups.getRequestsForRequester(
				new Token("token"), GetRequestsParams.getBuilder()
						.withNullableIncludeClosed(true)
						.withNullableSortAscending(false)
						.build()),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withType(RequestType.INVITE)
								.withResource(ResourceDescriptor.from(new UserName("invite")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
						)));
	}
	
	@Test
	public void getRequestsForRequesterFail() throws Exception {
		failGetRequestsForRequester(null, GetRequestsParams.getBuilder().build(),
				new NullPointerException("userToken"));
		failGetRequestsForRequester(new Token("t"), null, new NullPointerException("params"));
	}
	
	private void failGetRequestsForRequester(
			final Token token,
			final GetRequestsParams params,
			final Exception expected)
			throws Exception {
		final TestMocks mocks = initTestMocks();
		try {
			mocks.groups.getRequestsForRequester(token, params);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsForTargetEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.wsHandler.getAdministratedResources(new UserName("user")))
				.thenReturn(set(ResourceAdministrativeID.from(96)));
		when(mocks.catHandler.getAdministratedResources(new UserName("user")))
				.thenReturn(set(new ResourceAdministrativeID("mod")));
		when(mocks.storage.getRequestsByTarget(
				new UserName("user"), ImmutableMap.of(
						new ResourceType("workspace"), set(ResourceAdministrativeID.from(96)),
						new ResourceType("catalogmethod"),
								set(new ResourceAdministrativeID("mod"))),
				GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(10000))
						.withNullableIncludeClosed(true)
						.build()))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForTarget(
				new Token("token"), GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(10000))
						.withNullableIncludeClosed(true)
						.build()),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.wsHandler.getAdministratedResources(new UserName("target"))).thenReturn(
				set(ResourceAdministrativeID.from(96), ResourceAdministrativeID.from(24)));
		when(mocks.catHandler.getAdministratedResources(new UserName("target")))
				.thenReturn(set(new ResourceAdministrativeID("mod"),
						new ResourceAdministrativeID("mod2")));
		when(mocks.storage.getRequestsByTarget(
				new UserName("target"),
				ImmutableMap.of(
						new ResourceType("workspace"),
							set(ResourceAdministrativeID.from(96),
									ResourceAdministrativeID.from(24)),
						new ResourceType("catalogmethod"),
						set(new ResourceAdministrativeID("mod"),
								new ResourceAdministrativeID("mod2"))),
				GetRequestsParams.getBuilder()
						.withNullableSortAscending(false)
						.withNullableExcludeUpTo(inst(10000))
						.build()))
				.thenReturn(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.build())
								.withType(RequestType.INVITE)
								.withResource(ResourceDescriptor.from(new UserName("target")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withType(RequestType.INVITE)
								.withResourceType(new ResourceType("workspace"))
								.withResource(new ResourceDescriptor(new ResourceID("24")))
								.withStatus(GroupRequestStatus.accepted(new UserName("wsadmin")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withType(RequestType.INVITE)
								.withResourceType(new ResourceType("catalogmethod"))
								.withResource(new ResourceDescriptor(
										new ResourceAdministrativeID("mod2"),
										new ResourceID("mod2.meth")))
								.withStatus(GroupRequestStatus.canceled())
								.build()
						));
		
		assertThat("incorrect requests", mocks.groups.getRequestsForTarget(
				new Token("token"), GetRequestsParams.getBuilder()
						.withNullableSortAscending(false)
						.withNullableExcludeUpTo(inst(10000))
						.build()),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.withType(RequestType.INVITE)
								.withResource(ResourceDescriptor.from(new UserName("target")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withType(RequestType.INVITE)
								.withResourceType(new ResourceType("workspace"))
								.withResource(new ResourceDescriptor(new ResourceID("24")))
								.withStatus(GroupRequestStatus.accepted(new UserName("wsadmin")))
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withType(RequestType.INVITE)
								.withResourceType(new ResourceType("catalogmethod"))
								.withResource(new ResourceDescriptor(
								new ResourceAdministrativeID("mod2"),
								new ResourceID("mod2.meth")))
								.withStatus(GroupRequestStatus.canceled())
								.build()
						)));
	}
	
	@Test
	public void getRequestsForTargetFail() throws Exception {
		failGetRequestsForTarget(null, GetRequestsParams.getBuilder().build(),
				new NullPointerException("userToken"));
		failGetRequestsForTarget(new Token("t"), null, new NullPointerException("params"));
	}
	
	private void failGetRequestsForTarget(
			final Token token,
			final GetRequestsParams params,
			final Exception expected)
			throws Exception {
		final TestMocks mocks = initTestMocks();
		try {
			mocks.groups.getRequestsForTarget(token, params);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestsForGroupEmpty() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.storage.getRequestsByGroup(
				new GroupID("gid"), GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(21000))
						.withNullableIncludeClosed(true)
						.build()))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect requests", mocks.groups.getRequestsForGroup(
				new Token("token"), new GroupID("gid"), GetRequestsParams.getBuilder()
						.withNullableExcludeUpTo(inst(21000))
						.withNullableIncludeClosed(true)
						.build()),
				is(Collections.emptyList()));
	}
	
	@Test
	public void getRequestsForGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.storage.getRequestsByGroup(
				new GroupID("gid"), GetRequestsParams.getBuilder()
						.withNullableIncludeClosed(true)
						.withNullableSortAscending(false)
						.build()))
				.thenReturn(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
						));
		
		assertThat("incorrect requests", mocks.groups.getRequestsForGroup(
				new Token("token"), new GroupID("gid"), GetRequestsParams.getBuilder()
						.withNullableIncludeClosed(true)
						.withNullableSortAscending(false)
						.build()),
				is(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
										.build())
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("gid"), new UserName("user"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withStatus(GroupRequestStatus.accepted(new UserName("admin")))
								.build()
				)));
	}
	
	@Test
	public void getRequestsForGroupFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		
		failGetRequestsForGroup(g, null, new GroupID("i"), p,
				new NullPointerException("userToken"));
		failGetRequestsForGroup(g, new Token("t"), null, p, new NullPointerException("groupID"));
		failGetRequestsForGroup(g, new Token("t"), new GroupID("i"), null,
				new NullPointerException("params"));
	}
	
	@Test
	public void getRequestForGroupFailInvalidToken() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenThrow(new InvalidTokenException());
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("i"),
				GetRequestsParams.getBuilder().build(),
				new InvalidTokenException());
	}
	
	@Test
	public void getRequestForGroupFailNoSuchGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid")))
				.thenThrow(new NoSuchGroupException("gid"));
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("gid"),
				GetRequestsParams.getBuilder().build(),
				new NoSuchGroupException("gid"));
	}
	
	@Test
	public void getRequestForGroupFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("u2"))
				.build());
		
		failGetRequestsForGroup(mocks.groups, new Token("token"), new GroupID("gid"),
				GetRequestsParams.getBuilder().build(),
				new UnauthorizedException("User u1 cannot view requests for group gid"));
	}
	
	private void failGetRequestsForGroup(
			final Groups g,
			final Token t,
			final GroupID i,
			final GetRequestsParams params,
			final Exception expected) {
		try {
			g.getRequestsForGroup(t, i, params);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void cancelRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.withStatus(GroupRequestStatus.canceled())
						.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(15000));
		
		final GroupRequest req = mocks.groups.cancelRequest(new Token("token"), new RequestID(id));
		
		verify(mocks.storage).closeRequest(new RequestID(id), GroupRequestStatus.canceled(),
				Instant.ofEpochMilli(15000));
		verify(mocks.notifs).cancel(new RequestID(id));
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(15000))
						.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("invite")))
				.withStatus(GroupRequestStatus.canceled())
				.build()));
	}
	
	@Test
	public void cancelRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failCancelRequest(g, null, new RequestID(UUID.randomUUID()),
				new NullPointerException("userToken"));
		failCancelRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}
	
	@Test
	public void cancelRequestFailNoSuchRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id)))
				.thenThrow(new NoSuchRequestException(id.toString()));
		
		failCancelRequest(mocks.groups, new Token("token"), new RequestID(id),
				new NoSuchRequestException(id.toString()));
	}
	
	@Test
	public void cancelRequestFailUnauthed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("otheruser"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.build());
		
		failCancelRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User otheruser may not cancel request " + id));
	}
	
	@Test
	public void cancelRequestFailClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("invite")))
						.withStatus(GroupRequestStatus.accepted(new UserName("someguy")))
						.build());
		
		failCancelRequest(mocks.groups, new Token("token"), new RequestID(id),
				new ClosedRequestException(id + ""));
	}
	
	private void failCancelRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.cancelRequest(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private interface FuncExcept<T, R> {
		
		R apply(T t) throws Exception;
	}
	
	@Test
	public void denyRequestAdminRequestMembership() throws Exception {
		denyRequestAdmin(null, "own", b -> b);
	}
	
	@Test
	public void denyRequestAdminWhitespaceReasonRequestResource() throws Exception {
		denyRequestAdmin("    \t    ", "admin",
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("86"))));
	}
	
	@Test
	public void denyRequestAdminReasonInviteResource() throws Exception {
		denyRequestAdmin(" reason  ", "wsadmin",
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("86"))));
	}
	
	private void denyRequestAdmin(
			final String reason,
			final String admin,
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> buildFn)
			throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName(admin));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.build()))
						.build(),
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build()))
						.withStatus(GroupRequestStatus.denied(new UserName(admin), reason))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("86"), new UserName("wsadmin")))
				.thenReturn(true);
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(15000));
		
		final GroupRequest req = mocks.groups.denyRequest(
				new Token("token"), new RequestID(id), reason);
		
		verify(mocks.storage).closeRequest(
				new RequestID(id),
				GroupRequestStatus.denied(new UserName(admin), reason),
				Instant.ofEpochMilli(15000));
		verify(mocks.notifs).deny(
				set(),
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build()))
						.withStatus(GroupRequestStatus.denied(new UserName(admin), reason))
						.build());
		
		assertThat("incorrect request", req, is(buildFn.apply(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(15000))
						.build()))
				.withStatus(GroupRequestStatus.denied(new UserName(admin), reason))
				.build()));
	}
	
	@Test
	public void denyRequestTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.withStatus(GroupRequestStatus.denied(new UserName("target"), "reason"))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(15000));
		
		final GroupRequest req = mocks.groups.denyRequest(
				new Token("token"), new RequestID(id), "reason");
		
		verify(mocks.storage).closeRequest(
				new RequestID(id),
				GroupRequestStatus.denied(new UserName("target"), "reason"),
				Instant.ofEpochMilli(15000));
		verify(mocks.notifs).deny(
				set(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.withStatus(GroupRequestStatus.denied(new UserName("target"), "reason"))
						.build());
		
		assertThat("incorrect request", req, is(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(15000))
						.build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("target")))
				.withStatus(GroupRequestStatus.denied(new UserName("target"), "reason"))
				.build()));
	}
	
	@Test
	public void denyRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failDenyRequest(g, null, new RequestID(UUID.randomUUID()),
				new NullPointerException("userToken"));
		failDenyRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}
	
	@Test
	public void denyRequestFailNoGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("invite"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
				.withType(RequestType.INVITE)
				.withResource(ResourceDescriptor.from(new UserName("invite")))
				.withStatus(GroupRequestStatus.expired())
				.build());
		when(mocks.storage.getGroup(new GroupID("gid")))
				.thenThrow(new NoSuchGroupException("gid"));
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Request %s's group doesn't exist: 50000 No such group: gid", id)));
	}
	
	@Test
	public void denyRequestFailNotTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("nottarget"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User nottarget may not deny request " + id));
	}
	
	@Test
	public void denyRequestFailNotAdmin() throws Exception {
		denyRequestFailNotAdmin(b -> b);
	}
	
	@Test
	public void denyRequestFailNotAdminRequestResource() throws Exception {
		denyRequestFailNotAdmin(b -> b.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("foo"),
						new ResourceID("foo.bar"))));
	}
	
	@Test
	public void denyRequestFailNotAdminInviteResource() throws Exception {
		denyRequestFailNotAdmin(b -> b.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("foo"),
						new ResourceID("foo.baz"))));
	}
	
	private void denyRequestFailNotAdmin(
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> buildFn)
			throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.isAdministrator(new ResourceID("foo.bar"), new UserName("notadmin")))
				.thenReturn(false);
		when(mocks.catHandler.isAdministrator(new ResourceID("foo.baz"), new UserName("notadmin")))
				.thenReturn(false);
				
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User notadmin may not deny request " + id));
	}
	
	@Test
	public void denyRequestFailClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.withStatus(GroupRequestStatus.canceled())
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new ClosedRequestException(id + ""));
	}
	
	@Test
	public void denyRequestFailNoSuchResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("56")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("56"), new UserName("notadmin")))
				.thenThrow(new NoSuchResourceException("foo"));
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User notadmin may not deny request " + id));
	}
	
	@Test
	public void denyRequestFailIllegalValue() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("mod"),
								new ResourceID("mod.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.isAdministrator(
				new ResourceID("mod.meth"), new UserName("notadmin")))
				.thenThrow(new IllegalResourceIDException("bar"));
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Illegal value stored in request %s: 30030 Illegal resource ID: bar",
						id.toString())));
	}
	
	@Test
	public void denyRequestFailIllegalUserName() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(new ResourceDescriptor(new ResourceID("bad*user")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id), new RuntimeException(
				String.format("Invalid data in request %s: 30010 Illegal user name: " +
				"Illegal character in user name bad*user: *",
				id.toString())));
	}
	
	@Test
	public void denyRequestFailNoSuchResourceType() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("caterlogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("mod"),
								new ResourceID("mod.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failDenyRequest(mocks.groups, new Token("token"), new RequestID(id), new RuntimeException(
				"No handler configured for resource type caterlogmethod in request " +
				id.toString()));
	}
	
	private void failDenyRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.denyRequest(t, i, null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void acceptRequestAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		acceptRequest(mocks, new UserName("own"),
				set(new UserName("user"), new UserName("admin"), new UserName("a3")), b -> b);

		verify(mocks.storage).addMember(new GroupID("gid"), new UserName("user"), inst(12000));
	}
	
	@Test
	public void acceptRequestTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		acceptRequest(mocks, new UserName("target"),
				set(new UserName("own"), new UserName("admin"), new UserName("a3")),
				b -> b.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target"))));

		verify(mocks.storage).addMember(new GroupID("gid"), new UserName("target"), inst(12000));
	}
	
	@Test
	public void acceptRequestGroupAdminForRequestResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.wsHandler.getAdministrators(new ResourceID("56"))).thenReturn(
				set(new UserName("u1"), new UserName("u2")));
		acceptRequest(mocks, new UserName("admin"),
				set(new UserName("u1"), new UserName("u2"), new UserName("own"),
						new UserName("a3")),
				b -> b.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("56"))));

		verify(mocks.storage).addResource(
				new GroupID("gid"),
				new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("56")),
				inst(12000));
	}
	
	@Test
	public void acceptRequestResourceAdminForInviteResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.catHandler.isAdministrator(new ResourceID("mod.n"), new UserName("catadmin")))
				.thenReturn(true);
		when(mocks.catHandler.getAdministrators(new ResourceID("mod.n"))).thenReturn(
				set(new UserName("catadmin"), new UserName("u4")));
		acceptRequest(mocks, new UserName("catadmin"),
				set(new UserName("admin"), new UserName("u4"), new UserName("own"),
						new UserName("a3")),
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("mod"),
								new ResourceID("mod.n"))));

		verify(mocks.storage).addResource(
				new GroupID("gid"),
				new ResourceType("catalogmethod"),
				new ResourceDescriptor(
						new ResourceAdministrativeID("mod"), new ResourceID("mod.n")),
				inst(12000));
	}

	private void acceptRequest(
			final TestMocks mocks,
			final UserName tokenUser,
			final Set<UserName> targets,
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> buildFn)
			throws Exception {
		
		final UUID id = UUID.randomUUID();
		
		final GroupRequest openreq = buildFn.apply(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
				.build();
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(tokenUser);
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				openreq,
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build()))
						.withStatus(GroupRequestStatus.accepted(tokenUser))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(12000), inst(15000));
		
		final GroupRequest req = mocks.groups.acceptRequest(
				new Token("token"), new RequestID(id));
		
		verify(mocks.storage).closeRequest(
				new RequestID(id),
				GroupRequestStatus.accepted(tokenUser),
				Instant.ofEpochMilli(15000));
		verify(mocks.notifs).accept(targets,
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(15000))
								.build()))
						.withStatus(GroupRequestStatus.accepted(tokenUser))
						.build());
		
		assertThat("incorrect request", req, is(buildFn.apply(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("user"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(15000))
						.build()))
				.withStatus(GroupRequestStatus.accepted(tokenUser))
				.build()));
	}
	
	@Test
	public void acceptRequestFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		
		failAcceptRequest(g, null, new RequestID(UUID.randomUUID()),
				new NullPointerException("userToken"));
		failAcceptRequest(g, new Token("t"), null, new NullPointerException("requestID"));
	}

	@Test
	public void acceptRequestFailNotTarget() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("nottarget"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User nottarget may not accept request " + id));
	}
	
	@Test
	public void acceptRequestFailNotAdmin() throws Exception {
		acceptRequestFailNotAdmin(b -> b);
	}
	
	@Test
	public void acceptRequestResourceFailNotAdmin() throws Exception {
		acceptRequestFailNotAdmin(b -> b.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("34"))));
	}
	
	@Test
	public void acceptRequestResourceFailNotResourceAdmin() throws Exception {
		acceptRequestFailNotAdmin(b -> b.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("55"))));
	}
	
	private void acceptRequestFailNotAdmin(
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> buildFn)
			throws Exception {
		
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				buildFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("55"), new UserName("notadmin")))
				.thenReturn(false);
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User notadmin may not accept request " + id));
	}
	
	@Test
	public void acceptRequestFailClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.withStatus(GroupRequestStatus.expired())
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new ClosedRequestException(id.toString()));
	}
	
	// some of these could probably be merged, although they're not so long as to be
	// really unwieldy
	
	@Test
	public void acceptRequestFailUserIsMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(14000));
		
		doThrow(new UserIsMemberException("you silly")).when(mocks.storage)
				.addMember(new GroupID("gid"), new UserName("user"), inst(14000));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UserIsMemberException("you silly"));
	}
	
	@Test
	public void acceptRequestFailNoSuchGroupOnAdd() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("target"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(ResourceDescriptor.from(new UserName("target")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(14000));
		
		doThrow(new NoSuchGroupException("gid")).when(mocks.storage)
				.addMember(new GroupID("gid"), new UserName("target"), inst(14000));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						"Group gid unexpectedly doesn't exist: 50000 No such group: gid"));
	}
	
	@Test
	public void acceptRequestFailNoSuchResourceOnRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("56")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.wsHandler.getAdministrators(new ResourceID("56"))).thenThrow(
				new NoSuchResourceException("56"));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new NoSuchResourceException("56"));
	}
	
	@Test
	public void acceptRequestFailNoSuchResourceOnInvite() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("catadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("md"),
								new ResourceID("md.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.catHandler.isAdministrator(new ResourceID("md.meth"), new UserName("catadmin")))
				.thenThrow(new NoSuchResourceException("md.meth"));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new UnauthorizedException("User catadmin may not accept request " + id));
	}
	
	@Test
	public void acceptRequestFailNoSuchGroupOnAddResource() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("56")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.wsHandler.getAdministrators(new ResourceID("56"))).thenReturn(
				set(new UserName("u1"), new UserName("u2")));
		when(mocks.clock.instant()).thenReturn(inst(4400));
		doThrow(new NoSuchGroupException("gid")).when(mocks.storage)
				.addResource(
						new GroupID("gid"),
						new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("56")),
						inst(4400));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						"Group gid unexpectedly doesn't exist: 50000 No such group: gid"));
	}
	
	@Test
	public void acceptRequestFailIllegalValueOnRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("4")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.wsHandler.getAdministrators(new ResourceID("4"))).thenThrow(
				new IllegalResourceIDException("foo"));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Illegal value stored in request %s: 30030 Illegal resource ID: foo",
						id.toString())));
	}
	
	@Test
	public void acceptRequestFailIllegalValueOnInvite() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("catadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("md"),
								new ResourceID("md.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		when(mocks.catHandler.isAdministrator(new ResourceID("md.meth"), new UserName("catadmin")))
				.thenThrow(new IllegalResourceIDException("foo"));
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(String.format(
						"Illegal value stored in request %s: 30030 Illegal resource ID: foo",
						id.toString())));
	}
	
	@Test
	public void acceptRequestFailIllegalUserNameOnRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResource(new ResourceDescriptor(new ResourceID("bad*user")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						String.format("Invalid data in request %s: 30010 Illegal user name: " +
						"Illegal character in user name bad*user: *",
						id.toString())));
	}
	
	@Test
	public void acceptRequestFailNoSuchResourceTypeOnRequest() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("worksperce"))
						.withResource(new ResourceDescriptor(new ResourceID("4")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						"No handler configured for resource type worksperce in request " +
						id.toString()));
	}
	
	@Test
	public void acceptRequestFailIllegalUserNameOnInvite() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("catadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResource(new ResourceDescriptor(new ResourceID("bad*user")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						String.format("Invalid data in request %s: 30010 Illegal user name: " +
						"Illegal character in user name bad*user: *",
						id.toString())));
	}
	
	@Test
	public void acceptRequestFailNoSuchResourceTypeOnInvite() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("catadmin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("certerlergmerthod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("md"),
								new ResourceID("md.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.withAdministrator(new UserName("a3"))
				.build());
		
		failAcceptRequest(mocks.groups, new Token("token"), new RequestID(id),
				new RuntimeException(
						"No handler configured for resource type certerlergmerthod in request " +
						id.toString()));
	}
	
	private void failAcceptRequest(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.acceptRequest(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeMemberSelf() throws Exception {
		removeMember(new UserName("user"));
	}
	
	@Test
	public void removeMemberOwner() throws Exception {
		removeMember(new UserName("own"));
	}
	
	@Test
	public void removeMemberAdmin() throws Exception {
		removeMember(new UserName("admin"));
	}

	private void removeMember(final UserName user) throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(user);
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("user"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(12000));
		
		mocks.groups.removeMember(new Token("token"), new GroupID("gid"), new UserName("user"));
		
		verify(mocks.storage).removeMember(new GroupID("gid"), new UserName("user"), inst(12000));
	}
	
	@Test
	public void removeMemberFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("g");
		final UserName u = new UserName("u");
		
		failRemoveMember(g, null, i, u, new NullPointerException("userToken"));
		failRemoveMember(g, t, null, u, new NullPointerException("groupID"));
		failRemoveMember(g, t, i, null, new NullPointerException("member"));
	}
	
	@Test
	public void removeMemberFailUnauthed() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("someuser"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("user"))
				.withMember(new UserName("u3"))
				.build());
		
		failRemoveMember(mocks.groups, new Token("token"), new GroupID("gid"), new UserName("own"),
				new UnauthorizedException("User someuser may not administrate group gid"));
	}
	
	@Test
	public void removeMemberFailNotInGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(new UserName("user"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("user"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(5600));
		doThrow(new NoSuchUserException("Nope.")).when(mocks.storage)
				.removeMember(new GroupID("gid"), new UserName("user"), inst(5600));
		
		failRemoveMember(mocks.groups, new Token("token"), new GroupID("gid"),
				new UserName("user"), new NoSuchUserException("Nope."));
	}
	
	private void failRemoveMember(
			final Groups g,
			final Token t,
			final GroupID i,
			final UserName u,
			final Exception expected) {
		try {
			g.removeMember(t, i, u);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void promoteMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(14000));
		
		mocks.groups.promoteMember(new Token("t"), new GroupID("gid"), new UserName("u3"));
		
		verify(mocks.storage).addAdmin(new GroupID("gid"), new UserName("u3"), inst(14000));
	}
	
	@Test
	public void promoteMembersFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("i");
		final UserName u = new UserName("u");
		
		failPromoteMember(g, null, i, u, new NullPointerException("userToken"));
		failPromoteMember(g, t, null, u, new NullPointerException("groupID"));
		failPromoteMember(g, t, i, null, new NullPointerException("member"));
	}
	
	@Test
	public void promoteMemberFailNotOwner() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failPromoteMember(mocks.groups, new Token("t"), new GroupID("gid"), new UserName("u3"),
				new UnauthorizedException("Only the group owner can promote administrators"));
	}
	
	@Test
	public void promoteMemberFailNotMember() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failPromoteMember(mocks.groups, new Token("t"), new GroupID("gid"), new UserName("u2"),
				new NoSuchUserException("User u2 is not a standard member of group gid"));
	}
	
	@Test
	public void promoteMemberFailUserExists() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(14000));
		
		doThrow(new UserIsMemberException("boop")).when(mocks.storage)
				.addAdmin(new GroupID("gid"), new UserName("u3"), inst(14000));
		
		failPromoteMember(mocks.groups, new Token("t"), new GroupID("gid"), new UserName("u3"),
				new UserIsMemberException("boop"));
	}
	
	private void failPromoteMember(
			final Groups g,
			final Token t,
			final GroupID i,
			final UserName u,
			final Exception expected) {
		try {
			g.promoteMember(t, i, u);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void demoteAdmin() throws Exception {
		demoteAdmin("own");
	}
	
	@Test
	public void demoteAdminSelf() throws Exception {
		demoteAdmin("u3");
	}

	private void demoteAdmin(final String user) throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName(user));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withAdministrator(new UserName("u3"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(45000));
		
		mocks.groups.demoteAdmin(new Token("t"), new GroupID("gid"), new UserName("u3"));
		
		verify(mocks.storage).demoteAdmin(new GroupID("gid"), new UserName("u3"), inst(45000));
	}
	
	@Test
	public void demoteAdminFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("i");
		final UserName u = new UserName("u");
		
		failDemoteAdmin(g, null, i, u, new NullPointerException("userToken"));
		failDemoteAdmin(g, t, null, u, new NullPointerException("groupID"));
		failDemoteAdmin(g, t, i, null, new NullPointerException("admin"));
	}
	
	@Test
	public void demoteAdminFailUnauthed() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withAdministrator(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failDemoteAdmin(mocks.groups, new Token("t"), new GroupID("gid"), new UserName("u3"),
				new UnauthorizedException("Only the group owner can demote administrators"));
	}
	
	@Test
	public void demoteAdminFailNoSuchUser() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("own"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.clock.instant()).thenReturn(inst(3));
		
		doThrow(new NoSuchUserException("boop")).when(mocks.storage)
				.demoteAdmin(new GroupID("gid"), new UserName("u3"), inst(3));
		
		failDemoteAdmin(mocks.groups, new Token("t"), new GroupID("gid"), new UserName("u3"),
				new NoSuchUserException("boop"));
	}
	
	private void failDemoteAdmin(
			final Groups g,
			final Token t,
			final GroupID i,
			final UserName u,
			final Exception expected) {
		try {
			g.demoteAdmin(t, i, u);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	
	private ResourceDescriptor getWSRD(final int wsid) throws Exception {
		return new ResourceDescriptor(ResourceAdministrativeID.from(wsid),
				new ResourceID(wsid + ""));
	}
	
	@Test
	public void addWorkspace() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34"))).thenReturn(
				set(new UserName("admin"), new UserName("ws2")));
		when(mocks.clock.instant()).thenReturn(inst(3400));
		
		final Optional<GroupRequest> ret = mocks.groups.addWorkspace(
				new Token("t"), new GroupID("gid"), new ResourceID("34"));
		
		verify(mocks.storage).addResource(
				new GroupID("gid"),
				new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("34")),
				inst(3400));
		
		assertThat("incorrect request", ret, is(Optional.empty()));
	}
	
	@Test
	public void addWorkspaceWSAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("wsadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34"))).thenReturn(
				set(new UserName("wsadmin"), new UserName("ws2")));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(20000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final Optional<GroupRequest> ret = mocks.groups.addWorkspace(
				new Token("t"), new GroupID("gid"), new ResourceID("34"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("wsadmin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
						.build())
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("34")))
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("own"), new UserName("admin")),
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.withAdministrator(new UserName("admin"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("wsadmin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("34")))
						.build()
				);
		
		assertThat("incorrect request", ret, is(Optional.of(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("wsadmin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("34")))
						.build())));
	}
	
	@Test
	public void addWorkspaceGroupAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34"))).thenReturn(
				set(new UserName("ws1"), new UserName("ws2")));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(20000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final Optional<GroupRequest> ret = mocks.groups.addWorkspace(
				new Token("t"), new GroupID("gid"), new ResourceID("34"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("admin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("34")))
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("ws1"), new UserName("ws2")),
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.withAdministrator(new UserName("admin"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("admin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("34")))
						.build()
				);
		
		assertThat("incorrect request", ret, is(Optional.of(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("admin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("34")))
						.build())));
	}
	
	@Test
	public void addWorkspaceFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("i");
		final ResourceID w = new ResourceID("1");
		
		failAddWorkspace(g, null, i, w, new NullPointerException("userToken"));
		failAddWorkspace(g, t, null, w, new NullPointerException("groupID"));
		failAddWorkspace(g, t, i, null, new NullPointerException("resource"));
	}
	
	@Test
	public void addWorkspaceFailIllegalValue() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		when(mocks.wsHandler.getDescriptor(new ResourceID("4")))
				.thenThrow(new IllegalResourceIDException("bar"));
		
		failAddWorkspace(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("4"), new IllegalResourceIDException("bar"));
	}
	
	@Test
	public void addWorkspaceFailNoSuchWorkspace() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34")))
				.thenThrow(new NoSuchResourceException("34"));
		
		failAddWorkspace(mocks.groups, new Token("t"), new GroupID("gid"), new ResourceID("34"),
				new NoSuchResourceException("34"));
	}
	
	@Test
	public void addWorkspaceFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34"))).thenReturn(
				set(new UserName("ws1"), new UserName("ws2")));
		
		failAddWorkspace(mocks.groups, new Token("t"), new GroupID("gid"), new ResourceID("34"),
				new UnauthorizedException(
						"User u1 is not an admin for group gid or resource 34"));
	}
	
	@Test
	public void addWorkspaceFailWorkspaceInGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("34")))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		
		failAddWorkspace(mocks.groups, new Token("t"), new GroupID("gid"), new ResourceID("34"),
				new ResourceExistsException("34"));
	}
	
	@Test
	public void addWorkspaceFailWorkspaceInGroupAtStorage() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.getAdministrators(new ResourceID("34"))).thenReturn(
				set(new UserName("admin"), new UserName("ws2")));
		when(mocks.clock.instant()).thenReturn(inst(7000));
		
		doThrow(new ResourceExistsException("34")).when(mocks.storage)
				.addResource(
						new GroupID("gid"),
						new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("34")),
						inst(7000));
		
		failAddWorkspace(mocks.groups, new Token("t"), new GroupID("gid"), new ResourceID("34"),
				new ResourceExistsException("34"));
	}
	
	private void failAddWorkspace(
			final Groups g,
			final Token t,
			final GroupID i,
			final ResourceID w,
			final Exception expected) {
		try {
			g.addWorkspace(t, i, w);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeResourceGroupAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.isAdministrator(new ResourceID("34"), new UserName("admin")))
				.thenReturn(false);
		when(mocks.clock.instant()).thenReturn(inst(7100));
		
		mocks.groups.removeResource(new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("34"));
		
		verify(mocks.storage).removeResource(
				new GroupID("gid"),
				new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("34")),
				inst(7100));
	}
	
	@Test
	public void removeResourceResourceAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("wsadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.isAdministrator(new ResourceID("34"), new UserName("wsadmin")))
				.thenReturn(true);
		when(mocks.clock.instant()).thenReturn(inst(7500));
		
		mocks.groups.removeResource(new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("34"));
		
		verify(mocks.storage).removeResource(
				new GroupID("gid"),
				new ResourceType("workspace"),
				new ResourceDescriptor(new ResourceID("34")),
				inst(7500));
	}
	
	@Test
	public void removeResourceFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("i");
		final ResourceType ty = new ResourceType("t");
		final ResourceID w = new ResourceID("1");
		
		failRemoveResource(g, null, i, ty, w, new NullPointerException("userToken"));
		failRemoveResource(g, t, null, ty, w, new NullPointerException("groupID"));
		failRemoveResource(g, t, i, null, w, new NullPointerException("type"));
		failRemoveResource(g, t, i, ty, null, new NullPointerException("resource"));
	}
	
	@Test
	public void removeResourceFailNoSuchResourceType() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failRemoveResource(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceType("werkspace"), new ResourceID("34"),
				new NoSuchResourceTypeException("werkspace"));
	}
	
	@Test
	public void removeResourceFailIllegalResourceValue() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("6")))
				.thenThrow(new IllegalResourceIDException("bleah"));
		
		failRemoveResource(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("6"),
				new IllegalResourceIDException("bleah"));
	}
	
	@Test
	public void removeResourceFailCommError() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("34"), new UserName("notadmin")))
				.thenThrow(new ResourceHandlerException("bork"));
		
		failRemoveResource(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("34"),
				new ResourceHandlerException("bork"));
	}
	
	@Test
	public void removeResourceFailNotEitherAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("notadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.isAdministrator(new ResourceID("34"), new UserName("notadmin")))
				.thenReturn(false);
		
		failRemoveResource(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("34"),
				new UnauthorizedException(
						"User notadmin is not an admin for group gid or resource 34"));
	}
	
	@Test
	public void removeResourceFailResourceNotInGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("wsadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.wsHandler.getDescriptor(new ResourceID("34"))).thenReturn(getWSRD(34));
		when(mocks.wsHandler.isAdministrator(new ResourceID("34"), new UserName("wsadmin")))
				.thenReturn(true);
		when(mocks.clock.instant()).thenReturn(inst(7000));
		
		doThrow(new NoSuchResourceException("34 not in group")).when(mocks.storage)
				.removeResource(
						new GroupID("gid"),
						new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("34")),
						inst(7000));
		
		failRemoveResource(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceType("workspace"), new ResourceID("34"),
				new NoSuchResourceException("34 not in group"));
	}
	
	private void failRemoveResource(
			final Groups g,
			final Token t,
			final GroupID i,
			final ResourceType type,
			final ResourceID w,
			final Exception expected) {
		try {
			g.removeResource(t, i, type, w);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void setReadPermissionAdmin() throws Exception {
		setReadPermission(new UserName("admin"));
	}
	
	@Test
	public void setReadPermissionOwner() throws Exception {
		setReadPermission(new UserName("own"));
	}

	private void setReadPermission(final UserName user) throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("token"))).thenReturn(user);
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
								new ResourceID("m.meth")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("a1"))
				.withAdministrator(new UserName("a3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		mocks.groups.setReadPermission(new Token("token"), new RequestID(id));
		
		verify(mocks.catHandler).setReadPermission(new ResourceID("m.meth"), user);
	}
	
	@Test
	public void setReadPermissionFailNulls() throws Exception {
		final Groups g = initTestMocks().groups;
		
		failSetReadPermissions(g, null, new RequestID(UUID.randomUUID()),
				new NullPointerException("userToken"));
		failSetReadPermissions(g, new Token("t"), null,
				new NullPointerException("requestID"));
	}
	
	@Test
	public void setReadPermissionFailNoGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("43")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid")))
		.thenThrow(new NoSuchGroupException("gid"));
		
		failSetReadPermissions(mocks.groups, new Token("t"), new RequestID(id),
				new RuntimeException(String.format(
						"Request %s's group doesn't exist: 50000 No such group: gid",
						id.toString())));
	}
	
	@Test
	public void setReadPermissionFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("43")))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failSetReadPermissions(mocks.groups, new Token("t"), new RequestID(id),
				new UnauthorizedException("User u1 is not an admin for group gid"));
	}
	
	@Test
	public void setReadPermissionFailInvite() throws Exception {
		setReadPermissionFail(
				UUID.randomUUID(),
				b -> b.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace")),
				new UnauthorizedException(
						"Only Request type requests allow for resource permissions changes."));
	}
	
	@Test
	public void setReadPermissionFailResourceType() throws Exception {
		setReadPermissionFail(
				UUID.randomUUID(),
				b -> b.withResourceType(new ResourceType("user")),
				new UnauthorizedException("Requests with a user resource type do not allow " +
						"for permissions changes."));
	}
	
	@Test
	public void setReadPermissionFailNoResourceHandler() throws Exception {
		final UUID id = UUID.randomUUID();
		setReadPermissionFail(
				id,
				b -> b.withResourceType(new ResourceType("wrkspce")),
				new RuntimeException(
						"No handler configured for resource type wrkspce in request " +
						id.toString()));
	}
	
	private void setReadPermissionFail(
			final UUID id,
			final FuncExcept<GroupRequest.Builder, GroupRequest.Builder> builderFn,
			final Exception expected)
			throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("own"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				builderFn.apply(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build()))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failSetReadPermissions(mocks.groups, new Token("t"), new RequestID(id), expected);
	}
	
	@Test
	public void setReadPermissionFailClosed() throws Exception {
		final TestMocks mocks = initTestMocks();
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getRequest(new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("user"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build())
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("43")))
						.withStatus(GroupRequestStatus.denied(new UserName("d"), null))
						.build());
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		
		failSetReadPermissions(mocks.groups, new Token("t"), new RequestID(id),
				new ClosedRequestException(id + ""));
	}
	
	private void failSetReadPermissions(
			final Groups g,
			final Token t,
			final RequestID i,
			final Exception expected) {
		try {
			g.setReadPermission(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addCatalogMethod() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("mod.meth")))
				.thenReturn(getResDesc("mod", "mod.meth"));
		when(mocks.catHandler.getAdministrators(new ResourceID("mod.meth"))).thenReturn(
				set(new UserName("admin"), new UserName("cat2")));
		when(mocks.clock.instant()).thenReturn(inst(3400));
		
		final Optional<GroupRequest> ret = mocks.groups.addCatalogMethod(
				new Token("t"), new GroupID("gid"), new ResourceID("mod.meth"));
		
		verify(mocks.storage).addResource(
				new GroupID("gid"),
				new ResourceType("catalogmethod"),
				new ResourceDescriptor(
						new ResourceAdministrativeID("mod"), new ResourceID("mod.meth")),
				inst(3400));
		
		assertThat("incorrect request", ret, is(Optional.empty()));
	}
	
	
	private ResourceDescriptor getResDesc(final String aid, final String rid) throws Exception {
		return new ResourceDescriptor(new ResourceAdministrativeID(aid), new ResourceID(rid));
	}

	@Test
	public void addCatalogMethodModuleOwner() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("catadmin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenReturn(getResDesc("m", "m.n"));
		when(mocks.catHandler.getAdministrators(new ResourceID("m.n"))).thenReturn(
				set(new UserName("catadmin"), new UserName("cat2")));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(20000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final Optional<GroupRequest> ret = mocks.groups.addCatalogMethod(
				new Token("t"), new GroupID("gid"), new ResourceID("m.n"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("catadmin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
						.build())
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.n")))
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("own"), new UserName("admin")),
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.withAdministrator(new UserName("admin"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("catadmin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
								new ResourceID("m.n")))
						.build()
				);
		
		assertThat("incorrect request", ret, is(Optional.of(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("catadmin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
								new ResourceID("m.n")))
						.build())));
	}
	
	@Test
	public void addCatalogMethodGroupAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenReturn(getResDesc("m", "m.n"));
		when(mocks.catHandler.getAdministrators(new ResourceID("m.n"))).thenReturn(
				set(new UserName("catadmin"), new UserName("cat2")));
		when(mocks.clock.instant()).thenReturn(Instant.ofEpochMilli(20000));
		when(mocks.uuidGen.randomUUID()).thenReturn(id);
		
		final Optional<GroupRequest> ret = mocks.groups.addCatalogMethod(
				new Token("t"), new GroupID("gid"), new ResourceID("m.n"));
		
		verify(mocks.storage).storeRequest(GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("admin"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
						new ResourceID("m.n")))
				.build());
		
		verify(mocks.notifs).notify(
				set(new UserName("catadmin"), new UserName("cat2")),
				Group.getBuilder(
						new GroupID("gid"), new GroupName("name"), new UserName("own"),
						new CreateAndModTimes(Instant.ofEpochMilli(10000)))
						.withMember(new UserName("u1"))
						.withMember(new UserName("u3"))
						.withAdministrator(new UserName("admin"))
						.build(),
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("admin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build()).withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
								new ResourceID("m.n")))
						.build()
				);
		
		assertThat("incorrect request", ret, is(Optional.of(
				GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("admin"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(20000), Instant.ofEpochMilli(1209620000))
								.build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceAdministrativeID("m"),
								new ResourceID("m.n")))
						.build())));
	}
	
	@Test
	public void addCatalogMethodFailNulls() throws Exception {
		final TestMocks mocks = initTestMocks();
		final Groups g = mocks.groups;
		final Token t = new Token("t");
		final GroupID i = new GroupID("i");
		final ResourceID m = new ResourceID("m.n");
		
		failAddCatalogMethod(g, null, i, m, new NullPointerException("userToken"));
		failAddCatalogMethod(g, t, null, m, new NullPointerException("groupID"));
		failAddCatalogMethod(g, t, i, null, new NullPointerException("resource"));
	}
	
	@Test
	public void addCatalogEntryFailNoSuchMethod() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenReturn(getResDesc("m", "m.n"));
		when(mocks.catHandler.getAdministrators(new ResourceID("m.n")))
				.thenThrow(new NoSuchResourceException("m.n"));
		
		failAddCatalogMethod(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("m.n"), new NoSuchResourceException("m.n"));
	}
	
	@Test
	public void addCatalogMethodFailNotAdmin() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("u1"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenReturn(getResDesc("m", "m.n"));
		when(mocks.catHandler.getAdministrators(new ResourceID("m.n"))).thenReturn(
				set(new UserName("cat1"), new UserName("cat2")));
		
		failAddCatalogMethod(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("m.n"), new UnauthorizedException(
						"User u1 is not an admin for group gid or resource m.n"));
	}
	
	@Test
	public void addCatalogMethodFailMethodInGroup() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withResource(new ResourceType("catalogmethod"), new ResourceDescriptor(
						new ResourceAdministrativeID("m"), new ResourceID("m.n")))
				.withAdministrator(new UserName("admin"))
				.build());
		
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenReturn(getResDesc("m", "m.n"));
		
		failAddCatalogMethod(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("m.n"), new ResourceExistsException("m.n"));
	}
	
	@Test
	public void addCatalogMethodFailIllegalValue() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withResource(new ResourceType("catalogmethod"), new ResourceDescriptor(
						new ResourceAdministrativeID("m"), new ResourceID("m.n")))
				.withAdministrator(new UserName("admin"))
				.build());
		
		when(mocks.catHandler.getDescriptor(new ResourceID("m.n")))
				.thenThrow(new IllegalResourceIDException("bar"));
		
		failAddCatalogMethod(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("m.n"), new IllegalResourceIDException("bar"));
	}
	
	@Test
	public void addCatalogMethodFailMethodInGroupAtStorage() throws Exception {
		final TestMocks mocks = initTestMocks();
		
		when(mocks.userHandler.getUser(new Token("t"))).thenReturn(new UserName("admin"));
		when(mocks.storage.getGroup(new GroupID("gid"))).thenReturn(Group.getBuilder(
				new GroupID("gid"), new GroupName("name"), new UserName("own"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("u1"))
				.withMember(new UserName("u3"))
				.withAdministrator(new UserName("admin"))
				.build());
		when(mocks.catHandler.getDescriptor(new ResourceID("mod.m")))
				.thenReturn(getResDesc("mod", "mod.m"));
		when(mocks.catHandler.getAdministrators(new ResourceID("mod.m"))).thenReturn(
				set(new UserName("admin"), new UserName("cat2")));
		when(mocks.clock.instant()).thenReturn(inst(7000));

		doThrow(new ResourceExistsException("mod.m")).when(mocks.storage)
				.addResource(
						new GroupID("gid"),
						new ResourceType("catalogmethod"),
						new ResourceDescriptor(
								new ResourceAdministrativeID("mod"), new ResourceID("mod.m")),
						inst(7000));
		
		failAddCatalogMethod(mocks.groups, new Token("t"), new GroupID("gid"),
				new ResourceID("mod.m"), new ResourceExistsException("mod.m"));
	}
	
	private void failAddCatalogMethod(
			final Groups g,
			final Token t,
			final GroupID i,
			final ResourceID m,
			final Exception expected) {
		try {
			g.addCatalogMethod(t, i, m);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}