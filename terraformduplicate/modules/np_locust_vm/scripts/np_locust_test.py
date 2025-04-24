from locust import HttpUser, task, between

class APIUser(HttpUser):
    # Wait between 1 and 5 seconds between tasks
    wait_time = between(1, 5)

    @task
    def get_endpoint(self):
        # Simple GET request to the API endpoint
        self.client.get("/") 