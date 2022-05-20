import azure.functions as func
import psycopg2
import os
import json

def main(req: func.HttpRequest) -> func.HttpResponse:

    host = os.getenv("POSTGRES_HOST")
    user = os.getenv("POSTGRES_USER")
    password = os.getenv("POSTGRES_PASSWORD")

    if host and user and password:
        dbname = "postgres"
        user = f"{user}@{host}"
        sslmode = "require"

        conn_string = "host={0} user={1} dbname={2} password={3} sslmode={4}".format(host, user, dbname, password, sslmode)
        
        try:
            conn = psycopg2.connect(conn_string)
            cursor = conn.cursor()
            cursor.execute("SELECT \
                pid as pid, \
                now() - pg_stat_activity.query_start AS duration, \
                query, \
                state \
                FROM pg_stat_activity \
                WHERE (now() - pg_stat_activity.query_start) > interval '1 minutes' \
                AND state != 'idle' AND query NOT LIKE 'START_REPLICATION%';")
            rows = cursor.fetchall()
            cursor.close()
            conn.close()
            rowarray_list = []
            for row in rows:
                t = (row[0], row[1], row[2], row[3])
                rowarray_list.append(t)
            j = json.dumps(rowarray_list, indent=4, sort_keys=True, default=str)
            queries = j
        except Exception as e:
            queries = str(e)

        return func.HttpResponse(
            body=f'{{"queries":{queries}}}',
            status_code=200,
            headers={"Content-Type": "application/json"},
        )
    else:
        return func.HttpResponse(
            body='{"error":"Please add correct configuration settings"}',
            status_code=400,
        )
