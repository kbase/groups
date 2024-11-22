package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.fieldvalidation.CustomField;
import us.kbase.test.groups.TestCommon;

public class CustomFieldTest {

	@Test
	public void construct() throws Exception {
		final CustomField un = new CustomField("a8nba9");
		assertThat("incorrect field", un.getName(), is("a8nba9"));
		assertThat("incorrect toString", un.toString(), is("CustomField [name=a8nba9]"));
		assertThat("incorrect hashCode" , un.hashCode(), is(-1462848190));
		
		final UserName un2 = new UserName(TestCommon.LONG101.substring(0, 50));
		assertThat("incorrect username", un2.getName(), is(TestCommon.LONG101.substring(0, 50)));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("custom field"));
		failConstruct("   \t \n    ", new MissingParameterException("custom field"));
		failConstruct("9aabaea", new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER,
				"Custom field 9aabaea must start with a letter"));
		failConstruct("abaeataDfoo", new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER,
				"Illegal character in custom field abaeataDfoo: D"));
		failConstruct("abaeataΔfoo", new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER,
				"Illegal character in custom field abaeataΔfoo: Δ"));
		failConstruct("abaea*tafoo", new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER,
				"Illegal character in custom field abaea*tafoo: *"));
		failConstruct("abaea-tafoo", new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER,
				"Illegal character in custom field abaea-tafoo: -"));
		failConstruct(TestCommon.LONG101.substring(0, 51), new IllegalParameterException(
				ErrorType.ILLEGAL_PARAMETER,
				"custom field size greater than limit 50"));
	}

	private void failConstruct(
			final String field,
			final Exception exception) {
		try {
			new CustomField(field);
			fail("constructed bad custom field");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
}
