package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static us.kbase.test.groups.TestCommon.inst;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GetRequestsParams;

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
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GetRequestsParams p = GetRequestsParams.getBuilder()
				.withNullableExcludeUpTo(inst(10000))
				.withNullableIncludeClosed(true)
				.withNullableSortAscending(false)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.of(inst(10000))));
		assertThat("incorrect closed", p.isIncludeClosed(), is(true));
		assertThat("incorrect sort", p.isSortAscending(), is(false));
	}
	
}
