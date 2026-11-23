package org.example.learningprojectserver.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.UserDto;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.projection.UserCredentialsProjection;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.LoginResponse;
import org.example.learningprojectserver.response.RegisterResponse;
import org.example.learningprojectserver.response.ResetPasswordResponse;
import org.example.learningprojectserver.utils.EmailSender;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.learningprojectserver.utils.ApiEmailProcessor.sendEmail;
import static org.example.learningprojectserver.utils.GeneratorUtils.*;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;
    private final ActiveUserService activeUserService;
    private final AuthTokenService authTokenService;
    private final EmailSender emailSender;
    private final StudentRepository studentRepository;
    private static final ConcurrentHashMap<String, Map<String,String>> otpLoginStorage = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> otpStorage = new ConcurrentHashMap<>();
    /** userId -> time (ms) the password step last succeeded. sendOtp/verifyOtp require a fresh entry. */
    private static final ConcurrentHashMap<String, Long> passwordVerifiedAt = new ConcurrentHashMap<>();
    private static final long PASSWORD_VERIFIED_TTL_MS = 5 * 60 * 1000;
    private static final long OTP_TTL_MS = 2 * 60 * 1000;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final String OTP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int OTP_LENGTH = 5;
    /** Every login and password-reset code goes to this inbox (SMS is no longer used). */
    static final String OTP_EMAIL = "shai27133@gmail.com";
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    public RegisterResponse createUser(String username,String userId, String password, String confirmPassword, String email, String phone) {
//todo להוסיף פונקציה שבודקת תקינות סיסמא
     RegisterResponse registerResponse=new RegisterResponse();
     if (username == null || userId==null || password == null || confirmPassword == null || email == null || phone == null) {
         registerResponse.setMessage("השדות לא מלאים");
         registerResponse.setSuccess(false);
         return registerResponse;
     }

     if (!password.equals(confirmPassword)) {
         registerResponse.setPasswordDontMatch("סיסמאות לא זהות");
         registerResponse.setSuccess(false);
         return registerResponse;
     }

     if (!isValid(userId, phone, email, registerResponse)) {
         registerResponse.setSuccess(false);
         return registerResponse;
     }

     try {


         String salt = generateSalt();
         String hashedPassword = hashPassword(password, salt);

         StudentEntity newUser = new StudentEntity();
         newUser.setRole(Role.STUDENT);
         newUser.setUserId(userId);
         newUser.setUsername(username);
         newUser.setEmail(email);
         newUser.setPhoneNumber(phone);
         newUser.setSalt(salt);
         newUser.setPasswordHash(hashedPassword);

         StudentQuestionHistoryEntity studentQuestionHistoryEntity = new StudentQuestionHistoryEntity();
         StudentProgressEntity studentProgressEntity = new StudentProgressEntity();

         studentQuestionHistoryEntity.setStudent(newUser);
         studentProgressEntity.setStudent(newUser);

         newUser.setStudentProgressEntity(studentProgressEntity);
         newUser.setQuestionHistoryEntity(studentQuestionHistoryEntity);

         String textToSendInMailToUser = generateMailText(username);
         sendEmail(email, "ברוך הבא ל-'מרכז תרגול אישי' – החשבון שלך נוצר בהצלחה!", textToSendInMailToUser);

         userRepository.save(newUser);
         System.out.println(newUser);


         registerResponse.setMessage("User successfully registered");
         registerResponse.setSuccess(true);
         return registerResponse;
     } catch (Exception e) {
         registerResponse.setMessage("Error occurred during registration");
         registerResponse.setSuccess(false);
         e.printStackTrace();
         return registerResponse;
     }
 }
    private boolean isValid(String userId, String phoneNumber, String email, RegisterResponse registerResponse) {
        boolean isValid = true;
         UserEntity user= userRepository.findUserByUserId(userId);
        if (user!=null){
           registerResponse.setIdTaken("תעודת זהות זו כבר קיימת במערכת");
            isValid = false;
        }
//        if (!isValidIsraeliId(userId)){
//            registerResponse.setIdTaken("תעודת זהות זו לא תקינה");
//            isValid = false;
//        }
        if (!isValidPhoneNumber(phoneNumber)){
            registerResponse.setPhoneTaken("מיספר טלפון זה לא תקין");

            isValid = false;
        }
        if(userRepository.existsByPhoneNumber(phoneNumber)){
            registerResponse.setPhoneTaken("טלפון זה תפוס");

        }
        if (userRepository.existsByEmail(email)){
            registerResponse.setEmailTaken("אימייל זה תפוס");

            isValid = false;
        }
        if (!isValidEmail(email)){
            registerResponse.setEmailTaken("אימייל לא תקין");

            isValid = false;
        }

        return isValid;
    }

