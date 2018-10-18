package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;

public class CreateModAndExpireTimes extends CreateAndModTimes {
	
	private final Instant expirationTime;

	private CreateModAndExpireTimes(
			final Instant creationTime,
			final Instant modificationTime,
			final Instant expirationTime) {
		super(creationTime, modificationTime);
		this.expirationTime = expirationTime;
	}

	public Instant getExpirationTime() {
		return expirationTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expirationTime == null) ? 0 : expirationTime.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CreateModAndExpireTimes other = (CreateModAndExpireTimes) obj;
		if (expirationTime == null) {
			if (other.expirationTime != null) {
				return false;
			}
		} else if (!expirationTime.equals(other.expirationTime)) {
			return false;
		}
		return true;
	}
	
	public static Builder getBuilder(final Instant creationTime, final Instant expirationTime) {
		return new Builder(creationTime, expirationTime);
	}
	
	public static class Builder {

		private final Instant creationTime;
		private final Instant expirationTime;
		private Instant modificationTime = null;

		public Builder(
				final Instant creationTime,
				final Instant expirationTime) {
			checkNotNull(creationTime, "creationTime");
			checkNotNull(expirationTime, "expirationTime");
			this.creationTime = creationTime;
			this.expirationTime = expirationTime;
			if (creationTime.isAfter(expirationTime)) {
				throw new IllegalArgumentException("creation time must be before expiration time");
			}
		}
		
		public Builder withModificationTime(final Instant modificationTime) {
			checkNotNull(modificationTime, "modificationTime");
			if (creationTime.isAfter(modificationTime)) {
				throw new IllegalArgumentException(
						"creation time must be before modification time");
			}
			this.modificationTime = modificationTime;
			return this;
		}
		
		public CreateModAndExpireTimes build() {
			if (modificationTime == null) {
				return new CreateModAndExpireTimes(creationTime, creationTime, expirationTime);
			} else {
				return new CreateModAndExpireTimes(creationTime, modificationTime, expirationTime);
			}
		}
	}
	
}
