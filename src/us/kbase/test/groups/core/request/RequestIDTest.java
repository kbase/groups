package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.request.RequestID;
import us.kbase.test.groups.TestCommon;

public class RequestIDTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(RequestID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructString() throws Exception {
		final RequestID id = new RequestID("21389fc3-10f0-4d84-b187-87d327de3b48");
		assertThat("incorrect id", id.getID(), is("21389fc3-10f0-4d84-b187-87d327de3b48"));
	}
	
	@Test
	public void constructUUID() throws Exception {
		final RequestID id = new RequestID(
				UUID.fromString("21389fc3-10f0-4d84-b187-87d327de3b48"));
		assertThat("incorrect id", id.getID(), is("21389fc3-10f0-4d84-b187-87d327de3b48"));
	}
	
	@Test
	public void constructFail() {
		failConstruct((UUID) null, new NullPointerException("id"));
		failConstruct((String) null, new MissingParameterException("request id"));
		failConstruct("    \t    ", new MissingParameterException("request id"));
		failConstruct("21389fc3-10f04d84-b187-87d327de3b48", new IllegalParameterException(
				"21389fc3-10f04d84-b187-87d327de3b48 is not a valid request id"));
	}

	private void failConstruct(final String id, final Exception expected) {
		try {
			new RequestID(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConstruct(final UUID id, final Exception expected) {
		try {
			new RequestID(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
