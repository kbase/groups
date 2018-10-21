package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.UserName;
import us.kbase.test.groups.TestCommon;

public class GroupTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(Group.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", g.getDescription(), is(Optional.absent()));
		assertThat("incorrect name", g.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect member", g.getMembers(), is(set()));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect type", g.getType(), is(GroupType.ORGANIZATION));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withDescription("    \tmy desc     ")
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.withType(GroupType.PROJECT)
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", g.getDescription(), is(Optional.of("my desc")));
		assertThat("incorrect name", g.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect member", g.getMembers(),
				is(set(new UserName("bar"), new UserName("baz"))));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect type", g.getType(), is(GroupType.PROJECT));
	}
	
	@Test
	public void buildWithEmptyDescription() throws Exception {
		buildWithEmptyDescription(null);
		buildWithEmptyDescription("   \t     ");
	}
	
	private void buildWithEmptyDescription(final String description) throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withDescription(description)
				.build();

		assertThat("incorrect desc", g.getDescription(), is(Optional.absent()));
	}

	@Test
	public void immutable() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("bar"))
				.build();
		
		try {
			g.getMembers().add(new UserName("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final GroupID i = new GroupID("i");
		final GroupName n = new GroupName("n");
		final UserName o = new UserName("o");
		final CreateAndModTimes t = new CreateAndModTimes(Instant.ofEpochMilli(10000));
		
		failGetBuilder(null, n, o, t, new NullPointerException("id"));
		failGetBuilder(i, null, o, t, new NullPointerException("name"));
		failGetBuilder(i, n, null, t, new NullPointerException("owner"));
		failGetBuilder(i, n, o, null, new NullPointerException("times"));
	}
	
	private void failGetBuilder(
			final GroupID id,
			final GroupName name,
			final UserName owner,
			final CreateAndModTimes times,
			final Exception expected) {
		try {
			Group.getBuilder(id, name, owner, times);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withMemberFail() throws Exception {
		final Builder b = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		
		try {
			b.withMember(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("member"));
		}
	}
	
	@Test
	public void withTypeFail() throws Exception {
		final Builder b = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		
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
		
		final Builder build = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		
		build.withDescription(b.toString()); // should pass
		
		b.append("a");
		
		try {
			build.withDescription(b.toString());
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"description must be <= 5000 Unicode code points"));
		}
	}
	
	@Test
	public void isAdministator() throws Exception {
		//TODO ADMINS update when admins are implemented
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("bar")), is(false));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("foo")), is(true));
	}
	
	@Test
	public void failIsAdministrator() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		try {
			g.isAdministrator(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("user"));
		}
	}
	
	@Test
	public void isMember() throws Exception {
		//TODO ADMINS update when admins are implemented
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.build();
				
		assertThat("incorrect isMember", g.isMember(new UserName("bat")), is(false));
		assertThat("incorrect isMember", g.isMember(new UserName("foo")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("bar")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("baz")), is(true));
	}
	
	
	@Test
	public void failIsMember() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		try {
			g.isMember(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("user"));
		}
	}
	
	@Test
	public void withoutPrivateFields() throws Exception {
		//TODO WORKSPACE update when workspaces are implemented
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withDescription("    \tmy desc     ")
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.withType(GroupType.PROJECT)
				.build();
		
		final Group gprivate = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withDescription("    \tmy desc     ")
				.withType(GroupType.PROJECT)
				.build();
		
		assertThat("incorrect priv", g.withoutPrivateFields(), is(gprivate));
	}
}
