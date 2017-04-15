import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.net.InetAddress.getByName;

public class ChatClient {

    private final String mockedMultimediaMessage = "Mocked Multimedia Message";
    private final String mockedMultimediaMulticastMessage = "Mocked Multimedia Message using Multicast";
    private final Set<String> commandsShortcuts;
    private final int MULTICAST_PORT = 12343;
    private final int PORT = 9001;

    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleReader;
    private String myName;
    private Socket socket;
    private DatagramSocket datagramSocket;
    private MulticastSocket multicastSocket;
    private InetAddress IPAddress;

    public ChatClient() {
        initSockets();
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
        commandsShortcuts = new HashSet(Arrays.asList("M", "N"));
    }

    private void initSockets() {
        try {
            IPAddress = getByName("224.0.0.1");
            socket = new Socket("localhost", PORT);
            datagramSocket = new DatagramSocket();
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(IPAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getName() {
        try {
            myName = consoleReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            String message;
            if (line.startsWith("GETNAME")) {
                System.out.println("Please, insert your name.");
                getName();
                out.println(myName);
            } else if (line.startsWith("NAMEACK")) {
                System.out.println("You have just joined the chat!");
                Thread sender = new SenderThread();
                sender.start();
                MulticastPrinterThread udpPrinter = new MulticastPrinterThread();
                udpPrinter.start();
            } else if (line.startsWith("MESSAGE")) {
                message = line.substring(8);
                if (!message.startsWith(myName)) {
                    System.out.println(message);
                }
            }
        }
    }

    private class SenderThread extends Thread {
        public void run() {
            while (true) {
                String message = "";
                try {
                    message = consoleReader.readLine();
                    if (commandsShortcuts.contains(message)) {
                        if (message.equals("M")) {
                            InetAddress address = getByName("localhost");
                            byte[] dataToSend = mockedMultimediaMessage.getBytes();
                            DatagramPacket packetToSend = new DatagramPacket(dataToSend, dataToSend.length, address, PORT);
                            datagramSocket.send(packetToSend);
                        }

                        if (message.equals("N")) {
                            byte[] dataToSend = mockedMultimediaMulticastMessage.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, IPAddress, MULTICAST_PORT);
                            multicastSocket.leaveGroup(IPAddress);
                            multicastSocket.send(sendPacket);
                            multicastSocket.joinGroup(IPAddress);
                        }
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                 }
                 if (!message.isEmpty() && !commandsShortcuts.contains(message)) {
                    out.println(message);
                 }
            }
        }
    }

    private class MulticastPrinterThread extends Thread {
        public void run() {
            try {
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[128], 128);
                    multicastSocket.receive(datagramPacket);
                    System.out.println("Received new message via UDP connection: " + new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.run();
    }
}