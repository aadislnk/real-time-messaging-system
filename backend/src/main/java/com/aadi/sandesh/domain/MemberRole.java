package com.aadi.sandesh.domain;

public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER;

        public  boolean canManageMembers(){
            return this == OWNER || this == ADMIN;
        }
}
