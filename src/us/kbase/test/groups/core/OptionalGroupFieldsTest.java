package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.FieldItem;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.test.groups.TestCommon;

public class OptionalGroupFieldsTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(OptionalGroupFields.class).usingGetClass().verify();
	}
	
	@Test
	public void getDefault() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getDefault();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.noAction()));
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder().build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.noAction()));
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
	}
	
	@Test
	public void buildWithDescription() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withDescription(StringField.from("   foo    "))
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.from("foo")));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
	}
	
	@Test
	public void buildWithRemoveDescription() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withDescription(StringField.remove())
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.remove()));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
	}
	
	@Test
	public void withDescriptionFail() throws Exception {
		failWithDescription(null, new NullPointerException("description"));
		
		final String uni = "a₸𐍆"; // length 5, 4 code points, 11 bytes in utf-8
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 1250; i++) {
			b.append(uni);
		}
		
		// should pass
		OptionalGroupFields.getBuilder().withDescription(StringField.from(b.toString()));
		
		b.append("a");
		failWithDescription(StringField.from(b.toString()), new IllegalParameterException(
				"description size greater than limit 5000"));
	}

	private void failWithDescription(final StringField desc, final Exception expected) {
		try {
			OptionalGroupFields.getBuilder().withDescription(desc);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
