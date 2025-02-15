package gov.nist.toolkit.toolkitServices;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.errorrecording.TextErrorRecorder;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.errorrecording.factories.TextErrorRecorderBuilder;
import gov.nist.toolkit.fhir.simulators.sim.cons.DocConsActorSimulator;
import gov.nist.toolkit.fhir.simulators.sim.idc.ImgDocConsActorSimulator;
import gov.nist.toolkit.fhir.simulators.sim.src.XdrDocSrcActorSimulator;
import gov.nist.toolkit.fhir.simulators.support.StoredDocument;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymetadata.MetadataParser;
import gov.nist.toolkit.registrymsg.registry.AdhocQueryResponse;
import gov.nist.toolkit.registrymsg.registry.AdhocQueryResponseParser;
import gov.nist.toolkit.registrymsg.repository.*;
import gov.nist.toolkit.services.server.RegistrySimApi;
import gov.nist.toolkit.services.server.RepositorySimApi;
import gov.nist.toolkit.services.server.ToolkitApi;
import gov.nist.toolkit.simcommon.client.*;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simcommon.server.SimDb;
import gov.nist.toolkit.simcommon.server.SimPropertyTypeConflictException;
import gov.nist.toolkit.soap.DocumentMap;
import gov.nist.toolkit.toolkitServicesCommon.*;
import gov.nist.toolkit.toolkitServicesCommon.resource.*;
import gov.nist.toolkit.toolkitServicesCommon.resource.xdm.XdmItem;
import gov.nist.toolkit.toolkitServicesCommon.resource.xdm.XdmReportResource;
import gov.nist.toolkit.toolkitServicesCommon.resource.xdm.XdmRequestResource;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.valregmsg.xdm.OMap;
import gov.nist.toolkit.valregmsg.xdm.XdmDecoder;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.DefaultValidationContextFactory;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import org.apache.axiom.om.OMElement;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Path("/simulators")
public class SimulatorsController {
    ToolkitApi api;

    static Logger logger = Logger.getLogger(SimulatorsController.class.getName());

    public SimulatorsController() {
        api = ToolkitApi.forServiceUse();

        // This is commented out because when running inside Jetty there is a maximum
        // header size.  If you hit it with TRACING ALL you will see the error in the Jetty logs
        // header full: java.lang.RuntimeException: Header>6144
        // note this is also set in web.xml
//        ResourceConfig resourceConfig = new ResourceConfig(SimulatorsController.class);
//        resourceConfig.property(ServerProperties.TRACING, "ALL");
    }
    
