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
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
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
		assertThat("incorrect open", gr.isOpen(), is(true));
		assertThat("incorrect type", gr.getType(), is(RequestType.REQUEST));
		assertThat("incorrect res type", gr.getResourceType(), is(new ResourceType("user")));
		assertThat("incorrect resource", gr.getResource(),
				is(ResourceDescriptor.from(new UserName("foo"))));
		assertThat("incorrect is invite", gr.isInvite(), is(false));
	}
	
	@Test
	public void buildWithInviteModDateAndDenied() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
						.withModificationTime(Instant.ofEpochMilli(30000))
				.build())
				.withType(RequestType.INVITE)
				.withResourceType(new ResourceType("catalogmethod"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("module"),
						new ResourceID("module.method")))
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
		assertThat("incorrect open", gr.isOpen(), is(false));
		assertThat("incorrect type", gr.getType(), is(RequestType.INVITE));
		assertThat("incorrect res type", gr.getResourceType(),
				is(new ResourceType("catalogmethod")));
		assertThat("incorrect resource", gr.getResource(),
				is(new ResourceDescriptor(new ResourceAdministrativeID("module"),
						new ResourceID("module.method"))));
		assertThat("incorrect is invite", gr.isInvite(), is(true));
	}
	
	@Test
	public void buildWithCanceled() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withResourceType(new ResourceType("githubrepo"))
				.withResource(new ResourceDescriptor(new ResourceAdministrativeID("kbase"),
						new ResourceID("kbase/groups")))
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
		assertThat("incorrect open", gr.isOpen(), is(false));
		assertThat("incorrect type", gr.getType(), is(RequestType.REQUEST));
		assertThat("incorrect res type", gr.getResourceType(), is(new ResourceType("githubrepo")));
		assertThat("incorrect resource", gr.getResource(),
				is(new ResourceDescriptor(new ResourceAdministrativeID("kbase"),
						new ResourceID("kbase/groups"))));
		assertThat("incorrect is invite", gr.isInvite(), is(false));
	}
	
	@Test
	public void buildAccepted() throws Exception {
		final UUID id = UUID.randomUUID();
		final GroupRequest gr = GroupRequest.getBuilder(
				new RequestID(id), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build())
				.withType(RequestType.INVITE)
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
		assertThat("incorrect open", gr.isOpen(), is(false));
		assertThat("incorrect type", gr.getType(), is(RequestType.INVITE));
		assertThat("incorrect res type", gr.getResourceType(), is(new ResourceType("user")));
		assertThat("incorrect resource", gr.getResource(),
				is(ResourceDescriptor.from(new UserName("foo"))));
		assertThat("incorrect is invite", gr.isInvite(), is(true));
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
	
	private Builder getBuilder() throws Exception {
		return GroupRequest.getBuilder(
				new RequestID(UUID.randomUUID()), new GroupID("gid"), new UserName("foo"),
				CreateModAndExpireTimes.getBuilder(
						Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)).build());
	}
	
	@Test
	public void withTypeFail() throws Exception {
		try {
			getBuilder().withType(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("type"));
		}
	}
	
	@Test
	public void withResourceTypeFail() throws Exception {
		try {
			getBuilder().withResourceType(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("resourceType"));
		}
	}
	
	@Test
	public void withResourceFail() throws Exception {
		try {
			getBuilder().withResource(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("resource"));
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
}
