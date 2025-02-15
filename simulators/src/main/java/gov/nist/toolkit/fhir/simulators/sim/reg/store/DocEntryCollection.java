package gov.nist.toolkit.fhir.simulators.sim.reg.store;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.fhir.simulators.sim.reg.mu.AcceptableUpdate;
import gov.nist.toolkit.valregmsg.registry.SQCodeAnd;
import gov.nist.toolkit.valregmsg.registry.SQCodeOr;
import gov.nist.toolkit.valregmsg.registry.SQCodedTerm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DocEntryCollection extends RegObCollection implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private List<DocEntry> entries;
	
	transient public DocEntryCollection parent = null;

	public List<DocEntry> getAll() {
		List<DocEntry> all = new ArrayList<>();

		all.addAll(entries);

		DocEntryCollection theParent = parent;
		while (theParent != null) {
			all.addAll(theParent.getAll2(idsBeingDeleted()));
			theParent = theParent.parent;
		}

		List<String> deletedIds = idsBeingDeleted();

		List<DocEntry> deleted = new ArrayList<>();
		for (DocEntry de : entries) {
			if (deletedIds.contains(de.id))
				deleted.add(de);
		}

		all.removeAll(deleted);

		return all;
	}

	private List<DocEntry> getAll2(List<String> deletedIds) {
		List<DocEntry> all = new ArrayList<>();

		all.addAll(entries);

		DocEntryCollection theParent = parent;
		while (theParent != null) {
			all.addAll(theParent.getAll());
			theParent = theParent.parent;
		}

		List<DocEntry> deleted = new ArrayList<>();
		for (DocEntry de : entries) {
			if (deletedIds.contains(de.id))
				deleted.add(de);
		}

		all.removeAll(deleted);

		return all;
	}

	public List<DocEntry> getAllForUpdate() {
		return entries;
	}

	public List<DocEntry> getAllForDelete() {
		if (parent == null)
			return new ArrayList<>();
		return parent.entries;
	}
	
	public String toString() {
		return getAll().size() + " DocumentEntries";
	}
	
	public void init() {
		entries = new ArrayList<>();
	}
	
