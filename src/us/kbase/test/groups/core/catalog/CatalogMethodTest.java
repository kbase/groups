package us.kbase.test.groups.core.catalog;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.catalog.CatalogModule;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.test.groups.TestCommon;

public class CatalogMethodTest {

	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(CatalogMethod.class).usingGetClass().verify();
	}
	
	@Test
	public void constructSingleArg() throws Exception {
		final CatalogMethod cm = new CatalogMethod("     mod    .     meth     ");
		
		assertThat("incorrect module", cm.getModule(), is(new CatalogModule("mod")));
		assertThat("incorrect method", cm.getMethod(), is("meth"));
	}
	
	@Test
	public void constructSingleArgFail() throws Exception {
		failConstructSingleArg(null, new MissingParameterException("module.method"));
		failConstructSingleArg("   \t    ", new MissingParameterException("module.method"));
		failConstructSingleArg("   .foo    ", new MissingParameterException("catalog module"));
		failConstructSingleArg("   foo.    ", new MissingParameterException("catalog method"));
		failConstructSingleArg("   foo    ", new IllegalParameterException(
				"Illegal catalog method name: foo"));
		failConstructSingleArg("   foo.bar.baz    ", new IllegalParameterException(
				"Illegal catalog method name: foo.bar.baz"));
		failConstructSingleArg("   fo\no.barbaz    ", new IllegalParameterException(
				"catalog module contains control characters"));
	}
	
	private void failConstructSingleArg(final String modMeth, final Exception expected) {
		try {
			new CatalogMethod(modMeth);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void constructDualArg() throws Exception {
		final CatalogMethod cm = new CatalogMethod(new CatalogModule("mod"), "   \t   meth   ");
		
		assertThat("incorrect module", cm.getModule(), is(new CatalogModule("mod")));
		assertThat("incorrect method", cm.getMethod(), is("meth"));
	}
	
	@Test
	public void constructDualArgFail() throws Exception {
		final CatalogModule c = new CatalogModule("m");
		failConstructDualArg(null, "s", new NullPointerException("module"));
		failConstructDualArg(c, null, new MissingParameterException("catalog method"));
		failConstructDualArg(c, "  \t   ", new MissingParameterException("catalog method"));
	}
	
	private void failConstructDualArg(
			final CatalogModule mod,
			final String meth,
			final Exception expected) {
		try {
			new CatalogMethod(mod, meth);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