//    @PostConstruct
//    public void init() {
//        UserEntity u=userRepository.findUserByUserId("325256017");
//        System.out.println("/////////////////////////////");
//        StudentEntity s=studentRepository.findStudentByStudentId("325256017");
//    }



    public BasicResponse loginUser(String userId, String password) {
        BasicResponse basicResponse = new BasicResponse();

        if (userId == null || password == null) {
            basicResponse.setSuccess(false);
            basicResponse.setErrorCode("יש להזין שם משתמש וסיסמה");
            return basicResponse;
        }

        try {

            UserCredentialsProjection userCredentialsProjection=userRepository.findBasicCredentialsByUserId(userId);


            if (userCredentialsProjection.getSalt() == null) {
                basicResponse.setSuccess(false);
                basicResponse.setErrorCode("שם משתמש או סיסמה אינם נכונים");
                return basicResponse;
            }
            String hashedPassword = hashPassword(password, userCredentialsProjection.getSalt());
            if (!hashedPassword.equals(userCredentialsProjection.getPasswordHash())) {
                basicResponse.setSuccess(false);
                basicResponse.setErrorCode("הסיסמה שגויה");
                return basicResponse;
            }

            passwordVerifiedAt.put(userId, System.currentTimeMillis());
            basicResponse.setSuccess(true);
            basicResponse.setErrorCode("");
            basicResponse.setData(userCredentialsProjection.getPhoneNumber());
            return basicResponse;

        } catch (Exception e) {
            basicResponse.setSuccess(false);
            basicResponse.setErrorCode("אירעה שגיאה במהלך ההתחברות");
            return basicResponse;
        }
    }
    /** True when loginUser succeeded for this userId within PASSWORD_VERIFIED_TTL_MS. */
    private static boolean passwordRecentlyVerified(String userId) {
        Long at = userId == null ? null : passwordVerifiedAt.get(userId);
        if (at == null) return false;
        if (System.currentTimeMillis() - at > PASSWORD_VERIFIED_TTL_MS) {
            passwordVerifiedAt.remove(userId);
            return false;
        }
        return true;
    }

    static String generateLoginOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(OTP_ALPHABET.charAt(SECURE_RANDOM.nextInt(OTP_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Sends the login OTP by email to {@link #OTP_EMAIL}. Requires a password login that
     * succeeded in the last few minutes. The client's phoneNumber is ignored.
     */
    public BasicResponse sendOtp(String userId, String phoneNumber) {
        BasicResponse basicResponse = new BasicResponse();

        if (!passwordRecentlyVerified(userId)) {
            basicResponse.setSuccess(false);
            basicResponse.setErrorCode("יש להתחבר עם סיסמה לפני שליחת קוד אימות");
            return basicResponse;
        }
        try {
            UserCredentialsProjection credentials = userRepository.findBasicCredentialsByUserId(userId);
            if (credentials == null) {
                basicResponse.setSuccess(false);
                basicResponse.setErrorCode("משתמש לא נימצא");
                return basicResponse;
            }
            String otp = generateLoginOtp();

            Map<String, String> otpDetails = new ConcurrentHashMap<>();
            otpDetails.put("OTP", otp);
            otpDetails.put("timestamp", String.valueOf(System.currentTimeMillis()));
            otpDetails.put("attempts", "0");

            otpLoginStorage.put(userId, otpDetails);

            emailSender.send(OTP_EMAIL, "קוד אימות לכניסה – " + userId,
                    "קוד האימות לכניסה של משתמש " + userId + " הוא: " + otp + "\nהקוד תקף ל-2 דקות.");

            basicResponse.setSuccess(true);
            basicResponse.setErrorCode("OTP sent");
            return basicResponse;

        } catch (Exception e) {
            basicResponse.setSuccess(false);
            basicResponse.setErrorCode("אירעה שגיאה בשליחת קוד האימות");
            return basicResponse;
        }
    }




        public LoginResponse verifyOtp(String userId, String otp) {
            LoginResponse loginResponse = new LoginResponse();
            if (userId == null || otp == null) {
                loginResponse.setSuccess(false);
                loginResponse.setErrorCode("יש לספק שם משתמש וקוד אימות");
                return loginResponse;
            }
            if (!passwordRecentlyVerified(userId)) {
                otpLoginStorage.remove(userId);
                loginResponse.setSuccess(false);
                loginResponse.setErrorCode("יש להתחבר עם סיסמה מחדש");
                return loginResponse;
            }
            try {
                Map<String, String> otpDetails = otpLoginStorage.get(userId);
                if (otpDetails == null || otpDetails.get("OTP") == null) {
                    loginResponse.setSuccess(false);
                    loginResponse.setErrorCode("לא נשלח קוד אימות או שפג תוקפו");
                    return loginResponse;
                }

                long otpTimestamp = Long.parseLong(otpDetails.get("timestamp"));
                if (System.currentTimeMillis() - otpTimestamp > OTP_TTL_MS) {
                    otpLoginStorage.remove(userId);
                    loginResponse.setSuccess(false);
                    loginResponse.setErrorCode("עברו 2 דקות");
                    return loginResponse;
                }

                if (!otpDetails.get("OTP").equalsIgnoreCase(otp.trim())) {
                    int attempts = Integer.parseInt(otpDetails.getOrDefault("attempts", "0")) + 1;
                    if (attempts >= MAX_OTP_ATTEMPTS) {
                        otpLoginStorage.remove(userId);
                        passwordVerifiedAt.remove(userId);
                        loginResponse.setSuccess(false);
                        loginResponse.setErrorCode("יותר מדי ניסיונות שגויים. יש להתחבר מחדש");
                        return loginResponse;
                    }
                    otpDetails.put("attempts", String.valueOf(attempts));
                    loginResponse.setSuccess(false);
                    loginResponse.setErrorCode("קוד האימות שגוי");
                    return loginResponse;
                }

                UserEntity userEntity = userRepository.findUserByUserId(userId);
                if (userEntity == null) {
                    loginResponse.setSuccess(false);
                    loginResponse.setErrorCode("המשתמש לא נמצא");
                    return loginResponse;
                }

                otpLoginStorage.remove(userId);
                passwordVerifiedAt.remove(userId);
                loginResponse.setSuccess(true);
                loginResponse.setErrorCode("הקוד נכון");
                loginResponse.setTokens(authTokenService.issue(userId, userEntity.getUsername(), String.valueOf(userEntity.getRole())));
                userEntity.setOtp(null);
                userEntity.setOtpTimestamp(null);
                Session session = new Session();
                session.setUser(userEntity);
                session.setLastActivity(new Date());
                activeUserService.connectUser(userId);
                userEntity.getSessionList().add(session);

                userRepository.save(userEntity);
                return loginResponse;

            } catch (Exception e) {

                loginResponse.setSuccess(false);
                loginResponse.setErrorCode("אירעה שגיאה במהלך אימות הקוד");
                return loginResponse;
            }
        }
    public ResetPasswordResponse sendPasswordResetOtp(String userId) {
        ResetPasswordResponse resetPasswordResponse= new ResetPasswordResponse();
        Long id = userRepository.findIdByUserId(userId);
        if (id == null) {
            resetPasswordResponse.setSuccess(false);
            resetPasswordResponse.setUserIdError("משתמש לא נמצא");
            return resetPasswordResponse;
        }

        String otp = generatorCode();
        otpStorage.put(userId, otp);

       emailSender.send(OTP_EMAIL, "קוד לאיפוס סיסמה – " + userId,
               "קוד איפוס הסיסמה של משתמש " + userId + " הוא: " + otp);
            resetPasswordResponse.setSuccess(true);
        return resetPasswordResponse;
    }

    public ResetPasswordResponse resetPassword(String userId, String otp, String newPassword) {
       ResetPasswordResponse resetPasswordResponse= new ResetPasswordResponse();
        String subject = "הסיסמה שלך שונתה בהצלחה";
        String message = "שלום,\n\nסיסמתך שונתה בהצלחה. אם לא ביצעת שינוי זה, אנא צור קשר עם התמיכה שלנו באופן מיידי.\n\nבברכה,\nצוות התמיכה";

        if(!otpStorage.containsKey(userId)){
             resetPasswordResponse.setSuccess(false);
            resetPasswordResponse.setUserIdError("תז לא תקין");
            return resetPasswordResponse;

        }
        if (!otpStorage.get(userId).equals(otp)) {
            resetPasswordResponse.setSuccess(false);
            resetPasswordResponse.setOtpError("קוד האימות שגוי או שפג תוקפו.");
            return resetPasswordResponse;
        }

        if(!isPasswordStrong(newPassword)){
            resetPasswordResponse.setSuccess(false);
            resetPasswordResponse.setPasswordError("הסיסמא חייבת לכלול לפחות אות גדולה וקטנה, ו-6 תווים לפחות");
            return resetPasswordResponse;
        }

        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            resetPasswordResponse.setSuccess(false);
            resetPasswordResponse.setUserIdError("תז לא נמצא.");
            return resetPasswordResponse;
        }
        String userSalt = user.getSalt();
        String hashPassword=hashPassword(newPassword,userSalt);
        user.setPasswordHash(hashPassword);
        userRepository.save(user);
        otpStorage.remove(userId);
        authTokenService.revokeAllForUser(userId);
        sendEmail(user.getEmail(), subject, message);
        resetPasswordResponse.setSuccess(true);
        return resetPasswordResponse;
    }



}



