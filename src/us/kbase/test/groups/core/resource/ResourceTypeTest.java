package us.kbase.test.groups.core.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.test.groups.TestCommon;

public class ResourceTypeTest {
	
	@Test
	public void construct() throws Exception {
		final ResourceType un = new ResourceType("abcdefghijklmnopqrst");
		assertThat("incorrect type", un.getName(), is("abcdefghijklmnopqrst"));
		assertThat("incorrect toString", un.toString(),
				is("ResourceType [name=abcdefghijklmnopqrst]"));
		assertThat("incorrect hashCode" , un.hashCode(), is(-948484759));
		
		final ResourceType rt2 = new ResourceType("uvwxyz");
		assertThat("incorrect type", rt2.getName(), is("uvwxyz"));
		assertThat("incorrect toString", rt2.toString(),
				is("ResourceType [name=uvwxyz]"));
		assertThat("incorrect hashCode" , rt2.hashCode(), is(-832716798));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("resource type"));
		failConstruct("   \t \n    ", new MissingParameterException("resource type"));
		failConstruct("abaeataDfoo", new IllegalParameterException(
				"Illegal character in resource type abaeataDfoo: D"));
		failConstruct("abaeataΔfoo", new IllegalParameterException(
				"Illegal character in resource type abaeataΔfoo: Δ"));
		failConstruct("abaea*tafoo", new IllegalParameterException(
				"Illegal character in resource type abaea*tafoo: *"));
		failConstruct("abaea-tafoo", new IllegalParameterException(
				"Illegal character in resource type abaea-tafoo: -"));
		failConstruct("abaea_tafoo", new IllegalParameterException(
				"Illegal character in resource type abaea_tafoo: _"));
		failConstruct("abcdefghijklmnopqrstu", new IllegalParameterException(
				"resource type size greater than limit 20"));
	}

	private void failConstruct(
			final String name,
			final Exception exception) {
		try {
			new ResourceType(name);
			fail("constructed bad name");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
}
