package us.kbase.test.groups.fieldvalidators;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;
import us.kbase.groups.fieldvalidators.EnumFieldValidatorFactory;
import us.kbase.test.groups.TestCommon;

public class EnumFieldValidatorFactoryTest {
	
	@Test
	public void validate() throws Exception {
		final FieldValidator v = new EnumFieldValidatorFactory().getValidator(ImmutableMap.of(
				"allowed-values", "foo ,    bar   ,       , baz"));
		
		v.validate("foo");
		v.validate("bar");
		v.validate("baz");
		
		
		try {
			v.validate("bat");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalFieldValueException(
					"Value bat is not in the configured set of allowed values"));
		}
	}
	
	@Test
	public void buildFailMissingParam() throws Exception {
		final Map<String, String> c = new HashMap<>();
		failBuild(c, new IllegalParameterException(
				"allowed-values configuration setting is required"));
		c.put("allowed-values", null);
		failBuild(c, new IllegalParameterException(
				"allowed-values configuration setting is required"));
		c.put("allowed-values", "    \t     ");
		failBuild(c, new IllegalParameterException(
				"allowed-values configuration setting is required"));
	}
	
	@Test
	public void buildFailControlCharacters() throws Exception {
		failBuild(ImmutableMap.of("allowed-values", " foo\bbar"), new IllegalParameterException(
				"allowed-values contains control characters"));
	}
	
	@Test
	public void buildFailLong() throws Exception {
		new EnumFieldValidatorFactory().getValidator(ImmutableMap.of(
				"allowed-values", TestCommon.LONG101.substring(0, 50))); // should pass
		
		failBuild(ImmutableMap.of("allowed-values", TestCommon.LONG101.substring(0, 51)),
				new IllegalParameterException(
						"allowed-values contains value longer than maximum length 50"));
	}
	
	
	
	private void failBuild(final Map<String, String> config, final Exception expected) {
		try {
			new EnumFieldValidatorFactory().getValidator(config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
