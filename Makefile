.ONESHELL:# single shell invocation for all lines in the recipe
SHELL = bash# we depend on bash expansion for e.g. queue patterns

.DEFAULT_GOAL = help

HOSTNAME ?= rmq

### TARGETS ###

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

get-java-client: ## Get RabbitMq java client
	git clone git@github.com:ogolberg/rabbitmq-java-client.git
	git checkout dns-round-robin
build-java-client: ## Build RabbitMq java client
	@(cd rabbitmq-java-client; make deps; mvn clean install  -Dmaven.test.skip -P '!setup-test-cluster')

deploy-cluster: ## Deploy RabbitMQ cluster
	@(docker-compose -d up)

destroy-cluster: ## Destroy RabbitMQ cluster
	@(docker-compose down)

build-java-app: ## Build Java App
	@echo "Building sample apps "
	@(cd tls-java-app; mvn clean package)

run-java-app: ## Run Java App
	@(docker run --network test-rabbitmq-java-client-827_rabbitmq \
	 --env TRUST_STORE_PATH=/tls/java_keystore \
	 --env ENABLE_HOSTNAME_VERIFICATION=true \
	 --env URI=amqps://guest:guest@$(HOSTNAME):5671/%2F \
	 -v $(PWD)/tls:/tls \
	 -v $(PWD)/tls-java-app/target:/jars \
	 openjdk:11 java -jar jars/tls-java-app-0.0.1-SNAPSHOT.jar \
	 -Dorg.slf4j.simpleLogger.defaultLogLevel=debug)
