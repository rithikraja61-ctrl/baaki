package com.baaki.repository;

import com.baaki.model.SendLog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SendLogRepository extends JpaRepository<SendLog, Long> {
    List<SendLog> findBySendDate(LocalDate date);
    List<SendLog> findTop100ByOrderByIdDesc();
    List<SendLog> findByTestModeTrueAndStatus(String status);
}
