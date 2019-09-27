import json
import os
from Payload import *

os.chdir("C:\\Workspace\\Python\\")

payload = Payload()
with open("payload.json", "r") as read_file:
    payload = json.load(read_file)
print (payload)

