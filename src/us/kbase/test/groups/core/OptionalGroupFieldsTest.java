package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalString;
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
		
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.empty()));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.empty()));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder().build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.empty()));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.empty()));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildWithNulls() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withNullableIsPrivate(null)
				.withNullablePrivateMemberList(null)
				.build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(false));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.empty()));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.empty()));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withNullableIsPrivate(false)
				.withNullablePrivateMemberList(true)
				.withCustomField(new NumberedCustomField("foo-1"), OptionalString.of("  val  "))
				.withCustomField(new NumberedCustomField("foo"), OptionalString.empty())
				.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
				.build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.of(false)));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.of(true)));
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
	public void buildWithIsPrivateWithUpdate() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withNullableIsPrivate(true)
				.build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.of(true)));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.empty()));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildWithIsPrivateMemberListWithUpdate() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withNullablePrivateMemberList(false)
				.build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.empty()));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.of(false)));
		assertThat("incorrect fields", ofg.getCustomFields(), is(set()));
	}
	
	@Test
	public void buildWithCustomFieldWithUpdate() throws Exception {
		final OptionalGroupFields ofg = OptionalGroupFields.getBuilder()
				.withCustomField(new NumberedCustomField("bar"), OptionalString.empty())
				.build();
		
		assertThat("incorrect update", ofg.hasUpdate(), is(true));
		assertThat("incorrect priv", ofg.isPrivate(), is(Optional.empty()));
		assertThat("incorrect member priv", ofg.isPrivateMemberList(), is(Optional.empty()));
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
