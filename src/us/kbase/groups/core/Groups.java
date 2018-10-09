package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import us.kbase.groups.storage.GroupsStorage;

/** The core class in the Groups software. 
 * @author gaprice@lbl.gov
 *
 */
public class Groups {

	//TODO JAVADOC
	//TODO TEST
	
	@SuppressWarnings("unused")
	private final GroupsStorage storage;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 */
	public Groups(
			final GroupsStorage storage) {
		checkNotNull(storage, "storage");
		this.storage = storage;
	}
	
}
