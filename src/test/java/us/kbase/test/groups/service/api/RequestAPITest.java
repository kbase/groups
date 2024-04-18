package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.inst;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupHasRequests;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformation;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.service.api.RequestAPI;
import us.kbase.groups.service.api.RequestAPI.DenyRequestJSON;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class RequestAPITest {
	
	private static final UUID ID1 = UUID.randomUUID();
	private static final UUID ID2 = UUID.randomUUID();
	private static final UUID ID3 = UUID.randomUUID();

	private static final GroupRequest REQ_MIN;
	private static final GroupRequest REQ_TARG;
	private static final GroupRequest REQ_DENIED;
	static {
		try {
			REQ_MIN = GroupRequest.getBuilder(
					new RequestID(ID1), new GroupID("gid1"), new UserName("u1"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
							.build())
					.build();
			REQ_TARG = GroupRequest.getBuilder(
					new RequestID(ID2), new GroupID("gid2"), new UserName("u2"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(11000), Instant.ofEpochMilli(21000))
							.withModificationTime(Instant.ofEpochMilli(26000))
							.build())
					.withType(RequestType.INVITE)
					.withResource(GroupRequest.USER_TYPE,
							ResourceDescriptor.from(new UserName("targ")))
					//TODO TEST add tests if denied state is ever visible
					.build();
			REQ_DENIED = GroupRequest.getBuilder(
					new RequestID(ID3), new GroupID("gid3"), new UserName("u3"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(12000), Instant.ofEpochMilli(22000))
							.withModificationTime(Instant.ofEpochMilli(27000))
							.build())
					.withType(RequestType.INVITE)
					.withResource(GroupRequest.USER_TYPE,
							ResourceDescriptor.from(new UserName("targ1")))
					//TODO TEST add tests if denied state is ever visible
					.withStatus(GroupRequestStatus.denied(new UserName("d"), "reason"))
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix your tests newb", e);
		}
	}
	
	private static final Map<String, Object> REQ_MIN_JSON =
			MapBuilder.<String, Object>newHashMap()
					.with("id", ID1.toString())
					.with("groupid", "gid1")
					.with("requester", "u1")
					.with("resourcetype", "user")
					.with("resource", "u1")
					.with("type", "Request")
					.with("status", "Open")
					.with("createdate", 10000L)
					.with("moddate", 10000L)
					.with("expiredate", 20000L)
					.build();
	
	private static final Map<String, Object> REQ_TARG_JSON =
			MapBuilder.<String, Object>newHashMap()
					.with("id", ID2.toString())
					.with("groupid", "gid2")
					.with("requester", "u2")
					.with("resourcetype", "user")
					.with("resource", "targ")
					.with("type", "Invite")
					.with("status", "Open")
					.with("createdate", 11000L)
					.with("moddate", 26000L)
					.with("expiredate", 21000L)
					.build();
	
	private static final Map<String, Object> REQ_DENIED_JSON =
			MapBuilder.<String, Object>newHashMap()
					.with("id", ID3.toString())
					.with("groupid", "gid3")
					.with("requester", "u3")
					.with("resourcetype", "user")
					.with("resource", "targ1")
					.with("type", "Invite")
					.with("status", "Denied")
					.with("createdate", 12000L)
					.with("moddate", 27000L)
					.with("expiredate", 22000L)
					.build();
	
	@Test
	public void getRequestMinimal() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.getRequest(new Token("t"), new RequestID(id))).thenReturn(
				new GroupRequestWithActions(REQ_MIN, set(GroupRequestUserAction.CANCEL)));
		
		final Map<String, Object> ret = new RequestAPI(g).getRequest("t", id.toString());
		
		assertThat("incorrect request", ret, is(new MapBuilder<>(new HashMap<>(REQ_MIN_JSON))
				.with("actions", Arrays.asList("Cancel"))
				.build()));
	}
	
	@Test
	public void getRequestWithTarget() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.getRequest(new Token("t"), new RequestID(id))).thenReturn(
				new GroupRequestWithActions(REQ_TARG,
						set(GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY)));
		
		final Map<String, Object> ret = new RequestAPI(g).getRequest("t", id.toString());
		
		assertThat("incorrect request", ret, is(new MapBuilder<>(new HashMap<>(REQ_TARG_JSON))
				.with("actions", Arrays.asList("Accept", "Deny"))
				.build()));
	}
	
	@Test
	public void getRequestWithTargetDenied() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.getRequest(new Token("t"), new RequestID(id))).thenReturn(
				new GroupRequestWithActions(REQ_DENIED, set()));
		
		final Map<String, Object> ret = new RequestAPI(g).getRequest("t", id.toString());
		
		assertThat("incorrect request", ret, is(new MapBuilder<>(new HashMap<>(REQ_DENIED_JSON))
				.with("actions", Collections.emptyList())
				.build()));
	}
	
	@Test
	public void getRequestFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failGetRequest(g, null, id,
				new NoTokenProvidedException("No token provided"));
		failGetRequest(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		failGetRequest(g, "t", null,
				new MissingParameterException("request id"));
		failGetRequest(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		failGetRequest(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}

	@Test
	public void getRequestFailNoRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.getRequest(new Token("t"), new RequestID(id)))
				.thenThrow(new NoSuchRequestException(id.toString()));
		
		failGetRequest(g, "t", id.toString(), new NoSuchRequestException(id.toString()));
	}
	
	private void failGetRequest(
			final Groups g,
			final String token,
			final String requestid,
			final Exception expected) {
		try {
			new RequestAPI(g).getRequest(token, requestid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getGroupForRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.getGroupForRequest(new Token("t"), new RequestID(id)))
				.thenReturn(GroupView.getBuilder(
						Group.getBuilder(new GroupID("id"), new GroupName("n"),
								GroupUser.getBuilder(new UserName("u"), inst(10000)).build(),
								new CreateAndModTimes(inst(30000), inst(40000)))
								.withMember(GroupUser.getBuilder(new UserName("u2"), inst(10000))
										.build())
								.withCustomField(new NumberedCustomField("f"), "x")
								.build(),
						null)
						.withMinimalViewFieldDeterminer(f -> true)
						.withPublicFieldDeterminer(f -> true)
						.build());
		
		assertThat("incorrect group", new RequestAPI(g).getGroupForRequest("t", id.toString()),
				is(MapBuilder.newHashMap()
						.with("id", "id")
						.with("private", false)
						.with("role", "None")
						.with("name", "n")
						.with("owner", "u")
						.with("memcount", 2)
						.with("custom", ImmutableMap.of("f", "x"))
						.with("createdate", 30000L)
						.with("moddate", 40000L)
						.with("lastvisit", null)
						.with("rescount", Collections.emptyMap())
						.build()));
	}
	
	@Test
	public void getGroupForRequestFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failGetGroupForRequest(g, null, id,
				new NoTokenProvidedException("No token provided"));
		failGetGroupForRequest(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		failGetGroupForRequest(g, "t", null,
				new MissingParameterException("request id"));
		failGetGroupForRequest(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		failGetGroupForRequest(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}
	
	public void failGetGroupForRequest(
			final Groups g,
			final String token,
			final String requestID,
			final Exception expected) {
		try {
			new RequestAPI(g).getGroupForRequest(token, requestID);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getPerms() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		new RequestAPI(g).getPerms("t", id.toString());
		
		verify(g).setReadPermission(new Token("t"), new RequestID(id));
	}
	
	@Test
	public void getPermsFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failGetPerms(g, null, id,
				new NoTokenProvidedException("No token provided"));
		failGetPerms(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		failGetPerms(g, "t", null,
				new MissingParameterException("request id"));
		failGetPerms(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		failGetPerms(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}
	
	public void failGetPerms(
			final Groups g,
			final String token,
			final String requestID,
			final Exception expected) {
		try {
			new RequestAPI(g).getPerms(token, requestID);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getResourceInformation() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		when(g.getResourceInformation(new Token("token"), new RequestID(id)))
				.thenReturn(ResourceInformation.getBuilder(
						new ResourceType("t"), new ResourceID("i"))
						.withField("rid", 78) // expect overwrite
						.withField("resourcetype", 89) // expect overwrite
						.withField("f1", 90)
						.withField("f2", "foo")
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).getResourceInformation("token", id);
		
		assertThat("incorrect fields", ret, is(MapBuilder.newHashMap()
				.with("rid", "i")
				.with("resourcetype", "t")
				.with("f1", 90)
				.with("f2", "foo")
				.build()));
	}
	
	@Test
	public void getResourceInformationFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		getResourceInformationFail(g, null, id,
				new NoTokenProvidedException("No token provided"));
		getResourceInformationFail(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		getResourceInformationFail(g, "t", null,
				new MissingParameterException("request id"));
		getResourceInformationFail(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		getResourceInformationFail(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}
	
	public void getResourceInformationFail(
			final Groups g,
			final String token,
			final String requestID,
			final Exception expected) {
		try {
			new RequestAPI(g).getResourceInformation(token, requestID);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	// not really sure how to name these other than copy the params.
	@Test
	public void getCreatedRequests1() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.build();
		getCreatedRequests("   10000   ", "", "asc", params);
	}
	
	@Test
	public void getCreatedRequests2() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getCreatedRequests(null, null, "asc", params);
	}
	
	@Test
	public void getCreatedRequests3() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.build();
		getCreatedRequests(null, null, "desc", params);
	}
	
	@Test
	public void getCreatedRequests4() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getCreatedRequests(null, null, null, params);
	}
	
	@Test
	public void getCreatedRequests5() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false).build();
		getCreatedRequests(null, "", null, params);
	}
	
	@Test
	public void getCreatedRequests6() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.withResource(new ResourceType("t"), new ResourceID("r"))
				.build();
		getCreatedRequests(null, "", "desc", "t", "r", params);
	}
	
	private void getCreatedRequests(
			final String excludeUpTo,
			final String closed,
			final String order,
			final GetRequestsParams params)
			throws Exception {
		getCreatedRequests(excludeUpTo, closed, order, null, null, params);
	}
	
	private void getCreatedRequests(
			final String excludeUpTo,
			final String closed,
			final String order,
			final String resType,
			final String res,
			final GetRequestsParams params)
			throws Exception {	
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForRequester(new Token("t"), params))
				.thenReturn(Arrays.asList(REQ_DENIED, REQ_MIN, REQ_TARG));
		
		final List<Map<String, Object>> ret = new RequestAPI(g).getCreatedRequests(
				"t", excludeUpTo, closed, order, resType, res);
		
		assertThat("incorrect reqs", ret, is(Arrays.asList(
				REQ_DENIED_JSON, REQ_MIN_JSON, REQ_TARG_JSON)));
	}
	
	@Test
	public void getCreatedRequestsMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetCreatedRequests(g, null, null, null, null, null,
				new NoTokenProvidedException("No token provided"));
		failGetCreatedRequests(g, "    \t    ", null, null, null, null,
				new NoTokenProvidedException("No token provided"));
	}
	
	@Test
	public void getCreatedRequestsIllegalInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetCreatedRequests(g, "t", " yay ", null, null, null,
				new IllegalParameterException("Invalid epoch ms: yay"));
		failGetCreatedRequests(g, "t", null, "boo", null, null,
				new IllegalParameterException("Invalid sort direction: boo"));
		failGetCreatedRequests(g, "t", null, null, "t", null,
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
		failGetCreatedRequests(g, "t", null, null, null, "r",
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
	}

	@Test
	public void getCreatedRequestsFailInvalidToken() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForRequester(new Token("t"), GetRequestsParams.getBuilder().build()))
				.thenThrow(new InvalidTokenException());
		
		failGetCreatedRequests(g, "t", null, null, null, null, new InvalidTokenException());
	}
	
	private void failGetCreatedRequests(
			final Groups g,
			final String token,
			final String excludeUpTo,
			final String sortOrder,
			final String resType,
			final String res,
			final Exception expected) {
		try {
			new RequestAPI(g).getCreatedRequests(
					token, excludeUpTo, null, sortOrder, resType, res);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	// not really sure how to name these other than copy the params.
	@Test
	public void getTargetedRequests1() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.build();
		getTargetedRequests("   10000   ", "", "asc", params);
	}
	
	@Test
	public void getTargetedRequests2() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getTargetedRequests(null, null, "asc", params);
	}
	
	@Test
	public void getTargetedRequests3() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.build();
		getTargetedRequests(null, null, "desc", params);
	}
	
	@Test
	public void getTargetedRequests4() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getTargetedRequests(null, null, null, params);
	}
	
	@Test
	public void getTargetedRequests5() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false).build();
		getTargetedRequests(null, "", null, params);
	}
	
	@Test
	public void getTargetedRequests6() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.withResource(new ResourceType("t"), new ResourceID("r"))
				.build();
		getTargetedRequests(null, "", "desc", "t", "r", params);
	}

	private void getTargetedRequests(
			final String excludeUpTo,
			final String closed,
			final String order,
			final GetRequestsParams params)
			throws Exception {
		getTargetedRequests(excludeUpTo, closed, order, null, null, params);
	}
	
	private void getTargetedRequests(
			final String excludeUpTo,
			final String closed,
			final String order,
			final String resType,
			final String res,
			final GetRequestsParams params)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForTarget(new Token("t"), params))
				.thenReturn(Arrays.asList(REQ_MIN, REQ_DENIED, REQ_TARG));
		
		final List<Map<String, Object>> ret = new RequestAPI(g).getTargetedRequests(
				"t", excludeUpTo, closed, order, resType, res);
		
		assertThat("incorrect reqs", ret, is(Arrays.asList(
				REQ_MIN_JSON, REQ_DENIED_JSON, REQ_TARG_JSON)));
	}
	
	@Test
	public void getTargetedRequestsMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetTargetedRequests(g, null, null, null, null, null,
				new NoTokenProvidedException("No token provided"));
		failGetTargetedRequests(g, "    \t    ", null, null, null, null,
				new NoTokenProvidedException("No token provided"));
	}

	@Test
	public void getTargetedRequestsIllegalInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetTargetedRequests(g, "t", " whoo" , null, null, null,
				new IllegalParameterException("Invalid epoch ms: whoo"));
		failGetTargetedRequests(g, "t", null, "but mommy   ", null, null,
				new IllegalParameterException("Invalid sort direction: but mommy"));
		failGetTargetedRequests(g, "t", null, null, "t", null,
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
		failGetTargetedRequests(g, "t", null, null, null, "r",
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
	}
	
	@Test
	public void getTargetedRequestsFailAuth() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForTarget(new Token("t"), GetRequestsParams.getBuilder().build()))
				.thenThrow(new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "yikes"));
		
		failGetTargetedRequests(g, "t", null, null, null, null, new AuthenticationException(
				ErrorType.AUTHENTICATION_FAILED, "yikes"));
	}
	
	private void failGetTargetedRequests(
			final Groups g,
			final String token,
			final String excludeUpTo,
			final String order,
			final String resType,
			final String res,
			final Exception expected) {
		try {
			new RequestAPI(g).getTargetedRequests(token, excludeUpTo, null, order, resType, res);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	// not really sure how to name these other than copy the params.
	@Test
	public void getRequestsForAdministratedGroups1() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.build();
		getRequestsForAdministratedGroups("   10000   ", "", "asc", params);
	}
	
	@Test
	public void getRequestsForAdministratedGroups2() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getRequestsForAdministratedGroups(null, null, "asc", params);
	}
	
	@Test
	public void getRequestsForAdministratedGroups3() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableSortAscending(false)
				.build();
		getRequestsForAdministratedGroups(null, null, "desc", params);
	}
	
	@Test
	public void getRequestsForAdministratedGroups4() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder().build();
		getRequestsForAdministratedGroups(null, null, null, params);
	}
	
	@Test
	public void getRequestsForAdministratedGroups5() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false).build();
		getRequestsForAdministratedGroups(null, "", null, params);
	}
	
	@Test
	public void getRequestsForAdministratedGroups6() throws Exception {
		final GetRequestsParams params = GetRequestsParams.getBuilder()
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.withResource(new ResourceType("t"), new ResourceID("r"))
				.build();
		getRequestsForAdministratedGroups(null, "", "desc", "t", "r", params);
	}

	private void getRequestsForAdministratedGroups(
			final String excludeUpTo,
			final String closed,
			final String order,
			final GetRequestsParams params)
			throws Exception {
		getRequestsForAdministratedGroups(excludeUpTo, closed, order, null, null, params);
	}
	
	private void getRequestsForAdministratedGroups(
			final String excludeUpTo,
			final String closed,
			final String order,
			final String resType,
			final String res,
			final GetRequestsParams params)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForGroups(new Token("t"), params))
				.thenReturn(Arrays.asList(REQ_MIN, REQ_DENIED, REQ_TARG));
		
		final List<Map<String, Object>> ret = new RequestAPI(g).getRequestsForAdministratedGroups(
				"t", excludeUpTo, closed, order, resType, res);
		
		assertThat("incorrect reqs", ret, is(Arrays.asList(
				REQ_MIN_JSON, REQ_DENIED_JSON, REQ_TARG_JSON)));
	}
	
	@Test
	public void getRequestsForAdministratedGroupsMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForAdministratedGroups(g, null, null, null, null, null,
				new NoTokenProvidedException("No token provided"));
		failGetRequestsForAdministratedGroups(g, "    \t    ", null, null, null, null,
				new NoTokenProvidedException("No token provided"));
	}

	@Test
	public void getRequestsForAdministratedGroupsIllegalInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetRequestsForAdministratedGroups(g, "t", " whoo" , null, null, null,
				new IllegalParameterException("Invalid epoch ms: whoo"));
		failGetRequestsForAdministratedGroups(g, "t", null, "but mommy   ", null, null,
				new IllegalParameterException("Invalid sort direction: but mommy"));
		failGetRequestsForAdministratedGroups(g, "t", null, null, "t", null,
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
		failGetRequestsForAdministratedGroups(g, "t", null, null, null, "r",
				new IllegalParameterException("Either both or neither of the resource type " +
						"and resource ID must be provided"));
	}
	
	@Test
	public void getRequestsForAdministratedGroupsFailAuth() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForGroups(new Token("t"), GetRequestsParams.getBuilder().build()))
				.thenThrow(new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "yikes"));
		
		failGetRequestsForAdministratedGroups(g, "t", null, null, null, null,
				new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "yikes"));
	}
	
	private void failGetRequestsForAdministratedGroups(
			final Groups g,
			final String token,
			final String excludeUpTo,
			final String order,
			final String resType,
			final String res,
			final Exception expected) {
		try {
			new RequestAPI(g).getRequestsForAdministratedGroups(
					token, excludeUpTo, null, order, resType, res);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void cancelRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.cancelRequest(new Token("tok"), new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(new RequestID(id), new GroupID("gid"), new UserName("u"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(28000))
								.build())
						.withType(RequestType.REQUEST)
						.withResource(GroupRequest.USER_TYPE,
								ResourceDescriptor.from(new UserName("u")))
						.withStatus(GroupRequestStatus.canceled())
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).cancelRequest("tok", id.toString());
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("resourcetype", "user")
				.with("resource", "u")
				.with("type", "Request")
				.with("status", "Canceled")
				.with("createdate", 10000L)
				.with("moddate", 28000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void cancelRequestFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failCancelRequest(g, null, id,
				new NoTokenProvidedException("No token provided"));
		failCancelRequest(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		failCancelRequest(g, "t", null,
				new MissingParameterException("request id"));
		failCancelRequest(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		failCancelRequest(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}

	@Test
	public void cancelRequestFailUnauthed() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.cancelRequest(new Token("t"), new RequestID(id)))
				.thenThrow(new UnauthorizedException("naughty naughty"));
		
		failCancelRequest(g, "t", id.toString(), new UnauthorizedException("naughty naughty"));
	}
	
	private void failCancelRequest(
			final Groups g,
			final String token,
			final String requestid,
			final Exception expected) {
		try {
			new RequestAPI(g).cancelRequest(token, requestid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void acceptRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.acceptRequest(new Token("tok"), new RequestID(id))).thenReturn(
				GroupRequest.getBuilder(new RequestID(id), new GroupID("gid"), new UserName("u"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(28000))
								.build())
						.withType(RequestType.INVITE)
						.withResource(GroupRequest.USER_TYPE,
								ResourceDescriptor.from(new UserName("inv")))
						// normally the acceptor would be the same as the invited user,
						// but for testing purposes it's different.
						.withStatus(GroupRequestStatus.accepted(new UserName("inv2")))
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).acceptRequest("tok", id.toString());
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("resourcetype", "user")
				.with("resource", "inv")
				.with("type", "Invite")
				.with("status", "Accepted")
				.with("createdate", 10000L)
				.with("moddate", 28000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void acceptRequestFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failAcceptRequest(g, null, id,
				new NoTokenProvidedException("No token provided"));
		failAcceptRequest(g, "    \t    ", id,
				new NoTokenProvidedException("No token provided"));
		failAcceptRequest(g, "t", null,
				new MissingParameterException("request id"));
		failAcceptRequest(g, "t", "   \t   ",
				new MissingParameterException("request id"));
		failAcceptRequest(g, "t", "foo",
				new IllegalParameterException("foo is not a valid request id"));
	}

	@Test
	public void acceptRequestFailNoRequest() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.acceptRequest(new Token("t"), new RequestID(id)))
				.thenThrow(new NoSuchRequestException(id.toString()));
		
		failAcceptRequest(g, "t", id.toString(), new NoSuchRequestException(id.toString()));
	}
	
	private void failAcceptRequest(
			final Groups g,
			final String token,
			final String requestid,
			final Exception expected) {
		try {
			new RequestAPI(g).acceptRequest(token, requestid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void denyRequestWithNoBody() throws Exception {
		denyRequest(null, null);
	}
	
	@Test
	public void denyRequestWithNullReason() throws Exception {
		denyRequest(null, new DenyRequestJSON(null));
	}
	
	@Test
	public void denyRequestWithWhitespaceReason() throws Exception {
		denyRequest("   \t   ", new DenyRequestJSON("   \t   "));
	}
	
	@Test
	public void denyRequestWithReason() throws Exception {
		denyRequest("   reason    ", new DenyRequestJSON("   reason    "));
	}

	private void denyRequest(final String expectedReason, final DenyRequestJSON body)
			throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.denyRequest(new Token("tok"), new RequestID(id), expectedReason)).thenReturn(
				GroupRequest.getBuilder(new RequestID(id), new GroupID("gid"), new UserName("u"),
						CreateModAndExpireTimes.getBuilder(
								Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
								.withModificationTime(Instant.ofEpochMilli(28000))
								.build())
						.withType(RequestType.REQUEST)
						.withResource(GroupRequest.USER_TYPE,
								ResourceDescriptor.from(new UserName("u")))
						// should not show up in output for now
						.withStatus(GroupRequestStatus.denied(new UserName("d"), "testreason"))
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).denyRequest(
				"tok", id.toString(), body);
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("resourcetype", "user")
				.with("resource", "u")
				.with("type", "Request")
				.with("status", "Denied")
				.with("createdate", 10000L)
				.with("moddate", 28000L)
				.with("expiredate", 20000L)
				.build()));
	}
	
	@Test
	public void denyRequestFailMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		final String id = UUID.randomUUID().toString();
		
		failDenyRequest(g, null, id, null,
				new NoTokenProvidedException("No token provided"));
		failDenyRequest(g, "    \t    ", id, null,
				new NoTokenProvidedException("No token provided"));
		failDenyRequest(g, "t", null, null,
				new MissingParameterException("request id"));
		failDenyRequest(g, "t", "   \t   ", null,
				new MissingParameterException("request id"));
		failDenyRequest(g, "t", "foo", null,
				new IllegalParameterException("foo is not a valid request id"));
	}

	@Test
	public void denyRequestFailExtraParams() throws Exception {
		final Groups g = mock(Groups.class);
		
		final DenyRequestJSON j = new DenyRequestJSON(null);
		j.setAdditionalProperties("prop", "foo");
		
		failDenyRequest(g, "t", "i", j, new IllegalParameterException(
				"Unexpected parameters in request: prop"));
	}
	
	@Test
	public void denyRequestFailAuthException() throws Exception {
		final Groups g = mock(Groups.class);
		
		final UUID id = UUID.randomUUID();
		
		when(g.denyRequest(new Token("t"), new RequestID(id), null))
				.thenThrow(new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "boo"));
		
		failDenyRequest(g, "t", id.toString(), null,
				new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "boo"));
	}
	
	private void failDenyRequest(
			final Groups g,
			final String token,
			final String requestid,
			final DenyRequestJSON body,
			final Exception expected) {
		try {
			new RequestAPI(g).denyRequest(token, requestid, body);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void groupsHaveRequests() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.groupsHaveRequests(
				new Token("tokyn"),
				Arrays.asList(new GroupID("id1"), new GroupID("id2"), new GroupID("id3"))))
				.thenReturn(ImmutableMap.of(
						new GroupID("id1"), GroupHasRequests.NONE,
						new GroupID("id2"), GroupHasRequests.OLD,
						new GroupID("id3"), GroupHasRequests.NEW));
		
		assertThat("incorrect has requests", new RequestAPI(g).groupsHaveRequests(
				"tokyn", "  id1  , , id2, id3  \t"),
				is(ImmutableMap.of(
						"id1", ImmutableMap.of("new", "None"),
						"id2", ImmutableMap.of("new", "Old"),
						"id3", ImmutableMap.of("new", "New")
						)));
	}
	
	@Test
	public void failGroupsHaveRequestBadArds() throws Exception {
		failGroupHaveRequests(null, "i", new NoTokenProvidedException("No token provided"));
		failGroupHaveRequests("   \t   ", "i", new NoTokenProvidedException(
				"No token provided"));
		failGroupHaveRequests("t", "b*d", new IllegalParameterException(
				ErrorType.ILLEGAL_GROUP_ID, "Illegal character in group id b*d: *"));
	}
	
	private void failGroupHaveRequests(
			final String t,
			final String i,
			final Exception expected) {
		try {
			new RequestAPI(mock(Groups.class)).groupsHaveRequests(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}
}
