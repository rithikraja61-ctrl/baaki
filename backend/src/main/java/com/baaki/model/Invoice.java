package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String invoiceNumber;
    public Long customerId;
    public LocalDate invoiceDate;
    public LocalDate dueDate;
    /** Stored: DRAFT, SENT, PAID, DISPUTED. Display status is derived in InvoiceService. */
    public String status;
    /** TAX_INVOICE, BILL_OF_SUPPLY, INVOICE */
    public String docType;
    public String placeOfSupply;
    public boolean reverseCharge;
    public String deliveryAddress;
    public long taxablePaise;
    public long cgstPaise;
    public long sgstPaise;
    public long igstPaise;
    public long roundOffPaise;
    public long totalPaise;
    public String payToken;
    public LocalDateTime sentAt;
    public LocalDate promisedDate;
    public LocalDate snoozedUntil;
    public String notes;
}
