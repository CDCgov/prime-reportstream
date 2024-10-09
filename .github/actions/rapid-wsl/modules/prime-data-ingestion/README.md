# [prime-data-ingestion](https://github.com/CDCgov/prime-data-ingestion)

```sh
d="Ubuntu-18.04"; # Distro name
m="prime-data-ingestion"; # Module name
u="primeuser"; # Username

./rwsl init -d $d -m $m -u $u;
./rwsl status -d $d -m $m -u $u;
./rwsl backup -d $d -m $m -u $u;
./rwsl down -d $d -m $m -u $u;
./rwsl up -d $d -m $m -u $u;
./rwsl destroy -d $d -m $m -u $u;
./rwsl restore -d $d -m $m -u $u;
./rwsl test -d $d -m $m -u $u;

```
---
