package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
public class CustomerProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "tax_code")
    private String taxCode;

    private String industry;
    private String address;
    private String website;
    private String source; // Facebook, Google Ads, Referral...
}