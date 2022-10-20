package com.jiuzhang.seckill.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.service.SeckillActivityService;
import com.jiuzhang.seckill.service.cache.RedisService;
import com.jiuzhang.seckill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * "@Controller"：用于标记在一个类上，使用它标记的类是一个 Spring MVC Controller 对象。
 * "@Autowired"：Spring 的自动装配注解，对类成员变量、方法及构造函数进行标注，完成自动装配。
 * "@Resource"：JDK 的自动装配注解，对类成员变量、方法及构造函数进行标注，完成自动装配。
 * "@ResponseBody"：将 Controller 的方法返回的对象通过适当的转换器转换为指定的格式之后，写入到 response 对象的 body 区，通常用来返回 JSON 或 XML 数据。
 * "@RequestMapping"：用来处理请求地址映射的注解，可用于类或方法上。用于类上，表示类中的所有响应请求的方法都是以该地址作为父路径。
 * <p>
 * "@Autowired" 和 "@Resource" 的区别：
 * 两者都可以用来装配 Bean，都可以写在字段上，或写在 setter 方法上。
 * "@Autowired" 是 Spring 的注解，默认按类型装配，默认情况下要求依赖对象必须存在，如果要允许 null 值，可以设置它的 required 属性为 false，如：@Autowired(required=false)。如果想使用名称装配可以结合 @Qualifier 注解进行使用。
 *              （由于是按类型装配，所以当声明了两个相同类型的变量时，Spring 会无法区分而造成注入失败。）
 * "@Resource" 是 JDK 的注解，默认按照名称进行装配，名称可以通过 name 属性进行指定，如果没有指定 name 属性，当注解写在字段上时，默认取字段名进行安装名称查找，如果注解写在 setter 方法上默认取属性名进行装配。当找不到与名称匹配的 Bean 时才按照类型进行装配。
 *             但是需要注意的是，name 属性一旦指定，就只会按照名称进行装配。
 */
@Slf4j
@Controller
public class SeckillActivityController {

    private static final boolean ENABLE_PURCHASE_LIMIT = true;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Autowired
    private OrderDao orderDao;

    @Resource
    private RedisService redisService;


    /**
     * 创建秒杀活动
     */
    @RequestMapping("/addSeckillActivity") // 访问此 URL 路径则调用此方法
    public String addSeckillActivity() {
        return "add_activity"; // 对应 resources/templates 中的同名 HTML 文件（由 Spring Boot 自动映射）
    }

    /**
     * 秒杀活动创建成功
     */
    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            // 对应提交表单 <input> 中的 name 属性值
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("originalPrice") BigDecimal originalPrice,
            @RequestParam("seckillQuantity") long seckillQuantity,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap
    ) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();

        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(originalPrice);
        seckillActivity.setTotalStock(seckillQuantity);
        seckillActivity.setAvailableStock((int) seckillQuantity);
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));

        seckillActivityDao.insertSeckillActivity(seckillActivity);
        resultMap.put("seckillActivity", seckillActivity); // 返回页可以通过 "${seckillActivity.成员变量}" 获取对象中的成员变量值
        return "add_success";
    }

    /**
     * 秒杀活动列表
     */
    @RequestMapping("/seckills")
    public String activityList(Map<String, Object> resultMap) {
        try (Entry entry = SphU.entry(Constant.FLOW_RULE_SECKILL_LIST)) { // 加载限流规则
            List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitiesByStatus(Constant.ORDER_STATUS_UNPAID);
            resultMap.put("seckillActivities", seckillActivities);
            return "seckill_activity";

        } catch (BlockException be) {
            log.warn("查询秒杀活动列表被限流");
            resultMap.put("redirectPath", "/seckills");
            return "busy";
        }
    }

    /**
     * 秒杀商品详情
     */
    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(@PathVariable long seckillActivityId, Map<String, Object> resultMap) {
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;

        String seckillActivityData = redisService.getValue(Constant.REDIS_KEY_ACTIVITY_DATA + seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityData)) {
            log.info("来自 Redis 缓存数据：" + seckillActivityData);
            seckillActivity = JSON.parseObject(seckillActivityData, SeckillActivity.class);
        } else {
            seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }

        String seckillCommodityData = redisService.getValue(Constant.REDIS_KEY_COMMODITY_DATA + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityData)) {
            log.info("来自 Redis 缓存数据：" + seckillCommodityData);
            seckillCommodity = JSON.parseObject(seckillCommodityData, SeckillCommodity.class);
        } else {
            seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        }

        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);

        return "seckill_item";
    }

    /**
     * 简单处理抢购请求（无法正确处理并行请求，会造成库存超卖）
     */
