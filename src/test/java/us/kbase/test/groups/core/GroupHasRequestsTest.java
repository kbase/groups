package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.groups.core.GroupHasRequests;

public class GroupHasRequestsTest {
	
	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"NONE/None",
				"OLD/Old",
				"NEW/New"
				)) {
			final String[] split = enumRep.split("/");
			assertThat("incorrect rep",
					GroupHasRequests.valueOf(split[0]).getRepresentation(), is(split[1]));
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values",
				new HashSet<>(Arrays.asList(GroupHasRequests.values())),
				is(set(
						GroupHasRequests.NONE,
						GroupHasRequests.OLD,
						GroupHasRequests.NEW)));
	}

}
