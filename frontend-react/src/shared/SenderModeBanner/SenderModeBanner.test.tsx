import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import { SenderModeBannerBase } from "./SenderModeBanner";

describe("SenderModeBanner", () => {
    test("renders when sender is testing", async () => {
        render(<SenderModeBannerBase customerStatus="testing" />);
        const text = await screen.findByText("Learn more about onboarding.");
        expect(text).toBeInTheDocument();
    });
});
