package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;
import static us.kbase.groups.util.Util.exceptOnEmpty;

/** An item associated with a field in some object, optionally containing data, that may be set,
 * removed, or not changed.
 * @author gaprice@lbl.gov
 *
 * @param <T> the data item. 
 */
public class FieldItem<T> {
	
	private static final byte HAS_ITEM = 1;
	private static final byte NO_ACTION = 0;
	private static final byte REMOVE = -1;
	
	private final T item;
	private final byte state;
	
	private FieldItem(final T item, final byte state) {
		this.item = item;
		this.state = state;
	}
	
	/** Create a field item containing data.
	 * @param item the data.
	 * @return the field item.
	 */
	public static <T> FieldItem<T> from(final T item) {
		checkNotNull(item, "item");
		return new FieldItem<>(item, HAS_ITEM);
	}
	
	/** Create a field item from data that may be null. Calling this method with a null input
	 * is the equivalent of {@link #noAction()}.
	 * @param item the data or null.
	 * @return the field item.
	 */
	public static <T> FieldItem<T> fromNullable(final T item) {
		return new FieldItem<>(item, item == null ? NO_ACTION : HAS_ITEM);
	}
	
	/** Create a field item denoting that any data associated with the field should be removed.
	 * @return the field item.
	 */
	public static <T> FieldItem<T> remove() {
		return new FieldItem<T>(null, REMOVE);
	}
	
	/** Create a field item denoting that any data associated with the field should be left
	 * unchanged.
	 * @return the field item.
	 */
	public static <T> FieldItem<T> noAction() {
		return new FieldItem<T>(null, NO_ACTION);
	}
	
	/** Get the data associated with this field item.
	 * @return the data.
	 * @throws IllegalStateException if there is no data contained in the field item.
	 */
	public T get() throws IllegalStateException {
		if (item == null) {
			throw new IllegalStateException(
					"Cannot call get() on a FieldItem without an item");
		}
		return item;
	}
	
	/** Get the data associated with the field item or null if there is no data.
	 * @return the data.
	 */
	public T orNull() {
		return item;
	}
	
	/** Check if this field item denotes that any data associated with the field should be
	 * removed.
	 * @return true if the field value should be removed.
	 */
	public boolean isRemove() {
		return state == REMOVE;
	}
	
	/** Check if this field item contains data.
	 * @return true if the item contains data.
	 */
	public boolean hasItem() {
		return state == HAS_ITEM;
	}
	
	/** Check if this field item requires that action be taken - either that a field should be
	 * set with the value in this item, or that the field value should be removed. The inverse of
	 * {@link #isNoAction()}.
	 * @return true if an action should be taken.
	 */
	public boolean hasAction() {
		return !isNoAction();
	}
	
	/** Check if this field item denotes that any data associated with the field should be
	 * left alone.
	 * @return true if no action should be taken.
	 */
	public boolean isNoAction() {
		return state == NO_ACTION;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		result = prime * result + state;
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FieldItem)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		FieldItem<T> other = (FieldItem<T>) obj;
		if (item == null) {
			if (other.item != null) {
				return false;
			}
		} else if (!item.equals(other.item)) {
			return false;
		}
		if (state != other.state) {
			return false;
		}
		return true;
	}
	
	/** A {@link FieldItem} that optionally contains a non-whitespace only string.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class StringField extends FieldItem<String> {
		
		private StringField(final String item, final byte state) {
			super(item, state);
		}
		
		/** Create a new string item. The string is {@link String#trim()}ed.
		 * @param s the string. May not be null or the empty string.
		 * @return the field item.
		 */
		public static StringField from(final String s) {
			exceptOnEmpty(s, "s");
			return new StringField(s.trim(), HAS_ITEM);
		}
		
		/** Create a new string item. If the string is null or whitespace only, a
		 * {@link StringField#noAction()} is returned. Otherwise the string is
		 * {@link String#trim()}ed.
		 * @param s the string.
		 * @return the field item.
		 */
		public static StringField fromNullable(final String s) {
			if (isNullOrEmpty(s)) {
				return new StringField(null, NO_ACTION);
			} else {
				return new StringField(s.trim(), HAS_ITEM);
			}
		}
		
		/** Create a string field denoting that any data associated with the field should be
		 * removed.
		 * @return the field item.
		 */
		@SuppressWarnings("unchecked")
		public static StringField remove() {
			return new StringField(null, REMOVE);
		}
		
		/** Create a field item denoting that any data associated with the field should be left
		 * unchanged.
		 * @return the field item.
		 */
		@SuppressWarnings("unchecked")
		public static StringField noAction() {
			return new StringField(null, NO_ACTION);
		}
	}
}
