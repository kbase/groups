package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupCreationParams.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.test.groups.TestCommon;

public class GroupCreationParamsTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupCreationParams.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.build();
		
		assertThat("incorrect id", p.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect name", p.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect desc", p.getDescription(), is(Optional.absent()));
		assertThat("incorrect type", p.getType(), is(GroupType.ORGANIZATION));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.withDescription("    my desc   ")
				.withType(GroupType.TEAM)
				.build();
		
		assertThat("incorrect id", p.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect name", p.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect desc", p.getDescription(), is(Optional.of("my desc")));
		assertThat("incorrect type", p.getType(), is(GroupType.TEAM));
	}

	@Test
	public void buildWithEmptyDescription() throws Exception {
		buildWithEmptyDescription(null);
		buildWithEmptyDescription("   \t     ");
	}
	
	private void buildWithEmptyDescription(final String description) throws Exception {
		final GroupCreationParams g = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.withDescription(description)
				.build();

		assertThat("incorrect desc", g.getDescription(), is(Optional.absent()));
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final GroupID i = new GroupID("i");
		final GroupName n = new GroupName("n");
		
		failGetBuilder(null, n, new NullPointerException("id"));
		failGetBuilder(i, null, new NullPointerException("name"));
	}
	
	private void failGetBuilder(
			final GroupID id,
			final GroupName name,
			final Exception expected) {
		try {
			GroupCreationParams.getBuilder(id, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withTypeFail() throws Exception {
		final Builder b = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"));
		
		try {
			b.withType(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("type"));
		}
	}
	
	@Test
	public void withDescriptionFail() throws Exception {
		final String uni = "a₸𐍆"; // length 5, 4 code points, 11 bytes in utf-8
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 1250; i++) {
			b.append(uni);
		}
		
		final Builder build = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"));
		
		build.withDescription(b.toString()); // should pass
		
		b.append("a");
		
		try {
			build.withDescription(b.toString());
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalParameterException(
					"description size greater than limit 5000"));
		}
	}
	
	@Test
	public void toGroup() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.build();
		
		final Group g = p.toGroup(new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)));
		
		final Group expected = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.build();
		
		assertThat("incorrect group", g, is(expected));
	}
}
