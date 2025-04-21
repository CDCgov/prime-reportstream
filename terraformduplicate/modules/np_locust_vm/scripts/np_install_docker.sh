#!/bin/bash

# Update package list
apt-get update

# Install prerequisites
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common

# Add Docker's official GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -

# Add Docker repository
add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

# Update package list again
apt-get update

# Install Docker
apt-get install -y docker-ce docker-ce-cli containerd.io

# Start and enable Docker
systemctl start docker
systemctl enable docker

# Add current user to docker group
usermod -aG docker $USER

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Create directory for Locust
mkdir -p /opt/locust

# Create docker-compose.yml for Locust
cat > /opt/locust/docker-compose.yml << 'EOL'
version: '3'
services:
  master:
    image: locustio/locust
    ports:
      - "8089:8089"
    volumes:
      - ./:/mnt/locust
    command: -f /mnt/locust/locustfile.py --master -H ${locust_config.target_host}
  worker:
    image: locustio/locust
    volumes:
      - ./:/mnt/locust
    command: -f /mnt/locust/locustfile.py --worker --master-host=master
EOL

# Create locustfile.py
cat > /opt/locust/locustfile.py << 'EOL'
from locust import HttpUser, task, between

class WebsiteUser(HttpUser):
    wait_time = between(1, 5)

    @task
    def load_test(self):
        self.client.get("/")
EOL

# Create locust.conf
cat > /opt/locust/locust.conf << 'EOL'
locustfile = /mnt/locust/locustfile.py
host = ${locust_config.target_host}
users = ${locust_config.users}
spawn-rate = ${locust_config.spawn_rate}
run-time = ${locust_config.run_time}
EOL

# Start Locust
cd /opt/locust && docker-compose up -d 