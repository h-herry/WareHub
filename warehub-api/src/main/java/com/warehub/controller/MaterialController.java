package com.warehub.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehub.common.annotation.HasPerm;
import com.warehub.common.annotation.Log;
import com.warehub.common.annotation.RepeatSubmit;
import com.warehub.common.exception.BizException;
import com.warehub.common.result.PageResult;
import com.warehub.common.result.R;
import com.warehub.entity.Material;
import com.warehub.entity.MaterialCategory;
import com.warehub.service.MaterialCategoryService;
import com.warehub.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 物料管理控制器
 */
@Tag(name = "物料管理")
@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialCategoryService categoryService;

    @Operation(summary = "物料分页查询")
    @HasPerm("material:list")
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String materialName,
                                       @RequestParam(required = false) String materialCode,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) String materialType,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(materialService.pageMaterialMaps(pageNum, pageSize, materialName, materialCode, categoryId, materialType, status));
    }

    @Operation(summary = "物料详情")
    @HasPerm("material:list")
    @GetMapping("/{id}")
    public R<Material> detail(@PathVariable Long id) {
        return R.ok(materialService.getById(id));
    }

    @Operation(summary = "按条码查询物料（扫码自动识别）")
    @HasPerm("material:list")
    @GetMapping("/by-barcode")
    public R<Material> getByBarcode(@RequestParam String barcode) {
        Material material = materialService.getByBarcode(barcode);
        if (material == null) {
            return R.fail("未找到条码对应的物料：" + barcode);
        }
        return R.ok(material);
    }

    @RepeatSubmit
    @Operation(summary = "新增物料")
    @HasPerm("material:add")
    @Log(module = "物料", operation = "新增物料")
    @PostMapping
    public R<Long> add(@Valid @RequestBody Material material) {
        if (material.getMaterialCode() == null || material.getMaterialCode().isBlank()) {
            material.setMaterialCode(materialService.generateCode(material.getCategoryId(), material.getMaterialType()));
        } else if (materialService.existsByCode(material.getMaterialCode())) {
            throw new BizException("物料编码已存在");
        }
        materialService.save(material);
        return R.ok(material.getId());
    }

    @Operation(summary = "修改物料")
    @HasPerm("material:edit")
    @Log(module = "物料", operation = "修改物料")
    @PutMapping
    public R<Void> update(@RequestBody Material material) {
        if (material.getId() == null) throw new BizException("物料ID不能为空");
        materialService.updateById(material);
        return R.ok();
    }

    @Operation(summary = "删除物料")
    @HasPerm("material:delete")
    @Log(module = "物料", operation = "删除物料")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        materialService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "分类树")
    @HasPerm("material:category")
    @GetMapping("/category/tree")
    public R<List<Map<String, Object>>> categoryTree() {
        return R.ok(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @HasPerm("material:category")
    @Log(module = "物料", operation = "新增分类")
    @PostMapping("/category")
    public R<Void> addCategory(@RequestBody MaterialCategory category) {
        categoryService.save(category);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @HasPerm("material:category")
    @Log(module = "物料", operation = "修改分类")
    @PutMapping("/category")
    public R<Void> updateCategory(@RequestBody MaterialCategory category) {
        categoryService.updateById(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @HasPerm("material:category")
    @Log(module = "物料", operation = "删除分类")
    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        long childCount = categoryService.count(new LambdaQueryWrapper<MaterialCategory>().eq(MaterialCategory::getParentId, id));
        if (childCount > 0) throw new BizException("存在子分类，无法删除");
        categoryService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "导出物料")
    @HasPerm("material:export")
    @GetMapping("/export")
    public void exportMaterial(HttpServletResponse response) throws IOException {
        materialService.export(response);
    }

    @Operation(summary = "导入物料")
    @HasPerm("material:import")
    @Log(module = "物料", operation = "导入物料")
    @PostMapping("/import")
    public R<Integer> importMaterial(@RequestParam("file") MultipartFile file) throws IOException {
        int count = materialService.importExcel(file);
        return R.ok("导入成功", count);
    }
}
