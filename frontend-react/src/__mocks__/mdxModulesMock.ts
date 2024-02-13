const modules = new Proxy(
    {},
    {
        get() {
            // eslint-disable-next-line import/no-unresolved
            return () => import("../content/markdown-example.mdx");
        },
    },
);

export default modules;
