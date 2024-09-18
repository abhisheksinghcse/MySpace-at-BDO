package com.bdo.myinv.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dto.ReportRequest;
import com.bdo.myinv.dto.SftpAdminReportDto;
import com.bdo.myinv.model.master.EinvExcelReportConfigMasterModel;
import com.bdo.myinv.repository.master.EinvExcelReportConfigMasterRepository;
import com.bdo.myinv.utils.ResponseUtility;
import com.bdo.myinv.utils.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceAdminReportServiceImpl {
	
	@Value("${einv.report.download.path}")
	private String einvReportDownloadFilePath;

	/**
	 * Utility Objects
	 */
	private final ResponseUtility respUtil;
	private final Utility utility;
	
	/**
	 * Repository
	 */
	private final EinvExcelReportConfigMasterRepository einvExcelReportConfigMasterRepository;
	
	public JSONObject generateSftpAdminReport(List<SftpAdminReportDto> sftpAdminReportDto, ReportRequest reqData) throws IOException {
		
		log.info("Inside generateSftpAdminReport() of InvoiceAdminReportServiceImpl.java : START ");

		JSONObject responseJson = new JSONObject();
		String filePath = null;		
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.YYYYMMDDHHMMSSSSSS);
		String timestamp = dateFormat.format(new Date()); 
		String reportFileName = reqData.getReportName()+timestamp+DataConstants.XLSX_FILE_EXTENSION;

		filePath = einvReportDownloadFilePath+reportFileName;
		log.info("Inside generateSftpAdminReport() of InvoiceAdminReportServiceImpl.java : filepath is ::{} ", filePath);

		File f = new File(filePath);		
		try (FileOutputStream fs = new FileOutputStream(f)){			
			Workbook wb = new Workbook(fs, reqData.getReportName(), "1.0");
			String workBookSheet = reqData.getReportName();
			Worksheet sheet=wb.newWorksheet(workBookSheet);
			if(sftpAdminReportDto!=null) {							
				writeSftpAdminReportExcel(sftpAdminReportDto,sheet);
				sheet.finish();	
				wb.finish();					
			}
			responseJson = utility.createExportReportResponse(f, reportFileName);

			responseJson = respUtil.prepareSuccessResponse(responseJson);			
		} catch (IOException e) {
			log.error("Exception occurred in generateSftpAdminReport() of InvoiceAdminReportServiceImpl.java :: Exception {} ", e.getMessage(), e);	
			throw e;
		}finally {			
			boolean deleteflag = Files.deleteIfExists(f.toPath());
			log.info("Local file copy deleted succesfully: flag={}", deleteflag);
		}
		log.info("Leaving generateSftpAdminReport() of InvoiceAdminReportServiceImpl.java : END ");
		return responseJson;
	}

	private void writeSftpAdminReportExcel(List<SftpAdminReportDto> sftpBatchwiseStatusReportDto,
			Worksheet sheet) {
		log.info("Inside writeSftpAdminReportExcel() of InvoiceAdminReportServiceImpl.java : START ");

		List<EinvExcelReportConfigMasterModel> einvExcelReportConfigMasterModelList = einvExcelReportConfigMasterRepository.findByReportTypeAndReportName(DataConstants.ADMIN, DataConstants.SFTPADMINREPORT);

		int startRow = 0;

		startRow = utility.setFirstHeader(sheet, einvExcelReportConfigMasterModelList, startRow);

		try {
			for(SftpAdminReportDto sftpBatchwiseStatusReportDtoList : sftpBatchwiseStatusReportDto) {

				sheet.value(startRow, 0, sftpBatchwiseStatusReportDtoList.getBatchNo());
				sheet.value(startRow, 1, sftpBatchwiseStatusReportDtoList.getClientFileName());
				sheet.value(startRow, 2, sftpBatchwiseStatusReportDtoList.getSystemFileName());
				sheet.value(startRow, 3, sftpBatchwiseStatusReportDtoList.getFileReceivedDate());							
				sheet.value(startRow, 4, sftpBatchwiseStatusReportDtoList.getFileReceivedTimeStamp());
				sheet.value(startRow, 5, sftpBatchwiseStatusReportDtoList.getUserName());
				sheet.value(startRow, 6, sftpBatchwiseStatusReportDtoList.getIsUINGenerated());
				sheet.value(startRow, 7, sftpBatchwiseStatusReportDtoList.getReverseSyncStatus());
				sheet.value(startRow, 8, sftpBatchwiseStatusReportDtoList.getReverseSyncFileName());
				sheet.value(startRow, 9, sftpBatchwiseStatusReportDtoList.getReverseFileSendTime());
				sheet.value(startRow, 10, sftpBatchwiseStatusReportDtoList.getUploadType());
				sheet.value(startRow, 11, sftpBatchwiseStatusReportDtoList.getTemplateType());
				sheet.value(startRow, 12, sftpBatchwiseStatusReportDtoList.getIsCustomTemplate());
				sheet.value(startRow, 13, sftpBatchwiseStatusReportDtoList.getCustomTemplateName());					
				startRow++;
			}
		}catch(Exception e) {
			log.error("Exception occurred in writeSftpAdminReportExcel() of InvoiceAdminReportServiceImpl.java :: Exception {} ", e.toString());	
			throw e;
		}
		log.info("Leaving writeSftpAdminReportExcel() of InvoiceAdminReportServiceImpl.java : END ");

	}
	
	

}
