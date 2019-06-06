package uk.gov.justice.services.example.cakeshop.it.helpers;

import uk.gov.justice.services.jmx.command.SystemCommanderMBean;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

public class SystemCommandMBeanClient {

    private static final String SYSTEM_COMMANDER_DOMAIN = "systemCommander";
    private static final String SYSTEM_COMMANDER = "SystemCommander";

    private MBeanHelper mBeanHelper = new MBeanHelper();
    private JMXConnector jmxConnector = mBeanHelper.getJMXConnector();


    public SystemCommanderMBean getMbeanProxy() {
        final MBeanServerConnection connection;
        try {
            connection = jmxConnector.getMBeanServerConnection();
            final ObjectName objectName = new ObjectName(SYSTEM_COMMANDER_DOMAIN, "type", SYSTEM_COMMANDER);

            mBeanHelper.getMbeanDomains(connection);

            mBeanHelper.getMbeanOperations(objectName, connection);

            return mBeanHelper.getMbeanProxy(connection, objectName, SystemCommanderMBean.class);
        } catch (final IOException | MalformedObjectNameException | IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            throw new MBeanClientException("Failed to get MBean proxy", e);
        }
    }

    public void close() {
        try {
            jmxConnector.close();
        } catch (IOException e) {
            throw new MBeanClientException("Failed to close JmxConnector", e);
        }
    }
}
