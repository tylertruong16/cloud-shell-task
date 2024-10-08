package com.shell.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.bloomfilter.CellExtractor;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileItem implements Serializable {
    private String id = "";
    private String email = "";
    private String status = "";
    private String updateDate = "";
    private String profileFolderUrl = "";
    private String username = "";

    public static ProfileItem createOfflineProfile(String email, String username) {
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var id = new String(Base64.getEncoder().encode(email.getBytes(StandardCharsets.UTF_8)));
        return new ProfileItem(id, email, ProfileStatus.OFFLINE.name(), updateAt, "", username);
    }

    public ProfileItem updateLastUpdateTime() {
        var clone = SerializationUtils.clone(this);
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        clone.setUpdateDate(updateAt);
        return clone;
    }

    public boolean validDownloadUrl() {
        return StringUtils.isNoneBlank(this.getProfileFolderUrl());
    }


    public boolean accountCanRunShell() {
        var offlineStatus = StringUtils.equals(status, ProfileStatus.OFFLINE.name());
        CellExtractor.CellPredicate cellPredicate = (int a, int b) -> a < b;
        return offlineStatus && isValidAndFuture(updateDate, 10, cellPredicate) && StringUtils.isNoneBlank(profileFolderUrl);
    }

    public boolean accountAlreadyStop() {
        var onlineStatus = StringUtils.endsWithIgnoreCase(status, ProfileStatus.ONLINE.name());
        CellExtractor.CellPredicate cellPredicate = (int a, int b) -> a >= b;
        return onlineStatus && isValidAndFuture(updateDate, 4, cellPredicate);
    }

    public static boolean isValidAndFuture(String dateStr, int minute, CellExtractor.CellPredicate cellPredicate) {
        try {
            var formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
            var dateTime = ZonedDateTime.parse(dateStr, formatter);
            var now = ZonedDateTime.now();
            var duration = Duration.between(dateTime, now);
            return cellPredicate.test(Math.toIntExact(duration.toMinutes()), minute);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
