package br.com.insight.exception;

/**
 * @author Rodrigo G de Souza
 */
public class RelatorioException extends Exception {
	
	private static final long serialVersionUID = -849119594970966878L;
	
	private String message;
	
	public RelatorioException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
}