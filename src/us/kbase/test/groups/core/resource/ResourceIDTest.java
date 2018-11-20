package us.kbase.test.groups.core.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
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
		final ResourceID r = new ResourceID("    foooΔ   ");
		assertThat("incorrect groupname", r.getName(), is("foooΔ"));
		assertThat("incorrect toString", r.toString(), is("ResourceID [name=foooΔ]"));
	}
	
	@Test
	public void constructorAdmin() throws Exception {
		final ResourceAdministrativeID r = new ResourceAdministrativeID("    foooΔ   ");
		assertThat("incorrect groupname", r.getName(), is("foooΔ"));
		assertThat("incorrect toString", r.toString(),
				is("ResourceAdministrativeID [name=foooΔ]"));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("resource ID"));
		failConstruct("   \n  ", new MissingParameterException("resource ID"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"resource ID contains control characters"));
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

}
