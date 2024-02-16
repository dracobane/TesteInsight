package br.com.insight.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.insight.dto.HorarioDTO;
import br.com.insight.exception.HorarioException;
import br.com.insight.exception.RelatorioException;
import br.com.insight.model.Horario;
import br.com.insight.model.HorarioSuporte;
import br.com.insight.model.LinhaDoTempo;
import br.com.insight.report.Report;
import br.com.insight.report.ReportGenerate;
import br.com.insight.report.ReportPeriodo;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Rodrigo G de Souza
 */
@WebServlet(name = "ControlarFolhaPonto", urlPatterns = {"/ControlarFolhaPonto"})
public class ControlarFolhaPonto extends HttpServlet {
	
	/**
	 * Serial version.
	 */
	private static final long serialVersionUID = -1873002750257597327L;
	
	private static DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
	private static DateTimeFormatter DFMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
	
	public ControlarFolhaPonto() {
		super();
	}
	
	/**
	 * Realiza a organizacao e o processamento dos dados de atrasos e extras de acordo
	 * com o request da tela.
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	String[] entradaPadrao = request.getParameterValues("entradaPadrao");
    	String[] saidaPadrao = request.getParameterValues("saidaPadrao");
    	String[] entrada = request.getParameterValues("entrada");
    	String[] saida = request.getParameterValues("saida");
    	String acao = request.getParameter("acao");
    	
    	List<Horario> padraoList = new ArrayList<>();
		try {
			padraoList = obterEntradasSaidasPadrao(response, entradaPadrao, saidaPadrao);
		} catch (HorarioException e) {
			retornoDoProcessamento(response, e.getMessage(), false);
		}
    	
    	List<Horario> lancamentoList = new ArrayList<>();
		try {
			lancamentoList = obterLancamentos(response, padraoList, entrada, saida);
		} catch (HorarioException e) {
			retornoDoProcessamento(response, e.getMessage(), false);
		}
    	
		List<HorarioDTO> extrasList = new ArrayList<>();
		List<HorarioDTO> atrasosList = new ArrayList<>();
		processarExtrasEAtrasos(extrasList, atrasosList, padraoList, lancamentoList);
		
		if (acao.equals("download")) {
			geraArquivoJsonReports(response, extrasList, atrasosList, padraoList, lancamentoList);
			
		} else {
			String mensagem = "Processamento executado com sucesso";
			retornoDoProcessamento(response, mensagem, true, extrasList, atrasosList);
		}
	}

	/**
	 * Constroi o arquivo base para alimentar o Reports.
	 * 
	 * @param response 
	 * @param extrasList
	 * @param atrasosList
	 * @param padraoList
	 * @param lancamentoList
	 * @throws IOException 
	 */
	private void geraArquivoJsonReports(HttpServletResponse response, List<HorarioDTO> extrasList, List<HorarioDTO> atrasosList,
			List<Horario> padraoList, List<Horario> lancamentoList) throws IOException {
		
		List<ReportPeriodo> reportPadraoList = padraoList.stream().map(p->new ReportPeriodo(FMT.format(p.getEntrada()),FMT.format(p.getSaida()), calculaDiferencaTempo(p.getEntrada(), p.getSaida()).toString())).toList();
		List<ReportPeriodo> reportLancamentoList = lancamentoList.stream().map(p->new ReportPeriodo(FMT.format(p.getEntrada()),FMT.format(p.getSaida()), calculaDiferencaTempo(p.getEntrada(), p.getSaida()).toString())).toList();
		
		List<ReportPeriodo> reportAtrasoList = atrasosList.stream().map(p->new ReportPeriodo(p.entrada(),p.saida(),p.diferenca())).toList();
		List<ReportPeriodo> reportExtraList = extrasList.stream().map(p->new ReportPeriodo(p.entrada(),p.saida(),p.diferenca())).toList();
		
		Report report = new Report();
		report.setEmpresa("Pedreira Botucatu Ltda");
		report.setCnpj("02.313.036/0001-50");
		report.setEndereco("Alcides Soares");
		report.setCidade("Botucatu");
		report.setFuncionario("Antonio Moreira");
		report.setDepto("Engenharia");
		report.setMatricula("22554488");
		report.setCargo("22-Engenheiro Civil Jr");
		report.setPeriodo("01/2024");
		report.setData(DFMT.format(LocalDate.now()));
		report.setPadraoList(reportPadraoList);
		report.setLancamentoList(reportLancamentoList);
		report.setAtrasoList(reportAtrasoList);
		report.setExtraList(reportExtraList);
		report.setTotalTrabalhado(calcularTotalHorasReport(report.getLancamentoList()));
		report.setTotalAtrasos(calcularTotalHorasReport(report.getAtrasoList()));
		report.setTotalExtras(calcularTotalHorasReport(report.getExtraList()));
		
		ServletOutputStream ouputStream = response.getOutputStream();
		int randomico = (int)(Math.random() * 1000);
		String nomeDoArquivo = "relatorio_ponto_"+randomico+".pdf";
		try {
			ReportGenerate reportGenerate = new ReportGenerate();
			byte[] relt = reportGenerate.generateReport(getServletContext().getRealPath("WEB-INF/reports/folha_de_ponto.jasper"), new HashMap<>(), report);
			
			response.setContentType("application/pdf");
			response.setContentLength(relt.length);
			response.setHeader("Content-disposition", "inline;filename="+nomeDoArquivo);
			//response.setHeader("Content-disposition", "attachment;filename="+nomeDoArquivo);
			ouputStream.write(relt);
			response.getCharacterEncoding();
	        ouputStream.flush();
	        
		} catch (RelatorioException e) {
			retornoDoProcessamento(response, e.getMessage(), false);
		} finally {
			ouputStream.close();
		}
		
	}

