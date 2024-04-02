## HL7 Gap Analysis 

This document outlines the necessary steps to conduct a gap analysis on HL7 messages using the Universal Pipeline.

### Background

As HL7 messages pass through the Universal Pipeline, they get converted into FHIR format and then back to HL7. During this process, data loss can occur. To ensure a lossless translation through the Universal Pipeline, sample HL7 messages need to be processed through the UP and verify the input and output messages are the same.

### Process

1. The first step in the process is to obtain a sample message.
   * If testing an existing sender in ReportStream, it's best to use a production message
   * Otherwise, get samples from sender if onboarding a new sender
2. De-identify message if message contains sensitive data
   * List of fields that require de-identification https://www.nibib.nih.gov/covid-19/radx-tech-program/mars/hl7-message-de-identification-for-sending-to-hhs-protect
3. Create a new folder for the sender here https://cdc.sharepoint.com/:f:/r/teams/ReportStream/Shared%20Documents/Engagement/Gap%20Analysis?csf=1&web=1&e=DKrFrk
4. Save sample messages on newly created folder
5. Send HL7 message through the UP and make sure it gets routed to an HL7 receiver
   * Sender/Receiver combo should have base transforms, nothing custom, no receiver settings
6. Save output on previously created Gap Analysis folder
7. Use a diff-tool to compare input and output messages
    * Tools you can use are https://www.hl7inspector.com/
    * RS CLI `./prime hl7data --starter-file=<path> --comparison-file=<path>`
8. If using an online tool make sure message is de-identified
9. Develop list of differences (for example, excel), consider using this [Template](https://cdc.sharepoint.com/:x:/r/teams/ReportStream/_layouts/15/doc2.aspx?sourcedoc=%7BFAED6C19-BA41-4F03-A148-6C8490228A1C%7D&file=HL7%20IN%20vs%20HL7%20OUT%20_%20Gap_Analysis.xlsx&action=default&mobileredirect=true)
10. Save file on Sender Gap Analysis folder created on step 3