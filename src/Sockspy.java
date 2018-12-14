import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sockspy {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java Sockspy <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        boolean listening = true;
        // TODO limit to 20 connections.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try (ServerSocket welcomeSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket clientSocket = welcomeSocket.accept();
                Runnable worker = new SockHandler(clientSocket);
                worker.run();
//                executor.execute(worker);
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }
}
