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

// Set up page titles
enum TestDirPages {
    ONE = "one",
    TWO = "two",
}
// Set up directory tools
const dirTools = new ContentDirectoryTools()
    .setTitle("Test Directory") // Name it
    .setSubtitle("A sample directory for tests") // Describe it
    .setRoot("/test") // Index page's base root
    .setSlugs([
        { key: TestDirPages.ONE, slug: "slug-one" },
        { key: TestDirPages.TWO, slug: "slug-two" },
    ]); // Slugs for sub-navigation
// Set up your main directory array
const testDirectories: ContentDirectory[] = [
    new ElementDirectory()
        .setTitle(TestDirPages.ONE) // Name it
        .setDescription("Item One") // Describe it
        .setSlug(dirTools.prependRoot(TestDirPages.ONE)), // Page's path, use `ContentDirectoryTools.prependRoot()`
    new ElementDirectory()
        .setTitle(TestDirPages.TWO)
        .setDescription("Item Two")
        .setSlug(dirTools.prependRoot(TestDirPages.TWO)),
];
// Split your main array into sections
const testContentMap: ContentMap = new Map()
    .set(
        "Section 1",
        makeSectionFromTitles([TestDirPages.ONE], testDirectories)
    )
    .set(
        "Section 2",
        makeSectionFromTitles([TestDirPages.TWO], testDirectories)
    );
// Props for a single array page
const arrayPageProps: IACardGridProps = {
    title: dirTools.title,
    subtitle: dirTools.subtitle,
    directoriesToRender: testDirectories, // <- ContentDirectory[]
};
// Props for a sectioned page
const mapPageProps: IACardGridProps = {
    title: dirTools.title,
    subtitle: dirTools.subtitle,
    directoriesToRender: testContentMap, // <- Map<string, ContentDirectory[]>
};

// Test components
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
