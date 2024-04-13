package us.kbase.groups.core;

import static us.kbase.groups.util.Util.isNullOrEmpty;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.util.Optional;

/** A {@link String} specific version of {@link Optional}. 
 * 
 * The differences are that strings that are whitespace-only are treated as {@link #empty()} and
 * and whitespace is {@link String#trim()}ed from non-null non-whitespace-only values.
 * @author gaprice@lbl.gov
 *
 */
public final class OptionalString {
	
	// TODO CODE add more methods from Optional as needed.

	private final String value;
	
	private OptionalString(final String value) {
		this.value = value;
	}
	
	/** Create an empty {@link OptionalString}.
	 * @return the empty String, where empty in this case means no value.
	 */
	public static OptionalString empty() {
		return new OptionalString(null);
	}
	
	/** Create a new {@link OptionalString}. 
	 * @param value the value, which cannot be null or whitespace-only.
	 * @return the new string.
	 */
	public static OptionalString of(final String value) {
		exceptOnEmpty(value, "value");
		return new OptionalString(value.trim());
	}
	
	/** Create a new {@link OptionalString} from a value which may be null or whitespace-only.
	 * @param value the value.
	 * @return {@link #empty()} if the value is null or whitespace only, or {@link #of(String)}
	 * otherwise. The value is {@link String#trim()}ed.
	 */
	public static OptionalString ofEmptyable(final String value) {
		return isNullOrEmpty(value) ? empty() : of(value);
	}
	
	/** Get whether a value for the optional string exists.
	 * @return true if a value exists.
	 */
	public boolean isPresent() {
		return value != null;
	}
	
	/** Get the value, which must be present.
	 * @throws IllegalStateException if there is no value.
	 * @return the value.
	 */
	public String get() {
		if (value == null) {
			throw new IllegalStateException("Cannot call get() on an empty OptionalString");
		}
		return value;
	}
	
	/** Returns the string value or null if no value is present.
	 * @return the value or null.
	 */
	public String orNull() {
		return value;
	}

	@Override
	public String toString() {
		if (isPresent()) {
			return "OptionalString[" + value + "]";
		} else {
			return "OptionalString.empty";
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		OptionalString other = (OptionalString) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
}
