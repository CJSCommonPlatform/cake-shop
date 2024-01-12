package uk.gov.justice.services.cakeshop.cakeshop.it.helpers;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class EventSender {

    private final JmsBootstrapper jmsBootstrapper = new JmsBootstrapper();

    public void sendToTopic(final JsonEnvelope jsonEnvelope, final String topicName) throws JMSException {
        try (final Session jmsSession = jmsBootstrapper.jmsSession()) {
            final Topic topic = jmsSession.createTopic(topicName);

            try (final MessageProducer producer = jmsSession.createProducer(topic);) {

                @SuppressWarnings("deprecation") final String json = jsonEnvelope.toDebugStringPrettyPrint();
                final TextMessage message = jmsSession.createTextMessage();

                message.setText(json);
                message.setStringProperty("CPPNAME", jsonEnvelope.metadata().name());

                producer.send(message);
            }
        }
    }
}
