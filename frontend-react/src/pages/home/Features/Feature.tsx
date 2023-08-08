import React from "react";
import DOMPurify from "dompurify";

import { FeatureProp, SectionProp } from "../HomeProps";

export default function Feature({
    section,
    feature,
}: {
    section: SectionProp;
    feature: FeatureProp;
}) {
    let cleanSummaryHtml = DOMPurify.sanitize(feature!.summary!);
    const totalFeatures = section.features?.length || 0;
    let gridColValue = 12 / totalFeatures;
    const test = `tablet:grid-col-${gridColValue} margin-bottom-0`;

    return (
        <div className={test}>
            {feature.img && (
                <img
                    src={feature.img}
                    alt=""
                    className={feature.imgClassName}
                />
            )}
            {feature.title && (
                <h3
                    data-testid="heading"
                    className="usa-prose font-sans-lg padding-top-3 border-top-05 border-primary"
                >
                    {feature.title}
                </h3>
            )}
            <p
                data-testid="summary"
                className="usa-prose maxw-mobile-lg"
                dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
            ></p>
        </div>
    );
}
