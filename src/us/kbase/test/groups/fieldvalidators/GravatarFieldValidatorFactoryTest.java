package us.kbase.test.groups.fieldvalidators;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;

import org.junit.Test;

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
		final FieldValidator v = new GravatarFieldValidatorFactory().getValidator(null);
		
		// if there's no error, the test passes
		v.validate(KNOWN_GOOD);
		// check that extra characters pass
		v.validate(KNOWN_GOOD + "Z");
	}
	
	@Test
	public void validateFailBadHash() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory().getValidator(null);
		validateFail(v, KNOWN_GOOD.substring(0, LEN - 1)); // missing last char
		validateFail(v, KNOWN_GOOD.substring(0, LEN - 1) + "c"); // incorrect last char
	}

	private void validateFail(final FieldValidator v, final String value) throws Exception {
		
		try {
			v.validate(value);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalFieldValueException(
					"Gravatar service does not recognize Gravatar hash " +
					value));
		}
	}
	
	@Test
	public void validateFailGravatarError() throws Exception {
		final Class<?> inner = GravatarFieldValidatorFactory.class.getDeclaredClasses()[0];
		final Constructor<?> con = inner.getDeclaredConstructor(int.class);
		con.setAccessible(true);
		final FieldValidator instance = (FieldValidator) con.newInstance(405);
		
		try {
			instance.validate("87194228ef49d635fec5938099042b1");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new FieldValidatorException(
					"Error contacting Gravatar service: 404"));
		}
	}

}
