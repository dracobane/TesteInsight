package br.com.insight.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.insight.dto.HorarioDTO;
import br.com.insight.exception.HorarioException;
import br.com.insight.model.Horario;
import jakarta.servlet.ServletException;
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
    	
    	String mensagem = "Processamento executado com sucesso";
    	retornoDoProcessamento(response, mensagem, true, extrasList, atrasosList);
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
		int padraoCont = 0;
		
		for (int i=0; i < lancamentoList.size(); i++) {
			Horario lancamento = lancamentoList.get(i);
			if (padraoCont == padraoList.size()) {
				//todos lancamentos serao extras
				alimentaListaExtraOuAtraso(extrasList, lancamento.getEntrada(), lancamento.getSaida());
				continue;
			}
			Horario padrao = padraoList.get(padraoCont);
			
			//1- entrou e saiu antes do padrao de entrada atual
			if (lancamento.getEntrada().isBefore(padrao.getEntrada()) && 
					!lancamento.getSaida().isAfter(padrao.getEntrada())) {
				//1a- calcula esse extra e vai pro proximo lancamento
				alimentaListaExtraOuAtraso(extrasList, lancamento.getEntrada(), lancamento.getSaida());
				
				//1b- existem mais lancamentos se nao, os proximos padroes sao atrasos
				if (lancamentoList.size() > i+1) {
					continue;
				} else {
					alimentaListaExtraOuAtraso(atrasosList, padrao.getEntrada(), padrao.getSaida());
					if (padraoList.size() > padraoCont+1) {
						alimentaListaExtraOuAtraso(atrasosList, padraoList.get(padraoCont+1).getEntrada(), 
								padraoList.get(padraoCont+1).getSaida());
					}
					if (padraoList.size() > padraoCont+2) {
						alimentaListaExtraOuAtraso(atrasosList, padraoList.get(padraoCont+2).getEntrada(), 
								padraoList.get(padraoCont+2).getSaida());
					}
					continue;
				}
				
			}
			
			//2- entrou antes e saiu depois do padrao de entrada atual
			if (lancamento.getEntrada().isBefore(padrao.getEntrada()) && 
					lancamento.getSaida().isAfter(padrao.getEntrada())) {
				//2a- calcula o extra entre o lancamento da entrada o padrao de entrada
				alimentaListaExtraOuAtraso(extrasList, lancamento.getEntrada(), padrao.getEntrada());
			}
			
			//3- entrou depois do padrao de entrada atual
			if (lancamento.getEntrada().isAfter(padrao.getEntrada())) {
				//3a- a entrada atual eh posterior a saida desse periodo, se sim, esse periodo eh um atraso, calcula e vai pro proximo periodo
				if (lancamento.getEntrada().isAfter(padrao.getSaida())) {
					alimentaListaExtraOuAtraso(atrasosList, padrao.getEntrada(), padrao.getSaida());
					padraoCont++;
					i--;
				
				//3b- a entrada atual eh anterior a saida do periodo, calcular o atraso
				} else {
					alimentaListaExtraOuAtraso(atrasosList, padrao.getEntrada(), lancamento.getEntrada());
				}
			}
			
			//4- a saida eh anterior ao padrao atual
			if (lancamento.getSaida().isBefore(padrao.getSaida())) {
				
				int contadorAtual = i;
				boolean entrou = false;
				//4a verifica se existe uma nova entrada de lancamento antes da saida padrao atual, 
				// se sim, o atraso eh entre a saida atual e a nova entrada
				while (lancamentoList.size() < contadorAtual+1 && 
						lancamentoList.get(contadorAtual+1).getEntrada().isBefore(padrao.getSaida())) {
					entrou = true;
					Horario lancamentoAtual = lancamentoList.get(contadorAtual);
					alimentaListaExtraOuAtraso(atrasosList, lancamentoAtual.getSaida(), 
							lancamentoList.get(contadorAtual+1).getEntrada());
					
					//4a1- a saida da nova entrada eh anterior a saida padrao atual
					if (lancamentoList.get(contadorAtual+1).getSaida().isBefore(padrao.getSaida())) {
						//4a2- verificar se tem nova entrada e eh anterior a saida padrao
						contadorAtual++;
						i++;
						continue;
					
					//4a3- a saida da nova entrada eh posterior a saida padrao atual
					} else {
						lancamento = lancamentoList.get(i);
						break;
					}
					
				}
				//4b- nao ha nova entrada entao o atraso eh do lancamento de saida ao padrao saida
				if (!entrou) {
					alimentaListaExtraOuAtraso(atrasosList, lancamento.getSaida(), padrao.getSaida());
					padraoCont++;
				}
				
			} else {
				//5- a saida eh posterior ao padrao atual
				boolean entrou = false;
				while (lancamento.getSaida().isAfter(padrao.getSaida())) {
					entrou = true;
					//5a- verificar se temos outro padrao posterior
					if (padraoList.size() > padraoCont+1) {
						//5a1- a saida nao eh posterior a nova entrada
						if (!lancamento.getSaida().isAfter(padraoList.get(padraoCont+1).getEntrada())) {
							alimentaListaExtraOuAtraso(extrasList, padrao.getSaida(), lancamento.getSaida());
							padraoCont++;
							break;
							
						//5a2- saida atual posterior a nova entrada
						} else {
							alimentaListaExtraOuAtraso(extrasList, padrao.getSaida(), 
									padraoList.get(padraoCont+1).getEntrada());
							
							//5a3- valida se a saida eh anterior ao novo padrao
							if (lancamento.getSaida().isBefore(padraoList.get(padraoCont+1).getSaida())) {
								padrao = padraoList.get(padraoCont+1);
								int contadorAtual = i;
								boolean entrouInterno = false;
								//4a verifica se existe uma nova entrada de lancamento antes da saida padrao atual, 
								// se sim, o atraso eh entre a saida atual e a nova entrada
								while (lancamentoList.size() < contadorAtual+1 && 
										lancamentoList.get(contadorAtual+1).getEntrada().isBefore(padrao.getSaida())) {
									entrouInterno = true;
									Horario lancamentoAtual = lancamentoList.get(contadorAtual);
									alimentaListaExtraOuAtraso(atrasosList, lancamentoAtual.getSaida(), 
											lancamentoList.get(contadorAtual+1).getEntrada());
									
									//4a1- a saida da nova entrada eh anterior a saida padrao atual
									if (lancamentoList.get(contadorAtual+1).getSaida().isBefore(padrao.getSaida())) {
										//4a2- verificar se tem nova entrada e eh anterior a saida padrao
										contadorAtual++;
										i++;
										continue;
									
									//4a3- a saida da nova entrada eh posterior a saida padrao atual
									} else {
										lancamento = lancamentoList.get(i);
										break;
									}
									
								}
								//4b- nao ha nova entrada entao o atraso eh do lancamento de saida ao padrao saida
								if (!entrouInterno) {
									alimentaListaExtraOuAtraso(atrasosList, lancamento.getSaida(), padrao.getSaida());
								}
								
							} else {
								padraoCont++;
								padrao = padraoList.get(padraoCont);
								continue;
							}
						}
						
					//5b- sem padrao posterior, lancar extra de saida padrao para lancamento saida
					} else {
						alimentaListaExtraOuAtraso(extrasList, padrao.getSaida(), lancamento.getSaida());
						break;
					}
				}
				if (!entrou) {
					padraoCont++;
				}
			}
		}
		
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
    	for (int i=0; i < entrada.length; i++) {
    		if (!entrada[i].isBlank() && !saida[i].isBlank()) {
    			LocalTime entradaTime = entrada[i] != null ? LocalTime.parse(entrada[i]) : 
    				LocalTime.parse("00:00");
    			
    			// validar se algum periodo padrao ultrapassa o dia atual, se sim e a entrada coincide 
    			// com esse periodo, somar 1 dia a entrada
    			LocalDateTime diaAtual = LocalDateTime.of(LocalDate.now(), LocalTime.parse("23:59"));
    			LocalDateTime entradaDTime = null;
    			for (Horario h : padroes) {
    				LocalTime h1 = LocalTime.parse(FMT.format(h.getEntrada()));
    				LocalTime h2 = LocalTime.parse(FMT.format(h.getSaida()));
    				if (h.getEntrada().isBefore(diaAtual) && //entrada padrao anterior a 23H59
    					h.getSaida().isAfter(diaAtual) && //saida padrao posterior a 23h59
    					!LocalTime.parse(entrada[i]).isAfter(h1) && //lancamento de entrada igual ou anterior a entrada padrao
    					!LocalTime.parse(entrada[i]).isAfter(h2) && //lancamento de entrada igual ou anterior a saida padrao
    					LocalTime.parse(entrada[i]).isBefore(LocalTime.parse("23:59"))) { //lancamento de entrada anterior a 23H59
    					entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
    					diaAtual = diaAtual.plusDays(1);
    				}
    			}
    			if (entradaDTime == null) {
    				entradaDTime = LocalDateTime.of(LocalDate.now(), entradaTime);
    			}
    			
    			LocalTime saidaTime = saida[i] != null ? 
    					LocalTime.parse(saida[i]) : LocalTime.parse("00:00");
    			LocalDateTime saidaDTime = null;
    			if (saidaTime.isBefore(entradaTime) || LocalDate.now().isBefore(diaAtual.toLocalDate())) {
    				saidaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), saidaTime);
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
    					mensagem = "Precisa preencher ao menos uma entrada e saída";
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
    	for (int i=0; i < entradaPadrao.length; i++) {
    		if (!entradaPadrao[i].isBlank() && !saidaPadrao[i].isBlank()) {
	    		LocalTime entradaTime = entradaPadrao[i] != null ? LocalTime.parse(entradaPadrao[i]) : 
	    			LocalTime.parse("00:00");
	    		LocalDateTime entradaDTime = LocalDateTime.of(LocalDate.now(), entradaTime);
	    		
	    		LocalTime saidaTime = saidaPadrao[i] != null ? LocalTime.parse(saidaPadrao[i]) : 
	    			LocalTime.parse("00:00");
	    		LocalDateTime saidaDTime = null;
	    		if (saidaTime.isBefore(entradaTime)) {
	    			saidaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), saidaTime);
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
    					mensagem = "Precisa preencher ao menos uma entrada e saída padrão";
        				throw new HorarioException(mensagem);
    				}
    			}
    			if (entradaPadrao[i].isBlank() && !saidaPadrao[i].isBlank()) {
    				mensagem = "Precisa preencher a entrada padrão";
    				throw new HorarioException(mensagem);
    			}
    			if (!entradaPadrao[i].isBlank() && saidaPadrao[i].isBlank()) {
    				mensagem = "Precisa preencher a saida padrão";
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