package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.Group.Role;
import us.kbase.test.groups.TestCommon;

public class GetGroupsParamsTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GetGroupsParams.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final GetGroupsParams p = GetGroupsParams.getBuilder().build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect role", p.getRole(), is(Role.NONE));
	}
	
	@Test
	public void buildWithNulls() throws Exception {
		final GetGroupsParams p = GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo(null)
				.withNullableSortAscending(null)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect role", p.getRole(), is(Role.NONE));
	}
	
	@Test
	public void buildWithDefaults() throws Exception {
		final GetGroupsParams p = GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo(null)
				.withNullableSortAscending(true)
				.withRole(Role.NONE)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect role", p.getRole(), is(Role.NONE));
	}
	
	@Test
	public void buildWithWhitespace() throws Exception {
		final GetGroupsParams p = GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("    \t    ")
				.withNullableSortAscending(true)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.empty()));
		assertThat("incorrect sort", p.isSortAscending(), is(true));
		assertThat("incorrect role", p.getRole(), is(Role.NONE));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GetGroupsParams p = GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo("    foo    ")
				.withNullableSortAscending(false)
				.withRole(Role.ADMIN)
				.build();
		
		assertThat("incorrect exclude", p.getExcludeUpTo(), is(Optional.of("foo")));
		assertThat("incorrect sort", p.isSortAscending(), is(false));
		assertThat("incorrect role", p.getRole(), is(Role.ADMIN));
	}
	
	@Test
	public void withRoleFail() throws Exception {
		try {
			GetGroupsParams.getBuilder().withRole(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("role"));
		}
	}
	
}
