package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class GroupName extends Name {
	
	//TODO TEST
	//TODO JAVADOC
	
	public GroupName(final String name)
			throws MissingParameterException, IllegalParameterException {
		super(name, "group name", 256);
	}
}
