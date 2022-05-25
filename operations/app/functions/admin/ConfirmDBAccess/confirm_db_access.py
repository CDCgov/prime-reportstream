import azure.functions as func
import psycopg2
import os

def main(req: func.HttpRequest) -> func.HttpResponse:

    host = os.getenv("POSTGRES_HOST")
    db = req.params.get("db")
    user = os.getenv("POSTGRES_USER")
    password = os.getenv("POSTGRES_PASSWORD")

    if host and db and user and password:
        user = f"{user}@{host}"
        sslmode = "require"

        conn_string = "host={0} user={1} dbname={2} password={3} sslmode={4}".format(host, user, db, password, sslmode)
        
        try:
            conn = psycopg2.connect(conn_string)
            cursor = conn.cursor()
            cursor.execute("SELECT current_database();")
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
