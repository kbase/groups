package us.kbase.groups.storage.mongo;

/** This class defines the field names used in MongoDB documents for storing groups data.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {
	
	/** The separator between mongo field names. */
	public static final String FIELD_SEP = ".";

	/** The key for the MongoDB ID in documents. */
	public static final String MONGO_ID = "_id";

	/* ***********************
	 * groups fields
	 * ***********************
	 */
	
	/** The group id. */
	public static final String GROUP_ID = "id";
	/** The group name. */
	public static final String GROUP_NAME = "name";
	/** The group type. */
	public static final String GROUP_TYPE = "type";
	/** The username of the group owner. */
	public static final String GROUP_OWNER = "own";
	/** The group creation date. */
	public static final String GROUP_CREATION = "create";
	/** The group modifcation date. */
	public static final String GROUP_MODIFICATION = "mod";
	/** The group description. */
	public static final String GROUP_DESCRIPTION = "desc";
	
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
