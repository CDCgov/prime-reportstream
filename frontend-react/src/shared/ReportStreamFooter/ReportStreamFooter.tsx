import {
    Identifier,
    IdentifierMasthead,
    IdentifierLogos,
    IdentifierLogo,
    IdentifierIdentity,
    IdentifierLinks,
    Link,
    IdentifierGov,
    IdentifierLinkItem,
    IdentifierLink,
} from "@trussworks/react-uswds";
import classNames from "classnames";

import { USExtLink } from "../../components/USLink";

import styles from "./ReportStreamFooter.module.scss";

export interface ReportStreamFooterProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLElement>> {
    isAlternate?: boolean;
}

export const ReportStreamFooter = ({
    isAlternate,
    className,
    ...props
}: ReportStreamFooterProps) => {
    const classnames = classNames(
        isAlternate && styles["rs-footer--alternate"],
        className,
    );
    return (
        <footer className={classnames} {...props}>
            <Identifier>
                <IdentifierMasthead aria-label="Agency identifier">
                    <IdentifierLogos>
                        <IdentifierLogo
                            href="https://cdc.gov"
                            target="_blank"
                            rel="noopener"
                        >
                            <img
                                src="/assets/cdc-logo.svg"
                                alt="CDC logo, links to main CDC website"
                            />
                        </IdentifierLogo>
                    </IdentifierLogos>
                    <IdentifierIdentity domain="cdc.gov">
                        An official website of the{" "}
                        <Link
                            href="https://cdc.gov"
                            target="_blank"
                            rel="noopener"
                        >
                            Centers for Disease Control and Prevention
                        </Link>
                    </IdentifierIdentity>
                </IdentifierMasthead>
                <IdentifierLinks
                    className="column-count-3"
                    navProps={{ "aria-label": "Important links" }}
                >
                    <IdentifierLinkItem>
                        <IdentifierLink href="https://www.cdc.gov/about/default.htm">
                            About CDC
                        </IdentifierLink>
                    </IdentifierLinkItem>
                    <IdentifierLinkItem>
                        <IdentifierLink href="/about">
                            About ReportStream
                        </IdentifierLink>
                    </IdentifierLinkItem>
                    <IdentifierLinkItem>
                        <IdentifierLink href="https://www.cdc.gov/contact/accessibility.html">
                            Accessibilty support
                        </IdentifierLink>
                    </IdentifierLinkItem>
                    <IdentifierLinkItem>
                        <IdentifierLink href="https://www.cdc.gov/od/foia">
                            FOIA requests
                        </IdentifierLink>
                    </IdentifierLinkItem>
                    <IdentifierLinkItem>
                        <IdentifierLink href="https://www.cdc.gov/eeo/nofearact/index.htm">
                            No FEAR Act data
                        </IdentifierLink>
                    </IdentifierLinkItem>
                    <IdentifierLinkItem>
                        <IdentifierLink
                            href="https://oig.hhs.gov/"
                            className="usa-link--external"
                        >
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
                        <IdentifierLink href="/terms-of-service">
                            Terms of service
                        </IdentifierLink>
                    </IdentifierLinkItem>
                </IdentifierLinks>
                <IdentifierGov aria-label="U.S. government information and services">
                    Looking for U.S. government information and services?{" "}
                    <USExtLink href="https://www.usa.gov/">
                        Visit USA.gov
                    </USExtLink>
                </IdentifierGov>
            </Identifier>
        </footer>
    );
};
