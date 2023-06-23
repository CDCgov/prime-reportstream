FROM python:3.9

WORKDIR "/home"

RUN pip3 install flask
COPY FlaskRestTransportServer.py .

EXPOSE 80

CMD ["python3", "FlaskRestTransportServer.py"]