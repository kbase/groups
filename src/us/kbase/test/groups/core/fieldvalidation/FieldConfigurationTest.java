package us.kbase.test.groups.core.fieldvalidation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.fieldvalidation.FieldConfiguration;

public class FieldConfigurationTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(FieldConfiguration.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final FieldConfiguration c = FieldConfiguration.getBuilder().build();
		
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect user set", c.isUserSettable(), is(false));
	}
	
	@Test
	public void buildWithNulls() throws Exception {
		final FieldConfiguration c = FieldConfiguration.getBuilder()
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withNullableIsUserSettable(true)
				.withNullableIsMinimalViewField(null)
				.withNullableIsNumberedField(null)
				.withNullableIsPublicField(null)
				.withNullableIsUserSettable(false)
				.build();
		
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect user set", c.isUserSettable(), is(false));
	}
	
	@Test
	public void buildWithFalse() throws Exception {
		final FieldConfiguration c = FieldConfiguration.getBuilder()
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withNullableIsUserSettable(true)
				.withNullableIsMinimalViewField(false)
				.withNullableIsNumberedField(false)
				.withNullableIsPublicField(false)
				.withNullableIsUserSettable(false)
				.build();
		
		assertThat("incorrect num", c.isNumberedField(), is(false));
		assertThat("incorrect priv", c.isPublicField(), is(false));
		assertThat("incorrect list", c.isMinimalViewField(), is(false));
		assertThat("incorrect user set", c.isUserSettable(), is(false));
	}
	
	
	@Test
	public void buildMaximal() throws Exception {
		final FieldConfiguration c = FieldConfiguration.getBuilder()
				.withNullableIsMinimalViewField(true)
				.withNullableIsNumberedField(true)
				.withNullableIsPublicField(true)
				.withNullableIsUserSettable(true)
				.build();
				
		assertThat("incorrect num", c.isNumberedField(), is(true));
		assertThat("incorrect priv", c.isPublicField(), is(true));
		assertThat("incorrect list", c.isMinimalViewField(), is(true));
		assertThat("incorrect user set", c.isUserSettable(), is(true));
	}
}
