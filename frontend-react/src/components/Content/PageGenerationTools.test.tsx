import { ContentDirectoryTools } from "./PageGenerationTools";

describe("Page Generation Tools", () => {
    test("ContentDirectoryTools", () => {
        /* Testing default instantiation */
        const tools = new ContentDirectoryTools();
        expect(tools.name).toEqual("");
        expect(tools.slugs).toEqual(new Map());
        expect(tools.root).toEqual("");

        /* Testing each setter's return statement */
        const namedTools = tools.setName("Name");
        expect(namedTools.name).toEqual("Name");

        const slugsTools = tools.setSlugs([{ key: "Key", slug: "slug" }]);
        expect(slugsTools.slugs.get("Key")).toEqual("slug");

        const rootTools = tools.setRoot("root");
        expect(rootTools.root).toEqual("root");

        /* Testing full instantiation */
        const fullTools = new ContentDirectoryTools()
            .setName("Name")
            .setRoot("root")
            .setSlugs([{ key: "Key", slug: "slug" }]);
        expect(fullTools.name).toEqual("Name");
        expect(fullTools.slugs.get("Key")).toEqual("slug");
        expect(fullTools.root).toEqual("root");
    });
});
