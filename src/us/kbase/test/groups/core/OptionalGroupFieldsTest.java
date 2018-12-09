package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.FieldItem;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalString;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
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
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder().build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.noAction()));
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildWithDescription() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withDescription(StringField.from("   foo    "))
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.from("foo")));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildWithRemoveDescription() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withDescription(StringField.remove())
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.remove()));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
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
	
	@Test
	public void buildMaximal() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withDescription(StringField.from("   foo    "))
				.withCustomField(new NumberedCustomField("foo-1"), OptionalString.of("  val  "))
				.withCustomField(new NumberedCustomField("foo"), OptionalString.empty())
				.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.from("foo")));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect fields", ofg.getCustomFields(), is(
				set(new NumberedCustomField("foo-1"), new NumberedCustomField("foo"),
						new NumberedCustomField("bar"))));
		assertThat("incorrect val", ofg.getCustomValue(new NumberedCustomField("foo-1")),
				is(OptionalString.of("val")));
		assertThat("incorrect val", ofg.getCustomValue(new NumberedCustomField("foo")),
				is(OptionalString.empty()));
		assertThat("incorrect val", ofg.getCustomValue(new NumberedCustomField("bar")),
				is(OptionalString.empty()));
	}
	
	@Test
	public void buildWithCustomFieldWithUpdate() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
				.build();
		
		assertThat("incorrect desc", ofg.getDescription(), is(FieldItem.noAction()));
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect fields", ofg.getCustomFields(), is(
				set(new NumberedCustomField("bar"))));
		assertThat("incorrect val", ofg.getCustomValue(new NumberedCustomField("bar")),
				is(OptionalString.empty()));
	}
	
	@Test
	public void withCustomFieldFail() throws Exception {
		failWithCustomField(null, OptionalString.empty(), new NullPointerException("field"));
		failWithCustomField(new NumberedCustomField("a"), null, new NullPointerException("value"));
	}
	
	private void failWithCustomField(
			final NumberedCustomField field,
			final OptionalString value,
			final Exception expected) {
		try {
			OptionalGroupFields.getBuilder().withCustomField(field, value);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getCustomValueFail() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
				.build();

		failGetCustomValue(ofg, null, new NullPointerException("field"));
		failGetCustomValue(ofg, new NumberedCustomField("bar-1"),
				new IllegalArgumentException("No such field bar-1"));
	}
	
	private void failGetCustomValue(
			final OptionalGroupFields f,
			final NumberedCustomField field,
			final Exception expected) {
		try {
			f.getCustomValue(field);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
