package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;
import static us.kbase.test.groups.TestCommon.inst;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.Group.Role;
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
		assertThat("incorrect is private", g.isPrivate(), is(false));
		assertThat("incorrect member private", g.isPrivateMemberList(), is(true));
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
				.withIsPrivate(true)
				.withPrivateMemberList(false)
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
						new ResourceDescriptor(new ResourceID("3")),
						inst(25000))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceID("foo.bar")), null)
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
		assertThat("incorrect is private", g.isPrivate(), is(true));
		assertThat("incorrect member private", g.isPrivateMemberList(), is(false));
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
		assertThat("incorrect res", g.getResourceAddDate(
				new ResourceType("workspace"), new ResourceID("1")), is(Optional.empty()));
		assertThat("incorrect res", g.getResourceAddDate(
				new ResourceType("workspace"), new ResourceID("3")), is(Optional.of(inst(25000))));
		assertThat("incorrect res", g.getResources(new ResourceType("catalogmethod")),
				is(set(new ResourceDescriptor(new ResourceID("foo.bar")),
						new ResourceDescriptor(new ResourceAdministrativeID("baz"),
								new ResourceID("baz.bat")))));
		assertThat("incorrect res", g.getResourceAddDate(
				new ResourceType("catalogmethod"), new ResourceID("foo.bar")),
				is(Optional.empty()));
		assertThat("incorrect res", g.getResourceAddDate(
				new ResourceType("catalogmethod"), new ResourceID("baz.bat")),
				is(Optional.empty()));
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
		try {
			b.withResource(type, desc, null);
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
	public void getRole() throws Exception {
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
		
		assertThat("incorrect role", g.getRole(new UserName("nonmember")), is(Role.NONE));
		assertThat("incorrect role", g.getRole(new UserName("member")), is(Role.MEMBER));
		assertThat("incorrect role", g.getRole(new UserName("admin1")), is(Role.ADMIN));
		assertThat("incorrect role", g.getRole(new UserName("foo")), is(Role.OWNER));
	}
	
	@Test
	public void roleRepresentation() throws Exception {
		for (final String enumRep: Arrays.asList(
				"NONE/None",
				"MEMBER/Member",
				"ADMIN/Admin",
				"OWNER/Owner"
				)) {
			final String[] split = enumRep.split("/");
			assertThat("incorrect rep",
					Role.valueOf(split[0]).getRepresentation(), is(split[1]));
		}
	}
	
	@Test
	public void roleValues() {
		assertThat("incorrect values",
				new HashSet<>(Arrays.asList(Role.values())),
				is(set(
						Role.NONE,
						Role.MEMBER,
						Role.ADMIN,
						Role.OWNER)));
	}
	
	@Test
	public void roleGetFromRepresentation() throws Exception {
		assertThat("incorrect role", Role.fromRepresentation("None"), is(Role.NONE));
		assertThat("incorrect role", Role.fromRepresentation("Member"), is(Role.MEMBER));
		assertThat("incorrect role", Role.fromRepresentation("Admin"), is(Role.ADMIN));
		assertThat("incorrect role", Role.fromRepresentation("Owner"), is(Role.OWNER));
	}

	@Test
	public void roleGetFromRepresentationFail() throws Exception {
		roleGetFromRepresentationFail(null, new IllegalArgumentException("Invalid role: null"));
		roleGetFromRepresentationFail("   \t  ", new IllegalArgumentException(
				"Invalid role:    \t  "));
		roleGetFromRepresentationFail("foo", new IllegalArgumentException("Invalid role: foo"));
	}
	
	private void roleGetFromRepresentationFail(final String rep, final Exception expected) {
		try {
			Role.fromRepresentation(rep);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void failGetRole() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();

		try {
			g.getRole(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("userName"));
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
				.withResource(new ResourceType("othertype"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")),
						inst(32000))
				
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
	public void getResourceAddDate() throws Exception {
		final Group g = getBuilderWithResource();
		
		assertThat("incorrect get res",
				g.getResourceAddDate(new ResourceType("foo"), new ResourceID("b")),
				is(Optional.empty()));
		assertThat("incorrect get res",
				g.getResourceAddDate(new ResourceType("othertype"), new ResourceID("b")),
				is(Optional.of(inst(32000))));
	}
	
	@Test
	public void getResourceAlsoAddDateFail() throws Exception {
		final ResourceType t = new ResourceType("t");
		
		getResourceAlsoAddDateFail(null, new ResourceID("i"), new NullPointerException("type"));
		getResourceAlsoAddDateFail(t, (ResourceID) null, new NullPointerException("resourceID"));
		
		getResourceAlsoAddDateFail(new ResourceType("foo"), new ResourceID("a"),
				new IllegalArgumentException("No such resource foo a"));
		getResourceAlsoAddDateFail(new ResourceType("foob"), new ResourceID("b"),
				new IllegalArgumentException("No such resource foob b"));
	}
	
	private void getResourceAlsoAddDateFail(
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
		try {
			g.getResourceAddDate(t, d);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void removeResourcesEmpty() throws Exception {
		final Group g = getGroupForRemoveResources();
		
		final Group gnew = g.removeResources(new ResourceType("foo"), set());
		
		final Group original = getGroupForRemoveResources();
		
		assertThat("incorrect remove resources", gnew, is(original));
		assertThat("original group unchanged", g, is(original));
	}

	private Group getGroupForRemoveResources() throws Exception {
		return Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")),
						inst(36000))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("c"),
						new ResourceID("d")),
						inst(27000))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("e"),
						new ResourceID("f")))
				.withResource(new ResourceType("bar"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")),
						null)
				.build();
	}
	
	@Test
	public void removeResources() throws Exception {
		final Group g = getGroupForRemoveResources();
		
		final Group gnew = g.removeResources(new ResourceType("foo"),
				set(new ResourceID("b"), new ResourceID("f")));
		
		final Group expected = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("c"),
						new ResourceID("d")),
						inst(27000))
				.withResource(new ResourceType("bar"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.build();
		assertThat("incorrect remove resources", gnew, is(expected));
		
		final Group original = getGroupForRemoveResources();
		
		assertThat("original group unchanged", g, is(original));
	}
	
	@Test
	public void removeResourcesAndType() throws Exception {
		final Group g = getGroupForRemoveResources();
		
		final Group gnew = g.removeResources(new ResourceType("foo"),
				set(new ResourceID("b"), new ResourceID("f"), new ResourceID("d")));
		
		final Group expected = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("bar"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.build();
		assertThat("incorrect remove resources", gnew, is(expected));
		
		final Group original = getGroupForRemoveResources();
		
		assertThat("original group unchanged", g, is(original));
	}
	
	@Test
	public void removeResourcesFailNulls() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		final ResourceType t = new ResourceType("t");

		removeResourcesFail(g, null, set(), new NullPointerException("type"));
		removeResourcesFail(g, t, null, new NullPointerException("resources"));
		removeResourcesFail(g, t, set(new ResourceID("i"), null),
				new NullPointerException("Null item in collection resources"));
	}
	
	@Test
	public void removeResourcesFailNoType() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.build();

		removeResourcesFail(g, new ResourceType("bar"), set(),
				new IllegalArgumentException("No such resource type bar"));
	}
	
	@Test
	public void removeResourcesFailNoResource() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(20000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("a"),
						new ResourceID("b")))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(
						new ResourceAdministrativeID("d"),
						new ResourceID("d")))
				.build();

		removeResourcesFail(g, new ResourceType("foo"),
				set(new ResourceID("b"), new ResourceID("c")),
				new IllegalArgumentException("No such resource foo c"));
	}
	
	private void removeResourcesFail(
			final Group g,
			final ResourceType t,
			final Set<ResourceID> r,
			final Exception expected) {
		try {
			g.removeResources(t, r);
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
