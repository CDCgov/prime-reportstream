import { screen } from "@testing-library/react";

import { render } from "../utils/Test/render";

import Crumbs, { CrumbConfig, CrumbsProps } from "./Crumbs";

describe("Crumbs", () => {
    test("no crumbs to render", () => {
        render(<Crumbs />);
        const noCrumbs = screen.getByText("No crumbs given");
        expect(noCrumbs).toBeInTheDocument();
    });

    test("crumbs to render", () => {
        const sampleConfig: CrumbConfig = { label: "TEST", path: "/sample" };
        const sampleProps: CrumbsProps = {
            crumbList: [sampleConfig] as CrumbConfig[],
        };
        render(<Crumbs {...sampleProps} />);
        const testCrumb = screen.getByRole("link");
        expect(testCrumb).toHaveTextContent("TEST");
    });

    test("all crumbs render", () => {
        const sampleConfigs: CrumbConfig[] = [
            { label: "TEST", path: "/sample" },
            { label: "TEST2", path: "/sample/2" },
            { label: "TEST3", path: "/sample/" },
        ];
        render(<Crumbs crumbList={sampleConfigs} />);
        const allCrumbs = screen.getAllByRole("link");
        expect(allCrumbs.length).toEqual(3);
    });
});
