package org.openmrs.module.indianaemsadt.web.controller;

/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

public class Constants {
	
	// Env settings
	
	public static final String HL7_URL_ENV_VARIABLE = "HL7_URL";
	
	public static final String DEFAULT_HL7_URL = "localhost";
	
	public static final String HL7_PORT_ENV_VARIABLE = "HL7_PORT";
	
	public static final int DEFAULT_HL7_PORT = 6661;
	
	// ADT observation
	
	public static final String ADT_OBS_CONCEPT_GLOBAL_PROPERTY = "indianaemsadt.adtObsConceptUuid";
	
	// Default to CIEL's General Patient Note concept that ships with RefApp
	public static final String DEFAULT_ADT_OBS_CONCEPT = "165095AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	
	// MSH
	
	public static final String FIELD_SEPARATOR = "|";
	
	public static final String ENCODING_CHARACTERS = "^~\\&";
	
	public static final String MESSAGE_TYPE = "ADT";
	
	public static final String TRIGGER_EVENT = "A01";
	
	public static final String MESSAGE_STRUCTURE = "ADT_A01";
	
	public static final String INTERNATIONALIZATION_CODE = "USA";
	
	public static final String VERSION = "2.5";
	
	public static final String PROCESSING_ID = "P";
	
	public static final String SENDING_FACILITY = "Sender";
	
	public static final String RECEIVING_FACILITY = "INPC";
	
	public static final String SENDING_APPLICATION = "REG";
	
	public static final String RECEIVING_APPLICATION = "receivingApplication";
	
	public static final String ACK_TYPE = "";
	
	public static final String APPLICATION_ACK_TYPE = "";
	
	public static final String MSG_PROFILE_IDENTIFIER = "CLSM_V0.83";
	
	//ORC
	
	public static final String ORDER_CONTROL = "RE";
	
	// PV1
	
	public static final String IDPV1 = "1";
	
	public static final String PATIENT_CLASS = "O";
	
	// PID
	
	public static final String IDENTIFIER_TYPE = "MR";
	
	public static final String IDPID = "1";
	
	public static final String ATTR_TELEPHONE_NUMBER = "attributeTelephoneNum";
	
	public static final String ATTR_NEXT_OF_KIN = "attributeNextOfKin";
	
	//OBR ENC
	
	public static final String PROVIDER_IDENTIFIER_TYPE = "EPID";
	
	// OBX
	
	public static final String RW_CS = "RW_CS";
	
	public static final String RW_CN = "RW_CN";
	
	public static final String RW_AC = "RW_AC";
	
	public static final String RW_AS = "RW_AS";
	
	public static final String NAME_OF_CODING_SYSTEM = "MHM";
	
	public static final String CONCEPT_DATATYPE_NUMERIC = "Numeric";
	
	public static final String CONCEPT_DATATYPE_DATE = "Date";
	
	public static final String CONCEPT_DATATYPE_DATETIME = "Datetime";
	
	public static final String CONCEPT_DATATYPE_TEXT = "Text";
	
	public static final String CONCEPT_DATATYPE_CODED = "Coded";
	
	public static final String CODING_SYSTEM = "UCUM";
	
	public static final String OBSERVATION_TYPE = "ObservationType";
	
	public static final String UNIT_CODING_SYSTEM = "ucum";
	
	public static final String SSN = "SSN";
	
	public static final String TEL = "Telephone Number";
	
	// OBR
	
	public static final String UNIV_SERVICE_ID = "univServiceId";
	
	public static final String UNIV_SERVICE_NAME = "univServIdName";
	
	public static final String OBR_CODE_SYSTEM = "codeSys";
	
	public static final String RESULT_STATUS = "resultStatus";
	
}
