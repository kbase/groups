package us.kbase.test.groups.core.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Set;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;
import us.kbase.test.groups.TestCommon;

public class WorkspaceIDSetTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(WorkspaceIDSet.class).usingGetClass().verify();
	}
	
	@Test
	public void fromIDs() throws Exception {
		final WorkspaceIDSet wsids = WorkspaceIDSet.fromIDs(
				set(new WorkspaceID(1), new WorkspaceID(23)));
		
		assertThat("incorrect ids", wsids.getIDs(), is(set(1, 23)));
		
		assertImmutable(wsids);
	}

	private void assertImmutable(final WorkspaceIDSet wsids) {
		try {
			wsids.getIDs().add(2);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passes
		}
	}

	@Test
	public void fromInts() throws Exception {
		final WorkspaceIDSet wsids = WorkspaceIDSet.fromInts(set(45, 78));
		
		assertThat("incorrect ids", wsids.getIDs(), is(set(45, 78)));
		
		assertImmutable(wsids);
	}
	
	@Test
	public void fromIDsFail() throws Exception {
		failFromIDs(null, new NullPointerException("ids"));
		failFromIDs(set(new WorkspaceID(1), null),
				new NullPointerException("Null item in collection ids"));
	}
	
	private void failFromIDs(final Set<WorkspaceID> ids, final Exception expected) {
		try {
			WorkspaceIDSet.fromIDs(ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void fromIntsFail() throws Exception {
		failFromInts(null, new NullPointerException("ids"));
		failFromInts(set(1, null),
				new NullPointerException("Null item in collection ids"));
		failFromInts(set(-1),
				new IllegalArgumentException("ID -1 must be > 1"));
	}
	
	private void failFromInts(final Set<Integer> ids, final Exception expected) {
		try {
			WorkspaceIDSet.fromInts(ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void contains() throws Exception {
		final WorkspaceIDSet wsids = WorkspaceIDSet.fromIDs(
				set(new WorkspaceID(1), new WorkspaceID(23)));
		
		assertThat("incorrect contains", wsids.contains(new WorkspaceID(1)), is(true));
		assertThat("incorrect contains", wsids.contains(new WorkspaceID(23)), is(true));
		assertThat("incorrect contains", wsids.contains(new WorkspaceID(2)), is(false));
		assertThat("incorrect contains", wsids.contains(new WorkspaceID(22)), is(false));
		assertThat("incorrect contains", wsids.contains(new WorkspaceID(24)), is(false));
	}
}
