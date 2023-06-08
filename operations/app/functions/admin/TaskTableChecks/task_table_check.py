import azure.functions as func
import os
import datetime
import psycopg2
import smtplib
from email.mime.text import MIMEText

def main(req: func.HttpRequest) -> func.HttpResponse:
    host = os.getenv("POSTGRES_HOST")
    db = 'prime_data_hub'
    user = os.getenv("POSTGRES_USER")
    password = os.getenv("POSTGRES_PASSWORD")
    email_from = os.getenv("EMAIL_FROM")
    email_to = os.getenv("EMAIL_TO")
    smtp_host = os.getenv("SMTP_HOST")
    smtp_port = os.getenv("SMTP_PORT")
    smtp_username = os.getenv("SMTP_USERNAME")
    smtp_password = os.getenv("SMTP_PASSWORD")

    if host and db and user and password and email_from and email_to and smtp_host and smtp_port and smtp_username and smtp_password:
        user = f"{user}@{host}"
        sslmode = "require"

        conn_string = "host={0} user={1} dbname={2} password={3} sslmode={4}".format(host, user, db, password, sslmode)

        try:
            # Connect to the database
            conn = psycopg2.connect(conn_string)
            cursor = conn.cursor()
            # Execute the query
            cursor.execute("SELECT * FROM public.task WHERE NOW() - INTERVAL '10 minutes' > ANY (SELECT * FROM public.task);")
            # Check if there are any rows returned
            rows = cursor.fetchall()
            if len(rows) > 0:
                # Send email notification
                email_subject = "Data older than 10 minutes in the public.task table"
                email_message = "Found data older than 10 minutes in the public.task table:\n"
                for row in rows:
                    email_message += str(row) + "\n"

                msg = MIMEText(email_message)
                msg['From'] = email_from
                msg['To'] = email_to
                msg['Subject'] = email_subject

                with smtplib.SMTP(smtp_host, smtp_port) as server:
                    server.starttls()
                    server.login(smtp_username, smtp_password)
                    server.send_message(msg)

                print("Email notification sent successfully.")
            else:
                print("No data older than 10 minutes found in the public.task table.")

            # Close the database connection
            cursor.close()
            conn.close()
            access = 1
        except Exception as e:
            access = 0

        return func.HttpResponse(
            body=f'{{"access":{access}}}',
            status_code=200,
            headers={"Content-Type": "application/json"},
        )
    else:
        return func.HttpResponse(
            body='{"error":"Please pass the required configuration settings"}',
            status_code=400,
        )
