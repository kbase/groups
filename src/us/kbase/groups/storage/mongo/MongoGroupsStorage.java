package us.kbase.groups.storage.mongo;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.UpdateResult;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDAndName;
import us.kbase.groups.core.GroupIDNameMembership;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalString;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.Group.Role;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;
import us.kbase.groups.storage.exceptions.StorageInitException;

/** A {@link GroupsStorage} implementation using MongoDB as the backend.
 * @author gaprice@lbl.gov
 *
 */
public class MongoGroupsStorage implements GroupsStorage {
	
	/* Don't use mongo built in object mapping to create the returned objects
	 * since that tightly couples the classes to the storage implementation.
	 * Instead, if needed, create classes specific to the implementation for
	 * mapping purposes that produce the returned classes.
	 */
	
	/* Testing the (many) catch blocks for the general mongo exception is pretty hard, since it
	 * appears as though the mongo clients have a heartbeat, so just stopping mongo might trigger
	 * the heartbeat exception rather than the exception you're going for.
	 * 
	 * Mocking the mongo client is probably not the answer:
	 * http://stackoverflow.com/questions/7413985/unit-testing-with-mongodb
	 * https://github.com/mockito/mockito/wiki/How-to-write-good-tests
	 */
	
	private static final int SCHEMA_VERSION = 1;
	
	// collection names
	private static final String COL_CONFIG = "config";
	
	private static final String COL_GROUPS = "groups";
	private static final String COL_REQUESTS = "requests";
	
