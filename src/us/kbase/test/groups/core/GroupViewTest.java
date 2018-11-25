package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.set;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class GroupViewTest {
	
	private static final Group GROUP;
	static {
		try {
			final ResourceType ws = new ResourceType("workspace");
			final ResourceType cat = new ResourceType("catalogmethod");
			GROUP = Group.getBuilder(
					new GroupID("id"), new GroupName("name"), new UserName("user"),
					new CreateAndModTimes(
							Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
					.withAdministrator(new UserName("a1"))
					.withAdministrator(new UserName("a2"))
					.withMember(new UserName("m1"))
					.withMember(new UserName("m2"))
					.withDescription("desc")
					.withType(GroupType.PROJECT)
					.withResource(ws, new ResourceDescriptor(new ResourceID("45")))
					.withResource(ws, new ResourceDescriptor(new ResourceID("2")))
					.withResource(cat, new ResourceDescriptor(new ResourceID("m.n")))
					.withResource(cat, new ResourceDescriptor(new ResourceID("x.y")))
					.withCustomField(new NumberedCustomField("field"), "val")
					.build();
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests newb", e);
		}
	}

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupView.class).usingGetClass().verify();
	}
	
	@Test
	public void minimalView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, null).build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(Optional.empty()));
		assertThat("incorrect desc", gv.getDescription(), is(Optional.empty()));
		assertThat("incorrect name", gv.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect mod", gv.getModificationDate(), is(Optional.empty()));
		assertThat("incorrect own", gv.getOwner(), is(new UserName("user")));
		assertThat("incorrect type", gv.getType(), is(GroupType.PROJECT));
		assertThat("incorrect view type", gv.isStandardView(), is(false));
		assertThat("incorrect view type", gv.isMember(), is(false));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
	}

	@Test
	public void nonMemberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, null)
				.withStandardView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("ws"), ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
						.build())
				.withResource(new ResourceType("cat"), ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("a.b")))
						.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(),
				is(Optional.of(Instant.ofEpochMilli(10000))));
		assertThat("incorrect desc", gv.getDescription(), is(Optional.of("desc")));
		assertThat("incorrect name", gv.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect mod", gv.getModificationDate(),
				is(Optional.of(Instant.ofEpochMilli(20000))));
		assertThat("incorrect own", gv.getOwner(), is(new UserName("user")));
		assertThat("incorrect type", gv.getType(), is(GroupType.PROJECT));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect view type", gv.isMember(), is(false));
		assertThat("incorrect types", gv.getResourceTypes(), is(set(new ResourceType("bar"),
				new ResourceType("ws"), new ResourceType("cat"))));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("bar")),
				is(ResourceInformationSet.getBuilder(null).build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("ws")),
				is(ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
						.build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("cat")),
				is(ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("a.b")))
						.build()));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
	}
	
	@Test
	public void memberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("m1"))
				.withStandardView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("ws"), ResourceInformationSet.getBuilder(
						new UserName("m1"))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
						.build())
				.withResource(new ResourceType("cat"), ResourceInformationSet.getBuilder(
						new UserName("m1"))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("a.b")))
						.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(),
				is(Optional.of(Instant.ofEpochMilli(10000))));
		assertThat("incorrect desc", gv.getDescription(), is(Optional.of("desc")));
		assertThat("incorrect name", gv.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect members", gv.getMembers(),
				is(set(new UserName("m1"), new UserName("m2"))));
		assertThat("incorrect mod", gv.getModificationDate(),
				is(Optional.of(Instant.ofEpochMilli(20000))));
		assertThat("incorrect own", gv.getOwner(), is(new UserName("user")));
		assertThat("incorrect type", gv.getType(), is(GroupType.PROJECT));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect view type", gv.isMember(), is(true));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("bar")),
				is(ResourceInformationSet.getBuilder(new UserName("m1")).build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("ws")),
				is(ResourceInformationSet.getBuilder(new UserName("m1"))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
						.build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("cat")),
				is(ResourceInformationSet.getBuilder(new UserName("m1"))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("a.b")))
						.build()));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
	}
	
	private <T> void assertImmutable(final Collection<T> set, final T add) {
		try {
			set.add(add);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// it's immutable
		}
	}
	
	private <K, V> void assertImmutable(final Map<K, V> map, K addKey, V addValue) {
		try {
			map.put(addKey, addValue);
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// immutable 
		}
	}

	@Test
	public void getBuilderFail() throws Exception {
		try {
			GroupView.getBuilder(null, null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("group"));
		}
	}
	
	@Test
	public void withResourceFail() throws Exception {
		final UserName u = new UserName("u");
		failWithResource(u, null, ResourceInformationSet.getBuilder(null).build(),
				new NullPointerException("type"));
		failWithResource(u, new ResourceType("t"), null, new NullPointerException("info"));
		
		failWithResource(u, new ResourceType("t"), ResourceInformationSet.getBuilder(null).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
		failWithResource(u, new ResourceType("t"),
				ResourceInformationSet.getBuilder(new UserName("v")).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
		failWithResource(null, new ResourceType("t"),
				ResourceInformationSet.getBuilder(new UserName("v")).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
	}
	
	private void failWithResource(
			final UserName buildUser,
			final ResourceType type,
			final ResourceInformationSet info,
			final Exception expected) {
		try {
			GroupView.getBuilder(GROUP, buildUser).withResource(type, info);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	} 
	
	@Test
	public void withResourceTypeFail() throws Exception {
		try {
			GroupView.getBuilder(GROUP, null).withResourceType(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("type"));
		}
	}
	
	@Test
	public void getResourceInfoFail() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("u"))
				.withResource(new ResourceType("ws"), ResourceInformationSet.getBuilder(
						new UserName("u"))
						.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
						.build())
				.build();
		try {
			gv.getResourceInformation(new ResourceType("cat"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException(
					"No such resource type cat"));
		}
		
	}
}