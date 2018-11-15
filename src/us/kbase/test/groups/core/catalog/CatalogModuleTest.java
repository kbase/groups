package us.kbase.test.groups.core.catalog;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.catalog.CatalogModule;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class CatalogModuleTest {
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(CatalogModule.class).usingGetClass().verify();
	}
	
	@Test
	public void constructor() throws Exception {
		final CatalogModule n1 = new CatalogModule("    foooΔ   ");
		assertThat("incorrect groupname", n1.getName(), is("foooΔ"));
		assertThat("incorrect toString", n1.toString(), is("CatalogModule [name=foooΔ]"));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("catalog module"));
		failConstruct("   \n  ", new MissingParameterException("catalog module"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"catalog module contains control characters"));
	}

	private void failConstruct(final String module, final Exception exception) {
		try {
			new CatalogModule(module);
			fail("expected exception");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}

}
