package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static us.kbase.test.groups.TestCommon.set;

import org.junit.Test;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.fieldvalidation.CustomField;
import us.kbase.groups.core.fieldvalidation.FieldConfiguration;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidators;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.test.groups.TestCommon;

public class FieldValidatorsTest {
	
	private static final FieldConfiguration MTCFG = FieldConfiguration.getBuilder().build();
	
	@Test
	public void buildMinimal() throws Exception {
		final FieldValidators v = FieldValidators.getBuilder(7000).build();
		
		assertThat("incorrect size", v.getMaximumFieldValueSize(), is(7000));
		assertThat("incorrect size", v.getValidationTargetFields(), is(set()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(7000)
				.withValidator(
						new CustomField("foo"),
						FieldConfiguration.getBuilder().build(),
						v1)
				.withValidator(
						new CustomField("bar"),
						FieldConfiguration.getBuilder()
								.withNullableIsMinimalViewField(true)
								.build(),
						v2)
				.build();
		
		assertThat("incorrect size", vs.getMaximumFieldValueSize(), is(7000));
		assertThat("incorrect size", vs.getValidationTargetFields(), is(set(
				new CustomField("foo"), new CustomField("bar"))));
		assertThat("incorrect num", vs.getConfiguration(new CustomField("foo")),
				is(FieldConfiguration.getBuilder().build()));
		assertThat("incorrect num", vs.getConfiguration(new CustomField("bar")),
				is(FieldConfiguration.getBuilder().withNullableIsMinimalViewField(true).build()));
	}
	
	@Test
	public void immutable() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(7000)
				.withValidator(new CustomField("foo"), MTCFG, v1)
				.build();
		
		try {
			vs.getValidationTargetFields().add(new CustomField("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passes
		}
	}
	
	@Test
	public void validate() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(12)
				.withValidator(new CustomField("foo"), MTCFG, v1)
				.withValidator(
						new CustomField("bar"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v2)
				.build();
		
		vs.validate(new NumberedCustomField("foo"), "my val");
		vs.validate(new NumberedCustomField("bar-23"), "my other 𐍆al");
		
		verify(v1).validate("my val");
		verify(v2).validate("my other 𐍆al");
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		try {
			FieldValidators.getBuilder(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"maximumFieldValueSize must be > 0"));
		}
	}
	
	@Test
	public void withValidatorFail() throws Exception {
		final CustomField f = new CustomField("f");
		final FieldValidator v = mock(FieldValidator.class);
		failWithValidator(null, MTCFG, v, new NullPointerException("field"));
		failWithValidator(f, null, v, new NullPointerException("config"));
		failWithValidator(f, MTCFG, null, new NullPointerException("validator"));
	}
	
	private void failWithValidator(
			final CustomField f,
			final FieldConfiguration cfg,
			final FieldValidator v,
			final Exception expected) {
		try {
			FieldValidators.getBuilder(1).withValidator(f, cfg, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getConfigurationFail() throws Exception {
		final FieldValidators vs = FieldValidators.getBuilder(1)
				.withValidator(new CustomField("foo"), MTCFG, mock(FieldValidator.class))
				.build();
		
		getConfigurationFail(vs, null, new NullPointerException("field"));
		getConfigurationFail(vs, new CustomField("foa"),
				new IllegalArgumentException("No such custom field: foa"));
	}
	
	private void getConfigurationFail(
			final FieldValidators v,
			final CustomField f,
			final Exception expected) {
		try {
			v.getConfiguration(f);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void validationFail() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(4)
				.withValidator(
						new CustomField("foo"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v1)
				.withValidator(new CustomField("bar"), MTCFG, v2)
				.build();
		
		failValidation(vs, null, "v", new NullPointerException("field"));
		failValidation(vs, new NumberedCustomField("bar-2"), "v", new IllegalParameterException(
				"Field bar-2 may not be a numbered field"));
		failValidation(vs, new NumberedCustomField("bat-2"), "v", new NoSuchCustomFieldException(
				"Field bat-2 is not a configured field"));
		failValidation(vs, new NumberedCustomField("bar"), null, new MissingParameterException(
				"Value for field bar"));
		failValidation(vs, new NumberedCustomField("bar"), "  \t  ", new MissingParameterException(
				"Value for field bar"));
		final String s = "𐍆234𐍆";
		assertThat("incorrect length", s.length(), is(7));
		assertThat("incorrect points", s.codePointCount(0, s.length()), is(5));
		failValidation(vs, new NumberedCustomField("bar"), s, new IllegalParameterException(
				"Value for field bar size greater than limit 4"));
		
		doThrow(new IllegalFieldValueException("oh poop")).when(v1).validate("arg");
		failValidation(vs, new NumberedCustomField("foo-45"), "arg", new IllegalParameterException(
				"Field foo-45 has an illegal value: oh poop"));
		
	}
	
	private void failValidation(
			final FieldValidators v,
			final NumberedCustomField f,
			final String val,
			final Exception expected) {
		try {
			v.validate(f, val);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