	private static final Map<String, Map<List<String>, IndexOptions>> INDEXES;
	private static final IndexOptions IDX_UNIQ = new IndexOptions().unique(true);
//	private static final IndexOptions IDX_SPARSE = new IndexOptions().sparse(true);
	private static final IndexOptions IDX_UNIQ_SPARSE = new IndexOptions()
			.unique(true).sparse(true);
	static {
		//hardcoded indexes
		INDEXES = new HashMap<String, Map<List<String>, IndexOptions>>();
		
		// groups indexes
		final Map<List<String>, IndexOptions> groups = new HashMap<>();
		// will probably need to sort by time at some point
		groups.put(Arrays.asList(Fields.GROUP_ID), IDX_UNIQ);
		// find by owner and sort by ID
		groups.put(Arrays.asList(Fields.GROUP_OWNER, Fields.GROUP_ID), null);
		// find by admin and sort by ID
		groups.put(Arrays.asList(Fields.GROUP_ADMINS, Fields.GROUP_ID), null);
		// find public groups and sort by ID (not needed?)
		groups.put(Arrays.asList(Fields.GROUP_IS_PRIVATE, Fields.GROUP_ID), null);
		// find groups by member and sort by ID
		groups.put(Arrays.asList(
				Fields.GROUP_MEMBERS + Fields.FIELD_SEP + Fields.GROUP_MEMBER_NAME,
				Fields.GROUP_ID), null);
		INDEXES.put(COL_GROUPS, groups);
		
		// requests indexes
		// TODO CODE mongo 3.2 has partial indexes that might help here
		final Map<List<String>, IndexOptions> requests = new HashMap<>();
		// may need compound indexes to speed things up.
		requests.put(Arrays.asList(Fields.REQUEST_ID), IDX_UNIQ);
		// find by group & type & sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_GROUP_ID, Fields.REQUEST_TYPE,
				Fields.REQUEST_MODIFICATION), null);
		// find by group, status, and type and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_GROUP_ID, Fields.REQUEST_STATUS,
				Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION), null);
		// find by requester and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_REQUESTER, Fields.REQUEST_MODIFICATION), null);
		// find by requester and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_REQUESTER, Fields.REQUEST_STATUS,
				Fields.REQUEST_MODIFICATION), null);
		// find by resource admin ID and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION),
				null);
		// find by resource admin ID and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_STATUS, Fields.REQUEST_TYPE,
				Fields.REQUEST_MODIFICATION), null);
		// find by resource ID and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION),
				null);
		// find by resource ID and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_STATUS, Fields.REQUEST_TYPE,
				Fields.REQUEST_MODIFICATION), null);
		// find by resource ID and requester and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID, Fields.REQUEST_REQUESTER,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION),
				null);
		// find by resource ID and requester and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID, Fields.REQUEST_REQUESTER,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_STATUS, Fields.REQUEST_TYPE,
				Fields.REQUEST_MODIFICATION), null);
		// find by resource ID and group and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID, Fields.REQUEST_GROUP_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION),
				null);
		// find by resource ID and group and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ID, Fields.REQUEST_GROUP_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_STATUS, Fields.REQUEST_TYPE,
				Fields.REQUEST_MODIFICATION), null);
		// find expired requests.
		requests.put(Arrays.asList(Fields.REQUEST_EXPIRATION), null);
		// ensure equivalent requests are rejected. See getCharacteristicString()
		requests.put(Arrays.asList(Fields.REQUEST_CHARACTERISTIC_STRING), IDX_UNIQ_SPARSE);
		INDEXES.put(COL_REQUESTS, requests);
		
		//config indexes
		final Map<List<String>, IndexOptions> cfg = new HashMap<>();
		//ensure only one config object
		cfg.put(Arrays.asList(Fields.DB_SCHEMA_KEY), IDX_UNIQ);
		INDEXES.put(COL_CONFIG, cfg);
	}
	
	private static final long EXPIRATION_AGENT_FREQUENCY_SEC = 60;
	
	private ScheduledExecutorService executor;
	private boolean expirationAgentRunning = false;
	
	private final MongoDatabase db;
	private final Clock clock;
	
	/** Create MongoDB based storage for the Groups application.
	 * @param db the MongoDB database the storage system will use.
	 * @param types the resource types for which indexes will be created. Querying against
	 * resource types not declared here will result in a table scan. Note that 3 indexes
	 * are created per type and 5 indexes are created automatically, which means that at most
	 * 19 types can be registered given MongoDBs 64 index / collection limit.
	 * @throws StorageInitException if the storage system could not be initialized.
	 */
	public MongoGroupsStorage(final MongoDatabase db, final Collection<ResourceType> types)
			throws StorageInitException {
		this(db, types, Clock.systemDefaultZone());
	}
	
	// for tests
	private MongoGroupsStorage(
			final MongoDatabase db,
			final Collection<ResourceType> types,
			final Clock clock)
			throws StorageInitException {
		checkNotNull(db, "db");
		checkNoNullsInCollection(types, "types");
		this.db = db;
		this.clock = clock;
		ensureIndexes(INDEXES); // MUST come before check config
		ensureIndexes(types);
		checkConfig();
		startExpirationAgent(EXPIRATION_AGENT_FREQUENCY_SEC);
	}
	
	/** Schedule the request expiration agent with the given period between expirations.
	 * The agent calls {@link #expireRequests(Instant)}
	 * every periodInSeconds with the current {@link Instant} from a {@link Clock#instant()}.
	 * @param periodInSeconds how often the reaper runs.
	 * @throws IllegalArgumentException if the reaper is already running or the period is less
	 * than or equal to zero.
	 */
	public synchronized void startExpirationAgent(long periodInSeconds) {
		if (expirationAgentRunning) {
			throw new IllegalArgumentException("The expiration agent is already running");
		}
		if (periodInSeconds <= 0) {
			throw new IllegalArgumentException("periodInSeconds must be > 0");
		}
		expirationAgentRunning = true;
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(
				new ExpirationAgent(), 0, periodInSeconds, TimeUnit.SECONDS);
	}
	
	/** Returns true if the expiration agent is running, false otherwise.
	 * @return true if the agent is running.
	 */
	public synchronized boolean isExpirationAgentRunning() {
		return expirationAgentRunning;
	}
	
	/** Stops the expiration agent from running again. Call {@link #startExpirationAgent(long)}
	 * to restart the agent.
	 * Calling this method multiple times in succession has no effect.
	 */
	public synchronized void stopExpirationAgent() {
		executor.shutdown();
		expirationAgentRunning = false;
	}
	
	private class ExpirationAgent implements Runnable {

		@Override
		public void run() {
			try {
				LoggerFactory.getLogger(getClass()).info("Running expiration agent");
				expireRequests(clock.instant());
			} catch (Throwable e) {
				// the only error that can really occur here is losing the connection to mongo,
				// so we just punt, log, and retry next time.
				// unfortunately this is really difficult to test in an automated way, so test
				// manually by shutting down mongo.
				LoggerFactory.getLogger(getClass())
						.error("Error expiring requests: " + e.getMessage(), e);
			}
		}
	}
	
	
	private void checkConfig() throws StorageInitException  {
		final MongoCollection<Document> col = db.getCollection(COL_CONFIG);
		final Document cfg = new Document(Fields.DB_SCHEMA_KEY, Fields.DB_SCHEMA_VALUE);
		cfg.put(Fields.DB_SCHEMA_UPDATE, false);
		cfg.put(Fields.DB_SCHEMA_VERSION, SCHEMA_VERSION);
		try {
			col.insertOne(cfg);
		} catch (MongoWriteException dk) {
			if (!DuplicateKeyExceptionChecker.isDuplicate(dk)) {
				throw new StorageInitException("There was a problem communicating with the " +
						"database: " + dk.getMessage(), dk);
			}
			// ok, duplicate key means the version doc is already there, this isn't the first
			// startup
			if (col.countDocuments() != 1) {
				// if this occurs the indexes are broken, so there's no way to test without
				// altering ensureIndexes()
				throw new StorageInitException(
						"Multiple config objects found in the database. " +
						"This should not happen, something is very wrong.");
			}
			final FindIterable<Document> cur = db.getCollection(COL_CONFIG)
					.find(Filters.eq(Fields.DB_SCHEMA_KEY, Fields.DB_SCHEMA_VALUE));
			final Document doc = cur.first();
			if ((Integer) doc.get(Fields.DB_SCHEMA_VERSION) != SCHEMA_VERSION) {
				throw new StorageInitException(String.format(
						"Incompatible database schema. Server is v%s, DB is v%s",
						SCHEMA_VERSION, doc.get(Fields.DB_SCHEMA_VERSION)));
			}
			if ((Boolean) doc.get(Fields.DB_SCHEMA_UPDATE)) {
				throw new StorageInitException(String.format(
						"The database is in the middle of an update from " +
								"v%s of the schema. Aborting startup.", 
								doc.get(Fields.DB_SCHEMA_VERSION)));
			}
		} catch (MongoException me) {
			throw new StorageInitException(
					"There was a problem communicating with the database: " + me.getMessage(), me);
		}
	}

	private void ensureIndexes(Map<String, Map<List<String>, IndexOptions>> indexes)
			throws StorageInitException {
		for (final String col: indexes.keySet()) {
			for (final List<String> idx: indexes.get(col).keySet()) {
				final Document index = new Document();
				final IndexOptions opts = indexes.get(col).get(idx);
				for (final String field: idx) {
					index.put(field, 1);
				}
				final MongoCollection<Document> dbcol = db.getCollection(col);
				try {
					if (opts == null) {
						dbcol.createIndex(index);
					} else {
						dbcol.createIndex(index, opts);
					}
				} catch (MongoException me) {
					throw new StorageInitException(
							"Failed to create index: " + me.getMessage(), me);
				}
			}
		}
	}
	private void ensureIndexes(final Collection<ResourceType> types) throws StorageInitException {
		final Map<List<String>, IndexOptions> groups = new HashMap<>();
		for (final ResourceType t: types) {
			/* Note admins and members cannot be indexed here because mongo will not allow
			 * a compound index including more than one array.
			 * Hence we add a general resource ID index and allow Mongo to select the best index
			 * to use when doing user + resource ID look ups (and it might use both and combine
			 * the results).
			 */
			final String resourceIDField = Fields.GROUP_RESOURCES + Fields.FIELD_SEP + t.getName()
					+ Fields.FIELD_SEP + Fields.GROUP_RESOURCE_ID;
			// find by resource ID and sort by ID
			groups.put(Arrays.asList(resourceIDField, Fields.GROUP_ID), null);
			// find by resource ID & owner and sort by ID
			groups.put(Arrays.asList(resourceIDField, Fields.GROUP_OWNER, Fields.GROUP_ID), null);
			// find public groups by resource ID and sort by ID (not needed?)
			groups.put(Arrays.asList(resourceIDField, Fields.GROUP_IS_PRIVATE, Fields.GROUP_ID),
					null);
		}
		
		ensureIndexes(ImmutableMap.of(COL_GROUPS, groups));
	}
	
	private static class DuplicateKeyExceptionChecker {
		
		// might need this stuff later, so keeping for now.
		
		// super hacky and fragile, but doesn't seem another way to do this.
		private final Pattern keyPattern = Pattern.compile("dup key:\\s+\\{ : \"(.*)\" \\}");
		private final Pattern indexPattern = Pattern.compile(
				"duplicate key error (index|collection): " +
				"\\w+\\.(\\w+)( index: |\\.\\$)([\\.\\w]+)\\s+");
		
		private final boolean isDuplicate;
		private final Optional<String> collection;
		private final Optional<String> index;
		private final Optional<String> key;
		
		public DuplicateKeyExceptionChecker(final MongoWriteException mwe)
				throws GroupsStorageException {
			// split up indexes better at some point - e.g. in a Document
			isDuplicate = isDuplicate(mwe);
			if (isDuplicate) {
				final Matcher indexMatcher = indexPattern.matcher(mwe.getMessage());
				if (indexMatcher.find()) {
					collection = Optional.of(indexMatcher.group(2));
					index = Optional.of(indexMatcher.group(4));
				} else {
					throw new GroupsStorageException(
							"Unable to parse duplicate key error: " +
							// could include a token hash as the key, so split it out if it's there
							mwe.getMessage().split("dup key")[0], mwe);
				}
				final Matcher keyMatcher = keyPattern.matcher(mwe.getMessage());
				if (keyMatcher.find()) {
					key = Optional.of(keyMatcher.group(1));
				} else { // some errors include the dup key, some don't
					key = Optional.empty();
				}
			} else {
				collection = Optional.empty();
				index = Optional.empty();
				key = Optional.empty();
			}
		}
		
		public static boolean isDuplicate(final MongoWriteException mwe) {
			return mwe.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY);
		}
		
		public boolean isDuplicate() {
			return isDuplicate;
		}

		public Optional<String> getCollection() {
			return collection;
		}

		public Optional<String> getIndex() {
			return index;
		}

		@SuppressWarnings("unused") // may need later
		public Optional<String> getKey() {
			return key;
		}
	}

	@Override
	public void createGroup(final Group group)
			throws GroupExistsException, GroupsStorageException {
		checkNotNull(group, "group");
		final Map<String, List<Document>> resources = new HashMap<>();
		final Document u = new Document(
				Fields.GROUP_ID, group.getGroupID().getName())
				.append(Fields.GROUP_NAME, group.getGroupName().getName())
				.append(Fields.GROUP_IS_PRIVATE, group.isPrivate())
				.append(Fields.GROUP_MEMBERS_PRIVATE, group.isPrivateMemberList())
				.append(Fields.GROUP_OWNER, group.getOwner().getName())
				.append(Fields.GROUP_MEMBERS, toMembersDocList(group))
				.append(Fields.GROUP_ADMINS, toStringList(group.getAdministrators()))
				.append(Fields.GROUP_RESOURCES, resources)
				.append(Fields.GROUP_CREATION, Date.from(group.getCreationDate()))
				.append(Fields.GROUP_MODIFICATION, Date.from(group.getModificationDate()))
				.append(Fields.GROUP_CUSTOM_FIELDS, getCustomFields(group.getCustomFields()));
		for (final ResourceType t: group.getResourceTypes()) {
			resources.put(t.getName(), group.getResources(t).stream()
					.map(rd -> new Document(
							Fields.GROUP_RESOURCE_ADMINISTRATIVE_ID, rd.getAdministrativeID()
									.getName())
							.append(Fields.GROUP_RESOURCE_ID, rd.getResourceID().getName())
							.append(Fields.GROUP_RESOURCE_ADDED,
									group.getResourceAddDate(t, rd.getResourceID()).orElse(null)))
					.collect(Collectors.toList()));
		}
		try {
			db.getCollection(COL_GROUPS).insertOne(u);
		} catch (MongoWriteException mwe) {
			// not happy about this, but getDetails() returns an empty map
			if (DuplicateKeyExceptionChecker.isDuplicate(mwe)) {
				throw new GroupExistsException(group.getGroupID().getName());
			} else {
				// painful to test
				throw new GroupsStorageException("Database write failed", mwe);
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}

	private List<Document> toMembersDocList(final Group group) {
		return group.getAllMembers().stream().map(u -> toDoc(group.getMember(u)))
				.collect(Collectors.toList());
	}

	private Document toDoc(final GroupUser gu) {
		return new Document(Fields.GROUP_MEMBER_NAME, gu.getName().getName())
				.append(Fields.GROUP_MEMBER_JOIN_DATE, Date.from(gu.getJoinDate()))
				.append(Fields.GROUP_MEMBER_VISIT_DATE, gu.getLastVisit()
						.map(i -> Date.from(i)).orElse(null))
				.append(Fields.GROUP_MEMBER_CUSTOM_FIELDS,
						getCustomFields(gu.getCustomFields()));
	}

	private GroupsStorageException wrapMongoException(MongoException e) {
		return new GroupsStorageException("Connection to database failed: " +
				e.getMessage(), e);
	}
	
	private Map<String, String> getCustomFields(
			final Map<NumberedCustomField, String> customFields) {
		return customFields.entrySet().stream().collect(Collectors.toMap(
				e -> e.getKey().getField(),
				e -> e.getValue()));
	}
	
	private List<String> toStringList(final Set<UserName> users) {
		return users.stream().map(m -> m.getName()).collect(Collectors.toList());
	}
	
	@Override
	public void updateGroup(final GroupUpdateParams update, final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException {
		checkNotNull(update, "update");
		checkNotNull(modDate, "modDate");
		if (!update.hasUpdate()) {
			return;
		}
		final List<Document> or = new LinkedList<>();
		final Document query = new Document(Fields.GROUP_ID, update.getGroupID().getName())
				.append("$or", or);
		final Document set = new Document(Fields.GROUP_MODIFICATION, Date.from(modDate));
		
		buildUpdate(or, set, update.getGroupName(), Fields.GROUP_NAME, n -> n.get().getName());
		final OptionalGroupFields opts = update.getOptionalFields();
		buildUpdate(or, set, opts.isPrivate(), Fields.GROUP_IS_PRIVATE, p -> p.get());
		buildUpdate(or, set, opts.isPrivateMemberList(), Fields.GROUP_MEMBERS_PRIVATE,
				p -> p.get());
		final Document mod = buildQueryAndUpdateForCustomFields(
				opts.getCustomFields(),
				f -> opts.getCustomValue(f),
				Fields.GROUP_CUSTOM_FIELDS + Fields.FIELD_SEP,
				Fields.GROUP_CUSTOM_FIELDS + Fields.FIELD_SEP,
				or,
				set);
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, mod);
			if (res.getMatchedCount() != 1) {
				getGroup(update.getGroupID()); //throws no such group
				// otherwise we don't care - the update made no changes.
			}
			// if it matches, it gets modified, so we don't check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	/** This method is for updating custom fields in groups and users. It modifies the
	 * query in place (queryOr) to ensure the query *does not* match documents where the
	 * custom field changes would not modify the document (other fields in the or document
	 * might make the query match, of course). The set document (which is expected to already
	 * contain at least one update, and so is always contained in the returned document)
	 * is updated in place to update the custom fields to their new value.
	 * @param fields the custom fields.
	 * @param valueGetter a function to get the value of a custom field.
	 * @param queryFieldPrefix the prefix for the custom field query field. The actual field
	 * name is added to this prefix to point to the custom field. 
	 * @param updateFieldPrefix the prefix for the custom field update field. The update
	 * will be applied to this field.
	 * @param queryOr a list that will be modified in place with conditions that match the 
	 * negation of the custom field update. Thus at least one update must actually make a change
	 * to the document for the query to match.
	 * @param set a document that will be modified in place to update the custom fields.
	 * @return the overall update document, containing the set document as well as an
	 * unset document.
	 */
	private Document buildQueryAndUpdateForCustomFields(
			final Set<NumberedCustomField> fields,
			final Function<NumberedCustomField, OptionalString> valueGetter,
			final String queryFieldPrefix,
			final String updateFieldPrefix,
			final List<Document> queryOr,
			final Document set) {
		final Document unset = new Document();
		for (final NumberedCustomField ncf: fields) {
			final OptionalString item = valueGetter.apply(ncf);
			final String updateField = updateFieldPrefix + ncf.getField();
			// check that the field is different. At least one field must be different
			// for the update to occur
			queryOr.add(new Document(queryFieldPrefix + ncf.getField(),
					new Document("$ne", item.orNull())));
			if (item.isPresent()) {
				set.append(updateField, item.get());
			} else {
				unset.append(updateField, "");
			}
		}
		// set has the mod date in it, so always include
		final Document update = new Document("$set", set);
		if (!unset.isEmpty()) {
			update.append("$unset", unset);
		}
		return update;
	}
	
	private <T> void buildUpdate(
			final List<Document> or,
			final Document set,
			final Optional<T> item,
			final String field,
			final Function<Optional<T>, Object> getValue) {
		if (item.isPresent()) {
			final Object value = getValue.apply(item);
			or.add(new Document(field, new Document("$ne", value)));
			set.append(field, value);
		}
	}
	
	@Override
	public Group getGroup(final GroupID groupID)
			throws GroupsStorageException, NoSuchGroupException {
		return toGroup(getGroupDoc(groupID, null));
	}

	private Document getGroupDoc(final GroupID groupID, final Document projection)
			throws GroupsStorageException, NoSuchGroupException {
		requireNonNull(groupID, "groupID");
		final Document grp = findOne(
				COL_GROUPS, new Document(Fields.GROUP_ID, groupID.getName()), projection);
		if (grp == null) {
			throw new NoSuchGroupException(groupID.getName());
		}
		return grp;
	}
	
	@Override
	public Set<Group> getGroups(final Collection<GroupID> groupIDs)
			throws NoSuchGroupException, GroupsStorageException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		final Document query = new Document(Fields.GROUP_ID, new Document("$in", groupIDs.stream()
				.map(g -> g.getName()).collect(Collectors.toList())));
		final List<Group> retgrp = getList(COL_GROUPS, query, null, null, 0, d ->toGroup(d));
		final Set<GroupID> got = retgrp.stream().map(g -> g.getGroupID())
				.collect(Collectors.toSet());
		final Set<GroupID> missing = new HashSet<>(groupIDs); // in case groups is immutable
		missing.removeAll(got);
		if (!missing.isEmpty()) {
			throw new NoSuchGroupException(missing.iterator().next().getName());
		}
		return new HashSet<>(retgrp);
	}
	
	@Override
	public List<GroupIDNameMembership> getGroupNames(
			final UserName user,
			final Collection<GroupID> groupIDs)
			throws NoSuchGroupException, GroupsStorageException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		final Document projection = new Document(Fields.GROUP_ID, 1)
				.append(Fields.GROUP_IS_PRIVATE, 1)
				.append(Fields.GROUP_NAME, 1)
				.append(Fields.MONGO_ID, 0);
		if (user != null) {
			projection.append(Fields.GROUP_MEMBERS, new Document("$elemMatch",
					new Document(Fields.GROUP_MEMBER_NAME, user.getName())));
		}
		final Document query = new Document(Fields.GROUP_ID, new Document("$in",
				groupIDs.stream().map(i -> i.getName()).collect(Collectors.toList())));
		final Document sort = new Document(Fields.GROUP_ID, 1);
		
		final List<GroupIDNameMembership> ret = getList(
				COL_GROUPS, query, projection, sort, 0, d -> toGroupIDNameMembership(d));
		if (ret.size() != groupIDs.size()) {
			final Set<GroupID> got = ret.stream().map(g -> g.getID())
					.collect(Collectors.toSet());
			for (final GroupID g: groupIDs) { // can't cover this, guaranteed thrown exception
				if (!got.contains(g)) {
					throw new NoSuchGroupException(g.getName());
				}
			}
		}
		return ret;
	}

	private GroupIDNameMembership toGroupIDNameMembership(final Document gdoc)
			throws GroupsStorageException {
		try {
			return GroupIDNameMembership.getBuilder(new GroupID(gdoc.getString(Fields.GROUP_ID)))
					.withGroupName(new GroupName(gdoc.getString(Fields.GROUP_NAME)))
					.withIsMember(gdoc.containsKey(Fields.GROUP_MEMBERS))
					.withIsPrivate(gdoc.getBoolean(Fields.GROUP_IS_PRIVATE))
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new GroupsStorageException("Unexpected value in database: " + e.getMessage(), e);
		}
	}

	private GroupIDAndName toGroupIDAndName(final Document grp) throws GroupsStorageException {
		try {
			return GroupIDAndName.of(
					new GroupID(grp.getString(Fields.GROUP_ID)),
					new GroupName(grp.getString(Fields.GROUP_NAME)));
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new GroupsStorageException("Unexpected value in database: " + e.getMessage(), e);
		}
	}
	
	private GroupID toGroupID(final Document grp) throws GroupsStorageException {
		try {
			return new GroupID(grp.getString(Fields.GROUP_ID));
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new GroupsStorageException("Unexpected value in database: " + e.getMessage(), e);
		}
	}
	
	@Override
	public boolean getGroupExists(final GroupID groupID) throws GroupsStorageException {
		requireNonNull(groupID, "groupID");
		try {
			return db.getCollection(COL_GROUPS)
					.countDocuments(new Document(Fields.GROUP_ID, groupID.getName())) == 1;
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	@Override
	public List<GroupIDAndName> getMemberGroups(final UserName user)
			throws GroupsStorageException {
		// add filters for admin/owner later
		final Document query = new Document(Fields.GROUP_MEMBERS + Fields.FIELD_SEP +
				Fields.GROUP_MEMBER_NAME, requireNonNull(user, "user").getName());
		final Document projection = new Document(Fields.GROUP_ID, 1)
				.append(Fields.GROUP_NAME, 1)
				.append(Fields.MONGO_ID, 0);
		final Document sort = new Document(Fields.GROUP_ID, 1);
		
		return getList(COL_GROUPS, query, projection, sort, 0, d -> toGroupIDAndName(d));
	}

	@Override
	public Set<GroupID> getAdministratedGroups(final UserName user)
			throws GroupsStorageException {
		requireNonNull(user, "user");
		return new HashSet<>(getList( // could make a get set method but meh
				COL_GROUPS,
				new Document("$or", Arrays.asList(
						new Document(Fields.GROUP_OWNER, user.getName()),
						new Document(Fields.GROUP_ADMINS, user.getName()))),
				new Document(Fields.GROUP_ID, 1).append(Fields.MONGO_ID, 1),
				null,
				0,
				d -> toGroupID(d)));
	}
	
	private static interface FnParamExcept<T, R> {
		
		R apply(T t) throws GroupsStorageException;
	}

	private <T> List<T> getList(
			final String collection,
			final Document query,
			final Document projection,
			final Document sort,
			final int limit,
			final FnParamExcept<Document, T> docToClass)
			throws GroupsStorageException {
		final List<T> ret = new LinkedList<>();
		try {
			final FindIterable<Document> gdocs = db.getCollection(collection)
					.find(query)
					.projection(projection)
					.sort(sort)
					.limit(limit); 
			for (final Document gdoc: gdocs) {
				ret.add(docToClass.apply(gdoc));
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
		return ret;
	}
	
	@Override
	public List<Group> getGroups(
			final GetGroupsParams params,
			final boolean resourceIsPublic,
			final UserName user)
			throws GroupsStorageException {
		// ugh. This method is not pretty.
		requireNonNull(params, "params");
		if (user == null && (!params.getRole().equals(Role.NONE) ||
				(params.getResourceType().isPresent() && !resourceIsPublic))) {
			return Collections.emptyList();
		}
		final Document query = new Document();
		if (params.getExcludeUpTo().isPresent()) {
			final String inequality = params.isSortAscending() ? "$gt" : "$lt";
			query.append(Fields.GROUP_ID, new Document(inequality, params.getExcludeUpTo().get()));
		}
		final String memberField = Fields.GROUP_MEMBERS + Fields.FIELD_SEP +
				Fields.GROUP_MEMBER_NAME;
		appendResourceInPlace(params, query); // resource is public if present && user == null
		if (user == null) {
			query.append(Fields.GROUP_IS_PRIVATE, false);
		} else if (params.getRole().equals(Role.NONE)) {
			// members array contains all members
			final Document memberQuery = new Document(memberField, user.getName());
			if (resourceIsPublic || !params.getResourceType().isPresent()) {
				final Document pubquery = new Document(Fields.GROUP_IS_PRIVATE, false);
				query.append("$or", Arrays.asList(pubquery, memberQuery));
			} else {
				query.putAll(memberQuery);
			}
		} else if (params.getRole().equals(Role.OWNER)) {
			query.append(Fields.GROUP_OWNER, user.getName());
		} else if (params.getRole().equals(Role.ADMIN)) {
			query.append("$or", Arrays.asList(
					new Document(Fields.GROUP_ADMINS, user.getName()),
					new Document(Fields.GROUP_OWNER, user.getName())
					));
		} else {
			// members array contains all members
			query.append(memberField, user.getName());
		}
		// may want to allow alternate sorts later, will need indexes
		final Document sort = new Document(Fields.GROUP_ID, params.isSortAscending() ? 1 : -1);
		// could make limit a param (with a max), YAGNI for now
		return getList(COL_GROUPS, query, new Document(), sort, 100, d -> toGroup(d));
	}
	
	private Document appendResourceInPlace(final GetGroupsParams params, final Document query) {
		if (params.getResourceType().isPresent()) {
			final String resourceKey = Fields.GROUP_RESOURCES + Fields.FIELD_SEP +
					params.getResourceType().get().getName() + Fields.FIELD_SEP +
					Fields.GROUP_RESOURCE_ID;
			query.append(resourceKey, params.getResourceID().get().getName());
		}
		return query;
	}

	private Group toGroup(final Document grp) throws GroupsStorageException {
		try {
			final Map<UserName, GroupUser> members = getMembers(grp);
			final UserName owner = new UserName(grp.getString(Fields.GROUP_OWNER));
			final Group.Builder b = Group.getBuilder(
					new GroupID(grp.getString(Fields.GROUP_ID)),
					new GroupName(grp.getString(Fields.GROUP_NAME)),
					members.remove(owner),
					new CreateAndModTimes(
							grp.getDate(Fields.GROUP_CREATION).toInstant(),
							grp.getDate(Fields.GROUP_MODIFICATION).toInstant()))
					.withIsPrivate(grp.getBoolean(Fields.GROUP_IS_PRIVATE))
					.withPrivateMemberList(grp.getBoolean(Fields.GROUP_MEMBERS_PRIVATE, true));
			getUserSet(grp, Fields.GROUP_ADMINS).stream().forEach(a ->
					b.withAdministrator(members.remove(a)));
			members.values().stream().forEach(m -> b.withMember(m));
			@SuppressWarnings("unchecked")
			final Map<String, List<Document>> resources =
					(Map<String, List<Document>>) grp.get(Fields.GROUP_RESOURCES);
			for (final String restype: resources.keySet()) {
				final ResourceType t = new ResourceType(restype);
				for (final Document rd: resources.get(restype)) {
					final Date added = rd.getDate(Fields.GROUP_RESOURCE_ADDED);
					b.withResource(
							t,
							new ResourceDescriptor(
									new ResourceAdministrativeID(
											rd.getString(Fields.GROUP_RESOURCE_ADMINISTRATIVE_ID)),
									new ResourceID(rd.getString(Fields.GROUP_RESOURCE_ID))),
							added == null ? null : added.toInstant());
				}
			}
			addCustomFields((f, v) -> b.withCustomField(f, v), Fields.GROUP_CUSTOM_FIELDS, grp);
			return b.build();
		} catch (MissingParameterException | IllegalParameterException | NullPointerException |
				IllegalArgumentException e) {
			throw new GroupsStorageException(
					"Unexpected value in database: " + e.getMessage(), e);
		}
	}
	
	private void addCustomFields(
			final BiConsumer<NumberedCustomField, String> fieldConsumer,
			final String customFieldField,
			final Document groupDoc)
			throws IllegalParameterException, MissingParameterException {
		@SuppressWarnings("unchecked")
		final Map<String, String> custom = (Map<String, String>) groupDoc.get(customFieldField);
		for (final String field: custom.keySet()) {
			fieldConsumer.accept(new NumberedCustomField(field), custom.get(field));
		}
	}
	
	private Set<UserName> getUserSet(final Document grp, final String field)
			throws MissingParameterException, IllegalParameterException {
		@SuppressWarnings("unchecked")
		final List<String> users = (List<String>) grp.get(field);
		final Set<UserName> ret = new HashSet<>();
		for (final String s: users) {
			ret.add(new UserName(s));
		}
		return ret;
	}

	private Map<UserName, GroupUser> getMembers(final Document grp)
			throws MissingParameterException, IllegalParameterException {
		@SuppressWarnings("unchecked")
		final List<Document> members = (List<Document>) grp.get(Fields.GROUP_MEMBERS);
		final Map<UserName, GroupUser> ret = new HashMap<>();
		for (final Document m: members) {
			final UserName u = new UserName(m.getString(Fields.GROUP_MEMBER_NAME));
			final GroupUser.Builder b = GroupUser.getBuilder(
					u, m.getDate(Fields.GROUP_MEMBER_JOIN_DATE).toInstant())
					.withNullableLastVisit(Optional.ofNullable(
							m.getDate(Fields.GROUP_MEMBER_VISIT_DATE))
							.map(d -> d.toInstant()).orElse(null));
			addCustomFields(
					(f, v) -> b.withCustomField(f, v), Fields.GROUP_MEMBER_CUSTOM_FIELDS, m);
			ret.put(u, b.build());
		}
		return ret;
	}

	@Override
	public void addMember(final GroupID groupID, final GroupUser member, final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException {
		requireNonNull(member, "member");
		try {
			addUser(groupID, member.getName(), toDoc(member), modDate, false);
		} catch (NoSuchUserException e) {
			throw new RuntimeException("This should be impossible", e);
		}
	}
	
	@Override
	public void addAdmin(final GroupID groupID, final UserName admin, final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException,
				NoSuchUserException {
		requireNonNull(admin, "admin");
		addUser(groupID, admin, admin.getName(), modDate, true);
	}

	private void addUser(
			final GroupID groupID,
			final UserName member,
			final Object memberDocument,
			final Instant modDate,
			final boolean asAdmin)
			throws GroupsStorageException, NoSuchGroupException, UserIsMemberException,
				NoSuchUserException {
		requireNonNull(groupID, "groupID");
		requireNonNull(modDate, "modDate");
		
		final Document notEqualToMember = new Document("$ne", member.getName());
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(Fields.GROUP_OWNER, notEqualToMember)
				.append(Fields.GROUP_ADMINS, notEqualToMember)
				.append(Fields.GROUP_MEMBERS + Fields.FIELD_SEP + Fields.GROUP_MEMBER_NAME,
						asAdmin ? member.getName() : notEqualToMember);
			
		final Document modification =
				new Document("$addToSet", new Document(
						asAdmin ? Fields.GROUP_ADMINS : Fields.GROUP_MEMBERS, memberDocument))
				.append("$set", new Document(Fields.GROUP_MODIFICATION, Date.from(modDate)));
		
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, modification);
			if (res.getMatchedCount() != 1) {
				handleNoMatchOnUserAdd(groupID, member, asAdmin);
			}
			// if it matches, it gets modified, so we don't check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	private void handleNoMatchOnUserAdd(
			final GroupID groupID,
			final UserName member,
			final boolean asAdmin)
			throws GroupsStorageException, NoSuchGroupException, UserIsMemberException,
				NoSuchUserException {
		final Group g = getGroup(groupID); // will throw no such group
		if (g.getOwner().equals(member)) {
			throw new UserIsMemberException(String.format(
					"User %s is the owner of group %s",
					member.getName(), groupID.getName()));
		} else if (g.getAdministrators().contains(member)) {
			throw new UserIsMemberException(String.format(
					"User %s is %san administrator of group %s",
					member.getName(), asAdmin ? "already " : "", groupID.getName()));
		} else if (g.getMembers().contains(member) && !asAdmin) {
			throw new UserIsMemberException(String.format(
					"User %s is already a member of group %s",
					member.getName(), groupID.getName()));
		} else if (!g.getMembers().contains(member) && asAdmin) {
			// so yeah, this is lazy and maybe there's a better mongo data structure that means
			// we can add admins directly. However, it's pretty trivial to add them as is,
			// the current data structure works, so YAGNI
			throw new NoSuchUserException(String.format(
					"User %s must be a member of group %s before admin promotion",
					member.getName(), groupID.getName()));
		} else {
			/* this *could* be caused by a race condition if owner/admins/members change or group
			 * deletions are implemented. However, it should be extremely rare and the
			 * user add can just be retried so it's not worth special handling beyond
			 * throwing an exception.
			 * If this prediction is wrong, just retry X (5?) times.
			 * 
			 * This is also really hard to test.
			 */
			throw new RuntimeException(String.format("Unexpected result: No group " +
					"matched %saddmember query for group id %s and member %s.",
					asAdmin ? "admin " : "", groupID.getName(), member.getName()));
		}
	}
	
	@Override
	public void removeMember(final GroupID groupID, final UserName member, final Instant modDate)
			throws NoSuchGroupException, NoSuchUserException, GroupsStorageException {
		checkNotNull(member, "member");
		demoteMember(groupID, member, modDate, false);
	}
	
	@Override
	public void demoteAdmin(final GroupID groupID, final UserName admin, final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		checkNotNull(admin, "admin");
		demoteMember(groupID, admin, modDate, true);
	}
	
	private void demoteMember(
			final GroupID groupID,
			final UserName member,
			final Instant modDate,
			final boolean asAdmin)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		checkNotNull(groupID, "groupID");
		checkNotNull(modDate, "modDate");
		final String field = asAdmin ? Fields.GROUP_ADMINS :
				Fields.GROUP_MEMBERS + Fields.FIELD_SEP + Fields.GROUP_MEMBER_NAME;
		final Document notEqualToMember = new Document("$ne", member.getName());
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(Fields.GROUP_OWNER, notEqualToMember)
				.append(field, member.getName());
		if (!asAdmin) {
			query.append(Fields.GROUP_ADMINS, notEqualToMember);
		}
	
		final Document mod = new Document(
				"$set", new Document(Fields.GROUP_MODIFICATION, Date.from(modDate)));
		if (asAdmin) {
			mod.append("$pull", new Document(Fields.GROUP_ADMINS, member.getName()));
		} else {
			mod.append("$pull", new Document(Fields.GROUP_MEMBERS,
					new Document(Fields.GROUP_MEMBER_NAME, member.getName())));
		}
		
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, mod);
			if (res.getMatchedCount() != 1) {
				getGroup(groupID); // will throw no such group
				throw new NoSuchUserException(String.format("No %s %s in group %s",
						asAdmin ? "administrator" : "member",
						member.getName(), groupID.getName()));
			}
			// if it matched it got modified, so don't check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	@Override
	public void updateUser(
			final GroupID groupID,
			final UserName member,
			final Map<NumberedCustomField, OptionalString> fields,
			final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		updateUserCheckForNulls(groupID, member, fields, modDate);
		if (fields.isEmpty()) {
			return;
		}
		final List<Document> memberQueryOr = new LinkedList<>();
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				// This 1) finds the position of the member to alter, and
				// 2) ensures that we actually alter the member (see memberQueryOr updates in
				// fn below) before setting the mod date
				// 3) keeps the update atomic
				.append(Fields.GROUP_MEMBERS, new Document("$elemMatch",
						new Document(Fields.GROUP_MEMBER_NAME, member.getName())
								.append("$or", memberQueryOr)));
		final Document set = new Document(Fields.GROUP_MODIFICATION, Date.from(modDate));
		
		// see further explanation re memberQueryOr updates in this fn
		final Document update = buildQueryAndUpdateForCustomFields(
				fields.keySet(),
				f -> fields.get(f),
				Fields.GROUP_MEMBER_CUSTOM_FIELDS + Fields.FIELD_SEP,
				// now use the member position to set the fields
				Fields.GROUP_MEMBERS + ".$." + Fields.GROUP_MEMBER_CUSTOM_FIELDS +
						Fields.FIELD_SEP,
				memberQueryOr,
				set);
		// if it matches, it gets modified
		updateUser(groupID, member, query, update);
	}

	// only checks for a match, not modification, and will generally throw an error if no match
	// assumes match is on gid and username and throws errors for those conditions
	private void updateUser(
			final GroupID groupID,
			final UserName member,
			final Document query,
			final Document update)
			throws GroupsStorageException, NoSuchGroupException, NoSuchUserException {
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, update);
			if (res.getMatchedCount() != 1) {
				final Group g = getGroup(groupID); //throws no such group
				// there's a *tiny* possibility that the user could've been added between
				// the match failing and now, which would mean the function doesn't error
				// when it should.
				// So improbable not worth worrying about.
				if (!g.isMember(member)) {
					throw new NoSuchUserException(String.format(
							"User %s is not a member of group %s",
							member.getName(), groupID.getName()));
				}
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}

	private void updateUserCheckForNulls(
			final GroupID groupID,
			final UserName member,
			final Map<NumberedCustomField, OptionalString> fields,
			final Instant modDate) {
		requireNonNull(groupID, "groupID");
		requireNonNull(member, "member");
		requireNonNull(fields, "fields");
		requireNonNull(modDate, "modDate");
		for (final Entry<NumberedCustomField, OptionalString> e: fields.entrySet()) {
			requireNonNull(e.getKey(), "Null key in fields");
			requireNonNull(e.getValue(), String.format("Null value for key %s in fields",
					e.getKey().getField()));
		}
	}
	
	@Override
	public void updateUser(
			final GroupID groupID,
			final UserName member,
			final Instant lastVisited)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		requireNonNull(groupID, "groupID");
		requireNonNull(member, "member");
		requireNonNull(lastVisited, "lastVisited");
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(Fields.GROUP_MEMBERS, new Document("$elemMatch",
						new Document(Fields.GROUP_MEMBER_NAME, member.getName())));
		
		final Document update = new Document("$set", new Document(
				Fields.GROUP_MEMBERS + ".$." + Fields.GROUP_MEMBER_VISIT_DATE, lastVisited));
		updateUser(groupID, member, query, update);
	}

	@Override
	public void addResource(
			final GroupID groupID,
			final ResourceType type,
			final ResourceDescriptor resource,
			final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, ResourceExistsException {
		checkNotNull(resource, "resource");
		if (!modifyResourceInGroup(groupID, type, resource.getAdministrativeID(),
				resource.getResourceID(), modDate)) {
			throw new ResourceExistsException(String.format("%s %s",
					type.getName(), resource.getResourceID().getName()));
		}
	}
	
	@Override
	public void removeResource(
			final GroupID groupID,
			final ResourceType type,
			final ResourceID resource,
			final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchResourceException {
		checkNotNull(resource, "resource");
		if (!modifyResourceInGroup(groupID, type, null, resource, modDate)) {
			throw new NoSuchResourceException(String.format(
					"Group %s does not include %s %s",
					groupID.getName(), type.getName(), resource.getName()));
		}
	}
	
	// returns true if modified, false otherwise.
	// pass an admin ID to add, null to remove.
	private boolean modifyResourceInGroup(
			final GroupID groupID,
			final ResourceType type,
			final ResourceAdministrativeID resourceAdminID,
			final ResourceID resourceID,
			final Instant modDate)
			throws GroupsStorageException, NoSuchGroupException {
		checkNotNull(groupID, "groupID");
		checkNotNull(type, "type");
		checkNotNull(modDate, "modDate");
		// resource IDs should always have the same admin ID, so we check for equality
		// only on the resource ID and pull any resource with the resource ID, regardless of
		// admin ID
		final String resourceField = Fields.GROUP_RESOURCES + Fields.FIELD_SEP + type.getName();
		final String resIDStr = resourceID.getName();
		final String resourceIDField = resourceField + Fields.FIELD_SEP + Fields.GROUP_RESOURCE_ID;
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(resourceIDField,
						resourceAdminID == null ? resIDStr : new Document("$ne", resIDStr));
		final Document update = new Document("$set",
				new Document(Fields.GROUP_MODIFICATION, Date.from(modDate)));
		if (resourceAdminID != null) {
			update.append("$addToSet", new Document(
					resourceField, new Document(
							Fields.GROUP_RESOURCE_ADMINISTRATIVE_ID, resourceAdminID.getName())
							.append(Fields.GROUP_RESOURCE_ID, resIDStr)
							.append(Fields.GROUP_RESOURCE_ADDED, modDate)));
		} else {
			update.append("$pull", new Document(resourceField,
					new Document(Fields.GROUP_RESOURCE_ID, resIDStr)));
		}
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, update);
			if (res.getMatchedCount() != 1) {
				getGroup(groupID); // throws no such group exception
				return false; // no match, throw appropriate exception in calling method
			} else {
				return true;
			}
			// if it matched, the doc was updated, so no need to check for modification
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}

	@Override
	public void storeRequest(final GroupRequest request)
			throws RequestExistsException, GroupsStorageException {
		checkNotNull(request, "request");
		final String charString = getCharacteristicString(request);
		final Document req = new Document(
				Fields.REQUEST_ID, request.getID().getID())
				.append(Fields.REQUEST_GROUP_ID, request.getGroupID().getName())
				.append(Fields.REQUEST_REQUESTER, request.getRequester().getName())
				.append(Fields.REQUEST_STATUS, request.getStatusType().name())
				.append(Fields.REQUEST_TYPE, request.getType().name())
				.append(Fields.REQUEST_RESOURCE_TYPE, request.getResourceType().getName())
				.append(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID,
						request.getResource().getAdministrativeID().getName())
				.append(Fields.REQUEST_RESOURCE_ID,
						request.getResource().getResourceID().getName())
				.append(Fields.REQUEST_CLOSED_BY, request.getClosedBy()
						.map(cb -> cb.getName()).orElse(null))
				.append(Fields.REQUEST_REASON_CLOSED, request.getClosedReason().orElse(null))
				.append(Fields.REQUEST_CREATION, Date.from(request.getCreationDate()))
				.append(Fields.REQUEST_MODIFICATION, Date.from(request.getModificationDate()))
				.append(Fields.REQUEST_EXPIRATION, Date.from(request.getExpirationDate()));
		if (charString != null) {
				req.append(Fields.REQUEST_CHARACTERISTIC_STRING, charString);
		}
		try {
			db.getCollection(COL_REQUESTS).insertOne(req);
		} catch (MongoWriteException mwe) {
			// not happy about this, but getDetails() returns an empty map
			final DuplicateKeyExceptionChecker dk = new DuplicateKeyExceptionChecker(mwe);
			if (dk.isDuplicate() && COL_REQUESTS.equals(dk.getCollection().get())) {
				if ((Fields.REQUEST_ID + "_1").equals(dk.getIndex().get())) {
					throw new IllegalArgumentException(String.format("ID %s already exists " +
							"in the database. The programmer is responsible for maintaining " +
							"unique IDs.", request.getID().getID()));
				} else if ((Fields.REQUEST_CHARACTERISTIC_STRING + "_1")
						.equals(dk.getIndex().get())) {
					// there's a tiny possibility of race condition here but not worth
					// worrying about
					final String requestID = getRequestIDFromCharacteristicString(charString);
					throw new RequestExistsException("Request exists with ID: " +
						requestID);
				} // otherwise throw next exception
			}
			throw new GroupsStorageException("Database write failed", mwe);
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	// this should only be called when it's known the characteristic string is in the DB.
	private String getRequestIDFromCharacteristicString(final String charString)
			throws GroupsStorageException {
		checkNotNull(charString, "charString");
		final Document otherRequest = findOne(
				COL_REQUESTS, new Document(Fields.REQUEST_CHARACTERISTIC_STRING, charString));
		if (otherRequest == null) {
			// this is difficult to test without doing nutty reflection stuff assuming
			// this function is being called correctly
			throw new GroupsStorageException("Couldn't find request with characteristic string " +
					charString);
		}
		return otherRequest.getString(Fields.REQUEST_ID);
	}


	/** Get a string that is arbitrary in structure but represents the characteristics of a
	 * request that differentiate it from other requests. This string can be used to find
	 * requests that are effectively identical but not {@link GroupRequest#equals(Object)}.
	 * The fields used in the characteristic string are the group id, the requester,
	 * the type, and the target (if present). The charstring should ONLY BE SET ON OPEN REQUESTS.
	 * Requests in the closed state should have the charstring set to null. This prevents users
	 * from opening more than one request for the same thing, but allows new requests to be
	 * submitted if the open request is closed.
	 * @param request the request to characterize.
	 * @return the characteristic string.
	 */
	private String getCharacteristicString(final GroupRequest request) {
		if (!request.getStatusType().equals(GroupRequestStatusType.OPEN)) {
			return null;
		}
		final StringBuilder builder = new StringBuilder();
		builder.append(request.getGroupID().getName());
		builder.append(request.getRequester().getName());
		builder.append(request.getType().name());
		builder.append(request.getResourceType().getName());
		// since knowing the resource ID means we know the admin ID, no need to include it
		builder.append(request.getResource().getResourceID().getName());
		final MessageDigest digester;
		try {
			digester = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("This should be impossible", e);
		}
		final byte[] digest = digester.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
		final StringBuilder sb = new StringBuilder();
		for (final byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}


	@Override
	public GroupRequest getRequest(final RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException {
		checkNotNull(requestID, "requestID");
		final Document req = findOne(
				COL_REQUESTS, new Document(Fields.REQUEST_ID, requestID.getID()));
		if (req == null) {
			throw new NoSuchRequestException(requestID.getID());
		} else {
			return toRequest(req);
		}
	}
	
	@Override
	public List<GroupRequest> getRequestsByRequester(
			final UserName requester,
			final GetRequestsParams params)
			throws GroupsStorageException {
		checkNotNull(requester, "requester");
		return findRequests(new Document(Fields.REQUEST_REQUESTER, requester.getName()), params);
	}
	
	@Override
	public List<GroupRequest> getRequestsByTarget(
			final UserName target,
			final Map<ResourceType, Set<ResourceAdministrativeID>> resources,
			final GetRequestsParams params)
			throws GroupsStorageException {
		requireNonNull(target, "target");
		requireNonNull(resources, "resources");
		if (requireNonNull(params, "params").getResourceType().isPresent()) {
			// making a whole new class just to avoid this seems silly
			throw new IllegalArgumentException(
					"This method may not be parameterized with a specific resource ID");
		}
		final List<Document> or = new LinkedList<>();
		final Document query = new Document(Fields.REQUEST_TYPE, RequestType.INVITE.name())
				.append("$or", or);
		or.add(new Document(Fields.REQUEST_RESOURCE_TYPE, GroupRequest.USER_TYPE.getName())
				.append(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID, target.getName()));
		for (final ResourceType t: resources.keySet()) {
			checkNotNull(t, "null key in resources");
			checkNoNullsInCollection(resources.get(t), "resources key " + t.getName() + " value");
			if (resources.get(t).isEmpty()) {
				throw new IllegalArgumentException("No resource IDs for key " + t.getName());
			}
			or.add(new Document(Fields.REQUEST_RESOURCE_TYPE, t.getName())
					.append(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID, new Document(
							"$in", resources.get(t).stream().map(r -> r.getName())
									.collect(Collectors.toList()))));
		}
		return findRequests(query, params);
	}
	
	@Override
	public List<GroupRequest> getRequestsByTarget(final GetRequestsParams params)
			throws GroupsStorageException {
		if (!requireNonNull(params, "params").getResourceType().isPresent()) {
			// making a whole new class just to avoid this seems silly
			throw new IllegalArgumentException(
					"A resource must be specified in the method parameters");
		}
		final Document query = new Document(Fields.REQUEST_TYPE, RequestType.INVITE.name());
		return findRequests(query, params);
	}

	@Override
	public List<GroupRequest> getRequestsByGroup(
			final GroupID groupID,
			final GetRequestsParams params)
			throws GroupsStorageException {
		checkNotNull(groupID, "groupID");
		return getRequestsByGroups(new HashSet<>(Arrays.asList(groupID)), params);
	}
	
	@Override
	public List<GroupRequest> getRequestsByGroups(
			final Set<GroupID> groupIDs,
			final GetRequestsParams params)
			throws GroupsStorageException {
		checkNoNullsInCollection(groupIDs, "groupIDs");
		final Document query = new Document(Fields.REQUEST_GROUP_ID,
				new Document("$in", groupIDs.stream().map(i -> i.getName())
						.collect(Collectors.toList())))
				.append(Fields.REQUEST_TYPE, RequestType.REQUEST.name());
		return findRequests(query, params);
	}
	
	@Override
	public boolean groupHasRequest(final GroupID groupID, final Instant laterThan)
			throws GroupsStorageException {
		requireNonNull(groupID, "groupID");
		final Document query = new Document(Fields.REQUEST_GROUP_ID, groupID.getName())
				.append(Fields.REQUEST_TYPE, RequestType.REQUEST.name())
				.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name());
		if (laterThan != null) {
			// we used mod date vs create to avoid making another index, and since
			// the mod date isn't changed while the request is open
			query.append(Fields.REQUEST_MODIFICATION, new Document("$gt", laterThan));
		}
		final Document projection = new Document(Fields.MONGO_ID, 1);
		// mongo 2.6 doesn't support count with a limit
		return findOne(COL_REQUESTS, query, projection) != null;
	}

	private List<GroupRequest> findRequests(final Document query, final GetRequestsParams params)
			throws GroupsStorageException {
		checkNotNull(params, "params");
		if (!params.isIncludeClosed()) {
			query.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name());
		}
		if (params.getExcludeUpTo().isPresent()) {
			final String inequality = params.isSortAscending() ? "$gt" : "$lt";
			query.append(Fields.REQUEST_MODIFICATION,
					new Document(inequality, Date.from(params.getExcludeUpTo().get())));
		}
		if (params.getResourceType().isPresent()) {
			query.append(Fields.REQUEST_RESOURCE_TYPE, params.getResourceType().get().getName())
				.append(Fields.REQUEST_RESOURCE_ID, params.getResourceID().get().getName());
		}
		// allow other sorts? can't think of any particularly useful ones
		final Document sort = new Document(Fields.REQUEST_MODIFICATION,
				params.isSortAscending() ? 1 : -1);
		// could make limit a param, YAGNI for now
		return getList(COL_REQUESTS, query, new Document(), sort, 100, d -> toRequest(d));
	}
	
	private GroupRequest toRequest(final Document req) throws GroupsStorageException {
		try {
			final String closedBy = req.getString(Fields.REQUEST_CLOSED_BY);
			return GroupRequest.getBuilder(
					new RequestID(req.getString(Fields.REQUEST_ID)),
					new GroupID(req.getString(Fields.REQUEST_GROUP_ID)),
					new UserName(req.getString(Fields.REQUEST_REQUESTER)),
					CreateModAndExpireTimes.getBuilder(
							req.getDate(Fields.REQUEST_CREATION).toInstant(),
							req.getDate(Fields.REQUEST_EXPIRATION).toInstant())
							.withModificationTime(req.getDate(Fields.REQUEST_MODIFICATION)
									.toInstant())
							.build())
					.withType(RequestType.valueOf(req.getString(Fields.REQUEST_TYPE)))
					.withResource(
							new ResourceType(
									req.getString(Fields.REQUEST_RESOURCE_TYPE)),
							new ResourceDescriptor(
									new ResourceAdministrativeID(
											req.getString(
													Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID)),
									new ResourceID(req.getString(Fields.REQUEST_RESOURCE_ID))))
					.withStatus(GroupRequestStatus.from(
							GroupRequestStatusType.valueOf(req.getString(Fields.REQUEST_STATUS)),
							closedBy == null ? null : new UserName(closedBy),
							req.getString(Fields.REQUEST_REASON_CLOSED)))
					.build();
		} catch (IllegalParameterException | MissingParameterException |
				IllegalArgumentException e) {
			throw new GroupsStorageException(
					"Unexpected value in database: " + e.getMessage(), e);
		}
	}
	
	@Override
	public void closeRequest(
			final RequestID requestID,
			final GroupRequestStatus newStatus,
			final Instant modificationTime)
			throws NoSuchRequestException, GroupsStorageException {
		checkNotNull(requestID, "requestID");
		final Document query = new Document(Fields.REQUEST_ID, requestID.getID());
		closeRequests(query, newStatus, modificationTime, requestID);
	}

	// pass non-null request ID if modifying a single request. That'll cause an exception
	// if the query doesn't match.
	// the query is appended with a doc enforcing that the status is OPEN.
	private void closeRequests(
			final Document query, 
			final GroupRequestStatus newStatus,
			final Instant modificationTime,
			final RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException {
		query.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name());
		checkNotNull(newStatus, "newStatus");
		checkNotNull(modificationTime, "modificationTime");
		if (newStatus.getStatusType().equals(GroupRequestStatusType.OPEN)) {
			throw new IllegalArgumentException(
					"newStatus cannot be " + GroupRequestStatusType.OPEN);
		}
		final Document set = new Document(
				Fields.REQUEST_STATUS, newStatus.getStatusType().name())
				.append(Fields.REQUEST_MODIFICATION, Date.from(modificationTime));
		if (newStatus.getClosedBy().isPresent()) {
			set.append(Fields.REQUEST_CLOSED_BY, newStatus.getClosedBy().get().getName());
		}
		if (newStatus.getClosedReason().isPresent()) {
			set.append(Fields.REQUEST_REASON_CLOSED, newStatus.getClosedReason().get());
		}
		final Document unset = new Document(Fields.REQUEST_CHARACTERISTIC_STRING, "");
		try {
			final UpdateResult res = db.getCollection(COL_REQUESTS).updateMany(
					query, new Document("$set", set).append("$unset", unset));
			if (requestID != null && res.getMatchedCount() != 1) {
				throw new NoSuchRequestException("No open request with ID " +
						requestID.getID());
			}
			// has to be modified, so no need to check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}

	/** Set any requests in the {@link GroupRequestStatusType#OPEN} state where the 
	 * {@link GroupRequest#getExpirationDate()} is earlier than the expire time to
	 * {@link GroupRequestStatusType#EXPIRED}. The expire time is also used as the modification
	 * time for the altered requests.
	 * @param expireTime the cutoff time for requests - any requests with an expire time prior
	 * to this value will be expired.
	 * @throws GroupsStorageException if an error occurred contacting the server.
	 */
	public void expireRequests(final Instant expireTime) throws GroupsStorageException {
		/* there's no way to run a multi document update and see which documents were actually
		 * updated AFAICT which sucks.
		 * So to return the request ids that were updated we'd have to
		 * 1) Find the docs where status = OPEN and expire < expireTime
		 * 2) For each doc do an update where status = OPEN expire it
		 * 3) If the update modified the doc, add it to the returned list. Otherwise,
		 *   it was accepted/canceled/denied/expired by another thread.
		 *
		 * So YAGNI for now.
		 */
		checkNotNull(expireTime, "expireTime");
		final Document query = new Document(Fields.REQUEST_EXPIRATION,
				new Document("$lte", Date.from(expireTime)));
		try {
			closeRequests(query, GroupRequestStatus.expired(), expireTime, null);
		} catch (NoSuchRequestException e) {
			throw new RuntimeException("This should be impossible", e);
		}
	}
	
	/* Use this for finding documents where indexes should force only a single
	 * document. Assumes the indexes are doing their job.
	 */
	private Document findOne(
			final String collection,
			final Document query)
			throws GroupsStorageException {
		return findOne(collection, query, null);
	}
	
	/* Use this for finding documents where indexes should force only a single
	 * document. Assumes the indexes are doing their job.
	 */
	private Document findOne(
			final String collection,
			final Document query,
			final Document projection)
			throws GroupsStorageException {
		try {
			return db.getCollection(collection).find(query).projection(projection).first();
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
}
