package com.baaki.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String contactPerson;
    public String phone;
    public String email;
    public String address;
    public String city;
    public String stateCode;
    public String gstin;
    public int creditDays;
    /** en, hi or mr: language of this customer's reminders and pay link. */
    public String preferredLanguage = "en";
}
