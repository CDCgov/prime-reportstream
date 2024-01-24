import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import { renderApp } from "../utils/CustomRenderUtils";
import { RSNetworkError } from "../utils/RSNetworkError";
import { mockConsole } from "../__mocks__/console";
import { mockRsconsole } from "../utils/console/__mocks__/console";
import { mockSessionContentReturnValue } from "../contexts/__mocks__/SessionContext";

import { RSErrorBoundary } from "./RSErrorBoundary";

const rsError = new RSNetworkError(new AxiosError("rsnetwork error test"));

// Dummy components for testing
const ThrowsRSError = (): JSX.Element => {
    throw rsError;
};

describe("RSErrorBoundary", () => {
    beforeAll(() => {
        // shut up react's auto console.error
        mockConsole.error.mockImplementation(() => void 0);
        mockSessionContentReturnValue({
            config: {
                AI_CONSOLE_SEVERITY_LEVELS: { error: 0 },
            } as any,
        });
    });
    afterAll(() => {
        mockConsole.error.mockRestore();
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
        expect(mockRsconsole._error).toBeCalledTimes(1);
        expect(mockRsconsole._error.mock.lastCall[0].args[0]).toStrictEqual(
            rsError,
        );
    });

    test("Renders component when no error", () => {
        renderApp(<RSErrorBoundary>Success!</RSErrorBoundary>);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
