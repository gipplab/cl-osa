FROM mongo

# install python
RUN apt-get update
RUN apt-get install -y python
RUN apt-get install -y python-pip
RUN pip install pymongo

# copy python script
COPY wikidata-dump-mongo-import.py wikidata-dump-mongo-import.py

# copy creation bash script
COPY mongo_create.sh /docker-entrypoint-initdb.d/

