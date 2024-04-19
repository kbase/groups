package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class GroupIDTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupID.class).usingGetClass().verify();
	}

	@Test
	public void construct() throws Exception {
		final GroupID g1 = new GroupID("    a8n-ba9    ");
		assertThat("incorrect username", g1.getName(), is("a8n-ba9"));
		assertThat("incorrect toString", g1.toString(), is("GroupID [name=a8n-ba9]"));
		
		final GroupID g2 = new GroupID(TestCommon.LONG101.substring(0, 100));
		assertThat("incorrect username", g2.getName(), is(TestCommon.LONG101.substring(0, 100)));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("group id"));
		failConstruct("   \t \n    ", new MissingParameterException("group id"));
		failConstruct("9aabaea", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Group ID must start with a letter"));
		failConstruct("abaeataDfoo", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Illegal character in group id abaeataDfoo: D"));
		failConstruct("abaeataΔfoo", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Illegal character in group id abaeataΔfoo: Δ"));
		failConstruct("abaea*tafoo", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Illegal character in group id abaea*tafoo: *"));
		failConstruct("abaea_tafoo", new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
				"Illegal character in group id abaea_tafoo: _"));
		failConstruct(TestCommon.LONG101, new IllegalParameterException(
				ErrorType.ILLEGAL_PARAMETER, "group id size greater than limit 100"));
	}

	private void failConstruct(
			final String name,
			final Exception exception) {
		try {
			new GroupID(name);
			fail("constructed bad name");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
}
