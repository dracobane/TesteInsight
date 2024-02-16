package br.com.insight.report;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import br.com.insight.exception.RelatorioException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * @author Rodrigo G de Souza
 */
public class ReportGenerate {
	
	public byte[] generateReport(String realPath, HashMap<String, Object> params, Report report) 
			throws RelatorioException {
		JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(report));
		params.put("SUBREPORT_DIR", "WEB-INF/reports" + File.separator);
		try {
			JasperPrint jasperPrint = JasperFillManager.fillReport(realPath, params, dataSource);
			return JasperExportManager.exportReportToPdf(jasperPrint);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RelatorioException(e.getMessage());
		}
	}
	
}