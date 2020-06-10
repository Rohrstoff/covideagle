public class Commons {
    public final static String COVIDEAGLE_KAFKA_TOPIC = System.getenv("COVIDEAGLE_KAFKA_TOPIC") != null ?
            System.getenv("COVIDEAGLE_KAFKA_TOPIC") : "mqtt.covideagle";
    public final static String KAFKA_SERVER = System.getenv("KAFKA_SERVER") != null ?
            System.getenv("KAFKA_SERVER") : "localhost:9092";
}
