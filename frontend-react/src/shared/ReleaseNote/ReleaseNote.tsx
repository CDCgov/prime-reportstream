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
        <div className={classnames(styles.ReleaseNote, "grid-container")}>
            <p className="header">{header}</p>
            {sections.map((section: Section) => (
                <div className="section-container">
                    <p className="section-title">{section.title}</p>
                    <div className="grid-row section-content">
                        <div className="grid-col-2">
                            <NoteTag tag={section.tag} />
                        </div>
                        <div className="grid-col-10">{section.body}</div>
                    </div>
                </div>
            ))}
        </div>
    );
}
