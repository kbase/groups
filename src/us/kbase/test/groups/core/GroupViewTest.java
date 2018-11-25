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
import us.kbase.groups.core.GroupView.ViewType;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class GroupViewTest {
	
	private static final Group GROUP;
	private static final ResourceInformationSet RIS;
	private static final ResourceInformationSet CIS;
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

			RIS = ResourceInformationSet.getBuilder(new UserName("foo"))
					.withNonexistentResource(new ResourceDescriptor(new ResourceID("5")))
					.build();
			CIS = ResourceInformationSet.getBuilder(new UserName("foo"))
					.withNonexistentResource(new ResourceDescriptor(new ResourceID("a.b")))
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
		final GroupView gv = new GroupView(GROUP, RIS, CIS, ViewType.MINIMAL);
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(Optional.empty()));
		assertThat("incorrect desc", gv.getDescription(), is(Optional.empty()));
		assertThat("incorrect name", gv.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect mod", gv.getModificationDate(), is(Optional.empty()));
		assertThat("incorrect own", gv.getOwner(), is(new UserName("user")));
		assertThat("incorrect type", gv.getType(), is(GroupType.PROJECT));
		assertThat("incorrect view type", gv.getViewType(), is(ViewType.MINIMAL));
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(RIS));
		assertThat("incorrect wsinfo", gv.getCatalogInformation(), is(CIS));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
	}

	@Test
	public void nonMemberView() throws Exception {
		final GroupView gv = new GroupView(GROUP, RIS, CIS, ViewType.NON_MEMBER);
		
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
		assertThat("incorrect view type", gv.getViewType(), is(ViewType.NON_MEMBER));
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(RIS));
		assertThat("incorrect wsinfo", gv.getCatalogInformation(), is(CIS));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
	}
	
	@Test
	public void memberView() throws Exception {
		final GroupView gv = new GroupView(GROUP, RIS, CIS, ViewType.MEMBER);
		
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
		assertThat("incorrect view type", gv.getViewType(), is(ViewType.MEMBER));
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(RIS));
		assertThat("incorrect wsinfo", gv.getCatalogInformation(), is(CIS));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
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
	public void constructFail() throws Exception {
		final ViewType v = ViewType.MEMBER;
		
		failConstruct(null, RIS, CIS, v, new NullPointerException("group"));
		failConstruct(GROUP, null, CIS, v, new NullPointerException("workspaceSet"));
		failConstruct(GROUP, RIS, null, v, new NullPointerException("catalogSet"));
		failConstruct(GROUP, RIS, CIS, null, new NullPointerException("viewType"));
	}
	
	private void failConstruct(
			final Group g,
			final ResourceInformationSet wis,
			final ResourceInformationSet cis,
			final ViewType v,
			final Exception expected) {
		try {
			new GroupView(g, wis, cis, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}