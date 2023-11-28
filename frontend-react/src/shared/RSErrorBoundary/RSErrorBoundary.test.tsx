import { screen } from "@testing-library/react";
import { AxiosError } from "axios";

import { RSNetworkError } from "../../utils/RSNetworkError";
import { mockRsconsole } from "../../utils/console/__mocks__";
import { render } from "../../utils/Test/render";
import silenceVirtualConsole, {
    restoreVirtualConsole,
} from "../../utils/Test/silenceVirtualConsole";

import { RSErrorBoundaryBase } from "./RSErrorBoundary";

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
            <RSErrorBoundaryBase rsconsole={mockRsconsole}>
                <ThrowsRSError />
            </RSErrorBoundaryBase>,
        );
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        expect(mockRsconsole.aiError).toBeCalledTimes(1);
        expect(mockRsconsole.aiError.mock.lastCall?.[0]).toStrictEqual(rsError);
    });

    test("Renders component when no error", () => {
        render(
            <RSErrorBoundaryBase rsconsole={{} as any}>
                Success!
            </RSErrorBoundaryBase>,
        );
        expect(screen.getByText("Success!")).toBeInTheDocument();
    });
});
