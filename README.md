# Instructions

## System Context

To start the system context with Apache Pulsar, UI and Redis run in `./system-context/`

```
docker compose up
```

Then, to access the UI at http://localhost:9527 create an user and password with the following command:

```shell
CSRF_TOKEN=$(curl http://localhost:9527/pulsar-manager/csrf-token)
curl \
    -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
    -H "Cookie: XSRF-TOKEN=$CSRF_TOKEN;" \
    -H 'Content-Type: application/json' \
    -X PUT http://localhost:9527/pulsar-manager/users/superuser \
    -d '{"name": "admin", "password": "apachepulsar", "description": "test", "email": "username@test.org"}'
```

Log in and then create a new environment with the following parameter:

| Parameter        | Value |
|------------------|-------|
| environment name | localhost |
| service URL      |     http://pulsar:8080  |

Create new namespace in sample tenant and call it sample.

```
docker exec -it pulsar-io-redis-pulsar-1 \
   /pulsar/bin/pulsar-admin --admin-url http://pulsar:8080/ \
   namespaces create sample/sample
```

## Install the connector

Build the connector with maven:

```shell
./mvnw clean install
```

The install target copies the artifact into the system context. The `io` directroy is mounted into the pulsar container.
The source connector can then be created by executing the following snippet after creating the necessary namespace and
topic with the following command:

```shell
docker exec -it pulsar-io-pulsar-1 \
   /pulsar/bin/pulsar-admin --admin-url http://pulsar:8080/ \
   namespaces create  sample/sample
   
docker exec -it pulsar-io-pulsar-1 \
   /pulsar/bin/pulsar-admin --admin-url http://pulsar:8080/ \
   topics create persistent://sample/sample/redis

# Verify if topic was created
docker exec -it pulsar-io-pulsar-1 \
   /pulsar/bin/pulsar-admin --admin-url http://pulsar:8080/ \   
   topics list sample/sample
```

Now install the source connector.

```shell
docker exec -it pulsar-io-pulsar-1 \
   /pulsar/bin/pulsar-admin --admin-url http://pulsar:8080/ \
   sources create --source-config-file /opt/redis-source.yaml \
   -a /opt/io/pulsar-io-redis-1.0-SNAPSHOT.nar --name redis \
   --tenant sample --namespace sample -o redis
```

It creates a new redis source connector using the following configuration for the _sample_ tenant in the _sample_
namespace and writes to the _redis_ topic. The expected output is `"Created successfully"`. As visible in the
configuration yaml, it listens on the `pulsar` channel on the redis server.

```yaml
configs:
  connectionName: "redis"
  host: "redis"
  port: 6379
  channelName: "pulsar"
```

You can inspect the logfile of the connector within the container at `/pulsar/logs/functions/sample/sample/redis`.

## Test the connector

To test the connector create first a new consumer client which reads from the _redis_ topic.

```shell
docker exec -it pulsar-io-pulsar-1 \
/pulsar/bin/pulsar-client consume -s test -n 0 redis
```

Then open a connection to the redis service with the redis cli.

```shell
docker exec -it pulsar-io-redis-1 redis-cli
```

Now publish a new message on the _pulsar_ channel. Something like "Hello World".

```shell
127.0.0.1:6379> PUBLISH pulsar "Hello World"
(integer) 1
127.0.0.1:6379> 
```

You should see something like the following output in the terminal you startet the pulsar consumer.

```
----- got message -----
key:[pulsar], properties:[], content:Hello World
```