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

filename = "Users.xml"
users = ElementTree.iterparse(filename) 
print ("COPY users (id, reputation, creation, name, lastaccess, website, location, aboutme, views, upvotes, downvotes, age) FROM stdin;")
for event, user in users:
    if event == "end" and user.tag == "row":
        id = int(user.attrib["Id"])

        reputation = int(user.attrib["Reputation"])

        creation = user.attrib["CreationDate"]

        #if user.attrib.has_key("DisplayName"): # Yes, some users have no name, for instance 155 :-(
        if "DisplayName" in user.attrib:
            name = escape(user.attrib["DisplayName"])
        else:
            name = "\n"

        #if user.attrib.has_key("LastAccessDate"):
        if "LastAccessDate" in user.attrib:
            lastaccess = escape(user.attrib["LastAccessDate"])
        else:
            lastaccess = "\n"

        #if user.attrib.has_key("WebsiteUrl"):
        if "WebsiteUrl" in user.attrib:
            website = escape(user.attrib["WebsiteUrl"])
        else:
            website = "\n"

        #if user.attrib.has_key("Location"):
        if "Location" in user.attrib:
            location = escape(user.attrib["Location"])
        else:
            location = "\n"
        
        #if user.attrib.has_key("AboutMe"):
        if "AboutMe" in user.attrib:
            aboutme = escape(user.attrib["AboutMe"])
        else:
            aboutme = "\n"

        #if user.attrib.has_key("Views"):
        if "Views" in user.attrib:
            views = int(user.attrib["Views"])
        else:
            views = 0

        #if user.attrib.has_key("UpVotes"):
        if "UpVotes" in user.attrib:
            upvotes = int(user.attrib["UpVotes"])
        else:
            upvotes = 0

        #if user.attrib.has_key("DownVotes"):
        if "DownVotes" in user.attrib:
            downvotes = int(user.attrib["DownVotes"])
        else:
            downvotes = 0
        
        #if user.attrib.has_key("Age"):
        if "Age" in user.attrib:
            age = int(user.attrib["Age"])
        else:
            age = -1

        print ("%i\t%i\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s" % (id, reputation, creation, name.encode(encoding), lastaccess, website.encode(encoding), location.encode(encoding), aboutme.encode(encoding), views, upvotes, downvotes, age))
        user.clear()
print ("\.")