package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static us.kbase.test.groups.TestCommon.set;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.groups.core.request.GroupRequestStatusType;

public class GroupRequestStatusTypeTest {
	
	@Test
	public void representation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"OPEN/Open",
				"CANCELED/Canceled",
				"EXPIRED/Expired",
				"ACCEPTED/Accepted",
				"DENIED/Denied"
				)) {
			final String[] split = enumRep.split("/");
			assertThat("incorrect rep",
					GroupRequestStatusType.valueOf(split[0]).getRepresentation(), is(split[1]));
		}
	}
	
	@Test
	public void values() {
		assertThat("incorrect values",
				new HashSet<>(Arrays.asList(GroupRequestStatusType.values())),
				is(set(
						GroupRequestStatusType.OPEN,
						GroupRequestStatusType.CANCELED,
						GroupRequestStatusType.EXPIRED,
						GroupRequestStatusType.ACCEPTED,
						GroupRequestStatusType.DENIED)));
	}

}
