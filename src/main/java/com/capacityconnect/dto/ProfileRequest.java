package com.capacityconnect.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {

    @Size(max = 20)
    private String phoneNumber;

    private String department;
    private String qualifications;
    private String skills;
    private Integer experienceYears;
    private String interests;

    @Size(max = 500)
    private String profilePicUrl;
}
