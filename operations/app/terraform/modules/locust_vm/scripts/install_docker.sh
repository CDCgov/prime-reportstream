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
    command: -f /mnt/locust/locustfile.py --master -H http://master:8089
EOL

# Create a sample locustfile.py
cat > /opt/locust/locustfile.py << 'EOL'
from locust import HttpUser, task

class SimpleUser(HttpUser):
    @task
    def get_endpoint(self):
        self.client.get("/api/reports")
EOL

# Start Locust
cd /opt/locust && docker-compose up -d 