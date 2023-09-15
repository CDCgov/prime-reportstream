# How to Onboard a New Organization to Receive Data
Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md))

## Welcome

Our goal is to onboard as many states and local jurisdictions as we can, to receive Hub data!
This is our internal documentation for how we _currently_ do that onboarding work.


The main goal of receiver onboarding is to establish a connection to send data from ReportStream to the STLT.  
In order to do so there are multiple receiver configurations that need to be configured.  
During pre-onboarding ideally we will be able to know/collect/obtain all the unknown variables in order to set up the receiver configurations.

## Pre-Onboarding
* Identify how they want to receive data
* Determine if we need to be whitelisted prior to connecting
* Identify STLT Specific (HL7) values (e.g. MSH-5, MSH-6, etc)
* Determine if they need/want specific data quality or condition filters
* Determine how they want to receive AoE questions
* Create Okta Accounts (probably once they are set up fully).

## Table of Contents
1.    Set up new organization
2.    Set up new schema
3.    Generate  test data
4.    Test and commit, and deploy to Test and maybe Prod
5.    Testing in your Docker container
6.    Create access to the Download site
7.    Validation in Prod
8.    Set up transport SSH key for receiver using SFTP

### 1. Set up new organization
* Create a new branch in git for your changes.
* Create a new organization for the State, (canonical style: `lt-phd`), in organizations.yml, which is used by your local 
commandline ./prime cli tool.
* Follow the pattern of another existing organization.  Carefully set the initial 
jurisdiction-filter so that data is limited to that state.  (The jurisdiction: STATE and  stateCode: XY  fields should 
soon provide better enforcement of this)
* The new organization must have at least one `receiver` defined, but the `receiver` does not need to have a transport 
defined - the download site can be used until an automated delivery mechanism is set up.
* There are two fields that will be used as "keys" to further work, below.   The `-name` of the organization 
(eg, `lt-phd`) will be used as the Okta `group`, and the `translation.schemaName:` value (eg, `metadata/hl7_mapping/ORU_R01/ORU_R01-base`) will be 
used as the schema name in the next step.
* Below is an example of the organization file
  
```yaml
- name: lt-pdh
  description: LT Department of Health
  jurisdiction: STATE
  stateCode: LT
  receivers:
    - name: full-elr
      topic: full-elr
      jurisdictionalFilter: [ "(%performerState.exists() and %performerState = 'LT') or (%patientState.exists() and %patientState = 'LT')" ]
      translation: !<HL7>
        schemaName: "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        type: HL7
        useBatchHeaders: true
        receivingApplicationName: LT-PDH
        receivingApplicationOID:
        receivingFacilityName: LT-PDH
        receivingFacilityOID:
```
* In the above example, the jurisdictional filter searches the `ordering_facility_state` field in the report for anything 
that matches the code LT.
* Filters can be applied to the organization or receiver. For more information on filters see: 
(https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/playbooks/how-to-use-filters.md)
* In addition, there is the translation section, which specifies the output format that will be sent to the receiver. 
Currently, we have three formats available:
    - HL7
    - CSV
    - FHIR
* The quality filters verify that test results have the minimum fields required by most public health jurisdictions. 
* Those fields are made adjustable to be customized per jurisdictional specifications.
* These filters are applied by default to the receiver and set in code.
* They do not need to be added to the receiver setting.
* If a custom rule is added to the quality filter for a receiver, all default quality filters will be ignored. The default filter includes 
these requirements:
- The following fields all have a value:
     - Patient First Name
     - Patient Last Name
     - Patient DOB
     - Test Result
     - Specimen Type
     - Specimen ID/Accession Number
     - Device Identifier
    
- At least one of the following fields has a value:
     - Patient Street Address
     - Patient Zip Code
     - Patient Phone
     - Patient Email
    
- At least one of the following date fields has a valid date:
     - Test Ordered Date
     - Specimen Collection Date
     - Test Result Date
    
- The following fields are 10 characters in length:
     - Testing Lab CLIA
     - Ordering Facility CLIA
    
- Processing Code does not equal T nor D

- Only test results that pass all the above requirements will be transferred to the jurisdiction. There is only one option 
for jurisdictions that want all results reported.
- Create a secondary feed with the reverseQualityFilter() set to true. This will only allow results that fail the 
quality filters listed above.

The mechanism for how each record is translated is laid out in the schema, which is discussed in the next section.

### 2. Set up a new schema
*NOTE - IF YOU ARE WORKING ON AN HL7 RECEIVER, YOU DO NOT NEED TO CREATE A NEW SCHEMA.*

