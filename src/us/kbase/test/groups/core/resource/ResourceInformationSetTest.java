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
				.withNonexistentResource(new ResourceID("rid1"))
				.withNonexistentResource(new ResourceID("rid3"))
				.withResource(new ResourceID("rid6"))
				.withResource(new ResourceID("rid6"))
				.withResource(new ResourceID("rid7"))
				.withResourceField(new ResourceID("rid2"), "f", 1)
				.withResourceField(new ResourceID("rid2"), "f2", "yay")
				.withResourceField(new ResourceID("rid7"), "f2", "ya0")
				.build();
		
		assertThat("incorrect user", r.getUser(), is(Optional.of(new UserName("u"))));
		assertThat("incorrect resources", r.getResources(), is(set(
				new ResourceID("rid6"), new ResourceID("rid7"), new ResourceID("rid2"))));
		assertThat("incorrect nonexist", r.getNonexistentResources(), is(set(
				new ResourceID("rid1"), new ResourceID("rid3"))));
		assertThat("incorrect fields", r.getFields(new ResourceID("rid6")),
				is(Collections.emptyMap()));
		assertThat("incorrect fields", r.getFields(new ResourceID("rid7")),
				is(ImmutableMap.of("f2", "ya0")));
		assertThat("incorrect fields", r.getFields(new ResourceID("rid2")),
				is(ImmutableMap.of("f", 1, "f2", "yay")));
	}

	@Test
	public void immutable() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(new UserName("u"))
				.withNonexistentResource(new ResourceID("rid3"))
				.withResourceField(new ResourceID("rid7"), "f2", "ya0")
				.build();
		
		try {
			r.getResources().add(new ResourceID("rid"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			r.getNonexistentResources().add(new ResourceID("rid"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			r.getFields(new ResourceID("rid7")).put("f", 1);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
	@Test
	public void getFieldsFail() throws Exception {
		final ResourceInformationSet r = ResourceInformationSet.getBuilder(new UserName("u"))
				.withResourceField(new ResourceID("rid7"), "f2", "ya0")
				.build();
		
		try {
			r.getFields(new ResourceID("rid8"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"Provided resource not included in set"));
		}
	}
	
	@Test
	public void withResourceFail() throws Exception {
		try {
			ResourceInformationSet.getBuilder(null).withResource(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("resource"));
		}
	}

	@Test
	public void withResourceFieldFail() throws Exception {
		final ResourceID d = new ResourceID("rid");
		
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
			final ResourceID d,
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
