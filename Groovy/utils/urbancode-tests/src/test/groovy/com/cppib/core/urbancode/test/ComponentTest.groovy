package com.cppib.core.urbancode.test

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class ComponentTest extends AbstractTest {
	static Log log = LogFactory.getLog(ComponentTest.class)
	
	List componentList
	
	@Test
	void listComponents() {
		goMainPage()

		def components = getEnvProperty('components')
		assert components != null
		
		componentList = getPagedObjects('#componentList table tfoot a', '#componentList tbody tr a', components)
		log.info("Component list: ${componentList}")
	}
	
	void goMainPage() {
		go '#components'
		waitFor {
			verifyNotEmpty($('#componentList table tfoot a', text: 'Refresh'))
		}
	}

	@Test(dependsOnMethods = ['listComponents'], dataProvider = 'provideComponents' )
	void testComponent(component) {
		// dashboard
		log.info("Open component ${component.name}, href=${component.href}")
		go component.href
		
		waitFor {
			verifyNotEmpty($('#componentResources table tfoot a', text: 'Refresh'))
			verifyNotEmpty($('#componentRequestHistory table tfoot a', text: 'Refresh'))
		}
		
		testTabConfiguration()
		
		testTabVersions()
		
		testTabProcesses()
		
		testTabChanges()
	}
	
	void testTabConfiguration() {
		def href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('div.sectionLabel', text: 'Cleanup Configuration')) }
		
		// Component Properties
		href = $('div.twoPaneList div', text: 'Component Properties')
		href.click()
		waitFor { verifyNotEmpty($('div table tfoot a', text: 'Refresh')) }
		
		// Environment Property Definitions
		href = $('div.twoPaneList div', text: 'Environment Property Definitions')
		href.click()
		waitFor { verifyNotEmpty($('div table tfoot a', text: 'Refresh')) }
		
		// Version Import History
		href = $('div.twoPaneList div', text: 'Version Import History')
		href.click()
		waitFor { verifyNotEmpty($('div table tfoot a', text: 'Refresh')) }
	}
	
	void testTabVersions() {
		def href = $('#secondLevelTabs a span', text: 'Versions').parent()
		href.click()
		
		waitFor {
			verifyNotEmpty($('#versions table tfoot a', text: 'Refresh'))
		}
	}
	
	void testTabProcesses() {
		def href = $('#secondLevelTabs a span', text: 'Processes').parent()
		href.click()
		
		waitFor {
			verifyNotEmpty($('#componentProcesses table tfoot a', text: 'Refresh'))
		}
		
		def procLimit = getConfProperty('componentProcesses')
		
		// list processes
		List processList = getPagedObjects('#componentProcesses table tfoot a', '#componentProcesses tbody tr td:first-child a', [])
		processList.eachWithIndex { it, idx ->
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

		// Component Process Properties
		href = $('div.twoPaneList div', text: 'Component Process Properties')
		href.click()
		waitFor { verifyNotEmpty($('div.propDefs table tfoot a', text: 'Refresh')) }
		
		// Changes
		//---------------------
		href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		waitFor { verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh')) }
	}

	void testTabChanges() {
		def href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		
		waitFor {
			verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh'))
		}
	}

	@DataProvider
	public Object[][] provideComponents() {
		Object[][] result = new Object[componentList.size()][1]
		
		componentList.eachWithIndex { it, i ->
			result[i][0] = it
		}
		
		return result
	}
}
