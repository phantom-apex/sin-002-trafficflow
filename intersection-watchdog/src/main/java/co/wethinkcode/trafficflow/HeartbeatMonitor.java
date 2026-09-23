package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumes heartbeats from the "intersection-heartbeat-queue" and tracks how
 * recently Intersection Service was last heard from. A heartbeat older than
 * the alert threshold (or none ever received) means Intersection Service is
 * down — routes can no longer be validated.
 */
public class HeartbeatMonitor implements AutoCloseable, MessageListener {

    private final AtomicLong lastHeartbeatAt = new AtomicLong(0);
    private final Connection connection;
    private final Session session;

    public HeartbeatMonitor() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            throw new IllegalStateException("could not watch " + MqConfig.HEARTBEAT_QUEUE
                    + " at " + MqConfig.BROKER_URL, e);
        }
    }

    @Override
    public void onMessage(Message message) {
        lastHeartbeatAt.set(System.currentTimeMillis());
        System.out.println("[intersection-watchdog] heartbeat received from intersection-service");
    }

    /**
     * Seconds since the last heartbeat; -1 when no heartbeat has ever been
     * received (e.g. Intersection Service was already dead at startup).
     */
    public long secondsSinceLastHeartbeat() {
        long last = lastHeartbeatAt.get();
        return last == 0 ? -1 : (System.currentTimeMillis() - last) / 1000;
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
