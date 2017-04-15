import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashSet;


public class ChatServer {

    private static final int PORT = 9001;
    private static HashSet<String> clientNames = new HashSet<>();
    private static HashSet<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        Thread udp = new Thread(new UDPHandler());
        udp.start();
        try {
            while (true) {
                new TCPClientHandler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class TCPClientHandler extends Thread {
        private String clientName;
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;


        public TCPClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                while (true) {
                    out.println("GETNAME");
                    clientName = in.readLine();
                    if (clientName == null || clientName.equals("")) {
                        continue;
                    }
                    synchronized (clientNames) {
                        if (!clientNames.contains(clientName)) {
                            clientNames.add(clientName);
                            System.out.println("User " + clientName + " joined the chat");
                            break;
                        }
                    }
                    out.println("MESSAGE " + "Inserted name is currently in use.");
                }
                out.println("NAMEACK");
                writers.add(out);

                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        continue;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + clientName + ": " + input);
                        writer.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (clientName != null) {
                    clientNames.remove(clientName);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class UDPHandler extends Thread {
        private DatagramSocket datagramSocket;
        public void run() {
            try {
                datagramSocket = new DatagramSocket(PORT);
                byte[] buffer = new byte[128];

                while (true) {
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(receivedPacket);
                    String message = new String(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
                    System.out.println("Received new message via UDP connection: " + message);
                }
            } catch (IOException e) {
                datagramSocket.close();
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }
        }
    }
}