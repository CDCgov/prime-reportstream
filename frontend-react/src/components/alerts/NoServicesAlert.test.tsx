import { screen } from "@testing-library/react";

import { renderWithBase } from "../../utils/CustomRenderUtils";

import { NoServicesBanner } from "./NoServicesAlert";

describe("NoServicesAlert", () => {
    test("displays with undefined props", () => {
        renderWithBase(<NoServicesBanner />);
        expect(screen.getByText("Feature unavailable")).toBeInTheDocument();
        expect(
            screen.getByText("No valid service found for your organization")
        ).toBeInTheDocument();
    });
    test("displays with props", () => {
        renderWithBase(
            <NoServicesBanner
                featureName={"testing"}
                serviceType={"sender"}
                organization={"test-org"}
            />
        );
        expect(screen.getByText("Testing unavailable")).toBeInTheDocument();
        expect(
            screen.getByText("No valid sender found for test-org")
        ).toBeInTheDocument();
    });
});
