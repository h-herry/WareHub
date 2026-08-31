package com.warehub.controller;

import com.warehub.common.annotation.HasPerm;
import com.warehub.common.annotation.Log;
import com.warehub.common.annotation.RepeatSubmit;
import com.warehub.common.result.PageResult;
import com.warehub.common.result.R;
import com.warehub.dto.*;
import com.warehub.entity.Location;
import com.warehub.entity.StockLog;
import com.warehub.entity.Warehouse;
import com.warehub.service.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库存管理控制器
 */
@Tag(name = "库存管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final StockService stockService;
    private final WarehouseService warehouseService;
    private final LocationService locationService;
    private final InboundService inboundService;
    private final OutboundService outboundService;
    private final TransferService transferService;
    private final StocktakeService stocktakeService;

    // ===== 库存查询 =====
    @Operation(summary = "库存分页查询")
    @HasPerm("stock:list")
    @GetMapping("/stock/page")
    public R<PageResult<Map<String, Object>>> stockPage(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String materialName,
                                            @RequestParam(required = false) Long warehouseId,
                                            @RequestParam(required = false) Long locationId) {
        return R.ok(stockService.pageStock(pageNum, pageSize, materialName, warehouseId, locationId));
    }

    @Operation(summary = "库存流水")
    @HasPerm("stock:log")
    @GetMapping("/stock/log/page")
    public R<PageResult<Map<String, Object>>> stockLog(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Long materialId,
                                           @RequestParam(required = false) String businessOrderNo,
                                           @RequestParam(required = false) String changeType) {
        return R.ok(stockService.pageLog(pageNum, pageSize, materialId, businessOrderNo, changeType));
    }

    @Operation(summary = "库存预警")
    @HasPerm("stock:warning")
    @GetMapping("/stock/warning")
    public R<List<Map<String, Object>>> warning() {
        return R.ok(stockService.warning());
    }

    // ===== 仓库/库位 =====
    @Operation(summary = "仓库分页")
    @HasPerm("warehouse:list")
    @GetMapping("/warehouse/page")
    public R<PageResult<Map<String, Object>>> warehousePage(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String warehouseName) {
        return R.ok(warehouseService.page(pageNum, pageSize, warehouseName));
    }

    @Operation(summary = "新增仓库")
    @HasPerm("warehouse:list")
    @Log(module = "仓库", operation = "新增仓库")
    @PostMapping("/warehouse")
    public R<Void> addWarehouse(@RequestBody Warehouse warehouse) {
        warehouseService.save(warehouse);
        return R.ok();
    }

    @Operation(summary = "修改仓库")
    @HasPerm("warehouse:list")
    @Log(module = "仓库", operation = "修改仓库")
    @PutMapping("/warehouse")
    public R<Void> updateWarehouse(@RequestBody Warehouse warehouse) {
        warehouseService.updateById(warehouse);
        return R.ok();
    }

    @Operation(summary = "删除仓库")
    @HasPerm("warehouse:list")
    @Log(module = "仓库", operation = "删除仓库")
    @DeleteMapping("/warehouse/{id}")
    public R<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "库位分页")
    @HasPerm("location:list")
    @GetMapping("/location/page")
    public R<PageResult<Map<String, Object>>> locationPage(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) Long warehouseId,
                                               @RequestParam(required = false) String locationName) {
        return R.ok(locationService.page(pageNum, pageSize, warehouseId, locationName));
    }

    @Operation(summary = "新增库位")
    @HasPerm("location:list")
    @Log(module = "库位", operation = "新增库位")
    @PostMapping("/location")
    public R<Void> addLocation(@RequestBody Location location) {
        locationService.save(location);
        return R.ok();
    }

    @Operation(summary = "修改库位")
    @HasPerm("location:list")
    @Log(module = "库位", operation = "修改库位")
    @PutMapping("/location")
    public R<Void> updateLocation(@RequestBody Location location) {
        locationService.updateById(location);
        return R.ok();
    }

    @Operation(summary = "删除库位")
    @HasPerm("location:list")
    @Log(module = "库位", operation = "删除库位")
    @DeleteMapping("/location/{id}")
    public R<Void> deleteLocation(@PathVariable Long id) {
        locationService.removeById(id);
        return R.ok();
    }

    // ===== 入库 =====
    @Operation(summary = "入库单分页")
    @HasPerm("inbound:list")
    @GetMapping("/inbound/page")
    public R<PageResult<Map<String, Object>>> inboundPage(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String orderNo,
                                              @RequestParam(required = false) Long warehouseId) {
        return R.ok(inboundService.page(pageNum, pageSize, status, orderNo, warehouseId));
    }

    @RepeatSubmit
    @Operation(summary = "新增入库单")
    @HasPerm("inbound:add")
    @Log(module = "入库", operation = "新增入库单")
    @PostMapping("/inbound")
    public R<String> addInbound(@Valid @RequestBody InboundCreateDTO dto) {
        return R.ok("创建成功", inboundService.createInbound(dto));
    }

    @Operation(summary = "提交入库单")
    @HasPerm("inbound:submit")
    @Log(module = "入库", operation = "提交入库单")
    @PutMapping("/inbound/submit/{id}")
    public R<String> submitInbound(@PathVariable String id) {
        return R.ok("提交成功", inboundService.submit(id));
    }

    @Operation(summary = "执行入库")
    @HasPerm("inbound:execute")
    @Log(module = "入库", operation = "执行入库")
    @PutMapping("/inbound/execute/{id}")
    public R<Void> executeInbound(@PathVariable String id) {
        inboundService.execute(id);
        return R.ok();
    }

    // ===== 出库 =====
    @Operation(summary = "出库单分页")
    @HasPerm("outbound:list")
    @GetMapping("/outbound/page")
    public R<PageResult<Map<String, Object>>> outboundPage(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String orderNo,
                                               @RequestParam(required = false) Long warehouseId) {
        return R.ok(outboundService.page(pageNum, pageSize, status, orderNo, warehouseId));
    }

    @RepeatSubmit
    @Operation(summary = "新增出库单")
    @HasPerm("outbound:add")
    @Log(module = "出库", operation = "新增出库单")
    @PostMapping("/outbound")
    public R<String> addOutbound(@Valid @RequestBody OutboundCreateDTO dto) {
        return R.ok("创建成功", outboundService.createOutbound(dto));
    }

    @Operation(summary = "提交出库单")
    @HasPerm("outbound:add")
    @Log(module = "出库", operation = "提交出库单")
    @PutMapping("/outbound/submit/{id}")
    public R<String> submitOutbound(@PathVariable String id) {
        return R.ok("提交成功", outboundService.submit(id));
    }

    @Operation(summary = "执行出库")
    @HasPerm("outbound:execute")
    @Log(module = "出库", operation = "执行出库")
    @PutMapping("/outbound/execute/{id}")
    public R<Void> executeOutbound(@PathVariable String id) {
        outboundService.execute(id);
        return R.ok();
    }

    // ===== 调拨 =====
    @Operation(summary = "调拨单分页")
    @HasPerm("transfer:list")
    @GetMapping("/transfer/page")
    public R<PageResult<Map<String, Object>>> transferPage(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String orderNo,
                                               @RequestParam(required = false) Long warehouseId) {
        return R.ok(transferService.page(pageNum, pageSize, status, orderNo, warehouseId));
    }

    @RepeatSubmit
    @Operation(summary = "新增调拨单")
    @HasPerm("transfer:add")
    @Log(module = "调拨", operation = "新增调拨单")
    @PostMapping("/transfer")
    public R<String> addTransfer(@Valid @RequestBody TransferCreateDTO dto) {
        return R.ok("创建成功", transferService.createTransfer(dto));
    }

    @Operation(summary = "提交调拨单")
    @HasPerm({"transfer:add", "transfer:submit"})
    @Log(module = "调拨", operation = "提交调拨单")
    @PutMapping("/transfer/submit/{id}")
    public R<String> submitTransfer(@PathVariable String id) {
        return R.ok("提交成功", transferService.submit(id));
    }

    @Operation(summary = "执行调拨")
    @HasPerm({"transfer:add", "transfer:execute"})
    @Log(module = "调拨", operation = "执行调拨")
    @PutMapping("/transfer/execute/{id}")
    public R<Void> executeTransfer(@PathVariable String id) {
        transferService.execute(id);
        return R.ok();
    }

    // ===== 盘点 =====
    @Operation(summary = "盘点单分页")
    @HasPerm("stocktake:list")
    @GetMapping("/stocktake/page")
    public R<PageResult<Map<String, Object>>> stocktakePage(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String orderNo,
                                                @RequestParam(required = false) Long warehouseId) {
        return R.ok(stocktakeService.page(pageNum, pageSize, status, orderNo, warehouseId));
    }

    @RepeatSubmit
    @Operation(summary = "新增盘点单")
    @HasPerm("stocktake:add")
    @Log(module = "盘点", operation = "新增盘点单")
    @PostMapping("/stocktake")
    public R<String> addStocktake(@Valid @RequestBody StocktakeCreateDTO dto) {
        return R.ok("创建成功", stocktakeService.createStocktake(dto));
    }

    @Operation(summary = "提交盘点单")
    @HasPerm({"stocktake:add", "stocktake:submit"})
    @Log(module = "盘点", operation = "提交盘点单")
    @PutMapping("/stocktake/submit/{id}")
    public R<String> submitStocktake(@PathVariable String id) {
        return R.ok("提交成功", stocktakeService.submit(id));
    }

    @Operation(summary = "执行盘点")
    @HasPerm({"stocktake:add", "stocktake:execute"})
    @Log(module = "盘点", operation = "执行盘点")
    @PutMapping("/stocktake/execute/{id}")
    public R<Void> executeStocktake(@PathVariable String id) {
        stocktakeService.execute(id);
        return R.ok();
    }
}
