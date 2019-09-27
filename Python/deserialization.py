import json
import os
os.chdir("C:\\Workspace\\Python\\")

with open("data_file.json", "r") as read_file:
    data = json.load(read_file)
print (data)

json_string = """
{
    "researcher": {
        "name": "Ford Prefect",
        "species": "Betelgeusian",
        "relatives": [
            {
                "name": "Zaphod Beeblebrox",
                "species": "Betelgeusian"
            }
        ]
    }
}
"""
data = json.loads(json_string)
print (data)