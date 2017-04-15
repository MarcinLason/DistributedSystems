package pl.edu.agh.dsrg.sr.chat.protos;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Cinek on 2017-03-26.
 */
public class ManagementReceiver extends ReceiverAdapter {
    private Map<String, List<String>> channelsUsers;

    public ManagementReceiver(Map<String, List<String>> channelsUsers) {
        this.channelsUsers = channelsUsers;
    }

    public void viewAccepted(View newView) {
        System.out.println("Connected users:" + newView.getMembers());
    }

    public void receive(Message msg) {
        ChatOperationProtos.ChatAction action = null;
        try {
            action = ChatOperationProtos.ChatAction.parseFrom(msg.getBuffer());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        String channelName = action.getChannel();
        String nickname = action.getNickname();

        if (action.getAction().equals(ChatOperationProtos.ChatAction.ActionType.JOIN)) {
            if (!channelsUsers.containsKey(channelName)) {
                channelsUsers.put(channelName, new ArrayList<>());
            }
            channelsUsers.get(channelName).add(nickname);
        }
        if (action.getAction().equals(ChatOperationProtos.ChatAction.ActionType.LEAVE)) {
            channelsUsers.get(channelName).remove(nickname);
            if (channelsUsers.get(channelName).isEmpty()) {
                channelsUsers.remove(channelName);
            }
        }
        System.out.println(msg.getSrc() + ": " + action.getAction() + " " + channelName);
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (channelsUsers) {
            ChatOperationProtos.ChatState.Builder builder = ChatOperationProtos.ChatState.newBuilder();

            for (Map.Entry<String, List<String>> entry : channelsUsers.entrySet()) {
                for (String user : entry.getValue()) {
                    builder.addStateBuilder()
                            .setAction(ChatOperationProtos.ChatAction.ActionType.JOIN)
                            .setChannel(entry.getKey())
                            .setNickname(user);
                }
            }
            ChatOperationProtos.ChatState state = builder.build();
            state.writeTo(output);
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (channelsUsers) {
            ChatOperationProtos.ChatState state = ChatOperationProtos.ChatState.parseFrom(input);
            channelsUsers.clear();

            for (ChatOperationProtos.ChatAction action : state.getStateList()) {
                if (!channelsUsers.containsKey(action.getChannel())) {
                    channelsUsers.put(action.getChannel(), new ArrayList<String>());
                }
                channelsUsers.get(action.getChannel()).add(action.getNickname());
            }
        }
    }
}