//    @ResponseBody
//    @RequestMapping("seckill/{seckillActivityId}")
    public String seckillSimple(@PathVariable long seckillActivityId) {
        return seckillActivityService.processSeckill(seckillActivityId);
    }

    /**
     * 处理抢购请求（由 seckill_item.html 跳转传入的用户 ID 为固定值：1）
     *
     * @return Spring 框架的 ModelAndView，主要用于前端解析（也可以使用 MyBatis 的 resultMap + 返回页面名称 String，但 resultMap 主要用于后台数据处理）
     */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillPlaceOrder(@PathVariable long userId, @PathVariable long seckillActivityId) {
        ModelAndView modelAndView = new ModelAndView();

        try (Entry entry = SphU.entry(Constant.FLOW_RULE_PLACE_ORDER)) {
            try {
                // 判断用户是否在已购名单中（缓存）
                if (ENABLE_PURCHASE_LIMIT && redisService.isInPurchasedList(seckillActivityId, userId)) {
                    modelAndView.addObject("resultInfo", "对不起，您已购买过此商品");
                    modelAndView.setViewName("seckill_result");
                    return modelAndView;
                }

                // 在缓存中确认是否能够抢购
                boolean stockValidated = seckillActivityService.deductStockInCache(seckillActivityId);

                if (stockValidated) {
                    Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                    modelAndView.addObject("resultInfo", "恭喜，抢购成功！订单创建中……　订单 ID：" + order.getOrderNo());
                    modelAndView.addObject("orderNo", order.getOrderNo());
                    // 添加用户至已购名单（缓存）
                    redisService.addToPurchasedList(seckillActivityId, userId);
                } else {
                    modelAndView.addObject("resultInfo","抱歉，抢购失败，商品被抢完了……");
                }
            } catch (Exception e) {
                log.error("秒杀系统异常：" + e);
                modelAndView.addObject("resultInfo", "系统异常，秒杀失败");
            }

            modelAndView.setViewName("seckill_result");

        } catch (BlockException be) {
            log.warn("抢购商品被限流：seckillActivityId=" + seckillActivityId);
            modelAndView.addObject("redirectPath", "/item/" + seckillActivityId);
            modelAndView.setViewName("busy");
        }

        return modelAndView;
    }

    /**
     * 订单查询
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(@PathVariable String orderNo) {
        log.info("订单查询，订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();

        if (order != null) {
            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.addObject("redirectPath", "/seckill/orderQuery/" + orderNo);
            modelAndView.setViewName("order_wait");
        }

        return modelAndView;
    }

    /**
     * 订单支付
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public ModelAndView payOrder(@PathVariable String orderNo) throws Exception {
        boolean paySuccess = seckillActivityService.payOrderProcess(orderNo);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("orderNo", orderNo);
        return new ModelAndView(paySuccess ? "redirect:/seckill/orderQuery/" + orderNo : "pay_error");
    }

    /**
     * 获取当前服务器端的时间
     */
    @ResponseBody // 返回字符串本身而非映射到同名 HTML 文件
    @RequestMapping("/seckill/systemTime")
    public String getSystemTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

}
