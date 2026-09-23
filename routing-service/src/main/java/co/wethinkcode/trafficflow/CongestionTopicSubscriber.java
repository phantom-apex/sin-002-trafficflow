package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subscribes to the shared "congestion-topic" ActiveMQ topic and keeps the
 * latest observed level in memory. This replaces the per-request REST poll of
 * Congestion Service: once a message has arrived, /route reads the cached
 * level instead of calling out over HTTP.
 */
public class CongestionTopicSubscriber implements AutoCloseable, MessageListener {

    /** Sentinel meaning "no message received yet". */
    public static final int NO_LEVEL_YET = -1;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicInteger latestLevel = new AtomicInteger(NO_LEVEL_YET);
    private final Connection connection;
    private final Session session;

    public CongestionTopicSubscriber() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            throw new IllegalStateException("could not subscribe to " + MqConfig.TOPIC
                    + " at " + MqConfig.BROKER_URL, e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof javax.jms.TextMessage text) {
                JsonNode body = JSON.readTree(text.getText());
                if (body != null && body.has("level") && body.get("level").isInt()) {
                    int level = body.get("level").asInt();
                    latestLevel.set(level);
                    System.out.println("[routing-service] congestion update from topic: level " + level);
                }
            }
        } catch (Exception e) {
            System.err.println("[routing-service] discarding malformed congestion message: " + e.getMessage());
        }
    }

    /** Latest level seen on the topic, or {@link #NO_LEVEL_YET}. */
    public int latestLevel() {
        return latestLevel.get();
    }

    @Override
    public void close() {
        try {
            session.close();
            connection.close();
        } catch (JMSException ignored) {
            // best-effort shutdown
        }
    }
}
