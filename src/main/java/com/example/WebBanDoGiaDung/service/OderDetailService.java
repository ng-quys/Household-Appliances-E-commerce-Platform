package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import java.util.List;

public interface OderDetailService extends CrudService<OderDetail, OderDetailId> {
    List<OderDetail> findByOrderId(Integer orderId);

    List<OderDetail> findByProductId(Integer productId);
}
