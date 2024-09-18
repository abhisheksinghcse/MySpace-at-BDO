package com.bdo.myinv.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bdo.myinv.model.AmErrorCodeDetails;
import com.bdo.myinv.repository.AmErrorCodeDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonUtilityService {

	@Autowired
	private AmErrorCodeDetailsRepository amErrorCodeDetailsRepository;
	
	public String getErrorDescription(String errorCodes) {
        log.info("Inside getErrorDescription() of InvoiceSalesReportService.java : START ");
        if(errorCodes == null || errorCodes.isBlank()) {
        	return "";
        }
        List<String> errorCodesList = new ArrayList<>(Arrays.asList(errorCodes.split(",")));
        StringBuilder errorDesc = new StringBuilder();
		List<AmErrorCodeDetails> amErrorCodeDetailsList = amErrorCodeDetailsRepository.findAll();
		Map<String, String> errorCodeDescriptionMap = amErrorCodeDetailsList.stream()
                .collect(Collectors.toMap(AmErrorCodeDetails::getErrorCode, AmErrorCodeDetails::getErrorDescription));
		for (int i = 0; i < errorCodesList.size(); i++) {
		    errorDesc.append(errorCodeDescriptionMap.get(errorCodesList.get(i)));
		    if (i < errorCodesList.size() - 1) {
		        errorDesc.append("|");
		    }
		}
		log.info("Leaving getErrorDescription() of InvoiceSalesReportService.java : END ");
		return errorDesc.toString();
	}
}
