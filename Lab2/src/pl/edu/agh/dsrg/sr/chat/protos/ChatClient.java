package pl.edu.agh.dsrg.sr.chat.protos;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Cinek on 2017-03-25.
 */
public class ChatClient extends ReceiverAdapter {
    private Map<String, JChannel> channelsMap;
    private Map<String, List<String>> channelsUsers;
    private BufferedReader consoleReader;
    private String nickname;
    private JChannel managementChannel;

    private void setNickname() {
        System.out.println("Insert your nickname.");
        try {
            this.nickname = consoleReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        System.setProperty("java.net.preferIPv4Stack", new String("true"));
        channelsMap = new HashMap<>();
        channelsUsers = new HashMap<>();
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
        setNickname();
        try {
            ProtocolStack protocolStack = new ProtocolStack();
            managementChannel = new JChannel(false);
            managementChannel.setName(nickname);
            managementChannel.setProtocolStack(protocolStack);
            initProtocolStack(protocolStack, null);

            managementChannel.setReceiver(new ManagementReceiver(channelsUsers));
            managementChannel.connect("ChatManagement321321");
            managementChannel.getState(null, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initProtocolStack(ProtocolStack protocolStack, String address) {
        Protocol udp = new UDP();
        if (address != null) {
            try {
                udp = new UDP().setValue("mcast_group_addr", InetAddress.getByName(address));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        try {
            protocolStack.addProtocol(udp)
                    .addProtocol(new PING())
                    .addProtocol(new MERGE3())
                    .addProtocol(new FD_SOCK())
                    .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                    .addProtocol(new VERIFY_SUSPECT())
                    .addProtocol(new BARRIER())
                    .addProtocol(new NAKACK2())
                    .addProtocol(new UNICAST3())
                    .addProtocol(new STABLE())
                    .addProtocol(new GMS())
                    .addProtocol(new UFC())
                    .addProtocol(new MFC())
                    .addProtocol(new FRAG2())
                    .addProtocol(new STATE_TRANSFER())
                    .addProtocol(new FLUSH())
                    .init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinChannel(String address) {
        if (channelsMap.containsKey(address)) {
            System.out.println("You have joined this channel before.");
            return;
        }

        JChannel channel = new JChannel(false);
        channel.setName(nickname);
        ProtocolStack protocolStack = new ProtocolStack();
        channel.setProtocolStack(protocolStack);
        initProtocolStack(protocolStack, address);
        channel.setReceiver(new Receiver(channel));
        try {
            channel.connect(address, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        channelsMap.put(address, channel);
        if (!channelsUsers.containsKey(address)) {
            channelsUsers.put(address, new ArrayList<>());
        }
//        channelsUsers.get(address).add(nickname);
        sendAction(ChatOperationProtos.ChatAction.ActionType.JOIN, address);
    }

    private void leaveChannel(String address) {
        JChannel channel = channelsMap.get(address);
        channelsMap.remove(address);

        sendAction(ChatOperationProtos.ChatAction.ActionType.LEAVE, address);
        channel.close();
    }


    private void sendAction(ChatOperationProtos.ChatAction.ActionType actionType, String channelName) {
        ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder()
                .setAction(actionType)
                .setNickname(nickname)
                .setChannel(channelName)
                .build();

        Message msg = new Message(null, null, chatAction.toByteArray());
        try {
            managementChannel.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message, String address) {
        ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage
                .newBuilder()
                .setMessage(message)
                .build();
        Message msg = new Message(null, null, chatMessage.toByteArray());
        try {
            channelsMap.get(address).send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void eventLoop() {
        while (true) {
            try {
                System.out.print("> ");
                System.out.flush();
                String line = consoleReader.readLine();

                if (line.startsWith("-help")) {
                    System.out.println("1. Joining the channel: J$ <channel name>");
                    System.out.println("2. Leaving the channel: E$ <channel name");
                    System.out.println("3. Sending message: T$ <channel name># <message>");
                    System.out.println("4. Listing users: L$");
                    System.out.println("5. Quit the chat: Q$");
                }
                if (line.startsWith("J$")) {
                    String address = line.substring(3).trim();
                    if (!InetAddress.getByName(address).isMulticastAddress()) {
                        System.out.println("Inserted address is not multicast address!");
                    } else {
                        joinChannel(address);
                    }
                    continue;
                }
                if (line.startsWith("E$")) {
                    String address = line.substring(3).trim();
                    if (!channelsMap.containsKey(address)) {
                        System.out.println("You are not a member of that channel!");
                    } else {
                        leaveChannel(address);
                    }
                    continue;
                }
                if (line.startsWith("T$")) {
                    line = line.substring(3).trim();
                    int hashIndex = line.indexOf("#");
                    String address = line.substring(0, hashIndex);

                    line = line.substring(hashIndex + 1).trim();
                    sendMessage(line, address);
                }
                if (line.startsWith("L$")) {
                    if (channelsUsers.isEmpty()) {
                        System.out.println("There are no users in the chat");
                        continue;
                    }
                    for (Map.Entry<String, List<String>> entry : channelsUsers.entrySet()) {
                        System.out.print("Channel name: " + entry.getKey() + ", users: ");
                        for (String user : entry.getValue()) {
                            System.out.print(user + ", ");
                        }
                        System.out.println("");
                    }
                }
                if (line.startsWith("Q$"))
                    break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void start() {
        init();
        eventLoop();
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}