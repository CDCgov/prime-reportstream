/* eslint-disable no-restricted-globals */
import { ReactElement } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { render } from "@testing-library/react";

import { CODES, ErrorPage } from "./ErrorPage";

describe("testing ErrorPage", () => {
    // types of errors we can throw
    function Throw500(): ReactElement {
        const error: any = new Error("500");
        error.status = 500;
        throw error;
    }

    // intercept error handling mechanism since we WANT to generate errors
    function onError(e: any) {
        e.preventDefault();
    }

    beforeEach(() => {
        if (typeof addEventListener === "function")
            addEventListener("error", onError);
    });

    afterEach(() => {
        if (typeof removeEventListener === "function")
            removeEventListener("error", onError);
    });

    // actual tests
    it("checks basic error", () => {
        const { getAllByText, queryByText } = render(
            <ErrorPage>
                <div>child component</div>
            </ErrorPage>
        );

        expect(
            getByText(/application has encountered an unknown error/i)
        ).toBeInTheDocument();
        expect(queryByText(/child component/i)).not.toBeInTheDocument();
    });

    it("check UNSUPPORTED_BROWSER", () => {
        const { getByText, queryByText } = render(
            <ErrorPage code={CODES.UNSUPPORTED_BROWSER}>
                <div>child component</div>
            </ErrorPage>
        );
        expect(getByText(/does not support your browser/i)).toBeInTheDocument();
        expect(queryByText(/child component/i)).not.toBeInTheDocument();
    });

    it("check NOT_FOUND_404", () => {
        const { getByText, queryByText } = render(
            <ErrorPage code={CODES.NOT_FOUND_404}>
                <div>child component</div>
            </ErrorPage>
        );
        expect(getByText(/Page not found/i)).toBeInTheDocument();
        expect(queryByText(/child component/i)).not.toBeInTheDocument();
    });

    it("NetworkErrorBoundary 500", () => {
        const { getByText, queryByText } = render(
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="page" />}
            >
                <Throw500 />
                <div>never renders</div>
            </NetworkErrorBoundary>
        );
        expect(queryByText(/never renders/i)).not.toBeInTheDocument();
        expect(
            getByText(/application has encountered an unknown error/i)
        ).toBeInTheDocument();
    });
});
