import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Cinek on 2017-04-13.
 */
public class HospitalUtils {

    public static final List permittedSkills = new ArrayList(Arrays.asList("ankle", "knee", "elbow"));
    public static final String administratorExchangeName = "EXCHANGE_ADMIN";
    public static final String administratorMessagePrefix = "ADMINISTRATOR MESSAGE: ";
    public static final String exchangeName = "EXCHANGE";
    public static final String examinationPrefix = "hospital.toExamine";
    public static final String doctorsPrefix = "hospital.doctors";
    public static final String topicSeparator = ".";
    public static final String messageSeparator = ";";
    public static final String examinationResults = "EXAMINATION";

    private HospitalUtils() {}
}
