package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.fieldvalidation.CustomField;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.test.groups.TestCommon;

public class NumberedCustomFieldTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(NumberedCustomField.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimal() throws Exception {
		final NumberedCustomField n = new NumberedCustomField("    foo  \t  ");
		
		assertThat("incorrect root", n.getFieldRoot(), is(new CustomField("foo")));
		assertThat("incorrect root", n.getNumber(), is(Optional.empty()));
		assertThat("incorrect root", n.getField(), is("foo"));
		assertThat("incorrect root", n.isNumberedField(), is(false));
	}
	
	@Test
	public void constructMaximal() throws Exception {
		final NumberedCustomField n = new NumberedCustomField("   \t  foo-23   ");
		
		assertThat("incorrect root", n.getFieldRoot(), is(new CustomField("foo")));
		assertThat("incorrect root", n.getNumber(), is(Optional.of(23)));
		assertThat("incorrect root", n.getField(), is("foo-23"));
		assertThat("incorrect root", n.isNumberedField(), is(true));
	}
	
	@Test
	public void constructFailNoValue() throws Exception {
		failConstruct(null, new MissingParameterException("customField"));
		failConstruct("   \t  ", new MissingParameterException("customField"));
	}
	
	@Test
	public void constructFailNotNumber() throws Exception {
		failConstruct("f-foo", new IllegalParameterException(
				"Suffix after - of field f-foo must be an integer > 0"));
		failConstruct("f-1-1", new IllegalParameterException(
				"Suffix after - of field f-1-1 must be an integer > 0"));
		failConstruct("f-0", new IllegalParameterException(
				"Suffix after - of field f-0 must be an integer > 0"));
		failConstruct("f--1", new IllegalParameterException(
				"Suffix after - of field f--1 must be an integer > 0"));
	}
	
	@Test
	public void constructFailBadRoot() throws Exception {
		// only test a couple conditions here, the CustomField class tests handle most of this
		// stuff
		failConstruct("f*o-1", new IllegalParameterException(
				"Illegal character in custom field f*o: *"));
		failConstruct("-1", new IllegalParameterException("Illegal custom field: -1"));
	}
	
	private void failConstruct(final String f, final Exception expected) {
		try {
			new NumberedCustomField(f);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void compare() throws Exception {
		compare("foo", "foo", 0);
		compare("fao", "fbo", -1);
		compare("fbo", "fao", 1);
		compare("foo-3", "foo-3", 0);
		compare("foo-1", "foo-3", -2);
		compare("foo-7", "foo-3", 4);
	}
	
	private void compare(final String one, final String two, final int expected)
			throws Exception {
		assertThat("incorrect compare",
				new NumberedCustomField(one).compareTo(new NumberedCustomField(two)),
				is(expected));
	}
}
