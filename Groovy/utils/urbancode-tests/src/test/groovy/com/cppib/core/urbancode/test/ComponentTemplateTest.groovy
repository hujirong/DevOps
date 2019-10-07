package com.cppib.core.urbancode.test

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

// Test component template
class ComponentTemplateTest extends AbstractTest {
	static Log log = LogFactory.getLog(ComponentTemplateTest.class)
	
	List templateList
	
	@Test
	void listTemplates() {
		goMainPage()

		def templates = getEnvProperty('componentTemplates')
		assert templates != null
		
		templateList = getPagedObjects('#componentTemplateList table tfoot a', '#componentTemplateList tbody tr td:first-child a', templates)
		log.info("ComponentTemplate list: ${templateList}")
	}
	
	void goMainPage() {
		go '#components/templates'
		waitFor {
			verifyNotEmpty($('#componentTemplateList table tfoot a', text: 'Refresh'))
		}
	}

	@Test(dependsOnMethods = ['listTemplates'], dataProvider = 'provideTemplates' )
	void testTemplate(template) {
		log.info("Open template ${template.name}, href=${template.href}")
		goTemplate(template)
		
		testTabConfiguration()
		goTemplate(template)

		testTabProcesses()
		goTemplate(template)
		
		testTabChanges()
	}
	
	void goTemplate(template) {
		go template.href
		
		waitFor {
			verifyNotEmpty($('#componentList table tfoot a', text: 'Refresh'))
			def h1 = $('h1')
			verifyNotEmpty(h1) && h1.text().contains(template.name)
		}
	}
	
	void testTabConfiguration() {
		def href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		
		waitFor {
			verifyNotEmpty($('div.twoPaneDetail div.containerLabel', text: 'Basic Settings'))
		}
		
		// properties
		href = $('div.twoPaneList div', text: 'Properties')
		href.click()
		waitFor { verifyNotEmpty($('div.propValues table tfoot a', text: 'Refresh')) }
		
		// Component Property Definitions
		href = $('div.twoPaneList div', text: 'Component Property Definitions')
		href.click()
		waitFor { verifyNotEmpty($('div.twoPaneDetailPadding table tfoot a', text: 'Refresh')) }
		
		// Environment Property Definitions
		href = $('div.twoPaneList div', text: 'Environment Property Definitions')
		href.click()
		waitFor { verifyNotEmpty($('div.propDefs table tfoot a', text: 'Refresh')) }
		
		// Resource Property Definitions
		href = $('div.twoPaneList div', text: 'Resource Property Definitions')
		href.click()
		waitFor { verifyNotEmpty($('div.propDefs table tfoot a', text: 'Refresh')) }
	}
	
	void testTabProcesses() {
		def href = $('#secondLevelTabs a span', text: 'Processes').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#componentProcesses table tfoot a', text: 'Refresh')) }
		
		def procLimit = getConfProperty('componentTeamplteProcesses')
		
		// list processes
		List processList = getPagedObjects('#componentProcesses table tfoot a', '#componentProcesses tbody tr td:first-child a', [])
		processList.eachWithIndex { it, idx ->
			if (idx < procLimit) {
				testProcess(it)
			}
		}
	}

	void testProcess(proc) {
		log.info("Open template process ${proc.name}, href=${proc.href}")
		go(proc.href)
		
		waitFor {
			def href = $('div.toolbar-container')
			verifyNotEmpty(href)
		}
		
		// configuration
		// ---------------------------------------
		def href = $('#secondLevelTabs a span', text: 'Configuration').parent()
		href.click()
		
		waitFor {
			verifyNotEmpty($('div.twoPaneDetail div.containerLabel', text: 'Basic Settings'))
		}
		
		// Component Process Properties
		href = $('div.twoPaneList div', text: 'Component Process Properties')
		href.click()
		waitFor { verifyNotEmpty($('div.propDefs table tfoot a', text: 'Refresh')) }

		// Changes
		// ---------------------------------------
		href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		
		waitFor { verifyNotEmpty($('#changelog table tfoot a', text: 'Refresh')) }
	}
	
	// @Test(dependsOnMethods = ['testTemplate'])
	void testTabChanges() {
		def href = $('#secondLevelTabs a span', text: 'Changes').parent()
		href.click()
		
		waitFor {
			def obj = $('#changelog table tfoot a', text: 'Refresh')
			verifyNotEmpty(obj) 
		}
	}
	
	@DataProvider
	public Object[][] provideTemplates() {
		Object[][] result = new Object[templateList.size()][1]
		
		templateList.eachWithIndex { it, i ->
			result[i][0] = it
		}
		
		return result
	}
}
