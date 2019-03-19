package uk.gov.justice.services.example.cakeshop.it;

import org.junit.*;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeShopRepositoryManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.Querier;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.Catchup;
import uk.gov.justice.services.jmx.Shuttering;
import uk.gov.justice.services.jmx.ShutteringMBean;

import javax.management.*;
import javax.management.remote.*;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

//import com.sun.messaging.AdminConnectionFactory;
//import com.sun.messaging.jms.management.server.*;


//@RunWith(ApplicationComposer.class)
public class JmxMBeanIT {

//    @Module
//    @Classes(cdi = true, value = {
//            MBeanInstantiator.class,
//            DefaultShutteringMBean.class,
//            ShutteringMBean.class,
//            Shutterable.class,
//            ShutteringListener.class,
//            ApplicationStateController.class,
//            UtcClock.class,
//            MBeanRegistry.class
//    })
//
//    public WebApp war() {
//        return new WebApp()
//                .contextRoot("event-source-api")
//                .addServlet("JmxMBeanIT", Application.class.getName());
//    }

    private static final CakeShopRepositoryManager CAKE_SHOP_REPOSITORY_MANAGER = new CakeShopRepositoryManager();

    private Client client;
    private Querier querier;

    @BeforeClass
    public static void beforeClass() throws Exception {
        CAKE_SHOP_REPOSITORY_MANAGER.initialise();
    }

    @Before
    public void before() throws Exception {
        client = new RestEasyClientFactory().createResteasyClient();
        querier = new Querier(client);
    }

    @After
    public void cleanup() throws Exception {
        client.close();
    }

