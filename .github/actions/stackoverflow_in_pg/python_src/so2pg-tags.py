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

filename = "Tags.xml"
tags = ElementTree.iterparse(filename) 
print ("COPY tags (id, name, count, excerptpost, wikipost) FROM stdin;")
for event, tag in tags:
    if event == "end" and tag.tag == "row":
        id = int(tag.attrib["Id"])

        name = tag.attrib["TagName"]

        count = int(tag.attrib["Count"])
        
        #if tag.attrib.has_key("ExcerptPostId"):
        if "ExcerptPostId" in tag.attrib:
            excerptpost = int(tag.attrib["ExcerptPostId"])
        else:
            excerptpost = int("-1")
        
        #if tag.attrib.has_key("WikiPostId"):
        if "WikiPostId" in tag.attrib:
            wikipost = int(tag.attrib["WikiPostId"])
        else:
            #wikipost = "\n"
            wikipost = int("-1")

        print ("%i\t%s\t%d\t%d\t%d" % (id, name, count, excerptpost, wikipost))
        tag.clear()
print ("\.")