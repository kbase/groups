package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class UserNameTest {
	
	@Test
	public void construct() throws Exception {
		final UserName un = new UserName("a8n_ba9");
		assertThat("incorrect username", un.getName(), is("a8n_ba9"));
		assertThat("incorrect toString", un.toString(), is("UserName [name=a8n_ba9]"));
		assertThat("incorrect hashCode" , un.hashCode(), is(1896258321));
		
		final UserName un2 = new UserName(TestCommon.LONG101.substring(0, 100));
		assertThat("incorrect username", un2.getName(), is(TestCommon.LONG101.substring(0, 100)));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("user name"));
		failConstruct("   \t \n    ", new MissingParameterException("user name"));
		failConstruct("9aabaea", new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
				"Username must start with a letter"));
		failConstruct("abaeataDfoo", new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
				"Illegal character in user name abaeataDfoo: D"));
		failConstruct("abaeataΔfoo", new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
				"Illegal character in user name abaeataΔfoo: Δ"));
		failConstruct("abaea*tafoo", new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
				"Illegal character in user name abaea*tafoo: *"));
		failConstruct("abaea-tafoo", new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
				"Illegal character in user name abaea-tafoo: -"));
		failConstruct(TestCommon.LONG101, new IllegalParameterException(
				ErrorType.ILLEGAL_PARAMETER,
				"user name size greater than limit 100"));
	}

	private void failConstruct(
			final String name,
			final Exception exception) {
		try {
			new UserName(name);
			fail("constructed bad name");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
	@Test
	public void compareLessThan() throws Exception {
		assertThat("incorrect compare",
				new UserName("foo123").compareTo(new UserName("foo2")) < 0, is(true));
	}
	
	@Test
	public void compareEquals() throws Exception {
		assertThat("incorrect compare",
				new UserName("foo2").compareTo(new UserName("foo2")), is(0));
	}
	
	@Test
	public void compareGreaterThan() throws Exception {
		assertThat("incorrect compare",
				new UserName("foo13").compareTo(new UserName("foo111")) > 0, is(true));
	}
	
	@Test
	public void compareFail() throws Exception {
		try {
			new UserName("foo").compareTo(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("name"));
		}
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(UserName.class).usingGetClass().verify();
	}
	
}
