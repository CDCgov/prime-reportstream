import React from "react";

import { USExtLink } from "../USLink";
import site from "../../content/site.json";

export const NoServicesBanner = () => {
    return (
        <>
            <section className="rs-text-align-center">
                <img src="/assets/folder-search.svg" alt="" />
                <h3 className="margin-top-0">No available data</h3>
                <p>
                    If you need more help finding what you're looking for,{" "}
                    <USExtLink href={site.forms.contactUs.url}>
                        contact us
                    </USExtLink>
                    .
                </p>
            </section>
        </>
    );
};