	/**
	 * Calcula o total de horas para o Relatorio de acordo com a lista informada.
	 * 
	 * @param lista
	 * @return
	 */
	private String calcularTotalHorasReport(List<ReportPeriodo> lista) {
		LocalTime horas = LocalTime.parse("00:00");
		for (ReportPeriodo p : lista) {
			Long minutes = Duration.between(LocalDateTime.of(LocalDate.now(), LocalTime.parse("00:00")), 
					LocalDateTime.of(LocalDate.now(), LocalTime.parse(p.getIntervalo()))).toMinutes();
			horas = horas.plus(minutes, ChronoUnit.MINUTES);
		}
		return horas.toString();
	}

	/**
	 * Processa os atrasos e extras com base na lista de lancamentos do funcionario,
	 * comparando-a com a lista de padroes pre-estabelecidos.
	 * 
	 * @param extrasList
	 * @param atrasosList
	 * @param padraoList
	 * @param lancamentoList
	 */
	private void processarExtrasEAtrasos(List<HorarioDTO> extrasList, List<HorarioDTO> atrasosList,
			List<Horario> padraoList, List<Horario> lancamentoList) {
		
		List<LinhaDoTempo> tempo = constroiLinhaTemporal(padraoList, lancamentoList);
		
		// Percorre a lista temporal olhando a posicao atual e a proxima
		boolean iniciouPadrao = false;
		boolean iniciouLancamento = false;
		for (int i=0; i < tempo.size(); i++) {
			LinhaDoTempo atual = tempo.get(i);
			LinhaDoTempo futuro = tempo.size() > i+1 ? tempo.get(i+1) : null;
			
			if (atual.getPadrao() != null && atual.getLancamento() == null) {//eh padrao e nao eh lancamento
				iniciouPadrao = atual.getPadrao().isEntrada();
				if (futuro != null) {
					if (!iniciouLancamento && iniciouPadrao) {
						alimentaListaExtraOuAtraso(atrasosList, atual.getHorario(), futuro.getHorario());
					} else if (futuro.getPadrao() != null && !iniciouPadrao && iniciouLancamento) {
						alimentaListaExtraOuAtraso(extrasList, atual.getHorario(), futuro.getHorario());
					} else if (futuro.getLancamento() != null && iniciouLancamento && !iniciouPadrao) {
						alimentaListaExtraOuAtraso(extrasList, atual.getHorario(), futuro.getHorario());
					}
				}
			}
			if (atual.getPadrao() == null && atual.getLancamento() != null) {//eh lancamento e nao eh padrao
				iniciouLancamento = atual.getLancamento().isEntrada();
				if (futuro != null) {
					if (!iniciouPadrao && iniciouLancamento) {
						alimentaListaExtraOuAtraso(extrasList, atual.getHorario(), futuro.getHorario());
					} else if (futuro.getLancamento() != null && !iniciouLancamento && iniciouPadrao) {
						alimentaListaExtraOuAtraso(atrasosList, atual.getHorario(), futuro.getHorario());
					} else if (futuro.getPadrao() != null && iniciouPadrao && !iniciouLancamento) {
						alimentaListaExtraOuAtraso(atrasosList, atual.getHorario(), futuro.getHorario());
					}
				}
			}
			if (atual.getPadrao() != null && atual.getLancamento() != null) {//ambos atualizam os marcadores
				iniciouPadrao = atual.getPadrao().isEntrada();
				iniciouLancamento = atual.getLancamento().isEntrada();
				if (futuro != null) {
					if (!iniciouPadrao && iniciouLancamento) {
						alimentaListaExtraOuAtraso(extrasList, atual.getHorario(), futuro.getHorario());
					} else if (iniciouPadrao && !iniciouLancamento) {
						alimentaListaExtraOuAtraso(atrasosList, atual.getHorario(), futuro.getHorario());
					}
				}
			}
		}
		
	}
	
