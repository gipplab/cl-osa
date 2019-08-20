
The 50 html files found within this directory are the result of having human evaluators inspect automatically wikified news stories.
The details of the experiment can be found in the following paper:

	Milne, D. and Witten, I.H. (2008) 
	Learning to link with Wikipedia. 
	In Proceedings of the ACM Conference on Information and Knowledge Management (CIKM'2008), Napa Valley, California.

We have shared the wikified stories here, as an extra dataset for others to evaluate thier systems. These files only contain links that were 
manually created, or automatically created and manually checked. In other words, they only contain manually-verified links and can be 
used as ground-truth for wikification experiments. 

Links that were automatically created are in media-wiki format

	eg: [[New Zealand]] or [[New Zealand|N.Z.]]

Links that were created manually have an additional parameter to show how many of the participants identified it.

	eg:	[[New Zealand|0.2]] is a link that only one of 5 evaluators felt was missing (and therefore probably shouldnt be considered ground truth).
			[[New Zealand|1.0]] is a link that all of our evaluators felt was missing (and definitely should be considered ground truth).

If you have any questions, feel free to contact me 

	www.cs.waikato.ac.nz/~dnk2
	
Also, if you publish an experiment that uses these files, then please site the paper mentioned above. 

- David Milne
