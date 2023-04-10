interface MDXProviderProps {
    children: React.ReactNode;
}

export function MDXProvider({ children }: MDXProviderProps) {
    return <>{children}</>;
}
