import { screen } from "@testing-library/react";
import DateRangePickerAtomic from "./DateRangePickerAtomic";
import { renderApp } from "../../../../utils/CustomRenderUtils";

describe("DateRangePickingAtomic", () => {
    test("renders", () => {
        renderApp(
            <DateRangePickerAtomic
                defaultStartDate={new Date("2022-07-11T00:00:00.000Z")}
                defaultEndDate={new Date("2022-07-13T00:00:00.000Z")}
                onChange={(_props) => void 0}
            />,
        );
        expect(screen.getByText(/7\/11\/2022/)).toBeInTheDocument();
    });
});
