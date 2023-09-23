# Patches

This project takes advantage of npm package patching mechanisms (patch-package/yarn patch)
as a means to correct or make minor enhancements to libraries when there is no means to
accomplish what's needed through library-provided means.

The following packages are currently being patched:

## react-uswds

This library currently erroneously includes a copy of USWDS' style with default settings
inside of its supplementary stylesheet. This creates unintended visual side effects that
are difficult to identify and trace. Our patch changes the stylesheet to just the library-
specific additions as originally intended.

## @okta/okta-signin-widget

This library hardcodes the wrapper template for the okta sign-in widget as a `main` element
with no means of customizing. This is an issue because to be HTML-spec compliant there can
be only one of these element types in a document and our layout code already wraps content
inside of this element type. The way this library is currently coded reduces developer
agency in how the page is rendered. In order to ensure that there are no bugs from getting
our intended control back, we have the offending code patched from `main` to another type
(ex: `div`) rather than using custom react component code to manually manipulate the
DOM (okta-signin-widget is based off of Backbonejs Views, which includes an event system
that could be a source of unknown bugs if we bypass using a manually created element).
