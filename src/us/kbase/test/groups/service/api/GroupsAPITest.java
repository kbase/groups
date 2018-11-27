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
import us.kbase.groups.core.FieldItem.StringField;
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
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class GroupsAPITest {

	private static <T> Optional<T> op(T item) {
		return Optional.ofNullable(item);
	}
	
	private static final Group GROUP_MIN;
	private static final Group GROUP_MAX;
	static {
		try {
			GROUP_MIN = Group.getBuilder(
					new GroupID("id"), new GroupName("name"), new UserName("u"),
					new CreateAndModTimes(Instant.ofEpochMilli(10000)))
					.build();
			GROUP_MAX = Group.getBuilder(
					new GroupID("id2"), new GroupName("name2"), new UserName("u2"),
					new CreateAndModTimes(
							Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
					.withDescription("desc")
					.withMember(new UserName("foo"))
					.withMember(new UserName("bar"))
					.withAdministrator(new UserName("whee"))
					.withAdministrator(new UserName("whoo"))
					.withType(GroupType.PROJECT)
					.withCustomField(new NumberedCustomField("field-1"), "my val")
					.withCustomField(new NumberedCustomField("otherfield"), "fieldval")
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix your tests newb", e);
		}
	}
	
	private static final Map<String, Object> GROUP_MIN_JSON_STD = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id")
			.with("name", "name")
			.with("owner", "u")
			.with("createdate", 10000L)
			.with("moddate", 10000L)
			.with("type", "Organization")
			.with("description", null)
			.with("resources", Collections.emptyMap())
			.with("members", Collections.emptyList())
			.with("admins", Collections.emptyList())
			.with("custom", Collections.emptyMap())
			.build();
	
	private static final Map<String, Object> GROUP_MIN_JSON_MIN = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id")
			.with("name", "name")
			.with("owner", "u")
			.with("type", "Organization")
			.with("custom", Collections.emptyMap())
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_STD = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("name", "name2")
			.with("owner", "u2")
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("type", "Project")
			.with("description", "desc")
			.with("members", Arrays.asList("bar", "foo"))
			.with("admins", Arrays.asList("whee", "whoo"))
			.with("resources", Collections.emptyMap())
			.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_NON = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("name", "name2")
			.with("owner", "u2")
			.with("createdate", 20000L)
			.with("moddate", 30000L)
			.with("type", "Project")
			.with("description", "desc")
			.with("members", Collections.emptyList())
			.with("admins", Arrays.asList("whee", "whoo"))
			.with("resources", Collections.emptyMap())
			.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
			.build();
	
	private static final Map<String, Object> GROUP_MAX_JSON_MIN = MapBuilder
			.<String, Object>newHashMap()
			.with("id", "id2")
			.with("name", "name2")
			.with("owner", "u2")
			.with("type", "Project")
			.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
			.build();

	@Test
	public void getGroupsNulls() throws Exception {
		getGroups(null, null, GetGroupsParams.getBuilder().build());
	}
	
	@Test
	public void getGroupsWhitespace() throws Exception {
		getGroups("   \t   ", "   \t   ", GetGroupsParams.getBuilder().build());
	}
	
	@Test
	public void getGroupsWhitespaceValuesAsc() throws Exception {
		getGroups("   foo  \t  ", "  asc  \t ", GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("foo").build());
	}
	
	@Test
	public void getGroupsWhitespaceValuesDesc() throws Exception {
		getGroups("   foo  \t  ", "  desc  \t ", GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("foo")
				.withNullableSortAscending(false).build());
	}

	private void getGroups(
			final String excludeUpTo,
			final String order,
			final GetGroupsParams expected)
			throws Exception {
		final Groups g = mock(Groups.class);
		when(g.getGroups(expected)).thenReturn(Arrays.asList(
				GroupView.getBuilder(GROUP_MAX, null).build(),
				GroupView.getBuilder(GROUP_MIN, null).build()));
		final List<Map<String, Object>> ret = new GroupsAPI(g)
				.getGroups("unused for now", excludeUpTo, order);
		
		assertThat("incorrect groups", ret,
				is(Arrays.asList(GROUP_MAX_JSON_MIN, GROUP_MIN_JSON_MIN)));
	}
	
	@Test
	public void failGetGroups() throws Exception {
		final Groups g = mock(Groups.class);
		try {
			new GroupsAPI(g).getGroups("t", null, "  asd   ");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalParameterException(
					"Invalid sort direction: asd"));
		}
	}
	
	@Test
	public void createGroupMinimalNulls() throws Exception {
		createGroupMinimal(null, null);
	}
	
	@Test
	public void createGroupMinimalEmptyOptional() throws Exception {
		createGroupMinimal(Optional.empty(), MapBuilder.<String, String>newHashMap()
				.with("key", null).build());
	}
	
	@Test
	public void createGroupMinimalWhitespace() throws Exception {
		createGroupMinimal(Optional.of("    \t    "), MapBuilder.<String, String>newHashMap()
				.with("key", "    \t    ").build());
	}
	
	private void createGroupMinimal(
			final Optional<String> noInput,
			final Map<String, String> custom)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name")).build()))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("u2"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup(
				"toke", "gid", new CreateOrUpdateGroupJSON(op("name"), noInput, noInput, custom));
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_STD));
	}
	
	@Test
	public void createGroupMaximal() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.createGroup(new Token("toke"), GroupCreationParams.getBuilder(
				new GroupID("gid"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("my desc"))
						.withCustomField(new NumberedCustomField("foo-23"),
								StringField.from("yay"))
						.withCustomField(new NumberedCustomField("doodybutt"),
								StringField.from("yo"))
						.build())
				.withType(GroupType.TEAM)
				.build()))
				.thenReturn(GroupView.getBuilder(GROUP_MIN, new UserName("u"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).createGroup("toke", "gid",
				new CreateOrUpdateGroupJSON(op("name"), op("Team"), op("my desc"),
						ImmutableMap.of("foo-23", "yay", "doodybutt", "yo")));
		
		assertThat("incorrect group", ret, is(GROUP_MIN_JSON_STD));
	}
	
	@Test
	public void createGroupFailNullsAndWhitespace() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(op("n"), null, null, null);
		
		failCreateGroup(g, null, "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "  \t  ", "i", b, new NoTokenProvidedException("No token provided"));
		failCreateGroup(g, "t", null, b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "   \t  ", b, new MissingParameterException("group id"));
		failCreateGroup(g, "t", "i", null, new MissingParameterException("Missing JSON body"));
		failCreateGroup(g, "t", "i", new CreateOrUpdateGroupJSON(null, null, null, null),
				new MissingParameterException("group name"));
		failCreateGroup(g, "t", "i",
				new CreateOrUpdateGroupJSON(Optional.empty(), null, null, null),
				new MissingParameterException("group name"));
		failCreateGroup(g, "t", "i",
				new CreateOrUpdateGroupJSON(op("   \t    "), null, null, null),
				new MissingParameterException("group name"));
		
	}
	
	@Test
	public void createGroupFailExtraProperties() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(op("n"), null, null, null);
		b.setAdditionalProperties("foo", "bar");

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Unexpected parameters in request: foo"));
	}
	
	@Test
	public void createGroupFailBadType() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), op("Teem"), null, null);

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Invalid group type: Teem"));
	}
	
	@Test
	public void createGroupFailCustomNotMap() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, "customstr");

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"'custom' field must be a mapping"));
	}
	
	@Test
	public void createGroupFailCustomNotStringValue() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, ImmutableMap.of("foo-1", Collections.emptyList()));

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Value of 'foo-1' field in 'custom' map is not a string"));
	}
	
	@Test
	public void createGroupFailBadCustomField() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, ImmutableMap.of("foo-1-1", "yay"));

		failCreateGroup(g, "t", "i", b, new IllegalParameterException(
				"Suffix after - of field foo-1-1 must be an integer > 0"));
	}
	
	@Test
	public void createGroupFailGroupExists() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(op("n"), null, null, null);

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
		final Optional<String> mt = Optional.empty();
		updateGroup(mt, mt, mt, Collections.emptyMap(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove()).build())
				.build());
	}
	
	@Test
	public void updateGroupNullCustom() throws Exception {
		final Optional<String> mt = Optional.empty();
		updateGroup(mt, mt, mt, MapBuilder.<String, String>newHashMap()
				.with("foo", null).with("bar", null).build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove())
						.withCustomField(new NumberedCustomField("foo"), StringField.remove())
						.withCustomField(new NumberedCustomField("bar"), StringField.remove())
						.build())
				.build());
	}
	
	@Test
	public void updateGroupAllWhitespace() throws Exception {
		final Optional<String> ws = Optional.ofNullable("   \t    ");
		updateGroup(ws, ws, ws, MapBuilder.<String, String>newHashMap()
				.with("foo", "   \t   ").with("bar", "   \t    ").build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.remove())
						.withCustomField(new NumberedCustomField("foo"), StringField.remove())
						.withCustomField(new NumberedCustomField("bar"), StringField.remove())
						.build())
				.build());
	}
	
	@Test
	public void updateGroupWithValues() throws Exception {
		updateGroup(
				Optional.of("    name   "),
				Optional.of("    Team   "),
				Optional.of("   desc    "),
				MapBuilder.<String, String>newHashMap()
						.with("foo", "baz").with("bar", "bat").build(),
				GroupUpdateParams.getBuilder(new GroupID("gid"))
						.withName(new GroupName("name"))
						.withType(GroupType.TEAM)
						.withOptionalFields(OptionalGroupFields.getBuilder()
								.withDescription(StringField.from("desc"))
								.withCustomField(new NumberedCustomField("foo"),
										StringField.from("baz"))
								.withCustomField(new NumberedCustomField("bar"),
										StringField.from("bat"))
								.build())
						.build());
	}
	
	private void updateGroup(
			final Optional<String> groupName,
			final Optional<String> type,
			final Optional<String> description,
			final Map<String, String> custom,
			final GroupUpdateParams expected)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).updateGroup("tok", "    gid   ",
				new CreateOrUpdateGroupJSON(groupName, type, description, custom));
		
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
	public void updateGroupFailBadType() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				null, op("Teem"), null, null);

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"Invalid group type: Teem"));
	}
	
	@Test
	public void updateGroupFailCustomNotMap() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, "customstr");

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"'custom' field must be a mapping"));
	}
	
	@Test
	public void updateGroupFailCustomNotStringValue() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, ImmutableMap.of("foo-1", Collections.emptyList()));

		failUpdateGroup(g, "t", "i", b, new IllegalParameterException(
				"Value of 'foo-1' field in 'custom' map is not a string"));
	}
	
	@Test
	public void updateGroupFailBadCustomField() throws Exception {
		final Groups g = mock(Groups.class);
		final CreateOrUpdateGroupJSON b = new CreateOrUpdateGroupJSON(
				op("n"), null, null, ImmutableMap.of("foo-1-1", "yay"));

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

		failUpdateGroup(g, "tok", "  gid", new CreateOrUpdateGroupJSON(
				Optional.ofNullable("name"), null, null, null),
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
	
	private void getGroup(final String token, final Token expected) throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(expected, new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("bar"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup(token, "id");
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_STD));
	}
	
	@Test
	public void getGroupNonMember() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getGroup(new Token("toke"), new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("nonmember"))
						.withStandardView(true).build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		
		assertThat("incorrect group", ret, is(GROUP_MAX_JSON_NON));
	}
	
	@Test
	public void getGroupWithResources() throws Exception {
		final Groups g = mock(Groups.class);
		
		final ResourceDescriptor d1 = new ResourceDescriptor(
				new ResourceAdministrativeID("82"), new ResourceID("82"));
		final ResourceDescriptor d2 = new ResourceDescriptor(
				new ResourceAdministrativeID("45"), new ResourceID("45"));
		
		final ResourceDescriptor c1 = new ResourceDescriptor(
				new ResourceAdministrativeID("mod"), new ResourceID("mod.meth"));
		
		when(g.getGroup(new Token("toke"), new GroupID("id")))
				.thenReturn(GroupView.getBuilder(GROUP_MAX, new UserName("whee"))
						.withStandardView(true)
						.withResourceType(new ResourceType("foo"))
						.withResource(new ResourceType("workspace"),
								ResourceInformationSet.getBuilder(new UserName("whee"))
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
								ResourceInformationSet.getBuilder(new UserName("whee"))
										.withResourceDescriptor(c1)
										.build())
						.build());
		
		final Map<String, Object> ret = new GroupsAPI(g).getGroup("toke", "id");
		final Map<String, Object> expected = new HashMap<>();
		expected.putAll(GROUP_MAX_JSON_STD);
		expected.put("resources", ImmutableMap.of(
				"foo", Collections.emptyList(),
				"catalogmethod", Arrays.asList(ImmutableMap.of("rid", "mod.meth")),
				"workspace", Arrays.asList(
						MapBuilder.newHashMap()
								.with("rid", "45")
								.with("name", "name45")
								.with("narrname", null)
								.with("public", false)
								.with("perm", "None")
								.build(),
						MapBuilder.newHashMap()
								.with("rid", "82")
								.with("name", "name82")
								.with("narrname", "narrname")
								.with("public", true)
								.with("perm", "Admin")
								.build()
						)));
		
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
						.withResourceType(new ResourceType("user"))
						.withResource(ResourceDescriptor.from(new UserName("foo")))
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
						.withResourceType(new ResourceType("user"))
						.withResource(ResourceDescriptor.from(new UserName("bar")))
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
				.withNullableSortAscending(false).build();
		getRequestsForGroup(null, "", "desc", params);
	}

	private void getRequestsForGroup(
			final String excludeUpTo,
			final String closed,
			final String sortOrder,
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
								.withResourceType(new ResourceType("user"))
								.withResource(ResourceDescriptor.from(new UserName("baz")))
								.withStatus(GroupRequestStatus.canceled())
								.build()
						));
		
		final List<Map<String, Object>> ret = new GroupsAPI(g).getRequestsForGroup(
				"t", "id", excludeUpTo, closed, sortOrder);
		
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
		
		failGetRequestsForGroup(g, null, "i", null, null,
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "    \t    ", "i", null, null,
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForGroup(g, "t", null, null, null,
				new MissingParameterException("group id"));
		failGetRequestsForGroup(g, "t", "   \t   ", null, null,
				new MissingParameterException("group id"));
	}
	
	@Test
	public void getRequestsForGroupFailIllegalInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForGroup(g, "t", "g", " bar ", null,
				new IllegalParameterException("Invalid epoch ms: bar"));
		failGetRequestsForGroup(g, "t", "g", "", "   bat   ", 
				new IllegalParameterException("Invalid sort direction: bat"));
	}

	@Test
	public void getRequestsForGroupFailUnauthorized() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForGroup(new Token("t"), new GroupID("i"),
				GetRequestsParams.getBuilder().build()))
				.thenThrow(new UnauthorizedException("yay"));
		
		failGetRequestsForGroup(g, "t", "i",  null, null, new UnauthorizedException("yay"));
	}
	
	private void failGetRequestsForGroup(
			final Groups g,
			final String token,
			final String groupid,
			final String excludeUpTo,
			final String sortOrder,
			final Exception expected) {
		try {
			new GroupsAPI(g).getRequestsForGroup(token, groupid, excludeUpTo, null, sortOrder);
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
	public void addWorkspaceNoRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addWorkspace(new Token("my token"), new GroupID("foo"), new ResourceID("34")))
				.thenReturn(Optional.empty());
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addWorkspace("my token", "foo", "34");
		
		assertThat("incorrect ret", ret, is(ImmutableMap.of("complete", true)));
	}
	
	@Test
	public void addWorkspaceWithRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addWorkspace(new Token("my token"), new GroupID("foo"), new ResourceID("34")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.REQUEST)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("42")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addWorkspace("my token", "foo", "34");
		
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
	public void addWorkspaceWithRequestInvite() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addWorkspace(new Token("my token"), new GroupID("foo"), new ResourceID("34")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("workspace"))
						.withResource(new ResourceDescriptor(new ResourceID("42")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addWorkspace("my token", "foo", "34");
		
		assertThat("incorrect ret", ret, is(MapBuilder.newHashMap()
				.with("complete", false)
				.with("id", id.toString())
				.with("groupid", "foo")
				.with("requester", "u")
				.with("type", "Invite")
				.with("resourcetype", "workspace")
				.with("resource", "42")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void addWorkspaceFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failAddWorkspace(g, null, "id", "45", new NoTokenProvidedException("No token provided"));
		failAddWorkspace(g, "  \t  ", "id", "45",
				new NoTokenProvidedException("No token provided"));
		failAddWorkspace(g, "t", "illegal*id", "45",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failAddWorkspace(g, "t", "id", "   \t   ",
				new MissingParameterException("resource ID"));
	}
	
	@Test
	public void addWorkspaceFailNoSuchWorkspace() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addWorkspace(new Token("t"), new GroupID("i"), new ResourceID("34")))
				.thenThrow(new NoSuchResourceException("34"));
		
		failAddWorkspace(g, "t", "i", "34", new NoSuchResourceException("34"));
	}
	
	private void failAddWorkspace(
			final Groups g,
			final String t,
			final String i,
			final String w,
			final Exception expected) {
		try {
			new GroupsAPI(g).addWorkspace(t, i, w);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeWorkspace() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).removeWorkspace("t", "gid", "99");
		
		verify(g).removeWorkspace(new Token("t"), new GroupID("gid"), new ResourceID("99"));
	}
	
	@Test
	public void removeWorkspaceFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failRemoveWorkspace(g, null, "id", "45", new NoTokenProvidedException("No token provided"));
		failRemoveWorkspace(g, "  \t  ", "id", "45",
				new NoTokenProvidedException("No token provided"));
		failRemoveWorkspace(g, "t", "illegal*id", "45",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failRemoveWorkspace(g, "t", "id", "   foo\nbar    ",
				new IllegalParameterException("resource ID contains control characters"));
	}
	
	@Test
	public void removeWorkspaceFailNoSuchGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchGroupException("i")).when(g)
				.removeWorkspace(new Token("t"), new GroupID("i"), new ResourceID("34"));
		
		failRemoveWorkspace(g, "t", "i", "34", new NoSuchGroupException("i"));
	}
	
	private void failRemoveWorkspace(
			final Groups g,
			final String t,
			final String i,
			final String w,
			final Exception expected) {
		try {
			new GroupsAPI(g).removeWorkspace(t, i, w);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addCatalogMethodNoRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addCatalogMethod(
				new Token("my token"), new GroupID("foo"), new ResourceID("m.n")))
				.thenReturn(Optional.empty());
		
		final Map<String, Object> ret = new GroupsAPI(g).addCatalogMethod(
				"my token", "foo", "m.n");
		
		assertThat("incorrect ret", ret, is(ImmutableMap.of("complete", true)));
	}
	
	@Test
	public void addCatalogMethodWithRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addCatalogMethod(
				new Token("my token"), new GroupID("foo"), new ResourceID("m.x")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.REQUEST)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceID("m.x")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addCatalogMethod("my token", "foo", "m.x");
		
		assertThat("incorrect ret", ret, is(MapBuilder.newHashMap()
				.with("complete", false)
				.with("id", id.toString())
				.with("groupid", "foo")
				.with("requester", "u")
				.with("type", "Request")
				.with("resourcetype", "catalogmethod")
				.with("resource", "m.x")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void addCatalogMethodWithRequestInvite() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.addCatalogMethod(
				new Token("my token"), new GroupID("foo"), new ResourceID("m.y")))
				.thenReturn(Optional.of(GroupRequest.getBuilder(
						new RequestID(id), new GroupID("foo"), new UserName("u"),
							CreateModAndExpireTimes.getBuilder(
									Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
									.build())
						.withType(RequestType.INVITE)
						.withResourceType(new ResourceType("catalogmethod"))
						.withResource(new ResourceDescriptor(new ResourceID("m.y")))
						.build()));
		
		final Map<String, Object> ret = new GroupsAPI(g)
				.addCatalogMethod("my token", "foo", "m.y");
		
		assertThat("incorrect ret", ret, is(MapBuilder.newHashMap()
				.with("complete", false)
				.with("id", id.toString())
				.with("groupid", "foo")
				.with("requester", "u")
				.with("type", "Invite")
				.with("resourcetype", "catalogmethod")
				.with("resource", "m.y")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void addCatalogMethodFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failAddCatalogMethod(g, null, "id", "m.n",
				new NoTokenProvidedException("No token provided"));
		failAddCatalogMethod(g, "  \t  ", "id", "m.n",
				new NoTokenProvidedException("No token provided"));
		failAddCatalogMethod(g, "t", "illegal*id", "m.n",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failAddCatalogMethod(g, "t", "id", "   \t    ",
				new MissingParameterException("resource ID"));
	}
	
	@Test
	public void addCatalogMethodFailNoSuchMethod() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.addCatalogMethod(new Token("t"), new GroupID("i"), new ResourceID("m.n")))
				.thenThrow(new NoSuchResourceException("m.n"));
		
		failAddCatalogMethod(g, "t", "i", "m.n", new NoSuchResourceException("m.n"));
	}
	
	private void failAddCatalogMethod(
			final Groups g,
			final String t,
			final String i,
			final String m,
			final Exception expected) {
		try {
			new GroupsAPI(g).addCatalogMethod(t, i, m);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeCatalogMethod() throws Exception {
		final Groups g = mock(Groups.class);
		
		new GroupsAPI(g).removeCatalogMethod("t", "gid", "m.n");
		
		verify(g).removeCatalogMethod(
				new Token("t"), new GroupID("gid"), new ResourceID("m.n"));
	}
	
	@Test
	public void removeCatalogMethodFailBadArgs() throws Exception {
		final Groups g = mock(Groups.class);
		failRemoveCatalogMethod(g, null, "id", "m.n",
				new NoTokenProvidedException("No token provided"));
		failRemoveCatalogMethod(g, "  \t  ", "id", "m.n",
				new NoTokenProvidedException("No token provided"));
		failRemoveCatalogMethod(g, "t", "illegal*id", "m.n",
				new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
						"Illegal character in group id illegal*id: *"));
		failRemoveCatalogMethod(g, "t", "id", "   \t   ",
				new MissingParameterException("resource ID"));
	}
	
	@Test
	public void removeCatalogMethodFailNoSuchGroup() throws Exception {
		final Groups g = mock(Groups.class);
		
		doThrow(new NoSuchGroupException("i")).when(g)
				.removeCatalogMethod(new Token("t"), new GroupID("i"), new ResourceID("m.n"));
		
		failRemoveCatalogMethod(g, "t", "i", "m.n", new NoSuchGroupException("i"));
	}
	
	private void failRemoveCatalogMethod(
			final Groups g,
			final String t,
			final String i,
			final String m,
			final Exception expected) {
		try {
			new GroupsAPI(g).removeCatalogMethod(t, i, m);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
