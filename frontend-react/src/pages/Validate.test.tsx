import React, { Suspense } from "react";

import { renderWithSession } from "../utils/CustomRenderUtils";
import Spinner from "../components/Spinner";

import Validate from "./Validate";

describe("Validate", () => {
    test("Renders with no errors", async () => {
        expect(() =>
            renderWithSession(
                <Suspense fallback={<Spinner size={"fullpage"} />}>
                    <Validate />
                </Suspense>
            )
        ).not.toThrow();
    });
});
