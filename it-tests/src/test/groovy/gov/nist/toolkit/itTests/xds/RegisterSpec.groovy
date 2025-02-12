package gov.nist.toolkit.itTests.xds

import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.server.SimulatorActorType
import gov.nist.toolkit.installation.server.Installation
import gov.nist.toolkit.installation.shared.TestSession
import gov.nist.toolkit.itTests.support.ToolkitSpecification
import gov.nist.toolkit.results.client.Result
import gov.nist.toolkit.results.client.TestInstance
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.SimIdFactory
import gov.nist.toolkit.testengine.scripts.BuildCollections
import gov.nist.toolkit.toolkitApi.SimulatorBuilder
import spock.lang.Shared
/**
 * Test the Register transaction
 */
class RegisterSpec extends ToolkitSpecification {
    @Shared SimulatorBuilder spi


    @Shared String urlRoot = String.format("http://localhost:%s/xdstools2", remoteToolkitPort)
    @Shared String patientId = 'BR14^^^&1.2.360&ISO'
    @Shared String patientId2 = 'BR15^^^&1.2.360&ISO'
    @Shared String testSession = prefixNonce('bill')
    @Shared String reg = testSession + '__reg'
    @Shared SimId simId = SimIdFactory.simIdBuilder(reg)

    def setupSpec() {   // one time setup done when class launched
        startGrizzly('8889')

        // Initialize remote api for talking to toolkit on Grizzly
        // Needed to build simulators
        spi = getSimulatorApi(remoteToolkitPort)

        // local customization

        new BuildCollections().init(null)

        spi.delete('reg', testSession)

        spi.create(
                'reg',
                testSession,
                SimulatorActorType.REGISTRY,
                'test')
    }

    def cleanupSpec() {  // one time shutdown when everything is done
        spi.delete('reg', testSession)
        spi.delete('reg', 'test')
        api.deleteSimulatorIfItExists(simId)
//        server.stop()
//        ListenerFactory.terminateAll()
    }

    def setup() {
        println "EC is ${Installation.instance().externalCache().toString()}"
        println "${api.getSiteNames(true, new TestSession(testSession))}"
        api.createTestSession(testSession)
        if (!api.simulatorExists(simId)) {
            println "Creating sim ${simId}"
            api.createSimulator(ActorType.REGISTRY, simId)
        }
    }

    // submits the patient id configured above to the registry in a Patient Identity Feed transaction
    def 'Submit Pid transaction to Registry simulator'() {
        when:
        String siteName = testSession + '__reg'
        TestInstance testId = new TestInstance("15804")
        List<String> sections = new ArrayList<>()
        sections.add("section")
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        boolean stopOnFirstError = true

        and: 'Run pid transaction test'
        List<Result> results = api.runTest(testSession, siteName, testId, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

//    def 'Run all Register tests'() {
//        when:
//        String siteName = 'bill__reg'
//        TestInstance testId = new TestInstance("tc:R.b")
//        List<String> SECTIONS = new ArrayList<>()
//        Map<String, String> params = new HashMap<>()
//        params.put('$patientid$', patientId)
//        boolean stopOnFirstError = true
//
//        and: 'Run'
//        List<Result> results = api.runTest(testSession, siteName, testId, SECTIONS, params, stopOnFirstError)
//
//        then:
//        true
//        results.size() == 1
//        results.get(0).passed()
//    }


    def 'Run a failed test'() {
        when:
        String siteName = testSession + '__reg'
        TestInstance testId = new TestInstance("11993")
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        boolean stopOnFirstError = true

        and: 'Run'
        List<Result> results = api.runTest(testSession, siteName, testId, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

    def 'Run a submit with namespace tests'() {
        when:
        String siteName = testSession + '__reg'
        TestInstance testId = new TestInstance("regsubmit")
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', patientId)
        params.put('$ssid$',"1.42.20161129094708.61")
        boolean stopOnFirstError = true

        and: 'Run'
        List<Result> results = api.runTest(testSession, siteName, testId, sections, params, stopOnFirstError)

        then:
        true
        results.size() == 1
        results.get(0).passed()
    }

}
