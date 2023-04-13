import { Icon, SiteAlert } from "@trussworks/react-uswds";

import { USLink } from "../USLink";

export default function FileHandlerProgrammersGuideTip() {
    return (
        <SiteAlert variant="info" showIcon={false}>
            <Icon.Lightbulb />
            <span className="padding-left-1">
                Pages 18-29 in the{" "}
                <USLink href="/resources/programmers-guide">
                    API Programmer’s Guide
                </USLink>{" "}
                have the information you need to validate your file
                successfully. Pay special attention to which fields are required
                and common mistakes.
            </span>
        </SiteAlert>
    );
}
