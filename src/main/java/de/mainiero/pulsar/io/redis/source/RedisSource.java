package de.mainiero.pulsar.io.redis.source;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Connector(
        name = "redis",
        type = IOType.SOURCE,
        help = "A simple connector to move messages from a Redis pub/sub to a Pulsar topic",
        configClass = RedisSourceConfig.class)
public class RedisSource extends PushSource<String> {

    private static final Logger logger = LoggerFactory.getLogger(RedisSource.class);

    private RedisClient redisClient;
    private StatefulRedisPubSubConnection<String, String> connection;

    @Override
    public void open(Map<String, Object> config, SourceContext sourceContext) throws Exception {

        RedisSourceConfig redisSourceConfig = RedisSourceConfig.load(config);
        redisSourceConfig.validate();

        redisClient = RedisClient.create(RedisURI.Builder.redis(redisSourceConfig.getHost(), redisSourceConfig.getPort()).build());
        logger.info("A new client to {}:{} has been created successfully.",
                redisSourceConfig.getHost(),
                redisSourceConfig.getPort()
        );
        connection = redisClient.connectPubSub();
        connection.addListener(new RedisListener(this));
        final RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe(redisSourceConfig.getChannelName());
        logger.info("A new listener has been attached to channel {}.",
                redisSourceConfig.getChannelName()
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
        redisClient.shutdown();
    }

    private static class RedisListener implements RedisPubSubListener<String, String> {

        private static final Logger logger = LoggerFactory.getLogger(RedisListener.class);

        private final RedisSource source;

        public RedisListener(RedisSource source) {
            this.source = source;
        }

        @Override
        public void message(final String channel, final String message) {
            logger.info("Recieved message \"{}\" from channel \"{}\".", message, channel);
            RedisRecord record = new RedisRecord(Optional.ofNullable(channel), message);
            this.source.consume(record);
            logger.info("Message consumed.");
        }

        @Override
        public void message(String pattern, String channel, String message) {
            logger.info("Method not implemented.");
        }

        @Override
        public void subscribed(String channel, long count) {
            logger.info("Method not implemented.");
        }

        @Override
        public void psubscribed(String pattern, long count) {
            logger.info("Method not implemented.");
        }

        @Override
        public void unsubscribed(String channel, long count) {
            logger.info("Method not implemented.");
        }

        @Override
        public void punsubscribed(String pattern, long count) {
            logger.info("Method not implemented.");
        }
    }

    private static class RedisRecord implements Record<String> {
        private final Optional<String> key;
        private final String value;

        public RedisRecord(Optional<String> key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Optional<String> getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}