import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

public class DodgyClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(DodgyClass.class);

    public String dodgyString = "dodgy";

    private void dodgyMethod() {
        System.out.println("Hello");
        LOGGER.info("Hello: " + dodgyString);
    }
}
