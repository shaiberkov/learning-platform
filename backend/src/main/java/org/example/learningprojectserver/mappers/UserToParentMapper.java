package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.entities.ParentEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserToParentMapper implements Mapper<UserEntity, ParentEntity> {

    @Override
    public ParentEntity apply(UserEntity user) {
        ParentEntity parent = new ParentEntity();
        parent.setUserId(user.getUserId());
        parent.setUsername(user.getUsername());
        parent.setPassword(user.getPassword());
        parent.setPasswordConfirm(user.getPasswordConfirm());
        parent.setPasswordHash(user.getPasswordHash());
        parent.setSalt(user.getSalt());
        parent.setPhoneNumber(user.getPhoneNumber());
        parent.setEmail(user.getEmail());
        parent.setOtp(user.getOtp());
        parent.setOtpTimestamp(user.getOtpTimestamp());
        parent.setProfilePicture(user.getProfilePicture());
        parent.setRole(Role.PARENT);
        return parent;
    }
}
