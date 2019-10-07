package com.cppib.coretech.devops.urbancode.deploy;

def restClient = new DeployRestClient('https://dviappvmsvn02:8443')
restClient.login('admin', 'admin')

def rest = restClient.restCall("/rest/deploy/component/79be1004-ba07-4ad0-9bc6-ed63e7366ca9/versions/paginated/false")
println(rest)

restClient.logout()
