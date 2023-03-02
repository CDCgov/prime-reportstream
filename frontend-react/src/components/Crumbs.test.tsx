import { screen, renderApp } from "../utils/CustomRenderUtils";

import Crumbs, { CrumbConfig, CrumbsProps, WithCrumbs } from "./Crumbs";

describe("Crumbs", () => {
    test("no crumbs to render", () => {
        renderApp(<Crumbs />);
        const noCrumbs = screen.getByText("No crumbs given");
        expect(noCrumbs).toBeInTheDocument();
    });

    test("crumbs to render", () => {
        const sampleConfig: CrumbConfig = { label: "TEST", path: "/sample" };
        const sampleProps: CrumbsProps = {
            crumbList: [sampleConfig] as CrumbConfig[],
        };
        renderApp(<Crumbs {...sampleProps} />);
        const testCrumb = screen.getByRole("link");
        expect(testCrumb).toHaveTextContent("TEST");
    });

    test("all crumbs render", () => {
        const sampleConfigs: CrumbConfig[] = [
            { label: "TEST", path: "/sample" },
            { label: "TEST2", path: "/sample/2" },
            { label: "TEST3", path: "/sample/" },
        ];
        renderApp(<Crumbs crumbList={sampleConfigs} />);
        const allCrumbs = screen.getAllByRole("link");
        expect(allCrumbs.length).toEqual(3);
    });
});

const TestPage = () => {
    return (
        <div>
            <h1>Test Page</h1>
        </div>
    );
};
const fakeCrumbs: CrumbConfig[] = [{ label: "Test label", path: "/test-path" }];
describe("WithCrumbs", () => {
    test("HOC renders crumbs and page", () => {
        renderApp(<WithCrumbs page={<TestPage />} crumbList={fakeCrumbs} />);
        expect(screen.getByText("Test Page")).toBeInTheDocument();
        expect(screen.getByText("Test label")).toBeInTheDocument();
    });
});
