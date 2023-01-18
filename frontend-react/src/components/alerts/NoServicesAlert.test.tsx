import { render, screen } from "@testing-library/react";

import { NoServicesBanner } from "./NoServicesAlert";

describe("NoServicesAlert", () => {
    test("displays with undefined props", () => {
        render(<NoServicesBanner />);
        expect(screen.getByText("Feature unavailable")).toBeInTheDocument();
        expect(
            screen.getByText("No valid service found for your organization")
        ).toBeInTheDocument();
    });
    test("displays with props", () => {
        render(
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
