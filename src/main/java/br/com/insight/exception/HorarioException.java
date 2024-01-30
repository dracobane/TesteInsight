package br.com.insight.exception;

/**
 * @author Rodrigo G de Souza
 */
public class HorarioException extends Exception {
	
	private static final long serialVersionUID = -8143205585491188330L;
	
	private String message;
	
	public HorarioException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
}