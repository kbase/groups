package us.kbase.test.groups.core.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.test.groups.TestCommon;

public class ResourceInformationSetTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(ResourceInformationSet.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(null)
				.build();
		
		assertThat("incorrect user", r.getUser(), is(Optional.empty()));
		assertThat("incorrect resources", r.getResources(), is(set()));
		assertThat("incorrect nonexist", r.getNonexistentResources(), is(set()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(new UserName("u"))
				.withNonexistentResource(getResDesc("aid1", "rid1"))
				.withNonexistentResource(getResDesc("aid3", "rid3"))
				.withResourceDescriptor(getResDesc("aid6", "rid6"))
				.withResourceDescriptor(getResDesc("aid6", "rid6"))
				.withResourceDescriptor(getResDesc("aid7", "rid7"))
				.withResourceField(getResDesc("aid2", "rid2"), "f", 1)
				.withResourceField(getResDesc("aid2", "rid2"), "f2", "yay")
				.withResourceField(getResDesc("aid7", "rid7"), "f2", "ya0")
				.build();
		
		assertThat("incorrect user", r.getUser(), is(Optional.of(new UserName("u"))));
		assertThat("incorrect resources", r.getResources(), is(set(
				getResDesc("aid6", "rid6"), getResDesc("aid7", "rid7"),
				getResDesc("aid2", "rid2"))));
		assertThat("incorrect nonexist", r.getNonexistentResources(), is(set(
				getResDesc("aid1", "rid1"), getResDesc("aid3", "rid3"))));
		assertThat("incorrect fields", r.getFields(getResDesc("aid6", "rid6")),
				is(Collections.emptyMap()));
		assertThat("incorrect fields", r.getFields(getResDesc("aid7", "rid7")),
				is(ImmutableMap.of("f2", "ya0")));
		assertThat("incorrect fields", r.getFields(getResDesc("aid2", "rid2")),
				is(ImmutableMap.of("f", 1, "f2", "yay")));
	}

	private ResourceDescriptor getResDesc(final String aid, final String rid)
			throws MissingParameterException, IllegalParameterException {
		return new ResourceDescriptor(new ResourceAdministrativeID(aid), new ResourceID(rid));
	}
	
	@Test
	public void immutable() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(new UserName("u"))
				.withNonexistentResource(getResDesc("aid3", "rid3"))
				.withResourceField(getResDesc("aid7", "rid7"), "f2", "ya0")
				.build();
		
		try {
			r.getResources().add(getResDesc("aid", "rid"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			r.getNonexistentResources().add(getResDesc("aid", "rid"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			r.getFields(getResDesc("aid7", "rid7")).put("f", 1);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
	@Test
	public void getFieldsFail() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(new UserName("u"))
				.withResourceField(getResDesc("aid7", "rid7"), "f2", "ya0")
				.build();
		
		try {
			r.getFields(getResDesc("aid7", "rid8"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"Provided resource not included in set"));
		}
	}
	
	@Test
	public void withResourceDescriptorFail() throws Exception {
		try {
			ResourceInformationSet.getBuilder(null).withResourceDescriptor(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("resource"));
		}
	}

	@Test
	public void withResourceFieldFail() throws Exception {
		final ResourceDescriptor d = getResDesc("aid", "rid");
		
		failWithResourceField(null, "f", 1, new NullPointerException("resource"));
		failWithResourceField(d, null, 1, new IllegalArgumentException(
				"field cannot be null or whitespace only"));
		failWithResourceField(d, "  \t   ", 1, new IllegalArgumentException(
				"field cannot be null or whitespace only"));
	}
	
	@Test
	public void withNonexistantResourceFail() throws Exception {
		try {
			ResourceInformationSet.getBuilder(null).withNonexistentResource(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("resource"));
		}
	}
	
	private void failWithResourceField(
			final ResourceDescriptor d,
			final String f,
			final Object v,
			final Exception expected) {
		try {
			ResourceInformationSet.getBuilder(null).withResourceField(d, f, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
