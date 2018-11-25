package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
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
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", g.getDescription(), is(Optional.empty()));
		assertThat("incorrect name", g.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect member", g.getMembers(), is(set()));
		assertThat("incorrect admins", g.getAdministrators(), is(set()));
		assertThat("incorrect resources", g.getResourceTypes(), is(set()));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect type", g.getType(), is(GroupType.ORGANIZATION));
		assertThat("incorrect custom", g.getCustomFields(), is(Collections.emptyMap()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withDescription("    \tmy desc     ")
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.withAdministrator(new UserName("whee"))
				.withAdministrator(new UserName("whoo"))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("1")))
				.withResource(new ResourceType("workspace"),
						new ResourceDescriptor(new ResourceID("3")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceID("foo.bar")))
				.withResource(new ResourceType("catalogmethod"),
						new ResourceDescriptor(new ResourceID("baz.bat")))
				.withType(GroupType.PROJECT)
				.withCustomField(new NumberedCustomField("foo-1"), "bar")
				.withCustomField(new NumberedCustomField("baz"), "bat")
				.build();
		
		assertThat("incorrect id", g.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admin own", g.getAdministratorsAndOwner(),
				is(set(new UserName("foo"), new UserName("whee"), new UserName("whoo"))));
		assertThat("incorrect create", g.getCreationDate(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", g.getDescription(), is(Optional.of("my desc")));
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
						new ResourceDescriptor(new ResourceID("baz.bat")))));
		assertThat("incorrect mod", g.getModificationDate(), is(Instant.ofEpochMilli(20000)));
		assertThat("incorrect owner", g.getOwner(), is(new UserName("foo")));
		assertThat("incorrect type", g.getType(), is(GroupType.PROJECT));
		assertThat("incorrect custom", g.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("foo-1"), "bar", new NumberedCustomField("baz"), "bat")));
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

		assertThat("incorrect desc", g.getDescription(), is(Optional.empty()));
	}

	@Test
	public void immutable() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("bar"))
				.withAdministrator(new UserName("bat"))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(new ResourceID("b")))
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.build();
		
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
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withAdministrator(new UserName("admin"));
		
		failWithMember(b, null, new NullPointerException("member"));
		failWithMember(b, new UserName("foo"), new IllegalArgumentException(
				"Group already contains member as owner or admin"));
		failWithMember(b, new UserName("admin"), new IllegalArgumentException(
				"Group already contains member as owner or admin"));
	}
	
	private void failWithMember(final Builder b, final UserName member, final Exception expected) {
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
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("member"));
		
		failWithAdmin(b, null, new NullPointerException("admin"));
		failWithAdmin(b, new UserName("foo"), new IllegalArgumentException(
				"Group already contains member as owner or member"));
		failWithAdmin(b, new UserName("member"), new IllegalArgumentException(
				"Group already contains member as owner or member"));
	}
	
	private void failWithAdmin(final Builder b, final UserName admin, final Exception expected) {
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
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)));
		try {
			b.withResource(type, desc);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
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
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
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
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("member"))
				.withAdministrator(new UserName("admin1"))
				.withAdministrator(new UserName("admin3"))
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
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withMember(new UserName("bar"))
				.withMember(new UserName("baz"))
				.withAdministrator(new UserName("admin1"))
				.withAdministrator(new UserName("admin3"))
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
	public void getResourceFail() throws Exception {
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(new ResourceID("b")))
				.build();
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
		final Group g = Group.getBuilder(
				new GroupID("id"), new GroupName("name"), new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000)))
				.withResource(new ResourceType("foo"), new ResourceDescriptor(new ResourceID("b")))
				.build();
		
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceDescriptor(new ResourceID("b"))),
				is(true));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), null),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				null, new ResourceDescriptor(new ResourceID("b"))),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foob"), new ResourceDescriptor(new ResourceID("b"))),
				is(false));
		assertThat("incorrect contains", g.containsResource(
				new ResourceType("foo"), new ResourceDescriptor(new ResourceID("c"))),
				is(false));
		
	}
}
