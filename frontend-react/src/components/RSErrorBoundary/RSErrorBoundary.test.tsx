import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import RSErrorBoundary from "./RSErrorBoundary";
import { renderApp } from "../../utils/CustomRenderUtils";
import { mockRsconsole } from "../../utils/rsConsole/rsConsole.fixtures";
import { RSNetworkError } from "../../utils/RSNetworkError";

const rsError = new RSNetworkError(new AxiosError("rsnetwork error test"));

// Dummy components for testing
const ThrowsRSError = (): JSX.Element => {
    throw rsError;
};

describe("RSErrorBoundary", () => {
    beforeAll(() => {
        // shut up react's auto console.error
        mockRsconsole.error.mockImplementation(() => void 0);
    });
    afterAll(() => {
        mockRsconsole.error.mockRestore();
    });
    test("Catches error", () => {
        renderApp(
            <RSErrorBoundary>
                <ThrowsRSError />
            </RSErrorBoundary>,
        );
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        expect(mockRsconsole._error).toHaveBeenCalledTimes(1);
        expect(mockRsconsole._error.mock.lastCall?.[0].args[0]).toStrictEqual(
            rsError,
        );
    });

    test("Renders component when no error", () => {
        renderApp(<RSErrorBoundary>Success!</RSErrorBoundary>);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
