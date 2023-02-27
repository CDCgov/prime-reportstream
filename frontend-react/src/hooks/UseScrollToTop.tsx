import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export const useScrollToTop = () => {
    const { pathname } = useLocation();

    useEffect(() => {
        const canControlScrollRestoration =
            "scrollRestoration" in window.history;
        if (canControlScrollRestoration) {
            window.history.scrollRestoration = "manual";
        }
    }, []);

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [pathname]);

    return null;
};
