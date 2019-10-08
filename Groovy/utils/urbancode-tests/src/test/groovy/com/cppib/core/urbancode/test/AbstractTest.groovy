package com.cppib.core.urbancode.test

import geb.navigator.EmptyNavigator
import geb.navigator.Navigator
import geb.testng.GebReportingTest

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.testng.Assert
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest

import com.devops.utils.ConfigFile
import com.devops.versions.UdeployUpdate


/**
 * Abstract class to test UrbanCode Deploy
 * @author fhou
 *
 */
class AbstractTest extends GebReportingTest {
	static Log log = LogFactory.getLog(AbstractTest.class)
	
	String dashboardUrl = '#dashboard'
	UTestConf utestConf
	
	@BeforeTest	
	void init() {
		utestConf = new UTestConf()
		
		// login to udeploy
		login()
	}
	
	void login() {
		String baseUrl = utestConf.getBaseUrl()
		this.getBrowser().setBaseUrl(baseUrl)
		
		String user = utestConf.getEnvProperty("uitestUser")
		assert user?.trim()
		log.info("Login to $baseUrl with $user")
		
		// go to login page
		go()
		
		$('#usernameField').value(user)
		
		String passwordKey = user + '.password'
		$('#passwordField').value(utestConf.conf.getConfig(passwordKey, true) << Keys.ENTER)
		
		toDashboard()
	}
	
	void toDashboard() {
		verifyNoError()
		String currentUrl = getBrowser().getCurrentUrl()
		
		if (!currentUrl.endsWith(dashboardUrl)) {
			log.info("Go to dashboard, current url: $currentUrl")
		
			goPage(dashboardUrl)
			waitFor { verifyNotEmpty($('#mainLabel1')) }
			
			// verify we are at dashboard page
			assert $('#mainLabel1').text().startsWith('Current Activity')
		}
	}
	
	@AfterTest
	void logout() {
		println "Logout ..."
		
		go '/tasks/LoginTasks/logout'
		assert $('div', class: 'loginFrame') != null
	}
	
	List getPagedObjects(String domInPage, String rowFilter, List filterList) {
		List li = []
		_addObjectsInPage(li, rowFilter, filterList)

		// navigate to next page
		boolean last = false
		def nextPage = $('img', alt: 'next page')
		
		while (!last) {
			nextPage.parent().click()
			waitFor {
				Navigator nav = $('img', alt: 'next page')
				if (nav instanceof EmptyNavigator) {
					last = true
					nav = $('img', alt: 'next page disabled')
				}
				verifyNotEmpty(nav)
			}
			
			_addObjectsInPage(li, rowFilter, filterList)
			
			if (!last) {
				nextPage = $('img', alt: 'next page')
			}
		}
		
		return li
	}
	
	void _addObjectsInPage(List li, rowFilter, List filterList) {
		def rows = $(rowFilter)
		rows.each { it ->
			String name = it.text()
			if (_includeObject(filterList, name)) {
				li.add([name: name, href: it.@href])
			}
		}
	}
	
	boolean _includeObject(List filterList, String name) {
		if (filterList.size() == 0) {
			return true
		}
		
		boolean found = false
		filterList.find { it ->
			if (it == name) {
				found = true
				return true
			}
		}
		
		return found

	}
	
	def getEnvProperty(String name) {
		return utestConf.getEnvProperty(name)
	}
	
	def getConfProperty(String name) {
		return utestConf.confBinding.getProperty(name)
	}
	
	boolean verifyNotEmpty(Navigator page) {
		if (page instanceof EmptyNavigator) {
			return false
		}
		
		return (page.verifyNotEmpty() != null)
	}
	
	void verifyNoError() {
		try {
		// verify no error in page
		Navigator error = $('div.masterContainer span', text: startsWith('An error has occurred'))
		if (error instanceof EmptyNavigator) {
			return
		}
		
		Assert.assertNull(error.verifyNotEmpty(), "An error has occurred!")
		} catch (StaleElementReferenceException ex) {
			// Element is no longer attached to the DOM
			// it is not error
		}
	}
	
	public <T> T waitFor(Closure<T> block) {
		def pass = super.waitFor(block)
		
		if (pass) {
			verifyNoError()
		}
		
		return pass
	}
	
	public <T> T waitFor(long time, Closure<T> block) {
		def pass = super.waitFor(60, time, block)
		
		if (pass) {
			verifyNoError()
		}
		
		return pass
	}
}
