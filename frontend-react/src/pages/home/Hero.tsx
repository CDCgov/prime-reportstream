import content from "../../content/content.json";

export default function Hero() {
    return (
        <section className="rs-hero margin-top-neg-4 desktop:margin-top-0">
            <div className="grid-container usa-section padding-y-4 tablet:padding-y-6 tablet:margin-bottom-4">
                <div className="grid-row grid-gap margin-bottom-0 rs-hero__flex-center ">
                    <div className="tablet:grid-col-10">
                        <h1
                            data-testid="heading"
                            className="font-sans-xl tablet:font-sans-2xl margin-top-2"
                        >
                            {content.title}
                        </h1>
                        <p
                            data-testid="summary"
                            className="font-sans-lg line-height-sans-4"
                        >
                            {content.summary}
                        </p>
                    </div>
                </div>
            </div>
        </section>
    );
}
