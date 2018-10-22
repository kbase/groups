package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.RequestID;
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
				.with("type", "Invite to group")
				.with("status", "Denied")
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
		
		assertThat("incorrect request", APICommon.toGroupRequestJSON(Arrays.asList(r2, r1)),
				is(Arrays.asList(
						MapBuilder.newHashMap()
								.with("id", id2.toString())
								.with("groupid", "gid2")
								.with("requester", "n2")
								.with("targetuser", "inv")
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
								.with("type", "Request group membership")
								.with("status", "Open")
								.with("createdate", 10000L)
								.with("moddate", 10000L)
								.with("expiredate", 20000L)
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
		assertThat("incorrect token", APICommon.getToken(" tok \t ", false), is(new Token("tok")));
	}
	
	@Test
	public void getTokenRequired() throws Exception {
		assertThat("incorrect token", APICommon.getToken(" tok \t ", true), is(new Token("tok")));
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
}
