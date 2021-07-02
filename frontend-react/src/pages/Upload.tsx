import content from '../content/content.json'
import site from '../content/site.json'


interface FeatureProp {
  feature: {
    icon?: string,
    iconAlt?: string,
    title: string,
    summary: string
  }
}

const Feature = (props: FeatureProp) => {
    return (
      <div className="tablet:grid-col-4 margin-bottom-0">              
      <h3>
        <img src={"/assets/" + props.feature.icon} aria-hidden="true" alt={"Icon of " + props.feature.iconAlt} />
        {props.feature.title}
      </h3>
      <p className="usa-prose">{props.feature.summary}</p>
    </div>
    );
  }
  
  const FreeSecure = (props: FeatureProp) => {
    return (
      <div className="tablet:grid-col-6">
        <img src={"/assets/" + props.feature.icon} aria-hidden="true" alt={"Icon of " + props.feature.iconAlt} className="height-7"/>
        <h3 className="font-sans-lg">{props.feature.title}</h3>
        <p className="usa-prose">{props.feature.summary}</p>
      </div>
    );
  }

const Hero = () => {
  return (
    <section className="rs-hero">
    <div className="grid-container usa-section margin-bottom-10">
      <div className="grid-row grid-gap margin-bottom-0 rs-hero__flex-center">
        <div className="tablet:grid-col-6">
          <h1>{content.title}</h1>
          <p className="font-sans-lg line-height-sans-4">{content.summary}</p>
        </div>
        <div className="tablet:grid-col-6">
          <img className="shadow-2" src="/assets/report-stream-application.png"
            alt="Example screenshot of the ReportStream web application" />
        </div>
      </div>
    </div>
  </section>
  );
}

export const Upload = () => {
    return (
        <>
          <Hero />

      </>
    );
}

export default Upload