* By default, any HL7 receiver will use the universal or covid schema and you do not need to create a schema
specific to your receiver.
* In the UP for HL7 v2 we have to check what HL7 message type they want to receive data in. We support ADT_A01, OML_O21 and ORU_R01. Depending on the message type we can set translationSchema to the respective schema.
* If they are going to receive a FHIR file you *MUST* create a schema.(CSV file is not currently supported in UP)
* If the receiver wants specific receiver transforms that are not supported by the translation settings a schema can be created for them. More information on how to mange translation schemas can be found here (https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/universal-pipeline/translate.md)


### 3. Generate test data
* Generate fake, or better, synthesized test data. Prime has two ways to generate anonymous fake data (Fake or Syntheic data):
1. Fake data - Fake data uses a library to generate purely fake data for ALL data points except for the city, state, 
postal code, and county, which are tied to actual locations. The data that is generated is somewhat constrained to 
resemble reasonable defaults, but is designed to be very random, which allows us to test the limits of validation 
systems. *THERE IS NO PII OR PHI IN FAKE DATA*
2. Synthetic data - Synthetic data takes a file of actual clinical results and does a combination of shuffling some 
PHI/PII and faking other data points, so the records cannot be traced back to the patient, but the actual portion of 
positive to negative tests, lab names & CLIA's, names of ordering providers, etc will be actual valid information. The 
goal is to provide a higher-quality, less-random, dataset that can then be used to validate the information being sent 
from PRIME to the receivers. *While great care has been taken to ensure we do not leak PII/PHI, this should not be used 
except with receivers we are in the process of onboarding.*


### 4. Test and commit, and deploy to Test and maybe Prod

* Test locally using the above fake data.
* Once you've got the kinks out of the organizations.yml, carefully update settings in the staging environment. 
* `./prime multiple-settings set --help`
* Create a PR for the change, review, and push. The review is a good chance for someone to doublecheck the filters.
* It should deploy to staging automagically once the PR is approved and merged into master.
* Test again in Staging
* If you are ready, carefully update settings in the prod environment. Especially in production, check the batch 
timing. NOT every minute, eh?
* If needed, push to production following our procedures for doing that.

### 5. Testing in your Docker container

* Another important step to take when onboarding a receiver is to start the docker container and then submit a file to 
the container and make sure that it translates and routes correctly.

* Ensure that the transport is set to the default sftp:
```
transport:
    type: SFTP
    host: sftp
    port: 22
    filePath: ./upload
    credentialName: DEFAULT-SFTP
```
* First build the solution:

`./gradlew clean package`

* Then start the docker container:

`docker-compose up`

* Next, load the new organization and schema into the local DB using the below command:

`./gradlew reloadSettings`

* NOTE: If developing on an Apple Mac with a Silicon chip please follow our guide in place of the above three commands:

- [Using Apple Silicon Macs for Development](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/getting-started/Using-an-apple-silicon-mac.md)


