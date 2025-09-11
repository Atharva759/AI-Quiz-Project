package com.atharva.quiz_service.dao;


import com.atharva.quiz_service.Entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizDao extends JpaRepository<Quiz,Integer> {
}
