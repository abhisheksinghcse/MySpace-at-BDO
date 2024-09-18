package com.bdo.myinv.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dao.CustomReportDao;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.ReportsCustomColumnsDTO;
import com.bdo.myinv.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomReportsColumnService {
	
	private CustomReportDao customReportDao;
	private Utility utility;
	public CustomReportsColumnService(CustomReportDao customReportDao,Utility utility) {
		this.customReportDao = customReportDao;
		this.utility = utility;
	}
	
	public List<ReportsCustomColumnsDTO> getReportCustomColumn(ReportRequest reqData) {
	    log.info("Inside getReportCustomColumn() of CustomReportsColumnService.java : START ");
	    List<ReportsCustomColumnsDTO> reportsCustomColumnList = new ArrayList<>();
	    try {
	       // reportsCustomColumnList 
	        List<Object[]> objList  = customReportDao.getReportCustomColumnData(reqData);
	        reportsCustomColumnList=objList.stream()
	                .map(objData -> {
	                    ReportsCustomColumnsDTO reportsCustomColumnDto = new ReportsCustomColumnsDTO();
	                    reportsCustomColumnDto.setTitle(utility.checkNull((String) objData[0]));
						reportsCustomColumnDto.setField(checkForStageColumms(utility.checkNull((String) objData[1])));
	                    reportsCustomColumnDto.setColumnWidth(200);
	                    reportsCustomColumnDto.setOrderNo((Integer) objData[3]);
	                    reportsCustomColumnDto.setTextClass(objData[4]!=null?(String)objData[4]:"");
	                    return reportsCustomColumnDto;
	                })
	                .collect(Collectors.toList()); 
	        addExtraColumnsForCustom(reportsCustomColumnList);
			log.info("Leaving getReportCustomColumn() of CustomReportDao.java : END with size of reportsCustomColumnList - "+reportsCustomColumnList.size());

	        if(reqData.getAction().equalsIgnoreCase(DataConstants.VIEW) && (reqData.getReportName().equalsIgnoreCase(DataConstants.CUSTOMSALESREGISTERERRORREPORT)
	        		|| reqData.getReportName().equalsIgnoreCase(DataConstants.CUSTOMPURCHASEREGISTERERRORREPORT)))
	        {
		        
		        ReportsCustomColumnsDTO errorCodeDto = new ReportsCustomColumnsDTO();
		        errorCodeDto.setTitle(DataConstants.ERROR_CODE);
		        errorCodeDto.setField(DataConstants.ERROR_CODE_FIELD_NAME);
		        errorCodeDto.setOrderNo(0);
		        reportsCustomColumnList.add(errorCodeDto);
		        
		        ReportsCustomColumnsDTO errorDescDto = new ReportsCustomColumnsDTO();
		        errorDescDto.setTitle(DataConstants.ERROR_DESCRIPTION);
		        errorDescDto.setField(DataConstants.ERROR_DESCRIPTION_FIELD_NAME);
		        errorDescDto.setOrderNo(0);
		        reportsCustomColumnList.add(errorDescDto);
		        
				log.info("Leaving getReportCustomColumn() of CustomReportDao.java : END with size of reportsCustomColumnList - "+reportsCustomColumnList.size());

	        }
	        
	    } catch (Exception e) {
	        log.error("Exception occurred in getReportCustomColumn() of CustomReportsColumnService.java :: Exception {} ", e.toString());
	        throw e;
	    }
	    log.info("Leaving getReportCustomColumn() of CustomReportsColumnService.java : END ");
	    return reportsCustomColumnList;
	}
	
	public String checkForStageColumms(String fieldName) {
		switch (fieldName) {
		case "room": {
            return "udf03";
			
		}
		case "displayName": {

			 return "udf12";
		}
		case "trxDate": {

			 return "udf02";
		}
		case "folioNo": {

			 return "udf01";
		}
		case "supplierTIN": {

			 return "supplierTin";
		}
		case "supplierMSICCode": {

			 return "supplierMsicCode";
		}
		case "eInvoiceType": {

			 return "documentType";
		}
		
		case "folioType": {

			 return "udf14";
		}
		case "billGenerationDate": {

			 return "udf15";
		}
		case "fiscalBillNo": {

			 return "udf16";
		}
		case "trxStatus": {

			 return "udf17";
		}
		case "trxNo": {

			 return "udf18";
		}
		case "trxCode": {

			 return "udf26";
		}
		case "ftDebit": {

			 return "udf19";
		}
		case "ftCredit": {

			 return "udf20";
		}
		case "sumftDebitperbillNo": {

			 return "udf21";
		}
		case "sumftCreditperbillNo": {

			 return "udf22";
		}
		case "originalEinvoiceReferenceNumber": {

			 return "originalEinvReferenceNo";
		}
		case "originalEinvoiceUUID": {

			 return "originalEinvReferenceUuid";
		}
		case "originalEinvoiceReferenceDate": {

			 return "originalEinvReferenceDate";
		}
		case "prePaymentReferenceNumber": {

			 return "prePaymentReferenceNo";
		}
		case "shippingRecipientsRegistrationNumber": {

			 return "shippingRecipientsregistrationNo";
		}
		case "referenceNoOfCustomsFormNos": {

			 return "referenceNoOfCustomsFormnos";
		}
		case "incoTerms": {

			 return "incoterms";
		}
		case "authorisationNoForCertifiedExporter": {

			 return "authorisationnOfOrCertifiedExporter";
		}
		case "referenceNoOfCustomsFormNo2": {

			 return "referenceNoOfCustomsFormno2";
		}
		default:{
			return fieldName;
		}
	}
	}
		
	public void addExtraColumnsForCustom(List<ReportsCustomColumnsDTO> reportsCustomColumnList) {
		if(!reportsCustomColumnList.isEmpty()) {
		LinkedHashMap<String, String> extrColumnsMap = new LinkedHashMap<>();
		extrColumnsMap.put("documentStatusDescrp", "Document Status");
		extrColumnsMap.put("uinNo", "IRBM UIN");
		extrColumnsMap.put("uinDateAndTime", "UIN Generation Date and Time");
		extrColumnsMap.put("uinGeneratedBy", "UIN Generated By");
		extrColumnsMap.put("consolidatedRefNo", "Consolidated Ref No.");
		extrColumnsMap.put("consolidationDateAndTime", "Consolidation date and time");
		extrColumnsMap.put("consolidationPreference", "Consolidation Preference");
		extrColumnsMap.put("consolidationLogicSetting", "Consolidation Logic Setting");
		extrColumnsMap.put("irbmRemark", "IRBM Remark");
		extrColumnsMap.put("cancelDateAndTime", "Cancelled On");
		extrColumnsMap.put("cancelBy", "Cancelled By");
		Integer orderNo = reportsCustomColumnList.get(reportsCustomColumnList.size() - 1).getOrderNo();
		for (Entry<String, String> extrColumns : extrColumnsMap.entrySet()) {
			orderNo++;
			ReportsCustomColumnsDTO reportsCustomColumnDto = new ReportsCustomColumnsDTO();
			reportsCustomColumnDto.setTitle(extrColumns.getValue());
			reportsCustomColumnDto.setField(extrColumns.getKey());
			reportsCustomColumnDto.setColumnWidth(200);
			reportsCustomColumnDto.setOrderNo(orderNo);
			reportsCustomColumnList.add(reportsCustomColumnDto);
		}
	}
	}

}
