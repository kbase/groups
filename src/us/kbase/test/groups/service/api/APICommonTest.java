package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.service.api.APICommon;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class APICommonTest {
	
	private static Builder getGroupMaxBuilder()
			throws MissingParameterException, IllegalParameterException {
		return Group.getBuilder(
				new GroupID("id2"), new GroupName("name2"),
				GroupUser.getBuilder(new UserName("u2"), inst(20000))
						.withNullableLastVisit(inst(34000))
						.build(),
				new CreateAndModTimes(
						Instant.ofEpochMilli(20000), Instant.ofEpochMilli(30000)))
				.withMember(GroupUser.getBuilder(new UserName("foo"), inst(650000))
						.withCustomField(new NumberedCustomField("whee"), "whoo")
						.withNullableLastVisit(inst(25000))
						.build())
				.withMember(GroupUser.getBuilder(new UserName("bar"), inst(40000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whee"), inst(220000))
						.build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whoo"), inst(760000))
						.withCustomField(new NumberedCustomField("yay-6"), "boo")
						.withCustomField(new NumberedCustomField("bar"), "baz")
						.withNullableLastVisit(inst(62000))
						.build())
				.withResource(new ResourceType("ws"), new ResourceDescriptor(new ResourceID("a")),
						inst(45000))
				.withResource(new ResourceType("ws"), new ResourceDescriptor(new ResourceID("b")))
				.withResource(new ResourceType("cat"), new ResourceDescriptor(new ResourceID("c")))
				.withCustomField(new NumberedCustomField("field-1"), "my val")
				.withCustomField(new NumberedCustomField("otherfield"), "fieldval");
	}
	
	@Test
	public void toGroupRequestJSON() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("n")))
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("resource", "n")
				.with("resourcetype", "user")
				.with("type", "Request")
				.with("status", "Open")
				.with("createdate", 10000L)
				.with("moddate", 10000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONWithTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(25000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("inv")))
				.withStatus(GroupRequestStatus.denied(new UserName("den"), "r"))
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("resource", "inv")
				.with("resourcetype", "user")
				.with("type", "Invite")
				.with("status", "Denied")
				.with("createdate", 10000L)
				.with("moddate", 25000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONWithWSTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(25000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("42")))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("resource", "42")
				.with("resourcetype", "workspace")
				.with("type", "Invite")
				.with("status", "Canceled")
				.with("createdate", 10000L)
				.with("moddate", 25000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONWithInviteMethod() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(25000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceID("mod.meth")))
				.withStatus(GroupRequestStatus.expired())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("resource", "mod.meth")
				.with("resourcetype", "catalogmethod")
				.with("type", "Invite")
				.with("status", "Expired")
				.with("createdate", 10000L)
				.with("moddate", 25000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONWithRequestMethod() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(25000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceID("mod.meth")))
				.withStatus(GroupRequestStatus.expired())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("resource", "mod.meth")
				.with("resourcetype", "catalogmethod")
				.with("type", "Request")
				.with("status", "Expired")
				.with("createdate", 10000L)
				.with("moddate", 25000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONFail() throws Exception {
		try {
			APICommon.toGroupRequestJSON((GroupRequest) null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("request"));
		}
	}
	
	@Test
	public void toGroupRequestJSONList() throws Exception {
		final UUID id1 = UUID.randomUUID();
		final GroupRequest r1 = GroupRequest.getBuilder(
				new RequestID(id1), new GroupID("gid1"), new UserName("n1"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("n1")))
				.build();
		
		final UUID id2 = UUID.randomUUID();
		final GroupRequest r2 = GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("gid2"), new UserName("n2"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(11000), Instant.ofEpochMilli(21000))
						.withModificationTime(Instant.ofEpochMilli(26000))
						.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("user"))
				.withResource(ResourceDescriptor.from(new UserName("inv")))
				.withStatus(GroupRequestStatus.denied(new UserName("den"), "r"))
				.build();
		
		final UUID id3 = UUID.randomUUID();
		final GroupRequest r3 = GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("gid3"), new UserName("n3"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(12000), Instant.ofEpochMilli(22000))
						.withModificationTime(Instant.ofEpochMilli(27000))
						.build())
				.withType(RequestType.REQUEST)
				.withResourceType(new ResourceType("workspace"))
				.withResource(new ResourceDescriptor(new ResourceID("42")))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(Arrays.asList(r2, r1, r3)),
				is(Arrays.asList(
						MapBuilder.newHashMap()
								.with("id", id2.toString())
								.with("groupid", "gid2")
								.with("requester", "n2")
								.with("resource", "inv")
								.with("resourcetype", "user")
								.with("type", "Invite")
								.with("status", "Denied")
								.with("createdate", 11000L)
								.with("moddate", 26000L)
								.with("expiredate", 21000L)
								.build(),
						MapBuilder.newHashMap()
								.with("id", id1.toString())
								.with("groupid", "gid1")
								.with("requester", "n1")
								.with("resource", "n1")
								.with("resourcetype", "user")
								.with("type", "Request")
								.with("status", "Open")
								.with("createdate", 10000L)
								.with("moddate", 10000L)
								.with("expiredate", 20000L)
								.build(),
						MapBuilder.newHashMap()
								.with("id", id3.toString())
								.with("groupid", "gid3")
								.with("requester", "n3")
								.with("resource", "42")
								.with("resourcetype", "workspace")
								.with("type", "Request")
								.with("status", "Canceled")
								.with("createdate", 12000L)
								.with("moddate", 27000L)
								.with("expiredate", 22000L)
								.build()
						)));
	}

	
	@Test
	public void toGroupRequestJSONListFail() throws Exception {
		try {
			APICommon.toGroupRequestJSON((List<GroupRequest>) null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("requests"));
		}
	}
	
	@Test
	public void toGroupJSONPrivateGroup() throws Exception {
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().withIsPrivate(true).build(),
				new UserName("nonmember"))
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(f -> true)
				.withPublicUserFieldDeterminer(f -> true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(ImmutableMap.of(
				"id", "id2", "private", true, "role", "None")));
	}
	
	@Test
	public void toGroupJSONMinimalView() throws Exception {
		toGroupJSONMinimalView("foo", 25000L, "Member");
		toGroupJSONMinimalView("whee", null, "Admin");
	}

	private void toGroupJSONMinimalView(final String user, final Long lastVisit, final String role)
			throws MissingParameterException, IllegalParameterException {
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().build(),
				new UserName(user))
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(f -> true)
				.withPublicUserFieldDeterminer(f -> true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(MapBuilder.newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("role", role)
				.with("name", "name2")
				.with("owner", "u2")
				.with("lastvisit", lastVisit)
				.with("memcount", 5)
				.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("rescount", ImmutableMap.of("ws", 2, "cat", 1))
				.build()));
	}
	
	@Test
	public void toGroupJSONStandardView() throws Exception {
		toGroupJSONStandardView(new UserName("foo"), null, null, null, 25000L, "Member");
		toGroupJSONStandardView(new UserName("whoo"), 34000L, 62000L, 25000L, 62000L, "Admin");
	}

	private void toGroupJSONStandardView(
			final UserName userName,
			final Long ownerVisit,
			final Long adminVisit,
			final Long memberVisit,
			final Long lastVisit,
			final String role)
			throws Exception {
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().build(),
				userName)
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(f -> true)
				.withPublicUserFieldDeterminer(f -> true)
				.withResourceType(new ResourceType("cat"))
				.withResource(new ResourceType("ws"), ResourceInformationSet
						.getBuilder(userName)
						.withResourceField(new ResourceID("a"), "f1", "x")
						.withResourceField(new ResourceID("a"), "rid", "wrong")
						.withResource(new ResourceID("b"))
						.build())
				.withStandardView(true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(MapBuilder.newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("role", role)
				.with("lastvisit", lastVisit)
				.with("name", "name2")
				.with("owner", MapBuilder.newHashMap()
						.with("name", "u2")
						.with("joined", 20000L)
						.with("lastvisit", ownerVisit)
						.with("custom", Collections.emptyMap())
						.build())
				.with("memcount", 5)
				.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("rescount", ImmutableMap.of("ws", 2, "cat", 1))
				.with("privatemembers", true)
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
								.with("lastvisit", memberVisit)
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
								.with("lastvisit", adminVisit)
								.with("custom", ImmutableMap.of("yay-6", "boo", "bar", "baz"))
								.build()
						))
				.with("resources", ImmutableMap.of(
						"ws", Arrays.asList(
								MapBuilder.newHashMap()
										.with("rid", "a")
										.with("added", 45000L)
										.with("f1", "x")
										.build(),
								MapBuilder.newHashMap()
										.with("rid", "b")
										.with("added", null)
										.build()),
						"cat", Collections.emptyList()))
				.build()));
	}
	
	@Test
	public void toGroupJSONStandardViewResourcesNonMember() throws Exception {
		final UserName userName = new UserName("non");
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().build(),
				userName)
				.withResource(new ResourceType("ws"), ResourceInformationSet
						.getBuilder(userName)
						.withResourceField(new ResourceID("a"), "f1", "x")
						.build())
				.withStandardView(true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(MapBuilder.newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("role", "None")
				.with("lastvisit", null)
				.with("name", "name2")
				.with("owner", MapBuilder.newHashMap()
						.with("name", "u2")
						.with("joined", null)
						.with("lastvisit", null)
						.with("custom", Collections.emptyMap())
						.build())
				.with("memcount", 5)
				.with("custom", Collections.emptyMap())
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("rescount", Collections.emptyMap())
				.with("privatemembers", true)
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
						"ws", Arrays.asList(
								MapBuilder.newHashMap()
										.with("rid", "a")
										.with("added", null)
										.with("f1", "x")
										.build())))
				.build()));
	}
	
	@Test
	public void toGroupJSONFail() throws Exception {
		try {
			APICommon.toGroupJSON(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("group"));
		}
	}
	
	@Test
	public void getToken() throws Exception {
		assertThat("incorrect token", APICommon.getToken(null, false), is(nullValue()));
		assertThat("incorrect token", APICommon.getToken("    \t   ", false), is(nullValue()));
		assertThat("incorrect token", APICommon.getToken("tok", false), is(new Token("tok")));
	}
	
	@Test
	public void getTokenRequired() throws Exception {
		assertThat("incorrect token", APICommon.getToken("tok", true), is(new Token("tok")));
	}
	
	@Test
	public void getTokenFail() throws Exception {
		failGetToken(null, new NoTokenProvidedException("No token provided"));
		failGetToken("   \t    ", new NoTokenProvidedException("No token provided"));
	}
	
	private void failGetToken(final String token, final Exception expected) {
		try {
			APICommon.getToken(token, true);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getRequestParamsNulls() throws Exception {
		final GetRequestsParams p = APICommon.getRequestsParams(null, null, null, true);
		
		assertThat("incorrect params", p, is(GetRequestsParams.getBuilder().build()));
		
		final GetRequestsParams p2 = APICommon.getRequestsParams(null, null, null, false);
		
		assertThat("incorrect params", p2, is(GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.build()));
	}
	
	@Test
	public void getRequestParamsWhitespace() throws Exception {
		final String ws = "    \t    ";
		final GetRequestsParams p = APICommon.getRequestsParams(ws, ws, ws, true);
		
		assertThat("incorrect params", p, is(GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.build()));
		
		final GetRequestsParams p2 = APICommon.getRequestsParams(ws, ws, ws, false);
		
		assertThat("incorrect params", p2, is(GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.withNullableIncludeClosed(true)
				.build()));
	}
	
	@Test
	public void getRequestParamsValues() throws Exception {
		final GetRequestsParams p = APICommon.getRequestsParams(
				"   \t   12000   ", " yes ", "  asc  ", false);
		
		assertThat("incorrect params", p, is(GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(12000))
				.withNullableIncludeClosed(true)
				.build()));
		
		final GetRequestsParams p2 = APICommon.getRequestsParams(
				"   \t   " + Long.MAX_VALUE + "   ", " no ", "  desc  ", true);
		
		assertThat("incorrect params", p2, is(GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(Long.MAX_VALUE))
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.build()));
		
		final GetRequestsParams p3 = APICommon.getRequestsParams(
				"   \t   " + Long.MIN_VALUE + "   ", null, null, true);
		
		assertThat("incorrect params", p3, is(GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(Long.MIN_VALUE))
				.build()));
	}
	
	@Test
	public void getRequestParamsFail() throws Exception {
		failGetRequestParams("   foo   ", null,
				new IllegalParameterException("Invalid epoch ms: foo"));
		failGetRequestParams(null, "asd", new IllegalParameterException(
				"Invalid sort direction: asd"));
	}
	
	private void failGetRequestParams(
			final String excludeUpTo,
			final String sortDirection,
			final Exception expected) {
		try {
			APICommon.getRequestsParams(excludeUpTo, null, sortDirection, true);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void epochMilliStringToInstant() throws Exception {
		assertThat("incorrect instant", APICommon.epochMilliStringToInstant("   \t  5161634  \n "),
				is(Instant.ofEpochMilli(5161634)));
	}
	
	@Test
	public void failEpochMilliStringToInstant() throws Exception {
		failEpochMilliStringToInstant(null, new NullPointerException("epochMilli"));
		failEpochMilliStringToInstant("   \t    ", new IllegalParameterException(
				"Invalid epoch ms: "));
		failEpochMilliStringToInstant("   foo   ", new IllegalParameterException(
				"Invalid epoch ms: foo"));
	}
	
	private void failEpochMilliStringToInstant(final String em, final Exception expected) {
		try {
			APICommon.epochMilliStringToInstant(em);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupParamsNulls() throws Exception {
		final GetGroupsParams p = APICommon.getGroupsParams(null, null, true);
		
		assertThat("incorrect params", p, is(GetGroupsParams.getBuilder().build()));
		
		final GetGroupsParams p2 = APICommon.getGroupsParams(null, null, false);
		
		assertThat("incorrect params", p2, is(GetGroupsParams.getBuilder()
				.withNullableSortAscending(false).build()));
	}
	
	@Test
	public void getGroupParamsWhitespace() throws Exception {
		final String ws = "    \t  ";
		final GetGroupsParams p = APICommon.getGroupsParams(ws, ws, true);
		
		assertThat("incorrect params", p, is(GetGroupsParams.getBuilder().build()));
		
		final GetGroupsParams p2 = APICommon.getGroupsParams(ws, ws, false);
		
		assertThat("incorrect params", p2, is(GetGroupsParams.getBuilder()
				.withNullableSortAscending(false).build()));
	}
	
	@Test
	public void getGroupParamsValues() throws Exception {
		final GetGroupsParams p = APICommon.getGroupsParams("   foo   ", "asc", false);
		
		assertThat("incorrect params", p, is(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("foo").build()));
		
		final GetGroupsParams p2 = APICommon.getGroupsParams("  \t  bar  ", "desc", true);
		
		assertThat("incorrect params", p2, is(GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("bar")
				.withNullableSortAscending(false).build()));
	}
	
	@Test
	public void getGroupParamsFail() throws Exception {
		try {
			APICommon.getGroupsParams(null, "asd", false);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalParameterException(
					"Invalid sort direction: asd"));
		}
	}
	
	@Test
	public void toGroupIDs() throws Exception {
		assertThat("incorrect group IDs", APICommon.toGroupIDs("   \t     "), is(set()));
		assertThat("incorrect group IDs", APICommon.toGroupIDs("   foo,  \t ,  bar   ,baz,    \t"),
				is(set(new GroupID("foo"), new GroupID("bar"), new GroupID("baz"))));
	}
	
	@Test
	public void failToGroupIDs() throws Exception {
		failToGroupIDs(null, new NullPointerException("commaSeparatedGroupIDs"));
		failToGroupIDs("  \t , foo, bad*name, bar", new IllegalParameterException(
				ErrorType.ILLEGAL_GROUP_ID, "Illegal character in group id  bad*name: *"));
	}
	
	private void failToGroupIDs(final String ids, final Exception expected) {
		try {
			APICommon.toGroupIDs(ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
