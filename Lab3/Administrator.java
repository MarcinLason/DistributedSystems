import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

/**
 * Created by Cinek on 2017-04-11.
 */
public class Administrator {
    private BufferedReader br;
    private Channel channel;
    private String myQueue;
    private String myName;

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        myName = "Administrator";
    }

    private void start() {
        init();
        createChannel();
        createExchangeAndQueue();
        Thread listener = new AdminListener(myQueue, channel);
        listener.start();
        sendStatements();
    }

    private void sendStatements() {
        while (true){
            String message = null;
            try {
                System.out.println("Insert statement:");
                message = HospitalUtils.administratorMessagePrefix + br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                channel.basicPublish(HospitalUtils.administratorExchangeName, "", null, message.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createExchangeAndQueue() {
        try {
            channel.exchangeDeclare(HospitalUtils.exchangeName, BuiltinExchangeType.TOPIC);
            channel.exchangeDeclare(HospitalUtils.administratorExchangeName, BuiltinExchangeType.FANOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            myQueue = channel.queueDeclare(myName, false, false, false, null).getQueue();
            channel.queueBind(myQueue, HospitalUtils.exchangeName, "hospital.#");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createChannel() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private class AdminListener extends Thread {
        private String queueName;
        private Channel channel;

        private AdminListener(String queueName, Channel channel) {
            this.channel = channel;
            this.queueName = queueName;
        }

        @Override
        public void run() {
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Logged message: " + message);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };
            try {
                channel.basicConsume(queueName, false, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Administrator administrator = new Administrator();
        administrator.start();
    }
}
