import { GovBanner } from "@trussworks/react-uswds";
import classNames from "classnames";
import { Outlet, useMatches } from "react-router-dom";
import { ToastContainer } from "react-toastify";

import App from "../../App";
import { DAPHeader } from "../../components/header/DAPHeader";
import { ReportStreamHeader } from "../../components/header/ReportStreamHeader";
import RSErrorBoundary from "../../components/RSErrorBoundary";
import SenderModeBanner from "../../components/SenderModeBanner";
import { USLink } from "../../components/USLink";
import { useSessionContext } from "../../contexts/SessionContext";
import { ReportStreamFooter } from "../../shared/ReportStreamFooter/ReportStreamFooter";

export type MainLayoutProps = React.PropsWithChildren<{}>;

const MainLayout = ({ children }: MainLayoutProps) => {
    const { environment } = useSessionContext();
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isContentPage, isFullWidth } = handle;

    return (
        <App>
            <div
                className={classNames(
                    isContentPage && "rs-style--content",
                    isFullWidth && "rs-style--full-width",
                    // Currently all the full-width pages are alternate.
                    // This could change.
                    isFullWidth && "rs-style--alternate",
                )}
            >
                <ReportStreamHeader
                    className="margin-bottom-5"
                    id="site-header"
                >
                    <DAPHeader env={environment} />
                    <USLink className="usa-skipnav" href="#main-content">
                        Skip Nav
                    </USLink>
                    <GovBanner aria-label="Official government website" />
                    <SenderModeBanner />
                </ReportStreamHeader>
                <main>
                    <RSErrorBoundary>
                        {children}
                        <Outlet />
                    </RSErrorBoundary>
                </main>
                <ToastContainer limit={4} />
                <ReportStreamFooter id="site-footer" isAlternate />
            </div>
        </App>
    );
};

export default MainLayout;
