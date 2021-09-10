import content from '../../content/content.json'

export default function Hero() {
    return (
        <section className="rs-hero">
            <div className="grid-container usa-section margin-bottom-10">
                <div className="grid-row grid-gap margin-bottom-0 rs-hero__flex-center text-center">
                    <div className="tablet:grid-col-10 tablet:grid-offset-1">
                        <h1 className="tablet:font-sans-3xl">
                            {content.title}
                        </h1>
                    </div>
                    <div className="tablet:grid-col-8 tablet:grid-offset-2">
                        <p className="font-sans-lg line-height-sans-4">
                            {content.summary}
                        </p>
                    </div>
                </div>
            </div>
        </section>
    );
}
