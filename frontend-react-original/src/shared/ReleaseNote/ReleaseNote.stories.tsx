// AutoUpdateFileChromatic
import { ReactElement } from "react";

import ReleaseNote from "./ReleaseNote";

export default {
    title: "Components/ReleaseNote",
    component: ReleaseNote,
};

export const Default = (): ReactElement => (
    <ReleaseNote
        header="October 2020"
        sections={[
            {
                title: "Flu pilot launched",
                tag: "feature",
                body: (
                    <p>
                        We launched a pilot program receiving and sending flu
                        data through ReportStream. This is part of continued
                        efforts to expand our capabilities beyond COVID-19. Let
                        us know if you&apos;re interested in receiving flu data
                        from ReportStream.
                    </p>
                ),
            },
            {
                title: "Updates to the ReportStream homepage",
                tag: "improvement",
                body: (
                    <p>
                        The ReportStream homepage more accurately reflects the
                        work we are doing and provides a clear action step for
                        starting to send or receive data.
                    </p>
                ),
            },
            {
                title: "API navigation fixed in mobile view",
                tag: "bug",
                body: (
                    <p>
                        The navigation of the{" "}
                        <a href="https://reportstream.cdc.gov/resources/api">
                            API Guide
                        </a>{" "}
                        no longer overlaps with the content of the page on
                        mobile view.
                    </p>
                ),
            },
            {
                title: "South Dakota now connected with ReportStream",
                tag: "announcement",
                body: (
                    <p>
                        We’re excited to announce that ReportStream can now send
                        public health data to South Dakota.
                    </p>
                ),
            },
        ]}
    />
);
