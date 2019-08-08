#!/usr/bin/env bash
for LANG in es en de zh ja fr
do
	wget https://dumps.wikimedia.org/${LANG}wiki/20190801/${LANG}wiki-20190801-pages-articles-multistream.xml.bz2 -O ~/wikipedia/output_${LANG}
done

