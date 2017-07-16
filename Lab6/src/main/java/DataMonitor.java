import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

import static org.apache.zookeeper.Watcher.Event.EventType.*;

public class DataMonitor implements Watcher {

    private final String pathToExecutable;
    private ZooKeeper zooKeeper;
    private Process process;

    public DataMonitor(ZooKeeper zooKeeper, String pathToExecutable) {
        this.zooKeeper = zooKeeper;
        this.pathToExecutable = pathToExecutable;
    }

    public void init() {
        try {
            Stat stat = zooKeeper.exists(AppUtils.ZNODE_NAME, this);
            if (stat != null) {
                observeChildren(AppUtils.ZNODE_NAME);
                if (process == null) {
                    process = new ProcessBuilder(pathToExecutable).start();
                }
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void observeChildren(String name) throws KeeperException, InterruptedException {
        List<String> childrenList = zooKeeper.getChildren(name, this);
        for (String s : childrenList) {
            observeChildren(name + "/" + s);
        }
    }

    public void process(WatchedEvent watchedEvent) {
        try {
            Event.EventType eventType = watchedEvent.getType();

            if (eventType.equals(NodeCreated)) {
                process = new ProcessBuilder(pathToExecutable).start();
                zooKeeper.getChildren(AppUtils.ZNODE_NAME, this);
            }
            if (eventType.equals(NodeDeleted) && watchedEvent.getPath().equals(AppUtils.ZNODE_NAME) && process != null) {
                process.destroy();
                zooKeeper.exists(AppUtils.ZNODE_NAME, this);
            }
            if (eventType.equals(NodeChildrenChanged)) {
                int childrenAmount = countZnodeChildren(AppUtils.ZNODE_NAME);
                System.out.println("***CHILDREN INFO***");
                System.out.println(AppUtils.ZNODE_NAME + " has " + childrenAmount + " children.");
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int countZnodeChildren(String name) {
        int counter = 0;
        Stat stat;
        try {
            stat = zooKeeper.exists(name, this);
            counter = counter + stat.getNumChildren();
            List<String> childrenList = zooKeeper.getChildren(name, this);

            for (String s : childrenList) {
                counter = counter + countZnodeChildren(name + "/" + s);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return counter;
    }
}
