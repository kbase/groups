package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import us.kbase.common.exceptions.UnimplementedException;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
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
	@SuppressWarnings("unused")
	private final WorkspaceHandler wsHandler;
	private final Notifications notifications;
	private final UUIDGenerator uuidGen;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 * @param notifications where notification should be sent.
	 */
	public Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final WorkspaceHandler wsHandler,
			final Notifications notifications) {
		this(storage, userHandler, wsHandler, notifications, new UUIDGenerator(),
				Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final WorkspaceHandler wsHandler,
			final Notifications notifications,
			final UUIDGenerator uuidGen,
			final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		checkNotNull(wsHandler, "wsHandler");
		checkNotNull(notifications, "notifications");
		this.storage = storage;
		this.userHandler = userHandler;
		this.wsHandler = wsHandler;
		this.notifications = notifications;
		this.uuidGen = uuidGen;
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
		Group g = storage.getGroup(groupID);
		if (userToken != null) {
			final UserName user = userHandler.getUser(userToken);
			if (!g.isMember(user)) {
				g = g.withoutPrivateFields();
			}
		} else {
			g = g.withoutPrivateFields();
		}
		return g;
	}
	
	// this assumes the number of groups is small enough that listing them all is OK.
	// obviously if the number of groups gets > ~100k something will have to change
	public List<Group> getGroups()
			throws GroupsStorageException {
		return storage.getGroups().stream().map(g -> g.withoutPrivateFields())
				.collect(Collectors.toList());
	}
	
	public GroupRequest requestGroupMembership(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
				NoSuchGroupException, UserIsMemberException, RequestExistsException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (g.isMember(user)) {
			throw new UserIsMemberException(String.format(
					"User %s is already a member of group %s", user.getName(),
					g.getGroupID().getName()));
		}
		final Instant now = clock.instant();
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(uuidGen.randomUUID()), groupID, user,
				CreateModAndExpireTimes.getBuilder(
						now, now.plus(REQUEST_EXPIRE_TIME)).build())
				.withRequestGroupMembership()
				.build();
		storage.storeRequest(request);
		notifications.notify(g.getAdministratorsAndOwner(), g, request);
		return request;
	}
	
	public GroupRequest inviteUserToGroup(
			final Token userToken,
			final GroupID groupID,
			final UserName newMember)
			throws InvalidTokenException, AuthenticationException, UnauthorizedException,
				UserIsMemberException, NoSuchGroupException, GroupsStorageException,
				RequestExistsException, NoSuchUserException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(newMember, "newMember");
		final UserName user = userHandler.getUser(userToken);
		if (!userHandler.isValidUser(newMember)) {
			throw new NoSuchUserException(newMember.getName());
		}
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format("User %s may not administrate group %s",
					user.getName(), groupID.getName()));
		}
		if (g.isMember(newMember)) {
			throw new UserIsMemberException(String.format(
					"User %s is already a member of group %s", newMember.getName(),
					g.getGroupID().getName()));
		}
		final Instant now = clock.instant();
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(uuidGen.randomUUID()), groupID, user,
				CreateModAndExpireTimes.getBuilder(
						now, now.plus(REQUEST_EXPIRE_TIME)).build())
				.withInviteToGroup(newMember)
				.build();
		storage.storeRequest(request);
		notifications.notify(new HashSet<>(Arrays.asList(newMember)), g, request);
		return request;
	}
	
	//TODO NOW for all requests methods, check if request is expired. If it is, expire it in the DB and possibly re-search to get new requests.
	
	public GroupRequestWithActions getRequest(final Token userToken, final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(request);
		//TODO PRIVATE may want to censor accepter/denier and deny reason here and in other methods that return a closed request
		return getAuthorizedActions(user, request, g.isAdministrator(user));
	}
	
	private final Set<GroupRequestUserAction> CREATOR_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.CANCEL));
	private final Set<GroupRequestUserAction> TARGET_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	private final Set<GroupRequestUserAction> NO_ACTIONS = Collections.emptySet();

	private GroupRequestWithActions getAuthorizedActions(
			final UserName user,
			final GroupRequest request,
			final boolean isAdmin)
			throws UnauthorizedException {
		//TODO NOW handle case where user is workspace admin for request against workspace
		final boolean isOpen = request.getStatusType().equals(GroupRequestStatusType.OPEN);
		final Set<GroupRequestUserAction> creator = isOpen ? CREATOR_ACTIONS : NO_ACTIONS;
		final Set<GroupRequestUserAction> target = isOpen ? TARGET_ACTIONS : NO_ACTIONS;
		if (user.equals(request.getRequester())) {
			return new GroupRequestWithActions(request, creator);
		}
		if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP) && isAdmin) {
			return new GroupRequestWithActions(request, target);
		}
		if (request.getType().equals(GroupRequestType.INVITE_TO_GROUP)) {
			if (user.equals(request.getTarget().orNull())) {
				return new GroupRequestWithActions(request, target);
			}
			/* this means that admins can't see this request unless they created it (same deal
			 * for the list fns). I think that's ok - the request is between a particular admin
			 * and a user. The othe admins will get a notification if it's accepted and then can
			 * remove the user.
			 * Still though, maybe some admin endpoint or this one should just return any request.
			 * Or maybe admins should be able to view and cancel each other's requests.
			 */
		}
		throw new UnauthorizedException(String.format("User %s cannot access request %s",
				user.getName(), request.getID().getID()));
	}
	
	//TODO NOW allow getting closed requests
	public List<GroupRequest> getRequestsForRequester(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user);
	}
	
	//TODO NOW allow getting closed requests
	public List<GroupRequest> getRequestsForTarget(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByTarget(user);
	}

	//TODO NOW allow getting closed requests
	// only returns requests where the group is the target
	public List<GroupRequest> getRequestsForGroup(final Token userToken, final GroupID groupID)
			throws UnauthorizedException, InvalidTokenException, AuthenticationException,
				NoSuchGroupException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format(
					"User %s cannot view requests for group %s",
					user.getName(), groupID.getName()));
		}
		return storage.getRequestsByGroup(groupID);
	}

	private Group getGroupFromKnownGoodRequest(final GroupRequest request)
			throws GroupsStorageException {
		try {
			return storage.getGroup(request.getGroupID());
		} catch (NoSuchGroupException e) {
			// shouldn't happen
			throw new RuntimeException(String.format("Request %s's group doesn't exist: %s",
					request.getID().getID(), e.getMessage()), e);
		}
	}
	
	private void addMemberToKnownGoodGroup(final GroupID groupID, final UserName newMember)
			throws GroupsStorageException, UserIsMemberException {
		try {
			storage.addMember(groupID, newMember);
		} catch (NoSuchGroupException e) {
			// shouldn't happen
			throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		}
	}
	
	public GroupRequest cancelRequest(final Token userToken, final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest gr = storage.getRequest(requestID);
		if (!gr.getRequester().equals(user)) {
			throw new UnauthorizedException(String.format("User %s may not cancel request %s",
					user.getName(), requestID.getID()));
		}
		storage.closeRequest(requestID, GroupRequestStatus.canceled(), clock.instant());
		notifications.cancel(requestID);
		return storage.getRequest(requestID);
	}
	
	public GroupRequest denyRequest(
			final Token userToken,
			final RequestID requestID,
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
			final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, UserIsMemberException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureCanAcceptOrDeny(request, group, user, true);
		if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
			processAcceptGroupMembershipRequest(request, user, group);
		} else if (request.getType().equals(GroupRequestType.INVITE_TO_GROUP)) {
			processAcceptGroupInviteRequest(request, group);
		} else {
			throw new UnimplementedException();
		}
		return storage.getRequest(requestID);
	}

	// assumes group exists
	// this and the accept membership request code is similar - DRY up a bit?
	private void processAcceptGroupInviteRequest(
			final GroupRequest request,
			final Group group)
			throws GroupsStorageException, NoSuchRequestException, UserIsMemberException {
		final UserName acceptedBy = request.getTarget().get();
		addMemberToKnownGoodGroup(group.getGroupID(), acceptedBy);
		storage.closeRequest(
				request.getID(), GroupRequestStatus.accepted(acceptedBy), clock.instant());
		notifications.accept(
				new HashSet<>(group.getAdministratorsAndOwner()), request, acceptedBy);
	}

	// assumes group exists
	private void processAcceptGroupMembershipRequest(
			final GroupRequest request,
			final UserName acceptedBy,
			final Group group)
			throws GroupsStorageException, NoSuchRequestException, UserIsMemberException {
		addMemberToKnownGoodGroup(group.getGroupID(), request.getRequester());
		storage.closeRequest(
				request.getID(), GroupRequestStatus.accepted(acceptedBy), clock.instant());
		final Set<UserName> targets = new HashSet<>(group.getAdministratorsAndOwner());
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
					user.getName(), accept ? "accept" : "deny", request.getID().getID()));
		}
	}
	
	public void removeMember(final Token userToken, final GroupID groupID, final UserName member)
			throws NoSuchGroupException, GroupsStorageException, InvalidTokenException,
				AuthenticationException, NoSuchUserException, UnauthorizedException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(member, "member");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		if (member.equals(user) || group.isAdministrator(user)) {
			storage.removeMember(groupID, member);
			//any notification here? I don't think so
		} else {
			throw new UnauthorizedException(String.format("User %s may not administrate group %s",
					user.getName(), groupID.getName()));
		}
	}
}
