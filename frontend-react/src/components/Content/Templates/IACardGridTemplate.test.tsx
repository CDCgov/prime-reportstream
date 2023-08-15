import { screen } from "@testing-library/react";

import { renderApp } from "../../../utils/CustomRenderUtils";
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
        .setSlug(dirTools.getSlug(TestDirPages.ONE)),
    new ElementDirectory()
        .setTitle(TestDirPages.TWO)
        .setDescription("Item Two")
        .setSlug(dirTools.getSlug(TestDirPages.TWO)),
];
// Split your main array into sections
const testContentMap: ContentMap = new Map()
    .set(
        "Section 1",
        makeSectionFromTitles([TestDirPages.ONE], testDirectories),
    )
    .set(
        "Section 2",
        makeSectionFromTitles([TestDirPages.TWO], testDirectories),
    );
// Props for a single array page
const arrayPageProps: IACardGridProps = {
    pageName: "title",
    subtitle: "subtitle",
    directories: testDirectories, // <- ContentDirectory[]
};
// Props for a sectioned page
const mapPageProps: IACardGridProps = {
    pageName: "Title",
    subtitle: "Subtitle",
    directories: testContentMap, // <- Map<string, ContentDirectory[]>
};

// Test components
const TestCardGrid = () => <IACardGridTemplate {...arrayPageProps} />;
const TestSectionedCardGrid = () => <IACardGridTemplate {...mapPageProps} />;

describe("IACardGridTemplate", () => {
    test("renders single card gird", () => {
        renderApp(<TestCardGrid />);
        // Asserts card for ElementDirectory "ONE" is there
        expect(screen.getByText(TestDirPages.ONE)).toBeInTheDocument();
        // Asserts card for ElementDirectory "TWO" is there
        expect(screen.getByText(TestDirPages.TWO)).toBeInTheDocument();
    });
    test("renders sectioned card grid", () => {
        renderApp(<TestSectionedCardGrid />);
        expect(screen.getByText("Title")).toBeInTheDocument();
        expect(screen.getByText("Subtitle")).toBeInTheDocument();
        expect(screen.getByText("Section 1")).toBeInTheDocument();
        expect(screen.getByText("Section 2")).toBeInTheDocument();
        // Asserts card for ElementDirectory "ONE" is there
        expect(screen.getByText(TestDirPages.ONE)).toBeInTheDocument();
        // Asserts card for ElementDirectory "TWO" is there
        expect(screen.getByText(TestDirPages.TWO)).toBeInTheDocument();
    });
});
