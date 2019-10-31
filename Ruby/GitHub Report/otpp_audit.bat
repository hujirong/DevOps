set GITHUB_TOKEN=fb3ea746d9313545fd62d196de0c39af6b1b12fd
set SSL_CERT_FILE=cacert.pem
set GITHUB_API_ENDPOINT=http://github.otpp.com/api/v3/

for /F "tokens=*" %%A in (organizations.txt) do ruby team_audit.rb %%A