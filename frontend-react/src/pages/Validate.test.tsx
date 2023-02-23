import React, { Suspense } from "react";

import Spinner from "../components/Spinner";
import { renderWithFullAppContext } from "../utils/CustomRenderUtils";

import Validate from "./Validate";

describe("Validate", () => {
    test("Renders with no errors", async () => {
        expect(() =>
            renderWithFullAppContext(
                <Suspense fallback={<Spinner size={"fullpage"} />}>
                    <Validate />
                </Suspense>
            )
        ).not.toThrow();
    });
});
