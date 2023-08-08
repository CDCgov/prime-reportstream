import React from "react";
import DOMPurify from "dompurify";

import { FeatureProp, SectionProp } from "../HomeProps";

import DeliveryMethodsFeature from "./DeliveryMethodsFeature";
import LiveMapFeature from "./LiveMapFeature";

export default function Feature({
    section,
    feature,
}: {
    section: SectionProp;
    feature: FeatureProp;
}) {
    if (section.type === "deliveryMethods") {
        return <DeliveryMethodsFeature feature={feature} />;
    } else if (section.type === "liveMap") {
        return <LiveMapFeature feature={feature} />;
    } else {
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
                        className="usa-prose padding-top-3 border-top-05 border-primary"
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
}
