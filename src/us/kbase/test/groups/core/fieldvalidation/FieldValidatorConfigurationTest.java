package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.fieldvalidation.CustomField;
import us.kbase.groups.core.fieldvalidation.FieldValidatorConfiguration;
import us.kbase.test.groups.TestCommon;

public class FieldValidatorConfigurationTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(FieldValidatorConfiguration.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimal() throws Exception {
		final FieldValidatorConfiguration c = new FieldValidatorConfiguration(
				new CustomField("f"), "my class", false, Collections.emptyMap());
		
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(Collections.emptyMap()));
	}
	
	@Test
	public void constructMaximal() throws Exception {
		final FieldValidatorConfiguration c = new FieldValidatorConfiguration(
				new CustomField("f"), "my class", true, ImmutableMap.of(
						"foo", "bar", "baz", "bat"));
		
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(true));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(ImmutableMap.of(
				"foo", "bar", "baz", "bat")));
	}
	
	@Test
	public void constructFail() throws Exception {
		final CustomField f = new CustomField("f");
		final String vc = "vc";
		final Map<String, String> c = Collections.emptyMap();
		
		failConstruct(null, vc, c, new NullPointerException("field"));
		failConstruct(f, null, c, new IllegalArgumentException(
				"validatorClass cannot be null or whitespace only"));
		failConstruct(f, "   \t   ", c, new IllegalArgumentException(
				"validatorClass cannot be null or whitespace only"));
		failConstruct(f, vc, null, new NullPointerException("validatorConfiguration"));
		final Map<String, String> m = new HashMap<>();
		m.put(null, "s");
		failConstruct(f, vc, m, new IllegalArgumentException(
				"Validator configuration key cannot be null or whitespace only"));
		m.clear();
		m.put("    \t   ", "s");
		failConstruct(f, vc, m, new IllegalArgumentException(
				"Validator configuration key cannot be null or whitespace only"));
		m.clear();
		m.put("s", null);
		failConstruct(f, vc, m, new IllegalArgumentException(
				"Validator configuration value for key s cannot be null or whitespace only"));
		m.clear();
		m.put("s", "   \t   ");
		failConstruct(f, vc, m, new IllegalArgumentException(
				"Validator configuration value for key s cannot be null or whitespace only"));
	}
	
	private void failConstruct(
			final CustomField f,
			final String valclass,
			final Map<String, String> config,
			final Exception expected) {
		try {
			new FieldValidatorConfiguration(f, valclass, false, config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void immutable() throws Exception {
		final Map<String, String> m = new HashMap<>();
		m.put("foo", "bar");
		final FieldValidatorConfiguration c = new FieldValidatorConfiguration(
				new CustomField("f"), "my class", true, m);
		
		m.put("baz", "bat");
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(ImmutableMap.of(
				"foo", "bar")));
		
		try {
			c.getValidatorConfiguration().put("baz", "bat");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// passed
		}
		
	}
	
}
