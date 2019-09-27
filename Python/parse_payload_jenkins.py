import json
import os
import sys
import subprocess

## run from Jenkins
payload_file = sys.argv[1]
print (payload_file)

## run in VS
#os.chdir("C:\\Workspace\\Python\\")
#with open("payload.json", "r") as read_file:

with open(payload_file, "r") as read_file:
    payload = json.load(read_file)
# print (payload)

######### deserialization approach ##################
# list of first level arrtibutes
#for attribte in payload:
#   print (attribte)

# first level attributes
#print ("compare=" + payload["compare"])

commits = payload["commits"]
print (commits)

# second level attributes
for commit in commits:
    # third level attributes
    added = commit["added"]
    print (added)
    removed = commit["removed"]
    print (removed)
    modified = commit["modified"]
    print (modified)
# concatenated list contains list of file changed
changed = sorted(set(added + modified))
print ("unique list of chanaged files:")
print (changed)

for change in changed:
    print(change.strip('['))
    p = subprocess.Popen(["C:\\DevOps\\Strawberry\\perl\\bin\\perl", "C:\\Workspace\\Python\\main.pl", change], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    p.stdin.write(bytes(change, "utf-8"))    
    p.stdin.close()
    output = p.stdout.read()
    print ("output from perl script:\n" + output.decode("utf-8"))

