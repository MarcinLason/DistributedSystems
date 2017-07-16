import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;

public class ClientActor extends AbstractActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private static final String bookstorePath = "akka.tcp://bookstoreSystem@127.0.0.1:2552/user/";

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, s -> s.startsWith("-s"), s -> {
                    String name = s.substring(2).trim();
                    getContext().actorSelection(bookstorePath + "searchActor").tell(name, getSelf());
                })
                .match(String.class, s -> s.startsWith("-o"), s -> {
                    String name = s.substring(2).trim();
                    getContext().actorSelection(bookstorePath + "orderActor").tell(name, getSelf());
                })
                .match(String.class, s -> s.startsWith("-r"), s -> {
                    String name = s.substring(2).trim();
                    getContext().actorSelection(bookstorePath + "readActor").tell(name, getSelf());
                })
                .match(Integer.class, integer -> integer.intValue() == 0, integer -> {
                    System.out.println(BookstoreUtils.ORDER_SAVED_TEXT);
                })
                .match(Integer.class, integer -> integer.intValue() == -1, integer -> {
                    System.out.println(BookstoreUtils.BOOK_NOT_FOUND_TEXT);
                })
                .match(Double.class, d -> d.doubleValue() > 0, d -> {
                    System.out.println(BookstoreUtils.BOOK_FOUND_TEXT + d.doubleValue());
                })
                .match(String.class, s -> {
                    System.out.println(s);
                })
                .matchAny(o -> logger.info(BookstoreUtils.UNKNOWN_MESSAGE))
                .build();
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(3, Duration.create(3, TimeUnit.SECONDS), DeciderBuilder
                    .matchAny(o -> restart()).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
