# Save password
export PGPASSWORD='<password>'

# Backup Database
pg_dump --help

pg_dump -Fc -h localhost -U postgres DBA -f DBA.dump
pg_dump -Fc -h localhost -U postgres StackOverflow -f StackOverflow-Backup-20231214.dump -Z 9


# Restore Database
pg_restore --help

# connect, and create a database
psql -h localhost -d postgres -U postgres
create database DBA2 with owner = 'postgres';

# Restore database content from dump
pg_restore -d DBA2 -h localhost -U postgres DBA.dump

