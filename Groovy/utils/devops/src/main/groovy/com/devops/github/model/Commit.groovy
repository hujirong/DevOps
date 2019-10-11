package com.devops.github.model

class Commit {
	String id
	String tree_id
	String distinct
	String message
	String timestamp
	String url
	Map author
	Map committer
	
	// Added Files
	List added = []
	// Modified Files
	List modified = []
	// Removed Files
	List removed = []
	
	public List getId() {
		return id
	}
	
	public List getAdded() {
		return added
	}
	public List getModified() {
		return modified
	}
	public List getRemoved() {
		return removed
	}
}
