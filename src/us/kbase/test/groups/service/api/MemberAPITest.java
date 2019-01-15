package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDAndName;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.service.api.MemberAPI;
import us.kbase.test.groups.TestCommon;

public class MemberAPITest {

	@Test
	public void getMemberGroups() throws Exception {
		final Groups g = mock(Groups.class);
		
		final MemberAPI mapi = new MemberAPI(g);
		
		when(g.getMemberGroups(new Token("tok"))).thenReturn(Arrays.asList(
				GroupIDAndName.of(new GroupID("id1"), new GroupName("n1")),
				GroupIDAndName.of(new GroupID("id2"), new GroupName("n2"))));
		
		assertThat("incorrect groups", mapi.getMemberGroups("tok"), is(Arrays.asList(
				ImmutableMap.of("id", "id1", "name", "n1"),
				ImmutableMap.of("id", "id2", "name", "n2"))));
	}
	
	@Test
	public void failGetMemberGroups() throws Exception {
		failGetMemberGroups(null, new NoTokenProvidedException("No token provided"));
		failGetMemberGroups("   \t   ", new NoTokenProvidedException("No token provided"));
	}

	private void failGetMemberGroups(final String t, Exception expected) {
		try {
			new MemberAPI(mock(Groups.class)).getMemberGroups(t);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
