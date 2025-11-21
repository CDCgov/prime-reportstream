const SunsetNoticeBanner = () => {
    return (
        <section className="usa-site-alert usa-site-alert--emergency" aria-label="Site alert">
            <div className="usa-alert usa-alert--emergency">
                <div className="usa-alert__body">
                    <h3 className="usa-alert__heading">ReportStream Sunset Notice</h3>
                    <p className="usa-alert__text">
                        As part of CDC&apos;s long-term strategy to streamline data exchange, we will be sunsetting
                        ReportStream on <strong>December 31, 2025</strong>. ReportStream&apos;s functionalities will be
                        transitioned to the Association of Public Health Laboratories&apos; (APHL) Informatics Messaging
                        System (AIMS) platform. For organizations interested in also transitioning to the{" "}
                         <a href="https://www.aphl.org/programs/informatics/pages/aims_platform.aspx" className="usa-link">
                            Association of Public Health Laboratories&apos; (APHL) Informatics Messaging System (AIMS)
                        </a>{" "} platform.
                        For organizations interested in also transitioning to AIMS, APHL will provide detailed information and dedicated 
                        assistance to facilitate a smooth migration for your integration. Please use the{" "}
                        <a href="https://aphlinformatics.atlassian.net/servicedesk/customer/portal/23/" className="usa-link">
                            AIMS Platform Customer Portal
                        </a>{" "}
                        and submit a ticket to arrange your organization&apos;s onboarding to the APHL AIMS platform. 
                        If you have specific questions related
                        to the ReportStream sunset, please contact{" "}
                        <a href="mailto:OPHDST@cdc.gov?subject=ReportStream%20Sunset" className="usa-link">
                            OPHDST@cdc.gov
                        </a>{" "}
                        with the subject line: &quot;ReportStream Sunset&quot;.
                    </p>
                </div>
            </div>
        </section>
    );
};

export default SunsetNoticeBanner;
