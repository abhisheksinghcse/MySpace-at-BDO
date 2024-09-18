package com.bdo.myinv.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dao.ReportDao;
import com.bdo.myinv.dto.EinvReportGridDto;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.model.TxReportLogsModel;
import com.bdo.myinv.repository.TxReportLogsRepository;
import com.bdo.myinv.service.common.AzureBlobFileService;
import com.bdo.myinv.utils.DateUtility;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceBackgroundReportService {

	/**
	 * Utility Objects
	 */
	private final ResponseUtility respUtil;
	private final Utility utility;
	private final DateUtility dateUtility;

	/**
	 * Dao
	 */
	private final ReportDao reportDao;

	/**
	 * Repository
	 */
	private final TxReportLogsRepository txReportLogsRepository;

	/**
	 * Service
	 */
	private final AzureBlobFileService azureBlobFileService;

	public JSONObject getBackgroundReportList(ReportRequest reqData) {
		log.info("Inside getBackgroundReportList() of InvoiceBackgroundReportService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		BigInteger totalCount = BigInteger.ZERO;		
		List<EinvReportGridDto> einvReportDtoObj = new ArrayList<>();
		try {
			einvReportDtoObj = reportDao.getBackgroundReportData(reqData).stream().map(objData -> {
				EinvReportGridDto einvReportDto = new EinvReportGridDto();
				einvReportDto.setScheduleId((Long) objData[0]);
				einvReportDto.setTin(utility.checkNull((String) objData[1]));
				einvReportDto.setReportName(utility.checkNull((String) objData[2]));
				einvReportDto.setStartedOn(
						objData[3] != null ? dateUtility.convertTimestampToString((Timestamp) objData[3]) : "-");
				einvReportDto.setCompletedOn(
						objData[4] != null ? dateUtility.convertTimestampToString((Timestamp) objData[4]) : "-");
				einvReportDto.setCreatedBy(utility.checkNull((String) objData[5]));
				einvReportDto.setStatus(objData[6] != null ? utility.getStatusDescription((Integer) objData[6]) : "-");
				return einvReportDto;
			}).toList();

			jsonArrayData = new JSONArray(einvReportDtoObj);
			totalCount = reportDao.getCountOfBackgroundReportData(reqData);
			return respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData, totalCount,
					reqData.getPageNo(), reqData.getPageSize());
		} catch (Exception e) {
			log.error(
					"Exception occurred in getBackgroundReportList() of InvoiceBackgroundReportService.java :: Exception {} ",
					e.getMessage(), e);
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
	}

	public JSONObject backgroundReportDownloadAndRetrigger(ReportRequest reqData) {
		log.info("Inside backgroundReportDownloadAndRetrigger() of InvoiceBackgroundReportService.java : START ");

		JSONObject jsonResponse = new JSONObject();
		try {
			switch (reqData.getAction()) {
			case DataConstants.BACKGROUND_REPORT_DOWNLOAD:
				jsonResponse = backgroundReportDownload(reqData);
				break;

			case DataConstants.BACKGROUND_REPORT_SEND_EMAIL:

				break;

			case DataConstants.RETRIGGER:
				jsonResponse = backgroundReportRetrigger(reqData);
				break;

			default:
				log.info(
						"Inside backgroundReportDownloadAndRetrigger() in InvoiceBackgroundReportService.java deafult case :: Invalid Action :: {}",
						reqData.getAction());
				break;
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in backgroundReportDownloadAndRetrigger() of InvoiceBackgroundReportService.java :: Exception {} ",
					e.getMessage(), e);
			jsonResponse = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving backgroundReportDownloadAndRetrigger() of InvoiceBackgroundReportService.java : END ");
		return jsonResponse;
	}

	private JSONObject backgroundReportDownload(ReportRequest reqData) {
		log.info("Inside backgroundReportDownload() of InvoiceBackgroundReportService.java : START ");

		byte[] fileByte = null;
		JSONObject dataObj = new JSONObject();
		try {
			TxReportLogsModel txReportLog = txReportLogsRepository.findByTxLogId(reqData.getScheduledId());

			String azureFilePath = txReportLog.getFileName();

			String azureBlobFolderPath = azureFilePath.substring(0, azureFilePath.lastIndexOf('/'));// azure folder path
			String downloadFileName = azureFilePath.substring(azureFilePath.lastIndexOf('/') + 1);// download filename

			fileByte = azureBlobFileService.downloadFileFromAzureBlob(azureBlobFolderPath, downloadFileName);// download
																												// file
			if (fileByte != null) {
				StringBuilder base64String = new StringBuilder();
				base64String.append(Base64.getEncoder().encodeToString(fileByte));
				dataObj.put("response", base64String.toString());
				dataObj.put("filename", downloadFileName);
				dataObj.put(DataConstants.TYPE, "application/zip");
			} else {
				log.info("File not found or is empty in Azure Blob : file='{}'", downloadFileName);
				dataObj = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in backgroundReportDownload() of InvoiceBackgroundReportService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving backgroundReportDownload() of InvoiceBackgroundReportService.java : END ");
		return dataObj;
	}

	private JSONObject backgroundReportRetrigger(ReportRequest reqData) {
		log.info("Inside backgroundReportRetrigger() of InvoiceBackgroundReportService.java : START ");

		TxReportLogsModel txReportLogModel = txReportLogsRepository.findByTxLogId(reqData.getScheduledId());

		JSONObject responseJson = new JSONObject();
		TxReportLogsModel txReportLog = new TxReportLogsModel();
		try {
			txReportLog.setTin(String.join(",", txReportLogModel.getTin()));
			txReportLog.setDateRange(txReportLogModel.getDateRange());
			txReportLog.setReportName(txReportLogModel.getReportName());
			txReportLog.setPrintTemplateId(txReportLogModel.getPrintTemplateId());
			txReportLog.setTotalCount(String.valueOf(txReportLogModel.getTotalCount()));
			txReportLog.setReportType(DataConstants.SCHEDULE_DOWNLOAD_REPORT_TYPE); // 2 for BackGround Report Scheduler
																					// report
			txReportLog.setJobStatus(DataConstants.SCHEDULE_JOB_STATUS_PENDING); // 0 for pending
			txReportLog.setActionStartStamp(new Date());
			txReportLog.setProcStartStamp(new Date());
			txReportLog.setCreatedOn(new Date());
			txReportLog.setQueryEndStamp(new Date());
			txReportLog.setLastActionOn(new Date());
			txReportLog.setUserId(txReportLogModel.getUserId());
			txReportLog.setEntityId(txReportLogModel.getEntityId());
			txReportLog.setDocDatatype(txReportLogModel.getDocDatatype());
			txReportLog.setDivisionCode(
					txReportLogModel.getDivisionCode() != null && !txReportLogModel.getDivisionCode().isEmpty()
							? String.join(",", txReportLogModel.getDivisionCode())
							: null);
			txReportLog.setBranchCode(
					txReportLogModel.getBranchCode() != null && !txReportLogModel.getBranchCode().isEmpty()
							? String.join(",", txReportLogModel.getBranchCode())
							: null);
			txReportLog.setEinvStatus(
					txReportLogModel.getEinvStatus() != null && !txReportLogModel.getEinvStatus().isEmpty()
							? String.join(",", txReportLogModel.getEinvStatus())
							: null);
			txReportLog.setReportLevel(txReportLogModel.getReportLevel());
			txReportLog.setCustomTemplateId(txReportLogModel.getCustomTemplateId());
			log.info("saving data in tx_reports_log table");
			txReportLogsRepository.save(txReportLog);
			Long txLogId = txReportLog.getTxLogId();
			log.info("Leaving backgroundReportRetrigger() tx_log_id is :: ", txLogId.toString());
			JSONObject jobj = new JSONObject();
			jobj.put(DataConstants.MESSAGE, "Background report request initiated for "
					+ txReportLogModel.getReportName() + ". Your schedule ID is - " + txLogId);
			responseJson = respUtil.prepareSuccessResponse(jobj);
		} catch (Exception e) {
			log.error(
					"Exception occurred in backgroundReportRetrigger() of InvoiceBackgroundReportService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving ScheduledBackgroundReportData() of InvoiceBackgroundReportService.java : END ");
		return responseJson;

	}
}
