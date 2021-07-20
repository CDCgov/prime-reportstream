export const About = () => {
    return (<>
      <section className="margin-bottom-6">
        <h1 className="margin-top-0">About ReportStream</h1>
        <p className="usa-intro margin-top-0">ReportStream was created as part of the <a href="https://www.cdc.gov/surveillance/projects/pandemic-ready-it-systems.html" rel="noreferrer" className="usa-link" target="_blank">Pandemic-Ready Interoperability Modernization Effort (PRIME)</a>. PRIME is a multi-year collaboration between the Centers for Disease Control and Prevention (CDC) and the U.S. Digital Service (USDS) to strengthen data quality and information technology systems in state and local health departments. </p>
        <a href="../assets/pdf/PRIME-1-pager.pdf" className="usa-button usa-button--outline">Download PRIME 1-pager</a>
        <a href="mailto:reportstream@cdc.gov" className="usa-button usa-button--outline">Contact us</a>
      </section>
      <hr className="margin-y-6" />

      <section>
        <h2>Centers for Disease Control and Prevention</h2>
        <p>The <a href="https://www.cdc.gov/">Centers for Disease Control and Prevention</a> works 24/7 to protect America from health, safety and security threats, both foreign and in the U.S. Whether diseases start at home or abroad, are chronic or acute, curable or preventable, human error or deliberate attack, CDC fights disease and supports communities and citizens to do the same.</p>
        <p>CDC increases the health security of our nation. As the nation’s health protection agency, CDC saves lives and protects people from health threats. To accomplish our mission, CDC conducts critical science and provides health information that protects our nation against expensive and dangerous health threats, and responds when these arise.</p>
      </section>

      <section>
        <h2>U.S. Digital Service</h2>
        <p>The <a href="https://www.usds.gov/">U.S. Digital Service</a> is a group of technologists from diverse backgrounds working across the federal government to transform critical services for the people. These specialists join for tours of civic service to create a steady influx of fresh perspectives. Today USDS continues its non-partisan mission by bringing better government services to all Americans through design and technology.</p>
      </section>
      </>);
}