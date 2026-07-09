package com.warehub.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehub.entity.*;
import com.warehub.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 单据编号生成器
 * 规则: 前缀 + yyyyMMdd + 4位流水号，如 IN202607090001
 */
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final StocktakeOrderMapper stocktakeOrderMapper;

    private final Object lock = new Object();

    private String prefixOf(String orderType) {
        return switch (orderType) {
            case "INBOUND" -> "IN";
            case "OUTBOUND" -> "OUT";
            case "TRANSFER" -> "TR";
            case "STOCKTAKE" -> "PD";
            default -> "NO";
        };
    }

    /** 生成单据号（线程安全） */
    public String next(String orderType) {
        synchronized (lock) {
            String prefix = prefixOf(orderType);
            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String likePrefix = prefix + date;
            String maxNo = switch (orderType) {
                case "INBOUND" -> inboundOrderMapper.maxOrderNo(likePrefix);
                case "OUTBOUND" -> outboundOrderMapper.maxOrderNo(likePrefix);
                case "TRANSFER" -> transferOrderMapper.maxOrderNo(likePrefix);
                case "STOCKTAKE" -> stocktakeOrderMapper.maxOrderNo(likePrefix);
                default -> null;
            };
            int seq = 1;
            if (maxNo != null && maxNo.length() >= 14) {
                // 序列号固定在 position 10-13（4位），position 14+ 是随机后缀
                seq = Integer.parseInt(maxNo.substring(10, 14)) + 1;
            }
            // 追加3位随机数降低多实例碰撞概率（真正多实例请用数据库序列）
            return prefix + date + String.format("%04d", seq) + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        }
    }
}
