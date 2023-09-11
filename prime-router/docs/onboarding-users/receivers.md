# How to Onboard a New Organization to Receive Data
Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md))

## Receiver Configuration

- Transport Protocols
  - SFTP
    - SSH Keys
  - Azure Blob
  - REST
  - SOAP
 
The main goal of receiver onboarding is to establish a way (connection?) to send data from ReportStream to the STLT.  In order to do so there are multiple receiver configurations that need to be “setup’?  During pre-onboarding ideally we will be able to know/collect/obtain all the unknown variables in order to set up the receiver configurations.

Pre-Onboarding
Identify how they want to send data
Identify how they want to receive data
Determine if we need to be whitelisted prior to connecting
Identify STLT Specific (HL7) values (e.g. MSH-5, MSH-6, etc)
Determine if they need/want specific data quality filters
Determine how they want to receive AoE questions

Create Okta Accounts (probably once they are set up fully).

SFTP
(90% of STLTs use this)
SSH Keys
Assumptions
First and foremost: This assumes you have credentials with the CDC and have been given access to the Azure portal. If you do not have credentials, and access to the Azure portal, stop here. You cannot go any further.
This also assumes you have access to KeyBase, and are part of the prime_dev_ops team there. Also, we assume you have KeyBase installed, and have in mounted into /Volumes/. If you don't have it mounted as a drive, you can just drag and drop the files into KeyBase. If you feel you should have access to the prime_dev_ops team in KeyBase, contact your dev lead, or the dev ops team lead and request access.
Finally, this assumes you are working on a Unix-style system such as Linux or MacOS. You can probably do this on Windows as well provided you have a conformant shell with putty and openssl installed. The Git Bash shell would probably do, or the Linux Subsystem for Windows would as well, but no promises are made.
Steps on generating the keys you need and then assigning them to the receiver, and sharing them with a receiver
Creating the private key in Azure
Step One - Generating the Private Key in Azure. 
Open your browser and navigate to the Azure Portal at https://portal.azure.com/#home
At the top you should see an option for SSH Keys. If you don't, you can also type "SSH" into the search bar at the top to find it.
When it loads, you will likely not see any created keys. Make sure that you click the Subscription filter and select all. Once you do that, you will see all the SSH keys that have been created

In the upper left, click "New". You will be taken to this screen.

On the next screen, select the DMZ subscription, and under "Resource Group" select Prod.




