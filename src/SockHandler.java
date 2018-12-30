import com.sun.tools.internal.ws.wsdl.document.Output;
import jdk.internal.util.xml.impl.Input;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SockHandler extends Thread {
    private SocketChannel clientSocket = null;
    private int HEADER_SIZE = 8;
    private byte[] head;
    private Map<String,Object> headerValues;
    private SocketChannel serverSocket = null ;
    public SockHandler(SocketChannel socket) {
        super("SockHandler");
        this.clientSocket = socket; // TOOD set timeout
    }

    private Map<String,Object> headerParsing(SocketChannel sock) throws UnknownHostException {
        Map<String,Object> headerValues = new HashMap<String, Object>();

        int response = 0;
        byte[] headers;
        
        ByteBuffer buf = ByteBuffer.allocate(500);
        buf.clear();
        try {
           response = sock.read(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf.flip();
        headers = buf.array();
        headerValues.put("VERSION", (int) headers[0]);
        headerValues.put("COMMAND", headers[1]);
        headerValues.put("PORT", (headers[3] & 0xFF) | ((headers[2] & 0xFF)<< 8));
        headerValues.put("IP", InetAddress.getByAddress(new byte[]{headers[4], headers[5], headers[6], headers[7] }));
        headerValues.put("HEAD", headers);
        head = headers;

        return headerValues;
    }
    private void sendConnectedResponse(SocketChannel out) throws IOException {
        int portInt =  (int)this.headerValues.get("PORT");
        byte port = (byte) portInt;

        byte[] response = new byte[]{0, 90, 0, port, head[4], head[5], head[6], head[7]};

        out.write(ByteBuffer.wrap(response));
        return;
    }
    private void sendRejectedResponse(SocketChannel out) throws IOException {
       int portInt =  (int)this.headerValues.get("PORT");
       byte port = (byte) portInt;

        byte[] response = new byte[]{0, 91, 0, port , head[4], head[5], head[6], head[7]};

        out.write(ByteBuffer.wrap(response));
        return;
    }
    private void checkIfAuth(String request) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(request));
        String line; String host = ""; String path = "";

        while ((line = reader.readLine()) != null) {
            if(line.contains("HTTP")){
                String[] parts = line.split(" ");
                path = parts[1];
            }
            if(line.startsWith("Host")){
                String[] parts = line.split(": ");
                host = parts[1];
            }
            if(line.startsWith("Authorization")){
                String[] parts = line.split(": ");
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
        if(clientSocket != null){
            clientSocket.close();
        }
        if(serverSocket != null){
            serverSocket.close();
        }
        return;
    }
    private Boolean mixing(Selector selector, SocketChannel readFrom, SocketChannel writeTo, Boolean checkAuth){

        try {

            int readyChannels = selector.select();
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            readyKeys.clear(); // cleans the keys so they can reappear

            if(readyChannels != 2) return true;

            if(!selector.isOpen()) {
                return false;//graceful exit since selector was not in use anyway
            }
            ByteBuffer read = ByteBuffer.allocate(500);
            read.clear();
            int bytesRead = readFrom.read(read);
            if(bytesRead == -1) return false; //finished
            read.flip();
            if(checkAuth){
                String response = new String(read.array(), StandardCharsets.UTF_8);
                checkIfAuth(response);
            }
            writeTo.write(read);
            read.clear();
        } catch (ClosedSelectorException | IOException ex) {

            // selector was closed while being used
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    private void mixAndMatch(SocketChannel clientSocket, SocketChannel serverSocket) throws IOException {
        Selector s1 = Selector.open();
        Selector s2 = Selector.open();
        clientSocket.configureBlocking(false);
        serverSocket.configureBlocking(false);

        clientSocket.register(s1, SelectionKey.OP_READ);
        clientSocket.register(s2, SelectionKey.OP_WRITE);

        serverSocket.register(s2, SelectionKey.OP_READ);
        serverSocket.register(s1, SelectionKey.OP_WRITE);

        while (true){
           Boolean passed =  mixing(s1, clientSocket, serverSocket, true);
           Boolean passed2 = mixing(s2, serverSocket, clientSocket, false);
           if(!passed || !passed2){
               return;
           }
        }
    }

    public void run() {
         try {

            String inputLine, outputLine;
            this.headerValues = headerParsing(clientSocket);
            if((int) headerValues.get("VERSION") != 4)
            {
                System.err.println("Connection error: while parsing request: Unsupported SOCKS protocol version (got "+  headerValues.get("VERSION") +")");
                sendRejectedResponse(clientSocket);
                this.exit();
                return;
            }
//            System.out.println("Command: " + headerValues.get("COMMAND"));
            String ipAddress = ((InetAddress) headerValues.get("IP")).getHostAddress();
            this.serverSocket = SocketChannel.open();
            serverSocket.socket().setSoTimeout(120);
            serverSocket.connect(new InetSocketAddress(ipAddress, (int)headerValues.get("PORT")));
             SocketAddress clientAddress = clientSocket.socket().getLocalSocketAddress();
             SocketAddress serverAddress = serverSocket.socket().getRemoteSocketAddress();
             System.out.println("Succesfull connection from " + clientAddress.toString().replace("/", "") + " to " + serverAddress.toString().replace("/", "") );
            sendConnectedResponse(clientSocket);

            mixAndMatch(clientSocket,serverSocket);

            clientSocket.close();
            serverSocket.close();
            System.out.println("Closing connection from " + clientAddress.toString().replace("/", "") + " to " + serverAddress.toString().replace("/", "") );
        }
        catch (ConnectException e){
            System.err.println("Connection error: while connecting to server:" + e.getMessage());
            try {
                sendRejectedResponse(clientSocket);
                this.exit();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {

        }
    }
}
