package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.groups.core.request.GroupRequestUserAction;

public class GroupRequestUserActionTest {
	
	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"CANCEL/Cancel",
				"ACCEPT/Accept",
				"DENY/Deny"
				)) {
			final String[] split = enumRep.split("/");
			assertThat("incorrect rep",
					GroupRequestUserAction.valueOf(split[0]).getRepresentation(), is(split[1]));
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values",
				new HashSet<>(Arrays.asList(GroupRequestUserAction.values())),
				is(set(
						GroupRequestUserAction.CANCEL,
						GroupRequestUserAction.ACCEPT,
						GroupRequestUserAction.DENY)));
	}

}
