import { Suspense as SuspenseOrig } from "react";

import Spinner from "../../components/Spinner";

export function Suspense({
    children,
    fallback,
}: React.ComponentProps<typeof SuspenseOrig>) {
    return (
        <SuspenseOrig fallback={fallback ?? <Spinner />}>
            {children}
        </SuspenseOrig>
    );
}
