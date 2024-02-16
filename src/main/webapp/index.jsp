<html>
	<head>
		<meta charset="utf-8" />
		
		<title>Folha de Ponto - Projeto teste Insight</title>
		
		<link rel="icon" type="image/png" href="resources/img/favicon.png">
		
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
		
		<link href="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/css/bootstrap.min.css" rel="stylesheet" id="bootstrap-css">
		<script src="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/js/bootstrap.min.js"></script>
		
		<link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Roboto+Slab:400,700|Material+Icons" />

		<link rel="stylesheet" href="resources/styles/style.css">
		<link href="https://fonts.googleapis.com/css?family=Kaushan+Script" rel="stylesheet"> 
		
		<script>
			$(document).ready(function(){
				setTimeout(function() {
					$(".alert").fadeOut("slow", function() {
						$(this).alert('close');
					});
				}, 5000);
				
				var contador_padrao = 1;
				var quantidade_padrao = 1;
				var quantidade_lancamento = 1;
				
				$("#btnEnviar").click(function(){
					var contexto = '${pageContext.request.contextPath}';
					
					$.ajax({
						url: contexto+'/ControlarFolhaPonto',
						type: 'POST',
						data: $("#form_padrao").serialize(),
						success: function(data) {
							console.log(data);
							const objectJson = JSON.parse(data);
							if (!objectJson.status){
								mostraDialogo(objectJson.message, "danger", 3000);
							} else {
								var extrasJson = objectJson.extras;
								$("#tbody_extras").find("tr").remove();
								extrasJson.forEach(function(i) {
									var novaLinha = document.createElement('tr');
									novaLinha.insertCell(0).innerHTML = i.entrada;
									novaLinha.insertCell(1).innerHTML = i.saida;
									novaLinha.insertCell(2).innerHTML = i.diferenca;
									$("#table_extras").append(novaLinha);
								});
								
								var atrasosJson = objectJson.atrasos;
								$("#tbody_atrasos").find("tr").remove();
								atrasosJson.forEach(function(i) {
									var novaLinha = document.createElement('tr');
									novaLinha.insertCell(0).innerHTML = i.entrada;
									novaLinha.insertCell(1).innerHTML = i.saida;
									novaLinha.insertCell(2).innerHTML = i.diferenca;
									$("#table_atrasos").append(novaLinha);
								});
								
								mostraDialogo("Processado com sucesso", "success", 3000);
							}
						},
						error: function(reset){
							//console.log(reset);
							mostraDialogo("Erro ao chamar o processamento", "danger", 3000);
						}
					});
				});
				
				$("#btnLimpar").click(function(){
					location.reload(true);
				});
				
				
				$("#add_padrao").click(function(){
					if (quantidade_padrao < 3) {
						contador_padrao++;
						quantidade_padrao++;
						var novo_id = 'tr-'+contador_padrao.toString();
						var nova_class = 'padrao-'+contador_padrao.toString();
						var novaLinha = document.createElement('tr');
						novaLinha.setAttribute('id', novo_id);
						
						novaLinha.insertCell(0).innerHTML = '<input type="time" class="form-control '+nova_class+'" name="entradaPadrao" min="00:00" max="23:59" />';
						novaLinha.insertCell(1).innerHTML = '<input type="time" class="form-control" name="saidaPadrao" min="00:00" max="23:59" />';
						novaLinha.insertCell(2).innerHTML = '<button type="button"><i id="remove-padrao-'+contador_padrao.toString()+'" class="material-icons remover">delete</i></button>';
						
						$("#table_padrao").append(novaLinha);
						
						$("."+nova_class).focus();
					}
				});
				
				$('#table_padrao').on('click', '.remover', function () {
				    $(this).closest('tr').remove();
				    quantidade_padrao--;
				});
				
				
				$("#add_lancamento").click(function(){
					quantidade_lancamento++;
					var novo_id = 'tr-'+quantidade_lancamento.toString();
					var nova_class = 'lanc-'+quantidade_lancamento.toString();
					var novaLinha = document.createElement('tr');
					novaLinha.setAttribute('id', novo_id);
					
					novaLinha.insertCell(0).innerHTML = '<input type="time" class="form-control '+nova_class+'" name="entrada" min="00:00" max="23:59" />';
					novaLinha.insertCell(1).innerHTML = '<input type="time" class="form-control" name="saida" min="00:00" max="23:59" />';
					novaLinha.insertCell(2).innerHTML = '<button type="button"><i id="remove-lanc-'+quantidade_lancamento.toString()+'" class="material-icons remover">delete</i></button>';
					
					$("#table_lancamento").append(novaLinha);
					
					$("."+nova_class).focus();
				});
				
				$('#table_lancamento').on('click', '.remover', function () {
				    $(this).closest('tr').remove();
				});
				
				$("#btnPDF").click(function(){
					var acao = document.getElementById("acao")
					acao.value = 'download';
					var form = $("#form_padrao");
					form.submit();
					acao.value = 'calculo';
				});
				
			});
			
			function mostraDialogo(mensagem, tipo, tempo){
			    if($("#message").is(":visible")){
			        return false;
			    }
			    if(!tempo){
			        var tempo = 3000;
			    }
			    if(!tipo){
			        var tipo = "info";
			    }
			    var cssMessage = "display: block; position: fixed; top: 0; left: 20%; right: 20%; width: 60%; padding-top: 10px; z-index: 9999";
			    var cssInner = "margin: 0 auto; box-shadow: 1px 1px 5px black;";

			    var dialogo = "";
			    dialogo += '<div id="message" style="'+cssMessage+'">';
			    dialogo += '    <div class="alert alert-'+tipo+' alert-dismissable" style="'+cssInner+'">';
			    dialogo += '    <a href="#" class="close" data-dismiss="alert" aria-label="close">×</a>';
			    dialogo +=          mensagem;
			    dialogo += '    </div>';
			    dialogo += '</div>';

			    $("body").append(dialogo);
			    $("#message").hide();
			    $("#message").fadeIn(200);

			    setTimeout(function() {
			        $('#message').fadeOut(300, function(){
			            $(this).remove();
			        });
			    }, tempo); // milliseconds
			}
		</script>
	</head>
	
	<body>
		<div id="ponto">
			<h1 class="text-center text-white pt-5 fonte-titulo">Folha de Ponto - Insight</h1>
			<div class="container">
				<div id="ponto-row" class="row justify-content-center align-items-center">
					<div id="ponto-column" class="col-md-6">
						<div id="ponto-box" class="col-md-12">

							<form id="form_padrao" method="POST" target="_blank" action="ControlarFolhaPonto">
								<h3 class="text-center text-info fonte-titulo titulo_one">Horário Padrão</h3>
								<div class="table-responsive">
									<table class="table" id="table_padrao">
										<thead class="fonte_padrao">
											<th>Entrada</th>
											<th>Saída</th>
											<th><button type="button"><i id="add_padrao" class="material-icons">note_add</i></button></th>
										</thead>
										<tbody>
											<tr>
												<td><input type="time" class="form-control" name="entradaPadrao" required min="00:00" max="23:59" /></td>
												<td><input type="time" class="form-control" name="saidaPadrao" required min="00:00" max="23:59" /></td>
												<td><input type="hidden" id="acao" name="acao" value="calculo" /></td>
											</tr>
										</tbody>
									</table>
								</div>
								
								<h3 class="text-center text-info fonte-titulo">Marcações</h3>
								<div class="table-responsive">
									<table class="table" id="table_lancamento">
										<thead class="fonte_padrao">
											<th>Entrada</th>
											<th>Saída</th>
											<th><button type="button"><i id="add_lancamento" class="material-icons">note_add</i></button></th>
										</thead>
										<tbody>
											<tr>
												<td><input type="time" class="form-control" name="entrada" required min="00:00" max="23:59" /></td>
												<td><input type="time" class="form-control" name="saida" required min="00:00" max="23:59" /></td>
												<td>&nbsp;</td>
											</tr>
										</tbody>
									</table>
								</div>
							</form>
							
							<h3 class="text-center text-info fonte-titulo">Horas Extras</h3>
							<div class="table-responsive">
								<table class="table" id="table_extras">
									<thead class="fonte_padrao">
										<th>Entrada</th>
										<th>Saída</th>
										<th>Extra</th>
									</thead>
									<tbody id="tbody_extras">
										
									</tbody>
								</table>
							</div>
							
							<h3 class="text-center text-info fonte-titulo">Atrasos</h3>
							<div class="table-responsive">
								<table class="table" id="table_atrasos">
									<thead class="fonte_padrao">
										<th>Entrada</th>
										<th>Saída</th>
										<th>Atraso</th>
									</thead>
									<tbody id="tbody_atrasos">
										
									</tbody>
								</table>
							</div>
							
							<div class="table-responsive" id="botoes">
								<button type="button" id="btnEnviar" class="btn btn-info btn-md"><span>Enviar</span></button>
								<button type="button" id="btnLimpar" class="btn btn-info btn-md"><span>Limpar</span></button>
								<button type="button" id="btnPDF" class="btn btn-info btn-md"><span>PDF</span></button>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</body>
</html>