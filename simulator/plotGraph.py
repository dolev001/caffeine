import os
import csv
import sys
import re
import subprocess
from collections import defaultdict
import numpy as np
import matplotlib.pyplot as plt

cacheSize=[]
usedPolicy=[]
policies=[]
ways=[]
with open("simulatorResForGraph.csv") as csvfile:
	reader = csv.reader(csvfile)
	for r1 in reader:
		if r1[0]=='ways':
			ways=r1[1:]
			break
	csvfile.seek(0)	
	
	# first line have all the policies
	for r1 in reader:
		if r1[0]=='policies':
			policies=r1[1:]
			break
	csvfile.seek(0)	
		
	for r2 in reader:
		if r2[0]=='cache.Size':
			cacheSize=r2[1:]
			break
	csvfile.seek(0)
	
	countRow=0	
	ax = plt.subplot(111)
	colormap = plt.cm.gist_ncar
	numOfColors=len(ways) +3
	colors = [colormap(i) for i in np.linspace(0.1, 0.9, numOfColors)]
	filled_markers = ('o', 'v','*','s','d', '<', '>', '8', 'h', 'p','^','H', 'D')
	
	for policy in policies:
		headerPolicyNmae=policy.split('.')[1]
		headerCacheName= policy.split('.')[0]
		if headerCacheName=='kway': #or policy =='linked.Dolfu' or  policy =='linked.Dolfu_TinyLfu':
			csvfile.seek(0)
			countRow=0
			color=2
			#reader = csv.reader(csvfile)
			plt.figure()
			for row in reader:
				if row[0]!='cache.Size' and row[0]!='ways'and row[0]!='policies': #and countRow>2:
					cachename= row[0].split('.')[0]
					policyname= row[0].split('.')[1]
					if policyname==headerPolicyNmae or (headerPolicyNmae=='Lfu'and policyname=='Dolfu')or (headerPolicyNmae=='Lfu_TinyLfu'and policyname=='Dolfu_TinyLfu'):
						if cachename=='kway':
							plt.plot(cacheSize,row[2:] , linewidth=2.5, linestyle="-", marker= filled_markers[color],color=colors[color], label=row[1] )
							color+=1
						if cachename=='linked':
							if policyname=='Dolfu' or policyname=='Dolfu_TinyLfu':
								plt.plot(cacheSize,row[2:] , linewidth=2.5, linestyle="--",marker= 'D',color=colors[numOfColors-1], label=policyname+' linked'  )
							else:
								plt.plot(cacheSize,row[2:] , linewidth=2.5, linestyle="--",marker= filled_markers[0],color=colors[0], label=policyname+' linked'  )
						if cachename=='FA':
							plt.plot(cacheSize,row[2:] , linewidth=2.5, linestyle="--",marker= filled_markers[1],color=colors[1], label='FA'  )
						
						#print("count ",countRow ," polict ", policyname) 
				countRow+=1	
			# Put a legend to the right of the current axis
			plt.subplots_adjust(right = 0.8)
			plt.legend(loc='center left', bbox_to_anchor=(1, 0.5),fontsize=12)
			plt.xlabel('Cache Size',fontsize=14)
			plt.ylabel('Hit Rate',fontsize=14)
			plt.savefig(headerPolicyNmae+'.png', format='png')
			plt.clf()

