package uk.gov.justice.services.example.cakeshop.it;

import uk.gov.justice.services.example.cakeshop.it.helpers.MBeanHelper;
import uk.gov.justice.services.jmx.ShutteringMBean;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmxMBeanIT {

    private static final String SHUTTERING_DOMAIN = "shuttering";
    private static final String SHUTTERING = "Shuttering";

    private MBeanHelper mBeanHelper;
    private JMXConnector jmxConnector;
    private ShutteringMBean shutteringMBean;

    @Before
    public void before() throws IOException {
        mBeanHelper = new MBeanHelper();
        jmxConnector = mBeanHelper.getJMXConnector();
    }

    @After
    public void cleanup() {
        //invoke unshuttering - Always ensure unshutter is invoked as we cannot guarantee order of execution for other Cakeshop IT's
        shutteringMBean.doUnshutteringRequested();
    }

    @Test
    public void shouldInvokeShuttering() throws Exception {
        shutteringMBean = getMbeanProxy(SHUTTERING_DOMAIN, ShutteringMBean.class, SHUTTERING);
        shutteringMBean.doShutteringRequested();
    }

    @Test
    public void shouldInvokeUnShuttering() throws Exception {
        shutteringMBean = getMbeanProxy(SHUTTERING_DOMAIN, ShutteringMBean.class, SHUTTERING);
        shutteringMBean.doUnshutteringRequested();
    }

    private <T> T getMbeanProxy(final String domain, final Class<T> mBeanInterface, final String mBeanClassName) throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        final ObjectName objectName = new ObjectName(domain, "type", mBeanClassName);

        mBeanHelper.getMbeanDomains(connection);

        mBeanHelper.getMbeanOperations(objectName, connection);

        return mBeanHelper.getMbeanProxy(connection, objectName, mBeanInterface);
    }
}
