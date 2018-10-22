package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class TokenTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(Token.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() throws Exception {
		final Token t = new Token(" im a token  ");
		
		assertThat("incorrect token", t.getToken(), is(" im a token  "));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("token"));
		failConstruct("    \t     ", new MissingParameterException("token"));
	}
	
	private void failConstruct(final String token, final Exception expected) {
		try {
			new Token(token);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
