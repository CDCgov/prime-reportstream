# Admin Management

More details about the organization within okta can be found in [this doc](https://cdc.sharepoint.com/:p:/r/teams/ReportStream/_layouts/15/Doc.aspx?sourcedoc=%7B313111b2-502c-4f60-ac8c-bbcf3c9b1dab%7D&action=edit&wdPreviousSession=a28aeb1e-02b3-b6be-49ab-cafb30120e6f)

Okta admin potential responsibility areas are:
- App registry management
- User/group management
- Security configuration management
- Log checking

ReportStream's Okta has the following specialized admin roles for team members:
- Owners
- Support Team
- Onboarding Engineers
- Front-end Engineers
- Tech Leads


## App registry management

The app registry page can be found by the following side-navigation: Applications > Applications.

All reportstream-developed programs with authentication elements should be configured towards an application listed on this page.


## User/Group management

Accessible via the side-navigation: Directory > People or Directory > Groups

## Security configuration management

The policies are enforced in the following order (accessed through "Security" in side-navigation):
- Global Session Policy
- Authentication Policy
- Password Policy (from side-navigation: Security > Authenticators > Click Actions for the "Password" table line > Edit)

## Log checking

The global log can be accessed from side-navigation: Reports > System Log. They can also be filtered by user by going to the user's management page (side-navigation: Directory > People) and clicking "View Logs".