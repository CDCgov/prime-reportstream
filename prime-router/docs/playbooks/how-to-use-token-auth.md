##How to use token auth.

1. The SENDER generate a new keypair

openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
openssl ec -in my-es-keypair.pem -pubout -out  my-es-public-key.pem

2.  the REPORTSTREAM ONBOARDING MANAGER stores it in ReportStream, based on trust relationship

./prime sender addkey --public-key ./my-es-public-key.pem  --scope waters.default.report --name waters.default 
./prime sender addkey --public-key ./my-es-public-key.pem  --scope waters.default.report --name waters.default --doit
./prime sender get --name waters.default

./prime sender addkey --public-key ./my-es-public-key.pem  --scope ignore.ignore-waters.report --name ignore.ignore-waters
./prime sender addkey --public-key ./my-es-public-key.pem  --scope ignore.ignore-waters.report --name ignore.ignore-waters --doit
./prime sender get --name ignore.ignore-waters

3. The SENDER requests a token

./prime sender reqtoken --private-key my-es-keypair.pem --scope waters.default.report --name waters.default

4.  The SENDER uses that token to send a report:

curl -H "authorization:bearer ???" -H "client:waters"  -H "content-type:text/csv" --data-binary "@./junk/waters.csv" "http://localhost:7071/api/report"


