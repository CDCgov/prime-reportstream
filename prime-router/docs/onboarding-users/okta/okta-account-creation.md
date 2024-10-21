# Okta Account Creation

- Walkthrough for setting up a new sender/receiver with an Okta account (Please note this is the general process for creating all Okta accounts.)

## Creating New User
### Must haves: These are the minimum requirements to create an Okta account (this doesn’t mean everything will work properly, but you can still create an OKTA account with the below minimum requirements).
- Need to be an OKTA Admin in relevant OKTA environment (Prod or Staging)
- Need user email address (will be the Username)
- Need user first name and last name

### Steps to Create New Okta User 

1.) Log into Okta (for production accounts)/Okta Preview (for staging accounts) with Credentials.<br>
2.) Select “Admin” button in top right hand corner.<br>
3.) Select “People” from the "Directory" drop down in the left hand menu.<br>
4.) Select “Add Person” button.<br>
5.) Fill in user details. First Name, Last Name and email are required.<br>
6.) If you are ready to activate the user, check the “send user activation email now” box and hit save, or hit save and this step can be done later.

- Assigned Applications - If the user is part of a group that has an assigned application, the user will automatically get assigned to that application (see "Assign user to group section below). Users can also be manually assigned to an application with the following steps (This should almost never be needed).<br>

1.) Select “People” from the "Directory" drop down in the left hand menu.<br>
2.) Search for User by typing user name into search box with "Search for users by first name, primary email or username" grey text.<br>
3.) Click on person & username hyperlink to open person configuration.<br>
4.) Click on blue "Assign Applications" button.<br>
5.) Select "Assign" button next to relevant application (ReportStream).

## Creating and Managing Groups

### Most of the time for new senders and new receivers you’ll need to create the group and assign the users. Check if the group already exists before creating a new group.
- If a group already exists, skip to section assign user to group.
- Group name prefix determines what permissions/resources a user of that group will have access to (ex: DHmd_phd = receiver for organization md_phd).
- If the group name is of the incorrect format the user will be unable to access the correct resources (e.g. only users of the DHSender_+orgname group will be able to access the “Submissions” page).
- Users can be members of multiple groups.
- Be sure to assign an application to the group.
- The format for group names is always "Prefix" + "organization name" where the prefix is dependent on whether the group represents a sender or a receiver as shown in the table below:

| Prefix+Name           | Permissions | Example             |
|-----------------------|-------------|---------------------|
| DH{organization}      | Receiver    | DHmd-phd            |
| DHSender_{organization} | Sender      | DHSender_color-labs |
| Admins*               | Admin       | N/A                 |
*You should not be creating a new group with admin permissions. There are existing admin groups that users should be added to if they need admin permissions.

### Group Creation Steps

1.) Follow steps in new user creation to login and select "Admin" button in top right hand corner.<br>
2.) Select "Groups" from the "Directory" drop down in the left hand menu.<br>
3.) Select "Add Group" button.<br>
4.) Fill out group name and description. Refer to table above in this section for correct naming convention.<br>
5.) Hit "Save".<br>
6.) Search for group by typing newly created group name into search bar with "Search by group name" light grey text.<br>
7.) Click on group name hyperlink to open group configuration.<br>
8.) Select "Applications" tab.<br>
9.) Select blue "Assign applications" button.<br>
10.) Select "Assign" button next to relevant application (ReportStream).


### Assign User to Group Steps

1.) Select "Groups" from the "Directory" drop down in the left hand menu.<br>
2.) Search for group by typing group name into search bar with "Search by group name" light grey text.<br>
3.) Click on group name hyperlink to open group configuration.<br>
4.) Click on "Assign people" blue button on right hand side of screen.<br>
5.) Search for User by typing user name into search box with "Search for users by first name, primary email or username" grey text.<br>
6.) Click on blue "+" symbol on right side of screen to add to group.