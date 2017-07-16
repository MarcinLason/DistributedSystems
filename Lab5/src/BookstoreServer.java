import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.SmallestMailboxPool;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class BookstoreServer {
    private static final int NUMBER_OF_ACTORS = 100;

    public static void main(String[] args) throws IOException {
        Config config = ConfigFactory.parseFile(new File("bookstore_app.conf"));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        final ActorSystem bookstoreSystem = ActorSystem.create("bookstoreSystem", config);
        final ActorRef searchActor = bookstoreSystem.actorOf(Props.create(SearchActor.class).withRouter(new SmallestMailboxPool(NUMBER_OF_ACTORS)), "searchActor");
        final ActorRef orderActor = bookstoreSystem.actorOf(Props.create(OrderActor.class).withRouter(new SmallestMailboxPool(NUMBER_OF_ACTORS)), "orderActor");
        final ActorRef readActor = bookstoreSystem.actorOf(Props.create(ReadActor.class).withRouter(new SmallestMailboxPool(NUMBER_OF_ACTORS)), "readActor");

        System.out.println("Server started.");

        while (true) {
            String line = br.readLine();
            if (line.equals("-q")) {
                bookstoreSystem.terminate();
            }
        }
    }
}
