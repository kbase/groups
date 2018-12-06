package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;

import java.time.Instant;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupCreationParams.Builder;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.test.groups.TestCommon;

public class GroupCreationParamsTest {
	
	private static final OptionalGroupFields DEF_OPTS = OptionalGroupFields.getDefault();

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
		assertThat("incorrect opts", p.getOptionalFields(), is(DEF_OPTS));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.fromNullable("    my desc    "))
						.withCustomField(new NumberedCustomField("whee-1"),
								StringField.from("he bit my widdle nose"))
						.withCustomField(new NumberedCustomField("whee-2"),
								StringField.from("oh you brute"))
						.build())
				.build();
		
		assertThat("incorrect id", p.getGroupID(), is(new GroupID("id")));
		assertThat("incorrect name", p.getGroupName(), is(new GroupName("name")));
		assertThat("incorrect desc", p.getOptionalFields(), is(OptionalGroupFields.getBuilder()
				.withDescription(StringField.from("my desc"))
				.withCustomField(new NumberedCustomField("whee-1"),
						StringField.from("he bit my widdle nose"))
				.withCustomField(new NumberedCustomField("whee-2"),
						StringField.from("oh you brute"))
				.build()));
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
	public void withOptionalFieldsFail() throws Exception {
		final Builder b = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"));
		
		try {
			b.withOptionalFields(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("fields"));
		}
	}
	
	@Test
	public void toGroupMinimal() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.build();
		
		final Group g = p.toGroup(new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)));
		
		final Group expected = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(10000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.build();
		
		assertThat("incorrect group", g, is(expected));
	}
	
	@Test
	public void toGroupMaximal() throws Exception {
		final GroupCreationParams p = GroupCreationParams.getBuilder(
				new GroupID("id"), new GroupName("name"))
				.withOptionalFields(OptionalGroupFields.getBuilder()
						.withDescription(StringField.from("yay"))
						.withCustomField(new NumberedCustomField("foo"), StringField.from("bar"))
						.withCustomField(new NumberedCustomField("foo-1"), StringField.remove())
						.withCustomField(new NumberedCustomField("foo-1"), StringField.noAction())
						.build())
				.build();
		
		final Group g = p.toGroup(new UserName("foo"),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)));
		
		final Group expected = Group.getBuilder(
				new GroupID("id"), new GroupName("name"),
				GroupUser.getBuilder(new UserName("foo"), inst(10000)).build(),
				new CreateAndModTimes(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000)))
				.withCustomField(new NumberedCustomField("foo"), "bar")
				.withDescription("yay")
				.build();
		
		assertThat("incorrect group", g, is(expected));
	}
}
