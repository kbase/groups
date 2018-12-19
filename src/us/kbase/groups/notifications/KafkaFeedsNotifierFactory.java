package us.kbase.groups.notifications;

import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.checkString;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.notifications.NotificationsFactory;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

public class KafkaFeedsNotifierFactory implements NotificationsFactory {
	
	// TODO JAVADOC
	// TODO TEST integration tests w/ just client & also full group server
	
	/* Since this is expected to deal with low volumes (basically just adding users to groups
	 * based on user input plus admins adding resources here and there), we do things
	 * that slow down the send operation but improve reliability and user messaging:
	 * 1) Require full write to replicates before Kafka returns
	 * 2) Wait for the return and check it worked. If not, throw an exception *in the calling
	 * thread*. Thus the user is notified if something goes wrong, and they can cancel
	 * their request and try again if they want (this obviously doesn't work for accepting and
	 * denying requests. If something goes wrong there, too bad).
	 * 
	 * If this turns out to be a bad plan, we may need to relax those requirements.
	 * 
	 * To improve reliability further, we'd need persistent storage of unsent feeds messages -
	 * maybe a flag on the request object?.
	 */
	
	private static final String KAFKA = "Kafka";
	private static final String KCFG_BOOSTRAP_SERVERS = "bootstrap.servers";
	private static final String OP = "operation";
	private static final String OP_CANCEL = "expire";
	private static final String OP_NOTIFY = "notify";
	private static final String SOURCE = "source";
	private static final String GROUP_SOURCE = "groupsservice";
	private static final String FEEDS_TOPIC = "feeds-topic";
	private static final String KAFKA_FEEDS_TOPIC = KAFKA + " " + FEEDS_TOPIC;
	
	
	// https://stackoverflow.com/questions/37062904/what-are-apache-kafka-topic-name-limitations
	// Don't include . and _ because of
	// https://github.com/mcollina/ascoltatori/issues/165#issuecomment-267314016
	private final static Pattern INVALID_TOPIC_CHARS = Pattern.compile("[^a-zA-Z0-9-]+");

	@Override
	public Notifications getNotifier(final Map<String, String> configuration)
			throws IllegalParameterException, MissingParameterException {
		requireNonNull(configuration, "configuration");
		System.out.println("INIT KFNF NOTIFICATION AGENT - RUN PROTOCOL [HOMO SAPIENS SAPIENS " +
				"GLOBAL LIQUIFICATION]");
		//TODO FEEDS support other config options (ssl etc). Unfortunately will have to parse each key individually as different types are required.
		final Map<String, Object> cfg = new HashMap<>();
		final String topic = (String) configuration.get(FEEDS_TOPIC);
		final String bootstrapServers = checkString(
				configuration.get(KCFG_BOOSTRAP_SERVERS), KAFKA + " " + KCFG_BOOSTRAP_SERVERS);
		// maybe make this config accessible in the factory so it can be tested in integration tests
		cfg.put(KCFG_BOOSTRAP_SERVERS, bootstrapServers);
		cfg.put("acks", "all");
		cfg.put("enable.idempotence", true);
		cfg.put("delivery.timeout.ms", 30000);
		return new KafkaFeedsNotifier(
				topic,
				bootstrapServers,
				new KafkaProducer<>(cfg, new StringSerializer(), new MapSerializer()));
	}
	
	public static class MapSerializer implements Serializer<Map<String, Object>> {

		private static final ObjectMapper MAPPER = new ObjectMapper();
		
		public MapSerializer() {}
		
		@Override
		public void close() {
			// nothing to do;
		}

		@Override
		public void configure(Map<String, ?> arg0, boolean arg1) {
			// nothing to do
		}

		@Override
		public byte[] serialize(final String topic, final Map<String, Object> data) {
			try {
				return MAPPER.writeValueAsBytes(requireNonNull(data, "data"));
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Unserializable data sent to Kafka: " + e.getMessage(),
						e);
			}
		}
	}
	
	private static class KafkaFeedsNotifier implements Notifications {

		private final String topic;
		private final KafkaProducer<String, Map<String, Object>> client;
		
		// constructor is here to allow for unit tests
		private KafkaFeedsNotifier(
				final String topic,
				final String bootstrapServers,
				final KafkaProducer<String, Map<String, Object>> client)
				throws MissingParameterException, IllegalParameterException {
			this.topic = checkString(topic, KAFKA_FEEDS_TOPIC, 249);
			final Matcher m = INVALID_TOPIC_CHARS.matcher(this.topic);
			if (m.find()) {
				throw new IllegalParameterException(String.format("Illegal character in %s %s: %s",
						KAFKA_FEEDS_TOPIC, this.topic, m.group()));
			}
			this.client = requireNonNull(client, "client");
			try {
				client.partitionsFor(this.topic); // check kafka is up
			} catch (KafkaException e) {
				// TODO CODE this blocks forever, needs 2.2.0 for a fix.
				// https://issues.apache.org/jira/browse/KAFKA-5503
				client.close(0, TimeUnit.MILLISECONDS);
				// might want a notifier exception here
				throw new IllegalParameterException("Could not reach Kafka instance at " +
						bootstrapServers);
			}
		}
		
		private void post(final Map<String, Object> message) {
			final Future<RecordMetadata> res = client.send(new ProducerRecord<>(topic, message));
			try {
				res.get(35000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | TimeoutException e) {
				throw new RuntimeException("Failed sending notification to Kafka: " +
						e.getMessage(), e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Failed sending notification to Kafka: " +
						e.getCause().getMessage(), e.getCause());
			}
		}
		
		@Override
		public void notify(final Collection<UserName> targets, final GroupRequest request) {
			checkNoNullsInCollection(targets, "targets");
			requireNonNull(request, "request");
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

		@Override
		public void cancel(final RequestID requestID) {
			post(ImmutableMap.of(
					OP, OP_CANCEL,
					SOURCE, GROUP_SOURCE,
					"external_ids", Arrays.asList(
							requireNonNull(requestID, "requestID").getID())));
		}

		@Override
		public void deny(final Collection<UserName> targets, final GroupRequest request) {
			checkNoNullsInCollection(targets, "targets");
			requireNonNull(request, "request");
			postNotification(
					targets,
					GROUP_SOURCE,
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
			checkNoNullsInCollection(targets, "targets");
			requireNonNull(request, "request");
			postNotification(
					targets,
					GROUP_SOURCE,
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
			requireNonNull(user, "user");
			checkNoNullsInCollection(targets, "targets");
			postNotification(
					targets,
					GROUP_SOURCE,
					null,
					requireNonNull(groupID, "groupID"),
					requireNonNull(type, "type"),
					requireNonNull(resource, "resource"),
					null,
					"updated",
					"alert");
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
			if (targets.isEmpty()) {
				return;
			}
			final Map<String, Object> post = new HashMap<>();
			post.put(OP, OP_NOTIFY);
			post.put("users", targets.stream().map(t -> t.getName()).collect(Collectors.toSet()));
			post.put("target", Arrays.asList(resourceID.getName()));
			post.put("level", level);
			post.put("actor", actor);
			post.put("object", groupID.getName());
			post.put("verb", verb);
			if (requestID != null) {
				post.put("external_key", requestID.getID());
			}
			post.put("expires", expirationDate == null ? null : expirationDate.toEpochMilli());

			//TODO FEEDS include denyReason?
			post.put("context", ImmutableMap.of("resourcetype", resourceType.getName()));
			
			post.put(SOURCE, GROUP_SOURCE);
			
			post(post);
		}
		
	}
}
