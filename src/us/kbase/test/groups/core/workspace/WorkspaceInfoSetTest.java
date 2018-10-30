package us.kbase.test.groups.core.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.test.groups.TestCommon;

public class WorkspaceInfoSetTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(WorkspaceInfoSet.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final WorkspaceInfoSet wis = WorkspaceInfoSet.getBuilder(null).build();
		
		assertThat("incorrect user", wis.getUser(), is(Optional.absent()));
		assertThat("incorrect infos", wis.getWorkspaceInformation(), is(set()));
		assertThat("incorrect del ws", wis.getNonexistentWorkspaces(), is(set()));
	}
	
	@Test
	public void buildMaximalAndCheckAdminStatus() throws Exception {
		final WorkspaceInfoSet wis = WorkspaceInfoSet.getBuilder(new UserName("foo"))
				.withNonexistentWorkspace(1)
				.withNonexistentWorkspace(8)
				.withWorkspaceInformation(WorkspaceInformation.getBuilder(9, "n").build(), false)
				.withWorkspaceInformation(WorkspaceInformation.getBuilder(22, "n2")
						.withIsPublic(true)
						.withNullableNarrativeName("narr")
						.build(), true)
				.build();
		
		assertThat("incorrect user", wis.getUser(), is(Optional.of(new UserName("foo"))));
		assertThat("incorrect infos", wis.getWorkspaceInformation(), is(set(
				WorkspaceInformation.getBuilder(9, "n").build(),
				WorkspaceInformation.getBuilder(22, "n2")
						.withIsPublic(true)
						.withNullableNarrativeName("narr")
						.build())));
		assertThat("incorrect del ws", wis.getNonexistentWorkspaces(), is(set(1, 8)));
		
		assertThat("incorrect admin", wis.isAdministrator(
				WorkspaceInformation.getBuilder(9, "n").build()), is(false));
		assertThat("incorrect admin", wis.isAdministrator(
				WorkspaceInformation.getBuilder(22, "n2")
						.withIsPublic(true)
						.withNullableNarrativeName("narr")
						.build()),
				is(true));
	}
	
	@Test
	public void immutable() throws Exception {
		final WorkspaceInfoSet wis = WorkspaceInfoSet.getBuilder(new UserName("foo"))
				.withWorkspaceInformation(WorkspaceInformation.getBuilder(9, "n").build(), false)
				.withNonexistentWorkspace(8)
				.build();
		
		try {
			wis.getWorkspaceInformation().add(WorkspaceInformation.getBuilder(7, "a").build());
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		try {
			wis.getNonexistentWorkspaces().add(3);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
	@Test
	public void withWorkspaceInformationFail() throws Exception {
		try {
			WorkspaceInfoSet.getBuilder(null).withWorkspaceInformation(null, false);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("wsInfo"));
		}
	}
	
	@Test
	public void withNonexistentWorkspaceFail() throws Exception {
		try {
			WorkspaceInfoSet.getBuilder(null).withNonexistentWorkspace(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"Workspace IDs must be > 0"));
		}
	}
	
	@Test
	public void isAdministratorFail() throws Exception {
		final WorkspaceInfoSet wis = WorkspaceInfoSet.getBuilder(null)
				.withWorkspaceInformation(WorkspaceInformation.getBuilder(2, "n").build(), true)
				.build();
		
		failIsAdministrator(wis, null, new NullPointerException("wsInfo"));
		failIsAdministrator(wis, WorkspaceInformation.getBuilder(3, "n").build(),
				new IllegalArgumentException("Provided workspace info not included in set"));
				
		
	}
	
	private void failIsAdministrator(
			final WorkspaceInfoSet wis,
			final WorkspaceInformation info,
			final Exception expected) {
		try {
			wis.isAdministrator(info);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
