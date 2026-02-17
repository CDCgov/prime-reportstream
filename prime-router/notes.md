# LDE Validation

1. Run `./validate-secure-multiarch.sh --clean` success
2. Run `./validate-secure-multiarch.sh --use-container` success
   1. Everything looks good
   2. Output questions/warnings: (see log2.md)
      1. WARN: Azurite health check timeout - continuing anyway
      2. WARN: Orphan containers detected (1 containers from previous runs)
      3. API is running on port 7071
         1. INFO: Test endpoints:
            curl http://127.0.0.1:7071/api/lookuptables/list - This one seems to work
            curl http://127.0.0.1:7071/api - This one returns 404 not foun
            (This 127.0.0.1 page can’t be found - No webpage was found for the web address: http://127.0.0.1:7071/api - HTTP ERROR 404)  
3. Run `./validate-secure-multiarch.sh --fast` build fails due to unit tests failing (see log2.md)
```
BUILD FAILED in 50s
25 actionable tasks: 4 executed, 21 up-to-date
WARN: Some unit tests failed
```
4. Run `cd .. && ./gradlew :prime-router:quickRun` build failed with an exception (see log3.md)
   1. > Task :prime-router:composeUp FAILED
   2. Error response from daemon: failed to set up container networking: driver failed programming external connectivity on endpoint prime-router-azurite-1 (388ef5149905e7ac82856660d411b70ce55555866eb33f8a1ba9cfeee76b2bd6): Bind for 127.0.0.1:10000 failed: port is already allocated
5. Run recommended cleanup + restart Docker:
```
INFO: Recommended cleanup:
  docker-compose -f docker-compose.secure-multiarch.yml down --remove-orphans
  ./validate-secure-multiarch.sh --clean
```
Full cleanup completed - all infrastructure stopped
6. Run `./validate-secure-multiarch.sh --use-container` again (see log4.md)
   1. Output seems to be the same
      1. WARN: Azurite health check timeout - continuing anyway
      2. WARN: Orphan containers detected (1 containers from previous runs)
7. Run `./validate-secure-multiarch.sh` Cleanup and run again without --use-container (see log5.md)
   1. Same issues, only this time I don't see orphan conatiners warning but I still see the Azurite health check warning
      1. WARN: Azurite health check timeout - continuing anyway
8. Run `./validate-secure-multiarch.sh --fast` build fails again due to unit tests failing (see log6.md)
9. Run  `cd .. && ./gradlew :prime-router:quickRun` fails again (see log7.md)
10. VS Code Docker container plugin shows two containers are "unhealthy"
    1.  hashicorp/vaild:latest rs-vault (unhealthy) (see log8.md)
    2.  mcr.microsoft.com/azure-storage/azurite:3.34.0 rs-azurite (unhealthy) (see log9.md)
