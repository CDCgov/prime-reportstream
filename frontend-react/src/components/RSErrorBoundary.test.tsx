import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import { RSNetworkError } from "../utils/RSNetworkError";
import { mockRsconsole } from "../utils/console/__mocks__";
import { defaultCtx } from "../contexts/Session/__mocks__";
import { render } from "../utils/Test/render";
import silenceVirtualConsole, {
    restoreVirtualConsole,
} from "../utils/Test/silenceVirtualConsole";

import { RSErrorBoundary } from "./RSErrorBoundary";

const rsError = new RSNetworkError(
    new AxiosError("RSErrorBoundary error test"),
);

// Dummy components for testing
const ThrowsRSError = (): JSX.Element => {
    throw rsError;
};

describe("RSErrorBoundary", () => {
    beforeEach(() => {
        silenceVirtualConsole();
    });
    afterEach(() => {
        restoreVirtualConsole();
    });
    test("Catches error", () => {
        render(
            <RSErrorBoundary>
                <ThrowsRSError />
            </RSErrorBoundary>,
            {
                providers: {
                    Session: {
                        ...defaultCtx,
                        config: {
                            AI_CONSOLE_SEVERITY_LEVELS: { error: 0 },
                        },
                    },
                },
            },
        );
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        expect(mockRsconsole._error).toBeCalledTimes(1);
        expect(mockRsconsole._error.mock.lastCall?.[0].args[0]).toStrictEqual(
            rsError,
        );
    });

    test("Renders component when no error", () => {
        render(<RSErrorBoundary>Success!</RSErrorBoundary>);
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
