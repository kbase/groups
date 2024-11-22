package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.OptionalString;
import us.kbase.test.groups.TestCommon;

public class OptionalStringTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(OptionalString.class).usingGetClass().verify();
	}
	
	@Test
	public void empty() throws Exception {
		final OptionalString e = OptionalString.empty();
		
		assertThat("incorrect isPresent", e.isPresent(), is(false));
		assertThat("incorrect isPresent", e.orNull(), is((String) null));
		assertThat("incorrect isPresent", e.toString(), is("OptionalString.empty"));
	}
	
	@Test
	public void of() throws Exception {
		final OptionalString e = OptionalString.of("     val   \t   ");
		
		assertThat("incorrect get", e.get(), is("val"));
		assertThat("incorrect isPresent", e.isPresent(), is(true));
		assertThat("incorrect isPresent", e.orNull(), is("val"));
		assertThat("incorrect isPresent", e.toString(), is("OptionalString[val]"));
	}

	@Test
	public void ofEmptyableWithNull() throws Exception {
		final OptionalString e = OptionalString.ofEmptyable(null);
		
		assertThat("incorrect isPresent", e.isPresent(), is(false));
		assertThat("incorrect isPresent", e.orNull(), is((String) null));
		assertThat("incorrect isPresent", e.toString(), is("OptionalString.empty"));
	}
	
	@Test
	public void ofEmptyableWithWhitespace() throws Exception {
		final OptionalString e = OptionalString.ofEmptyable("   \t     ");
		
		assertThat("incorrect isPresent", e.isPresent(), is(false));
		assertThat("incorrect isPresent", e.orNull(), is((String) null));
		assertThat("incorrect isPresent", e.toString(), is("OptionalString.empty"));
	}
	
	@Test
	public void ofEmptyableWithValue() throws Exception {
		final OptionalString e = OptionalString.ofEmptyable("     val   \t   ");
		
		assertThat("incorrect get", e.get(), is("val"));
		assertThat("incorrect isPresent", e.isPresent(), is(true));
		assertThat("incorrect isPresent", e.orNull(), is("val"));
		assertThat("incorrect isPresent", e.toString(), is("OptionalString[val]"));
	}
	
	@Test
	public void ofFail() throws Exception {
		ofFail(null, new IllegalArgumentException("value cannot be null or whitespace only"));
		ofFail("  \t ", new IllegalArgumentException("value cannot be null or whitespace only"));
	}
	
	private void ofFail(final String value, final Exception expected) {
		try {
			OptionalString.of(value);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getFail() throws Exception {
		try {
			OptionalString.empty().get();
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalStateException(
					"Cannot call get() on an empty OptionalString"));
		}
	}

}
