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

filename = "Comments.xml"
comments = ElementTree.iterparse(filename) 
print ("COPY Comments (id, postid, score, text, creation, userid) FROM stdin;")

for event, comment in comments:
    if event == "end" and comment.tag == "row":
        id = int(comment.attrib["Id"])

        postid = int(comment.attrib["PostId"])

        score = int(comment.attrib["Score"])

        text = escape(comment.attrib["Text"])

        creation = comment.attrib["CreationDate"]

        if "UserId" in comment.attrib:
            userid = int(comment.attrib["UserId"])
        else:
            userid = -1

        print ("%i\t%s\t%s\t%s\t%s\t%s" % (id, postid, score, text.encode(encoding), creation, userid))
        comment.clear()
    
print ("\.")

