import { screen, render } from "@testing-library/react";

import { StaticAlert } from "./StaticAlert";

describe("StaticAlert", () => {
    test("renders correct class for success", async () => {
        render(<StaticAlert type={"success"} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--success");
    });

    test("renders correct class for success", async () => {
        render(<StaticAlert type={"error"} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--error");
    });
});
