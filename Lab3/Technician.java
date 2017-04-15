import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

/**
 * Created by Cinek on 2017-04-11.
 */

public class Technician {
    private BufferedReader br;
    private String name;
    private String firstSkill;
    private String secondSkill;
    private Channel channel1;
    private Channel channel2;
    private String queue1;
    private String queue2;
    private String myQueue;

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        try {
            obtainName();
            obtainSkills();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() throws IOException, TimeoutException {
        init();
        createChannels();
        createExchangesAndQueues();
        Thread listener1 = new TechnicianListener(queue1, channel1);
        Thread listener2 = new TechnicianListener(queue2, channel2);
        Thread listener3 = new TechnicianListener(myQueue, channel1);
        listener1.start();
        listener2.start();
        listener3.start();
    }

    private void createExchangesAndQueues() {
        try {
            channel1.exchangeDeclare(HospitalUtils.exchangeName, BuiltinExchangeType.TOPIC);
            channel2.exchangeDeclare(HospitalUtils.exchangeName, BuiltinExchangeType.TOPIC);
            channel1.exchangeDeclare(HospitalUtils.administratorExchangeName, BuiltinExchangeType.FANOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            queue1 = channel1.queueDeclare(firstSkill, false, false, false, null).getQueue();
            queue2 = channel2.queueDeclare(secondSkill, false, false, false, null).getQueue();
            myQueue = channel1.queueDeclare(name, false, false, false, null).getQueue();
            channel1.queueBind(queue1, HospitalUtils.exchangeName, HospitalUtils.examinationPrefix + HospitalUtils.topicSeparator + firstSkill);
            channel1.queueBind(myQueue, HospitalUtils.administratorExchangeName, "");
            channel2.queueBind(queue2, HospitalUtils.exchangeName, HospitalUtils.examinationPrefix + HospitalUtils.topicSeparator + secondSkill);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Exchanges and queues created.");
    }

    private void createChannels() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = null;
        try {
            connection = factory.newConnection();
            channel1 = connection.createChannel();
            channel2 = connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void obtainName() throws IOException {
        System.out.println("Technician started. Please insert his name.");
        name = br.readLine();
    }

    private void obtainSkills() throws IOException {
        System.out.println("Now, please insert " + name + "'s skills, separated by comma.");
        String skills = br.readLine();
        String[] array = skills.split(",");
        if (checkInsertedSkills(array)) {
            firstSkill = array[0].trim();
            secondSkill = array[1].trim();
        } else {
            System.out.println("You have inserted invalid skills. Try again.");
            obtainSkills();
            return;
        }
        System.out.println("Inserted skills correct.");
    }

    private boolean checkInsertedSkills(String[] array) {
        if (array.length != 2) {
            return false;
        }
        if (HospitalUtils.permittedSkills.contains(array[0].trim()) && HospitalUtils.permittedSkills.contains(array[1].trim())) {
            return true;
        }
        return false;
    }

    private class TechnicianListener extends Thread {
        String queueName;
        Channel channel;

        private TechnicianListener(String queueName, Channel channel) {
            this.channel = channel;
            this.queueName = queueName;
        }

        public void run() {
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message);
                    channel.basicAck(envelope.getDeliveryTag(), false);

                    if (!message.startsWith(HospitalUtils.administratorMessagePrefix)) {
                        String[] splitRequest = message.split(HospitalUtils.messageSeparator);
                        String doctorName = splitRequest[0];
                        String response = splitRequest[2] + HospitalUtils.messageSeparator + HospitalUtils.examinationResults;

                        String key = HospitalUtils.doctorsPrefix + HospitalUtils.topicSeparator + doctorName;
                        channel.basicPublish(HospitalUtils.exchangeName, key, null, response.getBytes("UTF-8"));
                    }
                }
            };
            try {
                channel.basicConsume(queueName, false, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        Technician technician = new Technician();
        technician.start();
    }
}
