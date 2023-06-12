#import azure.functions as func
import psycopg2
import os
from datetime import datetime, timedelta

def main(req: func.HttpRequest) -> func.HttpResponse:

    host = os.getenv("POSTGRES_HOST")
    db = "prime_data_hub"
    user = os.getenv("POSTGRES_USER")
    password = os.getenv("POSTGRES_PASSWORD")

    if host and db and user and password:
        user = f"{user}@{host}"
        sslmode = "require"

        conn_string = "host={0} user={1} dbname={2} password={3} sslmode={4}".format(host, user, db, password, sslmode)
        
        try:
            conn = psycopg2.connect(conn_string)
            cursor = conn.cursor()

            cursor.execute("SELECT * FROM public.task WHERE NOW() - INTERVAL '10 minutes' > ANY (SELECT * FROM public.task);")
            result = cursor.fetchall()

            # Set the data_found output based on the query result
            data_found = 'true' if result else 'false'
            print(f"::set-output name=data_found::{data_found}")
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
            body='{"error":"Please pass a db name on the query string and add correct configuration settings"}',
            status_code=400,
        )


