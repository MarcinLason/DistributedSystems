import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

/**
 * Created by Cinek on 2017-04-11.
 */
public class Doctor {
    private BufferedReader br;
    private Channel channel;
    private String myName;
    private String myQueue;

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        try {
            obtainName();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void obtainName() throws IOException {
        System.out.println("Doctor started. Please insert his name.");
        myName = br.readLine();
    }


    private void start() {
        init();
        createChannel();
        createExchangeAndQueue();
        Thread publisher = new DoctorsPublisher(br);
        Thread listener = new DoctorsListener(myQueue, channel);
        publisher.start();
        listener.start();
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
            channel.queueBind(myQueue, HospitalUtils.exchangeName, HospitalUtils.doctorsPrefix + HospitalUtils.topicSeparator + myName);
            channel.queueBind(myQueue, HospitalUtils.administratorExchangeName, "");
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


    private class DoctorsPublisher extends Thread {
        BufferedReader br;

        DoctorsPublisher(BufferedReader br) {
            this.br = br;
        }

        @Override
        public void run() {
            while (true) {
                String examinationType = null;
                String patientName = null;
                try {
                    System.out.println("Enter type of examination: ");
                    examinationType = br.readLine();
                    System.out.println("Enter patient's name: ");
                    patientName = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String key = HospitalUtils.examinationPrefix + HospitalUtils.topicSeparator + examinationType.trim();
                String message = myName + HospitalUtils.messageSeparator + examinationType + HospitalUtils.messageSeparator + patientName.trim();

                try {
                    channel.basicPublish(HospitalUtils.exchangeName, key, null, message.getBytes("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Sent: " + message);
            }
        }
    }

    private class DoctorsListener extends Thread {
        String queueName;
        Channel channel;

        private DoctorsListener(String queueName, Channel channel) {
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
        Doctor doctor = new Doctor();
        doctor.start();
    }
}
