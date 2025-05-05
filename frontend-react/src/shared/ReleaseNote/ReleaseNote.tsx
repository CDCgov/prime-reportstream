import { Tag } from "@trussworks/react-uswds";
import classnames from "classnames";

import styles from "./ReleaseNote.module.scss";

interface Section {
    title: string;
    tag: "feature" | "bug" | "announcement" | "improvement" | "recently-completed" | "working-on-now" | "next";
    body: JSX.Element;
}

interface ReleaseNoteProps {
    isDivided?: boolean;
    header: string;
    sections: Section[];
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
        case "recently-completed":
            return <Tag className="tag tag--recently-completed">Recently completed</Tag>;
        case "working-on-now":
            return <Tag className="tag tag--working-on-now">Working on now</Tag>;
        case "next":
            return <Tag className="tag tag--next">Next</Tag>;
    }
}

function ReleaseNote({ isDivided = true, header, sections }: ReleaseNoteProps) {
    return (
        <div
            className={
                isDivided
                    ? classnames(styles.ReleaseNote, "margin-top-0", "divider")
                    : classnames(styles.ReleaseNote, "margin-top-0")
            }
        >
            <p className="header">{header}</p>
            {sections.map((section: Section, index) => (
                <div className="section-container" key={`${section.title}-${index}`}>
                    <p className="section-title">{section.title}</p>
                    <div className="grid-row section-content">
                        <div className="grid-col-12 desktop:grid-col-3">
                            <NoteTag tag={section.tag} />
                        </div>
                        <div className="grid-col-12 margin-top-205 desktop:grid-col-9 desktop:margin-top-0">
                            {section.body}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}

export default ReleaseNote;
