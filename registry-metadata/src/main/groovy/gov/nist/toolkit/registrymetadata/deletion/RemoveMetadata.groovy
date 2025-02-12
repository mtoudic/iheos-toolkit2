package gov.nist.toolkit.registrymetadata.deletion

/**
 *
 */
class RemoveMetadata {
    List<Uuid> removeSet
    Registry r
    boolean muSupported = false

    /**
     * Does the object exist in the requested remove set
     * @param Uuid
     * @return
     */
    boolean request(Uuid id) { removeSet.contains(id)}

    /**
     * object not in Registry
     * @param id
     * @return
     */
    Response ruleNotInRegistry(Uuid id) {
        if (!r.exists(id))
            return new Response(ErrorType.UnresolvedReferenceException, id)
        return Response.NoError
    }

    /************************************************
     *
     * Rules to evaluate removal of Associations attached to Submission Sets
     *
     ************************************************/

    /**
     * Removing SS-DE HasMember without removing DE
     * @param id
     * @return
     */
    Response ruleDanglingSS_DE_HasMember(Uuid assn) {
        if (
            isHasMemberAttachedToSS(assn) &&
                r.isDE(r.target(assn)) &&
                !inRequest(r.target(assn)))
            return new Response(ErrorType.ObjectNotInDeletionSet, r.target(assn))
        return Response.NoError
    }

    /**
     * removing SS-FOL HasMember without removing Fol
     * @param fol
     * @return
     */
    Response ruleDanglingSS_Fol_HasMember(Uuid assn) {
        if (
            isHasMemberAttachedToSS(assn) &&
                r.isFol(r.target(assn)) &&
                !inRequest(r.target(assn)))
            return new Response(ErrorType.ObjectNotInDeletionSet, r.target(assn))
        return Response.NoError
    }

    /**
     * removing SS-ASSN HasMember without removing ASSN
     * @param fol
     * @return
     */
    Response ruleDanglingSS_ASSN_HasMember(Uuid assn) {
        if (
            isHasMemberAttachedToSS(assn) &&
                r.isASSN(r.target(assn)) &&
                !inRequest(r.target(assn)))
            return new Response(ErrorType.ObjectNotInDeletionSet, r.target(assn))
        return Response.NoError
    }

    /**
     * removing last Association referencing a Submission Set but not removing the Submission Set
     * @param assn
     * @return
     */
    Response ruleDanglingSS(Uuid assn) {
        if (
            isHasMemberAttachedToSS(assn) &&
                    !inRequest(r.source(assn)) &&
                    r.onlyAssn(r.source(assn), assn, AssnType.HasMember)
        )
            return new Response(ErrorType.LeavesEmptySubmissionSet, assn)
        return Response.NoError
    }

    /************************************************
     *
     * Rules to evaluate removal of Associations between Document Entries
     *
     ************************************************/

    /**
     * removing XFRM Association - sourceObject must be in deletion set
     * @param assn
     * @return
     */
    Response ruleDeletingXFRM(Uuid assn) {
        if (
            isDE_DE_Association(assn) &&
                    r.assnType(assn) == AssnType.XFRM &&
                    !inRequest(r.source(assn))
        )
            return new Response(ErrorType.ObjectNotInDeletionSet, r.source(assn))
        return Response.NoError
    }

//    /**
//     * removing APND Association - sourceObject must be in deletion set
//     * @param assn
//     * @return
//     */
//    Response ruleDeletingAPND(Uuid assn) {
//        if (
//            isDE_DE_Association(assn) &&
//                r.assnType(assn) == AssnType.APND &&
//                !inRequest(r.source(assn))
//        )
//            return new Response(ErrorType.ObjectNotInDeletionSet, r.source(assn))
//        return Response.NoError
//    }

