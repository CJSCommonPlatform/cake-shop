package uk.gov.justice.services.example.cakeshop.it.helpers;

import static java.lang.String.format;
import static javax.management.JMX.newMBeanProxy;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.Catchup;
import uk.gov.justice.services.jmx.CatchupMBean;
import uk.gov.justice.services.jmx.MBeanException;
import uk.gov.justice.services.jmx.ObjectNameFactory;
import uk.gov.justice.services.jmx.Shuttering;
import uk.gov.justice.services.jmx.ShutteringMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxMBeanFactory {

    private static final String HOST = getHost();
    private static final String PORT = System.getProperty("random.management.port");

    private static final String DEFAULT_URL = "service:jmx:remote+http://" + HOST + ":" + PORT;

    private final ObjectNameFactory objectNameFactory = new ObjectNameFactory();

    public JMXConnector createJMXConnector()  {

        final String url = System.getProperty("jmx.service.url", DEFAULT_URL);
        try {
            final JMXServiceURL serviceURL = new JMXServiceURL(url);

            return JMXConnectorFactory.connect(serviceURL, null);
        } catch (final IOException e) {
            throw new MBeanException(format("Failed to connect to JMX. URL: '%s'", url), e);
        }
    }

    public CatchupMBean getCatchupMBean(final JMXConnector jmxConnector) {

        final ObjectName objectName = objectNameFactory.create("catchup", "type", Catchup.class.getSimpleName());

        final MBeanServerConnection connection = getMBeanServerConnection(jmxConnector);
        return newMBeanProxy(connection, objectName, CatchupMBean.class, true);
    }

    public ShutteringMBean getShutteringMBean(final JMXConnector jmxConnector) {

        final ObjectName objectName = objectNameFactory.create("shuttering", "type", Shuttering.class.getSimpleName());

        final MBeanServerConnection connection = getMBeanServerConnection(jmxConnector);
        return newMBeanProxy(connection, objectName, ShutteringMBean.class, true);
    }


    private MBeanServerConnection getMBeanServerConnection(final JMXConnector jmxConnector)  {
        try {
            return jmxConnector.getMBeanServerConnection();
        } catch (final IOException e) {
            throw new MBeanException("Failed to connect to MBean server", e);
        }
    }
}
