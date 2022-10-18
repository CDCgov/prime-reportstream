import { render, screen } from "@testing-library/react";

import { StaticCompare } from "./StaticCompare";

describe("StaticCompare", () => {
    test("json diff", () => {
        const leftJson = JSON.stringify({ key: "left json value" }, null, 2);
        const rightJson = JSON.stringify({ key: "right json value" }, null, 2);
        render(
            <StaticCompare
                leftText={leftJson}
                rightText={rightJson}
                jsonDiffMode={false}
            />
        );
        const leftCompare = screen.getByTestId("left-compare-text");
        expect(leftCompare.innerHTML).toContain(
            `"key": "<mark>lef</mark>t json value"`
        );

        const rightCompare = screen.getByTestId("right-compare-text");
        expect(rightCompare.innerHTML).toContain(
            `"key": "<mark>righ</mark>t json value"`
        );
    });
});
