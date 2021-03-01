FROM python:3.9
WORKDIR /code
COPY requirements.txt .
COPY . .
RUN pip install -r requirements.txt
ENTRYPOINT ["python3", "put-local-settings.py"]
