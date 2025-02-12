package gov.nist.toolkit.fhir.simulators.sim.reg.sq;

import gov.nist.toolkit.errorrecording.client.XdsErrorCode.Code;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrysupport.logging.LoggerException;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.DocEntry;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.MetadataCollection;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.RegIndex;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.GetDocuments;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.QueryReturnType;
import gov.nist.toolkit.valregmsg.registry.storedquery.support.StoredQuerySupport;
import gov.nist.toolkit.xdsexception.client.MetadataException;
import gov.nist.toolkit.xdsexception.client.XdsException;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;

import java.util.ArrayList;
import java.util.List;

public class GetDocumentsSim extends GetDocuments {
	RegIndex ri;

	public void setRegIndex(RegIndex ri) {
		this.ri = ri;
	}


	public GetDocumentsSim(StoredQuerySupport sqs) {
		super(sqs);
	}

	protected Metadata runImplementation() throws MetadataException,
	XdsException, LoggerException {

		MetadataCollection mc = ri.getMetadataCollection();

		Metadata m = new Metadata();
		m.setVersion3();
		
		if (mc.vc.updateEnabled && !(metadataLevel == null || metadataLevel.equals("1") || metadataLevel.equals("2"))) {
			sqs.er.err(Code.XDSRegistryError, "Do not understand $MetadataLevel = " + metadataLevel, this, "ITI TF-2b: 3.18.4.1.2.3.5.1");
			return new Metadata();
		} 

		if (uuids != null) {
			List<DocEntry> des = new ArrayList<>();
			for (String uuid : uuids) {
				DocEntry de = mc.docEntryCollection.getById(uuid);
				if (de != null) {
					des.add(de);
				}
			}

			List<String> filteredUuidList = new ArrayList<>();
			for (DocEntry de : des) {
				filteredUuidList.add(de.getId());
			}

			if (!filteredUuidList.isEmpty()) {
				if (sqs.returnType == QueryReturnType.LEAFCLASS || sqs.returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT) {
					m = mc.loadRo(filteredUuidList);
				} else {
					m.mkObjectRefs(filteredUuidList);
				}
			}
		} else if (uids != null) {
			List<DocEntry> des = new ArrayList<DocEntry>();
			for (String uid : uids) {
				des.addAll(mc.docEntryCollection.getByUid(uid));
			}

			if (!isMetadataLevel2())
				des = mc.docEntryCollection.filterByLatestVersion(des);

			m = asUuids(mc, m, des);
		} else if (lids != null) {
			if (!mc.vc.updateEnabled) {
				sqs.er.err(Code.XDSRegistryError, "Do not understand parameter $XDSDocumentEntryLogicalID", this, "ITI TF-2b: 3.18.4.1.2.3.7.5");
				return new Metadata();
			}
			if (metadataLevel == null || metadataLevel.equals("1")) {
				sqs.er.err(Code.XDSRegistryError, "$XDSDocumentEntryLogicalID cannot be specified with $MetadataLevel = 1", this, "ITI TF-2b: 3.18.4.1.2.3.7.5");
				return new Metadata();
			}
			
			List<DocEntry> des = new ArrayList<DocEntry> ();
			for (String lid : lids) {
				des.addAll(mc.docEntryCollection.getByLid(lid));
			}
			m = asUuids(mc, m, des);

		}

		return m;
	}

	private Metadata asUuids(MetadataCollection mc, Metadata m, List<DocEntry> des) throws MetadataException, XdsInternalException {
		List<String> uuidList = new ArrayList<String>();
		for (DocEntry de : des) {
			uuidList.add(de.getId());
		}
		if (sqs.returnType == QueryReturnType.LEAFCLASS || sqs.returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT) {
			m = mc.loadRo(uuidList);
		} else {
			m.mkObjectRefs(uuidList);
		}
		return m;
	}

}
