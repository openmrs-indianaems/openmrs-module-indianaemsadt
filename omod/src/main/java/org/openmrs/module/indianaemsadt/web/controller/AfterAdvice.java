package org.openmrs.module.indianaemsadt.web.controller;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.context.Context;
import org.springframework.aop.AfterReturningAdvice;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.GenericParser;

public class AfterAdvice implements AfterReturningAdvice {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private int count = 0;
	
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals("savePatient")) {
			log.debug("Method: " + method.getName() + ". After advice called " + (++count) + " time(s) now.");
			Patient patient = (Patient) returnValue;
			
			ADT_A01 adt = generateADT(patient);
			String resp = getMessage(adt);
			System.out.println(resp);
			
			Obs o = new Obs();
			
			Concept concept = Context.getConceptService().getConcept(162725);
			o.setPerson(patient);
			//o.setEncounter(new Encounter());
			o.setConcept(concept);
			o.setDateCreated(new Date());
			o.setCreator(Context.getAuthenticatedUser());
			o.setObsDatetime(new Date());
			o.setValueText(resp);
			Context.getObsService().saveObs(o, "ADT");
			
		}
	}
	
	public String getMessage(ADT_A01 adt) {
		GenericParser parser = new GenericParser();
		String msg = null;
		try {
			msg = parser.encode(adt, "XML");
		}
		catch (HL7Exception e) {}
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
			msh.getSendingFacility().getNamespaceID()
			        .setValue(Context.getAdministrationService().getGlobalProperty("rheashradapter.sendingFaculty"));
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
			pid.getPatientName(0).getGivenName().setValue("waka waka");
			//PatientIdentifier patientIdentifier = patient.getPatientIdentifier(Constants.IDENTIFIER_TYPE);
			
			//pid.getPatientIdentifierList(0).getIDNumber().setValue(patientIdentifier.getIdentifier());
			//pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(Constants.IDENTIFIER_TYPE);
			
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
			
			//PersonAttribute personAttribute = person.getAttribute(Constants.TEL);
			//pid.getPhoneNumberHome(0).getTelephoneNumber().setValue(personAttribute.getValue());
			
			//personAttribute = person.getAttribute(Constants.SSN);
			//pid.getPid19_SSNNumberPatient().setValue(personAttribute.getValue());
			adt.getPV1();
		}
		catch (Exception e) {
			
		}
		return adt;
	}
	
}
