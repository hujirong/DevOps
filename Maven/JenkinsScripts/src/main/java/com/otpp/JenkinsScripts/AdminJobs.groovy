println polljobs("master")
println isonline("master")

//function to poll how many jobs running on a given slave
def polljobs(node){
	for (slave in jenkins.model.Jenkins.instance.slaves) {
		if (slave.name.equals(node)){
			return slave.getComputer().countBusy()
		}
	}
	return -1
}

///function for determing if a node is online
def isonline(node){
	for (slave in jenkins.model.Jenkins.instance.slaves)
	{
		if (slave.name.equals(node)){
			return slave.getComputer().isOnline()
		}
	}
	return false
}

findallsalves()
///function for find all slaves
def findallsalves(){
	for (slave in jenkins.model.Jenkins.instance.slaves) {
		if (slave.getComputer().isOnline()) {
			println slave.name + "is up"
		} else {
			println slave.name + "is down"
		}
	}

}


///function to restart a node
/// caution: this will restart the node server, not the slave
def restart(node){
	for (slave in jenkins.model.Jenkins.instance.slaves) {
		if (slave.name.equals(node)){
			def channel = slave.getComputer().getChannel()
			RemotingDiagnostics.executeGroovy( """
				if (Functions.isWindows()) {
					'shutdown /r /t 10 /c "Restarting after Jenkins test completed"'.execute()
				} else {
					"sudo reboot".execute()
				}
				""", channel)

		}

	}
}

def getJobs() {
	def hi = hudson.model.Hudson.instance
	return hi.getItems(hudson.model.Job)
}

def getBuildJob(String jobNam) {
	def buildJob = null
	def jobs = getJobs()
	(jobs).each { job ->
		if (job.displayName == jobNam) {
			println("Found")
			println("Exiting job search")
			buildJob = job
			return buildJob
		}
	}
	return buildJob
}

def getRunToMark(hudson.model.Job job, String
	buildNum) {
	 def runToMark = null
	 if(job != null) {
	 (job.getBuilds()).each { run ->
	 if (String.valueOf(run.number) ==
	buildNum) {
	 println("Found")
	 println("Exiting build search")
	 runToMark = run
	 return runToMark
	 }
	 }
	 }
	 return runToMark
	}
	