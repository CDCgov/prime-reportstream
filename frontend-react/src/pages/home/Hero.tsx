import content from '../../content/content.json'

export default function Hero() {
    return (
        <section className="rs-hero">
            <div className="grid-container usa-section padding-y-2 tablet:padding-y-4 tablet:margin-bottom-4">
                <div className="grid-row grid-gap margin-bottom-0 rs-hero__flex-center ">
                    <div className="tablet:grid-col-10">
                        <h1 className="font-sans-xl tablet:font-sans-2xl margin-top-2">
                            {content.title}
                        </h1>
                        <p className="font-sans-lg line-height-sans-4">
                            {content.summary}
                        </p>
                    </div>
                </div>
            </div>
        </section>
    );
}
