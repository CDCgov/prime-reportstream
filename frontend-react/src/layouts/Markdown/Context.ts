import { createContext, ReactNode } from "react";

const MarkdownLayoutContext = createContext<{
    sidenavContent?: ReactNode;
    setSidenavContent: (jsx: ReactNode) => void;
    mainContent?: ReactNode;
    setMainContent: (jsx: ReactNode) => void;
}>({} as any);

export default MarkdownLayoutContext;
