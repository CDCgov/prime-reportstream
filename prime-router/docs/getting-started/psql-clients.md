# PostgreSQL Client Tools

There are a variety of options to interact with your PostgreSQL instance:

* DBeaver Community Edition can be [downloaded here](https://dbeaver.io/download/). It is a locally installed, graphical tool that lets you connect to a variety of database systems, including PostgreSQL with [very extensive documentation wiki](https://github.com/dbeaver/dbeaver/wiki).
* pgAdmin: if you want to install pgAdmin, we recomment running it as a docker container inside the `prime-router_build` network so that it can talk to the `postgresql` container.
* `psql`: the command-line tool for text-based interactions with PostgreSQL

    ```
    $ psql --host localhost --dbname prime_data_hub --user prime
    Password for user prime:
    psql (13.3 (Ubuntu 13.3-0ubuntu0.21.04.1), server 11.12 (Debian 11.12-1.pgdg90+1))
    Type "help" for help.

    prime_data_hub=# select * from report_file ;
    prime_data_hub=# select count(1) from report_file ;
    count
    -------
    108
    (1 row)
    ```
