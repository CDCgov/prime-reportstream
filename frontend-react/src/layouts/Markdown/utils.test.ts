import { createMeta } from "./utils";

const configFixture = {
    PAGE_META: {
        defaults: {
            description: "test description",
        },
    },
};

const frontmatterFixture = {
    meta: {
        openGraph: {
            image: {
                src: "/assets/img/test/test.png",
                altText: "test",
            },
        },
    },
};

describe("createMeta", () => {
    test("merges", () => {
        const meta = createMeta(configFixture as any, frontmatterFixture);
        expect(meta.description).toBe(
            configFixture.PAGE_META.defaults.description,
        );
        expect(meta.openGraph).toEqual(frontmatterFixture.meta.openGraph);
    });
});
