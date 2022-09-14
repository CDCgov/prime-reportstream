/* eslint-disable no-restricted-globals */
import { render, screen } from "@testing-library/react";

import { ErrorName } from "../../utils/RSNetworkError";

import { errorContent } from "./ErrorDisplay";

describe("errorContent", () => {
    const genericMessageContent = errorContent(ErrorName.UNKNOWN);
    const genericPageContent = errorContent(ErrorName.UNKNOWN, {
        displayAsPage: true,
    });
    test("GenericMessage", () => {
        render(genericMessageContent);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
    test("GenericPage", () => {
        render(genericPageContent);
        expect(screen.getByText("An error has occurred")).toBeInTheDocument();
    });
});
