package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static us.kbase.groups.core.request.GroupRequest.USER_TYPE;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ClosedRequestException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchResourceTypeException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.FieldValidatorException;
import us.kbase.groups.core.fieldvalidation.FieldValidators;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.GroupRequestUserAction;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAccess;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** The core class in the Groups software. 
 * @author gaprice@lbl.gov
 *
 */
public class Groups {
	
	//TODO LOGGING for all actions
	
	private static final Duration REQUEST_EXPIRE_TIME = Duration.of(14, ChronoUnit.DAYS);
	private static final int MAX_GROUP_NAMES_RETURNED = 1000;
	private static final int MAX_GROUP_HAS_REQUESTS_COUNT = 100;
	private static final int MAX_GROUP_LIST_COUNT = 100;
	private final GroupsStorage storage;
	private final UserHandler userHandler;
	private final Map<ResourceType, ResourceHandler> resourceHandlers;
	private final FieldValidators validators;
	private final Notifications notifications;
	private final UUIDGenerator uuidGen;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 * @param resourceHandlers the resource handlers for providing information about resources.
	 * Note that changing the set of configured handlers for a storage instance may cause errors
	 * and unexpected behavior, as handlers may be missing for data in the storage system or
	 * handlers may be swapped, leading to inaccurate information being returned for the
	 * stored data.
	 * @param validators the validators for group custom fields.
	 * @param notifications where notification should be sent.
	 */
	public Groups(
			// getting to the point where a builder might be useful, but everything's required.
			final GroupsStorage storage,
			final UserHandler userHandler,
			final Map<ResourceType, ResourceHandler> resourceHandlers,
			final FieldValidators validators,
			final Notifications notifications) {
		this(storage, userHandler, resourceHandlers, validators, notifications,
				new UUIDGenerator(), Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(
			final GroupsStorage storage,
			final UserHandler userHandler,
			final Map<ResourceType, ResourceHandler> resourceHandlers,
			final FieldValidators validators,
			final Notifications notifications,
			final UUIDGenerator uuidGen,
			final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		checkNotNull(resourceHandlers, "resourceHandlers");
		checkNotNull(validators, "validators");
		checkNotNull(notifications, "notifications");
		this.storage = storage;
		this.userHandler = userHandler;
		if (resourceHandlers.containsKey(USER_TYPE)) {
			throw new IllegalArgumentException("resourceHandlers cannot contain built in type " +
					USER_TYPE.getName());
		}
		this.resourceHandlers = new HashMap<>(resourceHandlers);
		this.validators = validators;
		this.notifications = notifications;
		this.uuidGen = uuidGen;
		this.clock = clock;
	}
	
	/** Create a new group.
	 * @param userToken the token of the user that will be creating the group.
	 * @param createParams the paramaters describing how the group will be created.
	 * @return a view of the new group.
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
			// this is a standard, isMember view and so all fields are visible without needing
			// to specify field determiners
			return startViewBuild(storage.getGroup(createParams.getGroupID()), owner).build();
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(
					"Just created a group and it's already gone. Something's really broken", e);
		}
	}
	
	private void validateCustomFields(final OptionalGroupFields optFields)
			throws IllegalParameterException, NoSuchCustomFieldException, FieldValidatorException {
		for (final NumberedCustomField f: optFields.getCustomFields()) {
			final OptionalString value = optFields.getCustomValue(f);
			if (value.isPresent()) {
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
	
	/** Update a user's fields.
	 * The user must be either a group administrator or the user to be updated.
	 * @param userToken the user's token.
	 * @param groupID the group to update.
	 * @param member the member to update.
	 * @param fields the update to apply to the member. An {@link OptionalString#empty()} value
	 * indicates the field should be removed.
	 * @throws UnauthorizedException if the user is not a group administrator or the member
	 * to be updated, or the member tries to update a restricted field.
	 * @throws NoSuchUserException if the group does not contain the member. This exception
	 * is only thrown is the user is a group member.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no group with the provided ID.
	 * @throws NoSuchCustomFieldException if a custom field in the update is not configured.
	 * @throws IllegalParameterException if a custom field in the update has an illegal value.
	 * @throws FieldValidatorException if a validator could not validate the field.
	 */
	public void updateUser(
			final Token userToken,
			final GroupID groupID,
			final UserName member,
			final Map<NumberedCustomField, OptionalString> fields)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchUserException,
				IllegalParameterException, NoSuchCustomFieldException, FieldValidatorException {
		requireNonNull(userToken, "userToken");
		requireNonNull(groupID, "groupID");
		requireNonNull(member, "member");
		requireNonNull(fields, "fields");
		if (fields.isEmpty()) {
			return;
		}
		for (final Entry<NumberedCustomField, OptionalString> e: fields.entrySet()) {
			requireNonNull(e.getKey(), "Null key in fields");
			requireNonNull(e.getValue(), String.format("Null value for key %s in fields",
					e.getKey().getField()));
		}
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		checkUserUpdatePermission(member, user, g);

		validateUserCustomFields(fields);
		if (!g.isAdministrator(user)) {
			for (final NumberedCustomField f: fields.keySet()) {
				if (!validators.getUserFieldConfig(f.getFieldRoot()).isUserSettable()) {
					throw new UnauthorizedException(String.format(
							"User %s is not authorized to set field %s for group %s",
							user.getName(), f.getField(), groupID.getName()));
				}
			}
		}
		// ok to throw no such member here because either
		// 1) the user is an admin
		// 2) the user is changing their own fields and were a member a few milliseconds ago
		storage.updateUser(groupID, member, fields, clock.instant());
	}

	private void checkUserUpdatePermission(
			final UserName member,
			final UserName user,
			final Group group)
			throws NoSuchUserException, UnauthorizedException {
		// should admins be able to update each other's fields? For now yes, might need
		// to change later. If an admin is being a dbag the owner should demote them.
		if (!group.isMember(member)) {
			if (group.isMember(user)) {
				throw new NoSuchUserException(String.format("User %s is not a member of group %s",
						member.getName(), group.getGroupID().getName()));
			} else {
				// don't throw a "user X is not member of group Y" here. That effectively makes
				// the member list public
				throw updateUserUnauthorizedException(member, user, group);
			}
		} else if (!group.isAdministrator(user) && !user.equals(member)) {
			// this needs to be identical to the error above so that non-members can't tell the
			// difference between the two errors. Otherwise they can tell if a user is a member
			// of the group
			throw updateUserUnauthorizedException(member, user, group);
		}
	}

	private UnauthorizedException updateUserUnauthorizedException(
			final UserName member,
			final UserName user,
			final Group group) {
		return new UnauthorizedException(String.format(
				"User %s is not authorized to change record of user %s in group %s",
				user.getName(), member.getName(), group.getGroupID().getName()));
	}
	
	private void validateUserCustomFields(final Map<NumberedCustomField, OptionalString> fields)
			throws IllegalParameterException, NoSuchCustomFieldException, FieldValidatorException {
		for (final NumberedCustomField f: fields.keySet()) {
			final OptionalString value = fields.get(f);
			if (value.isPresent()) {
				try {
					validators.validateUserField(f, value.get());
				} catch (MissingParameterException e) {
					throw new RuntimeException(
							"This should be impossible. Please turn reality off and on again", e);
				}
			}
		}
	}
	
	/** Update the last visited date for a user and a group.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to update.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no group with the provided ID.
	 * @throws NoSuchUserException if the user is not a member of the group.
	 */
	public void userVisited(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				NoSuchUserException, GroupsStorageException {
		final UserName user = userHandler.getUser(requireNonNull(userToken, "userToken"));
		storage.updateUser(requireNonNull(groupID, "groupID"), user, clock.instant());
	}
	
	/** Get a view of a group.
	 * A null token or a non-member gets a non-member view.
	 * A null token will result in only public group workspaces being included in the view.
	 * A non-member token will also include workspaces the user administrates.
	 * A member will get a full view and all group workspaces will be included.
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
		Group g = storage.getGroup(groupID);
		final UserName user = getOptionalUser(userToken);
		final Map<ResourceType, ResourceInformationSet> resources = new HashMap<>();
		for (final ResourceType type: g.getResourceTypes()) {
			final ResourceInformationSet resourceInfo = getResourceInfo(g, user, type);
			resources.put(type, resourceInfo);
			g = g.removeResources(type, resourceInfo.getNonexistentResources());
		}
		final GroupView.Builder b = startViewBuild(g, user)
				// this seems odd. Maybe there's a better way to deal with this?
				.withPublicFieldDeterminer(
						f -> validators.getConfigOrEmpty(f.getFieldRoot())
								.map(c -> c.isPublicField()).orElse(false))
				.withPublicUserFieldDeterminer(
						f -> validators.getUserFieldConfigOrEmpty(f.getFieldRoot())
								.map(c -> c.isPublicField()).orElse(false));
		for (final ResourceType type: resources.keySet()) {
			b.withResource(type, resources.get(type).withoutNonexistentResources());
		}
		return b.build();
	}

	// returns null if token is null
	private UserName getOptionalUser(final Token userToken)
			throws InvalidTokenException, AuthenticationException {
		return userToken == null ? null : userHandler.getUser(userToken);
	}

	private GroupView.Builder startViewBuild(final Group g, final UserName user) {
		final GroupView.Builder b = GroupView.getBuilder(g, user).withStandardView(true);
		for (final ResourceType type: resourceHandlers.keySet()) {
			b.withResourceType(type);
		}
		return b;
	}
	
	private ResourceInformationSet getResourceInfo(
			final Group g,
			final UserName user,
			final ResourceType type)
			throws ResourceHandlerException, NoSuchGroupException, GroupsStorageException {
		final ResourceHandler h;
		try {
			h = getHandler(type);
		} catch (NoSuchResourceTypeException e) {
			throw new RuntimeException(String.format(
					"Group %s has %s data without a configured handler",
					g.getGroupID().getName(), type.getName()), e);
		}
		final ResourceInformationSet info;
		try {
			info = h.getResourceInformation(
					user,
					g.getResources(type).stream().map(r -> r.getResourceID())
							.collect(Collectors.toSet()),
					getAccessLevel(g, user));
		} catch (IllegalResourceIDException e) {
			throw new RuntimeException(String.format(
					"Illegal data associated with group %s: %s",
					g.getGroupID().getName(), e.getMessage()), e);
		}
		for (final ResourceID rid: info.getNonexistentResources()) {
			try {
				storage.removeResource(g.getGroupID(), type, rid, clock.instant());
			} catch (NoSuchResourceException e) {
				// do nothing, if the resource isn't there fine.
			}
		}
		return info;
	}

	private ResourceAccess getAccessLevel(final Group g, final UserName user) {
		if (g.isMember(user)) {
			return ResourceAccess.ALL;
		} else if (!g.isPrivate()) {
			return ResourceAccess.ADMINISTRATED_AND_PUBLIC;
		} else {
			return ResourceAccess.ADMINISTRATED;
		}
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
	
	/** Get the name of one or more groups given a list of group IDs. Group names will be absent
	 * if the group is private and the user is not a member of the group or is anonymous.
	 * No more than 1000 groups can be queried at once.
	 * @param userToken the user's token. If null and the group is private, no name is returned.
	 * @param groupIDs the group IDs.
	 * @return the groups' names and IDs.
	 * @throws NoSuchGroupException if one of the groups does not exist.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws IllegalParameterException if more than 1000 group IDs are queried.
	 */
	public List<GroupIDNameMembership> getGroupNames(
			final Token userToken,
			final Set<GroupID> groupIDs)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, IllegalParameterException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		if (groupIDs.size() > MAX_GROUP_NAMES_RETURNED) {
			throw new IllegalParameterException(String.format(
					"No more than %s group IDs are allowed", MAX_GROUP_NAMES_RETURNED));
		}
		final UserName user = getOptionalUser(userToken);
		return storage.getGroupNames(user, groupIDs);
	}

	/** Get the list of groups for which the user is a member.
	 * @param userToken the user's token.
	 * @return the list of groups containing the user as a member.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 */
	public List<GroupIDAndName> getMemberGroups(final Token userToken)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException {
		final UserName user = userHandler.getUser(requireNonNull(userToken, "userToken"));
		return storage.getMemberGroups(user);
	}
	
	/** Determine whether groups have open incoming (e.g. of type {@link RequestType#REQUEST})
	 * requests. Requests are considered {@link GroupHasRequests#NEW} if they are after the
	 * user's last visited date for the group. The user must be an admin of all the groups.
	 * @param userToken the user's token.
	 * @param groupIDs the group IDs of the groups to query. At most 100 IDs may be supplied.
	 * @return A mapping from the group ID to whether the group has any open requests.
	 * @throws NoSuchGroupException if one of the groups does not exist.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws IllegalParameterException if more than 100 group IDs are queried.
	 * @throws UnauthorizedException if the user is not an administrator of at least one of the
	 * groups.
	 */
	public Map<GroupID, GroupHasRequests> groupsHaveRequests(
			final Token userToken,
			final Set<GroupID> groupIDs)
			throws InvalidTokenException, AuthenticationException, UnauthorizedException,
				NoSuchGroupException, GroupsStorageException, IllegalParameterException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		if (groupIDs.size() > MAX_GROUP_HAS_REQUESTS_COUNT) {
			throw new IllegalParameterException(String.format(
					"No more than %s group IDs are allowed", MAX_GROUP_HAS_REQUESTS_COUNT));
		}
		final UserName user = userHandler.getUser(requireNonNull(userToken, "userToken"));
		final Map<GroupID, Optional<Instant>> gToLastVisit = new HashMap<>();
		for (final GroupID gid: groupIDs) {
			// could make a bulk method that returns less info per group if necessary
			// or even an isAdmin(Username, Set<GroupID>) method. YAGNI for now
			final Group g = storage.getGroup(gid);
			if (!g.isAdministrator(user)) {
				throw new UnauthorizedException(String.format(
						"User %s may not administrate group %s", user.getName(), gid.getName()));
			}
			gToLastVisit.put(gid, g.getMember(user).getLastVisit());
		}
		final Map<GroupID, GroupHasRequests> ret = new HashMap<>();
		for (final GroupID gid: groupIDs) {
			final Instant laterThan = gToLastVisit.get(gid).orElse(null);
			final GroupHasRequests reqstate;
			if (storage.groupHasRequest(gid, laterThan)) {
				reqstate = GroupHasRequests.NEW;
			} else if (laterThan != null) {
				reqstate = storage.groupHasRequest(gid, null) ?
						GroupHasRequests.OLD : GroupHasRequests.NONE;
			} else {
				reqstate = GroupHasRequests.NONE;
			}
			ret.put(gid, reqstate);
		}
		return ret;
	}
	
	/** Get minimal views of the groups in the system.
	 * At most 100 groups are returned.
	 * @param userToken the user's token. If null, only public groups are returned.
	 * @param params the parameters for getting the groups.
	 * @return the groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 */
	public List<GroupView> getGroups(final Token userToken, final GetGroupsParams params)
			throws GroupsStorageException, InvalidTokenException, AuthenticationException {
		checkNotNull(params, "params");
		final UserName user = getOptionalUser(userToken);
		return storage.getGroups(params, user).stream()
				.map(g -> toMinimalView(user, g)).collect(Collectors.toList());
	}

	private GroupView toMinimalView(final UserName user, final Group g) {
		return GroupView.getBuilder(g, user)
				// this seems odd. Maybe there's a better way to deal with this?
				.withMinimalViewFieldDeterminer(
						f -> validators.getConfigOrEmpty(f.getFieldRoot())
								.map(c -> c.isMinimalViewField()).orElse(false))
				.withPublicFieldDeterminer(
						f -> validators.getConfigOrEmpty(f.getFieldRoot())
								.map(c -> c.isPublicField()).orElse(false))
				.build();
	}
	
	/** Get a set of specified groups. At most 100 groups may be specified.
	 * @param userToken the user's token.
	 * @param groupIDs the IDs of the group to fetch.
	 * @return the groups, listed in the same order as the input IDs.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws NoSuchGroupException if there is no corresponding group for one of the IDs.
	 * @throws IllegalParameterException if more than 100 group IDs are submitted.
	 */
	public List<GroupView> getGroups(final Token userToken, final List<GroupID> groupIDs)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, IllegalParameterException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		if (groupIDs.isEmpty()) {
			return Collections.emptyList();
		}
		if (groupIDs.size() > MAX_GROUP_LIST_COUNT) {
			throw new IllegalParameterException(String.format(
					"No more than %s group IDs may be specified", MAX_GROUP_LIST_COUNT));
		}
		final UserName user = getOptionalUser(userToken);
		final Set<Group> groups = storage.getGroups(groupIDs);
		final Map<Object, GroupView> idToGroup = groups.stream()
				.collect(Collectors.toMap(g -> g.getGroupID(), g -> toMinimalView(user, g)));
		return groupIDs.stream().map(gid -> idToGroup.get(gid)).collect(Collectors.toList());
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
		return createRequestStoreAndNotify(g, user, RequestType.REQUEST, GroupRequest.USER_TYPE,
				ResourceDescriptor.from(user), g.getAdministratorsAndOwner());
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
		return createRequestStoreAndNotify(g, user, RequestType.INVITE, GroupRequest.USER_TYPE,
				ResourceDescriptor.from(newMember), Arrays.asList(newMember));
	}
	
