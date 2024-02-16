package br.com.insight.report;

import java.util.List;

public record ReportLancamento(
		List<ReportPeriodo> lancamentos,
		List<String> extras,
		List<String> atrasos
){}