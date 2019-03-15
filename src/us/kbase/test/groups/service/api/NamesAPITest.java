package us.kbase.test.groups.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDNameMembership;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.service.api.NamesAPI;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class NamesAPITest {
	
	@Test
	public void getGroupNamesAnonymous() throws Exception {
		final Groups g = mock(Groups.class);
		
		final NamesAPI napi = new NamesAPI(g);
		
		when(g.getGroupNames(null, Arrays.asList(
				new GroupID("i3"), new GroupID("i1"), new GroupID("i22"))))
				.thenReturn(Arrays.asList(
						GroupIDNameMembership.getBuilder(new GroupID("i1"))
								.withGroupName(new GroupName("n1"))
								.withIsPrivate(false)
								.build(),
						GroupIDNameMembership.getBuilder(new GroupID("i3"))
								.build(),
						GroupIDNameMembership.getBuilder(new GroupID("i22"))
								.withGroupName(new GroupName("n22"))
								.withIsPrivate(false)
								.build()));
		
		final List<Map<String, String>> expected = Arrays.asList(
				MapBuilder.<String, String>newHashMap()
						.with("id", "i1")
						.with("name", "n1")
						.build(),
				MapBuilder.<String, String>newHashMap()
						.with("id", "i3")
						.with("name", null)
						.build(),
				MapBuilder.<String, String>newHashMap()
						.with("id", "i22")
						.with("name", "n22")
						.build());
		
		assertThat("incorrect names", napi.getGroupNames(null, "  i3,   \t,   i1, i22   "),
				is(expected));
		assertThat("incorrect names", napi.getGroupNames("  \t   ", "  i3,   \t,   i1, i22   "),
				is(expected));
	}
	
	@Test
	public void getGroupNames() throws Exception {
		final Groups g = mock(Groups.class);
		
		final NamesAPI napi = new NamesAPI(g);
		
		when(g.getGroupNames(new Token("token"),
				Arrays.asList(new GroupID("i3"), new GroupID("i1"), new GroupID("i22"))))
				.thenReturn(Arrays.asList(
						GroupIDNameMembership.getBuilder(new GroupID("i1"))
								.withGroupName(new GroupName("n1"))
								.withIsPrivate(false)
								.build(),
						GroupIDNameMembership.getBuilder(new GroupID("i3"))
								.build(),
						GroupIDNameMembership.getBuilder(new GroupID("i22"))
								.withGroupName(new GroupName("n22"))
								.withIsPrivate(false)
								.build()));
		
		final List<Map<String, String>> expected = Arrays.asList(
				MapBuilder.<String, String>newHashMap()
						.with("id", "i1")
						.with("name", "n1")
						.build(),
				MapBuilder.<String, String>newHashMap()
						.with("id", "i3")
						.with("name", null)
						.build(),
				MapBuilder.<String, String>newHashMap()
						.with("id", "i22")
						.with("name", "n22")
						.build());
		
		assertThat("incorrect names", napi.getGroupNames("token", "  i3,   \t,   i1, i22   "),
				is(expected));
	}
	
	@Test
	public void failGetGroupNames() throws Exception {
		failGetGroupNames("  id1, bad*id, ", new IllegalParameterException(
				ErrorType.ILLEGAL_GROUP_ID, "Illegal character in group id bad*id: *"));
	}
	
	private void failGetGroupNames(final String ids, final Exception expected) {
		try {
			new NamesAPI(mock(Groups.class)).getGroupNames(null, ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
