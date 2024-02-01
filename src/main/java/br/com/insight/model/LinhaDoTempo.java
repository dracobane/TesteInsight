package br.com.insight.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class LinhaDoTempo {
	
	private LocalDateTime horario;
	private HorarioSuporte padrao;
	private HorarioSuporte lancamento;
	
	public LinhaDoTempo(LocalDateTime horario) {
		this.horario = horario;
	}

	public LinhaDoTempo() {
	}

	public LinhaDoTempo(LocalDateTime horario, HorarioSuporte padrao, HorarioSuporte lancamento) {
		this.horario = horario;
		this.padrao = padrao;
		this.lancamento = lancamento;
	}

	public LocalDateTime getHorario() {
		return horario;
	}
	
	public void setHorario(LocalDateTime horario) {
		this.horario = horario;
	}
	
	public HorarioSuporte getPadrao() {
		return padrao;
	}
	
	public void setPadrao(HorarioSuporte padrao) {
		this.padrao = padrao;
	}
	
	public HorarioSuporte getLancamento() {
		return lancamento;
	}
	
	public void setLancamento(HorarioSuporte lancamento) {
		this.lancamento = lancamento;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(horario);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinhaDoTempo other = (LinhaDoTempo) obj;
		return Objects.equals(horario, other.horario);
	}
	
	public String toString() {
		String retorno = "horario: ["+horario.toString()+"] :: ";
		if (padrao != null) {
			retorno += "Padrao ["+padrao.getHorario().toString()+" "+(padrao.isEntrada()?"entrada":"saida")+"] ";
		}
		if (lancamento != null) {
			retorno += "Lancamento ["+lancamento.getHorario().toString()+" "+(lancamento.isEntrada()?"entrada":"saida")+"] ";
		}
		return retorno;
	}

}