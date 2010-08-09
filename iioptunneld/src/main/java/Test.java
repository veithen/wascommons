import java.util.Properties;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;

import com.ibm.CORBA.services.IIOPTunnelServlet;
import com.ibm.ws.orb.GlobalORBFactory;

public class Test {
    public static void main(String[] args) throws Exception {
        Properties orbProps = new Properties();
        orbProps.setProperty("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
        GlobalORBFactory.init(new String[0], orbProps);
        Server server = new Server();
        SocketConnector connector = new SocketConnector();
        connector.setPort(19000);
        server.addConnector(connector);
        Context context = new Context(server, "/");
        context.addServlet(IIOPTunnelServlet.class, "/");
        server.start();
    }
}
