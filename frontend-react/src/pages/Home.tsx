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

export const Home = () => {
    return (
        <>
          <Hero />
            <section className="grid-container">

              <section className="usa-section margin-y-0 padding-y-0">
                <div className="grid-row grid-gap margin-bottom-0">
                  <div className="tablet:grid-col-8">
                    <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">{content.partners[0].title}</h2>
                    <p className="font-sans-lg margin-top-1 text-base">{content.partners[0].summary}</p>
                  </div>
                </div>
                <div className="grid-row grid-gap margin-bottom-8 padding-bottom-8 border-bottom-1px border-base-lighter">
                  { 
                    content.partners[0].features.map( feature => <Feature key={feature.title} feature={feature} />)
                  }            
                </div>
              </section>

              <section className="usa-section margin-top-0 padding-top-0 padding-bottom-6 border-bottom-1px border-base-lighter">
                <div className="grid-row grid-gap  margin-bottom-2 ">
                  <div className="tablet:grid-col-8">
                    <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">{content.cta.title}</h2>
                    <p className="font-sans-lg margin-top-1 margin-bottom-6">{content.cta.summary}</p>
                    <a className="usa-button" href={"mailto:" + site.orgs.RS.email}>Contact us at {site.orgs.RS.email}</a>
                  </div>
                </div>
              </section>

              <section className="usa-section">
                <div className="grid-row grid-gap  margin-bottom-2 padding-top-2">
                  {
                    content.freeSecure.map( (freeSecure, idx) => <FreeSecure key={idx} feature={freeSecure} /> )
                  }
                </div>
              </section>

            </section>
      </>
    );
}

export default Home;