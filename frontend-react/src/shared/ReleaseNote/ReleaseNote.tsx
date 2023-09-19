import { Tag } from "@trussworks/react-uswds";
import { ReactNode } from "react";

import styles from "./ReleaseNote.module.scss";

type Section = {
    title: string;
    tag: "feature" | "bug" | "announcement" | "improvement";
    body: ReactNode;
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
        <div className={styles.ReleaseNote}>
            <p>{header}</p>
            {sections.map((section: Section) => (
                <div>
                    <p>{section.title}</p>
                    <div className="section-container">
                        <NoteTag tag={section.tag} />
                        {section.body}
                    </div>
                </div>
            ))}
        </div>
    );
}
