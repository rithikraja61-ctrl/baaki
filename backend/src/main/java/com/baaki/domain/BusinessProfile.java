package com.baaki.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "business_profile")
public class BusinessProfile {
    @Id public Long id;
    public String name;
    public String ownerName;
    public String address;
    public String city;
    public String stateCode;
    public String gstin;
    /** REGULAR, COMPOSITION or UNREGISTERED */
    public String registrationType;
    public long turnoverPaise;
    public boolean udyamRegistered;
    public String udyamNumber;
    public String msmeCategory;
    public String phone;
    public String email;
    public String bankName;
    public String accountName;
    public String accountNumber;
    public String ifsc;
    public String upiId;
    public long cashInBankPaise;
    public long safetyBufferPaise;
    public LocalDate demoDate;
    public String language;
    public String textSize;
    /** null = invoices and demand letters in English only; hi or mr = add that language under each heading. */
    public String docSecondLanguage;
    @Column(columnDefinition = "MEDIUMTEXT") public String logoDataUrl;
    /** Reply-to address on automatic emails, and the inbox that receives everything in test mode. */
    public String ownerEmail;
    public boolean autoSendEnabled;
    public boolean autoSendTestMode;

    public LocalDate getDemoDate() { return demoDate; }
}
