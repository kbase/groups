package us.kbase.test.groups.core.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.test.groups.TestCommon;

public class WorkspaceInformationTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(WorkspaceInformation.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final WorkspaceInformation wi = WorkspaceInformation.getBuilder(1, "name").build();
		
		assertThat("incorrect id", wi.getID(), is(1));
		assertThat("incorrect name", wi.getName(), is("name"));
		assertThat("incorrect nar name", wi.getNarrativeName(), is(Optional.absent()));
		assertThat("incorrect pub", wi.isPublic(), is(false));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final WorkspaceInformation wi = WorkspaceInformation.getBuilder(1, "name")
				.withNullableNarrativeName("narr")
				.withIsPublic(true)
				.build();
		
		assertThat("incorrect id", wi.getID(), is(1));
		assertThat("incorrect name", wi.getName(), is("name"));
		assertThat("incorrect nar name", wi.getNarrativeName(), is(Optional.of("narr")));
		assertThat("incorrect pub", wi.isPublic(), is(true));
	}
	
	@Test
	public void buildRemoveNarrName() throws Exception {
		final WorkspaceInformation wi = WorkspaceInformation.getBuilder(1, "name")
				.withNullableNarrativeName("narr")
				.withNullableNarrativeName(null)
				.withIsPublic(true)
				.withIsPublic(false)
				.build();
		
		assertThat("incorrect id", wi.getID(), is(1));
		assertThat("incorrect name", wi.getName(), is("name"));
		assertThat("incorrect nar name", wi.getNarrativeName(), is(Optional.absent()));
		assertThat("incorrect pub", wi.isPublic(), is(false));
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		failGetBuilder(0, "n", new IllegalArgumentException("workspace IDs must be > 0"));
		failGetBuilder(1, null, new IllegalArgumentException(
				"name cannot be null or whitespace only"));
		failGetBuilder(1, "  \t   ", new IllegalArgumentException(
				"name cannot be null or whitespace only"));
	}
	
	private void failGetBuilder(final int wsid, final String name, final Exception expected) {
		try {
			WorkspaceInformation.getBuilder(wsid, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
