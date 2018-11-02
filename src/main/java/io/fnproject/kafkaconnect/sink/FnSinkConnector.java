package io.fnproject.kafkaconnect.sink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

public class FnSinkConnector extends SinkConnector {

    private Map<String, String> configProperties;
    
        
    @Override
    public void start(Map<String, String> config) {
        this.configProperties = config;
        System.out.println("Connector started with config " + config);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return FnInvocationTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int numOfMaxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();
        properties.putAll(configProperties);
        for (int i = 0; i < numOfMaxTasks; i++) {
            taskConfigs.add(properties);
        }
        System.out.println("Task configuration complete..");
        return taskConfigs;
    }

    @Override
    public void stop() {
        System.out.println("Connector stopped");
    }

    @Override
    public ConfigDef config() {
        System.out.println("Fetching connector config");
        return FnInvocationConfig.getConfigDef();
    }

    @Override
    public String version() {
        //TODO
        return "v1.0";
    }

}
