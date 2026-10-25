package com.baaki.repository;

import com.baaki.model.LadderStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LadderStageRepository extends JpaRepository<LadderStage, String> {
    List<LadderStage> findAllByOrderBySortOrder();
}
