import { GovBanner } from "@trussworks/react-uswds";
import classNames from "classnames";
import { Outlet, useMatches } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import React from "react";

import App from "../../App";
import { DAPHeader } from "../../components/header/DAPHeader";
import { ReportStreamHeader } from "../../components/header/ReportStreamHeader";
import RSErrorBoundary from "../../components/RSErrorBoundary";
import SenderModeBanner from "../../components/SenderModeBanner";
import { USLink } from "../../components/USLink";
import { useSessionContext } from "../../contexts/SessionContext";
import { ReportStreamFooter } from "../../shared/ReportStreamFooter/ReportStreamFooter";

const ArticleWrapper = (props: React.PropsWithChildren) => {
    return (
        <article id="main-content" className="tablet:grid-col-12" {...props} />
    );
};

export type MainLayoutBaseProps = React.PropsWithChildren<{}>;

export const MainLayoutBase = ({ children }: MainLayoutBaseProps) => {
    const { environment } = useSessionContext();
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isContentPage, isFullWidth, isLoginPage } = handle;
    // Okta signin widget rudely assumes to be the main element
    const OuterWrapper = isLoginPage ? React.Fragment : "main";
    // article element is currently handled within markdownlayout for markdown pages
    const InnerWrapper = isContentPage || isLoginPage ? "div" : ArticleWrapper;
    const innerWrapperClassnames = classNames(
        isContentPage && !isFullWidth && "grid-row grid-gap-6",
        isFullWidth && "width-full",
    );

    return (
        <div
            className={classNames(
                isContentPage && "rs-style--content",
                isFullWidth && "rs-style--full-width",
                // Currently all the full-width pages are alternate.
                // This could change.
                isFullWidth && "rs-style--alternate",
            )}
        >
            <ReportStreamHeader className="margin-bottom-5" id="site-header">
                <DAPHeader env={environment} />
                <USLink className="usa-skipnav" href="#main-content">
                    Skip Nav
                </USLink>
                <GovBanner aria-label="Official government website" />
                <SenderModeBanner />
            </ReportStreamHeader>
            <OuterWrapper>
                <InnerWrapper className={innerWrapperClassnames}>
                    <RSErrorBoundary>
                        {children}
                        <Outlet />
                    </RSErrorBoundary>
                </InnerWrapper>
            </OuterWrapper>
            <ToastContainer limit={4} />
            <ReportStreamFooter id="site-footer" isAlternate />
        </div>
    );
};

const MainLayout = (props: MainLayoutBaseProps) => {
    return (
        <App>
            <MainLayoutBase {...props} />
        </App>
    );
};

export default MainLayout;
