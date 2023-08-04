/**
 * Vite creates an object of all matches as import functions
 */
const modules = import.meta.glob("../**/*.mdx") as {
    [key: string]: () => Promise<{ default: React.ComponentType<any> }>;
};

export default modules;
