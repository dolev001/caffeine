import os
import csv
import sys
import re
import subprocess
from collections import defaultdict

policyToComper = 'linked'
#  a dict  of list that have  all the hit rate in all cashe sizes for policy /
# the key is the policy and the list is the hit rate of all cache sizes .
my_dict = defaultdict(list)
w=[8,16,32,64,128]
# the newline='' is just when using writer and not DictWriter
with open ('simulatorResForGraph.csv','w',newline='')as outfile:
	writer = csv.writer(outfile)
	for ways in w:
	#for ways in [8,16]:
		for i in range(2,53,2):
			size =i*64  # 64 is the number of ways 
			path='C:\\Users\\Dolev\\git\\caffeine\\simulator\\src\\main\\resources\\reference.conf'
			runSimulatorPath="C:\\Users\\Dolev\\git\\caffeine"
			#path = 'C:\\Users\\sdolevfe\\git\\caffeine\\simulator\\src\\main\\resources\\reference.conf'
			with open(path, "r+") as f:
				old = f.read() # read everything in the file
				temp = re.sub(r"  maximum-size = [0-9]+",r"  maximum-size = "+str(size), old)
				new = re.sub(r" ways = [0-9]+",r" ways = "+str(ways), temp)
				f.seek(0) # rewind
				f.write(new)
				
			# runing the simulator 
			subprocess.run("gradlew simulator:run", shell = True,cwd=runSimulatorPath)
			
			my_dict['cache.Size'].append(size)
			with open("simulatur_output.csv") as csvfile:
				reader = csv.DictReader(csvfile)
				for row in reader:
					cachename= row['Policy'].split('.')[0]
					if cachename=='kway' or cachename == policyToComper : #or row['Policy'] =='product.Caffeine'
						my_dict[row['Policy']].append(row['Hit rate'])
						
		if ways==8:
			writer.writerow(['ways']+w)
			writer.writerow(['policies']+sorted(my_dict.keys())[1:])
			writer.writerow(['cache.Size']+my_dict['cache.Size'])
		for policy, list in my_dict.items():
			if policy != 'cache.Size':
				cachename= policy.split('.')[0]
				if cachename=='kway' or ways ==8 :
					writer.writerow([policy]+[str(ways)+' ways']+list)
		
		my_dict.clear()
	#writer.writerow(['cache.Size']+my_dict['cache.Size'])
		
		
		
		