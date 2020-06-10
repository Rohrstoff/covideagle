import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.time.Instant;
import java.util.*;

public class Main {

    public static void main(final String... args)
    {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Commons.KAFKA_SERVER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "FlinkConsumerGroup");


        DataStream<ObjectNode> entrancesMessageStream = env.addSource(new FlinkKafkaConsumer010<>(Commons.COVIDEAGLE_KAFKA_TOPIC, new JSONDeserializationSchema(), props));
        DataStream<Tuple2<String, Integer>> messageStream = entrancesMessageStream.map(new MapFunction<ObjectNode, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(ObjectNode jsonNodes) throws Exception {
                String payload = new String( Base64.getDecoder().decode( jsonNodes.get("payload").asText() ) );
                String[] keyValue = payload.split(":");
                return new Tuple2<>(keyValue[0], Integer.valueOf( keyValue[1] ));
            }
        });

        List<HttpHost> httpHosts = new ArrayList<>();
        httpHosts.add(new HttpHost("elasticsearch", 9200, "http"));

        ElasticsearchSink.Builder<Tuple2<String, Integer>> esSink = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<>() {

                    private IndexRequest createIndexRequest(Tuple2<String, Integer> element) {
                        Map<String, Long> json = new HashMap<>();
                        json.put("customer", Long.valueOf( element.f1 ) );

                        return Requests.indexRequest()
                                .index("covideagle-live")
                                .id(String.valueOf(Instant.now().getEpochSecond()))
                                .source(json);
                    }

                    @Override
                    public void process(Tuple2<String, Integer> element, RuntimeContext ctx, RequestIndexer indexer) {
                        indexer.add(createIndexRequest(element));
                    }
                }
        );

        esSink.setBulkFlushMaxActions(1);

        esSink.setRestClientFactory( restClientBuilder -> {
            restClientBuilder.setDefaultHeaders(new BasicHeader[]{new BasicHeader("Content-Type","application/json")});
        });

        messageStream.addSink( esSink.build() );
        messageStream.print();

        messageStream = messageStream.keyBy(0).reduce( (t1, t2) -> new Tuple2<>( t1.f0, t1.f1 + t2.f1 ));
        messageStream.print();

        ElasticsearchSink.Builder<Tuple2<String, Integer>> esSinkBuilder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<>() {

                    private IndexRequest createIndexRequest(Tuple2<String, Integer> element) {
                        Map<String, Long> json = new HashMap<>();
                        json.put("customer", Long.valueOf( element.f1 ) );
                        json.put( "time", Instant.now().getEpochSecond() );

                        return Requests.indexRequest()
                                .index("covideagle-history")
                                .id(String.valueOf(Instant.now().getEpochSecond()))
                                .source(json);
                    }

                    @Override
                    public void process(Tuple2<String, Integer> element, RuntimeContext ctx, RequestIndexer indexer) {
                        indexer.add(createIndexRequest(element));
                    }
                }
        );

        esSinkBuilder.setBulkFlushMaxActions(1);

        esSinkBuilder.setRestClientFactory( restClientBuilder -> {
            restClientBuilder.setDefaultHeaders(new BasicHeader[]{new BasicHeader("Content-Type","application/json")});
        });

        messageStream.addSink(esSinkBuilder.build());


        try {
            env.execute();
        } catch (Exception e) {
        }
    }
}
