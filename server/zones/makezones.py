import json
import time
import operator
import csv
us = open("us-counties.csv", "r")
zips = open("zip2fips.json", "r")
zipsj = json.loads(zips.read())
tx = open("tx-counties.csv", "w")

unk = -1
def getZip(fips):
    global unk
    for key in zipsj:
        #time.sleep(1)
        #print("zip ", key, "has fips", zipsj[key])
        if (zipsj[key] == fips): # zip key has fips zipsj[key]
            #print("Found zip " + key)
            return key
        
    print("couldn't find zip for fips=", fips)
    unk = unk - 1
    return unk

tx.write("date,county,state,fips,cases,deaths,zip,mort,safe\n")

def isSafe(cases, mort):
    if (cases > 5):
        if (mort > 0.01): return False
    return True

for line in us:
    if "Texas" in line:
        z = getZip(line.split(",")[3])
        # Calculate mortality frequency and use case count to determine safe zone
        # Areas with higher mortality may indicate a strain of the COVID-19 that is
        # more virulent, or easier to spread due to the geography of the area,
        # or even poor hospitalization measures
        cases = line.split(",")[4]
        deaths = line.split(",")[5]
        mort = int(deaths)/int(cases)
        safe = isSafe(float(cases), float(mort))
        tx.write(line.split("\n")[0] + "," + z + "," + str(mort) + "," + str(safe) + "\n")
tx.close()

tx = open("tx-counties.csv", "r")
reader = csv.reader(tx, delimiter=",")
sortedlist = sorted(reader, key=operator.itemgetter(0), reverse=True)
tx = open("tx-counties.csv", "w")
print(",".join(sortedlist[1]))
for i in range(len(sortedlist)): 
    tx.write(",".join(sortedlist[i]) + "\n")

print("Reading tx-counties now")

tx = open("tx-counties.csv", "r")
tx_recent = open("tx-counties-recent.csv", "w")
tx_recent.write("date,county,state,fips,cases,deaths,zip,mort,safe\n")
tx.readline()

import fileinput
seen = set() # set for fast O(1) amortized lookup
for line in fileinput.FileInput('tx-counties.csv', inplace=1):
    if line.split(",")[6] in seen: 
        continue # skip duplicate
    
    seen.add(line.split(",")[6])
    print(line, end="") # standard output is now redirected to the file
