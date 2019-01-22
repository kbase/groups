package us.kbase.test.groups.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.test.groups.TestCommon;

public class CreateModExpireTimesTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(CreateAndModTimes.class).usingGetClass().verify();
	}
	
	@Test
	public void constructSingle() throws Exception {
		final CreateAndModTimes t = new CreateAndModTimes(Instant.ofEpochMilli(10000));
		
		assertThat("incorrect create", t.getCreationTime(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect mod", t.getModificationTime(), is(Instant.ofEpochMilli(10000)));
	}
	
	@Test
	public void constructDouble() throws Exception {
		final CreateAndModTimes t = new CreateAndModTimes(
				Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000));
		
		assertThat("incorrect create", t.getCreationTime(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect mod", t.getModificationTime(), is(Instant.ofEpochMilli(20000)));
	}
	
	@Test
	public void constructFail() throws Exception {
		final Instant i = Instant.ofEpochMilli(100000);
		failConstruct(null, new NullPointerException("creationTime"));
		failConstruct(i, null, new NullPointerException("modificationTime"));
		failConstruct(i, Instant.ofEpochMilli(90000), new IllegalArgumentException(
				"creation time must be before modification time"));
	}
	
	private void failConstruct(final Instant create, final Exception expected) {
		try {
			new CreateAndModTimes(create);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConstruct(final Instant create, final Instant mod, final Exception expected) {
		try {
			new CreateAndModTimes(create, mod);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void equalsExpire() throws Exception {
		EqualsVerifier.forClass(CreateModAndExpireTimes.class).usingGetClass().verify();
	}
	
	@Test
	public void buildExpireMinimal() throws Exception {
		final CreateModAndExpireTimes t = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.build();
		
		assertThat("incorrect create", t.getCreationTime(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect mod", t.getModificationTime(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect expire", t.getExpirationTime(), is(Instant.ofEpochMilli(20000)));
	}
	
	@Test
	public void buildExpireMaximal() throws Exception {
		final CreateModAndExpireTimes t = CreateModAndExpireTimes.getBuilder(
				Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000))
				.withModificationTime(Instant.ofEpochMilli(15000))
				.build();
		
		assertThat("incorrect create", t.getCreationTime(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect mod", t.getModificationTime(), is(Instant.ofEpochMilli(15000)));
		assertThat("incorrect expire", t.getExpirationTime(), is(Instant.ofEpochMilli(20000)));
	}
	
	@Test
	public void getBuilderExpireFail() throws Exception {
		final Instant i = Instant.ofEpochMilli(10000);
		
		failGetBuilder(null, i, new NullPointerException("creationTime"));
		failGetBuilder(i, null, new NullPointerException("expirationTime"));
		failGetBuilder(i, Instant.ofEpochMilli(5000), new IllegalArgumentException(
				"creation time must be before expiration time"));
	}
	
	private void failGetBuilder(
			final Instant create,
			final Instant expire,
			final Exception expected) {
		try {
			CreateModAndExpireTimes.getBuilder(create, expire);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void withModificationTimeFail() throws Exception {
		failWithModificationTime(null, new NullPointerException("modificationTime"));
		failWithModificationTime(Instant.ofEpochMilli(5000), new IllegalArgumentException(
				"creation time must be before modification time"));
	}
	
	private void failWithModificationTime(final Instant mod, final Exception expected) {
		final Instant i = Instant.ofEpochMilli(10000);
		try {
			CreateModAndExpireTimes.getBuilder(i, i).withModificationTime(mod);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
