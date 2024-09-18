package com.bdo.myinv.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dao.SftpReportDao;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.SftpAdminReportDto;
import com.bdo.myinv.model.master.UserMaster;
import com.bdo.myinv.repository.master.UserMasterRepository;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceAdminReportService {

	/**
	 * Utility Objects
	 */
	private final Utility utility;
	private final ResponseUtility respUtil;
	/**
	 * Repostories
	 */
	private final UserMasterRepository userMasterRepository;
	/**
	 * Dao
	 */
	private final SftpReportDao sftpReportDao;

	/**
	 * Service
	 */
	private final InvoiceAdminReportServiceImpl invoiceAdminReportServiceImpl;

	public JSONObject adminInvoiceReportData(ReportRequest reqData) {
		log.info("Inside adminInvoiceReportData() of InvoiceAdminReportService.java : START ");
		JSONObject jsonResponse = new JSONObject();
		try {

			switch (reqData.getReportName()) {
			case DataConstants.SFTPADMINREPORT:
				jsonResponse = getAdminSftpReportData(reqData);
				break;

			default:
				log.info(
						"Inside adminInvoiceReportData() in InvoiceAdminReportService.java deafult case :: Invalid Report Name :: {}",
						reqData.getReportName());
				break;
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in adminInvoiceReportData() of InvoiceAdminReportService.java :: Exception {} ",
					e.getMessage(), e);
			jsonResponse = respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving adminInvoiceReportData() of InvoiceAdminReportService.java : END ");
		return jsonResponse;
	}

	private JSONObject getAdminSftpReportData(ReportRequest reqData) {
		log.info("Inside getAdminSftpReportData() of InvoiceAdminReportService.java : START ");
		JSONArray jsonArrayData = new JSONArray();
		BigInteger totalCount = BigInteger.ZERO;
		JSONObject jsonResponse = null;
		List<SftpAdminReportDto> sftpAdminReportDto = adminSftpReportData(reqData);

		try {
			if (reqData.getAction().equalsIgnoreCase(DataConstants.VIEW)) {
				jsonArrayData = new JSONArray(sftpAdminReportDto);
				totalCount = sftpReportDao.getCountOfSftpAdminReportData(reqData);
				jsonResponse = respUtil.preparePaginationGridResponseWithTotalCount(jsonArrayData, totalCount,
						reqData.getPageNo(), reqData.getPageSize());
			} else if (reqData.getAction().equalsIgnoreCase(DataConstants.EXPORT)) {
				jsonResponse = invoiceAdminReportServiceImpl.generateSftpAdminReport(sftpAdminReportDto, reqData);
			} else {
				log.info("Inside else block getAdminSftpReportData() of InvoiceAdminReportService.java :: {} ",
						reqData.getReportLevel());
			}
		} catch (Exception e) {
			log.error(
					"Exception occurred in getAdminSftpReportData() of InvoiceAdminReportService.java :: Exception {} ",
					e.getMessage(), e);
			return respUtil.prepareErrorResponse(DataConstants.SOMETHING_WENT_WRONG_CONTACT_SUPPORT);
		}
		log.info("Leaving getAdminSftpReportData() of InvoiceAdminReportService.java : END ");
		return jsonResponse;
	}

	private List<SftpAdminReportDto> adminSftpReportData(ReportRequest reqData) {

		log.info("Inside adminSftpReportData() of InvoiceAdminReportService.java : START ");
		List<SftpAdminReportDto> sftpAdminReportDtoObj = new ArrayList<>();
		try {
			List<Object[]> adminSftpReportData = sftpReportDao.getAdminSftpReportData(reqData);
			Map<Integer, UserMaster> userMasterMap = userMasterRepository
					.findByUserIdIn(adminSftpReportData.parallelStream().map(e -> e[5]).filter(Objects::nonNull)
							.map(String::valueOf).filter(StringUtils::isNotBlank).map(Integer::parseInt).toList())
					.parallelStream().collect(Collectors.toMap(UserMaster::getUserId, e -> e, (o, n) -> n));
			sftpAdminReportDtoObj = adminSftpReportData.stream().map(objData -> {
				SftpAdminReportDto sftpAdminReportDto = new SftpAdminReportDto();

				sftpAdminReportDto.setBatchNo(utility.checkNull((String) objData[0]));
				sftpAdminReportDto.setClientFileName(utility.checkNull((String) objData[1]));
				sftpAdminReportDto.setSystemFileName(utility.checkNull((String) objData[2]));
				sftpAdminReportDto.setFileReceivedDate(utility.checkNull((String) objData[3]));
				sftpAdminReportDto.setFileReceivedTimeStamp(utility.checkNull((String) objData[4]));
				sftpAdminReportDto.setUserName(utility.getUserName(objData[5],userMasterMap));
				sftpAdminReportDto.setIsUINGenerated(utility.checkNull((String) objData[6]));
				sftpAdminReportDto.setReverseSyncStatus(utility.checkNull((String) objData[7]));
				sftpAdminReportDto.setReverseSyncFileName(utility.checkNull((String) objData[8]));
				sftpAdminReportDto.setReverseFileSendTime(utility.checkNull((String) objData[9]));

				sftpAdminReportDto.setUploadType(utility.checkNull((String) objData[10]));
				sftpAdminReportDto.setTemplateType(utility.checkNull((String) objData[11]));
				sftpAdminReportDto.setIsCustomTemplate(utility.checkNull((String) objData[12]));
				sftpAdminReportDto.setCustomTemplateName(utility.checkNull((String) objData[13]));
				return sftpAdminReportDto;
			}).toList();
		} catch (Exception e) {
			log.error("Exception occurred in adminSftpReportData() of InvoiceAdminReportService.java :: Exception {} ",
					e.getMessage(), e);
			throw e;
		}
		log.info("Leaving adminSftpReportData() of InvoiceAdminReportService.java : END ");
		return sftpAdminReportDtoObj;
	}
}
