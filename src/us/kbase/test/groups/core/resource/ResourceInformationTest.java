package us.kbase.test.groups.core.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformation;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.auth2.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class ResourceInformationTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(ResourceInformation.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final ResourceInformation ri = ResourceInformation.getBuilder(
				new ResourceType("t"), new ResourceID("i"))
				.build();
		
		assertThat("incorrect type", ri.getResourceType(), is(new ResourceType("t")));
		assertThat("incorrect id", ri.getResourceID(), is(new ResourceID("i")));
		assertThat("incorrect fields", ri.getResourceFields(), is(Collections.emptyMap()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final ResourceInformation ri = ResourceInformation.getBuilder(
				new ResourceType("t"), new ResourceID("i"))
				.withField("f", null)
				.withField("foo", "bar")
				.withField("baz", 1)
				.withField("whee", "   \t    ")
				.build();
		
		assertThat("incorrect type", ri.getResourceType(), is(new ResourceType("t")));
		assertThat("incorrect id", ri.getResourceID(), is(new ResourceID("i")));
		assertThat("incorrect fields", ri.getResourceFields(), is(MapBuilder.newHashMap()
				.with("f", null).with("foo", "bar").with("baz", 1).with("whee", "   \t    ")
				.build()));
	}

	@Test
	public void getBuilderFailNulls() throws Exception {
		getBuilderFail(null, new ResourceID("i"), new NullPointerException("type"));
		getBuilderFail(new ResourceType("t"), null, new NullPointerException("id"));
	}
	
	private void getBuilderFail(
			final ResourceType t,
			final ResourceID i,
			final Exception expected) {
		try {
			ResourceInformation.getBuilder(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void withFieldFailBadInput() throws Exception {
		withFieldFail(null, new IllegalArgumentException(
				"field cannot be null or whitespace only"));
		withFieldFail("  \t    ", new IllegalArgumentException(
				"field cannot be null or whitespace only"));
	}
	
	private void withFieldFail(final String field, final Exception expected) {
		try {
			ResourceInformation.getBuilder(new ResourceType("t"), new ResourceID("i"))
					.withField(field, null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
