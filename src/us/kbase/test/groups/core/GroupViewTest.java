package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;
import static us.kbase.test.groups.TestCommon.set;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.Group.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.GroupView.GroupUserView;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class GroupViewTest {
	
	private static final Group GROUP;
	private static final Group PRIVGROUP;
	private static final Group PUBMEMBERGROUP;
	static {
		try {
			final ResourceType ws = new ResourceType("workspace");
			final ResourceType cat = new ResourceType("catalogmethod");
			final Builder b = Group.getBuilder(
					new GroupID("id"), new GroupName("name"),
					GroupUser.getBuilder(new UserName("user"), inst(10000))
							.withCustomField(new NumberedCustomField("f-1"), "val")
							.build(),
					new CreateAndModTimes(
							Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
					.withAdministrator(GroupUser.getBuilder(new UserName("a1"), inst(60000))
							.withCustomField(new NumberedCustomField("admin"), "yar")
							.withNullableLastVisit(inst(35000))
							.build())
					.withAdministrator(GroupUser.getBuilder(new UserName("a2"), inst(30000))
							.build())
					.withMember(GroupUser.getBuilder(new UserName("m1"), inst(75000))
							.withNullableLastVisit(inst(62000))
							.build())
					.withMember(GroupUser.getBuilder(new UserName("m2"), inst(84000))
							.withCustomField(new NumberedCustomField("user-6"), "yay")
							.build())
					.withResource(ws, new ResourceDescriptor(new ResourceID("45")), inst(23000))
					.withResource(ws, new ResourceDescriptor(new ResourceID("2")))
					.withResource(ws, new ResourceDescriptor(new ResourceID("7")), inst(62000))
					.withResource(cat, new ResourceDescriptor(new ResourceID("m.n")))
					.withResource(cat, new ResourceDescriptor(new ResourceID("x.y")), inst(65000))
					.withCustomField(new NumberedCustomField("field"), "val")
					.withCustomField(new NumberedCustomField("field2"), "val2");
			GROUP = b.build();
			PRIVGROUP = b.withIsPrivate(true).build();
			PUBMEMBERGROUP = b.withIsPrivate(false).withPrivateMemberList(false).build();
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests newb", e);
		}
	}
	
	public GroupUserView getGUV(
			final String username,
			final Instant joinDate,
			final Instant visitDate,
			final Map<NumberedCustomField, String> fields)
			throws Exception {
		final Constructor<GroupUserView> c = GroupView.GroupUserView.class.getDeclaredConstructor(
				UserName.class, Instant.class, Instant.class, Map.class);
		c.setAccessible(true);
		return c.newInstance(new UserName(username), joinDate, visitDate, fields);
	}
	
	private <T> Optional<T> op(T item) {
		return Optional.ofNullable(item);
	}
	
	private <T> Optional<T> mt() {
		return Optional.empty();
	}

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupView.class).usingGetClass().verify();
		EqualsVerifier.forClass(GroupView.GroupUserView.class).usingGetClass().verify();
	}
	
	@Test
	public void userViewMinimal() throws Exception {
		final GroupUserView guv = getGUV("foo", null, null, Collections.emptyMap());
		
		assertThat("incorrect name", guv.getName(), is(new UserName("foo")));
		assertThat("incorrect join", guv.getJoinDate(), is(mt()));
		assertThat("incorrect visit", guv.getLastVisit(), is(mt()));
		assertThat("incorrect fields", guv.getCustomFields(), is(Collections.emptyMap()));
	}
	
	@Test
	public void userViewMaximal() throws Exception {
		final Map<NumberedCustomField, String> fields = new HashMap<>();
		fields.put(new NumberedCustomField("f-1"), "val");
		final GroupUserView guv = getGUV("foo", inst(40000), inst(60000), fields);
		
		assertThat("incorrect name", guv.getName(), is(new UserName("foo")));
		assertThat("incorrect join", guv.getJoinDate(), is(op(inst(40000))));
		assertThat("incorrect visit", guv.getLastVisit(), is(op(inst(60000))));
		assertThat("incorrect fields", guv.getCustomFields(),
				is(ImmutableMap.of(new NumberedCustomField("f-1"), "val")));
		
		assertImmutable(guv.getCustomFields(), new NumberedCustomField("g"), "val2");
	}
	
	@Test
	public void minimalView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, null).build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(false));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.empty()));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(false));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		
		getMemberFail(gv, new UserName("user"));
		getMemberFail(gv, new UserName("a1"));
		getMemberFail(gv, new UserName("a2"));
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void minimalMemberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("m1")).build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(false));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.empty()));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(op(inst(62000))));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(ImmutableMap.of(
				new ResourceType("workspace"), 3, new ResourceType("catalogmethod"), 2)));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(false));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.MEMBER));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		
		getMemberFail(gv, new UserName("user"));
		getMemberFail(gv, new UserName("a1"));
		getMemberFail(gv, new UserName("a2"));
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void privateMinimalView() throws Exception {
		final GroupView gv = GroupView.getBuilder(PRIVGROUP, null)
				.withOverridePrivateView(false)
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(true));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.empty()));
		assertThat("incorrect privview", gv.isPrivateView(), is(true));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(mt()));
		assertThat("incorrect mod", gv.getModificationDate(), is(mt()));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(mt()));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.empty()));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(mt()));
		assertThat("incorrect view type", gv.isStandardView(), is(false));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		
		getMemberFail(gv, new UserName("user"));
		getMemberFail(gv, new UserName("a1"));
		getMemberFail(gv, new UserName("a2"));
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void overridePrivateMinimalView() throws Exception {
		final GroupView gv = GroupView.getBuilder(PRIVGROUP, null)
				.withOverridePrivateView(true)
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(true));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(true));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.empty()));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(false));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		
		getMemberFail(gv, new UserName("user"));
		getMemberFail(gv, new UserName("a1"));
		getMemberFail(gv, new UserName("a2"));
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}

	@Test
	public void nonMemberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, null)
				.withStandardView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("workspace"),
						ResourceInformationSet.getBuilder(null)
								.withResource(new ResourceID("45"))
								.withResource(new ResourceID("7"))
								.build())
				.withResource(new ResourceType("catalogmethod"),
						ResourceInformationSet.getBuilder(null)
								.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(false));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.of(true)));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set(new ResourceType("bar"),
				new ResourceType("workspace"), new ResourceType("catalogmethod"))));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("bar")),
				is(ResourceInformationSet.getBuilder(null).build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("workspace")),
				is(ResourceInformationSet.getBuilder(null)
						.withResource(new ResourceID("45"))
						.withResource(new ResourceID("7"))
						.build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("catalogmethod")),
				is(ResourceInformationSet.getBuilder(null)
						.build()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		assertThat("incorrect user", gv.getMember(new UserName("user")),
				is(getGUV("user", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a1")),
				is(getGUV("a1", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a2")),
				is(getGUV("a2", null, null, Collections.emptyMap())));
		
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void nonMemberViewPublicMembers() throws Exception {
		final GroupView gv = GroupView.getBuilder(PUBMEMBERGROUP, new UserName("nonmember"))
				.withStandardView(true)
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(false));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.of(false)));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(),
				is(set(new UserName("m1"), new UserName("m2"))));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		assertThat("incorrect user", gv.getMember(new UserName("user")),
				is(getGUV("user", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a1")),
				is(getGUV("a1", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a2")),
				is(getGUV("a2", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("m1")),
				is(getGUV("m1", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("m2")),
				is(getGUV("m2", null, null, Collections.emptyMap())));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void privateNonMemberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(PRIVGROUP, null)
				.withStandardView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("workspace"),
						ResourceInformationSet.getBuilder(null)
								.withResource(new ResourceID("7"))
								.build())
				.withResource(new ResourceType("catalogmethod"),
						ResourceInformationSet.getBuilder(null)
								.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(true));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.empty()));
		assertThat("incorrect privview", gv.isPrivateView(), is(true));
		assertThat("incorrect admins", gv.getAdministrators(), is(set()));
		assertThat("incorrect create", gv.getCreationDate(), is(mt()));
		assertThat("incorrect mod", gv.getModificationDate(), is(mt()));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(mt()));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.empty()));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(mt()));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		
		getMemberFail(gv, new UserName("user"));
		getMemberFail(gv, new UserName("a1"));
		getMemberFail(gv, new UserName("a2"));
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void overridePrivateNonMemberView() throws Exception {
		final GroupView gv = GroupView.getBuilder(PRIVGROUP, null)
				.withStandardView(true)
				.withOverridePrivateView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("workspace"),
						ResourceInformationSet.getBuilder(null)
								.withResource(new ResourceID("45"))
								.withResource(new ResourceID("7"))
								.build())
				.withResource(new ResourceType("catalogmethod"),
						ResourceInformationSet.getBuilder(null)
								.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(true));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(true));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(Optional.of(true)));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(mt()));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(), is(set()));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(Collections.emptyMap()));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect role", gv.getRole(), is(Group.Role.NONE));
		assertThat("incorrect types", gv.getResourceTypes(), is(set(new ResourceType("bar"),
				new ResourceType("workspace"), new ResourceType("catalogmethod"))));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("bar")),
				is(ResourceInformationSet.getBuilder(null).build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("workspace")),
				is(ResourceInformationSet.getBuilder(null)
						.withResource(new ResourceID("45"))
						.withResource(new ResourceID("7"))
						.build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("catalogmethod")),
				is(ResourceInformationSet.getBuilder(null)
						.build()));
		assertThat("incorrect custom", gv.getCustomFields(), is(Collections.emptyMap()));
		assertThat("incorrect user", gv.getMember(new UserName("user")),
				is(getGUV("user", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a1")),
				is(getGUV("a1", null, null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("a2")),
				is(getGUV("a2", null, null, Collections.emptyMap())));
		
		getMemberFail(gv, new UserName("m1"));
		getMemberFail(gv, new UserName("m2"));
		
		assertImmutable(gv.getAdministrators(), new UserName("u"));
		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
	}
	
	@Test
	public void memberView() throws Exception {
		memberView(GROUP, false, Optional.of(true), new UserName("a1"), Group.Role.ADMIN,
				inst(35000), inst(62000), inst(35000));
		memberView(PRIVGROUP, true, Optional.of(true), new UserName("user"), Group.Role.OWNER,
				inst(35000), inst(62000), null);
		memberView(PUBMEMBERGROUP, false, Optional.of(false), new UserName("m1"),
				Group.Role.MEMBER, null, null, inst(62000));
	}

	private void memberView(
			final Group group,
			final boolean priv,
			final Optional<Boolean> privmemb,
			final UserName user,
			final Group.Role role,
			final Instant adminInstant,
			final Instant memberInstant,
			final Instant userInstant)
			throws Exception {
		final GroupView gv = GroupView.getBuilder(group, user)
				.withStandardView(true)
				.withResourceType(new ResourceType("bar"))
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						user)
						.withResource(new ResourceID("2"))
						.withResource(new ResourceID("7"))
						.build())
				.withResource(new ResourceType("catalogmethod"), ResourceInformationSet.getBuilder(
						user)
						.withResource(new ResourceID("m.n"))
						.build())
				.build();
		
		assertThat("incorrect id", gv.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect priv", gv.isPrivate(), is(priv));
		assertThat("incorrect override priv", gv.isOverridePrivateView(), is(false));
		assertThat("incorrect priv memb", gv.isPrivateMembersList(), is(privmemb));
		assertThat("incorrect privview", gv.isPrivateView(), is(false));
		assertThat("incorrect admins", gv.getAdministrators(),
				is(set(new UserName("a1"), new UserName("a2"))));
		assertThat("incorrect create", gv.getCreationDate(), is(op(inst(10000))));
		assertThat("incorrect mod", gv.getModificationDate(), is(op(inst(20000))));
		assertThat("incorrect visit", gv.getLastVisit(), is(op(userInstant)));
		assertThat("incorrect name", gv.getGroupName(), is(op(new GroupName("name"))));
		assertThat("incorrect members", gv.getMembers(),
				is(set(new UserName("m1"), new UserName("m2"))));
		assertThat("incorrect member count", gv.getMemberCount(), is(Optional.of(5)));
		assertThat("incorrect rescount", gv.getResourceCounts(), is(ImmutableMap.of(
				new ResourceType("workspace"), 3, new ResourceType("catalogmethod"), 2)));
		assertThat("incorrect own", gv.getOwner(), is(op(new UserName("user"))));
		assertThat("incorrect view type", gv.isStandardView(), is(true));
		assertThat("incorrect role", gv.getRole(), is(role));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("bar")),
				is(ResourceInformationSet.getBuilder(user).build()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("workspace")),
				is(ResourceInformationSet.getBuilder(user)
						.withResource(new ResourceID("7"))
						.withResource(new ResourceID("2"))
						.build()));
		assertThat("incorrect add date", gv.getResourceAddDate(
				new ResourceType("workspace"), new ResourceID("7")), is(Optional.of(inst(62000))));
		assertThat("incorrect add date", gv.getResourceAddDate(
				new ResourceType("workspace"), new ResourceID("2")), is(Optional.empty()));
		assertThat("incorrect info", gv.getResourceInformation(new ResourceType("catalogmethod")),
				is(ResourceInformationSet.getBuilder(user)
						.withResource(new ResourceID("m.n"))
						.build()));
		assertThat("incorrect add date", gv.getResourceAddDate(
				new ResourceType("catalogmethod"), new ResourceID("m.n")), is(Optional.empty()));
		assertThat("incorrect custom", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val",
				new NumberedCustomField("field2"), "val2")));
		assertThat("incorrect user", gv.getMember(new UserName("user")),
				is(getGUV("user", inst(10000), null,
						ImmutableMap.of(new NumberedCustomField("f-1"), "val"))));
		assertThat("incorrect user", gv.getMember(new UserName("a1")),
				is(getGUV("a1", inst(60000), adminInstant,
						ImmutableMap.of(new NumberedCustomField("admin"), "yar"))));
		assertThat("incorrect user", gv.getMember(new UserName("a2")),
				is(getGUV("a2", inst(30000), null, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("m1")),
				is(getGUV("m1", inst(75000), memberInstant, Collections.emptyMap())));
		assertThat("incorrect user", gv.getMember(new UserName("m2")),
				is(getGUV("m2", inst(84000), null,
						ImmutableMap.of(new NumberedCustomField("user-6"), "yay"))));

		assertImmutable(gv.getMembers(), new UserName("u"));
		assertImmutable(gv.getCustomFields(), new NumberedCustomField("foo"), "bar");
		assertImmutable(gv.getResourceTypes(), new ResourceType("t"));
		assertImmutable(gv.getResourceCounts(), new ResourceType("t"), 7);
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
	public void customFieldVisibility() throws Exception {
		GroupView gv = GroupView.getBuilder(GROUP, null)
				.withPublicUserFieldDeterminer(f -> true)
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(Collections.emptyMap()));
		
		gv = GroupView.getBuilder(GROUP, null)
				.withMinimalViewFieldDeterminer(f -> f.getField().equals("field"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(Collections.emptyMap()));
		
		gv = GroupView.getBuilder(GROUP, null)
				.withMinimalViewFieldDeterminer(f -> f.getField().equals("field"))
				.withPublicFieldDeterminer(f -> f.getField().equals("field2"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(Collections.emptyMap()));
		
		gv = GroupView.getBuilder(GROUP, null)
				.withMinimalViewFieldDeterminer(f -> f.getField().equals("field"))
				.withPublicFieldDeterminer(f -> f.getField().equals("field"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		gv = GroupView.getBuilder(GROUP, null)
				.withStandardView(true)
				.withPublicFieldDeterminer(f -> f.getField().equals("field2"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field2"), "val2")));
		
		gv = GroupView.getBuilder(GROUP, new UserName("m1"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(Collections.emptyMap()));
		
		gv = GroupView.getBuilder(GROUP, new UserName("user"))
				.withMinimalViewFieldDeterminer(f -> f.getField().equals("field"))
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val")));
		
		gv = GroupView.getBuilder(GROUP, new UserName("a1"))
				.withStandardView(true)
				.build();
		assertThat("incorrect field", gv.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("field"), "val",
				new NumberedCustomField("field2"), "val2")));
	}
	
	@Test
	public void userCustomFieldVisibility() throws Exception {
		final UserName owner = new UserName("o");
		final UserName admin = new UserName("a");
		final UserName member = new UserName("m");
		final Group g = Group.getBuilder(new GroupID("i"), new GroupName("n"),
				GroupUser.getBuilder(owner, inst(1))
						.withCustomField(new NumberedCustomField("field"), "val")
						.withCustomField(new NumberedCustomField("field2"), "val2")
						.build(),
				new CreateAndModTimes(inst(1000)))
				.withMember(GroupUser.getBuilder(member, inst(1))
						.withCustomField(new NumberedCustomField("field"), "val")
						.withCustomField(new NumberedCustomField("field2"), "val2")
						.build())
				.withAdministrator(GroupUser.getBuilder(admin, inst(1))
						.withCustomField(new NumberedCustomField("field"), "val")
						.withCustomField(new NumberedCustomField("field2"), "val2")
						.build())
				.build();
		GroupView gv = GroupView.getBuilder(g, null)
				.withPublicFieldDeterminer(f -> true)
				.withMinimalViewFieldDeterminer(f -> true)
				.build();
		getMemberFail(gv, owner);
		getMemberFail(gv, admin);
		getMemberFail(gv, member);
		
		gv = GroupView.getBuilder(g, null)
				.withPublicUserFieldDeterminer(f -> f.getField().equals("field2"))
				.build();
		getMemberFail(gv, owner);
		getMemberFail(gv, admin);
		getMemberFail(gv, member);

		gv = GroupView.getBuilder(g, null)
				.withStandardView(true)
				.withPublicUserFieldDeterminer(f -> f.getField().equals("field2"))
				.build();
		assertThat("incorrect user fields", gv.getMember(owner),
				is(getGUV(owner.getName(), null, null,
						ImmutableMap.of(new NumberedCustomField("field2"), "val2"))));
		assertThat("incorrect user fields", gv.getMember(admin),
				is(getGUV(admin.getName(), null, null,
						ImmutableMap.of(new NumberedCustomField("field2"), "val2"))));
		getMemberFail(gv, member);

		gv = GroupView.getBuilder(g, new UserName("m")).build();
		getMemberFail(gv, owner);
		getMemberFail(gv, admin);
		getMemberFail(gv, member);

		gv = GroupView.getBuilder(g, new UserName("o")).build();
		getMemberFail(gv, owner);
		getMemberFail(gv, admin);
		getMemberFail(gv, member);
		
		gv = GroupView.getBuilder(g, new UserName("m"))
				.withStandardView(true)
				.build();
		assertThat("incorrect user fields", gv.getMember(owner),
				is(getGUV(owner.getName(), inst(1), null,
						ImmutableMap.of(
								new NumberedCustomField("field"), "val",
								new NumberedCustomField("field2"), "val2"))));
		assertThat("incorrect user fields", gv.getMember(admin),
				is(getGUV(admin.getName(), inst(1), null,
						ImmutableMap.of(
								new NumberedCustomField("field"), "val",
								new NumberedCustomField("field2"), "val2"))));
		assertThat("incorrect user fields", gv.getMember(member),
				is(getGUV(member.getName(), inst(1), null,
						ImmutableMap.of(
								new NumberedCustomField("field"), "val",
								new NumberedCustomField("field2"), "val2"))));
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
		final ResourceType t = new ResourceType("workspace");
		
		// nulls
		failWithResource(u, null, ResourceInformationSet.getBuilder(null).build(),
				new NullPointerException("type"));
		failWithResource(u, t, null, new NullPointerException("info"));
		
		// user
		failWithResource(u, t, ResourceInformationSet.getBuilder(null).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
		failWithResource(u, t, ResourceInformationSet.getBuilder(new UserName("v")).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
		failWithResource(null, t, ResourceInformationSet.getBuilder(new UserName("v")).build(),
				new IllegalArgumentException("User in info does not match user in builder"));
		
		// non existent resources
		failWithResource(u, t, ResourceInformationSet.getBuilder(u)
				.withNonexistentResource(new ResourceID("i"))
				.build(),
				new IllegalArgumentException("Nonexistent resources are not allowed " +
						"in the information set"));
		
		// no type
		failWithResource(u, new ResourceType("wspce"), ResourceInformationSet.getBuilder(u)
				.build(),
				new IllegalArgumentException("Resource type does not exist in group: wspce"));
		
		// no resource
		failWithResource(u, t, ResourceInformationSet.getBuilder(u)
				.withResource(new ResourceID("45"))
				.withResource(new ResourceID("7"))
				.withResource(new ResourceID("6"))
				.build(),
				new IllegalArgumentException(
						"Resource 6 of type workspace does not exist in group"));
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
	public void withPublicFieldDeterminerFail() throws Exception {
		try {
			GroupView.getBuilder(GROUP, null).withPublicFieldDeterminer(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("isPublic"));
		}
	}
	
	@Test
	public void withMinimalViewFieldDeterminerFail() throws Exception {
		try {
			GroupView.getBuilder(GROUP, null).withMinimalViewFieldDeterminer(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("isMinimalView"));
		}
	}
	
	@Test
	public void withPublicUserFieldDeterminerFail() throws Exception {
		try {
			GroupView.getBuilder(GROUP, null).withPublicUserFieldDeterminer(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("isPublic"));
		}
	}
	
	@Test
	public void getResourceInfoFail() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("u"))
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						new UserName("u"))
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

	@Test
	public void getResourceAddDateFailNulls() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("user"))
				.withStandardView(true)
				.build();
		
		getResourceAddDateFail(gv, null, new ResourceID("i"), new NullPointerException("type"));
		getResourceAddDateFail(gv, new ResourceType("t"), null,
				new NullPointerException("resourceID"));
	}
	
	@Test
	public void getResourceAddDateFailNoType() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("user"))
				.withStandardView(true)
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						new UserName("user"))
						.withResource(new ResourceID("2"))
						.build())
				.build();
		
		getResourceAddDateFail(gv, new ResourceType("wrkspce"), new ResourceID("2"),
				new IllegalArgumentException("No such resource wrkspce 2"));
	}
	
	@Test
	public void getResourceAddDateFailNoResource() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("user"))
				.withStandardView(true)
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						new UserName("user"))
						.withResource(new ResourceID("2"))
						.build())
				.build();
		
		getResourceAddDateFail(gv, new ResourceType("workspace"), new ResourceID("3"),
				new IllegalArgumentException("No such resource workspace 3"));
	}
	
	@Test
	public void getResourceAddDateFailNotMember() throws Exception {
		final GroupView gv = GroupView.getBuilder(GROUP, new UserName("notmember"))
				.withStandardView(true)
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						new UserName("notmember"))
						.withResource(new ResourceID("2"))
						.build())
				.build();
		
		getResourceAddDateFail(gv, new ResourceType("workspace"), new ResourceID("2"),
				// could make this more helpful... yagni
				new IllegalArgumentException("No such resource workspace 2"));
	}
	
	@Test
	public void getResourceAddDateFailPrivate() throws Exception {
		final GroupView gv = GroupView.getBuilder(PRIVGROUP, new UserName("notmember"))
				.withStandardView(true)
				.withResource(new ResourceType("workspace"), ResourceInformationSet.getBuilder(
						new UserName("notmember"))
						.withResource(new ResourceID("2"))
						.build())
				.build();
		
		getResourceAddDateFail(gv, new ResourceType("workspace"), new ResourceID("2"),
				// could make this more helpful... yagni
				new IllegalArgumentException("No such resource workspace 2"));
	}
	
	
	private void getResourceAddDateFail(
			final GroupView gv,
			final ResourceType type,
			final ResourceID id,
			final Exception expected) {
		try {
			gv.getResourceAddDate(type, id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getMemberFail() throws Exception {
		final GroupView.Builder b = GroupView.getBuilder(Group.getBuilder(
				new GroupID("i"), new GroupName("n"),
				GroupUser.getBuilder(new UserName("n"), inst(1)).build(),
				new CreateAndModTimes(inst(1)))
				.withAdministrator(GroupUser.getBuilder(new UserName("a"), inst(1)).build())
				.withMember(GroupUser.getBuilder(new UserName("m"), inst(1)).build())
				.build(),
				new UserName("a"));
		
		getMemberFail(b.build(), null, new IllegalArgumentException("No such member"));
		getMemberFail(b.build(), new UserName("m"),
				new IllegalArgumentException("No such member"));
		getMemberFail(b.withStandardView(true).build(), new UserName("o"),
				new IllegalArgumentException("No such member"));
	}
	
	private void getMemberFail(final GroupView view, final UserName member) {
		getMemberFail(view, member, new IllegalArgumentException("No such member"));
	}
	
	private void getMemberFail(
			final GroupView g,
			final UserName member,
			final Exception expected) {
		try {
			g.getMember(member);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}