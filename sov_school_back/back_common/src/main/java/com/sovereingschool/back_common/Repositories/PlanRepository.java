package com.sovereingschool.back_common.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_common.Models.Plan;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

}
