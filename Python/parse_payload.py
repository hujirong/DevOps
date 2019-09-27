import json
import os
import sys
import subprocess

## run from Jenkins
#payload_file = sys.argv[1]
#print (payload_file)

## run in VS
os.chdir("C:\\Workspace\\Python\\")
with open("payload.json", "r") as read_file:
#with open(payload_file, "r") as read_file:
    payload = json.load(read_file)
# print (payload)

######### deserialization approach ##################
# list of first level arrtibutes
#for attribte in payload:
#   print (attribte)
# first level attributes
#print ("compare=" + payload["compare"])

commits = payload["commits"]
print ("Total commits:")
print (commits)
print ("\n")

# second level attributes
for commit in commits:
    # third level attributes
    added = commit["added"]
    print ("Add files:")
    print (added)

    print ("Removed files:")
    removed = commit["removed"]
    print (removed)

    print ("Modified files:")
    modified = commit["modified"]
    print (modified)

    print ("\n")
# concatenated list contains list of file changed
changed = sorted(set(added + modified))
print ("unique list of chanaged files:")
print (changed)
print ("\n")

for change in changed:
    print ("Input to Perl script:")
    print(change.strip('['))

    print ("\nUse Popen works ...")
    #p = subprocess.Popen(['C:\\DevOps\\Strawberry\\perl\\bin\\perl', '-IC:\\Workspace\\Python C:\\Workspace\\Python\\bccPmbld_main.pl', change], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    p = subprocess.Popen(["C:\\DevOps\\Strawberry\\perl\\bin\\perl", "C:\\Workspace\\Python\\main.pl", change], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    p.stdin.write(bytes(change, "utf-8"))    
    p.stdin.close()
    output = p.stdout.read()
    print ("Output from perl script:\n" + output.decode("utf-8"))
    
    print ("\nUse CMD works ...")
    output = subprocess.check_output("dir C:\\", shell=True).decode()
    #output = subprocess.check_output("C:\\DevOps\\Strawberry\\perl\\bin\\perl C:\\Workspace\\Python\\main.pl AOL/ASM/CX/JIRONG.ASM", shell=True).decode()
    parameter = "C:\\DevOps\\Strawberry\\perl\\bin\\perl C:\\Workspace\\Python\\main.pl " + change
    output = subprocess.check_output(parameter, shell=True).decode()
    print ("Output from perl script:\n" +output)
