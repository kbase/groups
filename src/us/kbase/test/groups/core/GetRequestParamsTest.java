package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class GetRequestParamsTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GetRequestsParams.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder().build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect closed", p.isIncludeClosed(), is(false));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect type", p.getResourceType(), is(Optional.empty()));
		assertThat("incorrect type", p.getResourceID(), is(Optional.empty()));
	}
	
	@Test
	public void buildWithNulls() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(null)
				.withNullableIncludeClosed(null)
				.withNullableSortAscending(null)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect closed", p.isIncludeClosed(), is(false));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect type", p.getResourceType(), is(Optional.empty()));
		assertThat("incorrect type", p.getResourceID(), is(Optional.empty()));
	}
	
	@Test
	public void buildWithDefaults() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(null)
				.withNullableIncludeClosed(false)
				.withNullableSortAscending(true)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect closed", p.isIncludeClosed(), is(false));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect type", p.getResourceType(), is(Optional.empty()));
		assertThat("incorrect type", p.getResourceID(), is(Optional.empty()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.withResource(new ResourceType("t"), new ResourceID("id"))
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.of(inst(10000))));
		assertThat("incorrect closed", p.isIncludeClosed(), is(true));
		assertThat("incorrect sort", p.isSortAscending(), is(false));
		assertThat("incorrect type", p.getResourceType(), is(Optional.of(new ResourceType("t"))));
		assertThat("incorrect type", p.getResourceID(), is(Optional.of(new ResourceID("id"))));
	}
	
	@Test
	public void withResourceFailNulls() throws Exception {
		withResourceFail(null, new ResourceID("i"), new NullPointerException("type"));
		withResourceFail(new ResourceType("t"), null, new NullPointerException("id"));
	}
	
	private void withResourceFail(
			final ResourceType t,
			final ResourceID i,
			final Exception expected) {
		try {
			GetRequestsParams.getBuilder().withResource(t, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
