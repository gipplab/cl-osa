FROM mongo

# install python
RUN apt-get update
RUN apt-get install -y python
RUN apt-get install -y python-pip
RUN pip install pymongo

# copy python script
COPY docker/mongo/wikidata-dump-mongo-import.py wikidata-dump-mongo-import.py

# copy creation bash script
COPY docker/mongo/mongo_create.sh /docker-entrypoint-initdb.d/
RUN ["chmod", "+x", "/docker-entrypoint-initdb.d/mongo_create.sh"]
RUN ["chmod", "777", "/home"]