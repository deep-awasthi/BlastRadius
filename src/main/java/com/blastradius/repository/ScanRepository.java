package com.blastradius.repository;

import com.blastradius.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findAllByOrderByCreatedAtDesc();
    List<Scan> findByStatus(Scan.ScanStatus status);
}
