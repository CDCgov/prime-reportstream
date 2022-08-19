import { screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import {
    ContentDirectoryTools,
    makeSectionFromTitles,
} from "../PageGenerationTools";
import { ContentDirectory, ElementDirectory } from "../MarkdownDirectory";

import {
    ContentMap,
    IACardGridProps,
    IACardGridTemplate,
} from "./IACardGridTemplate";

enum TestDirPages {
    ONE = "one",
    TWO = "two",
}
const dirTools = new ContentDirectoryTools()
    .setTitle("Test Directory")
    .setSubtitle("A sample directory for tests")
    .setRoot("/test")
    .setSlugs([
        { key: TestDirPages.ONE, slug: "slug-one" },
        { key: TestDirPages.TWO, slug: "slug-two" },
    ]);
const testDirectories: ContentDirectory[] = [
    new ElementDirectory()
        .setTitle(TestDirPages.ONE)
        .setDescription("Item One")
        .setSlug(dirTools.prependRoot(TestDirPages.ONE)),
    new ElementDirectory()
        .setTitle(TestDirPages.TWO)
        .setDescription("Item Two")
        .setSlug(dirTools.prependRoot(TestDirPages.TWO)),
];
const testContentMap: ContentMap = new Map()
    .set(
        "Section 1",
        makeSectionFromTitles([TestDirPages.ONE], testDirectories)
    )
    .set(
        "Section 2",
        makeSectionFromTitles([TestDirPages.TWO], testDirectories)
    );
const arrayPageProps: IACardGridProps = {
    title: dirTools.title,
    subtitle: dirTools.subtitle,
    directoriesToRender: testDirectories,
};
const mapPageProps: IACardGridProps = {
    title: dirTools.title,
    subtitle: dirTools.subtitle,
    directoriesToRender: testContentMap,
};
const TestCardGrid = () => <IACardGridTemplate {...arrayPageProps} />;
const TestSectionedCardGrid = () => <IACardGridTemplate {...mapPageProps} />;

describe("IACardGridTemplate", () => {
    test("renders single card gird", () => {
        renderWithRouter(<TestCardGrid />);
        // Asserts card for ElementDirectory "ONE" is there
        expect(screen.getByText(TestDirPages.ONE)).toBeInTheDocument();
        // Asserts card for ElementDirectory "TWO" is there
        expect(screen.getByText(TestDirPages.TWO)).toBeInTheDocument();
    });
    test("renders sectioned card grid", () => {
        renderWithRouter(<TestSectionedCardGrid />);
        expect(screen.getByText("Section 1")).toBeInTheDocument();
        expect(screen.getByText("Section 2")).toBeInTheDocument();
        // Asserts card for ElementDirectory "ONE" is there
        expect(screen.getByText(TestDirPages.ONE)).toBeInTheDocument();
        // Asserts card for ElementDirectory "TWO" is there
        expect(screen.getByText(TestDirPages.TWO)).toBeInTheDocument();
    });
});
