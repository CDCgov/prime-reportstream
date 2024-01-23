import { ReactNode } from "react";

interface MDXProviderProps {
    children: ReactNode;
}

export function MDXProvider({ children }: MDXProviderProps) {
    return <>{children}</>;
}
