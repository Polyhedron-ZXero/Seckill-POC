package com.jiuzhang.seckill.controller;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.jiuzhang.seckill.util.Constant;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Controller
public class FlowControlController {

    private static final int QPS = 2;

    /**
     * 定义并加载限流规则（全局只能运行一次）
     */
    @PostConstruct // 当前类的构造函数执行完之后执行
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        rules.add(seckillActivitiesRule()); // 每个资源都需要一个限流规则
        rules.add(seckillPlaceOrderRule());
        FlowRuleManager.loadRules(rules);
    }

    /**
     * 创建限流规则：秒杀活动列表
     */
    private FlowRule seckillActivitiesRule() {
        FlowRule seckillActivitiesRule = new FlowRule();
        seckillActivitiesRule.setResource(Constant.FLOW_RULE_SECKILL_LIST); // 定义资源名称，表示 Sentinel 会对此名称的资源生效
        seckillActivitiesRule.setGrade(RuleConstant.FLOW_GRADE_QPS); // 定义限流规则类型：QPS 类型
        seckillActivitiesRule.setCount(QPS); // 定义每秒可通过的请求数
        return seckillActivitiesRule;
    }

    /**
     * 创建限流规则：下单抢购
     */
    private FlowRule seckillPlaceOrderRule() {
        FlowRule seckillPlaceOrderRule = new FlowRule();
        seckillPlaceOrderRule.setResource(Constant.FLOW_RULE_PLACE_ORDER);
        seckillPlaceOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        seckillPlaceOrderRule.setCount(QPS);
        return seckillPlaceOrderRule;
    }

}
