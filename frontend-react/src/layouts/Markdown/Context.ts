import { createContext } from "react";

const MarkdownLayoutContext = createContext<{
    sidenavContent?: React.ReactNode;
    setSidenavContent: (jsx: React.ReactNode) => void;
    mainContent?: React.ReactNode;
    setMainContent: (jsx: React.ReactNode) => void;
}>({} as any);

export default MarkdownLayoutContext;
