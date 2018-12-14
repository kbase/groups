package us.kbase.test.groups.notifications;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.inst;
import static us.kbase.test.groups.TestCommon.set;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.notifications.KafkaFeedsNotifierFactory;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class KafkaFeedsNotifierFactoryTest {
	
	private static final GroupRequest REQUEST;
	static {
		try {
			REQUEST = GroupRequest.getBuilder(
					new RequestID(UUID.randomUUID()), new GroupID("i"), new UserName("n"),
					CreateModAndExpireTimes.getBuilder(inst(1), inst(2)).build())
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Fix yer tests newb");
		}
	}

	private static TestMocks initTestMocks(final String topic, final String bootstrapServers)
			throws Exception {
		@SuppressWarnings("unchecked")
		final KafkaProducer<String, Map<String, Object>> client = mock(KafkaProducer.class);
		final Notifications notis = getKafkaNotifier(topic, bootstrapServers, client);
		return new TestMocks(client, notis);
	}

	private static Notifications getKafkaNotifier(
			final String topic,
			final String bootstrapServers,
			final KafkaProducer<String, Map<String, Object>> client)
			throws Exception {
		final Class<?> inner = KafkaFeedsNotifierFactory.class.getDeclaredClasses()[0];
		final Constructor<?> con = inner.getDeclaredConstructor(
				String.class, String.class, KafkaProducer.class);
		con.setAccessible(true);
		final Notifications notis = (Notifications) con.newInstance(
				topic, bootstrapServers, client);
		return notis;
	}
	
	private static final class TestMocks {
		private KafkaProducer<String, Map<String, Object>> client;
		private Notifications notis;

		private TestMocks(
				final KafkaProducer<String, Map<String, Object>> client,
				final Notifications notis) {
			this.client = client;
			this.notis = notis;
		}
	}
	
	@Test
	public void addResourceNoTargets() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic", "localhost:9081");
		mocks.notis.addResource(
				new UserName("foo"),
				set(),
				new GroupID("id"),
				new ResourceType("restype"),
				new ResourceID("rid"));
		
		verify(mocks.client).partitionsFor("mytopic");
		verifyNoMoreInteractions(mocks.client);
	}
	
	@Test
	public void addResource() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic", "localhost:9081");
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "groupsservice")
				.with("users", set("bar", "baz"))
				.with("target", Arrays.asList("rid"))
				.with("expires", null)
				.with("level", "alert")
				.with("object", "id")
				.with("verb", "updated")
				.with("context", ImmutableMap.of("resourcetype", "restype"))
				.build())))
				.thenReturn(fut);
		
		mocks.notis.addResource(
				new UserName("foo"),
				set(new UserName("bar"), new UserName("baz")),
				new GroupID("id"),
				new ResourceType("restype"),
				new ResourceID("rid"));
		
		verify(mocks.client).partitionsFor("mytopic");
		verify(fut).get(35000, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void addResourceFailNulls() throws Exception {
		final UserName u = new UserName("foo");
		final GroupID g = new GroupID("id");
		final ResourceType t = new ResourceType("restype");
		final ResourceID r = new ResourceID("rid");
		
		addResourceFail(null, set(), g, t, r, new NullPointerException("user"));
		addResourceFail(u, null, g, t, r, new NullPointerException("targets"));
		addResourceFail(u, set(), null, t, r, new NullPointerException("groupID"));
		addResourceFail(u, set(), g, null, r, new NullPointerException("type"));
		addResourceFail(u, set(), g, t, null, new NullPointerException("resource"));
		
		addResourceFail(u, set(u, null), g, t, r, new NullPointerException(
				"Null item in collection targets"));
		
	}
	
	private void addResourceFail(
			final UserName user,
			final Set<UserName> targets,
			final GroupID gid,
			final ResourceType type,
			final ResourceID res,
			final Exception expected) {
		try {
			initTestMocks("t", "s").notis.addResource(user, targets, gid, type, res);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void cancel() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic2", "localhost:9081");
		
		final UUID id = UUID.randomUUID();
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic2",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "cancel")
				.with("external_ids", Arrays.asList(id.toString()))
				.with("source", "groupsservice")
				.build())))
				.thenReturn(fut);
		
		mocks.notis.cancel(new RequestID(id));
		
		verify(mocks.client).partitionsFor("mytopic2");
		verify(fut).get(35000, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void cancelFailNull() throws Exception {
		try {
			initTestMocks("t", "b").notis.cancel(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("requestID"));
		}
	}
	
	@Test
	public void notifyNoTargets() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic", "localhost:9081");
		mocks.notis.notify(
				set(),
				GroupRequest.getBuilder(new RequestID(UUID.randomUUID()), new GroupID("gid"),
						new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid")))
				.withResourceType(new ResourceType("rtype"))
				.withType(RequestType.REQUEST)
				.build());
		
		verify(mocks.client).partitionsFor("mytopic");
		verifyNoMoreInteractions(mocks.client);
	}
	
	@Test
	public void notifyRequest() throws Exception {
		notify(RequestType.REQUEST, "request");
	}
	
	@Test
	public void notifyInvite() throws Exception {
		notify(RequestType.INVITE, "invite");
	}

	private void notify(final RequestType rtype, final String expectedRType) throws Exception {
		final TestMocks mocks = initTestMocks("mytopic-1", "localhost:9081");
		final UUID id = UUID.randomUUID();
		
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic-1",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "act")
				.with("users", set("bar", "foo"))
				.with("target", Arrays.asList("resid"))
				.with("expires", 30000L)
				.with("external_key", id.toString())
				.with("level", "request")
				.with("object", "gid")
				.with("verb", expectedRType)
				.with("context", ImmutableMap.of("resourcetype", "rtype"))
				.build())))
				.thenReturn(fut);
		
		mocks.notis.notify(
				set(new UserName("foo"), new UserName("bar")),
				GroupRequest.getBuilder(new RequestID(id), new GroupID("gid"), new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid")))
				.withResourceType(new ResourceType("rtype"))
				.withType(rtype)
				.build());
		
		verify(mocks.client).partitionsFor("mytopic-1");
		verify(fut).get(35000, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void notifyFailNulls() throws Exception {
		notifyFail(null, REQUEST, new NullPointerException("targets"));
		notifyFail(set(new UserName("n"), null), REQUEST, new NullPointerException(
				"Null item in collection targets"));
		notifyFail(set(), null, new NullPointerException("request"));
	}
	
	private void notifyFail(
			final Set<UserName> targets,
			final GroupRequest g,
			final Exception expected) {
		try {
			initTestMocks("t", "s").notis.notify(targets, g);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void acceptNoTargets() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic", "localhost:9081");
		mocks.notis.accept(
				set(),
				GroupRequest.getBuilder(new RequestID(UUID.randomUUID()), new GroupID("gid"),
						new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid")))
				.withResourceType(new ResourceType("rtype"))
				.withType(RequestType.REQUEST)
				.build());
		
		verify(mocks.client).partitionsFor("mytopic");
		verifyNoMoreInteractions(mocks.client);
	}
	
	@Test
	public void accept() throws Exception {
		final TestMocks mocks = initTestMocks(
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMOPQRSTUVWXYZ0123456789", "localhost:9081");
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>(
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMOPQRSTUVWXYZ0123456789",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "groupsservice")
				.with("users", set("bar", "baz"))
				.with("target", Arrays.asList("resid2"))
				.with("expires", null)
				.with("level", "alert")
				.with("object", "id2")
				.with("verb", "accept")
				.with("context", ImmutableMap.of("resourcetype", "rtype2"))
				.build())))
				.thenReturn(fut);
		
		mocks.notis.accept(
				set(new UserName("bar"), new UserName("baz")),
				GroupRequest.getBuilder(new RequestID(id), new GroupID("id2"), new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid2")))
				.withResourceType(new ResourceType("rtype2"))
				.withType(RequestType.INVITE)
				.build());
		
		verify(mocks.client).partitionsFor(
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMOPQRSTUVWXYZ0123456789");
		verify(fut).get(35000, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void acceptFailNulls() throws Exception {
		acceptFail(null, REQUEST, new NullPointerException("targets"));
		acceptFail(set(new UserName("n"), null), REQUEST, new NullPointerException(
				"Null item in collection targets"));
		acceptFail(set(), null, new NullPointerException("request"));
	}
	
	private void acceptFail(
			final Set<UserName> targets,
			final GroupRequest g,
			final Exception expected) {
		try {
			initTestMocks("t", "s").notis.accept(targets, g);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void denyNoTargets() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic", "localhost:9081");
		mocks.notis.notify(
				set(),
				GroupRequest.getBuilder(new RequestID(UUID.randomUUID()), new GroupID("gid"),
						new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid")))
				.withResourceType(new ResourceType("rtype"))
				.withType(RequestType.REQUEST)
				.build());
		
		verify(mocks.client).partitionsFor("mytopic");
		verifyNoMoreInteractions(mocks.client);
	}
	
	@Test
	public void deny() throws Exception {
		final TestMocks mocks = initTestMocks(
				TestCommon.LONG1001.substring(0, 249), "localhost:9081");
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>(
				TestCommon.LONG1001.substring(0, 249),
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "groupsservice")
				.with("users", set("bat", "bang"))
				.with("target", Arrays.asList("resid9"))
				.with("expires", null)
				.with("level", "alert")
				.with("object", "id8")
				.with("verb", "reject")
				.with("context", ImmutableMap.of("resourcetype", "rtype9"))
				.build())))
				.thenReturn(fut);
		
		mocks.notis.deny(
				set(new UserName("bat"), new UserName("bang")),
				GroupRequest.getBuilder(new RequestID(id), new GroupID("id8"), new UserName("act"),
						CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
				.withResource(new ResourceDescriptor(new ResourceID("resid9")))
				.withResourceType(new ResourceType("rtype9"))
				.withType(RequestType.REQUEST)
				.build());
		
		verify(mocks.client).partitionsFor(TestCommon.LONG1001.substring(0, 249));
		verify(fut).get(35000, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void denyFailNulls() throws Exception {
		denyFail(null, REQUEST, new NullPointerException("targets"));
		denyFail(set(new UserName("n"), null), REQUEST, new NullPointerException(
				"Null item in collection targets"));
		denyFail(set(), null, new NullPointerException("request"));
	}
	
	private void denyFail(
			final Set<UserName> targets,
			final GroupRequest g,
			final Exception expected) {
		try {
			initTestMocks("t", "s").notis.deny(targets, g);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	/* The post method is the same for all the notification calls, so we don't repeat each
	 * post failure test for each call.
	 * We do test with different methods for each failure mode though.
	 */
	
	@Test
	public void postFailInterrupted() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic3", "localhost:9081");
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic3",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "groupsservice")
				.with("users", set("bat", "bang"))
				.with("target", Arrays.asList("resid9"))
				.with("expires", null)
				.with("level", "alert")
				.with("object", "id8")
				.with("verb", "reject")
				.with("context", ImmutableMap.of("resourcetype", "rtype9"))
				.build())))
				.thenReturn(fut);
			
		when(fut.get(35000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("oopsie"));
		
		try {
			mocks.notis.deny(
					set(new UserName("bat"), new UserName("bang")),
					GroupRequest.getBuilder(new RequestID(id), new GroupID("id8"),
							new UserName("act"),
							CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
					.withResource(new ResourceDescriptor(new ResourceID("resid9")))
					.withResourceType(new ResourceType("rtype9"))
					.withType(RequestType.REQUEST)
					.build());
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got,
					new RuntimeException("Failed sending notification to Kafka: oopsie"));
		}
	}
	
	@Test
	public void postFailTimeout() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic3", "localhost:9081");
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		final UUID id = UUID.randomUUID();
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic3",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "notify")
				.with("source", "groupsservice")
				.with("actor", "act")
				.with("users", set("bat", "bang"))
				.with("target", Arrays.asList("resid9"))
				.with("external_key", id.toString())
				.with("expires", 30000L)
				.with("level", "request")
				.with("object", "id8")
				.with("verb", "invite")
				.with("context", ImmutableMap.of("resourcetype", "rtype9"))
				.build())))
				.thenReturn(fut);
			
		when(fut.get(35000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException("time up"));
		
		try {
			mocks.notis.notify(
					set(new UserName("bat"), new UserName("bang")),
					GroupRequest.getBuilder(new RequestID(id), new GroupID("id8"),
							new UserName("act"),
							CreateModAndExpireTimes.getBuilder(inst(10000), inst(30000)).build())
					.withResource(new ResourceDescriptor(new ResourceID("resid9")))
					.withResourceType(new ResourceType("rtype9"))
					.withType(RequestType.INVITE)
					.build());
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got,
					new RuntimeException("Failed sending notification to Kafka: time up"));
		}
	}
	
	@Test
	public void postFailExecutionException() throws Exception {
		final TestMocks mocks = initTestMocks("mytopic2", "localhost:9081");
		
		final UUID id = UUID.randomUUID();
		@SuppressWarnings("unchecked")
		final Future<RecordMetadata> fut = mock(Future.class);
		
		when(mocks.client.send(new ProducerRecord<String, Map<String,Object>>("mytopic2",
				MapBuilder.<String, Object>newHashMap()
				.with("operation", "cancel")
				.with("external_ids", Arrays.asList(id.toString()))
				.with("source", "groupsservice")
				.build())))
				.thenReturn(fut);

		when(fut.get(35000, TimeUnit.MILLISECONDS)).thenThrow(
				new ExecutionException("not this one", new IllegalStateException("this one")));
		
		try {
			mocks.notis.cancel(new RequestID(id));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new RuntimeException(
					"Failed sending notification to Kafka: this one"));
		}
	}
	

	/* Kafka notifier constructor fail tests */
	
	@Test
	public void constructBadTopicFail() throws Exception {
		badTopicFail(null, new MissingParameterException("Kafka feeds-topic"));
		badTopicFail("   \t    \n    ", new MissingParameterException("Kafka feeds-topic"));
		badTopicFail("   \t    \n    ", new MissingParameterException("Kafka feeds-topic"));
		badTopicFail(TestCommon.LONG1001.substring(0, 250),
				new IllegalParameterException("Kafka feeds-topic size greater than limit 249"));
		badTopicFail("  topic.whee ", new IllegalParameterException(
				"Illegal character in Kafka feeds-topic topic.whee: ."));
		badTopicFail("  topic_whee ", new IllegalParameterException(
				"Illegal character in Kafka feeds-topic topic_whee: _"));
		badTopicFail("  topic*whee ", new IllegalParameterException(
				"Illegal character in Kafka feeds-topic topic*whee: *"));
	}
	
	private void badTopicFail(final String topic, final Exception expected) throws Exception {
		try {
			initTestMocks(topic, "foo");
			fail("expected exception");
		} catch (InvocationTargetException got) {
			TestCommon.assertExceptionCorrect(got.getCause(), expected);
		}
	}
	
	@Test
	public void constructNullClientFail() throws Exception {
		try {
			getKafkaNotifier("foo", "foo", null);
			fail("expected exception");
		} catch (InvocationTargetException got) {
			TestCommon.assertExceptionCorrect(got.getCause(), new NullPointerException("client"));
		}
		
	}
	
	@Test
	public void constructKafkaConnectFail() throws Exception {
		@SuppressWarnings("unchecked")
		final KafkaProducer<String, Map<String, Object>> client = mock(KafkaProducer.class);
		when(client.partitionsFor("topicalointment")).thenThrow(new KafkaException("well, darn"));
		
		try {
			getKafkaNotifier("topicalointment", "localhost:5467", client);
			fail("expected exception");
		} catch (InvocationTargetException got) {
			TestCommon.assertExceptionCorrect(got.getCause(), new IllegalParameterException(
					"Could not reach Kafka instance at localhost:5467"));
		}
		verify(client).close(0, TimeUnit.MILLISECONDS);
	}
	
	@Test
	public void mapSerializer() throws Exception {
		final KafkaFeedsNotifierFactory.MapSerializer mapSerializer =
				new KafkaFeedsNotifierFactory.MapSerializer();
		
		mapSerializer.configure(null, false); // does nothing;
		mapSerializer.close(); // does nothing
		
		final byte[] res = mapSerializer.serialize("ignored", ImmutableMap.of("foo", "bar"));
		
		assertThat("incorrect serialization", new String(res), is("{\"foo\":\"bar\"}"));
	}
	
	@Test
	public void mapSerializerFailNull() throws Exception {
		mapSerializerFail(null, new NullPointerException("data"));
	}

	@Test
	public void mapSerializerFailUnserializable() throws Exception {
		final String e = "Unserializable data sent to Kafka: No serializer found for " +
				"class java.io.ByteArrayOutputStream and no properties discovered to " +
				"create BeanSerializer (to avoid exception, disable " +
				"SerializationFeature.FAIL_ON_EMPTY_BEANS) ) (through reference chain: " +
				"com.google.common.collect.SingletonImmutableBiMap[\"foo\"])";
		mapSerializerFail(ImmutableMap.of("foo", new ByteArrayOutputStream()),
				new RuntimeException(e));
	}
	
	@SuppressWarnings("resource")
	private void mapSerializerFail(final Map<String, Object> data, final Exception expected) {
		try {
			new KafkaFeedsNotifierFactory.MapSerializer().serialize("ignored", data);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	/* Tests for building the notifier with the factory. Can really only test failure
	 * scenarios in unit tests.
	 */

	@Test
	public void getNotifierFailNull() throws Exception {
		getNotifierFail(null, new NullPointerException("configuration"));
	}
	
	@Test
	public void getNotifierFailBadBootstrapServer() throws Exception {
		final Map<String, String> c = new HashMap<>();
		c.put("boostrap.servers.wrong", null);
		getNotifierFail(c, new MissingParameterException("Kafka bootstrap.servers"));
		c.put("boostrap.servers", null);
		getNotifierFail(c, new MissingParameterException("Kafka bootstrap.servers"));
		c.put("boostrap.servers", "   \t      ");
		getNotifierFail(c, new MissingParameterException("Kafka bootstrap.servers"));
	}
	
	private void getNotifierFail(final Map<String, String> config, final Exception expected) {
		try {
			new KafkaFeedsNotifierFactory().getNotifier(config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
