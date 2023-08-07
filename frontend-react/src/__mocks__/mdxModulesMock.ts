const modules = new Proxy(
    {},
    {
        get() {
            return () => import("../content/markdown-example.mdx");
        },
    },
);

export default modules;
