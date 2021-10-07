import { FeatureProp } from "../HomeProps";
import site from "../../../content/site.json";

export default function DeliveryMethodsFeature({
    feature,
}: {
    feature: FeatureProp;
}) {
    return (
        <div className="grid-col-12 margin-bottom-3">
            <div className="grid-row grid-gap display-flex flex-row flex-align-top">
                <div className="tablet:grid-col-6">
                    <img
                        src={site.imgPath + feature.img}
                        alt="{ feature.imgAlt }"
                    />
                </div>
                <div className="tablet:grid-col-6 ">
                    <h3 className="font-sans-lg margin-top-0 padding-top-3 margin-bottom-1 tablet:border-top-05 tablet:border-base-lighter">
                        {feature.title}
                    </h3>
                    <p className="usa-prose">{feature!.items![0]?.summary}</p>
                    <p className="usa-prose">{feature!.items![1]?.summary}</p>
                </div>
            </div>
        </div>
    );
}
