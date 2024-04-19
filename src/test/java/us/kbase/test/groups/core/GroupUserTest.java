package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.groups.TestCommon.inst;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.test.groups.TestCommon;
import us.kbase.groups.core.GroupUser;

public class GroupUserTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupUser.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final GroupUser g = GroupUser.getBuilder(new UserName("n"), inst(5000)).build();
		
		assertThat("incorrect name", g.getName(), is(new UserName("n")));
		assertThat("incorrect date", g.getJoinDate(), is(inst(5000)));
		assertThat("incorrect vist", g.getLastVisit(), is(Optional.empty()));
		assertThat("incorrect fields", g.getCustomFields(), is(Collections.emptyMap()));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final GroupUser g = GroupUser.getBuilder(new UserName("n2"), inst(15000))
				.withCustomField(new NumberedCustomField("f-22"), "yay")
				.withCustomField(new NumberedCustomField("f-35"), "boo")
				.withNullableLastVisit(inst(16000))
				.build();
		
		assertThat("incorrect name", g.getName(), is(new UserName("n2")));
		assertThat("incorrect date", g.getJoinDate(), is(inst(15000)));
		assertThat("incorrect vist", g.getLastVisit(), is(Optional.of(inst(16000))));
		assertThat("incorrect fields", g.getCustomFields(), is(ImmutableMap.of(
				new NumberedCustomField("f-22"), "yay", new NumberedCustomField("f-35"), "boo")));
	}
	
	@Test
	public void buildResetVisit() throws Exception {
		final GroupUser g = GroupUser.getBuilder(new UserName("n2"), inst(15000))
				.withNullableLastVisit(inst(16000))
				.withNullableLastVisit(null)
				.build();
		
		assertThat("incorrect name", g.getName(), is(new UserName("n2")));
		assertThat("incorrect date", g.getJoinDate(), is(inst(15000)));
		assertThat("incorrect vist", g.getLastVisit(), is(Optional.empty()));
		assertThat("incorrect fields", g.getCustomFields(), is(Collections.emptyMap()));
	}
	
	@Test
	public void immutable() throws Exception {
		final GroupUser g = GroupUser.getBuilder(new UserName("n2"), inst(15000))
				.withCustomField(new NumberedCustomField("f-22"), "yay")
				.withCustomField(new NumberedCustomField("f-35"), "boo")
				.build();
		try {
			g.getCustomFields().put(new NumberedCustomField("f"), "v");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// pass
		}
	}
	
	@Test
	public void getBuilderFailNulls() throws Exception {
		getBuilderFail(null, inst(1), new NullPointerException("name"));
		getBuilderFail(new UserName("n"), null, new NullPointerException("joinDate"));
	}
	
	private void getBuilderFail(final UserName u, final Instant i, final Exception expected) {
		try {
			GroupUser.getBuilder(u, i);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withCustomFieldFailBadArgs() throws Exception {
		final NumberedCustomField f = new NumberedCustomField("f");
		withCustomFieldFail(null, "v", new NullPointerException("field"));
		withCustomFieldFail(f, null, new IllegalArgumentException(
				"value cannot be null or whitespace only"));
		withCustomFieldFail(f, "   \t   ", new IllegalArgumentException(
				"value cannot be null or whitespace only"));
	}
	
	private void withCustomFieldFail(
			final NumberedCustomField f,
			final String v,
			final Exception expected) {
		try {
			GroupUser.getBuilder(new UserName("n"), inst(1)).withCustomField(f, v);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
