package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDAndName;
import us.kbase.groups.core.GroupName;
import us.kbase.test.groups.TestCommon;

public class GroupIDAndNameTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupIDAndName.class).usingGetClass().verify();
	}
	
	@Test
	public void of() throws Exception {
		final GroupIDAndName gin = GroupIDAndName.of(new GroupID("i"), new GroupName("n"));
		
		assertThat("incorrect id", gin.getID(), is(new GroupID("i")));
		assertThat("incorrect id", gin.getName(), is(new GroupName("n")));
	}
	
	@Test
	public void ofFail() throws Exception {
		ofFail(null, new GroupName("n"), new NullPointerException("id"));
		ofFail(new GroupID("i"), null, new NullPointerException("name"));
	}
	
	private void ofFail(final GroupID i, final GroupName n, final Exception expected) {
		try {
			GroupIDAndName.of(i, n);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
