import { ReactNode, useContext, useLayoutEffect } from "react";

import MarkdownLayoutContext from "./Context";
import { USSmartLink } from "../../components/USLink";

export const LayoutSidenav = ({ children }: { children: ReactNode }) => {
    const { setSidenavContent } = useContext(MarkdownLayoutContext);

    useLayoutEffect(() => {
        setSidenavContent(children);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export const LayoutMain = ({ children }: { children: ReactNode }) => {
    const { setMainContent } = useContext(MarkdownLayoutContext);

    useLayoutEffect(() => {
        setMainContent(children);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export const LayoutBackToTop = () => {
    return (
        <USSmartLink className="rs-back-to-top" href="#top">
            Back to top
        </USSmartLink>
    );
};
