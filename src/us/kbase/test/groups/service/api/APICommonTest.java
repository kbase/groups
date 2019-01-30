package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;

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
				.withResource(new ResourceType("ws"), new ResourceDescriptor(new ResourceID("a")))
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
				"id", "id2", "private", true, "role", "none")));
	}
	
	@Test
	public void toGroupJSONMinimalView() throws Exception {
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().build(),
				new UserName("bar"))
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(f -> true)
				.withPublicUserFieldDeterminer(f -> true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(MapBuilder.newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("role", "member")
				.with("name", "name2")
				.with("owner", "u2")
				.with("memcount", 5)
				.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("rescount", ImmutableMap.of("ws", 2, "cat", 1))
				.build()));
	}
	
	@Test
	public void toGroupJSONStandardView() throws Exception {
		final GroupView gv = GroupView.getBuilder(
				getGroupMaxBuilder().build(),
				new UserName("bar"))
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(f -> true)
				.withPublicUserFieldDeterminer(f -> true)
				.withResourceType(new ResourceType("cat"))
				.withResource(new ResourceType("ws"), ResourceInformationSet
						.getBuilder(new UserName("bar"))
						.withResourceField(new ResourceID("a"), "f1", "x")
						.withResourceField(new ResourceID("a"), "rid", "wrong")
						.withResource(new ResourceID("b"))
						.build())
				.withStandardView(true)
				.build();
		
		assertThat("incorrect JSON", APICommon.toGroupJSON(gv), is(MapBuilder.newHashMap()
				.with("id", "id2")
				.with("private", false)
				.with("role", "member")
				.with("name", "name2")
				.with("owner", ImmutableMap.of(
						"name", "u2",
						"joined", 20000L,
						"custom", Collections.emptyMap()))
				.with("memcount", 5)
				.with("custom", ImmutableMap.of("field-1", "my val", "otherfield", "fieldval"))
				.with("createdate", 20000L)
				.with("moddate", 30000L)
				.with("rescount", ImmutableMap.of("ws", 2, "cat", 1))
				.with("privatemembers", true)
				.with("members", Arrays.asList(
						ImmutableMap.of(
								"name", "bar",
								"joined", 40000L,
								"custom", Collections.emptyMap()),
						ImmutableMap.of(
							"name", "foo",
							"joined", 650000L,
							"custom", ImmutableMap.of("whee", "whoo"))
						))
				.with("admins", Arrays.asList(
						ImmutableMap.of(
								"name", "whee",
								"joined", 220000L,
								"custom", Collections.emptyMap()),
						ImmutableMap.of(
							"name", "whoo",
							"joined", 760000L,
							"custom", ImmutableMap.of("yay-6", "boo", "bar", "baz"))
						))
				.with("resources", ImmutableMap.of(
						"ws", Arrays.asList(
								ImmutableMap.of("rid", "a", "f1", "x"),
								ImmutableMap.of("rid", "b")),
						"cat", Collections.emptyList()))
						
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
	
}
