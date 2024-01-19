package uk.gov.justice.services.cakeshop.cakeshop.it.helpers;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static uk.gov.justice.services.cakeshop.cakeshop.it.helpers.SystemPropertyFinder.findJmsPort;
import static uk.gov.justice.services.cakeshop.cakeshop.it.helpers.SystemPropertyFinder.findJmsUserName;
import static uk.gov.justice.services.cakeshop.cakeshop.it.helpers.SystemPropertyFinder.findJmsUserPassword;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class JmsBootstrapper {

    private static final String JMS_USERNAME = findJmsUserName();
    private static final String JMS_PASSWORD = findJmsUserPassword();
    private static final String JMS_PORT = findJmsPort();
    private static final String JMS_BROKER_URL = "tcp://localhost:" + JMS_PORT;

    private final ActiveMQConnectionFactory jmsConnectionFactory = new ActiveMQConnectionFactory(JMS_BROKER_URL);


    public Session jmsSession() throws JMSException {
        final Connection connection = jmsConnectionFactory.createConnection(JMS_USERNAME, JMS_PASSWORD);
        connection.start();
        return connection.createSession(false, AUTO_ACKNOWLEDGE);
    }

    public MessageConsumer topicConsumerOf(final String topicName, final Session session) throws JMSException {
        final Topic topic = session.createTopic(topicName);
        return session.createConsumer(topic);
    }

    public void clearDeadLetterQueue() throws Exception {
        try (final Session jmsSession = jmsSession();) {
            final MessageConsumer dlqConsumer = queueConsumerOf("DLQ", jmsSession);
            clear(dlqConsumer);
        }
    }

    public MessageConsumer queueConsumerOf(final String queueName, final Session session) throws JMSException {
        final Queue queue = session.createQueue(queueName);
        return session.createConsumer(queue);
    }

    public QueueBrowser queueBrowserOf(final String queueName, final Session session) throws JMSException {
        final Queue queue = session.createQueue(queueName);
        return session.createBrowser(queue);
    }

    private void clear(MessageConsumer msgConsumer) throws JMSException {
        while (msgConsumer.receiveNoWait() != null) {
        }
    }
}
