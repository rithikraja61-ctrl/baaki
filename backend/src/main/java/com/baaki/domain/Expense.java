package com.baaki.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    /** SALARY, RENT, ELECTRICITY, GST, SUPPLIER, LOAN */
    public String category;
    public long amountPaise;
    /** MONTHLY or ONCE */
    public String recurrence;
    public Integer dayOfMonth;
    public LocalDate dueDate;
}
