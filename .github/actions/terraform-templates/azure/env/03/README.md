# [PostgreSQL/Citus Migrate Production Data](https://docs.citusdata.com/en/stable/develop/migration_data.html#migrate-production-data)

## [Big Database Migration](https://docs.citusdata.com/en/stable/develop/migration_data_big.html#big-database-migration)

> ⚠️Recommends support request⚠️

## [Small Database Migration](https://docs.citusdata.com/en/stable/develop/migration_data_small.html#small-database-migration)

> No support request

### 1. Set variables

```sh {"id":"01HXCRAJCEMCY0AF3403N8NGVJ"}
export PGPASSWORD="<password>"

host="<cluster name>.postgres.cosmos.azure.com"
user="citus"
db="citus"
schema="public"
```

<details>
   <summary>Optional: create test table</summary>

```sql {"id":"01HXCRAJCEMCY0AF3405CYZXX0"}
create table public.test (name varchar)
insert into public.test (name) values ('test')
```

</details>

### 2. Dump schema

```sh {"id":"01HXCRAJCEMCY0AF3406ARMBC1"}
pg_dump \
--format=plain \
--no-owner \
--schema-only \
--file=/mnt/storage/$schema.sql \
--schema=$schema \
postgres://$user@$host:5432/$db
```

### 3. Dump data

```sh {"id":"01HXCRAJCEMCY0AF3407N1BHNM"}
pg_dump \
-Z 0 \
--format=custom \
--no-owner \
--data-only \
--file=/mnt/storage/${schema}data.dump \
--schema=$schema \
postgres://$user@$host:5432/$db
```

<details>
   <summary>Dump slow?</summary>

### Table bloats and vacuuming

High `dead_pct` (dead tubles percentage) indicates the table may need to be vacuumed:

```sql {"id":"01HXCRAJCEMCY0AF340AX8TQQ7"}
select 
schemaname,
relname as table_name,
n_dead_tup,
n_live_tup,
round(n_dead_tup::float/n_live_tup::float*100) dead_pct,
autovacuum_count,
last_vacuum,
last_autovacuum,
last_autoanalyze,
last_analyze 
from pg_stat_all_tables 
where n_live_tup >0 
order by round(n_dead_tup::float/n_live_tup::float*100) desc;
```

Vacuum:

```sql {"id":"01HXCRAJCEMCY0AF340CJ0QBCK"}
vacuum(analyze, verbose) <table_name>
```

### Reduce network bottlenecks

For Azure, create a [Container Instance](https://azure.microsoft.com/en-us/products/container-instances) with a mounted storage account file share. Install a version of PostgreSQL that matches the target server. Open the IP of the container instance on the PostgreSQL server. Run `pg_dump` on the container instance with the file targeting the storage account mount.

</details>

<details>
   <summary>Optional: drop test table</summary>

```sql {"id":"01HXCRAJCEMCY0AF340DZNSZJ4"}
drop table public.test;
```

</details>

### 4. Restore schema

```sh {"id":"01HXCRAJCEMCY0AF340HGJYP00"}
psql \
-h $host \
-d $db \
-U $user \
-f /mnt/storage/$schema.sql
```

### 5. (Citus) Run configuration functions

* Run your [create_distributed_table](https://docs.citusdata.com/en/stable/develop/api_udf.html#create-distributed-table) and [create_reference_table](https://docs.citusdata.com/en/stable/develop/api_udf.html#create-reference-table) statements. If you get an error about foreign keys, it’s generally due to the order of operations. Drop foreign keys before distributing tables and then re-add them.
* Put the application into maintenance mode, and disable any other writes to the old database.

### 6. Restore data

```sh {"id":"01HXCRAJCEMCY0AF340HXZ9BJY"}
pg_restore  \
--host=$host \
--dbname=$db \
--username=$user \
/mnt/storage/${schema}data.dump
```

<details>
   <summary>Example: SQL file restore</summary>

```sh {"id":"01HXCRAJCEMCY0AF340NX40Q7C"}
psql "host=$host port=5432 dbname=$db user=$user sslmode=require password=$PGPASSWORD" < /mnt/storage/posthistory.sql
```

</details>
<details>
   <summary>Optional: select test table</summary>

```sql {"id":"01HXCRAJCEMCY0AF340QGP2J3K"}
select * from public.test limit 1;
```

</details>