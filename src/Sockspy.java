
import java.net.*;
import java.io.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sockspy {
    public static void main(String[] args) throws IOException {


        int portNumber = 8080;
        boolean listening = true;
        // TODO limit to 20 connections.
        ExecutorService executor = Executors.newFixedThreadPool(20);

        ServerSocketChannel welcomeSocket = ServerSocketChannel.open();
        // TODO add timeout for welcome socket
        try{
            welcomeSocket.socket().bind(new InetSocketAddress(portNumber));

            while (listening) {
                SocketChannel clientSocket = welcomeSocket.accept();
                Runnable worker = new SockHandler(clientSocket);
                executor.execute(worker);
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }
}
