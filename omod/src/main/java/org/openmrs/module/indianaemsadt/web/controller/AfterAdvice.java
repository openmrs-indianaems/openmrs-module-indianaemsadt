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
import ca.uhn.hl7v2.model.v25.segment.PV1;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.context.Context;
import org.springframework.aop.AfterReturningAdvice;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.parser.PipeParser;

public class AfterAdvice implements AfterReturningAdvice {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private int count = 0;
	
	private HapiContext ctx;
	
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals("savePatient")) {
			Patient patient = (Patient) returnValue;
			
			ADT_A01 adt = generateADT(patient);
			String adtAsString = getMessage(adt);
			if (log.isDebugEnabled()) {
				log.debug("Sending ADT: " + adtAsString);
			}
			
			// Create an observation containing ADT messsage for backup
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
			o.setValueText(adtAsString);
			Context.getObsService().saveObs(o, "ADT");
			
			// Send ADT to HL7 destination
			post(adt);
		}
	}
	
	public void post(ADT_A01 adt) {
		try {
			String destinationServer = System.getenv(Constants.HL7_URL);
			int destinationPort = Integer.parseInt(System.getenv(Constants.HL7_PORT));
			
			ctx = new DefaultHapiContext();
			
			// create a new MLLP client over the specified port
			Connection connection = ctx.newClient(destinationServer, destinationPort, false);
			
			// The initiator which will be used to transmit our message
			Initiator initiator = connection.getInitiator();
			
			// send the previously created HL7 message over the connection established
			Message response = initiator.sendAndReceive(adt);
			
			// display the message response received from the remote party
			if (log.isDebugEnabled()) {
				PipeParser parser = new PipeParser();
				String responseString = parser.encode(response);
				log.debug("ADT response: " + responseString);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
	
	public ADT_A01 generateADT(Person person) {
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
			msh.getProcessingID().getProcessingMode().setValue(Constants.PROCESSING_MODE);
			msh.getMessageControlID().setValue(UUID.randomUUID().toString());
			
			msh.getAcceptAcknowledgmentType().setValue(Constants.ACK_TYPE);
			msh.getApplicationAcknowledgmentType().setValue(Constants.APPLICATION_ACK_TYPE);
			msh.getMessageProfileIdentifier(0).getEntityIdentifier().setValue(Constants.MSG_PROFILE_IDENTIFIER);
			
			Cohort singlePatientCohort = new Cohort();
			//System.out.println(Context.getPatientService().getAllPatients());
			singlePatientCohort.addMember(person.getId());
			
			//Map<Integer, String> patientIdentifierMap = Context.getPatientService().getPatientIdentifierByUuid(arg0);
			//Map<Integer, String> patientIdentifierMap = Context.getPatientSetService().getPatientIdentifierStringsByType(singlePatientCohort, Context.getPatientService().getPatientIdentifierTypeByName(Constants.IDENTIFIER_TYPE));
			
			Patient patient = (Patient) person;
			
			PID pid = adt.getPID();
			//PatientIdentifier patientIdentifier = patient.getPatientIdentifier(Constants.IDENTIFIER_TYPE);
			
			pid.getSetIDPID().setValue(Integer.toString(1));
			pid.getPatientIdentifierList(0).getIDNumber().setValue(Integer.toString(patient.getId()));
			pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(Constants.IDENTIFIER_TYPE);
			
			pid.getPatientName(0).getFamilyName().getSurname().setValue(patient.getFamilyName());
			pid.getPatientName(0).getGivenName().setValue(patient.getGivenName());
			pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(patient.getMiddleName());
			
			// dob
			Date dob = person.getBirthdate();
			String dobStr = "";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			dobStr = sdf.format(dob);
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);
			pid.getAdministrativeSex().setValue(patient.getGender());
			
			Set<PersonAddress> addresses = patient.getAddresses();
			
			int i = 0;
			for (PersonAddress address : addresses) {
				pid.getPatientAddress(i).getStreetAddress().getStreetName().setValue(address.getAddress1());
				pid.getPatientAddress(i).getCity().setValue(address.getCityVillage());
				pid.getPatientAddress(i).getZipOrPostalCode().setValue(address.getPostalCode());
				pid.getPatientAddress(i).getStateOrProvince().setValue(address.getStateProvince());
				i = i + 1;
			}
			
			PersonAttribute personAttribute = patient.getAttribute(Constants.TEL);
			if (personAttribute != null)
				pid.getPhoneNumberHome(0).getTelephoneNumber().setValue(personAttribute.getValue());
			
			personAttribute = person.getAttribute(Constants.SSN);
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
