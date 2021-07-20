import site from '../../content/site.json'

export const ELRIntegration = () => {
    return (<>
<section id="anchor-top" className="margin-bottom-6">
  <h1 className="margin-top-0">Electronic Laboratory Reporting (ELR) integration guide</h1>
  <p className="usa-intro margin-top-0 margin-bottom-4">Public health departments can build a direct connection with
    ReportStream to receive complete, well-structured COVID-19 ELR data. </p>
  <a href={ site.forms.intakeElr.url } target="_blank" rel="noreferrer" className="usa-button">ELR intake form</a>
  <a href="/assets/pdf/ReportStream-ELR-Onboarding-Guide-April-2021.pdf"
    className="usa-button usa-button--outline">Download this guide</a>
  <a href={ "mailto:" + site.orgs.RS.email } className="usa-button usa-button--outline">Contact us</a>
</section>
<hr className="margin-y-6" />
<section>
  <h2>Benefits of building a direct connection</h2>
  <ul className="padding-bottom-1">
    <li>
      <h3>Receive data from SimpleReport</h3>
      <p>If your jurisdiction is interested in receiving point-of-care tests results from <a
          href="https://simplereport.gov" target="_blank" rel="noreferrer"
          title="SimpleReport external link">SimpleReport</a>, you can access this data from the ReportStream
        connection.</p>
    </li>
    <li>
      <h3>Support fewer connections</h3>
      <p>ReportStream’s model focuses on aggregating data from multiple sources and senders, which you receive together
        in a single feed.</p>
    </li>
    <li>
      <h3>Get complete, timely data</h3>
      <p>ReportStream validates data from its senders, and transfers it to you at your preferred frequency and format.
      </p>
    </li>
  </ul>
</section>
<hr className="margin-top-3 margin-bottom-6" />
<section>
  <h2>Integration overview</h2>
  <p>Connecting with the ReportStream platform is very similar to setting up an ELR feed with a lab or hospital. Here is
    a sample timeline for an integration.</p>
  <ol className="usa-process-list">
    <li className="usa-process-list__item">
      <h4 className="usa-process-list__heading">Kickoff and intake</h4>
      <ul className="margin-top-2">
        <li><strong>Public health department (PHD)</strong> reviews the requirements for integrating with the
          ReportStream team, including the data requirements, testing process, and new site registration process.</li>
        <li><strong>PHD</strong> submits the <a href={ site.forms.intakeElr.url } target="_blank" rel="noreferrer"
            className="usa-link">ReportStream intake form</a>.</li>
        <li>After <strong>PHD</strong> has completed the intake form, the ReportStream team will review the information
          and reach out to start the process of establishing the integration.</li>
        <li>(Optional) Based on your configuration meeting with the ReportStream and your ELR team, it may be necessary
          to address custom requirements and outstanding questions.</li>
      </ul>
    </li>
    <li className="usa-process-list__item">
      <h4 className="usa-process-list__heading">Connecting and testing</h4>
      <ul className="margin-top-2">
        <li><strong>PHD</strong> generates and shares credentials for SFTP server.</li>
        <li><strong>PHD and ReportStream</strong> agree on data mappings.</li>
        <li><strong>ReportStream</strong> sends test files for confirmation.</li>
        <li>After credentials are created and test files are accepted on both ends, we are eligible to launch.</li>
      </ul>
    </li>
    <li className="usa-process-list__item">
      <h4 className="usa-process-list__heading">Launch and roll out</h4>
      <ul>
        <li><strong>PHD and ReportStream</strong> decide on the first sender/site for production data.</li>
        <li><strong>PHD</strong> receives production data from first sender.</li>
        <li><strong>ReportStream</strong> registers new facilities over time.</li>
        <li>As new senders onboard to ReportStream, the team will send you their registration details.</li>
      </ul>
    </li>
  </ol>
</section>
<hr className="margin-top-3 margin-bottom-6" />
<section>
  <h2>Integration details</h2>
  <p>The ReportStream team offers a standard set of configurations that enables a short turnaround time from kickoff to
    integration.</p>
  <h3>Data schema</h3>
  <ul>
    <li>ReportStream’s value set follows the HHS guidance for COVID-19 Lab Data Reporting, including the COVID-19 "Ask
      On Entry" questions.</li>
    <li>Our standard way of submitting this data is via HL7 version 2.5.1.</li>
    <li>Sending the "Ask On Entry" questions are optional, and are sent via OBX fields.</li>
    <li>Other file formats such as CSV can be supported but may lead to a delay in integration.</li>
  </ul>
  <h3>Data transport</h3>
  <ul>
    <li>ReportStream support SFTP connections as our standard connection type.</li>
    <li>Data can be sent real-time or batched in daily increments.</li>
    <li>Additional transport platforms such as VPN, PNIMS, SOAP, etc., can be supported but may lead to a delay in
      integration.</li>
  </ul>
  <h3>Site registration process</h3>
  <ul>
    <li>ReportStream works as an aggregator and submits data on behalf of multiple senders and organizations collecting
      COVID-19 data (eg. long-term care facilities, schools, etc).</li>
    <li>ReportStream collects unique identifiers from each sender as they are registered to send, such as
      Organization/Site Name, CLIA, and Address. The sending facility information is generally included in the daily
      feed already.</li>
    <li>To track new facilities and senders, ReportStream can optionally send a spreadsheet on a weekly cadence to an
      email address of your choice whenever there are new facilities.</li>
    <li>Any custom requirements around site registration may lead to be a delay in integration.</li>
  </ul>
  <h3>Sample file testing and launch process</h3>
  <p>ReportStream can support sending a variety of data types for testing before launch.</p>
  <ul>
    <li>
      <h4>Testing – synthetic data</h4>
      <p>ReportStream will always send one or more rounds of synthetic data in the standard data schema for your review.
      </p>
    </li>
    <li>
      <h4>Testing – patient data</h4>
      <p>If required, ReportStream can also send patient data to your test servers.</p>
      <p>In certain cases, ReportStream will not have a live senders in your jurisdiction. In this case, we can wait for
        a live facility to move to production sign off.</p>
    </li>
    <li>
      <h4>Production signoff/launch</h4>
      <p>For the first facility, we can send patient data to your testing server before moving onto production.</p>
      <p>For subsequent organizations/facilities, we will send data directly to production.</p>
    </li>
  </ul>

  <h3>Local jurisdictions</h3>
  <ul>
    <li>ReportStream has the ability to send data to state, county, city, territorial, and tribal public health
      departments.</li>
    <li>Once a state has been onboarded, then by default we send data to the state if 1) the patient is a resident or 2)
      the facility is present in the state.</li>
    <li>We also optionally make the data available to a local public health department based on these rules.</li>
  </ul>
