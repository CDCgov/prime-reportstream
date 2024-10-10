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

filename = "Votes.xml"
votes = ElementTree.iterparse(filename) 
print ("COPY votes (id, type, postid, creation) FROM stdin;")
for event, vote in votes:
    if event == "end" and vote.tag == "row":
        id = int(vote.attrib["Id"])

        type = int(vote.attrib["VoteTypeId"])

        postid = vote.attrib["PostId"]

        creation = vote.attrib["CreationDate"]
        
        print ("%i\t%s\t%s\t%s" % (id, type, postid, creation))
        vote.clear()
print ("\.")