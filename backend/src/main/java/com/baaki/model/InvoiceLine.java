package com.baaki.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long invoiceId;
    public int lineNo;
    public String description;
    public String hsnSac;
    public BigDecimal quantity;
    public String unit;
    public long ratePaise;
    public long discountPaise;
    public int gstRateBp;
    public long taxablePaise;
    public long cgstPaise;
    public long sgstPaise;
    public long igstPaise;
    public long totalPaise;
}
