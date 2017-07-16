import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class WatcherClass implements Watcher, Runnable {

    private final String pathToExecutable;
    private BufferedReader br;
    private ZooKeeper zooKeeper;

    WatcherClass(String pathToExecutable) {
        this.pathToExecutable = pathToExecutable;
        this.br = new BufferedReader(new InputStreamReader(System.in));
    }

    public void process(WatchedEvent watchedEvent) {
        System.out.println(watchedEvent.toString());
    }

    public void run() {
        try {
            this.zooKeeper = new ZooKeeper(AppUtils.ADDRESS, AppUtils.TIMEOUT, this);
            DataMonitor dataMonitor = new DataMonitor(zooKeeper, pathToExecutable);
            dataMonitor.init();
            String line;

            printHelp();
            while (true) {
                line = br.readLine();

                if (line.equals("-t")) {
                    Stat stat1 = zooKeeper.exists(AppUtils.ZNODE_NAME, false);
                    if (stat1 == null) {
                        System.out.println(AppUtils.ZNODE_NAME + " does not exists");
                    } else {
                        System.out.println("***ZNODES TREE***");
                        printTree(AppUtils.ZNODE_NAME, 0);
                    }
                } else if (line.equals("-h")) {
                    printHelp();
                } else if (line.equals("-q")) {
                    return;
                } else {
                    System.out.println("Wrong command. Type -h to see help.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    private void printHelp() {
        System.out.println("List of commands:");
        System.out.println("-q - quit the application.");
        System.out.println("-t - print znode tree.");
        System.out.println("-h - print help.");
    }


    private void printTree(String name, int i) throws KeeperException, InterruptedException {
        for (int j = 0; j < i; j++) {
            System.out.print("   ");
        }
        System.out.println(name);

        List<String> childrenList = zooKeeper.getChildren(name, false);
        for (String s : childrenList) {
            printTree(name + "/" + s, i + 1);
        }
    }
}
