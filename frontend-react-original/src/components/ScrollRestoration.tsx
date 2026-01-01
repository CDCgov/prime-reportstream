import { useLayoutEffect } from "react";
import { ScrollRestoration, useLocation } from "react-router-dom";

const ScrollToHashElement = () => {
    const location = useLocation();

    useLayoutEffect(() => {
        const hashElement = location.hash
            ? document.getElementById(location.hash.slice(1))
            : undefined;

        const scrollOptions: ScrollIntoViewOptions = {
            behavior: "smooth",
            inline: "nearest",
        };

        const obs = new MutationObserver((mutations, obs) => {
            for (const mutation of mutations) {
                for (const added of mutation.addedNodes) {
                    if (
                        added instanceof HTMLElement &&
                        added.id === location.hash
                    ) {
                        added.scrollIntoView(scrollOptions);
                        obs.disconnect();
                    }
                }
            }
        });

        if (hashElement) {
            // Unknown why sync call to scrollIntoView doesn't work
            setTimeout(() => hashElement.scrollIntoView(scrollOptions), 100);
        } else if (location.hash) {
            obs.observe(document.body, {
                childList: true,
                subtree: true,
                attributes: true,
            });
        }

        return () => obs.disconnect();
    }, [location.pathname, location.hash]);

    return (
        <>
            <ScrollRestoration />
        </>
    );
};

export default ScrollToHashElement;
