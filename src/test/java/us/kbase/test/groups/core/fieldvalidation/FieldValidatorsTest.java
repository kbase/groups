package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Optional;

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
		assertThat("incorrect fields", v.getValidationTargetFields(), is(set()));
		assertThat("incorrect user fields", v.getValidationTargetUserFields(), is(set()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidator v3 = mock(FieldValidator.class);
		final FieldValidator v4 = mock(FieldValidator.class);
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
				.withUserFieldValidator(new CustomField("foo"),
						FieldConfiguration.getBuilder()
								.withNullableIsUserSettable(true)
								.build(),
						v3)
				.withUserFieldValidator(new CustomField("baz"),
						FieldConfiguration.getBuilder().build(),
						v4)
				.build();
		
		assertThat("incorrect size", vs.getMaximumFieldValueSize(), is(7000));
		assertThat("incorrect fields", vs.getValidationTargetFields(), is(set(
				new CustomField("foo"), new CustomField("bar"))));
		assertThat("incorrect user fields", vs.getValidationTargetUserFields(), is(set(
				new CustomField("foo"), new CustomField("baz"))));
		assertThat("incorrect config", vs.getConfig(new CustomField("foo")),
				is(FieldConfiguration.getBuilder().build()));
		assertThat("incorrect config", vs.getConfig(new CustomField("bar")),
				is(FieldConfiguration.getBuilder().withNullableIsMinimalViewField(true).build()));
		assertThat("incorrect user config", vs.getUserFieldConfig(new CustomField("foo")),
				is(FieldConfiguration.getBuilder().withNullableIsUserSettable(true).build()));
		assertThat("incorrect user config", vs.getUserFieldConfig(new CustomField("baz")),
				is(FieldConfiguration.getBuilder().build()));
		
		assertThat("incorrect config", vs.getConfigOrEmpty(new CustomField("foo")),
				is(Optional.of(FieldConfiguration.getBuilder().build())));
		assertThat("incorrect config", vs.getConfigOrEmpty(new CustomField("bar")),
				is(Optional.of(FieldConfiguration.getBuilder()
						.withNullableIsMinimalViewField(true).build())));
		assertThat("incorrect user config", vs.getUserFieldConfigOrEmpty(new CustomField("foo")),
				is(Optional.of(FieldConfiguration.getBuilder()
						.withNullableIsUserSettable(true).build())));
		assertThat("incorrect user config", vs.getUserFieldConfigOrEmpty(new CustomField("baz")),
				is(Optional.of(FieldConfiguration.getBuilder().build())));
	}
	
	@Test
	public void immutable() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(7000)
				.withValidator(new CustomField("foo"), MTCFG, v1)
				.withUserFieldValidator(new CustomField("bar"), MTCFG, v1)
				.build();
		
		try {
			vs.getValidationTargetFields().add(new CustomField("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passes
		}
		
		try {
			vs.getValidationTargetUserFields().add(new CustomField("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passes
		}
	}
	
	@Test
	public void validate() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidator v3 = mock(FieldValidator.class);
		final FieldValidator v4 = mock(FieldValidator.class);
		final FieldValidator v5 = mock(FieldValidator.class);
		final FieldValidator v6 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(12)
				.withValidator(new CustomField("foo"), MTCFG, v1)
				.withValidator(
						new CustomField("bar"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v2)
				.withValidator(new CustomField("baz"), MTCFG, v5)
				.withUserFieldValidator(new CustomField("foo"), MTCFG, v6)
				.withUserFieldValidator(new CustomField("baz"), MTCFG, v3)
				.withUserFieldValidator(
						new CustomField("bat"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v4)
				.build();
		
		vs.validate(new NumberedCustomField("foo"), "my val");
		vs.validate(new NumberedCustomField("bar-23"), "my other 𐍆al");
		vs.validateUserField(new NumberedCustomField("baz"), "my val2");
		vs.validateUserField(new NumberedCustomField("bat-23"), "my 2ther 𐍆al");
		
		verify(v1).validate("my val");
		verify(v2).validate("my other 𐍆al");
		verify(v3).validate("my val2");
		verify(v4).validate("my 2ther 𐍆al");
		verifyZeroInteractions(v5);
		verifyZeroInteractions(v6);
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
		try {
			FieldValidators.getBuilder(1).withUserFieldValidator(f, cfg, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getConfigFail() throws Exception {
		final FieldValidators vs = FieldValidators.getBuilder(1)
				.withValidator(new CustomField("foo"), MTCFG, mock(FieldValidator.class))
				.withUserFieldValidator(new CustomField("foo"), MTCFG, mock(FieldValidator.class))
				.build();
		
		getConfigFail(vs, null, new NullPointerException("field"));
		getConfigFail(vs, new CustomField("foa"),
				new IllegalArgumentException("No such custom field: foa"));
	}
	
	private void getConfigFail(
			final FieldValidators v,
			final CustomField f,
			final Exception expected) {
		try {
			v.getConfig(f);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		try {
			v.getUserFieldConfig(f);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getConfigOrEmptyFail() throws Exception {
		try {
			FieldValidators.getBuilder(1).build().getConfigOrEmpty(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("field"));
		}
		try {
			FieldValidators.getBuilder(1).build().getUserFieldConfigOrEmpty(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("field"));
		}
	}

	@Test
	public void getConfigOrEmptyGetEmpty() throws Exception {
		final FieldValidators vs = FieldValidators.getBuilder(1)
				.withValidator(new CustomField("foo"), MTCFG, mock(FieldValidator.class))
				.withUserFieldValidator(new CustomField("foo"), MTCFG, mock(FieldValidator.class))
				.build();
		
		assertThat("incorrect config", vs.getConfigOrEmpty(new CustomField("foa")),
				is(Optional.empty()));
		assertThat("incorrect config", vs.getUserFieldConfigOrEmpty(new CustomField("foa")),
				is(Optional.empty()));
	}
	
	@Test
	public void validateFail() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(4)
				.withValidator(
						new CustomField("foo"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v1)
				.withValidator(new CustomField("bar"), MTCFG, v2)
				.build();
		
		validateFail(vs, null, "v", new NullPointerException("field"));
		validateFail(vs, new NumberedCustomField("bar-2"), "v", new IllegalParameterException(
				"Field bar-2 may not be a numbered field"));
		validateFail(vs, new NumberedCustomField("bat-2"), "v", new NoSuchCustomFieldException(
				"Field bat-2 is not a configured field"));
		validateFail(vs, new NumberedCustomField("bar"), null, new MissingParameterException(
				"Value for field bar"));
		validateFail(vs, new NumberedCustomField("bar"), "  \t  ", new MissingParameterException(
				"Value for field bar"));
		final String s = "𐍆234𐍆";
		assertThat("incorrect length", s.length(), is(7));
		assertThat("incorrect points", s.codePointCount(0, s.length()), is(5));
		validateFail(vs, new NumberedCustomField("bar"), s, new IllegalParameterException(
				"Value for field bar size greater than limit 4"));
		
		doThrow(new IllegalFieldValueException("oh poop")).when(v1).validate("arg");
		validateFail(vs, new NumberedCustomField("foo-45"), "arg", new IllegalParameterException(
				"Field foo-45 has an illegal value: oh poop"));
	}
	
	private void validateFail(
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
	
	// very similar to above. DRY later, maybe
	@Test
	public void validateUserFieldFail() throws Exception {
		final FieldValidator v1 = mock(FieldValidator.class);
		final FieldValidator v2 = mock(FieldValidator.class);
		final FieldValidators vs = FieldValidators.getBuilder(4)
				.withUserFieldValidator(
						new CustomField("foo"),
						FieldConfiguration.getBuilder().withNullableIsNumberedField(true).build(),
						v1)
				.withUserFieldValidator(new CustomField("bar"), MTCFG, v2)
				.build();
		
		validateUserFieldFail(vs, null, "v", new NullPointerException("field"));
		validateUserFieldFail(vs, new NumberedCustomField("bar-2"), "v",
				new IllegalParameterException("User field bar-2 may not be a numbered field"));
		validateUserFieldFail(vs, new NumberedCustomField("bat-2"), "v",
				new NoSuchCustomFieldException("User field bat-2 is not a configured field"));
		validateUserFieldFail(vs, new NumberedCustomField("bar"), null,
				new MissingParameterException("Value for user field bar"));
		validateUserFieldFail(vs, new NumberedCustomField("bar"), "  \t  ",
				new MissingParameterException("Value for user field bar"));
		final String s = "𐍆234𐍆";
		assertThat("incorrect length", s.length(), is(7));
		assertThat("incorrect points", s.codePointCount(0, s.length()), is(5));
		validateUserFieldFail(vs, new NumberedCustomField("bar"), s, new IllegalParameterException(
				"Value for user field bar size greater than limit 4"));
		
		doThrow(new IllegalFieldValueException("oh poop")).when(v1).validate("arg");
		validateUserFieldFail(vs, new NumberedCustomField("foo-45"), "arg",
				new IllegalParameterException("User field foo-45 has an illegal value: oh poop"));
	}
	
	private void validateUserFieldFail(
			final FieldValidators v,
			final NumberedCustomField f,
			final String val,
			final Exception expected) {
		try {
			v.validateUserField(f, val);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
