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
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.FieldItem;
import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.FieldItem.StringField;
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
		// find by owner
		groups.put(Arrays.asList(Fields.GROUP_OWNER), null);
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
		// find by resource and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID,
				Fields.REQUEST_RESOURCE_TYPE, Fields.REQUEST_TYPE, Fields.REQUEST_MODIFICATION),
				null);
		// find by resource and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID,
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
	 * @throws StorageInitException if the storage system could not be initialized.
	 */
	public MongoGroupsStorage(final MongoDatabase db) throws StorageInitException {
		this(db, Clock.systemDefaultZone());
	}
	
	// for tests
	private MongoGroupsStorage(final MongoDatabase db, final Clock clock)
			throws StorageInitException {
		checkNotNull(db, "db");
		this.db = db;
		this.clock = clock;
		ensureIndexes(); // MUST come before check config
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

	private void ensureIndexes() throws StorageInitException {
		for (final String col: INDEXES.keySet()) {
			for (final List<String> idx: INDEXES.get(col).keySet()) {
				final Document index = new Document();
				final IndexOptions opts = INDEXES.get(col).get(idx);
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
				.append(Fields.GROUP_OWNER, group.getOwner().getName())
				.append(Fields.GROUP_MEMBERS, toMembersDocList(group))
				.append(Fields.GROUP_ADMINS, toStringList(group.getAdministrators()))
				.append(Fields.GROUP_RESOURCES, resources)
				.append(Fields.GROUP_CREATION, Date.from(group.getCreationDate()))
				.append(Fields.GROUP_MODIFICATION, Date.from(group.getModificationDate()))
				.append(Fields.GROUP_DESCRIPTION, group.getDescription().orElse(null))
				.append(Fields.GROUP_CUSTOM_FIELDS, getCustomFields(group.getCustomFields()));
		for (final ResourceType t: group.getResourceTypes()) {
			resources.put(t.getName(), group.getResources(t).stream()
					.map(rd -> new Document(
							Fields.GROUP_RESOURCE_ADMINISTRATIVE_ID, rd.getAdministrativeID()
									.getName())
							.append(Fields.GROUP_RESOURCE_ID, rd.getResourceID().getName()))
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
		buildUpdate(or, set, opts.getDescription(), Fields.GROUP_DESCRIPTION, n -> n.get());
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
			final Function<NumberedCustomField, FieldItem<String>> valueGetter,
			final String queryFieldPrefix,
			final String updateFieldPrefix,
			final List<Document> queryOr,
			final Document set) {
		final Document unset = new Document();
		for (final NumberedCustomField ncf: fields) {
			final FieldItem<String> item = valueGetter.apply(ncf);
			if (item.hasAction()) {
				final String updateField = updateFieldPrefix + ncf.getField();
				// check that the field is different. At least one field must be different
				// for the update to occur
				queryOr.add(new Document(queryFieldPrefix + ncf.getField(),
						new Document("$ne", item.orNull())));
				if (item.hasItem()) {
					set.append(updateField, item.get());
				} else {
					unset.append(updateField, "");
				}
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
	
	private <T> void buildUpdate(
			final List<Document> or,
			final Document set,
			final FieldItem<T> item,
			final String field,
			final Function<FieldItem<T>, Object> getValue) {
		if (item.hasAction()) {
			final Object value = item.hasItem() ? getValue.apply(item) : null;
			or.add(new Document(field, new Document("$ne", value)));
			set.append(field, value);
		}
	}

	@Override
	public Group getGroup(final GroupID groupID)
			throws GroupsStorageException, NoSuchGroupException {
		checkNotNull(groupID, "groupID");
		final Document grp = findOne(
				COL_GROUPS, new Document(Fields.GROUP_ID, groupID.getName()));
		if (grp == null) {
			throw new NoSuchGroupException(groupID.getName());
		} else {
			return toGroup(grp);
		}
	}
	
	@Override
	public boolean getGroupExists(final GroupID groupID) throws GroupsStorageException {
		checkNotNull(groupID, "groupID");
		try {
			return db.getCollection(COL_GROUPS)
					.countDocuments(new Document(Fields.GROUP_ID, groupID.getName())) == 1;
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	@Override
	public List<Group> getGroups(final GetGroupsParams params) throws GroupsStorageException {
		checkNotNull(params, "params");
		final List<Group> ret = new LinkedList<>();
		final Document query = new Document();
		if (params.getExcludeUpTo().isPresent()) {
			final String inequality = params.isSortAscending() ? "$gt" : "$lt";
			query.append(Fields.GROUP_ID, new Document(inequality, params.getExcludeUpTo().get()));
		}
		try {
			final FindIterable<Document> gdocs = db.getCollection(COL_GROUPS).find(query)
					// may want to allow alternate sorts later, will need indexes
					.sort(new Document(Fields.GROUP_ID, params.isSortAscending() ? 1 : -1))
					// could make limit a param (with a max), YAGNI for now
					.limit(100); 
			for (final Document gdoc: gdocs) {
				ret.add(toGroup(gdoc));
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
		return ret;
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
					.withDescription(grp.getString(Fields.GROUP_DESCRIPTION));
			getUserSet(grp, Fields.GROUP_ADMINS).stream().forEach(a ->
					b.withAdministrator(members.remove(a)));
			members.values().stream().forEach(m -> b.withMember(m));
			@SuppressWarnings("unchecked")
			final Map<String, List<Document>> resources =
					(Map<String, List<Document>>) grp.get(Fields.GROUP_RESOURCES);
			for (final String restype: resources.keySet()) {
				final ResourceType t = new ResourceType(restype);
				for (final Document rd: resources.get(restype)) {
					b.withResource(t, new ResourceDescriptor(
							new ResourceAdministrativeID(
									rd.getString(Fields.GROUP_RESOURCE_ADMINISTRATIVE_ID)),
							new ResourceID(rd.getString(Fields.GROUP_RESOURCE_ID))));
				}
			}
			addCustomFields((f, v) -> b.withCustomField(f, v), Fields.GROUP_CUSTOM_FIELDS, grp);
			return b.build();
		} catch (MissingParameterException | IllegalParameterException |
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
					u, m.getDate(Fields.GROUP_MEMBER_JOIN_DATE).toInstant());
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
			// TODO CODE there's no need for StringField here. Absent = no change, Optional.empty() == remove
			// except that Optional allows whitespace only strings, bleah. Maybe a StringOptional class?
			final Map<NumberedCustomField, StringField> fields,
			final Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		updateUserCheckForNulls(groupID, member, fields, modDate);
		if (hasNoUpdates(fields)) {
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
				// otherwise we don't care - the update made no changes.
			}
			// if it matches, it gets modified, so we don't check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}

	// totally stupid. Get rid of StringField here. OptionalString class which treats
	// whitespace only strings like nulls would be useful.
	private boolean hasNoUpdates(final Map<NumberedCustomField, StringField> fields) {
		return fields.isEmpty() || new HashSet<>(fields.values())
				.equals(new HashSet<>(Arrays.asList(FieldItem.noAction())));
	}

	private void updateUserCheckForNulls(
			final GroupID groupID,
			final UserName member,
			final Map<NumberedCustomField, StringField> fields,
			final Instant modDate) {
		requireNonNull(groupID, "groupID");
		requireNonNull(member, "member");
		requireNonNull(fields, "fields");
		requireNonNull(modDate, "modDate");
		for (final Entry<NumberedCustomField, StringField> e: fields.entrySet()) {
			requireNonNull(e.getKey(), "Null key in fields");
			requireNonNull(e.getValue(), String.format("Null value for key %s in fields",
					e.getKey().getField()));
		}
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
							.append(Fields.GROUP_RESOURCE_ID, resIDStr)));
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
			final GetRequestsParams params) throws GroupsStorageException {
		checkNotNull(requester, "requester");
		return findRequests(new Document(Fields.REQUEST_REQUESTER, requester.getName()), params);
	}
	
	@Override
	public List<GroupRequest> getRequestsByTarget(
			final UserName target,
			final Map<ResourceType, Set<ResourceAdministrativeID>> resources,
			final GetRequestsParams params)
			throws GroupsStorageException {
		checkNotNull(target, "target");
		checkNotNull(resources, "resources");
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
	public List<GroupRequest> getRequestsByGroup(
			final GroupID groupID,
			final GetRequestsParams params)
			throws GroupsStorageException {
		checkNotNull(groupID, "groupID");
		final Document query = new Document(Fields.REQUEST_GROUP_ID, groupID.getName())
				.append(Fields.REQUEST_TYPE, RequestType.REQUEST.name());
		return findRequests(query, params);
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
		final List<GroupRequest> ret = new LinkedList<>();
		try {
			final FindIterable<Document> gdocs = db.getCollection(COL_REQUESTS).find(query)
					.limit(100) // could make a param, YAGNI for now
					// allow other sorts? can't think of any particularly useful ones
					.sort(new Document(Fields.REQUEST_MODIFICATION,
							params.isSortAscending() ? 1 : -1));
			for (final Document rdoc: gdocs) {
				ret.add(toRequest(rdoc));
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
		return ret;
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
					.withResourceType(new ResourceType(
							req.getString(Fields.REQUEST_RESOURCE_TYPE)))
					.withResource(new ResourceDescriptor(
							new ResourceAdministrativeID(
									req.getString(Fields.REQUEST_RESOURCE_ADMINISTRATIVE_ID)),
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
