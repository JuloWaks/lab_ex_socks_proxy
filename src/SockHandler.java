import com.sun.tools.internal.ws.wsdl.document.Output;
import jdk.internal.util.xml.impl.Input;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class SockHandler extends Thread {
    private Socket clientSocket = null;
    private int HEADER_SIZE = 8;
    private byte[] head;
    private Socket serverSocket ;
    public SockHandler(Socket socket) {
        super("SockHandler");
        this.clientSocket = socket; // TOOD set timeout
    }

    private Map<String,Object> headerParsing(InputStream in) throws UnknownHostException {
        Map<String,Object> headerValues = new HashMap<String, Object>();

        int response = 0;
        byte[] headers = new byte[500];
        try {
           response = in.read(headers);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if(response != HEADER_SIZE){
//            throw new Error("Could not read the whole header datagram");
//        }
        headerValues.put("VERSION", (int) headers[0]);
        headerValues.put("COMMAND", headers[1]);
        headerValues.put("PORT", (headers[3] & 0xFF) | ((headers[2] & 0xFF)<< 8));
        headerValues.put("IP", InetAddress.getByAddress(new byte[]{headers[4], headers[5], headers[6], headers[7] }));
        headerValues.put("HEAD", headers);
        head = headers;

        return headerValues;
    }
    private void sendResponse(OutputStream out) throws IOException {
        byte[] response = new byte[]{0, 90, 0, 80, head[4], head[5], head[6], head[7]};

        out.write(response);
        out.flush();
        return;
    }

    private void checkIfAuth(String request) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(request));

        String line;
        String host = "";
        String path = "";
        while ((line = reader.readLine()) != null) {

            System.out.println(line);
            if(line.contains("HTTP")){
                String[] parts = line.split(" ");
                path = parts[1];
            }
            if(line.startsWith("Host")){
                String[] parts = line.split(": ");
                host = parts[1];
            }
            if(line.startsWith("Authorization")){
                System.out.println(line);
                String[] parts = line.split(": ");
                System.out.println(parts);
                if(parts[1].startsWith("Basic")){
                    String[] basic = parts[1].split(" ");
                    byte[] decodedBytes = Base64.getDecoder().decode(basic[1]);
                    String decodedString = new String(decodedBytes);
                    System.out.println("Password Found! http://"+decodedString+"@"+host+path);
                }
            }
        }

    }
    private void exit() throws IOException {
        // TODO maybe consider doing a SOCKS closing
        clientSocket.close();
        return;
    }
    private void mixAndMatch(InputStream inClient, OutputStream outClient, InputStream inServer, OutputStream outServer) throws IOException {
        byte[] bufferRead = new byte[500];
        inClient.read(bufferRead);
        String request = new String(bufferRead, StandardCharsets.UTF_8);
        checkIfAuth(request);
        outServer.write(bufferRead);
        outServer.flush();
        byte[] bufferWrite = new byte[1500];
        inServer.read(bufferWrite);
        String response = new String(bufferWrite, StandardCharsets.UTF_8);
        checkIfAuth(response);
        outClient.write(bufferWrite);
        outClient.flush();
        return;
    }

    public void run() {
        System.out.println("On threaaad");

        try (
                PrintWriter outClient = new PrintWriter(clientSocket.getOutputStream(), true);
                OutputStream outOut = clientSocket.getOutputStream();
                InputStream inClient = clientSocket.getInputStream();
        ) {
            String inputLine, outputLine;
            Map<String,Object> headers = headerParsing(inClient);
            if((int) headers.get("VERSION") != 4)
            {
                System.err.println("Version is "+ headers.get("VERSION") + " which is not compatible");
                this.exit();
                return;
            }
            System.out.println("Command: " + headers.get("COMMAND"));
            String ipAddress = ((InetAddress) headers.get("IP")).getHostAddress();
            serverSocket = new Socket(ipAddress, (int)headers.get("PORT") );
            serverSocket.setSoTimeout(120 );
            int i = 0;
            sendResponse(outOut);
            System.out.println("After sending ack, will start to get request...");

            while (i<100){
                mixAndMatch(inClient, outOut, serverSocket.getInputStream(), serverSocket.getOutputStream());
                i++;
            }
            outClient.println("menea para mi");
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
