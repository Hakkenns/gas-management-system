package com.gas.sistema_gas.service.Implement;

import com.gas.sistema_gas.Mapper.MotoMapper;
import com.gas.sistema_gas.Model.Moto;
import com.gas.sistema_gas.Repository.MotoRepository;
import com.gas.sistema_gas.dto.MotoDTO;
import com.gas.sistema_gas.service.MotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotoServiceImplement implements MotoService {

    private final MotoRepository motoRepository;
    private final MotoMapper motoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MotoDTO.SimpleResponse> listDisponibles() {
        return motoRepository.findMotosDisponibles()
                .stream()
                .map(motoMapper::toSimpleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotoDTO.SimpleResponse> listMoto(){
        return motoRepository.findAll()
                .stream()
                .filter(m-> m.getEstado() == 1)
                .map(motoMapper::toSimpleResponse)
                .toList();
    }

    @Override
    @Transactional
    public MotoDTO.SimpleResponse createMoto(MotoDTO.Create createDto){
        String placa = cleanPlaca(createDto.placa());

        if(motoRepository.existsByPlaca(placa)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una moto registrada con esa placa");
        }

        // mapeamos de DTO a entidad
        Moto moto = motoMapper.toEntity(createDto);
        // Guardamos la placa en la bd (formateada)
        moto.setPlaca(placa);
        // Guardamos
        return motoMapper.toSimpleResponse(motoRepository.save(moto));
    }

    @Override
    @Transactional
    public MotoDTO.SimpleResponse updateMoto(Long id, MotoDTO.Update updateDto){
        Moto motoExistente =  motoRepository.findById(id)
                .filter(m->m.getEstado() == 1)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moto no encontrada"));

        // Limpiamos la nueva placa enviada
        String nuevaPlaca = cleanPlaca(updateDto.placa());

        if (!motoExistente.getPlaca().equals(nuevaPlaca)) {
            if (motoRepository.existsByPlaca(nuevaPlaca)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra moto registrada con esa placa");
            }
        }
        // agregamos los cambios modificados a travez del mapper
        motoMapper.toUpdateFromDto(updateDto, motoExistente);
        // asignamos la placa limpia explícitamente
        motoExistente.setPlaca(nuevaPlaca);
        // Retornamos una respuesta limpia
        return motoMapper.toSimpleResponse(motoRepository.save(motoExistente));
    }

    @Override
    @Transactional
    public void deleteMoto(Long id){
        Moto motoExistente = motoRepository.findById(id)
                .filter(m -> m.getEstado() == 1)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moto no encontrada"));
        // Eliminación lógica (cambiamos estado)
        motoExistente.setEstado(0);
        // guardamos los cambios y damos una respuesta simpple
        motoRepository.save(motoExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public MotoDTO.SimpleResponse findById(Long id){
        return motoRepository.findById(id)
                .filter(m->m.getEstado() == 1)
                .map(motoMapper::toSimpleResponse)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moto no encontrada"));
    }

    private String cleanPlaca(String placa) {
        if (placa == null) return "";
        return placa.replace("-", "")
                .replace(" ", "")
                .toUpperCase()
                .trim();
    }
}
