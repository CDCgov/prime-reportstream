import { Tag } from "@trussworks/react-uswds";
import classnames from "classnames";

import styles from "./ReleaseNote.module.scss";

type Section = {
    title: string;
    tag: "feature" | "bug" | "announcement" | "improvement";
    body: JSX.Element;
};

interface ReleaseNoteProps {
    header: string;
    sections: Array<Section>;
}

function NoteTag({ tag }: { tag: Section["tag"] }) {
    switch (tag) {
        case "feature":
            return <Tag className="tag tag--feature">New feature</Tag>;
        case "bug":
            return <Tag className="tag tag--bug">Bug fix</Tag>;
        case "announcement":
            return <Tag className="tag tag--announcement">Announcement</Tag>;
        case "improvement":
            return <Tag className="tag tag--improvement">Improvement</Tag>;
    }
}

export function ReleaseNote({ header, sections }: ReleaseNoteProps) {
    return (
        <div className={classnames(styles.ReleaseNote, "margin-top-0")}>
            <p className="header">{header}</p>
            {sections.map((section: Section, index) => (
                <div
                    className="section-container"
                    key={`${section.title}-${index}`}
                >
                    <p className="section-title">{section.title}</p>
                    <div className="grid-row section-content">
                        <div className="grid-col-12 desktop:grid-col-2">
                            <NoteTag tag={section.tag} />
                        </div>
                        <div className="grid-col-12 margin-top-205 desktop:grid-col-10 desktop:margin-top-0">
                            {section.body}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}
