import json
import os
print(os.getcwd())
os.chdir("C:\\Workspace\\Python\\")

data = {
    "president": {
        "name": "Zaphod Beeblebrox",
        "species": "Betelgeusian"
    }
}
with open("data_file.json", "w") as write_file:
    json.dump(data, write_file)

json_string = json.dumps(data)
print (json_string)

