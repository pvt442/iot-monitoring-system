package com.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.model.AlertConfig;

@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Integer> {
}
