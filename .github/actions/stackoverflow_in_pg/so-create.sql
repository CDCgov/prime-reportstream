ALTER TABLE Comments DROP CONSTRAINT comments_id_fkey;
DROP TABLE Votes;
DROP TABLE Tags;
DROP TABLE Posts;
DROP TABLE Comments;
DROP TABLE Users;
DROP TABLE PostLinks;
DROP TABLE PostHistory;
DROP TABLE Badges;



CREATE TABLE Users (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    reputation          INTEGER NOT NULL,           --Reputation
    creation            TIMESTAMP NOT NULL,         --CreationDate
    name                TEXT,                       --DisplayName Yes, can be null some times
    lastaccess          TIMESTAMP,                  --LastAccessDate
    website             TEXT,                       --WebsiteUrl
    location            TEXT,                       --Location
    aboutme             TEXT,                       --AboutMe
    views               INTEGER,                    --Views
    upvotes             INTEGER,                    --upvotes
    downvotes           INTEGER,                    --downvotes
    age                 INTEGER                     --age
);

CREATE TABLE Comments (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    postid              INTEGER NOT NULL,           --PostId
    score               INTEGER,                    --Score
    text                TEXT,                       --Text
    creation            TIMESTAMP NOT NULL,         --CreationDate
    userid              INTEGER                     --UserId
);

CREATE TABLE Posts (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    type                INTEGER NOT NULL,           --PostTypeId
    creation            TIMESTAMP NOT NULL,         --CreationDate
    score               INTEGER,                    --Score
    viewcount           INTEGER,                    --ViewCount
    title               TEXT,                       --Title
    body                TEXT,                       --Body
    userid              INTEGER,                    --OwnerUserId
    lastactivity        TIMESTAMP,                  --LastActivityDate
    tags                TEXT,                       --Tags
    answercount         INTEGER,                    --AnswerCount
    commentcount        INTEGER                     --CommentCount
    );

CREATE TABLE Tags (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    name                TEXT UNIQUE NOT NULL,       --TagName
    count               INTEGER,                    --Count
    excerptpost         INTEGER,                    --ExcerptPostId
    wikipost            INTEGER                     --WikiPostId
);

CREATE TABLE Votes (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    type                INTEGER NOT NULL,           --VoteTypeId
    postid              INTEGER NOT NULL,           --PostId
    creation            DATE NOT NULL               --CreationDate
);

CREATE TABLE PostLinks (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    creation            TIMESTAMP NOT NULL,         --CreationDate
    postid              INTEGER,                    --PostId
    relatedpostid       INTEGER,                    --RelatedPostId
    linktypeid          INTEGER                     --LinkTypeId
);

CREATE TABLE PostHistory (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    type                INTEGER,                    --PostHistoryTypeId
    postid              INTEGER,                    --PostId
    revisionguid        TEXT,                       --RevisionGUID
    creation            TIMESTAMP NOT NULL,         --CreationDate
    userid              INTEGER,                    --UserId
    userdisplaymame     TEXT,                       --UserDisplayName
    text                TEXT                        --Text
);

CREATE TABLE Badges (
    id                  INTEGER UNIQUE NOT NULL,    --Id
    userid              INTEGER,                    --UserId
    name                TEXT,                       --Name
    date                TIMESTAMP,                  --Date
    badgeclass          INTEGER,                    --Class
    tagbased            TEXT                        --TagBased
);

