export const SecurityPractices = () => {
    return (<>
<section id="anchor-top">
  <h1 className="margin-top-0">Security practices questions and answers</h1>
</section>
<hr className="margin-y-6" />
<section>
  <h2>What Security Audits, Certifications and Attestations does ReportStream have?</h2>
  <p>As a project of the US Federal government, PRIME ReportStream follows the provisions of the Federal Information System Modernization Act of 2014.  FISMA requires federal agencies to implement information security plans to protect sensitive data.</p>
  <p>FISMA and the National Institute of Standards and Technology (NIST) set the FISMA compliance guidelines. NIST is responsible for maintaining and updating the compliance documents as directed by FISMA. More specifically, NIST and the NIST SP 800-53 guideline:</p>
  <ul>
    <li>
      Sets minimum requirements for information security plans and procedures.
    </li>
    <li>
      Recommends types of security (systems, software, etc.) that agencies must implement and approves vendors.
    </li>
    <li>
      Standardizes risk assessment process and sets varying standards of information security based on agency risk assessments.
    </li>
  </ul>
  <p>As part of the FISMA process, PRIME ReportStream has a full authority to operate (ATO). This authority was granted by CDC’s Chief Information Officer/Authorizing Official after an extensive review process that includes documenting over 500 security controls, reviews by multiple teams, and penetration testing by third-parties.</p>
  <p>The following link contains more information about the FISMA process and <a href="https://csrc.nist.gov/projects/risk-management" className="usa-link" target="_blank" rel="noreferrer">NIST guidance: NIST Risk Management Framework | CSRC</a></p>
</section>
<section>
  <h2>Is ReportStream FedRAMP approved?</h2>
  <p>Federal information systems like ReportStream are not in the scope of the Federal Risk and Authorization Management Program (FedRAMP), which is for vendors that sell cloud products or services to the Federal government.  However, as mentioned above, ReportStream follows the FISMA law and both FISMA and FedRAMP share the same NIST security guidelines and standards.     </p>

  <h2>How is ReportStream hosted?</h2>
  <p>The PRIME ReportStream service is hosted in a secured FedRAMP-accredited environment. </p>

  <h2>Is data encrypted in transit?</h2>
  <p>All data is encrypted in transit.  ReportStream also uses data-in-transit encryption for connections to send test results to states.</p>

  <h2>Is data encrypted at rest? </h2>
  <p>All data is encrypted when stored using facilities of the secured FedRAMP-accredited environment.</p>
</section>
<section>
  <h2>Who has access to ReportStream data?</h2>
  <p>ReportStream filters and routes data to state and county Public Health Agencies through their electronic reporting interfaces. In addition, Public Health Agencies may designate users who can download ReportStream data from a ReportStream website. There are a number of security controls placed on these user accounts including multi-factor authentication and automatic disablement due to in-activity.</p>
  <p>Some members of the PRIME ReportStream team have administrative access to ReportStream data for the purposes of reliably operating the ReportStream service. All ReportStream team members who have this administrative access go through Federal government background checks. </p>

  <h2>What audit trail is kept by ReportStream?</h2>
  <p>For business and security purposes, ReportStream keeps many different logs of activity. In particular, ReportStream keeps a line-level audit trail of all test results that is sent to ReportStream and all the receivers of data from ReportStream. The ReportStream audit trail is kept for multiple years.</p>

  <h2>How long is data stored?</h2>
  <p>ReportStream is a conduit for data, not a store of record. However, to operate reliably, it is necessary to temporarily retain data in ReportStream. The current retention period for test results and other PII & PHI is 30 days.</p>

  <h2>Are there terms of service for ReportStream?</h2>
  <p>The CDC has a standard terms-of-service agreement for all organizations that have test results that they wish ReportStream to send to public health agencies.</p>

  <p className="margin-top-8"><a href="#anchor-top" className="usa-link">Back to top</a></p>
</section>

    </>);
}