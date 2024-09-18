package com.bdo.myinv.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dao.ReportDao;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.service.outward.InvoiceSalesReportImpl;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InvoiceCommonCustomReportService {
	

	
	private Utility utility;	
	private ResponseUtility respUtil;			
	private ReportDao reportDao;
	private InvoiceSalesReportImpl invoiceSalesReportImpl;	
	private InvoiceCustomSalesReportService invoiceCustomSalesReportService;
	private InvoiceCustomPurchaseReportService invoiceCustomPurchaseReportService;

	public InvoiceCommonCustomReportService(Utility utility, ResponseUtility respUtil, ReportDao reportDao,
			InvoiceSalesReportImpl invoiceSalesReportImpl,InvoiceCustomSalesReportService invoiceCustomSalesReportService,
			InvoiceCustomPurchaseReportService invoiceCustomPurchaseReportService) {
		this.utility = utility;
		this.respUtil = respUtil;		
		this.reportDao = reportDao;
		this.invoiceSalesReportImpl = invoiceSalesReportImpl;	
		this.invoiceCustomSalesReportService = invoiceCustomSalesReportService;
		this.invoiceCustomPurchaseReportService = invoiceCustomPurchaseReportService;
					
	}

	public JSONObject getInvoiceViewCustomReportData(ReportRequest reqData) {

		log.info("Inside getInvoiceViewCustomReportData() of InvoiceCommonCustomReportService.java : START ");
		JSONObject jsonResponse = new JSONObject();	 
		try {			
			if(reqData.getDocDataType() == 1) {
				switch (reqData.getReportName()) {
				case DataConstants.CUSTOMSALESREGISTERREPORT: 
					jsonResponse = invoiceCustomSalesReportService.getCustomSalesReportData(reqData);
					break;

				case DataConstants.CUSTOMSALESREGISTERERRORREPORT: 
					jsonResponse = invoiceCustomSalesReportService.getCustomSalesErrorReportData(reqData);
					break;	
				default:
					log.info("Inside getInvoiceViewCustomReportData() in InvoiceCommonCustomReportService.java deafult case :: Invalid Report Name :: {}", reqData.getReportName());
					break;
				}
			}else {
				switch (reqData.getReportName()) {
				case DataConstants.CUSTOMPURCHASEREGISTERREPORT:									
					jsonResponse = invoiceCustomPurchaseReportService.getCustomPurchaseReportData(reqData);
					break;

				case DataConstants.CUSTOMPURCHASEREGISTERERRORREPORT: 
					jsonResponse = invoiceCustomPurchaseReportService.getCustomPurchaseErrorReportData(reqData);
					break;

				default:
					log.info("Inside getInvoiceViewCustomReportData() in InvoiceCommonCustomReportService.java deafult case :: Invalid Report Name :: {}", reqData.getReportName());
					break;
				}	
			}			
		}catch(Exception e) {
			log.error("Exception occurred in getCustomViewReportData() of InvoiceCommonCustomReportService.java :: Exception {} ", e.toString());
			jsonResponse = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getCustomViewReportData() of InvoiceCommonCustomReportService.java : END ");
		return jsonResponse;
	
	}

	public JSONObject getInvoiceExportCustomReportData(ReportRequest reqData) {

		log.info("Inside getInvoiceExportCustomReportData() of InvoiceCommonCustomReportService.java : START ");
		JSONObject jsonResponse = new JSONObject();	
		try {
			if(reqData.getDocDataType() == 1) {
				switch (reqData.getReportName()) {
				case DataConstants.CUSTOMSALESREGISTERREPORT: 
					jsonResponse = invoiceCustomSalesReportService.getExportCustomSalesReportData(reqData);				
					break;

				case DataConstants.CUSTOMSALESREGISTERERRORREPORT: 
					jsonResponse = invoiceCustomSalesReportService.getExportCustomSalesErrorReportData(reqData);				
					break;
					
				default:
					log.info("Inside getInvoiceExportReportData() in InvoiceReportCommonService.java deafult case :: Invalid Report Name :: {}", reqData.getReportName());
					break;
				}
			}else {
				switch (reqData.getReportName()) {
				case DataConstants.CUSTOMPURCHASEREGISTERREPORT: 
					jsonResponse = invoiceCustomPurchaseReportService.getExportCustomPurchaseReportData(reqData);				
					break;

				case DataConstants.CUSTOMPURCHASEREGISTERERRORREPORT: 
					jsonResponse = invoiceCustomPurchaseReportService.getExportCustomPurchaseErrorReportData(reqData);				
					break;	

				default:
					log.info("Inside getInvoiceExportReportData() in InvoiceReportCommonService.java deafult case :: Invalid Report Name :: {}", reqData.getReportName());
					break;
				}
			}
		}catch(Exception e) {
			log.error("Exception occurred in getInvoiceExportCustomReportData() of InvoiceCommonCustomReportService.java :: Exception {} ", e.toString());
			jsonResponse = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getInvoiceExportCustomReportData() of InvoiceCommonCustomReportService.java : END ");
		return jsonResponse;
	}
	

	public JSONObject scheduledBackgroundCustomReportData(ReportRequest reqData) {
		// TODO Auto-generated method stub
		return null;
	}	




}
