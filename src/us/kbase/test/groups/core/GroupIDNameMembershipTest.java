package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDNameMembership;
import us.kbase.groups.core.GroupName;
import us.kbase.test.groups.TestCommon;

public class GroupIDNameMembershipTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(GroupIDNameMembership.class).usingGetClass().verify();
	}
	
	@Test
	public void buildWithNamePrivateMember() throws Exception {
		final GroupIDNameMembership gidnm = GroupIDNameMembership.getBuilder(new GroupID("i"))
				.withGroupName(new GroupName("gname"))
				.withIsMember(true)
				.withIsPrivate(true)
				.build();
		
		assertThat("incorrect gid", gidnm.getID(), is(new GroupID("i")));
		assertThat("incorrect name", gidnm.getName(),
				is(Optional.ofNullable(new GroupName("gname"))));
		assertThat("incorrect isMember", gidnm.isMember(), is(true));
		assertThat("incorrect isPrivate", gidnm.isPrivate(), is(true));
	}

	@Test
	public void buildWithNamePublicMember() throws Exception {
		final GroupIDNameMembership gidnm = GroupIDNameMembership.getBuilder(new GroupID("i"))
				.withGroupName(new GroupName("gname"))
				.withIsMember(true)
				.withIsPrivate(false)
				.build();
		
		assertThat("incorrect gid", gidnm.getID(), is(new GroupID("i")));
		assertThat("incorrect name", gidnm.getName(),
				is(Optional.ofNullable(new GroupName("gname"))));
		assertThat("incorrect isMember", gidnm.isMember(), is(true));
		assertThat("incorrect isPrivate", gidnm.isPrivate(), is(false));
	}

	@Test
	public void buildWithNamePublicNonMember() throws Exception {
		final GroupIDNameMembership gidnm = GroupIDNameMembership.getBuilder(new GroupID("i"))
				.withGroupName(new GroupName("gname"))
				.withIsMember(false)
				.withIsPrivate(false)
				.build();
		
		assertThat("incorrect gid", gidnm.getID(), is(new GroupID("i")));
		assertThat("incorrect name", gidnm.getName(),
				is(Optional.ofNullable(new GroupName("gname"))));
		assertThat("incorrect isMember", gidnm.isMember(), is(false));
		assertThat("incorrect isPrivate", gidnm.isPrivate(), is(false));
	}

	@Test
	public void buildWithNamePrivateNonMember() throws Exception {
		final GroupIDNameMembership gidnm = GroupIDNameMembership.getBuilder(new GroupID("i"))
				.withGroupName(new GroupName("gname"))
				.withIsMember(false)
				.withIsPrivate(true)
				.build();
		
		assertThat("incorrect gid", gidnm.getID(), is(new GroupID("i")));
		assertThat("incorrect name", gidnm.getName(), is(Optional.empty()));
		assertThat("incorrect isMember", gidnm.isMember(), is(false));
		assertThat("incorrect isPrivate", gidnm.isPrivate(), is(true));
	}
	
	@Test
	public void failGetBuilder() throws Exception {
		try {
			GroupIDNameMembership.getBuilder(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("groupID"));
		}
	}
	
	@Test
	public void failWithGroupName() throws Exception {
		try {
			GroupIDNameMembership.getBuilder(new GroupID("foo")).withGroupName(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("groupName"));
		}
	}

}
