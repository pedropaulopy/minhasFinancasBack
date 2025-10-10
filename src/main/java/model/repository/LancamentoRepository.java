package model.repository;

import model.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentosRepository extends JpaRepository<Lancamento,Long> {
}
