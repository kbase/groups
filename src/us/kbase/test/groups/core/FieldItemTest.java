package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.FieldItem;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.test.auth2.TestCommon;

public class FieldItemTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(FieldItem.class).verify();
		EqualsVerifier.forClass(StringField.class).verify();
	}
	
	@Test
	public void from() throws Exception {
		final FieldItem<Integer> fi = FieldItem.from(6);
		
		assertThat("incorrect item", fi.get(), is(6));
		assertThat("incorrect or null", fi.orNull(), is(6));
		assertThat("incorrect has", fi.hasItem(), is(true));
		assertThat("incorrect has", fi.hasAction(), is(true));
		assertThat("incorrect no action", fi.isNoAction(), is(false));
		assertThat("incorrect remove", fi.isRemove(), is(false));
	}
	
	@Test
	public void fromFail() {
		try {
			FieldItem.from(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("item"));
		}
	}
	
	@Test
	public void fromNullable() throws Exception {
		final FieldItem<Integer> fi = FieldItem.fromNullable(6);
		
		assertThat("incorrect item", fi.get(), is(6));
		assertThat("incorrect or null", fi.orNull(), is(6));
		assertThat("incorrect has", fi.hasItem(), is(true));
		assertThat("incorrect has", fi.hasAction(), is(true));
		assertThat("incorrect no action", fi.isNoAction(), is(false));
		assertThat("incorrect remove", fi.isRemove(), is(false));
		
		final FieldItem<Integer> fin = FieldItem.fromNullable(null);
		
		assertThat("incorrect or null", fin.orNull(), is(nullValue()));
		assertThat("incorrect has", fin.hasItem(), is(false));
		assertThat("incorrect has", fin.hasAction(), is(false));
		assertThat("incorrect no action", fin.isNoAction(), is(true));
		assertThat("incorrect remove", fin.isRemove(), is(false));
	}
	
	@Test
	public void remove() throws Exception {
		final FieldItem<Integer> fi = FieldItem.remove();
		
		assertThat("incorrect or null", fi.orNull(), is(nullValue()));
		assertThat("incorrect has", fi.hasItem(), is(false));
		assertThat("incorrect has", fi.hasAction(), is(true));
		assertThat("incorrect no action", fi.isNoAction(), is(false));
		assertThat("incorrect remove", fi.isRemove(), is(true));
	}
	
	@Test
	public void noAction() throws Exception {
		final FieldItem<Integer> fi = FieldItem.noAction();
		
		assertThat("incorrect or null", fi.orNull(), is(nullValue()));
		assertThat("incorrect has", fi.hasItem(), is(false));
		assertThat("incorrect has", fi.hasAction(), is(false));
		assertThat("incorrect no action", fi.isNoAction(), is(true));
		assertThat("incorrect remove", fi.isRemove(), is(false));
	}
	
	@Test
	public void getFail() throws Exception {
		
		try {
			FieldItem.remove().get();
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalStateException(
					"Cannot call get() on a FieldItem without an item"));
		}
	}
	
	@Test
	public void stringFieldFromNullable() {
		final StringField s1 = StringField.fromNullable(null);
		
		assertThat("incorrect or null", s1.orNull(), is(nullValue()));
		assertThat("incorrect has", s1.hasItem(), is(false));
		assertThat("incorrect has", s1.hasAction(), is(false));
		assertThat("incorrect no action", s1.isNoAction(), is(true));
		assertThat("incorrect remove", s1.isRemove(), is(false));
		
		final StringField s2 = StringField.fromNullable("   \t   " );
		
		assertThat("incorrect or null", s2.orNull(), is(nullValue()));
		assertThat("incorrect has", s2.hasItem(), is(false));
		assertThat("incorrect has", s2.hasAction(), is(false));
		assertThat("incorrect no action", s2.isNoAction(), is(true));
		assertThat("incorrect remove", s2.isRemove(), is(false));
		
		final StringField s3 = StringField.fromNullable("     a string   \t   ");
		
		assertThat("incorrect item", s3.get(), is("a string"));
		assertThat("incorrect or null", s3.orNull(), is("a string"));
		assertThat("incorrect has", s3.hasItem(), is(true));
		assertThat("incorrect has", s3.hasAction(), is(true));
		assertThat("incorrect no action", s3.isNoAction(), is(false));
		assertThat("incorrect remove", s3.isRemove(), is(false));
	}
	
	@Test
	public void stringFieldFrom() {
		final StringField s = StringField.from("     a string   \t   ");
		
		assertThat("incorrect item", s.get(), is("a string"));
		assertThat("incorrect or null", s.orNull(), is("a string"));
		assertThat("incorrect has", s.hasItem(), is(true));
		assertThat("incorrect has", s.hasAction(), is(true));
		assertThat("incorrect no action", s.isNoAction(), is(false));
		assertThat("incorrect remove", s.isRemove(), is(false));
	}
	
	@Test
	public void stringFieldFromFail() throws Exception {
		failStringFieldFrom(null);
		failStringFieldFrom("   \t   ");
	}
	
	private void failStringFieldFrom(final String s) {
		try {
			StringField.from(s);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"s cannot be null or whitespace only"));
		}
	}
	
	@Test
	public void stringFieldRemove() throws Exception {
		final StringField fi = StringField.remove();
		
		assertThat("incorrect or null", fi.orNull(), is(nullValue()));
		assertThat("incorrect has", fi.hasItem(), is(false));
		assertThat("incorrect has", fi.hasAction(), is(true));
		assertThat("incorrect no action", fi.isNoAction(), is(false));
		assertThat("incorrect remove", fi.isRemove(), is(true));
	}
	
	@Test
	public void stringFieldNoAction() throws Exception {
		final StringField fi = StringField.noAction();
		
		assertThat("incorrect or null", fi.orNull(), is(nullValue()));
		assertThat("incorrect has", fi.hasItem(), is(false));
		assertThat("incorrect has", fi.hasAction(), is(false));
		assertThat("incorrect no action", fi.isNoAction(), is(true));
		assertThat("incorrect remove", fi.isRemove(), is(false));
	}
	

}
