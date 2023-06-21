import { Icon } from "@trussworks/react-uswds";

import { USLink } from "../USLink";

export default function FileHandlerProgrammersGuideTip() {
    return (
        <section
            data-testid="siteAlert"
            className="usa-site-alert usa-site-alert--info usa-site-alert--no-heading usa-site-alert--no-icon border-primary-lighter border-width-1px border-solid radius-md"
            aria-label="Site alert"
        >
            <div className="usa-alert padding-x-2 display-flex">
                <span className="text-base-dark">
                    <Icon.Lightbulb size={3} />
                </span>
                <div className="padding-left-1">
                    Pages 18-29 in the{" "}
                    <USLink href="/resources/api">ReportStream API</USLink> have
                    the information you need to validate your file successfully.
                    Pay special attention to which fields are required and
                    common mistakes.
                </div>
            </div>
        </section>
    );
}
