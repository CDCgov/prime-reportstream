// compiled css so the resources process for the static site by the compiler
import "../content/generated/global.out.css";

export interface AppLayoutProps extends React.PropsWithChildren {}

const AppLayout = ({ children }: AppLayoutProps) => {
    return <>{children}</>;
};

export default AppLayout;
