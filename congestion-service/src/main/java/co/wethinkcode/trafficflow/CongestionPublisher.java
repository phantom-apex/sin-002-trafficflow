package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Publishes the city-wide congestion level to the shared "congestion-topic"
 * ActiveMQ topic whenever it changes, so consumers (routing-service) can react
 * without polling this service over REST.
 */
public class CongestionPublisher implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public CongestionPublisher() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            producer = session.createProducer(topic);
        } catch (JMSException e) {
            throw new IllegalStateException("could not connect to broker at " + MqConfig.BROKER_URL, e);
        }
    }

    /** Sends {"level": <n>} as a JSON text message. */
    public void publish(int level) {
        try {
            TextMessage message = session.createTextMessage("{\"level\": " + level + "}");
            producer.send(message);
            System.out.println("[congestion-service] published level " + level + " to " + MqConfig.TOPIC);
        } catch (JMSException e) {
            System.err.println("[congestion-service] failed to publish level " + level + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException ignored) {
            // best-effort shutdown
        }
    }
}
