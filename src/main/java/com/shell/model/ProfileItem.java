package com.shell.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Data
@AllArgsConstructor
public class ProfileItem {
    private String id;
    private String email;
    private String status;
    private String updateDate;


    public static ProfileItem createOfflineProfile(String email) {
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var id = new String(Base64.getEncoder().encode(email.getBytes(StandardCharsets.UTF_8)));
        return new ProfileItem(id, email, "OFFLINE", updateAt);
    }

}
