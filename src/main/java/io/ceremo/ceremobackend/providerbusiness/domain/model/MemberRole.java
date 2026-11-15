package io.ceremo.ceremobackend.providerbusiness.domain.model;

/**
 * OWNER marks the accountable owner's own team row. It is created only with the business and can never be
 * granted through an invitation; MANAGER and FINANCE are invited roles.
 */
public enum MemberRole { OWNER, MANAGER, FINANCE }
