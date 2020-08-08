package org.openmrs.module.indianaemsadt.web.controller;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.springframework.aop.AfterReturningAdvice;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;

public class AfterAdvice implements AfterReturningAdvice {
	
	private final Log log = LogFactory.getLog(this.getClass());
	
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals("savePatient")) {
			Patient patient = (Patient) returnValue;
			
			ADT_A01 adt = generateADT(patient);
			String adtAsString = getMessage(adt);
			if (log.isDebugEnabled()) {
				log.debug("Sending ADT: " + adtAsString);
			}
			
			// Send ADT to HL7 destination
			String response = post(adt);
			
			// Create an observation containing ADT message for backup
			String adtObsConceptUuid = Context.getAdministrationService().getGlobalProperty(
			    Constants.ADT_OBS_CONCEPT_GLOBAL_PROPERTY);
			if (adtObsConceptUuid == null)
				adtObsConceptUuid = Constants.DEFAULT_ADT_OBS_CONCEPT;
			Concept concept = Context.getConceptService().getConceptByUuid(adtObsConceptUuid);
			Obs o = new Obs();
			o.setPerson(patient);
			o.setConcept(concept);
			o.setDateCreated(new Date());
			o.setCreator(Context.getAuthenticatedUser());
			o.setObsDatetime(new Date());
			o.setValueText(adtAsString + "\n---\n" + response);
			Context.getObsService().saveObs(o, "ADT");
		}
	}
	
	public String post(ADT_A01 adt) {
		
		String responseString;
		
		String destinationServer = System.getenv(Constants.HL7_URL_ENV_VARIABLE);
		if (destinationServer == null)
			destinationServer = Constants.DEFAULT_HL7_URL;
		int destinationPort;
		try {
			destinationPort = Integer.parseInt(System.getenv(Constants.HL7_PORT_ENV_VARIABLE));
		}
		catch (Exception e) {
			destinationPort = Constants.DEFAULT_HL7_PORT;
		}
		if (log.isDebugEnabled()) {
			log.debug("Sending ADT to " + destinationServer + ":" + destinationPort);
		}
		
		try {
			
			HapiContext ctx = new DefaultHapiContext();
			
			// create a new MLLP client over the specified port
			Connection connection = ctx.newClient(destinationServer, destinationPort, false);
			
			// the initiator which will be used to transmit our message
			Initiator initiator = connection.getInitiator();
			
			// send the previously created HL7 message over the connection established
			Message response = initiator.sendAndReceive(adt);
			
			PipeParser parser = new PipeParser();
			responseString = parser.encode(response);
			log.debug("ADT response: " + responseString);
			
			// clean up
			connection.close();
			
		}
		catch (Exception e) {
			responseString = "Error: " + e.getMessage();
			e.printStackTrace();
		}
		
		return responseString;
	}
	
	public String getMessage(ADT_A01 adt) {
		PipeParser parser = new PipeParser();
		String msg = null;
		try {
			msg = parser.encode(adt);
		}
		catch (HL7Exception e) {
			log.error(e);
		}
		return msg;
	}
	
	public ADT_A01 generateADT(Patient patient) {
		ADT_A01 adt = new ADT_A01();
		try {
			
			MSH msh = adt.getMSH();
			
			// Get current date
			String dateFormat = "yyyyMMddHHmmss";
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			String formattedDate = formatter.format(new Date());
			
			msh.getFieldSeparator().setValue(Constants.FIELD_SEPARATOR);
			msh.getEncodingCharacters().setValue(Constants.ENCODING_CHARACTERS);
			msh.getVersionID().getInternationalizationCode().getIdentifier().setValue(Constants.INTERNATIONALIZATION_CODE);
			msh.getVersionID().getVersionID().setValue(Constants.VERSION);
			msh.getDateTimeOfMessage().getTime().setValue(formattedDate);
			
			Location location = Context.getLocationService().getLocation(Context.getUserContext().getLocationId());
			msh.getSendingFacility().getNamespaceID().setValue(location.getName());
			msh.getSendingApplication().getNamespaceID().setValue(Constants.SENDING_APPLICATION);
			msh.getMessageType().getMessageCode().setValue(Constants.MESSAGE_TYPE);
			msh.getMessageType().getTriggerEvent().setValue(Constants.TRIGGER_EVENT);
			msh.getMessageType().getMessageStructure().setValue(Constants.MESSAGE_STRUCTURE);
			msh.getReceivingFacility().getNamespaceID().setValue(Constants.RECEIVING_FACILITY);
			msh.getProcessingID().getProcessingID().setValue(Constants.PROCESSING_ID);
			msh.getMessageControlID().setValue(UUID.randomUUID().toString());
			
			msh.getAcceptAcknowledgmentType().setValue(Constants.ACK_TYPE);
			msh.getApplicationAcknowledgmentType().setValue(Constants.APPLICATION_ACK_TYPE);
			msh.getMessageProfileIdentifier(0).getEntityIdentifier().setValue(Constants.MSG_PROFILE_IDENTIFIER);
			
			EVN evn = adt.getEVN();
			evn.getRecordedDateTime().getTs1_Time().setValue(new Date());
			
			PID pid = adt.getPID();
			PatientIdentifier patientIdentifier = patient.getPatientIdentifier(Constants.IDENTIFIER_TYPE);
			pid.getSetIDPID().setValue(patientIdentifier.getIdentifier());
			pid.getPatientAccountNumber().getIDNumber().setValue(patientIdentifier.getIdentifier());
			
			pid.getPatientName(0).getFamilyName().getSurname().setValue(patient.getFamilyName());
			pid.getPatientName(0).getGivenName().setValue(patient.getGivenName());
			pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(patient.getMiddleName());
			
			// dob
			Date dob = patient.getBirthdate();
			String dobStr = "";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			dobStr = sdf.format(dob);
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);
			pid.getAdministrativeSex().setValue(patient.getGender());
			
			Set<PersonAddress> addresses = patient.getAddresses();
			
			int i = 0;
			for (PersonAddress address : addresses) {
				pid.getPatientAddress(i).getStreetAddress().parse(address.getAddress1());
				pid.getPatientAddress(i).getCity().setValue(address.getCityVillage());
				pid.getPatientAddress(i).getZipOrPostalCode().setValue(address.getPostalCode());
				pid.getPatientAddress(i).getStateOrProvince().setValue(address.getStateProvince());
				i = i + 1;
			}
			
			PersonAttribute personAttribute = patient.getAttribute(Constants.TEL);
			if (personAttribute != null)
				pid.getPhoneNumberHome(0).getTelephoneNumber().setValue(personAttribute.getValue());
			
			personAttribute = patient.getAttribute(Constants.SSN);
			if (personAttribute != null)
				pid.getPid19_SSNNumberPatient().setValue(personAttribute.getValue());
			
			PV1 pv1 = adt.getPV1();
			pv1.getSetIDPV1().setValue(Constants.IDPV1);
			pv1.getPatientClass().setValue(Constants.PATIENT_CLASS);
			
		}
		catch (Exception e) {
			log.error(e);
		}
		return adt;
	}
	
}
