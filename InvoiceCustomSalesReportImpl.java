package com.bdo.myinv.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bdo.myinv.buyer.report.service.ReportsBuyersDownloadService;
import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dto.EinvReportGridDto;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.ReportsCustomColumnsDTO;
import com.bdo.myinv.report.constants.ReportsConstants;
import com.bdo.myinv.utils.CustomTemplateValueMapUtil;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InvoiceCustomSalesReportImpl {
	
	@Value("${einv.report.download.path}")
	private String einvReportDownloadFilePath;

	private ResponseUtility respUtil;
	private Utility utility;
	private CustomReportsColumnService customReportsColumnService;

	public InvoiceCustomSalesReportImpl(ResponseUtility respUtil, Utility utility,
			CustomReportsColumnService customReportsColumnService) {
		this.respUtil = respUtil;
		this.utility = utility;
		this.customReportsColumnService = customReportsColumnService;
	}

	@Autowired
	ReportsBuyersDownloadService reportsBuyersDownloadService;
	
	public JSONObject generateCustomSalesRegisterReport(List<EinvReportGridDto> einvReportDtoObj, ReportRequest reqData,String reportFileName) throws IOException{
		log.info("Inside generateCustomSalesRegisterReport() of InvoiceCustomSalesReportImpl.java : START ");
		
		JSONObject responseJson = new JSONObject();
		String filePath = null;		

		filePath = einvReportDownloadFilePath+reportFileName;
		log.info("Inside generateCustomSalesRegisterReport() of InvoiceCustomSalesReportImpl.java : filepath is ::{} ", filePath);
		
		File f = new File(filePath);		
		try (FileOutputStream fs = new FileOutputStream(f)){			
			Workbook wb = new Workbook(fs, reqData.getReportName()+DataConstants.DETAIL, "1.0");
			String workBookSheet = reqData.getReportName()+DataConstants.DETAIL;
			Worksheet sheet=wb.newWorksheet(workBookSheet);
			if(einvReportDtoObj!=null) {							
				writeCustomSalesRegisterReportExcel(einvReportDtoObj,sheet,reqData);
				sheet.finish();	
				wb.finish();					
			}
			responseJson = utility.createExportReportResponse(f, reportFileName);
			
			responseJson = respUtil.prepareSuccessResponse(responseJson);			
		} catch (Exception e) {
			log.error("Exception occurred in generateCustomSalesRegisterReport() of InvoiceCustomSalesReportImpl.java :: Exception {} ", e.toString());	
			throw e;
		}finally {			
			boolean deleteflag = Files.deleteIfExists(f.toPath());
			log.info("Local file copy deleted succesfully: flag={}", deleteflag);
		}
		log.info("Leaving generateCustomSalesRegisterReport() of InvoiceCustomSalesReportImpl.java : END ");
		return responseJson;
	}
	
	private void writeCustomSalesRegisterReportExcel(List<EinvReportGridDto> einvReportDtoObjList, Worksheet sheet, ReportRequest reqData) {
		log.info("Inside writeCustomSalesRegisterReportExcel() of InvoiceCustomSalesReportImpl.java : START ");

		List<ReportsCustomColumnsDTO> reportsCustomColumnsDTOList = new ArrayList<>(customReportsColumnService.getReportCustomColumn(reqData));
		 
		Set<String> fieldsAlignmentList = new HashSet<>(Arrays.asList("unitPrice", "itemTaxableAmount","subTotal","discountRate","discountAmount","feeOrChargeRate","feeOrChargeAmount","itemExcludingTax","itemTaxableValue","taxRate","taxAmount","itemIncludingTax",
        		 "invoiceAdditionalDiscountAmount","invoiceAdditionalFeeAmount","totalDiscountValue","totalFeeOrChargeAmount","totalNetAmount","totalExcludingTax",
        		 "totalTaxAmount","totalIncludingTax","prePaymentAmount","roundingAmount","totalPayableAmount","docLevelAmountExemptedFromTax","totalPayableAmount"));
         Set<String> fieldsAlignment = fieldsAlignmentList.stream()
                 .map(String::toLowerCase)
                 .collect(Collectors.toSet());
		int startRow = 0;

		startRow = utility.setFirstHeaderCustomTemplate(sheet, reportsCustomColumnsDTOList, startRow,reqData);

		try {
			List<Map<String,Object>> customTemplateValueMapList = CustomTemplateValueMapUtil.customTemplateValueMapping(einvReportDtoObjList);
			
			if (reqData.getReportName().equalsIgnoreCase(DataConstants.CUSTOMSALESREGISTERERRORREPORT)) {
				for(int i = 0; i < customTemplateValueMapList.size(); i++) {
					int columnNo=0;
					sheet.value(startRow, columnNo++, checkNullValue(customTemplateValueMapList.get(i).get(DataConstants.ERROR_CODE)));
					sheet.value(startRow, columnNo++, checkNullValue(customTemplateValueMapList.get(i).get(DataConstants.ERROR_DESCRIPTION)));
					for(ReportsCustomColumnsDTO reportsCustomColumnsDTO : reportsCustomColumnsDTOList ) {
				             sheet.value(startRow,columnNo++ ,checkNullValue(customTemplateValueMapList.get(i).get(reportsCustomColumnsDTO.getField())));
				             if (fieldsAlignment.contains(reportsCustomColumnsDTO.getTitle().toLowerCase())) {
				            	   sheet.style(startRow, columnNo).horizontalAlignment(ReportsConstants.RIGHT_ALIGNMENT).set();
				            	}
					}
					startRow++;
			     }
			}
			else {
				for(int i = 0; i < customTemplateValueMapList.size(); i++) {
					int columnNo=0;
					for(ReportsCustomColumnsDTO reportsCustomColumnsDTO : reportsCustomColumnsDTOList ) {
				             sheet.value(startRow,columnNo++ ,checkNullValue(customTemplateValueMapList.get(i).get(reportsCustomColumnsDTO.getField())));
				            
				             if (fieldsAlignment.contains(reportsCustomColumnsDTO.getTitle().toLowerCase())) {
				            	   sheet.style(startRow, columnNo).horizontalAlignment(ReportsConstants.RIGHT_ALIGNMENT).set();
				            	}
					}
					startRow++;
			     }
			}
		}catch(Exception e) {
			log.error("Exception occurred in writeCustomSalesRegisterReportExcel() of InvoiceCustomSalesReportImpl.java :: Exception {} ", e.toString());	
			throw e;
		}

		log.info("Leaving writeCustomSalesRegisterReportExcel() of InvoiceCustomSalesReportImpl.java : END ");
	}

	
	private String checkNullValue(Object obj) {
		return (obj == null) ? "-" : obj.toString();
	}

}
