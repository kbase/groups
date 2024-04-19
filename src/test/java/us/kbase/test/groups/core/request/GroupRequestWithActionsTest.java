package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;
import static us.kbase.groups.core.request.GroupRequestUserAction.ACCEPT;
import static us.kbase.groups.core.request.GroupRequestUserAction.CANCEL;
import static us.kbase.groups.core.request.GroupRequestUserAction.DENY;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.test.groups.TestCommon;

public class GroupRequestWithActionsTest {

	private static final UUID ID = UUID.randomUUID();
	private static final GroupRequest REQ;
	private static final GroupRequest EXPECTED;
	static {
		try {
			REQ = GroupRequest.getBuilder(
					new RequestID(ID), new GroupID("id"), new UserName("n"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
							.build())
					.build();
			EXPECTED = GroupRequest.getBuilder(
					new RequestID(ID), new GroupID("id"), new UserName("n"),
					CreateModAndExpireTimes.getBuilder(
							Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
							.build())
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix yer tests newb");
		}
	}

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupRequestWithActions.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() throws Exception {
		final GroupRequestWithActions a = new GroupRequestWithActions(REQ, set());
		
		assertThat("incorrect request", a.getRequest(), is(EXPECTED));
		assertThat("incorrect actions", a.getActions(), is(set()));
	}
	
	@Test
	public void constructCancel() throws Exception {
		final GroupRequestWithActions a = new GroupRequestWithActions(REQ, set(CANCEL));
		
		assertThat("incorrect request", a.getRequest(), is(EXPECTED));
		assertThat("incorrect actions", a.getActions(), is(set(CANCEL)));
	}
	
	@Test
	public void constructAcceptDeny() throws Exception {
		final GroupRequestWithActions a = new GroupRequestWithActions(REQ, set(ACCEPT, DENY));
		
		assertThat("incorrect request", a.getRequest(), is(EXPECTED));
		assertThat("incorrect actions", a.getActions(), is(set(ACCEPT, DENY)));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, set(), new NullPointerException("request"));
		failConstruct(REQ, null, new NullPointerException("actions"));
		failConstruct(REQ, set(ACCEPT, null), new NullPointerException(
				"Null item in collection actions"));
	}
	
	private void failConstruct(
			final GroupRequest r,
			final Set<GroupRequestUserAction> actions,
			final Exception expected) {
		try {
			new GroupRequestWithActions(r, actions);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}

