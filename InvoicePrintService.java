package com.bdo.myinv.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dto.EinvPrintGridResponseDto;
import com.bdo.myinv.dto.GridRequest;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.request.CustomPrintFileRequestDto;
import com.bdo.myinv.init.MasterModelInitializer;
import com.bdo.myinv.model.AmCustomReportRegisterModel;
import com.bdo.myinv.model.B2CConsolidatedDocumentHeader;
import com.bdo.myinv.model.InwardConsolidatedDocumentHeader;
import com.bdo.myinv.model.TxReportLogsModel;
import com.bdo.myinv.model.inward.EinvInwardBillH;
import com.bdo.myinv.model.inward.EinvIrbmInwardDocumentsUinDetails;
import com.bdo.myinv.model.master.AmEntityMasterModel;
import com.bdo.myinv.model.outward.EinvIrbmOutwardDocumentsUinDetails;
import com.bdo.myinv.model.outward.EinvOutwardBillH;
import com.bdo.myinv.multitenant.master.service.AuthenticationService;
import com.bdo.myinv.repository.AmCustomReportRegisterRepository;
import com.bdo.myinv.repository.B2CConsolidatedDocumentDetailRepo;
import com.bdo.myinv.repository.B2CConsolidatedDocumentHeaderRepo;
import com.bdo.myinv.repository.EinvInwardBillHRepository;
import com.bdo.myinv.repository.EinvOutwardBillHRepository;
import com.bdo.myinv.repository.InwardConsolidatedDocumentDetailRepo;
import com.bdo.myinv.repository.InwardConsolidatedDocumentHeaderRepo;
import com.bdo.myinv.repository.TxReportLogsRepository;
import com.bdo.myinv.repository.master.AmEntityMasterRepository;
import com.bdo.myinv.utils.DateUtility;
import com.bdo.myinv.utils.FileUtility;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * @author SayaleeDukre
 * @author SaptakPatil
 * @since Mar 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePrintService {

	@Value("${einv.jasper.report.jrxml.path}")
	private String einvJrxmlReport;

	@Value("${einv.jasper.qrSalesFilename}")
	private String qrSalesFilename;

	@Value("${einv.jasper.qrPurchaseFilename}")
	private String qrPurchaseFilename;

	@Value("${irbm.api.url}")
	private String irbmQrDomain;

	/**
	 * Master init
	 */
	private final MasterModelInitializer masterModelInitializer;

	/**
	 * Utility Objects
	 */
	// private DataSource dataSource;
	private final DateUtility dateUtils;
	private final ResponseUtility respUtil;
	private final Utility utility;
	private final FileUtility fileUtility;

	/**
	 * Repository
	 */
	private final EinvOutwardBillHRepository einvOutBillHRepository;
	private final EinvInwardBillHRepository einvInwardBillHRepository;
	private final TxReportLogsRepository txReportLogsRepository;
	private final AmCustomReportRegisterRepository amCustomReportRegisterRepository;
	private final AmEntityMasterRepository amEntityMasterRepository;

	/**
	 * Dao
	 */
	private final B2CConsolidatedDocumentHeaderRepo b2CConsolidatedDocumentHeaderRepo;
	private final B2CConsolidatedDocumentDetailRepo b2CConsolidatedDocumentDetailRepo;
	private final InwardConsolidatedDocumentHeaderRepo inwardConsolidatedDocumentHeaderRepo;
	private final InwardConsolidatedDocumentDetailRepo inwardConsolidatedDocumentDetailRepo;

	@Autowired
	private AuthenticationService authenticationService;

	/**
	 * Print Module Grids
	 * 
	 * @param reqData : Request Data
	 * @return Invoices Data for Print
	 * @throws Exception
	 * 
	 * @author SayaleeDukre
	 * @author SaptakPatil
	 * @since Mar 2024
	 */
	public JSONObject getInvoicePrintGridData(GridRequest reqData) throws Exception {
		log.info("Inside getInvoicePrintGridData() of InvoicePrintService.java : START ");
		JSONObject jsonResponse = new JSONObject();
		try {

			Date fromDate = dateUtils.convertStringToDate(reqData.getFromDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);
			Date toDate = dateUtils.convertStringToDate(reqData.getToDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);

			List<String> tinList = new ArrayList<>(reqData.getTinList());

			if (reqData.getTabId() != null) {
				switch (reqData.getTabId()) {
				case 1: // Regular E-invoice Print
					jsonResponse = getRegularInvoiceGrid(tinList, fromDate, toDate, reqData);// get grid data
					break;

				case 2: // Consolidated E-Invoice Print
					switch (reqData.getDocDataType()) {
					// for sales
					case 1:
						jsonResponse = getConsolidatedInvoiceGrid(tinList, fromDate, toDate, reqData);// get grid data
						break;

					// for purchase
					case 2:
						jsonResponse = getSelfConsolidatedInvoiceGrid(tinList, fromDate, toDate, reqData);// get grid
																											// data
						break;
					default:
						log.info("Inside deafult case :: Invalid doc type id");
						break;
					}
					break;

				case 3: // Self-Billed E-invoice Print
					jsonResponse = getSelfBilledInvoiceGrid(tinList, fromDate, toDate, reqData);// get grid data
					break;
				default:
					log.info("Inside deafult case :: Invalid grid tab id");
					break;
				}
			}
		} catch (Exception e) {
			log.error("Exception occurred in getInvoicePrintGridData() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getInvoicePrintGridData() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	private JSONObject getSelfBilledInvoiceGrid(List<String> tinList, Date fromDate, Date toDate, GridRequest reqData)
			throws Exception {
		log.info("Inside getSelfBilledInvoiceGrid() of InvoicePrintService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		try {
			Pageable page = PageRequest.of(reqData.getPageNo(), reqData.getPageSize());
			Page<EinvInwardBillH> pageEinvInwardBillH = null;
			BigInteger totalCount = BigInteger.ZERO;
			if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
				pageEinvInwardBillH = einvInwardBillHRepository
						.findByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceTypeOrderByBillHidDesc(fromDate,
								toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED, reqData.getEinvoiceType(),
								page);
				totalCount = einvInwardBillHRepository
						.countByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceType(fromDate, toDate, tinList,
								DataConstants.STATUS_CODE_UIN_GENERATED, reqData.getEinvoiceType());
			} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty()
					&& reqData.getBranchList().isEmpty()) {
				pageEinvInwardBillH = einvInwardBillHRepository
						.findByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInOrderByBillHidDesc(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), page);
				totalCount = einvInwardBillHRepository
						.countByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeIn(fromDate,
								toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED, reqData.getEinvoiceType(),
								reqData.getDivisionList());
			} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty()
					&& !reqData.getBranchList().isEmpty()) {
				pageEinvInwardBillH = einvInwardBillHRepository
						.findByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInAndBranchCodeInOrderByBillHidDesc(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), reqData.getBranchList(), page);
				totalCount = einvInwardBillHRepository
						.countByDocumentDateBetweenAndBuyerTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInAndBranchCodeIn(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), reqData.getBranchList());
			}
			List<EinvInwardBillH> einvInwardBillHList = pageEinvInwardBillH != null ? pageEinvInwardBillH.getContent()
					: null;
			log.info("Total records gridCount={}, paginationTotalRecordsCount={} ",
					einvInwardBillHList != null ? einvInwardBillHList.size() : 0, totalCount);
			if (einvInwardBillHList == null || einvInwardBillHList.isEmpty()) {
				return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
			}
			List<EinvPrintGridResponseDto> printDtlsRespObj = new ArrayList<>();

			for (EinvInwardBillH einvInwardBillH : einvInwardBillHList) {
				EinvPrintGridResponseDto printGridDto = new EinvPrintGridResponseDto();
				BeanUtils.copyProperties(printGridDto, einvInwardBillH);
				printGridDto.setBillHid(einvInwardBillH.getBillHid());
				printGridDto.setDocumentStatus(einvInwardBillH.getEinvStatus());
				printGridDto.setDocumentStatusDescrp(masterModelInitializer.getEinvPurchaseDocStatusMstList().stream()
						.filter(x -> x.getStatusCode().equals(einvInwardBillH.getEinvStatus())).toList().get(0)
						.getStatusDesc());
				EinvIrbmInwardDocumentsUinDetails irdmOutDocDtls = einvInwardBillH.getEinvIrbmInDocumentsUinDetails();
				if (irdmOutDocDtls != null) {
					printGridDto.setIrbmUIN(!utility.isEmptyString(irdmOutDocDtls.getIrbmDocumentUuid())
							? irdmOutDocDtls.getIrbmDocumentUuid()
							: DataConstants.EMPTY_RESPONSE);
					printGridDto.setDateAndTimeOfValidation(
							dateUtils.convertLocalDateTimeToString(irdmOutDocDtls.getDateTimeValidated()));
				} else {
					printGridDto.setIrbmUIN(DataConstants.EMPTY_RESPONSE);
					printGridDto.setDateAndTimeOfValidation(DataConstants.EMPTY_RESPONSE);
				}
				printGridDto.setDocumentType(masterModelInitializer.getEinvDocTypeMstList().stream()
						.filter(x -> x.getDocTypeCode().equals(einvInwardBillH.getEinvoiceType())).toList().get(0)
						.getDocTypeDescription());
				printGridDto.setDocumentTypeCode(einvInwardBillH.getEinvoiceType());
				printGridDto.setTransactionType(einvInwardBillH.getTransactionType());
				printGridDto.setSupplierTin(einvInwardBillH.getSupplierTin());
				printGridDto.setSupplierName(einvInwardBillH.getSupplierName());
				printGridDto.setBuyerTin(einvInwardBillH.getBuyerTin());
				printGridDto.setBuyerName(einvInwardBillH.getBuyerName());
				printGridDto.setDocumentNumber(einvInwardBillH.getDocumentNo());
				printGridDto.setDocumentDateAndTime(dateUtils.convertDateToString(einvInwardBillH.getDocumentDate())
						+ " " + dateUtils.convertTimeToString(einvInwardBillH.getDocumentTime()));
				printGridDto.setTotalTaxAmount(utility.decimalToCurrencyString(einvInwardBillH.getTotalTaxAmount()));
				printGridDto
						.setTotalIncludingTax(utility.decimalToCurrencyString(einvInwardBillH.getTotalIncludingTax()));
				printGridDto.setSubTotal(utility.decimalToCurrencyString(einvInwardBillH.getItemLevelSubTotal()));
				printDtlsRespObj.add(printGridDto);
			}
			jsonArrayData = new JSONArray(printDtlsRespObj);
			jsonResponse = respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData, totalCount,
					reqData.getPageNo(), reqData.getPageSize());
		} catch (Exception e) {
			log.error("Exception occurred in getSelfBilledInvoiceGrid() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getSelfBilledInvoiceGrid() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	private JSONObject getConsolidatedInvoiceGrid(List<String> tinList, Date fromDate, Date toDate, GridRequest reqData)
			throws Exception {
		log.info("Inside getConsolidatedInvoiceGrid() of InvoicePrintService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		try {
			Pageable page = PageRequest.of(reqData.getPageNo(), reqData.getPageSize());
			Page<Object[]> consolidateDocPage = null;
			Integer status = DataConstants.UINGENERATED;
			consolidateDocPage = getConsolidatedInvoiceGridDataList(fromDate, toDate, tinList, status, reqData, page);
			Long totalCount = consolidateDocPage != null ? consolidateDocPage.getTotalElements() : 0L;

			List<Object[]> consolidateObjectList = Objects.nonNull(consolidateDocPage) ? consolidateDocPage.getContent()
					: new ArrayList<>();

			jsonArrayData = createPrintConsolidateDocumentsGridResponse(consolidateObjectList,
					reqData.getDocDataType());
			jsonResponse = respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData,
					BigInteger.valueOf(totalCount), reqData.getPageNo(), reqData.getPageSize());
		} catch (Exception e) {
			log.error("Exception occurred in getConsolidatedInvoiceGrid() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getConsolidatedInvoiceGrid() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	private JSONObject getSelfConsolidatedInvoiceGrid(List<String> tinList, Date fromDate, Date toDate,
			GridRequest reqData) throws Exception {
		log.info("Inside getSelfConsolidatedInvoiceGrid() of InvoicePrintService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		try {
			Pageable page = PageRequest.of(reqData.getPageNo(), reqData.getPageSize());
			Page<Object[]> consolidateDocPage = null;
			Integer status = DataConstants.UINGENERATED;
			consolidateDocPage = getConsolidatedInvoiceGridDataList(fromDate, toDate, tinList, status, reqData, page);
			Long totalCount = consolidateDocPage != null ? consolidateDocPage.getTotalElements() : 0L;

			List<Object[]> consolidateObjectList = Objects.nonNull(consolidateDocPage) ? consolidateDocPage.getContent()
					: new ArrayList<>();

			jsonArrayData = createPrintConsolidateDocumentsGridResponse(consolidateObjectList,
					reqData.getDocDataType());
			jsonResponse = respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData,
					BigInteger.valueOf(totalCount), reqData.getPageNo(), reqData.getPageSize());
		} catch (Exception e) {
			log.error(
					"Exception occurred in getSelfConsolidatedInvoiceGrid() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getSelfConsolidatedInvoiceGrid() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	public JSONArray createPrintConsolidateDocumentsGridResponse(List<Object[]> objectList, Integer docDataType) {
		log.info("createPrintConsolidateDocumentsGridResponse :: START");
		try {
			return new JSONArray(createPrintConsolidateDocumentsGridResponseList(objectList, docDataType));
		} catch (Exception e) {
			log.error("createPrintConsolidateDocumentsGridResponse >> Exception :: {}", e);
			throw e;
		}

	}

	private List<EinvPrintGridResponseDto> createPrintConsolidateDocumentsGridResponseList(List<Object[]> objectList,
			Integer docDataType) {
		log.info("createPrintConsolidateDocumentsGridResponseList :: START");
		List<EinvPrintGridResponseDto> einvPrintGridResponseList = new ArrayList<>();
		try {
			if (objectList == null || objectList.isEmpty()) {
				log.info("createPrintConsolidateDocumentsGridResponseList :: objectList is empty");
				return Collections.emptyList();
			}
			if (DataConstants.EINV_DOC_DATA_TYPE_SALES.equals(docDataType)) {
				for (Object[] object : objectList) {
					// for sales
					B2CConsolidatedDocumentHeader b2CConsolidatedDocument = (B2CConsolidatedDocumentHeader) object[0];
					String generatedUIN = object[1] != null ? object[1].toString() : DataConstants.EMPTY_STRING;
					String generatedOn = DateUtility.convertLocalDateTimeToString((LocalDateTime) object[3],
							DataConstants.TIMESTAMP_TYPE_DD_MM_YYYY_HH_MM_SS);
					EinvPrintGridResponseDto printGridDto = new EinvPrintGridResponseDto();
					printGridDto.setBillHid(b2CConsolidatedDocument.getId());
					printGridDto.setConsolidatedDocHeaderId(b2CConsolidatedDocument.getId());
					printGridDto.setDocumentStatus(DataConstants.STATUS_CODE_UIN_GENERATED);
					printGridDto.setDocumentStatusDescrp("UIN Generated");
					printGridDto.setIrbmUIN(generatedUIN);
					printGridDto.setConsolidatedRefNo(b2CConsolidatedDocument.getDocumentNo());
					printGridDto.setDocumentType(masterModelInitializer.getEinvDocTypeMstList().stream()
							.filter(x -> x.getDocTypeCode().equals(b2CConsolidatedDocument.getEinvoiceType())).toList()
							.get(0).getDocTypeDescription());
					printGridDto.setDocumentTypeCode(b2CConsolidatedDocument.getEinvoiceType());
					printGridDto.setTransactionType(b2CConsolidatedDocument.getTransactionType());
					printGridDto.setSupplierTin(b2CConsolidatedDocument.getSupplierTin());
					printGridDto.setSupplierName(b2CConsolidatedDocument.getSupplierName());
					printGridDto.setTotalTaxAmount(
							utility.decimalToCurrencyString(b2CConsolidatedDocument.getTotalTaxAmount()));
					printGridDto.setTotalIncludingTax(
							utility.decimalToCurrencyString(b2CConsolidatedDocument.getTotalIncludingTax()));
					printGridDto.setDateAndTimeOfValidation(generatedOn);
					printGridDto.setSubTotal(utility.decimalToCurrencyString(
							b2CConsolidatedDocumentDetailRepo.sumSubTotalByHeaderId(b2CConsolidatedDocument.getId())));
					einvPrintGridResponseList.add(printGridDto);
				}
			} else {
				// for purchase
				for (Object[] object : objectList) {
					InwardConsolidatedDocumentHeader inwardConsolidatedDocument = (InwardConsolidatedDocumentHeader) object[0];
					String generatedUIN = object[1] != null ? object[1].toString() : DataConstants.EMPTY_STRING;
					String generatedOn = DateUtility.convertLocalDateTimeToString((LocalDateTime) object[3],
							DataConstants.TIMESTAMP_TYPE_DD_MM_YYYY_HH_MM_SS);
					EinvPrintGridResponseDto printGridDto = new EinvPrintGridResponseDto();
					printGridDto.setBillHid(inwardConsolidatedDocument.getId());
					printGridDto.setConsolidatedDocHeaderId(inwardConsolidatedDocument.getId());
					printGridDto.setDocumentStatus(DataConstants.STATUS_CODE_UIN_GENERATED);
					printGridDto.setDocumentStatusDescrp("UIN Generated");
					printGridDto.setIrbmUIN(generatedUIN);
					printGridDto.setConsolidatedRefNo(inwardConsolidatedDocument.getDocumentNo());
					printGridDto.setDocumentType(masterModelInitializer.getEinvDocTypeMstList().stream()
							.filter(x -> x.getDocTypeCode().equals(inwardConsolidatedDocument.getEinvoiceType()))
							.toList().get(0).getDocTypeDescription());
					printGridDto.setDocumentTypeCode(inwardConsolidatedDocument.getEinvoiceType());
					printGridDto.setTransactionType(inwardConsolidatedDocument.getTransactionType());
					printGridDto.setBuyerTin(inwardConsolidatedDocument.getBuyerTin());
					printGridDto.setBuyerName(inwardConsolidatedDocument.getBuyerName());
					printGridDto.setTotalTaxAmount(
							utility.decimalToCurrencyString(inwardConsolidatedDocument.getTotalTaxAmount()));
					printGridDto.setTotalIncludingTax(
							utility.decimalToCurrencyString(inwardConsolidatedDocument.getTotalIncludingTax()));
					printGridDto.setDateAndTimeOfValidation(generatedOn);
					printGridDto.setSubTotal(utility.decimalToCurrencyString(inwardConsolidatedDocumentDetailRepo
							.sumSubTotalByHeaderId(inwardConsolidatedDocument.getId())));
					einvPrintGridResponseList.add(printGridDto);
				}
			}

		} catch (Exception e) {
			log.error("createPrintConsolidateDocumentsGridResponseList >> Exception :: {}", e);
			throw e;
		}
		log.info("createPrintConsolidateDocumentsGridResponseList :: END");
		return einvPrintGridResponseList;
	}

	private Page<Object[]> getConsolidatedInvoiceGridDataList(Date fromDate, Date toDate, List<String> tinList,
			Integer status, GridRequest gridRequest, Pageable page) {
		if (gridRequest.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_SALES)) {
			if (gridRequest.getDivisionList().isEmpty() && gridRequest.getBranchList().isEmpty()) {

				return b2CConsolidatedDocumentHeaderRepo.findBySupplierTinInOrderByIdDesc(fromDate, toDate, tinList,
						status, gridRequest.getEinvoiceType(), page);

			} else if (tinList.size() == 1 && !gridRequest.getDivisionList().isEmpty()
					&& gridRequest.getBranchList().isEmpty()) {

				return b2CConsolidatedDocumentHeaderRepo.findBySupplierTinInAndDivisionCodeInOrderByIdDesc(fromDate,
						toDate, tinList, status, gridRequest.getEinvoiceType(), gridRequest.getDivisionList(), page);

			} else if (tinList.size() == 1 && !gridRequest.getDivisionList().isEmpty()
					&& !gridRequest.getBranchList().isEmpty()) {

				return b2CConsolidatedDocumentHeaderRepo
						.findBySupplierTinInAndDivisionCodeInAndBranchCodeInOrderByIdDesc(fromDate, toDate, tinList,
								status, gridRequest.getEinvoiceType(), gridRequest.getDivisionList(),
								gridRequest.getBranchList(), page);
			} else {
				return null;
			}
		} else if (gridRequest.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_PURCHASE_SELF_INV)) {
			if (gridRequest.getDivisionList().isEmpty() && gridRequest.getBranchList().isEmpty()) {

				return inwardConsolidatedDocumentHeaderRepo.findByBuyerTinInOrderByIdDesc(fromDate, toDate, tinList,
						status, gridRequest.getEinvoiceType(), page);

			} else if (tinList.size() == 1 && !gridRequest.getDivisionList().isEmpty()
					&& gridRequest.getBranchList().isEmpty()) {

				return inwardConsolidatedDocumentHeaderRepo.findByBuyerTinInAndDivisionCodeInOrderByIdDesc(fromDate,
						toDate, tinList, status, gridRequest.getEinvoiceType(), gridRequest.getDivisionList(), page);

			} else if (tinList.size() == 1 && !gridRequest.getDivisionList().isEmpty()
					&& !gridRequest.getBranchList().isEmpty()) {

				return inwardConsolidatedDocumentHeaderRepo
						.findByBuyerTinInAndDivisionCodeInAndBranchCodeInOrderByIdDesc(fromDate, toDate, tinList,
								status, gridRequest.getEinvoiceType(), gridRequest.getDivisionList(),
								gridRequest.getBranchList(), page);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private JSONObject getRegularInvoiceGrid(List<String> tinList, Date fromDate, Date toDate, GridRequest reqData)
			throws Exception {
		log.info("Inside getRegularInvoiceGrid() of InvoicePrintService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		try {
			Pageable page = PageRequest.of(reqData.getPageNo(), reqData.getPageSize());
			Page<EinvOutwardBillH> pageEinvOutwardBillH = null;
			BigInteger totalCount = BigInteger.ZERO;

			if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
				pageEinvOutwardBillH = einvOutBillHRepository
						.findByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceTypeOrderByBillHidDesc(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), page);
				totalCount = einvOutBillHRepository
						.countByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceType(fromDate, toDate,
								tinList, DataConstants.STATUS_CODE_UIN_GENERATED, reqData.getEinvoiceType());
			} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty()
					&& reqData.getBranchList().isEmpty()) {
				pageEinvOutwardBillH = einvOutBillHRepository
						.findByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInOrderByBillHidDesc(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), page);
				totalCount = einvOutBillHRepository
						.countByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeIn(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList());
			} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty()
					&& !reqData.getBranchList().isEmpty()) {
				pageEinvOutwardBillH = einvOutBillHRepository
						.findByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInAndBranchCodeInOrderByBillHidDesc(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), reqData.getBranchList(), page);
				totalCount = einvOutBillHRepository
						.countByDocumentDateBetweenAndSupplierTinInAndEinvStatusAndEinvoiceTypeAndDivisionCodeInAndBranchCodeIn(
								fromDate, toDate, tinList, DataConstants.STATUS_CODE_UIN_GENERATED,
								reqData.getEinvoiceType(), reqData.getDivisionList(), reqData.getBranchList());
			}
			List<EinvOutwardBillH> einvOutwardBillHList = pageEinvOutwardBillH != null
					? pageEinvOutwardBillH.getContent()
					: null;
			log.info("Total records gridCount={}, paginationTotalRecordsCount={} ",
					einvOutwardBillHList != null ? einvOutwardBillHList.size() : 0, totalCount);
			if (einvOutwardBillHList == null || einvOutwardBillHList.isEmpty()) {
				return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
			}
			List<EinvPrintGridResponseDto> printDtlsRespObj = new ArrayList<>();

			for (EinvOutwardBillH einvOutwardBillH : einvOutwardBillHList) {
				EinvPrintGridResponseDto printGridDto = new EinvPrintGridResponseDto();
				BeanUtils.copyProperties(printGridDto, einvOutwardBillH);
				printGridDto.setBillHid(einvOutwardBillH.getBillHid());
				printGridDto.setDocumentStatus(einvOutwardBillH.getEinvStatus());
				printGridDto.setDocumentStatusDescrp(masterModelInitializer.getEinvSalesDocStatusMstList().stream()
						.filter(x -> x.getStatusCode().equals(einvOutwardBillH.getEinvStatus())).toList().get(0)
						.getStatusDesc());
				EinvIrbmOutwardDocumentsUinDetails irdmOutDocDtls = einvOutwardBillH
						.getEinvIrbmOutwardDocumentsUinDetails();
				if (irdmOutDocDtls != null) {
					printGridDto.setIrbmUIN(!utility.isEmptyString(irdmOutDocDtls.getIrbmDocumentUuid())
							? irdmOutDocDtls.getIrbmDocumentUuid()
							: DataConstants.EMPTY_RESPONSE);
					printGridDto.setDateAndTimeOfValidation(
							dateUtils.convertLocalDateTimeToString(irdmOutDocDtls.getDateTimeValidated()));
				} else {
					printGridDto.setIrbmUIN(DataConstants.EMPTY_RESPONSE);
					printGridDto.setDateAndTimeOfValidation(DataConstants.EMPTY_RESPONSE);
				}
				printGridDto.setDocumentType(masterModelInitializer.getEinvDocTypeMstList().stream()
						.filter(x -> x.getDocTypeCode().equals(einvOutwardBillH.getEinvoiceType())).toList().get(0)
						.getDocTypeDescription());
				printGridDto.setDocumentTypeCode(einvOutwardBillH.getEinvoiceType());
				printGridDto.setTransactionType(einvOutwardBillH.getTransactionType());
				printGridDto.setSupplierTin(einvOutwardBillH.getSupplierTin());
				printGridDto.setSupplierName(einvOutwardBillH.getSupplierName());
				printGridDto.setBuyerTin(einvOutwardBillH.getBuyerTin());
				printGridDto.setBuyerName(einvOutwardBillH.getBuyerName());
				printGridDto.setDocumentNumber(einvOutwardBillH.getDocumentNo());
				String doctime = dateUtils.convertTimeToString(einvOutwardBillH.getDocumentTime()) != null
						? dateUtils.convertTimeToString(einvOutwardBillH.getDocumentTime())
						: "";
				printGridDto.setDocumentDateAndTime(
						dateUtils.convertDateToString(einvOutwardBillH.getDocumentDate()) + " " + doctime);
				printGridDto.setTotalTaxAmount(utility.decimalToCurrencyString(einvOutwardBillH.getTotalTaxAmount()));
				printGridDto
						.setTotalIncludingTax(utility.decimalToCurrencyString(einvOutwardBillH.getTotalIncludingTax()));
				printGridDto.setSubTotal(utility.decimalToCurrencyString(einvOutwardBillH.getItemLevelSubTotal()));
				printDtlsRespObj.add(printGridDto);
			}
			jsonArrayData = new JSONArray(printDtlsRespObj);

			jsonResponse = respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData, totalCount,
					reqData.getPageNo(), reqData.getPageSize());
		} catch (Exception e) {
			log.error("Exception occurred in getRegularInvoiceGrid() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getRegularInvoiceGrid() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	public JSONObject insertInTxReportLogs(ReportRequest reqData) {
		log.info("Inside insertInTxReportLogs() of InvoicePrintService.java : START ");
		JSONObject responseJson = new JSONObject();
		TxReportLogsModel txReportLog = new TxReportLogsModel();
		try {
			txReportLog.setTin(String.join(",", reqData.getTinList()));
			txReportLog.setDateRange(reqData.getFromDate().concat(" - ").concat(reqData.getToDate()));
			txReportLog.setReportName("Bulk Invoice Print");
			txReportLog.setPrintTemplateId(reqData.getPrintTemplateId());
			txReportLog.setTotalCount(String.valueOf(reqData.getBillHidList().size()));
			txReportLog.setReportType(DataConstants.SCHEDULE_DOWNLOAD_REPORT_TYPE); // 9 for BackGround Download report
			txReportLog.setFileName("");
			txReportLog.setEinvBillHid(String.join(",", reqData.getBillHidList()));
			txReportLog.setJobStatus(DataConstants.SCHEDULE_JOB_STATUS_PENDING); // 0 for pending
			txReportLog.setActionStartStamp(new Date());
			txReportLog.setProcStartStamp(new Date());
			txReportLog.setCreatedOn(new Date());
			txReportLog.setQueryEndStamp(new Date());
			txReportLog.setLastActionOn(new Date());
			txReportLog.setUserId(reqData.getUserId());
			txReportLog.setEntityId(reqData.getGroupEntityId());
			log.info("saving data in tx_reports_log table");
			txReportLogsRepository.save(txReportLog);
			Long txLogId = txReportLog.getTxLogId();
			log.info("Leaving insertInTxReportLogs() tx_log_id is :: ", txLogId.toString());
			JSONObject jobj = new JSONObject();
			jobj.put(DataConstants.SCHEDULE_ID, txLogId);
			responseJson = respUtil.prepareSuccessResponse(jobj);
		} catch (Exception e) {
			log.error("Exception occurred in insertInTxReportLogs() of InvoicePrintService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving insertInTxReportLogs() of InvoicePrintService.java : END ");
		return responseJson;
	}

	/**
	 * Upload JRXML and Logo File for Custom Print
	 * 
	 * @param inputJasperFile
	 * @param logoInputFile
	 * @param reqData
	 * @return
	 * 
	 * @author SaptakPatil
	 * @param signatureInputFile
	 * @throws IOException
	 * @since 2024
	 */
	public JSONObject insertCustomReport(MultipartFile inputJasperFile, MultipartFile logoInputFile,
			MultipartFile signatureInputFile, CustomPrintFileRequestDto reqData) throws IOException {
		log.info("Inside insertCustomReport() of  InvoicePrintService.java : start ...  ");
		JSONObject responseJson = new JSONObject();
		try {
			if (inputJasperFile == null || inputJasperFile.isEmpty()) {
				log.info("Empty Jasper file received : filesize={}",
						(inputJasperFile != null ? inputJasperFile.getSize() : null));
				return respUtil.prepareErrorResponse("JRXML File is empty. Please Try again!");
			}

			StringBuilder filePath = new StringBuilder();
			filePath.append(einvJrxmlReport);// base jasper files path
			if (reqData.getIsCustom().equals(DataConstants.CUSTOM_PRINT)) {// custom
				filePath.append(DataConstants.CUSTOM_FOLDER + DataConstants.FILE_DELIMITER);
				filePath.append(reqData.getEntityId() + DataConstants.FILE_DELIMITER);
				log.info("Custom Template...");
			} else {// default
				log.info("Default Template...");
			}

			/* Jasper .jrxml File */
			File newJasperfile = new File(filePath.toString() + inputJasperFile.getOriginalFilename());
			transferFileToFilePath(inputJasperFile, newJasperfile);
			log.info("Jasper file uploaded... :: filename='{}' ", inputJasperFile.getOriginalFilename());

			/* Logo image File */
			if (logoInputFile != null && !logoInputFile.isEmpty()) {
				File newLogofile = new File(filePath.toString() + logoInputFile.getOriginalFilename());
				transferFileToFilePath(logoInputFile, newLogofile);
				log.info("Logo file uploaded... :: filename='{}' ", logoInputFile.getOriginalFilename());
			}

			/* Signature image File */
			if (signatureInputFile != null && !signatureInputFile.isEmpty()) {
				File newSignaturefile = new File(filePath.toString() + signatureInputFile.getOriginalFilename());
				transferFileToFilePath(signatureInputFile, newSignaturefile);
				log.info("Signature file uploaded... :: filename='{}' ", signatureInputFile.getOriginalFilename());
			}

			responseJson = respUtil.prepareSuccessResponse(DataConstants.FILE_UPLOADED_SUCCESSFULLY);
		} catch (IllegalStateException e) {
			log.error(
					"Error occurred during insert data in InvoicePrintService.java :: IllegalStateException '{}', {} ",
					e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			log.error("Error occurred during insert data in InvoicePrintService.java  :: Exception '{}', {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving insertCustomReport() of  InvoicePrintService.java : END ");
		return responseJson;
	}

	private void transferFileToFilePath(MultipartFile file, File newFile) throws IOException {
		try {
			newFile.getParentFile().mkdirs();

			file.transferTo(newFile);
			log.info("File transfer to the Azure Report Service file Location successfully. :: fileName='{}'",
					newFile.getName());
		} catch (IllegalStateException e) {
			log.error(
					"Failed to transfer file to new Jasperfile path location in transferFileToJasperPath() of InvoicePrintService.java :: IllegalStateException '{}', {} ",
					e.getMessage(), e);
			throw e;
		} catch (IOException e) {
			log.error(
					"Failed to transfer file to new Jasperfile path location in transferFileToJasperPath() of InvoicePrintService.java :: IOException '{}', {} ",
					e.getMessage(), e);
			throw e;
		}
	}

	public JSONObject getListofCustomreportTemplates(ReportRequest reqData) {
		log.info("Inside getListofCustomreportTemplates() of  InvoicePrintService.java : start ...  ");
		JSONObject responseJson = new JSONObject();
		JSONArray responceJsonArray = new JSONArray();
		try {
			String tin = Base64.getEncoder().encodeToString(reqData.getTin().getBytes());

			AmEntityMasterModel entityModel = amEntityMasterRepository.findByTin(tin);
			Long entityId = entityModel.getEntityId();
			Integer parentEntityId = entityModel.getParentId();

			List<AmCustomReportRegisterModel> customTemplateReportList = amCustomReportRegisterRepository
					.getListOfAmCustomReportRegisterModel(reqData.getDocType(), reqData.getDocDataType(), entityId,
							parentEntityId, reqData.getIsConsolidate());

			if (!customTemplateReportList.isEmpty()) {

				for (AmCustomReportRegisterModel customReportModel : customTemplateReportList) {
					JSONObject jsonObj = new JSONObject();
					jsonObj.put(DataConstants.TEMPLATE_ID, customReportModel.getReportId());
					jsonObj.put(DataConstants.TEMPLATE_NAME, customReportModel.getReportName());
					responceJsonArray.put(jsonObj);
				}
				responseJson = respUtil.prepareSuccessResponse(responceJsonArray);
			} else {
				responseJson = respUtil.prepareErrorResponse("No Template Found");
			}
		} catch (Exception e) {
			log.error("Error occurred during getListofCustomreportTemplates data in InvoicePrintService.java :: {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving getListofCustomreportTemplates() of  InvoicePrintService.java : END ");
		return responseJson;
	}

	public JSONObject printInvoiceCustomReport(ReportRequest reqData, String accountId)
			throws JRException, SQLException {
		log.info("Inside printInvoiceCustomReport() of  InvoicePrintService.java : start ...  ");
		JSONObject responseJson = new JSONObject();
		StringBuilder base64String = new StringBuilder();
		JasperPrint jasperPrint = null;
		Map<String, Object> reportParameter = new HashMap<>();
		DataSource dataSource = authenticationService.getDataSourceOfTenant(accountId);
		if (Objects.nonNull(dataSource)) {
			try (Connection dbConnection = dataSource.getConnection()) {
				ArrayList<String> billHidArr = (ArrayList<String>) reqData.getBillHidList();
				Integer printTemplateId = reqData.getPrintTemplateId();
				reportParameter.put(DataConstants.PARAM_BILL_HID, billHidArr);
				reportParameter.put(DataConstants.IRBM_QR_DOMAIN, irbmQrDomain);
				reportParameter.put(DataConstants.PRINTTEMPLATEID, printTemplateId);
				AmCustomReportRegisterModel customReportData = getReportName(printTemplateId);
				log.info("printInvoiceCustomReport() of InvoiceReportService.java :  compile jasperReport ...");
				JasperReport jasperReport = JasperCompileManager
						.compileReport(customReportData.getPathUrl().concat(customReportData.getReportFileName()));
				log.info("printInvoiceCustomReport() of InvoiceReportService.java : report parameter :: {}",
						new JSONObject(reportParameter).toString());
				jasperPrint = JasperFillManager.fillReport(jasperReport, reportParameter, dbConnection);
				log.info("printInvoiceCustomReport() of InvoiceReportService.java : fillReport() for report id is : {}",
						printTemplateId);
				log.info("printInvoiceCustomReport() of InvoiceReportService.java :  out");
				byte[] fileByte = JasperExportManager.exportReportToPdf(jasperPrint);
				base64String.append(Base64.getEncoder().encodeToString(fileByte));
				responseJson = respUtil.prepareSuccessResponse(base64String.toString());
			} catch (Exception e) {
				log.error("Error occurred during printInvoiceCustomReport data in InvoicePrintService.java :: {} ",
						e.getMessage(), e);
				throw e;
			}
		}
		log.info("Leaving printInvoiceCustomReport() of InvoicePrintService.java : End ");
		return responseJson;

	}

	private AmCustomReportRegisterModel getReportName(Integer printTemplateId) {
		log.info("Inside getReportName() of InvoicePrintService.java :: START");
		return amCustomReportRegisterRepository.findByReportId(printTemplateId);
	}

	public JSONObject invoiceCustomReportPrintForSftp(ReportRequest reqData, String accountId) throws Exception {
		log.info("Inside invoiceCustomReportPrintForSftp() of  InvoicePrintService.java : start ...  ");
		JSONObject response = new JSONObject();
		try {
			if ((DataConstants.DOCTYPEOUTWARD.equals(reqData.getDocDataType())
					|| DataConstants.DOCTYPEINWARD.equals(reqData.getDocDataType()))
					&& (!reqData.getBatchNo().isEmpty() || !reqData.getBatchNo().isBlank())) {
				// Integer printTemplateId = reqData.getPrintTemplateId();
				// AmCustomReportRegisterModel customReportData =
				// getReportName(printTemplateId);
				// log.info("Inside invoiceCustomReportPrintForSftp() of
				// InvoicePrintService.java : Template ID: {}: for batch no: {}:",
				// printTemplateId,reqData.getBatchNo());
				// log.info(
				// "Inside invoiceCustomReportPrintForSftp() of InvoicePrintService.java :
				// Compiling Jasper report ...");
				// JasperReport jasperReport = JasperCompileManager
				// .compileReport(customReportData.getPathUrl().concat(customReportData.getReportFileName()));
				List<BigInteger> billHidArr = reqData.getBillHidList().stream().map(BigInteger::new).toList();
				if (DataConstants.DOCTYPEOUTWARD.equals(reqData.getDocDataType())
						|| DataConstants.DOCTYPEINWARD.equals(reqData.getDocDataType())) {
					response = printService(billHidArr, reqData.getBatchNo(), reqData.getDocDataType(),
							reqData.getPrintTemplateId(), accountId, reqData.getIsConsolidate());
				} else {
					log.info(
							"Inside invoiceCustomReportPrintForSftp() of InvoicePrintService.java : Invalid document type or print template ID: {} ",
							DataConstants.ERROR_RESPONSE_MSG_FOR_DOC_TYPE_AND_PRINT_TEMPLATE_ID);
					response = respUtil
							.prepareErrorResponse(DataConstants.ERROR_RESPONSE_MSG_FOR_DOC_TYPE_AND_PRINT_TEMPLATE_ID);
				}

			} else {
				log.info(
						"Inside invoiceCustomReportPrintForSftp() of InvoicePrintService.java : Invalid document type or batch number: {} ",
						DataConstants.ERROR_RESPONSE_MSG_FOR_DOC_TYPE_AND_BATCH_NO);
				response = respUtil.prepareErrorResponse(DataConstants.ERROR_RESPONSE_MSG_FOR_DOC_TYPE_AND_BATCH_NO);
			}
		} catch (Exception e) {
			log.error("Error occurred during invoiceCustomReportPrintForSftp() data in InvoicePrintService.java :: {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving invoiceCustomReportPrintForSftp() of InvoicePrintService.java : End ");
		return response;
	}

	public JSONObject printService(List<BigInteger> billHIdArray, String batchNo, Integer docDataType,
			Integer defaultPrintTemplateId, String accountId, Integer isConsolidate) throws Exception {
		log.info("Inside printService() of  InvoicePrintService.java : start ..." + billHIdArray + "::batchNo::"
				+ batchNo + "::docDataType::" + "" + docDataType + "::defaultPrintTemplateId::" + defaultPrintTemplateId
				+ "::accountId::" + accountId);
		JSONObject responseJson = new JSONObject();
		JasperPrint jasperPrint = null;
		List<JSONObject> jasperPrintObject = new ArrayList<>();
		DataSource dataSource = authenticationService.getDataSourceOfTenant(accountId);
		if (Objects.nonNull(dataSource)) {
			try (Connection dbConnection = dataSource.getConnection()) {
				responseJson.put(DataConstants.BATCH_NUMBER, batchNo);
				for (BigInteger billHId : billHIdArray) {
					Integer printTemplateId = null;
					if (isConsolidate != null && isConsolidate == 1) {
						if (DataConstants.DOCTYPEOUTWARD.equals(docDataType)) {
							printTemplateId = amCustomReportRegisterRepository
									.getCustomTemplateForConsolidateOutward(billHId);
						} else if (DataConstants.DOCTYPEINWARD.equals(docDataType)) {
							printTemplateId = amCustomReportRegisterRepository
									.getCustomTemplateForConsolidateInward(billHId);
						}

					} else if (DataConstants.DOCTYPEOUTWARD.equals(docDataType)) {
						printTemplateId = amCustomReportRegisterRepository.getCustomTemplateForOutward(billHId);
					} else if (DataConstants.DOCTYPEINWARD.equals(docDataType)) {
						printTemplateId = amCustomReportRegisterRepository.getCustomTemplateForInward(billHId);
					}

					if (Objects.isNull(printTemplateId) && isConsolidate != 1) {
						log.info("Inside printService() for billHid ID :::" + billHId + "::print templateid is null");
						printTemplateId = defaultPrintTemplateId;
					}

					log.info("Inside printService() for billHidID :::" + billHId + "::batchNo::" + batchNo
							+ "::printTemplateId::" + printTemplateId);
					if (printTemplateId != null) {

						AmCustomReportRegisterModel customReportData = getReportName(printTemplateId);

						log.info("Inside printService() for billHidID :::" + billHId + "::customReportData::"
								+ customReportData);

						JasperReport jasperReport = JasperCompileManager.compileReport(
								customReportData.getPathUrl().concat(customReportData.getReportFileName()));

						JSONObject jsonData = new JSONObject();
						Map<String, Object> reportParameter = new HashMap<>();
						reportParameter.put(DataConstants.PARAM_BILL_HID, Arrays.asList(billHId));
						reportParameter.put(DataConstants.IRBM_QR_DOMAIN, irbmQrDomain);
						reportParameter.put(DataConstants.PRINTTEMPLATEID, printTemplateId);
						log.info("printInvoiceCustomReport() of InvoiceReportService.java : report parameter :: {}",
								new JSONObject(reportParameter).toString());
						jasperPrint = JasperFillManager.fillReport(jasperReport, reportParameter, dbConnection);
						byte[] fileByte = JasperExportManager.exportReportToPdf(jasperPrint);
//					log.info(
//							"Inside printService() of InvoicePrintService.java : Print process is completed for the bill Hid {} and fileByte is {}",
//							billHId,fileByte);
						jsonData.put(DataConstants.BILLHID, billHId);
						if (isConsolidate != null && isConsolidate == 1) {
							if (docDataType.equals(DataConstants.DOCTYPEOUTWARD)) {
								jsonData.put(DataConstants.DOCNO,
										b2CConsolidatedDocumentHeaderRepo.findById(billHId).getDocumentNo());
							} else {
								jsonData.put(DataConstants.DOCNO,
										inwardConsolidatedDocumentHeaderRepo.findById(billHId).getDocumentNo());
							}

						} else if (docDataType.equals(DataConstants.DOCTYPEOUTWARD)) {
							jsonData.put(DataConstants.DOCNO,
									einvOutBillHRepository.findByBillHid(billHId).getDocumentNo());
						} else {
							jsonData.put(DataConstants.DOCNO,
									einvInwardBillHRepository.findByBillHid(billHId).getDocumentNo());
						}
						jsonData.put(DataConstants.PRINTBYTEARRAY, Base64.getEncoder().encodeToString(fileByte));
						jasperPrintObject.add(jsonData);
					}
				}
				responseJson.put(DataConstants.PDFOBJECT, jasperPrintObject);
				responseJson = respUtil.prepareSuccessResponse(responseJson);

			} catch (Exception e) {
				log.error("Error occurred during printService() data in InvoicePrintService.java :: {} ",
						e.getMessage(), e);
				throw e;
			}
		}
		log.info("Leaving printService() of InvoicePrintService.java : End  " );
		return responseJson;
	}

	public JSONObject getSellerPurchaseTinListForQrPrint(GridRequest reqData) {
		log.info("Inside getSellerPurchaseTinListForQrPrint() of InvoicePrintService.java : START ");
		JSONObject jsonResponse = new JSONObject();
		try {
			Date fromDate = dateUtils.convertStringToDate(reqData.getFromDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);
			Date toDate = dateUtils.convertStringToDate(reqData.getToDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);
			List<String> tinList = new ArrayList<>(reqData.getTinList());
			if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_SALES)) {
				jsonResponse = getBuyerTinListForSales(reqData, tinList, fromDate, toDate);
			} else if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_PURCHASE_SELF_INV)) {
				jsonResponse = getSellerTinListForPurchase(reqData, tinList, fromDate, toDate);
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in getSellerPurchaseTinListForQrPrint() of InvoiceReportController.java :: Exception {} ",
					e.getMessage(), e);
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getSellerPurchaseTinListForQrPrint() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	private JSONObject getSellerTinListForPurchase(GridRequest reqData, List<String> tinList, Date fromDate,
			Date toDate) {
		log.info("Inside getSellerTinListForPurchase() of InvoicePrintService.java : START ");
		List<Object[]> einvInwardBillHList = new ArrayList<>();
		if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceType(
					fromDate, toDate, tinList, reqData.getEinvoiceType());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository
					.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceTypeAndDivisionCodeIn(fromDate, toDate, tinList,
							reqData.getEinvoiceType(), reqData.getDivisionList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && !reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository
					.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceTypeAndDivisionCodeInAndBranchCodeIn(fromDate,
							toDate, tinList, reqData.getEinvoiceType(), reqData.getDivisionList(),
							reqData.getBranchList());
		}
		log.info("Leaving getSellerTinListForPurchase() of InvoicePrintService.java : END ");
		return getSellerTinListForPurchase(einvInwardBillHList);
	}

	private JSONObject getSellerTinListForPurchase(List<Object[]> einvInwardBillHList) {
		log.info("Inside getSellerTinListForPurchase() of InvoicePrintService.java : START ");
		List<Map<String, Object>> supplierTinAndBusinessNameList = new ArrayList<>();
		if (einvInwardBillHList == null || einvInwardBillHList.isEmpty()) {
			return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
		} else {
			einvInwardBillHList.forEach(einvInwardBillH -> {
				Map<String, Object> supplierTinAndBusinessName = new HashMap<>();
				supplierTinAndBusinessName.put(DataConstants.TIN, utility.checkNull((String) einvInwardBillH[0]));
				supplierTinAndBusinessName.put(DataConstants.BUSINESS_NAME,
						utility.checkNull((String) einvInwardBillH[1]));
				supplierTinAndBusinessNameList.add(supplierTinAndBusinessName);
			});
		}
		log.info("Leaving getSellerTinListForPurchase() of InvoicePrintService.java : END ");
		return respUtil.prepareSuccessResponse(supplierTinAndBusinessNameList);
	}

	private JSONObject getBuyerTinListForSales(GridRequest reqData, List<String> tinList, Date fromDate, Date toDate) {
		log.info("Inside getBuyerTinListForSales() of InvoicePrintService.java : START ");
		List<Object[]> einvOutwardBillHList = new ArrayList<>();
		if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceType(
					fromDate, toDate, tinList, reqData.getEinvoiceType());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository
					.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceTypeAndDivisionCodeIn(fromDate, toDate,
							tinList, reqData.getEinvoiceType(), reqData.getDivisionList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && !reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository
					.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceTypeAndDivisionCodeInAndBranchCodeIn(fromDate,
							toDate, tinList, reqData.getEinvoiceType(), reqData.getDivisionList(),
							reqData.getBranchList());
		}
		log.info("Leaving getBuyerTinListForSales() of InvoicePrintService.java : END ");
		return getBuyerTinListForSales(einvOutwardBillHList);
	}

	private JSONObject getBuyerTinListForSales(List<Object[]> einvOutwardBillHList) {
		log.info("Inside getBuyerTinListForSales() of InvoicePrintService.java : START ");
		List<Map<String, Object>> buyerTinAndBusinessNameList = new ArrayList<>();
		if (einvOutwardBillHList == null || einvOutwardBillHList.isEmpty()) {
			return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
		} else {
			einvOutwardBillHList.forEach(einvOutwardbillH -> {
				Map<String, Object> buyerTinAndBusinessName = new HashMap<>();
				buyerTinAndBusinessName.put(DataConstants.TIN, utility.checkNull((String) einvOutwardbillH[0]));
				buyerTinAndBusinessName.put(DataConstants.BUSINESS_NAME,
						utility.checkNull((String) einvOutwardbillH[1]));
				buyerTinAndBusinessNameList.add(buyerTinAndBusinessName);
			});
		}
		log.info("Leaving getBuyerTinListForSales() of InvoicePrintService.java : END ");
		return respUtil.prepareSuccessResponse(buyerTinAndBusinessNameList);
	}

	public JSONObject getInvoiceListForQrPrint(GridRequest reqData) {
		log.info("Inside getInvoiceListForQrPrint() of InvoicePrintService.java : START ");
		JSONObject jsonResponse = new JSONObject();
		try {
			Date fromDate = dateUtils.convertStringToDate(reqData.getFromDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);
			Date toDate = dateUtils.convertStringToDate(reqData.getToDate(), DataConstants.DATE_TYPE_DD_MM_YYYY);
			List<String> tinList = new ArrayList<>(reqData.getTinList());
			if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_SALES)) {
				jsonResponse = getSalesInvoiceList(reqData, tinList, fromDate, toDate);
			} else if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_PURCHASE_SELF_INV)) {
				jsonResponse = getPurchaseInvoiceList(reqData, tinList, fromDate, toDate);
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in getSellerPurchaseTinListForQrPrint() of InvoiceReportController.java :: Exception {} ",
					e.getMessage(), e);
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getInvoiceListForQrPrint() of InvoicePrintService.java : END ");
		return jsonResponse;
	}

	private JSONObject getPurchaseInvoiceList(GridRequest reqData, List<String> tinList, Date fromDate, Date toDate) {
		log.info("Inside getPurchaseInvoiceList() of InvoicePrintService.java : START ");
		List<Object[]> einvInwardBillHList = new ArrayList<>();
		if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository
					.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceTypeAndSupplierTinIn(fromDate, toDate, tinList,
							reqData.getEinvoiceType(), reqData.getSellerPurchasetinList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository
					.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceTypeAndSupplierTinAndDivisionCodeIn(fromDate,
							toDate, tinList, reqData.getEinvoiceType(), reqData.getSellerPurchasetinList(),
							reqData.getDivisionList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && !reqData.getBranchList().isEmpty()) {
			einvInwardBillHList = einvInwardBillHRepository
					.findByDocumentDateBetweenAndBuyerTinInAndEinvoiceTypeAndSupplierTinInDivisionCodeInAndBranchCodeIn(
							fromDate, toDate, tinList, reqData.getEinvoiceType(), reqData.getSellerPurchasetinList(),
							reqData.getDivisionList(), reqData.getBranchList());
		}
		List<Map<String, Object>> billHidAndDocNoList = new ArrayList<>();
		if (einvInwardBillHList == null || einvInwardBillHList.isEmpty()) {
			return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
		} else {
			einvInwardBillHList.forEach(einvInwardbillH -> {
				Map<String, Object> billHidAndDocNo = new HashMap<>();
				billHidAndDocNo.put(DataConstants.BILLHID, einvInwardbillH[0]);
				billHidAndDocNo.put(DataConstants.INVOICE_NO, utility.checkNull((String) einvInwardbillH[1]));
				billHidAndDocNoList.add(billHidAndDocNo);
			});
		}
		log.info("Leaving getPurchaseInvoiceList() of InvoicePrintService.java : END ");
		return respUtil.prepareSuccessResponse(billHidAndDocNoList);
	}

	private JSONObject getSalesInvoiceList(GridRequest reqData, List<String> tinList, Date fromDate, Date toDate) {
		log.info("Inside getSalesInvoiceList() of InvoicePrintService.java : START ");
		List<Object[]> einvOutwardBillHList = new ArrayList<>();
		if (reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository
					.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceTypeAndBuyerTinIn(fromDate, toDate, tinList,
							reqData.getEinvoiceType(), reqData.getSellerPurchasetinList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository
					.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceTypeAndBuyerTinAndDivisionCodeIn(fromDate,
							toDate, tinList, reqData.getEinvoiceType(), reqData.getSellerPurchasetinList(),
							reqData.getDivisionList());
		} else if (tinList.size() == 1 && !reqData.getDivisionList().isEmpty() && !reqData.getBranchList().isEmpty()) {
			einvOutwardBillHList = einvOutBillHRepository
					.findByDocumentDateBetweenAndSupplierTinInAndEinvoiceTypeAndBuyerTinInDivisionCodeInAndBranchCodeIn(
							fromDate, toDate, tinList, reqData.getEinvoiceType(), reqData.getSellerPurchasetinList(),
							reqData.getDivisionList(), reqData.getBranchList());
		}
		List<Map<String, Object>> billHidAndDocNoList = new ArrayList<>();
		if (einvOutwardBillHList == null || einvOutwardBillHList.isEmpty()) {
			return respUtil.prepareErrorResponse(DataConstants.NO_RECORDS_FOUND);
		} else {
			einvOutwardBillHList.forEach(einvOutwardbillH -> {
				Map<String, Object> billHidAndDocNo = new HashMap<>();
				billHidAndDocNo.put(DataConstants.BILLHID, einvOutwardbillH[0]);
				billHidAndDocNo.put(DataConstants.INVOICE_NO, utility.checkNull((String) einvOutwardbillH[1]));
				billHidAndDocNoList.add(billHidAndDocNo);
			});
		}
		log.info("Leaving getSalesInvoiceList() of InvoicePrintService.java : END ");
		return respUtil.prepareSuccessResponse(billHidAndDocNoList);
	}

	public JSONObject getQrCodePrint(ReportRequest reqData, String accountId) throws JRException, SQLException {
		log.info("Inside getQrCodePrint() of  InvoicePrintService.java : start ...  ");
		JSONObject responseJson = new JSONObject();
		StringBuilder base64String = new StringBuilder();
		JasperPrint jasperPrint = null;
		Map<String, Object> reportParameter = new HashMap<>();
		DataSource dataSource = authenticationService.getDataSourceOfTenant(accountId);
		if (Objects.nonNull(dataSource)) {
			try (Connection dbConnection = dataSource.getConnection()) {
				ArrayList<String> billHidArr = (ArrayList<String>) reqData.getBillHidList();
				reportParameter.put(DataConstants.PARAM_BILL_HID, billHidArr);
				reportParameter.put(DataConstants.IRBM_QR_DOMAIN, irbmQrDomain);
				log.info("getQrCodePrint() of InvoiceReportService.java : BillHid are : {}", billHidArr.toString());
				log.info("getQrCodePrint() of InvoiceReportService.java :  compile jasperReport ...");
				JasperReport jasperReport = null;
				if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_SALES)) {
					String qrPrintSaleFilePathName = einvJrxmlReport + qrSalesFilename;
					jasperReport = JasperCompileManager.compileReport(qrPrintSaleFilePathName);
				} else if (reqData.getDocDataType().equals(DataConstants.EINV_DOC_DATA_TYPE_PURCHASE_SELF_INV)) {
					String qrPrintPurchaseFilePathName = einvJrxmlReport + qrPurchaseFilename;
					jasperReport = JasperCompileManager.compileReport(qrPrintPurchaseFilePathName);
				}
				jasperPrint = JasperFillManager.fillReport(jasperReport, reportParameter, dbConnection);
				log.info("getQrCodePrint() of InvoiceReportService.java :  out");
				byte[] fileByte = JasperExportManager.exportReportToPdf(jasperPrint);
				base64String.append(Base64.getEncoder().encodeToString(fileByte));
				log.info("getQrCodePrint() of InvoiceReportService.java : base64 response string length : {}",
						base64String.length());
				responseJson = respUtil.prepareSuccessResponse(base64String.toString());
			} catch (Exception e) {
				log.error("Error occurred during getQrCodePrint data in InvoicePrintService.java :: {} ",
						e.getMessage(), e);
				throw e;
			}
		}
		log.info("Leaving getQrCodePrint() of InvoicePrintService.java : End ");
		return responseJson;
	}

	/**
	 * Download JRXML File for Custom Print
	 * 
	 * @param reqData
	 * @return
	 * 
	 * @author SaptakPatil
	 * @throws IOException
	 * @since July 2024
	 */
	public JSONObject downloadCustomReport(CustomPrintFileRequestDto reqData) throws IOException {
		log.info("Inside downloadCustomReport() of InvoicePrintService.java : START ");
		JSONObject responseJson = new JSONObject();
		try {
			// base path
			StringBuilder filePath = new StringBuilder();
			filePath.append(einvJrxmlReport + DataConstants.CUSTOM_FOLDER + DataConstants.FILE_DELIMITER);

			Integer reportId = reqData.getReportId();
			if (reportId == null) {
				log.info("Empty report id received...");
				return respUtil.prepareErrorResponse("No Custom Print Config found for the selection.");
			}
			log.info("Report id received ##### {} ##### ", reportId);

			// get report master entry
			AmCustomReportRegisterModel reportConfig = amCustomReportRegisterRepository.findByReportId(reportId);

			// set report entity_id path
			filePath.append(reportConfig.getEntityId().toString() + DataConstants.FILE_DELIMITER);

			// check and get list of files to zip (eg. Logo, Signature, etc.)
			List<String> fileNamesList = new ArrayList<>();
			setFileNames(fileNamesList, reportConfig);

			if (fileNamesList.isEmpty()) {
				log.info("Empty report filename received...");
				return respUtil.prepareErrorResponse("No Custom Print File found for the selection.");
			}

			// zip creation and response bytes
			log.info("Creating files zip and writing bytes :: started...");
			byte[] fileBytes = fileUtility.createZipFileBytes(fileNamesList, filePath.toString());
			log.info("Download Zip File bytes : length='{}' ", fileBytes.length);
			log.info("Creating files zip and writing bytes :: ended...");

			if (fileBytes.length > 0) {
				String zipFileName = reportConfig.getReportName() + DataConstants.EMPTY_RESPONSE
						+ LocalDateTime.now().format(DateTimeFormatter.ofPattern(DataConstants.YYYYMMDDHHMMSS))
						+ ".zip";
				StringBuilder base64String = new StringBuilder();
				base64String.append(Base64.getEncoder().encodeToString(fileBytes));
				responseJson.put(DataConstants.RESPONSE, base64String.toString());
				responseJson.put(DataConstants.FILENAME, zipFileName);
				responseJson.put(DataConstants.STATUS, 1);
				responseJson.put(DataConstants.TYPE, "application/zip");
			} else {
				log.info("File not found or is empty in Azure Service Location : reportId='{}'",
						reportConfig.getReportId());
				responseJson = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
			}
		} catch (Exception e) {
			log.error("Error occurred during file download in InvoicePrintService.java  :: Exception '{}', {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving insertCustomReport() of  InvoicePrintService.java : END ");
		return responseJson;
	}

	private void setFileNames(List<String> fileNamesList, AmCustomReportRegisterModel reportConfig) {
		if (!utility.isEmptyString(reportConfig.getReportFileName())) {
			fileNamesList.add(reportConfig.getReportFileName());
		}
		if (!utility.isEmptyString(reportConfig.getLogoFileName())) {
			fileNamesList.add(reportConfig.getLogoFileName());
		}
		if (!utility.isEmptyString(reportConfig.getSignatureFileName())) {
			fileNamesList.add(reportConfig.getSignatureFileName());
		}
	}

	/**
	 * Edit JRXML and Logo File for Custom Print
	 * 
	 * @param inputJasperFile
	 * @param logoInputFile
	 * @param reqData
	 * @return
	 * 
	 * @author SaptakPatil
	 * @param signatureInputFile
	 * @throws IOException
	 * @since 2024
	 */
	public JSONObject editCustomReport(MultipartFile inputJasperFile, MultipartFile logoInputFile,
			MultipartFile signatureInputFile, CustomPrintFileRequestDto reqData) throws IOException {
		log.info("Inside editCustomReport() of InvoicePrintService.java : START ");
		JSONObject responseJson = new JSONObject();
		try {
			if (inputJasperFile == null || inputJasperFile.isEmpty()) {
				log.info("Empty Jasper file received : filesize={}",
						(inputJasperFile != null ? inputJasperFile.getSize() : null));
				return respUtil.prepareErrorResponse("JRXML File is empty. Please Try again!");
			}

			Integer reportId = reqData.getReportId();
			if (reportId == null) {
				log.info("Empty report id received...");
				return respUtil.prepareErrorResponse("No Custom Print Config found for the selection.");
			}
			log.info("Report id received ##### {} ##### ", reportId);

			StringBuilder filePath = new StringBuilder();
			filePath.append(einvJrxmlReport);// base jasper files path
			if (reqData.getIsCustom().equals(DataConstants.CUSTOM_PRINT)) {// custom
				filePath.append(DataConstants.CUSTOM_FOLDER + DataConstants.FILE_DELIMITER);
				filePath.append(reqData.getEntityId() + DataConstants.FILE_DELIMITER);
				log.info("Custom Template...");
			} else {// default
				log.info("Default Template...");
			}

			/* get report master entry */
			AmCustomReportRegisterModel reportConfig = amCustomReportRegisterRepository.findByReportId(reportId);

			/* old files moved to backup folder */
			moveOldFilesToBackup(reportConfig, filePath.toString());

			/* Jasper .jrxml File */
			File newJasperfile = new File(filePath.toString() + inputJasperFile.getOriginalFilename());
			transferFileToFilePath(inputJasperFile, newJasperfile);
			log.info("Jasper file uploaded and edited... :: filename='{}' ", inputJasperFile.getOriginalFilename());

			/* Logo image File */
			if (logoInputFile != null && !logoInputFile.isEmpty()) {
				File newLogofile = new File(filePath.toString() + logoInputFile.getOriginalFilename());
				transferFileToFilePath(logoInputFile, newLogofile);
				log.info("Logo file uploaded and edited... :: filename='{}' ", logoInputFile.getOriginalFilename());
			}

			/* Signature image File */
			if (signatureInputFile != null && !signatureInputFile.isEmpty()) {
				File newSignaturefile = new File(filePath.toString() + signatureInputFile.getOriginalFilename());
				transferFileToFilePath(signatureInputFile, newSignaturefile);
				log.info("Signature file uploaded and edited... :: filename='{}' ",
						signatureInputFile.getOriginalFilename());
			}

			responseJson = respUtil.prepareSuccessResponse(DataConstants.FILE_UPLOADED_SUCCESSFULLY);
		} catch (IllegalStateException e) {
			log.error("Error occurred during edit data in InvoicePrintService.java :: IllegalStateException '{}', {} ",
					e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			log.error("Error occurred during edit data in InvoicePrintService.java  :: Exception '{}', {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving editCustomReport() of  InvoicePrintService.java : END ");
		return responseJson;
	}

	/**
	 * Move old Print Files to New backup directory path and rename with timestamp
	 * 
	 * @param reportConfig
	 * @param originalDirPath
	 * @throws IOException
	 */
	private void moveOldFilesToBackup(AmCustomReportRegisterModel reportConfig, String originalDirPath)
			throws IOException {
		log.info("Inside moveOldFilesToBackup() of InvoicePrintService.java : START ");
		try {
			// fetch all old files (eg. Logo, Signature, etc.)
			List<String> oldFileList = new ArrayList<>();
			setFileNames(oldFileList, reportConfig);
			log.info("Files list found...");

			// create backup directory
			File backupDirectory = new File(einvJrxmlReport + "Report_Backup");
			if (!backupDirectory.exists()) {
				backupDirectory.mkdirs();
				log.info("Backup dirtectory created...");
			}

			// Create subfolder with the current date
			File dateSubfolder = new File(backupDirectory, LocalDate.now().toString());
			if (!dateSubfolder.exists()) {
				dateSubfolder.mkdir();
				log.info("Todays Date backup dirtectory created...");
			}

			// Rename and Move selected files
			for (String fileName : oldFileList) {
				File sourceFile = new File(originalDirPath + fileName);// fetch file
				if (sourceFile.exists() && sourceFile.isFile()) {
					String newFileName = renameOriginalFilenameToTimestampedFilename(fileName);
					File destinationFile = new File(dateSubfolder, newFileName);
					Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					log.info("File moved to today's backup folder :: originalFilename='{}', backupFilename='{}'",
							sourceFile.getName(), destinationFile.getName());
				} else {
					log.error("Issue while moving file :: filename='{}'", sourceFile.getName());
				}
			}
		} catch (Exception e) {
			log.error("Error occurred while moveOldFilesToBackup in InvoicePrintService.java  :: Exception '{}', {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving moveOldFilesToBackup() of InvoicePrintService.java : END ");
	}

	public String renameOriginalFilenameToTimestampedFilename(String originalFileName) {
		String fileBaseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
		String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DataConstants.YYYYMMDDHHMMSS));
		return fileBaseName + "_" + timestamp + fileExtension;
	}

}