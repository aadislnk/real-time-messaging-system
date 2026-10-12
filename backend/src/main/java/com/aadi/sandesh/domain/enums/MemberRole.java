package com.aadi.sandesh.domain.enums;

public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER;

        public  boolean canManageMembers(){
            return this == OWNER || this == ADMIN;
        }
}
