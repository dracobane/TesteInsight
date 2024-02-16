package br.com.insight.report;

import java.util.List;

public class Report {
	
	private String empresa;
	private String cnpj;
	private String endereco;
	private String cidade;
	private String funcionario;
	private String depto;
	private String matricula;
	private String cargo;
	private String periodo;
	private String data;
	private List<ReportPeriodo> padraoList;
	private List<ReportPeriodo> lancamentoList;
	private List<ReportPeriodo> atrasoList;
	private List<ReportPeriodo> extraList;
	private String totalTrabalhado;
	private String totalAtrasos;
	private String totalExtras;
	
	public String getEmpresa() {
		return empresa;
	}
	public void setEmpresa(String empresa) {
		this.empresa = empresa;
	}
	public String getCnpj() {
		return cnpj;
	}
	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}
	public String getEndereco() {
		return endereco;
	}
	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}
	public String getCidade() {
		return cidade;
	}
	public void setCidade(String cidade) {
		this.cidade = cidade;
	}
	public String getFuncionario() {
		return funcionario;
	}
	public void setFuncionario(String funcionario) {
		this.funcionario = funcionario;
	}
	public String getDepto() {
		return depto;
	}
	public void setDepto(String depto) {
		this.depto = depto;
	}
	public String getMatricula() {
		return matricula;
	}
	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}
	public String getCargo() {
		return cargo;
	}
	public void setCargo(String cargo) {
		this.cargo = cargo;
	}
	public String getPeriodo() {
		return periodo;
	}
	public void setPeriodo(String periodo) {
		this.periodo = periodo;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public List<ReportPeriodo> getPadraoList() {
		return padraoList;
	}
	public void setPadraoList(List<ReportPeriodo> padraoList) {
		this.padraoList = padraoList;
	}
	public List<ReportPeriodo> getLancamentoList() {
		return lancamentoList;
	}
	public void setLancamentoList(List<ReportPeriodo> lancamentoList) {
		this.lancamentoList = lancamentoList;
	}
	public List<ReportPeriodo> getAtrasoList() {
		return atrasoList;
	}
	public void setAtrasoList(List<ReportPeriodo> atrasoList) {
		this.atrasoList = atrasoList;
	}
	public List<ReportPeriodo> getExtraList() {
		return extraList;
	}
	public void setExtraList(List<ReportPeriodo> extraList) {
		this.extraList = extraList;
	}
	public String getTotalTrabalhado() {
		return totalTrabalhado;
	}
	public void setTotalTrabalhado(String totalTrabalhado) {
		this.totalTrabalhado = totalTrabalhado;
	}
	public String getTotalAtrasos() {
		return totalAtrasos;
	}
	public void setTotalAtrasos(String totalAtrasos) {
		this.totalAtrasos = totalAtrasos;
	}
	public String getTotalExtras() {
		return totalExtras;
	}
	public void setTotalExtras(String totalExtras) {
		this.totalExtras = totalExtras;
	}
	
}