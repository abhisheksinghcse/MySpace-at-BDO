package com.bdo.myinv.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bdo.myinv.buyer.report.repository.ApiUsagesReportRepository;
import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dto.ApiUsagesDetailsDataDTO;
import com.bdo.myinv.dto.ApiUsagesReportDTO;
import com.bdo.myinv.dto.ApiUsagesSearchDataDTO;
import com.bdo.myinv.dto.ProductUsagesReportDTO;
import com.bdo.myinv.dto.ProductUsagesReportDataDTO;
import com.bdo.myinv.model.PageableDataResponse;
import com.bdo.myinv.utils.DateUtility;
import com.bdo.myinv.utils.ResponseUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApiUsagesReportService {

	@Autowired
	ResponseUtility respUtils;
	@Autowired
	DateUtility dateUtils;

	@Autowired
	private ApiUsagesReportRepository apiUsagesReportRepo;

	public ResponseEntity<Object> getApiUsagesReports(ApiUsagesReportDTO apiUsagesReportDto) {

		log.info(" Inside apiUsagesReport() of ApiUsagesReportService.java :: START ");
		log.info(" Inside apiUsagesReport() of ApiUsagesReportService.java :: request are::{} " +apiUsagesReportDto.toString());
		
		ResponseEntity<Object> responseEntity = null;
		JSONArray jsonArrayData = new JSONArray();
		Date fromDate = dateUtils.convertStringToDate(apiUsagesReportDto.getDateFrom());
		log.info("Inside getApiUsagesReports() fromDate::" +fromDate);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate date = LocalDate.parse(apiUsagesReportDto.getDateTo(), formatter);
	    LocalDate newDate = date.plusDays(1);
	    String nextToDate = newDate.format(formatter);
	    Date toDate = dateUtils.convertStringToDate(nextToDate);
		log.info("Inside getApiUsagesReports() toDate::" +toDate);
		try {

			if (!apiUsagesReportDto.getAction().isEmpty() && apiUsagesReportDto.getIsSummary() != null) {
				switch (apiUsagesReportDto.getAction()) {
				case DataConstants.VIEW:
					if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isSummary)) {
						responseEntity = getViewDataSummaryByEntityId(apiUsagesReportDto, fromDate, toDate);
						log.info("Inside apiUsagesReport() of ApiUsagesReportService.java getting Summary Response are::" +responseEntity);
					} else if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isDetails)) {
						responseEntity = getViewDataDetails(apiUsagesReportDto, fromDate, toDate);
						log.info("Inside apiUsagesReport() of ApiUsagesReportService.java getting Details Response are::" +responseEntity);
					} else {
						log.info("Inside getApiUsagesReports() of else case :: Invalid Request Summary ID !");
						responseEntity = ResponseEntity.badRequest().body("Invalid Request Summary ID");
					}
					break;
				case DataConstants.EXPORT:
					if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isSummary)) {
						responseEntity = generateApiUsagesReport(apiUsagesReportDto, fromDate, toDate);
						log.info("Inside apiUsagesReport() of ApiUsagesReportService.java getting Summary Excel Response are::" +responseEntity);
					} else if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isDetails)) {

						responseEntity = generateApiUsagesReport(apiUsagesReportDto, fromDate, toDate);
						log.info("Inside apiUsagesReport() of ApiUsagesReportService.java getting Details Excel Response are::" +responseEntity);
					} else {
						log.info("Inside getApiUsagesReports() of else case :: Invalid Request Summary ID !");
						responseEntity = ResponseEntity.badRequest().body("Invalid Request Summary ID");
					}

					break;
				default:
					log.info("Inside deafult case :: Invalid Action !");
					responseEntity = ResponseEntity.badRequest().body("Invalid Action");
					break;
				}
			}
		} catch (Exception e) {

			log.error("Exception occured Inside getApiUsagesReports() of ApiUsagesReportService ::: Exception{} ",
					e.toString());
		}

		log.info(" Inside apiUsagesReport() of ApiUsagesReportService.java :: END ");
		return responseEntity;
	}

	// To view Reports for Summary
	private ResponseEntity<Object> getViewDataSummaryByEntityId(ApiUsagesReportDTO apiUsagesReportDto, Date fromDate,
			Date toDate) {

		ResponseEntity<Object> responseEntity = null;

		Pageable page = PageRequest.of(apiUsagesReportDto.getPage(), apiUsagesReportDto.getSize());
		log.info("Inside getViewDataSummaryByEntityId() fromDate::" +fromDate);
		log.info("Inside getViewDataSummaryByEntityId() toDate::" +toDate);

		Page<Object[]> pageOutResponseData = apiUsagesReportRepo.fetchApiSummary(apiUsagesReportDto.getGrpId(),fromDate,toDate, page);
		log.info("Inside getViewDataSummaryByEntityId() Response from Database:::"+pageOutResponseData);

		List<ApiUsagesSearchDataDTO> responseDTOList = pageOutResponseData.stream().map((Object[] object) -> {

			// String groupNameEncrypt = object[0].toString();
			//String groupNameEncrypt = object[0] != null ? object[0].toString() : "";
			//String groupName = decrypt(groupNameEncrypt);

			String businessNameEncrypt = object[0] != null ? object[0].toString() : "";
			String businessName = decrypt(businessNameEncrypt);

			String date = object[1] != null ? object[1].toString() : "-";

			String tradeNameEncrypt = object[2] != null ? object[2].toString() : "";
			String tradeName = decrypt(tradeNameEncrypt);
			String apiName = object[3] != null ? object[3].toString() : "";
			long totalCall = Long.parseLong(object[4] != null ? object[4].toString() : "");
			int successCalls = Integer.parseInt(object[5] != null ? object[5].toString() : "");
			int failedCalls = Integer.parseInt(object[6] != null ? object[6].toString() : "");
			String clientId = object[7] != null ? object[7].toString() : "";
			String groupName = object[8] != null ? object[8].toString() : "";
			return ApiUsagesSearchDataDTO.builder().groupName(groupName).date(date).businessName(businessName)
					.tradeName(tradeName).clientId(clientId).apiName(apiName).totalCalls(totalCall)
					.successCalls(successCalls).failedCalls(failedCalls).build();
		}).collect(Collectors.toList());

		// List<Object[]> searchReportData = pageOutResponseData.getContent();

		int totalPages = pageOutResponseData.getTotalPages();
		Long totalElements = pageOutResponseData.getTotalElements();

		PageableDataResponse pageDataRec = new PageableDataResponse();

		pageDataRec.setTotalPages(totalPages);
		pageDataRec.setTotalElements(totalElements);
		pageDataRec.setResponseData(responseDTOList);
		responseEntity = ResponseEntity.ok(pageDataRec);

		return responseEntity;
	}

	// get ViewDetailsData for Details
	private ResponseEntity<Object> getViewDataDetails(ApiUsagesReportDTO apiUsagesReportDto, Date fromDate,
			Date toDate) {

		ResponseEntity<Object> responseEntity = null;

		Pageable page = PageRequest.of(apiUsagesReportDto.getPage(), apiUsagesReportDto.getSize());
		log.info("Inside getViewDataDetails() fromDate::" +fromDate);
		log.info("Inside getViewDataDetails() toDate::" +toDate);

		Page<Object[]> pageOutResponseData = apiUsagesReportRepo.fetchApiDetails(apiUsagesReportDto.getGrpId(),apiUsagesReportDto.getClientId(),apiUsagesReportDto.getResStatusCode(),apiUsagesReportDto.getReqDate(),apiUsagesReportDto.getApiName(),apiUsagesReportDto.getGroupName(),page);
		 log.info("Inside getViewDataDetails() Response from Database:::" +pageOutResponseData);
		List<ApiUsagesDetailsDataDTO> responseDTOList = pageOutResponseData.stream().map((Object[] object) -> {

			String scheduledID = object[1] != null ? object[1].toString() : "";
			//String groupNameEncrypt = object[2] != null ? object[2].toString() : "";
			//String groupName = decrypt(groupNameEncrypt);
			String businessNameEncrypt = object[2] != null ? object[2].toString() : "";
			String businessName = decrypt(businessNameEncrypt);
			String date = object[3] != null ? object[3].toString() : "-";
			String tradeNameEncrypt = object[4] != null ? object[4].toString() : "";
			String tradeName = decrypt(tradeNameEncrypt);
			String apiName = object[5] != null ? object[5].toString() : "";
			String url = object[6] != null ? object[6].toString() : "";
			String apiStatus = object[7] != null ? object[7].toString() : "";
			String clientId = object[8] != null ? object[8].toString() : "";
			String remark = null;
			if (object[9] == null) {
			    // Handle the case where object[9] is null
			    remark = "-";
			}else if (object[9] instanceof byte[]) {
				// Case 1: object[9] is a byte array
				byte[] remarkBytes = (byte[]) object[9];
				remark = remarkBytes != null && remarkBytes.length > 0 ? convertByteArrayToString(remarkBytes) : "-";
			} else {
				// Handle other cases or throw an exception
				throw new IllegalArgumentException("Unexpected type for object[9]: " + object[9].getClass().getName());
			}
			String groupName = object[10] != null ? object[10].toString() : "";
			return ApiUsagesDetailsDataDTO.builder().scheduledId(scheduledID).groupName(groupName).date(date)
					.businessName(businessName).tradeName(tradeName).clientId(clientId).apiName(apiName).url(url)
					.APIStatus(apiStatus).remark(remark).build();
		}).collect(Collectors.toList());

		// List<Object[]> searchReportData = pageOutResponseData.getContent();

		int totalPages = pageOutResponseData.getTotalPages();
		Long totalElements = pageOutResponseData.getTotalElements();

		PageableDataResponse pageDataRec = new PageableDataResponse();

		pageDataRec.setTotalPages(totalPages);
		pageDataRec.setTotalElements(totalElements);
		pageDataRec.setResponseData(responseDTOList);
		responseEntity = ResponseEntity.ok(pageDataRec);

		return responseEntity;
	}

	// To generate Reports
	public ResponseEntity<Object> generateApiUsagesReport(ApiUsagesReportDTO apiUsagesReportDto, Date fromDate,
			Date toDate) {
		ResponseEntity<Object> responseEntity = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		String timestamp = dateFormat.format(new Date());
		String filename = DataConstants.API_USAGES_REPORT_NAME + "_" + timestamp + DataConstants.XLSX_FILE_EXTENSION;
		Pageable page = PageRequest.of(apiUsagesReportDto.getPage(), apiUsagesReportDto.getSize());
		Page<Object[]> pageOutResponseData = null;
		try {
			byte[] excelByteData = null;
			// generate Reports for Summary
			if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isSummary)) {
				pageOutResponseData = apiUsagesReportRepo.fetchApiSummary(apiUsagesReportDto.getGrpId(),fromDate,toDate, page);
			//	pageOutResponseData = apiUsagesReportRepo.fetchApiSummary(apiUsagesReportDto.getGrpId(), page);
				excelByteData = generateExcelReportsForSummary(pageOutResponseData);
			} else if (apiUsagesReportDto.getIsSummary().equals(DataConstants.isDetails)) { // generate Reports for Details																				
				pageOutResponseData = apiUsagesReportRepo.fetchApiDetails(apiUsagesReportDto.getGrpId(),apiUsagesReportDto.getClientId(),apiUsagesReportDto.getResStatusCode(),apiUsagesReportDto.getReqDate(),apiUsagesReportDto.getApiName(),apiUsagesReportDto.getGroupName(),page);
				excelByteData = generateExcelReportsForDetails(pageOutResponseData);
			}

			/*
			 * HttpHeaders headers = new HttpHeaders();
			 * headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			 * headers.setContentDispositionFormData(filename, filename);
			 * headers.setContentLength(excelByteData.length); return new
			 * ResponseEntity<>(excelByteData, headers,
			 * org.springframework.http.HttpStatus.OK);
			 */

			String base64EncodedData = Base64.getEncoder().encodeToString(excelByteData);

			// int totalPages = pageOutResponseData.getTotalPages();
			// Long totalElements = pageOutResponseData.getTotalElements();

			// PageableDataResponse pageDataRec = new PageableDataResponse();
			// Create a map to hold the response data, message, and status code
			Map<String, Object> responseData = new HashMap<>();

			responseData.put("filename", filename);
			responseData.put("response", base64EncodedData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setContentLength(base64EncodedData.getBytes().length);

			responseEntity = ResponseEntity.ok(responseData);

			return responseEntity;

		} catch (Exception e) {
			log.error("Exception occured while generating Excel", e.toString());
			// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new
			// byte[0]);
		}
		return (ResponseEntity<Object>) ResponseEntity.ok();

	}

	private byte[] generateExcelReportsForSummary(Page<Object[]> pageOutResponseData) throws IOException {

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {

			XSSFSheet sheet = workbook.createSheet("API Usages Report");

			// create Header Row
			XSSFRow headerRow = sheet.createRow(0);
			String[] headers = { "Group Name", "Date", "Business Name", "Trade Name", "Client ID", "API Name",
					"Total Calls", "Success Calls", "Failed Calls" };
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				// Set header cell style
				XSSFCellStyle style = workbook.createCellStyle();
				style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				// Make the Header Font Bold
				XSSFFont font = workbook.createFont();
				font.setBold(true);
				font.setColor(IndexedColors.WHITE.getIndex()); // Set font color to white
				style.setFont(font);
				cell.setCellStyle(style);

				// Center align the header text
				style.setAlignment(HorizontalAlignment.CENTER);
				cell.setCellStyle(style);

				// create data row
				int rowNum = 1;
				for (Object[] object : pageOutResponseData) {

					//String groupNameEncrypt = object[0] != null ? object[0].toString() : "";
					//String groupName = decrypt(groupNameEncrypt);

					String businessNameEncrypt = object[0] != null ? object[0].toString() : "";
					String businessName = decrypt(businessNameEncrypt);
					String date = object[1] != null ? object[1].toString() : "";

					String tradeNameEncrypt = object[2] != null ? object[2].toString() : "";
					String tradeName = decrypt(tradeNameEncrypt);
					String apiName = object[3] != null ? object[3].toString() : "";
					long totalCall = Long.parseLong(object[4] != null ? object[4].toString() : "");
					int successCalls = Integer.parseInt(object[5] != null ? object[5].toString() : "");
					int failedCalls = Integer.parseInt(object[6] != null ? object[6].toString() : "");
					String clientId = object[7] != null ? object[7].toString() : "";
					String groupName = object[8] != null ? object[8].toString() : "";

					XSSFRow row = sheet.createRow(rowNum++);
					row.createCell(0).setCellValue(groupName);
					row.createCell(1).setCellValue(date);
					row.createCell(2).setCellValue(businessName);
					row.createCell(3).setCellValue(tradeName);
					row.createCell(4).setCellValue(clientId);
					row.createCell(5).setCellValue(apiName);
					row.createCell(6).setCellValue(totalCall);
					row.createCell(7).setCellValue(successCalls);
					row.createCell(8).setCellValue(failedCalls);

				}

			}

			// set the columns width
			ResponseUtility.autoSizeAllColumnsWithMinWidth(sheet, 23 * 256);
			// create a ByteArrayOutputStream
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			// write to ByteArrayOutputStream
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}

	}

	private byte[] generateExcelReportsForDetails(Page<Object[]> pageOutResponseData) throws IOException {

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {

			XSSFSheet sheet = workbook.createSheet("API Usages Report");

			// create Header Row
			XSSFRow headerRow = sheet.createRow(0);
			String[] headers = { "Scheduled ID", "Group Name", "Date", "Business Name", "Trade Name", "Client ID",
					"API Name", "URL", "API Status", "Remarks" };
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				// Set header cell style
				XSSFCellStyle style = workbook.createCellStyle();
				style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				// Make the Header Font Bold
				XSSFFont font = workbook.createFont();
				font.setBold(true);
				font.setColor(IndexedColors.WHITE.getIndex()); // Set font color to white
				style.setFont(font);
				cell.setCellStyle(style);

				// Center align the header text
				style.setAlignment(HorizontalAlignment.CENTER);
				cell.setCellStyle(style);

				// create data row
				int rowNum = 1;
				for (Object[] object : pageOutResponseData) {

					String ScheduledID = object[1] != null ? object[1].toString() : "";
					//String groupNameEncrypt = object[2] != null ? object[2].toString() : "";
					//String groupName = decrypt(groupNameEncrypt);
				
					String date = object[3] != null ? object[3].toString() : "";

					String businessNameEncrypt = object[2] != null ? object[2].toString() : "";
					String businessName = decrypt(businessNameEncrypt);

					String tradeNameEncrypt = object[4] != null ? object[4].toString() : "";
					String tradeName = decrypt(tradeNameEncrypt);

					String apiName = object[5] != null ? object[5].toString() : "";

					String Url = object[6] != null ? object[6].toString() : "";

					String ApiStatus = object[7] != null ? object[7].toString() : "";
					String clientId = object[8] != null ? object[8].toString() : "";
					String remark = null;
					if (object[9] == null) {
					    // Handle the case where object[9] is null
					    remark = "-";
					}else if (object[9] instanceof byte[]) {
						// Case 1: object[9] is a byte array
						byte[] remarkBytes = (byte[]) object[9];
						remark = remarkBytes != null && remarkBytes.length > 0 ? convertByteArrayToString(remarkBytes) : "-";
					} else {
						// Handle other cases or throw an exception
						throw new IllegalArgumentException("Unexpected type for object[9]: " + object[9].getClass().getName());
					}
					String groupName = object[10] != null ? object[10].toString() : "";
					XSSFRow row = sheet.createRow(rowNum++);
					/*
					 * for (Integer id : scheduledID) { int cellNum=0;
					 * row.createCell(cellNum++).setCellValue(id); // Increase the cell number for
					 * the next column }
					 */
					row.createCell(0).setCellValue(ScheduledID);
					row.createCell(1).setCellValue(groupName);
					row.createCell(2).setCellValue(date);
					row.createCell(3).setCellValue(businessName);
					row.createCell(4).setCellValue(tradeName);
					row.createCell(5).setCellValue(clientId);
					row.createCell(6).setCellValue(apiName);
					row.createCell(7).setCellValue(Url);
					row.createCell(8).setCellValue(ApiStatus);
					row.createCell(9).setCellValue(remark);

				}
			}

			// set the columns width
			ResponseUtility.autoSizeAllColumnsWithMinWidth(sheet, 23 * 256);
			// create a ByteArrayOutputStream
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			// write to ByteArrayOutputStream
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

   public ResponseEntity<Object> getProductUsagesReport(ProductUsagesReportDTO productUsagesReportDto) {

		log.info(" Inside apiUsagesReport() of ApiUsagesReportService.java :: START ");
		ResponseEntity<Object> responseEntity = null;
		Date fromDate = dateUtils.convertStringToDate(productUsagesReportDto.getDateFrom());
		Date toDate = dateUtils.convertStringToDate(productUsagesReportDto.getDateTo());
		try {

			if (!productUsagesReportDto.getAction().isEmpty()) {
				switch (productUsagesReportDto.getAction()) {

				case DataConstants.VIEW:
					responseEntity = getViewDataProduct(productUsagesReportDto, fromDate, toDate);
					break;
				case DataConstants.EXPORT:
					responseEntity = generateApiUsagesReportProduct(productUsagesReportDto, fromDate, toDate);
					break;
				default:
					log.info("Inside deafult case :: Invalid Action !");
					break;
				}
			}
		} catch (Exception e) {

			log.error("Exception occured Inside getApiUsagesReports() of ApiUsagesReportService ::: Exception{} ",
					e.toString());
		}

		log.info(" Inside apiUsagesReport() of ApiUsagesReportService.java :: END ");
		return responseEntity;
	}

	private ResponseEntity<Object> getViewDataProduct(ProductUsagesReportDTO productUsagesReportDto, Date fromDate,
			Date toDate) {

		ResponseEntity<Object> responseEntity = null;

		try {				
			List<Object[]> pageOutResponseData = apiUsagesReportRepo.getViewDataProduct(
					productUsagesReportDto.getGrpId().toString(), productUsagesReportDto.getDateFrom(),
					productUsagesReportDto.getDateTo(), productUsagesReportDto.getSize(),
					productUsagesReportDto.getDataType(), productUsagesReportDto.getPage());
			System.out.println("1 " +productUsagesReportDto.getGrpId().toString());
			System.out.println("2 " +productUsagesReportDto.getDateFrom());
			System.out.println("3 " +productUsagesReportDto.getDateTo());
			System.out.println("4 " +productUsagesReportDto.getSize());
			System.out.println("5 " +productUsagesReportDto.getDataType());
			System.out.println("6 " +productUsagesReportDto.getPage());
			List<ProductUsagesReportDataDTO> responseDTOList = new ArrayList<>();
			PageableDataResponse pageDataRec = new PageableDataResponse();
			if (!pageOutResponseData.isEmpty()) {
				for (Object[] obj : pageOutResponseData) {

					ProductUsagesReportDataDTO dataDTO = new ProductUsagesReportDataDTO();
					dataDTO.setGroupName(obj[0] != null ? obj[0].toString() : "");
					dataDTO.setBusinessName(obj[1] != null ? obj[1].toString() : "");
					dataDTO.setTradeName(obj[2] != null ? obj[2].toString() : "");
					dataDTO.setMonth(obj[3] != null ? obj[3].toString() : "");
					dataDTO.setNoOfCalls(obj[4] != null ? obj[4].toString() : "");
					dataDTO.setNoOftransUpldData(obj[5] != null ? obj[5].toString() : "");
					dataDTO.setNoOfUinErorr("0");
					dataDTO.setNoOfUinGenerate(obj[6] != null ? obj[6].toString() : "");
					dataDTO.setNoOfUinCancelled(obj[7] != null ? obj[7].toString() : "");
					dataDTO.setNoOfUinRejected("0");

					responseDTOList.add(dataDTO);
				}
			} else {

				pageDataRec.setTotalPages(0);
				pageDataRec.setTotalElements(0);

				pageDataRec.setResponseData(responseDTOList);
				responseEntity = ResponseEntity.ok(pageDataRec);
			}

			int totalPages = (pageOutResponseData.size() + productUsagesReportDto.getSize() - 1)
					/ productUsagesReportDto.getSize();
			int totalElements = pageOutResponseData.size();

			pageDataRec.setTotalPages(totalPages);
			pageDataRec.setTotalElements(totalElements);

			pageDataRec.setResponseData(responseDTOList);
			responseEntity = ResponseEntity.ok(pageDataRec);
		} catch (Exception e) {
			log.info("Inside getViewDataProduct() catch Block ApiUsagesReportService {}", e.toString());
		}

		return responseEntity;
	}

	private ResponseEntity<Object> generateApiUsagesReportProduct(ProductUsagesReportDTO productUsagesReportDto,
			Date fromDate, Date toDate) {

		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		String timestamp = dateFormat.format(new Date());
		String filename = DataConstants.PRODUCT_USAGES_REPORT_NAME + "_" + timestamp
				+ DataConstants.XLSX_FILE_EXTENSION;
		// Pageable page = PageRequest.of(productUsagesReportDto.getPage(),
		// productUsagesReportDto.getSize());	   
		ResponseEntity<Object> responseEntity = null;	
		List<Object[]> pageOutResponseData = apiUsagesReportRepo.getViewDataProduct(
				productUsagesReportDto.getGrpId().toString(), productUsagesReportDto.getDateFrom(),
				productUsagesReportDto.getDateTo(), productUsagesReportDto.getSize(),
				productUsagesReportDto.getDataType(), productUsagesReportDto.getPage());

		try {

			byte[] excelByteData = generateExcelReportsProduct(pageOutResponseData);

			String base64EncodedData = Base64.getEncoder().encodeToString(excelByteData);

			/*
			 * HttpHeaders headers = new HttpHeaders();
			 * headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			 * headers.setContentDispositionFormData(filename, filename);
			 * headers.setContentLength(excelByteData.length);
			 */
			Map<String, Object> responseData = new HashMap<>();

			responseData.put("filename", filename);
			responseData.put("response", base64EncodedData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setContentLength(base64EncodedData.getBytes().length);

			responseEntity = ResponseEntity.ok(responseData);

			return responseEntity;
		} catch (Exception e) {
			log.error("Exception occured while generating Excel", e.toString());
		}
		return (ResponseEntity<Object>) ResponseEntity.ok();

	}

	private byte[] generateExcelReportsProduct(List<Object[]> pageOutResponseData) throws IOException {

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {

			XSSFSheet sheet = workbook.createSheet("Product Usages Report");

			// create Header Row
			XSSFRow headerRow = sheet.createRow(0);
			String[] headers = { "Group Name", "Business Name", "Trade Name", "Month", "No Of Calls",
					"No Of Transaction Uploaded Data", "No of UIN Validation Error", "No of UIN Generated",
					"No of UIN Cancelled", "No of UIN Rejected" };
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				// Set header cell style
				XSSFCellStyle style = workbook.createCellStyle();
				style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				// Make the Header Font Bold
				XSSFFont font = workbook.createFont();
				font.setBold(true);
				font.setColor(IndexedColors.WHITE.getIndex()); // Set font color to white
				style.setFont(font);
				cell.setCellStyle(style);

				// Center align the header text
				style.setAlignment(HorizontalAlignment.CENTER);
				cell.setCellStyle(style);

				// create data row
				int rowNum = 1;
				for (Object[] obj : pageOutResponseData) {

					ProductUsagesReportDataDTO dataDTO = new ProductUsagesReportDataDTO();
					dataDTO.setGroupName(obj[0] != null ? obj[0].toString() : "");
					dataDTO.setBusinessName(obj[1] != null ? obj[1].toString() : "");
					dataDTO.setTradeName(obj[2] != null ? obj[2].toString() : "");
					dataDTO.setMonth(obj[3] != null ? obj[3].toString() : "");
					dataDTO.setNoOfCalls(obj[4] != null ? obj[4].toString() : "");
					dataDTO.setNoOftransUpldData(obj[5] != null ? obj[5].toString() : "");
					dataDTO.setNoOfUinErorr("0");
					dataDTO.setNoOfUinGenerate(obj[6] != null ? obj[6].toString() : "");
					dataDTO.setNoOfUinCancelled(obj[7] != null ? obj[7].toString() : "");
					dataDTO.setNoOfUinRejected("0");

					/**
					 * Setting Values in Excel Sheet By rows
					 */
					XSSFRow row = sheet.createRow(rowNum++);
					row.createCell(0).setCellValue(dataDTO.getGroupName());
					row.createCell(1).setCellValue(dataDTO.getBusinessName());
					row.createCell(2).setCellValue(dataDTO.getTradeName());
					row.createCell(3).setCellValue(dataDTO.getMonth());
					row.createCell(4).setCellValue(dataDTO.getNoOfCalls());
					row.createCell(5).setCellValue(dataDTO.getNoOftransUpldData());
					row.createCell(6).setCellValue(dataDTO.getNoOfUinErorr());
					row.createCell(7).setCellValue(dataDTO.getNoOfUinGenerate());
					row.createCell(8).setCellValue(dataDTO.getNoOfUinCancelled());
					row.createCell(9).setCellValue(dataDTO.getNoOfUinRejected());

				}

			}

			// set the columns width
			ResponseUtility.autoSizeAllColumnsWithMinWidth(sheet, 23 * 256);
			// create a ByteArrayOutputStream
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			// write to ByteArrayOutputStream
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}

	}

	// for decryption
	public static String decrypt(String encryptedText) {
		byte[] decryptedBytes = Base64.getDecoder().decode(encryptedText.getBytes());
		return new String(decryptedBytes);
	}

     // convert BLOB into String
	public static String convertByteArrayToString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Excepton occured Inside convertByteArrayToString() while converting Blob to String", e);
			return null;
		}
	}
}
