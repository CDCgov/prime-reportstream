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

filename = "PostLinks.xml"
postlinks = ElementTree.iterparse(filename) 
print ("COPY postlinks (id, creation, postid, relatedpostid, linktypeid) FROM stdin;")
for event, postlink in postlinks:
    if event == "end" and postlink.tag == "row":
        id = int(postlink.attrib["Id"])

        creation = postlink.attrib["CreationDate"]

        postid = int(postlink.attrib["PostId"])
        
        #if postlink.attrib.has_key("RelatedPostId"):
        if "RelatedPostId" in postlink.attrib:
            relatedpostid = postlink.attrib["RelatedPostId"]
        else:
            relatedpostid = "\n"
        
        if "LinkTypeId" in postlink.attrib:
        #if postlink.attrib.has_key("LinkTypeId"):
            linktypeid = postlink.attrib["LinkTypeId"]
        else:
            linktypeid = "\n"

        print ("%i\t%s\t%s\t%s\t%s" % (id, creation, postid, relatedpostid, linktypeid))
        postlink.clear()
print ("\.")