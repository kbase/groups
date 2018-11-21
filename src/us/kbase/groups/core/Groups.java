package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import us.kbase.common.exceptions.UnimplementedException;
import us.kbase.groups.core.GroupView.ViewType;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.catalog.CatalogModule;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.CatalogMethodExistsException;
import us.kbase.groups.core.exceptions.ClosedRequestException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCatalogEntryException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.exceptions.WorkspaceExistsException;
import us.kbase.groups.core.fieldvalidation.FieldValidatorException;
import us.kbase.groups.core.fieldvalidation.FieldValidators;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** The core class in the Groups software. 
 * @author gaprice@lbl.gov
 *
 */
public class Groups {
	
	//TODO NNOW limits on resource IDs (both)
	
	//TODO LOGGING for all actions
	
	//TODO NNOW major refactor. See below.
	/* A request is 3 things
	 * invite to group vs. request addition to group (invite vs. request)
	 * The type of resource (user, ws, catalog method)
	 * The resource.
	 * Right now there's a lot of redundant code between ws and method. Refactor into a general
	 * typed resource system (including user where possible, but users are a little different).
	 * 
	 * Always include users as the target of requests though. Generalize the target.
	 */
	
	/* could probably abstract the workspace & catalog handling in a
	 * general resource handling system, where resources and handlers for those resources
	 * could be specified in a configuration file or just hard coded.
	 * Then you could add new resources w/o major code changes. Indexing might be tricky but
	 * doable.
	 * 
	 * That being said, it's probably only ever going to be workspaces and apps so YAGNI.
	 */
	
	// TODO NNOW mimimize info sent to notifications. Don't send request ID on deny/accept/cancel
	