	/**
	 * Constroi uma lista com a linha do tempo e seus lancamentos, tanto padrao como trabalhadas.
	 * 
	 * @param padraoList
	 * @param lancamentoList
	 * @return
	 */
	private List<LinhaDoTempo> constroiLinhaTemporal(List<Horario> padraoList, List<Horario> lancamentoList) {
		List<LinhaDoTempo> tempo = new LinkedList<>();
		for (Horario h : padraoList) {
			tempo.add(new LinhaDoTempo(h.getEntrada(), new HorarioSuporte(true, h.getEntrada()), null));
			tempo.add(new LinhaDoTempo(h.getSaida(), new HorarioSuporte(false, h.getSaida()), null));
		}
		for (Horario l : lancamentoList) {
			LinhaDoTempo t = tempo.get(0);
			if (t.getHorario().equals(l.getEntrada())) {
				t.setLancamento(new HorarioSuporte(true, l.getEntrada()));
			} else {
				for (int j=0; j < tempo.size(); j++) {
					LinhaDoTempo temp = tempo.get(j);
					if (temp.getHorario().equals(l.getEntrada())) {
						temp.setLancamento(new HorarioSuporte(true, l.getEntrada()));
						break;
					} else if (temp.getHorario().isAfter(l.getEntrada())) {
						tempo.add(j, new LinhaDoTempo(l.getEntrada(), null, new HorarioSuporte(true, l.getEntrada())));
						break;
					}
				}
				if (!tempo.contains(new LinhaDoTempo(l.getEntrada()))) {
					tempo.add(new LinhaDoTempo(l.getEntrada(), null, new HorarioSuporte(true, l.getEntrada())));
				}
			}
			if (t.getHorario().equals(l.getSaida())) {
				t.setLancamento(new HorarioSuporte(false, l.getSaida()));
			} else {
				for (int j=0; j < tempo.size(); j++) {
					LinhaDoTempo temp = tempo.get(j);
					if (temp.getHorario().equals(l.getSaida())) {
						temp.setLancamento(new HorarioSuporte(false, l.getSaida()));
						break;
					} else if (temp.getHorario().isAfter(l.getSaida())) {
						tempo.add(j, new LinhaDoTempo(l.getSaida(), null, new HorarioSuporte(false, l.getSaida())));
						break;
					}
				}
				if (!tempo.contains(new LinhaDoTempo(l.getSaida()))) {
					tempo.add(new LinhaDoTempo(l.getSaida(), null, new HorarioSuporte(false, l.getSaida())));
				}
			}
		}
		return tempo;
	}

