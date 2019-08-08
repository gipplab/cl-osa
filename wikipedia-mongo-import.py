"""
This Python script downloads and imports a Wikipedia dump into a running MongoDB database.

Wikipedia .json.bz2 dumps are taken from the official site: "https://dumps.wikimedia.org/wikidatawiki/entities/".

Usage: wikidata-dump-mongo-import.py -h <host> -p <port>
Example: wikidata-dump-mongo-import.py -h localhost -p 27017

"""

from bs4 import BeautifulSoup
from pymongo import MongoClient

languages = ["en", "de", "fr", "es", "ja", "zh"]  # supported languages


# reporting
def reporthook(count, block_size, total_size):
    global start_time

    # start with count == 0
    if count == 0:
        start_time = time.time()
        return

    # duration is difference between current time and start time
    duration = time.time() - start_time
    progress_size = int(count * block_size)
    speed = int(progress_size / (1024 * duration))
    percent = int(count * block_size * 100 / total_size)

    # print
    sys.stdout.write("\r...%d%%, %d MB, %d KB/s, %d seconds passed" %
                     (percent, progress_size / (1024 * 1024), speed, duration))
    sys.stdout.flush()


# download
def wikidata_dump_mongo_download():
    for lang in languages:
        remote_dump_path = "https://dumps.wikimedia.org/" + lang + "wiki/20190801/" + lang + "wiki-20190801-pages-articles-multistream.xml.bz2"
        local_dump_path = "~/wikipedia/output_" + lang

        # if file already exists
        if not os.path.isfile(local_dump_path):
            # get remote file size
            site = urllib.urlopen(remote_dump_path)
            meta = site.info()
            remote_dump_size = int(meta.getheaders("Content-Length")[0])

            statvfs = os.statvfs('/')
            free_space = statvfs.f_frsize * statvfs.f_bfree

            if remote_dump_size < free_space:
                # download Wikidata dump from url
                print "Downloading from " + remote_dump_path
                urllib.urlretrieve(remote_dump_path, local_dump_path, reporthook=reporthook)
                print "\n"
            else:
                # not enough disk space
                print "Not enough disk space, need additional " + str(remote_dump_size - free_space) + " bytes on disk."


# import
def wikipedia_dump_mongo_import(host, port):
    # mongodb information
    database_name = 'wikipedia'
    client = MongoClient(host, port)
    db = client[database_name]

    for lang in languages:
        local_dump_path = "~/wikipedia/output_" + lang

        # import into database
        print "Importing " + local_dump_path + " into " \
              + host + ":" + str(port) + "[" + database_name + "]" + "[articles]"

        soup = BeautifulSoup(local_dump_path, 'html.parser')

        documents = soup.find_all("doc")

        for document in documents:
            id = document.get('id')
            url = document.get('url')
            title = document.get('title')

            text = document.contents

        local_dump_path.close()
