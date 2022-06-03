# Make Markdown Pages

Building static pages from markdown is a simple process. First, you create 
your markdown file, then you create a `MarkdownDirectory` by providing a 
label (seen in navigation), slug (url extension), and array of imported 
markdown files.

## Create a `MarkdownDirectory`

To set up a page, you need one or more directories to render from. These 
will determine what is in your dropdown and side nav.

```typescript jsx
export const BUILT_FOR_YOU: MarkdownDirectory[] = [
    new MarkdownDirectory("June 2022", "june-2022", [june2022]),
    new MarkdownDirectory("May 2022", "may-2022", [may2022]),
];
```

This is an example of our new Built For You set of static pages. Because we 
want to use side-navigation to segment content by month, we set up our directories 
to be labeled by month. The best practice for slugs is to match the title in a url-friendly 
way. Lastly the array of files.

### Adding files

The file array is what is rendered on a specific page. Take, for example, the 
Built For You, May 2022 directory. This holds a single mardown file that 
was imported like so:

```typescript jsx
import may2022 from "../../content/built-for-you/2022-may.md";
```

We can now import markdown files as modules like this. That said, _if_ you 
wanted to segment bi-weekly or semi-monthly updates in separate markdown files, 
you could. To do this, just import both files, and in the directory object, change 
your array to include both files. They will render on the **same page** and in the 
**same order** that you list them in the array.

```typescript jsx
import may2022update2 from //...
export const BUILT_FOR_YOU: MarkdownDirectory[] = [
    /* ... */,
    new MarkdownDirectory(/* ... */ [may2022, may2022update2]),
];
```

## Create a `DropdownNav`

We can now use `MarkdownDirectory` arrays to generate dropdown navigation items 
for our header.

```typescript jsx
export const BuiltForYouDropdown = () => {
    return (
        <DropdownNav
            label="Built For You"
            root="/built-for-you"
            directories={BUILT_FOR_YOU}
        />
    );
};
```

Label, again, shows in the navigation item. The root is the root URL of this set of 
static pages. It's assumed that a dropdown links to a sub set of pages that stem from 
this route, so when you pass in your array of directories, each one's `slug` is applied 
to the url _after_ the `root`. 

```
https://reportstream.cdc.gov/{root}/{slug}
```

Once created, you'll need to add this to the `ReportStreamHeader` component.

## Add the `Route`

In App.tsx, add the `Route` element that links to the root of the markdown pages.

```typescript jsx
<Route
    path="/built-for-you"
    component={BuiltForYouIndex}
/>
```