import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;


public class SearchActor extends AbstractActor {
    private static final int timeoutValue = 10;
    private static final Logger logger = LoggerFactory.getLogger(SearchActor.class);
    private final ExecutorService executors = Executors.newFixedThreadPool(2);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, bookName -> {
                    Future<String> future1 = searchDatabase("database1.txt", bookName);
                    Future<String> future2 = searchDatabase("database2.txt", bookName);

                    String result1 = future1.get(timeoutValue, TimeUnit.SECONDS);
                    String result2 = future2.get(timeoutValue, TimeUnit.SECONDS);

                    if (result1 != null) {
                        getSender().tell(Double.parseDouble(result1), getSelf());
                        return;
                    }

                    if (result2 != null) {
                        getSender().tell(Double.parseDouble(result2), getSelf());
                        return;
                    }
                    getSender().tell(BookstoreUtils.BOOK_NOT_FOUND_VALUE, getSelf());
                })
                .matchAny(s -> logger.info(BookstoreUtils.UNKNOWN_MESSAGE))
                .build();
    }

    private Future<String> searchDatabase(String databaseName, String bookName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new FileInputStream(new File(databaseName)));
        return executors.submit(() -> {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(bookName)) {
                    return line.substring(bookName.length() + 1).trim();
                }
            }
            return null;
        });
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(3, Duration.create(10, TimeUnit.SECONDS), DeciderBuilder
                    .match(FileNotFoundException.class, e -> restart())
                    .matchAny(o -> escalate()).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
