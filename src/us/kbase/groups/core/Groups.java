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
	
	//TODO WS deal with deleted workspaces on lookup

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
	 * @param wsHandler the workspace handler by which information from the workspace service
	 * will be retrieved.
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
	
	/** Create a new group.
	 * @param userToken the token of the user that will be creating the group.
	 * @param createParams the paramaters describing how the group will be created.
	 * @return the new group.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupExistsException if the group already exists.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
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
	
	/** Get a group. If a token is provided and the user is a member of the group, the member
	 * list will be populated. Otherwise, it will be empty.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to get.
	 * @return the group.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
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
	/** Get all the groups in the system.
	 * @return all the groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	public List<Group> getGroups()
			throws GroupsStorageException {
		return storage.getGroups().stream().map(g -> g.withoutPrivateFields())
				.collect(Collectors.toList());
	}
	
	/** Request membership in a group.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group for which membership is desired.
	 * @return a new request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UserIsMemberException if the user is already a member of the group.
	 * @throws RequestExistsException if there's already an equivalent request in the system.
	 */
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
	
	/** Invite a user to a group. The user must be a group administrator.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to which the invitation will apply.
	 * @param newMember the user to invite to the group.
	 * @return a new request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws UnauthorizedException if the user is not a group administrator.
	 * @throws UserIsMemberException if the user is already a member of the group.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws RequestExistsException if an equivalent request already exists in the system.
	 * @throws NoSuchUserException if the user name is invalid.
	 */
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
	
	/** Get a request. A request can be viewed if the user created the request, is the target
	 * user of the request, or is an administrator of the group that is the target of the
	 * request.
	 * @param userToken the user's token.
	 * @param requestID the ID of the request.
	 * @return the request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchRequestException if there is no such request.
	 * @throws UnauthorizedException if the user may not view the request.
	 */
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
	/** Get requests that were created by the user.
	 * @param userToken the user's token.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	public List<GroupRequest> getRequestsForRequester(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user);
	}
	
	//TODO NOW allow getting closed requests
	/** Get requests where the user is the target of the request.
	 * @param userToken the user's token.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	public List<GroupRequest> getRequestsForTarget(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByTarget(user);
	}

	//TODO NOW allow getting closed requests
	/** Get requests where the group is the target of the request.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group for which requests will be returned.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws UnauthorizedException if the user is not a group admin.
	 * @throws NoSuchGroupException if the group does not exist.
	 */
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
	
	/** Cancel a request.
	 * @param userToken the user's token.
	 * @param requestID the ID of the request to cancel.
	 * @return the updated request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchRequestException if there is no such request.
	 * @throws UnauthorizedException if the user is not the creator of the request.
	 */
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
	
	/** Deny a request.
	 * @param userToken the user's token.
	 * @param requestID the ID of the request to deny.
	 * @param reason the optional reason the request was denied.
	 * @return the updated request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchRequestException if there is no such request.
	 * @throws UnauthorizedException if the user is not the target of the request or an
	 * administrator of the group targeted in the request, if a group is targeted.
	 */
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
		final GroupRequest r = storage.getRequest(requestID);
		//TODO NOW who should get notified?
		notifications.deny(new HashSet<>(), r);
		return r;
	}
	
	/** Accept a request.
	 * @param userToken the user's token.
	 * @param requestID the ID of the request to accept.
	 * @return the updated request.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchRequestException if there is no such request.
	 * @throws UnauthorizedException if the user is not the target of the request or an
	 * administrator of the group targeted in the request, if a group is targeted.
	 * @throws UserIsMemberException if the user to be added to a group is already a member of
	 * the group.
	 */
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
			return processAcceptGroupMembershipRequest(request, user, group);
		} else if (request.getType().equals(GroupRequestType.INVITE_TO_GROUP)) {
			return processAcceptGroupInviteRequest(request, group);
		} else {
			// untestable. Here to throw an error if a type is added and not accounted for
			throw new UnimplementedException();
		}
	}

	// assumes group exists
	// this and the accept membership request code is similar - DRY up a bit?
	private GroupRequest processAcceptGroupInviteRequest(
			final GroupRequest request,
			final Group group)
			throws GroupsStorageException, NoSuchRequestException, UserIsMemberException {
		final UserName acceptedBy = request.getTarget().get();
		addMemberToKnownGoodGroup(group.getGroupID(), acceptedBy);
		storage.closeRequest(
				request.getID(), GroupRequestStatus.accepted(acceptedBy), clock.instant());
		final GroupRequest r = storage.getRequest(request.getID());
		notifications.accept(new HashSet<>(group.getAdministratorsAndOwner()), r);
		return r;
	}

	// assumes group exists
	private GroupRequest processAcceptGroupMembershipRequest(
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
		final GroupRequest r = storage.getRequest(request.getID());
		notifications.accept(targets, r);
		return r;
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
	
	/** Remove a user from a group.
	 * @param userToken the token of the user performing the remove operation.
	 * @param groupID the ID of the group from which the user will be removed.
	 * @param member the member to remove from the group.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws NoSuchUserException if the user is not a member of the group.
	 * @throws UnauthorizedException if the user to be removed is not the user from the token, or
	 * if the user from the token is not a group administrator.
	 */
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
	
	/** Promote a standard member to an administrator.
	 * @param userToken the token of the user performing the promotion.
	 * @param groupID the group to modify.
	 * @param member the user to be promoted.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user represented by the token is not the group owner.
	 * @throws NoSuchUserException if the user is not a standard group member.
	 * @throws UserIsMemberException if the user is the group owner or an adminstrator.
	 */
	public void promoteMember(final Token userToken, final GroupID groupID, final UserName member)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchUserException,
				UserIsMemberException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(member, "member");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		if (!user.equals(group.getOwner())) {
			throw new UnauthorizedException(
					"Only the group owner can promote administrators");
		}
		if (!group.getMembers().contains(member)) {
			throw new NoSuchUserException(String.format(
					"User %s is not a standard member of group %s",
					member.getName(), groupID.getName()));
		}
		// possibility of a race condition here if the user if removed now, but not
		// a big deal
		// this method will check that the user is not already an admin or the owner per the
		// docs
		storage.addAdmin(groupID, member);
		// may want to notify here
	}
	
	/** Demote an administrator to a standard member.
	 * @param userToken the token of the user performing the demotion.
	 * @param groupID the group to modify.
	 * @param admin the admin to demote.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user represented by the token is not the group owner
	 * and not the admin to be demoted.
	 * @throws NoSuchUserException if the user is not an admin.
	 */
	public void demoteAdmin(final Token userToken, final GroupID groupID, final UserName admin)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
			GroupsStorageException, UnauthorizedException, NoSuchUserException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(admin, "admin");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		if (!user.equals(group.getOwner()) && !user.equals(admin)) {
			throw new UnauthorizedException(
					"Only the group owner can demote administrators");
		}
		// this method will throw an error if the user is not an admin.
		storage.demoteAdmin(groupID, admin);
		// notify? I'm thinking not
	}
}
