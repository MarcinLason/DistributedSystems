import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class App {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String pathToExecutable = null;

        try {
            System.out.println("Please insert path to the exe file of application to launch.");
            pathToExecutable = br.readLine();
            while (!pathToExecutable.endsWith(".exe")) {
                System.out.println("You did not passed path to .exe file. Try again.");
                pathToExecutable = br.readLine();
            }
            pathToExecutable = pathToExecutable.replace('\\', '/');
        } catch (IOException e) {
            e.printStackTrace();
        }

        WatcherClass watcherClass = new WatcherClass(pathToExecutable);
        watcherClass.run();
    }
}
