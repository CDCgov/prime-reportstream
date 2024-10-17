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

filename = "PostHistory.xml"
postHistory = ElementTree.iterparse(filename) 
tags = {}
tag_id = 1
print ("COPY posthistory (id, type, postid, revisionguid, creation, userid, userdisplaymame, text) FROM stdin;")

for event, post in postHistory:
    if event == "end" and post.tag == "row":
        id = int(post.attrib["Id"])

        type = int(post.attrib["PostHistoryTypeId"])

        postid = int(post.attrib["PostId"])

        revisionguid = post.attrib["RevisionGUID"]

        creation = post.attrib["CreationDate"]

        #if post.attrib.has_key("UserId"):
        if "UserId" in post.attrib:
            userid = int(post.attrib["UserId"])
        else:
            userid = -1

        #if post.attrib.has_key("UserDisplayName"):
        if "UserDisplayName" in post.attrib:
            userdisplaymame = escape(post.attrib["UserDisplayName"])
        else:
            userdisplaymame = "\n"

        #if post.attrib.has_key("Text"):
        if "Text" in post.attrib:
            text = escape(post.attrib["Text"])
        else:
            text = "\n"

        print ("%i\t%s\t%s\t%s\t%s\t%s\t%s\t%s" % (id, type, postid, revisionguid, creation, userid, userdisplaymame.encode(encoding), text.encode(encoding)))
        post.clear()
    
print ("\.")

