import os
import csv
import sys
import re
import subprocess
from collections import defaultdict
my_dict = defaultdict(list)
with open ('simulatorResForGraph.csv','a',newline='')as outfile:
	writer = csv.writer(outfile)
	for i in range(2,53,2):
			size =i*64  # 64 is the number of ways 
			path='C:\\Users\\Dolev\\git\\caffeine\\simulator\\src\\main\\resources\\reference.conf'
			runSimulatorPath="C:\\Users\\Dolev\\git\\caffeine"
			#path = 'C:\\Users\\sdolevfe\\git\\caffeine\\simulator\\src\\main\\resources\\reference.conf'
			with open(path, "r+") as f:
				old = f.read() # read everything in the file
				temp = re.sub(r"  maximum-size = [0-9]+",r"  maximum-size = "+str(size), old)
				new = re.sub(r" ways = [0-9]+",r" ways = "+str(size), temp)
				f.seek(0) # rewind
				f.write(new)
				
			# runing the simulator 
			subprocess.run("gradlew simulator:run", shell = True,cwd=runSimulatorPath)
			
			with open("simulatur_output.csv") as csvfile:
				reader = csv.DictReader(csvfile)
				for row in reader:
					cachename= row['Policy'].split('.')[0]
					if cachename=='kway':
						my_dict[row['Policy']].append(row['Hit rate'])
						

	for policy, list in my_dict.items():
		if policy != 'cache.Size':
			cachename= policy.split('.')[0]
			policyname= policy.split('.')[1]
			if cachename == 'kway' :
				writer.writerow(['FA.'+policyname]+[' ways']+list)
	
	my_dict.clear()
	#writer.writerow(['cache.Size']+my_dict['cache.Size'])
		
		
		