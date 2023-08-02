# Release notes

## Stay up to date with new features, enhancements, and fixes

### June 2023
**New Features**:
[The API Programmer's Guide,](https://reportstream.cdc.gov/resources/api) previously only available as a PDF download, is now part of the ReportStream website. The web pages provide a user-friendly format that ensures users always have the most updated information.

**Bug Fixes**:

- Users that receive data through ReportStream can now access the [File Validator](https://reportstream.cdc.gov/file-handler/validate) while logged in.
- Contact and service request links on the [support](https://reportstream.cdc.gov/support) page have been fixed.

**New State Connection**:

We’re excited to announce that ReportStream can now send public health data to South Dakota.

---
### May 2023
**New Features**

- The [ReportStream File Validator tool](https://reportstream.cdc.gov/file-handler/validate) enables users to validate data formatting for COVID test results against three standard schemas (HL7, CSV, CSV-OTC). If a testing facility has a custom data model, they can validate against that schema if logged in. Users can also troubleshoot issues with their datasets by seeing specific errors and warnings for their files.

- The [Public Key tool](https://reportstream.cdc.gov/resources/manage-public-key) allows new testing facilities to upload their shared public key directly through the ReportStream site as they are setting up authentication. Improvements are planned for the future to allow a user to manage their public keys through this tool as well.

**Improvements**

- Usability: The [FAQ Page](https://reportstream.cdc.gov/support/faq) has been updated with additional questions and clearer answers.

**New State Connection**
We’re excited to announce that ReportStream can now send public health data to Pennsylvania.

---
### April 2023

#### New State Connection

We’re excited to announce that ReportStream can now send public health data to Kansas. 

You can also view release notes on [Github](https://github.com/CDCgov/prime-reportstream/releases).

---
### March 2023

#### Improvements

* Usability: Users can now see more data on wider screens. Tables on [Daily Data](https://reportstream.cdc.gov/daily-data) (for public health departments) and [Submissions](https://reportstream.cdc.gov/submissions) (for testing facilities) have been updated with widescreen grid containers. 

You can also view release notes on [Github](https://github.com/CDCgov/prime-reportstream/releases).


---
### February 2023

* The ReportStream [Terms of Service](https://reportstream.cdc.gov/terms-of-service) have been updated. 

#### Improvements

* Usability: Based on user feedback, the file name is now shown on the Daily Data table for data receivers. Previously, it only showed the data type without a file name.

* Usability: On the Daily Data table, if the user has multiple ELR settings, the first, active one is automatically selected. This prevents an inactive setting from being selected upon table load.

* Visual differences that appeared in the past few weeks after the ReportStream website updated to USWDS 3.0 have been fixed.

* Bug fix: When typing a non-numeric character into the Daily Data table date range, the table would show an error and disable use of the table. Now, the table remains active and allows for the user to fix the entry into a numeric character. This prevents the user from needing to reload the table.

You can also view release notes on [Github](https://github.com/CDCgov/prime-reportstream/releases).


---

### January 2023

See Github release notes anytime at [https://github.com/CDCgov/prime-reportstream/releases](https://github.com/CDCgov/prime-reportstream/releases). 

#### Improvements

* Accessibility: The ReportStream website now has a "skip navigation" link that takes users with screen readers to the main page content instead of tabbing through each nav item. To access it with a keyboard, for example, a user just hits TAB once, then ENTER to "click" the link.

* Usability: Now you don't need to re-login or re-logout across multiple browser tabs open on the ReportStream website. When logged in to the ReportStream website, if you open a new browser tab to the ReportStream website, you will already be logged in to the website. When you log out of the website on one browser tab, you will be logged out of all ReportStream browser tabs.

* Usability: On Daily Data table, to make it easier to find the file you are looking for, the "File" column now displays the entire filename instead of just the file type.

#### New State Connections

We’re excited to announce that ReportStream has recently established connections to send and report public health data with the following state: Utah.


---

### December 2022

See Github release notes anytime at [https://github.com/CDCgov/prime-reportstream/releases](https://github.com/CDCgov/prime-reportstream/releases). 


* To assist in onboarding new labs and facilities that would like to send data through ReportStream, we have added a new resource: [Guide to Submitting Data to ReportStream](https://reportstream.cdc.gov/resources/getting-started-submitting-data).

* In an effort to be as useful as possible to ReportStream users and visitors, we have updated the content in several items on the ReportStream resources page including: [Account Registration Guide](https://reportstream.cdc.gov/resources/account-registration-guide), [Guide to Receiving ReportStream Data](https://reportstream.cdc.gov/resources/getting-started-public-health-departments), [Manual Data Download Guide](https://reportstream.cdc.gov/resources/data-download-guide).




---

### November 2022

See Github release notes anytime at [https://github.com/CDCgov/prime-reportstream/releases](https://github.com/CDCgov/prime-reportstream/releases). 

#### Improvements 

* ReportStream offers many methods to connect to public health authorities to send them data. Among them, REST/HTTP transport is fastest, most secure, and able to provide feedback. Expanding our capabilities in delivery methods enhances ReportStream’s ability to respond to future pandemics and gives us more flexibility for future data types in public health. If your jurisdiction is interested in REST/HTTP transport, [connect with our team.](https://reportstream.cdc.gov/support/contact)

* To make it easier for authenticated ReportStream users to load and view their data on the Daily Data page, we have added pagination to each page. This will display in the lower left-hand side of the page as “number of records” and as “previous” or “next” on the lower right-hand side.

#### Additional Resources 

We’re excited to announce that ReportStream has recently established connections to send and report public health data with the following state: Oklahoma. 




---

### October 2022

See Github release notes anytime at [https://github.com/CDCgov/prime-reportstream/releases](https://github.com/CDCgov/prime-reportstream/releases). 

#### New Features 

* ReportStream launched a new online support center, which you can access on our website at [reportstream.cdc.gov/support](reportstream.cdc.gov/support). Submit any support requests directly to our team by filling out this form: [https://reportstream.cdc.gov/support/service-request]([url](https://reportstream.cdc.gov/support/service-request)). 

* If you are set up to download data from the ReportStream web application, you can now filter the daily data table by date range.  

#### Improvements 

Given the new mapping tool updates by the LOINC In Vitro Diagnostic (LIVD) team, we’ve made those additional fields available in your organization settings. This allows ReportStream users who are receiving data to adjust filters based on those fields. 

#### Additional Resources 

We’re excited to announce that ReportStream has recently established connections to send and report public health data with the following states: Indiana, Missouri, New York, and Rhode Island.




---

### June 2022

#### New Features

* ReportStream now supports RADx MARS, the NIH standard for at-home test reporting via HL7 and CSV formats.

* The Standard CSV schema now accepts “Residence Type” data in your file submission. For details, visit the online guide.

* ReportStream now accepts international phone numbers.

#### Enhancements

* From your ReportStream user account, you can now sort the data column on the Submissions page by ascending or descending order.

#### Fixed Issues

* From your ReportStream user account, you must select a valid date range when using the fixed date filters on the Submissions page.

#### Additional Resources

ReportStream [ReportStream API](/resources/api) is updated to version 2.2 with information on how to submit data for at-home test results.
