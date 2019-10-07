package com.cppib.core.urbancode.test

import geb.navigator.NonEmptyNavigator

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class ApplicationTest extends AbstractTest {
	static Log log = LogFactory.getLog(ApplicationTest.class)
	
	List appList
	
	@Test
	void listApplications() {
		goMainApp()

		def filterList = getEnvProperty('applications')
		assert filterList != null
		
		appList = getPagedObjects('#applicationList table tfoot a', '#applicationList tbody tr a', filterList)
	}
	
	void goMainApp() {
		go '#main/applications'
		waitFor {
			verifyNotEmpty($('#applicationList table tfoot a', text: 'Refresh'))
		}
	}

	@Test(dependsOnMethods = ['listApplications'], dataProvider = 'provideApplications' )
	void testApplication(app) {
		log.info("Open application ${app.name}, href=${app.href}")
		goApplication(app)
		
		testTabHistory()
		goApplication(app)
		
		testTabEnvironments()
		goApplication(app)
		
		testTabProcesses()
		goApplication(app)

		testTabComponents()
		goApplication(app)
		
		testTabChanges()
		goApplication(app)
		
		testTabSnapshots()
		goApplication(app)
	}
	
	void goApplication(app) {
		go app.href
		
		waitFor { verifyNotEmpty($('#applicationEnvironmentList')) }
	}
	
	void testTabEnvironments() {
		log.info("Open tab Environments...")
		def href = $('#secondLevelTabs a span', text: 'Environments').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#applicationEnvironmentList')) }

		List envs = []
		
		def li = $('a.environment-name-label')
		li.each { it ->
			envs.add([name: it.text(), href: it.@href])
		}
		
		log.info("Application environments: ${envs}")
		envs.each { it ->
			testEnvironment(it)
		}
	}
	
	void testEnvironment(env) {
		// Resources
		//---------------------
		go env.href
		waitFor { verifyNotEmpty($('#resourceList table tfoot a', text: 'Refresh')) }
		
		// History
		//---------------------
		def href = $('#secondLevelTabs a span', text: 'History').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('div.environmentRequestHistory table tfoot a', text: 'Refresh')) }
		
		/*
		List historyItems = getPagedObjects('div.environmentRequestHistory table tfoot a', 'div.environmentRequestHistory tbody tr td:first-child a', [])
		log.info("Environment ${env.name} history records: ${historyItems.size()}")
		*/
		
		// Configuration
		//---------------------
		href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		
		waitFor {
			def obj = $('div.twoPaneDetail div.containerLabel', text: 'Basic Settings')
			verifyNotEmpty(obj)
		}

		// Environment Properties
		href = $('div.twoPaneList div', text: 'Environment Properties')
		href.click()
		waitFor { verifyNotEmpty($('div.twoPaneDetail div.containerLabel', text: 'Component Environment Properties')) }
		
		// Changes
		//---------------------
		href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		waitFor { verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh')) }
	}
	
	void testTabHistory() {
		log.info("Open tab History...")
		def href = $('#secondLevelTabs a span', text: 'History').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#applicationRequestHistory table tfoot a', text: 'Refresh')) }

		/*
		$('div.perPage span', role: 'option').click()
		Thread.sleep(1000)
		$('div.dijitPopup table td', text: '100').click()
		waitFor { verifyNotEmpty($('#applicationRequestHistory table tfoot a', text: 'Refresh')) }
		*/
		
		List items = getPagedObjects('#applicationRequestHistory table tfoot a', '#applicationRequestHistory tbody tr td:first-child a', [])
		log.info("Application history records: ${items.size()}")
	}
	
	void testTabComponents() {
		log.info("Open tab Components...")
		def href = $('#secondLevelTabs a span', text: 'Components').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#applicationComponents table tfoot a', text: 'Refresh')) }

		List items = getPagedObjects('#applicationComponents table thead th span', '#applicationComponents tbody tr td:first-child a', [])
		log.info("Application component records: ${items.size()}")
	}
	
	void testTabChanges() {
		log.info("Open tab Changes...")
		def href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh')) }
	}
	
	void testTabProcesses() {
		log.info("Open tab Processes...")
		def href = $('#secondLevelTabs a span', text: 'Processes').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#applicationProcesses table tfoot a', text: 'Refresh')) }
		
		List items = getPagedObjects('#applicationProcesses table thead th span', '#applicationProcesses tbody tr td:first-child a', [])
		log.info("Application process records: ${items.size()}")
		
		def procLimit = getConfProperty('applicationProcesses')
		items.eachWithIndex { it, idx ->
			if (idx < procLimit) {
				testProcess(it)
			}
		}
	}
	
	void testProcess(proc) {
		// Design
		//---------------------
		go proc.href
		waitFor {
			def href = $('div.toolbar-container')
			verifyNotEmpty(href)
		}
		
		// Configuration
		//---------------------
		def href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('div.twoPaneDetail div.containerLabel', text: 'Basic Settings')) }

		/*
		// Inventory Changes -- not available to read-only user
		href = $('#secondLevelTabs a span', text: 'Inventory Changes').parent()
		href.click()
		waitFor { verifyNotEmpty($('#inventoryChanges tfoot a', text: 'Refresh')) }
		*/
		
		// Changes
		//---------------------
		href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		waitFor { verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh')) }
	}
	
	void testTabSnapshots() {
		log.info("Open tab Snapshots...")
		def href = $('#secondLevelTabs a span', text: 'Snapshots').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#snapshots table tfoot a', text: 'Refresh')) }
		
		def obj = $('#snapshots table thead th span')
		if (obj instanceof NonEmptyNavigator) {
			List items = getPagedObjects('#snapshots table thead th span', '#snapshots tbody tr td:first-child a', [])
			log.info("Application snapshot records: ${items.size()}")
			
			items.each { it ->
				testSnapshot(it)
			}
		}
	}

	void testSnapshot(snapshot) {
		// Dashboard
		//---------------------
		go snapshot.href
		waitFor { verifyNotEmpty($('#snapshotEnvironmentList')) }
		
		// Component Versions
		//---------------------
		def href = $('#secondLevelTabs a span', text: 'Component Versions').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#snapshotVersions thread th span', text: 'Component')) }

		// Configuration
		href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		waitFor { verifyNotEmpty($('div.twoPaneDetail div.containerLabel', text: 'Basic Settings')) }
	}
	
	@DataProvider
	public Object[][] provideApplications() {
		Object[][] result = new Object[appList.size()][1]
		
		appList.eachWithIndex { it, i ->
			result[i][0] = it
		}
		
		return result
	}
}
