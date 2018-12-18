package us.kbase.test.groups.fieldvalidators;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorException;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;
import us.kbase.groups.fieldvalidators.GravatarFieldValidatorFactory;
import us.kbase.test.groups.TestCommon;

public class GravatarFieldValidatorFactoryTest {
	
	private static final String KNOWN_GOOD = "87194228ef49d635fec5938099042b1d";
	private static final int LEN = KNOWN_GOOD.length();
	
	@Test
	public void validate() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory()
				.getValidator(Collections.emptyMap());
		
		// if there's no error, the test passes
		v.validate(KNOWN_GOOD);
		// check that extra characters pass
		v.validate(KNOWN_GOOD + "Z");
	}
	
	@Test
	public void validateStrictLength() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory()
				.getValidator(ImmutableMap.of("strict-length", "true"));
		
		// if there's no error, the test passes
		v.validate(KNOWN_GOOD);
	}
	
	@Test
	public void validateFailBadHash() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory()
				.getValidator(ImmutableMap.of("strict-length", "nottrue"));
		
		validateFail(v, null, new NullPointerException("fieldValue"));
		validateFail(v, "  \t   ", new IllegalFieldValueException(
				"Gravatar hash less than 32 characters"));
		
		validateFail(v, "                                ", new IllegalFieldValueException(
				"Gravatar hash is not a valid MD5 string"));
		final StringBuilder b = new StringBuilder(KNOWN_GOOD);
		b.setCharAt(7, 'g');
		validateFail(v, b.toString(), new IllegalFieldValueException(
				"Gravatar hash is not a valid MD5 string"));
		
		// missing last char
		validateFail(v, KNOWN_GOOD.substring(0, LEN - 1), new IllegalFieldValueException(
				"Gravatar hash less than 32 characters"));
		// incorrect last char
		validateFail(v, KNOWN_GOOD.substring(0, LEN - 1) + "c", new IllegalFieldValueException(
				"Gravatar service does not recognize Gravatar hash " +
				KNOWN_GOOD.substring(0, LEN - 1) + "c"));
	}
	
	@Test
	public void validateFailStrictLength() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory()
				.getValidator(ImmutableMap.of("strict-length", "true"));
		
		validateFail(v, KNOWN_GOOD + "Z", new IllegalFieldValueException(
				"Gravatar hash must be exactly 32 characters"));
	}

	private void validateFail(
			final FieldValidator v,
			final String value,
			final Exception expected) throws Exception {
		
		try {
			v.validate(value);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void validateFailGravatarError() throws Exception {
		final Class<?> inner = GravatarFieldValidatorFactory.class.getDeclaredClasses()[0];
		final Constructor<?> con = inner.getDeclaredConstructor(boolean.class, int.class);
		con.setAccessible(true);
		final FieldValidator instance = (FieldValidator) con.newInstance(false, 405);
		
		try {
			instance.validate(KNOWN_GOOD.substring(0, LEN - 1) + "c");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new FieldValidatorException(
					"Error contacting Gravatar service: 404"));
		}
	}
	
	@Test
	public void getValidatorFail() throws Exception {
		try {
			new GravatarFieldValidatorFactory().getValidator(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("configuration"));
		}
	}

}
