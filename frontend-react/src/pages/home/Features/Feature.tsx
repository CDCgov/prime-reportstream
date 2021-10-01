import DeliveryMethodsFeature from "./DeliveryMethodsFeature";
import { FeatureProp, SectionProp } from "../HomeProps";
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
    } else
        return (
            <div className="tablet:grid-col-4 margin-bottom-0">
                <h3 className="font-sans-md tablet:font-sans-lg padding-top-3 border-top-05 border-base-lighter">
                    {feature.title}
                </h3>
                <p
                    className="usa-prose"
                    dangerouslySetInnerHTML={{ __html: feature!.summary! }}
                ></p>
            </div>
        );
};