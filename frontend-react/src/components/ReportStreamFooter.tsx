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
        <Link href="#">Centers for Disease Control and Prevention</Link>
      </IdentifierIdentity>
    </IdentifierMasthead>
    <IdentifierLinks navProps={{ 'aria-label': 'Important links' }}>
      <IdentifierLinkItem><IdentifierLink href="/">About CDC</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">Accessibilty support</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">FOIA requests</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">No FEAR Act data</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">Office of the Inspector General</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">Privacy policy</IdentifierLink></IdentifierLinkItem>
      <IdentifierLinkItem><IdentifierLink href="/">Terms of service</IdentifierLink></IdentifierLinkItem>
    </IdentifierLinks>
    <IdentifierGov aria-label="U.S. government information and services">
      Looking for U.S. government information and services? <a href="#usa">Visit USA.gov</a>
    </IdentifierGov>
  </Identifier> )
    }
