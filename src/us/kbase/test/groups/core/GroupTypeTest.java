package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.test.groups.TestCommon;

public class GroupTypeTest {

	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"ORGANIZATION/Organization",
				"PROJECT/Project",
				"TEAM/Team"
				)) {
			final String[] split = enumRep.split("/");
			final GroupType gt = GroupType.valueOf(split[0]);
			assertThat("incorrect rep",
					gt.getRepresentation(), is(split[1]));
			assertThat("incorrect type", GroupType.fromRepresentation(split[1]), is(gt));
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values", new HashSet<>(Arrays.asList(GroupType.values())),
				is(set(GroupType.ORGANIZATION, GroupType.PROJECT, GroupType.TEAM)));
	}
	
	@Test
	public void fromRepresentationFail() {
		failFromRepresentation(null, new IllegalParameterException("Invalid group type: null"));
		failFromRepresentation("   \t  ",
				new IllegalParameterException("Invalid group type:    \t  "));
		failFromRepresentation("Organisation",
				new IllegalParameterException("Invalid group type: Organisation"));
	}
	
	private void failFromRepresentation(final String rep, final Exception expected) {
		try {
			GroupType.fromRepresentation(rep);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
