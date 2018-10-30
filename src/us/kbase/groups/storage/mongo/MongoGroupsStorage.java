package us.kbase.groups.storage.mongo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;

import com.google.common.base.Optional;
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
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.CreateAndModTimes;
import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.exceptions.WorkspaceExistsException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.workspace.WorkspaceID;
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
		// find by wsid
		groups.put(Arrays.asList(Fields.GROUP_WORKSPACES), null);
		INDEXES.put(COL_GROUPS, groups);
		
		// requests indexes
		// TODO CODE mongo 3.2 has partial indexes that might help here
		final Map<List<String>, IndexOptions> requests = new HashMap<>();
		// may need compound indexes to speed things up.
		requests.put(Arrays.asList(Fields.REQUEST_ID), IDX_UNIQ);
		// find by group & sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_GROUP_ID, Fields.REQUEST_MODIFICATION), null);
		// find by group & status and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_GROUP_ID, Fields.REQUEST_STATUS,
				Fields.REQUEST_MODIFICATION), null);
		// find by requester and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_REQUESTER, Fields.REQUEST_MODIFICATION), null);
		// find by requester and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_REQUESTER, Fields.REQUEST_STATUS,
				Fields.REQUEST_MODIFICATION), null);
		// find by target and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_TARGET, Fields.REQUEST_MODIFICATION), null);
		// find by target and state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_TARGET, Fields.REQUEST_STATUS,
				Fields.REQUEST_MODIFICATION), null);
		// sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_MODIFICATION), null);
		// find by state and sort/filter by modification time.
		requests.put(Arrays.asList(Fields.REQUEST_STATUS, Fields.REQUEST_MODIFICATION), null);
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
	
	private final MongoDatabase db;
	
	/** Create MongoDB based storage for the Groups application.
	 * @param db the MongoDB database the storage system will use.
	 * @throws StorageInitException if the storage system could not be initialized.
	 */
	public MongoGroupsStorage(final MongoDatabase db) 
			throws StorageInitException {
		checkNotNull(db, "db");
		this.db = db;
		ensureIndexes(); // MUST come before check config;
		checkConfig();
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
			if (col.count() != 1) {
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
					key = Optional.absent();
				}
			} else {
				collection = Optional.absent();
				index = Optional.absent();
				key = Optional.absent();
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
		final Document u = new Document(
				Fields.GROUP_ID, group.getGroupID().getName())
				.append(Fields.GROUP_NAME, group.getGroupName().getName())
				.append(Fields.GROUP_OWNER, group.getOwner().getName())
				.append(Fields.GROUP_MEMBERS, toStringList(group.getMembers()))
				.append(Fields.GROUP_ADMINS, toStringList(group.getAdministrators()))
				.append(Fields.GROUP_TYPE, group.getType().name())
				.append(Fields.GROUP_WORKSPACES, group.getWorkspaceIDs().getIDs())
				.append(Fields.GROUP_CREATION, Date.from(group.getCreationDate()))
				.append(Fields.GROUP_MODIFICATION, Date.from(group.getModificationDate()))
				.append(Fields.GROUP_DESCRIPTION, group.getDescription().orNull());
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

	private GroupsStorageException wrapMongoException(MongoException e) {
		return new GroupsStorageException("Connection to database failed: " +
				e.getMessage(), e);
	}
	
	private List<String> toStringList(final Set<UserName> users) {
		return users.stream().map(m -> m.getName()).collect(Collectors.toList());
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
	public List<Group> getGroups() throws GroupsStorageException {
		final List<Group> ret = new LinkedList<>();
		try {
			final FindIterable<Document> gdocs = db.getCollection(COL_GROUPS)
					// may want to allow alternate sorts later, will need indexes
					.find().sort(new Document(Fields.GROUP_ID, 1));
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
			final Group.Builder b = Group.getBuilder(
					new GroupID(grp.getString(Fields.GROUP_ID)),
					new GroupName(grp.getString(Fields.GROUP_NAME)),
					new UserName(grp.getString(Fields.GROUP_OWNER)),
					new CreateAndModTimes(
							grp.getDate(Fields.GROUP_CREATION).toInstant(),
							grp.getDate(Fields.GROUP_MODIFICATION).toInstant()))
					.withType(GroupType.valueOf(grp.getString(Fields.GROUP_TYPE)))
					.withDescription(grp.getString(Fields.GROUP_DESCRIPTION));
			addMembers(b, grp);
			addAdmins(b, grp);
			addWorkspaces(b, grp);
			return b.build();
		} catch (MissingParameterException | IllegalParameterException |
				IllegalArgumentException e) {
			throw new GroupsStorageException(
					"Unexpected value in database: " + e.getMessage(), e);
		}
	}
	
	// could probably combine the next 3 methods with lambdas, but eh
	private void addMembers(final Group.Builder builder, final Document groupDoc)
			throws MissingParameterException, IllegalParameterException {
		// can't be null
		@SuppressWarnings("unchecked")
		final List<String> members = (List<String>) groupDoc.get(Fields.GROUP_MEMBERS);
		for (final String m: members) {
			builder.withMember(new UserName(m));
		}
	}
	
	private void addAdmins(final Group.Builder builder, final Document groupDoc)
			throws MissingParameterException, IllegalParameterException {
		// can't be null
		@SuppressWarnings("unchecked")
		final List<String> admins = (List<String>) groupDoc.get(Fields.GROUP_ADMINS);
		for (final String a: admins) {
			builder.withAdministrator(new UserName(a));
		}
	}
	
	private void addWorkspaces(final Group.Builder builder, final Document groupDoc)
			throws MissingParameterException, IllegalParameterException {
		// can't be null
		@SuppressWarnings("unchecked")
		final List<Integer> wsids = (List<Integer>) groupDoc.get(Fields.GROUP_WORKSPACES);
		for (final int w: wsids) {
			builder.withWorkspace(new WorkspaceID(w));
		}
	}
	
	@Override
	public void addMember(final GroupID groupID, final UserName member)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException {
		addUser(groupID, member, false);
	}
	
	@Override
	public void addAdmin(final GroupID groupID, final UserName admin)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException {
		checkNotNull(admin, "admin");
		addUser(groupID, admin, true);
	}

	private void addUser(final GroupID groupID, final UserName member, final boolean asAdmin)
			throws GroupsStorageException, NoSuchGroupException, UserIsMemberException {
		checkNotNull(groupID, "groupID");
		checkNotNull(member, "member");
		
		final Document notEqualToMember = new Document("$ne", member.getName());
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(Fields.GROUP_OWNER, notEqualToMember)
				.append(Fields.GROUP_ADMINS, notEqualToMember);
			
		final Document modification = new Document("$addToSet",
				new Document(asAdmin ? Fields.GROUP_ADMINS : Fields.GROUP_MEMBERS,
						member.getName()));
		if (asAdmin) {
			modification.append("$pull", new Document(Fields.GROUP_MEMBERS, member.getName()));
		}
		
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, modification);
			if (res.getMatchedCount() != 1) {
				handleNoMatchOnUserAdd(groupID, member, asAdmin);
			}
			if (res.getModifiedCount() != 1) {
				// this can't happen if asAdmin is true
				throw new UserIsMemberException(String.format(
						"User %s is already a member of group %s",
						member.getName(), groupID.getName()));
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	private void handleNoMatchOnUserAdd(
			final GroupID groupID,
			final UserName member,
			final boolean asAdmin)
			throws GroupsStorageException, NoSuchGroupException, UserIsMemberException {
		final Group g = getGroup(groupID); // will throw no such group
		if (g.getOwner().equals(member)) {
			throw new UserIsMemberException(String.format(
					"User %s is the owner of group %s",
					member.getName(), groupID.getName()));
		} else if (g.getAdministrators().contains(member)) {
			throw new UserIsMemberException(String.format(
					"User %s is %san administrator of group %s",
					member.getName(), asAdmin ? "already " : "", groupID.getName()));
		} else {
			/* this *could* be caused by a race condition if owner/admins change or group
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
	public void removeMember(final GroupID groupID, final UserName member)
			throws NoSuchGroupException, NoSuchUserException, GroupsStorageException {
		checkNotNull(groupID, "groupID");
		checkNotNull(member, "member");
		
		final Document query = new Document(Fields.GROUP_ID, groupID.getName());
		final Document pull = new Document("$pull",
				new Document(Fields.GROUP_MEMBERS, member.getName()));
		
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, pull);
			if (res.getMatchedCount() != 1) {
				throw new NoSuchGroupException(groupID.getName());
			}
			if (res.getModifiedCount() != 1) {
				throw new NoSuchUserException(String.format("No member %s in group %s",
						member.getName(), groupID.getName()));
			}
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	@Override
	public void demoteAdmin(final GroupID groupID, final UserName admin)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException {
		checkNotNull(groupID, "groupID");
		checkNotNull(admin, "admin");
		
		final Document query = new Document(Fields.GROUP_ID, groupID.getName())
				.append(Fields.GROUP_ADMINS, admin.getName());
		
		final Document mod = new Document("$pull",
					new Document(Fields.GROUP_ADMINS, admin.getName()))
				.append("$addToSet", new Document(Fields.GROUP_MEMBERS, admin.getName()));
		
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, mod);
			if (res.getMatchedCount() != 1) {
				getGroup(groupID); // will throw no such group
				throw new NoSuchUserException(String.format("No administrator %s in group %s",
						admin.getName(), groupID.getName()));
			}
			// if there's a match, the document must be modified, so we don't check
		} catch (MongoException e) {
			throw wrapMongoException(e);
		}
	}
	
	@Override
	public void addWorkspace(final GroupID groupID, final WorkspaceID wsid)
			throws NoSuchGroupException, GroupsStorageException, WorkspaceExistsException {
		if (!alterWorkspaceInGroup(groupID, wsid, true)) {
			throw new WorkspaceExistsException(wsid.getID() + "");
		}
	}
	
	@Override
	public void removeWorkspace(final GroupID groupID, final WorkspaceID wsid)
			throws NoSuchGroupException, GroupsStorageException, NoSuchWorkspaceException {
		if (!alterWorkspaceInGroup(groupID, wsid, false)) {
			throw new NoSuchWorkspaceException(String.format(
					"Group %s does not include workspace %s", groupID.getName(), wsid.getID()));
		}
	}
		
	// true if modified, false otherwise.
	private boolean alterWorkspaceInGroup(
			final GroupID groupID,
			final WorkspaceID wsid,
			final boolean add)
			throws NoSuchGroupException, GroupsStorageException {
		checkNotNull(groupID, "groupID");
		checkNotNull(wsid, "wsid");
		final Document query = new Document(Fields.GROUP_ID, groupID.getName());
		final Document update = new Document(add ? "$addToSet" : "$pull",
				new Document(Fields.GROUP_WORKSPACES, wsid.getID()));
		try {
			final UpdateResult res = db.getCollection(COL_GROUPS).updateOne(query, update);
			if (res.getMatchedCount() != 1) {
				throw new NoSuchGroupException(groupID.getName());
			}
			return res.getModifiedCount() == 1;
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
				.append(Fields.REQUEST_TARGET, request.getTarget().isPresent() ?
						request.getTarget().get().getName() : null)
				.append(Fields.REQUEST_CLOSED_BY, request.getClosedBy().isPresent() ?
						request.getClosedBy().get().getName() : null)
				.append(Fields.REQUEST_REASON_CLOSED, request.getClosedReason().orNull())
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


	//TODO WORKSPACE will also need to include ws id
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
		builder.append(request.getTarget().isPresent() ? request.getTarget().get().getName() : "");
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
	
	// TODO NOW need to provide limit and specify date range to split up large request lists - sort by created, will need new indexes
	// TODO NOW allow including closed requests
	@Override
	public List<GroupRequest> getRequestsByRequester(
			final UserName requester) throws GroupsStorageException {
		return getRequestsByUser(requester, Fields.REQUEST_REQUESTER, "requester");
	}
	
	// TODO NOW need to provide limit and specify date range to split up large request lists - sort by created, will need new indexes
	// TODO NOW allow including closed requests
	@Override
	public List<GroupRequest> getRequestsByTarget(
			final UserName target)
			throws GroupsStorageException {
		return getRequestsByUser(target, Fields.REQUEST_TARGET, "target");
	}

	private List<GroupRequest> getRequestsByUser(
			final UserName requester,
			final String field,
			final String fieldType)
			throws GroupsStorageException {
		checkNotNull(requester, fieldType);
		return findRequests(new Document(field, requester.getName())
				.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name()));
	}

	// TODO NOW need to provide limit and specify date range to split up large request lists - sort by created, will need new indexes
	// TODO NOW allow including closed requests
	@Override
	public List<GroupRequest> getRequestsByGroup(
			final GroupID groupID)
			throws GroupsStorageException {
		checkNotNull(groupID, "groupID");
		final Document query = new Document(Fields.REQUEST_GROUP_ID, groupID.getName())
				.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name())
				.append(Fields.REQUEST_TYPE, new Document("$in", Arrays.asList(
						//TODO WORKSPACE add type where admins must approve workspace addition
						GroupRequestType.REQUEST_GROUP_MEMBERSHIP.name())));
		return findRequests(query);
	}

	private List<GroupRequest> findRequests(final Document query)
			throws GroupsStorageException {
		final List<GroupRequest> ret = new LinkedList<>();
		try {
			final FindIterable<Document> gdocs = db.getCollection(COL_REQUESTS).find(query)
					// allow other sorts? can't think of any particularly useful ones
					// maybe allow reverse sort. Default should be oldest first for
					// open requests & newest first for closed (e.g. what happened most recently)
					.sort(new Document(Fields.REQUEST_MODIFICATION, 1));
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
			final String target = req.getString(Fields.REQUEST_TARGET);
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
					.withType(
							GroupRequestType.valueOf(req.getString(Fields.REQUEST_TYPE)),
							target == null ? null : new UserName(target))
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
		final Document query = new Document(Fields.REQUEST_ID, requestID.getID())
				.append(Fields.REQUEST_STATUS, GroupRequestStatusType.OPEN.name());
		try {
			final UpdateResult res = db.getCollection(COL_REQUESTS).updateOne(
					query, new Document("$set", set).append("$unset", unset));
			if (res.getMatchedCount() != 1) {
				throw new NoSuchRequestException("No open request with ID " +
						requestID.getID());
			}
			// has to be modified, so no need to check
		} catch (MongoException e) {
			throw wrapMongoException(e);
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
