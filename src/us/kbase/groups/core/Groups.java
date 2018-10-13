package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import us.kbase.groups.build.GroupsBuilder;
import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** The core class in the Groups software. 
 * @author gaprice@lbl.gov
 *
 */
public class Groups {

	//TODO JAVADOC
	//TODO TEST
	//TODO LOGGING for all actions
	
	private static final Duration REQUEST_EXPIRE_TIME = Duration.of(14, ChronoUnit.DAYS);
	private final GroupsStorage storage;
	private final UserHandler userHandler;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 */
	public Groups(final GroupsStorage storage, final UserHandler userHandler) {
		this(storage, userHandler, Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(final GroupsStorage storage, final UserHandler userHandler, final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		this.storage = storage;
		this.userHandler = userHandler;
		this.clock = clock;
	}
	
	public Group createGroup(
			final Token userToken,
			final GroupCreationParams createParams)
			throws InvalidTokenException, AuthenticationException, GroupExistsException,
				GroupsStorageException {
		checkNotNull(userToken, "userToken");
		checkNotNull(createParams, "createParams");
		final UserName owner = userHandler.getUser(userToken);
		final Instant now = clock.instant();
		storage.createGroup(createParams.toGroup(owner, new CreateAndModTimes(now)));
		
		try {
			return storage.getGroup(createParams.getGroupID());
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(
					"Just created a group and it's already gone. Something's really broken", e);
		}
	}
	
	public Group getGroup(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException {
		// TODO PRIVATE handle privacy concerns
		// TODO PRIVATE allow admins & std users to see private parts
		// for now there's no private stuff in groups so we ignore the token. Usually we'd
		// check and see if the user is a group member, and if not, remove private stuff
		final Group g = storage.getGroup(groupID);
		if (userToken != null) {
			@SuppressWarnings("unused")
			final UserName user = userHandler.getUser(userToken);
			// if not a member, remove private stuff from group
		}
		return g;
	}
	
	// this assumes the number of groups is small enough that listing them all is OK.
	// obviously if the number of groups gets > ~100k something will have to change
	public List<Group> getGroups()
			throws GroupsStorageException {
		final List<Group> groups = storage.getGroups();
		// TODO PRIVATE handle privacy concerns - remove all private stuff
		// for now no private stuff to remove
		return groups;
	}
	
	public GroupRequest requestGroupMembership(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
				NoSuchGroupException, UserIsMemberException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		final UserName user = userHandler.getUser(userToken);
		//TODO NOW pass in UUID factory for mocking purposes
		//TODO NOW check an equivalent request doesn't already exist
		final Group g = storage.getGroup(groupID);
		if (g.isMember(user)) {
			throw new UserIsMemberException(String.format(
					"User %s is already a member of group %s", user.getName(),
					g.getGroupID().getName()));
		}
		final Instant now = clock.instant();
		final GroupRequest request = GroupRequest.getBuilder(
				UUID.randomUUID(), groupID, user, CreateModAndExpireTimes.getBuilder(
						now, now.plus(REQUEST_EXPIRE_TIME)).build())
				.withRequestGroupMembership()
				.build();
		try {
			storage.storeRequest(request);
		} catch (RequestExistsException e) {
			throw new RuntimeException("This should be impossible", e);
		}
		return request;
	}
	
	//TODO NOW for all requests methods, check if request is expired. If it is, expire it in the DB and possibly re-search to get new requests.
	
	public GroupRequest getRequest(final Token userToken, final UUID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(request);
		//TODO NOW handle case where user is workspace admin for request against workspace
		if (!user.equals(request.getTarget().orNull()) &&
				!user.equals(request.getRequester()) &&
				!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format("User %s cannot access request %s",
					user.getName(), request.getID().toString()));
		}
		return request;
	}
	
	//TODO CODE allow getting closed requests
	public Set<GroupRequest> getRequestsForRequester(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user, GroupRequestStatus.OPEN);
	}
	
	//TODO CODE allow getting closed requests
	public Set<GroupRequest> getRequestsForTarget(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByTarget(user, GroupRequestStatus.OPEN);
	}
	
	public Set<GroupRequest> getRequestsForGroupID(final Token userToken, final GroupID groupID)
			throws UnauthorizedException, InvalidTokenException, AuthenticationException,
				NoSuchGroupException, GroupsStorageException {
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format(
					"User %s cannot view requests for group %s",
					user.getName(), groupID.getName()));
		}
		return storage.getRequestsByGroupID(groupID, GroupRequestStatus.OPEN);
	}

	private Group getGroupFromKnownGoodRequest(final GroupRequest request)
			throws GroupsStorageException {
		final Group g;
		try {
			g = storage.getGroup(request.getGroupID());
		} catch (NoSuchGroupException e) {
			// shouldn't happen
			throw new RuntimeException(String.format("Request %s's group doesn't exist: %s",
					request.getID().toString(), e.getMessage()), e);
		}
		return g;
	}
	
	public static void main(final String[] args) throws Exception {
		System.setProperty("KB_DEPLOYMENT_CONFIG", "./deploy.cfg");
		final Groups g = new GroupsBuilder(new GroupsConfig()).getGroups();
		
		final Token t = new Token(args[0]);
		final Group g1 = g.createGroup(
				t,
				GroupCreationParams.getBuilder(
						new GroupID("foo"), new GroupName("from Groups.java"))
						.withType(GroupType.team)
						.withDescription("desc")
						.build());
		System.out.println(g1);
		
		System.out.println(g.getGroup(t, new GroupID("foo")));
		
		g.requestGroupMembership(t, new GroupID("foo"));
	}
	
}
