package websocket.ExampleEchoServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Created by fahslaj on 4/16/2016.
 */
public class ServerRunner {
    public static void main(String[] args) {
        runEchoServer(10240);
    }

    private static boolean isRunning = false;

    public static void runEchoServer(int port) {
        if (isRunning)
            return;
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add websocket servlet
        ServletHolder wsHolder = new ServletHolder("echo", new EchoSocketServlet());
        context.addServlet(wsHolder, "/");

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRunning = true;
    }
}
