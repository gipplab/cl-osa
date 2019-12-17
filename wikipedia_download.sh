#!/usr/bin/env bash
mkdir /data/wikipedia

for LANG in es en zh ja fr
do
	wget https://dumps.wikimedia.org/${LANG}wiki/20191201/${LANG}wiki-20191201-pages-articles-multistream.xml.bz2 -O /data/wikipedia/dump_${LANG}.xml.bz2
done

for LANG in es en zh ja fr
do
  python ./WikiExtractor.py -o /data/wikipedia/output_${LANG} /data/wikipedia/dump_${LANG}.xml.bz2
done