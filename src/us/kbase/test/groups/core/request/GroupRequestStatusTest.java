package us.kbase.test.groups.core.request;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.test.groups.TestCommon;

public class GroupRequestStatusTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(GroupRequestStatus.class).usingGetClass().verify();
	}
	
	@Test
	public void open() throws Exception {
		final GroupRequestStatus s = GroupRequestStatus.open();
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s2 = GroupRequestStatus.from(
				GroupRequestStatusType.OPEN, new UserName("n"), "foo");
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.OPEN));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.empty()));
	}
	
	@Test
	public void canceled() throws Exception {
		final GroupRequestStatus s = GroupRequestStatus.canceled();
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.CANCELED));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s2 = GroupRequestStatus.from(
				GroupRequestStatusType.CANCELED, new UserName("n"), "foo");
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.CANCELED));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.empty()));
	}
	
	@Test
	public void expired() throws Exception {
		final GroupRequestStatus s = GroupRequestStatus.expired();
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.EXPIRED));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s2 = GroupRequestStatus.from(
				GroupRequestStatusType.EXPIRED, new UserName("n"), "foo");
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.EXPIRED));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.empty()));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.empty()));
	}
	
	@Test
	public void accepted() throws Exception {
		final GroupRequestStatus s = GroupRequestStatus.accepted(new UserName("a"));
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.ACCEPTED));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.of(new UserName("a"))));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s2 = GroupRequestStatus.from(
				GroupRequestStatusType.ACCEPTED, new UserName("n"), "foo");
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.ACCEPTED));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.of(new UserName("n"))));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.empty()));
	}
	
	@Test
	public void acceptedFail() throws Exception {
		try {
			GroupRequestStatus.accepted(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("acceptedBy"));
		}
	}
	
	@Test
	public void denied() throws Exception {
		final GroupRequestStatus s = GroupRequestStatus.denied(new UserName("a"), null);
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.of(new UserName("a"))));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s2 = GroupRequestStatus.denied(new UserName("a2"), "  \t   ");
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.of(new UserName("a2"))));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s3 = GroupRequestStatus.from(
				GroupRequestStatusType.DENIED, new UserName("n"), null);
		
		assertThat("incorrect type", s3.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s3.getClosedBy(), is(Optional.of(new UserName("n"))));
		assertThat("incorrect reason", s3.getClosedReason(), is(Optional.empty()));
		
		final GroupRequestStatus s4 = GroupRequestStatus.from(
				GroupRequestStatusType.DENIED, new UserName("n"), "  \t    ");
		
		assertThat("incorrect type", s4.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s4.getClosedBy(), is(Optional.of(new UserName("n"))));
		assertThat("incorrect reason", s4.getClosedReason(), is(Optional.empty()));
	}
	
	@Test
	public void deniedWithReason() throws Exception {
		final String st = StringUtils.repeat("𐍆𐍆𐍆𐍆𐍆", 100);
		assertThat("incorrect size", st.length(), is(1000));
		assertThat("incorrect code points", st.codePointCount(0, st.length()), is(500));
		
		final GroupRequestStatus s = GroupRequestStatus.denied(
				new UserName("a"), "    " + st + "   \t  ");
		
		assertThat("incorrect type", s.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s.getClosedBy(), is(Optional.of(new UserName("a"))));
		assertThat("incorrect reason", s.getClosedReason(), is(Optional.of(st)));
		
		final GroupRequestStatus s2 = GroupRequestStatus.from(
				GroupRequestStatusType.DENIED, new UserName("n"), st);
		
		assertThat("incorrect type", s2.getStatusType(), is(GroupRequestStatusType.DENIED));
		assertThat("incorrect user", s2.getClosedBy(), is(Optional.of(new UserName("n"))));
		assertThat("incorrect reason", s2.getClosedReason(), is(Optional.of(st)));
	}
	
	@Test
	public void deniedFail() throws Exception {
		final String st = StringUtils.repeat("𐍆𐍆𐍆𐍆𐍆", 100) + "a";
		assertThat("incorrect size", st.length(), is(1001));
		assertThat("incorrect code points", st.codePointCount(0, st.length()), is(501));

		deniedFail(null, null, new NullPointerException("deniedBy"));
		deniedFail(new UserName("f"), st, new IllegalParameterException(
				"reason size greater than limit 500"));
	}
	
	private void deniedFail(
			final UserName user,
			final String reason,
			final Exception expected) {
		try {
			GroupRequestStatus.denied(user, reason);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void fromFail() throws Exception {
		failFrom(null, null, null, new NullPointerException("statusType"));
		failFrom(GroupRequestStatusType.ACCEPTED, null, null,
				new NullPointerException("closedBy"));
		failFrom(GroupRequestStatusType.DENIED, null, null,
				new NullPointerException("closedBy"));
		
		final String st = StringUtils.repeat("𐍆𐍆𐍆𐍆𐍆", 100) + "a";
		assertThat("incorrect size", st.length(), is(1001));
		assertThat("incorrect code points", st.codePointCount(0, st.length()), is(501));
		failFrom(GroupRequestStatusType.DENIED, new UserName("f"), st,
				new IllegalParameterException("reason size greater than limit 500"));
	}
	
	private void failFrom(
			final GroupRequestStatusType type,
			final UserName name,
			final String reason,
			final Exception expected) {
		try {
			GroupRequestStatus.from(type, name, reason);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}
}
