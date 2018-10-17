package us.kbase.groups.core;

public enum GroupType {

	// NOTE: these values are stored in the database, so do not change them
	
	// TODO NOW consider making presentation names to display to users. Store the actual enum vals in the db so presentation names can change
	
	organization,
	project,
	team;
	
}