</section>
<hr className="margin-y-6" />
<section>
  <h2>Frequently asked questions</h2>
  <ul>
    <li>
      <h3>Who owns the data?</h3>
      <p>The ordering facilities own their own data. We submit on their behalf. We have a policy of transitory data
        retention.</p>
    </li>
    <li>
      <h3>Where does the data live?</h3>
      <p>The data is currently encrypted and routed through a CDC Cloud environment, where it is deleted after 14 days.
      </p>
    </li>
    <li>
      <h3>Can the ReportStream interface with senders other than SimpleReport? </h3>
      <p>Yes. We are currently prioritizing large, multi-state senders, but if you have other senders you’d like to
        re-direct to ReportStream, please get in contact with us regarding interfacing with additional senders.</p>
    </li>
    <li>
      <h3>Do you currently send data to NHSN?</h3>
      <p>Not currently.</p>
    </li>
    <li>
      <h3>Do you send data to APHL?</h3>
      <p>Not currently.</p>
    </li>
  </ul>
</section>
<hr className="margin-y-6" />
<section>
  <h2>Onboarding checklist</h2>
  <p><strong>To begin the process of integrating ReportStream and your ELR connection, <a href={ site.forms.intakeElr.url }
        target="_blank" rel="noreferrer" className="usa-link">you will need to fill out the ReportStream intake
        form</a>.</strong></p>
  <p>The below outline will help you gather all of the information you will need prior to filling out the intake form
    online. Everything will be captured in the intake form unless explicitly mentioned otherwise.</p>
  <ul>
    <li>
      <p><strong>Public health department information</strong></p>
      <ul>
        <li>Full Name</li>
        <li>State/Territory Code</li>
      </ul>
    </li>
    <li>
      <p><strong>Contact information</strong></p>
      <ul>
        <li>
          ELR Contact information
          <ul>
            <li>Name</li>
            <li>Email address</li>
            <li>Phone number</li>
          </ul>
        </li>
        <li>
          Secondary ELR contact information (optional)
          <ul>
            <li>Name</li>
            <li>Email address</li>
            <li>Phone number</li>
          </ul>
        </li>
      </ul>
    </li>
    <li>
      <p><strong>Data requirements and preferences</strong></p>
      <p>By default, ReportStream sends data as an HL7 file via SFTP. If your preferred data format/transport mechanism
        differs, there may be a delay in building our connection.</p>
      <ul>
        <li>
          File type
          <ul>
            <li>HL7</li>
            <li>CSV</li>
            <li>Other</li>
          </ul>
        </li>
        <li>
          Whether HL7 messages should be batched using FHS and BHS segments.
        </li>
        <li>
          Preferred cadence for receiving aggregated data.
          <ul>
            <li>Realtime</li>
            <li>
              Daily
              <ul>
                <li>If so, what time of the day?</li>
              </ul>
            </li>
          </ul>
        </li>
        <li>
          HL7 Receiving Fields
          <ul>
            <li>Receiving Application Name</li>
            <li>Receiving Application OID</li>
            <li>Receiving Facility Name</li>
            <li>Receiving Facility OID</li>
          </ul>
        </li>
        <li>
          Transport details
          <ul>
            <li>SFTP</li>
            <li>VPN</li>
            <li>Health Information Exchange</li>
            <li>SOAP</li>
            <li>PHINMS</li>
            <li>Other</li>
          </ul>
        </li>
        <li>
          SFTP Details
          <ul>
            <li>Staging Host Name (DNS Name)</li>
            <li>Staging Folder (Folder name)</li>
            <li>Prod Host Name (DNS Name)</li>
            <li>Prod Host Folder (Folder Name)</li>
          </ul>
        </li>
      </ul>
    </li>
    <li>
      <p><strong>Data requirements and preferences</strong></p>
      <p>ReportStream captures HHS's required fields, including Ask on Entry questions. Any other custom fields may lead
        to a delay in building our connection.</p>
      <ul>
        <li>Whether you’d like Ask on Entry questions as OBX segments.</li>
        <li>Whether you have any additional fields you’d like to be collected.</li>
        <li>If you have any example files for us to take a look at.</li>
      </ul>
    </li>
    <li>
      <p><strong>Operation preferences</strong></p>
      <ul>
        <li>
          Confirm you have reviewed the ReportStream “New Site Registration Process” and your preference.
          <ul>
            <li>You don’t require organizations to register before data is sent to you.</li>
            <li>You require organizations to register and the standard fields (Name, CLIA and/or address) are
              sufficient.</li>
            <li>You have a custom Site Registration process.</li>
          </ul>
        </li>
        <li>
          Confirm you have reviewed the ReportStream “Data Testing Process” and your requirements around test data.
          <ul>
            <li>
              Whether you require synthetic and/or real patient data to be sent as test data before going live.
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li>
      <p><strong>Local jurisdictions</strong></p>
      <p>Identify any local jurisdiction reporting requirements that are different from the state.</p>
    </li>
    <li>
      <p><strong>Data use/custody</strong></p>
      <p>If required, please upload any registration or data use agreements necessary for ReportStream to complete its
        integration with your ELR.</p>
      <p>Please note: CDC & ReportStream will not have access to the data routed to your health department – data will
        be encrypted at rest and in transit.</p>
    </li>
  </ul>
  <p className="margin-top-8"><a href="#anchor-top" className="usa-link">Back to top</a></p>
</section>

    </>);
}