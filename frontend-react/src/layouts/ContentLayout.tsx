// compiled css so the resources process for the static site by the compiler
import "../content/generated/content.out.css";

export interface ContentLayoutProps extends React.PropsWithChildren {}

const ContentLayout = ({ children }: ContentLayoutProps) => {
    return <>{children}</>;
};

export default ContentLayout;
