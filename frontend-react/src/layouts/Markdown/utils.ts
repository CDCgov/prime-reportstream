import { merge } from "lodash";
import { AppConfig } from "../../config";

export function createMeta(config: AppConfig, frontmatter: Frontmatter) {
    return merge({}, config.PAGE_META.defaults, frontmatter.meta);
}
