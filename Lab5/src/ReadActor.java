import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ReadActor extends AbstractActor {
    private static final Logger logger = LoggerFactory.getLogger(ReadActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, bookName -> {
                    ActorMaterializer materializer = ActorMaterializer.create(context());
                    Path path = Paths.get(bookName);

                    final Source<String, NotUsed> source = Source.fromIterator(() -> Files.lines(path).iterator());
                    source.throttle(1, Duration.create(1, TimeUnit.SECONDS), 1, ThrottleMode.shaping())
                            .to(Sink.actorRef(getSender(), NotUsed.getInstance()))
                            .run(materializer);
                })
                .matchAny(o -> logger.info(BookstoreUtils.UNKNOWN_MESSAGE))
                .build();
    }

    private static SupervisorStrategy strategy
            = new OneForOneStrategy(3, Duration.create(10, TimeUnit.SECONDS), DeciderBuilder
            .matchAny(o -> SupervisorStrategy.restart()).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
