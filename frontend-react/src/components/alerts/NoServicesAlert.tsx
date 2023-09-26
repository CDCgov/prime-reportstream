import React from "react";

import { Link } from "../../shared/Link/Link";
import site from "../../content/site.json";

export const NoServicesBanner = () => {
    return (
        <>
            <section className="rs-text-align-center">
                <img
                    className="margin-bottom-6"
                    src="/assets/folder-search.svg"
                    alt="Map of states using ReportStream"
                />
                <h3>No available data</h3>
                <p>
                    If you need more help finding what you're looking for,{" "}
                    <Link href={site.forms.contactUs.url}>contact us</Link>.
                </p>
            </section>
        </>
    );
};
