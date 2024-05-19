/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.Date;

/**
 *
 * @author brian
 */
public class HistoryEntityFactory {
    
    private static HistoryEntity newHistory(UserAccountEntity userAccount, String action, String fieldName, String oldValue, String newValue) {
        final HistoryEntity history = new HistoryEntity();
        history.setCreatorName(userAccount.getUserName());
        history.setCreationDate(new Date());
        history.setAction(action);
        history.setField(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        return history;
    }

    /**
     * Create a History object
     *
     */
    public static HistoryEntity add(UserAccountEntity userAccount, ConceptNameEntity conceptName) {
        return newHistory(userAccount, HistoryEntity.ACTION_ADD, HistoryEntity.FIELD_CONCEPTNAME, null, conceptName.getName());
    }

    /**
     *
     * @param userAccount The UserAccount for the user adding a concept.
     * @param concept The concept that is being added
     * @return A History object representing a new Concept. This History object
     *  should be added to the parent of the Concept supplied as an argument
     */
    public static HistoryEntity add(UserAccountEntity userAccount, ConceptEntity concept) {
        return newHistory(userAccount, HistoryEntity.ACTION_ADD, HistoryEntity.FIELD_CONCEPT_CHILD, null,  concept.getPrimaryConceptName().getName());
    }

    public static HistoryEntity add(UserAccountEntity userAccount, LinkRealizationEntity linkRealization) {
        return newHistory(userAccount, HistoryEntity.ACTION_ADD, HistoryEntity.FIELD_LINKREALIZATION, null, linkRealization.stringValue());
    }

    public static HistoryEntity add(UserAccountEntity userAccount, LinkTemplateEntity linkTemplate) {
        return newHistory(userAccount, HistoryEntity.ACTION_ADD, HistoryEntity.FIELD_LINKTEMPLATE, null, linkTemplate.stringValue());
    }

    public static HistoryEntity add(UserAccountEntity userAccount, MediaEntity media) {
        return newHistory(userAccount, HistoryEntity.ACTION_ADD, HistoryEntity.FIELD_MEDIA, null, media.getUrl());
    }



    public static HistoryEntity delete(UserAccountEntity userAccount, ConceptNameEntity conceptName) {
        return newHistory(userAccount, HistoryEntity.ACTION_DELETE, HistoryEntity.FIELD_CONCEPTNAME, conceptName.getName(), null);
    }

    /**
     * When deleting a Concept the History object should be added to the parent
     * of the Concept you are deleting.
     */
    public static HistoryEntity delete(UserAccountEntity userAccount, ConceptEntity concept) {
        return newHistory(userAccount, HistoryEntity.ACTION_DELETE, HistoryEntity.FIELD_CONCEPT_CHILD, concept.getPrimaryConceptName().getName(), null);
    }

    public static HistoryEntity delete(UserAccountEntity userAccount, LinkRealizationEntity linkRealization) {
        return newHistory(userAccount, HistoryEntity.ACTION_DELETE, HistoryEntity.FIELD_LINKREALIZATION, linkRealization.stringValue(), null);
    }

    public static HistoryEntity delete(UserAccountEntity userAccount, LinkTemplateEntity linkTemplate) {
        return newHistory(userAccount, HistoryEntity.ACTION_DELETE, HistoryEntity.FIELD_LINKTEMPLATE, linkTemplate.stringValue(), null);
    }

    public static HistoryEntity delete(UserAccountEntity userAccount, MediaEntity media) {
        return newHistory(userAccount, HistoryEntity.ACTION_DELETE, HistoryEntity.FIELD_MEDIA, media.getUrl(), null);
    }

    public static HistoryEntity replaceParentConcept(UserAccountEntity userAccount, ConceptEntity oldParent, ConceptEntity newParent) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_PARENT,
                oldParent.getPrimaryConceptName().getName(), newParent.getPrimaryConceptName().getName());
    }

    public static HistoryEntity replaceOriginator(UserAccountEntity userAccount, String oldOrig, String newOrig ) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_ORIGINATOR, oldOrig, newOrig);
    }

    public static HistoryEntity replaceRankName(UserAccountEntity userAccount, String oldRankName, String newRankName) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_RANKNAME, oldRankName, newRankName);
    }

    public static HistoryEntity replaceRankLevel(UserAccountEntity userAccount, String oldRankLevel, String newRankLevel) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_RANKLEVEL, oldRankLevel, newRankLevel);
    }

    public static HistoryEntity replaceStructureType(UserAccountEntity userAccount, String oldSt, String newSt) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_STRUCTURETYPE, oldSt, newSt);
    }

    public static HistoryEntity replaceReference(UserAccountEntity userAccount, String oldRef, String newRef) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_REFERENCE, oldRef, newRef);
    }

    public static HistoryEntity replaceConceptName(UserAccountEntity userAccount, ConceptNameEntity oldName, ConceptNameEntity newName) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPTNAME, oldName.stringValue(), newName.stringValue());
    }

    public static HistoryEntity replacePrimaryConceptName(UserAccountEntity userAccount, ConceptNameEntity oldName, ConceptNameEntity newName) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPTNAME_PRIMARY, oldName.getName(), newName.getName());
    }

    public static HistoryEntity replaceNodcCode(UserAccountEntity userAccount, String oldCode, String newCode) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_CONCEPT_NODCCODE, oldCode, newCode);
    }

    public static HistoryEntity replaceLinkTemplate(UserAccountEntity userAccount, LinkTemplateEntity oldValue, LinkTemplateEntity newValue) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_LINKTEMPLATE, oldValue.stringValue(), newValue.stringValue());
    }

    public static HistoryEntity replaceLinkRealization(UserAccountEntity userAccount, LinkRealizationEntity oldValue, LinkRealizationEntity newValue) {
        return newHistory(userAccount, HistoryEntity.ACTION_REPLACE, HistoryEntity.FIELD_LINKREALIZATION, oldValue.stringValue(), newValue.stringValue());
    }

}


