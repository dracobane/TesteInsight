package br.com.insight.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class HorarioSuporte {
	
	private boolean entrada;
	private LocalDateTime horario;
	
	public HorarioSuporte(boolean entrada, LocalDateTime horario) {
		this.entrada = entrada;
		this.horario = horario;
	}
	
	public boolean isEntrada() {
		return entrada;
	}
	
	public void setEntrada(boolean entrada) {
		this.entrada = entrada;
	}
	
	public LocalDateTime getHorario() {
		return horario;
	}
	
	public void setHorario(LocalDateTime horario) {
		this.horario = horario;
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
		HorarioSuporte other = (HorarioSuporte) obj;
		return Objects.equals(horario, other.horario);
	}

}