package com.warehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.entity.Material;
import com.warehub.entity.MaterialCategory;
import com.warehub.mapper.MaterialMapper;
import com.warehub.service.MaterialService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 物料服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    private final com.warehub.mapper.MaterialCategoryMapper categoryMapper;

    @Value("${warehub.material-code.type-codes.RAW:RAW}")
    private String rawCode;
    @Value("${warehub.material-code.type-codes.SEM:SEM}")
    private String semCode;
    @Value("${warehub.material-code.type-codes.FIN:FIN}")
    private String finCode;
    @Value("${warehub.material-code.type-codes.AUX:AUX}")
    private String auxCode;

    @Override
    public PageResult<Material> pageMaterials(int pageNum, int pageSize, String materialName,
                                              String materialCode, Long categoryId, String materialType, Integer status) {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.like(materialName != null, Material::getMaterialName, materialName)
           .like(materialCode != null, Material::getMaterialCode, materialCode)
           .eq(categoryId != null, Material::getCategoryId, categoryId)
           .eq(materialType != null, Material::getMaterialType, materialType)
           .eq(status != null, Material::getStatus, status)
           .orderByDesc(Material::getId);
        IPage<Material> page = page(new Page<>(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public PageResult<Map<String, Object>> pageMaterialMaps(int pageNum, int pageSize, String materialName,
                                                              String materialCode, Long categoryId, String materialType, Integer status) {
        PageResult<Material> pr = pageMaterials(pageNum, pageSize, materialName, materialCode, categoryId, materialType, status);
        // 补充分类名称
        Set<Long> catIds = pr.getRecords().stream()
                .map(Material::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catNameMap = catIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getCategoryName));
        List<Map<String, Object>> records = new ArrayList<>();
        for (Material m : pr.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("materialCode", m.getMaterialCode());
            map.put("materialName", m.getMaterialName());
            map.put("shortName", m.getShortName());
            map.put("categoryId", m.getCategoryId());
            map.put("categoryName", catNameMap.getOrDefault(m.getCategoryId(), ""));
            map.put("spec", m.getSpec());
            map.put("unit", m.getUnit());
            map.put("materialType", m.getMaterialType());
            map.put("safetyStock", m.getSafetyStock());
            map.put("maxStock", m.getMaxStock());
            map.put("status", m.getStatus());
            map.put("remark", m.getRemark());
            map.put("createTime", m.getCreateTime());
            records.add(map);
        }
        return new PageResult<>(pr.getTotal(), records);
    }

    @Override
    public synchronized String generateCode(Long categoryId, String materialType) {
        MaterialCategory category = categoryMapper.selectById(categoryId);
        if (category == null) throw new BizException("物料分类不存在");
        String catCode = category.getCategoryCode();
        String typeCode = switch (materialType) {
            case "RAW" -> rawCode;
            case "SEM" -> semCode;
            case "FIN" -> finCode;
            case "AUX" -> auxCode;
            default -> materialType;
        };
        String prefix = catCode + "-" + typeCode + "-";
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.likeRight(Material::getMaterialCode, prefix).orderByDesc(Material::getMaterialCode);
        List<Material> list = list(qw);
        int seq = 1;
        if (!list.isEmpty()) {
            String last = list.get(0).getMaterialCode();
            String numPart = last.substring(prefix.length());
            try {
                seq = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%06d", seq);
    }

    @Override
    public boolean existsByCode(String code) {
        return count(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, code)) > 0;
    }

    @Override
    public Material getByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<Material>()
                .eq(Material::getBarCode, barcode.trim())
                .last("LIMIT 1");
        return getOne(qw);
    }

    @Override
    public void export(HttpServletResponse response) throws IOException {
        List<Material> list = list();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("物料");
        String[] headers = {"物料编码", "物料名称", "简称", "规格", "单位", "类型", "安全库存", "最大库存", "状态"};
        Row head = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) head.createCell(i).setCellValue(headers[i]);
        for (int i = 0; i < list.size(); i++) {
            Material m = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(nz(m.getMaterialCode()));
            row.createCell(1).setCellValue(nz(m.getMaterialName()));
            row.createCell(2).setCellValue(nz(m.getShortName()));
            row.createCell(3).setCellValue(nz(m.getSpec()));
            row.createCell(4).setCellValue(nz(m.getUnit()));
            row.createCell(5).setCellValue(nz(m.getMaterialType()));
            row.createCell(6).setCellValue(m.getSafetyStock() != null ? m.getSafetyStock().toString() : "");
            row.createCell(7).setCellValue(m.getMaxStock() != null ? m.getMaxStock().toString() : "");
            row.createCell(8).setCellValue(m.getStatus() != null && m.getStatus() == 1 ? "启用" : "停用");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("物料导出.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        wb.write(response.getOutputStream());
        wb.close();
    }

    @Override
    public int importExcel(MultipartFile file) throws IOException {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int count = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Material m = new Material();
                m.setMaterialCode(getCell(row, 0));
                m.setMaterialName(getCell(row, 1));
                m.setShortName(getCell(row, 2));
                m.setSpec(getCell(row, 3));
                m.setUnit(getCell(row, 4));
                m.setMaterialType(getCell(row, 5));
                m.setSafetyStock(toDecimal(getCell(row, 6)));
                m.setMaxStock(toDecimal(getCell(row, 7)));
                m.setStatus("停用".equals(getCell(row, 8)) ? 0 : 1);
                m.setCategoryId(1L);
                if (m.getMaterialName() == null || m.getMaterialName().isBlank()) continue;
                if (m.getMaterialCode() == null || m.getMaterialCode().isBlank()) {
                    m.setMaterialCode(generateCode(1L, m.getMaterialType() == null ? "RAW" : m.getMaterialType()));
                }
                if (!existsByCode(m.getMaterialCode())) {
                    save(m);
                    count++;
                }
            }
            return count;
        }
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String getCell(Row row, int idx) {
        var cell = row.getCell(idx);
        return cell == null ? "" : cell.toString().trim();
    }

    private java.math.BigDecimal toDecimal(String s) {
        if (s == null || s.isBlank()) return java.math.BigDecimal.ZERO;
        try { return new java.math.BigDecimal(s); } catch (NumberFormatException e) { return java.math.BigDecimal.ZERO; }
    }
}
