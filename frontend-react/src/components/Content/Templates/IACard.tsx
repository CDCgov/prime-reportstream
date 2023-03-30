import { ContentDirectory } from "../MarkdownDirectory";
import { USNavLink } from "../../USLink";

/** Takes an array of ContentDirectories and returns a gird list of IACards
 * with navigation links to the directories, and descriptions.
 * @param dirs {ContentDirectory[]} A list of Markdown and Element directories */
export const IACardList = ({
    dirs,
    order = [],
}: {
    dirs: ContentDirectory[];
    order?: number[];
}) => {
    return (
        <ul className="usa-card-group">
            {dirs.map((res, idx) => (
                <IACard
                    dir={res}
                    key={res.slug}
                    style={{ order: order[idx] }}
                />
            ))}
        </ul>
    );
};

const IACard = ({ dir, style }: { dir: ContentDirectory; style?: {} }) => {
    return (
        <li className="usa-card tablet:grid-col-4" style={style}>
            <div className="usa-card__container">
                <div className="usa-card__header padding-top-1">
                    <USNavLink className={"usa-link"} href={dir.slug}>
                        <h3 className="margin-bottom-0">{dir.title}</h3>
                    </USNavLink>
                </div>
                <div className="usa-card__body">
                    <p>{dir.desc}</p>
                </div>
            </div>
        </li>
    );
};
