package br.com.insight.report;

public class ReportPeriodo {
	
	private String entrada;
	private String saida;
	private String intervalo;
	
	public ReportPeriodo(String entrada, String saida, String intervalo) {
		this.entrada = entrada;
		this.saida = saida;
		this.intervalo = intervalo;
	}
	
	public String getEntrada() {
		return entrada;
	}
	public void setEntrada(String entrada) {
		this.entrada = entrada;
	}
	public String getSaida() {
		return saida;
	}
	public void setSaida(String saida) {
		this.saida = saida;
	}
	public String getIntervalo() {
		return intervalo;
	}
	public void setIntervalo(String intervalo) {
		this.intervalo = intervalo;
	}
	
}