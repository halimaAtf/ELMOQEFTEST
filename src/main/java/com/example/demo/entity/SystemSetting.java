package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting {
    @Id
    private Long id = 1L;

    private String platformName = "ELMOQEF";
    private String supportEmail = "support@elmoqef.com";
    private Integer platformFee = 10;
    private Boolean emailNotifications = true;
    private Boolean autoApprove = false;
    private String availableLanguages = "en,fr,ar";
}
