
# Requirement

## install postgres 

On ubuntu, [install postgres on ubuntu](install_pg/install_ubuntu.md)\
On centos, [install postgres on centos](install_pg/install_centos.md)

# Import Stackoverflow database in pg


Original source: http://www.bortzmeyer.org/stackoverflow-to-postgresql.html

The social network Stackoverflow (https://stackoverflow.com/) regularly publishes a dump of its database under a Creative Commons free licence. We can find dump file here:

Main download link: https://archive.org/download/stackexchange

File download link:
https://archive.org/download/stackexchange/stackoverflow.com-Badges.7z
https://archive.org/download/stackexchange/stackoverflow.com-Comments.7z
https://archive.org/download/stackexchange/stackoverflow.com-PostHistory.7z
https://archive.org/download/stackexchange/stackoverflow.com-PostLinks.7z
https://archive.org/download/stackexchange/stackoverflow.com-Posts.7z
https://archive.org/download/stackexchange/stackoverflow.com-Tags.7z
https://archive.org/download/stackexchange/stackoverflow.com-Users.7z
https://archive.org/download/stackexchange/stackoverflow.com-Votes.7z

Extract the XML file of each downloaded file.

At the time of writing this document, the dump files are from June 2017
Each XML file store a class of Stack Overflow objects:

| Object        | 7zip file size    | XML file size | SQL file size (parsed xml)    | Lines in pg table | Pg table size |
|---------------|:----------------: |:-------------:|:--------------------------:   |:-----------------:|--------------:|
| Badges        | 166 MB            | 2.51 GB       | 1.2 GB                        | 22 997 200 lines  | 1 658 MB      |
| Comments      | 3.16 GB           | 14.2 GB       | 10.6 GB                       | 58 187 400 lines  | 11 GB         |
| PostHistory   | 18.2 GB           | 88.9 GB       | 66.4 GB                       | 93 512 900 lines  | 54 GB         |
| PostLinks     | 57 MB             | 492 MB        | 215 MB                        | 4 214 710 lines   | 242 MB        |
| Posts         | 10.4 GB           | 52.3 GB       | 38.1 GB                       | 36 149 100 lines  | 31 GB         |
| Tags          | 704 KB            | 4.18 MB       | 1.67 MB                       | 49 306 lines      | 2 808 kB      |
| Users         | 284 MB            | 2.07 GB       | 753 MB                        | 7 246 590 lines   | 773 MB        |
| Votes         | 757 MB            | 11.7 GB       | 5.23 GB                       | 128 369 000 lines | 5 422 MB      |

## Transform xml file to sql file

The python_src directory contains a python file per xml file to parse. Copy these `*.py` files and `*.xml` dump files in same directory.
> [!NOTE]
> Following code has been tested with Python 3.11.5 with Postgres 14.

To launch the parser
```bash
python so2pg-<xml_to_parse>.py <xml_file>.xml > <parsed_xml>.sql
```
If you want test it, in python directory there is a subdirectory named sample_xml.
```bash
#change to directory python_src
#launch 
python so2pg-badges.py sample_xml > badges.sql

#you obtain a sql file ready to load to postgres
```

## Load sql file in pg database

```bash
# connect to user postgres
sudo su - postgres
# create your database
createdb --encoding=UTF-8 StackOverflow
#create database table in database StackOverflow
psql -U postgres -d StackOverflow -f so-create.sql
```
All you have to do now is to load the sql files.
You can load them in the order you want.
I disable all integrity constraints.
Move to the directory where the sql files are located and start the loads like this

```bash
psql -U postgres -d StackOverflow -f badges.sql
psql -U postgres -d StackOverflow -f comments.sql 
psql -U postgres -d StackOverflow -f posthistory.sql 
psql -U postgres -d StackOverflow -f postlinks.sql 
psql -U postgres -d StackOverflow -f posts.sql 
psql -U postgres -d StackOverflow -f tags.sql 
psql -U postgres -d StackOverflow -f users.sql 
psql -U postgres -d StackOverflow -f votes.sql  
```
