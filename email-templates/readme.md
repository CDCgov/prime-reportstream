# Notification email templates

Responsive HTML templates built with [Zurb's Foundation for Emails](https://get.foundation/emails.html).

## Setup & HTML inlining

- [Foundation for Emails documentation](https://get.foundation/emails/docs/).
- To be compatible with a broader array of mail clients, any custom styles in a template `<head>` need to be inlined before going to production. Use a CSS inlining tool (Ex: [Zurb's Responsive Email Inliner](https://get.foundation/emails/inliner.html)) to facilitate the process.

## Updating templates in SendGrid

Email templates are not automatically pulled into SendGrid from this repo. When making edits to existing templates or creating new templates, manually copy any `*_inline.html` template files into SendGrid.
