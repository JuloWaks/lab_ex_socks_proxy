import java.net.*;
import java.io.*;

public class SockHandler extends Thread {
    private Socket clientSocket = null;

    public SockHandler(Socket socket) {
        super("SockHandler");
        this.clientSocket = socket; // TOOD set timeout
    }


    public void run() {
        System.out.println("On threaaad");

        try (
                PrintWriter outClient = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader inClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String inputLine, outputLine;
            inputLine = inClient.readLine();
            Byte first = inClient.read()
            System.out.println(inputLine);
            while (() != null) {
//                outputLine = kkp.processInput(inputLine);
                outClient.println(inputLine + "WASAAAAAA");
//                if (outputLine.equals("Bye"))
//                    break;
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
