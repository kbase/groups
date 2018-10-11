package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import us.kbase.groups.build.GroupsBuilder;
import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.storage.GroupsStorage;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** The core class in the Groups software. 
 * @author gaprice@lbl.gov
 *
 */
public class Groups {

	//TODO JAVADOC
	//TODO TEST
	//TODO LOGGING for all actions
	
	private final GroupsStorage storage;
	private final UserHandler userHandler;
	private final Clock clock;
	
	/** Create a new {@link Groups} class.
	 * @param storage the storage system to be used by the class.
	 * @param userHandler the user handler by which users shall be handled.
	 */
	public Groups(final GroupsStorage storage, final UserHandler userHandler) {
		this(storage, userHandler, Clock.systemDefaultZone());
	}
	
	// for testing
	private Groups(final GroupsStorage storage, final UserHandler userHandler, final Clock clock) {
		checkNotNull(storage, "storage");
		checkNotNull(userHandler, "userHandler");
		this.storage = storage;
		this.userHandler = userHandler;
		this.clock = clock;
	}
	
	public Group createGroup(
			final Token userToken,
			final GroupCreationParams createParams)
			throws InvalidTokenException, AuthenticationException, GroupExistsException,
				GroupsStorageException {
		checkNotNull(userToken, "userToken");
		checkNotNull(createParams, "createParams");
		final UserName owner = userHandler.getUser(userToken);
		final Instant now = clock.instant();
		storage.createGroup(createParams.toGroup(owner, new CreateAndModTimes(now)));
		
		try {
			return storage.getGroup(createParams.getGroupID());
		} catch (NoSuchGroupException e) {
			throw new RuntimeException(
					"Just created a group and it's already gone. Something's really broken", e);
		}
	}
	
	public Group getGroup(final Token userToken, final GroupID groupID)
			throws InvalidTokenException, AuthenticationException, NoSuchGroupException,
				GroupsStorageException {
		// TODO PRIVATE handle privacy concerns
		// TODO PRIVATE allow admins & std users to see private parts
		// for now there's no private stuff in groups so we ignore the token. Usually we'd
		// check and see if the user is a group member, and if not, remove private stuff
		final Group g = storage.getGroup(groupID);
		if (userToken != null) {
			@SuppressWarnings("unused")
			final UserName user = userHandler.getUser(userToken);
			// if not a member, remove private stuff from group
		}
		return g;
	}
	
	// this assumes the number of groups is small enough that listing them all is OK.
	// obviously if the number of groups gets > ~100k something will have to change
	public List<Group> getGroups()
			throws GroupsStorageException {
		final List<Group> groups = storage.getGroups();
		// TODO PRIVATE handle privacy concerns - remove all private stuff
		// for now no private stuff to remove
		return groups;
	}
	
	public static void main(final String[] args) throws Exception {
		System.setProperty("KB_DEPLOYMENT_CONFIG", "./deploy.cfg");
		final Groups g = new GroupsBuilder(new GroupsConfig()).getGroups();
		
		final Token t = new Token(args[0]);
		final Group g1 = g.createGroup(
				t,
				GroupCreationParams.getBuilder(new GroupID("foo"), new GroupName("bar"))
						.withType(GroupType.team)
						.withDescription("desc")
						.build());
		System.out.println(g1);
		
		System.out.println(g.getGroup(t, new GroupID("foo")));
	}
	
}
