set GITHUB_TOKEN=2302c3d91789c274ef07ea06f227575cf4c9904f
set SSL_CERT_FILE=cacert.pem
set GITHUB_API_ENDPOINT=http://github.com/api/v3/

for /F "tokens=*" %%A in (orgs.txt) do ruby team_audit.rb %%A