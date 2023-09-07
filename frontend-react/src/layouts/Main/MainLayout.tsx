import classNames from "classnames";
import { Outlet, useMatches } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import React from "react";

import App from "../../App";
import RSErrorBoundary from "../../components/RSErrorBoundary";
import { ReportStreamFooter } from "../../shared/ReportStreamFooter/ReportStreamFooter";
import { ReportStreamNavbar } from "../../components/header/ReportStreamNavbar";

const ArticleWrapper = (props: React.PropsWithChildren) => {
    return <article className="tablet:grid-col-12" {...props} />;
};

export type MainLayoutBaseProps = React.PropsWithChildren<{}>;

export const MainLayoutBase = ({ children }: MainLayoutBaseProps) => {
    const matches = useMatches() as RsRouteObject[];
    const { handle = {} } = matches.at(-1) ?? {};
    const { isContentPage, isFullWidth, isLoginPage } = handle;
    // article element is currently handled within markdownlayout for markdown pages
    const InnerWrapper = isContentPage || isLoginPage ? "div" : ArticleWrapper;
    const innerWrapperClassnames = classNames(
        isContentPage && !isFullWidth && "grid-row grid-gap-6",
        isFullWidth && "width-full",
        !isContentPage && !isLoginPage && "tablet:grid-col-12",
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
            <ReportStreamNavbar blueVariant={isFullWidth} />
            <main className="padding-top-5" id="main-content">
                <InnerWrapper className={innerWrapperClassnames}>
                    <RSErrorBoundary>
                        {children}
                        <Outlet />
                    </RSErrorBoundary>
                </InnerWrapper>
            </main>
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
