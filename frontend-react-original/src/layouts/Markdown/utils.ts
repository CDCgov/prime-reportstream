import { mergeWith } from "lodash";
import { AppConfig } from "../../config";

/**
 * Deep merge default meta with frontmatter meta. Has customizer that takes empty strings into account.
 */
export function createMeta(config: AppConfig, frontmatter: Frontmatter) {
    return mergeWith(
        {},
        config.PAGE_META.defaults,
        frontmatter.meta,
        (a, b) => {
            if (typeof b === "string" && b === "") return a;
            return undefined;
        },
    );
}
