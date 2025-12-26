import { PropsWithChildren, ReactNode, Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";

import { ErrorPage } from "../../pages/error/ErrorPage";
import Spinner from "../Spinner";

interface AdminFormWrapperProps {
    header: ReactNode | string;
}

export function AdminFormWrapper({ header, children }: PropsWithChildren<AdminFormWrapperProps>) {
    return (
        <NetworkErrorBoundary fallbackComponent={() => <ErrorPage type="page" />}>
            <section className="grid-container margin-bottom-5">{header}</section>
            <Suspense
                fallback={
                    <span className="text-normal text-base">
                        <Spinner />
                    </span>
                }
            >
                {children}
            </Suspense>
        </NetworkErrorBoundary>
    );
}
