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

filename = "Badges.xml"
badges = ElementTree.iterparse(filename) 
print ("COPY Badges (id, userid, name, date, badgeclass, tagbased) FROM stdin;")

for event, badge in badges:
    if event == "end" and badge.tag == "row":
        id = int(badge.attrib["Id"])

        userid = int(badge.attrib["UserId"])

        name = escape(badge.attrib["Name"])

        date = escape(badge.attrib["Date"])

        badgeclass = badge.attrib["Class"]

        tagbased = badge.attrib["TagBased"]
        
        print ("%i\t%s\t%s\t%s\t%s\t%s" % (id, userid, name.encode(encoding), date, badgeclass, tagbased))
        badge.clear()
    
print ("\.")

