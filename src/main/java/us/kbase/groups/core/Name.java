package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;
import static us.kbase.groups.util.Util.containsControlCharacters;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;


/** The base class for objects that are names.
 * @author gaprice@lbl.gov
 *
 */
public class Name implements Comparable<Name> {
	
	//TODO CODE would be useful to pass in a regex and have this do the matching + check 1st char
	// vs duplicating everywhere
	private final String name;
	
	/** Create a new name.
	 * 
	 * Prior to any operations .trim() is called on the name.
	 * 
	 * @param name the name to create.
	 * @param type the type of the name. This string is included in exceptions but is otherwise
	 * unused.
	 * @param maxCodePoints the maximum number of code points in the name. Values less than 1
	 * are ignored.
	 * @throws MissingParameterException if the name is null or the empty string.
	 * @throws IllegalParameterException if the name is too long or if the name contains
	 * control characters.
	 */
	public Name(final String name, final String type, final int maxCodePoints)
			throws MissingParameterException, IllegalParameterException {
		this.name = checkValidName(name, type, maxCodePoints);
	}
	
	/** Check that a name is valid.
	 * 
	 * Prior to any operations .trim() is called on the name.
	 * 
	 * @param name the name.
	 * @param type the type of the name. This string is included in exceptions but is otherwise
	 * unused.
	 * @param maxCodePoints the maximum number of code points in the name. Values less than 1
	 * are ignored.
	 * @return the trimmed name.
	 * @throws MissingParameterException if the name is null or the empty string.
	 * @throws IllegalParameterException if the name is too long or if the name contains
	 * control characters.
	 */
	public static String checkValidName(
			String name,
			final String type,
			final int maxCodePoints)
			throws MissingParameterException, IllegalParameterException {
		exceptOnEmpty(type, "type");
		checkString(name, type, maxCodePoints);
		name = name.trim();
		if (containsControlCharacters(name)) {
			throw new IllegalParameterException(type + " contains control characters");
		}
		return name;
	}

	/** Get the name.
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	@Override
	public int compareTo(final Name name) {
		checkNotNull(name, "name");
		return getName().compareTo(name.getName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Name other = (Name) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + " [name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}
}
