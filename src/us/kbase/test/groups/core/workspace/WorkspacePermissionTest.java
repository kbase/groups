package us.kbase.test.groups.core.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.junit.Test;

import us.kbase.groups.core.workspace.WorkspacePermission;
import us.kbase.test.groups.TestCommon;

public class WorkspacePermissionTest {
	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"NONE/None/n",
				"READ/Read/r",
				"WRITE/Write/w",
				"ADMIN/Admin/a",
				"OWN/Own/-"
				)) {
			final String[] split = enumRep.split("/");
			final WorkspacePermission wp = WorkspacePermission.valueOf(split[0]);
			assertThat("incorrect rep", wp.getRepresentation(), is(split[1]));
			assertThat("incorrect type", WorkspacePermission.fromRepresentation(split[1]), is(wp));
			if (split[2].equals("-")) {
				assertThat("incorrect rep", wp.getWorkspaceRepresentation(), is(Optional.empty()));
			} else {
				assertThat("incorrect rep",
						wp.getWorkspaceRepresentation(), is(Optional.ofNullable(split[2])));
				assertThat("incorrect type",
						WorkspacePermission.fromWorkspaceRepresentation(split[2]), is(wp));
			}
			
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values", new HashSet<>(Arrays.asList(WorkspacePermission.values())),
				is(set(WorkspacePermission.OWN,
						WorkspacePermission.ADMIN,
						WorkspacePermission.WRITE,
						WorkspacePermission.READ,
						WorkspacePermission.NONE)));
	}
	
	@Test
	public void isAdmin() {
		assertThat("incorrect admin", WorkspacePermission.OWN.isAdmin(), is(true));
		assertThat("incorrect admin", WorkspacePermission.ADMIN.isAdmin(), is(true));
		assertThat("incorrect admin", WorkspacePermission.WRITE.isAdmin(), is(false));
		assertThat("incorrect admin", WorkspacePermission.READ.isAdmin(), is(false));
		assertThat("incorrect admin", WorkspacePermission.NONE.isAdmin(), is(false));
	}
	
	@Test
	public void fromRepresentationFail() throws Exception {
		failfromRepresentation(null);
		failfromRepresentation("   \t   ");
		failfromRepresentation("Onw");
	}
	
	private void failfromRepresentation(final String r) {
		
		try {
			WorkspacePermission.fromRepresentation(r);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"No such representation: " + r));
		}
	}
	
	@Test
	public void fromWorkspaceRepresentationFail() throws Exception {
		failfromWorkspaceRepresentation(null);
		failfromWorkspaceRepresentation("   \t   ");
		failfromWorkspaceRepresentation("Own");
		failfromWorkspaceRepresentation("Read");
		failfromWorkspaceRepresentation("b");
	}
	
	private void failfromWorkspaceRepresentation(final String r) {
		
		try {
			WorkspacePermission.fromWorkspaceRepresentation(r);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"No such workspace representation: " + r));
		}
	}

}
