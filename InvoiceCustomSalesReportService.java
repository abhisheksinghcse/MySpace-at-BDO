package com.bdo.myinv.service;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dao.ReportDao;
import com.bdo.myinv.dto.EinvReportGridDto;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.ReportsCustomColumnsDTO;
import com.bdo.myinv.service.outward.InvoiceSalesReportService;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InvoiceCustomSalesReportService {
	
	private Utility utility;	
	private ResponseUtility respUtil;			
	private ReportDao reportDao;
	private InvoiceSalesReportService invoiceSalesReportService;
	private CustomReportsColumnService customReportsColumnService;
	private InvoiceCustomSalesReportImpl invoiceCustomSalesReportImpl;

	public InvoiceCustomSalesReportService(Utility utility, ResponseUtility respUtil, ReportDao reportDao,InvoiceSalesReportService invoiceSalesReportService,
			CustomReportsColumnService customReportsColumnService,InvoiceCustomSalesReportImpl invoiceCustomSalesReportImpl) {
		this.utility = utility;
		this.respUtil = respUtil;		
		this.reportDao = reportDao;
		this.invoiceSalesReportService = invoiceSalesReportService;
		this.customReportsColumnService = customReportsColumnService;
		this.invoiceCustomSalesReportImpl = invoiceCustomSalesReportImpl;
					
	}

	public JSONObject getCustomSalesReportData(ReportRequest reqData) {

		log.info("Inside getCustomSalesReportData() of InvoiceCustomSalesReportService.java : START ");

		JSONObject dataAndColumn = new JSONObject();
		JSONArray data = new JSONArray();
		JSONArray columnData = new JSONArray();
		List<EinvReportGridDto> einvReportGridDtoObj = new ArrayList<>();	
		List<ReportsCustomColumnsDTO> reportsCustomColumnsDTO = new ArrayList<>();	
		BigInteger totalCount = BigInteger.ZERO;
		Integer totalPages = 0;		

		try {
			if (reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_DETAIL)) {
				einvReportGridDtoObj = invoiceSalesReportService.getSalesRegisterData(reqData);
				reportsCustomColumnsDTO = customReportsColumnService.getReportCustomColumn(reqData);
				data = new JSONArray(einvReportGridDtoObj);
				columnData = new JSONArray(reportsCustomColumnsDTO);
				if (reqData.getReportName().equalsIgnoreCase(DataConstants.SALESREGISTERREPORT)
						|| reqData.getReportName().equalsIgnoreCase(DataConstants.CUSTOMSALESREGISTERREPORT)) {
					if (reqData.getConsolidatedDocument().equalsIgnoreCase("YES")) {
						totalCount = reportDao.getCountOfReportDataConsolidate(reqData);
					} else if (reqData.getConsolidatedDocument().equalsIgnoreCase("ALL")) {
						totalCount = reportDao.getCountOfReportDataUnion(reqData);
					} else if (reqData.getConsolidatedDocument().equalsIgnoreCase("NO")) {
						totalCount = reportDao.getCountOfReportData(reqData);
					}
				}

			}else if(reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_SUMMARY)){									
				einvReportGridDtoObj = invoiceSalesReportService.getSalesRegisterSummaryData(reqData);
				reportsCustomColumnsDTO = customReportsColumnService.getReportCustomColumn(reqData);
				data = new JSONArray(einvReportGridDtoObj);
				columnData = new JSONArray(reportsCustomColumnsDTO);
				if (reqData.getReportName().equalsIgnoreCase(DataConstants.SALESREGISTERREPORT)
						|| reqData.getReportName().equalsIgnoreCase(DataConstants.CUSTOMSALESREGISTERREPORT)) {
					if (reqData.getConsolidatedDocument().equalsIgnoreCase("YES")) {
						totalCount = reportDao.getCountOfReportDataConsolidate(reqData);
					} else if (reqData.getConsolidatedDocument().equalsIgnoreCase("ALL")) {
						totalCount = reportDao.getCountOfReportDataUnion(reqData);
					} else if (reqData.getConsolidatedDocument().equalsIgnoreCase("NO")) {
						totalCount = reportDao.getCountOfReportData(reqData);
					}
				}

			}else {
				log.info("Inside else block getCustomSalesReportData() of InvoiceCustomSalesReportService.java :: {} ", reqData.getReportLevel());
			}
			//total pages for pagination			
			if(totalCount.compareTo(BigInteger.ZERO) > 0) {				
				double totalCountDouble = totalCount.doubleValue();			   
			    totalPages = (int) Math.ceil(totalCountDouble / reqData.getPageSize());
			}
			dataAndColumn.put(DataConstants.DATA, data);
			dataAndColumn.put("ColumnData", columnData);
		}catch(Exception e) {
			log.error("Exception occurred in getCustomSalesReportData() of InvoiceCustomSalesReportService.java :: Exception {} ", e.toString());
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getCustomSalesReportData() of InvoiceCustomSalesReportService.java : END ");
		return respUtil.prepareSuccessGridResponse(dataAndColumn, totalCount, reqData.getPageNo()+1, totalPages);
	
	}
	
	public JSONObject getCustomSalesErrorReportData(ReportRequest reqData) {

		log.info("Inside getCustomSalesErrorReportData() of InvoiceCustomSalesReportService.java : START ");

		JSONObject dataAndColumn = new JSONObject();
		JSONArray data = new JSONArray();
		JSONArray columnData = new JSONArray();
		List<EinvReportGridDto> einvReportGridDtoObj = new ArrayList<>();	
		List<ReportsCustomColumnsDTO> reportsCustomColumnsDTO = new ArrayList<>();	
		BigInteger totalCount = BigInteger.ZERO;
		Integer totalPages = 0;		

		try {
				einvReportGridDtoObj = invoiceSalesReportService.getSalesRegisterErrorData(reqData);
				reportsCustomColumnsDTO = customReportsColumnService.getReportCustomColumn(reqData);
				data = new JSONArray(einvReportGridDtoObj);
				columnData = new JSONArray(reportsCustomColumnsDTO);
				totalCount = reportDao.getCountOfErrorReportData(reqData);

			//total pages for pagination			
			if(totalCount.compareTo(BigInteger.ZERO) > 0) {				
				double totalCountDouble = totalCount.doubleValue();			   
			    totalPages = (int) Math.ceil(totalCountDouble / reqData.getPageSize());
			}
			dataAndColumn.put(DataConstants.DATA, data);
			dataAndColumn.put("ColumnData", columnData);
		}catch(Exception e) {
			log.error("Exception occurred in getCustomSalesErrorReportData() of InvoiceCustomSalesReportService.java :: Exception {} ", e.toString());
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getCustomSalesErrorReportData() of InvoiceCustomSalesReportService.java : END ");
		return respUtil.prepareSuccessGridResponse(dataAndColumn, totalCount, reqData.getPageNo()+1, totalPages);
	
	}
	
	
	public JSONObject getExportCustomSalesReportData(ReportRequest reqData) throws Exception {
		log.info("Inside getExportCustomSalesReportData() of InvoiceCustomSalesReportService.java : START ");

		JSONObject jsonResponse = new JSONObject();	
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		
		if (reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_DETAIL)) {	

			List<EinvReportGridDto> einvReportDtoObj = invoiceSalesReportService.getSalesRegisterData(reqData);
			
			//Checking If size is greater than 10,000
			
		    if(einvReportDtoObj.size()>10000) {
			return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
		    }
		    
			String timestamp = dateFormat.format(new Date()); 
			String reportFileName = reqData.getReportName()+DataConstants.DETAIL+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
			jsonResponse = invoiceCustomSalesReportImpl.generateCustomSalesRegisterReport(einvReportDtoObj, reqData,reportFileName);

		}else if(reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_SUMMARY)){	

			List<EinvReportGridDto> einvReportDtoObj = invoiceSalesReportService.getSalesRegisterSummaryData(reqData);
			
			//Checking If size is greater than 10,000
			
			if(einvReportDtoObj.size() > 10000) {
				return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
			}
		    
			String timestamp = dateFormat.format(new Date()); 
			String reportFileName = reqData.getReportName()+DataConstants.SUMMAY+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
			jsonResponse = invoiceCustomSalesReportImpl.generateCustomSalesRegisterReport(einvReportDtoObj, reqData,reportFileName);

		}else {
			log.info("Inside else condition getExportCustomSalesReportData() of InvoiceCustomSalesReportService.java :: {} ", reqData.getReportLevel());
		}	
		log.info("Leaving getExportCustomSalesReportData() of InvoiceCustomSalesReportService.java : END ");
		return jsonResponse;
	}

	
	public JSONObject getExportCustomSalesErrorReportData(ReportRequest reqData) throws Exception {
		log.info("Inside getExportCustomSalesReportData() of InvoiceCustomSalesReportService.java : START ");

		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		List<EinvReportGridDto> einvReportDtoObj = invoiceSalesReportService.getSalesRegisterErrorData(reqData);
		
		//Checking If size is greater than 10,000
		
	    if(einvReportDtoObj.size()>10000) {
		return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
	    }
	    
		String timestamp = dateFormat.format(new Date()); 
		String reportFileName = reqData.getReportName()+DataConstants.DETAIL+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
		JSONObject jsonResponse = invoiceCustomSalesReportImpl.generateCustomSalesRegisterReport(einvReportDtoObj, reqData,reportFileName);	
		log.info("Leaving getExportCustomSalesReportData() of InvoiceCustomSalesReportService.java : END ");
		return jsonResponse;
	}


}
