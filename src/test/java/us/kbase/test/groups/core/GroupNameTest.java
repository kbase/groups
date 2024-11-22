package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class GroupNameTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupName.class).usingGetClass().verify();
	}
	
	@Test
	public void constructor() throws Exception {
		final GroupName n1 = new GroupName("    foooΔ   ");
		assertThat("incorrect groupname", n1.getName(), is("foooΔ"));
		assertThat("incorrect toString", n1.toString(), is("GroupName [name=foooΔ]"));
		
		final GroupName n2 = new GroupName(TestCommon.LONG1001.substring(0, 256));
		assertThat("incorrect groupname", n2.getName(), is(TestCommon.LONG1001.substring(0, 256)));
		
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("group name"));
		failConstruct("   \n  ", new MissingParameterException("group name"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"group name contains control characters"));
		failConstruct(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"group name size greater than limit 256"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new GroupName(name);
			fail("expected exception");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	

}