	private static final Duration REQUEST_EXPIRE_TIME = Duration.of(14, ChronoUnit.DAYS);
	private final GroupsStorage storage;
	private final UserHandler userHandler;
	private final ResourceHandler wsHandler;
	private final ResourceHandler catHandler;
	private final FieldValidators validators;
	private final Notifications notifications;
	private final UUIDGenerator uuidGen;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 * @param wsHandler the workspace handler by which information from the workspace service
	 * will be retrieved.
	 * @param catHandler the catalog handler by which information from the catalog service
	 * will be retrieved.
	 * @param validators the validators for group custom fields.
	 * @param notifications where notification should be sent.
	 */
	public Groups(
			// getting to the point where a builder might be useful, but everything's required.
			final GroupsStorage storage,
			final UserHandler userHandler,
			final ResourceHandler wsHandler,
			final ResourceHandler catHandler,
			final FieldValidators validators,
			final Notifications notifications) {
		this(storage, userHandler, wsHandler, catHandler, validators, notifications,
				new UUIDGenerator(), Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final ResourceHandler wsHandler,
			final ResourceHandler catHandler,
			final FieldValidators validators,
			final Notifications notifications,
			final UUIDGenerator uuidGen,
			final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		checkNotNull(wsHandler, "wsHandler");
		checkNotNull(catHandler, "catHandler");
		checkNotNull(validators, "validators");
		checkNotNull(notifications, "notifications");
		this.storage = storage;
		this.userHandler = userHandler;
		this.wsHandler = wsHandler;
		this.catHandler = catHandler;
		this.validators = validators;
		this.notifications = notifications;
		this.uuidGen = uuidGen;
		this.clock = clock;
	}
	
	/** Create a new group.
	 * @param userToken the token of the user that will be creating the group.
	 * @param createParams the paramaters describing how the group will be created.
	 * @return a view of the new group where the view type is {@link ViewType#MEMBER}.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupExistsException if the group already exists.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchCustomFieldException if a custom field in the update is not configured.
	 * @throws IllegalParameterException if a custom field in the update has an illegal value.
	 * @throws FieldValidatorException if a validator could not validate the field.
	 */
	public GroupView createGroup(
			final Token userToken,
			final GroupCreationParams createParams)
			throws InvalidTokenException, AuthenticationException, GroupExistsException,
				GroupsStorageException, IllegalParameterException, NoSuchCustomFieldException,
				FieldValidatorException {
		checkNotNull(userToken, "userToken");
		checkNotNull(createParams, "createParams");
		validateCustomFields(createParams.getOptionalFields());
		final UserName owner = userHandler.getUser(userToken);
		storage.createGroup(createParams.toGroup(owner, new CreateAndModTimes(clock.instant())));
		
		try {
			return new GroupView(
					storage.getGroup(createParams.getGroupID()),
					ResourceInformationSet.getBuilder(owner).build(),
					ViewType.MEMBER);
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(
					"Just created a group and it's already gone. Something's really broken", e);
		}
	}
	
	private void validateCustomFields(final OptionalGroupFields optFields)
			throws IllegalParameterException, NoSuchCustomFieldException, FieldValidatorException {
		for (final NumberedCustomField f: optFields.getCustomFields()) {
			final FieldItem<String> value = optFields.getCustomValue(f);
			if (value.hasItem()) {
				try {
					validators.validate(f, value.get());
				} catch (MissingParameterException e) {
					throw new RuntimeException(
							"This should be impossible. Please turn reality off and on again", e);
				}
			}
		}
	}

	/** Update a group's fields.
	 * @param userToken the token of the user that is modifying the group.
	 * @param updateParams the update to apply.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no group with the provided ID.
	 * @throws UnauthorizedException if the user is not a group administrator.
	 * @throws NoSuchCustomFieldException if a custom field in the update is not configured.
	 * @throws IllegalParameterException if a custom field in the update has an illegal value.
	 * @throws FieldValidatorException if a validator could not validate the field.
	 */
	public void updateGroup(final Token userToken, final GroupUpdateParams updateParams)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException,
				IllegalParameterException, NoSuchCustomFieldException, FieldValidatorException {
		checkNotNull(userToken, "userToken");
		checkNotNull(updateParams, "updateParams");
		if (!updateParams.hasUpdate()) {
			return;
		}
		validateCustomFields(updateParams.getOptionalFields());
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(updateParams.getGroupID());
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format("User %s may not administrate group %s",
					user.getName(), updateParams.getGroupID().getName()));
		}
		storage.updateGroup(updateParams, clock.instant());
	}
	
	/** Get a view of a group.
	 * A null token or a non-member gets a {@link ViewType#NON_MEMBER} view.
	 * A null token will result in only public group workspaces being included in the view.
	 * A non-member token will also include workspaces the user administrates.
	 * A member will get a {@link ViewType#MEMBER} view and all group 
	 * workspaces will be included.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to get.
	 * @return a view of the group.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 */
	public GroupView getGroup(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, ResourceHandlerException {
		final Group g = storage.getGroup(groupID);
		final UserName user;
		if (userToken != null) {
			user = userHandler.getUser(userToken);
		} else {
			user = null;
		}
		final ResourceInformationSet ris;
		try {
			ris = wsHandler.getResourceInformation(
					user, toResIDs(g.getWorkspaceIDs()), !g.isMember(user));
		} catch (IllegalResourceIDException e) {
			throw new RuntimeException(String.format("Illegal data associated with group %s: %s",
					g.getGroupID().getName(), e.getMessage()), e);
		}
		for (final ResourceDescriptor wsid: ris.getNonexistentResources()) {
			try {
				storage.removeWorkspace(g.getGroupID(), toWSID(wsid), clock.instant());
			} catch (NoSuchWorkspaceException e) {
				// do nothing, if the workspace isn't there fine.
			}
		}
		return new GroupView(g, ris, g.isMember(user) ? ViewType.MEMBER : ViewType.NON_MEMBER);
	}
	
	// TODO NNOW remove
	private WorkspaceID toWSID(final ResourceDescriptor wsid) {
		try {
			return new WorkspaceID(Integer.parseInt(wsid.getResourceID().getName()));
		} catch (NumberFormatException | IllegalParameterException e) {
			throw new RuntimeException("impossible", e);
		}
	}

	//TODO NNOW remove
	private Set<ResourceID> toResIDs(final WorkspaceIDSet workspaceIDs) {
		return workspaceIDs.getIDs().stream().map(i -> toResID(i)).collect(Collectors.toSet());
	}

	//TODO NNOW remove
	private ResourceID toResID(final WorkspaceID id) {
		return toResID(id.getID());
	}
	
	//TODO NNOW remove
	private ResourceID toResID(final int id) {
		final ResourceID r;
		try {
			r = new ResourceID(id + "");
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Impossible", e);
		}
		return r;
	}

	/** Check if a group exists based on the group ID.
	 * @param groupID the group ID.
	 * @return true if the group exists, false otherwise.
	 * @throws GroupsStorageException
	 */
	public boolean getGroupExists(final GroupID groupID) throws GroupsStorageException {
		checkNotNull(groupID, "groupID");
		return storage.getGroupExists(groupID);
	}
	
	/** Get views of the groups in the system, where the view type is
	 * {@link ViewType#MINIMAL}.
	 * At most 100 groups are returned.
	 * @param params the parameters for getting the groups.
	 * @return the groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	public List<GroupView> getGroups(final GetGroupsParams params)
			throws GroupsStorageException {
		checkNotNull(params, "params");
		return storage.getGroups(params).stream()
				.map(g -> new GroupView(
						g, ResourceInformationSet.getBuilder(null).build(), ViewType.MINIMAL))
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
		return createRequestStoreAndNotify(g, user, b -> b.withRequestGroupMembership(),
				g.getAdministratorsAndOwner());
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
		return createRequestStoreAndNotify(g, user, b -> b.withInviteToGroup(newMember),
				Arrays.asList(newMember));
	}
	
	private GroupRequest createRequestStoreAndNotify(
			final Group group,
			final UserName creator,
			final Function<GroupRequest.Builder, GroupRequest.Builder> builderFunction,
			final Collection<UserName> notifyTargets)
			throws RequestExistsException, GroupsStorageException {
		final Instant now = clock.instant();
		final GroupRequest request = builderFunction.apply(GroupRequest.getBuilder(
				new RequestID(uuidGen.randomUUID()), group.getGroupID(), creator,
				CreateModAndExpireTimes.getBuilder(
						now, now.plus(REQUEST_EXPIRE_TIME)).build()))
				.build();
		storage.storeRequest(request);
		notifications.notify(notifyTargets, group, request);
		return request;
	}
	
	private final Set<GroupRequestUserAction> CREATOR_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.CANCEL));
	private final Set<GroupRequestUserAction> TARGET_ACTIONS = new HashSet<>(Arrays.asList(
			GroupRequestUserAction.ACCEPT, GroupRequestUserAction.DENY));
	private final Set<GroupRequestUserAction> NO_ACTIONS = Collections.emptySet();
	
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
	 * @throws ResourceHandlerException  if an error occurs contacting the resource service.
	 */
	public GroupRequestWithActions getRequest(final Token userToken, final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(request);
		//TODO PRIVATE may want to censor accepter/denier and deny reason here and in other methods that return a closed request
		if (user.equals(request.getRequester())) {
			return new GroupRequestWithActions(request,
					request.isOpen() ? CREATOR_ACTIONS : NO_ACTIONS);
		} else {
			/* For the invite types, this code means that admins can't see the request unless they
			 * created it (same deal for the list fns). I think that's ok - the request is between
			 * a particular admin and a user or workspace. The other admins will get a notification
			 * if it's accepted and then can remove the user / ws.
			 * Still though, maybe some admin endpoint or this one should just return any request.
			 * Or maybe admins should be able to view and cancel each other's requests.
			 */
			ensureIsRequestTarget(request, g.isAdministrator(user), user, "access");
			return new GroupRequestWithActions(request,
					request.isOpen() ? TARGET_ACTIONS : NO_ACTIONS);
		}
	}
	
	/** Get requests that were created by the user.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	public List<GroupRequest> getRequestsForRequester(
			final Token userToken,
			final GetRequestsParams params)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		checkNotNull(params, "params");
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user, params);
	}
	
	/** Get requests where the user is the target of the request.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 */
	public List<GroupRequest> getRequestsForTarget(
			final Token userToken,
			final GetRequestsParams params)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
				ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(params, "params");
		final UserName user = userHandler.getUser(userToken);
		final Set<ResourceAdministrativeID> ws = wsHandler.getAdministratedResources(user);
		final Set<ResourceAdministrativeID> adIDs = catHandler.getAdministratedResources(user);
		final Set<Integer> wsids = new HashSet<>();
		for (final ResourceAdministrativeID id: ws) {
			try {
				wsids.add(Integer.parseInt(id.getName()));
			} catch (NumberFormatException e) {
				throw new RuntimeException("Impossible", e); // TODO NNOW remove
			}
		}
		final Set<CatalogModule> mods = new HashSet<>();
		for (final ResourceAdministrativeID id: adIDs) {
			try {
				mods.add(new CatalogModule(id.getName()));
			} catch (MissingParameterException | IllegalParameterException e) {
				throw new RuntimeException("Impossible", e); //TODO NNOW remove
			}
		}
		return storage.getRequestsByTarget(user, WorkspaceIDSet.fromInts(wsids), mods, params);
	}

	/** Get requests where the group is the target of the request.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group for which requests will be returned.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws UnauthorizedException if the user is not a group admin.
	 * @throws NoSuchGroupException if the group does not exist.
	 */
	public List<GroupRequest> getRequestsForGroup(
			final Token userToken,
			final GroupID groupID,
			final GetRequestsParams params)
			throws UnauthorizedException, InvalidTokenException, AuthenticationException,
				NoSuchGroupException, GroupsStorageException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(params, "params");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format(
					"User %s cannot view requests for group %s",
					user.getName(), groupID.getName()));
		}
		return storage.getRequestsByGroup(groupID, params);
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
			storage.addMember(groupID, newMember, clock.instant());
		} catch (NoSuchGroupException e) {
			// shouldn't happen
			throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		}
	}
	
	private void addWorkspaceToKnownGoodGroup(final GroupID groupID, final WorkspaceID wsid)
			throws GroupsStorageException, ResourceExistsException {
		try {
			storage.addWorkspace(groupID, wsid, clock.instant());
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		} catch (WorkspaceExistsException e) {
			throw new ResourceExistsException(e.getMessage().split(":", 2)[1].trim(), e);
		}
	}
	
	private void addMethodToKnownGoodGroup(final GroupID groupID, final CatalogMethod method)
			throws GroupsStorageException, ResourceExistsException {
		try {
			storage.addCatalogMethod(groupID, method, clock.instant());
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		} catch (CatalogMethodExistsException e) {
			throw new ResourceExistsException(e.getMessage().split(":", 2)[1].trim(), e);
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
	 * @throws ClosedRequestException if the request is closed.
	 */
	public GroupRequest cancelRequest(final Token userToken, final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, ClosedRequestException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest gr = storage.getRequest(requestID);
		if (!gr.getRequester().equals(user)) {
			throw new UnauthorizedException(String.format("User %s may not cancel request %s",
					user.getName(), requestID.getID()));
		}
		ensureIsOpen(gr);
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
	 * @throws ClosedRequestException if the request is closed.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 */
	public GroupRequest denyRequest(
			final Token userToken,
			final RequestID requestID,
			final String reason)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, ClosedRequestException,
				ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureIsRequestTarget(request, group.isAdministrator(user), user, "deny");
		ensureIsOpen(request);
		
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
	 * @throws ClosedRequestException if the request is closed.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if the resource associated with the request no longer
	 * exists.
	 * @throws ResourceExistsException if the resource is already associated with the group.
	 */ 
	public GroupRequest acceptRequest(
			final Token userToken,
			final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, UserIsMemberException,
				ClosedRequestException, ResourceHandlerException, NoSuchResourceException,
				ResourceExistsException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureIsRequestTarget(request, group.isAdministrator(user), user, "accept");
		ensureIsOpen(request);
		final Set<UserName> notifyTargets = processRequest(group.getGroupID(), request);
		notifyTargets.addAll(group.getAdministratorsAndOwner());
		notifyTargets.remove(user);
		storage.closeRequest(request.getID(), GroupRequestStatus.accepted(user), clock.instant());
		final GroupRequest r = storage.getRequest(request.getID());
		notifications.accept(notifyTargets, r);
		return r;
	}

	// returns any users other than the group administrators that should be notified
	private Set<UserName> processRequest(
			final GroupID groupID,
			final GroupRequest request)
			throws GroupsStorageException, UserIsMemberException, NoSuchResourceException,
				ResourceHandlerException, ResourceExistsException {
		final Collection<UserName> toNotify;
		if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP) ||
				request.getType().equals(GroupRequestType.INVITE_TO_GROUP)) {
			addMemberToKnownGoodGroup(groupID, request.getTarget().get());
			toNotify = Arrays.asList(request.getTarget().get());
		// TODO NNOW combine next 2
		} else if (request.getType().equals(GroupRequestType.REQUEST_ADD_WORKSPACE) ||
				request.getType().equals(GroupRequestType.INVITE_WORKSPACE)) {
			final WorkspaceID wsid = request.getWorkspaceTarget().get();
			// do this first in case the ws has been deleted
			toNotify = getAdmins(wsHandler, toResID(wsid), request);
			addWorkspaceToKnownGoodGroup(groupID, wsid);
		} else if (request.getType().equals(GroupRequestType.REQUEST_ADD_CATALOG_METHOD) ||
				request.getType().equals(GroupRequestType.INVITE_CATALOG_METHOD)) {
			final CatalogMethod m = request.getCatalogMethodTarget().get();
			// do this first in case the catalog ever allows module deletion
			toNotify = getAdmins(catHandler, toResourceID(m), request);
			addMethodToKnownGoodGroup(groupID, m);
		} else {
			// untestable. Here to throw an error if a type is added and not accounted for
			throw new UnimplementedException();
		}
		return new HashSet<>(toNotify);
	}

	private Collection<UserName> getAdmins(
			final ResourceHandler handler,
			final ResourceID resource, //TODO NNOW get from request
			final GroupRequest request)
			throws NoSuchResourceException, ResourceHandlerException {
		try {
			return handler.getAdministrators(resource);
		} catch (IllegalResourceIDException e) {
			throw new RuntimeException(String.format("Illegal value stored in request %s: %s",
					request.getID().getID(), e.getMessage()), e);
		}
	}

	//TODO NNOW remove
	private ResourceID toResourceID(final CatalogMethod m) {
		try {
			return new ResourceID(m.getFullMethod());
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Impossible", e);
		}
	}
	
	private void ensureIsOpen(final GroupRequest request) throws ClosedRequestException {
		if (!request.isOpen()) {
			throw new ClosedRequestException(request.getID().getID().toString());
		}
	}

	private void ensureIsRequestTarget(
			final GroupRequest request,
			final boolean isGroupAdmin,
			final UserName user,
			final String actionVerb)
			throws UnauthorizedException, ResourceHandlerException {
		if (!request.isInvite() && isGroupAdmin) {
			return;
		} else if (request.getType().equals(GroupRequestType.INVITE_TO_GROUP) &&
				user.equals(request.getTarget().get())) {
			return;
		// TODO NNOW combine next 2
		} else if (request.getType().equals(GroupRequestType.INVITE_WORKSPACE) &&
				isAdministrator(wsHandler, user, request,
						toResID(request.getWorkspaceTarget().get()))) {
			return;
		} else if (request.getType().equals(GroupRequestType.INVITE_CATALOG_METHOD) &&
				isAdministrator(catHandler, user, request,
						toResourceID(request.getCatalogMethodTarget().get()))) {
			return;
		} else {
			throw new UnauthorizedException(String.format("User %s may not %s request %s",
					user.getName(), actionVerb, request.getID().getID()));
		}
	}
	
	// TODO CODE if the NoSuch exception is thrown, maybe request should be closed
	// returns false if ws is missing / deleted
	private boolean isAdministrator(
			final ResourceHandler handler,
			final UserName user,
			final GroupRequest request,
			final ResourceID resource) // TODO NNOW get from request
			throws ResourceHandlerException {
		try {
			return handler.isAdministrator(resource, user);
		} catch (NoSuchResourceException e) {
			return false;
		} catch (IllegalResourceIDException e) {
			throw new RuntimeException(String.format("Illegal value stored in request %s: %s",
					request.getID().getID(), e.getMessage()), e);
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
			storage.removeMember(groupID, member, clock.instant());
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
		storage.addAdmin(groupID, member, clock.instant());
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
		storage.demoteAdmin(groupID, admin, clock.instant());
		// notify? I'm thinking not
	}
	
	/** Add a workspace to a group. The workspace is added immediately if the user is an
	 * administrator of both the group and the workspace. Otherwise, a {@link GroupRequest} is
	 * added to the system and returned.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param resource the workspace ID.
	 * @return A request if required or {@link Optional#empty()} if the operation is already
	 * complete.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not an administrator of the group or the
	 * workspace.
	 * @throws RequestExistsException if there's already an equivalent request in the system.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 * @throws ResourceExistsException  if the resource is already associated with the group.
	 */
	public Optional<GroupRequest> addWorkspace(
			final Token userToken,
			final GroupID groupID,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, RequestExistsException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException,
				ResourceExistsException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		final ResourceDescriptor d = wsHandler.getDescriptor(resource);
		final WorkspaceID wsid = toWSID(d); // TODO NNOW remove
		if (g.getWorkspaceIDs().contains(wsid)) {
			throw new ResourceExistsException(resource.getName());
		}
		final Set<UserName> wsadmins = wsHandler.getAdministrators(resource);
		if (g.isAdministrator(user) && wsadmins.contains(user)) {
			try {
				storage.addWorkspace(groupID, wsid, clock.instant());
			} catch (WorkspaceExistsException e) { // TODO NNOW remove
				throw new ResourceExistsException(e.getMessage().split(":", 2)[1].trim(), e);
			}
			//TODO NNOW notify
			return Optional.empty();
		}
		if (wsadmins.contains(user)) {
			return Optional.of(createRequestStoreAndNotify(
					g, user, b -> b.withRequestAddWorkspace(wsid), g.getAdministratorsAndOwner()));
		}
		if (g.isAdministrator(user)) {
			return Optional.of(createRequestStoreAndNotify(
					g, user, b -> b.withInviteWorkspace(wsid), wsadmins));
		}
		throw new UnauthorizedException(String.format(
				"User %s is not an admin for group %s or resource %s",
				user.getName(), groupID.getName(), resource.getName()));
	}
	
	/** Remove a workspace from a group.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param resource the workspace ID.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not an administrator of the group or the
	 * workspace.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	public void removeWorkspace(
			final Token userToken,
			final GroupID groupID,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		final ResourceDescriptor d = wsHandler.getDescriptor(resource);
		// should check if ws not in group & fail early?
		if (group.isAdministrator(user) || wsHandler.isAdministrator(resource, user)) {
			try {
				final WorkspaceID wsid = toWSID(d); //TODO NNOW remove
				storage.removeWorkspace(groupID, wsid, clock.instant());
			} catch (NoSuchWorkspaceException e) { // TODO NNOW remove
				throw new NoSuchResourceException(e.getMessage().split(":", 2)[1].trim(), e);
			}
		} else {
			throw new UnauthorizedException(String.format(
					"User %s is not an admin for group %s or resource %s",
					user.getName(), groupID.getName(), resource.getName()));
		}
	}
	
	/** Set read permissions on a workspace that has been requested to be added to a group
	 * for the user if the workspace is not already readable (including publicly so). The user
	 * must be a group administrator for the request and the request type must be
	 * {@link GroupRequestType#REQUEST_ADD_WORKSPACE}.
	 * @param userToken the user's token.
	 * @param requestID the ID of the request.
	 * @throws NoSuchRequestException if no request with that ID exists.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws UnauthorizedException if the user is not an administrator of the group or the
	 * request type is not correct.
	 * @throws ClosedRequestException if the request is closed.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	public void setReadPermissionOnWorkspace(
			final Token userToken,
			final RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException, InvalidTokenException,
				AuthenticationException, UnauthorizedException, ClosedRequestException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException {
		//TODO NNOW generalize this method to all resources
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest r = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(r);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format("User %s is not an admin for group %s",
					user.getName(), g.getGroupID().getName()));
		}
		if (!GroupRequestType.REQUEST_ADD_WORKSPACE.equals(r.getType())) {
			throw new UnauthorizedException(
					"Only workspace add requests allow for workspace permissions changes.");
		}
		ensureIsOpen(r);
		final ResourceID rid = toResID(r.getWorkspaceTarget().get()); //TODO NNOW remove
		wsHandler.setReadPermission(rid, user);
	}
	
	/** Add a catalog method to a group. The method is added immediately if the user is an
	 * administrator of both the group and the catalog module. Otherwise, a {@link GroupRequest} is
	 * added to the system and returned.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param resource the method.
	 * @return A request if required or {@link Optional#empty()} if the operation is already
	 * complete.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not an administrator of the group or an
	 * owner of the module.
	 * @throws RequestExistsException if there's already an equivalent request in the system.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 * @throws ResourceExistsException  if the resource is already associated with the group.
	 */
	public Optional<GroupRequest> addCatalogMethod(
			final Token userToken,
			final GroupID groupID,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, RequestExistsException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException,
				ResourceExistsException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		final ResourceDescriptor d = catHandler.getDescriptor(resource);
		final CatalogMethod m; // TODO NNOW remove
		try {
			m = new CatalogMethod(d.getResourceID().getName());
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("impossible", e);
		}
		if (g.getCatalogMethods().contains(m)) {
			throw new ResourceExistsException(resource.getName());
		}
		final Set<UserName> modowners = catHandler.getAdministrators(resource);
		if (g.isAdministrator(user) && modowners.contains(user)) {
			try {
				storage.addCatalogMethod(groupID, m, clock.instant());
			} catch (CatalogMethodExistsException e) { // TODO NNOW remove
				throw new ResourceExistsException(e.getMessage().split(":", 2)[1].trim(), e);
			}
			//TODO NNOW notify
			return Optional.empty();
		}
		if (modowners.contains(user)) {
			return Optional.of(createRequestStoreAndNotify(g, user,
					b -> b.withRequestAddCatalogMethod(m), g.getAdministratorsAndOwner()));
		}
		if (g.isAdministrator(user)) {
			return Optional.of(createRequestStoreAndNotify(
					g, user, b -> b.withInviteCatalogMethod(m), modowners));
		}
		throw new UnauthorizedException(String.format(
				"User %s is not an admin for group %s or resource %s",
				user.getName(), groupID.getName(), resource.getName()));
	}
	
	/** Remove a catalog method from a group.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param resource the method.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not an administrator of the group or the
	 * catalog module.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	public void removeCatalogMethod(
			final Token userToken,
			final GroupID groupID,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		// should check if method not in group & fail early?
		final ResourceDescriptor d = catHandler.getDescriptor(resource);
		if (group.isAdministrator(user) || catHandler.isAdministrator(resource, user)) {
			final CatalogMethod m; // TODO NNOW remove
			try {
				m = new CatalogMethod(d.getResourceID().getName());
				storage.removeCatalogMethod(groupID, m, clock.instant());
			} catch (MissingParameterException | IllegalParameterException e) {
				throw new RuntimeException("impossible", e);
			} catch (NoSuchCatalogEntryException e) { // TODO NNOW remove
				throw new NoSuchResourceException(e.getMessage().split(":", 2)[1].trim(), e);
			}
		} else {
			throw new UnauthorizedException(String.format(
					"User %s is not an admin for group %s or resource %s",
					user.getName(), groupID.getName(), resource.getName()));
		}
	}
}
