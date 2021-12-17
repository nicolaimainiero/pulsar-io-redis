package de.mainiero.pulsar.io.redis.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import org.apache.pulsar.io.core.annotations.FieldDoc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class RedisSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @FieldDoc(
            required = true,
            defaultValue = "",
            help = "The connection name used for connecting to Redis")
    private String connectionName;

    @FieldDoc(
            required = true,
            defaultValue = "",
            help = "The Redis host to connect to")
    private String host;

    @FieldDoc(
            required = true,
            defaultValue = "6379",
            help = "The Redis port to connect to")
    private int port = 6379;

    @FieldDoc(
            required = true,
            defaultValue = "",
            help = "The Redis channel from which messages should be read")
    private String channelName;


    public String getConnectionName() {
        return connectionName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getChannelName() {
        return channelName;
    }

    public static RedisSourceConfig load(String yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(yamlFile), RedisSourceConfig.class);
    }

    public static RedisSourceConfig load(Map<String, Object> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new ObjectMapper().writeValueAsString(map), RedisSourceConfig.class);
    }

    public void validate() {
        Preconditions.checkNotNull(connectionName, "connectionName property not set.");
        Preconditions.checkNotNull(host, "host property not set.");
        Preconditions.checkArgument(port > 0, "port property must be > 0.");
        Preconditions.checkNotNull(channelName, "channelName property not set.");
    }
}