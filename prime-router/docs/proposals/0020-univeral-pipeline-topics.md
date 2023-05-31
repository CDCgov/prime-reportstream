# Topic partitioning in the Universal Pipeline

## Introduction

The Universal Pipeline is meant to be the path for all data going through ReportStream. This includes Electronic
Laboratory Reporting (ELR) as well as Electronic Test Orders and Results (ETOR), which are associated with different
message types and have different purposes. These two use cases utilize different message types, require different data
fields to be provided, and will likely be relevant to different sets of senders and receivers.

## Goals

Since ELR and ETOR have different purposes and their data is held to different expectations, we want organization
settings to prevent the two data types (as well as future other data types) from getting mixed up between
organizations (senders/receivers) that are intended for the other data type. The solution for this should be simple,
foolproof, and independent as laid out here:

- Simple: The Requirement is best described with an example: It is likely that ETOR onboarding and operations will be
  managed by Technical Assistants (TA) hired directly by CDC, not by people on the ReportStream team. Those
  non-ReportStream teams need a very simple foolproof way to stovepipe their data, to avoid confusion and errors.
- Foolproof: The implementation should default to not send -- it can't depend on a human remembering to put in an extra
  clause in one of the filters. (Although that solution may be fine for a UP MVP).
- Independent of the data itself: You can easily conceive of situations where the data itself might not contain enough
  info to determine if it belongs within a particular topic. Or, situations where the customers repeatedly change
  datatypes as they set up their system. Therefore, the information that tells us where certain data goes should be tied
  to senders and receivers, and may not be solely determinable from the data itself. Topics should be an abstraction.

## Proposal

Currently, the Universal Pipeline (UP) is used for both use cases, and organization settings specify to use the UP by
setting the `topic` field to `full-elr`. This proposal suggests retaining the `full-elr` topic, but also adding
an `etor-ti` (to specifically indicate ETOR usage in collaboration with partner application Trusted Intermediary, or TI)
topic. Those topics will both signify usage of the UP, however senders for one topic will only be able to send
data to receivers of the same topic. Additionally, each topic may have a different set of default filters; for instance,
the current default quality filter for the `full-elr` topic expects a patient to have contact information, but for
newborn screening ETOR requests, that information won't be available. So a separate default quality filter for
the `etor-ti` topic could set a reasonable starting set of quality expectations that are reasonable for ETOR requests.

### Implications on internal types

Currently, senders in the code are represented by a few different types. The overall parent class is called `Sender`.
`Sender` is an abstract class which is extended by `FullELRSender` (used for UP) and `TopicSender` (used for the legacy
pipeline). `TopicSender` has subtypes `CovidSender` and `MonkeypoxSender`.

Under this proposal, `FullELRSender` will be renamed to `UniversalPipelineSender`, and `TopicSender` will be renamed
to `LegacyPipelineSender` (still with subclasses `CovidSender` and `MonkeypoxSender`). Topic will still be used to
determine which subclass of `Sender` to use; `full-elr` and `etor-ti` will correspond to `UniversalPipelineSender`,
while `covid-19` will correspond to `CovidSender` and `monkeypox` will correspond to `MonkeypoxSender`.

Receivers don't have the same kind of structure as senders, so the `Receiver` class and subclasses will not have the
same kinds of changes. Instead, during routing in the UP, the topic of the receiver will be considered so that reports
from senders only go to receivers with matching topics. This evaluation should occur prior to the evaluation of any
filters for performance reasons.

### Implications on organization settings

After the changes here, the existing settings for ETOR senders and receivers will largely still hold, with the exception
of the value of `topic`, which will be `etor-ti` instead of `full-elr`.

Since ELR senders and receivers have a topic of `full-elr` already, they will not require any changes.

Monkeypox and Covid-19 are special cases initially, but eventually, those will be phased out, and condition-specific
filtering will only occur in the Universal Pipeline by using condition filters. However, to minimize impact in the
short-term, we will continue to support `covid-19` and `monkeypox` as topics, so those settings will not require
changes.
