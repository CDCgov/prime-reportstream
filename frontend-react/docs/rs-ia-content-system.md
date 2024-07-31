# IA Content System

The template system handles the rendering and generation of components, while the content
system handles the bundling and exporting of site content written in markdown.

> As of `Sep 2, 2022`, we are aiming to transition off of tsx elements as more functionality
> is added to the markdown renderer.

## Set up your index

First, under `src/content` add your content folder (e.x. `product`) and create an `index.ts`
file inside. This file exports your `DirectoryTools` and your array of `ContentDirectory` 
objects to render.

### Slugs

Your content needs to be properly routed. In the template system, we handle generating it based
on your defined slugs. To define your slugs, set up the following code:

```typescript
// Enumeration of names ensures no bad strings anywhere!
enum HelpDirectories {
    FAQ = "Frequently Asked Questions"
}
// This array gets passed into your DirectoryTools in just a moment!
const slugs: SlugParams[] = [
    { key: SubDirectories.FAQ, slug: "faq" },
    // Will result in: localhost:3000/help/faq
    // We define the `help` portion of the url in a moment
];
```

### Directory tools

Now that you have your slugs set up, you can pass them into a new `DirectoryTool` object and
let that code handle the rest.

```typescript
/* Tools to help generate Directories */
export const HelpDirectoryTools = new ContentDirectoryTools()
    .setTitle("Help")
    .setSubtitle("I need somebody, help!")
    .setRoot("/help") // <-- Defines slug's root
    .setSlugs(slugs); // <-- Pass in slugs
```

### Your content

Lastly, your content needs to be bundled into an array of objects. You have two options, either
the `MarkdownDirectory` or `ElementDirectory`. You can use them interchangeably as needed.

```typescript jsx
export const helpDirectories: ContentDirectory[] = [
    new MarkdownDirectory()
        .setTitle("FAQ")
        .setSlug(HelpDirectoryTools.getSlug(HelpDirectories.FAQ))
        .addFile(FaqMd), // <-- Imported MD file
    new ElementDirectory()
        .setTitle("Another one")
        .setSlug(HelpDirectoryTools.getSlug(HelpDirectories.ANOTHER_ONE))
        .addElement(AnotherOneTsx), // <-- Imported element from TSX file
];
```

### Boilerplate

This copy/paste of a blank `index.ts` should help get you started!

```typescript
import {ContentDirectoryTools} from "./PageGenerationTools";
import {ContentDirectory} from "./MarkdownDirectory";

// Name your content pages
enum MyDirectories {}

// Define your slugs
const slugs: SlugParams[] = [];

// Create your tools
export const MyDirectoryTools = new ContentDirectoryTools()
    .setTitle("My Directory")
    .setSubtitle("A content system template")
    .setRoot("/my-root")
    .setSlugs(slugs)

// Package and export content
export const myContentDirectories: ContentDirectory[] = [];
```

## How to

### Import a markdown file as a module
Use a relative path to the file, and **explicitly** add the `.md` extension to the import string.
This tells TypeScript that we want it to import MD as a module, which is what we require for `MarkdownDirectory`.
```typescript
import MyMdFile from "./file.md";
```