	private GroupRequest createRequestStoreAndNotify(
			final Group group,
			final UserName creator,
			final RequestType type,
			final ResourceType resourceType,
			final ResourceDescriptor resource,
			final Collection<UserName> notifyTargets)
			throws RequestExistsException, GroupsStorageException {
		final Instant now = clock.instant();
		final GroupRequest request = GroupRequest.getBuilder(
				new RequestID(uuidGen.randomUUID()), group.getGroupID(), creator,
				CreateModAndExpireTimes.getBuilder(
						now, now.plus(REQUEST_EXPIRE_TIME)).build())
				.withType(type)
				.withResource(resourceType, resource)
				.build();
		storage.storeRequest(request);
		notifications.notify(notifyTargets, request);
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
			 * a particular admin and a user or resource. The other admins will get a notification
			 * if it's accepted and then can remove the user / resource.
			 * Still though, maybe some admin endpoint or this one should just return any request.
			 * Or maybe admins should be able to view and cancel each other's requests.
			 */
			ensureIsRequestTarget(request, g.isAdministrator(user), user, "access");
			return new GroupRequestWithActions(request,
					request.isOpen() ? TARGET_ACTIONS : NO_ACTIONS);
		}
	}
	
	/** Get information about a group linked to a request.
	 * @param userToken the user's token. The user must be the target of the request or an
	 * administrator of the request target resource.
	 * @param requestID the ID of the request. The request must be an {@link RequestType#INVITE}
	 * and must be {@link GroupRequestStatusType#OPEN}.
	 * @return a minimal view of the group. All public fields are included, and no resource
	 * information is included. The user is treated as a non-member.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchRequestException if there is no such request.
	 * @throws UnauthorizedException if the user may not view the request.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws ClosedRequestException if the request is closed.
	 */
	public GroupView getGroupForRequest(final Token userToken, final RequestID requestID)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, ResourceHandlerException,
				ClosedRequestException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest request = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(request);
		ensureIsRequestTarget(request, g.isAdministrator(user), user, "access");
		if (!request.isInvite()) {
			throw new UnauthorizedException(
					"Only Invite requests may access group information by the request ID");
		}
		ensureIsOpen(request);
		return GroupView.getBuilder(g, null)
				.withOverridePrivateView(true)
				// include all public fields
				.withMinimalViewFieldDeterminer(f -> true)
				.withPublicFieldDeterminer(
						f -> validators.getConfigOrEmpty(f.getFieldRoot())
								.map(c -> c.isPublicField()).orElse(false))
				.build();
	}
	
	/** Get requests that were created by the user.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 */
	public List<GroupRequest> getRequestsForRequester(
			final Token userToken,
			final GetRequestsParams params)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
					NoSuchResourceTypeException {
		checkNotNull(userToken, "userToken");
		checkResourceRegisted(checkNotNull(params, "params"));
		final UserName user = userHandler.getUser(userToken);
		return storage.getRequestsByRequester(user, params);
	}
	
	/** Get requests where the user is the target of the request, including requests
	 * associated with resources the user administrates.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 * @throws UnauthorizedException if the user is not an administrator for the resource.
	 */
	public List<GroupRequest> getRequestsForTarget(
			final Token userToken,
			final GetRequestsParams params)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
				ResourceHandlerException, NoSuchResourceTypeException, NoSuchResourceException,
				IllegalResourceIDException, UnauthorizedException {
		requireNonNull(userToken, "userToken");
		requireNonNull(params, "params");
		final UserName user = userHandler.getUser(userToken);
		final List<GroupRequest> ret;
		if (params.getResourceType().isPresent()) {
			final ResourceType type = params.getResourceType().get();
			final ResourceID id = params.getResourceID().get();
			if (!getHandler(type).isAdministrator(id, user)) {
				throw new UnauthorizedException(String.format(
						"User %s is not an admin for %s %s",
						user.getName(), type.getName(), id.getName()));
			}
			ret = storage.getRequestsByTarget(params);
		} else {
			final Map<ResourceType, Set<ResourceAdministrativeID>> resources = new HashMap<>();
			for (final ResourceType t: resourceHandlers.keySet()) {
				final Set<ResourceAdministrativeID> reslist = resourceHandlers.get(t)
						.getAdministratedResources(user);
				if (!reslist.isEmpty()) {
					resources.put(t, reslist);
				}
			}
			ret = storage.getRequestsByTarget(user, resources, params);
		}
		return ret;
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
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 */
	public List<GroupRequest> getRequestsForGroup(
			final Token userToken,
			final GroupID groupID,
			final GetRequestsParams params)
			throws UnauthorizedException, InvalidTokenException, AuthenticationException,
				NoSuchGroupException, GroupsStorageException, NoSuchResourceTypeException {
		requireNonNull(groupID, "groupID");
		checkResourceRegisted(requireNonNull(params, "params"));
		final UserName user = userHandler.getUser(requireNonNull(userToken, "userToken"));
		final Group g = storage.getGroup(groupID);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format(
					"User %s cannot view requests for group %s",
					user.getName(), groupID.getName()));
		}
		return storage.getRequestsByGroup(groupID, params);
	}
	
	private void checkResourceRegisted(final GetRequestsParams params)
			throws NoSuchResourceTypeException {
		if (params.getResourceType().isPresent()) {
			getHandler(params.getResourceType().get());
		}
	}

	/** Get requests where the user administrates groups that are the target of the request.
	 * At most 100 requests are returned.
	 * @param userToken the user's token.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 */
	public List<GroupRequest> getRequestsForGroups(
			final Token userToken,
			final GetRequestsParams params)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
			NoSuchResourceTypeException {
		checkResourceRegisted(requireNonNull(params, "params"));
		final UserName user = userHandler.getUser(requireNonNull(userToken, "userToken"));
		final Set<GroupID> gids = storage.getAdministratedGroups(user);
		return storage.getRequestsByGroups(gids, params);
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
	 * @throws IllegalParameterException if the reason is too long.
	 */
	public GroupRequest denyRequest(
			final Token userToken,
			final RequestID requestID,
			final String reason)
			throws InvalidTokenException, AuthenticationException, NoSuchRequestException,
				GroupsStorageException, UnauthorizedException, ClosedRequestException,
				ResourceHandlerException, IllegalParameterException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		// fail early on long reason string
		final GroupRequestStatus denied = GroupRequestStatus.denied(user, reason);
		final GroupRequest request = storage.getRequest(requestID);
		final Group group = getGroupFromKnownGoodRequest(request);
		ensureIsRequestTarget(request, group.isAdministrator(user), user, "deny");
		ensureIsOpen(request);
		
		storage.closeRequest(requestID, denied, clock.instant());
		final GroupRequest r = storage.getRequest(requestID);
		//TODO FEEDS who should get notified?
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
		if (request.getResourceType().equals(USER_TYPE)) {
			// don't notify all users when a new user joins, just on resource addition
			notifyTargets.addAll(group.getAdministratorsAndOwner());
		} else {
			notifyTargets.addAll(group.getAllMembers());
		}
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
		if (request.getResourceType().equals(GroupRequest.USER_TYPE)) {
			final UserName target = toUserName(request);
			final Instant now = clock.instant();
			try {
				storage.addMember(groupID, GroupUser.getBuilder(target, now).build(), now);
			} catch (NoSuchGroupException e) {
				// shouldn't happen
				throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
						groupID.getName(), e.getMessage()), e);
			}
			return new HashSet<>(Arrays.asList(target));
		} else {
			return addResourceAndGetAdmins(groupID, request);
		}
	}

	private UserName toUserName(final GroupRequest request) {
		try {
			return new UserName(request.getResource().getResourceID().getName());
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException(String.format("Invalid data in request %s: %s",
					request.getID().getID(), e.getMessage()), e);
		}
	}

	private Set<UserName> addResourceAndGetAdmins(
			final GroupID groupID,
			final GroupRequest request)
			throws ResourceHandlerException, NoSuchResourceException, GroupsStorageException,
				ResourceExistsException {
		final Set<UserName> toNotify;
		try {
			// do this before adding to group in case the resource has been deleted
			toNotify = getHandlerRuntimeException(request)
					.getAdministrators(request.getResource().getResourceID());
		} catch (IllegalResourceIDException e) {
			throw new RuntimeException(String.format("Illegal value stored in request %s: %s",
					request.getID().getID().toString(), e.getMessage()), e);
		}
		try {
			storage.addResource(groupID, request.getResourceType(), request.getResource(),
					clock.instant());
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(String.format("Group %s unexpectedly doesn't exist: %s",
					groupID.getName(), e.getMessage()), e);
		}
		return toNotify;
	}

	private ResourceHandler getHandler(final ResourceType type)
			throws NoSuchResourceTypeException {
		if (!resourceHandlers.containsKey(type)) {
			throw new NoSuchResourceTypeException(type.getName());
		}
		return resourceHandlers.get(type);
	}
	
	private ResourceHandler getHandlerRuntimeException(final GroupRequest request) {
		if (!resourceHandlers.containsKey(request.getResourceType())) {
			throw new RuntimeException(String.format(
					"No handler configured for resource type %s in request %s",
					request.getResourceType().getName(), request.getID().getID().toString()));
		}
		return resourceHandlers.get(request.getResourceType());
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
		} else if (request.isInvite()) {
			if (request.getResourceType().equals(USER_TYPE)) {
				if (user.equals(toUserName(request))) {
					return;
				}
			} else if (isResourceAdministrator(user, request)) {
				return;
			}
		}
		throw new UnauthorizedException(String.format("User %s may not %s request %s",
				user.getName(), actionVerb, request.getID().getID()));
	}
	
	// TODO CODE if the NoSuch exception is thrown, maybe request should be closed
	// returns false if resource is missing / deleted
	private boolean isResourceAdministrator(
			final UserName user,
			final GroupRequest request)
			throws ResourceHandlerException {
		try {
			return getHandlerRuntimeException(request)
					.isAdministrator(request.getResource().getResourceID(), user);
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
	 * @throws UnauthorizedException if the user represented by the token is not a group
	 * administrator.
	 * @throws NoSuchUserException if the user is not a standard group member.
	 * @throws UserIsMemberException if the user is the group owner or an administrator.
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
		if (!group.isAdministrator(user)) {
			throw new UnauthorizedException(
					"Only group administrators can promote administrators");
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
	 * @throws UnauthorizedException if the user represented by the token is not a group
	 * administrator.
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
		if (!group.isAdministrator(user)) {
			throw new UnauthorizedException(
					"Only group administrators can demote administrators");
		}
		// this method will throw an error if the user is not an admin.
		storage.demoteAdmin(groupID, admin, clock.instant());
		// notify? I'm thinking not
	}
	
	/** Add a resource to a group. The resource is added immediately if the user is an
	 * administrator of both the group and the resource. Otherwise, a {@link GroupRequest} is
	 * added to the system and returned.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param resource the resource ID.
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
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 */
	public Optional<GroupRequest> addResource(
			final Token userToken,
			final GroupID groupID,
			final ResourceType type,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, RequestExistsException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException,
				ResourceExistsException, NoSuchResourceTypeException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(type, "type");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		final ResourceHandler h = getHandler(type);
		if (g.containsResource(type, resource)) {
			throw new ResourceExistsException(resource.getName());
		}
		final ResourceDescriptor d = h.getDescriptor(resource);
		final Set<UserName> admins = h.getAdministrators(resource);
		if (g.isAdministrator(user) && admins.contains(user)) {
			storage.addResource(groupID, type, d, clock.instant());
			final Set<UserName> targets = new HashSet<>(admins);
			targets.addAll(g.getAllMembers());
			targets.remove(user);
			notifications.addResource(user, targets, groupID, type, resource);
			return Optional.empty();
		}
		if (admins.contains(user)) {
			return Optional.of(createRequestStoreAndNotify(
					g, user, RequestType.REQUEST, type, d, g.getAdministratorsAndOwner()));
		}
		if (g.isAdministrator(user)) {
			return Optional.of(createRequestStoreAndNotify(
					g, user, RequestType.INVITE, type, d, admins));
		}
		throw new UnauthorizedException(String.format(
				"User %s is not an admin for group %s or %s %s",
				user.getName(), groupID.getName(), type.getName(), resource.getName()));
	}
	
	/** Remove a resource from a group.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param type the type of the resource.
	 * @param resource the resource ID.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not an administrator of the group or the
	 * resource.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws IllegalResourceIDException if the resource ID is illegal.
	 * @throws NoSuchResourceException if there is no such resource.
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 */
	public void removeResource(
			final Token userToken,
			final GroupID groupID,
			final ResourceType type,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException, NoSuchResourceTypeException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(type, "type");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group group = storage.getGroup(groupID);
		final ResourceHandler h = getHandler(type);
		h.getDescriptor(resource); // check that the id is valid
		// should check if ws not in group & fail early?
		if (group.isAdministrator(user) || h.isAdministrator(resource, user)) {
			storage.removeResource(groupID, type, resource, clock.instant());
		} else {
			throw new UnauthorizedException(String.format(
					"User %s is not an admin for group %s or %s %s",
					user.getName(), groupID.getName(), type.getName(), resource.getName()));
		}
	}
	
	/** Set read permissions on a resource that has been requested to be added to a group
	 * for the user if the resource is not already readable (including publicly so). The user
	 * must be a group administrator for the request, the request type must be
	 * {@link RequestType#INVITE}, and the resource type may not be "user".
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
	public void setReadPermission(final Token userToken, final RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException, InvalidTokenException,
				AuthenticationException, UnauthorizedException, ClosedRequestException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(requestID, "requestID");
		final UserName user = userHandler.getUser(userToken);
		final GroupRequest r = storage.getRequest(requestID);
		final Group g = getGroupFromKnownGoodRequest(r);
		if (!g.isAdministrator(user)) {
			throw new UnauthorizedException(String.format("User %s is not an admin for group %s",
					user.getName(), g.getGroupID().getName()));
		}
		if (!RequestType.REQUEST.equals(r.getType())) {
			throw new UnauthorizedException(
					"Only Request type requests allow for resource permissions changes.");
		}
		if (USER_TYPE.equals(r.getResourceType())) {
			throw new UnauthorizedException(
					"Requests with a user resource type do not allow for permissions changes.");
		}
		ensureIsOpen(r);
		final ResourceHandler h = getHandlerRuntimeException(r);
		h.setReadPermission(r.getResource().getResourceID(), user);
	}
	
	/** Set read permission on a resource for the user.
	 * @param userToken the user's token.
	 * @param groupID the ID of the group to be modified.
	 * @param type the type of the resource.
	 * @param resource the resource ID.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if authentication fails.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no such group.
	 * @throws UnauthorizedException if the user is not a member of the group.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 * @throws NoSuchResourceTypeException if the resource type does not exist.
	 * 
	 */
	public void setReadPermission(
			final Token userToken,
			final GroupID groupID,
			final ResourceType type,
			final ResourceID resource)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException, UnauthorizedException, NoSuchResourceTypeException,
				NoSuchResourceException, ResourceHandlerException {
		checkNotNull(userToken, "userToken");
		checkNotNull(groupID, "groupID");
		checkNotNull(type, "type");
		checkNotNull(resource, "resource");
		final UserName user = userHandler.getUser(userToken);
		final Group g = storage.getGroup(groupID);
		if (!g.isMember(user)) {
			throw new UnauthorizedException(String.format("User %s is not a member of group %s",
					user.getName(), g.getGroupID().getName()));
		}
		final ResourceHandler h = getHandler(type);
		if (!g.containsResource(type, resource)) {
			throw new NoSuchResourceException(String.format("Group %s does not contain %s %s",
					groupID.getName(), type.getName(), resource.getName()));
		}
		try {
			h.setReadPermission(resource, user);
		} catch (IllegalResourceIDException e) {
			// since the resourceID is in the group
			throw new RuntimeException("This should be impossible", e);
		}
	}
}
