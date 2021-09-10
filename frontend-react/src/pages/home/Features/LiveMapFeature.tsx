import { FeatureProp } from "../HomeProps";

export default function LiveMapFeature({ feature }: { feature: FeatureProp }) {
    return (
        <>
            <div className="tablet:grid-col-4 margin-bottom-0">
                <h3 className="font-sans-md tablet:font-sans-lg padding-top-3 border-top-05 border-base-lighter">
                    {feature.title}
                </h3>
                <p className="usa-prose">{feature.summary}</p>
            </div>
        </>
    );
};