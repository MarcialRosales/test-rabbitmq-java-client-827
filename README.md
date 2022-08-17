# Verify PR Re-enable DNS round robin

After you have cloned this repo and cd'ed into it run these steps to set everything up:

1. `make get-java-client` to get the PR [Re-enable DNS round robin](https://github.com/rabbitmq/rabbitmq-java-client/pull/827)
2. `make build-java-client` to build the PR which produces a jar with version `6.0.0-SNAPSHOT`
3. `make build-app` to build a sample Java app which has as a dependency the built library
4. `make deploy-cluster` to deploy a 3 node cluster
5. `docker-compose log -f ` to watch the logs until the cluster is fully running

**Note on certificates and private-keys under tls folder**: The certificates and private keys were generated following these steps:
```
git clone https://github.com/michaelklishin/tls-gen
cd tls-gen/basic
make
make CN=rmq0 SERVER_ALT_NAME=rmq gen-server
make CN=rmq1 SERVER_ALT_NAME=rmq gen-server
make CN=rmq2 SERVER_ALT_NAME=rmq gen-server
cd result
cp ca* server_rmq* ../../../tls
cd ../../../tls
keytool -import -alias rmq -file ca_certificate.pem -keystore java_keystore -storepass password
```

To verify we can connect to one of the RabbitMQ Servers:
```
make run-java-app HOSTNAME=rmq1
```
To verify that we do have a dns round-robin setup, we run this `nslookup` query against `rmq`
```
docker container run --rm --network test-rabbitmq-java-client-827_rabbitmq alpine nslookup rmq

Non-authoritative answer:
Name:	rmq
Address: 172.24.0.4
Name:	rmq
Address: 172.24.0.2
Name:	rmq
Address: 172.24.0.3

```
we can see that there are 3 ip-addesses resolved to the hostname `rmq`.

Finally, we test if we can use `rmq` to connect to the cluster using the alias hostname `rmq` and
having `enableHostnameVerification=true`.
```
make run-java-app HOSTNAME=rmq
```
