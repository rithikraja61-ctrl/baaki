package com.baaki.repository;

import com.baaki.model.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, String> {
    /** SELECT ... FOR UPDATE: serialises concurrent invoice numbering. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InvoiceSequence s where s.seqKey = :key")
    Optional<InvoiceSequence> lockByKey(@Param("key") String key);
}
