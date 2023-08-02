**File Name – build_frontend.yml**

There are 2 events that trigger the workflow on push to master or pull request to master or production. The virtual environment used to run the workflow is an Ubuntu machine in the latest version.
&nbsp;  
**Step1: run yarn install --ignore-platform**
Installs the dependencies required to run the application:
&nbsp;  
**Step 2: yarn test:ci**
***        yarn build:production**
Runs the test and builds the node app if the environment is prod. 
&nbsp;  
**Step 3: yarn lint**
**       yarn test:ci**
**       yarn build:$ENV**
Enables linting and runs test and build the app for all non-prod branches.
&nbsp;  
**Step 4: Sonarcloud scan**
Runs the code through code analysis scans using sonar cloud.
&nbsp;  
**Step 5: Tar frontend files**
This step packages the build output into a tar file
&nbsp;  
**Step 6: Upload frontend artifact**
Uploads the package generated from previous step into the GitHub workflow that can be used for deployment.

