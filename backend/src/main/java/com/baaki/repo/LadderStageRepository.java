package com.baaki.repo;

import com.baaki.domain.LadderStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LadderStageRepository extends JpaRepository<LadderStage, String> {
    List<LadderStage> findAllByOrderBySortOrder();
}
