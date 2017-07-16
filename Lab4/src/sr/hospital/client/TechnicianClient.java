package sr.hospital.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sr.hospital.gen.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cinek on 2017-05-03.
 */
public class TechnicianClient {
    private Technician technician = null;
    private BufferedReader br;
    private ManagedChannel managedChannel;
    private HospitalServiceGrpc.HospitalServiceBlockingStub hospitalServiceBlockingStub;

    public TechnicianClient(String host, int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
    }

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        setTechnician();
        hospitalServiceBlockingStub = HospitalServiceGrpc.newBlockingStub(managedChannel);
    }

    private void start() {
        init();
        work();
    }

    private void setTechnician() {
        try {
            technician = Technician.newBuilder().setId(askForTechnicianId()).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String askForTechnicianId() throws IOException {
        System.out.println("Please insert your ID.");
        return br.readLine();
    }

    private Examination askForExamination() {
        String doctorsID = null;
        String date = null;
        List<Parameter> parameterList = new ArrayList<Parameter>();
        try {
            System.out.println("Insert doctor's ID.");
            doctorsID = br.readLine();
            System.out.println("Insert today's date in format: <day>-<month>-<year>");
            date = br.readLine().replace("-", "");

            while (true) {
                System.out.println("Insert parameters of examination: <name>:<value>:<unitName>:<minNorm>:<maxNom>. After last parameter insert 'stop'.");
                String readParameter = br.readLine();
                if (readParameter.equals("stop")) {
                    break;
                }
                String[] splitParameter = readParameter.split(":");
                Norm norm = Norm.newBuilder()
                        .setMinValue(Integer.valueOf(splitParameter[3]))
                        .setMaxValue(Integer.valueOf(splitParameter[4]))
                        .build();
                Parameter parameter = Parameter.newBuilder()
                        .setName(splitParameter[0])
                        .setValue(Integer.valueOf(splitParameter[1]))
                        .setUnitName(splitParameter[2])
                        .setNorm(norm)
                        .build();
                parameterList.add(parameter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Examination examination = Examination.newBuilder()
                .setDate(Long.valueOf(date))
                .setDoctorsID(doctorsID)
                .addAllListOfParameters(parameterList)
                .build();
        return examination;
    }

    private void work() {
        while (true) {
            try {
                System.out.println("==>");
                System.out.flush();
                String line = br.readLine();

                if (line.equals("add")) {
                    System.out.println("Insert patient's ID:");
                    String patientsID = br.readLine();
                    AddingPatientsExamination patientsExamination = AddingPatientsExamination.newBuilder()
                            .setId(patientsID)
                            .setExamination(askForExamination())
                            .build();
                    OperationStatus operationStatus = hospitalServiceBlockingStub.addPatientExamination(patientsExamination);
                    if(operationStatus.getCode().equals(OperationCode.OK)){
                        System.out.println("Examination successfully added.");
                    }else {
                        System.out.println("Error while adding examination!");
                    }
                }
                if (line.equals("exit")) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        TechnicianClient technicianClient = new TechnicianClient("localhost", 50052);
        technicianClient.start();
    }
}
