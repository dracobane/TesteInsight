package br.com.insight.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
		boolean fim = false;
		int contadorPadrao = 0;
		int contadorLancamento = 0;
		while (!fim) {
			Horario lancamentoAnterior = contadorLancamento-1 >= 0 && lancamentoList.size() > contadorLancamento-1 ? 
					lancamentoList.get(contadorLancamento-1) : null;
			Horario padraoAnterior = contadorPadrao-1 >= 0 && padraoList.size() > contadorPadrao-1 ? 
					padraoList.get(contadorPadrao-1) : null;
			Horario lancamentoAtual = lancamentoList.size() > contadorLancamento ? 
					lancamentoList.get(contadorLancamento) : null;
			Horario padraoAtual = padraoList.size() > contadorPadrao ? padraoList.get(contadorPadrao) : null;
			Horario lancamentoFuturo = lancamentoList.size() > contadorLancamento+1 ? 
					lancamentoList.get(contadorLancamento+1) : null;
			Horario padraoFuturo = padraoList.size() > contadorPadrao+1 ? padraoList.get(contadorPadrao+1) : null;
			
			// nao ha mais lancamentos e padroes na lista
			if (lancamentoAtual == null && padraoAtual == null) {
				break;
			}
			
			// lancamento de entrada e saida coincidem com a entrada e saida padrao
			if (lancamentoAtual != null && padraoAtual != null && 
				lancamentoAtual.getEntrada().toString().equals(padraoAtual.getEntrada().toString()) && 
				lancamentoAtual.getSaida().toString().equals(padraoAtual.getSaida().toString())) {
				contadorPadrao++;
				contadorLancamento++;
			}
			
			// nao ha mais lancamentos mas ha padroes de entrada e saida
			if (lancamentoAtual == null && padraoAtual != null) {
				// ultimo lancamento de saida excedeu a saida do ultimo padrao e a entrada pertencia ao anterior
				if (padraoAnterior != null && lancamentoAnterior != null && 
					lancamentoAnterior.getEntrada().isBefore(padraoAnterior.getSaida()) && 
					lancamentoAnterior.getSaida().isAfter(padraoAnterior.getSaida())) {
					if (lancamentoAnterior.getSaida().isAfter(padraoAtual.getEntrada())) {
						alimentaListaExtraOuAtraso(extrasList, padraoAnterior.getSaida(), padraoAtual.getEntrada());
					} else {
						alimentaListaExtraOuAtraso(extrasList, padraoAnterior.getSaida(), lancamentoAnterior.getSaida());
						contadorLancamento++;
					}
				}
				// saida do ultimo lancamento ultrapassa a entrada do padrao atual mas termina antes desta saida
				if (lancamentoAnterior != null && lancamentoAnterior.getSaida().isAfter(padraoAtual.getEntrada())) { 
					if (lancamentoAnterior.getSaida().isBefore(padraoAtual.getSaida())) {
						alimentaListaExtraOuAtraso(atrasosList, lancamentoAnterior.getSaida(), padraoAtual.getSaida());
					} else if (padraoFuturo == null) {
						alimentaListaExtraOuAtraso(extrasList, padraoAtual.getSaida(), lancamentoAnterior.getSaida());
					}
				// saida do ultimo lancamento nao invade o novo periodo
				} else {
					alimentaListaExtraOuAtraso(atrasosList, padraoAtual.getEntrada(), padraoAtual.getSaida());
				}
				contadorPadrao++;
				continue;
			}
			
			// ainda ha lancamentos mas nao ha mais padroes
			if (lancamentoAtual != null && padraoAtual == null) {
				if (lancamentoAtual.getEntrada().isBefore(padraoAnterior.getSaida())) {
					alimentaListaExtraOuAtraso(extrasList, padraoAnterior.getSaida(), lancamentoAtual.getSaida());
				} else {
					alimentaListaExtraOuAtraso(extrasList, lancamentoAtual.getEntrada(), lancamentoAtual.getSaida());
				}
				contadorLancamento++;
				continue;
			}
			
			// lancamento de periodo posterior ao padrao atual
			if (lancamentoAtual != null && padraoAtual != null &&
				!lancamentoAtual.getEntrada().isBefore(padraoAtual.getSaida())) {
				alimentaListaExtraOuAtraso(atrasosList, padraoAtual.getEntrada(), padraoAtual.getSaida());
				contadorPadrao++;
				continue;
			}
			
			// lancamento de entrada e saida sao anteriores ao padrao de entrada mas nao coincidem com o padrao anterior
			if (lancamentoAtual != null && padraoAtual != null &&
				lancamentoAtual.getEntrada().isBefore(padraoAtual.getEntrada()) && 
				!lancamentoAtual.getSaida().isAfter(padraoAtual.getEntrada()) && 
				(padraoAnterior != null && !padraoAnterior.getSaida().isBefore(lancamentoAtual.getEntrada()) || 
				padraoAnterior == null)) {
				alimentaListaExtraOuAtraso(extrasList, lancamentoAtual.getEntrada(), lancamentoAtual.getSaida());
				contadorLancamento++;
				continue;
			}
			
			//--- COMECAM AS INTERSECCOES --------------------------------//
			// Entrada atrasada mas dentro do periodo
			if (lancamentoAtual != null && padraoAtual != null &&
				lancamentoAtual.getEntrada().isAfter(padraoAtual.getEntrada()) && 
				lancamentoAtual.getEntrada().isBefore(padraoAtual.getSaida())) {
				
				// existe um lancamento anterior onde terminou no periodo atual
				if (lancamentoAnterior != null && !lancamentoAnterior.getSaida().isBefore(padraoAtual.getEntrada())) {
					alimentaListaExtraOuAtraso(atrasosList, lancamentoAnterior.getSaida(), lancamentoAtual.getEntrada());
				// nao existe ou nao terminou no periodo atual
				} else {
					alimentaListaExtraOuAtraso(atrasosList, padraoAtual.getEntrada(), lancamentoAtual.getEntrada());
				}
				
				// encerrou antes da saida padrao
				if (lancamentoAtual.getSaida().isBefore(padraoAtual.getSaida())) {
					if (lancamentoFuturo != null && !lancamentoFuturo.getEntrada().isAfter(padraoAtual.getSaida())) {
						contadorLancamento++;
						continue;
					} else {
						alimentaListaExtraOuAtraso(atrasosList, lancamentoAtual.getSaida(), padraoAtual.getSaida());
						contadorLancamento++;
						contadorPadrao++;
						continue;
					}
				// encerrou no horario ou posterior
				} else {
					if (padraoFuturo != null && 
						!lancamentoAtual.getSaida().isAfter(padraoFuturo.getEntrada()) ||
						lancamentoAtual.getSaida().isAfter(padraoAtual.getSaida())) {
						alimentaListaExtraOuAtraso(extrasList, padraoAtual.getSaida(), lancamentoAtual.getSaida());
					}
					contadorLancamento++;
					contadorPadrao++;
					continue;
				}
			}
			
			// Entrada correta mas saida dentro do periodo
			if (lancamentoAtual != null && padraoAtual != null &&
				lancamentoAtual.getEntrada().toString().equals(padraoAtual.getEntrada().toString()) && 
				lancamentoAtual.getSaida().isBefore(padraoAtual.getSaida())) {
				
				// nao existem lancamentos futuros ou nao iniciam dentro deste periodo
				if ((lancamentoFuturo != null && !lancamentoFuturo.getEntrada().isBefore(padraoAtual.getSaida())) ||
					lancamentoFuturo == null) {
					alimentaListaExtraOuAtraso(atrasosList, lancamentoAtual.getSaida(), padraoAtual.getSaida());
					contadorPadrao++;
				}
				contadorLancamento++;
				continue;
			}
			
			// entrada anterior e saida no periodo ou posterior ao periodo
			if (lancamentoAtual != null && padraoAtual != null &&
				lancamentoAtual.getEntrada().isBefore(padraoAtual.getEntrada()) && 
				lancamentoAtual.getSaida().isAfter(padraoAtual.getEntrada())) {
				
				// padrao anterior nao existe ou, lancamento da entrada nao antecede a saida anterior
				if (padraoAnterior == null || 
					!padraoAnterior.getSaida().isAfter(lancamentoAtual.getEntrada())) {
					alimentaListaExtraOuAtraso(extrasList, lancamentoAtual.getEntrada(), padraoAtual.getEntrada());
					if (lancamentoAtual.getSaida().isAfter(padraoAtual.getSaida()) && 
						(padraoFuturo == null || !padraoFuturo.getEntrada().isBefore(lancamentoAtual.getSaida()))) {
						alimentaListaExtraOuAtraso(extrasList, padraoAtual.getSaida(), lancamentoAtual.getSaida());
						contadorPadrao++;
					}
					contadorLancamento++;
					continue;
				
				// lancamento da entrada esta presente no periodo anterior
				} else if (padraoAnterior != null && 
						lancamentoAtual.getEntrada().isBefore(padraoAnterior.getSaida())) {
					alimentaListaExtraOuAtraso(extrasList, padraoAnterior.getSaida(), padraoAtual.getEntrada());
					contadorLancamento++;
					continue;
				}
				
			}
			//------------------------------------------------------------//
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
    							(h2f != null && LocalTime.parse(entrada[i]).isBefore(h2f))) && // a entrada nao eh maior que a saida posterior
    							LocalTime.parse(entrada[i]).isBefore(LocalTime.parse(saida[i]))) { 
						entradaDTime = LocalDateTime.of(LocalDate.now().plusDays(1), entradaTime);
						diaPosterior = true;
						break;
						
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