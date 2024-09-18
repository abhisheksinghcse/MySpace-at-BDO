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
import com.bdo.myinv.service.inward.InvoicePurchaseReportService;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InvoiceCustomPurchaseReportService {

	private Utility utility;	
	private ResponseUtility respUtil;			
	private ReportDao reportDao;
	private InvoicePurchaseReportService invoicePurchaseReportService;
	private CustomReportsColumnService customReportsColumnService;
	private InvoiceCustomPurchaseReportImpl invoiceCustomPurchaseReportImpl;

	public InvoiceCustomPurchaseReportService(Utility utility, ResponseUtility respUtil, ReportDao reportDao,InvoicePurchaseReportService invoicePurchaseReportService,
			CustomReportsColumnService customReportsColumnService,InvoiceCustomPurchaseReportImpl invoiceCustomPurchaseReportImpl) {
		this.utility = utility;
		this.respUtil = respUtil;		
		this.reportDao = reportDao;
		this.invoicePurchaseReportService = invoicePurchaseReportService;
		this.customReportsColumnService = customReportsColumnService;
		this.invoiceCustomPurchaseReportImpl = invoiceCustomPurchaseReportImpl;
					
	}

	public JSONObject getCustomPurchaseReportData(ReportRequest reqData) {

		log.info("Inside getCustomPurchaseReportData() of InvoiceCustomPurchaseReportService.java : START ");

		JSONObject dataAndColumn = new JSONObject();
		JSONArray data = new JSONArray();
		JSONArray columnData = new JSONArray();
		List<EinvReportGridDto> einvReportGridDtoObj = new ArrayList<>();	
		List<ReportsCustomColumnsDTO> reportsCustomColumnsDTO = new ArrayList<>();	
		BigInteger totalCount = BigInteger.ZERO;
		Integer totalPages = 0;		

		try {
			if (reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_DETAIL)) {
				einvReportGridDtoObj = invoicePurchaseReportService.getPurchaseRegisterData(reqData);
				reportsCustomColumnsDTO = customReportsColumnService.getReportCustomColumn(reqData);
				data = new JSONArray(einvReportGridDtoObj);
				columnData = new JSONArray(reportsCustomColumnsDTO);
				totalCount = reportDao.getCountOfReportData(reqData);

			}else if(reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_SUMMARY)){									
				einvReportGridDtoObj = invoicePurchaseReportService.getPurchaseRegisterSummaryData(reqData);
				reportsCustomColumnsDTO = customReportsColumnService.getReportCustomColumn(reqData);
				data = new JSONArray(einvReportGridDtoObj);
				columnData = new JSONArray(reportsCustomColumnsDTO);
				totalCount = reportDao.getCountOfReportData(reqData);

			}else {
				log.info("Inside else block getCustomPurchaseReportData() of InvoiceCustomPurchaseReportService.java :: {} ", reqData.getReportLevel());
			}
			//total pages for pagination			
			if(totalCount.compareTo(BigInteger.ZERO) > 0) {				
				double totalCountDouble = totalCount.doubleValue();			   
			    totalPages = (int) Math.ceil(totalCountDouble / reqData.getPageSize());
			}
			dataAndColumn.put(DataConstants.DATA, data);
			dataAndColumn.put("ColumnData", columnData);
		}catch(Exception e) {
			log.error("Exception occurred in getCustomPurchaseReportData() of InvoiceCustomPurchaseReportService.java :: Exception {} ", e.toString());
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getCustomPurchaseReportData() of InvoiceCustomPurchaseReportService.java : END ");
		return respUtil.prepareSuccessGridResponse(dataAndColumn, totalCount, reqData.getPageNo()+1, totalPages);
	
	}
	
	public JSONObject getCustomPurchaseErrorReportData(ReportRequest reqData) {

		log.info("Inside getCustomSalesErrorReportData() of InvoiceCustomSalesReportService.java : START ");

		JSONObject dataAndColumn = new JSONObject();
		JSONArray data = new JSONArray();
		JSONArray columnData = new JSONArray();
		List<EinvReportGridDto> einvReportGridDtoObj = new ArrayList<>();	
		List<ReportsCustomColumnsDTO> reportsCustomColumnsDTO = new ArrayList<>();	
		BigInteger totalCount = BigInteger.ZERO;
		Integer totalPages = 0;		

		try {
			einvReportGridDtoObj = invoicePurchaseReportService.getPurchaseRegisterErrorData(reqData);
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
	
	public JSONObject getExportCustomPurchaseReportData(ReportRequest reqData) throws Exception {
		log.info("Inside getExportPurchaseReportData() of InvoicePurchaseReportService.java : START ");

		JSONObject jsonResponse = new JSONObject();	
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		
		if (reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_DETAIL)) {	

			List<EinvReportGridDto> einvReportDtoObj = invoicePurchaseReportService.getPurchaseRegisterData(reqData);
			
			//Checking If size is greater than 10,000
			
		    if(einvReportDtoObj.size()>10000) {
			return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
		    }
		    
			String timestamp = dateFormat.format(new Date()); 
			String reportFileName = reqData.getReportName()+DataConstants.DETAIL+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
			jsonResponse = invoiceCustomPurchaseReportImpl.generateCustomPurchaseRegisterReport(einvReportDtoObj, reqData,reportFileName);

		}else if(reqData.getReportLevel().equalsIgnoreCase(DataConstants.REPORT_LEVEL_SUMMARY)){	

			List<EinvReportGridDto> einvReportDtoObj = invoicePurchaseReportService.getPurchaseRegisterSummaryData(reqData);
			
			//Checking If size is greater than 10,000
			
		    if(einvReportDtoObj.size()>10000) {
			return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
		    }
		    
			String timestamp = dateFormat.format(new Date()); 
			String reportFileName = reqData.getReportName()+DataConstants.SUMMAY+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
			jsonResponse = invoiceCustomPurchaseReportImpl.generateCustomPurchaseRegisterReport(einvReportDtoObj, reqData,reportFileName);

		}else {
			log.info("Inside else condition getExportPurchaseReportData() of InvoicePurchaseReportService.java :: {} ", reqData.getReportLevel());
		}	
		log.info("Leaving getExportPurchaseReportData() of InvoicePurchaseReportService.java : END ");
		return jsonResponse;
	}
	
	public JSONObject getExportCustomPurchaseErrorReportData(ReportRequest reqData) throws Exception {
		log.info("Inside getExportPurchaseReportData() of InvoicePurchaseReportService.java : START ");

		JSONObject jsonResponse = new JSONObject();	
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);

		List<EinvReportGridDto> einvReportDtoObj = invoicePurchaseReportService.getPurchaseRegisterErrorData(reqData);
		
		//Checking If size is greater than 10,000
		
	    if(einvReportDtoObj.size()>10000) {
		return respUtil.prepareErrorResponse(DataConstants.DATA_LIMIT_EXCEED);
	    }
	    
		String timestamp = dateFormat.format(new Date()); 
		String reportFileName = reqData.getReportName()+DataConstants.DETAIL+"_"+timestamp+DataConstants.XLSX_FILE_EXTENSION;
		jsonResponse = invoiceCustomPurchaseReportImpl.generateCustomPurchaseRegisterReport(einvReportDtoObj, reqData,reportFileName);	
		log.info("Leaving getExportPurchaseReportData() of InvoicePurchaseReportService.java : END ");
		return jsonResponse;
	}
	


}
