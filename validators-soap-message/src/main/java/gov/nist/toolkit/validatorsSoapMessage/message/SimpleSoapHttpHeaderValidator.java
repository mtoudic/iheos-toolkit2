package gov.nist.toolkit.validatorsSoapMessage.message;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.http.HttpHeader;
import gov.nist.toolkit.http.HttpParserBa;
import gov.nist.toolkit.http.ParseException;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.validatorsSoapMessage.factories.SoapMessageValidatorFactory;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.AbstractMessageValidator;
import gov.nist.toolkit.valsupport.registry.RegistryValidationInterface;

/**
 * Validate SIMPLE SOAP message. The input (an HTTP stream) has already been parsed
 * and the headers are in a HttpParserBa class and the body in a byte[]. This 
 * validator only evaluates the HTTP headers. Validation of the body is passed
 * off to CommonMessageValidatorFactory.
 * @author bill
 *
 */
public class SimpleSoapHttpHeaderValidator extends AbstractMessageValidator {
	HttpParserBa hparser;
	ErrorRecorderBuilder erBuilder;
	MessageValidatorEngine mvc;
	byte[] bodyBytes;
	String charset = null;
	RegistryValidationInterface rvi;
	TestSession testSession;

	public SimpleSoapHttpHeaderValidator(ValidationContext vc, HttpParserBa hparser, byte[] body, ErrorRecorderBuilder erBuilder, MessageValidatorEngine mvc, RegistryValidationInterface rvi, TestSession testSession) {
		super(vc);
		this.hparser = hparser;
		this.erBuilder = erBuilder;
		this.mvc = mvc;
		this.rvi = rvi;
		this.bodyBytes = body;
		this.testSession = testSession;
	}

	public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
		this.er = er;
		er.registerValidator(this);

		er.challenge("Validate SIMPLE SOAP HTTP headers");

		String contentTypeString = hparser.getHttpMessage().getHeader("content-type");
		try {
			HttpHeader contentTypeHeader = new HttpHeader(contentTypeString);
			String contentTypeValue = contentTypeHeader.getValue();
			if (contentTypeValue == null) contentTypeValue = "";
			if (!"application/soap+xml".equals(contentTypeValue.toLowerCase()))
                er.error("??", "Content-Type header", contentTypeValue, "application/soap+xml","http://www.w3.org/TR/soap12-part0 - Section 4.1.2");
            else
                er.success("??", "Content-Type header", contentTypeValue, "application/soap+xml", "http://www.w3.org/TR/soap12-part0 - Section 4.1.2");
            //err("Content-Type header must have value application/soap+xml - found instead " + contentTypeValue,"http://www.w3.org/TR/soap12-part0 - Section 4.1.2");

			charset = contentTypeHeader.getParam("charset");
			if (charset == null || charset.equals("")) {
				charset = "UTF-8";
				er.report("No message CharSet found in Content-Type header - using default", charset);
			} else {
				er.report("Message CharSet", charset);
			}

//			String body = new String(bodyBytes, charset);
			vc.isSimpleSoap = true;
			vc.hasSoap = true;

//			er.detail("Scheduling validation of SOAP content");
            er.sectionHeading("SOAP Message");
			SoapMessageValidatorFactory.getValidatorContext(erBuilder, bodyBytes, mvc, "Validate SOAP", vc, rvi, testSession);

		} catch (ParseException e) {
			err(e);
//		} catch (UnsupportedEncodingException e) {
//			err(e);
		}
		finally {
			er.unRegisterValidator(this);
		}

	}
	
	void err(String msg, String ref) {
		er.err(XdsErrorCode.Code.NoCode, msg, this, ref);
	}
	
	void err(Exception e) {
		er.err(XdsErrorCode.Code.NoCode, e);
	}

	@Override
	public boolean isSystemValidator() { return false; }


}
