package uk.gov.justice.services.example.cakeshop.it;

import uk.gov.justice.services.example.cakeshop.it.helpers.JMXBeanHelper;
import uk.gov.justice.services.jmx.Shuttering;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class JmxMBeanIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxMBeanIT.class.getName());

    private JMXBeanHelper jmxBeanHelper;

    @Before
    public void before() throws Exception {
        jmxBeanHelper = new JMXBeanHelper();
    }

    @Test
    public void shouldInvokeShuttering() throws Exception {

        JMXConnector jmxConnector = jmxBeanHelper.getJMXConnector();
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

        jmxBeanHelper.getMbeanDomains(connection);

        jmxBeanHelper.getMbeanOperations(objectName, connection);

        jmxBeanHelper.getMbeanProxy(objectName, connection).doShutteringRequested();

        //Close JMX connector
        jmxConnector.close();
    }
}
