package us.kbase.test.groups.service.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.Instant;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.service.exceptions.ErrorMessage;
import us.kbase.test.groups.TestCommon;

public class ErrorMessageTest {

	@Test
	public void equals() {
		EqualsVerifier.forClass(ErrorMessage.class).usingGetClass().verify();
	}
	
	@Test
	public void constructWithAssyHomolExceptionAndCallID() {
		final ErrorMessage em = new ErrorMessage(
				new IllegalParameterException("some message"),
				"some id",
				Instant.ofEpochMilli(10000));
		
		assertThat("incorrect app code", em.getAppcode(), is(30001));
		assertThat("incorrect app err", em.getApperror(), is("Illegal input parameter"));
		assertThat("incorrect call id", em.getCallid(), is("some id"));
		assertThat("incorrect http code", em.getHttpcode(), is(400));
		assertThat("incorrect http status", em.getHttpstatus(), is("Bad Request"));
		assertThat("incorrect message", em.getMessage(),
				is("30001 Illegal input parameter: some message"));
		assertThat("incorrect time", em.getTime(), is(10000L));
	}
	
	@Test
	public void constructWithAuthenticationExceptionNoCallID() {
		final ErrorMessage em = new ErrorMessage(
				new NoTokenProvidedException("token"),
				null,
				Instant.ofEpochMilli(20000));
		
		assertThat("incorrect app code", em.getAppcode(), is(10010));
		assertThat("incorrect app err", em.getApperror(), is("No authentication token"));
		assertThat("incorrect call id", em.getCallid(), is((String) null));
		assertThat("incorrect http code", em.getHttpcode(), is(401));
		assertThat("incorrect http status", em.getHttpstatus(), is("Unauthorized"));
		assertThat("incorrect message", em.getMessage(),
				is("10010 No authentication token: token"));
		assertThat("incorrect time", em.getTime(), is(20000L));
	}
	
	@Test
	public void constructWithUnauthorizedExceptionNoCallID() {
		final ErrorMessage em = new ErrorMessage(
				new UnauthorizedException("naughty person"),
				null,
				Instant.ofEpochMilli(20000));
		
		assertThat("incorrect app code", em.getAppcode(), is(20000));
		assertThat("incorrect app err", em.getApperror(), is("Unauthorized"));
		assertThat("incorrect call id", em.getCallid(), is((String) null));
		assertThat("incorrect http code", em.getHttpcode(), is(403));
		assertThat("incorrect http status", em.getHttpstatus(), is("Forbidden"));
		assertThat("incorrect message", em.getMessage(),
				is("20000 Unauthorized: naughty person"));
		assertThat("incorrect time", em.getTime(), is(20000L));
	}
	
	@Test
	public void constructWithNoDataExceptionNoCallID() {
		final ErrorMessage em = new ErrorMessage(
				new NoSuchGroupException("mygroup"),
				null,
				Instant.ofEpochMilli(20000));
		
		assertThat("incorrect app code", em.getAppcode(), is(50000));
		assertThat("incorrect app err", em.getApperror(), is("No such group"));
		assertThat("incorrect call id", em.getCallid(), is((String) null));
		assertThat("incorrect http code", em.getHttpcode(), is(404));
		assertThat("incorrect http status", em.getHttpstatus(), is("Not Found"));
		assertThat("incorrect message", em.getMessage(),
				is("50000 No such group: mygroup"));
		assertThat("incorrect time", em.getTime(), is(20000L));
	}
	
	@Test
	public void constructWithWebApplicationExceptionWithCallID() {
		final ErrorMessage em = new ErrorMessage(
				new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).build()),
				"foobar",
				Instant.ofEpochMilli(30000));
		
		assertThat("incorrect app code", em.getAppcode(), is((Integer) null));
		assertThat("incorrect app err", em.getApperror(), is((String) null));
		assertThat("incorrect call id", em.getCallid(), is("foobar"));
		assertThat("incorrect http code", em.getHttpcode(), is(406));
		assertThat("incorrect http status", em.getHttpstatus(), is("Not Acceptable"));
		assertThat("incorrect message", em.getMessage(), is("HTTP 406 Not Acceptable"));
		assertThat("incorrect time", em.getTime(), is(30000L));
	}
	
	@Test
	public void constructWithJsonMappingExceptionNoCallID() {
		final ErrorMessage em = new ErrorMessage(
				new JsonMappingException("crappy JSON here"),
				null,
				Instant.ofEpochMilli(30000));
		
		assertThat("incorrect app code", em.getAppcode(), is((Integer) null));
		assertThat("incorrect app err", em.getApperror(), is((String) null));
		assertThat("incorrect call id", em.getCallid(), is((String) null));
		assertThat("incorrect http code", em.getHttpcode(), is(400));
		assertThat("incorrect http status", em.getHttpstatus(), is("Bad Request"));
		assertThat("incorrect message", em.getMessage(), is("crappy JSON here"));
		assertThat("incorrect time", em.getTime(), is(30000L));
	}
	
	@Test
	public void constructWithOtherExceptionWithCallID() {
		final ErrorMessage em = new ErrorMessage(
				new IOException("FNF"),
				"",
				Instant.ofEpochMilli(40000));
		
		assertThat("incorrect app code", em.getAppcode(), is((Integer) null));
		assertThat("incorrect app err", em.getApperror(), is((String) null));
		assertThat("incorrect call id", em.getCallid(), is(""));
		assertThat("incorrect http code", em.getHttpcode(), is(500));
		assertThat("incorrect http status", em.getHttpstatus(), is("Internal Server Error"));
		assertThat("incorrect message", em.getMessage(), is("FNF"));
		assertThat("incorrect time", em.getTime(), is(40000L));
	}
	
	@Test
	public void constructFail() {
		failConstruct(null, Instant.ofEpochMilli(100000), new NullPointerException("ex"));
		failConstruct(new IOException(), null, new NullPointerException("time"));
	}
	
	private void failConstruct(final Throwable ex, final Instant time, final Exception expected) {
		try {
			new ErrorMessage(ex, null, time);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
