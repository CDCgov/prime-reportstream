# Install postgresql from source on Centos 7

For example, I want to install a postgresql 9.6.3 (pg) database on Centos 7 \
Pg will be installed in /opt/pgsql/9.6 \
My data (PGDATA) will be in /opt/pgsql/9.6/data \
My Logs (PGLOG) will be in /opt/pgsql/9.6/logs

### Step 1: Download the sources of postgresql
Download link: https://ftp.postgresql.org/pub/source/v9.6.3/postgresql-9.6.3.tar.bz2

### Step 2: Install the required pacquets
```bash
yum install -y bison-devel readline-devel zlib-devel openssl-devel
yum groupinstall -y 'Development Tools'
```

### Step 3: Creating the postgres user with for homedir /opt/pgsql
```bash
sudo useradd -d /opt/pgsql -m -r -s /bin/bash postgres
```

### Step 4: Move postgres sources to /opt/pgsql/src
```bash
sudo mkdir -p /opt/pgsql/src
sudo mv postgresql-9.6.3.tar.bz2 /opt/pgsql/src/postgresql-9.6.3.tar.bz2
sudo chown -R postgres: postgres /opt/pgsql/
```
### Step 5: Connect with postgres user
```bash
sudo su - postgres
```

### Step 6: Export the PATH, LD\_LIBRARY, PGDATA, PGLOG variables to .bashrc_profile
```bash
#config postgres
export PATH = /opt/pgsql/9.6/bin: $PATH
export LD_LIBRARY_PATH = /opt/pgsql/9.6/lib: $LD_LIBRARY_PATH
export PGDATA = /opt/pgsql/9.6/data
export PDLOG = /opt/pgsql/9.6/data/serverlog
```
> You can add these lines to your .bashrc_profile

### Step 7: Unpack the postgresql source
```bash
cd src/
tar -xvjf postgresql-9.6.3.tar.bz2
```

### Step 8: Install pg from sources
```bash
cd postgresql-9.6.3/
./configure --prefix /opt/pgsql/9.6
make
make install
```

### Step 9: Initialize the pg database
```bash
initdb -D $PGDATA -U postgres
```

### Step 10: Service settings for start pg on boot

```bash
#with an account with a root/sudoer
sudo cp ~postgres/src/postgresql-9.6.3/contrib/start-scripts/linux /etc/init.d/postgresql
sudo chmod +x /etc/init.d/postgresql
```
Modify the prefix, PGDATA, PGUSER and PGLOG variables in /etc/init.d/postgresql:
```bash
# Installation prefix
prefix = /opt/pgsql/9.6

# Data directory
PGDATA = "/opt/pgsql/9.6/data"

# Who to run the postmaster as, usually "postgres". (NOT "root")
PGUSER = postgres

# Where to keep a log file
PGLOG = "$PGDATA/ServerLog"
```

### Step 11: Starting the service
```bash
sudo chkconfig postgresql
sudo service postgresql start
```

### Step 12: Test Connect to Base
```bash
sudo su - postgres
psql
```
> If you add this
> `export PATH = /opt/pgsql/9.6/bin: $PATH`
> In your .bash_profile, you can run the psql command without having to connect to the user system postgres
> By running the following command:
> `Psql -U postgres`

### Step 13 (optional): Installing pgadmin4
```bash
sudo yum install https://download.postgresql.org/pub/repos/yum/9.6/redhat/rhel-7-x86_64/pgdg-centos96-9.6-3.noarch.rpm
sudo yum install -y pgadmin4
```