    @POST
    @Produces("application/json")
    @Path("/{id}/xdsi/retrieve/{type}")
    public Response retrieveImagingDocSet(final RetImgDocSetReqResource request, @PathParam("type") String transaction) {
        logger.info(String.format("POST simulators/%s/xdsi/retrieve/%s", 
           (request.isDirect() ? request.getEndpoint() : request.getFullId()), transaction));
        String dest = "";
        TransactionType type = TransactionType.RET_IMG_DOC_SET;
        if (transaction.equals("rad69Iig")) type = TransactionType.RET_IMG_DOC_SET_GW;
        if (transaction.equals("rad75")) type = TransactionType.XC_RET_IMG_DOC_SET;
        try {
            
            // Transfer from request resource to request model
            
            RetrieveImageRequestModel rModel = new RetrieveImageRequestModel();            
            for (RetImgDocSetReqStudy eRequest : request.getRetrieveImageStudyRequests()) {
               RetrieveImageStudyRequestModel eModel = new RetrieveImageStudyRequestModel();
               eModel.setStudyInstanceUID(eRequest.getStudyInstanceUID());
               rModel.addStudyRequest(eModel);
               for (RetImgDocSetReqSeries sRequest: eRequest.getRetrieveImageSeriesRequests()) {
                  RetrieveImageSeriesRequestModel sModel = new RetrieveImageSeriesRequestModel();
                  sModel.setSeriesInstanceUID(sRequest.getSeriesInstanceUID());
                  eModel.addSeriesRequest(sModel);
                  for (RetImgDocSetReqDocument dRequest : sRequest.getRetrieveImageDocumentRequests()) {
                     RetrieveItemRequestModel dModel = new RetrieveItemRequestModel();
                     dModel.setRepositoryId(dRequest.getRepositoryUniqueId());
                     dModel.setDocumentId(dRequest.getDocumentUniqueId());
                     dModel.setHomeId(dRequest.getHomeCommunityId());
                     sModel.addDocumentRequest(dModel);
                  } // EO document request loop
               } // EO series request loop
            } // EO study request loop
            
            for (String xferSyntax : request.getTransferSyntaxUIDs())
               rModel.addTransferSyntaxUID(xferSyntax);

            // Trigger simulator to do the retrieve

            ImgDocConsActorSimulator sim = new ImgDocConsActorSimulator();
            sim.setTransactionType(type);
            sim.setTls(request.isTls());
            sim.setMessageDir(request.getMessageDir());
            sim.setDirect(request.isDirect());
            if (request.isDirect()) {
               dest = "direct";
               sim.setEndpoint(request.getEndpoint());
            }
            else {
               sim.setSite(api.getActorConfig(request.getFullId()));
               dest = request.getId();
            }
            RetrievedDocumentsModel sModel = sim.retrieve(rModel);
            
            // Transfer from response model to response resource
            
            RetImgDocSetRespResource rsp = new RetImgDocSetRespResource();
            rsp.setAbbreviatedResponse(sModel.getAbbreviatedMessage());
            
            List<RetImgDocSetRespDocumentResource> dRsps = new ArrayList<>();
            for ( RetrievedDocumentModel dModel : sModel.getMap().values()) {
               RetImgDocSetRespDocumentResource dRsp = new RetImgDocSetRespDocumentResource();
               dRsp.setDocumentUid(dModel.getDocUid());
               dRsp.setRepositoryUid(dModel.getRepUid());
               dRsp.setHomeCommunityUid(dModel.getHome());
               dRsp.setDocumentContents(dModel.getContents());
               dRsp.setMimeType(dModel.getContent_type());
               dRsps.add(dRsp);
            }
            rsp.setDocuments(dRsps);
            
            return Response.ok(rsp).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, dest, ResponseType.RESPONSE);
        }
    }

    @Context
    private UriInfo _uriInfo;

    /**
     * Create new simulator with default settings.
     * @param simIdResource - Simulator ID
     * @return
     *     Status.OK if successful
     *     Status.BAD_REQUEST if Simulator ID is invalid
     *     Status.INTERNAL_SERVER_ERROR if necessary
     *     Simulator config if successful
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(final SimIdResource simIdResource) {
        SimId simId = ToolkitFactory.asServerSimId(simIdResource);
        logger.info(String.format("Create simulator %s", simId.toString()));
        try {
            String errors = simId.validateState();
            if (errors != null)
                throw new BadSimConfigException(String.format("Create simulator %s - %s", simId.toString(), errors));
            Simulator simulator = api.createSimulator(simId);
            SimConfigResource bean = ToolkitFactory.asSimConfigBean(simulator.getConfig(0));
            Response r = Response
                    .ok(bean)
                    .header("Location",
                            String.format("%s/%s", _uriInfo.getAbsolutePath().toString(),
                                    simId.getId()))
                    .build();
            return r;
        }
        catch (Exception e) {
           logger.warning(e.getMessage());
           return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
        }
    }

    enum PropType {STRING, BOOLEAN, LIST};
    PropType propType(SimulatorConfigElement config) {
        if (config.hasList()) return PropType.LIST;
        if (config.hasBoolean()) return PropType.BOOLEAN;
        if (config.hasString()) return PropType.STRING;
        return null;
    }

    PropType propType(SimConfigResource res, String name) {
        if (res.isList(name)) return PropType.LIST;
        if (res.isBoolean(name)) return PropType.BOOLEAN;
        if (res.isString(name)) return PropType.STRING;
        return null;
    }

    static List<String> UPDATE_EXCEPTIONS = new ArrayList<>();
    static {
        UPDATE_EXCEPTIONS.add(SimulatorProperties.environment);
    }

    /**
     * Update Simulator Configuration.
     * @param config containing updates
     * @return accepted (202) and full updated config if changes actually made, notModified (304) and no body if no
     * actual changes made, Conflict (409) if boolean/String type is wrong on a property.
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("{id}")
    public Response update(final SimConfigResource config) {
        logger.info(String.format("Update request for %s", config.getFullId()));
        SimId simId = null;
        simId = ToolkitFactory.asServerSimId(config);
        try {
            SimulatorConfig currentConfig = api.getConfig(simId);
            if (currentConfig == null) throw new NoSimException("");

            boolean makeUpdate = false;
            for (String propName : config.getPropertyNames()) {
                SimulatorConfigElement ele = currentConfig.get(propName);
                if (ele == null) continue;  // no such property
                if (!ele.isEditable() && !UPDATE_EXCEPTIONS.contains(propName)) {
                    continue;  // ignore
                }

                PropType currentType = propType(ele);
                PropType updateType = propType(config, propName);
                if (currentType != updateType)
                    throw new SimPropertyTypeConflictException(propName, currentType.name(), updateType.name());

                if (propType(ele) == PropType.BOOLEAN) {
                    if (ele.asBoolean() == config.asBoolean(propName)) continue;  // no change
                    if (!makeUpdate)  // first update
                        logger.info(String.format("...property %s", propName));
                    makeUpdate = true;
                    logger.info(String.format("......%s ==> %s", ele.asBoolean(), config.asBoolean(propName)));
                    ele.setBooleanValue(config.asBoolean(propName));
                }
                else if (propType(ele) == PropType.STRING) {
                    if (ele.asString().equals(config.asString(propName))) continue;  // no change
                    if (!makeUpdate)  // first update
                        logger.info(String.format("...property %s", propName));
                    makeUpdate = true;
                    logger.info(String.format("%s ==> %s", ele.asString(), config.asString(propName)));
                    ele.setStringValue(config.asString(propName));
                }
                else if (propType(ele) == PropType.LIST) {
                    if (listCompare(ele.asList(), config.asList(propName))) continue; // no change
                    if (!makeUpdate)  // first update
                        logger.info(String.format("...property %s", propName));
                    makeUpdate = true;
                    logger.info(String.format("%s ==> %s", ele.asString(), config.asString(propName)));
                    ele.setStringListValue(config.asList(propName));
                }
            }
//            if (config.getPatientErrorMap() != null) {
//                SimulatorConfigElement ele = currentConfig.get(SimulatorProperties.errorForPatient);
//                if (ele != null) {
//                    ele.setBooleanValue(config.getPatientErrorMap());
//                }
//            }
            if (makeUpdate) {
                logger.info(String.format("Updating Sim %s", config.getFullId()));
                api.saveSimulator(currentConfig);
                SimConfigResource bean = ToolkitFactory.asSimConfigBean(currentConfig);
                logger.info("Returning updated bean");
                return Response.accepted(bean).build();
            } else
                return Response.notModified().build();
        } catch (Throwable e) {
            logger.severe(ExceptionUtil.exception_details(e));
            return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
        }
    }

    boolean listCompare(List<String> a, List<String> b) {
        Set<String> aSet = new HashSet<>(a);
        Set<String> bSet = new HashSet<>(b);
        return a.equals(b);
    }

    /**
     * Delete simulator with
     * This cannot be used with FHIR since the FHIR'ness of it is not passed
     * @param id
     * @return
     */
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String id) {
        logger.info("Delete " + id);
        try {
            SimId simId = SimDb.simIdBuilder(id);
            api.deleteSimulatorIfItExists(simId);
        }
        catch (Throwable e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.THROW);
        }
        return Response.status(Response.Status.OK).build();
    }


    @POST
    @Path("/_delete")
    @Consumes("application/json")
    @Produces("application/json")
    public Response deleteFhir(final SimIdResource simIdResource) {
        logger.info("Delete " + simIdResource.asSimId());
        try {
            SimId simId = ToolkitFactory.asServerSimId(simIdResource);
//            if (simIdResource.isFhir()) simId.forFhir();
            api.deleteSimulatorIfItExists(simId);
        }
        catch (Throwable e) {
            return new ResultBuilder().mapExceptionToResponse(e, simIdResource.getId(), ResponseType.THROW);
        }
        return Response.status(Response.Status.OK).build();
    }


    /**
     * Get full SimId given id
     * @param id
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Response getSim(@PathParam("id") String id) {
        logger.info("GET simulators/" +  id);
        try {
            SimId simId = SimDb.simIdBuilder(id);
            SimulatorConfig config = api.getConfig(simId);
            if (config == null) throw new NoSimException("");
            SimConfigResource bean = ToolkitFactory.asSimConfigBean(config);
            return Response.ok(bean).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    /**
     * Get ids for all DocumentEntries for patient id
     * @param id Simulator ID
     * @param pid Patient ID
     * @return DocumentEntry.ids
     */
    @GET
    @Produces("application/json")
    @Path("/{id}/xds/GetAllDocs/{pid}")
    public Response getAllDocs(@PathParam("id") String id, @PathParam("pid") String pid) {
        logger.info(String.format("GET simulators/%s/xds/GetAllDocs/%s", id, pid));
        try {
            SimId simId = SimIdFactory.simIdBuilder(id);
            RegistrySimApi api = new RegistrySimApi(simId);
            List<String> objectRefs = api.findDocsByPidObjectRef(pid);
            RefListResource or = new RefListResource();
            or.setRefs(objectRefs);
            return Response.ok(or).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    @GET
    @Produces("application/xml")
    @Path("/{id}/xds/GetDoc/{docId}")
    public Response getDoc(@PathParam("id") String id, @PathParam("docId") String docId) {
        logger.info(String.format("GET simulators/%s/xds/GetDoc/%s", id, docId));
        try {
            SimId simId = SimDb.simIdBuilder(id);
            RegistrySimApi api = new RegistrySimApi(simId);
            OMElement ele = api.getDocEle(docId);
            String xml = new OMFormatter(ele).toString();
            return Response.ok(xml).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}/events/{transaction}")
    public Response getEventIds(@PathParam("id") String id, @PathParam("transaction") String transaction) {
        logger.info(String.format("GET simulators/%s/events", id));
        try {
            SimId simId = SimDb.simIdBuilder(id);
            List<String> eventIds = api.getSimulatorEventIds(simId, transaction);
            RefListResource resource = new RefListResource();
            resource.setRefs(eventIds);
            return Response.ok(resource).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}/document/{uniqueid}")
    public Response getDocument(@PathParam("id") String id, @PathParam("uniqueid") String uniqueId) {
        logger.info(String.format("GET simulators/%s/document/%s", id, uniqueId));
        try {
            SimId simId = SimDb.simIdBuilder(id);
            DocumentContentResource resource = new DocumentContentResource();
            RepositorySimApi repoApi = new RepositorySimApi(simId);
            StoredDocument document = repoApi.getDocument(uniqueId);
            if (document == null) throw new NoContentException("Document " + uniqueId);
            resource.setContent(document.getContent());
            resource.setUniqueId(uniqueId);
            return Response.ok(resource).build();
        } catch (Throwable e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}/event/{transaction}/{eventid}")
    public Response getEvent(@PathParam("id") String id, @PathParam("transaction") String transaction, @PathParam("eventid") String eventid) {
        logger.info(String.format("GET simulators/%s/event/%s/%s", id, transaction, eventid));
        try {
            SimId simId = SimDb.simIdBuilder(id);
            String event = api.getSimulatorEvent(simId, transaction, eventid);
            RefListResource resource = new RefListResource();
            resource.addRef(event);
            return Response.ok(resource).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, id, ResponseType.RESPONSE);
        }
    }

    @POST
    @Produces("application/json")
    @Path("/{id}/xds/retrieve")
    public Response retrieve(final RetrieveRequestResource request) {
        logger.info(String.format("POST simulators/%s/xds/retrieve", request.getFullId()));
        SimulatorConfig config;
        SimId simId = ToolkitFactory.asServerSimId(request);
        logger.info("simid is " + simId);
        RetrieveResponseResource returnResource = new RetrieveResponseResource();
        try {
            config = api.getConfig(simId);
            if (config == null) throw new NoSimException("");

            // Build internal model from external request resource
            RetrieveRequestModel iModel = new RetrieveRequestModel();
            RetrieveItemRequestModel rModel = new RetrieveItemRequestModel();
            rModel.setHomeId(request.getHomeCommunityId());
            rModel.setRepositoryId(request.getRepositoryUniqueId());
            rModel.setDocumentId(request.getDocumentUniqueId());
            iModel.add(rModel);

            // Trigger simulator to do the retrieve
            DocConsActorSimulator sim = new DocConsActorSimulator();
            sim.setTls(request.isTls());
            RetrievedDocumentsModel sModel = sim.retrieve(config, iModel);

            // Package results for return
            RetrievedDocumentModel m = sModel.getMap().values().iterator().next();
            returnResource.setDocumentContents(m.getContents());
            returnResource.setMimeType(m.getContent_type());

            return Response.ok(returnResource).build();
        } catch (Exception e) {
        return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
    }
}

    @POST
    @Produces("application/json")
    @Path("/{id}/xds/QueryForLeafClass")
    public Response queryForLeafClass(final StoredQueryRequestResource request) {
        logger.info(String.format("POST simulators/%s/xds/QueryForLeafClass", request.getFullId()));
        SimulatorConfig config;
        SimId simId = ToolkitFactory.asServerSimId(request);
        logger.info("simid is " + simId);
        try {
            config = api.getConfig(simId);
            if (config == null) throw new NoSimException("");
            String queryId = request.getQueryId();
            String queryName = MetadataSupport.getSQName(queryId);
            logger.info("Query is " + queryName);
            if (queryName.equals(""))
                throw new BadSimRequestException("Do not understand query ID " + queryId);
            DocConsActorSimulator sim = new DocConsActorSimulator();

            gov.nist.toolkit.fhir.simulators.sim.cons.QueryParameters queryParameters =
                    new gov.nist.toolkit.fhir.simulators.sim.cons.QueryParameters();
            queryParameters.addParameter(request.getKey1(), request.getValues1());
            queryParameters.addParameter(request.getKey2(), request.getValues2());
            queryParameters.addParameter(request.getKey3(), request.getValues3());
            queryParameters.addParameter(request.getKey4(), request.getValues4());
            queryParameters.addParameter(request.getKey5(), request.getValues5());
            queryParameters.addParameter(request.getKey6(), request.getValues6());
            queryParameters.addParameter(request.getKey7(), request.getValues7());
            queryParameters.addParameter(request.getKey8(), request.getValues8());
            queryParameters.addParameter(request.getKey9(), request.getValues9());
            queryParameters.addParameter(request.getKey10(), request.getValues10());
            queryParameters.addParameter(request.getKey11(), request.getValues11());
            queryParameters.addParameter(request.getKey12(), request.getValues12());
            queryParameters.addParameter(request.getKey13(), request.getValues13());
            queryParameters.addParameter(request.getKey14(), request.getValues14());
            queryParameters.addParameter(request.getKey15(), request.getValues15());
            queryParameters.addParameter(request.getKey16(), request.getValues16());
            queryParameters.addParameter(request.getKey17(), request.getValues17());
            queryParameters.addParameter(request.getKey18(), request.getValues18());
            queryParameters.addParameter(request.getKey19(), request.getValues19());
            queryParameters.addParameter(request.getKey20(), request.getValues20());

            OMElement responseEle = sim.query(config, queryId, queryParameters, true, request.isTls());
//            OMElement responseEle = sim.query(config, queryId, QueryParametersManager.internalize(request.getQueryParameters()), true, request.isTls());
//            logger.info(new OMFormatter(responseEle).toString());
            Metadata metadata = MetadataParser.parseNonSubmission(responseEle);
            List<OMElement> objects = metadata.getMajorObjects();
            LeafClassRegistryResponseResource returnResource = new LeafClassRegistryResponseResource();
            for (OMElement e : objects) {
                returnResource.addLeafClass(new OMFormatter(e).toString());
            }

            AdhocQueryResponse adhocQueryResponse = new AdhocQueryResponseParser(responseEle).getResponse();
            returnResource.setStatus(ResponseStatusType.getStatus(adhocQueryResponse.getStatus()));
            List<RegistryErrorResource> errors = new ArrayList<RegistryErrorResource>();
            for (gov.nist.toolkit.registrymsg.registry.RegistryError error : adhocQueryResponse.getRegistryErrorList()) {
                RegistryErrorResource error1 = new RegistryErrorResource();
                error1.setErrorCode(error.errorCode);
                error1.setErrorContext(error.codeContext);
                error1.setLocation(error.location);
                error1.setStatus((error.isWarning) ? ResponseStatusType.WARNING : ResponseStatusType.ERROR);
                errors.add(error1);
            }
            returnResource.setErrorList(errors);

            return Response.ok(returnResource).build();
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}/xdr")
    public Response xdr(final RawSendRequestResource request)  {
        logger.info(String.format("XDR Send request for %s", request.getFullId()));
        SimId simId = null;
        SimulatorConfig config;
        simId = ToolkitFactory.asServerSimId(request);
        logger.info("simid is " + simId);
        try {
            config = api.getConfig(simId);
            if (config == null) throw new NoSimException("");
        } catch (Exception e) {
            return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
        }

        try {
            TransactionType transactionType = ActorType.XDR_DOC_SRC.getTransaction(request.getTransactionName());
            if (transactionType == null)
                throw new BadSimConfigException(String.format("Do not understand transaction %s", request.getTransactionName()));
            XdrDocSrcActorSimulator sim = new XdrDocSrcActorSimulator();
            if (request.getMetadata() == null)
                throw new BadSimRequestException("No message body provided in request.");
            OMElement messageBody = Util.parse_xml(request.getMetadata());
            sim.setMessageBody(messageBody);
            sim.setDocumentMap(internalizeDocs(request));
            for (String extraHeader : request.getExtraHeaders()) {
                OMElement ele = Util.parse_xml(extraHeader);
                sim.addSoapHeaderElement(ele);
            }
            OMElement responseEle = sim.run(
                    config,
                    transactionType,
                    internalizeDocs(request),
                    request.isTls(),
                    config.get(SimulatorProperties.environment).asString()
            );
            RawSendResponseResource responseResource = new RawSendResponseResource();
            responseResource.setResponseSoapBody(new OMFormatter(responseEle).toString());
            return Response.ok(responseResource).build();
        } catch (Throwable e) {
            return new ResultBuilder().mapExceptionToResponse(e, simId.toString(), ResponseType.RESPONSE);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/xdmValidation")
    public Response xdmValidation(final XdmRequestResource request) {
        ValidationContext vc = DefaultValidationContextFactory.validationContext();
        vc.isXDM = true;
        ErrorRecorderBuilder erBuilder = new TextErrorRecorderBuilder();
        TextErrorRecorder er = (TextErrorRecorder) erBuilder.buildNewErrorRecorder();
        MessageValidatorEngine mvc = new MessageValidatorEngine();

        XdmDecoder xd = new XdmDecoder(vc, erBuilder, Io.bytesToInputStream(request.getZip()));
        xd.er = er;

        if (!xd.isXDM()) {
            XdmReportResource report = new XdmReportResource();
            report.setReport(er.toString());
            return Response.ok(report).build();
        }
        xd.run(er, mvc);
        OMap omap = xd.getContents();
        XdmReportResource report = new XdmReportResource();
        report.setReport(er.toString());
        report.setPass(!er.hasErrors());
        for (gov.nist.toolkit.valregmsg.xdm.Path path : omap.keySet()) {
            report.addItem(new XdmItem(path.toString(), omap.get(path).get()));
        }

        return Response.ok(report).build();
    }

    DocumentMap internalizeDocs(RawSendRequestResource request) throws BadSimRequestException {
        DocumentMap map = new DocumentMap();

        for (String id : request.getDocuments().keySet()) {
            Document requestDoc = request.getDocuments().get(id);
            gov.nist.toolkit.soap.Document storedDoc = new gov.nist.toolkit.soap.Document();
            if (requestDoc.getMimeType() == null)
                throw new BadSimRequestException("Null mimeType not acceptable.");
            if (requestDoc.getContents() == null)
                throw new BadSimRequestException("Null contents not acceptable.");
            storedDoc.setMimeType(requestDoc.getMimeType());
            storedDoc.setContents(requestDoc.getContents());
            map.addDocument(id, storedDoc);
        }

        return map;
    }


}


