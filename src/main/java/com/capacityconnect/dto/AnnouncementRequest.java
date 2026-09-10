package com.capacityconnect.dto;

import com.capacityconnect.entity.Announcement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String audience;

    @NotNull
    private Announcement.Status status;

    @NotNull
    private Announcement.Type type;
}
