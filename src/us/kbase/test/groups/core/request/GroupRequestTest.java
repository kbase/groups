package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequest.Builder;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.test.groups.TestCommon;

public class GroupRequestTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupRequest.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_GROUP_MEMBERSHIP));
	}
	
	@Test
	public void buildWithInviteUserModDateAndDenied() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(30000))
				.build())
				.withInviteToGroup(new UserName("targ"))
				.withStatus(GroupRequestStatus.denied(new UserName("immean"), "targ is junky"))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(),
				is(Optional.of(new UserName("immean"))));
		assertThat("incorrect closed reason", gr.getClosedReason(),
				is(Optional.of("targ is junky")));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(30000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect target", gr.getTarget(), is(Optional.of(new UserName("targ"))));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.INVITE_TO_GROUP));
	}
	
	@Test
	public void buildWithRequestWS() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withRequestAddWorkspace(new WorkspaceID(24))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(),
				is(Optional.of(new WorkspaceID(24))));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_ADD_WORKSPACE));
	}
	
	@Test
	public void buildWithInviteWS() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withInviteWorkspace(new WorkspaceID(24))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(),
				is(Optional.of(new WorkspaceID(24))));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.INVITE_WORKSPACE));
	}
	
	@Test
	public void buildWithResetToRequestAndCanceled() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withInviteToGroup(new UserName("targ"))
				.withRequestGroupMembership()
				.withStatus(GroupRequestStatus.canceled())
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.CANCELED));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_GROUP_MEMBERSHIP));
	}
	
	@Test
	public void buildWithTypeRequestUserAndAccepted() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withType(GroupRequestType.REQUEST_GROUP_MEMBERSHIP, new UserName("drop"), null)
				.withStatus(GroupRequestStatus.accepted(new UserName("imnice")))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(),
				is(Optional.of(new UserName("imnice"))));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.ACCEPTED));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_GROUP_MEMBERSHIP));
	}
	
	@Test
	public void buildWithTypeRequestNullUserAndExpired() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withType(GroupRequestType.REQUEST_GROUP_MEMBERSHIP, null, null)
				.withStatus(GroupRequestStatus.expired())
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.EXPIRED));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_GROUP_MEMBERSHIP));
	}
	
	@Test
	public void buildWithTypeInviteAndOpen() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withType(GroupRequestType.INVITE_TO_GROUP, new UserName("inv"), null)
				.withStatus(GroupRequestStatus.open())
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.of(new UserName("inv"))));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(), is(Optional.empty()));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.INVITE_TO_GROUP));
	}
	
	@Test
	public void buildWithTypeRequestWSAndNukeInviteUser() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withInviteToGroup(new UserName("inv"))
				.withType(GroupRequestType.REQUEST_ADD_WORKSPACE, null, new WorkspaceID(42))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(),
				is(Optional.of(new WorkspaceID(42))));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.REQUEST_ADD_WORKSPACE));
	}
	
	@Test
	public void buildWithTypeInviteWSAndNukeRequestUser() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withRequestGroupMembership()
				.withType(GroupRequestType.INVITE_WORKSPACE, null, new WorkspaceID(42))
				.build();
		
		assertThat("incorrect id", gr.getID(), is(new RequestID(id)));
		assertThat("incorrect closed by", gr.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect closed reason", gr.getClosedReason(), is(Optional.empty()));
		assertThat("incorrect create", gr.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", gr.getExpirationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect gid", gr.getGroupID(), is(new GroupID("gid")));
		assertThat("incorrect mod", gr.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect requester", gr.getRequester(), is(new UserName("foo")));
		assertThat("incorrect status", gr.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect target", gr.getTarget(), is(Optional.empty()));
		assertThat("incorrect ws target", gr.getWorkspaceTarget(),
				is(Optional.of(new WorkspaceID(42))));
		assertThat("incorrect type", gr.getType(), is(GroupRequestType.INVITE_WORKSPACE));
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final RequestID r = new RequestID(UUID.randomUUID());
		final GroupID g = new GroupID("gid");
		final UserName u = new UserName("u");
		final CreateModAndExpireTimes c = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build();
		
		failGetBuilder(null, g, u, c, new NullPointerException("id"));
		failGetBuilder(r, null, u, c, new NullPointerException("groupID"));
		failGetBuilder(r, g, null, c, new NullPointerException("requester"));
		failGetBuilder(r, g, u, null, new NullPointerException("times"));
		
	}
	
	private void failGetBuilder(
			final RequestID id,
			final GroupID gid,
			final UserName requester,
			final CreateModAndExpireTimes times,
			final Exception expected) {
		try {
			GroupRequest.getBuilder(id, gid, requester, times);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withInviteToGroupFail() throws Exception {
		final Builder b = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
		try {
			b.withInviteToGroup(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("target"));
		}
	}
	
	@Test
	public void withRequestWorkspaceFail() throws Exception {
		final Builder b = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
		
		try {
			b.withRequestAddWorkspace(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("wsid"));
		}
	}
	
	@Test
	public void withInviteWorkspaceFail() throws Exception {
		final Builder b = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
		
		try {
			b.withInviteWorkspace(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("wsid"));
		}
	}
	
	@Test
	public void withStatusFail() throws Exception {
		final Builder b = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
		try {
			b.withStatus(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("status"));
		}
	}
	
	@Test
	public void withTypeFail() throws Exception {
		failWithType(null, new UserName("u"), null, new NullPointerException("type"));
		failWithType(GroupRequestType.INVITE_TO_GROUP, null, null, new IllegalArgumentException(
				"Group invites must have a target user"));
		failWithType(GroupRequestType.REQUEST_ADD_WORKSPACE, null, null,
				new IllegalArgumentException(
						"Workspace requests and invites must have a target workspace"));
		failWithType(GroupRequestType.INVITE_WORKSPACE, null, null,
				new IllegalArgumentException(
						"Workspace requests and invites must have a target workspace"));
		
	}
	
	private void failWithType(
			final GroupRequestType type,
			final UserName user,
			final WorkspaceID wsid,
			final Exception expected)
			throws Exception {
		final Builder b = GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
		
		try {
			b.withType(type, user, wsid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}
}
