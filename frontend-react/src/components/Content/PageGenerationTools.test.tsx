import { ContentDirectoryTools } from "./PageGenerationTools";

describe("ContentDirectoryTools", () => {
    test("Default", () => {
        /* Testing default instantiation */
        const tools = new ContentDirectoryTools();
        expect(tools.title).toEqual("");
        expect(tools.slugs).toEqual(new Map());
        expect(tools.root).toEqual("");

        /* Testing full instantiation */
        const fullTools = tools
            .setTitle("Name")
            .setRoot("root")
            .setSlugs([{ key: "Key", slug: "slug" }]);
        expect(fullTools.title).toEqual("Name");
        expect(fullTools.slugs.get("Key")).toEqual("slug");
        expect(fullTools.root).toEqual("root");
    });
    test("Update name", () => {
        const tools = new ContentDirectoryTools().setTitle("Name");
        expect(tools.title).toEqual("Name");
    });
    test("Update slugs", () => {
        const tools = new ContentDirectoryTools().setSlugs([
            { key: "Key", slug: "slug" },
        ]);
        expect(tools.slugs.get("Key")).toEqual("slug");
    });
    test("Update root", () => {
        const tools = new ContentDirectoryTools().setRoot("root");
        expect(tools.root).toEqual("root");
    });
});
