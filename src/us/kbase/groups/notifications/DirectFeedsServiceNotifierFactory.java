package us.kbase.groups.notifications;

import static us.kbase.groups.util.Util.checkString;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.notifications.NotificationsFactory;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

public class DirectFeedsServiceNotifierFactory implements NotificationsFactory {
	
	// this is probably going to change significantly, so no docs or tests for now, and
	// minimal implementation. Might be switching to Kafka, not clear what policy needs to be
	// re failures, etc.

	// TODO JAVADOC
	// TODO TEST
	

	@Override
	public Notifications getNotifier(final Map<String, String> configuration)
			throws IllegalParameterException, MissingParameterException {
		System.out.println("INIT DFSNF NOTIFICATION AGENT - VAPORIZING ALL HUMANS IN AREA");
		final String url = configuration.get("url");
		checkString(url, "direct feeds notifier url");
		final String token = configuration.get("token");
		checkString(token, "direct feeds notifier token");
		return new DirectFeedsServiceNotifier(url, new Token(token));
	}
	
	private static class DirectFeedsServiceNotifier implements Notifications {
	
		private static final ObjectMapper MAPPER = new ObjectMapper();
		private static final Client CLI = ClientBuilder.newClient();
		private static final String PATH_CREATE = "api/V1/notification";
		private static final String PATH_PERMS = "permissions";
		private static final String AUTH_HEADER = "Authorization";
		private final String url;
		private final Token token;
		
		public DirectFeedsServiceNotifier(final String url, final Token token)
				throws IllegalParameterException {
			this.url = url;
			this.token = token;
			final URI target = UriBuilder.fromUri(url).path(PATH_PERMS).build();
			
			final WebTarget wt = CLI.target(target);
			final Builder req = wt.request().header(AUTH_HEADER, token.getToken());

			final Response res = req.get();
			
			if (res.getStatus() != 200) {
				final String err = res.readEntity(String.class);
				final Map<String, Object> errMap;
				try {
					errMap = MAPPER.readValue(err, new TypeReference<Map<String, Object>>() {});
				} catch (IOException e) {
					// ok, we got some random web page probably
					LoggerFactory.getLogger(getClass()).error("Error contacting feeds service:\n" +
							err);
					throw new IllegalParameterException("Error contacting feeds service:\n" +
							truncate(err));
				}
				// assume we're talking to the feeds service at this point
				throw new IllegalParameterException("Error contacting feeds service: " +
						errMap.get("error")); // just shove the whole thing in there for now
			} else {
				// we'll assume that if we get a 200 we are actually talking to the feeds
				// service
				final Map<String, Object> resp = res.readEntity(
						new GenericType<Map<String, Object>>() {});
				@SuppressWarnings("unchecked")
				final Map<String, String> tokenInfo = (Map<String, String>) resp.get("token");
				if (tokenInfo.get("service") == null) {
					// this error seems crummy. Probably want a better errors.
					throw new IllegalParameterException(
							"Feeds notifier token must be a service token");
				}
			}
		}

		private String truncate(final String err) {
			if (err.length() > 500) {
				return err.substring(0, 497) + "...";
			} else {
				return err;
			}
		}

		@Override
		public void notify(
				final Collection<UserName> targets,
				final Group group,
				final GroupRequest request) {
			postNotification(
					targets,
					request.getRequester().getName(),
					request.getID(),
					request.getGroupID(),
					request.getResourceType(),
					request.getResource().getResourceID(),
					request.getExpirationDate(),
					request.isInvite() ? "invite" : "request",
					"request");
		}

		private void postNotification(
				final Collection<UserName> targets,
				final String actor,
				final RequestID requestID,
				final GroupID groupID,
				final ResourceType resourceType,
				final ResourceID resourceID, 
				final Instant expirationDate,
				final String verb,
				final String level) {
			final Map<String, Object> post = new HashMap<>();
			post.put("users", targets.stream().map(t -> t.getName())
					.collect(Collectors.toList()));
			post.put("target", resourceID.getName());
			post.put("level", level);
			post.put("actor", actor);
			post.put("object", groupID.getName());
			post.put("verb", verb);
			if (requestID != null) {
				post.put("external_key", requestID.getID());
			}
			post.put("expires", expirationDate == null ? null : expirationDate.toEpochMilli());

			final Map<String, Object> context = new HashMap<>();
			//TODO NOW include denyReason?
			context.put("resourcetype", resourceType.getName());
			context.put("groupid", groupID.getName());
			post.put("context", context);
			
			//TODO NOW remove later
			post.put("source", "fake");
			if (requestID != null) {
				context.put("requestid", requestID.getID());
			}
			
			final URI target = UriBuilder.fromUri(url).path(PATH_CREATE).build();
			
			final WebTarget wt = CLI.target(target);
			final Builder req = wt.request().header(AUTH_HEADER, token.getToken());

			final Response res = req.post(Entity.json(post));
			
			System.out.println(res.readEntity(String.class));
			
			//TODO LOG log id (or just the entire package?)
			
			//TODO NOW retry on fail - circular queue? should probably submit these to a queue in the first place and return immediately. put in DB?
		}
		
		@Override
		public void cancel(final RequestID requestID) {
			// TODO NOW waiting on feeds cancel endpoint
		
		}
		
		@Override
		public void deny(final Collection<UserName> targets, final GroupRequest request) {
			if (targets.isEmpty()) {
				// currently deny is unused
				return;
			}
			postNotification(
					targets,
					"groupservice",
					null,
					request.getGroupID(),
					request.getResourceType(),
					request.getResource().getResourceID(),
					null,
					"reject",
					"alert");
		}
		
		@Override
		public void accept(final Collection<UserName> targets, final GroupRequest request) {
			postNotification(
					targets,
					"groupservice",
					null,
					request.getGroupID(),
					request.getResourceType(),
					request.getResource().getResourceID(),
					null,
					"accept",
					"alert");
		
		}

		@Override
		public void addResource(
				final UserName user,
				final Set<UserName> targets,
				final GroupID groupID,
				final ResourceType type,
				final ResourceID resource) {
			postNotification(
					targets,
					"groupservice",
					null,
					groupID,
					type,
					resource,
					null,
					"updated",
					"alert");
		}
	}
}
