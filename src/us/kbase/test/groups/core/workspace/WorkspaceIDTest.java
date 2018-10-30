package us.kbase.test.groups.core.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.test.groups.TestCommon;

public class WorkspaceIDTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(WorkspaceID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructWithInt() throws Exception {
		final WorkspaceID i = new WorkspaceID(1);
		
		assertThat("incorrect id", i.getID(), is(1));
	}
	
	@Test
	public void constructWithString() throws Exception {
		final WorkspaceID i = new WorkspaceID("  1   ");
		
		assertThat("incorrect id", i.getID(), is(1));
	}
	
	@Test
	public void constructFailInt() throws Exception {
		try {
			new WorkspaceID(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got,
					new IllegalParameterException("Workspace IDs are > 0"));
		}
	}
	
	@Test
	public void constructFailString() throws Exception {
		failConstructString(null, new NullPointerException("workspaceID"));
		failConstructString("foo", new IllegalParameterException("Illegal workspace ID: foo"));
		failConstructString(" 0  ", new IllegalParameterException("Workspace IDs are > 0"));
	}
	
	private void failConstructString(final String id, final Exception expected) {
		
		try {
			new WorkspaceID(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
