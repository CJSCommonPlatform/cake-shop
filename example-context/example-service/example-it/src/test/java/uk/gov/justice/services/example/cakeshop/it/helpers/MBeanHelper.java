package uk.gov.justice.services.example.cakeshop.it.helpers;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static javax.management.JMX.newMBeanProxy;
import static javax.management.remote.JMXConnectorFactory.connect;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import java.io.IOException;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;

public class MBeanHelper {

    private static final Logger logger = getLogger(MBeanHelper.class.getName());

    private static final String HOST = getHost();
    private static final String RANDOM_MANAGEMENT_PORT = "random.management.port";

    public <T> T getMbeanProxy(final MBeanServerConnection connection, final ObjectName objectName, final Class<T> mBeanInterface) {
        return newMBeanProxy(connection, objectName, mBeanInterface, true);
    }

    public void getMbeanDomains(final MBeanServerConnection connection) throws IOException {
        final String [] domains = connection.getDomains();
        final List<String> mbeanDomains = asList(domains);

        logger.info("MBean Domains: ");
        mbeanDomains.forEach(mbeanDomain -> logger.info(mbeanDomain));
    }

    public void getMbeanOperations(final ObjectName objectName, final MBeanServerConnection connection) throws IOException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        final MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
        final MBeanOperationInfo[] operations = mBeanInfo.getOperations();
        final List<MBeanOperationInfo> mbeanOperations = asList(operations);

        logger.info("MBean Operations: ");
        mbeanOperations.forEach(mBeanOperationInfo -> logger.info(mBeanOperationInfo.getName()));
    }

    public JMXConnector getJMXConnector() throws IOException {
        final int managementPort = valueOf(getProperty(RANDOM_MANAGEMENT_PORT));

        final String urlString =
                getProperty("jmx.service.url","service:jmx:remote+http://" + HOST + ":" + managementPort);
        final JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        return connect(serviceURL, null);
    }
}
