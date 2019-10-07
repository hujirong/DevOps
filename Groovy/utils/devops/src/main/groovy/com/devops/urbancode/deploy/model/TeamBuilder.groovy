package com.devops.urbancode.deploy.model

import java.io.PrintWriter;


/**
 * Build Team configuration file
 * 
 *
 */
class TeamBuilder extends BuilderSupport {
	IndentPrinter out
	List stacks = []
	
	TeamBuilder() {
		this(new PrintWriter(System.out))
	}
	
	TeamBuilder(Writer writer) {
		this(new IndentPrinter(writer, '\t'))
	}
	
	TeamBuilder(IndentPrinter out) {
		this.out = out
	}
	
	def println() {
		out.println()
	}
	
	def comments(String msg) {
		out.printIndent()
		out.println('//' + msg)
	}
	
	@Override
	protected void setParent(Object parent, Object child) {
		
	}

	@Override
	protected Object createNode(Object name) {
		return createNode(name, null, null)
	}

	@Override
	protected Object createNode(Object name, Object value) {
		return createNode(name, null, value)
	}

	@Override
	protected Object createNode(Object name, Map attributes) {
		return createNode(name, attributes, null)
	}

	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		if (current != null) {
			out.printIndent()
		}
		
		printNode(name, value, attributes)
		out.flush()
		
		return name
	}

	def printNode(name, value, attrs) {
		out.print(name)
		
		if (value != null) {
			if (attrs != null) {
				out.println(" ('$value') {")
			} else {
				out.println(" = '$value'")
			}
		}
		
		if (attrs != null) {
			out.incrementIndent()
			printAttributes(attrs)
			stacks.push(true)
		} else {
			stacks.push(false)
		}
	}
	
	def printAttributes(Map attrs) {
		attrs.each { key, value ->
			out.printIndent()
			out.println("$key = '$value'")
		}
	}
	
	@Override
	protected void nodeCompleted(Object parent, Object node) {
		def incremented = stacks.pop()
		
		if (incremented) {
			out.decrementIndent()
			if (current != null) {
				out.printIndent()
			}
		}
		
		
		if (incremented) {
			out.println('}')
		}
		out.flush()
	}
}
