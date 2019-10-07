/*
	This is the Geb configuration file.
	
	See: http://www.gebish.org/manual/current/configuration.html
*/


import org.openqa.selenium.firefox.FirefoxDriver

waiting {
	timeout = 60
	retryInterval = 1
}

environments {
	chrome {
		driver = { new FirefoxDriver() }
	}
}

autoClearCookies = false

reportsDir = "target/test-reports/geb"