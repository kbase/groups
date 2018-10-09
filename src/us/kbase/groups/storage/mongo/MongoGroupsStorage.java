package us.kbase.groups.storage.mongo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;

import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.StorageInitException;

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
	
	private static final Map<String, Map<List<String>, IndexOptions>> INDEXES;
	private static final IndexOptions IDX_UNIQ = new IndexOptions().unique(true);
//	private static final IndexOptions IDX_SPARSE = new IndexOptions().sparse(true);
//	private static final IndexOptions IDX_UNIQ_SPARSE =
//			new IndexOptions().unique(true).sparse(true);
	static {
		//hardcoded indexes
		INDEXES = new HashMap<String, Map<List<String>, IndexOptions>>();
		
		//config indexes
		final Map<List<String>, IndexOptions> cfg = new HashMap<>();
		//ensure only one config object
		cfg.put(Arrays.asList(Fields.DB_SCHEMA_KEY), IDX_UNIQ);
		INDEXES.put(COL_CONFIG, cfg);
	}
	
	private final MongoDatabase db;
	@SuppressWarnings("unused")
	private final Clock clock;
	
	/** Create MongoDB based storage for the Groups application.
	 * @param db the Mongo database the storage system will use.
	 * @throws StorageInitException if the storage system could not be initialized.
	 */
	public MongoGroupsStorage(final MongoDatabase db) throws StorageInitException {
		this(db, Clock.systemDefaultZone());
	}
	
	// for testing
	private MongoGroupsStorage(final MongoDatabase db, final Clock clock) 
			throws StorageInitException {
		checkNotNull(db, "db");
		this.clock = clock;
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
		
		/*
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
				throws AssemblyHomologyStorageException {
			// split up indexes better at some point - e.g. in a Document
			isDuplicate = isDuplicate(mwe);
			if (isDuplicate) {
				final Matcher indexMatcher = indexPattern.matcher(mwe.getMessage());
				if (indexMatcher.find()) {
					collection = Optional.of(indexMatcher.group(2));
					index = Optional.of(indexMatcher.group(4));
				} else {
					throw new AssemblyHomologyStorageException(
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
			
		} */
		
		public static boolean isDuplicate(final MongoWriteException mwe) {
			return mwe.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY);
		}
		
		/*
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
		*/
	}

}
