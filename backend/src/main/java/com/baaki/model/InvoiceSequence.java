package com.baaki.model;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_sequence")
public class InvoiceSequence {
    @Id public String seqKey;
    public long nextValue;
}
