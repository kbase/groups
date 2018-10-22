package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
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
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
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
					.withInviteToGroup(new UserName("targ"))
					//TODO TEST add tests if denied state is ever visible
					.build();
			REQ_DENIED = GroupRequest.getBuilder(
					new RequestID(ID3), new GroupID("gid3"), new UserName("u3"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(12000), Instant.ofEpochMilli(22000))
							.withModificationTime(Instant.ofEpochMilli(27000))
							.build())
					.withInviteToGroup(new UserName("targ1"))
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
					.with("targetuser", null)
					.with("type", "Request group membership")
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
					.with("targetuser", "targ")
					.with("type", "Invite to group")
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
					.with("targetuser", "targ1")
					.with("type", "Invite to group")
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
	public void getCreatedRequests() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForRequester(new Token("t"))).thenReturn(Arrays.asList(
				REQ_DENIED, REQ_MIN, REQ_TARG));
		
		final List<Map<String, Object>> ret = new RequestAPI(g).getCreatedRequests("t");
		
		assertThat("incorrect reqs", ret, is(Arrays.asList(
				REQ_DENIED_JSON, REQ_MIN_JSON, REQ_TARG_JSON)));
	}
	
	@Test
	public void getCreatedRequestsMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetCreatedRequests(g, null,
				new NoTokenProvidedException("No token provided"));
		failGetCreatedRequests(g, "    \t    ",
				new NoTokenProvidedException("No token provided"));
	}

	@Test
	public void getCreatedRequestsFailInvalidToken() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForRequester(new Token("t")))
				.thenThrow(new InvalidTokenException());
		
		failGetCreatedRequests(g, "t", new InvalidTokenException());
	}
	
	private void failGetCreatedRequests(
			final Groups g,
			final String token,
			final Exception expected) {
		try {
			new RequestAPI(g).getCreatedRequests(token);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getTargetedRequests() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForTarget(new Token("t"))).thenReturn(Arrays.asList(
				REQ_MIN, REQ_DENIED, REQ_TARG));
		
		final List<Map<String, Object>> ret = new RequestAPI(g).getTargetedRequests("t");
		
		assertThat("incorrect reqs", ret, is(Arrays.asList(
				REQ_MIN_JSON, REQ_DENIED_JSON, REQ_TARG_JSON)));
	}
	
	@Test
	public void getTargetedRequestsMissingInput() throws Exception {
		final Groups g = mock(Groups.class);
		
		failGetTargetedRequests(g, null,
				new NoTokenProvidedException("No token provided"));
		failGetTargetedRequests(g, "    \t    ",
				new NoTokenProvidedException("No token provided"));
	}

	@Test
	public void getTargetedRequestsFailAuth() throws Exception {
		final Groups g = mock(Groups.class);
		
		when(g.getRequestsForTarget(new Token("t")))
				.thenThrow(new AuthenticationException(ErrorType.AUTHENTICATION_FAILED, "yikes"));
		
		failGetTargetedRequests(g, "t", new AuthenticationException(
				ErrorType.AUTHENTICATION_FAILED, "yikes"));
	}
	
	private void failGetTargetedRequests(
			final Groups g,
			final String token,
			final Exception expected) {
		try {
			new RequestAPI(g).getTargetedRequests(token);
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
						.withRequestGroupMembership()
						.withStatus(GroupRequestStatus.canceled())
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).cancelRequest("  tok  ", id.toString());
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("targetuser", null)
				.with("type", "Request group membership")
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
						.withInviteToGroup(new UserName("inv"))
						// normally the acceptor would be the same as the invited user,
						// but for testing purposes it's different.
						.withStatus(GroupRequestStatus.accepted(new UserName("inv2")))
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).acceptRequest("  tok  ", id.toString());
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("targetuser", "inv")
				.with("type", "Invite to group")
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
						.withRequestGroupMembership()
						// should not show up in output for now
						.withStatus(GroupRequestStatus.denied(new UserName("d"), "testreason"))
						.build());
		
		final Map<String, Object> ret = new RequestAPI(g).denyRequest(
				"  tok  ", id.toString(), body);
		
		assertThat("incorrect request", ret, is(MapBuilder.newHashMap()
				.with("id", id.toString())
				.with("groupid", "gid")
				.with("requester", "u")
				.with("targetuser", null)
				.with("type", "Request group membership")
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
}
