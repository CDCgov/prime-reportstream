import {Identifier, IdentifierMasthead, IdentifierLogos, IdentifierLogo, IdentifierIdentity, IdentifierLinks, Link, IdentifierGov, IdentifierLinkItem,IdentifierLink } from '@trussworks/react-uswds'


export const ReportStreamFooter = () => {
    return   ( <Identifier>
    <IdentifierMasthead aria-label="Agency identifier">
      <IdentifierLogos>
        <IdentifierLogo href="#">
          <img src="/assets/cdc-logo.svg" alt="CDC Log" />
        </IdentifierLogo>
      </IdentifierLogos>
      <IdentifierIdentity domain="cdc.gov">
        {`An official website of the `}
        <Link href="https://cdc.gov">Centers for Disease Control and Prevention</Link>
      </IdentifierIdentity>
    </IdentifierMasthead>
    <IdentifierLinks navProps={{ 'aria-label': 'Important links' }}>
      <IdentifierLinkItem><IdentifierLink href="https://www.cdc.gov/about/default.htm">About CDC</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="https://www.cdc.gov/contact/accessibility.html">Accessibilty support</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="https://www.cdc.gov/od/foia">FOIA requests</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="https://www.cdc.gov/eeo/nofearact/index.htm">No FEAR Act data</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="https://oig.hhs.gov/">Office of the Inspector General</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="https://www.cdc.gov/other/privacy.html">Privacy policy</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/terms-of-service">Terms of service</IdentifierLink></IdentifierLinkItem>
    </IdentifierLinks>
    <IdentifierGov aria-label="U.S. government information and services">
      Looking for U.S. government information and services? <a href="https://www.usa.gov/">Visit USA.gov</a>
    </IdentifierGov>
  </Identifier> )
    }
