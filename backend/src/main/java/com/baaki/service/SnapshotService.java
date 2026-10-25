package com.baaki.service;

import com.baaki.dto.Snapshot;
import java.time.LocalDate;

public interface SnapshotService {
    LocalDate today();
    Snapshot load();
}
