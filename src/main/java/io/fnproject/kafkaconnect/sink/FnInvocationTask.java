package io.fnproject.kafkaconnect.sink;

import io.fnproject.httpclient.FnHTTPPost;
import java.util.Collection;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

public class FnInvocationTask extends SinkTask {

    private Map<String, String> config;

    @Override
    public void start(Map<String, String> config) {
        System.out.println("Task started with config... " + config);
        this.config = config;
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        super.open(partitions);
        this.context.assignment().stream()
                                 .forEach((tp) -> System.out.println("Task assigned partition "+ tp.partition() + " in topic "+ tp.topic()));
    }
    
    

    @Override
    public void put(Collection<SinkRecord> records) {
        System.out.println("No. of records " + records.size());
        
        String tenancyOCID = config.get(FnInvocationConfig.TENANT_OCID_CONFIG);
        String usrOCID = config.get(FnInvocationConfig.USER_OCID_CONFIG);
        String publicKeyFingerprint = config.get(FnInvocationConfig.PUBLIC_KEY_FINGERPRINT_CONFIG);
        String privateKeyFilename = config.get(FnInvocationConfig.PRIVATE_KEY_CONFIG);
        String endpoint = config.get(FnInvocationConfig.FUNCTION_URL_CONFIG);
        
        for (SinkRecord record : records) {
            System.out.println("Got record from offset "+ record.kafkaOffset() + " in partition "+ record.kafkaPartition() + " of topic "+ record.topic());
            System.out.println("Invoking " + endpoint + " in thread " + Thread.currentThread().getName() + " with value " + (String) record.value());

            try {
                FnHTTPPost fnPOST = new FnHTTPPost(tenancyOCID, usrOCID, publicKeyFingerprint, privateKeyFilename);
                String fnPOSTResult = fnPOST.invoke(endpoint, (String) record.value());
                System.out.println("Invocation result for payload " + (String) record.value() + " - " + fnPOSTResult);
            } catch (Exception e) {
                System.out.println("Fn invoke failed...");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        System.out.println("Task stopped... ");
    }

    @Override
    public String version() {
        System.out.println("Getting Task version...");
        return "v1.0";
    }
}
