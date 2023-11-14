import { screen } from "@testing-library/react";

import { render } from "../utils/CustomRenderUtils";

import { StaticAlert, StaticAlertType } from "./StaticAlert";

describe("StaticAlert", () => {
    test("renders correct class for success", async () => {
        render(<StaticAlert type={StaticAlertType.Success} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--success");
    });

    test("renders correct class for success", async () => {
        render(<StaticAlert type={StaticAlertType.Error} heading={"any"} />);

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--error");
    });
});
