package com.capacityconnect.dto;

import com.capacityconnect.entity.Announcement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String audience;
    private Announcement.Status status;
    private Announcement.Type type;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