    @Ignore
    @Test
    public void shouldCreateShutterableMbean() throws Exception {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());
        final MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectName);

        assertNotNull(mbeanInfo);
        assertThat(mbeanInfo.getOperations()[0].getName(), is("doShutteringRequested"));
        assertThat(mbeanInfo.getOperations()[1].getName(), is("doUnshutteringRequested"));
    }

    @Ignore
    @Test
    public void shouldCreateCatchupMbean() throws Exception {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName("catchup", "type", Catchup.class.getSimpleName());
        final MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectName);

        assertNotNull(mbeanInfo);
        assertThat(mbeanInfo.getOperations()[0].getName(), is("doCatchupRequested"));
    }

    @Ignore
    @Test
    public void shouldRequestShuttering() throws Exception {

//        int port = 9999;

        int httpPort = Integer.valueOf(System.getProperty("random.http.port"));

        int managementPort = Integer.valueOf(System.getProperty("random.management.port"));

        System.out.println("httpPort: " + httpPort);

        System.out.println("managementPort: " + managementPort);

        LocateRegistry.createRegistry(managementPort);

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+managementPort+"/server");
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null, mbeanServer);
        if (cs == null) {
            throw new RuntimeException("Could not setUpClass() for test! JMXConnectorServerFactory.newJMXConnectorServer FAILED (Returned null...)! Url: " + jmxServiceURL + ", mbs: " + mbeanServer);
        }
        cs.start();
        System.out.println("Registry created / JMX connector started.");

        JMXConnector jmxc = JMXConnectorFactory.connect(jmxServiceURL, null);

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

        //Invoke on the WildFly MBean server
        int count = mbsc.getMBeanCount();
        System.out.println(count);
        MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectName);
        System.out.println(mbeanInfo.getClassName());


        echo("\nDomains:");
        String domains[] = mbeanServer.getDomains();
        Arrays.sort(domains);
        for (String domain : domains) {
            echo("\tDomain = " + domain);
        }

        try {
            final String status = (String) mbsc.invoke(objectName,
                    "doShutteringRequested",
                    null,  // no parameter
                    null );
            System.out.println("Status is: " + status);
        }
        catch (InstanceNotFoundException noMBeanInstanceFound)  // checked
        {
            System.err.println(  "ERROR: Could not find MBean with name "
                    + objectName.toString() + " in MBeanServer:\n"
                    + noMBeanInstanceFound.getMessage() );
        }
        catch (MBeanException mbeanEx)  // checked
        {
            System.err.println(  "ERROR: Exception encountered on invoked MBean:\n"
                    + mbeanEx.getMessage() );
        }
        catch (ReflectionException reflectionEx)  // checked
        {
            System.err.println(  "ERROR trying to reflectively invoke remote MBean:\n"
                    + reflectionEx.getMessage() );
        }
        catch (IOException ioEx)  // checked
        {
            System.err.println(  "ERROR trying to communicate with remote MBeanServer:\n"
                    + ioEx.getMessage() );
        }

        // Create a dedicated proxy for the MBean instead of
        // going directly through the MBean server connection
        //
        ShutteringMBean mbeanProxy =
                JMX.newMBeanProxy(mbsc, objectName, ShutteringMBean.class, true);

        //waitForEnterPressed();

        //mbeanProxy.doShutteringRequested();


        //Close JMX connector
        jmxc.close();
    }

    private void waitForEnterPressed() {
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void echo(String msg) {
        System.out.println(msg);
    }

    @Ignore
    @Test
    public void shouldConnectToWildFlyMbeanServer() throws Exception {
        //Get a connection to the WildFly MBean server on localhost
        //String host = "localhost";

        int port = Integer.valueOf(System.getProperty("random.http.port"));  // management-web port
        //int port = 9999;

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        System.out.println("port: "+port);

        LocateRegistry.createRegistry(port);

//        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+port+"/jmxrmi");

        JMXServiceURL serviceURL =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/server");

//        String urlString =
//                System.getProperty("jmx.service.url","service:jmx:remote+http://" + host + ":" + port);
//        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        //Invoke on the WildFly MBean server
        int count = connection.getMBeanCount();
            System.out.println(count);
            jmxConnector.close();
    }

//    private void executeOperation(MBeanServer mbeanServer) throws Exception {
//
//        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9999/server");
//        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null, mbeanServer);
//        if (cs == null) {
//            throw new RuntimeException("Could not setUpClass() for test! JMXConnectorServerFactory.newJMXConnectorServer FAILED (Returned null...)! Url: " + jmxServiceURL + ", mbs: " + mbeanServer);
//        }
//        cs.start();
//        System.out.println("Registry created / JMX connector started.");
//
//        JMXConnector jmxc = JMXConnectorFactory.connect(jmxServiceURL, null);
//
//        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
//
//        final ObjectName objectName = new ObjectName("shuttering", "type", ShutteringMBean.class.getSimpleName());
//
//        // Create a dedicated proxy for the MBean instead of
//        // going directly through the MBean server connection
//        //
//        ShutteringMBean mbeanProxy =
//                JMX.newMBeanProxy(mbsc, objectName, ShutteringMBean.class, true);
//
//        //mbeanProxy.doShutteringRequested();
//
//        //Close JMX connector
//        jmxc.close();
//    }

//    private void waitForEnterPressed() {
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    @Ignore
//    @Test
//    public void invokeOperation() {
//        try { //  Create administration connection factory
//            AdminConnectionFactory  acf = new AdminConnectionFactory();
//
//            //  Get JMX connector, supplying user name and password
//            JMXConnector jmxc = acf.createConnection(null, null);
//
//            //  Get MBean server connection
//            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
//
//            //  Create object name
//            ObjectName  serviceConfigName = MQObjectName.createServiceConfig("jms");
//
//            //  Invoke operation
//            mbsc.invoke(serviceConfigName, ServiceOperations.PAUSE, null, null);
//
//            //  Close JMX connector
//            jmxc.close();
//        }
//
//        catch (Exception  e)
//        { System.out.println( "Exception occurred: " + e.toString() );
//            e.printStackTrace();
//        }
//    }

//    private void invokeOperation() throws Exception {
//        // Create an RMI connector client and
//        // connect it to the RMI connector server
//        //
//        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi");
//        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
//
//        // Get an MBeanServerConnection
//        //
//        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
//    }

//    @Test
//    public void getPortNumber() {
//        Properties props = new Properties();
//        String version = "unknown";
//
//        try ( props.load(MyClassName.class.getClassLoader()
//                .getResourceAsStream("project.properties"))) {
//
//            version = props.getProperty("version");
//
//        } catch (IOException ex) {
//            // exception handling here
//        }
//    }

//    @Test
//    public void getPortNumber() {
//        ClassPathResource resource = new ClassPathResource( "mvn.properties" );
//        Properties p = new Properties();
//
//        final InputStream is = getClass().getClassLoader().getResourceAsStream("mvn.properties");
//
//        //InputStream inputStream = null;
//        try {
//            //inputStream = resource.getInputStream();
//            p.load( is );
//        } catch ( IOException e ) {
//        } finally {
//            Closeables.closeQuietly( is );
//        }
//
//        System.out.println(p.getProperty( "jmx.port" ));
//    }

    @Ignore
    @Test
    public void shouldConnectToWildFlyServer() throws Exception {
        //Get a connection to the WildFly MBean server on localhost
        String host = "localhost";
        int port = 9990;  // management-web port

        int managementPort = Integer.valueOf(System.getProperty("random.management.port"));

        String urlString =
                System.getProperty("jmx.service.url","service:jmx:remote+http://" + host + ":" + port);
//                System.getProperty("jmx.service.url","service:jmx:rmi:///jndi/rmi://localhost:"+managementPort+"/server");

        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        //Invoke on the WildFly MBean server
        int count = connection.getMBeanCount();
        System.out.println(count);
        jmxConnector.close();
    }

//    @Ignore
    @Test
    public void shouldConnectToJMXServer() throws Exception {
        //Get a connection to the WildFly MBean server on localhost
        String host = "localhost";
        //int port = 9990;  // management-web port

        int managementPort = Integer.valueOf(System.getProperty("random.management.port"));

        String urlString =
                System.getProperty("jmx.service.url","service:jmx:remote+http://" + host + ":" + managementPort);

//        String urlString = System.getProperty("jmx.service.url","service:jmx:remoting-jmx://" + host + ":" + managementPort);

        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        String username = "admin";
        String password = "admin";
        HashMap env = new HashMap();
        String[] creds = new String[2];
        creds[0] = username;
        creds[1] = password;
        env.put(JMXConnector.CREDENTIALS, creds);

        //JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        //Invoke on the WildFly MBean server
        int count = connection.getMBeanCount();
        System.out.println(count);

        System.out.println("Domains:");
        String domains[] = connection.getDomains();
        Arrays.sort(domains);
        for (String domain : domains) {
            System.out.println("Domain = " + domain);
        }

        final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

        final MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
        MBeanOperationInfo[] operations = mBeanInfo.getOperations();
        List<MBeanOperationInfo> mbeanOperations = Arrays.asList(operations);
        mbeanOperations.forEach(mBeanOperationInfo -> System.out.println(mBeanOperationInfo.getName()));

        // Create a dedicated proxy for the MBean instead of
        // going directly through the MBean server connection
        //
        ShutteringMBean mbeanProxy =
                JMX.newMBeanProxy(connection, objectName, ShutteringMBean.class, true);

//        waitForEnterPressed();

        mbeanProxy.doShutteringRequested();

        jmxConnector.close();
    }

    @Ignore
    @Test
    public void shouldConnectToJMXServerAndFireShutteringRequest() throws Exception {
        //Get a connection to the WildFly MBean server on localhost
        String host = "localhost";
        int port = 9990;  // management-web port

        int managementPort = Integer.valueOf(System.getProperty("random.management.port"));

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        String urlString =
                System.getProperty("jmx.service.url","service:jmx:remote+http://" + host + ":" + managementPort);

//        String urlString =
//                System.getProperty("jmx.service.url","service:jmx:rmi:///jndi/rmi://" + host + ":" + managementPort + "/jmxrmi");


        JMXServiceURL serviceURL = new JMXServiceURL(urlString);

        String username = "admin";
        String password = "admin";
        HashMap env = new HashMap();
        String[] creds = new String[2];
        creds[0] = username;
        creds[1] = password;
        env.put(JMXConnector.CREDENTIALS, creds);

        //JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        //Invoke on the WildFly MBean server
        int count = connection.getMBeanCount();
        System.out.println(count);

//        System.out.println("Domains:");
//        String domains[] = connection.getDomains();
//        Arrays.sort(domains);
//        for (String domain : domains) {
//            System.out.println("Domain = " + domain);
//        }

//        System.out.println("\n     Now we have a look at " + connection.getMBeanCount() + " mbeans!");
//        int objectCount = 0;
//        for (String domain : connection.getDomains()) {
//            System.out.println("\n***********************************************************************************");
//            System.out.println("DOMAIN: " + domain);
//
//            // query all the beans for this domain using a wildcard filter
//            for (ObjectName objectName : connection.queryNames(new ObjectName(domain + ":*"), null)) {
//                if (objectName.toString().contains("hutter")) {
//                    System.out.println("    objectName " + ++objectCount + ": " + objectName);
//                }
//                MBeanInfo info = connection.getMBeanInfo(objectName);
//                for (MBeanAttributeInfo attr : info.getAttributes()) {
//                    System.out.print("        attr: " + attr.getDescription());
//                    try {
//                        String val = connection.getAttribute(objectName, attr.getName()).toString();
//                        System.out.println(" -> " + abbreviate(val));
//                    } catch (Exception e) {
//                        System.out.println(" FAILED: " + e);
//                    }
//                }
//
//                for (MBeanOperationInfo op : info.getOperations()) {
//                    System.out.println("        op: " + op.getName());
//                }
//            }
//        }


        //ObjectName objectName = new ObjectName("uk.gov.justice.services.example.cakeshop.it:name=Shuttering");
        //ObjectName loaderName = new ObjectName("uk.gov.justice.services.example.cakeshop.it:name=Shuttering");

        ObjectName objectName = new ObjectName("uk.gov.justice.services.example.cakeshop.it", "type",Shuttering.class.getSimpleName());
        ObjectName loaderName = new ObjectName("shuttering", "type",Shuttering.class.getSimpleName());


        String domain = connection.getDefaultDomain();
        // Create a new MLet MBean and add it to the MBeanServer.
        String mletClass = "javax.management.loading.MLet";
        ObjectName mletName = new ObjectName(domain + ":name=" + mletClass);
        connection.createMBean(mletClass, mletName);

        String shutteringClass = "uk.gov.justice.services.jmx.Shuttering";
        ObjectName shutteringName = new ObjectName(shutteringClass + ":name=" + shutteringClass);
        connection.createMBean(shutteringClass, shutteringName, mletName);

        //connection.createMBean("uk.gov.justice.services.jmx", objectName, loaderName);


        //ObjectName objectName = new ObjectName(":type=Shuttering");
//        connection.createMBean(Shuttering.class.getSimpleName(), loaderName);

        //connection.createMBean(Shuttering.class.getName(), loaderName);


//
//        MBeanInfo mbeanInfo = connection.getMBeanInfo(objectName);
//        System.out.println(mbeanInfo.getClassName());
//
//        System.out.println("Domains:");
//        domains = connection.getDomains();
//        Arrays.sort(domains);
//        for (String domain : domains) {
//            System.out.println("Domain = " + domain);
//        }



//        try {
//            final String status = (String) connection.invoke(objectName,
//                    "doShutteringRequested",
//                    null,  // no parameter
//                    null );
//            System.out.println("Status is: " + status);
//        }
//        catch (InstanceNotFoundException noMBeanInstanceFound)  // checked
//        {
//            System.err.println(  "ERROR: Could not find MBean with name "
//                    + objectName.toString() + " in MBeanServer:\n"
//                    + noMBeanInstanceFound.getMessage() );
//        }
//        catch (MBeanException mbeanEx)  // checked
//        {
//            System.err.println(  "ERROR: Exception encountered on invoked MBean:\n"
//                    + mbeanEx.getMessage() );
//        }
//        catch (ReflectionException reflectionEx)  // checked
//        {
//            System.err.println(  "ERROR trying to reflectively invoke remote MBean:\n"
//                    + reflectionEx.getMessage() );
//        }
//        catch (IOException ioEx)  // checked
//        {
//            System.err.println(  "ERROR trying to communicate with remote MBeanServer:\n"
//                    + ioEx.getMessage() );
//        }

        // Create a dedicated proxy for the MBean instead of
        // going directly through the MBean server connection
        //
        ShutteringMBean mbeanProxy =
                JMX.newMBeanProxy(connection, objectName, ShutteringMBean.class, true);

        //waitForEnterPressed();

        mbeanProxy.doShutteringRequested();


        //Close JMX connector
        jmxConnector.close();
    }

    private String abbreviate(String text) {
        if (text != null && text.length() > 42) {
            return text.substring(0, 42) + "...";
        } else {
            return text;
        }
    }
}
