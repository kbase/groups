package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;

/** Creation and modification times for some item.
 * @author gaprice@lbl.gov
 *
 */
public class CreateAndModTimes {
	
	private final Instant creationTime;
	private final Instant modificationTime;
	
	/** Construct times where the creation and modification times are identical.
	 * @param creationTime the creation and modification time.
	 */
	public CreateAndModTimes(final Instant creationTime) {
		this(creationTime, creationTime);
	}
	
	/** Construct times.
	 * @param creationTime the creation time.
	 * @param modificationTime the modification time.
	 */
	public CreateAndModTimes(final Instant creationTime, final Instant modificationTime) {
		checkNotNull(creationTime, "creationTime");
		checkNotNull(modificationTime, "modificationTime");
		this.creationTime = creationTime;
		this.modificationTime = modificationTime;
		if (creationTime.isAfter(modificationTime)) {
			throw new IllegalArgumentException("creation time must be before modification time");
		}
	}

	/** Get the creation time.
	 * @return the creation time.
	 */
	public Instant getCreationTime() {
		return creationTime;
	}

	/** Get the modification time.
	 * @return the modification time.
	 */
	public Instant getModificationTime() {
		return modificationTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
		result = prime * result + ((modificationTime == null) ? 0 : modificationTime.hashCode());
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
		CreateAndModTimes other = (CreateAndModTimes) obj;
		if (creationTime == null) {
			if (other.creationTime != null) {
				return false;
			}
		} else if (!creationTime.equals(other.creationTime)) {
			return false;
		}
		if (modificationTime == null) {
			if (other.modificationTime != null) {
				return false;
			}
		} else if (!modificationTime.equals(other.modificationTime)) {
			return false;
		}
		return true;
	}
	
}
