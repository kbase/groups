package us.kbase.test.groups.fieldvalidators;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;
import us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class SimpleFieldValidatorFactoryTest {
	
	@Test
	public void validateSimple() throws Exception {
		final FieldValidator v = new SimpleFieldValidatorFactory()
				.getValidator(ImmutableMap.of(
						"allow-line-feeds-and-tabs", "false",
						"max-length", "    \t   "));
		
		v.validate("some string or other, there's really almost no way this can fail");
		
		for (final String c: Arrays.asList("\n", "\t", "\r", "\b", "\f")) {
			failValidate(v, "this " +  c + " can", "value contains control characters");
		}
	}
	
	@Test
	public void validateAllowControlChars() throws Exception {
		final FieldValidator v = new SimpleFieldValidatorFactory()
				.getValidator(MapBuilder.<String, String>newHashMap()
						.with("allow-line-feeds-and-tabs", "true")
						.with("max-length", null)
						.build());
		
		v.validate("control \t char");
		v.validate("control \n char");
		v.validate("control \r char");
		
		for (final String c: Arrays.asList("\b", "\f")) {
			failValidate(v, "this " +  c + " can", "value contains control characters");
		}
	}

	@Test
	public void validateMaxLength() throws Exception {
		final FieldValidator v = new SimpleFieldValidatorFactory()
				.getValidator(ImmutableMap.of("max-length", "6"));
		
		final String s = "𐍆whee𐍆";
		assertThat("incorrect size", s.length(), is(8));
		v.validate(s);
		
		failValidate(v, "𐍆wheeo𐍆", "value is greater than maximum length 6");
		
	}
	
	private void failValidate(final FieldValidator v, final String s, final String exception) {
		try {
			v.validate(s);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalFieldValueException(exception));
		}
	}
	
	@Test
	public void buildFailNull() throws Exception {
		failBuild(null, new NullPointerException("configuration"));
	}
	
	@Test
	public void buildFailBadLength() throws Exception {
		final Map<String, String> c = new HashMap<>();
		c.put("max-length", "foo");
		failBuild(c, new IllegalParameterException(
				"max-length parameter must be an integer > 0"));
		c.put("max-length", "0");
		failBuild(c, new IllegalParameterException(
				"max-length parameter must be an integer > 0"));
	}
	
	private void failBuild(final Map<String, String> config, final Exception expected) {
		try {
			new SimpleFieldValidatorFactory().getValidator(config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