    /**
     * removing SIGNS Association - sourceObject must be in deletion set
     * @param assn
     * @return
     */
    Response SIGNS(Uuid assn) {
        if (
            isDE_DE_Association(assn) &&
                r.assnType(assn) == AssnType.SIGNS &&
                !inRequest(r.source(assn))
        )
            return new Response(ErrorType.ObjectNotInDeletionSet, r.source(assn))
        return Response.NoError
    }

    /**
     * removing IsSnapshotOf Association - sourceObject must be in deletion set
     * @param assn
     * @return
     */
    Response ruleDeletingIsSnapshotOf(Uuid assn) {
        if (
            isDE_DE_Association(assn) &&
                r.assnType(assn) == AssnType.IsSnapshotOf &&
                !inRequest(r.source(assn))
        )
            return new Response(ErrorType.ObjectNotInDeletionSet, r.source(assn))
        return Response.NoError
    }

    Response ruleDeletingRPLCIsIllegalWithoutMU(Uuid assn) {
        if (
            !muSupported &&
                isDE_DE_Association(assn) &&
                    (r.assnType(assn) == AssnType.RPLC || r.assnType(assn) == AssnType.XFRM_RPLC)
        )
            return new Response(ErrorType.RPLCCannotBeDeleted, r.source(assn))
        return Response.NoError
    }

    /************************************************
     *
     * Rules to evaluate removal of Document Entries, Submission Sets,
     * and Folders
     *
     ************************************************/

    /**
     * does the DocumentEntry have any Associations pointing to it
     * that are not in the deletion set?
     * @param de
     * @return errors for linking associations not in deletion set
     */
    MultiResponse ruleDeletingDocumentEntry(Uuid de) {
        MultiResponse multiResponse = new MultiResponse()
        if ( r.isDE(de) ) {
            notInRequest(r.assnLinkedToDE(de)).each {
                multiResponse.add(ErrorType.ObjectNotInDeletionSet, it)
            }
            return multiResponse
        }
        return new MultiResponse(Response.NoError)
    }

    /**
     * does the SubmissionSet have any Associations pointing to it
     * that are not in the deletion set?
     * @param de
     * @return errors for linking associations not in deletion set
     */
    MultiResponse ruleDeletingSubmissionSet(Uuid ss) {
        MultiResponse multiResponse = new MultiResponse()
        if ( r.isSS(ss) ) {
            notInRequest(r.assnLinkedToSS(ss)).each {
                multiResponse.add(ErrorType.ObjectNotInDeletionSet, it)
            }
            return multiResponse
        }
        return new MultiResponse(Response.NoError)
    }

    /**
     * does the Folder have any Associations pointing to it
     * that are not in the deletion set?
     * @param de
     * @return errors for linking associations not in deletion set
     */
    MultiResponse ruleDeletingFolder(Uuid fol) {
        MultiResponse multiResponse = new MultiResponse()
        if ( r.isFol(fol) ) {
            notInRequest(r.assnLinkedToFol(fol)).each {
                multiResponse.add(ErrorType.ObjectNotInDeletionSet, it)
            }
            return multiResponse
        }
        return new MultiResponse(Response.NoError)
    }

    private boolean isDE_DE_Association(Uuid assn) {
        r.isASSN(assn) && r.isDE(r.source(assn)) && r.isDE(r.target(assn))
    }

    /**
     * is object an Association attached to a Submission set
     * @param assn
     * @return
     */
    private boolean isHasMemberAttachedToSS(Uuid assn) {
        r.isHasMember(assn) &&
                r.isSS(r.source(assn)) }

    /**
     * is the object in the requested remove set
     * @param id
     * @return
     */
    private boolean inRequest(Uuid id) { removeSet.contains(id) }

    /**
     * identify and return the IDs from ids that are not in removeSet
     * @param ids - ids to look for
     * @return - the IDs that are not in the request
     */
    private List<Uuid> notInRequest(List<Uuid> ids) {
        ids.findAll { !removeSet.contains(it) }
    }
}
