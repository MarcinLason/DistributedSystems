package sr.hospital.server;

import io.grpc.stub.StreamObserver;
import sr.hospital.gen.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Cinek on 2017-05-03.
 */
public class HospitalServiceImpl extends HospitalServiceGrpc.HospitalServiceImplBase {
    private int streamCounter;
    private Map<String, Patient> patientsMap = new HashMap();

    @Override
    public void getPatientExaminations(PatientID request, StreamObserver<Examination> responseObserver) {
        Patient currentPatient = patientsMap.get(request.getId());
        streamCounter = 0;

        while (streamCounter < currentPatient.getListOfExaminationsList().size()) {
            responseObserver.onNext(currentPatient.getListOfExaminations(streamCounter++));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public void addPatientToDatabase(Patient request, StreamObserver<OperationStatus> responseObserver) {
        OperationStatus result;
        if (!patientsMap.containsKey(request.getId())) {
            patientsMap.put(request.getId(), request);
            result = OperationStatus.newBuilder().setCode(OperationCode.OK).build();
        } else {
            result = OperationStatus.newBuilder().setCode(OperationCode.FAIL).build();
        }

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void addPatientExamination(AddingPatientsExamination request, StreamObserver<OperationStatus> responseObserver) {
        OperationStatus result;
        if (patientsMap.containsKey(request.getId())) {
            Patient patient = patientsMap.get(request.getId());
            patient = patient.toBuilder().addListOfExaminations(request.getExamination()).build();
            patientsMap.put(request.getId(), patient);
            result = OperationStatus.newBuilder().setCode(OperationCode.OK).build();
        } else {
            result = OperationStatus.newBuilder().setCode(OperationCode.FAIL).build();
        }

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarExaminations(Examination request, StreamObserver<Examination> responseObserver) {
        List<Parameter> parameterList = request.getListOfParametersList();
        Map<String, Integer> mapOfParameters = new HashMap<>();

        for (Parameter param : parameterList) {
            mapOfParameters.put(param.getName(), param.getValue());
        }

        for (Map.Entry<String, Patient> entry : patientsMap.entrySet()) {
            List<Examination> examinationList = entry.getValue().getListOfExaminationsList();
            for (Examination examination : examinationList) {
                List<Parameter> parameterList1 = examination.getListOfParametersList();
                for (Parameter parameter : parameterList1) {
                    String parameterName = parameter.getName();
                    int parameterValue = parameter.getValue();

                    if (mapOfParameters.containsKey(parameterName) && similarValue(mapOfParameters.get(parameterName), parameterValue)) {
                        responseObserver.onNext(examination);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
        responseObserver.onCompleted();
    }

    private boolean similarValue(Integer expected, Integer found) {
        if (Float.max(expected, found) / Float.min(expected, found) < 1.5) {
            return true;
        }
        return false;
    }
}
