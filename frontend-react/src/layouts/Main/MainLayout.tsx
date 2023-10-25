import classNames from "classnames";
import { Outlet, useMatches } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import React, { Suspense, useMemo } from "react";

import RSErrorBoundary from "../../components/RSErrorBoundary";
import { ReportStreamFooter } from "../../shared/ReportStreamFooter/ReportStreamFooter";
import ScrollRestoration from "../../components/ScrollRestoration";
import { useScrollToTop } from "../../hooks/UseScrollToTop";
import { ReportStreamHeader } from "../../components/header/ReportStreamHeader";
import Spinner from "../../components/Spinner";

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
        isContentPage && !isFullWidth && "width-full grid-row grid-gap-6",
        isFullWidth && "width-full",
        !isContentPage && "tablet:grid-col-12",
    );
    const suspenseFallback = useMemo(() => <Spinner size={"fullpage"} />, []);
    useScrollToTop();

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
            <ReportStreamHeader blueVariant={isFullWidth} />
            <main className="padding-top-5" id="main-content">
                <InnerWrapper className={innerWrapperClassnames}>
                    <RSErrorBoundary>
                        <ScrollRestoration />
                        {children}
                        <Suspense fallback={suspenseFallback}>
                            <Outlet />
                        </Suspense>
                    </RSErrorBoundary>
                </InnerWrapper>
            </main>
            <ToastContainer limit={4} />
            <ReportStreamFooter />
        </div>
    );
};

const MainLayout = (props: MainLayoutBaseProps) => {
    return <MainLayoutBase {...props} />;
};

export default MainLayout;
