package com.learntech.backend.repository;

import com.learntech.backend.model.PostIt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostItRepository extends JpaRepository<PostIt, Integer> {
    List<PostIt> findByAlunoRaAndAtividadeId(Integer ra, Integer atividadeId);
}
