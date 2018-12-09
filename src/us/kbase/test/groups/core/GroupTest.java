package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;
import static us.kbase.test.groups.TestCommon.inst;

import java.time.Instant;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class GroupTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(Group.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect name", g.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect members", g.getMembers(), is(set()));
		assertThat("incorrect admins", g.getAdministrators(), is(set()));
		assertThat("incorrect resources", g.getResourceTypes(), is(set()));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect custom", g.getCustomFields(), is(Collections.emptyMap()));
		assertThat("incorrect all members", g.getAllMembers(), is(set(new UserName("foo"))));
		assertThat("incorrect get member", g.getMember(new UserName("foo")),
				is(GroupUser.getBuilder(new UserName("foo"), inst(20000)).build()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000))
						.withCustomField(new NumberedCustomField("own"), "yep")
						.withCustomField(new NumberedCustomField("yay"), "boo")
						.build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withMember(GroupUser.getBuilder(new UserName("bar"), inst(35000)).build())
				.withMember(GroupUser.getBuilder(new UserName("baz"), inst(6000))
						.withCustomField(new NumberedCustomField("f"), "v")
						.build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whee"), inst(70000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("whoo"), inst(65000))
						.withCustomField(new NumberedCustomField("f-66"), "66")
						.withCustomField(new NumberedCustomField("f22-35"), "-42")
						.build())
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("1")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("3")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceID("foo.bar")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceAdministrativeID("bar"),
								new ResourceID("baz.bat")))
				// overwrite previous resource
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceAdministrativeID("baz"),
								new ResourceID("baz.bat")))
				.withCustomField(new NumberedCustomField("foo-1"), "bar")
				.withCustomField(new NumberedCustomField("baz"), "bat")
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"), new UserName("whee"), new UserName("whoo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect name", g.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect member", g.getMembers(),
				is(set(new UserName("bar"), new UserName("baz"))));
		assertThat("incorrect admin", g.getAdministrators(),
				is(set(new UserName("whee"), new UserName("whoo"))));
		assertThat("incorrect resources", g.getResourceTypes(),
				is(set(new ResourceType("workspace"), new ResourceType("catalogmethod"))));
		assertThat("incorrect res", g.getResources(new ResourceType("workspace")),
				is(set(new ResourceDescriptor(new ResourceID("1")),
						new ResourceDescriptor(new ResourceID("3")))));
		assertThat("incorrect res", g.getResources(new ResourceType("catalogmethod")),
				is(set(new ResourceDescriptor(new ResourceID("foo.bar")),
						new ResourceDescriptor(new ResourceAdministrativeID("baz"),
								new ResourceID("baz.bat")))));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect custom", g.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("foo-1"), "bar", new NumberedCustomField("baz"), "bat")));
		assertThat("incorrect all members", g.getAllMembers(), is(set(
				new UserName("foo"), new UserName("bar"), new UserName("baz"),
				new UserName("whee"), new UserName("whoo"))));
		assertThat("incorrect get member", g.getMember(new UserName("foo")),
				is(GroupUser.getBuilder(new UserName("foo"), inst(20000))
						.withCustomField(new NumberedCustomField("own"), "yep")
						.withCustomField(new NumberedCustomField("yay"), "boo")
						.build()));
		assertThat("incorrect get member", g.getMember(new UserName("bar")),
				is(GroupUser.getBuilder(new UserName("bar"), inst(35000)).build()));
		assertThat("incorrect get member", g.getMember(new UserName("baz")),
				is(GroupUser.getBuilder(new UserName("baz"), inst(6000))
						.withCustomField(new NumberedCustomField("f"), "v")
						.build()));
		assertThat("incorrect get member", g.getMember(new UserName("whee")),
				is(GroupUser.getBuilder(new UserName("whee"), inst(70000)).build()));
		assertThat("incorrect get member", g.getMember(new UserName("whoo")),
				is(GroupUser.getBuilder(new UserName("whoo"), inst(65000))
						.withCustomField(new NumberedCustomField("f-66"), "66")
						.withCustomField(new NumberedCustomField("f22-35"), "-42")
						.build()));
	}
	
	@Test
	public void immutable() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(GroupUser.getBuilder(new UserName("bar"), inst(20000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("bat"), inst(20000)).build())
				.withResource(new ResourceType("foo"), new ResourceDescriptor(new ResourceID("b")))
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build();
		
		try {
			g.getAllMembers().add(new UserName("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			g.getMembers().add(new UserName("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			g.getAdministrators().add(new UserName("baz"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			g.getResourceTypes().add(new ResourceType("f"));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			g.getResources(new ResourceType("foo")).add(
					new ResourceDescriptor(new ResourceID("f")));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
		
		try {
			g.getCustomFields().put(new NumberedCustomField("whoo"), "whee");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final GroupID i = new GroupID("i");
		final GroupName n = new GroupName("n");
		final GroupUser u = GroupUser.getBuilder(new UserName("foo"), inst(20000)).build();
		final CreateAndModTimes t = new CreateAndModTimes(Instant.ofEpochMilli(10000));
		
		failGetBuilder(null, n, u, t, new NullPointerException("id"));
		failGetBuilder(i, null, u, t, new NullPointerException("name"));
		failGetBuilder(i, n, null, t, new NullPointerException("owner"));
		failGetBuilder(i, n, u, null, new NullPointerException("times"));
	}
	
	private void failGetBuilder(
			final GroupID id,
			final GroupName name,
			final GroupUser owner,
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
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withAdministrator(GroupUser.getBuilder(new UserName("admin"), inst(20000))
						.build());
		
		failWithMember(b, null, new NullPointerException("member"));
		failWithMember(b, GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new IllegalArgumentException("Group already contains member foo"));
		failWithMember(b, GroupUser.getBuilder(new UserName("admin"), inst(20000)).build(),
				new IllegalArgumentException("Group already contains member admin"));
	}
	
	private void failWithMember(
			final Builder b,
			final GroupUser member,
			final Exception expected) {
		try {
			b.withMember(member);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withAdminFail() throws Exception {
		final Builder b = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(GroupUser.getBuilder(new UserName("member"), inst(20000)).build());
		
		failWithAdmin(b, null, new NullPointerException("admin"));
		failWithAdmin(b, GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new IllegalArgumentException("Group already contains member foo"));
		failWithAdmin(b, GroupUser.getBuilder(new UserName("member"), inst(20000)).build(),
				new IllegalArgumentException("Group already contains member member"));
	}
	
	private void failWithAdmin(final Builder b, final GroupUser admin, final Exception expected) {
		try {
			b.withAdministrator(admin);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withResourceFail() throws Exception {
		failWithResource(null, new ResourceDescriptor(new ResourceID("i")),
				new NullPointerException("type"));
		failWithResource(new ResourceType("t"), null,
				new NullPointerException("descriptor"));
	}
	
	private void failWithResource(
			final ResourceType type,
			final ResourceDescriptor desc,
			final Exception expected)
			throws Exception {
		final Builder b = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		try {
			b.withResource(type, desc);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withCustomFieldFailNulls() throws Exception {
		failWithCustomField(null, "v", new NullPointerException("field"));
		failWithCustomField(new NumberedCustomField("f"), null, new IllegalArgumentException(
				"value cannot be null or whitespace only"));
	}
	
	private void failWithCustomField(
			final NumberedCustomField field,
			final String value,
			final Exception expected)
			throws Exception {
		final Builder build = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		try {
			build.withCustomField(field, value);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}
	
	@Test
	public void isAdministator() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(GroupUser.getBuilder(new UserName("member"), inst(20000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("admin1"), inst(20000))
						.build())
				.withAdministrator(GroupUser.getBuilder(new UserName("admin3"), inst(20000))
						.build())
				.build();
		
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("bar")), is(false));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("member")), is(false));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("admin2")), is(false));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("foo")), is(true));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("admin1")), is(true));
		assertThat("incorrect isAdmin", g.isAdministrator(new UserName("admin3")), is(true));
	}
	
	@Test
	public void failIsAdministrator() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
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
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(GroupUser.getBuilder(new UserName("bar"), inst(20000)).build())
				.withMember(GroupUser.getBuilder(new UserName("baz"), inst(20000)).build())
				.withAdministrator(GroupUser.getBuilder(new UserName("admin1"), inst(20000))
						.build())
				.withAdministrator(GroupUser.getBuilder(new UserName("admin3"), inst(20000))
						.build())
				.build();
		
		assertThat("incorrect isMember", g.isMember(null), is(false));
		assertThat("incorrect isMember", g.isMember(new UserName("bat")), is(false));
		assertThat("incorrect isMember", g.isMember(new UserName("admin2")), is(false));
		assertThat("incorrect isMember", g.isMember(new UserName("foo")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("bar")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("baz")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("admin1")), is(true));
		assertThat("incorrect isMember", g.isMember(new UserName("admin3")), is(true));
	}
	
	@Test
	public void getResourcesFail() throws Exception {
		final Group g = getBuilderWithResource();
		try {
			g.getResources(new ResourceType("bar"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"Type not found in group: bar"));
		}
	}
	
	@Test
	public void containsResource() throws Exception {
		final Group g = getBuilderWithResource();
		
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b"))),
				is(true));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foob"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b"))),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("b"),
						new ResourceID("b"))),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("c"))),
				is(false));
		
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceID("b")),
				is(true));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foob"), new ResourceID("b")),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceID("c")),
				is(false));
		
	}

	private Group getBuilderWithResource() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.build();
		return g;
	}
	
	@Test
	public void containsResourceFail() throws Exception {
		final ResourceType t = new ResourceType("t");
		
		containsResourceFail(null, new ResourceDescriptor(new ResourceID("i")),
				new NullPointerException("type"));
		containsResourceFail(null, new ResourceID("i"), new NullPointerException("type"));
		containsResourceFail(t, (ResourceDescriptor) null, new NullPointerException("descriptor"));
		containsResourceFail(t, (ResourceID) null, new NullPointerException("resourceID"));
	}
	
	private void containsResourceFail(
			final ResourceType t,
			final ResourceDescriptor d,
			final Exception expected)
			throws Exception {
		final Group g = getBuilderWithResource();
		try {
			g.containsResource(t, d);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void containsResourceFail(
			final ResourceType t,
			final ResourceID d,
			final Exception expected)
			throws Exception {
		final Group g = getBuilderWithResource();
		try {
			g.containsResource(t, d);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getResource() throws Exception {
		final Group g = getBuilderWithResource();
		
		assertThat("incorrect get res",
				g.getResource(new ResourceType("foo"), new ResourceID("b")),
				is(new ResourceDescriptor(
						new ResourceAdministrativeID("a"), new ResourceID("b"))));
	}
	
	@Test
	public void getResourceFail() throws Exception {
		final ResourceType t = new ResourceType("t");
		
		getResourceFail(null, new ResourceID("i"), new NullPointerException("type"));
		getResourceFail(t, (ResourceID) null, new NullPointerException("resourceID"));
		
		getResourceFail(new ResourceType("foo"), new ResourceID("a"),
				new IllegalArgumentException("No such resource foo a"));
		getResourceFail(new ResourceType("foob"), new ResourceID("b"),
				new IllegalArgumentException("No such resource foob b"));
	}
	
	private void getResourceFail(
			final ResourceType t,
			final ResourceID d,
			final Exception expected)
			throws Exception {
		final Group g = getBuilderWithResource();
		try {
			g.getResource(t, d);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getMemberFail() throws Exception {
		final Group g = Group.getBuilder(new GroupID("i"), new GroupName("n"),
				GroupUser.getBuilder(new UserName("n"), inst(1)).build(),
				new CreateAndModTimes(inst(1)))
				.withAdministrator(GroupUser.getBuilder(new UserName("a"), inst(1)).build())
				.withMember(GroupUser.getBuilder(new UserName("m"), inst(1)).build())
				.build();
		
		getMemberFail(g, null, new IllegalArgumentException("No such member"));
		getMemberFail(g, new UserName("o"), new IllegalArgumentException("No such member"));
	}
	
	private void getMemberFail(final Group g, final UserName member, final Exception expected) {
		try {
			g.getMember(member);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
