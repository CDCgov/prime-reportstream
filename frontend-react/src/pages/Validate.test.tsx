// import { screen } from "@testing-library/react";

import React, { ReactElement, Suspense } from "react";
import { Fixture, MockResolver } from "@rest-hooks/test";
import { CacheProvider } from "rest-hooks";

import Spinner from "../components/Spinner";
import { renderWithRouter } from "../utils/CustomRenderUtils";

import Validate from "./Validate";

describe("Validate", () => {
    const renderWithResolver = (ui: ReactElement, fixtures: Fixture[]) =>
        renderWithRouter(
            <CacheProvider>
                <MockResolver fixtures={fixtures}>{ui}</MockResolver>
            </CacheProvider>
        );
    test("Renders with no errors", () => {
        renderWithResolver(
            <Suspense fallback={<Spinner size={"fullpage"} />}>
                <Validate />
            </Suspense>,
            []
        );
    });
});
