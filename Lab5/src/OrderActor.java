import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;


public class OrderActor extends AbstractActor {
    private static final Logger logger = LoggerFactory.getLogger(OrderActor.class);
    private File ordersFile;

    public OrderActor() throws IOException {
        this.ordersFile = new File("orders.txt");
        if (!ordersFile.exists()) {
            ordersFile.createNewFile();
        }
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, orderName -> {
                    synchronized (BookstoreServer.class) {
                        PrintWriter pw = new PrintWriter(new FileOutputStream(ordersFile, true));
                        pw.append(orderName).append("\n");
                        pw.flush();
                        pw.close();
                        getSender().tell(BookstoreUtils.ORDER_SAVED_VALUE, getSelf());
                    }
                })
                .matchAny(o -> logger.info(BookstoreUtils.UNKNOWN_MESSAGE))
                .build();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private static SupervisorStrategy strategy
            = new OneForOneStrategy(3, Duration.create(10, TimeUnit.SECONDS), DeciderBuilder
            .match(IOException.class, e -> SupervisorStrategy.stop())
            .matchAny(o -> SupervisorStrategy.restart())
            .build());
}
