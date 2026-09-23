package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publishes a heartbeat JSON message to the "intersection-heartbeat-queue"
 * every {@link #INTERVAL_SECONDS} seconds. The watchdog watches this queue:
 * if the heartbeats stop, this service has died and routes can no longer be
 * validated.
 */
public class HeartbeatPublisher implements AutoCloseable {

    public static final long INTERVAL_SECONDS = 5;

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heartbeat-publisher");
        thread.setDaemon(true);
        return thread;
    });

    public HeartbeatPublisher() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            producer = session.createProducer(queue);
        } catch (JMSException e) {
            throw new IllegalStateException("could not connect to broker at " + MqConfig.BROKER_URL, e);
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        try {
            TextMessage message = session.createTextMessage(
                    "{\"service\": \"intersection-service\", \"sentAt\": " + System.currentTimeMillis() + "}");
            producer.send(message);
            System.out.println("[intersection-service] heartbeat sent to " + MqConfig.HEARTBEAT_QUEUE);
        } catch (JMSException e) {
            System.err.println("[intersection-service] heartbeat failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException ignored) {
            // best-effort shutdown
        }
    }
}
