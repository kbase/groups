package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;

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
	public void buildMinimal() throws Exception {
		final FieldValidatorConfiguration c = FieldValidatorConfiguration.getBuilder(
				new CustomField("f"), "my class")
				.build();
		
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(Collections.emptyMap()));
	}
	
	@Test
	public void buildWithNulls() throws Exception {
		final FieldValidatorConfiguration c = FieldValidatorConfiguration.getBuilder(
				new CustomField("f"), "my class")
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withNullableIsMinimalViewField(null)
				.withNullableIsNumberedField(null)
				.withNullableIsPublicField(null)
				.build();
		
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(Collections.emptyMap()));
	}
	
	@Test
	public void buildWithFalse() throws Exception {
		final FieldValidatorConfiguration c = FieldValidatorConfiguration.getBuilder(
				new CustomField("f"), "my class")
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withNullableIsMinimalViewField(false)
				.withNullableIsNumberedField(false)
				.withNullableIsPublicField(false)
				.build();
		
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(Collections.emptyMap()));
	}
	
	
	@Test
	public void buildMaximal() throws Exception {
		final FieldValidatorConfiguration c = FieldValidatorConfiguration.getBuilder(
				new CustomField("f"), "my class")
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withConfigurationEntry("foo", "bar")
				.withConfigurationEntry("baz", "bat")
				.build();
				
				
		assertThat("incorrect field", c.getField(), is(new CustomField("f")));
		assertThat("incorrect class", c.getValidatorClass(), is("my class"));
		assertThat("incorrect num", c.isNumberedField(), is(true));
		assertThat("incorrect priv", c.isPublicField(), is(true));
		assertThat("incorrect list", c.isMinimalViewField(), is(true));
		assertThat("incorrect cfg", c.getValidatorConfiguration(), is(ImmutableMap.of(
				"foo", "bar", "baz", "bat")));
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final CustomField f = new CustomField("f");
		getBuilderFail(null, "c", new NullPointerException("field"));
		getBuilderFail(f, null, new IllegalArgumentException(
				"validatorClass cannot be null or whitespace only"));
		getBuilderFail(f, "   \t   ", new IllegalArgumentException(
				"validatorClass cannot be null or whitespace only"));
	}
	
	private void getBuilderFail(final CustomField f, final String c, final Exception expected) {
		try {
			FieldValidatorConfiguration.getBuilder(f, c);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withConfigurationEntryFail() throws Exception {
		withConfigurationEntryFail(null, "v", new IllegalArgumentException(
				"key cannot be null or whitespace only"));
		withConfigurationEntryFail("    \t    ", "v", new IllegalArgumentException(
				"key cannot be null or whitespace only"));
		withConfigurationEntryFail("k", null, new IllegalArgumentException(
				"value cannot be null or whitespace only"));
		withConfigurationEntryFail("k", "    \t    ", new IllegalArgumentException(
				"value cannot be null or whitespace only"));
	}
	
	private void withConfigurationEntryFail(
			final String k,
			final String v,
			final Exception expected) {
		try {
			FieldValidatorConfiguration.getBuilder(new CustomField("f"), "s")
					.withConfigurationEntry(k, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void immutable() throws Exception {
		final FieldValidatorConfiguration c = FieldValidatorConfiguration.getBuilder(
				new CustomField("f"), "my class")
				.withConfigurationEntry("foo", "bar")
				.build();
		
		try {
			c.getValidatorConfiguration().put("baz", "bat");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// passed
		}
		
	}
	
}
