# Below worked for me
export PGPASSWORD='<password>'
psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./badges.sql # done

psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./comments.sql # done
psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./posthistory.sql # done

psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./postlinks.sql # done

psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./posts.sql # done
psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./users.sql # done
psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./tags.sql # done
psql -h localhost -d StackOverflow -U postgres -p 5432 -a -q -f ./votes.sql # done


psql -U postgres -d StackOverflow -f comments.sql
psql -U postgres -d StackOverflow -f posthistory.sql
psql -U postgres -d StackOverflow -f postlinks.sql
psql -U postgres -d StackOverflow -f posts.sql
psql -U postgres -d StackOverflow -f tags.sql
psql -U postgres -d StackOverflow -f users.sql
psql -U postgres -d StackOverflow -f votes.sql