//	// caller handles synchronization
//	public void applyDelta(Delta d) {
//		if (d.idsToDelete != null)
//			for (String id : d.idsToDelete)
//				syncDelete(id);
//		
//		if (d.entriesToAdd != null) 
//			for (Object o : d.entriesToAdd) 
//				entries.add((DocEntry) o);
//		
//	}

	public boolean okForRMU(MetadataCollection mc, ErrorRecorder er) {
		boolean ok = true;

		List<DocEntry> allDe = entries;

		for (DocEntry de : allDe) {
			if (!AcceptableUpdate.acceptableRMU(de, mc, er))
				ok = false;
		}

		return ok;
	}

	// caller handles synchronization
	public boolean delete(String id) {
		boolean deleted = false;
		DocEntry toDelete = null;
		for (DocEntry a : getAllForDelete()) {
			if (a.id.equals(id)) {
				toDelete = a;
				break;
			}
		}
		if (toDelete != null) {
			getAllForDelete().remove(toDelete);
			deleted = true;
		}
		return deleted;
	}

		@Override
		public List<?> getNonDeprecated() {
			List<DocEntry> nonDep = new ArrayList<>();
			for (DocEntry a : getAll()) {
				if (!a.isDeprecated())
					nonDep.add(a);
			}
			if (parent != null)
				return parent.getNonDeprecated();
			return nonDep;
		}

	public int size() { return getAll().size(); }

	public String statsToString() {
		int siz = 0;
		if (parent != null)
			siz = parent.getAll().size();
		return (siz + getAll().size()) + " DocumentEntries";
	}

	public Ro getRo(String id) {
		for (DocEntry de : getAll()) {
			if (de.id.equals(id))
				return de;
		}
		if (parent == null)
			return null;
		return parent.getRo(id);
	}

	public DocEntry getById(String id) {
		if (id == null)
			return null;
		for (DocEntry de : getAll()) {
			if (id.equals(de.id))
				return de;
		}
		if (parent == null)
			return null;
		return parent.getById(id);
	}
	
	public List<DocEntry> getByUid(String uid) {
		List<DocEntry> des = new ArrayList<DocEntry>();
		if (uid == null)
			return des;
		for (DocEntry de : getAll()) {
			if (uid.equals(de.uid))
				des.add(de);
		}
		if (parent != null)
			des.addAll(parent.getByUid(uid));
		return des;
	}
	
	public List<DocEntry> getByLid(String lid) {
		List<DocEntry> des = new ArrayList<DocEntry>();
		if (lid == null)
			return des;
		for (DocEntry de : getAll()) {
			if (lid.equals(de.lid))
				des.add(de);
		}
		if (parent != null)
			des.addAll(parent.getByLid(lid));
		return des;
	}
	
	public DocEntry getLatestVersion(String lid) {
		List<DocEntry> des = getByLid(lid);
		
		DocEntry latest = null;
		int version = -1;
		
		for (DocEntry de : des) {
			if (de.version > version) {
				version = de.version;
				latest = de;
			}
		}
		
		return latest;
	}
	
	public DocEntry getPreviousVersion(String id) {
		DocEntry de = getById(id);
		return getPreviousVersion(de);
	}
	
	public DocEntry getPreviousVersion(DocEntry de) {
		int ver = de.version;
		String lid = de.lid;
		List<DocEntry> des = getByLid(lid);
		for (DocEntry d : des) {
			if (d.version == ver-1) 
				return d;
		}
		return null;
	}

	public boolean isMostRecentVersion(DocEntry de) {
		int ver = de.version;
		String lid = de.lid;
		List<DocEntry> des = getByLid(lid);
		for (DocEntry d : des) {
			if (d.version > ver)
				return false;
		}
		return true;
	}

	public List<DocEntry> findByPid(String pid) {
		List<DocEntry> results = new ArrayList<DocEntry>();
		
		for (int i=0; i<getAll().size(); i++) {
			DocEntry de = getAll().get(i);
			// pid == null handles MPQ
			if (pid != null && pid.equals(de.pid))
				results.add(de);
		}
		if (parent != null)
			results.addAll(parent.findByPid(pid));
		return results;
	}

	public List<DocEntry> filterByLatestVersion(List<DocEntry> des) {
		if (des.isEmpty()) return des;
		for (int i=0; i<des.size(); i++) {
			DocEntry d = des.get(i);
			if (isMostRecentVersion(d))
				continue;
			des.remove(i);
			i--;
		}
		return des;
	}

	public List<DocEntry> filterByStatus(List<StatusValue> statuses, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (statuses == null || statuses.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (statuses.contains(de.getAvailabilityStatus()))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public List<DocEntry> filterByAuthorPerson(List<String> authorPersons, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (authorPersons == null || authorPersons.isEmpty()) return docs;
		nextDoc:
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			for (String value : de.authorNames) {
				if (matchAnyAuthor(value, authorPersons))
					continue nextDoc;
			}
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	// _ match any character
	// % matches any string
	// this is too complicated to do with Java Regex ...
	static boolean matchAnyAuthor(String value, List<String> authors) {
		for (String author : authors) {
			if (matchAuthor(value, author))
				return true;
		}
		return false;
	}
	
	static boolean matchAuthor(String value, String pattern) {
		int vi = 0;
		
		for (int pi=0; pi<pattern.length(); pi++) {
			if (pattern.charAt(pi) == '_') {
				vi++;
			} else if (pattern.charAt(pi) == '%') {
				String after = getAfterText(pattern, pi);
				int afterI = value.indexOf(after, vi);
				if (afterI == -1)
					return false;
				vi = afterI;
			} else {
				if (pattern.charAt(pi) != value.charAt(vi))
					return false;
				vi++;
			}
			if (pi + 1 == pattern.length() && pattern.charAt(pi) == '%')
				return true;
			if (pattern.length() == pi+1 && value.length() == vi)
				return true;
			if (pi + 2 == pattern.length() && pattern.charAt(pi + 1) == '%')
				return true;
			if (value.length() == vi)
				return false;
		}
		
		return false;
	}
	
	// return text after % char at startAt and before next % (or end if no next %)
	// expect initial % to possibly be %% 
	static String getAfterText(String pattern, int startAt) {
		while (pattern.charAt(startAt) == '%') {
			startAt++;
			if (startAt == pattern.length())
				return "";
		}
		
		int endAt = startAt;
		
		while(pattern.charAt(endAt) != '%') {
			endAt++;
			if (endAt == pattern.length())
				return pattern.substring(startAt);
		}
		return pattern.substring(startAt, endAt);
	}

	public List<DocEntry> filterByReferenceId(List<String> refIds, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (refIds.isEmpty()) return docs;

		nextDoc:
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			for (String value : de.referenceIdList) {
				if (refIds.contains(value))
					continue nextDoc;
			}
			docs.remove(i);
			i--;
		}
		return docs;
	}



	public List<DocEntry> filterByCreationTime(String from, String to, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if ((from == null || from.equals("")) && (to == null || to.equals(""))) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			String creationTime = de.creationTime;
			if (timeCompare(creationTime, from, to))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	public List<DocEntry> filterByServiceStartTime(String from, String to, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if ((from == null || from.equals("")) && (to == null || to.equals(""))) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			String time = de.serviceStartTime;
			if (timeCompare(time, from, to))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	public List<DocEntry> filterByServiceStopTime(String from, String to, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if ((from == null || from.equals("")) && (to == null || to.equals(""))) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			String time = de.serviceStopTime;
			if (timeCompare(time, from, to))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	static public boolean timeCompare(String att, String from, String to) {
		if (att == null || att.equals(""))
			return false;
		if (from != null && !from.equals("")) {
			if ( !  (from.compareTo(att) <= 0))
				return false;
		}
		if (to != null && !to.equals("")) {
			if ( !  (att.compareTo(to) < 0)) 
				return false;
		}
		return true;
	}

	public List<DocEntry> filterByFormatCode(SQCodedTerm values, List<DocEntry> docs) throws Exception {
		if (values instanceof SQCodeOr) 
			return filterByFormatCode((SQCodeOr) values, docs);
		throw new Exception("DocEntryCollection#filterByFormatCode cannot decode " + values.getClass().getName());
	}
	
	public List<DocEntry> filterByFormatCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.isMatch(de.formatCode))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public List<DocEntry> filterByClassCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.isMatch(de.classCode))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	public List<DocEntry> filterByTypeCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.isMatch(de.typeCode))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public List<DocEntry> filterByPracticeSettingCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.isMatch(de.practiceSettingCode))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	public List<DocEntry> filterByHcftCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.isMatch(de.healthcareFacilityTypeCode))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public List<DocEntry> filterByEventCode(SQCodeAnd values, List<DocEntry> docs) {
		for (SQCodeOr ors : values.codeOrs) {
			docs = filterByEventCode(ors, docs);
		}
		return docs;
	}
	
	public List<DocEntry> filterByEventCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		eachdoc:
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			for (String val : de.eventCode) {
				if (values.isMatch(val))
					continue eachdoc;
			}
			docs.remove(i);
			i--;
		}
		return docs;
	}
	
	public List<DocEntry> filterByConfCode(SQCodeAnd values, List<DocEntry> docs) {
		for (SQCodeOr ors : values.codeOrs) {
			docs = filterByConfCode(ors, docs);
		}
		return docs;
	}
	
	public List<DocEntry> filterByConfCode(SQCodedTerm values, List<DocEntry> docs) throws Exception {
		if (values instanceof SQCodeOr) 
			return filterByConfCode((SQCodeOr) values, docs);
		throw new Exception("DocEntryCollection#filterByConfCode cannot decode SQCodedTerm");
	}
	


	public List<DocEntry> filterByConfCode(SQCodeOr values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		eachdoc:
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			for (String val : de.confidentialityCode) {
				if (values.isMatch(val))
					continue eachdoc;
			}
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public List<DocEntry> filterByObjectType(List<String> values, List<DocEntry> docs) {
		if (docs.isEmpty()) return docs;
		if (values == null || values.isEmpty()) return docs;
		eachdoc:
		for (int i=0; i<docs.size(); i++) {
			DocEntry de = docs.get(i);
			if (values.contains(de.objecttype))
				continue;
			docs.remove(i);
			i--;
		}
		return docs;
	}

	public boolean hasObject(String id) {
		if (id == null)
			return false;
		for (DocEntry a : getAll()) {
			if (a.id == null)
				continue;
			if (a.id.equals(id))
				return true;
		}
		if (parent == null)
			return false;
		return parent.hasObject(id);
	}
	
	public boolean hasObject(String id, List<DocEntry> docEntries) {
		for (DocEntry de : docEntries) {
			if (id.equals(de.getId()))
				return true;
		}
		return false;
	}

	public Ro getRoByUid(String uid) {
		for (DocEntry de : getAll()) {
			if (de.uid.equals(uid))
				return de;
		}
		if (parent == null)
			return null;
		return parent.getRoByUid(uid);
	}

	@Override
	public List<Ro> getRosByUid(String uid) {
		List<Ro> list = new ArrayList<>();
		for (DocEntry de : getAll()) {
			if (de.uid.equals(uid))
				list.add(de);
		}
		if (! list.isEmpty()) {
			return list;
		}
		if (parent == null)
			return null;
		return parent.getRosByUid(uid);
	}

	public List<?> getAllRo() {
		return getAll();
	}

	@Override
	public List<String> getIds() {
		List<String> ids = new ArrayList<>();
		for (DocEntry a : getAll()) ids.add(a.getId());
		return ids;
	}

    static public List<String> getIds(List<DocEntry> ros) {
        List<String> ids = new ArrayList<>();
        for (Ro r : ros) ids.add(r.getId());
        return ids;
    }

}