* At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: simple_report' -H 'Content-Type: application/hl7-v2' --data-binary '@/Path/to/test/file.hl7' 'http://localhost:7071/api/reports'
```
* You will then see a report of the result of your post to the local container.  After a few minutes, you can view the 
output here: `/prime-router/build/sftp`

### 6. Create access to the Download site

* If the organization has elected for download access, set up an Okta account.
* If you are testing in Test, obviously you'll need to set up access to that download site.

### 7. Validation in Prod

* Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data 
(yes, it appears many PHDs test using real data.)
* At this point you should be able to send data through to the customer, and they can validate.
* You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
* Customer from **LT** should be able to go to the download site and pull down data.

### 8. How to Create and Manage a Key for a Receiver
#### SFTP 
#### Introduction
This is a introduction on how to create and manage a public/private key pair for a receiver using SSH Keys.
#### Assumptions
* First and foremost: This assumes you have credentials with the CDC and have been given access to the Azure portal. If you do not have credentials, and access to the Azure portal, stop here. You cannot go any further.
* This also assumes you have access to KeyBase, and are part of the prime_dev_ops team there.
* Also, we assume you have KeyBase installed, and have in mounted into /Volumes/. If you don't have it mounted as a drive, you can just drag and drop the files into KeyBase. If you feel you should have access to the prime_dev_ops team in KeyBase, contact your dev lead, or the dev ops team lead and request access.
* Finally, this assumes you are working on a Unix-style system such as Linux or MacOS. You can probably do this on Windows as well provided you have a conformant shell with putty and openssl installed. The Git Bash shell would probably do, or the Linux Subsystem for Windows would as well, but no promises are made.
* Have an active Okta admin account

#### Background
Most of the states that we partner with are using SFTP to send files. (90% of Receivers use this). If your receiver is using SOAP, REST, or Azure Blob, stop here. There will be more documentation on how to use these transport methods.

#### Steps
Steps on generating the keys you need and then assigning them to the receiver, and sharing them with a receiver
1. Creating the private key in Azure
2. Preparing the key for use.

#### Step One - Generating the Private Key in Azure. 
1.    Open your browser and navigate to the Azure Portal at [https://portal.azure.com/#home](https://portal.azure.com/#home)
2.    At the top you should see an option for SSH Keys. If you don't, you can also type "SSH" into the search bar at the top to find it.
3.    When it loads, you will likely not see any created keys. Make sure that you click the Subscription filter and select all. Once you do that, you will see all the SSH keys that have been created
4.    In the upper left, click "New". You will be taken to this screen. ![](assets/receiver-ssh-key/2-CreateSSHKeyScreen.png)
5.    On the next screen, select the DMZ subscription, and under "Resource Group" select Prod. ![](assets/receiver-ssh-key/3-SelectResourceGroup.png)
6.    You're prompted to name the SSH key pair. Our old naming convention is DH + the state initials + "_phd" but now, you really don’t need to follow this naming convention.  So, for the example below,with       NJ, it would be "DHnj_phd". ![](assets/receiver-ssh-key/4-NameSSHKey.png)
7.    Leave the last option on "Generate new key pair"
8.    Click the "Next: Tags" button
9.    On this screen, you can associate tags with the key you're creating. Under "Name" type "state", and under "Value" enter the two letter abbreviation for the state.
10.   Click "Next". At this point Azure will validate your options and then be prepared to create your key for you. ![](assets/receiver-ssh-key/5-AddStateTag.png)
11.   You will be prompted to download the private key and create the resource. This is your ONLY chance to download this key. You MUST download it here or you will have to recreate the key again. ![](assets/receiver-ssh-key/7-DownloadConfirmation.png)
12.   Once you download the key copy it to the folder where you are going to work with it.

You will be redirected to the screen that lists the SSH keys we have created. You will get a confirmation message in the upper right that the resources has been created, but you will not immediately see the key you created. You can click "Refresh" to see the key you generated. ![](assets/receiver-ssh-key/8-CreationConfirmation.png)

Download the keys to your Downloads folder. The naming convention folder will be something like “Keys->State”
3 files will be downloaded. “State.pub”, “State.ppk” and “State.pem”

#### Step Two: Preparing the Key for Use
Below are the steps to prepare the key for use by ReportStream, and by our receivers. You should have a folder somewhere where you are working with the keys. In my case, I've created a local folder called keys, and then a folder in there for each state.

1.    You will need to send the PUB to the Receiver
2.    Once you've created and downloaded the .pub file, you send it to the receiver so they can create credentials for you, and assign the public key to those credentials so you can log in. There is no danger in sharing the pub file contents with the receiver, and sending it via email. Your public key is just that, public, and anyone can access it without posing a security risk.

#### Creating the Connection Info YML File
Once the receiver has created credentials for you, and assigned your public key to it, they will share the credentials with you, and we need to store the credentials in KeyBase so they're available to the whole team.

In KeyBase we have connection info files that have one of two formats, depending on whether the file describes SFTP password authentication, or SFTP key-based authentication. As we're dealing with key-based authentication, we'll focus on that here.

#### Here is the basic format of the yaml file.

```yaml
---
- state: NJ
  sftp:
    - host: # the host name provided by the receiver
      port: # the port to connect to
      path: # provided by the receiver (the file path we write to)
      userName: # provided by the receiver
      password: # not used
      ppkPassword: # the password generated above
      ppkFileName: # the file name generated above
      ppkFileContents: # the pub key file contents (or the putty file contents)
```
        
```sh
# create a connection info file in yml format and then copy that into keybase
cp ~/development/nj-connection-info.yml /Volumes/Keybase/team/prime_dev_ops/state_info/NJ/ 
```
Or just manually upload the yml file into the respective keybase folder

#### Testing Locally
1.    Once you've created the files and have credentials from the receiver, you want to test and make sure that your configuration is successful and the
2.    One SFTP program you can use to test is Cyberduck. You can download it from the AppStore or directly from cyberduck.com.
3.    Once you have downloaded the app, click on Open connection.
4.    Dropdown to the SFTP transfer. 
5.    The STLT will provide you with the servername and username. 
6.    Not all STLT will provide you the password. But that is okay.
7.    If they don’t provide you with the password, that is okay. Leave it blank.
8.    Goto SSH PrivateKey, and choose the PEM file.
9.    You then will get either a “successful connection” message.
10.    Goto www.reportstream.cdc.gov ->PrimeAdmin (upper right corner) .
11.    Click edit on the organization you are working on.
12.    Then click on Check and if you are successful, you will get a 200 success message









