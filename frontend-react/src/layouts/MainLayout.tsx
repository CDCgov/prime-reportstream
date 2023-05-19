import { GovBanner } from "@trussworks/react-uswds";
import classNames from "classnames";
import { Outlet, useMatches } from "react-router-dom";
import { ToastContainer } from "react-toastify";

import App from "../App";
import { DAPHeader } from "../components/header/DAPHeader";
import { ReportStreamHeader } from "../components/header/ReportStreamHeader";
import { ReportStreamFooter } from "../components/ReportStreamFooter";
import SenderModeBanner from "../components/SenderModeBanner";
import { USLink } from "../components/USLink";
import { useSessionContext } from "../contexts/SessionContext";

const MainLayout = () => {
    const { environment } = useSessionContext();
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isContentPage } = handle;

    return (
        <App>
            <DAPHeader env={environment} />
            <USLink className="usa-skipnav" href="#main-content">
                Skip Nav
            </USLink>
            <GovBanner aria-label="Official government website" />
            <SenderModeBanner />
            <ReportStreamHeader />
            <main
                id="main-content"
                className={classNames(isContentPage && "rs-style__content")}
            >
                <Outlet />
            </main>
            <ToastContainer limit={4} />
            <footer className="usa-identifier footer">
                <ReportStreamFooter />
            </footer>
        </App>
    );
};

export default MainLayout;
