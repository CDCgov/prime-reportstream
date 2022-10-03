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
                jsonDiffMode={true}
            />
        );
        expect(screen.getByText("left json value")).toBeInTheDocument();
        expect(screen.getByText("right json value")).toBeInTheDocument();
    });

    test("text diff", () => {
        const leftJson = JSON.stringify({ key: "left json value" }, null, 2);
        const rightJson = JSON.stringify({ key: "right json value" }, null, 2);
        render(
            <StaticCompare
                leftText={leftJson}
                rightText={rightJson}
                jsonDiffMode={false}
            />
        );
        expect(screen.getByText("left json value")).toBeInTheDocument();
        expect(screen.getByText("right json value")).toBeInTheDocument();
    });
});
