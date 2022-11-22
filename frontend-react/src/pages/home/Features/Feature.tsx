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
        return (
            <div className="tablet:grid-col-4 margin-bottom-0">
                <h3
                    data-testid="heading"
                    className="font-sans-md tablet:font-sans-lg padding-top-3 border-top-05 border-base-lighter"
                >
                    {feature.title}
                </h3>
                <p
                    data-testid="summary"
                    className="usa-prose"
                    dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
                ></p>
            </div>
        );
    }
}
