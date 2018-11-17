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
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.groups.core.workspace.WorkspacePermission;
import us.kbase.test.groups.TestCommon;

public class GroupViewTest {
	
	private static final Group GROUP;
	private static final WorkspaceInformation WS1;
	private static final WorkspaceInformation WS2;
	private static final WorkspaceInfoSet WIS;
	static {
		try {
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
					.withWorkspace(new WorkspaceID(45))
					.withWorkspace(new WorkspaceID(2))
					.withCatalogMethod(new CatalogMethod("m.n"))
					.withCatalogMethod(new CatalogMethod("x.y"))
					.withCustomField(new NumberedCustomField("field"), "val")
					.build();
			WS1 = WorkspaceInformation.getBuilder(7, "n1")
					.withIsPublic(true)
					.withNullableNarrativeName("narr1")
					.build();
			WS2 = WorkspaceInformation.getBuilder(22, "n2").build();
			WIS = WorkspaceInfoSet.getBuilder(new UserName("foo"))
					.withNonexistentWorkspace(5)
					.withNonexistentWorkspace(8)
					.withWorkspaceInformation(WS1, WorkspacePermission.READ)
					.withWorkspaceInformation(WS2, WorkspacePermission.ADMIN)
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
		final GroupView gv = new GroupView(GROUP, WIS, ViewType.MINIMAL);
		
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
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(set(WS1, WS2)));
		assertThat("incorrect methods", gv.getCatalogMethods(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getCatalogMethods(), new CatalogMethod("m.n"));
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getWorkspaceInformation(), WS1);
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
	}

	@Test
	public void nonMemberView() throws Exception {
		final GroupView gv = new GroupView(GROUP, WIS, ViewType.NON_MEMBER);
		
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
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(set(WS1, WS2)));
		assertThat("incorrect methods", gv.getCatalogMethods(),
				is(set(new CatalogMethod("m.n"), new CatalogMethod("x.y"))));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getCatalogMethods(), new CatalogMethod("m.n"));
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getWorkspaceInformation(), WS1);
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
	}
	
	@Test
	public void memberView() throws Exception {
		final GroupView gv = new GroupView(GROUP, WIS, ViewType.MEMBER);
		
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
		assertThat("incorrect wsinfo", gv.getWorkspaceInformation(), is(set(WS1, WS2)));
		assertThat("incorrect methods", gv.getCatalogMethods(),
				is(set(new CatalogMethod("m.n"), new CatalogMethod("x.y"))));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		assertImmutable(gv.getCatalogMethods(), new CatalogMethod("m.n"));
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getWorkspaceInformation(), WS1);
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
		
		failConstruct(null, WIS, v, new NullPointerException("group"));
		failConstruct(GROUP, null, v, new NullPointerException("workspaceSet"));
		failConstruct(GROUP, WIS, null, new NullPointerException("viewType"));
	}
	
	private void failConstruct(
			final Group g,
			final WorkspaceInfoSet wis,
			final ViewType v,
			final Exception expected) {
		try {
			new GroupView(g, wis, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getPermission() throws Exception {
		final GroupView gv = new GroupView(GROUP, WIS, ViewType.MINIMAL);
		
		assertThat("incorrect admin", gv.getPermission(WS1), is(WorkspacePermission.READ));
		assertThat("incorrect admin", gv.getPermission(WS2), is(WorkspacePermission.ADMIN));
	}
	
	@Test
	public void isAdministratorFail() throws Exception {
		failIsAdministrator(null, new NullPointerException("wsInfo"));
		failIsAdministrator(WorkspaceInformation.getBuilder(86, "j").build(),
				new IllegalArgumentException("Provided workspace info not included in view"));
	}

	private void failIsAdministrator(
			final WorkspaceInformation wi,
			final Exception expected) {
		try {
			new GroupView(GROUP, WIS, ViewType.MEMBER).getPermission(wi);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}