	/**
	 * Recebe uma lista, podendo ser atraso ou extra, e alimenta a lista com os dados informados
	 * atraves de um novo objeto.
	 * 
	 * @param lista
	 * @param h1
	 * @param h2
	 */
	private void alimentaListaExtraOuAtraso(List<HorarioDTO> lista, LocalDateTime h1, LocalDateTime h2) {
		LocalTime dif = calculaDiferencaTempo(h1, h2);
		HorarioDTO h = new HorarioDTO(FMT.format(h1), FMT.format(h2), dif.toString());
		lista.add(h);
	}

	/**
	 * Calcula a diferenca em horas e minutos entre dois horarios.
	 * 
	 * @param horario1
	 * @param horario2
	 * @return
	 */
	private LocalTime calculaDiferencaTempo(LocalDateTime horario1, LocalDateTime horario2) {
		LocalTime dif = LocalTime.parse("00:00");
		return dif.plus(horario1.until(horario2, ChronoUnit.MINUTES), ChronoUnit.MINUTES);
	}

	/**
	 * Obtem lancamentos atraves dos campos da tela.
	 * Na falta da data no lancamento, alguns tratamentos automaticos sao realizados porem, na
	 * aplicacao real, a data provavelmente vira do momento do lancamento.
	 * 
	 * @param response
	 * @param entrada
	 * @param saida
	 * @return
	 * @throws IOException
	 * @throws HorarioException
	 */
	private List<Horario> obterLancamentos(HttpServletResponse response, List<Horario> padroes, 
			String[] entrada, String[] saida) throws IOException, HorarioException {
		String mensagem;
		List<Horario> lancamentoList = new ArrayList<Horario>();
    	boolean umLancamento = false;
    	boolean diaPosterior = false;
    	LocalDateTime diaAtual = LocalDateTime.of(LocalDate.now(), LocalTime.parse("23:59"));
    	
    	for (int i=0; i < entrada.length; i++) {
    		if (!entrada[i].isBlank() && !saida[i].isBlank()) {
    			LocalTime entradaTime = entrada[i] != null ? LocalTime.parse(entrada[i]) : 
    				LocalTime.parse("00:00");
    			LocalTime saidaTime = saida[i] != null ? 
    					LocalTime.parse(saida[i]) : LocalTime.parse("00:00");
    			
    			// validar se algum periodo padrao ultrapassa o dia atual, se sim e a entrada coincide 
    			// com esse periodo, somar 1 dia a entrada
    			LocalDateTime entradaDTime = null;
    			for (int p=0; p < padroes.size(); p++) {
    				Horario h = padroes.get(p);
    				LocalTime h1 = LocalTime.parse(FMT.format(h.getEntrada()));
    				LocalTime h2 = LocalTime.parse(FMT.format(h.getSaida()));
    				LocalTime h2f = padroes.size() > p+1 ? LocalTime.parse(FMT.format(padroes.get(p+1).getSaida())) : null;
    				
    				if (!h.getEntrada().isAfter(diaAtual) && //entrada padrao anterior a 23H59 do dia atual e
    					h.getSaida().isAfter(diaAtual) && //saida padrao posterior a 23h59 do dia atual e
    					!LocalTime.parse(entrada[i]).isAfter(h1) && //hora lancamento de entrada igual ou anterior a entrada padrao e
    					!LocalTime.parse(entrada[i]).isAfter(h2) && //hora lancamento de entrada igual ou anterior a saida padrao e
    					LocalTime.parse(entrada[i]).isBefore(LocalTime.parse("23:59"))) { //lancamento de entrada anterior a 23H59
    					entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
    					diaPosterior = true;
    					break;
    					
    				} else if (diaPosterior) {
    					entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
    					break;
    					
    				} else if (!h.getEntrada().isAfter(diaAtual) && //entrada padrao anterior a 23H59 do dia atual e
    							(h.getSaida().isAfter(diaAtual) || //saida padrao posterior a 23h59 do dia atual ou
    							padroes.size() > p+1 && padroes.get(p+1).getEntrada().isAfter(diaAtual)) && //proxima entrada apos dia atual e
    							(LocalTime.parse(entrada[i]).isBefore(h1) || //entrada anterior a entrada padrao ou
    							(h2f != null && LocalTime.parse(entrada[i]).isBefore(h2f))) && // a entrada nao eh maior que a saida posterior e
    							LocalTime.parse(entrada[i]).isBefore(LocalTime.parse(saida[i]))) { // lancamento da entrada eh anterior a propria saida
    					if (LocalTime.parse(entrada[i]).getHour() - padroes.get(padroes.size()-1).getSaida().getHour() < 10) {
							entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
							diaPosterior = true;
							break;
    					}
    				}
    				
    			}
    			if (entradaDTime == null) {
    				entradaDTime = LocalDateTime.of(LocalDate.now(), entradaTime);
    			}
    			
    			LocalDateTime saidaDTime = null;
    			if (saidaTime.isBefore(entradaTime) || diaPosterior) {
    				saidaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), saidaTime);
    				diaPosterior = true;
    			} else {
    				saidaDTime = LocalDateTime.of(LocalDate.now(), saidaTime);
    			}
    			
	    		Horario h = new Horario(entradaDTime, saidaDTime, false);
	    		lancamentoList.add(h);
	    		umLancamento = true;
	    		
    		} else {
    			if (entrada[i].isBlank() && saida[i].isBlank() && !umLancamento) {
    				boolean algumPreenchimento = false;
    				for (int j=0; j < entrada.length; j++) {
    					if (entrada[j] != null && (!entrada[j].isBlank() || !saida[j].isBlank())) {
    						algumPreenchimento = true;
    						break;
    					}
    				}
    				if (!algumPreenchimento) {
    					mensagem = "Precisa preencher ao menos uma entrada e saida";
        				throw new HorarioException(mensagem);
    				}
    			}
    			if (entrada[i].isBlank() && !saida[i].isBlank()) {
    				mensagem = "Precisa preencher a entrada da saida: "+saida[i];
    				throw new HorarioException(mensagem);
    			}
    			if (!entrada[i].isBlank() && saida[i].isBlank()) {
    				mensagem = "Precisa preencher a saida da entrada "+entrada[i];
    				throw new HorarioException(mensagem);
    			}
    		}
    	}
		return lancamentoList;
	}

	/**
	 * Obtem os lancamentos padrao atraves dos campos da tela.
	 * Na falta da data no lancamento, alguns tratamentos automaticos sao realizados porem, na
	 * aplicacao real, a data provavelmente vira do momento do lancamento.
	 * 
	 * @param response
	 * @param entradaPadrao
	 * @param saidaPadrao
	 * @return
	 * @throws IOException
	 * @throws HorarioException
	 */
	private List<Horario> obterEntradasSaidasPadrao(HttpServletResponse response, String[] entradaPadrao,
			String[] saidaPadrao) throws IOException, HorarioException {
		String mensagem;
		List<Horario> padraoList = new ArrayList<Horario>();
    	boolean umPadrao = false;
    	boolean diaPosterior = false;
    	for (int i=0; i < entradaPadrao.length; i++) {
    		String saidaPadraoAnterior = i > 0 ? saidaPadrao[i-1] : null;
    		if (!entradaPadrao[i].isBlank() && !saidaPadrao[i].isBlank()) {
	    		LocalTime entradaTime = entradaPadrao[i] != null ? LocalTime.parse(entradaPadrao[i]) : 
	    			LocalTime.parse("00:00");
	    		LocalDateTime entradaDTime = null;
	    		
	    		if (saidaPadraoAnterior != null && 
	    			LocalTime.parse(saidaPadrao[i]).isBefore(LocalTime.parse(saidaPadraoAnterior))) {
	    			diaPosterior = true;
	    		}
	    		
	    		if (diaPosterior) {
	    			entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
	    		}
	    		
	    		if (entradaDTime == null) {
	    			entradaDTime = LocalDateTime.of(LocalDate.now(), entradaTime);
	    		}
	    		
	    		LocalTime saidaTime = saidaPadrao[i] != null ? LocalTime.parse(saidaPadrao[i]) : 
	    			LocalTime.parse("00:00");
	    		LocalDateTime saidaDTime = null;
	    		if (saidaTime.isBefore(entradaTime) || diaPosterior) {
	    			saidaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), saidaTime);
	    			diaPosterior = true;
	    		} else {
	    			saidaDTime = LocalDateTime.of(LocalDate.now(), saidaTime);
	    		}
	    		
	    		Horario h = new Horario(entradaDTime, saidaDTime, true);
	    		padraoList.add(h);
	    		umPadrao = true;
	    		
    		} else {
    			if (entradaPadrao[i].isBlank() && saidaPadrao[i].isBlank() && !umPadrao) {
    				boolean algumPreenchimento = false;
    				for (int j=0; j < entradaPadrao.length; j++) {
    					if (entradaPadrao[j] != null && (!entradaPadrao[j].isBlank() || 
    							!saidaPadrao[j].isBlank())) {
    						algumPreenchimento = true;
    						break;
    					}
    				}
    				if (!algumPreenchimento) {
    					mensagem = "Precisa preencher ao menos uma entrada e saida padrao";
        				throw new HorarioException(mensagem);
    				}
    			}
    			if (entradaPadrao[i].isBlank() && !saidaPadrao[i].isBlank()) {
    				mensagem = "Precisa preencher a entrada padrao";
    				throw new HorarioException(mensagem);
    			}
    			if (!entradaPadrao[i].isBlank() && saidaPadrao[i].isBlank()) {
    				mensagem = "Precisa preencher a saida padrao";
    				throw new HorarioException(mensagem);
    			}
    		}
    	}
		return padraoList;
	}

	/**
	 * Sobrecarga do metodo de retorno a tela, passando duas listas vazias em extras e atrasos.
	 * 
	 * @param response
	 * @param mensagem
	 * @param status
	 * @throws IOException
	 */
	private void retornoDoProcessamento(HttpServletResponse response, String mensagem, boolean status) 
			throws IOException {
		retornoDoProcessamento(response, mensagem, status, new ArrayList<HorarioDTO>(), 
				new ArrayList<HorarioDTO>());
	}

	/**
	 * Retorno do processamento, enviando para a tela um JSON com os dados pertinentes.
	 * 
	 * @param resp
	 * @param mensagem
	 * @param status
	 * @param extras
	 * @param atrasos
	 * @throws IOException
	 */
	private void retornoDoProcessamento(HttpServletResponse resp, String mensagem, boolean status, 
			List<HorarioDTO> extras, List<HorarioDTO> atrasos) throws IOException {
		Gson gson = new Gson();
		JsonObject json = new JsonObject();
		json.add("message", gson.toJsonTree(mensagem));
		json.add("extras", gson.toJsonTree(extras));
		json.add("atrasos", gson.toJsonTree(atrasos));
		json.add("status", gson.toJsonTree(status));
		String retorno = json.toString();
		
		PrintWriter writer = resp.getWriter();
		writer.println(retorno);
		writer.flush();
		writer.close();
	}

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException{
        process(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException{
        process(request, response);
    }

}