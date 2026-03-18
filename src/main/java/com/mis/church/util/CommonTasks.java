package com.mis.church.util;

import com.mis.church.entity.Status;
import com.mis.church.repository.StatusRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonTasks {

    private final StatusRepo statusRepo;
    private static final String SECRET_KEY = "3$RcX@8eWp9Tq3Ls"; // Must match the JS secret key
    private static final String Church ="CH";
    private static final String Equipment ="EQ";



    public Status getStatus(int id) {
        return statusRepo.findById(id).orElse(null);
    }

    public String AESdecrypt(String encryptedPassword) throws Exception {
        // Decode the base64-encoded string
        byte[] decodedKey = SECRET_KEY.getBytes("UTF-8");
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");

        // Initialize the cipher for decryption
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt the password
        byte[] decodedPassword = org.apache.commons.codec.binary.Base64.decodeBase64(encryptedPassword);
        byte[] originalPassword = cipher.doFinal(decodedPassword);
        return new String(originalPassword);
    }

    public String generateOtp(){
        try {

            final int max = 50000;
            final int min = 10000;

            final int ans = (int) (Math.random() * (max - min + 1)) + min;
            return ans + "";
        } catch (final Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String generateRequestId(Long equipId ) {
        Date valueDate = new Date();
        // Parse the string to LocalDate
        LocalDateTime localDateTime = valueDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Define formatter for the output string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDate = localDateTime.format(formatter);


        String padded = String.format("%04d", equipId);

        return Church+Equipment+equipId+formattedDate+padded;

    }

}
