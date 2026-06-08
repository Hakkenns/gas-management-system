package com.gas.sistema_gas.service;
import com.gas.sistema_gas.Model.AsignacionMoto;
import com.gas.sistema_gas.dto.AsignacionMotoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AsignacionMotoService {

    List<AsignacionMotoDTO.SimpleResponse> listAll();
    void asignar(AsignacionMotoDTO.Create create);
    void finalizar(Long id);
}