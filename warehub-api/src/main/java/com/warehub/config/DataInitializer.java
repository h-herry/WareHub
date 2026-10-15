package com.warehub.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehub.common.constant.OrderStatus;
import com.warehub.common.security.LoginUser;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 数据初始化 - 首次启动播种菜单、角色、管理员及演示数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final DepartmentMapper departmentMapper;
    private final MaterialCategoryMapper categoryMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;
    private final StockMapper stockMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundItemMapper inboundItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final AuditRecordMapper auditRecordMapper;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void init() {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()) > 0) {
            log.info("系统已初始化，跳过数据播种");
            return;
        }
        log.info("开始初始化仓枢 WareHub 基础数据...");
        initMenus();
        initRoles();
        initAdmin();
        if ("h2".equals(activeProfile)) {
            initDemoData();
            initRichDemoData();
        }
        log.info("仓枢 WareHub 基础数据初始化完成");
    }

    /** 菜单权限数据 */
    private void initMenus() {
        List<Menu> menus = new ArrayList<>();
        // id, parentId, name, permCode, type(1菜单/2按钮), path, icon, sort
        menus.add(m(1L, 0L, "仪表盘", "", 1, "/dashboard", "Odometer", 1));
        menus.add(m(2L, 0L, "物料管理", "", 1, "/material", "Box", 2));
        menus.add(m(3L, 2L, "物料分类", "material:category", 1, "/material/category", "", 1));
        menus.add(m(4L, 2L, "物料列表", "material:list", 1, "/material/list", "", 2));
        menus.add(m(5L, 4L, "物料新增", "material:add", 2, "", "", 1));
        menus.add(m(6L, 4L, "物料编辑", "material:edit", 2, "", "", 2));
        menus.add(m(7L, 4L, "物料删除", "material:delete", 2, "", "", 3));
        menus.add(m(8L, 4L, "物料导入", "material:import", 2, "", "", 4));
        menus.add(m(9L, 4L, "物料导出", "material:export", 2, "", "", 5));

        menus.add(m(10L, 0L, "库存管理", "", 1, "/inventory", "DataLine", 3));
        menus.add(m(11L, 10L, "入库管理", "inbound:list", 1, "/inventory/inbound", "", 1));
        menus.add(m(12L, 10L, "出库管理", "outbound:list", 1, "/inventory/outbound", "", 2));
        menus.add(m(13L, 10L, "调拨管理", "transfer:list", 1, "/inventory/transfer", "", 3));
        menus.add(m(14L, 10L, "盘点管理", "stocktake:list", 1, "/inventory/stocktake", "", 4));
        menus.add(m(15L, 10L, "库存查询", "stock:list", 1, "/inventory/stock", "", 5));
        menus.add(m(16L, 10L, "库存预警", "stock:warning", 1, "/inventory/warning", "", 6));
        menus.add(m(17L, 10L, "仓库管理", "warehouse:list", 1, "/inventory/warehouse", "", 7));
        menus.add(m(52L, 10L, "库位管理", "location:list", 1, "/inventory/location", "", 8));
        menus.add(m(18L, 11L, "入库开单", "inbound:add", 2, "", "", 1));
        menus.add(m(19L, 11L, "入库提交", "inbound:submit", 2, "", "", 2));
        menus.add(m(20L, 11L, "入库执行", "inbound:execute", 2, "", "", 3));
        menus.add(m(21L, 12L, "出库开单", "outbound:add", 2, "", "", 1));
        menus.add(m(22L, 12L, "出库执行", "outbound:execute", 2, "", "", 2));

        // 调拨作业按钮
        menus.add(m(45L, 13L, "调拨开单", "transfer:add", 2, "", "", 1));
        menus.add(m(46L, 13L, "调拨提交", "transfer:submit", 2, "", "", 2));
        menus.add(m(47L, 13L, "调拨执行", "transfer:execute", 2, "", "", 3));
        // 盘点作业按钮
        menus.add(m(48L, 14L, "盘点开单", "stocktake:add", 2, "", "", 1));
        menus.add(m(49L, 14L, "盘点提交", "stocktake:submit", 2, "", "", 2));
        menus.add(m(50L, 14L, "盘点执行", "stocktake:execute", 2, "", "", 3));
        // 库存流水
        menus.add(m(51L, 15L, "库存流水", "stock:log", 2, "", "", 1));

        menus.add(m(30L, 0L, "人员管理", "", 1, "/system", "User", 4));
        menus.add(m(31L, 30L, "用户管理", "user:list", 1, "/system/user", "", 1));
        menus.add(m(32L, 30L, "角色管理", "role:list", 1, "/system/role", "", 2));
        menus.add(m(33L, 30L, "菜单权限", "menu:list", 1, "/system/menu", "", 3));
        menus.add(m(34L, 30L, "部门管理", "dept:list", 1, "/system/dept", "", 4));
        menus.add(m(35L, 31L, "用户新增", "user:add", 2, "", "", 1));
        menus.add(m(36L, 31L, "用户编辑", "user:edit", 2, "", "", 2));
        menus.add(m(37L, 32L, "角色赋权", "role:perm", 2, "", "", 1));
        menus.add(m(38L, 32L, "角色新增", "role:add", 2, "", "", 2));

        menus.add(m(40L, 0L, "审核中心", "", 1, "/audit", "Stamp", 5));
        menus.add(m(41L, 40L, "待办审核", "audit:list", 1, "/audit/todo", "", 1));
        menus.add(m(42L, 40L, "审核记录", "audit:record", 1, "/audit/record", "", 2));
        menus.add(m(43L, 41L, "审核通过", "audit:pass", 2, "", "", 1));
        menus.add(m(44L, 41L, "审核驳回", "audit:reject", 2, "", "", 2));

        menus.forEach(menu -> {
            if (menuMapper.selectById(menu.getId()) == null) {
                menuMapper.insert(menu);
            }
        });
    }

    /** 预置角色 */
    private void initRoles() {
        Role admin = r(1L, "admin", "系统管理员", "全部权限，系统配置");
        Role manager = r(2L, "warehouse_manager", "仓库主管", "审核、查询、统计");
        Role keeper = r(3L, "keeper", "库管员", "入库、出库、调拨、盘点录入");
        Role query = r(4L, "query", "查询员", "只读查询");
        insertRole(admin);
        insertRole(manager);
        insertRole(keeper);
        insertRole(query);

        // 管理员拥有全部菜单
        menuMapper.selectList(null).forEach(menu ->
                insertRoleMenu(1L, menu.getId()));
        // 仓库主管：物料/库存作业 + 审核（不含系统管理）
        Arrays.asList(1L,2L,3L,4L,10L,11L,12L,13L,14L,15L,16L,17L,52L,
                18L,19L,20L,21L,22L,            // 入库/出库 开单·提交·执行
                40L,41L,42L,43L,44L,            // 审核中心（含通过/驳回）
                45L,46L,47L,48L,49L,50L,51L)    // 调拨/盘点/库存流水
                .forEach(id -> insertRoleMenu(2L, id));
        // 库管员：作业录入（不含审核通过/驳回）
        Arrays.asList(1L,2L,3L,4L,10L,11L,12L,13L,14L,15L,16L,17L,52L,
                18L,19L,20L,21L,22L,            // 入库/出库 开单·提交·执行
                45L,46L,47L,48L,49L,50L,51L)    // 调拨/盘点/库存流水
                .forEach(id -> insertRoleMenu(3L, id));
        // 查询员：只读
        Arrays.asList(1L,2L,4L,10L,15L,16L,17L,52L).forEach(id ->
                insertRoleMenu(4L, id));
    }

    private void insertRole(Role role) {
        if (roleMapper.selectById(role.getId()) == null) {
            roleMapper.insert(role);
        }
    }

    private void insertRoleMenu(Long roleId, Long menuId) {
        long cnt = roleMenuMapper.selectCount(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId).eq(RoleMenu::getMenuId, menuId));
        if (cnt == 0) {
            roleMenuMapper.insert(new RoleMenu(null, roleId, menuId));
        }
    }

    /** 管理员账号 */
    private void initAdmin() {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        Long adminId;
        if (existing == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setPhone("13800000000");
            admin.setDeptId(1L);
            admin.setStatus(1);
            userMapper.insert(admin);
            adminId = admin.getId();
            log.info("默认管理员账号: admin / admin123");
        } else {
            adminId = existing.getId();
        }
        long ur = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, adminId).eq(UserRole::getRoleId, 1L));
        if (ur == 0) {
            userRoleMapper.insert(new UserRole(null, adminId, 1L));
        }
    }

    /** H2 演示数据 */
    private void initDemoData() {
        if (departmentMapper.selectCount(new LambdaQueryWrapper<Department>().eq(Department::getDeptName, "总仓管理中心")) == 0) {
            Department dept = new Department();
            dept.setId(1L); dept.setParentId(0L); dept.setDeptName("总仓管理中心");
            dept.setLeader("张三"); dept.setStatus(1);
            departmentMapper.insert(dept);
        }

        if (warehouseMapper.selectCount(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getWarehouseCode, "WH001")) == 0) {
            Warehouse wh = new Warehouse();
            wh.setId(1L); wh.setWarehouseCode("WH001"); wh.setWarehouseName("中央仓库");
            wh.setAddress("园区A栋"); wh.setManager("李四"); wh.setStatus(1);
            warehouseMapper.insert(wh);
        }

        if (locationMapper.selectCount(new LambdaQueryWrapper<Location>().eq(Location::getLocationCode, "A-01-01")) == 0) {
            Location loc = new Location();
            loc.setId(1L); loc.setWarehouseId(1L); loc.setLocationCode("A-01-01");
            loc.setLocationName("A区01排01架"); loc.setZoneCode("ZONE-A"); loc.setStatus(1);
            locationMapper.insert(loc);
        }

        if (categoryMapper.selectCount(new LambdaQueryWrapper<MaterialCategory>().eq(MaterialCategory::getCategoryCode, "RAW")) == 0) {
            MaterialCategory cat = new MaterialCategory();
            cat.setId(1L); cat.setParentId(0L); cat.setCategoryCode("RAW");
            cat.setCategoryName("原材料"); cat.setSort(1); cat.setStatus(1);
            categoryMapper.insert(cat);
        }

        if (materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, "RM-RAW-000001")) == 0) {
            Material mat = new Material();
            mat.setId(1001L); mat.setMaterialCode("RM-RAW-000001"); mat.setMaterialName("不锈钢板");
            mat.setShortName("钢板"); mat.setCategoryId(1L); mat.setSpec("1.2mm*1250mm");
            mat.setUnit("张"); mat.setMaterialType("RAW");
            mat.setSafetyStock(new BigDecimal("50")); mat.setMaxStock(new BigDecimal("500"));
            mat.setBarCode("6901001001001"); mat.setStatus(1);
            materialMapper.insert(mat);
        }

        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getWarehouseId, 1L).eq(Stock::getLocationId, 1L)
                .eq(Stock::getMaterialId, 1001L).eq(Stock::getBatchNo, "B20260701")) == 0) {
            Stock stock = new Stock();
            stock.setWarehouseId(1L); stock.setLocationId(1L); stock.setMaterialId(1001L);
            stock.setBatchNo("B20260701"); stock.setQuantity(new BigDecimal("120"));
            stock.setLockedQuantity(BigDecimal.ZERO); stock.setVersion(0);
            stock.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(stock);
        }

        // 第二个仓库与库位，使演示更贴近真实多仓场景
        if (warehouseMapper.selectCount(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getWarehouseCode, "WH002")) == 0) {
            Warehouse wh2 = new Warehouse();
            wh2.setId(2L); wh2.setWarehouseCode("WH002"); wh2.setWarehouseName("华东分仓");
            wh2.setAddress("园区B栋"); wh2.setManager("王五"); wh2.setStatus(1);
            warehouseMapper.insert(wh2);
        }

        if (locationMapper.selectCount(new LambdaQueryWrapper<Location>().eq(Location::getLocationCode, "B-01-01")) == 0) {
            Location loc2 = new Location();
            loc2.setId(2L); loc2.setWarehouseId(2L); loc2.setLocationCode("B-01-01");
            loc2.setLocationName("B区01排01架"); loc2.setZoneCode("ZONE-B"); loc2.setStatus(1);
            locationMapper.insert(loc2);
        }

        if (locationMapper.selectCount(new LambdaQueryWrapper<Location>().eq(Location::getLocationCode, "A-01-02")) == 0) {
            Location loc3 = new Location();
            loc3.setId(3L); loc3.setWarehouseId(1L); loc3.setLocationCode("A-01-02");
            loc3.setLocationName("A区01排02架"); loc3.setZoneCode("ZONE-A"); loc3.setStatus(1);
            locationMapper.insert(loc3);
        }

        // 补充物料，让库存查询/仪表盘首屏即有可见数据
        if (materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, "RM-RAW-000002")) == 0) {
            Material mat2 = new Material();
            mat2.setId(1002L); mat2.setMaterialCode("RM-RAW-000002"); mat2.setMaterialName("镀锌螺栓");
            mat2.setShortName("螺栓"); mat2.setCategoryId(1L); mat2.setSpec("M8*40");
            mat2.setUnit("盒"); mat2.setMaterialType("RAW");
            // 安全库存 120 > 现存 80，用于演示"库存预警"页面（含仓库名/库位名）
            mat2.setSafetyStock(new BigDecimal("120")); mat2.setMaxStock(new BigDecimal("300"));
            mat2.setBarCode("6902002002002"); mat2.setStatus(1);
            materialMapper.insert(mat2);
        }

        if (materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, "RM-RAW-000003")) == 0) {
            Material mat3 = new Material();
            mat3.setId(1003L); mat3.setMaterialCode("RM-RAW-000003"); mat3.setMaterialName("瓦楞纸箱");
            mat3.setShortName("纸箱"); mat3.setCategoryId(1L); mat3.setSpec("600*400*400");
            mat3.setUnit("个"); mat3.setMaterialType("PACK");
            mat3.setSafetyStock(new BigDecimal("100")); mat3.setMaxStock(new BigDecimal("2000"));
            mat3.setBarCode("6903003003003"); mat3.setStatus(1);
            materialMapper.insert(mat3);
        }

        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getWarehouseId, 2L).eq(Stock::getLocationId, 2L)
                .eq(Stock::getMaterialId, 1002L).eq(Stock::getBatchNo, "B20260702")) == 0) {
            Stock stock2 = new Stock();
            stock2.setWarehouseId(2L); stock2.setLocationId(2L); stock2.setMaterialId(1002L);
            stock2.setBatchNo("B20260702"); stock2.setQuantity(new BigDecimal("80"));
            stock2.setLockedQuantity(BigDecimal.ZERO); stock2.setVersion(0);
            stock2.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(stock2);
        }

        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getWarehouseId, 1L).eq(Stock::getLocationId, 3L)
                .eq(Stock::getMaterialId, 1003L).eq(Stock::getBatchNo, "B20260703")) == 0) {
            Stock stock3 = new Stock();
            stock3.setWarehouseId(1L); stock3.setLocationId(3L); stock3.setMaterialId(1003L);
            stock3.setBatchNo("B20260703"); stock3.setQuantity(new BigDecimal("200"));
            stock3.setLockedQuantity(BigDecimal.ZERO); stock3.setVersion(0);
            stock3.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(stock3);
        }
    }

    /** 演示数据扩充：作业账号、物料、库存、四类单据 */
    private void initRichDemoData() {
        if (inboundOrderMapper.selectCount(null) > 0) {
            log.info("演示业务单据已存在，跳过扩充");
            return;
        }
        initDemoAccounts();
        List<Long> materialIds = initDemoMaterials();
        initDemoStocks();
        initDemoOrders(materialIds);
        log.info("演示数据扩充完成：8 个作业账号 / 50 物料 / 370 单据（5 待审 + 8 待执行）/ 4 条库存预警");
    }

    /** 演示账号：1 仓库主管 + 4 库管员 + 3 查询员，密码统一 admin123 */
    private void initDemoAccounts() {
        createAccount("manager1", "张伟", "13800000001", 2L);
        String[] keepers = {"李强", "王芳", "刘洋", "陈静"};
        for (int i = 0; i < keepers.length; i++) {
            createAccount("keeper" + (i + 1), keepers[i], "1380000001" + i, 3L);
        }
        String[] viewers = {"赵敏", "孙悦", "周涛"};
        for (int i = 0; i < viewers.length; i++) {
            createAccount("query" + (i + 1), viewers[i], "1380000002" + i, 4L);
        }
    }

    private void createAccount(String username, String realName, String phone, Long roleId) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            return;
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("admin123"));
        u.setRealName(realName);
        u.setPhone(phone);
        u.setDeptId(1L);
        u.setStatus(1);
        userMapper.insert(u);
        userRoleMapper.insert(new UserRole(null, u.getId(), roleId));
    }

    /** 物料补足到 50 个 */
    private List<Long> initDemoMaterials() {
        List<Long> ids = new ArrayList<>();
        for (long id = 1001L; id <= 1003L; id++) {
            ids.add(id);
        }
        String[] names = {"铝合金型材", "铜箔卷", "深沟球轴承", "直齿齿轮", "液压油", "O型密封圈",
                "控制电缆", "中间继电器", "接近传感器", "标准气缸", "不锈钢管", "冷轧钢板",
                "PVC管材", "伺服电机", "变频器", "气动阀", "步进电机", "线性导轨",
                "同步带轮", "工业滤网", "切削刀具", "润滑脂", "接线端子", "断路器", "按钮开关"};
        String[] types = {"RAW", "SEM", "FIN", "AUX", "PACK"};
        String[] units = {"kg", "件", "套", "米", "个", "桶", "箱", "只"};
        String[] specs = {"Φ20mm", "M12*50", "100*50*30", "DN25", "500ml", "2.5mm²", "24V", "0.75kW"};
        for (int i = 4; i <= 50; i++) {
            long mid = 1000L + i;
            ids.add(mid);
            String code = String.format("RM-%s-%06d", types[i % types.length], i);
            if (materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, code)) > 0) {
                continue;
            }
            String base = names[(i - 4) % names.length];
            Material mat = new Material();
            mat.setId(mid);
            mat.setMaterialCode(code);
            mat.setMaterialName(base + "-" + String.format("%02d", (i - 4) / names.length + 1));
            mat.setShortName(base);
            mat.setCategoryId(1L);
            mat.setSpec(specs[i % specs.length]);
            mat.setUnit(units[i % units.length]);
            mat.setMaterialType(types[i % types.length]);
            mat.setSafetyStock(new BigDecimal(50 + (i % 5) * 20));
            mat.setMaxStock(new BigDecimal(800 + (i % 7) * 100));
            mat.setBarCode("69" + String.format("%011d", i));
            mat.setStatus(1);
            materialMapper.insert(mat);
        }
        return ids;
    }

    /** 库存：1004/1005/1006 三条低于安全库存，与既有 1002 合计 4 条预警 */
    private void initDemoStocks() {
        insertStock(1L, 1L, 1004L, "30");
        insertStock(2L, 2L, 1005L, "20");
        insertStock(1L, 3L, 1006L, "25");
        for (int i = 7; i <= 40; i++) {
            long mid = 1000L + i;
            long wh = (i % 2 == 0) ? 1L : 2L;
            long loc = (wh == 1L) ? ((i % 3 == 0) ? 3L : 1L) : 2L;
            insertStock(wh, loc, mid, String.valueOf(200 + (i % 8) * 40));
        }
    }

    private void insertStock(Long warehouseId, Long locationId, Long materialId, String qty) {
        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getWarehouseId, warehouseId).eq(Stock::getLocationId, locationId)
                .eq(Stock::getMaterialId, materialId).eq(Stock::getBatchNo, "B20260801")) > 0) {
            return;
        }
        Stock s = new Stock();
        s.setWarehouseId(warehouseId);
        s.setLocationId(locationId);
        s.setMaterialId(materialId);
        s.setBatchNo("B20260801");
        s.setQuantity(new BigDecimal(qty));
        s.setLockedQuantity(BigDecimal.ZERO);
        s.setVersion(0);
        s.setUpdateTime(LocalDateTime.now());
        stockMapper.insert(s);
    }

    /** 单据：入库 100 / 出库 200 / 调拨 50 / 盘点 20（含 5 条待审核、8 条待执行） */
    private void initDemoOrders(List<Long> materialIds) {
        LocalDateTime now = LocalDateTime.now();
        String[] inTypes = {"PURCHASE", "PRODUCTION", "RETURN"};
        String[] outTypes = {"PICKUP", "SALE", "TRANSFER"};
        String[] stTypes = {"CYCLE", "RANDOM", "FULL"};
        int auditSeq = 0;

        // 入库 100：2 待审 / 3 待执行 / 5 草稿 / 90 已完成
        for (int i = 1; i <= 100; i++) {
            String status = i <= 2 ? OrderStatus.PENDING_AUDIT
                    : i <= 5 ? OrderStatus.PENDING_IN
                    : i <= 10 ? OrderStatus.DRAFT : OrderStatus.COMPLETED;
            BigDecimal qty = new BigDecimal(20 + (i % 15) * 10);
            InboundOrder o = new InboundOrder();
            o.setOrderNo(String.format("IN2026%05d", i));
            o.setInboundType(inTypes[i % inTypes.length]);
            o.setWarehouseId((i % 2 == 0) ? 1L : 2L);
            o.setStatus(status);
            o.setTotalQuantity(qty);
            o.setRemark("演示入库单 #" + i);
            if (OrderStatus.COMPLETED.equals(status)) {
                o.setAuditUser("admin");
                o.setAuditTime(now.minusDays(i % 30));
                o.setAuditRemark("同意");
            }
            inboundOrderMapper.insert(o);
            insertInboundItem(o.getId(), materialIds.get(i % materialIds.size()), qty);
            if (OrderStatus.COMPLETED.equals(status) && auditSeq < 30) {
                insertAuditRecord("INBOUND", o.getOrderNo(), now.minusDays(i % 30));
                auditSeq++;
            }
        }

        // 出库 200：2 待审 / 3 待执行 / 5 草稿 / 190 已完成
        for (int i = 1; i <= 200; i++) {
            String status = i <= 2 ? OrderStatus.PENDING_AUDIT
                    : i <= 5 ? OrderStatus.PENDING_OUT
                    : i <= 10 ? OrderStatus.DRAFT : OrderStatus.COMPLETED;
            BigDecimal qty = new BigDecimal(10 + (i % 12) * 5);
            OutboundOrder o = new OutboundOrder();
            o.setOrderNo(String.format("OUT2026%05d", i));
            o.setOutboundType(outTypes[i % outTypes.length]);
            o.setWarehouseId((i % 2 == 0) ? 1L : 2L);
            o.setStatus(status);
            o.setTotalQuantity(qty);
            o.setRemark("演示出库单 #" + i);
            if (OrderStatus.COMPLETED.equals(status)) {
                o.setAuditUser("admin");
                o.setAuditTime(now.minusDays(i % 30));
                o.setAuditRemark("同意");
            }
            outboundOrderMapper.insert(o);
            insertOutboundItem(o.getId(), materialIds.get(i % materialIds.size()), qty);
            if (OrderStatus.COMPLETED.equals(status) && auditSeq < 40) {
                insertAuditRecord("OUTBOUND", o.getOrderNo(), now.minusDays(i % 30));
                auditSeq++;
            }
        }

        // 调拨 50：1 待审 / 2 待执行 / 2 草稿 / 45 已完成
        for (int i = 1; i <= 50; i++) {
            String status = i == 1 ? OrderStatus.PENDING_AUDIT
                    : i <= 3 ? OrderStatus.PENDING
                    : i <= 5 ? OrderStatus.DRAFT : OrderStatus.COMPLETED;
            BigDecimal qty = new BigDecimal(15 + (i % 10) * 8);
            TransferOrder o = new TransferOrder();
            o.setOrderNo(String.format("TR2026%05d", i));
            o.setFromWarehouseId(1L);
            o.setToWarehouseId(2L);
            o.setFromLocationId(1L);
            o.setToLocationId(2L);
            o.setStatus(status);
            o.setTotalQuantity(qty);
            o.setItems(String.format("[{\"materialId\":%d,\"batchNo\":\"B20260801\",\"quantity\":%s}]",
                    materialIds.get(i % materialIds.size()), qty));
            o.setRemark("演示调拨单 #" + i);
            if (OrderStatus.COMPLETED.equals(status)) {
                o.setAuditUser("admin");
                o.setAuditTime(now.minusDays(i % 30));
                o.setAuditRemark("同意");
            }
            transferOrderMapper.insert(o);
        }

        // 盘点 20：2 草稿 / 18 已完成
        for (int i = 1; i <= 20; i++) {
            String status = i <= 2 ? OrderStatus.DRAFT : OrderStatus.COMPLETED;
            BigDecimal book = new BigDecimal(100 + (i % 9) * 20);
            BigDecimal actual = book.add(new BigDecimal((i % 5) - 2));
            StocktakeOrder o = new StocktakeOrder();
            o.setOrderNo(String.format("PD2026%05d", i));
            o.setWarehouseId((i % 2 == 0) ? 1L : 2L);
            o.setStatus(status);
            o.setStocktakeType(stTypes[i % stTypes.length]);
            o.setBookQuantity(book);
            o.setActualQuantity(actual);
            o.setDiffQuantity(actual.subtract(book));
            o.setItems(String.format(
                    "[{\"materialId\":%d,\"batchNo\":\"B20260801\",\"locationId\":1,\"bookQuantity\":%s,\"actualQuantity\":%s}]",
                    materialIds.get(i % materialIds.size()), book, actual));
            o.setRemark("演示盘点单 #" + i);
            if (OrderStatus.COMPLETED.equals(status)) {
                o.setAuditUser("admin");
                o.setAuditTime(now.minusDays(i % 30));
                o.setAuditRemark("同意");
            }
            stocktakeOrderMapper.insert(o);
        }
    }

    private void insertInboundItem(Long orderId, Long materialId, BigDecimal qty) {
        InboundItem it = new InboundItem();
        it.setOrderId(orderId);
        it.setMaterialId(materialId);
        it.setBatchNo("B20260801");
        it.setLocationId(1L);
        it.setPlanQuantity(qty);
        it.setActualQuantity(qty);
        it.setUnitPrice(new BigDecimal("12.50"));
        it.setRemark("演示明细");
        inboundItemMapper.insert(it);
    }

    private void insertOutboundItem(Long orderId, Long materialId, BigDecimal qty) {
        OutboundItem it = new OutboundItem();
        it.setOrderId(orderId);
        it.setMaterialId(materialId);
        it.setBatchNo("B20260801");
        it.setLocationId(1L);
        it.setPlanQuantity(qty);
        it.setActualQuantity(qty);
        it.setUnitPrice(new BigDecimal("18.00"));
        it.setRemark("演示明细");
        outboundItemMapper.insert(it);
    }

    private void insertAuditRecord(String businessType, String orderNo, LocalDateTime time) {
        AuditRecord r = new AuditRecord();
        r.setBusinessType(businessType);
        r.setBusinessOrderNo(orderNo);
        r.setAuditUser("admin");
        r.setAuditResult(1);
        r.setAuditRemark("演示审核意见：同意");
        r.setAuditTime(time);
        auditRecordMapper.insert(r);
    }

    private Menu m(Long id, Long parentId, String name, String perm, int type, String path, String icon, int sort) {
        Menu menu = new Menu();
        menu.setId(id); menu.setParentId(parentId); menu.setMenuName(name);
        menu.setPermCode(perm); menu.setMenuType(type);
        menu.setPath(path); menu.setIcon(icon); menu.setSort(sort); menu.setStatus(1);
        return menu;
    }

    private Role r(Long id, String code, String name, String remark) {
        Role role = new Role();
        role.setId(id); role.setRoleCode(code); role.setRoleName(name);
        role.setStatus(1); role.setRemark(remark);
        return role;
    }
}
