#!/usr/bin/env python

import xml.etree.cElementTree as ElementTree
import os
import sys
import re

db = "so"
encoding = "UTF-8"

def escape(str):
    str = re.sub("\s", " ", str)
    # \ are special for Python strings *and* for regexps. Hence the
    # multiple escaping. Here,we just replace every \ by \\ for
    # PostgreSQL
    str = re.sub("\\\\", "\\\\\\\\", str)
    return str

def tag_parse(str):
    index = 0
    while index < len(str):
        if str[index] == '<':
            try:
                end_tag = str[index:].index('>')
                yield str[(index+1):(index+end_tag)]
                index += end_tag + 1
            except ValueError:
                raise Exception("Tag parsing error in \"%s\"" % str);
        else:
            raise Exception("Tag parsing error in \"%s\"" % str);

if len(sys.argv) != 2:
    raise Exception("Usage: %s so-files-directory" % sys.argv[0])

#os.chdir(sys.argv[1])

filename = "Posts.xml"
posts = ElementTree.iterparse(filename) 
tags = {}
tag_id = 1
print ("COPY posts (id, type, creation, score, viewcount, title, body, userid, lastactivity, tags, answercount, commentcount) FROM stdin;")

for event, post in posts:
    if event == "end" and post.tag == "row":
        id = int(post.attrib["Id"])

        #if post.attrib.has_key("PostTypeId"):
        if "PostTypeId" in post.attrib:
            type = int(post.attrib["PostTypeId"])
        else:
            type = "\n"

        creation = post.attrib["CreationDate"]

        #if post.attrib.has_key("Score"):
        if "Score" in post.attrib:
            score = int(post.attrib["Score"])
        else:
            score = "-1"

        #if post.attrib.has_key("ViewCount"):
        if "ViewCount" in post.attrib:
            viewcount = int(post.attrib["ViewCount"])
        else:
            viewcount = "-1"

        #if post.attrib.has_key("Title"):
        if "Title" in post.attrib:
            title = escape(post.attrib["Title"])
        else:
            title = "\n"

        #if post.attrib.has_key("Body"):
        if "Body" in post.attrib:
            body = escape(post.attrib["Body"])
        else:
            body = "\n"

        #if post.attrib.has_key("OwnerUserId"):
        if "OwnerUserId" in post.attrib:
            owner = post.attrib["OwnerUserId"]
        else:
            owner = "-1"

        #if post.attrib.has_key("LastActivityDate"):
        if "LastActivityDate" in post.attrib:
            lastactivity = post.attrib["LastActivityDate"]
        else:
            lastactivity = "\n"
        
        #if post.attrib.has_key("Tags"):
        if "Tags" in post.attrib:
            tags = escape(post.attrib["Tags"])
        else:
            tags = "\n"
        
        #if post.attrib.has_key("AnswerCount"):
        if "AnswerCount" in post.attrib:
            answercount = int(post.attrib["AnswerCount"])
        else:
            answercount = "-1"

        #if post.attrib.has_key("CommentCount"):
        if "CommentCount" in post.attrib:
            commentcount = int(post.attrib["CommentCount"])
        else:
            commentcount = "-1"

        print ("%i\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s" % (id, type, creation, score, viewcount, title.encode(encoding), body.encode(encoding), owner, lastactivity, tags.encode(encoding), answercount, commentcount))
        post.clear()
    
print ("\.")

