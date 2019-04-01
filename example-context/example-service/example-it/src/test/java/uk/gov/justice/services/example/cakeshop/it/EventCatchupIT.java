package uk.gov.justice.services.example.cakeshop.it;

import uk.gov.justice.services.example.cakeshop.it.helpers.JMXBeanHelper;
import uk.gov.justice.services.jmx.Catchup;
import uk.gov.justice.services.jmx.CatchupMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class EventCatchupIT {

    private JMXBeanHelper jmxBeanHelper;

    @Before
    public void before() throws Exception {
        jmxBeanHelper = new JMXBeanHelper();
    }

    @Test
    public void shouldRunCatchup() throws Exception {

        try(final JMXConnector jmxConnector = jmxBeanHelper.getJMXConnector()) {
            final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

            final ObjectName objectName = new ObjectName("catchup", "type", Catchup.class.getSimpleName());

            jmxBeanHelper.getMbeanDomains(connection);

            jmxBeanHelper.getMbeanOperations(objectName, connection);

            final CatchupMBean catchupMBean = JMX.newMBeanProxy(connection, objectName, CatchupMBean.class, true);

            catchupMBean.doCatchupRequested();
        }
    }
}
