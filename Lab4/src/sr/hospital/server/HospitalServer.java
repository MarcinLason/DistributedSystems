package sr.hospital.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Cinek on 2017-05-03.
 */
public class HospitalServer {

    private static final Logger logger = Logger.getLogger(HospitalServer.class.getName());

    private int port = 50052;
    private Server server;

    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new HospitalServiceImpl())
                .build()
                .start();
        logger.info("Server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HospitalServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HospitalServer hospitalServer = new HospitalServer();
        hospitalServer.start();
        hospitalServer.blockUntilShutdown();
    }
}
