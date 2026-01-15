import {
    Identifier,
    IdentifierGov,
    IdentifierIdentity,
    IdentifierLink,
    IdentifierLinkItem,
    IdentifierLinks,
    IdentifierLogo,
    IdentifierLogos,
    IdentifierMasthead,
    Link,
} from "@trussworks/react-uswds";

import styles from "./ReportStreamFooter.module.scss";
import { USExtLink, USLinkButton } from "../../components/USLink";
import site from "../../content/site.json";

export const ReportStreamFooter = () => {
    return (
        <footer id="site-footer" className={styles.Footer}>
            <Identifier className="grid-container">
                <div className="grid-row">
                    <div className="grid-col-8">
                        <IdentifierMasthead className="margin-bottom-4 padding-top-0" aria-label="Agency identifier">
                            <IdentifierLogos>
                                <IdentifierLogo href="https://cdc.gov" target="_blank" rel="noopener">
                                    <img src="/assets/cdc-logo.svg" alt="CDC logo, links to main CDC website" />
                                </IdentifierLogo>
                            </IdentifierLogos>
                            <IdentifierIdentity domain="cdc.gov">
                                An official website of the{" "}
                                <Link href="https://cdc.gov" target="_blank" rel="noopener">
                                    Centers for Disease Control and Prevention
                                </Link>
                            </IdentifierIdentity>
                        </IdentifierMasthead>
                        <IdentifierLinks className="column-count-3" navProps={{ "aria-label": "Important links" }}>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://www.cdc.gov/about/cdc/index.html">
                                    About CDC
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="/about">About ReportStream</IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://www.cdc.gov/other/accessibility.html">
                                    Accessibilty
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://www.cdc.gov/foia/">FOIA requests</IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://www.cdc.gov/eeo/nofearact/index.htm">
                                    No FEAR Act data
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://oig.hhs.gov/" className="usa-link--external">
                                    Office of the Inspector General
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="https://www.cdc.gov/other/privacy.html">
                                    Privacy policy
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink
                                    href="https://www.hhs.gov/vulnerability-disclosure-policy/index.html"
                                    className="usa-link--external"
                                >
                                    Vulnerability disclosure policy
                                </IdentifierLink>
                            </IdentifierLinkItem>
                            <IdentifierLinkItem>
                                <IdentifierLink href="/terms-of-service">Terms of service</IdentifierLink>
                            </IdentifierLinkItem>
                        </IdentifierLinks>
                        <IdentifierGov
                            className="margin-top-1 padding-bottom-0"
                            aria-label="U.S. government information and services"
                        >
                            Looking for U.S. government information and services?{" "}
                            <USExtLink href="https://www.usa.gov/">Visit USA.gov</USExtLink>
                        </IdentifierGov>
                    </div>
                    <div className="grid-col-1"></div>
                    <div className="grid-col-3">
                        <h4 className="margin-top-3">Need help?</h4>
                        <hr className="margin-top-2 margin-bottom-2" />
                        <p className="margin-bottom-4 usa-identifier__response">
                            Our team will respond to your questions or set up a time to learn more about how we can help
                            you.
                        </p>
                        <USLinkButton href={site.forms.connectWithRS.url} outline inverse>
                            Connect now
                        </USLinkButton>
                    </div>
                    <div className="grid-col-1"></div>
                </div>
            </Identifier>
        </footer>
    );
};

export default ReportStreamFooter;
