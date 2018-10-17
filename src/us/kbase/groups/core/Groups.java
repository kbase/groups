package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import us.kbase.common.exceptions.UnimplementedException;
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
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
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
	private final Notifications notifications;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 * @param notifications where notification should be sent.
	 */
	public Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final Notifications notifications) {
		this(storage, userHandler, notifications, Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final Notifications notifications,
			final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		this.storage = storage;
		this.userHandler = userHandler;
		this.notifications = notifications;
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
				NoSuchGroupException, UserIsMemberException, RequestExistsException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		final UserName user = userHandler.getUser(userToken);
		//TODO NOW pass in UUID factory for mocking purposes
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
		storage.storeRequest(request);
		notifications.notify(g.getAdministrators(), g, request);
		return request;
	}
	
	//TODO NOW for all requests methods, check if request is expired. If it is, expire it in the DB and possibly re-search to get new requests.
	
	public GroupRequestWithActions getRequest(final Token userToken, final UUID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(request);
		return getAuthorizedActions(user, request, g);
	}
	
	private final Set<GroupRequestUserAction> CREATOR_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.CANCEL));
	private final Set<GroupRequestUserAction> TARGET_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));

	private GroupRequestWithActions getAuthorizedActions(
			final UserName user,
			final GroupRequest request,
			final Group group)
			throws UnauthorizedException {
		//TODO NOW handle case where user is workspace admin for request against workspace
		if (user.equals(request.getRequester())) {
			return new GroupRequestWithActions(request, CREATOR_ACTIONS);
		}
		if (user.equals(request.getTarget().orNull()) || group.isAdministrator(user)) {
			return new GroupRequestWithActions(request, TARGET_ACTIONS);
		} else {
			throw new UnauthorizedException(String.format("User %s cannot access request %s",
					user.getName(), request.getID().toString()));
		}
	}
	
	//TODO CODE allow getting closed requests
	public Set<GroupRequest> getRequestsForRequester(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user, GroupRequestStatusType.OPEN);
	}
	
	//TODO CODE allow getting closed requests
	public Set<GroupRequest> getRequestsForTarget(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByTarget(user, GroupRequestStatusType.OPEN);
	}
	
	public Set<GroupRequest> getRequestsForGroupID(final Token userToken, final GroupID groupID)
			throws UnauthorizedException, InvalidTokenException, AuthenticationException,
				NoSuchGroupException, GroupsStorageException {
		// TODO NOW specify request types in search - just excluding target in mongo storage class won't work.
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format(
					"User %s cannot view requests for group %s",
					user.getName(), groupID.getName()));
		}
		return storage.getRequestsByGroupID(groupID, GroupRequestStatusType.OPEN);
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
	
	private void addMemberToKnownGoodGroup(final GroupID groupID, final UserName newMember)
			throws GroupsStorageException {
		try {
			storage.addMember(groupID, newMember);
		} catch (NoSuchGroupException e) {
			// shouldn't happen
			throw new RuntimeException(String.format("Group %s doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		}
	}
	
	public GroupRequest cancelRequest(final Token userToken, final UUID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest gr = storage.getRequest(requestID);
		if (!gr.getRequester().equals(user)) {
			throw new UnauthorizedException(String.format("User %s may not cancel request %s",
					user.getName(), requestID.toString()));
		}
		storage.closeRequest(requestID, GroupRequestStatus.canceled(), clock.instant());
		notifications.cancel(requestID);
		return storage.getRequest(requestID);
	}
	
	public GroupRequest denyRequest(
			final Token userToken,
			final UUID requestID,
			final String reason)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureCanAcceptOrDeny(request, group, user, false);
		
		storage.closeRequest(requestID, GroupRequestStatus.denied(user, reason), clock.instant());
		//TODO NOW who should get notified?
		notifications.deny(new HashSet<>(), request, user);
		
		return storage.getRequest(requestID);
	}
	
	public GroupRequest acceptRequest(
			final Token userToken,
			final UUID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureCanAcceptOrDeny(request, group, user, true);
		if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
			processAcceptGroupMembershipRequest(request, user, group);
		} else {
			throw new UnimplementedException();
		}
		return storage.getRequest(requestID);
	}

	// assumes group exists
	private void processAcceptGroupMembershipRequest(
			final GroupRequest request,
			final UserName acceptedBy,
			final Group group)
			throws GroupsStorageException, NoSuchRequestException {
		addMemberToKnownGoodGroup(group.getGroupID(), request.getRequester());
		storage.closeRequest(
				request.getID(), GroupRequestStatus.accepted(acceptedBy), clock.instant());
		final Set<UserName> targets = new HashSet<>(group.getAdministrators());
		targets.add(request.getRequester());
		targets.remove(acceptedBy);
		notifications.accept(targets, request, acceptedBy);
	}
	
	private void ensureCanAcceptOrDeny(
			final GroupRequest request,
			final Group group,
			final UserName user,
			final boolean accept)
			throws UnauthorizedException {
		//TODO WORKSPACE will need to handle workspace based auth for requests aimed at workspaces
		if (user.equals(request.getTarget().orNull())) {
			return;
		} else if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP) &&
				group.isAdministrator(user)) {
			return;
		} else {
			throw new UnauthorizedException(String.format("User %s may not %s request %s",
					user.getName(), accept ? "accept" : "deny", request.getID().toString()));
		}
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
