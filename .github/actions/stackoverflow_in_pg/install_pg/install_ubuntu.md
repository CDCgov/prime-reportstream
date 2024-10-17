# Install Postgresql from source on Ubuntu 16.04

For example, I want to install a postgresql 9.6.3 (pg) database on xubuntu 16.04.
Pg will be installed in /opt/pgsql/9.6 \
My data (PGDATA) will be in /opt/pgsql/9.6/data \
My Logs (PGLOG) will be in /opt/pgsql/9.6/logs

### Step 1: Download the sources of postgresql
Download link: https://ftp.postgresql.org/pub/source/v9.6.3/postgresql-9.6.3.tar.bz2

### Step 2: Install the required packages
```Bash
sudo apt install -y build-essential libreadline-dev zlib1g-dev flex bison libxml2-dev libxslt-dev libssl-dev libsystemd-dev
```

### Step 3: Creating the postgres user with for homedir /opt/pgsql
```Bash
sudo useradd -d /opt/pgsql -m -r -s /bin/bash postgres
```

### Step 4: Move postgres sources to /opt/pgsql/src
```Bash
sudo mkdir -p /opt/pgsql/src
sudo mv postgresql-9.6.3.tar.bz2 /opt/pgsql/src/postgresql-9.6.3.tar.bz2
sudo chown -R postgres:postgres /opt/pgsql/
```
### Step 5: Connect with postgres user
```Bash
sudo su - postgres
```

### Step 6: Export the PATH, LD\_LIBRARY, PGDATA, PGLOG variables to .bashrc
```Bash
#config postgres
export PATH=/opt/pgsql/9.6/bin:$PATH
export LD_LIBRARY_PATH=/opt/pgsql/9.6/lib:$LD_LIBRARY_PATH
export PGDATA=/opt/pgsql/9.6/data
export PDLOG=/opt/pgsql/9.6/data/serverlog
```
> You can add these lines to your .bashrc

### Step 7: Uncompress the postgresql source
```Bash
cd src/
tar -xvjf postgresql-9.6.3.tar.bz2
```

### Step 8: Install pg from sources
```Bash
cd postgresql-9.6.3/
./configure --prefix /opt/pgsql/9.6 --with-systemd
make
make install
```

### Step 9: Initialize the pg database
```Bash
initdb -D $PGDATA -U postgres
```

### Step 10: Service settings for start pg on boot

```Bash
#with an account with a root/sudoer
sudo cp ~postgres/src/postgresql-9.6.3/contrib/start-scripts/linux /etc/init.d/postgresql
sudo chmod +x /etc/init.d/postgresql
```
Modify the prefix, PGDATA, PGUSER and PGLOG variables in /etc/init.d/postgresql:
```Bash
# Installation prefix
prefix=/opt/pgsql/9.6

# Data directory
PGDATA="/opt/pgsql/9.6/data"

# Who to run the postmaster as, usually "postgres". (NOT "root")
PGUSER=postgres

# Where to keep a log file
PGLOG="$PGDATA/serverLog"
```

### Step 11: Start the Service
```Bash
sudo update-rc.d postgresql defaults
sudo systemctl start postgresql
```

### Step 12: Test connection to Base
```Bash
sudo su - postgres
psql
```
> If you add this
> `export PATH = / opt / pgsql / 9.6 / bin: $ PATH`
> In your .bashrc, you can run the psql command without having to log on to the user system postgres
> By running the following command:
> `Psql -U postgres`
