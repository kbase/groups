package us.kbase.groups.notifications;

import static us.kbase.groups.util.Util.checkString;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.notifications.NotificationsFactory;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.RequestID;

public class DirectFeedsServiceNotifierFactory implements NotificationsFactory {

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
	
		private static final Client CLI = ClientBuilder.newClient();
		private static final String CREATE_PATH = "api/V1/notification";
		private static final String AUTH_HEADER = "Authorization";
		private final String url;
		private final Token token;
		
		public DirectFeedsServiceNotifier(final String url, final Token token) {
			this.url = url;
			this.token = token;
			//TODO NOW finish startup check
			//TODO CODE handle clock skew
		}

		@Override
		public void notify(
				final Collection<UserName> targets,
				final Group group,
				final GroupRequest request) {
			final Map<String, Object> post = new HashMap<>();
			post.put("target", targets.stream().map(t -> t.getName())
					.collect(Collectors.toList()));
			post.put("level", "request");
			post.put("actor", request.getRequester().getName());
			if (request.getType().equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
				post.put("object", request.getGroupID().getName());
			} else if (request.getType().equals(GroupRequestType.INVITE_TO_GROUP)) {
				post.put("object", request.getTarget().get().getName());
			} else if (request.getType().equals(GroupRequestType.REQUEST_ADD_WORKSPACE) ||
					request.getType().equals(GroupRequestType.INVITE_WORKSPACE)) {
				post.put("object", request.getWorkspaceTarget().get().getID());
			}
			post.put("verb", request.isInvite() ? "invite" : "request");
			post.put("external_key", request.getID().getID());
			post.put("expires", request.getExpirationDate().toEpochMilli());

			final Map<String, Object> context = new HashMap<>();
			context.put("requesttype", request.getType().getRepresentation());
			context.put("groupid", request.getGroupID().getName());
			post.put("context", context);
			
			//TODO NOW remove later
			post.put("source", "fake");
			context.put("requestid", request.getID().getID());
			
			final URI target = UriBuilder.fromUri(url).path(CREATE_PATH).build();
			
			final WebTarget wt = CLI.target(target);
			final Builder req = wt.request().header(AUTH_HEADER, token.getToken());

			final Response res = req.post(Entity.json(post));
			
			System.out.println(res.readEntity(String.class));
			
			//TODO LOG log id (or just the entire package?)
			
			//TODO NOW retry on fail - circular queue? should probably submit these to a queue in the first place and return immediately. put in DB?
		}
		
		@Override
		public void cancel(final RequestID requestID) {
			// TODO NOW Auto-generated method stub
		
		}
		
		@Override
		public void deny(final Collection<UserName> targets, final GroupRequest request) {
			// TODO NOW Auto-generated method stub
		
		}
		
		@Override
		public void accept(final Collection<UserName> targets, final GroupRequest request) {
			// TODO NOW Auto-generated method stub
		
		}
	}
}
