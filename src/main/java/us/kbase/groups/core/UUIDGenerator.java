package us.kbase.groups.core;

import java.util.UUID;

/** Generates UUIDs.
 * @author gaprice@lbl.gov
 *
 */
public class UUIDGenerator {
	
	//TODO TEST not much to test here, but...
	
	/** Generate a UUID via {@link UUID#randomUUID()}.
	 * @return a new random UUID.
	 */
	public UUID randomUUID() {
		return UUID.randomUUID();
	}

}
