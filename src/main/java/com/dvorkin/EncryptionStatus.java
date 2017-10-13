package com.dvorkin;

/**
 * Created by dvorkin on 12.10.2017.
 */
public enum EncryptionStatus
{
    LOGICAL_ERROR, //impossible combination, should never appear
    POTENTIAL_MIRACLE, //should not happen, but potentially indicates all is OK. A miracle happen?
    DECRYPTION_ERROR, //indicates that decryption went wrong
    PROVED_NO_RECOVERY, //File is encrypted and no recovery exists
    ENCRYPTION_ERROR, //encryption went wrong
    OK, //everything seem ok
    MIRACLE_DUPLICATE, //potentially duplicates file. No restore needed
    NOT_ENCRYPTED, //File was not encrypted at all
    OK_DUPLICATE, //ok but do not delete original, remove duplicate if it is really duplicate
    OK_DIFFERENT_FILE //ok but do not delete original
}
