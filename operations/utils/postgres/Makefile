# Variables
IMAGE_NAME = rs-postgres-16.6
CONTAINER_NAME = rs-postgres
DOCKERFILE = Dockerfile.postgres
POSTGRES_PORT = 5432
NETWORK_NAME = pg_network


build:
	docker build -t $(IMAGE_NAME) -f $(DOCKERFILE) .

network:
	@docker network ls | grep -q $(NETWORK_NAME) || docker network create $(NETWORK_NAME)


run: stop network
	docker run -d --name $(CONTAINER_NAME) --network $(NETWORK_NAME) \
		-p $(POSTGRES_PORT):5432 \
		-e POSTGRES_USER=user \
		-e POSTGRES_PASSWORD=password \
		-e POSTGRES_DB=mydb \
		$(IMAGE_NAME)

stop:
	@docker ps -q --filter "name=$(CONTAINER_NAME)" | grep -q . && docker stop $(CONTAINER_NAME) || echo "Container not running"

destroy: stop
	@docker ps -a -q --filter "name=$(CONTAINER_NAME)" | grep -q . && docker rm $(CONTAINER_NAME) || echo "No container to remove"

logs:
	docker logs -f $(CONTAINER_NAME)

psql:
	docker exec -it $(CONTAINER_NAME) psql -U muser -d mydb

shell:
	docker exec -it $(CONTAINER_NAME) /bin/sh

status:
	docker ps | grep $(CONTAINER_NAME) || echo "Container not running"

help:
	@echo "Available commands:"
	@echo "  make build      - Build the PostgreSQL image"
	@echo "  make run        - Start the PostgreSQL container"
	@echo "  make stop       - Stop the running PostgreSQL container"
	@echo "  make destroy    - Stop and remove the container"
	@echo "  make logs       - View PostgreSQL logs"
	@echo "  make psql       - Connect to PostgreSQL via psql shell"
	@echo "  make shell      - Open a shell inside the PostgreSQL container"
	@echo "  make status     - Check if the PostgreSQL container is running"
	@echo "  make clean      - Remove unused Docker resources"
	@echo "  make compose    - Start PostgreSQL using Docker Compose"
	@echo "  make down       - Stop and remove Compose containers"
	@echo "  make help       - Show this help message"

.PHONY: build run stop destroy logs psql shell status clean network help
