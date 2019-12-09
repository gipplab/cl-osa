#!/usr/bin/env bash
mkdir /data/wikipedia

for LANG in es en de zh ja fr
do
	wget https://dumps.wikimedia.org/${LANG}wiki/20191201/${LANG}wiki-20191201-pages-articles-multistream.xml.bz2 -O /data/wikipedia/output_${LANG}
done

