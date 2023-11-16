import { vi } from "vitest";
import React from "react";

const ReactHelmetAsync =
    await vi.importActual<typeof import("react-helmet-async")>(
        "react-helmet-async",
    );

function Helmet({ children }: React.PropsWithChildren) {
    return children;
}

module.exports = {
    ...ReactHelmetAsync,
    Helmet,
};
