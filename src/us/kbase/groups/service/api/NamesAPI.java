package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.service.api.APICommon.toGroupIDs;
import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import us.kbase.groups.core.GroupIDNameMembership;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.NAMES)
public class NamesAPI {

	// TODO JAVADOC / swagger
	
	private final Groups groups;
	
	// normally instantiated by Jersey
	@Inject
	public NamesAPI(final Groups groups) {
		this.groups = groups;
	}
	
	@GET
	@Path(ServicePaths.NAMES_BULK)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, String>> getGroupNames(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.IDS) final String ids)
			throws GroupsStorageException, IllegalParameterException, NoTokenProvidedException,
				InvalidTokenException, AuthenticationException, NoSuchGroupException {
		// ids cannot be null or empty, otherwise the endpoint hit would be /names/
		return groups.getGroupNames(getToken(token, false), toGroupIDs(ids)).stream()
				.map(g -> toMap(g))
				.collect(Collectors.toList());
	}

	private Map<String, String> toMap(final GroupIDNameMembership g) {
		final Map<String, String> ret = new HashMap<>();
		ret.put(Fields.GROUP_ID, g.getID().getName());
		ret.put(Fields.GROUP_NAME, g.getName().map(n -> n.getName()).orElse(null));
		return ret;
	}	
}
