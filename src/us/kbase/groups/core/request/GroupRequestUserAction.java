package us.kbase.groups.core.request;

public enum GroupRequestUserAction {

	// TODO JAVADOC
	// TODO TEST
	
	//TODO NOW make names so easy to change representation
	
	CANCEL, // request creator only
	ACCEPT, // for target of request: user, group admins, workspace admins
	DENY;   // as above
}
