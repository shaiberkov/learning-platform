package org.example.learningprojectserver.service.ai;

import org.example.learningprojectserver.enums.Role;

/** Who is calling the AI: their id, role and school (null for a system admin or an unassigned student). */
public record AiUser(String userId, Role role, String schoolCode) {

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isStaff() {
        return role == Role.TEACHER || role == Role.SCHOOLMANAGER || role == Role.SYSTEM_ADMIN;
    }
}
