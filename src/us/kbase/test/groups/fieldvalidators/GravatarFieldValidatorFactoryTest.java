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
	
	@Test
	public void validate() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory().getValidator(null);
		
		v.validate("87194228ef49d635fec5938099042b1d");
		// if there's no error, the test passes
	}
	
	@Test
	public void validateFailBadHash() throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory().getValidator(null);
		
		try {
			v.validate("87194228ef49d635fec5938099042b1");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalFieldValueException(
					"Gravatar service does not recognize Gravatar hash " +
					"87194228ef49d635fec5938099042b1"));
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
