package us.kbase.test.groups.core.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.test.groups.TestCommon;

public class ResourceIDTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(ResourceID.class).usingGetClass().verify();
	}
	
	@Test
	public void equalsAdmin() throws Exception {
		EqualsVerifier.forClass(ResourceAdministrativeID.class).usingGetClass().verify();
	}
	
	@Test
	public void equalsDescriptor() throws Exception {
		EqualsVerifier.forClass(ResourceDescriptor.class).usingGetClass().verify();
	}
	
	@Test
	public void constructor() throws Exception {
		final ResourceID r = new ResourceID("    " + TestCommon.LONG1001.substring(0, 251) +
				"foooΔ   ");
		assertThat("incorrect groupname", r.getName(), is(TestCommon.LONG1001.substring(0, 251) +
				"foooΔ"));
		assertThat("incorrect toString", r.toString(), is("ResourceID [name=" +
				TestCommon.LONG1001.substring(0, 251) + "foooΔ]"));
	}
	
	@Test
	public void constructorAdmin() throws Exception {
		final ResourceAdministrativeID r = new ResourceAdministrativeID(
				"    " + TestCommon.LONG1001.substring(0, 251) + "foooΔ   ");
		assertThat("incorrect groupname", r.getName(), is(TestCommon.LONG1001.substring(0, 251) +
				"foooΔ"));
		assertThat("incorrect toString", r.toString(),
				is("ResourceAdministrativeID [name=" + TestCommon.LONG1001.substring(0, 251) +
						"foooΔ]"));
	}
	
	@Test
	public void adminFrom() throws Exception {
		final ResourceAdministrativeID r = ResourceAdministrativeID.from(34);
		assertThat("incorrect groupname", r.getName(), is("34"));
		assertThat("incorrect toString", r.toString(),
				is("ResourceAdministrativeID [name=34]"));
		
		final ResourceAdministrativeID r2 = ResourceAdministrativeID.from(82L);
		assertThat("incorrect groupname", r2.getName(), is("82"));
		assertThat("incorrect toString", r2.toString(),
				is("ResourceAdministrativeID [name=82]"));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("resource ID"));
		failConstruct("   \n  ", new MissingParameterException("resource ID"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"resource ID contains control characters"));
		failConstruct(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"resource ID size greater than limit 256"));
	}

	private void failConstruct(final String r, final Exception exception) {
		try {
			new ResourceID(r);
			fail("expected exception");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
	@Test
	public void constructAdminFail() throws Exception {
		failConstructAdmin(null, new MissingParameterException("administrative resource ID"));
		failConstructAdmin("   \n  ", new MissingParameterException("administrative resource ID"));
		failConstructAdmin("    fo\no\boΔ\n", new IllegalParameterException(
				"administrative resource ID contains control characters"));
		failConstructAdmin(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"administrative resource ID size greater than limit 256"));
	}

	private void failConstructAdmin(final String r, final Exception exception) {
		try {
			new ResourceAdministrativeID(r);
			fail("expected exception");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
	@Test
	public void constructDescriptor() throws Exception {
		final ResourceDescriptor d = new ResourceDescriptor(
				new ResourceAdministrativeID("aid"), new ResourceID("rid"));
		
		assertThat("incorrect aid", d.getAdministrativeID(),
				is(new ResourceAdministrativeID("aid")));
		assertThat("incorrect rid", d.getResourceID(),
				is(new ResourceID("rid")));
		
		final ResourceDescriptor d2 = new ResourceDescriptor(new ResourceID("rid"));
		
		assertThat("incorrect aid", d2.getAdministrativeID(),
				is(new ResourceAdministrativeID("rid")));
		assertThat("incorrect rid", d2.getResourceID(),
				is(new ResourceID("rid")));
	}

	@Test
	public void constructDescriptorFail() throws Exception {
		failConstructDescriptor(null, new ResourceID("f"),
				new NullPointerException("administrativeID"));
		failConstructDescriptor(new ResourceAdministrativeID("a"), null,
				new NullPointerException("resourceID"));
	}
	
	private void failConstructDescriptor(
			final ResourceAdministrativeID aid,
			final ResourceID rid,
			final Exception expected) {
		try {
			new ResourceDescriptor(aid, rid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void descriptorFrom() throws Exception {
		final ResourceDescriptor d = ResourceDescriptor.from(new UserName("foo"));
		assertThat("incorrect aid", d.getAdministrativeID(),
				is(new ResourceAdministrativeID("foo")));
		assertThat("incorrect rid", d.getResourceID(), is(new ResourceID("foo")));
	}
	
	@Test
	public void descriptorFromFail() throws Exception {
		try {
			ResourceDescriptor.from(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("user"));
		}
	}

}
