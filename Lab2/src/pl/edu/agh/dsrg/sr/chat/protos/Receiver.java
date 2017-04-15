package pl.edu.agh.dsrg.sr.chat.protos;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

/**
 * Created by Cinek on 2017-03-28.
 */
public class Receiver extends ReceiverAdapter {
    private JChannel channel;

    public Receiver(JChannel channel){
        this.channel = channel;
    }

    public void receive(Message msg) {
        ChatOperationProtos.ChatMessage chatMessage = null;
        try {
            chatMessage = ChatOperationProtos.ChatMessage.parseFrom(msg.getBuffer());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        System.out.println(channel.getClusterName() + " " + msg.getSrc() + " " + chatMessage.getMessage());
    }
}
