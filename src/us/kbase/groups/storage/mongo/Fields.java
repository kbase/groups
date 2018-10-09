package us.kbase.groups.storage.mongo;

/** This class defines the field names used in MongoDB documents for storing assembly homology
 * data.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {
	
	/** The separator between mongo field names. */
	public static final String FIELD_SEP = ".";

	/** The key for the MongoDB ID in documents. */
	public static final String MONGO_ID = "_id";

	
	/* ***********************
	 * database schema fields
	 * ***********************
	 */
	
	/** The key for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_KEY = "schema";
	/** The value for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_VALUE = "schema";
	/** Whether the database schema is in the process of being updated. */
	public static final String DB_SCHEMA_UPDATE = "inupdate";
	/** The version of the database schema. */
	public static final String DB_SCHEMA_VERSION = "schemaver";

}
