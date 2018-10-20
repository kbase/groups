package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.groups.core.request.GroupRequestType;

public class GroupRequestTypeTest {
	
	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"INVITE_TO_GROUP/Invite to group",
				"REQUEST_GROUP_MEMBERSHIP/Request group membership"
				)) {
			final String[] split = enumRep.split("/");
			assertThat("incorrect rep",
					GroupRequestType.valueOf(split[0]).getRepresentation(), is(split[1]));
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values",
				new HashSet<>(Arrays.asList(GroupRequestType.values())),
				is(set(
						GroupRequestType.INVITE_TO_GROUP,
						GroupRequestType.REQUEST_GROUP_MEMBERSHIP)));
	}

}
