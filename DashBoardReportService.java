package com.bdo.myinv.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bdo.myinv.constants.DataConstants;
import com.bdo.myinv.dto.DashBoardDataFlowDTO;
import com.bdo.myinv.dto.DashBoardRequestDTO;
import com.bdo.myinv.dto.DashBoardResponseDTO;
import com.bdo.myinv.dto.EinvoiceTypeSalesDTO;
import com.bdo.myinv.dto.SalesTypeDTO;
import com.bdo.myinv.dto.TotalUserDetailsDTO;
import com.bdo.myinv.dto.TransactionTypeDTO;
import com.bdo.myinv.repository.EinvOutwardBillHRepository;
import com.bdo.myinv.utils.ResponseEntityData;
import com.bdo.myinv.multitenant.master.repository.TenantConnectionMasterRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashBoardReportService {

	@Autowired
	private EinvOutwardBillHRepository einvOutwardBillhRepository;
	@Autowired
	private TenantConnectionMasterRepository tenantConnectionMasterRepository;

	public ResponseEntity<ResponseEntityData> getDashBoardReport(DashBoardRequestDTO request) {

		log.info("Inside DashBoardReportService getDashBoardReport() ");
		DashBoardResponseDTO responseDto = new DashBoardResponseDTO();
		ResponseEntity<ResponseEntityData> response = null;
		if (request != null && !request.getFp().isEmpty() && !request.getSupllierTin().isEmpty()) {
			log.info("Inside DashBoardReportService getDashBoardReport() request :: ", request.toString());
			List<SalesTypeDTO> salesDetails = new ArrayList<>();
			try {
				salesDetails = einvOutwardBillhRepository.getSalesTypeDTOs(request.getSupllierTin(), request.getFp());

				if (salesDetails != null && !salesDetails.isEmpty()) {
					log.info("Inside DashBoardReportService getDashBoardReport()  Details after Query :: ",
							salesDetails.toString());
					responseDto = new DashBoardResponseDTO();
					TransactionTypeDTO transactionTypeSales = new TransactionTypeDTO();
					EinvoiceTypeSalesDTO envoiceTypeSales = new EinvoiceTypeSalesDTO();

					for (SalesTypeDTO s : salesDetails) {

						switch (s.getTransactionType()) {

						case "e_invoice_type_1":
							transactionTypeSales.setInvoice(transactionTypeSales.getInvoice().add(s.getExchangeRate()));

							break;
						case "e_invoice_type_2":
							transactionTypeSales.setCreditNote(transactionTypeSales.getCreditNote().add(s.getExchangeRate()));
							break;

						case "e_invoice_type_3":
							transactionTypeSales.setDebitNote(transactionTypeSales.getDebitNote().add(s.getExchangeRate()));
							break;

						case "e_invoice_type_4":
							transactionTypeSales.setRefundNote(transactionTypeSales.getRefundNote().add(s.getExchangeRate()));
							break;

						case "transaction_type_B2B":
							envoiceTypeSales.setB2b(envoiceTypeSales.getB2b().add(s.getExchangeRate()));
							break;

						case "transaction_type_B2G":
							envoiceTypeSales.setB2g(envoiceTypeSales.getB2g().add(s.getExchangeRate()));
							break;

						case "transaction_type_B2C":
							envoiceTypeSales.setB2c(envoiceTypeSales.getB2c().add(s.getExchangeRate()));
							break;

						case "transaction_type_Export":
							envoiceTypeSales.setExport(envoiceTypeSales.getExport().add(s.getExchangeRate()));
							break;

						case "transaction_type_Import":
							envoiceTypeSales.setImports(envoiceTypeSales.getImports().add(s.getExchangeRate()));
							break;

						}

					}
					BigDecimal value1 = (transactionTypeSales.getInvoice() != null ? transactionTypeSales.getInvoice()
							: BigDecimal.ZERO)
							.add(transactionTypeSales.getDebitNote() != null ? transactionTypeSales.getDebitNote()
									: BigDecimal.ZERO);
					BigDecimal value2 = (transactionTypeSales.getCreditNote() != null
							? transactionTypeSales.getCreditNote()
							: BigDecimal.ZERO)
							.add(transactionTypeSales.getRefundNote() != null ? transactionTypeSales.getRefundNote()
									: BigDecimal.ZERO);

					BigDecimal total = value1.subtract(value2);

					responseDto.setTotalSales(total);
					responseDto.setTransactionTypeSales(transactionTypeSales);
					responseDto.setEinvoiceTypeSales(envoiceTypeSales);

					response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
							.message(DataConstants.SUCCESS_MESSAGE).data(responseDto).build(), HttpStatus.OK);

				} else {
					response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
							.message(DataConstants.NOCONTENT).data(responseDto).build(), HttpStatus.OK);
				}

			} catch (Exception e) {
				log.error("Inside DashBoardReportService getDashBoardReport() Exception   {} ", e);
			}
		} else {
			response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.FALSE)
					.message(DataConstants.INVALID_REQUEST).data(responseDto).build(), HttpStatus.BAD_REQUEST);
		}
		return response;
	}

	public ResponseEntity<ResponseEntityData> getDashBoardDataFlow(DashBoardRequestDTO request) {

		log.info("Inside DashBoardReportService getDashBoardDataFlow ");
		ResponseEntity<ResponseEntityData> response = null;
		if (request != null && !request.getFp().isEmpty() && !request.getSupllierTin().isEmpty()) {
			log.info("Inside DashBoardReportService getDashBoardDataFlow() request :: ", request.toString());
			List<Object[]> objList = einvOutwardBillhRepository.findStatusAndDescription(request.getSupllierTin(),
					request.getFp());

			List<DashBoardDataFlowDTO> responseList = new ArrayList<>();

			if (!objList.isEmpty()) {
				log.info("Inside DashBoardReportService getDashBoardDataFlow()  Details after Query :: ",
						objList.toString());
				for (Object[] obj : objList) {
					DashBoardDataFlowDTO dto = new DashBoardDataFlowDTO();

					dto.setDocType(obj[0] != null ? obj[0].toString() : "");
					dto.setStatusDescription(obj[1] != null ? obj[1].toString() : "");
					dto.setCount(obj[2] != null ? obj[2].toString() : "");

					responseList.add(dto);

				}

				response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
						.message(DataConstants.SUCCESS_MESSAGE).data(responseList).build(), HttpStatus.OK);
			} else {

				response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
						.message(DataConstants.NOCONTENT).data(responseList).build(), HttpStatus.OK);

			}
		} else {

			response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.FALSE)
					.message(DataConstants.INVALID_REQUEST).data("").build(), HttpStatus.BAD_REQUEST);

		}
		return response;
	}

	public ResponseEntity<ResponseEntityData> getDashBoardTotalUserDetails(DashBoardRequestDTO request) {

		log.info("Inside DashBoardReportService getDashBoardTotalUserDetails ");
		ResponseEntity<ResponseEntityData> response = null;
		if (request != null && !request.getFp().isEmpty() && !request.getSupllierTin().isEmpty()) {
			log.info("Inside DashBoardReportService getDashBoardTotalUserDetails() request :: ", request.toString());
			List<Object[]> objList = tenantConnectionMasterRepository.findTotalUserStatus(request.getSupllierTin());

			List<TotalUserDetailsDTO> responseList = new ArrayList<>();

			if (!objList.isEmpty()) {
				log.info("Inside DashBoardReportService getDashBoardTotalUserDetails() Details after value :: ",
						objList.toString());
				for (Object[] obj : objList) {
					TotalUserDetailsDTO dto = new TotalUserDetailsDTO();

					dto.setEntityStatus(obj[0] != null ? obj[0].toString() : "");
					dto.setCount(obj[1] != null ? obj[1].toString() : "");
					dto.setUserStatus(obj[2] != null ? obj[2].toString() : "");

					responseList.add(dto);

				}

				response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
						.message(DataConstants.SUCCESS_MESSAGE).data(responseList).build(), HttpStatus.OK);
			} else {

				response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.TRUE)
						.message(DataConstants.NOCONTENT).data(responseList).build(), HttpStatus.OK);

			}
		} else {

			response = new ResponseEntity<>(ResponseEntityData.builder().status(DataConstants.FALSE)
					.message(DataConstants.INVALID_REQUEST).data("").build(), HttpStatus.BAD_REQUEST);

		}
		return response;
	}

}
