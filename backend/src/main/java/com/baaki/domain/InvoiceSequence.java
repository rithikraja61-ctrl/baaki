package com.baaki.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_sequence")
public class InvoiceSequence {
    @Id public String seqKey;
    public long nextValue;
}
