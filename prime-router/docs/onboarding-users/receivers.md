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

You're prompted to name the SSH key pair. Our old naming convention is DH + the state initials + "_phd" but now, you really don’t need to follow this naming convention.  So, for the example below, with NJ, it would be "DHnj_phd". 

Leave the last option on "Generate new key pair"
Click the "Next: Tags" button
On this screen, you can associate tags with the key you're creating. Under "Name" type "state", and under "Value" enter the two letter abbreviation for the state.
Click "Next". At this point Azure will validate your options and then be prepared to create your key for you.

You will be prompted to download the private key and create the resource. This is your ONLY chance to download this key. You MUST download it here or you will have to recreate the key again.
Once you download the key copy it to the folder where you are going to work with it.
You will be redirected to the screen that lists the SSH keys we have created. You will get a confirmation message in the upper right that the resources has been created, but you will not immediately see the key you created. You can click "Refresh" to see the key you generated.

Download the keys to your Downloads folder. The naming convention folder will be something like “Keys->State”
3 files will be downloaded. “State.pub”, “State.ppk” and “State.pem”

Step Two: Preparing the Key for Use

Below are the steps to prepare the key for use by ReportStream, and by our receivers. You should have a folder somewhere where you are working with the keys. In my case, I've created a local folder called keys, and then a folder in there for each state.


Sending the PUB to the Receiver
Once you've created and downloaded the .pub file, you send it to the receiver so they can create credentials for you, and assign the public key to those credentials so you can log in. There is no danger in sharing the pub file contents with the receiver, and sending it via email. Your public key is just that, public, and anyone can access it without posing a security risk.
Creating the Connection Info YML File
Once the receiver has created credentials for you, and assigned your public key to it, they will share the credentials with you, and we need to store the credentials in KeyBase so they're available to the whole team.


In KeyBase we have connection info files that have one of two formats, depending on whether the file describes SFTP password authentication, or SFTP key-based authentication. As we're dealing with key-based authentication, we'll focus on that here.


Here is the basic format of the yaml file.
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
        
create a connection info file in yml format and then copy that into keybase by:
cp ~/development/nj-connection-info.yml /Volumes/Keybase/team/prime_dev_ops/state_info/NJ/
Or just manually upload the yml file into the respective keybase folder

Testing Locally
Once you've created the files and have credentials from the receiver, you want to test and make sure that your configuration is successful and the
One SFTP program you can use to test is Cyberduck. You can download it from the AppStore or directly from cyberduck.com.
Once you have downloaded the app, click on Open connection.

Dropdown to the SFTP transfer. 
The STLT will provide you with the servername and username. 
Some will provide you the password. 
If they don’t provide you with the password, that is okay. Leave it blank.
Goto SSH PrivateKey, and choose the PEM file.
You then will get either a “successful connection” message.
Goto www.reportstream.cdc.gov ->PrimeAdmin (upper right corner) .
Click edit on the organization you are working on.
Then click on Check and if you are successful, you will get a 200 success message









