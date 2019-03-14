package us.kbase.test.groups.service.api;

import static us.kbase.test.groups.TestCommon.inst;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalString;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.service.api.GroupsAPI;
import us.kbase.groups.service.api.GroupsAPI.CreateOrUpdateGroupJSON;
import us.kbase.groups.service.api.GroupsAPI.UpdateUserJSON;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class GroupsAPITest {

	private static final Group GROUP_MIN;
	private static final Group GROUP_MAX;
	private static final Group GROUP_PRIV;
	private static final Group GROUP_PUB_MEMB;
	static {
		try {
			GROUP_MIN = Group.getBuilder(
					new GroupID("id"), new GroupName("name"),
					GroupUser.getBuilder(new UserName("u"), inst(10000))
							.withCustomField(new NumberedCustomField("f-1"), "val")
							.withCustomField(new NumberedCustomField("something"), "nothing")
							.build(),
					new CreateAndModTimes(Instant.ofEpochMilli(10000)))
					.build();
			final Builder b = getGroupMaxBuilder();
			GROUP_MAX = b.build();
			GROUP_PRIV = b.withIsPrivate(true).build();
			GROUP_PUB_MEMB = b.withIsPrivate(false).withPrivateMemberList(false).build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix your tests newb", e);
		}
	}

	private static Builder getGroupMaxBuilder()
			throws MissingParameterException, IllegalParameterException {
		return Group.getBuilder(
				new GroupID("id2"), new GroupName("name2"),
				GroupUser.getBuilder(new UserName("u2"), inst(20000)).build(),
				new CreateAndModTimes(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withMember(GroupUser.getBuilder(new UserName("foo"), inst(650000))
						.withCustomField(new NumberedCustomField("whee"), "whoo")
						.build())
				.withMember(GroupUser.getBuilder(new UserName("bar"), inst(40000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whee"), inst(220000))
						.build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whoo"), inst(760000))
						.withCustomField(new NumberedCustomField("yay-6"), "boo")
						.withCustomField(new NumberedCustomField("bar"), "baz")
						.build())
				.withCustomField(new NumberedCustomField("field-1"), "my val")
				.withCustomField(new NumberedCustomField("otherfield"), "fieldval");
	}
	
	private static final Map<String, Object> GROUP_MIN_JSON_STD = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id")
			.with("private", false)
			.with("privatemembers", true)
			.with("role", "Owner")
			.with("lastvisit", null)
			.with("name", "name")
			.with("memcount", 1)
			.with("rescount", Collections.emptyMap())
			.with("owner", MapBuilder.newHashMap()
					.with("name", "u")
					.with("joined", 10000L)
					.with("lastvisit", null)
					.with("custom", ImmutableMap.of("f-1", "val", "something", "nothing"))
					.build())
			.with("createdate", 10000L)
			.with("moddate", 10000L)
			.with("resources", Collections.emptyMap())
			.with("members", Collections.emptyList())
			.with("admins", Collections.emptyList())
			.with("custom", Collections.emptyMap())
			.build();
	
	private static final Map<String, Object> GROUP_MIN_JSON_MIN = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id")
			.with("private", false)
			.with("role", "None")
			.with("lastvisit", null)
			.with("name", "name")
			.with("memcount", 1)
			.with("rescount", Collections.emptyMap())
			.with("owner", "u")
			.with("createdate", 10000L)
			.with("moddate", 10000L)
			.with("custom", Collections.emptyMap())
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_STD = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("private", false)
			.with("privatemembers", true)
			.with("lastvisit", null)
			.with("role", "Owner")
			.with("name", "name2")
			.with("memcount", 5)
			.with("rescount", Collections.emptyMap())
			.with("owner", MapBuilder.newHashMap()
					.with("name", "u2")
					.with("joined", 20000L)
					.with("lastvisit", null)
					.with("custom", Collections.emptyMap())
					.build())
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("members", Arrays.asList(
					MapBuilder.newHashMap()
							.with("name", "bar")
							.with("joined", 40000L)
							.with("lastvisit", null)
							.with("custom", Collections.emptyMap())
							.build(),
					MapBuilder.newHashMap()
							.with("name", "foo")
							.with("joined", 650000L)
							.with("lastvisit", null)
							.with("custom", ImmutableMap.of("whee", "whoo"))
							.build()
					))
			.with("admins", Arrays.asList(
					MapBuilder.newHashMap()
							.with("name", "whee")
							.with("joined", 220000L)
							.with("lastvisit", null)
							.with("custom", Collections.emptyMap())
							.build(),
					MapBuilder.newHashMap()
							.with("name", "whoo")
							.with("joined", 760000L)
							.with("lastvisit", null)
							.with("custom", ImmutableMap.of("yay-6", "boo", "bar", "baz"))
							.build()
					))
			.with("resources", Collections.emptyMap())
			.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_NON = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("private", false)
			.with("privatemembers", true)
			.with("role", "None")
			.with("lastvisit", null)
			.with("name", "name2")
			.with("memcount", 5)
			.with("rescount", Collections.emptyMap())
			.with("owner", MapBuilder.newHashMap()
					.with("name", "u2")
					.with("joined", null)
					.with("lastvisit", null)
					.with("custom", Collections.emptyMap())
					.build())
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("members", Collections.emptyList())
			.with("admins", Arrays.asList(
					MapBuilder.newHashMap()
							.with("name", "whee")
							.with("joined", null)
							.with("lastvisit", null)
							.with("custom", Collections.emptyMap())
							.build(),
					MapBuilder.newHashMap()
							.with("name", "whoo")
							.with("joined", null)
							.with("lastvisit", null)
							.with("custom", ImmutableMap.of("yay-6", "boo"))
							.build()
					))
			.with("resources", Collections.emptyMap())
			.with("custom", ImmutableMap.of("otherfield", "fieldval"))
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_MIN = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("private", false)
			.with("role", "Owner")
			.with("lastvisit", null)
			.with("name", "name2")
			.with("memcount", 5)
			.with("rescount", Collections.emptyMap())
			.with("owner", "u2")
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("custom", ImmutableMap.of("field-1", "my val"))
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_PRIV = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("private", true)
			.with("role", "None")
			.with("resources", Collections.emptyMap())
			.build();

	@Test
	public void getGroupsNulls() throws Exception {
		getGroups(null, null, null, null, null, GetGroupsParams.getBuilder().build());
	}
	
	@Test
	public void getGroupsWhitespace() throws Exception {
		getGroups("   \t   ", "   \t   ", "   \t   ", "   \t    ", null,
				GetGroupsParams.getBuilder().build());
	}
	
	@Test
	public void getGroupsWhitespaceValuesAsc() throws Exception {
		getGroups("    tok \t   ", "   foo  \t  ", "  asc  \t ", "    ,    \t   ,    ",
				new Token("    tok \t   "),
				GetGroupsParams.getBuilder()
						.withNullableExcludeUpTo("foo").build());
	}
	
	@Test
	public void getGroupsWhitespaceValuesDesc() throws Exception {
		getGroups("t", "   foo  \t  ", "  desc  \t ", ",", new Token("t"),
				GetGroupsParams.getBuilder()
						.withNullableExcludeUpTo("foo")
						.withNullableSortAscending(false).build());
	}

	private void getGroups(
			final String token,
			final String excludeUpTo,
			final String order,
			final String ids, // this must be null or contain ws (with commas)
			final Token expectedToken,
			final GetGroupsParams expected)
			throws Exception {
		final Groups g = mock(Groups.class);
		when(g.getGroups(expectedToken, expected)).thenReturn(Arrays.asList(
				GroupView.getBuilder(GROUP_MAX, new UserName("u2"))
						.withMinimalViewFieldDeterminer(f -> f.getField().equals("field-1"))
						.build(),
				GroupView.getBuilder(GROUP_MIN, new UserName("u2"))
						.withPublicUserFieldDeterminer(f -> f.getField().equals("something"))
						.build()));
		final List<Map<String, Object>> ret = new GroupsAPI(g)
				.getGroups(token, excludeUpTo, order, ids);
		
		assertThat("incorrect groups", ret,
				is(Arrays.asList(GROUP_MAX_JSON_MIN, GROUP_MIN_JSON_MIN)));
	}
	
	@Test
	public void getGroupsWithIDs() throws Exception {
		getGroupsWithIDs(null, null);
		getGroupsWithIDs("   \t   ", null);
		getGroupsWithIDs("t", new Token("t"));
	}
	
	private void getGroupsWithIDs(
			final String token,
			final Token expectedToken)
			throws Exception {
		final Groups g = mock(Groups.class);
		when(g.getGroups(expectedToken,
				Arrays.asList(new GroupID("id2"), new GroupID("priv"), new GroupID("id"))))
				.thenReturn(Arrays.asList(
						GroupView.getBuilder(GROUP_MAX, new UserName("u2"))
								.withMinimalViewFieldDeterminer(
										f -> f.getField().equals("field-1"))
								.build(),
						GroupView.getBuilder(Group.getBuilder(
								new GroupID("priv"), new GroupName("fake"),
								GroupUser.getBuilder(new UserName("u2"), inst(20000)).build(),
								new CreateAndModTimes(inst(1000)))
								.withIsPrivate(true)
								.build(),
								new UserName("nonmember"))
								.build(),
						GroupView.getBuilder(GROUP_MIN, new UserName("u2"))
								.withPublicUserFieldDeterminer(
										f -> f.getField().equals("something"))
								.build()));
		
		final List<Map<String, Object>> ret = new GroupsAPI(g)
				.getGroups(token, "id", "asc", "id2   , priv,  id   ");
		
		assertThat("incorrect groups", ret,
				is(Arrays.asList(
						GROUP_MAX_JSON_MIN,
						ImmutableMap.of("private", true, "role", "None", "id", "priv"),
						GROUP_MIN_JSON_MIN)));
	}
	
	@Test
	public void getGroupsFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetGroups(g, "t", null, "  asd   ", null, new IllegalParameterException(
				"Invalid sort direction: asd"));
		failGetGroups(g, "t", null, null, " id1 , id*bad", new IllegalParameterException(
				ErrorType.ILLEGAL_GROUP_ID, "Illegal character in group id id*bad: *"));
	}
	
	private void failGetGroups(
			final Groups g,
			final String token,
			final String excludeUpTo,
			final String order,
			final String ids,
			final Exception expected) {
		try {
			new GroupsAPI(g).getGroups(token, excludeUpTo, order, ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void createGroupMinimalNulls() throws Exception {
		createGroupMinimal(null, null, null);
	}
	
	@Test
	public void createGroupMinimalFalsePrivateEmptyOptional() throws Exception {
		createGroupMinimal(false, false, MapBuilder.<String, String>newHashMap().with("key", null)
				.build());
	}
	
	@Test
	public void createGroupMinimalTruePrivateWhitespace() throws Exception {
		createGroupMinimal(true, true, MapBuilder.<String, String>newHashMap()
				.with("key", "    \t    ").build());
	}
	
	private void createGroupMinimal(
			final Boolean isPrivate,
			final Boolean isPrivateMembers,
			final Map<String, String> custom)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withNullableIsPrivate(isPrivate)
						.withNullablePrivateMemberList(isPrivateMembers)
						.build())
				.build()))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("u2"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup(
				"toke", "gid", new CreateOrUpdateGroupJSON(
						"name", isPrivate, isPrivateMembers, custom));
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_STD));
	}
	
	@Test
	public void createGroupMaximal() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withNullableIsPrivate(true)
						.withNullablePrivateMemberList(false)
						.withCustomField(new NumberedCustomField("foo-23"),
								OptionalString.of("yay"))
						.withCustomField(new NumberedCustomField("doodybutt"),
								OptionalString.of("yo"))
						.build())
				.build()))
				.thenReturn(GroupView.getBuilder(GROUP_MIN, new UserName("u"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup("toke", "gid",
				new CreateOrUpdateGroupJSON("name", true, false,
						ImmutableMap.of("foo-23", "yay", "doodybutt", "yo")));
		
		assertThat("incorrect group", ret, is(GROUP_MIN_JSON_STD));
	}
	
	@Test
	public void createGroupFailNullsAndWhitespace() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON("n", null, null, null);
		
		failCreateGroup(g, null, "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "  \t  ", "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "t", null, b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "   \t  ", b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "i", null, new MissingParameterException("Missing JSON body"));
		failCreateGroup(g, "t", "i", new CreateOrUpdateGroupJSON(null, null, null, null),
				new MissingParameterException("group name"));
		failCreateGroup(g, "t", "i",
				new CreateOrUpdateGroupJSON("   \t    ", null, null, null),
				new MissingParameterException("group name"));
		
	}
	
	@Test
	public void createGroupFailExtraProperties() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON("n", null, null, null);
		b.setAdditionalProperties("foo", "bar");

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Unexpected parameters in request: foo"));
	}
	
	@Test
	public void createGroupFailCustomNotMap() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, "customstr");

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"'custom' field must be a mapping"));
	}
	
	@Test
	public void createGroupFailCustomNotStringValue() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, ImmutableMap.of("foo-1", Collections.emptyList()));

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Value of 'foo-1' field in 'custom' map is not a string"));
	}
	
	@Test
	public void createGroupFailBadCustomField() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, ImmutableMap.of("foo-1-1", "yay"));

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Suffix after - of field foo-1-1 must be an integer > 0"));
	}
	
	@Test
	public void createGroupFailGroupExists() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON("n", null, null, null);

		when(g.createGroup(new Token("t"), GroupCreationParams.getBuilder(
				new GroupID("i"), new GroupName("n")).build()))
				.thenThrow(new GroupExistsException("i"));
				
		failCreateGroup(g, "t", "i", b, new GroupExistsException("i"));
	}
	
	private void failCreateGroup(
			final Groups g,
			final String token,
			final String groupID,
			final CreateOrUpdateGroupJSON body,
			final Exception expected) {
		try {
			new GroupsAPI(g).createGroup(token, groupID, body);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void updateGroupAllNulls() throws Exception {
		updateGroup(null, null, null, null, GroupUpdateParams.getBuilder(new GroupID("gid"))
				.build());
	}
	
	@Test
	public void updateGroupAllEmpty() throws Exception {
		updateGroup(null, null, null, Collections.emptyMap(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder().build())
				.build());
	}
	
	@Test
	public void updateGroupNullCustom() throws Exception {
		updateGroup(null, null, null, MapBuilder.<String, String>newHashMap()
				.with("foo", null).with("bar", null).build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withCustomField(new NumberedCustomField("foo"), OptionalString.empty())
						.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
						.build())
				.build());
	}
	
	@Test
	public void updateGroupAllWhitespaceFalsePrivate() throws Exception {
		updateGroup("   \t    ", false, false, MapBuilder.<String, String>newHashMap()
				.with("foo", "   \t   ").with("bar", "   \t    ").build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withNullableIsPrivate(false)
						.withNullablePrivateMemberList(false)
						.withCustomField(new NumberedCustomField("foo"), OptionalString.empty())
						.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
						.build())
				.build());
	}
	
	@Test
	public void updateGroupWithValues() throws Exception {
		updateGroup(
				"    name   ",
				true,
				true,
				MapBuilder.<String, String>newHashMap()
						.with("foo", "baz").with("bar", "bat").build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
						.withName(new GroupName("name"))
						.withOptionalFields(OptionalGroupFields.getBuilder()
								.withNullableIsPrivate(true)
								.withNullablePrivateMemberList(true)
								.withCustomField(new NumberedCustomField("foo"),
										OptionalString.of("baz"))
								.withCustomField(new NumberedCustomField("bar"),
										OptionalString.of("bat"))
								.build())
						.build());
	}
	
	private void updateGroup(
			final String groupName,
			final Boolean isPrivate,
			final Boolean isPrivateMembers,
			final Map<String, String> custom,
			final GroupUpdateParams expected)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).updateGroup("tok", "    gid   ",
				new CreateOrUpdateGroupJSON(groupName, isPrivate, isPrivateMembers, custom));
		
		verify(g).updateGroup(new Token("tok"), expected);
	}
	
	@Test
	public void updateGroupFailNullsAndWhitespace() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(null, null, null, null);
		
		failUpdateGroup(g, null, "i", b, new NoTokenProvidedException("No token provided"));
		failUpdateGroup(g, "  \t  ", "i", b, new NoTokenProvidedException("No token provided"));
		failUpdateGroup(g, "t", null, b, new MissingParameterException("group id"));
		failUpdateGroup(g, "t", "   \t  ", b, new MissingParameterException("group id"));
		failUpdateGroup(g, "t", "i", null, new MissingParameterException("Missing JSON body"));
	}
	
	@Test
	public void updateGroupFailExtraProperties() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(null, null, null, null);
		b.setAdditionalProperties("foo", "bar");

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"Unexpected parameters in request: foo"));
	}
	
	@Test
	public void updateGroupFailCustomNotMap() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, "customstr");

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"'custom' field must be a mapping"));
	}
	
	@Test
	public void updateGroupFailCustomNotStringValue() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, ImmutableMap.of("foo-1", Collections.emptyList()));

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"Value of 'foo-1' field in 'custom' map is not a string"));
	}
	
	@Test
	public void updateGroupFailBadCustomField() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				"n", null, null, ImmutableMap.of("foo-1-1", "yay"));

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"Suffix after - of field foo-1-1 must be an integer > 0"));
	}
	
	@Test
	public void updateGroupFailNoSuchGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchGroupException("gid"))
				.when(g).updateGroup(
						new Token("tok"), GroupUpdateParams.getBuilder(new GroupID("gid"))
								.withName(new GroupName("name"))
								.build());

		failUpdateGroup(g, "tok", "  gid", new CreateOrUpdateGroupJSON("name", null, null, null),
				new NoSuchGroupException("gid"));
	}
	
	private void failUpdateGroup(
			final Groups g,
			final String token,
			final String groupID,
			final CreateOrUpdateGroupJSON update,
			final Exception expected) {
		try {
			new GroupsAPI(g).updateGroup(token, groupID, update);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupNoTokenNull() throws Exception {
		getGroup(null, null);
	}
	
	@Test
	public void getGroupNoTokenWhitespace() throws Exception {
		getGroup("   \t    ", null);
	}
	
	@Test
	public void getGroupWithToken() throws Exception {
		getGroup("foo", new Token("foo"));
	}
	
	private void getGroup(final String token, final Token expectedToken) throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(expectedToken, new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("bar"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup(token, "id");
		final Map<String, Object> expected = new HashMap<>(GROUP_MAX_JSON_STD);
		expected.put("role", "Member");
		
		assertThat("incorrect group", ret, is(expected));
	}
	
	@Test
	public void getGroupNonMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("toke"), new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("nonmember"))
						.withStandardView(true)
						.withPublicFieldDeterminer(f -> f.getField().equals("otherfield"))
						.withPublicUserFieldDeterminer(f -> f.getField().equals("yay-6"))
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_NON));
	}
	
	@Test
	public void getGroupNonMemberPublicMembers() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("toke"), new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_PUB_MEMB, new UserName("nonmember"))
						.withStandardView(true)
						.withPublicFieldDeterminer(f -> f.getField().equals("otherfield"))
						.withPublicUserFieldDeterminer(f -> f.getField().equals("yay-6"))
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		
		final Map<String, Object> expected = MapBuilder.<String, Object>newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("privatemembers", false)
				.with("role", "None")
				.with("lastvisit", null)
				.with("name", "name2")
				.with("memcount", 5)
				.with("rescount", Collections.emptyMap())
				.with("owner", MapBuilder.newHashMap()
						.with("name", "u2")
						.with("joined", null)
						.with("lastvisit", null)
						.with("custom", Collections.emptyMap())
						.build())
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("members", Arrays.asList(
						MapBuilder.newHashMap()
								.with("name", "bar")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", Collections.emptyMap())
								.build(),
						MapBuilder.newHashMap()
								.with("name", "foo")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", Collections.emptyMap())
								.build()
						))
				.with("admins", Arrays.asList(
						MapBuilder.newHashMap()
								.with("name", "whee")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", Collections.emptyMap())
								.build(),
						MapBuilder.newHashMap()
								.with("name", "whoo")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", ImmutableMap.of("yay-6", "boo"))
								.build()
						))
				.with("resources", Collections.emptyMap())
				.with("custom", ImmutableMap.of("otherfield", "fieldval"))
				.build();
		
		assertThat("incorrect group", ret, is(expected));
	}
	
	@Test
	public void getGroupPrivate() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("t"), new GroupID("id"))).thenReturn(
				GroupView.getBuilder(GROUP_PRIV, new UserName("nonmember"))
						.withStandardView(true)
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("t", "id");
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_PRIV));
	}
	
	@Test
	public void getGroupPrivateMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("t"), new GroupID("id"))).thenReturn(
				GroupView.getBuilder(GROUP_PRIV, new UserName("bar"))
						.withStandardView(true)
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("t", "id");
		
		final Map<String, Object> expected = new HashMap<>(GROUP_MAX_JSON_STD);
		expected.put("private", true);
		expected.put("role", "Member");
		
		assertThat("incorrect group", ret, is(expected));
	}
	
	@Test
	public void getGroupMemberPublicMembers() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("toke"), new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_PUB_MEMB, new UserName("foo"))
						.withStandardView(true)
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		
		final Map<String, Object> expected = new HashMap<>(GROUP_MAX_JSON_STD);
		expected.put("role", "Member");
		expected.put("privatemembers", false);
		
		assertThat("incorrect group", ret, is(expected));
	}
	
	@Test
	public void getGroupWithResources() throws Exception {
		final Groups g = mock(Groups.class);
		
		final ResourceID d1 = new ResourceID("82");
		final ResourceID d2 = new ResourceID("45");
		
		final ResourceID c1 = new ResourceID("mod.meth");
		final Group group = getGroupMaxBuilder()
				.withResource(new ResourceType("workspace"), new ResourceDescriptor(d1),
						inst(45000))
				.withResource(new ResourceType("workspace"), new ResourceDescriptor(d2))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceAdministrativeID("mod"), c1),
						inst(670000))
				.build();
		
		final GroupView.Builder gv = GroupView.getBuilder(group, new UserName("whoo"))
				.withStandardView(true)
				.withResourceType(new ResourceType("foo"))
				.withResource(new ResourceType("workspace"),
						ResourceInformationSet.getBuilder(new UserName("whoo"))
								.withResourceField(d1, "name", "name82")
								.withResourceField(d1, "public", true)
								.withResourceField(d1, "narrname", "narrname")
								.withResourceField(d1, "perm", "Admin")
								.withResourceField(d2, "name", "name45")
								.withResourceField(d2, "public", false)
								.withResourceField(d2, "narrname", null)
								.withResourceField(d2, "perm", "None")
								.build())
				.withResource(new ResourceType("catalogmethod"),
						ResourceInformationSet.getBuilder(new UserName("whoo"))
								.withResource(c1)
								.build());
		
		when(g.getGroup(new Token("toke"), new GroupID("id"))).thenReturn(gv.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		final Map<String, Object> expected = new HashMap<>();
		expected.putAll(GROUP_MAX_JSON_STD);
		expected.put("resources", ImmutableMap.of(
				"foo", Collections.emptyList(),
				"catalogmethod", Arrays.asList(
						MapBuilder.newHashMap()
								.with("rid", "mod.meth")
								.with("added", 670000L)
								.build()),
				"workspace", Arrays.asList(
						MapBuilder.newHashMap()
								.with("rid", "45")
								.with("added", null)
								.with("name", "name45")
								.with("narrname", null)
								.with("public", false)
								.with("perm", "None")
								.build(),
						MapBuilder.newHashMap()
								.with("rid", "82")
								.with("added", 45000L)
								.with("name", "name82")
								.with("narrname", "narrname")
								.with("public", true)
								.with("perm", "Admin")
								.build()
						)));
		expected.put("role", "Admin");
		expected.put("rescount", ImmutableMap.of("workspace", 2, "catalogmethod", 1));
		
		assertThat("incorrect group", ret, is(expected));
		
		when(g.getGroups(new Token("toke2"), GetGroupsParams.getBuilder().build()))
				.thenReturn(Arrays.asList(gv.withStandardView(false).build()));
		
		final Map<String, Object> retmin = new GroupsAPI(g).getGroups("toke2", null, null, null)
				.get(0);
		final Map<String, Object> expectedmin = new HashMap<>();
		expectedmin.putAll(GROUP_MAX_JSON_MIN);
		expectedmin.put("role", "Admin");
		expectedmin.put("rescount", ImmutableMap.of("workspace", 2, "catalogmethod", 1));
		expectedmin.put("custom", Collections.emptyMap());

		assertThat("incorrect group", retmin, is(expectedmin));
	}
	
	@Test
	public void getGroupWithResourcesNonMember() throws Exception {
		// the user must be an administrator of the included resources
		final Groups g = mock(Groups.class);
		
		final ResourceID d1 = new ResourceID("82");
		
		final Group group = getGroupMaxBuilder()
				.withResource(new ResourceType("workspace"), new ResourceDescriptor(d1),
						inst(45000))
				.build();
		
		final GroupView.Builder gv = GroupView.getBuilder(group, new UserName("nonmember"))
				.withStandardView(true)
				.withResourceType(new ResourceType("foo"))
				.withResource(new ResourceType("workspace"),
						ResourceInformationSet.getBuilder(new UserName("nonmember"))
								.withResourceField(d1, "name", "name82")
								.withResourceField(d1, "public", true)
								.withResourceField(d1, "narrname", "narrname")
								.withResourceField(d1, "perm", "Admin")
								.build());
		
		when(g.getGroup(new Token("toke"), new GroupID("id"))).thenReturn(gv.build());
		
		final Map<String, Object> expected = MapBuilder
				.<String, Object>newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("privatemembers", true)
				.with("lastvisit", null)
				.with("role", "None")
				.with("name", "name2")
				.with("memcount", 5)
				.with("rescount", ImmutableMap.of("workspace", 1))
				.with("owner", MapBuilder.newHashMap()
						.with("name", "u2")
						.with("joined", null)
						.with("lastvisit", null)
						.with("custom", Collections.emptyMap())
						.build())
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("members", Collections.emptyList())
				.with("admins", Arrays.asList(
						MapBuilder.newHashMap()
								.with("name", "whee")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", Collections.emptyMap())
								.build(),
						MapBuilder.newHashMap()
								.with("name", "whoo")
								.with("joined", null)
								.with("lastvisit", null)
								.with("custom", Collections.emptyMap())
								.build()
						))
				.with("resources", ImmutableMap.of(
						"foo", Collections.emptyList(),
						"workspace", Arrays.asList(
								MapBuilder.newHashMap()
										.with("rid", "82")
										.with("added", null)
										.with("name", "name82")
										.with("narrname", "narrname")
										.with("public", true)
										.with("perm", "Admin")
										.build()
								)))
				.with("custom", Collections.emptyMap())
				.build();
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		
		assertThat("incorrect group", ret, is(expected));
	}
	
	@Test
	public void getGroupFailMissingID() {
		final Groups g = mock(Groups.class);
		
		failGetGroup(g, null, null, new MissingParameterException("group id"));
		failGetGroup(g, null, "   \t   ", new MissingParameterException("group id"));
	}
	
	@Test
	public void getGroupFailInvalidToken() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("t"), new GroupID("i"))).thenThrow(new InvalidTokenException());
		
		failGetGroup(g, "t", "i", new InvalidTokenException());
	}
	
	private void failGetGroup(
			final Groups g,
			final String token,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).getGroup(token, groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupExists() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroupExists(new GroupID("g1"))).thenReturn(true);
		when(g.getGroupExists(new GroupID("g2"))).thenReturn(false);
		
		final GroupsAPI api = new GroupsAPI(g);
		
		assertThat("incorrect exists", api.getGroupExists("   g1  "),
				is(ImmutableMap.of("exists", true)));
		assertThat("incorrect exists", api.getGroupExists("   g2  "),
				is(ImmutableMap.of("exists", false)));
	}
	
	@Test
	public void getGroupExistsFailMissingID() {
		final Groups g = mock(Groups.class);
		
		failGetGroupExists(g, null, new MissingParameterException("group id"));
		failGetGroupExists(g, "   \t   ", new MissingParameterException("group id"));
	}
	
	private void failGetGroupExists(
			final Groups g,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).getGroupExists(groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void visitGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).visitGroup("tokeytoke", "gid1");
		
		verify(g).userVisited(new Token("tokeytoke"), new GroupID("gid1"));
	}
	
	@Test
	public void visitGroupFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		
		visitGroupFail(g, null, "g", new NoTokenProvidedException("No token provided"));
		visitGroupFail(g, "   \t  ", "g", new NoTokenProvidedException("No token provided"));
		visitGroupFail(g, "t", "b*d", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Illegal character in group id b*d: *"));
	}
	
	private void visitGroupFail(
			final Groups g,
			final String t,
			final String gid,
			final Exception expected) {
		try {
			new GroupsAPI(g).visitGroup(t, gid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void requestGroupMembership() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.requestGroupMembership(new Token("t"), new GroupID("gid")))
				.thenReturn(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(30000))
								.build())
						.withType(RequestType.REQUEST)
						.withResource(GroupRequest.USER_TYPE,
								ResourceDescriptor.from(new UserName("foo")))
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).requestGroupMembership("t", "gid");
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "foo")
				.with("type", "Request")
				.with("resourcetype", "user")
				.with("resource", "foo")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 30000L)
				.build()));
	}
	
	@Test
	public void requestGroupMembershipFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failRequestGroupMembership(g, null, "i",
				new NoTokenProvidedException("No token provided"));
		failRequestGroupMembership(g, "    \t    ", "i",
				new NoTokenProvidedException("No token provided"));
		failRequestGroupMembership(g, "t", null,
				new MissingParameterException("group id"));
		failRequestGroupMembership(g, "t", "   \t   ",
				new MissingParameterException("group id"));
	}
	
	@Test
	public void requestGroupMembershipFailUserIsMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.requestGroupMembership(new Token("t"), new GroupID("i"))).thenThrow(
				new UserIsMemberException("foo"));
		
		failRequestGroupMembership(g, "t", "i", new UserIsMemberException("foo"));
	}
	
	private void failRequestGroupMembership(
			final Groups g,
			final String token,
			final String groupid,
			final Exception expected) {
		try {
			new GroupsAPI(g).requestGroupMembership(token, groupid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void inviteMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.inviteUserToGroup(new Token("t"), new GroupID("gid"), new UserName("bar")))
				.thenReturn(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("gid"), new UserName("foo"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(30000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(GroupRequest.USER_TYPE,
								ResourceDescriptor.from(new UserName("bar")))
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).inviteMember("t", "gid", "bar");
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "foo")
				.with("type", "Invite")
				.with("resourcetype", "user")
				.with("resource", "bar")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 30000L)
				.build()));
	}
	
	@Test
	public void inviteMemberFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failInviteMember(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failInviteMember(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failInviteMember(g, "t", null, "u",
				new MissingParameterException("group id"));
		failInviteMember(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failInviteMember(g, "t", "i", null,
				new MissingParameterException("user name"));
		failInviteMember(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void inviteMemberRequestExists() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.inviteUserToGroup(new Token("t"), new GroupID("i"), new UserName("foo"))).thenThrow(
				new RequestExistsException("reeeeeeee"));
		
		failInviteMember(g, "t", "i", "foo", new RequestExistsException("reeeeeeee"));
	}
	
	
	private void failInviteMember(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).inviteMember(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	// not really sure how to name these other than copy the params.
	@Test
	public void getRequestsForGroup1() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.build();
		getRequestsForGroup("   10000   ", "", "asc", params);
	}
	
	@Test
	public void getRequestsForGroup2() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getRequestsForGroup(null, null, "asc", params);
	}
	
	@Test
	public void getRequestsForGroup3() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.build();
		getRequestsForGroup(null, null, "desc", params);
	}
	
	@Test
	public void getRequestsForGroup4() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getRequestsForGroup(null, null, null, params);
	}
	
	@Test
	public void getRequestsForGroup5() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false).build();
		getRequestsForGroup(null, "", null, params);
	}
	
	@Test
	public void getRequestsForGroup6() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.withResource(new ResourceType("t"), new ResourceID("r"))
				.build();
		getRequestsForGroup(null, "", "desc", "t", "r", params);
	}

	private void getRequestsForGroup(
			final String excludeUpTo,
			final String closed,
			final String sortOrder,
			final GetRequestsParams params)
			throws Exception {
		getRequestsForGroup(excludeUpTo, closed, sortOrder, null, null, params);
	}
	
	private void getRequestsForGroup(
			final String excludeUpTo,
			final String closed,
			final String sortOrder,
			final String resType,
			final String resource,
			final GetRequestsParams params)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		
		
		when(g.getRequestsForGroup(
				new Token("t"), new GroupID("id"), params))
				.thenReturn(Arrays.asList(
						GroupRequest.getBuilder(
								new RequestID(id1), new GroupID("id"), new UserName("foo"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(10000), Instant.ofEpochMilli(40000))
										.build())
								.build(),
						GroupRequest.getBuilder(
								new RequestID(id2), new GroupID("id"), new UserName("bar"),
								CreateModAndExpireTimes.getBuilder(
										Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000))
										.withModificationTime(Instant.ofEpochMilli(25000))
										.build())
								.withType(RequestType.INVITE)
								.withResource(GroupRequest.USER_TYPE,
										ResourceDescriptor.from(new UserName("baz")))
								.withStatus(GroupRequestStatus.canceled())
								.build()
						));
		
		final List<Map<String, Object>> ret = new GroupsAPI(g).getRequestsForGroup(
				"t", "id", excludeUpTo, closed, sortOrder, resType, resource);
		
		assertThat("incorrect requests", ret, is(Arrays.asList(
				MapBuilder.newHashMap()
						.with("id", id1.toString())
						.with("groupid", "id")
						.with("requester", "foo")
						.with("type", "Request")
						.with("resourcetype", "user")
						.with("resource", "foo")
						.with("status", "Open")
						.with("createdate", 10000L)
						.with("moddate", 10000L)
						.with("expiredate", 40000L)
						.build(),
				MapBuilder.newHashMap()
						.with("id", id2.toString())
						.with("groupid", "id")
						.with("requester", "bar")
						.with("type", "Invite")
						.with("resourcetype", "user")
						.with("resource", "baz")
						.with("status", "Canceled")
						.with("createdate", 20000L)
						.with("moddate", 25000L)
						.with("expiredate", 30000L)
						.build()
				)));
	}
	
	@Test
	public void getRequestsForGroupFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForGroup(g, null, "i", null, null, null, null,
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "    \t    ", "i", null, null, null, null,
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "t", null, null, null, null, null,
				new MissingParameterException("group id"));
		failGetRequestsForGroup(g, "t", "   \t   ", null, null, null, null,
				new MissingParameterException("group id"));
	}
	
	@Test
	public void getRequestsForGroupFailIllegalInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForGroup(g, "t", "g", " bar ", null, null, null,
				new IllegalParameterException("Invalid epoch ms: bar"));
		failGetRequestsForGroup(g, "t", "g", "", "   bat   ", null, null,
				new IllegalParameterException("Invalid sort direction: bat"));
		failGetRequestsForGroup(g, "t", "g", null, null, "t", null,
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
		failGetRequestsForGroup(g, "t", "g", null, null, null, "r",
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
	}

	@Test
	public void getRequestsForGroupFailUnauthorized() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForGroup(new Token("t"), new GroupID("i"),
				GetRequestsParams.getBuilder().build()))
				.thenThrow(new UnauthorizedException("yay"));
		
		failGetRequestsForGroup(g, "t", "i",  null, null, null, null,
				new UnauthorizedException("yay"));
	}
	
	private void failGetRequestsForGroup(
			final Groups g,
			final String token,
			final String groupid,
			final String excludeUpTo,
			final String sortOrder,
			final String resType,
			final String resource,
			final Exception expected) {
		try {
			new GroupsAPI(g).getRequestsForGroup(
					token, groupid, excludeUpTo, null, sortOrder, resType, resource);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).removeMember("t", "i", "foo");
		
		verify(g).removeMember(new Token("t"), new GroupID("i"), new UserName("foo"));
	}
	
	@Test
	public void removeMemberFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failRemoveMember(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failRemoveMember(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failRemoveMember(g, "t", null, "u",
				new MissingParameterException("group id"));
		failRemoveMember(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failRemoveMember(g, "t", "i", null,
				new MissingParameterException("user name"));
		failRemoveMember(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void removeMemberNoSuchUser() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchUserException("foo")).when(g)
				.removeMember(new Token("t"), new GroupID("i"), new UserName("foo"));
		
		failRemoveMember(g, "t", "i", "foo", new NoSuchUserException("foo"));
	}
	
	
	private void failRemoveMember(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).removeMember(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void promoteMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).promoteMember("t", "i", "foo");
		
		verify(g).promoteMember(new Token("t"), new GroupID("i"), new UserName("foo"));
	}
	
	@Test
	public void promoteMemberFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failPromoteMember(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failPromoteMember(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failPromoteMember(g, "t", null, "u",
				new MissingParameterException("group id"));
		failPromoteMember(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failPromoteMember(g, "t", "i", null,
				new MissingParameterException("user name"));
		failPromoteMember(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void promoteMemberNoSuchUser() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchUserException("foo")).when(g)
				.promoteMember(new Token("t"), new GroupID("i"), new UserName("foo"));
		
		failPromoteMember(g, "t", "i", "foo", new NoSuchUserException("foo"));
	}
	
	private void failPromoteMember(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).promoteMember(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void demoteAdmin() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).demoteAdmin("t", "i", "foo");
		
		verify(g).demoteAdmin(new Token("t"), new GroupID("i"), new UserName("foo"));
	}
	
	@Test
	public void demoteAdminFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failDemoteAdmin(g, null, "i", "u",
				new NoTokenProvidedException("No token provided"));
		failDemoteAdmin(g, "    \t    ", "i", "u",
				new NoTokenProvidedException("No token provided"));
		failDemoteAdmin(g, "t", null, "u",
				new MissingParameterException("group id"));
		failDemoteAdmin(g, "t", "   \t   ", "u",
				new MissingParameterException("group id"));
		failDemoteAdmin(g, "t", "i", null,
				new MissingParameterException("user name"));
		failDemoteAdmin(g, "t", "i", "  \t    ",
				new MissingParameterException("user name"));
	}
	
	@Test
	public void demoteAdminNoSuchUser() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchUserException("foo")).when(g)
				.demoteAdmin(new Token("t"), new GroupID("i"), new UserName("foo"));
		
		failDemoteAdmin(g, "t", "i", "foo", new NoSuchUserException("foo"));
	}
	
	private void failDemoteAdmin(
			final Groups g,
			final String token,
			final String groupid,
			final String user,
			final Exception expected) {
		try {
			new GroupsAPI(g).demoteAdmin(token, groupid, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void updateUserNullFields() throws Exception {
		final Groups g = mock(Groups.class);
		
		final Map<String, String> fields = new HashMap<>();
		fields.put("f1", null);
		fields.put("f2", null);
		
		
		new GroupsAPI(g).updateUser("   tok  ", "   gid  \t  ", "user",
				new UpdateUserJSON(fields));
		
		verify(g).updateUser(new Token("   tok  "), new GroupID("gid"), new UserName("user"),
				ImmutableMap.of(new NumberedCustomField("f1"), OptionalString.empty(),
						new NumberedCustomField("f2"), OptionalString.empty()));
	}

	@Test
	public void updateUserEmptyFields() throws Exception {
		final Groups g = mock(Groups.class);
		
		final Map<String, String> fields = new HashMap<>();
		fields.put("f1", "  \t    ");
		fields.put("f2", "     \t    ");
		
		
		new GroupsAPI(g).updateUser("   tok  ", "   gid  \t  ", "user",
				new UpdateUserJSON(fields));
		
		verify(g).updateUser(new Token("   tok  "), new GroupID("gid"), new UserName("user"),
				ImmutableMap.of(new NumberedCustomField("f1"), OptionalString.empty(),
						new NumberedCustomField("f2"), OptionalString.empty()));
	}
	
	@Test
	public void updateUserPopulatedFields() throws Exception {
		final Groups g = mock(Groups.class);
		
		final Map<String, String> fields = new HashMap<>();
		fields.put("f1", "   val1  ");
		fields.put("f2", "  \t    val2");
		
		
		new GroupsAPI(g).updateUser("   tok  ", "   gid  \t  ", "user",
				new UpdateUserJSON(fields));
		
		verify(g).updateUser(new Token("   tok  "), new GroupID("gid"), new UserName("user"),
				ImmutableMap.of(new NumberedCustomField("f1"), OptionalString.of("val1"),
						new NumberedCustomField("f2"), OptionalString.of("val2")));
	}
	
	@Test
	public void updateUserFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		final UpdateUserJSON j = new UpdateUserJSON(ImmutableMap.of("f", "v"));
		
		updateUserFail(g, null, "i", "u", j,
				new NoTokenProvidedException("No token provided"));
		updateUserFail(g, "    \t    ", "i", "u", j,
				new NoTokenProvidedException("No token provided"));
		updateUserFail(g, "t", null, "u", j,
				new MissingParameterException("group id"));
		updateUserFail(g, "t", "   \t   ", "u", j,
				new MissingParameterException("group id"));
		updateUserFail(g, "t", "i", null, j,
				new MissingParameterException("user name"));
		updateUserFail(g, "t", "i", "  \t    ", j,
				new MissingParameterException("user name"));
		updateUserFail(g, "t", "i", "u", null,
				new MissingParameterException("Missing JSON body"));
		updateUserFail(g, "t", "i", "u", new UpdateUserJSON(null),
				new MissingParameterException("No fields provided to update"));
		updateUserFail(g, "t", "i", "u", new UpdateUserJSON(Collections.emptyMap()),
				new MissingParameterException("No fields provided to update"));
	}
	
	@Test
	public void updateUserFailExtraProperties() throws Exception {
		final Groups g = mock(Groups.class);
		final UpdateUserJSON b = new UpdateUserJSON(ImmutableMap.of("a", "b"));
		b.setAdditionalProperties("foo", "bar");

		updateUserFail(g, "t", "i", "u", b, new IllegalParameterException(
				"Unexpected parameters in request: foo"));
	}
	
	@Test
	public void updateUserFailCustomNotMap() throws Exception {
		final Groups g = mock(Groups.class);
		final UpdateUserJSON b = new UpdateUserJSON("customstr");

		updateUserFail(g, "t", "i", "u", b, new IllegalParameterException(
				"'custom' field must be a mapping"));
	}
	
	@Test
	public void updateUserFailCustomNotStringValue() throws Exception {
		final Groups g = mock(Groups.class);
		final UpdateUserJSON b = new UpdateUserJSON(
				ImmutableMap.of("foo-1", Collections.emptyList()));

		updateUserFail(g, "t", "i", "u", b, new IllegalParameterException(
				"Value of 'foo-1' field in 'custom' map is not a string"));
	}
	
	@Test
	public void updateUserFailBadCustomField() throws Exception {
		final Groups g = mock(Groups.class);
		final UpdateUserJSON b = new UpdateUserJSON(ImmutableMap.of("foo-1-1", "yay"));

		updateUserFail(g, "t", "i", "u", b, new IllegalParameterException(
				"Suffix after - of field foo-1-1 must be an integer > 0"));
	}
	
	@Test
	public void updateUserFailNoSuchGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchUserException("foo")).when(g).updateUser(
				new Token("tok"), new GroupID("i"), new UserName("n"),
				ImmutableMap.of(new NumberedCustomField("f"), OptionalString.empty()));

		updateUserFail(g, "tok", "  i", "n", new UpdateUserJSON(ImmutableMap.of("f", "  ")),
				new NoSuchUserException("foo"));
	}
	
	private void updateUserFail(
			final Groups g,
			final String token,
			final String groupID,
			final String user,
			final UpdateUserJSON body,
			final Exception expected) {
		try {
			new GroupsAPI(g).updateUser(token, groupID, user, body);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addResourceNoRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addResource(new Token("my token"), new GroupID("foo"),
				new ResourceType("workspace"), new ResourceID("34")))
				.thenReturn(Optional.empty());
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addResource("my token", "foo", "workspace", "34");
		
		assertThat("incorrect ret", ret, is(ImmutableMap.of("complete", true)));
	}
	
	@Test
	public void addResourceWithRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addResource(new Token("my token"), new GroupID("foo"),
				new ResourceType("workspace"), new ResourceID("42")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.REQUEST)
						.withResource(new ResourceType("workspace"),
								new ResourceDescriptor(new ResourceID("42")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addResource("my token", "foo", "workspace", "42");
		
		assertThat("incorrect ret", ret, is(MapBuilder.newHashMap()
				.with("complete", false)
				.with("id", id.toString())
				.with("groupid", "foo")
				.with("requester", "u")
				.with("type", "Request")
				.with("resourcetype", "workspace")
				.with("resource", "42")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void addResourceWithRequestInvite() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addResource(new Token("my token"), new GroupID("foo"),
				new ResourceType("catalogmethod"), new ResourceID("mod.meth")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.INVITE)
						.withResource(new ResourceType("catalogmethod"),
								new ResourceDescriptor(new ResourceAdministrativeID("mod"),
										new ResourceID("mod.meth")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addResource("my token", "foo", "catalogmethod", "mod.meth");
		
		assertThat("incorrect ret", ret, is(MapBuilder.newHashMap()
				.with("complete", false)
				.with("id", id.toString())
				.with("groupid", "foo")
				.with("requester", "u")
				.with("type", "Invite")
				.with("resourcetype", "catalogmethod")
				.with("resource", "mod.meth")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void addResourceFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failAddResource(g, null, "id", "t", "45", new NoTokenProvidedException("No token provided"));
		failAddResource(g, "  \t  ", "id", "t", "45",
				new NoTokenProvidedException("No token provided"));
		failAddResource(g, "t", "illegal*id", "t", "45",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failAddResource(g, "t", "id", null, "45",
				new MissingParameterException("resource type"));
		failAddResource(g, "t", "id", "t", "   \t   ",
				new MissingParameterException("resource ID"));
	}
	
	@Test
	public void addResourceFailNoSuchResource() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addResource(new Token("t"), new GroupID("i"),
				new ResourceType("ty"), new ResourceID("34")))
				.thenThrow(new NoSuchResourceException("34"));
		
		failAddResource(g, "t", "i", "ty", "34", new NoSuchResourceException("34"));
	}
	
	private void failAddResource(
			final Groups g,
			final String t,
			final String i,
			final String type,
			final String w,
			final Exception expected) {
		try {
			new GroupsAPI(g).addResource(t, i, type, w);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeResource() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).removeResource("t", "gid", "rtype", "99");
		
		verify(g).removeResource(new Token("t"), new GroupID("gid"), new ResourceType("rtype"),
				new ResourceID("99"));
	}
	
	@Test
	public void removeResourceFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failRemoveResource(g, null, "id", "t", "45",
				new NoTokenProvidedException("No token provided"));
		failRemoveResource(g, "  \t  ", "id", "t", "45",
				new NoTokenProvidedException("No token provided"));
		failRemoveResource(g, "t", "illegal*id", "t", "45",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failRemoveResource(g, "t", "i", "illegal*type", "45",
				new IllegalParameterException(
						"Illegal character in resource type illegal*type: *"));
		failRemoveResource(g, "t", "id", "t", "   foo\nbar    ",
				new IllegalParameterException("resource ID contains control characters"));
	}
	
	@Test
	public void removeResourceFailNoSuchGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchGroupException("i")).when(g)
				.removeResource(new Token("t"), new GroupID("i"), new ResourceType("type"),
						new ResourceID("34"));
		
		failRemoveResource(g, "t", "i", "type", "34", new NoSuchGroupException("i"));
	}
	
	private void failRemoveResource(
			final Groups g,
			final String t,
			final String i,
			final String type,
			final String r,
			final Exception expected) {
		try {
			new GroupsAPI(g).removeResource(t, i, type, r);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getPerms() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).getPerms("t", "gid", "rtype", "99");
		
		verify(g).setReadPermission(new Token("t"), new GroupID("gid"), new ResourceType("rtype"),
				new ResourceID("99"));
	}
	
	@Test
	public void getPermsFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		getPermsFail(g, null, "id", "t", "45",
				new NoTokenProvidedException("No token provided"));
		getPermsFail(g, "  \t  ", "id", "t", "45",
				new NoTokenProvidedException("No token provided"));
		getPermsFail(g, "t", "illegal*id", "t", "45",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		getPermsFail(g, "t", "i", "illegal*type", "45",
				new IllegalParameterException(
						"Illegal character in resource type illegal*type: *"));
		getPermsFail(g, "t", "id", "t", "   foo\nbar    ",
				new IllegalParameterException("resource ID contains control characters"));
	}
	
	@Test
	public void getPermsFailNoSuchResource() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchResourceException("type")).when(g)
				.setReadPermission(new Token("t"), new GroupID("i"), new ResourceType("type"),
						new ResourceID("34"));
		
		getPermsFail(g, "t", "i", "type", "34", new NoSuchResourceException("type"));
	}
	
	private void getPermsFail(
			final Groups g,
			final String t,
			final String i,
			final String type,
			final String r,
			final Exception expected) {
		try {
			new GroupsAPI(g).getPerms(t, i, type, r);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
