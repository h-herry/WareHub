package com.warehub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Material;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 物料服务
 */
public interface MaterialService extends IService<Material> {

    /** 分页查询物料 */
    PageResult<Material> pageMaterials(int pageNum, int pageSize, String materialName,
                                        String materialCode, Long categoryId, String materialType, Integer status);

    /** 分页查询物料（含分类名称，用于列表展示） */
    PageResult<Map<String, Object>> pageMaterialMaps(int pageNum, int pageSize, String materialName,
                                                      String materialCode, Long categoryId, String materialType, Integer status);

    /** 生成物料编码：分类码-类型码-流水号，如 RM-RAW-000001 */
    String generateCode(Long categoryId, String materialType);

    /** 校验编码唯一 */
    boolean existsByCode(String code);

    /** 按条码查询物料（用于扫码出库/入库自动识别） */
    Material getByBarcode(String barcode);

    /** 导出物料 Excel */
    void export(HttpServletResponse response) throws IOException;

    /** 导入物料 Excel，返回导入条数 */
    int importExcel(MultipartFile file) throws IOException;
}
