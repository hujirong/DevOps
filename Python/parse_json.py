import json
import os
os.chdir(r"C:\Workspace\Python")

try:
    with open('payload.json', 'r') as fp:
        loaded_json = json.load(fp)
except ValueError:
    print ("error loading JSON")

print (loaded_json["compare"])
