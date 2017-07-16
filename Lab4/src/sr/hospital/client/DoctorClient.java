package sr.hospital.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sr.hospital.gen.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Cinek on 2017-05-03.
 */
public class DoctorClient {
    private Doctor doctor = null;
    private BufferedReader br;
    private HospitalServiceGrpc.HospitalServiceBlockingStub hospitalServiceBlockingStub;
    private ManagedChannel managedChannel;

    public DoctorClient(String host, int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
    }

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        hospitalServiceBlockingStub = HospitalServiceGrpc.newBlockingStub(managedChannel);
        doctor = Doctor.newBuilder().setId(askForId()).build();
    }

    private String askForId() {
        String myId = null;
        try {
            System.out.println("Please insert your ID");
            myId = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myId;
    }

    private Examination askForExamination() {
        List<Parameter> parameterList = new ArrayList<Parameter>();
        while(true){
            try {
                System.out.println("Insert examination's parameters in form: <name>:<value>. To finish insert 'stop'.");
                String parametersInput = br.readLine();
                if(parametersInput.equals("stop")){
                    break;
                }
                String[] splitParametersInput = parametersInput.split(":");
                Parameter parameter = Parameter.newBuilder()
                        .setName(splitParametersInput[0])
                        .setValue(Integer.valueOf(splitParametersInput[1]))
                        .build();
                parameterList.add(parameter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Examination examination = Examination.newBuilder()
                .addAllListOfParameters(parameterList)
                .build();
        return examination;
    }

    private void start() {
        init();
        work();
    }

    private void work() {
        while(true){
            try {
                System.out.println("==>");
                System.out.flush();
                String line = br.readLine();

                if (line.equals("exit")) {
                    return;
                }
                if (line.equals("show")) {
                    System.out.println("Insert patient's ID:");
                    String patient = br.readLine();
                    PatientID patientID = PatientID.newBuilder().setId(patient).build();
                    Iterator<Examination> iterator = hospitalServiceBlockingStub.getPatientExaminations(patientID);
                    while (iterator.hasNext()) {
                        System.out.println(iterator.next());
                    }
                }
                if (line.equals("search")) {
                    Iterator<Examination> iterator = hospitalServiceBlockingStub.getSimilarExaminations(askForExamination());
                    while (iterator.hasNext()) {
                        System.out.println(iterator.next());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        DoctorClient doctorClient = new DoctorClient("localhost", 50052);
        doctorClient.start();
    }
}
