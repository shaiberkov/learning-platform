package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.Role;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "user_entity", indexes = {
        @Index(name = "idx_user_userId", columnList = "userId")
})
public abstract class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false)
    private String userId;
    @Transient
    private String password;
    @Transient
    private String passwordConfirm;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String salt;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String email;
    @Column(name = "otp")
    private String otp;
    @Column(name="otpTimestamp")
    private Long otpTimestamp;
//    @Column(nullable = false)
//    private int age;
    @Column(nullable = true)
    private String profilePicture;
    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY ,cascade = CascadeType.ALL)
    private List<Session> sessionList=new ArrayList<>();

    @OneToMany(mappedBy = "sender",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> receivedMessages = new ArrayList<>();


    // VARCHAR, not MySQL's native ENUM, so adding a role never needs the column type changed again.
    // Existing databases still hold an ENUM column: see docs/MIGRATIONS.md and DevSchemaPatcher.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Role role;

}





