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

const configOverlapFixture = {
    PAGE_META: {
        defaults: {
            openGraph: {
                image: {
                    src: "/assets/img/test/test.png",
                    altText: "test",
                },
            },
        },
    },
};

const frontmatterOverlapFixture = {
    meta: {
        openGraph: {
            image: {
                altText: "overlap test",
            },
        },
    },
};

const frontmatterOverlapEmptyFixture = {
    meta: {
        openGraph: {
            image: {
                altText: "",
            },
        },
    },
};

describe("createMeta", () => {
    test("merges separate areas", () => {
        const meta = createMeta(configFixture as any, frontmatterFixture);
        expect(meta.description).toBe(configFixture.PAGE_META.defaults.description);
        expect(meta.openGraph).toEqual(frontmatterFixture.meta.openGraph);
    });
    test("merges overlapping areas", () => {
        const meta = createMeta(configOverlapFixture as any, frontmatterOverlapFixture as any);
        expect(meta.openGraph.image.src).toBe(configOverlapFixture.PAGE_META.defaults.openGraph.image.src);
        expect(meta.openGraph.image.altText).toBe(frontmatterOverlapFixture.meta.openGraph.image.altText);
    });
    test("doesn't merge empty strings", () => {
        const meta = createMeta(configOverlapFixture as any, frontmatterOverlapEmptyFixture as any);
        expect(meta.openGraph.image.altText).toBe(configOverlapFixture.PAGE_META.defaults.openGraph.image.altText);
    });
});
