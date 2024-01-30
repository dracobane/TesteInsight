package br.com.insight.model;

import java.time.LocalDateTime;

/**
 * @author Rodrigo G de Souza
 */
public class Horario {
	
	private LocalDateTime entrada;
	private LocalDateTime saida;
	private boolean padrao;
	
	public Horario(LocalDateTime entrada, LocalDateTime saida, boolean padrao) {
		this.entrada = entrada;
		this.saida = saida;
		this.padrao = padrao;
	}

	public LocalDateTime getEntrada() {
		return entrada;
	}

	public void setEntrada(LocalDateTime entrada) {
		this.entrada = entrada;
	}

	public LocalDateTime getSaida() {
		return saida;
	}

	public void setSaida(LocalDateTime saida) {
		this.saida = saida;
	}

	public boolean isPadrao() {
		return padrao;
	}

	public void setPadrao(boolean padrao) {
		this.padrao = padrao;
	}

}