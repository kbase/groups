package us.kbase.test.groups.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import us.kbase.groups.config.GroupsConfigurationException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;
import us.kbase.groups.fieldvalidators.EnumFieldValidatorFactory;
import us.kbase.groups.util.Util;
import us.kbase.test.groups.TestCommon;

public class UtilTest {

	@Test
	public void exceptOnEmpty() {
		Util.exceptOnEmpty("s", "name"); // expect pass 
		
		exceptOnEmpty(null, "myname",
				new IllegalArgumentException("myname cannot be null or whitespace only"));
		exceptOnEmpty("  \n   ", "myname",
				new IllegalArgumentException("myname cannot be null or whitespace only"));
	}

	private void exceptOnEmpty(final String s, final String name, final Exception expected) {
		try {
			Util.exceptOnEmpty(s, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isNullOrEmpty() {
		assertThat("incorrect null or empty", Util.isNullOrEmpty("s"), is(false));
		assertThat("incorrect null or empty", Util.isNullOrEmpty(null), is(true));
		assertThat("incorrect null or empty", Util.isNullOrEmpty("   \t    \n  "), is(true));
	}
	
	@Test
	public void checkString() throws Exception {
		assertThat("incorrect check", Util.checkString("   foo    ", "bar"), is("foo"));
		assertThat("incorrect check", Util.checkString(TestCommon.LONG1001, "name", 0),
				is(TestCommon.LONG1001));
		assertThat("incorrect check", Util.checkString("ok", "name", 2), is("ok"));
		assertThat("incorrect check", Util.checkString(" \n  ok   \t", "name", 2), is("ok"));
	}
	
	@Test
	public void checkStringFailMissingString() throws Exception {
		failCheckString(null, "foo", new MissingParameterException("foo"));
		failCheckString("    \n \t  ", "foo", new MissingParameterException("foo"));
		failCheckString(null, "foo", 10, new MissingParameterException("foo"));
		failCheckString("    \n \t  ", "foo", 10, new MissingParameterException("foo"));
	}

	@Test
	public void checkStringLengthFail() throws Exception {
		failCheckString("abc", "foo", 2,
				new IllegalParameterException("foo size greater than limit 2"));
	}
	
	@Test
	public void checkStringUnicodeAndLength() throws Exception {
		final String s = "ab𐎂c";
		assertThat("incorrect String length", s.length(), is(5));
		Util.checkString(s, "foo", 4);
		failCheckString(s, "foo", 3,
				new IllegalParameterException("foo size greater than limit 3"));
	}
	
	private void failCheckString(
			final String s,
			final String name,
			final Exception e) {
		try {
			Util.checkString(s, name);
			fail("check string failed");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, e);
		}
	}
	
	private void failCheckString(
			final String s,
			final String name,
			final int length,
			final Exception e) {
		try {
			Util.checkString(s, name, length);
			fail("check string failed");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, e);
		}
	}
	
	@Test
	public void codePoints() throws Exception {
		final String s = "𐍆a₸𐍆𐍆";
		assertThat("incorrect length", s.length(), is(9));
		assertThat("incorrect count", Util.codePoints(s), is(6));
	}
	
	@Test
	public void containsControlCharactersFalse() throws Exception {
		assertThat("incorrect ccc", Util.containsControlCharacters("no control chars here"),
				is(false));
	}
	
	@Test
	public void containsControlCharactersTrue() throws Exception {
		for (final String cc: Arrays.asList("\n", "\t", "\r", "\b", "\f")) {
			// can't do \a for some reason
			final String s = "here's" + cc + "one";
			assertThat("incorrect ccc for " + s, Util.containsControlCharacters(s), is(true));
		}
	}
	
	@Test
	public void noNullsCollection() throws Exception {
		Util.checkNoNullsInCollection(Arrays.asList("foo", "bar"), "whee"); // should work
	}
	
	@Test
	public void noNullsCollectionFail() throws Exception {
		failNoNullsCollection(null, "whee",
				new NullPointerException("whee"));
		failNoNullsCollection(new HashSet<>(Arrays.asList("foo", null, "bar")), "whee1",
				new NullPointerException("Null item in collection whee1"));
		failNoNullsCollection(Arrays.asList("foo", null, "bar"), "whee3",
				new NullPointerException("Null item in collection whee3"));
	}
	
	private void failNoNullsCollection(
			final Collection<?> col,
			final String name,
			final Exception expected) {
		try {
			Util.checkNoNullsInCollection(col, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void noNullsOrEmptiesCollection() throws Exception {
		Util.checkNoNullsOrEmpties(Arrays.asList("foo", "bar"), "whee"); // should work
	}
	
	@Test
	public void noNullsOrEmptiesCollectionFail() throws Exception {
		failNoNullsOrEmptiesCollection(null, "whee",
				new NullPointerException("whee"));
		failNoNullsOrEmptiesCollection(new HashSet<>(Arrays.asList("foo", null, "bar")), "whee1",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee1"));
		failNoNullsOrEmptiesCollection(new HashSet<>(Arrays.asList("foo", "   \n   \t   ", "bar")),
				"whee7", new IllegalArgumentException(
						"Null or whitespace only string in collection whee7"));
		failNoNullsOrEmptiesCollection(Arrays.asList("foo", null, "bar"), "whee6",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee6"));
		failNoNullsOrEmptiesCollection(Arrays.asList("foo", "   \n   \t   ", "bar"), "whee3",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee3"));
	}
	
	private void failNoNullsOrEmptiesCollection(
			final Collection<String> col,
			final String name,
			final Exception expected) {
		try {
			Util.checkNoNullsOrEmpties(col, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void loadClassWithInterface() throws Exception {
		final FieldValidatorFactory fac = Util.loadClassWithInterface(
				EnumFieldValidatorFactory.class.getName(), FieldValidatorFactory.class);
		assertThat("incorrect class loaded", fac, instanceOf(EnumFieldValidatorFactory.class));
	}
	
	@Test
	public void loadClassWithInterfaceFailNoSuchClass() throws Exception {
		failLoadClassWithInterface(EnumFieldValidatorFactory.class.getName() + "a",
				EnumFieldValidatorFactory.class, new GroupsConfigurationException(
						"Cannot load class us.kbase.groups.fieldvalidators." +
						"EnumFieldValidatorFactorya: us.kbase.groups.fieldvalidators." +
						"EnumFieldValidatorFactorya"));
	}
	
	@Test
	public void loadClassWithInterfaceFailIncorrectInterface() throws Exception {
		failLoadClassWithInterface(Map.class.getName(), FieldValidatorFactory.class, 
				new GroupsConfigurationException("Module java.util.Map must implement " +
						"us.kbase.groups.core.fieldvalidation.FieldValidatorFactory interface"));
	}
	
	@Test
	public void loadClassWithInterfaceFailOnConstruct() throws Exception {
		failLoadClassWithInterface(FailOnInstantiation.class.getName(),
				FieldValidatorFactory.class, new IllegalArgumentException("foo"));
	}
	
	@Test
	public void loadClassWithInterfaceFailNoNullaryConstructor() throws Exception {
		failLoadClassWithInterface(FailOnInstantiationNoNullaryConstructor.class.getName(),
				FieldValidatorFactory.class, new GroupsConfigurationException(
						"Module us.kbase.test.groups.util.FailOnInstantiation" +
						"NoNullaryConstructor could not be instantiated: us.kbase.test.groups." +
						"util.FailOnInstantiationNoNullaryConstructor"));
	}
	
	@Test
	public void loadClassWithInterfaceFailPrivateConstructor() throws Exception {
		try {
			Util.loadClassWithInterface(
					FailOnInstantiationPrivateConstructor.class.getName(),
					FieldValidatorFactory.class);
			fail("expected exception");
		} catch (GroupsConfigurationException got) {
			assertThat("incorrect exception message", got.getMessage(), startsWith(
					"Module us.kbase.test.groups.util.FailOnInstantiation" +
					"PrivateConstructor could not be instantiated: "
			));
			// trivial text changes from java 8 -> 11
			assertThat("incorrect exception message", got.getMessage(), endsWith(
					"not access a member of class us." +
					"kbase.test.groups.util.FailOnInstantiationPrivateConstructor " +
					"with modifiers \"private\""
			));
		}
	}
	
	private void failLoadClassWithInterface(
			final String className,
			final Class<?> interfce,
			final Exception e)
			throws Exception {
		try {
			Util.loadClassWithInterface(className, interfce);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, e);
		}
	}
}
