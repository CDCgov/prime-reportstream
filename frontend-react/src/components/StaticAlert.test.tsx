import { screen } from "@testing-library/react";

import { renderWithBase } from "../utils/CustomRenderUtils";

import { StaticAlert, StaticAlertType } from "./StaticAlert";

describe("StaticAlert", () => {
    test("renders correct class for success", async () => {
        renderWithBase(
            <StaticAlert type={StaticAlertType.Success} heading={"any"} />
        );

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--success");
    });

    test("renders correct class for success", async () => {
        renderWithBase(
            <StaticAlert type={StaticAlertType.Error} heading={"any"} />
        );

        const wrapper = await screen.findByRole("alert");
        expect(wrapper).toHaveClass("usa-alert--error");
    });
});
