import React, { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";

import { ErrorPage } from "../../pages/error/ErrorPage";
import Spinner from "../Spinner";

interface AdminFormWrapperProps {
    header: React.ReactNode | string;
}

export function AdminFormWrapper({
    header,
    children,
}: React.PropsWithChildren<AdminFormWrapperProps>) {
    return (
        <>
            <section className="grid-container margin-bottom-5">
                {header}
            </section>
            <Suspense
                fallback={
                    <span className="text-normal text-base">
                        <Spinner />
                    </span>
                }
            >
                <NetworkErrorBoundary
                    fallbackComponent={(props) => (
                        <ErrorPage type="page" error={props.error} />
                    )}
                >
                    {children}
                </NetworkErrorBoundary>
            </Suspense>
        </>
    );
}
