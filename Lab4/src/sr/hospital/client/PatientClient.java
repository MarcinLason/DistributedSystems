package sr.hospital.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sr.hospital.gen.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Created by Cinek on 2017-05-03.
 */
public class PatientClient {
    private Patient patient = null;
    private PatientID patientID = null;
    private BufferedReader br;
    private ManagedChannel managedChannel;
    private HospitalServiceGrpc.HospitalServiceBlockingStub hospitalServiceBlockingStub;

    public PatientClient(String host, int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
    }

    private void start() {
        init();
        work();
    }

    private void init() {
        br = new BufferedReader(new InputStreamReader(System.in));
        hospitalServiceBlockingStub = HospitalServiceGrpc.newBlockingStub(managedChannel);
        setPatient();
        patientID = PatientID.newBuilder().setId(patient.getId()).build();
        checkIfPatientAdded(hospitalServiceBlockingStub.addPatientToDatabase(patient).getCode());
    }

    private void checkIfPatientAdded(OperationCode operationCode){
        if(operationCode.equals(OperationCode.OK)){
            System.out.println("You are now in hospital's database.");
        }else{
            System.out.println("Error, something went wrong while adding to hospital's database!");
        }
    }

    private void setPatient() {
        try {
            patient = Patient.newBuilder()
                    .setId(askForId())
                    .setName(askForName())
                    .setContact(askForContact())
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String askForId() throws IOException {
        System.out.println("Please insert your ID.");
        return br.readLine();
    }

    private Name askForName() throws IOException {
        System.out.println("Please insert your firstname");
        String firstname = br.readLine();
        System.out.println("Please insert your lastname");
        String lastname = br.readLine();
        return Name.newBuilder()
                .setFirstname(firstname)
                .setLastname(lastname)
                .build();
    }

    private Contact askForContact() {
        String telephone = null;
        String address = null;
        String email = null;

        try {
            System.out.println("Please insert your telephone number");
            telephone = br.readLine();
            System.out.println("Please insert your address");
            address = br.readLine();
            System.out.println("Please insert your email");
            email = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Contact.newBuilder()
                .setTelephone(telephone)
                .setAddress(address)
                .setEmail(email).build();
    }

    private void work() {
        while (true) {
            try {
                System.out.println("==>");
                System.out.flush();
                String line = br.readLine();

                if (line.equals("exit")) {
                    return;
                }
                if (line.equals("show")) {
                    System.out.println("List of your examinations:");
                    Iterator<Examination> iterator = hospitalServiceBlockingStub.getPatientExaminations(patientID);
                    while (iterator.hasNext()){
                        System.out.println(iterator.next());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        PatientClient patientClient = new PatientClient("localhost", 50052);
        patientClient.start();
    }
}
