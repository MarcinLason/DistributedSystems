import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientApp {

    public static void main(String[] args) throws IOException {
        Config config = ConfigFactory.parseFile(new File("client_app.conf"));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        final ActorSystem clientSystem = ActorSystem.create("clientSystem", config);
        final ActorRef clientActor = clientSystem.actorOf(Props.create(ClientActor.class), "clientActor");

        System.out.println("Welcome in our bookstore's service. Type \"-h\" to see all options.");

        while (true) {
            String line = br.readLine();
            if (line.equals("-q")) {
                break;
            }
            else if (line.equals("-h")) {
                printHelp();
            } else {
                clientActor.tell(line, null);
            }
        }
        clientSystem.terminate();
    }

    private static void printHelp() {
        System.out.println("ALL OPTIONS:");
        System.out.println("-h to print help,");
        System.out.println("-s <name> to search for the specified book,");
        System.out.println("-o <name> to order specified book,");
        System.out.println("-r <name> to read specified book online,");
        System.out.println("-q to quit the application.");
    }
}
