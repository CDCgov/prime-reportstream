import { useContext, useEffect } from "react";

import { USSmartLink } from "../../components/USLink";

import MarkdownLayoutContext from "./Context";

export const LayoutSidenav = ({ children }: { children: React.ReactNode }) => {
    const { setSidenavContent } = useContext(MarkdownLayoutContext);

    useEffect(() => {
        setSidenavContent(children);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return null;
};

export const LayoutMain = ({ children }: { children: React.ReactNode }) => {
    const { setMainContent } = useContext(MarkdownLayoutContext);

    useEffect(() => {
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
