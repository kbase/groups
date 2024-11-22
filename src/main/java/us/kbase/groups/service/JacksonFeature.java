package us.kbase.groups.service;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public final class JacksonFeature implements Feature {
	// see https://stackoverflow.com/a/22152612/643675

	private static final ObjectMapper MAPPER;

	static {
		MAPPER = new ObjectMapper().registerModule(new Jdk8Module());
		MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
	}
	
	@Override
	public boolean configure(final FeatureContext context) {
		final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider(
				MAPPER, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
		context.register(provider);
		return true;
	}
}