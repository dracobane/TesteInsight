package br.com.insight.model;

import java.time.LocalDateTime;
import java.util.Objects;

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
	
	public String toString() {
		return "["+entrada.toString()+"] - ["+saida.toString()+"]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(entrada, saida);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Horario other = (Horario) obj;
		return Objects.equals(entrada, other.entrada) && Objects.equals(saida, other.saida);
	}

}