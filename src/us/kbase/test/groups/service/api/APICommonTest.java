package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.service.api.APICommon;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class APICommonTest {
	
	@Test
	public void toGroupRequestJSON() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.build())
				.withRequestGroupMembership()
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("targetuser", null)
				.with("targetws", null)
				.with("targetmeth", null)
				.with("type", "Request group membership")
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
				.withInviteToGroup(new UserName("inv"))
				.withStatus(GroupRequestStatus.denied(new UserName("den"), "r"))
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("targetuser", "inv")
				.with("targetws", null)
				.with("targetmeth", null)
				.with("type", "Invite to group")
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
				.withInviteWorkspace(new WorkspaceID(42))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("targetuser", null)
				.with("targetws", 42)
				.with("targetmeth", null)
				.with("type", "Invite workspace to group")
				.with("status", "Canceled")
				.with("createdate", 10000L)
				.with("moddate", 25000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void toGroupRequestJSONWithMethodTarget() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest r = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("n"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(25000))
						.build())
				.withInviteCatalogMethod(new CatalogMethod("mod.meth"))
				.withStatus(GroupRequestStatus.expired())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(r), is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "n")
				.with("targetuser", null)
				.with("targetws", null)
				.with("targetmeth", "mod.meth")
				.with("type", "Invite catalog method to group")
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
				.withRequestGroupMembership()
				.build();
		
		final UUID id2 = UUID.randomUUID();
		final GroupRequest r2 = GroupRequest.getBuilder(
				new RequestID(id2), new GroupID("gid2"), new UserName("n2"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(11000), Instant.ofEpochMilli(21000))
						.withModificationTime(Instant.ofEpochMilli(26000))
						.build())
				.withInviteToGroup(new UserName("inv"))
				.withStatus(GroupRequestStatus.denied(new UserName("den"), "r"))
				.build();
		
		final UUID id3 = UUID.randomUUID();
		final GroupRequest r3 = GroupRequest.getBuilder(
				new RequestID(id3), new GroupID("gid3"), new UserName("n3"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(12000), Instant.ofEpochMilli(22000))
						.withModificationTime(Instant.ofEpochMilli(27000))
						.build())
				.withRequestAddWorkspace(new WorkspaceID(42))
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(Arrays.asList(r2, r1, r3)),
				is(Arrays.asList(
						MapBuilder.newHashMap()
								.with("id", id2.toString())
								.with("groupid", "gid2")
								.with("requester", "n2")
								.with("targetuser", "inv")
								.with("targetws", null)
								.with("targetmeth", null)
								.with("type", "Invite to group")
								.with("status", "Denied")
								.with("createdate", 11000L)
								.with("moddate", 26000L)
								.with("expiredate", 21000L)
								.build(),
						MapBuilder.newHashMap()
								.with("id", id1.toString())
								.with("groupid", "gid1")
								.with("requester", "n1")
								.with("targetuser", null)
								.with("targetws", null)
								.with("targetmeth", null)
								.with("type", "Request group membership")
								.with("status", "Open")
								.with("createdate", 10000L)
								.with("moddate", 10000L)
								.with("expiredate", 20000L)
								.build(),
						MapBuilder.newHashMap()
								.with("id", id3.toString())
								.with("groupid", "gid3")
								.with("requester", "n3")
								.with("targetuser", null)
								.with("targetws", 42)
								.with("targetmeth", null)
								.with("type", "Request add workspace to group")
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
