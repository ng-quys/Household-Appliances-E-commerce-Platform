package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.repository.OderDetailRepository;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OderDetailServiceImpl extends AbstractCrudService<OderDetail, OderDetailId> implements OderDetailService {

    private final OderDetailRepository repository;

    @Override
    protected JpaRepository<OderDetail, OderDetailId> getRepository() {
        return repository;
    }

    @Override
    public List<OderDetail> findByOrderId(Integer orderId) {
        return repository.findByOrderOrderId(orderId);
    }

    @Override
    public List<OderDetail> findByProductId(Integer productId) {
        return repository.findByProductProductId(productId);
    }
}
