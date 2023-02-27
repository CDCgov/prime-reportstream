import { screen } from "@testing-library/react";

import {
    GENERIC_ERROR_PAGE_CONFIG,
    GENERIC_ERROR_STRING,
} from "../../content/error/ErrorMessages";
import { renderWithRouter } from "../../utils/CustomRenderUtils";

import { ErrorPage } from "./ErrorPage";

describe("ErrorPage tests", () => {
    test("Renders as message with no props", () => {
        renderWithRouter(<ErrorPage />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
    test('Renders as page with type="page" prop', () => {
        renderWithRouter(<ErrorPage type={"page"} />);
        expect(screen.getByRole("heading")).toHaveTextContent(
            "An error has occurred"
        );
    });
    test('Renders as message with type="message" prop', () => {
        renderWithRouter(<ErrorPage type={"message"} />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
    test("Renders as page with config prop as ErrorDisplayPageConfig", () => {
        renderWithRouter(<ErrorPage config={GENERIC_ERROR_PAGE_CONFIG} />);
        expect(screen.getByRole("heading")).toHaveTextContent(
            "An error has occurred"
        );
    });
    test("Renders as message with config prop as ErrorDisplayPageConfig", () => {
        renderWithRouter(<ErrorPage config={GENERIC_ERROR_STRING} />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
    test("Including type (old prop) and config (new prop) will result in NEW prop being used", () => {
        renderWithRouter(
            <ErrorPage type={"message"} config={GENERIC_ERROR_PAGE_CONFIG} />
        );
        expect(screen.getByRole("heading")).toHaveTextContent(
            "An error has occurred"
        );
    });
});
