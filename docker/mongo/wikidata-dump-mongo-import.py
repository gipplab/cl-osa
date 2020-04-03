"""
This Python script downloads and imports a Wikidata dump into a running MongoDB database.

Wikidata .json.bz2 dumps are taken from the official site: "https://dumps.wikimedia.org/wikidatawiki/entities/".

Usage: wikidata-dump-mongo-import.py -h <host> -p <port>
Example: wikidata-dump-mongo-import.py -h localhost -p 27017

"""
import bz2
import getopt
import itertools
import json
import os
import pymongo
import sys
import time
import urllib
import psutil
from bz2 import BZ2File
from pymongo import MongoClient

local_dump_path = "D:\Test\latest-all.json.bz2"  # json file location


# reporting
def reporthook(count, block_size, total_size):
    global start_time

    # start with count == 0
    if count == 0:
        start_time = time.time()
        return

    # duration is difference between current time and start time
    duration = time.time() - start_time
    if duration == 0:
        sys.stdout("Duration 0 wont report")
        return
    progress_size = int(count * block_size)
    speed = int(progress_size / (1024 * duration))
    percent = int(count * block_size * 100 / total_size)

    # print
    sys.stdout.write("\r...%d%%, %d MB, %d KB/s, %d seconds passed" %
                     (percent, progress_size / (1024 * 1024), speed, duration))
    sys.stdout.flush()


# download
def wikidata_dump_mongo_download():
    # Wikidata json bz2 url
    remote_dump_path = "https://dumps.wikimedia.org/wikidatawiki/entities/latest-all.json.bz2"

    # Workaround: Just remove file at start if it already exists
    if os.path.isfile(local_dump_path):
        os.remove(local_dump_path)

    # if file doesn't exist
    if not os.path.isfile(local_dump_path):
        # get remote file size
        site = urllib.urlopen(remote_dump_path)
        meta = site.info()
        remote_dump_size = int(meta.getheaders("Content-Length")[0])

        local_dump_base = os.path.basename(local_dump_path)
        my_os = os.name

        if my_os is 'nt':
            # the operating system is windows  use psutil
            free_space = psutil.disk_usage('D:/').free
        else:
            # the operating system is linux, use the linux-specific statvfs call
            statvfs = os.statvfs('/')
            free_space = statvfs.f_frsize * statvfs.f_bfree
        # diff =  free_space - remote_dump_size # remote dump size is
        if remote_dump_size < free_space:
            # download Wikidata dump from url
            print "Downloading from " + remote_dump_path
            urllib.urlretrieve(remote_dump_path, local_dump_path, reporthook=reporthook)
            print "\n"
        else:
            # not enough disk space
            print "Not enough disk space, need additional " + str(remote_dump_size - free_space) + " bytes on disk."


# import
def wikidata_dump_mongo_import(host, port):
    # mongodb information
    database_name = 'wikidata'
    client = MongoClient(host, port)
    db = client[database_name]

    languages = ["en", "de", "fr", "es", "ja", "zh"]  # supported languages

    # import into database
    print "Importing " + local_dump_path + " into " \
          + host + ":" + str(port) + "[" + database_name + "]" + "[entities]"

    source_file = bz2.BZ2File(local_dump_path, "r")  # type: BZ2File
    i = 0

    limit = 54633440
    skip = db['entities'].count()

    if limit > skip:
        for line in itertools.islice(source_file, skip, limit, None):
            if len(line) > 2:
                try:
                    # insert into collection entities
                    # print line[:-2]
                    entity_id = db['entities'].insert_one(json.loads(line[:-2])).inserted_id
                    # print entity_id
                except pymongo.errors.DuplicateKeyError:
                    # skip document because it already exists in collection
                    continue
            reporthook(i, 1, limit - skip)
            i += 1

        source_file.close()
    else:
        print "Skipped import because database contains " + str(skip) + " of required " + str(limit) + " entities"

    print "Indexing..."

    # index creation for id, labels and aliases
    db['entities'].create_index([('id', pymongo.ASCENDING)], unique=True)

    for language in languages:
        db['entities'].create_index([('labels.' + language + '.value', pymongo.ASCENDING)])
        db['entities'].create_index([('aliases.' + language + '.value', pymongo.ASCENDING)])
        db['entities'].create_index([('sitelinks.' + language + 'wiki.title', pymongo.ASCENDING)])

    # index for instanceOf and subclassOf
    db['entities'].create_index([('claims.P279.mainsnak.datavalue.value.id', pymongo.ASCENDING)])
    db['entities'].create_index([('claims.P31.mainsnak.datavalue.value.id', pymongo.ASCENDING)])


    # create collection entitiesGraph
    print "\n"
    print "Creating collection " + host + ":" + str(port) + "[" + database_name + "]" + "[entitiesGraph] ..."

    db.command(
        "create", "entitiesGraph",
        viewOn="entities",
        pipeline=[{
            "$project": {
                "id": 1,
                "label": "$labels.en.value",
                "subclassOf": "$claims.P279.mainsnak.datavalue.value.id",
                "instanceOf": "$claims.P31.mainsnak.datavalue.value.id",
                "subclassInstanceOf": {
                    "$concatArrays": [
                        {"$ifNull": ["$claims.P279.mainsnak.datavalue.value.id", []]},
                        {"$ifNull": ["$claims.P31.mainsnak.datavalue.value.id", []]}
                    ]
                }
            }
        }]
    )

    # create collection entitiesHierarchyPersistent
    print "\n"
    print "Creating collection " + host + ":" + str(
        port) + "[" + database_name + "]" + "[entitiesHierarchyPersistent] ..."

    db['entitiesGraph'].aggregate([{
        "$graphLookup": {
            "from": "entitiesGraph",
            "startWith": "$subclassInstanceOf",
            "connectFromField": "subclassInstanceOf",
            "connectToField": "id",
            "depthField": "depth",
            "as": "hierarchy"
        }
    }, {
        "$project": {
            "_id": 0,
            "id": 1,
            "label": 1,
            "hierarchy.id": 1,
            "hierarchy.label": 1,
            "hierarchy.depth": 1
        }
    }, {
        "$out": "entitiesHierarchyPersistent"
    }])

    # indices on entitiesHierarchyPersistent
    print "Indexing " + host + ":" + str(port) + "[" + database_name + "]" + "[entitiesHierarchyPersistent] ..."

    db['entitiesHierarchyPersistent'].create_index([('id', pymongo.ASCENDING)], unique=True)
    db['entitiesHierarchyPersistent'].create_index([('hierarchy', pymongo.ASCENDING)])
    db['entitiesHierarchyPersistent'].create_index([('hierarchy.id', pymongo.ASCENDING)])
    db['entitiesHierarchyPersistent'].create_index([('hierarchy.label', pymongo.ASCENDING)])
    db['entitiesHierarchyPersistent'].create_index([('hierarchy.depth', pymongo.ASCENDING)])

    print "\n"
    print "finished."


def main(argv):
    host = 'localhost'
    port = 27017

    try:
        opts, args = getopt.getopt(argv, "h:p:", ["host=", "port="])
    except getopt.GetoptError:
        print 'wikidata-dump-mongo-import.py -h <host> -p <port>'
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-h", "--host"):
            host = arg
        elif opt in ("-p", "--port"):
            port = int(arg)
    # print 'Host is ', host
    # print 'Port is ', port

    wikidata_dump_mongo_download()
    wikidata_dump_mongo_import(host, port)


if __name__ == '__main__':
    main(sys.argv[1:])
