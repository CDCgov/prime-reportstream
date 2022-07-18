import { Switch } from "react-router-dom";

import { ContentDirectory, GeneratedRouter } from "./MarkdownDirectory";
import GeneratedSideNav from "./GeneratedSideNav";

const StaticPagesFromDirectories = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={directories} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Switch>
                        <GeneratedRouter directories={directories} />
                    </Switch>
                </section>
            </div>
        </section>
    );
};

export default StaticPagesFromDirectories;
