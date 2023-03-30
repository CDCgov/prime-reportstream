import { IASection } from "../../../content/resources";
import { ContentDirectory } from "../MarkdownDirectory";

export type ContentMap = Map<string | IASection, ContentDirectory[]>; // Key should be section title

export interface IAComponentProps {
    directories: ContentDirectory[] | ContentMap;
}
