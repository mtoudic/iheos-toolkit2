
import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.client.TransactionType
import gov.nist.toolkit.sitemanagement.client.TransactionBean
import gov.nist.toolkit.sitemanagement.client.TransactionBean.RepositoryType
import gov.nist.toolkit.sitemanagement.client.TransactionCollection
import org.junit.Test

import static org.junit.Assert.*

class TransactionCollectionTest {

	///////////////////////////////////////////////////////////
	@Test
	public void testEqualsTransactionCollection() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			false,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		TransactionBean b1 = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			false,
			false)
		TransactionCollection tc1 = new TransactionCollection(false)
		tc1.addTransaction(b1)
		
		assertTrue tc.equals(tc1)
	}

	@Test
	public void testNotEqualsTransactionCollection() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		TransactionBean b1 = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			false,
			false)
		TransactionCollection tc1 = new TransactionCollection(false)
		tc1.addTransaction(b1)
		
		assertFalse tc.equals(tc1)
	}

	@Test
	public void testFixTlsEndpoints() {
		String origEndpoint = 'http://fooo:40/bar' 
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			origEndpoint,
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		
		TransactionBean b2 = tc.find(TransactionType.REGISTER, true, false)
		assertTrue b2 != null
		String endpoint = b2.getEndpoint();
		assertTrue endpoint != null
		assertEquals origEndpoint, endpoint
		tc.fixTlsEndpoints()
		String fixedEndpoint = b2.getEndpoint()
		assertTrue fixedEndpoint != null
		assertFalse endpoint == fixedEndpoint
		assertTrue fixedEndpoint.startsWith('https')
		assertFalse endpoint.startsWith('https')
	}

	///////////////////////////////////////////////////////////
	@Test
	public void testContains() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			false,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		assertTrue tc.contains(b)
	}

	@Test
	public void testAddTransaction() {
		// testContains() handles this
	}

	@Test
	public void testSize() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			false,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		assertEquals tc.size(), 1
	}

	@Test
	public void testHasActor() {
		String origEndpoint = 'http://fooo:40/bar' 
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			origEndpoint,
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		assertTrue tc.hasActor(ActorType.REGISTRY)
		assertFalse tc.hasActor(ActorType.REPOSITORY)
	}

	@Test
	public void testHasTransaction() {
		String origEndpoint = 'http://fooo:40/bar' 
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			origEndpoint,
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		
		assertTrue tc.hasTransaction(TransactionType.REGISTER)
		assertFalse tc.hasTransaction(TransactionType.RETRIEVE)
	}

	@Test
	public void testFindTransactionTypeBooleanBoolean() {
		String origEndpoint = 'http://fooo:40/bar' 
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			origEndpoint,
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)

		assertTrue null != tc.find(TransactionType.REGISTER, true, false)
		assertTrue null == tc.find(TransactionType.REGISTER, false, false)
	}

	@Test
	public void testFindStringBooleanBoolean() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)
		
		TransactionBean b2 = tc.find(TransactionType.REGISTER, true, false)
		assertTrue b2 != null
	}

	@Test
	public void testFindAll() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)

		List<TransactionBean> tb = tc.findAll("Register", true, false)		
		assertEquals 1, tb.size()
		tb = tc.findAll("Stored Query", true, false)
		assertEquals 0, tb.size()
	}

	@Test
	public void testGetTransactionTypeBooleanBoolean() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)

		assertEquals 'http://fooo:40/bar', tc.get(TransactionType.REGISTER, true, false)
	}

	@Test
	public void testGetStringBooleanBoolean() {
		TransactionBean b = new TransactionBean(TransactionType.REGISTER,
			RepositoryType.NONE,
			'http://fooo:40/bar',
			true,
			false)
		TransactionCollection tc = new TransactionCollection(false)
		tc.addTransaction(b)

		assertEquals 'http://fooo:40/bar', tc.get("Register", true, false)
	}

	@Test
	public void testAdd() {
		TransactionCollection tc = new TransactionCollection(false)
		tc.add("Register", 'http://fooo:40/bar', true, false)
		assertEquals 'http://fooo:40/bar', tc.get("Register", true, false)
	}

}
